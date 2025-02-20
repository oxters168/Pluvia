package com.winlator.core

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.system.ErrnoException
import android.system.Os
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Stack
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.math.max
import timber.log.Timber

object FileUtils {

    fun read(context: Context, assetFile: String): ByteArray? {
        return try {
            context.assets.open(assetFile).use { inStream ->
                return@use StreamUtils.copyToByteArray(inStream)
            }
        } catch (e: IOException) {
            null
        }
    }

    fun read(file: File?): ByteArray? {
        return try {
            BufferedInputStream(FileInputStream(file)).use { inStream ->
                return@use StreamUtils.copyToByteArray(inStream)
            }
        } catch (e: IOException) {
            null
        }
    }

    fun readString(context: Context, assetFile: String): String {
        return String(read(context, assetFile)!!, StandardCharsets.UTF_8)
    }

    fun readString(file: File?): String {
        return String(read(file)!!, StandardCharsets.UTF_8)
    }

    fun readString(context: Context, uri: Uri): String? {
        val sb = StringBuilder()
        try {
            context.contentResolver.openInputStream(uri).use { inputStream ->
                BufferedReader(
                    InputStreamReader(inputStream),
                ).use { reader ->
                    var line: String?

                    while ((reader.readLine().also { line = it }) != null) {
                        sb.append(line)
                    }

                    return sb.toString()
                }
            }
        } catch (e: IOException) {
            return null
        }
    }

    fun write(file: File?, data: ByteArray): Boolean {
        try {
            FileOutputStream(file).use { os ->
                os.write(data, 0, data.size)
                return true
            }
        } catch (e: IOException) {
            Timber.w(e)
        }

        return false
    }

