package com.OxGames.Pluvia

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.WindowManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.micewine.emu.core.EnvVars.getEnv
import com.micewine.emu.core.RatPackageManager
import com.micewine.emu.core.ShellLoader.runCommandWithOutput
import java.io.File
import java.util.Collections
import java.util.Locale

object MiceWineUtils {

    private val gson = Gson()

    /* General Settings Activity */
    const val ACTION_PREFERENCE_SELECT = "com.micewine.emu.ACTION_PREFERENCE_SELECT"

    const val BOX64_LOG = "BOX64_LOG"
    const val BOX64_LOG_DEFAULT_VALUE = "1"
    const val BOX64_MMAP32 = "BOX64_MMAP32"
    const val BOX64_MMAP32_DEFAULT_VALUE = true
    const val BOX64_AVX = "BOX64_AVX"
    const val BOX64_AVX_DEFAULT_VALUE = "2"
    const val BOX64_SSE42 = "BOX64_SSE42"
    const val BOX64_SSE42_DEFAULT_VALUE = true
    const val BOX64_DYNAREC_BIGBLOCK = "BOX64_DYNAREC_BIGBLOCK"
    const val BOX64_DYNAREC_BIGBLOCK_DEFAULT_VALUE = "1"
    const val BOX64_DYNAREC_STRONGMEM = "BOX64_DYNAREC_STRONGMEM"
    const val BOX64_DYNAREC_STRONGMEM_DEFAULT_VALUE = "1"
    const val BOX64_DYNAREC_WEAKBARRIER = "BOX64_DYNAREC_WEAKBARRIER"
    const val BOX64_DYNAREC_WEAKBARRIER_DEFAULT_VALUE = "1"
    const val BOX64_DYNAREC_PAUSE = "BOX64_DYNAREC_PAUSE"
    const val BOX64_DYNAREC_PAUSE_DEFAULT_VALUE = "0"
    const val BOX64_DYNAREC_X87DOUBLE = "BOX64_DYNAREC_X87DOUBLE"
    const val BOX64_DYNAREC_X87DOUBLE_DEFAULT_VALUE = false
    const val BOX64_DYNAREC_FASTNAN = "BOX64_DYNAREC_FASTNAN"
    const val BOX64_DYNAREC_FASTNAN_DEFAULT_VALUE = true
    const val BOX64_DYNAREC_FASTROUND = "BOX64_DYNAREC_FASTROUND"
    const val BOX64_DYNAREC_FASTROUND_DEFAULT_VALUE = true
    const val BOX64_DYNAREC_SAFEFLAGS = "BOX64_DYNAREC_SAFEFLAGS"
    const val BOX64_DYNAREC_SAFEFLAGS_DEFAULT_VALUE = "1"
    const val BOX64_DYNAREC_CALLRET = "BOX64_DYNAREC_CALLRET"
    const val BOX64_DYNAREC_CALLRET_DEFAULT_VALUE = true
    const val BOX64_DYNAREC_ALIGNED_ATOMICS = "BOX64_DYNAREC_ALIGNED_ATOMICS"
    const val BOX64_DYNAREC_ALIGNED_ATOMICS_DEFAULT_VALUE = false
    const val BOX64_DYNAREC_NATIVEFLAGS = "BOX64_DYNAREC_NATIVEFLAGS"
    const val BOX64_DYNAREC_NATIVEFLAGS_DEFAULT_VALUE = true
    const val BOX64_DYNAREC_WAIT = "BOX64_DYNAREC_WAIT"
    const val BOX64_DYNAREC_WAIT_DEFAULT_VALUE = true
    const val BOX64_DYNAREC_DIRTY = "BOX64_DYNAREC_DIRTY"
    const val BOX64_DYNAREC_DIRTY_DEFAULT_VALUE = false
    const val BOX64_DYNAREC_FORWARD = "BOX64_DYNAREC_FORWARD"
    const val BOX64_DYNAREC_FORWARD_DEFAULT_VALUE = "128"
    const val BOX64_SHOWSEGV = "BOX64_SHOWSEGV"
    const val BOX64_SHOWSEGV_DEFAULT_VALUE = false
    const val BOX64_SHOWBT = "BOX64_SHOWBT"
    const val BOX64_SHOWBT_DEFAULT_VALUE = false
    const val BOX64_NOSIGSEGV = "BOX64_NOSIGSEGV"
    const val BOX64_NOSIGSEGV_DEFAULT_VALUE = false
    const val BOX64_NOSIGILL = "BOX64_NOSIGILL"
    const val BOX64_NOSIGILL_DEFAULT_VALUE = false

