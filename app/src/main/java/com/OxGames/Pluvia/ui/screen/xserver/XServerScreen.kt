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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.OxGames.Pluvia.ui.component.dialog.MessageDialog
import com.OxGames.Pluvia.ui.data.XServerState
import com.OxGames.Pluvia.utils.ContainerUtils
import com.winlator.container.ContainerManager
import com.winlator.core.DXVKHelper
import com.winlator.core.OnExtractFileListener
import com.winlator.core.ProcessHelper
import com.winlator.core.WineInfo
import com.winlator.core.envvars.EnvVars
import com.winlator.inputcontrols.TouchMouse
import com.winlator.widget.XServerView
import com.winlator.winhandler.WinHandler
import com.winlator.xenvironment.ImageFs
import com.winlator.xserver.Keyboard
import com.winlator.xserver.Property
import com.winlator.xserver.ScreenInfo
import com.winlator.xserver.Window
import com.winlator.xserver.WindowManager
import com.winlator.xserver.XServer
import java.io.File
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
    viewModel: XServerViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                XServerViewModel.XServerUiEvent.OnExit -> onExit()
                XServerViewModel.XServerUiEvent.OnNavigateBack -> navigateBack()
            }
        }
    }

    val xServerState = rememberSaveable(stateSaver = XServerState.Saver) {
        if (ContainerUtils.hasContainer(context, appId)) {
            val container = ContainerUtils.getContainer(context, appId)
            mutableStateOf(
                XServerState(
                    graphicsDriver = container.graphicsDriver,
                    audioDriver = container.audioDriver,
                    dxwrapper = container.dxWrapper,
                    dxwrapperConfig = DXVKHelper.parseConfig(container.dxWrapperConfig),
                    screenSize = container.screenSize,
                ),
            )
        } else {
            mutableStateOf(XServerState())
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
        confirmBtnText = "Close",
        dismissBtnText = "Cancel",
        icon = Icons.AutoMirrored.Filled.ExitToApp,
        title = "Exit Game",
        message = "Are you sure you want to close ${viewModel.gameName}?",
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

            viewModel.appId = appId

            XServerView(
                context,
                XServer(ScreenInfo(xServerState.value.screenSize)),
            ).apply {
                viewModel.xServerView = this

                val renderer = this.renderer
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
                            if (!xServerState.value.winStarted && window.isApplicationWindow()) {
                                renderer.setCursorVisible(true)
                                xServerState.value.winStarted = true
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
                    viewModel.xEnvironment = viewModel.shiftXEnvironmentToContext(
                        context = context,
                        xServer = xServer,
                    )
                } else {
                    val containerManager = ContainerManager(context)
                    val container = ContainerUtils.getContainer(context, appId)
                    // Timber.d("1 Container drives: ${container.drives}")
                    containerManager.activateContainer(container)
                    // Timber.d("2 Container drives: ${container.drives}")

                    viewModel.taskAffinityMask = ProcessHelper.getAffinityMask(container.getCPUList(true)).toShort().toInt()
                    viewModel.taskAffinityMaskWoW64 =
                        ProcessHelper.getAffinityMask(container.getCPUListWoW64(true)).toShort().toInt()
                    viewModel.firstTimeBoot = container.getExtra("appVersion").isEmpty()
                    Timber.i("First time boot: ${viewModel.firstTimeBoot}")

                    val wineVersion = container.wineVersion
                    xServerState.value = xServerState.value.copy(
                        wineInfo = WineInfo.fromIdentifier(context, wineVersion),
                    )

                    if (xServerState.value.wineInfo != WineInfo.MAIN_WINE_VERSION) {
                        ImageFs.find(context).winePath = xServerState.value.wineInfo.path
                    }

                    val onExtractFileListener = if (!xServerState.value.wineInfo.isWin64) {
                        OnExtractFileListener { destination, _ ->
                            destination?.path?.let {
                                if (it.contains("system32/")) {
                                    null
                                } else {
                                    File(it.replace("syswow64/", "system32/"))
                                }
                            }
                        }
                    } else {
                        null
                    }

                    Timber.i("Doing things once")
                    val envVars = EnvVars()

                    viewModel.setupWineSystemFiles(
                        context = context,
                        xServerState = xServerState,
                        container = container,
                        containerManager = containerManager,
                        envVars = envVars,
                        onExtractFileListener = onExtractFileListener,
                    )
                    viewModel.extractGraphicsDriverFiles(
                        context = context,
                        graphicsDriver = xServerState.value.graphicsDriver,
                        dxwrapper = xServerState.value.dxwrapper,
                        dxwrapperConfig = xServerState.value.dxwrapperConfig!!,
                        container = container,
                        envVars = envVars,
                    )
                    viewModel.changeWineAudioDriver(xServerState.value.audioDriver, container, ImageFs.find(context))
                    viewModel.xEnvironment = viewModel.setupXEnvironment(
                        context = context,
                        appId = appId,
                        bootToContainer = bootToContainer,
                        xServerState = xServerState,
                        envVars = envVars,
                        container = container,
                    )
                }
            }
        },
        update = {},
        onRelease = {},
    )
}
