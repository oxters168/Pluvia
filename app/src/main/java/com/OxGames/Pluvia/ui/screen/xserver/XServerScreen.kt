package com.OxGames.Pluvia.ui.screen.xserver

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.OxGames.Pluvia.service.SteamService
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
import timber.log.Timber

@Composable
fun XServerScreen(
    appId: Int,
    bootToContainer: Boolean,
    navigateBack: () -> Unit,
    onExit: () -> Unit,
    onWindowMapped: ((Window) -> Unit)? = null,
    onWindowUnmapped: ((Window) -> Unit)? = null,
    xServerViewModel: XServerViewModel = viewModel(),
) {
    val context = LocalContext.current

    var firstTimeBoot: Boolean
    var taskAffinityMask = 0
    var taskAffinityMaskWoW64 = 0

    LaunchedEffect(Unit) {
        xServerViewModel.uiEvent.collect { event ->
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

    val appLaunchInfo = SteamService.getAppInfoOf(appId)?.let {
        SteamService.getWindowsLaunchInfos(appId).firstOrNull()
    }

    BackHandler {
        Timber.i("BackHandler")
        xServerViewModel.exit(xServerViewModel.xServerView!!.xServer.winHandler, xServerViewModel.xEnvironment, onExit)
    }

    // var launchedView by rememberSaveable { mutableStateOf(false) }
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .pointerHoverIcon(PointerIcon(0))
            .pointerInteropFilter {
                xServerViewModel.touchMouse?.onTouchEvent(it)
                true
            },
        factory = {
            Timber.i("Creating XServerView and XServer")

            XServerView(
                context,
                XServer(ScreenInfo(xServerState.value.screenSize)),
            ).apply {
                xServerViewModel.xServerView = this

                val renderer = this.renderer
                renderer.isCursorVisible = false

                xServer.renderer = renderer
                xServer.winHandler = WinHandler(xServer, this)
                xServerViewModel.touchMouse = TouchMouse(xServer)
                xServerViewModel.keyboard = Keyboard(xServer)

                if (!bootToContainer) {
                    renderer.setUnviewableWMClasses("explorer.exe")
                    // TODO: make 'force fullscreen' be an option of the app being launched
                    appLaunchInfo?.let { renderer.forceFullscreenWMClass = Paths.get(it.executable).name }
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
                            xServerViewModel.assignTaskAffinity(window, xServer.winHandler, taskAffinityMask, taskAffinityMaskWoW64)
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

                if (xServerViewModel.xEnvironment != null) {
                    xServerViewModel.xEnvironment = xServerViewModel.shiftXEnvironmentToContext(
                        context = context,
                        xEnvironment = xServerViewModel.xEnvironment!!,
                        xServer = xServer,
                    )
                } else {
                    val containerManager = ContainerManager(context)
                    val container = ContainerUtils.getContainer(context, appId)
                    // Timber.d("1 Container drives: ${container.drives}")
                    containerManager.activateContainer(container)
                    // Timber.d("2 Container drives: ${container.drives}")

                    taskAffinityMask = ProcessHelper.getAffinityMask(container.getCPUList(true)).toShort().toInt()
                    taskAffinityMaskWoW64 = ProcessHelper.getAffinityMask(container.getCPUListWoW64(true)).toShort().toInt()
                    firstTimeBoot = container.getExtra("appVersion").isEmpty()
                    Timber.i("First time boot: $firstTimeBoot")

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

                    xServerViewModel.setupWineSystemFiles(
                        context = context,
                        firstTimeBoot = firstTimeBoot,
                        screenInfo = xServerViewModel.xServerView!!.xServer.screenInfo,
                        xServerState = xServerState,
                        container = container,
                        containerManager = containerManager,
                        envVars = envVars,
                        onExtractFileListener = onExtractFileListener,
                    )
                    xServerViewModel.extractGraphicsDriverFiles(
                        context = context,
                        graphicsDriver = xServerState.value.graphicsDriver,
                        dxwrapper = xServerState.value.dxwrapper,
                        dxwrapperConfig = xServerState.value.dxwrapperConfig!!,
                        container = container,
                        envVars = envVars,
                    )
                    xServerViewModel.changeWineAudioDriver(xServerState.value.audioDriver, container, ImageFs.find(context))
                    xServerViewModel.xEnvironment = xServerViewModel.setupXEnvironment(
                        context = context,
                        appId = appId,
                        bootToContainer = bootToContainer,
                        xServerState = xServerState,
                        envVars = envVars,
                        container = container,
                        appLaunchInfo = appLaunchInfo,
                        xServer = xServerViewModel.xServerView!!.xServer,
                    )
                }
            }
        },
        update = {},
        onRelease = {},
    )
}