    const val SELECTED_BOX64 = "selectedBox64"
    const val SELECTED_VULKAN_DRIVER = "selectedVulkanDriver"
    const val SELECTED_WINE_PREFIX = "selectedWinePrefix"
    const val SELECTED_TU_DEBUG_PRESET = "selectedTuDebugPreset"
    const val SELECTED_TU_DEBUG_PRESET_DEFAULT_VALUE = "noconform,sysmem"
    const val ENABLE_DRI3 = "enableDRI3"
    const val ENABLE_DRI3_DEFAULT_VALUE = true
    const val ENABLE_MANGOHUD = "enableMangoHUD"
    const val ENABLE_MANGOHUD_DEFAULT_VALUE = true
    const val WINE_LOG_LEVEL = "wineLogLevel"
    const val WINE_LOG_LEVEL_DEFAULT_VALUE = "default"
    const val SELECTED_GL_PROFILE = "selectedGLProfile"
    const val SELECTED_GL_PROFILE_DEFAULT_VALUE = "GL 3.2"
    const val SELECTED_DXVK_HUD_PRESET = "selectedDXVKHudPreset"
    const val SELECTED_DXVK_HUD_PRESET_DEFAULT_VALUE = ""
    const val SELECTED_MESA_VK_WSI_PRESENT_MODE = "MESA_VK_WSI_PRESENT_MODE"
    const val SELECTED_MESA_VK_WSI_PRESENT_MODE_DEFAULT_VALUE = "mailbox"
    const val DEAD_ZONE = "deadZone"
    const val MOUSE_SENSIBILITY = "mouseSensibility"
    const val FPS_LIMIT = "fpsLimit"
    const val PA_SINK = "pulseAudioSink"
    const val PA_SINK_DEFAULT_VALUE = "SLES"

