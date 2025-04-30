package com.OxGames.Pluvia.ui.screen.xserver

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

data object Window

@Composable
fun XServerScreen(
    appId: Int,
    bootToContainer: Boolean,
    navigateBack: () -> Unit,
    onExit: () -> Unit,
    onWindowMapped: ((Window) -> Unit)? = null,
    onWindowUnmapped: ((Window) -> Unit)? = null,
    // viewModel: XServerViewModel = hiltViewModel(),
) {
    TODO()
//    val context = LocalContext.current
//    val scope = rememberCoroutineScope()
//
//    val xServerState by viewModel.state.collectAsStateWithLifecycle()
//
//    LaunchedEffect(Unit) {
//        viewModel.uiEvent.collect { event ->
//            when (event) {
//                XServerViewModel.XServerUiEvent.OnExit -> onExit()
//                XServerViewModel.XServerUiEvent.OnNavigateBack -> navigateBack()
//            }
//        }
//    }
//
//    var exitDialogVisible by rememberSaveable { mutableStateOf(false) }
//    MessageDialog(
//        visible = exitDialogVisible,
//        onDismissRequest = { exitDialogVisible = false },
//        onConfirmClick = {
//            scope.launch {
//                viewModel.exit()
//            }
//            exitDialogVisible = false
//        },
//        onDismissClick = { exitDialogVisible = false },
//        confirmBtnText = R.string.close,
//        dismissBtnText = R.string.cancel,
//        icon = Icons.AutoMirrored.Filled.ExitToApp,
//        title = R.string.dialog_title_exit_game,
//        message = context.getString(
//            R.string.dialog_message_exit_game,
//            xServerState.gameName.ifEmpty { context.getString(R.string.xserver_no_game_name) },
//        ),
//    )
//
//    BackHandler {
//        exitDialogVisible = true
//    }
//
//    // Verify
//    val touchHandler = remember {
//        { event: MotionEvent ->
//            viewModel.touchMouse?.onTouchEvent(event)
//            true
//        }
//    }
//
//    AndroidView(
//        modifier = Modifier
//            .fillMaxSize()
//            .pointerHoverIcon(PointerIcon(0))
//            .pointerInteropFilter(onTouchEvent = touchHandler),
//        factory = {
//            Timber.i("Creating XServerView and XServer")
//
//            viewModel.init(appId)
//
//            XServerView(context, XServer(ScreenInfo(xServerState.screenSize))).apply {
//                viewModel.xServerView = this
//
//                renderer.isCursorVisible = false
//
//                xServer.renderer = renderer
//                xServer.winHandler = WinHandler(xServer, this)
//
//                viewModel.touchMouse = TouchMouse(xServer)
//                viewModel.keyboard = Keyboard(xServer)
//
//                if (!bootToContainer) {
//                    renderer.setUnviewableWMClasses("explorer.exe")
//                    // TODO: make 'force fullscreen' be an option of the app being launched
//                    viewModel.appLaunchInfo?.let { renderer.forceFullscreenWMClass = Paths.get(it.executable).name }
//                }
//
//                xServer.windowManager.addOnWindowModificationListener(
//                    object : WindowManager.OnWindowModificationListener {
//                        override fun onUpdateWindowContent(window: Window) {
//                            if (!xServerState.winStarted && window.isApplicationWindow()) {
//                                renderer.setCursorVisible(true)
//                                viewModel.winStarted(true)
//                            }
//                        }
//
//                        override fun onModifyWindowProperty(window: Window, property: Property) {
//                        }
//
//                        override fun onMapWindow(window: Window) {
//                            Timber.i(
//                                "onMapWindow:" +
//                                    "\n\twindowName: ${window.name}" +
//                                    "\n\twindowClassName: ${window.className}" +
//                                    "\n\tprocessId: ${window.processId}" +
//                                    "\n\thasParent: ${window.parent != null}" +
//                                    "\n\tchildrenSize: ${window.children.size}",
//                            )
//
//                            viewModel.assignTaskAffinity(window, xServer.winHandler)
//
//                            onWindowMapped?.invoke(window)
//                        }
//
//                        override fun onUnmapWindow(window: Window) {
//                            Timber.i(
//                                "onUnmapWindow:" +
//                                    "\n\twindowName: ${window.name}" +
//                                    "\n\twindowClassName: ${window.className}" +
//                                    "\n\tprocessId: ${window.processId}" +
//                                    "\n\thasParent: ${window.parent != null}" +
//                                    "\n\tchildrenSize: ${window.children.size}",
//                            )
//
//                            // changeFrameRatingVisibility(window, null)
//                            onWindowUnmapped?.invoke(window)
//                        }
//                    },
//                )
//
//                if (viewModel.xEnvironment != null) {
//                    viewModel.xEnvironment = viewModel.shiftXEnvironmentToContext(xServer = xServer)
//                } else {
//                    val containerManager = ContainerManager(context)
//                    val container = ContainerUtils.getContainer(context, appId)
//                    containerManager.activateContainer(container)
//
//                    viewModel.setBootConfig(container, containerManager, bootToContainer)
//                }
//            }
//        },
//        update = {},
//        onRelease = {
//            Timber.w("AndroidView Release")
//        },
//    )
}
