package com.OxGames.Pluvia.ui.screen.xserver

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.OxGames.Pluvia.BuildConfig
import com.OxGames.Pluvia.Constants
import com.OxGames.Pluvia.PluviaApp
import com.OxGames.Pluvia.PrefManager
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.data.LaunchInfo
import com.OxGames.Pluvia.events.AndroidEvent
import com.OxGames.Pluvia.events.SteamEvent
import com.OxGames.Pluvia.service.SteamService
import com.OxGames.Pluvia.utils.ContainerUtils
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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

@HiltViewModel
class XServerViewModel @Inject constructor(
    @ApplicationContext val context: Context,
) : ViewModel() {

    @SuppressLint("StaticFieldLeak")
    internal var xServerView: XServerView? = null
    internal var xEnvironment: XEnvironment? = null
    internal var touchMouse: TouchMouse? = null
    internal var keyboard: Keyboard? = null

    // Some of the variables below `could` be in a state.

    private val _state = MutableStateFlow(XServerState())
    val state: StateFlow<XServerState> = _state.asStateFlow()

    val appLaunchInfo: LaunchInfo?
        get() {
            if (_state.value.appId < 0) {
                Timber.w("AppID is ${_state.value.appId}")
                return null
            }

            // TODO: What?
            return SteamService.getAppInfoOf(_state.value.appId)?.let { appInfo ->
                _state.update { it.copy(gameName = appInfo.name) }
                SteamService.getWindowsLaunchInfos(_state.value.appId).firstOrNull()
            }
        }

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
            Timber.d("Gamepad Handled: $handled")
            // handled = ExternalController.onKeyEvent(xServer.winHandler, it.event)
        }
        if (!handled && isKeyboard) {
            handled = keyboard?.onKeyEvent(it.event) == true
            Timber.d("Keyboard Handled: $handled")
        }

        Timber.d("isKeyboard: $isKeyboard, isGamepad: $isGamepad \n Handled KeyEvent in XServer: (${it.event.keyCode}) with $handled")

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

    // AndroidView inflates quicker than the ViewModel init, so we'll make this a function to be explicitly called.
    fun init(appId: Int) {
        _state.update { it.copy(appId = appId) }

        Timber.i("Starting up XServerScreen")

        // PluviaApp.events.emit(AndroidEvent.SetAppBarVisibility(false))
        PluviaApp.events.emit(AndroidEvent.SetSystemUIVisibility(false))
        PluviaApp.events.emit(AndroidEvent.SetAllowedOrientation(PrefManager.allowedOrientation))

        PluviaApp.events.on<AndroidEvent.ActivityDestroyed, Unit>(onActivityDestroyed)
        PluviaApp.events.on<AndroidEvent.KeyEvent, Boolean>(onKeyEvent)
        PluviaApp.events.on<AndroidEvent.MotionEvent, Boolean>(onMotionEvent)
        PluviaApp.events.on<AndroidEvent.GuestProgramTerminated, Unit>(onGuestProgramTerminated)
        PluviaApp.events.on<SteamEvent.ForceCloseApp, Unit>(onForceCloseApp)

        if (BuildConfig.DEBUG) {
            ProcessHelper.addDebugCallback(debugCallback)
        }

        if (ContainerUtils.hasContainer(context, appId)) {
            val container = ContainerUtils.getContainer(context, appId)
            _state.update {
                it.copy(
                    graphicsDriver = container.graphicsDriver,
                    audioDriver = container.audioDriver,
                    dxwrapper = container.dxWrapper,
                    dxwrapperConfig = DXVKHelper.parseConfig(container.dxWrapperConfig),
                    screenSize = container.screenSize,
                )
            }
        } else {
            Timber.w("Did not find existing container")
        }

        viewModelScope.launch {
            while (isActive) {
                _state.update { it.copy(currentTime = System.currentTimeMillis()) }
                ensureActive()
                delay(1.seconds)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Timber.i("onCleared")

        PluviaApp.events.off<AndroidEvent.ActivityDestroyed, Unit>(onActivityDestroyed)
        PluviaApp.events.off<AndroidEvent.KeyEvent, Boolean>(onKeyEvent)
        PluviaApp.events.off<AndroidEvent.MotionEvent, Boolean>(onMotionEvent)
        PluviaApp.events.off<AndroidEvent.GuestProgramTerminated, Unit>(onGuestProgramTerminated)
        PluviaApp.events.off<SteamEvent.ForceCloseApp, Unit>(onForceCloseApp)

        if (BuildConfig.DEBUG) {
            ProcessHelper.removeDebugCallback(debugCallback)
        }

        xServerView = null
    }

    fun assignTaskAffinity(
        window: Window,
        winHandler: WinHandler,
    ) {
        if (_state.value.taskAffinityMask == 0) {
            return
        }

        val processId = window.getProcessId()
        val className = window.getClassName()
        val processAffinity = if (window.isWoW64()) _state.value.taskAffinityMaskWoW64 else _state.value.taskAffinityMask

        if (processId > 0) {
            winHandler.setProcessAffinity(processId, processAffinity)
        } else if (className.isNotEmpty()) {
            winHandler.setProcessAffinity(window.getClassName(), processAffinity)
        }
    }

    fun shiftXEnvironmentToContext(xServer: XServer): XEnvironment {
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
        appId: Int,
        bootToContainer: Boolean,
        envVars: EnvVars,
        container: Container?,
    ) {
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
            val guestExecutable = state.value.wineInfo.getExecutable(context, wow64Mode) + " explorer /desktop=shell," +
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

        if (state.value.audioDriver == "alsa") {
            envVars.put("ANDROID_ALSA_SERVER", UnixSocketConfig.ALSA_SERVER_PATH)
            envVars.put("ANDROID_ASERVER_USE_SHM", "true")
            environment.addComponent(
                ALSAServerComponent(UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.ALSA_SERVER_PATH)),
            )
        } else if (state.value.audioDriver == "pulseaudio") {
            envVars.put("PULSE_SERVER", UnixSocketConfig.PULSE_SERVER_PATH)
            environment.addComponent(
                PulseAudioComponent(UnixSocketConfig.createSocket(rootPath, UnixSocketConfig.PULSE_SERVER_PATH)),
            )
        }

        if (state.value.graphicsDriver == "virgl") {
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
        _state.update { it.copy(dxwrapperConfig = null) }

        xEnvironment = environment
    }

    fun getWineStartCommand(
        appId: Int,
        container: Container,
        bootToContainer: Boolean,
        appLaunchInfo: LaunchInfo?,
    ): String {
        val tempDir = File(container.rootDir, ".wine/drive_c/windows/temp")
        FileUtils.clear(tempDir)

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

            "/dir $drive:/${appLaunchInfo.workingDir} \"${appLaunchInfo.executable}" +
                "${if (container.launchParams.isNotBlank()) " " + container.launchParams.trim() else ""}\""
        }

        Timber.i("WineStartCommand: $args")

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
            applyGeneralPatches(container, imageFs, state.value.wineInfo, onExtractFileListener)
            container.putExtra("appVersion", appVersion)
            container.putExtra("imgVersion", imgVersion)
            containerDataChanged = true
        }

        // val dxwrapper = this.dxwrapper
        if (state.value.dxwrapper == "dxvk") {
            val dxvkVersion = state.value.dxwrapperConfig?.get("version")
            Timber.d("DXVK version: $dxvkVersion")
            _state.update { it.copy(dxwrapper = "dxvk-$dxvkVersion") }
        }

        if (state.value.dxwrapper != container.getExtra("dxwrapper")) {
            extractDXWrapperFiles(
                firstTimeBoot = _state.value.firstTimeBoot,
                container = container,
                containerManager = containerManager,
                dxwrapper = state.value.dxwrapper,
                imageFs = imageFs,
                onExtractFileListener = onExtractFileListener,
            )
            container.putExtra("dxwrapper", state.value.dxwrapper)
            containerDataChanged = true
        }

        if (state.value.dxwrapper == "cnc-ddraw") {
            envVars.put("CNC_DDRAW_CONFIG_FILE", "C:\\ProgramData\\cnc-ddraw\\ddraw.ini")
        }

        // val wincomponents = if (shortcut != null) shortcut.getExtra("wincomponents", container.winComponents) else container.winComponents
        val wincomponents = container.winComponents
        if (!wincomponents.equals(container.getExtra("wincomponents"))) {
            // extractWinComponentFiles(context, firstTimeBoot, imageFs, container, containerManager, shortcut, onExtractFileListener)
            extractWinComponentFiles(_state.value.firstTimeBoot, imageFs, container, containerManager, onExtractFileListener)
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
        container: Container,
        imageFs: ImageFs,
        wineInfo: WineInfo,
        onExtractFileListener: OnExtractFileListener?,
    ) {
        val rootDir = imageFs.rootDir
        FileUtils.delete(File(rootDir, "/opt/apps"))
        TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, "imagefs_patches.tzst", rootDir, onExtractFileListener)
        TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, context, "pulseaudio.tzst", File(context.filesDir, "pulseaudio"))
        WineUtils.applySystemTweaks(context, wineInfo)
        container.putExtra("graphicsDriver", null)
        container.putExtra("desktopTheme", null)
        // SettingsFragment.resetBox86_64Version(context)
    }

    fun extractDXWrapperFiles(
        firstTimeBoot: Boolean,
        container: Container,
        containerManager: ContainerManager,
        dxwrapper: String,
        imageFs: ImageFs,
        onExtractFileListener: OnExtractFileListener?,
    ) {
        val dlls = arrayOf(
            "d3d10.dll", "d3d10_1.dll", "d3d10core.dll", "d3d11.dll", "d3d12.dll",
            "d3d12core.dll", "d3d8.dll", "d3d9.dll", "dxgi.dll", "ddraw.dll",
        )

        if (firstTimeBoot && dxwrapper != "vkd3d") {
            cloneOriginalDllFiles(imageFs, dlls)
        }

        val rootDir = imageFs.rootDir
        val windowsDir = File(rootDir, ImageFs.WINEPREFIX + "/drive_c/windows")

        when (dxwrapper) {
            "wined3d" -> restoreOriginalDllFiles(container, containerManager, imageFs, dlls)
            "cnc-ddraw" -> {
                restoreOriginalDllFiles(container, containerManager, imageFs, dlls)
                val assetDir = "dxwrapper/cnc-ddraw-" + DefaultVersion.CNC_DDRAW
                val configFile = File(rootDir, ImageFs.WINEPREFIX + "/drive_c/ProgramData/cnc-ddraw/ddraw.ini")
                if (!configFile.isFile()) {
                    FileUtils.copy(context, "$assetDir/ddraw.ini", configFile)
                }
                val shadersDir = File(rootDir, ImageFs.WINEPREFIX + "/drive_c/ProgramData/cnc-ddraw/Shaders")
                FileUtils.delete(shadersDir)
                FileUtils.copy(context, "$assetDir/Shaders", shadersDir)
                TarCompressorUtils.extract(
                    TarCompressorUtils.Type.ZSTD,
                    context,
                    "$assetDir/ddraw.tzst",
                    windowsDir,
                    onExtractFileListener,
                )
            }

            "vkd3d" -> {
                val dxvkVersions = context.resources.getStringArray(R.array.dxvk_version_entries)
                TarCompressorUtils.extract(
                    TarCompressorUtils.Type.ZSTD,
                    context,
                    "dxwrapper/dxvk-" + (dxvkVersions[dxvkVersions.size - 1]) + ".tzst",
                    windowsDir,
                    onExtractFileListener,
                )
                TarCompressorUtils.extract(
                    TarCompressorUtils.Type.ZSTD,
                    context,
                    "dxwrapper/vkd3d-" + DefaultVersion.VKD3D + ".tzst",
                    windowsDir,
                    onExtractFileListener,
                )
            }

            else -> {
                restoreOriginalDllFiles(container, containerManager, imageFs, arrayOf("d3d12.dll", "d3d12core.dll", "ddraw.dll"))
                TarCompressorUtils.extract(
                    TarCompressorUtils.Type.ZSTD,
                    context,
                    "dxwrapper/$dxwrapper.tzst",
                    windowsDir,
                    onExtractFileListener,
                )
                TarCompressorUtils.extract(
                    TarCompressorUtils.Type.ZSTD,
                    context,
                    "dxwrapper/d8vk-${DefaultVersion.D8VK}.tzst",
                    windowsDir,
                    onExtractFileListener,
                )
            }
        }
    }

    fun cloneOriginalDllFiles(imageFs: ImageFs, dlls: Array<String>) {
        val rootDir = imageFs.rootDir
        val cacheDir = File(rootDir, ImageFs.CACHE_PATH + "/original_dlls")

        if (!cacheDir.isDirectory) {
            cacheDir.mkdirs()
        }

        val windowsDir = File(rootDir, ImageFs.WINEPREFIX + "/drive_c/windows")
        val dirnames = arrayOf("system32", "syswow64")

        dlls.forEach { dll ->
            dirnames.forEach { dirname ->
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
        dlls: Array<String>,
    ) {
        val rootDir = imageFs.rootDir
        val cacheDir = File(rootDir, ImageFs.CACHE_PATH + "/original_dlls")
        if (cacheDir.isDirectory) {
            val windowsDir = File(rootDir, ImageFs.WINEPREFIX + "/drive_c/windows")
            val dirnames = cacheDir.list().orEmpty()
            var filesCopied = 0

            dlls.forEach { dll ->
                var success = false
                dirnames.forEach { dirname ->
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
            container.wineVersion,
            container.rootDir,
            object : OnExtractFileListener {
                override fun onExtractFile(destination: File, size: Long): File? {
                    val path = destination.path
                    if (path.contains("system32/") || path.contains("syswow64/")) {
                        dlls.forEach { dll ->
                            if (path.endsWith("system32/$dll") || path.endsWith("syswow64/$dll")) {
                                return destination
                            }
                        }
                    }

                    return null
                }
            },
        )

        cloneOriginalDllFiles(imageFs, dlls)
    }

    fun extractWinComponentFiles(
        firstTimeBoot: Boolean,
        imageFs: ImageFs,
        container: Container,
        containerManager: ContainerManager,
        onExtractFileListener: OnExtractFileListener?,
    ) {
        val rootDir = imageFs.rootDir
        val windowsDir = File(rootDir, ImageFs.WINEPREFIX + "/drive_c/windows")
        val systemRegFile = File(rootDir, ImageFs.WINEPREFIX + "/system.reg")

        try {
            val wincomponentsJSONObject = JSONObject(FileUtils.readString(context, "wincomponents/wincomponents.json"))
            val dlls = arrayListOf<String>()
            val wincomponents = container.winComponents

            if (firstTimeBoot) {
                KeyValueSet(wincomponents).forEach { wincomponent ->
                    val dlnames = wincomponentsJSONObject.getJSONArray(wincomponent[0])
                    for (i in 0 until dlnames.length()) {
                        val dlname = dlnames.getString(i)
                        dlls.add(if (!dlname.endsWith(".exe")) "$dlname.dll" else dlname)
                    }
                }

                cloneOriginalDllFiles(imageFs, dlls.toTypedArray())
                dlls.clear()
            }

            val oldWinComponentsIter = KeyValueSet(container.getExtra("wincomponents", Container.FALLBACK_WINCOMPONENTS)).iterator()

            KeyValueSet(wincomponents).forEach { wincomponent ->
                if (wincomponent[1].equals(oldWinComponentsIter.next()[1])) {
                    return@forEach
                }

                val identifier = wincomponent[0]
                val useNative = wincomponent[1].equals("1")

                if (useNative) {
                    TarCompressorUtils.extract(
                        TarCompressorUtils.Type.ZSTD,
                        context,
                        "wincomponents/$identifier.tzst",
                        windowsDir,
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
                restoreOriginalDllFiles(container, containerManager, imageFs, dlls.toTypedArray())
            }

            WineUtils.overrideWinComponentDlls(context, container, wincomponents)
        } catch (e: JSONException) {
            Timber.e(e)
        }
    }

    fun extractGraphicsDriverFiles(
        container: Container,
        envVars: EnvVars,
    ) {
        var cacheId = _state.value.graphicsDriver
        if (_state.value.graphicsDriver == "turnip") {
            cacheId += "-" + DefaultVersion.TURNIP + "-" + DefaultVersion.ZINK
        } else if (_state.value.graphicsDriver == "virgl") {
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

        if (_state.value.graphicsDriver == "turnip") {
            if (_state.value.dxwrapper == "dxvk") {
                DXVKHelper.setEnvVars(context, _state.value.dxwrapperConfig, envVars)
            } else if (_state.value.dxwrapper == "vkd3d") {
                envVars.put("VKD3D_FEATURE_LEVEL", "12_1")
            }

            envVars.put("GALLIUM_DRIVER", "zink")
            envVars.put("TU_OVERRIDE_HEAP_SIZE", "4096")
            if (!envVars.has("MESA_VK_WSI_PRESENT_MODE")) {
                envVars.put("MESA_VK_WSI_PRESENT_MODE", "mailbox")
            }
            envVars.put("vblank_mode", "0")

            if (!GPUInformation.isAdreno6xx(context)) {
                val userEnvVars = EnvVars(container.envVars)
                val tuDebug = userEnvVars.get("TU_DEBUG")
                if (!tuDebug.contains("sysmem")) {
                    userEnvVars.put("TU_DEBUG", (if (tuDebug.isNotEmpty()) "$tuDebug," else "") + "sysmem")
                }
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
                    context,
                    "graphics_driver/turnip-" + DefaultVersion.TURNIP + ".tzst",
                    rootDir,
                )
                TarCompressorUtils.extract(
                    TarCompressorUtils.Type.ZSTD,
                    context,
                    "graphics_driver/zink-" + DefaultVersion.ZINK + ".tzst",
                    rootDir,
                )
            }
        } else if (_state.value.graphicsDriver == "virgl") {
            envVars.put("GALLIUM_DRIVER", "virpipe")
            envVars.put("VIRGL_NO_READBACK", "true")
            envVars.put("VIRGL_SERVER_PATH", UnixSocketConfig.VIRGL_SERVER_PATH)
            envVars.put("MESA_EXTENSION_OVERRIDE", "-GL_EXT_vertex_array_bgra")
            envVars.put("MESA_GL_VERSION_OVERRIDE", "3.1")
            envVars.put("vblank_mode", "0")
            if (changed) {
                TarCompressorUtils.extract(
                    TarCompressorUtils.Type.ZSTD,
                    context,
                    "graphics_driver/virgl-" + DefaultVersion.VIRGL + ".tzst",
                    rootDir,
                )
            }
        }
    }

    fun changeWineAudioDriver(container: Container, imageFs: ImageFs) {
        if (_state.value.audioDriver != container.getExtra("audioDriver")) {
            val rootDir = imageFs.rootDir
            val userRegFile = File(rootDir, ImageFs.WINEPREFIX + "/user.reg")
            WineRegistryEditor(userRegFile).use { registryEditor ->
                if (_state.value.audioDriver == "alsa") {
                    registryEditor.setStringValue("Software\\Wine\\Drivers", "Audio", "alsa")
                } else if (_state.value.audioDriver == "pulseaudio") {
                    registryEditor.setStringValue("Software\\Wine\\Drivers", "Audio", "pulse")
                }
            }
            container.putExtra("audioDriver", _state.value.audioDriver)
            container.saveData()
        }
    }

    fun winStarted(value: Boolean) {
        _state.update { it.copy(winStarted = value) }
    }

    fun setBootConfig(container: Container, containerManager: ContainerManager, bootToContainer: Boolean) {
        _state.update {
            it.copy(
                taskAffinityMask = ProcessHelper.getAffinityMask(container.getCPUList(true)).toShort().toInt(),
                taskAffinityMaskWoW64 = ProcessHelper.getAffinityMask(container.getCPUListWoW64(true)).toShort().toInt(),
                firstTimeBoot = container.getExtra("appVersion").isEmpty(),
                wineInfo = WineInfo.fromIdentifier(context, container.wineVersion),
            )
        }

        Timber.i("First time boot: ${_state.value.firstTimeBoot}")

        if (_state.value.wineInfo != WineInfo.MAIN_WINE_VERSION) {
            ImageFs.find(context).winePath = _state.value.wineInfo.path
        }

        val onExtractFileListener = if (!_state.value.wineInfo.isWin64) {
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
        setupWineSystemFiles(container, containerManager, envVars, onExtractFileListener)
        extractGraphicsDriverFiles(container, envVars)
        changeWineAudioDriver(container, ImageFs.find(context))
        setupXEnvironment(_state.value.appId, bootToContainer, envVars, container)
    }
}