    /* Main Activity */
    @SuppressLint("SdCardPath")
    val appRootDir = File("/data/data/com.micewine.emu/files")
    var ratPackagesDir = File("$appRootDir/packages")
    var appBuiltinRootfs: Boolean = false
    var deviceArch = Build.SUPPORTED_ABIS[0].replace("arm64-v8a", "aarch64")
    private val unixUsername = runCommandWithOutput("whoami").replace("\n", "")
    var customRootFSPath: String? = null
    var usrDir = File("$appRootDir/usr")
    var tmpDir = File("$usrDir/tmp")
    var homeDir = File("$appRootDir/home")
    var setupDone: Boolean = false
    var enableRamCounter: Boolean = false
    var enableCpuCounter: Boolean = false
    var enableDebugInfo: Boolean = false
    var enableDRI3: Boolean = false
    var enableMangoHUD: Boolean = false
    var appLang: String? = null
    var box64LogLevel: String? = null
    var box64Mmap32: String? = null
    var box64Avx: String? = null
    var box64Sse42: String? = null
    var box64DynarecBigblock: String? = null
    var box64DynarecStrongmem: String? = null
    var box64DynarecWeakbarrier: String? = null
    var box64DynarecPause: String? = null
    var box64DynarecX87double: String? = null
    var box64DynarecFastnan: String? = null
    var box64DynarecFastround: String? = null
    var box64DynarecSafeflags: String? = null
    var box64DynarecCallret: String? = null
    var box64DynarecAlignedAtomics: String? = null
    var box64DynarecNativeflags: String? = null
    var box64DynarecBleedingEdge: String? = null
    var box64DynarecWait: String? = null
    var box64DynarecDirty: String? = null
    var box64DynarecForward: String? = null
    var box64ShowSegv: String? = null
    var box64ShowBt: String? = null
    var box64NoSigSegv: String? = null
    var box64NoSigill: String? = null
    var wineLogLevel: String? = null
    var selectedBox64: String? = null
    var selectedD3DXRenderer: String? = null
    var selectedWineD3D: String? = null
    var selectedDXVK: String? = null
    var selectedVKD3D: String? = null
    var selectedGLProfile: String? = null
    var selectedDXVKHud: String? = null
    var selectedMesaVkWsiPresentMode: String? = null
    var selectedTuDebugPreset: String? = null
    var selectedFragmentId = 0
    var memoryStats = "??/??"
    var totalCpuUsage = "???%"
    var winePrefixesDir: File = File("$appRootDir/winePrefixes")
    var wineDisksFolder: File? = null
    var winePrefix: File? = null
    var wineESync: Boolean = false
    var wineServices: Boolean = false
    var selectedCpuAffinity: String? = null
    var enableXInput: Boolean = false
    var enableWineVirtualDesktop: Boolean = false
    var selectedWine: String? = null
    var fileManagerDefaultDir: String = ""
    var fileManagerCwd: String? = null
    var selectedFile: String = ""
    var miceWineVersion: String =
        "MiceWine ${BuildConfig.VERSION_NAME}" + if (BuildConfig.DEBUG) " (git-${BuildConfig.GIT_SHORT_SHA})" else ""
    var vulkanDriverDeviceName: String? = null
    var vulkanDriverDriverVersion: String? = null
    var screenFpsLimit: Int = 60
    var fpsLimit: Int = 0
    var paSink: String? = null
    var selectedResolution: String? = null
    var useAdrenoTools: Boolean = false
    var adrenoToolsDriverFile: File? = null

    const val ACTION_RUN_WINE = "com.micewine.emu.ACTION_RUN_WINE"
    const val ACTION_SETUP = "com.micewine.emu.ACTION_SETUP"
    const val ACTION_INSTALL_RAT = "com.micewine.emu.ACTION_INSTALL_RAT"
    const val ACTION_INSTALL_ADTOOLS_DRIVER = "com.micewine.emu.ACTION_INSTALL_ADTOOLS_DRIVER"
    const val ACTION_STOP_ALL = "com.micewine.emu.ACTION_STOP_ALL"
    const val ACTION_SELECT_FILE_MANAGER = "com.micewine.emu.ACTION_SELECT_FILE_MANAGER"
    const val ACTION_SELECT_ICON = "com.micewine.emu.ACTION_SELECT_ICON"
    const val ACTION_CREATE_WINE_PREFIX = "com.micewine.emu.ACTION_CREATE_WINE_PREFIX"
    const val RAM_COUNTER = "ramCounter"
    const val RAM_COUNTER_DEFAULT_VALUE = true
    const val CPU_COUNTER = "cpuCounter"
    const val CPU_COUNTER_DEFAULT_VALUE = false
    const val ENABLE_DEBUG_INFO = "debugInfo"
    const val ENABLE_DEBUG_INFO_DEFAULT_VALUE = true
    const val APP_VERSION = "appVersion"

    private fun strBoolToNumStr(strBool: String): String = strBoolToNumStr(strBool.toBoolean())

    fun strBoolToNumStr(strBool: Boolean): String = if (strBool) "1" else "0"

    private fun getVulkanDriverInfo(info: String): String =
        runCommandWithOutput("echo $(${getEnv()} DISPLAY= vulkaninfo | grep $info | cut -d '=' -f 2)")


