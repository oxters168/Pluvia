package com.OxGames.Pluvia.ui.screen.xserver

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.OxGames.Pluvia.Constants
import com.OxGames.Pluvia.PluviaApp
import com.OxGames.Pluvia.PrefManager
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.data.LaunchInfo
import com.OxGames.Pluvia.events.AndroidEvent
import com.OxGames.Pluvia.events.SteamEvent
import com.OxGames.Pluvia.service.SteamService
import com.OxGames.Pluvia.ui.data.XServerState
import com.winlator.container.Container
import com.winlator.container.ContainerManager
import com.winlator.core.AppUtils
import com.winlator.core.Callback
import com.winlator.core.DXVKHelper
import com.winlator.core.DefaultVersion
import com.winlator.core.FileUtils
import com.winlator.core.GPUInformation
import com.winlator.core.KeyValueSet
import com.winlator.core.OnExtractFileListener
import com.winlator.core.ProcessHelper
import com.winlator.core.TarCompressorUtils
import com.winlator.core.WineInfo
import com.winlator.core.WineRegistryEditor
import com.winlator.core.WineStartMenuCreator
import com.winlator.core.WineThemeManager
import com.winlator.core.WineUtils
import com.winlator.core.envvars.EnvVars
import com.winlator.inputcontrols.ExternalController
import com.winlator.inputcontrols.TouchMouse
import com.winlator.widget.XServerView
import com.winlator.winhandler.WinHandler
import com.winlator.xconnector.UnixSocketConfig
import com.winlator.xenvironment.ImageFs
import com.winlator.xenvironment.XEnvironment
import com.winlator.xenvironment.components.ALSAServerComponent
import com.winlator.xenvironment.components.GuestProgramLauncherComponent
import com.winlator.xenvironment.components.NetworkInfoUpdateComponent
import com.winlator.xenvironment.components.PulseAudioComponent
import com.winlator.xenvironment.components.SteamClientComponent
import com.winlator.xenvironment.components.SysVSharedMemoryComponent
import com.winlator.xenvironment.components.VirGLRendererComponent
import com.winlator.xenvironment.components.XServerComponent
import com.winlator.xserver.Keyboard
import com.winlator.xserver.Window
import com.winlator.xserver.XServer
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

class XServerViewModel : ViewModel() {

    @SuppressLint("StaticFieldLeak")
    internal var xServerView: XServerView? = null

    internal var xEnvironment: XEnvironment? = null

    internal var touchMouse: TouchMouse? = null
    internal var keyboard: Keyboard? = null

    // Some of the variables below `could` be in a state.

    internal var gameName: String = "your game"

    var appId: Int = -1
    val appLaunchInfo: LaunchInfo?
        get() {
            if (appId < 0) {
                Timber.w("AppID is $appId")
                return null
            }

            return SteamService.getAppInfoOf(appId)?.let {
                gameName = it.name
                SteamService.getWindowsLaunchInfos(appId).firstOrNull()
            }
        }

    var firstTimeBoot: Boolean = false
    var taskAffinityMask = 0
    var taskAffinityMaskWoW64 = 0

    sealed class XServerUiEvent {
        data object OnExit : XServerUiEvent()
        data object OnNavigateBack : XServerUiEvent()
    }

    private val _uiEvent = Channel<XServerUiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val onActivityDestroyed: (AndroidEvent.ActivityDestroyed) -> Unit = {
        Timber.i("onActivityDestroyed")
        viewModelScope.launch {
            exit()
        }
    }

    private val onKeyEvent: (AndroidEvent.KeyEvent) -> Boolean = {
        val isKeyboard = Keyboard.isKeyboardDevice(it.event.device)
        val isGamepad = ExternalController.isGameController(it.event.device)

        var handled = false
        if (isGamepad) {
            handled = xServerView!!.xServer.winHandler.onKeyEvent(it.event)
            // handled = ExternalController.onKeyEvent(xServer.winHandler, it.event)
        }
        if (!handled && isKeyboard) {
            handled = keyboard?.onKeyEvent(it.event) == true
        }

        handled
    }

    private val onMotionEvent: (AndroidEvent.MotionEvent) -> Boolean = {
        val isMouse = TouchMouse.isMouseDevice(it.event?.device)
        val isGamepad = ExternalController.isGameController(it.event?.device)

        var handled = false
        if (isGamepad) {
            handled = xServerView!!.xServer.winHandler.onGenericMotionEvent(it.event)
            // handled = ExternalController.onMotionEvent(xServer.winHandler, it.event)
        }
        if (!handled && isMouse) {
            handled = touchMouse?.onExternalMouseEvent(it.event) == true
        }
        handled
    }

    private val onGuestProgramTerminated: (AndroidEvent.GuestProgramTerminated) -> Unit = {
        Timber.i("onGuestProgramTerminated")
        viewModelScope.launch {
            exit()
            _uiEvent.send(XServerUiEvent.OnNavigateBack)
        }
    }

