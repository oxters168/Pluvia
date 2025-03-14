package com.OxGames.Pluvia.ui.screen.xserver

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.ui.component.dialog.MessageDialog
import com.OxGames.Pluvia.utils.ContainerUtils
import com.winlator.container.ContainerManager
import com.winlator.inputcontrols.TouchMouse
import com.winlator.widget.XServerView
import com.winlator.winhandler.WinHandler
import com.winlator.xserver.Keyboard
import com.winlator.xserver.Property
import com.winlator.xserver.ScreenInfo
import com.winlator.xserver.Window
import com.winlator.xserver.WindowManager
import com.winlator.xserver.XServer
import java.nio.file.Paths
import kotlin.io.path.name
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun XServerScreen(
    appId: Int,
    bootToContainer: Boolean,
    navigateBack: () -> Unit,
    onExit: () -> Unit,
    onWindowMapped: ((Window) -> Unit)? = null,
    onWindowUnmapped: ((Window) -> Unit)? = null,
    viewModel: XServerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val xServerState by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                XServerViewModel.XServerUiEvent.OnExit -> onExit()
                XServerViewModel.XServerUiEvent.OnNavigateBack -> navigateBack()
            }
        }
    }

    var exitDialogVisible by rememberSaveable { mutableStateOf(false) }
    MessageDialog(
        visible = exitDialogVisible,
        onDismissRequest = { exitDialogVisible = false },
        onConfirmClick = {
            scope.launch {
                viewModel.exit()
            }
            exitDialogVisible = false
        },
        onDismissClick = { exitDialogVisible = false },
        confirmBtnText = R.string.close,
        dismissBtnText = R.string.cancel,
        icon = Icons.AutoMirrored.Filled.ExitToApp,
        title = R.string.dialog_title_exit_game,
        message = "Are you sure you want to close ${xServerState.gameName.ifEmpty { stringResource(R.string.empty_game_name) }}?",
    )

    BackHandler {
        exitDialogVisible = true
    }

    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .pointerHoverIcon(PointerIcon(0))
            .pointerInteropFilter {
                viewModel.touchMouse?.onTouchEvent(it)
                true
            },
        factory = {
            Timber.i("Creating XServerView and XServer")

            viewModel.updateAppID(appId)

            XServerView(context, XServer(ScreenInfo(xServerState.screenSize))).apply {
                viewModel.xServerView = this

                renderer.isCursorVisible = false

                xServer.renderer = renderer
                xServer.winHandler = WinHandler(xServer, this)

                viewModel.touchMouse = TouchMouse(xServer)
                viewModel.keyboard = Keyboard(xServer)

                if (!bootToContainer) {
                    renderer.setUnviewableWMClasses("explorer.exe")
                    // TODO: make 'force fullscreen' be an option of the app being launched
                    viewModel.appLaunchInfo?.let { renderer.forceFullscreenWMClass = Paths.get(it.executable).name }
                }

                xServer.windowManager.addOnWindowModificationListener(
                    object : WindowManager.OnWindowModificationListener {
                        override fun onUpdateWindowContent(window: Window) {
                            if (!xServerState.winStarted && window.isApplicationWindow()) {
                                renderer.setCursorVisible(true)
                                viewModel.winStarted(true)
                            }
                        }

                        override fun onModifyWindowProperty(window: Window, property: Property) {
                        }

                        override fun onMapWindow(window: Window) {
                            Timber.i(
                                "onMapWindow:" +
                                    "\n\twindowName: ${window.name}" +
                                    "\n\twindowClassName: ${window.className}" +
                                    "\n\tprocessId: ${window.processId}" +
                                    "\n\thasParent: ${window.parent != null}" +
                                    "\n\tchildrenSize: ${window.children.size}",
                            )

                            viewModel.assignTaskAffinity(window, xServer.winHandler)

                            onWindowMapped?.invoke(window)
                        }

                        override fun onUnmapWindow(window: Window) {
                            Timber.i(
                                "onUnmapWindow:" +
                                    "\n\twindowName: ${window.name}" +
                                    "\n\twindowClassName: ${window.className}" +
                                    "\n\tprocessId: ${window.processId}" +
                                    "\n\thasParent: ${window.parent != null}" +
                                    "\n\tchildrenSize: ${window.children.size}",
                            )

                            // changeFrameRatingVisibility(window, null)
                            onWindowUnmapped?.invoke(window)
                        }
                    },
                )

                if (viewModel.xEnvironment != null) {
                    viewModel.xEnvironment = viewModel.shiftXEnvironmentToContext(xServer = xServer)
                } else {
                    val containerManager = ContainerManager(context)
                    val container = ContainerUtils.getContainer(context, appId)
                    containerManager.activateContainer(container)

                    viewModel.setBootConfig(container, containerManager, bootToContainer)
                }
            }
        },
        update = {},
        onRelease = {
            Timber.w("AndroidView Release")
        },
    )
}
