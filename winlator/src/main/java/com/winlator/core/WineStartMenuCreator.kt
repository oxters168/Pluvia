package com.winlator.core

import android.content.Context
import com.winlator.container.Container
import com.winlator.core.FileUtils.isEmpty
import com.winlator.core.FileUtils.readString
import com.winlator.core.FileUtils.writeString
import com.winlator.core.MSLink.createFile
import java.io.File
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

object WineStartMenuCreator {

    private fun parseShowCommand(value: String): Int = when (value) {
        "SW_SHOWMAXIMIZED" -> MSLink.SW_SHOWMAXIMIZED.toInt()
        "SW_SHOWMINNOACTIVE" -> MSLink.SW_SHOWMINNOACTIVE.toInt()
        else -> MSLink.SW_SHOWNORMAL.toInt()
    }

    @Throws(JSONException::class)
    private fun createMenuEntry(item: JSONObject, currentDir: File) {
        var currentDir = currentDir

        if (item.has("children")) {
            currentDir = File(currentDir, item.getString("name"))
            currentDir.mkdirs()

            val children = item.getJSONArray("children")
            for (i in 0..<children.length()) {
                createMenuEntry(children.getJSONObject(i), currentDir)
            }
        } else {
            val outputFile = File(currentDir, item.getString("name") + ".lnk")
            val options = MSLink.Options()
            options.targetPath = item.getString("path")
            options.cmdArgs = item.optString("cmdArgs")
            options.iconLocation = item.optString("iconLocation", options.targetPath)
            options.iconIndex = item.optInt("iconIndex", 0)

            if (item.has("showCommand")) {
                options.showCommand = parseShowCommand(item.getString("showCommand"))
            }

            createFile(options, outputFile)
        }
    }

    @Throws(JSONException::class)
    private fun removeMenuEntry(item: JSONObject, currentDir: File) {
        var currentDir = currentDir

        if (item.has("children")) {
            currentDir = File(currentDir, item.getString("name"))

            val children = item.getJSONArray("children")

            for (i in 0..<children.length()) {
                removeMenuEntry(children.getJSONObject(i), currentDir)
            }

            if (isEmpty(currentDir)) {
                currentDir.delete()
            }
        } else {
            (File(currentDir, item.getString("name") + ".lnk")).delete()
        }
    }

    @Throws(JSONException::class)
    private fun removeOldMenu(containerStartMenuFile: File, startMenuDir: File) {
        if (!containerStartMenuFile.isFile) {
            return
        }

        val data = JSONArray(readString(containerStartMenuFile))

        for (i in 0..<data.length()) {
            removeMenuEntry(data.getJSONObject(i), startMenuDir)
        }
    }

    fun create(context: Context, container: Container) {
        try {
            val startMenuDir = container.startMenuDir
            val containerStartMenuFile = File(container.rootDir, ".startmenu")

            removeOldMenu(containerStartMenuFile, startMenuDir)

            val data = JSONArray(readString(context, "wine_startmenu.json"))

            writeString(containerStartMenuFile, data.toString())

            for (i in 0..<data.length()) {
                createMenuEntry(data.getJSONObject(i), startMenuDir)
            }
        } catch (e: JSONException) {
            Timber.w(e)
        }
    }
}
