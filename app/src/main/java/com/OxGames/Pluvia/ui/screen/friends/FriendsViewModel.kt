package com.OxGames.Pluvia.ui.screen.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.OxGames.Pluvia.PluviaApp
import com.OxGames.Pluvia.PrefManager
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.data.OwnedGames
import com.OxGames.Pluvia.db.dao.SteamFriendDao
import com.OxGames.Pluvia.events.SteamEvent
import com.OxGames.Pluvia.service.ServiceConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import `in`.dragonbra.javasteam.types.SteamID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class FriendsViewModel @Inject constructor(
    private val steamFriendDao: SteamFriendDao,
    private val service: ServiceConnectionManager,
) : ViewModel() {

    private val _friendsState = MutableStateFlow(FriendsState())
    val friendsState: StateFlow<FriendsState> = _friendsState.asStateFlow()

    private var selectedFriendJob: Job? = null
    private var observeFriendListJob: Job? = null

    private val onAliasHistory: (SteamEvent.OnAliasHistory) -> Unit = {
        _friendsState.update { currentState -> currentState.copy(profileFriendAlias = it.names) }
    }

    init {
        Timber.d("Initializing")

        observeFriendList()
        PluviaApp.events.on<SteamEvent.OnAliasHistory, Unit>(onAliasHistory)
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("onCleared")

        selectedFriendJob?.cancel()
        observeFriendListJob?.cancel()
        PluviaApp.events.off<SteamEvent.OnAliasHistory, Unit>(onAliasHistory)
    }

    fun observeSelectedFriend(friendID: Long) {
        selectedFriendJob?.cancel()

        // Force clear states when this method if is called again.
        _friendsState.update {
            it.copy(
                profileFriend = null,
                profileFriendGames = emptyList(),
                profileFriendInfo = null,
                profileFriendAlias = emptyList(),
            )
        }

        viewModelScope.launch {
            launch {
                val resp = service.serviceConnection!!.getProfileInfo(SteamID(friendID))
                _friendsState.update { it.copy(profileFriendInfo = resp) }
            }
            launch {
                val resp = service.serviceConnection!!.getOwnedGames(friendID).sortedWith(
                    compareBy<OwnedGames> { (it.sortAs ?: it.name).lowercase() }
                        .thenByDescending { it.playtimeTwoWeeks },
                )

                resp.forEach {
                    Timber.d(it.toString())
                }

                _friendsState.update { it.copy(profileFriendGames = resp) }
            }
            selectedFriendJob = launch {
                steamFriendDao.findFriendFlow(friendID).collect { friend ->
                    if (friend == null) {
                        Timber.w("Collecting friend was null")
                        return@collect
                    }
                    _friendsState.update { it.copy(profileFriend = friend) }
                }
            }
        }
    }

    fun onHeaderAction(value: String) {
        _friendsState.update { currentState ->
            val list = currentState.collapsedListSections.toMutableSet()
            if (value in list) {
                list.remove(value)
            } else {
                list.add(value)
            }
            PrefManager.friendsListHeader = list
            currentState.copy(collapsedListSections = list)
        }
    }

    fun onBlock(friendID: Long) {
        viewModelScope.launch {
            service.serviceConnection!!.blockFriend(friendID)
        }
    }

    fun onRemove(friendID: Long) {
        viewModelScope.launch {
            service.serviceConnection!!.removeFriend(friendID)
        }
    }

    fun onNickName(value: String) {
        viewModelScope.launch {
            service.serviceConnection!!.setNickName(_friendsState.value.profileFriend!!.id, value)
        }
    }

    fun onAlias() {
        viewModelScope.launch {
            service.serviceConnection!!.requestAliasHistory(_friendsState.value.profileFriend!!.id)
        }
    }

    private fun observeFriendList() {
        observeFriendListJob = viewModelScope.launch(Dispatchers.IO) {
            steamFriendDao.getAllFriendsFlow().collect { friends ->
                _friendsState.update { currentState ->
                    val sortedList = friends
                        .filter { it.isFriend && !it.isBlocked }
                        .sortedWith(
                            compareBy(
                                { it.isRequestRecipient.not() },
                                { it.isPlayingGame.not() },
                                { it.isInGameAwayOrSnooze },
                                { it.isOnline.not() },
                                { it.isAwayOrSnooze },
                                { it.isOffline.not() },
                                { it.nameOrNickname.lowercase() },
                            ),
                        )

                    val groupedList = sortedList.groupBy { friend ->
                        when {
                            friend.isRequestRecipient -> R.string.friend_sticky_request
                            friend.isPlayingGame || friend.isInGameAwayOrSnooze -> R.string.friend_sticky_game
                            friend.isOnline || friend.isAwayOrSnooze -> R.string.friend_sticky_online
                            else -> R.string.friend_sticky_offline
                        }
                    }.toMap()

                    currentState.copy(friendsList = groupedList)
                }
            }
        }
    }
}
