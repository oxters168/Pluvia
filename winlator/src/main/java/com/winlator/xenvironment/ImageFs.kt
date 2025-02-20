package com.winlator.xenvironment

import android.content.Context
import com.winlator.core.FileUtils.readLines
import com.winlator.core.FileUtils.toRelativePath
import com.winlator.core.FileUtils.writeString
import java.io.File
import java.io.IOException
import java.util.Locale

class ImageFs private constructor(val rootDir: File) {

    companion object {
        const val USER: String = "xuser"
        const val HOME_PATH: String = "/home/$USER"
        const val CACHE_PATH: String = "/home/$USER/.cache"
        const val CONFIG_PATH: String = "/home/$USER/.config"
        const val WINEPREFIX: String = "/home/$USER/.wine"

        fun find(context: Context): ImageFs = ImageFs(File(context.filesDir, "imagefs"))
    }

    var winePath: String = "/opt/wine"
        set(winePath) {
            field = toRelativePath(rootDir.path, winePath)
        }

    val isValid: Boolean
        get() = rootDir.isDirectory && imgVersionFile.exists()

    val version: Int
        get() {
            val imgVersionFile = imgVersionFile
            return if (imgVersionFile.exists()) readLines(imgVersionFile)[0].toInt() else 0
        }

    val formattedVersion: String
        get() = String.format(Locale.ENGLISH, "%.1f", version.toFloat())

    fun createImgVersionFile(version: Int) {
        configDir.mkdirs()

        val file = imgVersionFile

        try {
            file.createNewFile()
            writeString(file, version.toString())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    val configDir: File
        get() = File(rootDir, ".winlator")

    val imgVersionFile: File
        get() = File(configDir, ".img_version")

    val installedWineDir: File
        get() = File(rootDir, "/opt/installed-wine")

    val tmpDir: File
        get() = File(rootDir, "/tmp")

    val lib32Dir: File
        get() = File(rootDir, "/usr/lib/arm-linux-gnueabihf")

    val lib64Dir: File
        get() = File(rootDir, "/usr/lib/aarch64-linux-gnu")

    override fun toString(): String = rootDir.path
}