    @Suppress("DEPRECATION")
    fun setSharedVars(
        context: Context,
        box64Version: String? = null,
        box64Preset: String? = null,
        d3dxRenderer: String? = null,
        wineD3D: String? = null,
        dxvk: String? = null,
        vkd3d: String? = null,
        displayResolution: String? = null,
        esync: Boolean? = null,
        services: Boolean? = null,
        virtualDesktop: Boolean? = null,
        cpuAffinity: String? = null,
        adrenoTools: Boolean? = null,
        adrenoToolsDriverPath: String? = null,
    ) {

        if (adrenoTools == true) {
            useAdrenoTools = true
            adrenoToolsDriverFile = File(adrenoToolsDriverPath!!)
        }

        appLang = "en_US" // activity.resources.getString(R.string.app_lang)
        appBuiltinRootfs = context.assets.list("")?.contains("rootfs.zip")!!

        selectedBox64 = box64Version ?: getBox64Version(selectedGameName)
        box64LogLevel = PrefManager.getString(BOX64_LOG, BOX64_LOG_DEFAULT_VALUE)

        setBox64Preset(box64Preset)

        enableDRI3 = PrefManager.getBoolean(ENABLE_DRI3, ENABLE_DRI3_DEFAULT_VALUE)
        enableMangoHUD = PrefManager.getBoolean(ENABLE_MANGOHUD, ENABLE_MANGOHUD_DEFAULT_VALUE)
        wineLogLevel = PrefManager.getString(WINE_LOG_LEVEL, WINE_LOG_LEVEL_DEFAULT_VALUE)

        selectedD3DXRenderer = d3dxRenderer ?: getD3DXRenderer(selectedGameName)
        selectedWineD3D = wineD3D ?: getWineD3DVersion(selectedGameName)
        selectedDXVK = dxvk ?: getDXVKVersion(selectedGameName)
        selectedVKD3D = vkd3d ?: getVKD3DVersion(selectedGameName)

        selectedResolution = displayResolution ?: getDisplaySettings(selectedGameName)[1]
        wineESync = esync ?: getWineESync(selectedGameName)
        wineServices = services ?: getWineServices(selectedGameName)
        enableWineVirtualDesktop = virtualDesktop ?: getWineVirtualDesktop(selectedGameName)
        selectedCpuAffinity = cpuAffinity ?: getCpuAffinity(selectedGameName)

        selectedGLProfile = PrefManager.getString(SELECTED_GL_PROFILE, SELECTED_GL_PROFILE_DEFAULT_VALUE)
        selectedDXVKHud = PrefManager.getString(SELECTED_DXVK_HUD_PRESET, SELECTED_DXVK_HUD_PRESET_DEFAULT_VALUE)
        selectedMesaVkWsiPresentMode =
            PrefManager.getString(SELECTED_MESA_VK_WSI_PRESENT_MODE, SELECTED_MESA_VK_WSI_PRESENT_MODE_DEFAULT_VALUE)
        selectedTuDebugPreset = PrefManager.getString(SELECTED_TU_DEBUG_PRESET, SELECTED_TU_DEBUG_PRESET_DEFAULT_VALUE)

        enableRamCounter = PrefManager.getBoolean(RAM_COUNTER, RAM_COUNTER_DEFAULT_VALUE)
        enableCpuCounter = PrefManager.getBoolean(CPU_COUNTER, CPU_COUNTER_DEFAULT_VALUE)
        enableDebugInfo = PrefManager.getBoolean(ENABLE_DEBUG_INFO, ENABLE_DEBUG_INFO_DEFAULT_VALUE)

        screenFpsLimit = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.refreshRate.toInt()
        fpsLimit = PrefManager.getInt(FPS_LIMIT, screenFpsLimit)

        vulkanDriverDeviceName = getVulkanDriverInfo("deviceName") + if (useAdrenoTools) " (AdrenoTools)" else ""
        vulkanDriverDriverVersion = getVulkanDriverInfo("driverVersion").split(" ")[0]

        winePrefix = File("$winePrefixesDir/${PrefManager.getString(SELECTED_WINE_PREFIX, "default")}")
        wineDisksFolder = File("$winePrefix/dosdevices/")

        val winePrefixConfigFile = File("$winePrefix/config")
        if (winePrefixConfigFile.exists()) {
            selectedWine = winePrefixConfigFile.readLines()[0]
        }

        fileManagerDefaultDir = wineDisksFolder!!.path

        paSink = PrefManager.getString(PA_SINK, PA_SINK_DEFAULT_VALUE)?.lowercase(Locale.getDefault())
    }

