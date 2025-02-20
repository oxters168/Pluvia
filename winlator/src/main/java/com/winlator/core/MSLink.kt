package com.winlator.core

import com.winlator.core.ArrayUtils.concat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

object MSLink {

    const val SW_SHOWNORMAL: Byte = 1
    const val SW_SHOWMAXIMIZED: Byte = 3
    const val SW_SHOWMINNOACTIVE: Byte = 7

    private const val HAS_LINK_TARGET_ID_LIST = 1 shl 0
    private const val HAS_ARGUMENTS = 1 shl 5
    private const val HAS_ICON_LOCATION = 1 shl 6
    private const val FORCE_NO_LINK_INFO = 1 shl 8

    private fun charToHexDigit(chr: Char): Int {
        return if (chr >= 'A') chr.code - 'A'.code + 10 else chr.code - '0'.code
    }

    private fun twoCharsToByte(chr1: Char, chr2: Char): Byte {
        return (charToHexDigit(chr1.uppercaseChar()) * 16 + charToHexDigit(chr2.uppercaseChar())).toByte()
    }

    private fun convertCLSIDtoDATA(str: String): ByteArray {
        return byteArrayOf(
            twoCharsToByte(str[6], str[7]),
            twoCharsToByte(str[4], str[5]),
            twoCharsToByte(str[2], str[3]),
            twoCharsToByte(str[0], str[1]),
            twoCharsToByte(str[11], str[12]),
            twoCharsToByte(str[9], str[10]),
            twoCharsToByte(str[16], str[17]),
            twoCharsToByte(str[14], str[15]),
            twoCharsToByte(str[19], str[20]),
            twoCharsToByte(str[21], str[22]),
            twoCharsToByte(str[24], str[25]),
            twoCharsToByte(str[26], str[27]),
            twoCharsToByte(str[28], str[29]),
            twoCharsToByte(str[30], str[31]),
            twoCharsToByte(str[32], str[33]),
            twoCharsToByte(str[34], str[35]),
        )
    }

    private fun stringToByteArray(str: String): ByteArray {
        val bytes = ByteArray(str.length)

        for (i in bytes.indices) {
            bytes[i] = str[i].code.toByte()
        }

        return bytes
    }

    private fun intToByteArray(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }

    private fun stringSizePaddedToByteArray(str: String): ByteArray {
        val buffer = ByteBuffer.allocate(str.length + 2).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(str.length.toShort())

        for (element in str) {
            buffer.put(element.code.toByte())
        }

        return buffer.array()
    }

    private fun generateIDLIST(bytes: ByteArray): ByteArray {
        val buffer = ByteBuffer
            .allocate(2)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putShort((bytes.size + 2).toShort())

        return concat(buffer.array(), bytes)
    }

    fun createFile(targetPath: String?, outputFile: File?) {
        val options = Options()
        options.targetPath = targetPath
        createFile(options, outputFile)
    }

