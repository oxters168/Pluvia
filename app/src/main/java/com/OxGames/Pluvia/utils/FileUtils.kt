package com.OxGames.Pluvia.utils

import android.content.res.AssetManager
import android.os.StatFs
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Stream
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    /**
     * Move games from internal only storage to user storage.
     * This should be removed after a few versions and just
     * remove the old path to free up space.
     */
    suspend fun moveGamesFromOldPath(
        sourceDir: String,
        targetDir: String,
        onProgressUpdate: (currentFile: String, fileProgress: Float, movedFiles: Int, totalFiles: Int) -> Unit,
        onComplete: () -> Unit,
    ) = withContext(Dispatchers.IO) {
        try {
            val sourcePath = Paths.get(sourceDir)
            val targetPath = Paths.get(targetDir)

            if (!Files.exists(targetPath)) {
                Files.createDirectories(targetPath)
            }

            val allFiles = mutableListOf<Path>()
            Files.walkFileTree(
                sourcePath,
                object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        if (Files.isRegularFile(file)) {
                            allFiles.add(file)
                        }
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
                        Timber.e(exc, "Failed to visit file: $file")
                        return FileVisitResult.CONTINUE
                    }
                },
            )

            val totalFiles = allFiles.size
            var filesMoved = 0

            for (sourcePath in allFiles) {
                val relativePath = sourcePath.subpath(Paths.get(sourceDir).nameCount, sourcePath.nameCount)
                val targetFilePath = Paths.get(targetDir, relativePath.toString())

                Files.createDirectories(targetFilePath.parent)

                // val fileName = sourcePath.fileName.toString()

                try {
                    Files.move(sourcePath, targetFilePath, StandardCopyOption.ATOMIC_MOVE)

                    withContext(Dispatchers.Main) {
                        onProgressUpdate(relativePath.toString(), 1f, filesMoved++, totalFiles)
                    }
                } catch (e: Exception) {
                    val fileSize = Files.size(sourcePath)
                    var bytesCopied = 0L

                    FileChannel.open(sourcePath, StandardOpenOption.READ).use { sourceChannel ->
                        FileChannel.open(
                            targetFilePath,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.WRITE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                        ).use { targetChannel ->
                            val buffer = ByteBuffer.allocateDirect(8 * 1024 * 1024)
                            var bytesRead: Int

                            while (sourceChannel.read(buffer).also { bytesRead = it } > 0) {
                                buffer.flip()
                                targetChannel.write(buffer)
                                buffer.compact()

                                bytesCopied += bytesRead

                                val fileProgress = if (fileSize > 0) {
                                    bytesCopied.toFloat() / fileSize
                                } else {
                                    1f
                                }

                                withContext(Dispatchers.Main) {
                                    onProgressUpdate(relativePath.toString(), fileProgress, filesMoved, totalFiles)
                                }
                            }

                            targetChannel.force(true)
                        }
                    }

                    Files.delete(sourcePath)
                    withContext(Dispatchers.Main) {
                        onProgressUpdate(relativePath.toString(), 1f, filesMoved++, totalFiles)
                    }
                }
            }

            Files.walkFileTree(
                sourcePath,
                object : SimpleFileVisitor<Path>() {
                    override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                        if (exc == null) {
                            try {
                                var isEmpty = true
                                Files.newDirectoryStream(dir).use { stream ->
                                    if (stream.iterator().hasNext()) {
                                        isEmpty = false
                                    }
                                }

                                if (isEmpty && (dir != sourcePath)) {
                                    Files.delete(dir)
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to delete directory: $dir")
                            }
                        }
                        return FileVisitResult.CONTINUE
                    }
                },
            )

            try {
                var isEmpty = true
                Files.newDirectoryStream(sourcePath).use { stream ->
                    if (stream.iterator().hasNext()) {
                        isEmpty = false
                    }
                }

                if (isEmpty) {
                    Files.delete(sourcePath)
                }
            } catch (e: Exception) {
                Timber.e(e)
            }

            withContext(Dispatchers.Main) {
                onComplete()
            }
        } catch (e: Exception) {
            Timber.e(e)
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}
