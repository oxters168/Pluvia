package com.micewine.emu.utils

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import com.micewine.emu.MiceWineUtils
import java.io.File
import androidx.core.net.toUri
import timber.log.Timber

object FilePathResolver {
    fun resolvePath(context: Context, uri: Uri): String? {
        val realPath = resolveRealPathFromUri(context, uri) // Get the resolved real path

        if (realPath != null) {
            getDosHomeDirs().forEach { dosHome ->
                val symlinkTo = File(dosHome).canonicalPath
                if (realPath.startsWith(symlinkTo)) {
                    val resolvedPath = realPath.replaceFirst(symlinkTo, dosHome)
                    return resolvedPath
                }
            }
        }

        return realPath
    }

    private fun getDosHomeDirs(): List<String> {
        val dosHomeDirs = mutableListOf<String>()
        val dosHomeDir = File(MiceWineUtils.Main.fileManagerDefaultDir)

        if (dosHomeDir.exists() && dosHomeDir.isDirectory) {
            dosHomeDirs.addAll(dosHomeDir.listFiles()?.map { it.absolutePath } ?: emptyList())
        }

        return dosHomeDirs
    }

    private fun resolveRealPathFromUri(context: Context, uri: Uri): String? {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                return if ("primary".equals(type, ignoreCase = true)) {
                    if (split.size > 1) {
                        Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                    } else {
                        Environment.getExternalStorageDirectory().toString() + "/"
                    }
                } else {
                    if (File("storage" + "/" + docId.replace(":", "/")).exists()) {
                        return "/storage/" + docId.replace(":", "/")
                    }
                    val availableExternalStorages = getStorageDirectories()
                    var root = ""
                    for (s in availableExternalStorages) {
                        root = if (split[1].startsWith("/")) {
                            s + split[1]
                        } else {
                            s + "/" + split[1]
                        }
                    }
                    if (root.contains(type)) {
                        "storage" + "/" + docId.replace(":", "/")
                    } else {
                        if (root.startsWith("/storage/") || root.startsWith("storage/")) {
                            root
                        } else if (root.startsWith("/")) {
                            "/storage$root"
                        } else {
                            "/storage/$root"
                        }
                    }
                }
            } else if (isRawDownloadsDocument(uri)) {
                val fileName = getFilePath(context, uri)
                val subFolderName = getSubFolders(uri)
                if (fileName != null) {
                    return Environment.getExternalStorageDirectory().toString() + "/Download/" + subFolderName + fileName
                }
                val id = DocumentsContract.getDocumentId(uri)
                val contentUri = ContentUris.withAppendedId(
                    "content://downloads/public_downloads".toUri(),
                    java.lang.Long.valueOf(id),
                )

                return getDataColumn(context, contentUri, null, null)
            } else if (isDownloadsDocument(uri)) {
                val fileName = getFilePath(context, uri)

                if (fileName != null) {
                    return Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName
                }
                var id = DocumentsContract.getDocumentId(uri)

                if (id.startsWith("raw:")) {
                    id = id.replaceFirst("raw:".toRegex(), "")
                    val file = File(id)
                    if (file.exists()) return id
                }
                if (id.startsWith("raw%3A%2F")) {
                    id = id.replaceFirst("raw%3A%2F".toRegex(), "")
                    val file = File(id)
                    if (file.exists()) return id
                }
                val contentUri = ContentUris.withAppendedId(
                    "content://downloads/public_downloads".toUri(),
                    java.lang.Long.valueOf(id),
                )

                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])

                return getDataColumn(context, null, selection, selectionArgs)
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            if (getDataColumn(context, uri, null, null) == null) {
                Timber.tag("FileResolver").d("Failed to resolve path from uri: dataReturnedNull")
            }
            return getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }

    private fun getSubFolders(uri: Uri): String {
        val replaceChars = uri.toString().replace("%2F", "/").replace("%20", " ").replace("%3A", ":")
        val bits = replaceChars.split("/").toTypedArray()
        val sub5 = bits[bits.size - 2]
        val sub4 = bits[bits.size - 3]
        val sub3 = bits[bits.size - 4]
        val sub2 = bits[bits.size - 5]
        val sub1 = bits[bits.size - 6]

        return when {
            sub1 == "Download" -> "$sub2/$sub3/$sub4/$sub5/"
            sub2 == "Download" -> "$sub3/$sub4/$sub5/"
            sub3 == "Download" -> "$sub4/$sub5/"
            sub4 == "Download" -> "$sub5/"
            else -> ""
        }
    }

    private fun getDataColumn(
        context: Context,
        uri: Uri?,
        selection: String?,
        selectionArgs: Array<String>?,
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)
        try {
            cursor =
                context.contentResolver.query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } catch (e: Exception) {
            Timber.tag("FileResolver").d("Failed to resolve path from uri: ${e.message}")
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun getFilePath(context: Context, uri: Uri?): String? {
        var cursor: Cursor? = null
        val projection = arrayOf(MediaStore.Files.FileColumns.DISPLAY_NAME)
        try {
            cursor = context.contentResolver.query(
                uri!!, projection, null, null,
                null,
            )
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                return cursor.getString(index)
            }
        } catch (e: Exception) {
            Timber.tag("FileResolver").d("Failed to resolve path from uri: ${e.message}")
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isRawDownloadsDocument(uri: Uri): Boolean {
        val uriToString = uri.toString()
        return uriToString.contains("com.android.providers.downloads.documents/document/raw")
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    private fun getStorageDirectories(): List<String> {
        val paths = mutableListOf<String>()
        paths.add(Environment.getExternalStorageDirectory().toString()) // Add the default external storage path
        paths.addAll(getAdditionalStoragePaths())

        return paths
    }

    private fun getAdditionalStoragePaths(): List<String> {
        val storagePaths = mutableListOf<String>()
        val file = File("/storage")

        if (file.exists()) {
            file.listFiles()?.forEach { storagePaths.add(it.absolutePath) }
        }

        return storagePaths
    }
}
