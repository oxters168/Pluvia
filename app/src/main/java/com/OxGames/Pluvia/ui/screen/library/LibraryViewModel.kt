package com.OxGames.Pluvia.ui.screen.library

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.OxGames.Pluvia.PrefManager
import com.OxGames.Pluvia.data.LibraryItem
import com.OxGames.Pluvia.data.SteamApp
import com.OxGames.Pluvia.db.dao.SteamAppDao
import com.OxGames.Pluvia.enums.AppFilter
import com.OxGames.Pluvia.service.SteamService
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.EnumSet
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val steamAppDao: SteamAppDao,
) : ViewModel() {

    private val _state: MutableStateFlow<LibraryState> = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    // Keep the library scroll state. This will last longer as the VM will stay alive.
    var listState: LazyListState by mutableStateOf(LazyListState(0, 0))

    // Complete and unfiltered app list
    private var appList: List<SteamApp> = emptyList()

    init {
        Timber.d("Initializing")

        viewModelScope.launch {
            steamAppDao.getAllOwnedApps().distinctUntilChanged().collect { apps ->
                Timber.tag("LibraryViewModel").d("Collecting ${apps.size} apps")

                appList = apps

                onFilterApps()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("onCleared")
    }

    fun onModalBottomSheet(value: Boolean) {
        _state.update { it.copy(modalBottomSheet = value) }
    }

    fun onIsSearching(value: Boolean) {
        _state.update { it.copy(isSearching = value) }
        if (!value && _state.value.searchQuery.isNotEmpty()) {
            onSearchQuery("")
        }
    }

    fun onSearchQuery(value: String) {
        _state.update { it.copy(searchQuery = value) }
        onFilterApps()
    }

    // TODO: include other sort types
    fun onFilterChanged(value: AppFilter) {
        _state.update { currentState ->
            val updatedFilter = currentState.appInfoSortType.clone() as EnumSet<AppFilter>

            if (updatedFilter.contains(value)) {
                updatedFilter.remove(value)
            } else {
                updatedFilter.add(value)
            }

            PrefManager.libraryFilter = updatedFilter

            currentState.copy(appInfoSortType = updatedFilter)
        }

        onFilterApps()
    }

    private fun onFilterApps() {
        if (_state.value.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }

            val currentState = _state.value
            val query = currentState.searchQuery
            val filters = currentState.appInfoSortType

            val userAccountId = SteamService.userSteamId!!.accountID.toInt()
            val familyMembersList = SteamService.familyMembers.ifEmpty { listOf(userAccountId) }
            val currentFilter = AppFilter.getAppType(filters)

            Timber.tag("LibraryViewModel").d("Applying filter: $currentFilter")

            val result = appList
                .asSequence()
                .filter { app ->
                    familyMembersList.any { app.ownerAccountId.contains(it) } &&
                        currentFilter.any { app.type == it } &&
                        (filters.contains(AppFilter.SHARED) || app.ownerAccountId.contains(userAccountId)) &&
                        (query.isEmpty() || app.name.contains(query, ignoreCase = true)) &&
                        (!filters.contains(AppFilter.INSTALLED) || SteamService.isAppInstalled(app.id))
                }
                .mapIndexed { idx, item ->
                    LibraryItem(
                        index = idx,
                        appId = item.id,
                        name = item.name,
                        iconHash = item.clientIconHash,
                        isShared = !item.ownerAccountId.contains(userAccountId),
                    )
                }
                .toList()

            Timber.tag("LibraryViewModel").d("Filtered list size: ${result.size}")

            _state.update { it.copy(appInfoList = result, isLoading = false) }
        }
    }
}
