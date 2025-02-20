package com.winlator.core

import com.winlator.core.FileUtils.copy
import com.winlator.core.FileUtils.createTempFile
import com.winlator.core.FileUtils.getBasename
import com.winlator.math.Mathf
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import timber.log.Timber

class WineRegistryEditor(private val file: File) : Closeable {

    private val cloneFile = createTempFile(file.parentFile, getBasename(file.path))
    private var modified = false
    private var createKeyIfNotExist = true
    private var lastParentKeyPosition = 0
    private var lastParentKey = ""

    class Location(val offset: Int, val start: Int, val end: Int) {
        fun length(): Int = end - start
    }

    init {
        if (!file.isFile) {
            try {
                cloneFile.createNewFile()
            } catch (e: IOException) {
                Timber.w(e)
            }
        } else {
            copy(file, cloneFile)
        }
    }

    override fun close() {
        if (modified && cloneFile.exists()) {
            cloneFile.renameTo(file)
        } else {
            cloneFile.delete()
        }
    }

    private fun resetLastParentKeyPositionIfNeed(newKey: String) {
        val lastIndex = newKey.lastIndexOf("\\")

        if (lastIndex == -1) {
            lastParentKeyPosition = 0
            lastParentKey = ""
            return
        }

        val parentKey = newKey.substring(0, lastIndex)

        if (parentKey != lastParentKey) {
            lastParentKeyPosition = 0
        }

        lastParentKey = parentKey
    }

    fun setCreateKeyIfNotExist(createKeyIfNotExist: Boolean) {
        this.createKeyIfNotExist = createKeyIfNotExist
    }

    private fun createKey(key: String): Location? {
        lastParentKeyPosition = 0

        val location = getParentKeyLocation(key)
        var success = false
        var offset = 0
        var totalLength = 0

        val buffer = CharArray(StreamUtils.BUFFER_SIZE)
        val tempFile = createTempFile(file.parentFile, getBasename(file.path))

        try {
            BufferedReader(FileReader(cloneFile), StreamUtils.BUFFER_SIZE).use { reader ->
                BufferedWriter(FileWriter(tempFile), StreamUtils.BUFFER_SIZE).use { writer ->
                    var length: Int
                    var i = 0
                    val end = if (location != null) location.end + 1 else cloneFile.length().toInt()
                    while (i < end) {
                        length = min(buffer.size.toDouble(), (end - i).toDouble()).toInt()
                        reader.read(buffer, 0, length)
                        writer.write(buffer, 0, length)
                        totalLength += length
                        i += length
                    }

                    offset = totalLength

                    val ticks1601To1970 = 86400L * (369 * 365 + 89) * 10000000
                    val currentTime = System.currentTimeMillis() + ticks1601To1970
                    val content = "\n[${escape(key)}] ${(currentTime - ticks1601To1970) / 1000}" +
                        String.format(Locale.ENGLISH, "\n#time=%x%08x", currentTime shr 32, currentTime.toInt()) + "\n"

                    writer.write(content)
                    totalLength += content.length - 1

                    while ((reader.read(buffer).also { length = it }) != -1) {
                        writer.write(buffer, 0, length)
                    }

                    success = true
                }
            }
        } catch (e: IOException) {
            Timber.w(e)
        }

        if (success) {
            modified = true
            tempFile.renameTo(cloneFile)

            return Location(offset, totalLength, totalLength)
        } else {
            tempFile.delete()

            return null
        }
    }

    fun getStringValue(key: String, name: String?): String = getStringValue(key, name, null)

    fun getStringValue(key: String, name: String?, fallback: String?): String {
        val value = getRawValue(key, name)
        return value?.substring(1, value.length - 1) ?: fallback!!
    }

    fun setStringValue(key: String, name: String?, value: String?) {
        setRawValue(key, name, if (value != null) "\"" + escape(value) + "\"" else "\"\"")
    }

    fun getDwordValue(key: String, name: String?): Int = getDwordValue(key, name, null)

    fun getDwordValue(key: String, name: String?, fallback: Int?): Int {
        val value = getRawValue(key, name)
        return if (value != null) Integer.decode("0x" + value.substring(6)) else fallback!!
    }

    fun setDwordValue(key: String, name: String?, value: Int) {
        setRawValue(key, name, "dword:" + String.format("%08x", value))
    }

