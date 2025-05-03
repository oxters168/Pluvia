package com.micewine.emu

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.edit
import com.OxGames.Pluvia.BuildConfig
import com.OxGames.Pluvia.MainActivity
import com.OxGames.Pluvia.PrefManager
import com.OxGames.Pluvia.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.micewine.emu.core.EnvVars.getEnv
import com.micewine.emu.core.RatPackageManager.listRatPackagesId
import com.micewine.emu.core.ShellLoader.runCommand
import com.micewine.emu.core.ShellLoader.runCommandWithOutput
import com.micewine.emu.core.WineWrapper
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.util.Collections
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * This class is a 'temporary' throw all since Pluvia strips MiceWine down to its core.
 * Anything public `companion object` from Activity's and Fragment's <~> Core are placed here.
 */

object MiceWineUtils {

    private val gson = Gson()

    object GeneralSettings {
        const val ACTION_PREFERENCE_SELECT = "com.micewine.emu.ACTION_PREFERENCE_SELECT"
        const val SWITCH = 1
        const val SPINNER = 2
        const val CHECKBOX = 3
        const val SEEKBAR = 4

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
    }

    object Main {
        var runningXServer = false

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

        fun setupWinePrefix(winePrefix: File, wine: String) {
            if (!winePrefix.exists()) {
                val driveC = File("$winePrefix/drive_c")
                val wineUtils = File("$appRootDir/wine-utils")
                val startMenu = File("$driveC/ProgramData/Microsoft/Windows/Start Menu")
                val userSharedFolder = File("/storage/emulated/0/MiceWine")
                val localAppData = File("$driveC/users/$unixUsername/AppData")
                val localSavedGames = File("$driveC/users/$unixUsername/Saved Games")
                val system32 = File("$driveC/windows/system32")
                val syswow64 = File("$driveC/windows/syswow64")
                val winePrefixConfigFile = File("$winePrefix/config")

                winePrefix.mkdirs()
                winePrefixConfigFile.writeText(wine + "\n")

                selectedWine = wine

                WineWrapper.wine("wineboot -i")

                File("$appRootDir/wine-utils/CoreFonts").copyRecursively(File("$winePrefix/drive_c/windows/Fonts"), true)

                localAppData.copyRecursively(File("$userSharedFolder/AppData"))
                localAppData.deleteRecursively()

                File("$userSharedFolder/AppData").mkdirs()

                localSavedGames.deleteRecursively()

                File("$userSharedFolder/Saved Games").mkdirs()

                runCommand("ln -sf '$userSharedFolder/AppData' '$localAppData'")
                runCommand("ln -sf '$userSharedFolder/Saved Games' '$localSavedGames'")

                startMenu.deleteRecursively()

                File("$wineUtils/Start Menu").copyRecursively(File("$startMenu"), true)
                File("$wineUtils/Addons").copyRecursively(File("$driveC/Addons"), true)
                File("$wineUtils/Addons/Windows").copyRecursively(File("$driveC/windows"), true)
                File("$wineUtils/DirectX/x64").copyRecursively(system32, true)
                File("$wineUtils/DirectX/x32").copyRecursively(syswow64, true)
                File("$wineUtils/OpenAL/x64").copyRecursively(system32, true)
                File("$wineUtils/OpenAL/x32").copyRecursively(syswow64, true)

                WineWrapper.wine("regedit '$driveC/Addons/DefaultDLLsOverrides.reg'")
                WineWrapper.wine("regedit '$driveC/Addons/Themes/DarkBlue/DarkBlue.reg'")
                WineWrapper.wine("reg add HKCU\\\\Software\\\\Wine\\\\X11\\ Driver /t REG_SZ /v Decorated /d N /f")
                WineWrapper.wine("reg add HKCU\\\\Software\\\\Wine\\\\X11\\ Driver /t REG_SZ /v Managed /d N /f")
            }
        }

        private fun strBoolToNumStr(strBool: String): String {
            return strBoolToNumStr(strBool.toBoolean())
        }

        fun strBoolToNumStr(strBool: Boolean): String {
            return if (strBool) "1" else "0"
        }

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

            appLang = context.resources.getString(R.string.app_lang)
            appBuiltinRootfs = context.assets.list("")?.contains("rootfs.zip")!!

            selectedBox64 = box64Version ?: Shortcuts.getBox64Version(Game.selectedGameName)
            box64LogLevel = PrefManager.getString(GeneralSettings.BOX64_LOG, GeneralSettings.BOX64_LOG_DEFAULT_VALUE)

            setBox64Preset(box64Preset)

            enableDRI3 = PrefManager.getBoolean(GeneralSettings.ENABLE_DRI3, GeneralSettings.ENABLE_DRI3_DEFAULT_VALUE)
            enableMangoHUD = PrefManager.getBoolean(GeneralSettings.ENABLE_MANGOHUD, GeneralSettings.ENABLE_MANGOHUD_DEFAULT_VALUE)
            wineLogLevel = PrefManager.getString(GeneralSettings.WINE_LOG_LEVEL, GeneralSettings.WINE_LOG_LEVEL_DEFAULT_VALUE)

            selectedD3DXRenderer = d3dxRenderer ?: Shortcuts.getD3DXRenderer(Game.selectedGameName)
            selectedWineD3D = wineD3D ?: Shortcuts.getWineD3DVersion(Game.selectedGameName)
            selectedDXVK = dxvk ?: Shortcuts.getDXVKVersion(Game.selectedGameName)
            selectedVKD3D = vkd3d ?: Shortcuts.getVKD3DVersion(Game.selectedGameName)

            selectedResolution = displayResolution ?: Shortcuts.getDisplaySettings(Game.selectedGameName)[1]
            wineESync = esync ?: Shortcuts.getWineESync(Game.selectedGameName)
            wineServices = services ?: Shortcuts.getWineServices(Game.selectedGameName)
            enableWineVirtualDesktop = virtualDesktop ?: Shortcuts.getWineVirtualDesktop(Game.selectedGameName)
            selectedCpuAffinity = cpuAffinity ?: Shortcuts.getCpuAffinity(Game.selectedGameName)

            selectedGLProfile = PrefManager.getString(
                GeneralSettings.SELECTED_GL_PROFILE,
                GeneralSettings.SELECTED_GL_PROFILE_DEFAULT_VALUE,
            )
            selectedDXVKHud = PrefManager.getString(
                GeneralSettings.SELECTED_DXVK_HUD_PRESET,
                GeneralSettings.SELECTED_DXVK_HUD_PRESET_DEFAULT_VALUE,
            )
            selectedMesaVkWsiPresentMode = PrefManager.getString(
                GeneralSettings.SELECTED_MESA_VK_WSI_PRESENT_MODE,
                GeneralSettings.SELECTED_MESA_VK_WSI_PRESENT_MODE_DEFAULT_VALUE,
            )
            selectedTuDebugPreset = PrefManager.getString(
                GeneralSettings.SELECTED_TU_DEBUG_PRESET,
                GeneralSettings.SELECTED_TU_DEBUG_PRESET_DEFAULT_VALUE,
            )