    private fun setBox64Preset(name: String?) {
        var selectedBox64Preset = name ?: PrefManager.getString(SELECTED_BOX64_PRESET_KEY, "default")!!

        if (name == "--") selectedBox64Preset = PrefManager.getString(SELECTED_BOX64_PRESET_KEY, "default")!!

        box64Mmap32 = strBoolToNumStr(getBox64Mapping(selectedBox64Preset, BOX64_MMAP32)[0])
        box64Avx = getBox64Mapping(selectedBox64Preset, BOX64_AVX)[0]
        box64Sse42 = strBoolToNumStr(getBox64Mapping(selectedBox64Preset, BOX64_SSE42)[0])
        box64DynarecBigblock = getBox64Mapping(selectedBox64Preset, BOX64_DYNAREC_BIGBLOCK)[0]
        box64DynarecStrongmem = getBox64Mapping(selectedBox64Preset, BOX64_DYNAREC_STRONGMEM)[0]
        box64DynarecWeakbarrier = getBox64Mapping(selectedBox64Preset, BOX64_DYNAREC_WEAKBARRIER)[0]
        box64DynarecPause = getBox64Mapping(selectedBox64Preset, BOX64_DYNAREC_PAUSE)[0]
        box64DynarecX87double = strBoolToNumStr(getBox64Mapping(selectedBox64Preset, BOX64_DYNAREC_X87DOUBLE)[0])
        box64DynarecFastnan = strBoolToNumStr(getBox64Mapping(selectedBox64Preset, BOX64_DYNAREC_FASTNAN)[0])
        box64DynarecFastround = strBoolToNumStr(getBox64Mapping(selectedBox64Preset, BOX64_DYNAREC_FASTROUND)[0])
        box64DynarecSafeflags = getBox64Mapping(selectedBox64Preset, BOX64_DYNAREC_SAFEFLAGS)[0]
        box64DynarecCallret = strBoolToNumStr(getBox64Mapping(selectedBox64Preset, BOX64_DYNAREC_CALLRET)[0])
        box64DynarecAlignedAtomics = strBoolToNumStr(getBox64Mapping(selectedBox64Preset, BOX64_DYNAREC_ALIGNED_ATOMICS)[0])
        box64DynarecNativeflags = strBoolToNumStr(getBox64Mapping(selectedBox64Preset, BOX64_DYNAREC_NATIVEFLAGS)[0])
        box64DynarecWait = strBoolToNumStr(getBox64Mapping(selectedBox64Preset, BOX64_DYNAREC_WAIT)[0])
        box64DynarecDirty = strBoolToNumStr(getBox64Mapping(selectedBox64Preset, BOX64_DYNAREC_DIRTY)[0])
        box64DynarecForward = getBox64Mapping(selectedBox64Preset, BOX64_DYNAREC_FORWARD)[0]
    }

    /* Sound Settings Fragment */
    fun generatePAFile() {
        val paFile = File("$usrDir/etc/pulse/default.pa")

        paFile.writeText(
            "" +
                "#!/data/data/com.micewine.emu/files/usr/bin/pulseaudio -nF\n" +
                ".fail\n" +
                "\n" +
                "load-module module-device-restore\n" +
                "load-module module-stream-restore\n" +
                "load-module module-card-restore\n" +
                "load-module module-augment-properties\n" +
                "load-module module-switch-on-port-available\n" +
                "\n" +
                ".ifexists module-esound-protocol-unix.so\n" +
                "load-module module-esound-protocol-unix\n" +
                ".endif\n" +
                "load-module module-native-protocol-unix\n" +
                "load-module module-default-device-restore\n" +
                "load-module module-always-sink\n" +
                "load-module module-intended-roles\n" +
                "load-module module-position-event-sounds\n" +
                "load-module module-role-cork\n" +
                "load-module module-filter-heuristics\n" +
                "load-module module-filter-apply\n" +
                "\n" +
                ".nofail\n" +
                ".include /data/data/com.micewine.emu/files/usr/etc/pulse/default.pa.d\n" +
                "\n" +
                "load-module module-$paSink-sink\n",
        )
    }

