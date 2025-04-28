package com.micewine.emu.utils

import com.micewine.emu.activities.MainActivity
import java.io.File

object DriveUtils {
    class DriveInfo(val letter: String, val source: String, private val relativePath: String) {
        private val drivesBaseDirectory = MainActivity.fileManagerDefaultDir + UNIX_SEPARATOR

        fun getWindowsPath(absolute: Boolean = false): String {
            var path = "$letter:$WINDOWS_SEPARATOR" + relativePath.replace(
                UNIX_SEPARATOR, WINDOWS_SEPARATOR)
            if (absolute) {
                path = drivesBaseDirectory.replace(UNIX_SEPARATOR, WINDOWS_SEPARATOR) + path
            }
            return path
        }

        fun getUnixPath(): String {
            val relativePath = if (relativePath.endsWith(UNIX_SEPARATOR)) {
                relativePath.substring(0, relativePath.lastIndexOf(UNIX_SEPARATOR))
            } else {
                relativePath.substring(0, relativePath.lastIndexOf(UNIX_SEPARATOR))
            }
            return "$drivesBaseDirectory$letter:$UNIX_SEPARATOR$relativePath"
        }
    }

    private const val UNIX_SEPARATOR = "/"
    private const val WINDOWS_SEPARATOR = "\\"

    private val drivesBaseDirectory = MainActivity.fileManagerDefaultDir + UNIX_SEPARATOR

    fun parseUnixPath(path: String): DriveInfo? {
        val drives = File(drivesBaseDirectory).listFiles()
        drives?.mapIndexed { _, file ->
            if (file.isDirectory && path.startsWith(file.absolutePath)) {
                val symlinkTo = file.canonicalPath
                val letter = file.absolutePath.replace(drivesBaseDirectory, "")[0].toString()
                val relativePath = path.replace(drivesBaseDirectory, "").replace("$letter:$UNIX_SEPARATOR", "")
                return DriveInfo(letter, symlinkTo, relativePath)
            }
        }
        return null
    }

    fun parseWindowsPath(path: String): DriveInfo? {
        val absPath = drivesBaseDirectory + path.replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR)
        return parseUnixPath(absPath)
    }
}