    fun createFile(options: Options, outputFile: File?) {
        val headerSize = byteArrayOf(0x4c, 0x00, 0x00, 0x00)
        val linkCLSID = convertCLSIDtoDATA("00021401-0000-0000-c000-000000000046")

        var linkFlags = HAS_LINK_TARGET_ID_LIST or FORCE_NO_LINK_INFO

        if (options.cmdArgs != null && options.cmdArgs!!.isNotEmpty()) {
            linkFlags =
                linkFlags or HAS_ARGUMENTS
        }

        if (options.iconLocation != null && options.iconLocation!!.isNotEmpty()) {
            linkFlags = linkFlags or HAS_ICON_LOCATION
        }

        val linkFlagsByteArray = intToByteArray(linkFlags)

        val fileAttributes: ByteArray
        val prefixOfTarget: ByteArray

        options.targetPath = options.targetPath!!.replace("/+".toRegex(), "\\\\")

        if (options.targetPath!!.endsWith("\\")) {
            fileAttributes = byteArrayOf(0x10, 0x00, 0x00, 0x00)
            prefixOfTarget = byteArrayOf(0x31, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
            options.targetPath = options.targetPath!!.replace("\\\\+$".toRegex(), "")
        } else {
            fileAttributes = byteArrayOf(0x20, 0x00, 0x00, 0x00)
            prefixOfTarget = byteArrayOf(0x32, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        }

        val creationTime: ByteArray
        val accessTime: ByteArray
        val writeTime = byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)

        accessTime = writeTime
        creationTime = accessTime

        val fileSize = intToByteArray(options.fileSize)
        val iconIndex = intToByteArray(options.iconIndex)
        val showCommand = intToByteArray(options.showCommand)
        val hotkey = byteArrayOf(0x00, 0x00)
        val reserved1 = byteArrayOf(0x00, 0x00)
        val reserved2 = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        val reserved3 = byteArrayOf(0x00, 0x00, 0x00, 0x00)

        val clsidComputer = convertCLSIDtoDATA("20d04fe0-3aea-1069-a2d8-08002b30309d")
        val clsidNetwork = convertCLSIDtoDATA("208d2c60-3aea-1069-a2d7-08002b30309d")

        val itemData: ByteArray
        val prefixRoot: ByteArray
        var targetRoot: ByteArray
        val targetLeaf: ByteArray?
        if (options.targetPath!!.startsWith("\\")) {
            prefixRoot = byteArrayOf(0xc3.toByte(), 0x01, 0x81.toByte())

            targetRoot = stringToByteArray(options.targetPath!!)

            targetLeaf = if (!options.targetPath!!.endsWith("\\")) {
                stringToByteArray(options.targetPath!!.substring(options.targetPath!!.lastIndexOf("\\") + 1))
            } else {
                null
            }

            itemData = concat(byteArrayOf(0x1f, 0x58), clsidNetwork)
        } else {
            prefixRoot = byteArrayOf(0x2f)

            val index = options.targetPath!!.indexOf("\\")

            targetRoot = stringToByteArray(options.targetPath!!.substring(0, index + 1))
            targetLeaf = stringToByteArray(options.targetPath!!.substring(index + 1))

            itemData = concat(byteArrayOf(0x1f, 0x50), clsidComputer)
        }

        targetRoot = concat(targetRoot, ByteArray(21))

        val endOfString = byteArrayOf(0x00)
        var idListItems = concat(
            generateIDLIST(itemData),
            generateIDLIST(concat(prefixRoot, targetRoot, endOfString)),
        )

        if (targetLeaf != null) {
            idListItems = concat(idListItems, generateIDLIST(concat(prefixOfTarget, targetLeaf, endOfString)))
        }

        val idList = generateIDLIST(idListItems)

        val terminalID = byteArrayOf(0x00, 0x00)

        var stringData = ByteArray(0)

        if ((linkFlags and HAS_ARGUMENTS) != 0) {
            stringData = concat(stringData, stringSizePaddedToByteArray(options.cmdArgs!!))
        }

        if ((linkFlags and HAS_ICON_LOCATION) != 0) {
            stringData = concat(stringData, stringSizePaddedToByteArray(options.iconLocation!!))
        }

        try {
            FileOutputStream(outputFile).use { os ->
                os.write(headerSize)
                os.write(linkCLSID)
                os.write(linkFlagsByteArray)
                os.write(fileAttributes)
                os.write(creationTime)
                os.write(accessTime)
                os.write(writeTime)
                os.write(fileSize)
                os.write(iconIndex)
                os.write(showCommand)
                os.write(hotkey)
                os.write(reserved1)
                os.write(reserved2)
                os.write(reserved3)
                os.write(idList)
                os.write(terminalID)

                if (stringData.isNotEmpty()) {
                    os.write(stringData)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    class Options {
        var targetPath: String? = null
        var cmdArgs: String? = null
        var iconLocation: String? = null
        var iconIndex: Int = 0
        var fileSize: Int = 0
        var showCommand: Int = SW_SHOWNORMAL.toInt()
    }
}