    private val onForceCloseApp: (SteamEvent.ForceCloseApp) -> Unit = {
        Timber.i("onForceCloseApp")
        viewModelScope.launch {
            exit()
        }
    }

    private val debugCallback = Callback<String> { outputLine ->
        Timber.i(outputLine ?: "")
    }

    init {
        Timber.i("Starting up XServerScreen")

        // PluviaApp.events.emit(AndroidEvent.SetAppBarVisibility(false))
        PluviaApp.events.emit(AndroidEvent.SetSystemUIVisibility(false))
        PluviaApp.events.emit(AndroidEvent.SetAllowedOrientation(PrefManager.allowedOrientation))

        PluviaApp.events.on<AndroidEvent.ActivityDestroyed, Unit>(onActivityDestroyed)
        PluviaApp.events.on<AndroidEvent.KeyEvent, Boolean>(onKeyEvent)
        PluviaApp.events.on<AndroidEvent.MotionEvent, Boolean>(onMotionEvent)
        PluviaApp.events.on<AndroidEvent.GuestProgramTerminated, Unit>(onGuestProgramTerminated)
        PluviaApp.events.on<SteamEvent.ForceCloseApp, Unit>(onForceCloseApp)
        ProcessHelper.addDebugCallback(debugCallback)
    }

    override fun onCleared() {
        Timber.i("onCleared")

        PluviaApp.events.off<AndroidEvent.ActivityDestroyed, Unit>(onActivityDestroyed)
        PluviaApp.events.off<AndroidEvent.KeyEvent, Boolean>(onKeyEvent)
        PluviaApp.events.off<AndroidEvent.MotionEvent, Boolean>(onMotionEvent)
        PluviaApp.events.off<AndroidEvent.GuestProgramTerminated, Unit>(onGuestProgramTerminated)
        PluviaApp.events.off<SteamEvent.ForceCloseApp, Unit>(onForceCloseApp)
        ProcessHelper.removeDebugCallback(debugCallback)

        xServerView = null
    }

    fun assignTaskAffinity(
        window: Window,
        winHandler: WinHandler,
    ) {
        if (taskAffinityMask == 0) return
        val processId = window.getProcessId()
        val className = window.getClassName()
        val processAffinity = if (window.isWoW64()) taskAffinityMaskWoW64 else taskAffinityMask

        if (processId > 0) {
            winHandler.setProcessAffinity(processId, processAffinity)
        } else if (className.isNotEmpty()) {
            winHandler.setProcessAffinity(window.getClassName(), processAffinity)
        }
    }

