package com.winlator.container

import android.content.Context
import android.os.Handler
import com.winlator.R
import com.winlator.core.Callback
import com.winlator.core.FileUtils
import com.winlator.core.OnExtractFileListener
import com.winlator.core.TarCompressorUtils
import com.winlator.core.WineInfo
import com.winlator.xenvironment.ImageFs
import java.io.File
import java.util.concurrent.Executors
import java.util.function.Function
import kotlin.math.max
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

class ContainerManager(private val context: Context) {

    val containers: ArrayList<Container> = ArrayList<Container>()

    private var maxContainerId = 0

    private val homeDir: File

    init {
        val rootDir = ImageFs.find(context).rootDir
        homeDir = File(rootDir, "home")

        loadContainers()
    }

    private fun loadContainers() {
        containers.clear()
        maxContainerId = 0

        try {
            val files = homeDir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory()) {
                        if (file.getName().startsWith(ImageFs.USER + "-")) {
                            val container =
                                Container(file.getName().replace(ImageFs.USER + "-", "").toInt())
                            container.rootDir = File(homeDir, ImageFs.USER + "-" + container.id)
                            val data = JSONObject(FileUtils.readString(container.configFile))
                            container.loadData(data)
                            containers.add(container)
                            maxContainerId =
                                max(maxContainerId.toDouble(), container.id.toDouble()).toInt()
                        }
                    }
                }
            }
        } catch (e: JSONException) {
        }
    }

    fun activateContainer(container: Container) {
        container.rootDir = File(homeDir, ImageFs.USER + "-" + container.id)
        val file = File(homeDir, ImageFs.USER)
        file.delete()
        FileUtils.symlink("./" + ImageFs.USER + "-" + container.id, file.path)
    }

    fun createContainerAsync(data: JSONObject, callback: Callback<Container?>) {
        val handler = Handler()
        Executors.newSingleThreadExecutor().execute(
            Runnable {
                val container = createContainer(data)
                handler.post(Runnable { callback.call(container) })
            },
        )
    }

    fun duplicateContainerAsync(container: Container, callback: Runnable) {
        val handler = Handler()
        Executors.newSingleThreadExecutor().execute(
            Runnable {
                duplicateContainer(container)
                handler.post(callback)
            },
        )
    }

    fun removeContainerAsync(container: Container, callback: Runnable) {
        val handler = Handler()
        Executors.newSingleThreadExecutor().execute(
            Runnable {
                removeContainer(container)
                handler.post(callback)
            },
        )
    }

    private fun createContainer(data: JSONObject): Container? {
        try {
            val id = maxContainerId + 1
            data.put("id", id)

            val containerDir = File(homeDir, ImageFs.USER + "-" + id)
            if (!containerDir.mkdirs()) return null

            val container = Container(id)
            container.rootDir = containerDir
            container.loadData(data)

            val isMainWineVersion = !data.has("wineVersion") || WineInfo.isMainWineVersion(data.getString("wineVersion"))

            if (!isMainWineVersion) container.wineVersion = data.getString("wineVersion")

            if (!extractContainerPatternFile(container.wineVersion, containerDir, null)) {
                FileUtils.delete(containerDir)
                return null
            }

            container.saveData()
            maxContainerId++
            containers.add(container)
            return container
        } catch (e: JSONException) {
        }
        return null
    }

    private fun duplicateContainer(srcContainer: Container) {
        val id = maxContainerId + 1

        val dstDir = File(homeDir, ImageFs.USER + "-" + id)

        if (!dstDir.mkdirs()) return

        if (!FileUtils.copy(srcContainer.rootDir, dstDir, Callback { file: File? -> FileUtils.chmod(file, 505) })) {
            FileUtils.delete(dstDir)
            return
        }

        val dstContainer = Container(id).apply {
            rootDir = dstDir
            name = srcContainer.name + " (" + context.getString(R.string.copy) + ")"
            screenSize = srcContainer.screenSize
            envVars = srcContainer.envVars
            cpuList = srcContainer.cpuList
            cpuListWoW64 = srcContainer.cpuListWoW64
            graphicsDriver = srcContainer.graphicsDriver
            dXWrapper = srcContainer.dXWrapper
            dXWrapperConfig = srcContainer.dXWrapperConfig
            audioDriver = srcContainer.audioDriver
            winComponents = srcContainer.winComponents
            drives = srcContainer.drives
            isShowFPS = srcContainer.isShowFPS
            isWoW64Mode = srcContainer.isWoW64Mode
            startupSelection = srcContainer.startupSelection
            box86Preset = srcContainer.box86Preset
            box64Preset = srcContainer.box64Preset
            desktopTheme = srcContainer.desktopTheme
        }.also { it.saveData() }

        maxContainerId++
        containers.add(dstContainer)
    }

    private fun removeContainer(container: Container) {
        if (FileUtils.delete(container.rootDir)) {
            containers.remove(container)
        }
    }

    fun loadShortcuts(): ArrayList<Shortcut?> {
        val shortcuts = ArrayList<Shortcut?>()
        containers.forEach { container ->
            val desktopDir = container.desktopDir
            desktopDir.listFiles().orEmpty().forEach { file ->
                if (file.getName().endsWith(".desktop")) {
                    shortcuts.add(Shortcut(container, file))
                }
            }
        }

        shortcuts.sortWith(Comparator.comparing<Shortcut?, String?>(Function { a: Shortcut? -> a!!.name }))

        return shortcuts
    }

    fun getNextContainerId(): Int {
        return maxContainerId + 1
    }

    fun getContainerById(id: Int): Container? {
        for (container in containers) {
            if (container.id == id) {
                return container
            }
        }

        return null
    }

    @Throws(JSONException::class)
    private fun extractCommonDlls(
        srcName: String?,
        dstName: String,
        commonDlls: JSONObject,
        containerDir: File?,
        onExtractFileListener: OnExtractFileListener?,
    ) {
        val srcDir = File(ImageFs.find(context).rootDir, "/opt/wine/lib/wine/$srcName")
        val dlnames = commonDlls.getJSONArray(dstName)

        for (i in 0..<dlnames.length()) {
            val dlname = dlnames.getString(i)
            var dstFile: File? = File(containerDir, ".wine/drive_c/windows/$dstName/$dlname")

            if (onExtractFileListener != null) {
                dstFile = onExtractFileListener.onExtractFile(dstFile, 0)
                if (dstFile == null) continue
            }

            FileUtils.copy(File(srcDir, dlname), dstFile)
        }
    }

    fun extractContainerPatternFile(
        wineVersion: String?,
        containerDir: File?,
        onExtractFileListener: OnExtractFileListener?,
    ): Boolean {
        if (WineInfo.isMainWineVersion(wineVersion)) {
            val result = TarCompressorUtils.extract(
                TarCompressorUtils.Type.ZSTD,
                context,
                "container_pattern.tzst",
                containerDir,
                onExtractFileListener,
            )

            if (result) {
                try {
                    val commonDlls = JSONObject(FileUtils.readString(context, "common_dlls.json"))
                    extractCommonDlls("x86_64-windows", "system32", commonDlls, containerDir, onExtractFileListener)
                    extractCommonDlls("i386-windows", "syswow64", commonDlls, containerDir, onExtractFileListener)
                } catch (e: JSONException) {
                    Timber.w(e)
                    return false
                }
            }

            return result
        } else {
            val installedWineDir = ImageFs.find(context).installedWineDir
            val wineInfo = WineInfo.fromIdentifier(context, wineVersion)
            val suffix = wineInfo.fullVersion() + "-" + wineInfo.arch
            val file = File(installedWineDir, "container-pattern-$suffix.tzst")

            return TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, file, containerDir, onExtractFileListener)
        }
    }
}
