package com.OxGames.Pluvia.ui.screen.settings

import android.content.res.Configuration
import android.os.Bundle
import android.os.Parcelable
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

private const val BUNDLE_CONTENT_TYPE = "content_type"

@Parcelize
class SettingsPaneDetails(val currentPane: SettingsCurrentPane, val extras: Bundle? = null) : Parcelable

enum class SettingsCurrentPane(@StringRes val string: Int?) {
    DETAIL_EMULATION(R.string.settings_group_emulation),
    DETAIL_INTERFACE(R.string.settings_group_interface),
    DETAIL_DEBUG(R.string.settings_group_debug),
    EXTRA_CRASH_LOG(null),
    NONE(null),
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
    val navigator = rememberListDetailPaneScaffoldNavigator<SettingsPaneDetails>()
    val scope = rememberCoroutineScope()
    val snackBarHostState = remember { SnackbarHostState() }
    val windowWidth = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    fun navigateToDetailPane(paneType: SettingsCurrentPane, contentType: SettingsCurrentPane) {
        val extras = Bundle().apply {
            putString(BUNDLE_CONTENT_TYPE, contentType.name)
        }
        scope.launch {
            navigator.navigateTo(
                pane = ListDetailPaneScaffoldRole.Detail,
                contentKey = SettingsPaneDetails(paneType, extras),
            )
        }
    }

    fun navigateToExtraPane(detailContentType: SettingsCurrentPane, extraContentType: SettingsCurrentPane) {
        val extras = Bundle().apply {
            putString(BUNDLE_CONTENT_TYPE, detailContentType.name)
            putString("BUNDLE_EXTRA_CONTENT_TYPE", extraContentType.name)
        }

        scope.launch {
            navigator.navigateTo(
                pane = ListDetailPaneScaffoldRole.Extra,
                contentKey = SettingsPaneDetails(
                    currentPane = navigator.currentDestination?.contentKey?.currentPane ?: SettingsCurrentPane.NONE,
                    extras = extras,
                ),
            )
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
                            item { SettingsGroupMain(onClick = { navigateToDetailPane(it, it) }) }
                            item { SettingsGroupInfo() }
                            item { SettingsGroupCredits() }
                        },
                    )
                }
            }
        },
        detailPane = {
            val currentPane = navigator.currentDestination?.contentKey ?: SettingsPaneDetails(SettingsCurrentPane.NONE)
            val contentType = currentPane.extras?.getString(BUNDLE_CONTENT_TYPE)?.let {
                SettingsCurrentPane.valueOf(it)
            }

            AnimatedPane {
                Scaffold(
                    topBar = {
                        if (currentPane.currentPane != SettingsCurrentPane.NONE) {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        text = stringResource(contentType!!.string!!),
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
                            SettingsCurrentPane.DETAIL_EMULATION -> SettingsGroupEmulation()

                            SettingsCurrentPane.DETAIL_INTERFACE -> SettingsGroupInterface(
                                appTheme = appTheme,
                                paletteStyle = paletteStyle,
                                onAppTheme = onAppTheme,
                                onPaletteStyle = onPaletteStyle,
                            )

                            SettingsCurrentPane.DETAIL_DEBUG -> SettingsGroupDebug(
                                onShowLog = {
                                    navigateToExtraPane(SettingsCurrentPane.DETAIL_DEBUG, SettingsCurrentPane.EXTRA_CRASH_LOG)
                                },
                            )

                            else -> SettingsEmptyDetail()
                        }
                    }
                }
            }
        },
        extraPane = {
            val currentPane = navigator.currentDestination?.contentKey ?: SettingsPaneDetails(SettingsCurrentPane.NONE)
            val extraContentType = currentPane.extras?.getString("BUNDLE_EXTRA_CONTENT_TYPE")?.let {
                SettingsCurrentPane.valueOf(it)
            }
            AnimatedPane {
                Box(modifier = Modifier.fillMaxSize()) {
                    when (extraContentType) {
                        SettingsCurrentPane.EXTRA_CRASH_LOG -> {
                            SettingsLogViewerScreen(
                                onClose = {
                                    scope.launch { navigator.navigateBack() }
                                },
                                onToast = {
                                    scope.launch {
                                        snackBarHostState.showSnackbar(it)
                                        navigator.navigateBack()
                                    }
                                },
                            )
                        }

                        else -> SettingsEmptyDetail()

                    }
                }
            }
        },
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
    device = "spec:parent=pixel_5,orientation=landscape",
)
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
