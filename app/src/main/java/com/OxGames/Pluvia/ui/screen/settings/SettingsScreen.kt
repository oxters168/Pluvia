package com.OxGames.Pluvia.ui.screen.settings

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowWidthSizeClass
import com.OxGames.Pluvia.PrefManager
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.enums.AppTheme
import com.OxGames.Pluvia.ui.component.topbar.BackButton
import com.OxGames.Pluvia.ui.screen.settings.components.SettingsEmptyDetail
import com.OxGames.Pluvia.ui.screen.settings.components.SettingsGroupCredits
import com.OxGames.Pluvia.ui.screen.settings.components.SettingsGroupDebug
import com.OxGames.Pluvia.ui.screen.settings.components.SettingsGroupEmulation
import com.OxGames.Pluvia.ui.screen.settings.components.SettingsGroupInfo
import com.OxGames.Pluvia.ui.screen.settings.components.SettingsGroupInterface
import com.OxGames.Pluvia.ui.screen.settings.components.SettingsGroupMain
import com.OxGames.Pluvia.ui.screen.settings.components.SettingsLogViewerScreen
import com.OxGames.Pluvia.ui.screen.settings.components.SettingsMiceWineDebug
import com.OxGames.Pluvia.ui.screen.settings.components.SettingsMiceWineDriver
import com.OxGames.Pluvia.ui.screen.settings.components.SettingsMiceWineDriverInfo
import com.OxGames.Pluvia.ui.screen.settings.components.SettingsMiceWineSound
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.launch

enum class SettingsCurrentPane(@StringRes val string: Int?) {
    DETAIL_EMULATION(R.string.settings_group_emulation),
    DETAIL_INTERFACE(R.string.settings_group_interface),
    DETAIL_DEBUG(R.string.settings_group_debug),
    NONE(null),
}

data class SettingsPane(
    val currentPane: SettingsCurrentPane = SettingsCurrentPane.NONE,
    val extra: String = "",
) {
    companion object {
        val Saver = Saver<SettingsPane, Pair<Int, String>>(
            save = { settingsPane ->
                Pair(settingsPane.currentPane.ordinal, settingsPane.extra)
            },
            restore = { savedState ->
                val (ordinal, extra) = savedState
                SettingsPane(
                    currentPane = SettingsCurrentPane.entries[ordinal],
                    extra = extra,
                )
            },
        )

    }
}

// See link for implementation
// https://github.com/alorma/Compose-Settings

