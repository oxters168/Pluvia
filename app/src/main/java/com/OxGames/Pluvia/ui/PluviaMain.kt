package com.OxGames.Pluvia.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.OxGames.Pluvia.BuildConfig
import com.OxGames.Pluvia.Constants
import com.OxGames.Pluvia.PluviaApp
import com.OxGames.Pluvia.PrefManager
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.enums.AppTheme
import com.OxGames.Pluvia.enums.DialogType
import com.OxGames.Pluvia.enums.LoginResult
import com.OxGames.Pluvia.enums.Orientation
import com.OxGames.Pluvia.enums.PathType
import com.OxGames.Pluvia.enums.SaveLocation
import com.OxGames.Pluvia.enums.SyncResult
import com.OxGames.Pluvia.events.AndroidEvent
import com.OxGames.Pluvia.service.SteamService
import com.OxGames.Pluvia.ui.component.dialog.LoadingDialog
import com.OxGames.Pluvia.ui.component.dialog.MessageDialog
import com.OxGames.Pluvia.ui.component.dialog.state.MessageDialogState
import com.OxGames.Pluvia.ui.screen.HomeScreen
import com.OxGames.Pluvia.ui.screen.PluviaScreen
import com.OxGames.Pluvia.ui.screen.chat.ChatScreen
import com.OxGames.Pluvia.ui.screen.login.UserLoginScreen
import com.OxGames.Pluvia.ui.screen.settings.SettingsScreen
import com.OxGames.Pluvia.ui.screen.xserver.XServerScreen
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import com.OxGames.Pluvia.utils.ContainerUtils
import com.google.android.play.core.splitcompat.SplitCompat
import com.winlator.container.ContainerManager
import com.winlator.xenvironment.ImageFsInstaller
import `in`.dragonbra.javasteam.protobufs.steamclient.SteammessagesClientObjects.ECloudPendingRemoteOperation
import java.util.Date
import java.util.EnumSet
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
                    navController.navigate(PluviaScreen.XServer.route)
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

    LaunchedEffect(navController) {
        Timber.i("LaunchedEffect - navController changed")

        if (!state.hasLaunched) {
            viewModel.setHasLaunched(true)

            Timber.i("Creating on destination changed listener")

            PluviaApp.onDestinationChangedListener = NavController.OnDestinationChangedListener { _, destination, _ ->
                Timber.i("onDestinationChanged to ${destination.route}")
                // in order not to trigger the screen changed launch effect
                viewModel.setCurrentScreen(destination.route)
            }
            PluviaApp.events.emit(AndroidEvent.StartOrientator)
        } else {
            navController.removeOnDestinationChangedListener(PluviaApp.onDestinationChangedListener!!)
        }

        navController.addOnDestinationChangedListener(PluviaApp.onDestinationChangedListener!!)
    }

    // TODO merge to VM?
    LaunchedEffect(state.currentScreen) {
        Timber.i("LaunchedEffect - currentScreen: ${state.currentScreen}")

        // do the following each time we navigate to a new screen
        if (state.resettedScreen != state.currentScreen) {
            viewModel.setScreen()
            // Log.d("PluviaMain", "Screen changed to $currentScreen, resetting some values")
            // TODO: remove this if statement once XServerScreen orientation change bug is fixed
            if (state.currentScreen != PluviaScreen.XServer) {
                // reset system ui visibility
                PluviaApp.events.emit(AndroidEvent.SetSystemUIVisibility(true))
                // TODO: add option for user to set
                // reset available orientations
                PluviaApp.events.emit(AndroidEvent.SetAllowedOrientation(EnumSet.of(Orientation.UNSPECIFIED)))
            }
            // find out if back is available
            hasBack = navController.previousBackStackEntry?.destination?.route != null
        }
    }

    LaunchedEffect(lifecycleOwner) {
        if (!state.isSteamConnected) {
            Timber.i("Starting Steam service.")
            val intent = Intent(context, SteamService::class.java)
            context.startForegroundService(intent)
        }

        // Go to the Home screen if we're already logged in.
        if (SteamService.isLoggedIn && state.currentScreen == PluviaScreen.LoginUser) {
            Timber.i("Navigating to Home")
            navController.navigate(PluviaScreen.Home.route)
        }
    }

    val onDismissRequest: (() -> Unit)?
    val onDismissClick: (() -> Unit)?
    val onConfirmClick: (() -> Unit)?
    when (msgDialogState.type) {
        DialogType.SUPPORT -> {
            onConfirmClick = {
                uriHandler.openUri(Constants.Misc.KO_FI_LINK)
                PrefManager.tipped = true
                msgDialogState = MessageDialogState(visible = false)
            }
            onDismissRequest = {
                msgDialogState = MessageDialogState(visible = false)
            }
            onDismissClick = {
                msgDialogState = MessageDialogState(visible = false)
            }
        }

        DialogType.SYNC_CONFLICT -> {
            onConfirmClick = {
                preLaunchApp(
                    context = context,
                    appId = state.launchedAppId,
                    preferredSave = SaveLocation.Remote,
                    setLoadingDialogVisible = viewModel::setLoadingDialogVisible,
                    setLoadingProgress = viewModel::setLoadingDialogProgress,
                    setMessageDialogState = setMessageDialogState,
                    onSuccess = viewModel::launchApp,
                )
                msgDialogState = MessageDialogState(false)
            }
            onDismissClick = {
                preLaunchApp(
                    context = context,
                    appId = state.launchedAppId,
                    preferredSave = SaveLocation.Local,
                    setLoadingDialogVisible = viewModel::setLoadingDialogVisible,
                    setLoadingProgress = viewModel::setLoadingDialogProgress,
                    setMessageDialogState = setMessageDialogState,
                    onSuccess = viewModel::launchApp,
                )
                msgDialogState = MessageDialogState(false)
            }
            onDismissRequest = {
                msgDialogState = MessageDialogState(false)
            }
        }

        DialogType.SYNC_FAIL -> {
            onDismissClick = {
                setMessageDialogState(MessageDialogState(false))
            }
            onDismissRequest = {
                setMessageDialogState(MessageDialogState(false))
            }
            onConfirmClick = null
        }

        DialogType.PENDING_UPLOAD_IN_PROGRESS -> {
            onDismissClick = {
                setMessageDialogState(MessageDialogState(false))
            }
            onDismissRequest = {
                setMessageDialogState(MessageDialogState(false))
            }
            onConfirmClick = null
        }

        DialogType.PENDING_UPLOAD -> {
            onConfirmClick = {
                setMessageDialogState(MessageDialogState(false))
                preLaunchApp(
                    context = context,
                    appId = state.launchedAppId,
                    ignorePendingOperations = true,
                    setLoadingDialogVisible = viewModel::setLoadingDialogVisible,
                    setLoadingProgress = viewModel::setLoadingDialogProgress,
                    setMessageDialogState = setMessageDialogState,
                    onSuccess = viewModel::launchApp,
                )
            }
            onDismissClick = {
                setMessageDialogState(MessageDialogState(false))
            }
            onDismissRequest = {
                setMessageDialogState(MessageDialogState(false))
            }
        }

        DialogType.APP_SESSION_ACTIVE -> {
            onConfirmClick = {
                setMessageDialogState(MessageDialogState(false))
                preLaunchApp(
                    context = context,
                    appId = state.launchedAppId,
                    ignorePendingOperations = true,
                    setLoadingDialogVisible = viewModel::setLoadingDialogVisible,
                    setLoadingProgress = viewModel::setLoadingDialogProgress,
                    setMessageDialogState = setMessageDialogState,
                    onSuccess = viewModel::launchApp,
                )
            }
            onDismissClick = {
                setMessageDialogState(MessageDialogState(false))
            }
            onDismissRequest = {
                setMessageDialogState(MessageDialogState(false))
            }
        }

        DialogType.APP_SESSION_SUSPENDED -> {
            onDismissClick = {
                setMessageDialogState(MessageDialogState(false))
            }
            onDismissRequest = {
                setMessageDialogState(MessageDialogState(false))
            }
            onConfirmClick = null
        }

        DialogType.PENDING_OPERATION_NONE -> {
            onDismissClick = {
                setMessageDialogState(MessageDialogState(false))
            }
            onDismissRequest = {
                setMessageDialogState(MessageDialogState(false))
            }
            onConfirmClick = null
        }

        DialogType.MULTIPLE_PENDING_OPERATIONS -> {
            onDismissClick = {
                setMessageDialogState(MessageDialogState(false))
            }
            onDismissRequest = {
                setMessageDialogState(MessageDialogState(false))
            }
            onConfirmClick = null
        }

        DialogType.CRASH -> {
            onDismissClick = null
            onDismissRequest = {
                viewModel.setHasCrashedLastStart(false)
                setMessageDialogState(MessageDialogState(false))
            }
            onConfirmClick = {
                viewModel.setHasCrashedLastStart(false)
                setMessageDialogState(MessageDialogState(false))
            }
        }

        else -> {
            onDismissRequest = null
            onDismissClick = null
            onConfirmClick = null
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
        LoadingDialog(
            visible = state.loadingDialogVisible,
            progress = state.loadingDialogProgress,
        )

        MessageDialog(
            visible = msgDialogState.visible,
            onDismissRequest = onDismissRequest,
            onConfirmClick = onConfirmClick,
            confirmBtnText = msgDialogState.confirmBtnText,
            onDismissClick = onDismissClick,
            dismissBtnText = msgDialogState.dismissBtnText,
            icon = msgDialogState.type.icon,
            title = msgDialogState.title,
            message = msgDialogState.message,
        )

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

                            viewModel.setLaunchAppInfo(launchAppId, asContainer)

                            preLaunchApp(
                                context = context,
                                appId = launchAppId,
                                setLoadingDialogVisible = viewModel::setLoadingDialogVisible,
                                setLoadingProgress = viewModel::setLoadingDialogProgress,
                                setMessageDialogState = { msgDialogState = it },
                                onSuccess = viewModel::launchApp,
                            )
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
                            SteamService.logOut()
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
                    XServerScreen(
                        appId = state.launchedAppId,
                        bootToContainer = state.bootToContainer,
                        navigateBack = {
                            CoroutineScope(Dispatchers.Main).launch {
                                navController.popBackStack()
                            }
                        },
                        onWindowMapped = { window ->
                            viewModel.onWindowMapped(window, state.launchedAppId)
                        },
                        onExit = {
                            viewModel.exitSteamApp(context, state.launchedAppId)
                        },
                    )
                }

                /** Settings **/
                composable(route = PluviaScreen.Settings.route) {
                    SettingsScreen(
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

fun preLaunchApp(
    context: Context,
    appId: Int,
    ignorePendingOperations: Boolean = false,
    preferredSave: SaveLocation = SaveLocation.None,
    setLoadingDialogVisible: (Boolean) -> Unit,
    setLoadingProgress: (Float) -> Unit,
    setMessageDialogState: (MessageDialogState) -> Unit,
    onSuccess: () -> Unit,
) {
    Timber.i("Pre launching app: $appId, $ignorePendingOperations, $preferredSave")

    setLoadingDialogVisible(true)
    // TODO: add a way to cancel
    // TODO: add fail conditions
    CoroutineScope(Dispatchers.IO).launch {
        // set up Ubuntu file system
        SplitCompat.install(context)
        ImageFsInstaller.installIfNeededFuture(context, context.assets) { progress ->
            // Log.d("XServerScreen", "$progress")
            setLoadingProgress(progress / 100f)
        }.get()
        setLoadingProgress(-1f)

        // create container if it does not already exist
        // TODO: combine somehow with container creation in HomeLibraryAppScreen
        Timber.i("Loading container manager and getting container info")
        val containerManager = ContainerManager(context)
        val container = ContainerUtils.getOrCreateContainer(context, appId)
        // must activate container before downloading save files
        containerManager.activateContainer(container)
        Timber.i("Got container: ${container.name}")

        // sync save files and check no pending remote operations are running
        val prefixToPath: (String) -> String = { prefix ->
            PathType.from(prefix).toAbsPath(context, appId)
        }
        val postSyncInfo = SteamService.beginLaunchApp(
            appId = appId,
            prefixToPath = prefixToPath,
            ignorePendingOperations = ignorePendingOperations,
            preferredSave = preferredSave,
            parentScope = this,
        ).await()

        setLoadingDialogVisible(false)

        Timber.i("Post Sync Info was: $postSyncInfo")

        when (postSyncInfo.syncResult) {
            SyncResult.Conflict -> {
                setMessageDialogState(
                    MessageDialogState(
                        visible = true,
                        type = DialogType.SYNC_CONFLICT,
                        title = R.string.dialog_title_sync_conflict,
                        message = context.getString(
                            R.string.dialog_message_sync_conflict,
                            Date(postSyncInfo.localTimestamp).toString(),
                            Date(postSyncInfo.remoteTimestamp).toString(),
                        ),
                        dismissBtnText = R.string.dialog_action_keep_local,
                        confirmBtnText = R.string.dialog_action_keep_remote,
                    ),
                )
            }

            SyncResult.InProgress,
            SyncResult.UnknownFail,
            SyncResult.DownloadFail,
            SyncResult.UpdateFail,
            -> {
                setMessageDialogState(
                    MessageDialogState(
                        visible = true,
                        type = DialogType.SYNC_FAIL,
                        title = R.string.dialog_title_sync_error,
                        message = context.getString(R.string.dialog_message_sync_error, postSyncInfo.syncResult),
                        dismissBtnText = R.string.ok,
                    ),
                )
            }

            SyncResult.PendingOperations -> {
                Timber.i(
                    "Pending remote operations:${
                        postSyncInfo.pendingRemoteOperations.map { pro ->
                            "\n\tmachineName: ${pro.machineName}" +
                                "\n\ttimestamp: ${Date(pro.timeLastUpdated * 1000L)}" +
                                "\n\toperation: ${pro.operation}"
                        }.joinToString("\n")
                    }",
                )
                if (postSyncInfo.pendingRemoteOperations.size == 1) {
                    val pro = postSyncInfo.pendingRemoteOperations.first()
                    when (pro.operation) {
                        ECloudPendingRemoteOperation.k_ECloudPendingRemoteOperationUploadInProgress -> {
                            // maybe this should instead wait for the upload to finish and then
                            // launch the app
                            setMessageDialogState(
                                MessageDialogState(
                                    visible = true,
                                    type = DialogType.PENDING_UPLOAD_IN_PROGRESS,
                                    title = R.string.dialog_title_sync_upload,
                                    message = context.getString(
                                        R.string.dialog_message_sync_upload,
                                        SteamService.getAppInfoOf(appId)?.name,
                                        pro.machineName,
                                        Date(pro.timeLastUpdated * 1000L).toString(),
                                    ),
                                    dismissBtnText = R.string.ok,
                                ),
                            )
                        }

                        ECloudPendingRemoteOperation.k_ECloudPendingRemoteOperationUploadPending -> {
                            setMessageDialogState(
                                MessageDialogState(
                                    visible = true,
                                    type = DialogType.PENDING_UPLOAD,
                                    title = R.string.dialog_title_pending_upload,
                                    message = context.getString(
                                        R.string.dialog_message_pending_upload,
                                        SteamService.getAppInfoOf(appId)?.name,
                                        pro.machineName,
                                        Date(pro.timeLastUpdated * 1000L).toString(),
                                    ),
                                    confirmBtnText = R.string.dialog_action_play_anyway,
                                    dismissBtnText = R.string.cancel,
                                ),
                            )
                        }

                        ECloudPendingRemoteOperation.k_ECloudPendingRemoteOperationAppSessionActive -> {
                            setMessageDialogState(
                                MessageDialogState(
                                    visible = true,
                                    type = DialogType.APP_SESSION_ACTIVE,
                                    title = R.string.dialog_title_app_running,
                                    message = context.getString(
                                        R.string.dialog_message_app_running,
                                        pro.machineName,
                                        SteamService.getAppInfoOf(appId)?.name,
                                        Date(pro.timeLastUpdated * 1000L).toString(),
                                    ),
                                    confirmBtnText = R.string.dialog_action_play_anyway,
                                    dismissBtnText = R.string.cancel,
                                ),
                            )
                        }

                        ECloudPendingRemoteOperation.k_ECloudPendingRemoteOperationAppSessionSuspended -> {
                            // I don't know what this means, yet
                            setMessageDialogState(
                                MessageDialogState(
                                    visible = true,
                                    type = DialogType.APP_SESSION_SUSPENDED,
                                    title = R.string.dialog_title_sync_error,
                                    message = context.getString(R.string.dialog_message_sync_terminated),
                                    dismissBtnText = R.string.ok,
                                ),
                            )
                        }

                        ECloudPendingRemoteOperation.k_ECloudPendingRemoteOperationNone -> {
                            // why are we here
                            setMessageDialogState(
                                MessageDialogState(
                                    visible = true,
                                    type = DialogType.PENDING_OPERATION_NONE,
                                    title = R.string.dialog_title_sync_error,
                                    message = context.getString(R.string.dialog_message_sync_none),
                                    dismissBtnText = R.string.ok,
                                ),
                            )
                        }
                    }
                } else {
                    // this should probably be handled differently
                    setMessageDialogState(
                        MessageDialogState(
                            visible = true,
                            type = DialogType.MULTIPLE_PENDING_OPERATIONS,
                            title = R.string.dialog_title_sync_error,
                            message = context.getString(R.string.dialog_message_sync_multiple),
                            dismissBtnText = R.string.ok,
                        ),
                    )
                }
            }

            SyncResult.UpToDate,
            SyncResult.Success,
            -> onSuccess()
        }
    }
}
