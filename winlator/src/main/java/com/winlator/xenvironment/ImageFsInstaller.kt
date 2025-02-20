package com.winlator.xenvironment

import android.content.Context
import com.winlator.R
import com.winlator.container.ContainerManager
import com.winlator.core.FileUtils
import com.winlator.core.TarCompressorUtils
import com.winlator.core.WineInfo
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

object ImageFsInstaller {
    const val LATEST_VERSION: Byte = 8

    private fun resetContainerImgVersions(context: Context) {
        val manager = ContainerManager(context)
        for (container in manager.containers) {
            val imgVersion = container.getExtra("imgVersion")
            val wineVersion = container.wineVersion

            if (imgVersion!!.isNotEmpty() && WineInfo.isMainWineVersion(wineVersion) && imgVersion.toShort() <= 5) {
                container.putExtra("wineprefixNeedsUpdate", "t")
            }

            container.putExtra("imgVersion", null)
            container.saveData()
        }
    }

    // TODO: Coroutines
    private fun installFromAssets(context: Context, onProgress: (Int) -> Unit, onFailure: (String) -> Unit = {}) {
        val imageFs = ImageFs.find(context)
        val rootDir = imageFs.rootDir

        Executors.newSingleThreadExecutor().execute {
            clearRootDir(rootDir)
            val compressionRatio: Byte = 22
            val contentLength = (FileUtils.getSize(context, "imagefs.txz") * (100.0f / compressionRatio)).toLong()
            val totalSizeRef = AtomicLong()

            val success = TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, context, "imagefs.txz", rootDir) { file, size ->
                if (size > 0) {
                    val totalSize = totalSizeRef.addAndGet(size)
                    val progress = ((totalSize.toFloat() / contentLength) * 100).toInt()

                    onProgress(progress)
                }

                file
            }

            if (success) {
                imageFs.createImgVersionFile(LATEST_VERSION.toInt())
                resetContainerImgVersions(context)
            } else {
                onFailure(context.getString(R.string.unable_to_install_system_files))
            }
        }
    }

    fun installIfNeeded(context: Context, onProgress: (Int) -> Unit) {
        val imageFs = ImageFs.find(context)

        if (!imageFs.isValid || imageFs.version < LATEST_VERSION) {
            installFromAssets(context, onProgress)
        }
    }

    private fun clearOptDir(optDir: File) {
        optDir.listFiles()
            .orEmpty()
            .forEach { file ->
                if (file.name == "installed-wine") {
                    return@forEach
                }

                FileUtils.delete(file)
            }
    }

    private fun clearRootDir(rootDir: File) {
        if (rootDir.isDirectory) {
            rootDir.listFiles()
                .orEmpty()
                .forEach { file ->
                    if (file.isDirectory) {
                        val name = file.name
                        if (name == "home" || name == "opt") {
                            if (name == "opt") {
                                clearOptDir(file)
                            }

                            return@forEach
                        }
                    }

                    FileUtils.delete(file)
                }
        } else {
            rootDir.mkdirs()
        }
    }
}
