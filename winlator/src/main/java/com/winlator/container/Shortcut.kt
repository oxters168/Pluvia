package com.winlator.container

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.winlator.core.FileUtils
import com.winlator.core.StringUtils
import java.io.File
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

class Shortcut(val container: Container, val file: File) {

    val name: String
    val path: String
    val icon: Bitmap?
    val iconFile: File?
    val wmClass: String

    private val extraData = JSONObject()

    init {
        var execArgs = ""
        var icon: Bitmap? = null
        var iconFile: File? = null
        var wmClass = ""

        val iconDirs = arrayOf<File?>(
            container.getIconsDir(64),
            container.getIconsDir(48),
            container.getIconsDir(32),
            container.getIconsDir(16),
        )

        var section = ""

        var index: Int
        for (line in FileUtils.readLines(file)) {
            var line = line
            line = line.trim { it <= ' ' }
            if (line.isEmpty() || line.startsWith("#")) continue // Skip empty lines and comments

            if (line.startsWith("[")) {
                section = line.substring(1, line.indexOf("]"))
            } else {
                index = line.indexOf("=")

                if (index == -1) continue

                val key = line.substring(0, index)
                val value = line.substring(index + 1)

                if (section == "Desktop Entry") {
                    if (key == "Exec") {
                        execArgs = value
                    }

                    if (key == "Icon") {
                        for (iconDir in iconDirs) {
                            iconFile = File(iconDir, "$value.png")
                            if (iconFile.isFile()) {
                                icon = BitmapFactory.decodeFile(iconFile.path)
                                break
                            }
                        }
                    }
                    if (key == "StartupWMClass") wmClass = value
                } else if (section == "Extra Data") {
                    try {
                        extraData.put(key, value)
                    } catch (e: JSONException) {
                        Timber.w(e)
                    }
                }
            }
        }

        this.name = FileUtils.getBasename(file.path)
        this.icon = icon
        this.iconFile = iconFile
        this.path = StringUtils.unescape(execArgs.substring(execArgs.lastIndexOf("wine ") + 4))
        this.wmClass = wmClass

        Container.checkObsoleteOrMissingProperties(extraData)
    }

    fun getExtra(name: String): String? {
        return getExtra(name, "")
    }

    fun getExtra(name: String, fallback: String?): String? {
        return try {
            if (extraData.has(name)) extraData.getString(name) else fallback
        } catch (e: JSONException) {
            Timber.w(e)
            fallback
        }
    }

    fun putExtra(name: String, value: String?) {
        try {
            if (value != null) {
                extraData.put(name, value)
            } else {
                extraData.remove(name)
            }
        } catch (e: JSONException) {
            Timber.w(e)
        }
    }

    fun saveData() {
        var content = "[Desktop Entry]\n"
        for (line in FileUtils.readLines(file)) {
            if (line.contains("[Extra Data]")) {
                break
            }

            if (!line.contains("[Desktop Entry]") && !line.isEmpty()) {
                content += line + "\n"
            }
        }

        if (extraData.length() > 0) {
            content += "\n[Extra Data]\n"

            val keys = extraData.keys()

            while (keys.hasNext()) {
                val key = keys.next()

                try {
                    content += key + "=" + extraData.getString(key) + "\n"
                } catch (e: JSONException) {
                    Timber.w(e)
                }
            }
        }

        FileUtils.writeString(file, content)
    }
}