    /* ShortCutsFragment */

    private var gameListNames: MutableList<GameItem> = mutableListOf()
    private var gameList: MutableList<GameItem> = getGameList()

    const val HIGHLIGHT_SHORTCUT_PREFERENCE_KEY = "highlightedShortcut"
    const val ACTION_UPDATE_WINE_PREFIX_SPINNER = "com.micewine.emu.ACTION_UPDATE_WINE_PREFIX_SPINNER"

    const val MESA_DRIVER = 0
    const val ADRENO_TOOLS_DRIVER = 1

    fun getWineVirtualDesktop(name: String): Boolean {
        val index = gameList.indexOfFirst { it.name == name }

        if (index == -1) return false

        return gameList[index].wineVirtualDesktop
    }

    fun getCpuAffinity(name: String): String {
        val index = gameList.indexOfFirst { it.name == name }

        if (index == -1) return availableCPUs.joinToString(",")

        return gameList[index].cpuAffinityCores
    }

    fun getWineServices(name: String): Boolean {
        val index = gameList.indexOfFirst { it.name == name }

        if (index == -1) return false

        return gameList[index].wineServices
    }

    fun getWineESync(name: String): Boolean {
        val index = gameList.indexOfFirst { it.name == name }

        if (index == -1) return true

        return gameList[index].wineESync
    }

    fun getVKD3DVersion(name: String): String {
        val index = gameList.indexOfFirst { it.name == name }

        if (index == -1) return ""

        return gameList[index].vkd3dVersion
    }

    fun getWineD3DVersion(name: String): String {
        val index = gameList.indexOfFirst { it.name == name }

        if (index == -1) return ""

        return gameList[index].wineD3DVersion
    }

    fun getDXVKVersion(name: String): String {
        val index = gameList.indexOfFirst { it.name == name }

        if (index == -1) return ""

        return gameList[index].dxvkVersion
    }

    fun getD3DXRenderer(name: String): String {
        val index = gameList.indexOfFirst { it.name == name }

        if (index == -1) return "DXVK"

        return gameList[index].d3dxRenderer
    }

    fun getVulkanDriverType(name: String): Int {
        val index = gameList.indexOfFirst { it.name == name }

        if (index == -1) return MESA_DRIVER

        return gameList[index].vulkanDriverType
    }

    fun getVulkanDriver(name: String): String {
        val index = gameList.indexOfFirst { it.name == name }

        if (index == -1) return ""

        return gameList[index].vulkanDriver
    }

    fun getDisplaySettings(name: String): List<String> {
        val index = gameList.indexOfFirst { it.name == name }

        if (index == -1) return listOf("16:9", "1280x720")

        return listOf(gameList[index].displayMode, gameList[index].displayResolution)
    }

    fun getBox64Version(name: String): String {
        val index = gameList.indexOfFirst { it.name == name }

        if (index == -1) return RatPackageManager.listRatPackagesId("Box64-").firstOrNull() ?: ""

        return gameList[index].box64Version
    }

    private fun getGameList(): MutableList<GameItem> {
        val json = PrefManager.getString("gameList", "")
        val listType = object : TypeToken<MutableList<GameItem>>() {}.type
        val gameList = gson.fromJson<MutableList<GameItem>>(json, listType)

        return gameList ?: mutableListOf()
    }