    fun writeString(file: File?, data: String?): Boolean {
        try {
            BufferedWriter(FileWriter(file)).use { bw ->
                bw.write(data)
                bw.flush()

                return true
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return false
    }

    fun symlink(linkTarget: File, linkFile: File) {
        symlink(linkTarget.absolutePath, linkFile.absolutePath)
    }

    fun symlink(linkTarget: String?, linkFile: String) {
        try {
            (File(linkFile)).delete()
            Os.symlink(linkTarget, linkFile)
        } catch (e: ErrnoException) {
            Timber.w(e)
        }
    }

    fun isSymlink(file: File): Boolean {
        return Files.isSymbolicLink(file.toPath())
    }

    @JvmStatic
    fun delete(targetFile: File?): Boolean {
        if (targetFile == null) {
            return false
        }

        if (targetFile.isDirectory) {
            if (!isSymlink(targetFile)) {
                if (!clear(targetFile)) {
                    return false
                }
            }
        }

        return targetFile.delete()
    }

    fun clear(targetFile: File?): Boolean {
        if (targetFile == null) {
            return false
        }

        if (targetFile.isDirectory) {
            val files = targetFile.listFiles()

            if (files != null) {
                for (file in files) {
                    if (!delete(file)) {
                        return false
                    }
                }
            }
        }

        return true
    }

    fun isEmpty(targetFile: File?): Boolean {
        if (targetFile == null) {
            return true
        }

        if (targetFile.isDirectory) {
            val files = targetFile.list()

            return files == null || files.isEmpty()
        } else {
            return targetFile.length() == 0L
        }
    }

    @JvmOverloads
    fun copy(srcFile: File, dstFile: File, callback: Callback<File>? = null): Boolean {
        if (isSymlink(srcFile)) {
            return true
        }

        if (srcFile.isDirectory) {
            if (!dstFile.exists() && !dstFile.mkdirs()) {
                return false
            }

            callback?.call(dstFile)

            val filenames = srcFile.list()
            if (filenames != null) {
                for (filename in filenames) {
                    if (!copy(File(srcFile, filename), File(dstFile, filename), callback)) {
                        return false
                    }
                }
            }
        } else {
            val parent = dstFile.parentFile

            if (!srcFile.exists() || (parent != null && !parent.exists() && !parent.mkdirs())) {
                return false
            }

            try {
                val inChannel = (FileInputStream(srcFile)).channel
                val outChannel = (FileOutputStream(dstFile)).channel

                inChannel.transferTo(0, inChannel.size(), outChannel)

                inChannel.close()
                outChannel.close()

                callback?.call(dstFile)
                return dstFile.exists()
            } catch (e: IOException) {
                return false
            }
        }

        return true
    }

    fun copy(context: Context, assetFile: String, dstFile: File) {
        var dstFile = dstFile

        if (isDirectory(context, assetFile)) {
            if (!dstFile.isDirectory) {
                dstFile.mkdirs()
            }

            try {
                val filenames = context.assets.list(assetFile)

                for (filename in filenames!!) {
                    val relativePath = StringUtils.addEndSlash(assetFile) + filename
                    if (isDirectory(context, relativePath)) {
                        copy(context, relativePath, File(dstFile, filename))
                    } else {
                        copy(context, relativePath, dstFile)
                    }
                }
            } catch (e: IOException) {
                Timber.w(e)
            }
        } else {
            if (dstFile.isDirectory) {
                dstFile = File(dstFile, getName(assetFile))
            }

            val parent = dstFile.parentFile

            if (!parent!!.isDirectory) {
                parent.mkdirs()
            }

            try {
                context.assets.open(assetFile).use { inStream ->
                    BufferedOutputStream(
                        FileOutputStream(dstFile), StreamUtils.BUFFER_SIZE,
                    ).use { outStream ->
                        StreamUtils.copy(inStream, outStream)
                    }
                }
            } catch (e: IOException) {
                Timber.w(e)
            }
        }
    }

    fun readLines(file: File?): ArrayList<String> {
        val lines = ArrayList<String>()

        try {
            FileInputStream(file).use { fis ->
                val reader = BufferedReader(InputStreamReader(fis))
                var line: String

                while ((reader.readLine().also { line = it }) != null) {
                    lines.add(line)
                }
            }
        } catch (e: IOException) {
            Timber.w(e)
        }

        return lines
    }

    fun getName(path: String): String {
        var path: String = path

        path = StringUtils.removeEndSlash(path)

        val index = max(path.lastIndexOf('/').toDouble(), path.lastIndexOf('\\').toDouble()).toInt()

        return path.substring(index + 1)
    }

    fun getBasename(path: String): String {
        return getName(path).replaceFirst("\\.[^\\.]+$".toRegex(), "")
    }

    @JvmStatic
    fun getDirname(path: String): String {
        var path: String = path
        path = StringUtils.removeEndSlash(path)

        val index = max(path.lastIndexOf('/').toDouble(), path.lastIndexOf('\\').toDouble()).toInt()

        return path.substring(0, index)
    }

    fun chmod(file: File, mode: Int) {
        try {
            Os.chmod(file.absolutePath, mode)
        } catch (e: ErrnoException) {
            Timber.w(e)
        }
    }

    fun createTempFile(parent: File?, prefix: String): File {
        var tempFile: File? = null
        var exists = true

        while (exists) {
            tempFile = File(parent, prefix + "-" + UUID.randomUUID().toString().replace("-", "") + ".tmp")
            exists = tempFile.exists()
        }

        return tempFile!!
    }

    fun getFilePathFromUri(uri: Uri): String? {
        var path: String? = null

        if (uri.authority == "com.android.externalstorage.documents") {
            val parts = uri.lastPathSegment!!.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

            if (parts[0].equals("primary", ignoreCase = true)) {
                path = Environment.getExternalStorageDirectory().toString() + "/" + parts[1]
            }
        }

        return path
    }

    fun contentEquals(origin: File, target: File): Boolean {
        if (origin.length() != target.length()) {
            return false
        }

        try {
            BufferedInputStream(FileInputStream(origin)).use { inStream1 ->
                BufferedInputStream(FileInputStream(target)).use { inStream2 ->
                    var data: Int

                    while ((inStream1.read().also { data = it }) != -1) {
                        if (data != inStream2.read()) return false
                    }

                    return true
                }
            }
        } catch (e: IOException) {
            return false
        }
    }

    fun getSizeAsync(file: File?, callback: Callback<Long>) {
        Executors.newSingleThreadExecutor().execute { getSize(file, callback) }
    }

    private fun getSize(file: File?, callback: Callback<Long>) {
        if (file == null) {
            return
        }

        if (file.isFile) {
            callback.call(file.length())
            return
        }

        val stack = Stack<File>()
        stack.push(file)

        while (!stack.isEmpty()) {
            val current = stack.pop()
            val files = current.listFiles() ?: continue

            for (f in files) {
                if (f.isDirectory) {
                    stack.push(f)
                } else {
                    val length = f.length()
                    if (length > 0) callback.call(length)
                }
            }
        }
    }

    fun getSize(context: Context, assetFile: String): Long {
        try {
            context.assets.open(assetFile).use { inStream ->
                return inStream.available().toLong()
            }
        } catch (e: IOException) {
            return 0
        }
    }

    val internalStorageSize: Long
        get() {
            val dataDir = Environment.getDataDirectory()
            val stat = StatFs(dataDir.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            return totalBlocks * blockSize
        }

    fun isDirectory(context: Context, assetFile: String): Boolean {
        try {
            val files = context.assets.list(assetFile)
            return !files.isNullOrEmpty()
        } catch (e: IOException) {
            return false
        }
    }

    fun toRelativePath(basePath: String, fullPath: String): String {
        return StringUtils.removeEndSlash(
            (if (fullPath.startsWith("/")) "/" else "") + (File(basePath).toURI().relativize(File(fullPath).toURI()).path),
        )
    }

    fun readInt(path: String?): Int {
        var result = 0

        try {
            RandomAccessFile(path, "r").use { reader ->
                val line = reader.readLine()

                result = if (line.isNotEmpty()) {
                    line.toInt()
                } else {
                    0
                }
            }
        } catch (e: Exception) {
            Timber.w(e)
        }

        return result
    }

    fun readSymlink(file: File): String {
        return try {
            Files.readSymbolicLink(file.toPath()).toString()
        } catch (e: IOException) {
            ""
        }
    }
}
