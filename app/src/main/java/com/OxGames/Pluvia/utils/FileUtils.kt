package com.OxGames.Pluvia.utils

import android.content.res.AssetManager
import android.os.StatFs
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import timber.log.Timber

object FileUtils {

    fun getAvailableSpace(path: String): Long {
        val stat = StatFs(path)
        return stat.blockSizeLong * stat.availableBlocksLong
    }

    fun getFolderSize(folderPath: String): Long {
        val folder = File(folderPath)
        return if (folder.exists()) {
            folder.walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()
        } else {
            0L
        }
    }

    fun formatBinarySize(bytes: Long, decimalPlaces: Int = 2): String {
        require(bytes > Long.MIN_VALUE) { "Out of range" }
        require(decimalPlaces >= 0) { "Negative decimal places unsupported" }

        val isNegative = bytes < 0
        val absBytes = kotlin.math.abs(bytes)

        if (absBytes < 1024) {
            return "$bytes B"
        }

        val units = arrayOf("KiB", "MiB", "GiB", "TiB", "PiB")
        val digitGroups = (63 - absBytes.countLeadingZeroBits()) / 10
        val value = absBytes.toDouble() / (1L shl (digitGroups * 10))

        val result = "%.${decimalPlaces}f %s".format(
            if (isNegative) -value else value,
            units[digitGroups - 1],
        )

        return result
    }

    fun makeDir(dirName: String) {
        val homeItemsDir = File(dirName)
        homeItemsDir.mkdirs()
    }

    fun makeFile(fileName: String, errorTag: String? = "FileUtils", errorMsg: ((Exception) -> String)? = null) {
        try {
            val file = File(fileName)
            if (!file.exists()) {
                file.createNewFile()
            }
        } catch (e: Exception) {
            Timber.e("%s encountered an issue in makeFile()", errorTag)
            Timber.e(errorMsg?.invoke(e) ?: "Error creating file: $e")
        }
    }

    fun createPathIfNotExist(filepath: String) {
        val file = File(filepath)
        var dirs = filepath

        // if the file path is not a directory and if we're not at the root directory then get the parent directory
        if (!filepath.endsWith('/') && filepath.lastIndexOf('/') > 0) {
            dirs = file.parent!!
        }

        makeDir(dirs)
    }

    fun readFileAsString(path: String, errorTag: String = "FileUtils", errorMsg: ((Exception) -> String)? = null): String? {
        var fileData: String? = null
        try {
            val r = BufferedReader(FileReader(path))
            val total = StringBuilder()
            var line: String?

            while ((r.readLine().also { line = it }) != null) {
                total.append(line).append('\n')
            }

            fileData = total.toString()
        } catch (e: Exception) {
            Timber.e("%s encountered an issue in readFileAsString()", errorTag)
            Timber.e(errorMsg?.invoke(e) ?: "Error reading file: $e")
        }

        return fileData
    }

    fun writeStringToFile(data: String, path: String, errorTag: String? = "FileUtils", errorMsg: ((Exception) -> String)? = null) {
        createPathIfNotExist(path)

        try {
            val fOut = FileOutputStream(path)
            val myOutWriter = OutputStreamWriter(fOut)
            myOutWriter.append(data)
            myOutWriter.close()
            fOut.flush()
            fOut.close()
        } catch (e: Exception) {
            Timber.e("%s encounted an issue in writeStringToFile()", errorTag)
            Timber.e(errorMsg?.invoke(e) ?: "Error writing to file: $e")
        }
    }

    fun findFiles(rootPath: Path, pattern: String, includeDirectories: Boolean = false): Stream<Path> {
        val patternParts = pattern.split("*").filter { it.isNotEmpty() }
        Timber.i("$pattern -> $patternParts")
        if (!Files.exists(rootPath)) return emptyList<Path>().stream()
        return Files.list(rootPath).filter { path ->
            if (path.isDirectory() && !includeDirectories) {
                false
            } else {
                val fileName = path.name
                Timber.i("Checking $fileName for pattern $pattern")
                var startIndex = 0
                !patternParts.map {
                    val index = fileName.indexOf(it, startIndex)
                    if (index >= 0) {
                        startIndex = index + it.length
                    }
                    index
                }.any { it < 0 }
            }
        }
    }

    fun assetExists(assetManager: AssetManager, assetPath: String): Boolean {
        return try {
            assetManager.open(assetPath).use {
                true
            }
        } catch (e: IOException) {
            // Timber.e(e)
            false
        }
    }
}