    data class GameItem(
        var name: String,
        var exePath: String,
        var exeArguments: String,
        var iconPath: String,
        var box64Version: String,
        var box64Preset: String,
        var controllersPreset: MutableList<String>,
        var controllersEnableXInput: MutableList<Boolean>,
        var virtualControllerPreset: String,
        var virtualControllerEnableXInput: Boolean,
        var displayMode: String,
        var displayResolution: String,
        var vulkanDriver: String,
        var vulkanDriverType: Int,
        var d3dxRenderer: String,
        var dxvkVersion: String,
        var wineD3DVersion: String,
        var vkd3dVersion: String,
        var wineESync: Boolean,
        var wineServices: Boolean,
        var cpuAffinityCores: String,
        var wineVirtualDesktop: Boolean,
        var enableXInput: Boolean,
    )

    /* DebugSettingsFragment */
    val availableCPUs = (0 until Runtime.getRuntime().availableProcessors()).map { it.toString() }.toTypedArray()

    /* AdapterGame */
    var selectedGameName = ""

    /* PresetManagerActivity */
    const val BUTTON_A_KEY = "buttonA"
    const val BUTTON_B_KEY = "buttonB"
    const val BUTTON_X_KEY = "buttonX"
    const val BUTTON_Y_KEY = "buttonY"
    const val BUTTON_START_KEY = "buttonStart"
    const val BUTTON_SELECT_KEY = "buttonSelect"
    const val BUTTON_R1_KEY = "buttonR1"
    const val BUTTON_R2_KEY = "buttonR2"
    const val BUTTON_L1_KEY = "buttonL1"
    const val BUTTON_L2_KEY = "buttonL2"
    const val BUTTON_THUMBL_KEY = "thumbLKey"
    const val BUTTON_THUMBR_KEY = "thumbRKey"
    const val AXIS_X_PLUS_KEY = "axisX+"
    const val AXIS_X_MINUS_KEY = "axisX-"
    const val AXIS_Y_PLUS_KEY = "axisY+"
    const val AXIS_Y_MINUS_KEY = "axisY-"
    const val AXIS_Z_PLUS_KEY = "axisZ+"
    const val AXIS_Z_MINUS_KEY = "axisZ-"
    const val AXIS_RZ_PLUS_KEY = "axisRZ+"
    const val AXIS_RZ_MINUS_KEY = "axisRZ-"
    const val AXIS_HAT_X_PLUS_KEY = "axisHatX+"
    const val AXIS_HAT_X_MINUS_KEY = "axisHatX-"
    const val AXIS_HAT_Y_PLUS_KEY = "axisHatY+"
    const val AXIS_HAT_Y_MINUS_KEY = "axisHatY-"

    const val SELECTED_CONTROLLER_PRESET_KEY = "selectedControllerPreset"
    const val SELECTED_VIRTUAL_CONTROLLER_PRESET_KEY = "selectedVirtualControllerPreset"
    const val SELECTED_BOX64_PRESET_KEY = "selectedBox64Preset"

    const val ACTION_EDIT_CONTROLLER_MAPPING = "com.micewine.emu.ACTION_EDIT_CONTROLLER_MAPPING"
    const val ACTION_EDIT_BOX64_PRESET = "com.micewine.emu.ACTION_EDIT_BOX64_PRESET"

    /* Box64PresetManagerFragment */
    private val presetListNames: MutableList<Item> = mutableListOf()
    private var presetList: MutableList<MutableList<String>> = getBox64Presets()

    private val mappingMap = mapOf(
        BOX64_MMAP32 to 1,
        BOX64_AVX to 2,
        BOX64_SSE42 to 3,
        BOX64_DYNAREC_BIGBLOCK to 4,
        BOX64_DYNAREC_STRONGMEM to 5,
        BOX64_DYNAREC_WEAKBARRIER to 6,
        BOX64_DYNAREC_PAUSE to 7,
        BOX64_DYNAREC_X87DOUBLE to 8,
        BOX64_DYNAREC_FASTNAN to 9,
        BOX64_DYNAREC_FASTROUND to 10,
        BOX64_DYNAREC_SAFEFLAGS to 11,
        BOX64_DYNAREC_CALLRET to 12,
        BOX64_DYNAREC_ALIGNED_ATOMICS to 13,
        BOX64_DYNAREC_NATIVEFLAGS to 14,
        BOX64_DYNAREC_WAIT to 15,
        BOX64_DYNAREC_DIRTY to 16,
        BOX64_DYNAREC_FORWARD to 17,
    )

