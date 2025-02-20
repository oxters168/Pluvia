package com.winlator.container

import android.os.Environment
import com.winlator.box86_64.Box86_64Preset
import com.winlator.core.DefaultVersion
import com.winlator.core.EnvVars
import com.winlator.core.FileUtils
import com.winlator.core.KeyValueSet
import com.winlator.core.WineInfo
import com.winlator.core.WineThemeManager
import com.winlator.xenvironment.ImageFs
import java.io.File
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

class Container(val id: Int) {
    var audioDriver: String = DEFAULT_AUDIO_DRIVER
    var box64Preset: String = Box86_64Preset.COMPATIBILITY
    var box86Preset: String = Box86_64Preset.COMPATIBILITY
    var cpuList: String = ""
    var cpuListWoW64: String = ""
    var dXWrapper: String = DEFAULT_DXWRAPPER
    var dXWrapperConfig: String = ""
    var desktopTheme: String = WineThemeManager.DEFAULT_DESKTOP_THEME
    var drives: String = DEFAULT_DRIVES
    var envVars: String = DEFAULT_ENV_VARS
    var extraData: JSONObject? = null
    var graphicsDriver: String = DEFAULT_GRAPHICS_DRIVER
    var isShowFPS: Boolean = false
    var isWoW64Mode: Boolean = true
    var name: String = "Container-$id"
    var rootDir: File? = null
    var screenSize: String = DEFAULT_SCREEN_SIZE
    var startupSelection: Byte = STARTUP_SELECTION_ESSENTIAL
    var winComponents: String = DEFAULT_WINCOMPONENTS
    var wineVersion: String = WineInfo.MAIN_WINE_VERSION.identifier()

    var box86Version: String = DefaultVersion.BOX86
    var box64Version: String = DefaultVersion.BOX64

    var dxwrapper: String = DEFAULT_DXWRAPPER
    var dxwrapperConfig: String = ""

    fun getCPUList(allowFallback: Boolean = false): String? {
        return if (cpuList.isNotEmpty()) cpuList else (if (allowFallback) fallbackCPUList else null)
    }

    fun getCPUListWoW64(allowFallback: Boolean = false): String? {
        return if (cpuListWoW64.isNotEmpty()) cpuListWoW64 else (if (allowFallback) fallbackCPUListWoW64 else null)
    }

    fun getExtra(name: String): String? {
        return getExtra(name, "")
    }

    fun getExtra(name: String, fallback: String): String {
        try {
            return if (extraData != null && extraData!!.has(name)) extraData!!.getString(name) else fallback
        } catch (e: JSONException) {
            Timber.w(e)
            return fallback
        }
    }

    fun putExtra(name: String, value: Any?) {
        if (extraData == null) extraData = JSONObject()
        try {
            if (value != null) {
                extraData!!.put(name, value)
            } else {
                extraData!!.remove(name)
            }
        } catch (e: JSONException) {
            Timber.w(e)
        }
    }

    val configFile: File
        get() = File(rootDir, ".container")

    val desktopDir: File
        get() = File(rootDir, ".wine/drive_c/users/" + ImageFs.USER + "/Desktop/")

    val startMenuDir: File
        get() = File(rootDir, ".wine/drive_c/ProgramData/Microsoft/Windows/Start Menu/")

    fun getIconsDir(size: Int): File? {
        return File(rootDir, ".local/share/icons/hicolor/" + size + "x" + size + "/apps/")
    }

    fun drivesIterator(): Iterable<Array<String>> {
        return drivesIterator(drives)
    }

    fun saveData() {
        try {
            val data = JSONObject()
            data.put("id", id)
            data.put("name", name)
            data.put("screenSize", screenSize)
            data.put("envVars", envVars)
            data.put("cpuList", cpuList)
            data.put("cpuListWoW64", cpuListWoW64)
            data.put("graphicsDriver", graphicsDriver)
            data.put("dxwrapper", dXWrapper)

            if (dXWrapperConfig.isNotEmpty()) {
                data.put("dxwrapperConfig", dXWrapperConfig)
            }

            data.put("audioDriver", audioDriver)
            data.put("wincomponents", winComponents)
            data.put("drives", drives)
            data.put("showFPS", isShowFPS)
            data.put("wow64Mode", isWoW64Mode)
            data.put("startupSelection", startupSelection.toInt())
            data.put("box86Preset", box86Preset)
            data.put("box64Preset", box64Preset)
            data.put("desktopTheme", desktopTheme)
            data.put("extraData", extraData)

            if (!WineInfo.isMainWineVersion(wineVersion)) data.put("wineVersion", wineVersion)
            FileUtils.writeString(configFile, data.toString())
        } catch (e: JSONException) {
            Timber.w(e)
        }
    }

    @Throws(JSONException::class)
    fun loadData(data: JSONObject) {
        wineVersion = WineInfo.MAIN_WINE_VERSION.identifier()
        dXWrapperConfig = ""
        checkObsoleteOrMissingProperties(data)

        val it = data.keys()
        while (it.hasNext()) {
            val key = it.next()
            when (key) {
                "name" -> name = data.getString(key)
                "screenSize" -> screenSize = data.getString(key)
                "envVars" -> envVars = data.getString(key)
                "cpuList" -> cpuList = data.getString(key)
                "cpuListWoW64" -> cpuListWoW64 = data.getString(key)
                "graphicsDriver" -> graphicsDriver = data.getString(key)
                "wincomponents" -> winComponents = data.getString(key)
                "dxwrapper" -> dXWrapper = data.getString(key)
                "dxwrapperConfig" -> dXWrapperConfig = data.getString(key)
                "drives" -> drives = data.getString(key)
                "showFPS" -> isShowFPS = data.getBoolean(key)
                "wow64Mode" -> isWoW64Mode = data.getBoolean(key)
                "startupSelection" -> startupSelection = data.getInt(key).toByte()
                "extraData" -> {
                    val extraData = data.getJSONObject(key)
                    checkObsoleteOrMissingProperties(extraData)
                    this.extraData = extraData
                }

                "wineVersion" -> wineVersion = data.getString(key)
                "box86Preset" -> box86Preset = data.getString(key)
                "box64Preset" -> box64Preset = data.getString(key)
                "audioDriver" -> audioDriver = data.getString(key)
                "desktopTheme" -> desktopTheme = data.getString(key)
            }
        }
    }