            enableRamCounter = PrefManager.getBoolean(RAM_COUNTER, RAM_COUNTER_DEFAULT_VALUE)
            enableCpuCounter = PrefManager.getBoolean(CPU_COUNTER, CPU_COUNTER_DEFAULT_VALUE)
            enableDebugInfo = PrefManager.getBoolean(ENABLE_DEBUG_INFO, ENABLE_DEBUG_INFO_DEFAULT_VALUE)

            screenFpsLimit = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.refreshRate.toInt()
            fpsLimit = PrefManager.getInt(GeneralSettings.FPS_LIMIT, screenFpsLimit)

            vulkanDriverDeviceName = getVulkanDriverInfo("deviceName") + if (useAdrenoTools) " (AdrenoTools)" else ""
            vulkanDriverDriverVersion = getVulkanDriverInfo("driverVersion").split(" ")[0]

            winePrefix = File("$winePrefixesDir/${PrefManager.getString(GeneralSettings.SELECTED_WINE_PREFIX, "default")}")
            wineDisksFolder = File("$winePrefix/dosdevices/")

            val winePrefixConfigFile = File("$winePrefix/config")
            if (winePrefixConfigFile.exists()) {
                selectedWine = winePrefixConfigFile.readLines()[0]
            }

            fileManagerDefaultDir = wineDisksFolder!!.path

            paSink = PrefManager.getString(
                GeneralSettings.PA_SINK,
                GeneralSettings.PA_SINK_DEFAULT_VALUE,
            )?.lowercase(Locale.getDefault())
        }

        private fun setBox64Preset(name: String?) {
            var selectedBox64Preset = name ?: PrefManager.getString(PresetManager.SELECTED_BOX64_PRESET_KEY, "default")!!

            if (name == "--") selectedBox64Preset = PrefManager.getString(PresetManager.SELECTED_BOX64_PRESET_KEY, "default")!!

            box64Mmap32 = strBoolToNumStr(
                strBool = Box64PresetManager.getBox64Mapping(selectedBox64Preset, GeneralSettings.BOX64_MMAP32)[0],
            )
            box64Avx = Box64PresetManager.getBox64Mapping(selectedBox64Preset, GeneralSettings.BOX64_AVX)[0]
            box64Sse42 = strBoolToNumStr(strBool = Box64PresetManager.getBox64Mapping(selectedBox64Preset, GeneralSettings.BOX64_SSE42)[0])
            box64DynarecBigblock = Box64PresetManager.getBox64Mapping(selectedBox64Preset, GeneralSettings.BOX64_DYNAREC_BIGBLOCK)[0]
            box64DynarecStrongmem = Box64PresetManager.getBox64Mapping(selectedBox64Preset, GeneralSettings.BOX64_DYNAREC_STRONGMEM)[0]
            box64DynarecWeakbarrier = Box64PresetManager.getBox64Mapping(selectedBox64Preset, GeneralSettings.BOX64_DYNAREC_WEAKBARRIER)[0]
            box64DynarecPause = Box64PresetManager.getBox64Mapping(selectedBox64Preset, GeneralSettings.BOX64_DYNAREC_PAUSE)[0]
            box64DynarecX87double = strBoolToNumStr(
                strBool = Box64PresetManager.getBox64Mapping(selectedBox64Preset, GeneralSettings.BOX64_DYNAREC_X87DOUBLE)[0],
            )
            box64DynarecFastnan = strBoolToNumStr(
                strBool = Box64PresetManager.getBox64Mapping(selectedBox64Preset, GeneralSettings.BOX64_DYNAREC_FASTNAN)[0],
            )
            box64DynarecFastround = strBoolToNumStr(
                strBool = Box64PresetManager.getBox64Mapping(selectedBox64Preset, GeneralSettings.BOX64_DYNAREC_FASTROUND)[0],
            )
            box64DynarecSafeflags = Box64PresetManager.getBox64Mapping(selectedBox64Preset, GeneralSettings.BOX64_DYNAREC_SAFEFLAGS)[0]
            box64DynarecCallret = strBoolToNumStr(
                strBool = Box64PresetManager.getBox64Mapping(selectedBox64Preset, GeneralSettings.BOX64_DYNAREC_CALLRET)[0],
            )
            box64DynarecAlignedAtomics = strBoolToNumStr(
                strBool = Box64PresetManager.getBox64Mapping(selectedBox64Preset, GeneralSettings.BOX64_DYNAREC_ALIGNED_ATOMICS)[0],
            )
            box64DynarecNativeflags = strBoolToNumStr(
                strBool = Box64PresetManager.getBox64Mapping(selectedBox64Preset, GeneralSettings.BOX64_DYNAREC_NATIVEFLAGS)[0],
            )
            box64DynarecWait = strBoolToNumStr(
                strBool = Box64PresetManager.getBox64Mapping(selectedBox64Preset, GeneralSettings.BOX64_DYNAREC_WAIT)[0],
            )
            box64DynarecDirty = strBoolToNumStr(
                strBool = Box64PresetManager.getBox64Mapping(selectedBox64Preset, GeneralSettings.BOX64_DYNAREC_DIRTY)[0],
            )
            box64DynarecForward = Box64PresetManager.getBox64Mapping(selectedBox64Preset, GeneralSettings.BOX64_DYNAREC_FORWARD)[0]
        }

