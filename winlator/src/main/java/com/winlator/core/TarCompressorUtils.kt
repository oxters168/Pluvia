package com.winlator.core

import android.content.Context
import android.net.Uri
import android.system.OsConstants
import com.winlator.core.FileUtils
import com.winlator.core.StreamUtils
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import org.apache.commons.compress.archivers.ArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.archivers.tar.TarConstants
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorOutputStream
import timber.log.Timber

object TarCompressorUtils {

    private fun addFile(tar: ArchiveOutputStream<TarArchiveEntry>, file: File, entryName: String) {
        try {
            tar.putArchiveEntry(tar.createArchiveEntry(file, entryName))

            BufferedInputStream(FileInputStream(file), StreamUtils.BUFFER_SIZE).use { inStream ->
                StreamUtils.copy(inStream, tar)
            }

            tar.closeArchiveEntry()
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    private fun addLinkFile(tar: ArchiveOutputStream<TarArchiveEntry>, file: File, entryName: String) {
        try {
            val entry = TarArchiveEntry(entryName, TarConstants.LF_SYMLINK)
            entry.linkName = FileUtils.readSymlink(file)
            tar.putArchiveEntry(entry)
            tar.closeArchiveEntry()
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    @Throws(IOException::class)
    private fun addDirectory(tar: ArchiveOutputStream<TarArchiveEntry>, folder: File, basePath: String) {
        val files = folder.listFiles() ?: return
        for (file in files) {
            if (FileUtils.isSymlink(file)) {
                addLinkFile(tar, file, basePath + file.name)
            } else if (file.isDirectory) {
                val entryName = basePath + file.name + "/"
                tar.putArchiveEntry(tar.createArchiveEntry(folder, entryName))
                tar.closeArchiveEntry()
                addDirectory(tar, file, entryName)
            } else {
                addFile(tar, file, basePath + file.name)
            }
        }
    }

    @JvmOverloads
    fun compress(type: Type, file: File, destination: File?, level: Int = 3) {
        compress(type, arrayOf(file), destination, level)
    }

    fun compress(type: Type, files: Array<File>, destination: File?, level: Int) {
        try {
            getCompressorOutputStream(type, destination, level).use { outStream ->
                TarArchiveOutputStream(outStream).use { tar ->
                    tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
                    for (file in files) {
                        if (FileUtils.isSymlink(file)) {
                            addLinkFile(tar, file, file.name)
                        } else if (file.isDirectory) {
                            val basePath = file.name + "/"
                            tar.putArchiveEntry(tar.createArchiveEntry(file, basePath))
                            tar.closeArchiveEntry()
                            addDirectory(tar, file, basePath)
                        } else {
                            addFile(tar, file, file.name)
                        }
                    }
                    tar.finish()
                }
            }
        } catch (e: IOException) {
            Timber.w(e)
        }
    }

    @JvmOverloads
    fun extract(
        type: Type,
        context: Context,
        assetFile: String,
        destination: File?,
        onExtractFileListener: OnExtractFileListener? = null,
    ): Boolean {
        return try {
            extract(type, context.assets.open(assetFile), destination, onExtractFileListener)
        } catch (e: IOException) {
            false
        }
    }

    @JvmOverloads
    fun extract(
        type: Type,
        context: Context,
        source: Uri?,
        destination: File,
        onExtractFileListener: OnExtractFileListener? = null,
    ): Boolean {
        if (source == null) return false
        return try {
            extract(type, context.contentResolver.openInputStream(source), destination, onExtractFileListener)
        } catch (e: FileNotFoundException) {
            false
        }
    }

    @JvmOverloads
    fun extract(
        type: Type,
        source: File?,
        destination: File?,
        onExtractFileListener: OnExtractFileListener? = null,
    ): Boolean {
        if (source == null || !source.isFile) return false
        return try {
            extract(type, BufferedInputStream(FileInputStream(source), StreamUtils.BUFFER_SIZE), destination, onExtractFileListener)
        } catch (e: FileNotFoundException) {
            false
        }
    }

    private fun extract(type: Type, source: InputStream?, destination: File?, onExtractFileListener: OnExtractFileListener?): Boolean {
        if (source == null) {
            return false
        }

        return try {
            getCompressorInputStream(type, source).use { inStream ->
                TarArchiveInputStream(inStream).use { tar ->
                    var entry: TarArchiveEntry? = null
                    while (tar.nextEntry?.let {
                            entry = it
                            true
                        } == true
                    ) {
                        entry?.let { currentEntry ->
                            if (!tar.canReadEntryData(currentEntry)) return@let
                            var file = File(destination, currentEntry.name)
                            if (onExtractFileListener != null) {
                                file = onExtractFileListener.onExtractFile(file, currentEntry.size) ?: return@let
                            }
                            if (currentEntry.isDirectory) {
                                if (!file.isDirectory) file.mkdirs()
                            } else {
                                if (currentEntry.isSymbolicLink) {
                                    FileUtils.symlink(currentEntry.linkName, file.absolutePath)
                                } else {
                                    BufferedOutputStream(FileOutputStream(file), StreamUtils.BUFFER_SIZE).use { outStream ->
                                        if (!StreamUtils.copy(tar, outStream)) return false
                                    }
                                }
                            }

                            val mode = (OsConstants.S_IRWXU or OsConstants.S_IRWXG or OsConstants.S_IXOTH) // 0771
                            FileUtils.chmod(file, mode)
                        }
                    }
                    true
                }
            }
        } catch (e: IOException) {
            false
        }
    }

    @Throws(IOException::class)
    private fun getCompressorInputStream(type: Type, source: InputStream): InputStream? {
        if (type == Type.XZ) {
            return XZCompressorInputStream(source)
        } else if (type == Type.ZSTD) {
            return ZstdCompressorInputStream(source)
        }

        return null
    }

    @Throws(IOException::class)
    private fun getCompressorOutputStream(type: Type, destination: File?, level: Int): OutputStream? {
        if (type == Type.XZ) {
            return XZCompressorOutputStream(BufferedOutputStream(FileOutputStream(destination), StreamUtils.BUFFER_SIZE), level)
        } else if (type == Type.ZSTD) {
            return ZstdCompressorOutputStream(BufferedOutputStream(FileOutputStream(destination), StreamUtils.BUFFER_SIZE), level)
        }

        return null
    }

    enum class Type {
        XZ,
        ZSTD,
    }
}