    fun getBox64Mapping(name: String, key: String): List<String> {
        val index = presetList.indexOfFirst { it[0] == name }

        if (index == -1) {
            return listOf("")
        }

        return presetList[index][mappingMap[key]!!].split(":")
    }

    fun getBox64Presets(): MutableList<MutableList<String>> {
        val json = PrefManager.getString("box64PresetList", "")
        val listType = object : TypeToken<MutableList<MutableList<String>>>() {}.type

        return gson.fromJson(json, listType) ?: mutableListOf(
            ArrayList(Collections.nCopies(mappingMap.size + 1, ":")).apply {
                this[0] = "default"
                this[mappingMap[BOX64_MMAP32]!!] = "true"
                this[mappingMap[BOX64_AVX]!!] = "2"
                this[mappingMap[BOX64_SSE42]!!] = "true"
                this[mappingMap[BOX64_DYNAREC_BIGBLOCK]!!] = "2"
                this[mappingMap[BOX64_DYNAREC_STRONGMEM]!!] = "1"
                this[mappingMap[BOX64_DYNAREC_WEAKBARRIER]!!] = "2"
                this[mappingMap[BOX64_DYNAREC_PAUSE]!!] = "0"
                this[mappingMap[BOX64_DYNAREC_X87DOUBLE]!!] = "false"
                this[mappingMap[BOX64_DYNAREC_FASTNAN]!!] = "true"
                this[mappingMap[BOX64_DYNAREC_FASTROUND]!!] = "true"
                this[mappingMap[BOX64_DYNAREC_SAFEFLAGS]!!] = "1"
                this[mappingMap[BOX64_DYNAREC_CALLRET]!!] = "true"
                this[mappingMap[BOX64_DYNAREC_ALIGNED_ATOMICS]!!] = "false"
                this[mappingMap[BOX64_DYNAREC_NATIVEFLAGS]!!] = "true"
                this[mappingMap[BOX64_DYNAREC_WAIT]!!] = "true"
                this[mappingMap[BOX64_DYNAREC_DIRTY]!!] = "false"
                this[mappingMap[BOX64_DYNAREC_FORWARD]!!] = "128"
            },
        )
    }

    /* Adapter Preset */
    class Item(var titleSettings: String, var type: Int, var userPreset: Boolean, var showRadioButton: Boolean = false)

    const val PHYSICAL_CONTROLLER = 0
    const val VIRTUAL_CONTROLLER = 1
    var clickedPresetName = ""
    var clickedPresetType = -1
    var selectedPresetId = -1

    /* CreatePresetFragment */
    const val WINEPREFIX_PRESET = 1
    const val CONTROLLER_PRESET = 2
    const val VIRTUAL_CONTROLLER_PRESET = 3
    const val BOX64_PRESET = 4

    /* RatManagerActivity */
            fun generateICDFile(driverLib: String, destIcd: File) {
            val json = gson.toJson(
                mapOf(
                    "ICD" to mapOf(
                        "api_version" to "1.1.296",
                        "library_path" to driverLib
                    ),
                    "file_format_version" to "1.0.0"
                )
            )

            destIcd.writeText(json)
        }

        fun generateMangoHUDConfFile() {
            val mangoHudConfFile = File("$usrDir/etc/MangoHud.conf")
            val options = StringBuilder()

            options.append("fps_limit=$fpsLimit\n")

            if (!enableMangoHUD) {
                options.append("no_display\n")
            }

            mangoHudConfFile.writeText(options.toString())
        }

    /* EnvVarsSettingsFragment */
    data class EnvironmentVariable(val key: String, val value: String)

    const val ENV_VARS_KEY = "environmentVariables"
}