        fun copyAssets(activity: Activity, filename: String, outputPath: String) {
            Setup.dialogTitleText = activity.getString(R.string.extracting_from_assets)

            val assetManager = activity.assets

            if (appBuiltinRootfs) {
                var input: InputStream? = null
                var out: OutputStream? = null
                try {
                    input = assetManager.open(filename)
                    val outFile = File(outputPath, filename)
                    out = Files.newOutputStream(outFile.toPath())
                    copyFile(input, out)
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    try {
                        input?.close()
                        out?.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        private fun getVulkanDriverInfo(info: String): String {
            return runCommandWithOutput("echo $(${getEnv()} DISPLAY= vulkaninfo | grep $info | cut -d '=' -f 2)")
        }

        @Throws(IOException::class)
        fun copyFile(input: InputStream, out: OutputStream?) {
            val buffer = ByteArray(1024)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                out!!.write(buffer, 0, read)
            }
        }

        private fun getClassPath(context: Context): String {
            return File(getLibsPath(context)).parentFile?.parentFile?.absolutePath + "/base.apk"
        }

        private fun getLibsPath(context: Context): String {
            return context.applicationInfo.nativeLibraryDir
        }

        suspend fun getMemoryInfo(context: Context) {
            withContext(Dispatchers.IO) {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memoryInfo = ActivityManager.MemoryInfo()
                var totalMemory: Long
                var availableMemory: Long
                var usedMemory: Long

                while (enableRamCounter) {
                    activityManager.getMemoryInfo(memoryInfo)

                    totalMemory = memoryInfo.totalMem / (1024 * 1024)
                    availableMemory = memoryInfo.availMem / (1024 * 1024)
                    usedMemory = totalMemory - availableMemory

                    memoryStats = "$usedMemory/$totalMemory"

                    Thread.sleep(800)
                }
            }
        }

        suspend fun getCpuInfo() {
            withContext(Dispatchers.IO) {
                val availProcessors = Runtime.getRuntime().availableProcessors()

                while (enableCpuCounter) {
                    val usageInfo = runCommandWithOutput("top -bn 1 -u \$(whoami) -o %CPU -q | head -n 1").toFloat() / availProcessors

                    totalCpuUsage = "$usageInfo%"

                    Thread.sleep(800)
                }
            }
        }

        val resolutions16_9 = arrayOf(
            "640x360", "854x480",
            "960x540", "1280x720",
            "1366x768", "1600x900",
            "1920x1080", "2560x1440",
            "3840x2160",
        )

        val resolutions4_3 = arrayOf(
            "640x480", "800x600",
            "1024x768", "1280x960",
            "1400x1050", "1600x1200",
        )

        @Suppress("DEPRECATION")
        fun getNativeResolution(context: Context): String {
            val windowManager = context.getSystemService(WindowManager::class.java)
            val displayMetrics = DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)

            return if (displayMetrics.widthPixels > displayMetrics.heightPixels) {
                "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
            } else {
                "${displayMetrics.heightPixels}x${displayMetrics.widthPixels}"
            }
        }

        private fun getPercentOfResolution(original: String, percent: Int): String {
            val resolution = original.split("x")
            val width = resolution[0].toInt() * percent / 100
            val height = resolution[1].toInt() * percent / 100

            return "${width}x$height"
        }

        fun getNativeResolutions(activity: Activity): List<String> {
            val parsedResolutions = mutableListOf<String>()
            val nativeResolution = getNativeResolution(activity)

            parsedResolutions.add(nativeResolution)
            parsedResolutions.add(getPercentOfResolution(nativeResolution, 90))
            parsedResolutions.add(getPercentOfResolution(nativeResolution, 80))
            parsedResolutions.add(getPercentOfResolution(nativeResolution, 70))
            parsedResolutions.add(getPercentOfResolution(nativeResolution, 60))
            parsedResolutions.add(getPercentOfResolution(nativeResolution, 50))
            parsedResolutions.add(getPercentOfResolution(nativeResolution, 40))
            parsedResolutions.add(getPercentOfResolution(nativeResolution, 30))

            return parsedResolutions
        }

        const val EXPORT_LNK_ACTION = 1
    }

    object PresetManager {
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
    }

    object RatManager {
        fun generateICDFile(driverLib: String, destIcd: File) {
            val json = gson.toJson(
                mapOf(
                    "ICD" to mapOf(
                        "api_version" to "1.1.296",
                        "library_path" to driverLib,
                    ),
                    "file_format_version" to "1.0.0",
                ),
            )

            destIcd.writeText(json)
        }

        fun generateMangoHUDConfFile() {
            val mangoHudConfFile = File("${Main.usrDir}/etc/MangoHud.conf")
            val options = StringBuilder()

            options.append("fps_limit=${Main.fpsLimit}\n")

            if (!Main.enableMangoHUD) {
                options.append("no_display\n")
            }

            mangoHudConfFile.writeText(options.toString())
        }
    }

    object VirtualControllerOverlayMapper {
        const val ACTION_EDIT_VIRTUAL_BUTTON = "com.micewine.emu.ACTION_EDIT_VIRTUAL_BUTTON"
        const val ACTION_INVALIDATE = "com.micewine.emu.ACTION_INVALIDATE"
    }

    object Shortcuts {
        private var gameListNames: MutableList<Game.Item> = mutableListOf()
        private var gameList: MutableList<GameItem> = getGameList()

        const val HIGHLIGHT_SHORTCUT_PREFERENCE_KEY = "highlightedShortcut"
        const val ACTION_UPDATE_WINE_PREFIX_SPINNER = "com.micewine.emu.ACTION_UPDATE_WINE_PREFIX_SPINNER"

        const val MESA_DRIVER = 0
        const val ADRENO_TOOLS_DRIVER = 1

        fun initialize() {
            gameList = getGameList()
        }

        fun putWineVirtualDesktop(name: String, enabled: Boolean) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].wineVirtualDesktop = enabled

            saveShortcuts()
        }

        fun getWineVirtualDesktop(name: String): Boolean {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return false

            return gameList[index].wineVirtualDesktop
        }

        fun putCpuAffinity(name: String, cpuCores: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].cpuAffinityCores = cpuCores