    companion object {
        const val DEFAULT_ENV_VARS: String =
            "ZINK_DESCRIPTORS=lazy ZINK_DEBUG=compact MESA_SHADER_CACHE_DISABLE=false MESA_SHADER_CACHE_MAX_SIZE=512MB mesa_glthread=true WINEESYNC=1 MESA_VK_WSI_PRESENT_MODE=mailbox TU_DEBUG=noconform"
        const val DEFAULT_SCREEN_SIZE: String = "1280x720"
        const val DEFAULT_GRAPHICS_DRIVER: String = "turnip"
        const val DEFAULT_AUDIO_DRIVER: String = "alsa"
        const val DEFAULT_DXWRAPPER: String = "dxvk"
        const val DEFAULT_WINCOMPONENTS: String =
            "direct3d=1,directsound=1,directmusic=0,directshow=0,directplay=0,vcrun2010=1,wmdecoder=1"
        const val FALLBACK_WINCOMPONENTS: String =
            "direct3d=0,directsound=0,directmusic=0,directshow=0,directplay=0,vcrun2010=0,wmdecoder=0"
        val DEFAULT_DRIVES: String =
            "D:" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "E:/data/data/com.winlator/storage"
        const val STARTUP_SELECTION_NORMAL: Byte = 0
        const val STARTUP_SELECTION_ESSENTIAL: Byte = 1
        const val STARTUP_SELECTION_AGGRESSIVE: Byte = 2
        const val MAX_DRIVE_LETTERS: Byte = 8

        fun drivesIterator(drives: String): Iterable<Array<String>> {
            val index = intArrayOf(drives.indexOf(":"))
            val item = arrayOfNulls<String>(2)

            return Iterable {
                object : Iterator<Array<String>> {
                    override fun hasNext(): Boolean {
                        return index[0] != -1
                    }

                    override fun next(): Array<String> {
                        item[0] = drives[index[0] - 1].toString()
                        val nextIndex = drives.indexOf(":", index[0] + 1)
                        item[1] = drives.substring(index[0] + 1, if (nextIndex != -1) nextIndex - 1 else drives.length)
                        index[0] = nextIndex
                        return item as Array<String>
                    }
                }
            }
        }

        fun checkObsoleteOrMissingProperties(data: JSONObject) {
            try {
                if (data.has("dxcomponents")) {
                    data.put("wincomponents", data.getString("dxcomponents"))
                    data.remove("dxcomponents")
                }

                if (data.has("dxwrapper")) {
                    val dxwrapper = data.getString("dxwrapper")
                    if (dxwrapper == "original-wined3d") {
                        data.put("dxwrapper", DEFAULT_DXWRAPPER)
                    } else if (dxwrapper.startsWith("d8vk-") || dxwrapper.startsWith("dxvk-")) {
                        data.put("dxwrapper", dxwrapper.substring(0, dxwrapper.indexOf("-")))
                    }
                }

                if (data.has("graphicsDriver")) {
                    val graphicsDriver = data.getString("graphicsDriver")
                    if (graphicsDriver == "turnip-zink") {
                        data.put("graphicsDriver", "turnip")
                    } else if (graphicsDriver == "llvmpipe") {
                        data.put("graphicsDriver", "virgl")
                    }
                }

                if (data.has("envVars") && data.has("extraData")) {
                    val extraData = data.getJSONObject("extraData")
                    val appVersion = extraData.optString("appVersion", "0").toInt()
                    if (appVersion < 16) {
                        val defaultEnvVars = EnvVars(DEFAULT_ENV_VARS)
                        val envVars = EnvVars(data.getString("envVars"))
                        for (name in defaultEnvVars) {
                            if (!envVars.has(name)) {
                                envVars.put(name, defaultEnvVars.get(name))
                            }
                        }
                        data.put("envVars", envVars.toString())
                    }
                }

                val wincomponents1 = KeyValueSet(DEFAULT_WINCOMPONENTS)
                val wincomponents2 = KeyValueSet(data.getString("wincomponents"))
                var result = ""

                for (wincomponent1 in wincomponents1) {
                    var value = wincomponent1[1]

                    for (wincomponent2 in wincomponents2) {
                        if (wincomponent1[0] == wincomponent2[0]) {
                            value = wincomponent2[1]
                            break
                        }
                    }

                    result += (if (result.isNotEmpty()) "," else "") + wincomponent1[0] + "=" + value
                }

                data.put("wincomponents", result)
            } catch (e: JSONException) {
                Timber.w(e)
            }
        }

        val fallbackCPUList: String
            get() {
                var cpuList = ""
                val numProcessors = Runtime.getRuntime().availableProcessors()
                for (i in 0..<numProcessors) cpuList += (if (cpuList.isNotEmpty()) "," else "") + i
                return cpuList
            }

        val fallbackCPUListWoW64: String
            get() {
                var cpuList = ""
                val numProcessors = Runtime.getRuntime().availableProcessors()
                for (i in numProcessors / 2..<numProcessors) cpuList += (if (cpuList.isNotEmpty()) "," else "") + i
                return cpuList
            }
    }
}
