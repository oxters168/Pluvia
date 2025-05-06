package com.OxGames.Pluvia.ui

import android.content.Intent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.OxGames.Pluvia.BuildConfig
import com.OxGames.Pluvia.PluviaApp
import com.OxGames.Pluvia.PrefManager
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.enums.AppTheme
import com.OxGames.Pluvia.enums.DialogType
import com.OxGames.Pluvia.enums.LoginResult
import com.OxGames.Pluvia.events.AndroidEvent
import com.OxGames.Pluvia.service.SteamService
import com.OxGames.Pluvia.ui.component.dialog.state.MessageDialogState
import com.OxGames.Pluvia.ui.screen.HomeScreen
import com.OxGames.Pluvia.ui.screen.PluviaScreen
import com.OxGames.Pluvia.ui.screen.chat.ChatScreen
import com.OxGames.Pluvia.ui.screen.login.UserLoginScreen
import com.OxGames.Pluvia.ui.screen.settings.SettingsScreen
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun PluviaMain(
    viewModel: MainViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    val state by viewModel.state.collectAsStateWithLifecycle()

    var msgDialogState by rememberSaveable(stateSaver = MessageDialogState.Saver) {
        mutableStateOf(MessageDialogState(false))
    }
    val setMessageDialogState: (MessageDialogState) -> Unit = { msgDialogState = it }

    var hasBack by rememberSaveable { mutableStateOf(navController.previousBackStackEntry?.destination?.route != null) }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            Timber.i("Received UI event: $event")

            when (event) {
                MainViewModel.MainUiEvent.LaunchApp -> {
                    TODO()
                    // navController.navigate(PluviaScreen.XServer.route)
                }

                MainViewModel.MainUiEvent.OnBackPressed -> {
                    if (hasBack) {
                        // TODO: check if back leads to log out and present confidence modal
                        navController.popBackStack()
                    } else {
                        // TODO: quit app?
                    }
                }

                MainViewModel.MainUiEvent.OnLoggedOut -> {
                    navController.navigate(PluviaScreen.LoginUser.route) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }

                is MainViewModel.MainUiEvent.OnLogonEnded -> {
                    when (event.result) {
                        LoginResult.Success -> {
                            Timber.i("Navigating to library")
                            navController.navigate(PluviaScreen.Home.route) {
                                popUpTo(PluviaScreen.LoginUser.route) { inclusive = true }
                            }

                            // If a crash happen, lets not ask for a tip yet.
                            // Instead, ask the user to contribute their issues to be addressed.
                            if (!state.annoyingDialogShown && state.hasCrashedLastStart) {
                                viewModel.setAnnoyingDialogShown(true)
                                msgDialogState = MessageDialogState(
                                    visible = true,
                                    type = DialogType.CRASH,
                                    title = R.string.dialog_title_recent_crash,
                                    message = context.getString(R.string.dialog_message_crash),
                                    confirmBtnText = R.string.ok,
                                )
                            } else if (!(PrefManager.tipped || BuildConfig.GOLD) && !state.annoyingDialogShown) {
                                viewModel.setAnnoyingDialogShown(true)
                                msgDialogState = MessageDialogState(
                                    visible = true,
                                    type = DialogType.SUPPORT,
                                    message = context.getString(R.string.dialog_message_tip),
                                    confirmBtnText = R.string.tip,
                                    dismissBtnText = R.string.close,
                                )
                            }
                        }

                        else -> Timber.i("Received non-result: ${event.result}")
                    }
                }
            }
        }
    }

    LaunchedEffect(lifecycleOwner) {
        if (!state.isSteamConnected) {
            val intent = Intent(context, SteamService::class.java)
            context.startForegroundService(intent)
        }

        // Go to the Home screen if we're already logged in.
        if (SteamService.isLoggedIn.value && state.currentScreen == PluviaScreen.LoginUser) {
            navController.navigate(PluviaScreen.Home.route)
        }
    }

    PluviaTheme(
        isDark = when (state.appTheme) {
            AppTheme.AUTO -> isSystemInDarkTheme()
            AppTheme.DAY -> false
            AppTheme.NIGHT -> true
            AppTheme.AMOLED -> true
        },
        isAmoled = (state.appTheme == AppTheme.AMOLED),
        style = state.paletteStyle,
    ) {
        NavHost(
            navController = navController,
            startDestination = PluviaScreen.LoginUser.route,
            builder = {
                /** Login **/
                composable(route = PluviaScreen.LoginUser.route) {
                    UserLoginScreen()
                }
                /** Library, Downloads, Friends **/
                composable(
                    route = PluviaScreen.Home.route,
                    deepLinks = listOf(navDeepLink { uriPattern = "pluvia://home" }),
                ) {
                    HomeScreen(
                        onClickPlay = { launchAppId, asContainer ->
                            Timber.i("onClickPlay: $launchAppId, $asContainer")
                            TODO()
                        },
                        onClickExit = {
                            PluviaApp.events.emit(AndroidEvent.EndProcess)
                        },
                        onChat = {
                            navController.navigate(PluviaScreen.Chat.route(it))
                        },
                        onSettings = {
                            navController.navigate(PluviaScreen.Settings.route)
                        },
                        onLogout = {
                            scope.launch {
                                viewModel.service.serviceConnection!!.logOut()
                            }
                        },
                    )
                }

                /** Full Screen Chat **/
                composable(
                    route = "chat/{id}",
                    arguments = listOf(
                        navArgument(PluviaScreen.Chat.ARG_ID) {
                            type = NavType.LongType
                        },
                    ),
                ) {
                    val id = it.arguments?.getLong(PluviaScreen.Chat.ARG_ID) ?: throw RuntimeException("Unable to get ID to chat")
                    ChatScreen(
                        friendId = id,
                        onBack = {
                            CoroutineScope(Dispatchers.Main).launch {
                                navController.popBackStack()
                            }
                        },
                    )
                }

                /** Game Screen **/
                composable(route = PluviaScreen.XServer.route) {
                    // XServerScreen(
                    //     appId = state.launchedAppId,
                    //     bootToContainer = state.bootToContainer,
                    //     navigateBack = {
                    //         CoroutineScope(Dispatchers.Main).launch {
                    //             navController.popBackStack()
                    //         }
                    //     },
                    //     onWindowMapped = { window ->
                    //         viewModel.onWindowMapped(window, state.launchedAppId)
                    //     },
                    //     onExit = {
                    //         viewModel.exitSteamApp(context, state.launchedAppId)
                    //     },
                    // )
                }

                /** Settings **/
                composable(route = PluviaScreen.Settings.route) {
                    SettingsScreen(
                        service = viewModel.service,
                        appTheme = state.appTheme,
                        paletteStyle = state.paletteStyle,
                        onAppTheme = viewModel::setTheme,
                        onPaletteStyle = viewModel::setPalette,
                        onBack = { navController.navigateUp() },
                    )
                }
            },
        )
    }
}
