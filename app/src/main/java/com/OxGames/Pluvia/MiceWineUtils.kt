package com.OxGames.Pluvia

import android.annotation.SuppressLint
import android.os.Build
import com.micewine.emu.core.ShellLoader.runCommandWithOutput
import java.io.File

object MiceWineUtils {

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
    var miceWineVersion: String = "MiceWine ${BuildConfig.VERSION_NAME}" + if (BuildConfig.DEBUG) " (git-${BuildConfig.GIT_SHORT_SHA})" else ""
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

}
