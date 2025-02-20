package com.winlator.core

import android.content.Context
import java.nio.charset.Charset
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

object StringUtils {
    fun removeEndSlash(value: String): String {
        var value = value

        while (value.endsWith("/") || value.endsWith("\\")) {
            value = value.substring(0, value.length - 1)
        }

        return value
    }

    fun addEndSlash(value: String): String {
        return if (value.endsWith("/")) value else "$value/"
    }

    fun insert(text: String, index: Int, value: String): String {
        return text.substring(0, index) + value + text.substring(index)
    }

    fun replace(text: String, start: Int, end: Int, value: String): String {
        return text.substring(0, start) + value + text.substring(end)
    }

    fun unescape(path: String): String {
        return path.replace(
            "\\\\([^\\\\]+)"
                .toRegex(),
            "$1",
        )
            .replace("\\\\([^\\\\]+)".toRegex(), "$1")
            .replace("\\\\\\\\".toRegex(), "\\\\")
            .trim { it <= ' ' }
    }

    fun parseIdentifier(text: Any): String {
        return text.toString()
            .lowercase()
            .replace(" *\\(([^\\)]+)\\)$".toRegex(), "")
            .replace("( \\+ )+| +".toRegex(), "-")
    }

    fun parseNumber(text: Any): String {
        return text.toString().replace("[^0-9\\.]+".toRegex(), "")
    }

    fun getString(context: Context, resName: String): String? {
        var resName = resName

        try {
            resName = resName.lowercase()
            val resID = context.resources.getIdentifier(resName, "string", context.packageName)

            return context.getString(resID)
        } catch (e: Exception) {
            return null
        }
    }

    @JvmOverloads
    fun formatBytes(bytes: Long, withSuffix: Boolean = true): String {
        if (bytes <= 0) {
            return "0 bytes"
        }

        val units = arrayOf("bytes", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()

        val suffix = if (withSuffix) {
            " " + units[digitGroups]
        } else {
            ""
        }

        return String.format(Locale.ENGLISH, "%.2f", bytes / 1024.0.pow(digitGroups.toDouble())) + suffix
    }

    @JvmStatic
    @JvmOverloads
    fun fromANSIString(bytes: ByteArray?, charset: Charset? = null): String {
        val value = if (charset != null) {
            String(bytes!!, charset)
        } else {
            String(bytes!!)
        }

        val indexOfNull = value.indexOf('\u0000')

        return if (indexOfNull != -1) {
            value.substring(0, indexOfNull)
        } else {
            value
        }
    }
}