    fun setHexValue(key: String, name: String, value: String) {
        val start = Mathf.roundTo(name.length.toFloat(), 2f).toInt() + 7
        val lines = StringBuilder()
        var i = 0
        var j = start
        while (i < value.length) {
            if (i > 0 && (i % 2) == 0) {
                lines.append(",")
            }

            if (j++ > 56) {
                lines.append("\\\n  ")
                j = 8
            }

            lines.append(value[i])

            i++
        }

        setRawValue(key, name, "hex:$lines")
    }

    fun setHexValue(key: String, name: String, bytes: ByteArray) {
        val data = StringBuilder()

        for (b in bytes) {
            data.append(String.format(Locale.ENGLISH, "%02x", java.lang.Byte.toUnsignedInt(b)))
        }

        setHexValue(key, name, data.toString())
    }

    private fun getRawValue(key: String, name: String?): String? {
        lastParentKeyPosition = 0

        val keyLocation = getKeyLocation(key) ?: return null

        val valueLocation = getValueLocation(keyLocation, name) ?: return null
        var success = false
        val buffer = CharArray(valueLocation.length())

        try {
            BufferedReader(FileReader(cloneFile), StreamUtils.BUFFER_SIZE).use { reader ->
                reader.skip(valueLocation.start.toLong())
                success = reader.read(buffer) == buffer.size
            }
        } catch (e: IOException) {
            Timber.w(e)
        }

        return if (success) {
            unescape(String(buffer))
        } else {
            null
        }
    }

    private fun setRawValue(key: String, name: String?, value: String) {
        resetLastParentKeyPositionIfNeed(key)

        var keyLocation = getKeyLocation(key)
        if (keyLocation == null) {
            if (createKeyIfNotExist) {
                keyLocation = createKey(key)
            } else {
                return
            }
        }

        val valueLocation = getValueLocation(keyLocation!!, name)
        val buffer = CharArray(StreamUtils.BUFFER_SIZE)
        var success = false

        val tempFile = createTempFile(file.parentFile, getBasename(file.path))

        try {
            BufferedReader(FileReader(cloneFile), StreamUtils.BUFFER_SIZE).use { reader ->
                BufferedWriter(FileWriter(tempFile), StreamUtils.BUFFER_SIZE).use { writer ->
                    var length: Int
                    var i = 0
                    val end = valueLocation?.start ?: keyLocation.end

                    while (i < end) {
                        length = min(buffer.size.toDouble(), (end - i).toDouble()).toInt()

                        reader.read(buffer, 0, length)
                        writer.write(buffer, 0, length)

                        i += length
                    }

                    if (valueLocation == null) {
                        writer.write("\n${if (name != null) "\"${escape(name)}\"" else "@"}=$value")
                    } else {
                        writer.write(value)
                        reader.skip(valueLocation.length().toLong())
                    }

                    while ((reader.read(buffer).also { length = it }) != -1) {
                        writer.write(buffer, 0, length)
                    }

                    success = true
                }
            }
        } catch (e: IOException) {
            Timber.w(e)
        }

        if (success) {
            modified = true
            tempFile.renameTo(cloneFile)
        } else {
            tempFile.delete()
        }
    }

    fun removeValue(key: String, name: String?) {
        lastParentKeyPosition = 0
        val keyLocation = getKeyLocation(key) ?: return

        val valueLocation = getValueLocation(keyLocation, name) ?: return
        removeRegion(valueLocation)
    }

    @JvmOverloads
    fun removeKey(key: String, removeTree: Boolean = false): Boolean {
        lastParentKeyPosition = 0

        var removed = false

        if (removeTree) {
            var location: Location?

            while ((getKeyLocation(key, true).also { location = it }) != null) {
                if (removeRegion(location!!)) {
                    removed = true
                }
            }
        } else {
            val location = getKeyLocation(key, false)

            if (location != null && removeRegion(location)) {
                removed = true
            }
        }

        return removed
    }

    private fun removeRegion(location: Location): Boolean {
        val buffer = CharArray(StreamUtils.BUFFER_SIZE)
        var success = false

        val tempFile = createTempFile(file.parentFile, getBasename(file.path))

        try {
            BufferedReader(FileReader(cloneFile), StreamUtils.BUFFER_SIZE).use { reader ->
                BufferedWriter(FileWriter(tempFile), StreamUtils.BUFFER_SIZE).use { writer ->
                    var length = 0
                    var i = 0

                    while (i < location.offset) {
                        length = min(buffer.size.toDouble(), (location.offset - i).toDouble()).toInt()

                        reader.read(buffer, 0, length)
                        writer.write(buffer, 0, length)

                        i += length
                    }

                    val skipLine = length > 1 && buffer[length - 1] == '\n'
                    reader.skip((location.end - location.offset + (if (skipLine) 1 else 0)).toLong())

                    while ((reader.read(buffer).also { length = it }) != -1) {
                        writer.write(buffer, 0, length)
                    }

                    success = true
                }
            }
        } catch (e: IOException) {
            Timber.w(e)
        }

        if (success) {
            modified = true
            tempFile.renameTo(cloneFile)
        } else {
            tempFile.delete()
        }

        return success
    }