            saveShortcuts()
        }

        fun getCpuAffinity(name: String): String {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return DebugSettings.availableCPUs.joinToString(",")

            return gameList[index].cpuAffinityCores
        }

        fun putWineServices(name: String, enabled: Boolean) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].wineServices = enabled

            saveShortcuts()
        }

        fun getWineServices(name: String): Boolean {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return false

            return gameList[index].wineServices
        }

        fun putWineESync(name: String, enabled: Boolean) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].wineESync = enabled

            saveShortcuts()
        }

        fun getWineESync(name: String): Boolean {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return true

            return gameList[index].wineESync
        }

        fun putVKD3DVersion(name: String, vkd3dVersion: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].vkd3dVersion = vkd3dVersion

            saveShortcuts()
        }

        fun getVKD3DVersion(name: String): String {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return ""

            return gameList[index].vkd3dVersion
        }

        fun putWineD3DVersion(name: String, wineD3DVersion: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].wineD3DVersion = wineD3DVersion

            saveShortcuts()
        }

        fun getWineD3DVersion(name: String): String {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return ""

            return gameList[index].wineD3DVersion
        }

        fun putDXVKVersion(name: String, dxvkVersion: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].dxvkVersion = dxvkVersion

            saveShortcuts()
        }

        fun getDXVKVersion(name: String): String {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return ""

            return gameList[index].dxvkVersion
        }

        fun putD3DXRenderer(name: String, d3dxRenderer: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].d3dxRenderer = d3dxRenderer

            saveShortcuts()
        }

        fun getD3DXRenderer(name: String): String {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return "DXVK"

            return gameList[index].d3dxRenderer
        }

        private fun putVulkanDriverType(name: String, driverType: Int) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].vulkanDriverType = driverType

            saveShortcuts()
        }

        fun getVulkanDriverType(name: String): Int {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return MESA_DRIVER

            return gameList[index].vulkanDriverType
        }

        fun putVulkanDriver(name: String, driverName: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].vulkanDriver = driverName

            if (driverName.startsWith("AdrenoToolsDriver-")) {
                putVulkanDriverType(name, ADRENO_TOOLS_DRIVER)
            } else {
                (
                    putVulkanDriverType(name, MESA_DRIVER)
                    )
            }

            saveShortcuts()
        }

        fun getVulkanDriver(name: String): String {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return ""

            return gameList[index].vulkanDriver
        }

        fun putDisplaySettings(name: String, displayMode: String, displayResolution: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].displayMode = displayMode
            gameList[index].displayResolution = displayResolution

            saveShortcuts()
        }

        fun getDisplaySettings(name: String): List<String> {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return listOf("16:9", "1280x720")

            return listOf(gameList[index].displayMode, gameList[index].displayResolution)
        }

        fun putBox64Version(name: String, box64VersionId: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].box64Version = box64VersionId

            saveShortcuts()
        }

        fun getBox64Version(name: String): String {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return listRatPackagesId("Box64-").firstOrNull() ?: ""

            return gameList[index].box64Version
        }

        fun putBox64Preset(name: String, presetName: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].box64Preset = presetName

            saveShortcuts()
        }

        fun getBox64Preset(name: String): String {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return "default"

            return gameList[index].box64Preset
        }

        fun putControllerXInput(name: String, enabled: Boolean, controllerIndex: Int) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].controllersEnableXInput[controllerIndex] = enabled

            saveShortcuts()
        }

        fun getControllerXInput(name: String, controllerIndex: Int): Boolean {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return false

            return gameList[index].controllersEnableXInput[controllerIndex]
        }

        fun putControllerPreset(name: String, presetName: String, controllerIndex: Int) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].controllersPreset[controllerIndex] = presetName

            saveShortcuts()
        }

        fun getControllerPreset(name: String, controllerIndex: Int): String {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return "default"

            return gameList[index].controllersPreset[controllerIndex]
        }

        fun putVirtualControllerXInput(name: String, enabled: Boolean) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].virtualControllerEnableXInput = enabled

            saveShortcuts()
        }

        fun getVirtualControllerXInput(name: String): Boolean {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return false

            return gameList[index].virtualControllerEnableXInput
        }

        fun putVirtualControllerPreset(name: String, presetName: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].virtualControllerPreset = presetName

            saveShortcuts()
        }

        fun getVirtualControllerPreset(name: String): String {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return "default"

            return gameList[index].virtualControllerPreset
        }

        fun addGameToList(path: String, prettyName: String, icon: String) {
            val gameExists = gameList.any { it.name == prettyName }

            if (gameExists) return

            gameList.add(
                GameItem(
                    name = prettyName,
                    exePath = path,
                    exeArguments = "",
                    iconPath = icon,
                    box64Version = "Global",
                    box64Preset = "default",
                    controllersPreset = mutableListOf("default", "default", "default", "default"),
                    controllersEnableXInput = mutableListOf(false, false, false, false),
                    virtualControllerPreset = "default",
                    virtualControllerEnableXInput = false,
                    displayMode = "16:9",
                    displayResolution = "1280x720",
                    vulkanDriver = "Global",
                    vulkanDriverType = MESA_DRIVER,
                    d3dxRenderer = "DXVK",
                    dxvkVersion = listRatPackagesId("DXVK").first(),
                    wineD3DVersion = listRatPackagesId("WineD3D").first(),
                    vkd3dVersion = listRatPackagesId("VKD3D").first(),
                    wineESync = true,
                    wineServices = false,
                    cpuAffinityCores = DebugSettings.availableCPUs.joinToString(","),
                    wineVirtualDesktop = false,
                    enableXInput = false,
                ),
            )
            gameListNames.add(
                Game.Item(prettyName, path, "", icon),
            )

            saveShortcuts()

            // recyclerView?.post {
            //     recyclerView?.adapter?.notifyItemInserted(gameListNames.size)
            // }
        }

        fun removeGameFromList(name: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList.removeAt(index)
            gameListNames.removeAt(index)

            saveShortcuts()

            // recyclerView?.adapter?.notifyItemRemoved(index)
        }

        fun editGameFromList(name: String, newName: String, newArguments: String) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            gameList[index].name = newName
            gameList[index].exeArguments = newArguments
            gameListNames[index].name = newName
            gameListNames[index].exeArguments = newArguments

            saveShortcuts()

            // recyclerView?.adapter?.notifyItemChanged(index)
        }

        fun setIconToGame(name: String, context: Context, uri: Uri) {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return

            createIconCache(context, uri, name)

            gameList[index].iconPath = "${Main.usrDir}/icons/$name-icon"
            gameListNames[index].iconPath = "${Main.usrDir}/icons/$name-icon"

            saveShortcuts()

            // recyclerView?.adapter?.notifyItemChanged(index)
        }

        fun getGameIcon(name: String): Bitmap? {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return null

            return BitmapFactory.decodeFile(gameList[index].iconPath)
        }

        fun getGameExeArguments(name: String): String {
            val index = gameList.indexOfFirst { it.name == name }

            if (index == -1) return ""

            return gameList[index].exeArguments
        }

        private fun createIconCache(context: Context, uri: Uri, gameName: String) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val outputStream = File("${Main.usrDir}/icons/$gameName-icon").outputStream()

            Main.copyFile(inputStream!!, outputStream)
        }

        fun saveShortcuts() {
            PrefManager.putString("gameList", gson.toJson(gameList))
        }

        private fun getGameList(): MutableList<GameItem> {
            val json = PrefManager.getString("gameList", "")
            val listType = object : TypeToken<MutableList<GameItem>>() {}.type
            val gameList = gson.fromJson<MutableList<GameItem>>(json, listType)

            return gameList ?: mutableListOf()
        }

        fun addGameToLauncher(context: Context, name: String) {
            val index = gameList.indexOfFirst { it.name == name }
            val shortcutManager = context.getSystemService(ShortcutManager::class.java)

            if (shortcutManager!!.isRequestPinShortcutSupported) {
                val intent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra("exePath", gameList[index].exePath)
                    putExtra("exeArguments", gameList[index].exeArguments)
                }

                val pinShortcutInfo = ShortcutInfo.Builder(context, gameList[index].name)
                    .setShortLabel(gameList[index].name)
                    .setIcon(
                        if (File(gameList[index].iconPath).exists()) {
                            Icon.createWithBitmap(BitmapFactory.decodeFile(gameList[index].iconPath))
                        } else {
                            Icon.createWithResource(context, R.drawable.default_icon)
                        },
                    )
                    .setIntent(intent)
                    .build()

                val pinnedShortcutCallbackIntent = shortcutManager.createShortcutResultIntent(pinShortcutInfo)
                val successCallback = PendingIntent.getBroadcast(context, 0, pinnedShortcutCallbackIntent, PendingIntent.FLAG_IMMUTABLE)

                shortcutManager.requestPinShortcut(pinShortcutInfo, successCallback.intentSender)
            }
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
    }

    object DebugSettings {
        val availableCPUs = (0 until Runtime.getRuntime().availableProcessors()).map { it.toString() }.toTypedArray()
    }

    object Game {
        var selectedGameName = ""

        data class Item(var name: String, var exePath: String, var exeArguments: String, var iconPath: String)
    }

    object Setup {
        var progressBarValue: Int = 0
        var progressBarIsIndeterminate: Boolean = false
        var dialogTitleText: String = ""
        var abortSetup: Boolean = false
    }

    object Box64PresetManager {
        private val presetListNames: MutableList<Preset.Item> = mutableListOf()
        private var presetList: MutableList<MutableList<String>> = getBox64Presets()

        private val mappingMap = mapOf(
            GeneralSettings.BOX64_MMAP32 to 1,
            GeneralSettings.BOX64_AVX to 2,
            GeneralSettings.BOX64_SSE42 to 3,
            GeneralSettings.BOX64_DYNAREC_BIGBLOCK to 4,
            GeneralSettings.BOX64_DYNAREC_STRONGMEM to 5,
            GeneralSettings.BOX64_DYNAREC_WEAKBARRIER to 6,
            GeneralSettings.BOX64_DYNAREC_PAUSE to 7,
            GeneralSettings.BOX64_DYNAREC_X87DOUBLE to 8,
            GeneralSettings.BOX64_DYNAREC_FASTNAN to 9,
            GeneralSettings.BOX64_DYNAREC_FASTROUND to 10,
            GeneralSettings.BOX64_DYNAREC_SAFEFLAGS to 11,
            GeneralSettings.BOX64_DYNAREC_CALLRET to 12,
            GeneralSettings.BOX64_DYNAREC_ALIGNED_ATOMICS to 13,
            GeneralSettings.BOX64_DYNAREC_NATIVEFLAGS to 14,
            GeneralSettings.BOX64_DYNAREC_WAIT to 15,
            GeneralSettings.BOX64_DYNAREC_DIRTY to 16,
            GeneralSettings.BOX64_DYNAREC_FORWARD to 17,
        )

        fun initialize() {
            presetList = getBox64Presets()
        }

        fun getBox64Mapping(name: String, key: String): List<String> {
            val index = presetList.indexOfFirst { it[0] == name }

            if (index == -1) {
                return listOf("")
            }

            return presetList[index][mappingMap[key]!!].split(":")
        }

        fun editBox64Mapping(name: String, key: String, value: String) {
            var index = presetList.indexOfFirst { it[0] == name }

            if (index == -1) {
                presetList[0][0] = name
                index = 0
            }

            presetList[index][mappingMap[key]!!] = value

            saveBox64Preset()
        }

        fun addBox64Preset(context: Context, name: String) {
            if (presetListNames.firstOrNull { it.titleSettings == name } != null) {
                Toast.makeText(context, context.getString(R.string.executable_already_added), Toast.LENGTH_LONG).show()
                return
            }

            val defaultPreset = ArrayList(Collections.nCopies(mappingMap.size + 1, ":")).apply {
                this[0] = name
                this[mappingMap[GeneralSettings.BOX64_MMAP32]!!] = "true"
                this[mappingMap[GeneralSettings.BOX64_AVX]!!] = "2"
                this[mappingMap[GeneralSettings.BOX64_SSE42]!!] = "true"
                this[mappingMap[GeneralSettings.BOX64_DYNAREC_BIGBLOCK]!!] = "2"
                this[mappingMap[GeneralSettings.BOX64_DYNAREC_STRONGMEM]!!] = "1"
                this[mappingMap[GeneralSettings.BOX64_DYNAREC_WEAKBARRIER]!!] = "2"
                this[mappingMap[GeneralSettings.BOX64_DYNAREC_PAUSE]!!] = "0"
                this[mappingMap[GeneralSettings.BOX64_DYNAREC_X87DOUBLE]!!] = "false"
                this[mappingMap[GeneralSettings.BOX64_DYNAREC_FASTNAN]!!] = "true"
                this[mappingMap[GeneralSettings.BOX64_DYNAREC_FASTROUND]!!] = "true"
                this[mappingMap[GeneralSettings.BOX64_DYNAREC_SAFEFLAGS]!!] = "1"
                this[mappingMap[GeneralSettings.BOX64_DYNAREC_CALLRET]!!] = "true"
                this[mappingMap[GeneralSettings.BOX64_DYNAREC_ALIGNED_ATOMICS]!!] = "false"
                this[mappingMap[GeneralSettings.BOX64_DYNAREC_NATIVEFLAGS]!!] = "true"
                this[mappingMap[GeneralSettings.BOX64_DYNAREC_WAIT]!!] = "true"
                this[mappingMap[GeneralSettings.BOX64_DYNAREC_DIRTY]!!] = "false"
                this[mappingMap[GeneralSettings.BOX64_DYNAREC_FORWARD]!!] = "128"
            }

            presetList.add(defaultPreset)
            presetListNames.add(
                Preset.Item(name, CreatePreset.BOX64_PRESET, true),
            )

            // recyclerView?.adapter?.notifyItemInserted(presetListNames.size)

            saveBox64Preset()
        }

        fun deleteBox64Preset(name: String) {
            val index = presetList.indexOfFirst { it[0] == name }

            presetList.removeAt(index)
            presetListNames.removeAt(index)

            // recyclerView?.adapter?.notifyItemRemoved(index)

            if (index == Preset.selectedPresetId) {
                PrefManager.putString(PresetManager.SELECTED_BOX64_PRESET_KEY, presetListNames.first().titleSettings)

                // recyclerView?.adapter?.notifyItemChanged(0)
            }

            saveBox64Preset()
        }

        fun renameBox64Preset(name: String, newName: String) {
            val index = presetList.indexOfFirst { it[0] == name }

            presetList[index][0] = newName
            presetListNames[index].titleSettings = newName

            // recyclerView?.adapter?.notifyItemChanged(index)

            saveBox64Preset()
        }

        fun importBox64Preset(activity: Activity, path: String) {
            val json = File(path).readLines()
            val listType = object : TypeToken<MutableList<String>>() {}.type

            if (json.size < 2 || json[0] != "box64Preset") {
                activity.runOnUiThread {
                    Toast.makeText(activity, activity.getString(R.string.invalid_box64_preset_file), Toast.LENGTH_LONG).show()
                }
                return
            }

            val processed = gson.fromJson<MutableList<String>>(json[1], listType)

            var presetName = processed[0]
            var count = 1

            while (presetList.any { it[0] == presetName }) {
                presetName = "${processed[0]}-$count"
                count++
            }

            processed[0] = presetName

            presetList.add(processed)
            presetListNames.add(
                Preset.Item(processed[0], CreatePreset.BOX64_PRESET, true),
            )

            // activity.runOnUiThread {
            //     recyclerView?.adapter?.notifyItemInserted(presetListNames.size)
            // }

            saveBox64Preset()
        }

        fun exportBox64Preset(context: Context, name: String, path: String) {
            val index = presetList.indexOfFirst { it[0] == name }
            val file = File(path)

            file.writeText("box64Preset\n" + gson.toJson(presetList[index]))

            Toast.makeText(context, "Box64 Preset '$name' exported", Toast.LENGTH_LONG).show()
        }

        private fun saveBox64Preset() {
            PrefManager.putString("box64PresetList", gson.toJson(presetList))
        }

        fun getBox64Presets(): MutableList<MutableList<String>> {
            val json = PrefManager.getString("box64PresetList", "")
            val listType = object : TypeToken<MutableList<MutableList<String>>>() {}.type

            return gson.fromJson(json, listType) ?: mutableListOf(
                ArrayList(Collections.nCopies(mappingMap.size + 1, ":")).apply {
                    this[0] = "default"
                    this[mappingMap[GeneralSettings.BOX64_MMAP32]!!] = "true"
                    this[mappingMap[GeneralSettings.BOX64_AVX]!!] = "2"
                    this[mappingMap[GeneralSettings.BOX64_SSE42]!!] = "true"
                    this[mappingMap[GeneralSettings.BOX64_DYNAREC_BIGBLOCK]!!] = "2"
                    this[mappingMap[GeneralSettings.BOX64_DYNAREC_STRONGMEM]!!] = "1"
                    this[mappingMap[GeneralSettings.BOX64_DYNAREC_WEAKBARRIER]!!] = "2"
                    this[mappingMap[GeneralSettings.BOX64_DYNAREC_PAUSE]!!] = "0"
                    this[mappingMap[GeneralSettings.BOX64_DYNAREC_X87DOUBLE]!!] = "false"
                    this[mappingMap[GeneralSettings.BOX64_DYNAREC_FASTNAN]!!] = "true"
                    this[mappingMap[GeneralSettings.BOX64_DYNAREC_FASTROUND]!!] = "true"
                    this[mappingMap[GeneralSettings.BOX64_DYNAREC_SAFEFLAGS]!!] = "1"
                    this[mappingMap[GeneralSettings.BOX64_DYNAREC_CALLRET]!!] = "true"
                    this[mappingMap[GeneralSettings.BOX64_DYNAREC_ALIGNED_ATOMICS]!!] = "false"
                    this[mappingMap[GeneralSettings.BOX64_DYNAREC_NATIVEFLAGS]!!] = "true"
                    this[mappingMap[GeneralSettings.BOX64_DYNAREC_WAIT]!!] = "true"
                    this[mappingMap[GeneralSettings.BOX64_DYNAREC_DIRTY]!!] = "false"
                    this[mappingMap[GeneralSettings.BOX64_DYNAREC_FORWARD]!!] = "128"
                },
            )
        }
    }

    object Preset {
        const val PHYSICAL_CONTROLLER = 0
        const val VIRTUAL_CONTROLLER = 1
        var clickedPresetName = ""
        var clickedPresetType = -1
        var selectedPresetId = -1

        class Item(var titleSettings: String, var type: Int, var userPreset: Boolean, var showRadioButton: Boolean = false)
    }

    object CreatePreset {
        const val WINEPREFIX_PRESET = 1
        const val CONTROLLER_PRESET = 2
        const val VIRTUAL_CONTROLLER_PRESET = 3
        const val BOX64_PRESET = 4
    }

    object EnvVarsSettings {
        const val ENV_VARS_KEY = "environmentVariables"

        data class EnvironmentVariable(val key: String, val value: String)
    }

    object ControllerPresetManager {
        private val presetListNames: MutableList<Preset.Item> = mutableListOf()
        private var presetList: MutableList<MutableList<String>> = getControllerPresets()
        private var preferences: SharedPreferences? = null
        private var editShortcut: Boolean = false

        private val mappingMap = mapOf(
            PresetManager.BUTTON_A_KEY to 1,
            PresetManager.BUTTON_B_KEY to 2,
            PresetManager.BUTTON_X_KEY to 3,
            PresetManager.BUTTON_Y_KEY to 4,
            PresetManager.BUTTON_START_KEY to 5,
            PresetManager.BUTTON_SELECT_KEY to 6,
            PresetManager.BUTTON_R1_KEY to 7,
            PresetManager.BUTTON_R2_KEY to 8,
            PresetManager.BUTTON_L1_KEY to 9,
            PresetManager.BUTTON_L2_KEY to 10,
            PresetManager.BUTTON_THUMBL_KEY to 11,
            PresetManager.BUTTON_THUMBR_KEY to 12,
            PresetManager.AXIS_X_PLUS_KEY to 13,
            PresetManager.AXIS_X_MINUS_KEY to 14,
            PresetManager.AXIS_Y_PLUS_KEY to 15,
            PresetManager.AXIS_Y_MINUS_KEY to 16,
            PresetManager.AXIS_Z_PLUS_KEY to 17,
            PresetManager.AXIS_Z_MINUS_KEY to 18,
            PresetManager.AXIS_RZ_PLUS_KEY to 19,
            PresetManager.AXIS_RZ_MINUS_KEY to 20,
            PresetManager.AXIS_HAT_X_PLUS_KEY to 21,
            PresetManager.AXIS_HAT_X_MINUS_KEY to 22,
            PresetManager.AXIS_HAT_Y_PLUS_KEY to 23,
            PresetManager.AXIS_HAT_Y_MINUS_KEY to 24,
            GeneralSettings.DEAD_ZONE to 25,
            GeneralSettings.MOUSE_SENSIBILITY to 26,
        )

        fun initialize(boolean: Boolean = false) {
            presetList = getControllerPresets()
            editShortcut = boolean
        }

        fun getMouseSensibility(name: String): Int {
            val index = presetList.indexOfFirst { it[0] == name }

            if (index == -1) {
                return 100
            }

            return presetList[index][mappingMap[GeneralSettings.MOUSE_SENSIBILITY]!!].toInt()
        }

        fun putMouseSensibility(name: String, value: Int) {
            val index = presetList.indexOfFirst { it[0] == name }

            if (index == -1) {
                return
            }

            presetList[index][mappingMap[GeneralSettings.MOUSE_SENSIBILITY]!!] = value.toString()

            saveControllerPresets()
        }

        fun getDeadZone(name: String): Int {
            val index = presetList.indexOfFirst { it[0] == name }

            if (index == -1) {
                return 25
            }

            return presetList[index][mappingMap[GeneralSettings.DEAD_ZONE]!!].toInt()
        }

        fun putDeadZone(name: String, value: Int) {
            val index = presetList.indexOfFirst { it[0] == name }

            if (index == -1) {
                return
            }

            presetList[index][mappingMap[GeneralSettings.DEAD_ZONE]!!] = value.toString()

            saveControllerPresets()
        }

        fun getMapping(name: String, key: String): List<String> {
            val index = presetList.indexOfFirst { it[0] == name }

            if (index == -1) {
                return listOf("", "")
            }

            return presetList[index][mappingMap[key]!!].split(":")
        }

        fun editControllerPreset(name: String, key: String, selectedItem: String) {
            var index = presetList.indexOfFirst { it[0] == name }

            if (index == -1) {
                presetList[0][0] = name
                index = 0
            }

            presetList[index][mappingMap[key]!!] = selectedItem

            saveControllerPresets()
        }

        fun addControllerPreset(context: Context, name: String) {
            if (presetListNames.firstOrNull { it.titleSettings == name } != null) {
                Toast.makeText(context, context.getString(com.micewine.emu.R.string.executable_already_added), Toast.LENGTH_LONG).show()
                return
            }

            val defaultPreset = ArrayList(Collections.nCopies(mappingMap.size + 1, ":")).apply {
                this[0] = name
                this[mappingMap[GeneralSettings.DEAD_ZONE]!!] = "25"
                this[mappingMap[GeneralSettings.MOUSE_SENSIBILITY]!!] = "100"
            }

            presetList.add(defaultPreset)
            presetListNames.add(
                Preset.Item(name, Preset.PHYSICAL_CONTROLLER, true, editShortcut),
            )

            // recyclerView?.adapter?.notifyItemInserted(presetListNames.size)

            saveControllerPresets()
        }

        fun deleteControllerPreset(name: String) {
            val index = presetList.indexOfFirst { it[0] == name }

            presetList.removeAt(index)
            presetListNames.removeAt(index)

            // recyclerView?.adapter?.notifyItemRemoved(index)

            if (index == Preset.selectedPresetId) {
                PrefManager.putString(PresetManager.SELECTED_CONTROLLER_PRESET_KEY, presetListNames.first().titleSettings)
                // recyclerView?.adapter?.notifyItemChanged(0)
            }

            saveControllerPresets()
        }

        fun renameControllerPreset(name: String, newName: String) {
            val index = presetList.indexOfFirst { it[0] == name }

            presetList[index][0] = newName
            presetListNames[index].titleSettings = newName

            // recyclerView?.adapter?.notifyItemChanged(index)

            saveControllerPresets()
        }

        fun importControllerPreset(activity: Activity, path: String) {
            val json = File(path).readLines()
            val listType = object : TypeToken<MutableList<String>>() {}.type

            if (json.size < 2 || json[0] != "controllerPreset") {
                activity.runOnUiThread {
                    Toast.makeText(
                        activity,
                        activity.getString(com.micewine.emu.R.string.invalid_controller_preset_file),
                        Toast.LENGTH_LONG,
                    ).show()
                }
                return
            }

            val processed = gson.fromJson<MutableList<String>>(json[1], listType)

            var presetName = processed[0]
            var count = 1

            while (presetList.any { it[0] == presetName }) {
                presetName = "${processed[0]}-$count"
                count++
            }

            processed[0] = presetName

            presetList.add(processed)
            presetListNames.add(
                Preset.Item(processed[0], Preset.PHYSICAL_CONTROLLER, true),
            )

            // activity.runOnUiThread {
            //     recyclerView?.adapter?.notifyItemInserted(presetListNames.size)
            // }

            saveControllerPresets()
        }

        fun exportControllerPreset(name: String, path: String) {
            val index = presetList.indexOfFirst { it[0] == name }
            val file = File(path)

            file.writeText("controllerPreset\n" + gson.toJson(presetList[index]))
        }

        private fun saveControllerPresets() {
            preferences?.edit {
                putString("controllerPresetList", gson.toJson(presetList))
                apply()
            }
        }

        fun getControllerPresets(): MutableList<MutableList<String>> {
            val json = preferences?.getString("controllerPresetList", "")
            val listType = object : TypeToken<MutableList<List<String>>>() {}.type

            return gson.fromJson(json, listType) ?: mutableListOf(
                ArrayList(
                    Collections.nCopies(mappingMap.size + 1, ":"),
                ).apply {
                    this[0] = "default"
                    this[mappingMap[GeneralSettings.DEAD_ZONE]!!] = "25"
                    this[mappingMap[GeneralSettings.MOUSE_SENSIBILITY]!!] = "100"
                },
            )
        }
    }

    object VirtualControllerPresetManager {
        private val presetListNames: MutableList<Preset.Item> = mutableListOf()
        private var presetList: MutableList<VirtualControllerPreset> = mutableListOf()
        var preferences: SharedPreferences? = null
        private var editShortcut: Boolean = false

        fun initialize(context: Context, boolean: Boolean = false) {
            presetList = getVirtualControllerPresets(context)
            editShortcut = boolean
        }

        fun getMapping(name: String): VirtualControllerPreset? {
            val index = presetList.indexOfFirst { it.name == name }

            if (index == -1) return null

            return presetList[index]
        }

        fun putMapping(
            name: String,
            resolution: String,
            buttonList: MutableList<Overlay.VirtualButton>,
            analogList: MutableList<Overlay.VirtualAnalog>,
            dpadList: MutableList<Overlay.VirtualDPad>,
        ) {
            val index = presetList.indexOfFirst { it.name == name }

            if (index == -1) return

            presetList[index] = VirtualControllerPreset(name, resolution, mutableListOf(), mutableListOf(), mutableListOf())

            buttonList.forEach {
                presetList[index].buttons.add(it)
            }
            analogList.forEach {
                presetList[index].analogs.add(it)
            }
            dpadList.forEach {
                presetList[index].dpads.add(it)
            }

            saveVirtualControllerPresets()
        }

        fun addVirtualControllerPreset(context: Context, name: String) {
            if (presetListNames.firstOrNull { it.titleSettings == name } != null) {
                Toast.makeText(context, context.getString(R.string.executable_already_added), Toast.LENGTH_LONG).show()
                return
            }

            val defaultPreset = VirtualControllerPreset(name, "", mutableListOf(), mutableListOf(), mutableListOf())

            presetList.add(defaultPreset)
            presetListNames.add(
                Preset.Item(name, Preset.VIRTUAL_CONTROLLER, true, editShortcut),
            )

            // recyclerView?.adapter?.notifyItemInserted(presetListNames.size)

            saveVirtualControllerPresets()
        }

        fun deleteVirtualControllerPreset(name: String) {
            val index = presetList.indexOfFirst { it.name == name }

            presetList.removeAt(index)
            presetListNames.removeAt(index)

            // recyclerView?.adapter?.notifyItemRemoved(index)

            if (index == Preset.selectedPresetId) {
                preferences?.edit {
                    putString(PresetManager.SELECTED_VIRTUAL_CONTROLLER_PRESET_KEY, presetListNames.first().titleSettings)
                    apply()
                }

                // recyclerView?.adapter?.notifyItemChanged(0)
            }

            saveVirtualControllerPresets()
        }

        fun renameVirtualControllerPreset(name: String, newName: String) {
            val index = presetList.indexOfFirst { it.name == name }

            presetList[index].name = newName
            presetListNames[index].titleSettings = newName

            // recyclerView?.adapter?.notifyItemChanged(index)

            saveVirtualControllerPresets()
        }

        fun importVirtualControllerPreset(activity: Activity, path: String) {
            val lines = File(path).readLines()
            val canAutoAdjust = lines[1].contains("resolution")
            val listType = object : TypeToken<VirtualControllerPreset>() {}.type

            if (lines.size < 2 || lines[0] != "virtualControllerPreset") {
                activity.runOnUiThread {
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.invalid_virtual_controller_preset_file),
                        Toast.LENGTH_LONG,
                    ).show()
                }
                return
            }

            val processed = gson.fromJson<VirtualControllerPreset>(lines[1], listType)

            var presetName = processed.name
            var count = 1

            while (presetList.any { it.name == presetName }) {
                presetName = "${processed.name}-$count"
                count++
            }

            processed.name = presetName

            if (canAutoAdjust) {
                val nativeResolution = Main.getNativeResolution(activity)

                if (processed.resolution != nativeResolution) {
                    val nativeSplit = nativeResolution.split("x").map { it.toFloat() }
                    val processedSplit = processed.resolution.split("x").map { it.toFloat() }

                    val multiplierX = nativeSplit[0] / processedSplit[0] * 100F
                    val multiplierY = nativeSplit[1] / processedSplit[1] * 100F

                    processed.buttons.forEach {
                        it.x = (it.x / 100F * multiplierX / OverlayCreator.GRID_SIZE).roundToInt() * OverlayCreator.GRID_SIZE.toFloat()
                        it.y = (it.y / 100F * multiplierY / OverlayCreator.GRID_SIZE).roundToInt() * OverlayCreator.GRID_SIZE.toFloat()
                    }
                    processed.analogs.forEach {
                        it.x = (it.x / 100F * multiplierX / OverlayCreator.GRID_SIZE).roundToInt() * OverlayCreator.GRID_SIZE.toFloat()
                        it.y = (it.y / 100F * multiplierY / OverlayCreator.GRID_SIZE).roundToInt() * OverlayCreator.GRID_SIZE.toFloat()
                    }
                    processed.dpads.forEach {
                        it.x = (it.x / 100F * multiplierX / OverlayCreator.GRID_SIZE).roundToInt() * OverlayCreator.GRID_SIZE.toFloat()
                        it.y = (it.y / 100F * multiplierY / OverlayCreator.GRID_SIZE).roundToInt() * OverlayCreator.GRID_SIZE.toFloat()
                    }
                }
            }

            presetList.add(processed)
            presetListNames.add(
                Preset.Item(processed.name, Preset.VIRTUAL_CONTROLLER, true),
            )

            // recyclerView?.post {
            //     recyclerView?.adapter?.notifyItemInserted(presetListNames.size)
            // }

            saveVirtualControllerPresets()
        }

        fun exportVirtualControllerPreset(name: String, path: String) {
            val index = presetList.indexOfFirst { it.name == name }
            val file = File(path)

            file.writeText("virtualControllerPreset\n" + gson.toJson(presetList[index]))
        }

        private fun saveVirtualControllerPresets() {
            preferences?.edit {
                putString("virtualControllerPresetList", gson.toJson(presetList))
                apply()
            }
        }

        fun getVirtualControllerPresets(context: Context): MutableList<VirtualControllerPreset> {
            val json = preferences?.getString("virtualControllerPresetList", "")
            val listType = object : TypeToken<MutableList<VirtualControllerPreset>>() {}.type

            return gson.fromJson(json, listType) ?: mutableListOf(
                VirtualControllerPreset("default", Main.getNativeResolution(context), mutableListOf(), mutableListOf(), mutableListOf()),
            )
        }

        data class VirtualControllerPreset(
            var name: String,
            var resolution: String,
            var analogs: MutableList<Overlay.VirtualAnalog>,
            var buttons: MutableList<Overlay.VirtualButton>,
            var dpads: MutableList<Overlay.VirtualDPad>,
        )
    }

    object Overlay {
        const val SHAPE_CIRCLE = 0
        const val SHAPE_SQUARE = 1
        const val SHAPE_RECTANGLE = 2
        const val SHAPE_DPAD = 3

        val buttonList = mutableListOf<VirtualButton>()
        val analogList = mutableListOf<VirtualAnalog>()
        val dpadList = mutableListOf<VirtualDPad>()

        fun detectClick(event: MotionEvent, index: Int, x: Float, y: Float, radius: Float, shape: Int): Boolean {
            return when (shape) {
                SHAPE_RECTANGLE -> {
                    (event.getX(index) >= x - radius / 2 && event.getX(index) <= (x + (radius / 2))) &&
                        (event.getY(index) >= y - radius / 4 && event.getY(index) <= (y + (radius / 4)))
                }

                SHAPE_DPAD -> {
                    (event.getX(index) >= x - radius - 20 && event.getX(index) <= (x + (radius - 20))) &&
                        (event.getY(index) >= y - radius - 20 && event.getY(index) <= (y + (radius - 20)))
                }

                else -> (event.getX(index) >= x - radius / 2 && event.getX(index) <= (x + (radius / 2))) &&
                    (event.getY(index) >= y - radius / 2 && event.getY(index) <= (y + (radius / 2)))
            }
        }

        class VirtualButton(
            var id: Int,
            var x: Float,
            var y: Float,
            var radius: Float,
            var keyName: String,
            var keyCodes: List<Int>?,
            var fingerId: Int,
            var isPressed: Boolean,
            var shape: Int,
        )

        class VirtualDPad(
            var id: Int,
            var x: Float,
            var y: Float,
            var radius: Float,
            var upKeyName: String,
            var upKeyCodes: List<Int>,
            var downKeyName: String,
            var downKeyCodes: List<Int>,
            var leftKeyName: String,
            var leftKeyCodes: List<Int>,
            var rightKeyName: String,
            var rightKeyCodes: List<Int>,
            var fingerId: Int,
            var isPressed: Boolean,
            var fingerX: Float,
            var fingerY: Float,
            var dpadStatus: Int,
        )

        class VirtualAnalog(
            var id: Int,
            var x: Float,
            var y: Float,
            var fingerX: Float,
            var fingerY: Float,
            var radius: Float,
            var upKeyName: String,
            var upKeyCodes: List<Int>,
            var downKeyName: String,
            var downKeyCodes: List<Int>,
            var leftKeyName: String,
            var leftKeyCodes: List<Int>,
            var rightKeyName: String,
            var rightKeyCodes: List<Int>,
            var isPressed: Boolean,
            var fingerId: Int,
            var deadZone: Float,
        )
    }

    object OverlayCreator {
        const val BUTTON = 0
        const val ANALOG = 1
        const val DPAD = 2
        const val GRID_SIZE = 35

        var lastSelectedButton = 0
        var lastSelectedType = BUTTON
    }
}