    fun shiftXEnvironmentToContext(
        context: Context,
        xServer: XServer,
    ): XEnvironment {
        val environment = XEnvironment(context, xEnvironment!!.imageFs)
        val rootPath = xEnvironment!!.imageFs.rootDir.path
        xEnvironment!!.getComponent(SysVSharedMemoryComponent::class.java).stop()
        val sysVSharedMemoryComponent = SysVSharedMemoryComponent(
            xServer,
            UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.SYSVSHM_SERVER_PATH),
        )
        // val sysVSharedMemoryComponent = xEnvironment.getComponent<SysVSharedMemoryComponent>(SysVSharedMemoryComponent::class.java)
        // sysVSharedMemoryComponent.connectToXServer(xServer)
        environment.addComponent(sysVSharedMemoryComponent)
        xEnvironment!!.getComponent(XServerComponent::class.java).stop()
        val xServerComponent = XServerComponent(xServer, UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.XSERVER_PATH))
        // val xServerComponent = xEnvironment.getComponent<XServerComponent>(XServerComponent::class.java)
        // xServerComponent.connectToXServer(xServer)
        environment.addComponent(xServerComponent)
        xEnvironment!!.getComponent(NetworkInfoUpdateComponent::class.java).stop()
        val networkInfoComponent = NetworkInfoUpdateComponent()
        environment.addComponent(networkInfoComponent)
        // environment.addComponent(xEnvironment.getComponent<NetworkInfoUpdateComponent>(NetworkInfoUpdateComponent::class.java))
        environment.addComponent(xEnvironment!!.getComponent(SteamClientComponent::class.java))
        val alsaComponent = xEnvironment!!.getComponent(ALSAServerComponent::class.java)
        if (alsaComponent != null) {
            environment.addComponent(alsaComponent)
        }
        val pulseComponent = xEnvironment!!.getComponent(PulseAudioComponent::class.java)
        if (pulseComponent != null) {
            environment.addComponent(pulseComponent)
        }
        var virglComponent: VirGLRendererComponent? =
            xEnvironment!!.getComponent(VirGLRendererComponent::class.java)
        if (virglComponent != null) {
            virglComponent.stop()
            virglComponent = VirGLRendererComponent(
                xServer,
                UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.VIRGL_SERVER_PATH),
            )
            environment.addComponent(virglComponent)
        }
        environment.addComponent(xEnvironment!!.getComponent(GuestProgramLauncherComponent::class.java))

        FileUtils.clear(XEnvironment.getTmpDir(context))
        sysVSharedMemoryComponent.start()
        xServerComponent.start()
        networkInfoComponent.start()
        virglComponent?.start()
        // environment.startEnvironmentComponents()

        return environment
    }

    fun setupXEnvironment(
        context: Context,
        appId: Int,
        bootToContainer: Boolean,
        xServerState: MutableState<XServerState>,
        // xServerViewModel: XServerViewModel,
        envVars: EnvVars,
        // generateWinePrefix: Boolean,
        container: Container?,
        // shortcut: Shortcut?,
    ): XEnvironment {
        envVars.put("MESA_DEBUG", "silent")
        envVars.put("MESA_NO_ERROR", "1")
        envVars.put("WINEPREFIX", ImageFs.WINEPREFIX)

        val enableWineDebug = true
        // preferences.getBoolean("enable_wine_debug", false)
        val wineDebugChannels = PrefManager.getString("wine_debug_channels", Constants.XServer.DEFAULT_WINE_DEBUG_CHANNELS)
        envVars.put(
            "WINEDEBUG",
            if (enableWineDebug && wineDebugChannels.isNotEmpty()) "+" + wineDebugChannels.replace(",", ",+") else "-all",
        )

        val imageFs = ImageFs.find(context)
        val rootPath = imageFs.rootDir.path
        FileUtils.clear(imageFs.tmpDir)

        val guestProgramLauncherComponent = GuestProgramLauncherComponent()

        if (container != null) {
            if (container.startupSelection == Container.STARTUP_SELECTION_AGGRESSIVE) {
                xServerView!!.xServer.winHandler.killProcess("services.exe")
            }

            val wow64Mode = container.isWoW64Mode
            val guestExecutable = xServerState.value.wineInfo.getExecutable(context, wow64Mode) + " explorer /desktop=shell," +
                xServerView!!.xServer.screenInfo + " " + getWineStartCommand(appId, container, bootToContainer, appLaunchInfo)
            guestProgramLauncherComponent.isWoW64Mode = wow64Mode
            guestProgramLauncherComponent.guestExecutable = guestExecutable

            envVars.putAll(container.envVars)
            if (!envVars.has("WINEESYNC")) {
                envVars.put("WINEESYNC", "1")
            }

            // Timber.d("3 Container drives: ${container.drives}")
            val bindingPaths = mutableListOf<String>()
            for (drive in container.drivesIterator()) {
                Timber.i("Binding drive ${drive[0]} with path of ${drive[1]}")
                bindingPaths.add(drive[1])
            }
            guestProgramLauncherComponent.bindingPaths = bindingPaths.toTypedArray()
            guestProgramLauncherComponent.box86Version = container.box86Version
            guestProgramLauncherComponent.box64Version = container.box64Version
            guestProgramLauncherComponent.box86Preset = container.box86Preset
            guestProgramLauncherComponent.box64Preset = container.box64Preset
        }

        val environment = XEnvironment(context, imageFs)
        environment.addComponent(
            SysVSharedMemoryComponent(
                xServerView!!.xServer,
                UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.SYSVSHM_SERVER_PATH),
            ),
        )
        environment.addComponent(
            XServerComponent(
                xServerView!!.xServer,
                UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.XSERVER_PATH),
            ),
        )
        environment.addComponent(NetworkInfoUpdateComponent())
        environment.addComponent(SteamClientComponent())

        // environment.addComponent(SteamClientComponent(UnixSocketConfig.createSocket(
        //     rootPath,
        //     Paths.get(ImageFs.WINEPREFIX, "drive_c", UnixSocketConfig.STEAM_PIPE_PATH).toString()
        // )))
        // environment.addComponent(SteamClientComponent(UnixSocketConfig.createSocket(SteamService.getAppDirPath(appId), "/steam_pipe")))
        // environment.addComponent(SteamClientComponent(UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.STEAM_PIPE_PATH)))

        if (xServerState.value.audioDriver == "alsa") {
            envVars.put("ANDROID_ALSA_SERVER", UnixSocketConfig.ALSA_SERVER_PATH)
            envVars.put("ANDROID_ASERVER_USE_SHM", "true")
            environment.addComponent(
                ALSAServerComponent(UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.ALSA_SERVER_PATH)),
            )
        } else if (xServerState.value.audioDriver == "pulseaudio") {
            envVars.put("PULSE_SERVER", UnixSocketConfig.PULSE_SERVER_PATH)
            environment.addComponent(
                PulseAudioComponent(UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.PULSE_SERVER_PATH)),
            )
        }

        if (xServerState.value.graphicsDriver == "virgl") {
            environment.addComponent(
                VirGLRendererComponent(xServerView!!.xServer, UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.VIRGL_SERVER_PATH)),
            )
        }

        guestProgramLauncherComponent.envVars = envVars
        guestProgramLauncherComponent.setTerminationCallback {
            PluviaApp.events.emit(AndroidEvent.GuestProgramTerminated)
        }
        environment.addComponent(guestProgramLauncherComponent)

        // if (generateWinePrefix) {
        //     generateWineprefix(
        //         context,
        //         imageFs,
        //         xServerState.value.wineInfo,
        //         envVars,
        //         environment,
        //     )
        // }
        environment.startEnvironmentComponents()

        // put in separate scope since winhandler start method does some network stuff
        CoroutineScope(Dispatchers.IO).launch {
            xServerView!!.xServer.winHandler.start()
        }
        envVars.clear()
        xServerState.value = xServerState.value.copy(dxwrapperConfig = null)

        return environment
    }

    fun getWineStartCommand(
        appId: Int,
        container: Container,
        bootToContainer: Boolean,
        appLaunchInfo: LaunchInfo?,
    ): String {
        val tempDir = File(container.rootDir, ".wine/drive_c/windows/temp")
        FileUtils.clear(tempDir)

        // Log.d("XServerScreen", "Converting $appLocalExe to wine start command")
        val args = if (bootToContainer || appLaunchInfo == null) {
            "\"wfm.exe\""
        } else {
            val appDirPath = SteamService.getAppDirPath(appId)
            val drives = container.drives
            val driveIndex = drives.indexOf(appDirPath)
            // greater than 1 since there is the drive character and the colon before the app dir path
            val drive = if (driveIndex > 1) {
                drives[driveIndex - 2]
            } else {
                Timber.e("Could not locate game drive")
                'D'
            }
            "/dir $drive:/${appLaunchInfo.workingDir} \"${appLaunchInfo.executable}\""
        }

        return "winhandler.exe $args"
    }

    suspend fun exit() {
        Timber.i("Exit called")

        xServerView?.xServer?.winHandler?.stop()
        xEnvironment?.stopEnvironmentComponents()

        // AppUtils.restartApplication(this)
        // PluviaApp.xServerState = null
        // PluviaApp.xServer = null
        // PluviaApp.xServerView = null
        xEnvironment = null
        // PluviaApp.touchMouse = null
        // PluviaApp.keyboard = null

        _uiEvent.send(XServerUiEvent.OnExit)
    }

    @Suppress("unused")
    fun generateWineprefix(
        context: Context,
        imageFs: ImageFs,
        wineInfo: WineInfo,
        envVars: EnvVars,
        environment: XEnvironment,
    ) {
        // Intent intent = getIntent()
        // public static final @IntRange(from = 1, to = 19) byte CONTAINER_PATTERN_COMPRESSION_LEVEL = 9

        val rootDir = imageFs.rootDir
        val installedWineDir = imageFs.installedWineDir
        // wineInfo = intent.getParcelableExtra("wine_info")
        // WineUtils.extractWineFileForInstallAsync(context, )
        // WineUtils.findWineVersionAsync(context, )
        envVars.put("WINEARCH", if (wineInfo.isWin64) "win64" else "win32")
        imageFs.winePath = wineInfo.path

        val containerPatternDir = File(installedWineDir, "/preinstall/container-pattern")
        if (containerPatternDir.isDirectory) {
            FileUtils.delete(containerPatternDir)
        }
        containerPatternDir.mkdirs()

        val linkFile = File(rootDir, ImageFs.HOME_PATH)
        linkFile.delete()
        FileUtils.symlink(".." + FileUtils.toRelativePath(rootDir.path, containerPatternDir.path), linkFile.path)

        val guestProgramLauncherComponent = environment.getComponent(GuestProgramLauncherComponent::class.java)
        guestProgramLauncherComponent.guestExecutable =
            wineInfo.getExecutable(context, false) + " explorer /desktop=shell," + Container.DEFAULT_SCREEN_SIZE + " winecfg"

        // val preloaderDialog = PreloaderDialog(context)
        guestProgramLauncherComponent.terminationCallback = object : Callback<Int> {
            override fun call(status: Int) {
                CoroutineScope(Dispatchers.IO).launch {
                    if (status > 0) {
                        // AppUtils.showToast(context, R.string.unable_to_install_wine)
                        FileUtils.delete(File(installedWineDir, "/preinstall"))
                        AppUtils.restartApplication(context)
                        return@launch
                    }

                    // preloaderDialog.showOnUiThread(R.string.finishing_installation)
                    // TODO: show loading modal
                    FileUtils.writeString(File(rootDir, ImageFs.WINEPREFIX + "/.update-timestamp"), "disable\n")

                    File(rootDir, ImageFs.WINEPREFIX + "/drive_c/users/xuser")
                        .listFiles()
                        .orEmpty()
                        .forEach { userFile ->
                            if (FileUtils.isSymlink(userFile)) {
                                val path = userFile.path
                                userFile.delete()
                                File(path).mkdirs()
                            }
                        }

                    val suffix = wineInfo.fullVersion() + "-" + wineInfo.arch
                    val containerPatternFile = File(installedWineDir, "/preinstall/container-pattern-$suffix.tzst")
                    TarCompressorUtils.compress(
                        /* type = */
                        TarCompressorUtils.Type.ZSTD,
                        /* file = */
                        File(rootDir, ImageFs.WINEPREFIX),
                        /* destination = */
                        containerPatternFile,
                        /* level = */
                        Constants.XServer.CONTAINER_PATTERN_COMPRESSION_LEVEL,
                    )

                    if (!containerPatternFile.renameTo(File(installedWineDir, containerPatternFile.name)) ||
                        !(File(wineInfo.path)).renameTo(File(installedWineDir, wineInfo.identifier()))
                    ) {
                        containerPatternFile.delete()
                    }

                    FileUtils.delete(File(installedWineDir, "/preinstall"))

                    // preloaderDialog.closeOnUiThread()
                    // TODO: put away loading modal
                    // AppUtils.restartApplication(context, R.id.main_menu_settings)
                }
            }
        }
    }

    fun setupWineSystemFiles(
        context: Context,
        xServerState: MutableState<XServerState>,
        container: Container,
        containerManager: ContainerManager,
        envVars: EnvVars,
        onExtractFileListener: OnExtractFileListener?,
    ) {
        val imageFs = ImageFs.find(context)
        val appVersion = AppUtils.getVersionCode(context).toString()
        val imgVersion = imageFs.getVersion().toString()
        var containerDataChanged = false

        if (!container.getExtra("appVersion").equals(appVersion) || !container.getExtra("imgVersion").equals(imgVersion)) {
            applyGeneralPatches(context, container, imageFs, xServerState.value.wineInfo, onExtractFileListener)
            container.putExtra("appVersion", appVersion)
            container.putExtra("imgVersion", imgVersion)
            containerDataChanged = true
        }

        // val dxwrapper = this.dxwrapper
        if (xServerState.value.dxwrapper == "dxvk") {
            xServerState.value = xServerState.value.copy(
                dxwrapper = "dxvk-" + xServerState.value.dxwrapperConfig?.get("version"),
            )
        }

        if (xServerState.value.dxwrapper != container.getExtra("dxwrapper")) {
            extractDXWrapperFiles(
                context = context,
                firstTimeBoot = firstTimeBoot,
                container = container,
                containerManager = containerManager,
                dxwrapper = xServerState.value.dxwrapper,
                imageFs = imageFs,
                onExtractFileListener = onExtractFileListener,
            )
            container.putExtra("dxwrapper", xServerState.value.dxwrapper)
            containerDataChanged = true
        }

        if (xServerState.value.dxwrapper == "cnc-ddraw") {
            envVars.put("CNC_DDRAW_CONFIG_FILE", "C:\\ProgramData\\cnc-ddraw\\ddraw.ini")
        }

        // val wincomponents = if (shortcut != null) shortcut.getExtra("wincomponents", container.winComponents) else container.winComponents
        val wincomponents = container.winComponents
        if (!wincomponents.equals(container.getExtra("wincomponents"))) {
            // extractWinComponentFiles(context, firstTimeBoot, imageFs, container, containerManager, shortcut, onExtractFileListener)
            extractWinComponentFiles(context, firstTimeBoot, imageFs, container, containerManager, onExtractFileListener)
            container.putExtra("wincomponents", wincomponents)
            containerDataChanged = true
        }

        val desktopTheme = container.desktopTheme
        val screenInfo = xServerView!!.xServer.screenInfo
        if (("$desktopTheme,$screenInfo") != container.getExtra("desktopTheme")) {
            WineThemeManager.apply(context, WineThemeManager.ThemeInfo(desktopTheme), screenInfo)
            container.putExtra("desktopTheme", "$desktopTheme,$screenInfo")
            containerDataChanged = true
        }

        WineStartMenuCreator.create(context, container)
        WineUtils.createDosdevicesSymlinks(container)

        val startupSelection = container.startupSelection.toString()
        if (startupSelection != container.getExtra("startupSelection")) {
            WineUtils.changeServicesStatus(container, container.startupSelection != Container.STARTUP_SELECTION_NORMAL)
            container.putExtra("startupSelection", startupSelection)
            containerDataChanged = true
        }

        if (containerDataChanged) {
            container.saveData()
        }
    }

    fun applyGeneralPatches(
        context: Context,
        container: Container,
        imageFs: ImageFs,
        wineInfo: WineInfo,
        onExtractFileListener: OnExtractFileListener?,
    ) {
        val rootDir = imageFs.rootDir
        FileUtils.delete(File(rootDir, "/opt/apps"))
        TarCompressorUtils.extract(
            /* type = */
            TarCompressorUtils.Type.ZSTD,
            /* assetManager = */
            context.assets,
            /* assetFile = */
            "imagefs_patches.tzst",
            /* destination = */
            rootDir,
            /* onExtractFileListener = */
            onExtractFileListener,
        )
        TarCompressorUtils.extract(
            /* type = */
            TarCompressorUtils.Type.ZSTD,
            /* assetManager = */
            context.assets,
            /* assetFile = */
            "pulseaudio.tzst",
            /* destination = */
            File(context.filesDir, "pulseaudio"),
        )
        WineUtils.applySystemTweaks(context, wineInfo)
        container.putExtra("graphicsDriver", null)
        container.putExtra("desktopTheme", null)
        // SettingsFragment.resetBox86_64Version(context)
    }

    fun extractDXWrapperFiles(
        context: Context,
        firstTimeBoot: Boolean,
        container: Container,
        containerManager: ContainerManager,
        dxwrapper: String,
        imageFs: ImageFs,
        onExtractFileListener: OnExtractFileListener?,
    ) {
        val dlls = arrayOf(
            "d3d10.dll",
            "d3d10_1.dll",
            "d3d10core.dll",
            "d3d11.dll",
            "d3d12.dll",
            "d3d12core.dll",
            "d3d8.dll",
            "d3d9.dll",
            "dxgi.dll",
            "ddraw.dll",
        )

        if (firstTimeBoot && dxwrapper != "vkd3d") {
            cloneOriginalDllFiles(imageFs, *dlls)
        }

        val rootDir = imageFs.rootDir
        val windowsDir = File(rootDir, ImageFs.WINEPREFIX + "/drive_c/windows")

        when (dxwrapper) {
            "wined3d" -> {
                restoreOriginalDllFiles(container, containerManager, imageFs, *dlls)
            }

            "cnc-ddraw" -> {
                restoreOriginalDllFiles(container, containerManager, imageFs, *dlls)
                val assetDir = "dxwrapper/cnc-ddraw-" + DefaultVersion.CNC_DDRAW
                val configFile = File(rootDir, ImageFs.WINEPREFIX + "/drive_c/ProgramData/cnc-ddraw/ddraw.ini")

                if (!configFile.isFile) {
                    FileUtils.copy(context, "$assetDir/ddraw.ini", configFile)
                }

                val shadersDir = File(rootDir, ImageFs.WINEPREFIX + "/drive_c/ProgramData/cnc-ddraw/Shaders")
                FileUtils.delete(shadersDir)
                FileUtils.copy(context, "$assetDir/Shaders", shadersDir)

                TarCompressorUtils.extract(
                    /* type = */
                    TarCompressorUtils.Type.ZSTD,
                    /* assetManager = */
                    context.assets,
                    /* assetFile = */
                    "$assetDir/ddraw.tzst",
                    /* destination = */
                    windowsDir,
                    /* onExtractFileListener = */
                    onExtractFileListener,
                )
            }

            "vkd3d" -> {
                val dxvkVersions = context.resources.getStringArray(R.array.dxvk_version_entries)
                TarCompressorUtils.extract(
                    /* type = */
                    TarCompressorUtils.Type.ZSTD,
                    /* assetManager = */
                    context.assets,
                    /* assetFile = */
                    "dxwrapper/dxvk-" + (dxvkVersions[dxvkVersions.size - 1]) + ".tzst",
                    /* destination = */
                    windowsDir,
                    /* onExtractFileListener = */
                    onExtractFileListener,
                )
                TarCompressorUtils.extract(
                    /* type = */
                    TarCompressorUtils.Type.ZSTD,
                    /* assetManager = */
                    context.assets,
                    /* assetFile = */
                    "dxwrapper/vkd3d-" + DefaultVersion.VKD3D + ".tzst",
                    /* destination = */
                    windowsDir,
                    /* onExtractFileListener = */
                    onExtractFileListener,
                )
            }

            else -> {
                restoreOriginalDllFiles(container, containerManager, imageFs, "d3d12.dll", "d3d12core.dll", "ddraw.dll")
                TarCompressorUtils.extract(
                    /* type = */
                    TarCompressorUtils.Type.ZSTD,
                    /* assetManager = */
                    context.assets,
                    /* assetFile = */
                    "dxwrapper/$dxwrapper.tzst",
                    /* destination = */
                    windowsDir,
                    /* onExtractFileListener = */
                    onExtractFileListener,
                )
                TarCompressorUtils.extract(
                    /* type = */
                    TarCompressorUtils.Type.ZSTD,
                    /* assetManager = */
                    context.assets,
                    /* assetFile = */
                    "dxwrapper/d8vk-${DefaultVersion.D8VK}.tzst",
                    /* destination = */
                    windowsDir,
                    /* onExtractFileListener = */
                    onExtractFileListener,
                )
            }
        }
    }

    fun cloneOriginalDllFiles(imageFs: ImageFs, vararg dlls: String) {
        val rootDir = imageFs.rootDir
        val cacheDir = File(rootDir, ImageFs.CACHE_PATH + "/original_dlls")
        if (!cacheDir.isDirectory) cacheDir.mkdirs()
        val windowsDir = File(rootDir, ImageFs.WINEPREFIX + "/drive_c/windows")
        val dirnames = arrayOf("system32", "syswow64")

        for (dll in dlls) {
            for (dirname in dirnames) {
                val dllFile = File(windowsDir, "$dirname/$dll")

                if (dllFile.isFile) {
                    FileUtils.copy(dllFile, File(cacheDir, "$dirname/$dll"))
                }
            }
        }
    }

    fun restoreOriginalDllFiles(
        container: Container,
        containerManager: ContainerManager,
        imageFs: ImageFs,
        vararg dlls: String,
    ) {
        val rootDir = imageFs.rootDir
        val cacheDir = File(rootDir, ImageFs.CACHE_PATH + "/original_dlls")
        if (cacheDir.isDirectory) {
            val windowsDir = File(rootDir, ImageFs.WINEPREFIX + "/drive_c/windows")
            val dirnames = cacheDir.list()
            var filesCopied = 0

            for (dll in dlls) {
                var success = false
                for (dirname in dirnames!!) {
                    val srcFile = File(cacheDir, "$dirname/$dll")
                    val dstFile = File(windowsDir, "$dirname/$dll")

                    if (FileUtils.copy(srcFile, dstFile)) {
                        success = true
                    }
                }

                if (success) {
                    filesCopied++
                }
            }

            if (filesCopied == dlls.size) {
                return
            }
        }

        containerManager.extractContainerPatternFile(
            /* wineVersion = */
            container.wineVersion,
            /* containerDir = */
            container.rootDir,
            /* onExtractFileListener = */
            object : OnExtractFileListener {
                override fun onExtractFile(file: File, size: Long): File? {
                    val path = file.path
                    if (path.contains("system32/") || path.contains("syswow64/")) {
                        for (dll in dlls) {
                            if (path.endsWith("system32/$dll") || path.endsWith("syswow64/$dll")) return file
                        }
                    }

                    return null
                }
            },
        )

        cloneOriginalDllFiles(imageFs, *dlls)
    }

    fun extractWinComponentFiles(
        context: Context,
        firstTimeBoot: Boolean,
        imageFs: ImageFs,
        container: Container,
        containerManager: ContainerManager,
        // shortcut: Shortcut?,
        onExtractFileListener: OnExtractFileListener?,
    ) {
        val rootDir = imageFs.rootDir
        val windowsDir = File(rootDir, ImageFs.WINEPREFIX + "/drive_c/windows")
        val systemRegFile = File(rootDir, ImageFs.WINEPREFIX + "/system.reg")

        try {
            val wincomponentsJSONObject = JSONObject(FileUtils.readString(context, "wincomponents/wincomponents.json"))
            val dlls = mutableListOf<String>()
            // val wincomponents = if (shortcut != null) shortcut.getExtra("wincomponents", container.winComponents) else container.winComponents
            val wincomponents = container.winComponents

            if (firstTimeBoot) {
                for (wincomponent in KeyValueSet(wincomponents)) {
                    val dlnames = wincomponentsJSONObject.getJSONArray(wincomponent[0])
                    for (i in 0 until dlnames.length()) {
                        val dlname = dlnames.getString(i)
                        dlls.add(if (!dlname.endsWith(".exe")) "$dlname.dll" else dlname)
                    }
                }

                cloneOriginalDllFiles(imageFs, *dlls.toTypedArray())
                dlls.clear()
            }

            val oldWinComponentsIter = KeyValueSet(container.getExtra("wincomponents", Container.FALLBACK_WINCOMPONENTS)).iterator()

            for (wincomponent in KeyValueSet(wincomponents)) {
                if (wincomponent[1].equals(oldWinComponentsIter.next()[1])) {
                    continue
                }

                val identifier = wincomponent[0]
                val useNative = wincomponent[1].equals("1")

                if (useNative) {
                    TarCompressorUtils.extract(
                        /* type = */
                        TarCompressorUtils.Type.ZSTD,
                        /* assetManager = */
                        context.assets,
                        /* assetFile = */
                        "wincomponents/$identifier.tzst",
                        /* destination = */
                        windowsDir,
                        /* onExtractFileListener = */
                        onExtractFileListener,
                    )
                } else {
                    val dlnames = wincomponentsJSONObject.getJSONArray(identifier)
                    for (i in 0 until dlnames.length()) {
                        val dlname = dlnames.getString(i)
                        dlls.add(if (!dlname.endsWith(".exe")) "$dlname.dll" else dlname)
                    }
                }

                WineUtils.setWinComponentRegistryKeys(systemRegFile, identifier, useNative)
            }

            if (dlls.isNotEmpty()) {
                restoreOriginalDllFiles(container, containerManager, imageFs, *dlls.toTypedArray())
            }

            WineUtils.overrideWinComponentDlls(context, container, wincomponents)
        } catch (e: JSONException) {
            Timber.e("Failed to read JSON: $e")
        }
    }

    fun extractGraphicsDriverFiles(
        context: Context,
        graphicsDriver: String,
        dxwrapper: String,
        dxwrapperConfig: KeyValueSet,
        container: Container,
        envVars: EnvVars,
    ) {
        var cacheId = graphicsDriver
        if (graphicsDriver == "turnip") {
            cacheId += "-" + DefaultVersion.TURNIP + "-" + DefaultVersion.ZINK
        } else if (graphicsDriver == "virgl") {
            cacheId += "-" + DefaultVersion.VIRGL
        }

        val changed = cacheId != container.getExtra("graphicsDriver")
        val imageFs = ImageFs.find(context)
        val rootDir = imageFs.rootDir

        if (changed) {
            FileUtils.delete(File(imageFs.lib32Dir, "libvulkan_freedreno.so"))
            FileUtils.delete(File(imageFs.lib64Dir, "libvulkan_freedreno.so"))
            FileUtils.delete(File(imageFs.lib32Dir, "libGL.so.1.7.0"))
            FileUtils.delete(File(imageFs.lib64Dir, "libGL.so.1.7.0"))
            container.putExtra("graphicsDriver", cacheId)
            container.saveData()
        }

        if (graphicsDriver == "turnip") {
            if (dxwrapper == "dxvk") {
                DXVKHelper.setEnvVars(context, dxwrapperConfig, envVars)
            } else if (dxwrapper == "vkd3d") {
                envVars.put("VKD3D_FEATURE_LEVEL", "12_1")
            }

            envVars.put("GALLIUM_DRIVER", "zink")
            envVars.put("TU_OVERRIDE_HEAP_SIZE", "4096")
            if (!envVars.has("MESA_VK_WSI_PRESENT_MODE")) envVars.put("MESA_VK_WSI_PRESENT_MODE", "mailbox")
            envVars.put("vblank_mode", "0")

            if (!GPUInformation.isAdreno6xx(context)) {
                val userEnvVars = EnvVars(container.envVars)
                val tuDebug = userEnvVars.get("TU_DEBUG")
                if (!tuDebug.contains("sysmem")) userEnvVars.put("TU_DEBUG", (if (tuDebug.isNotEmpty()) "$tuDebug," else "") + "sysmem")
                container.envVars = userEnvVars.toString()
            }

            val useDRI3 = PrefManager.getBoolean("use_dri3", true)
            if (!useDRI3) {
                envVars.put("MESA_VK_WSI_PRESENT_MODE", "immediate")
                envVars.put("MESA_VK_WSI_DEBUG", "sw")
            }

            if (changed) {
                TarCompressorUtils.extract(
                    TarCompressorUtils.Type.ZSTD,
                    context.assets,
                    "graphics_driver/turnip-${DefaultVersion.TURNIP}.tzst",
                    rootDir,
                )
                TarCompressorUtils.extract(
                    TarCompressorUtils.Type.ZSTD,
                    context.assets,
                    "graphics_driver/zink-${DefaultVersion.ZINK}.tzst",
                    rootDir,
                )
            }
        } else if (graphicsDriver == "virgl") {
            envVars.put("GALLIUM_DRIVER", "virpipe")
            envVars.put("VIRGL_NO_READBACK", "true")
            envVars.put("VIRGL_SERVER_PATH", UnixSocketConfig.VIRGL_SERVER_PATH)
            envVars.put("MESA_EXTENSION_OVERRIDE", "-GL_EXT_vertex_array_bgra")
            envVars.put("MESA_GL_VERSION_OVERRIDE", "3.1")
            envVars.put("vblank_mode", "0")
            if (changed) {
                TarCompressorUtils.extract(
                    /* type = */
                    TarCompressorUtils.Type.ZSTD,
                    /* assetManager = */
                    context.assets,
                    /* assetFile = */
                    "graphics_driver/virgl-" + DefaultVersion.VIRGL + ".tzst",
                    /* destination = */
                    rootDir,
                )
            }
        }
    }

    fun changeWineAudioDriver(audioDriver: String, container: Container, imageFs: ImageFs) {
        if (audioDriver != container.getExtra("audioDriver")) {
            val rootDir = imageFs.rootDir
            val userRegFile = File(rootDir, ImageFs.WINEPREFIX + "/user.reg")
            WineRegistryEditor(userRegFile).use { registryEditor ->
                if (audioDriver == "alsa") {
                    registryEditor.setStringValue("Software\\Wine\\Drivers", "Audio", "alsa")
                } else if (audioDriver == "pulseaudio") {
                    registryEditor.setStringValue("Software\\Wine\\Drivers", "Audio", "pulse")
                }
            }
            container.putExtra("audioDriver", audioDriver)
            container.saveData()
        }
    }
}
