package com.winlator.core

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import timber.log.Timber

object ElfHelper {
    private const val ELF_CLASS_32: Byte = 1
    private const val ELF_CLASS_64: Byte = 2

    private fun getEIClass(binFile: File): Int {
        try {
            FileInputStream(binFile).use { inStream ->
                val header = ByteArray(52)

                inStream.read(header)

                if (header[0].toInt() == 0x7F &&
                    header[1] == 'E'.code.toByte() &&
                    header[2] == 'L'.code.toByte() &&
                    header[3] == 'F'.code.toByte()
                ) {
                    return header[4].toInt()
                }
            }
        } catch (e: IOException) {
            Timber.w(e)
        }
        return 0
    }

    fun is32Bit(binFile: File): Boolean = getEIClass(binFile) == ELF_CLASS_32.toInt()

    fun is64Bit(binFile: File): Boolean = getEIClass(binFile) == ELF_CLASS_64.toInt()
}