@Composable
fun SettingsScreen(
    appTheme: AppTheme,
    paletteStyle: PaletteStyle,
    onAppTheme: (AppTheme) -> Unit,
    onPaletteStyle: (PaletteStyle) -> Unit,
    onBack: () -> Unit,
) {
    SettingsScreenContent(
        appTheme = appTheme,
        paletteStyle = paletteStyle,
        onAppTheme = onAppTheme,
        onPaletteStyle = onPaletteStyle,
        onBack = onBack,
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreenContent(
    appTheme: AppTheme,
    paletteStyle: PaletteStyle,
    onAppTheme: (AppTheme) -> Unit,
    onPaletteStyle: (PaletteStyle) -> Unit,
    onBack: () -> Unit,
) {
    val navigator = rememberListDetailPaneScaffoldNavigator<Nothing>()
    var currentPane by rememberSaveable(stateSaver = SettingsPane.Saver) { mutableStateOf(SettingsPane()) }

    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }
    val windowWidth = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    val onNavBack: () -> Unit = {
        scope.launch {
            currentPane = currentPane.copy(extra = "")
            navigator.navigateBack()
        }
    }

    BackHandler(enabled = navigator.canNavigateBack()) {
        scope.launch {
            navigator.navigateBack()
        }
    }

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            AnimatedPane {
                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text(text = stringResource(R.string.title_settings)) },
                            navigationIcon = { BackButton(onClick = onBack) },
                        )
                    },
                ) { paddingValues ->
                    LazyColumn(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        content = {
                            item {
                                SettingsGroupMain(
                                    onClick = {
                                        scope.launch {
                                            currentPane = currentPane.copy(currentPane = it)
                                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail)
                                        }
                                    },
                                )
                            }
                            item { SettingsGroupInfo() }
                            item { SettingsGroupCredits() }
                        },
                    )
                }
            }
        },
        detailPane = {
            AnimatedPane {
                Scaffold(
                    topBar = {
                        if (currentPane.currentPane != SettingsCurrentPane.NONE) {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        text = stringResource(currentPane.currentPane.string!!),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                navigationIcon = {
                                    if (windowWidth == WindowWidthSizeClass.COMPACT ||
                                        windowWidth == WindowWidthSizeClass.MEDIUM
                                    ) {
                                        BackButton(onClick = { scope.launch { navigator.navigateBack() } })
                                    }
                                },
                            )
                        }
                    },
                ) { paddingValues ->
                    Box(modifier = Modifier.padding(paddingValues)) {
                        when (currentPane.currentPane) {
                            SettingsCurrentPane.DETAIL_EMULATION -> SettingsGroupEmulation(
                                onClick = {
                                    scope.launch {
                                        currentPane = currentPane.copy(extra = it)
                                        navigator.navigateTo(ListDetailPaneScaffoldRole.Extra)
                                    }
                                },
                            )

                            SettingsCurrentPane.DETAIL_INTERFACE -> SettingsGroupInterface(
                                appTheme = appTheme,
                                paletteStyle = paletteStyle,
                                onAppTheme = onAppTheme,
                                onPaletteStyle = onPaletteStyle,
                            )

                            SettingsCurrentPane.DETAIL_DEBUG -> SettingsGroupDebug(
                                onShowLog = {
                                    scope.launch {
                                        currentPane = currentPane.copy(extra = "crash_log")
                                        navigator.navigateTo(ListDetailPaneScaffoldRole.Extra)
                                    }
                                },
                            )

                            else -> SettingsEmptyDetail()
                        }
                    }
                }
            }
        },
        extraPane = {
            AnimatedPane {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentPane.currentPane) {
                        SettingsCurrentPane.DETAIL_DEBUG -> {
                            if (currentPane.extra == "crash_log") {
                                SettingsLogViewerScreen(
                                    onClose = onNavBack,
                                    onToast = {
                                        scope.launch {
                                            snackBarHostState.showSnackbar(it)
                                            onNavBack()
                                        }
                                    },
                                )
                            }
                        }

                        SettingsCurrentPane.DETAIL_EMULATION -> {
                            when (currentPane.extra) {
                                "mw_general_debug" -> SettingsMiceWineDebug(onBack = onNavBack)
                                "mw_general_sound" -> SettingsMiceWineSound(onBack = onNavBack)
                                "mw_general_driver" -> SettingsMiceWineDriver(onBack = onNavBack)
                                "mw_general_driver_info" -> SettingsMiceWineDriverInfo(onBack = onNavBack)
                                "mw_general_environment" -> TODO()
                                "mw_controller_mapper" -> TODO()
                                "mw_controller_virtual_mapper" -> TODO()
                                "mw_box64_preset" -> TODO()
                                "mw_rat_package_manager" -> TODO()
                                "mw_rat_package_downloader" -> TODO()
                            }
                        }

                        else -> SettingsEmptyDetail()

                    }
                }
            }
        },
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview
@Composable
private fun Preview_SettingsScreen() {
    val context = LocalContext.current
    PrefManager.init(context)
    PluviaTheme {
        Surface {
            SettingsScreenContent(
                appTheme = AppTheme.DAY,
                paletteStyle = PaletteStyle.TonalSpot,
                onAppTheme = { },
                onPaletteStyle = { },
                onBack = { },
            )
        }
    }
}