    private fun getKeyLocation(key: String): Location? = getKeyLocation(key, false)

    private fun getKeyLocation(key: String, keyAsPrefix: Boolean): Location? {
        var key = key
        try {
            BufferedReader(FileReader(cloneFile), StreamUtils.BUFFER_SIZE).use { reader ->
                val lastIndex = key.lastIndexOf("\\")
                var parentKey = if (lastParentKeyPosition == 0 && lastIndex != -1) {
                    "[" + escape(key.substring(0, lastIndex))
                } else {
                    null
                }

                if (lastParentKeyPosition > 0) {
                    reader.skip(lastParentKeyPosition.toLong())
                }

                key = "[" + escape(key) + (if (!keyAsPrefix) "]" else "")

                var totalLength = lastParentKeyPosition
                var start = -1
                var end = -1
                var emptyLines = 0
                var offset = 0

                var line: String
                while ((reader.readLine().also { line = it }) != null) {
                    if (start == -1) {
                        if (parentKey != null && line.startsWith(parentKey)) {
                            lastParentKeyPosition = totalLength
                            parentKey = null
                        }

                        if (parentKey == null && line.startsWith(key)) {
                            offset = totalLength - 1
                            start = totalLength + line.length + 1
                        }
                    } else {
                        if (line.startsWith("[")) {
                            end = max(-1.0, (totalLength - emptyLines - 1).toDouble()).toInt()
                            break
                        } else {
                            emptyLines = if (line.isEmpty()) {
                                emptyLines + 1
                            } else {
                                0
                            }
                        }
                    }

                    totalLength += line.length + 1
                }

                if (end == -1) {
                    end = totalLength - 1
                }

                return if (start != -1) {
                    Location(offset, start, end)
                } else {
                    null
                }
            }
        } catch (e: IOException) {
            return null
        }
    }

    private fun getParentKeyLocation(key: String): Location? {
        val parts = key.split("\\\\".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val stack = ArrayList(listOf(*parts).subList(0, parts.size - 1))

        while (stack.isNotEmpty()) {
            val currentKey = stack.joinToString("\\")
            val location = getKeyLocation(currentKey, true)

            if (location != null) {
                return location
            }

            stack.removeAt(stack.size - 1)
        }

        return null
    }

    private fun getValueLocation(keyLocation: Location, name: String?): Location? {
        var name = name

        if (keyLocation.start == keyLocation.end) {
            return null
        }

        try {
            BufferedReader(FileReader(cloneFile), StreamUtils.BUFFER_SIZE).use { reader ->
                reader.skip(keyLocation.start.toLong())
                name = if (name != null) "\"" + escape(name!!) + "\"=" else "@="

                var totalLength = 0
                var start = -1
                var end = -1
                var offset = 0

                var line: String
                while ((reader.readLine().also { line = it }) != null && totalLength < keyLocation.length()) {
                    if (start == -1) {
                        if (line.startsWith(name!!)) {
                            offset = totalLength - 1
                            start = totalLength + name!!.length
                        }
                    } else {
                        if (line.isEmpty() || lineHasName(line)) {
                            end = totalLength - 1
                            break
                        }
                    }

                    totalLength += line.length + 1
                }

                if (end == -1) {
                    end = totalLength - 1
                }

                return if (start != -1) {
                    Location(
                        keyLocation.start + offset,
                        keyLocation.start + start,
                        keyLocation.start + end,
                    )
                } else {
                    null
                }
            }
        } catch (e: IOException) {
            return null
        }
    }

    companion object {
        private fun escape(str: String): String = str.replace("\\", "\\\\").replace("\"", "\\\"")

        private fun unescape(str: String): String = str.replace("\\\"", "\"").replace("\\\\", "\\")

        private fun lineHasName(line: String): Boolean {
            var index: Int
            return (line.indexOf('"').also { index = it }) != -1 &&
                (line.indexOf('"', index).also { index = it }) !=
                -1 &&
                (line.indexOf('=', index).also { index = it }) != -1
        }
    }
}
