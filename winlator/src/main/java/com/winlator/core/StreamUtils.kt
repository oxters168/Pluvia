package com.winlator.core

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object StreamUtils {

    const val BUFFER_SIZE: Int = 64 * 1024

    fun copyToByteArray(inStream: InputStream?): ByteArray {
        if (inStream == null) {
            return ByteArray(0)
        }

        val outStream = ByteArrayOutputStream(BUFFER_SIZE)
        copy(inStream, outStream)

        return outStream.toByteArray()
    }

    fun copy(inStream: InputStream, outStream: OutputStream): Boolean {
        try {
            val buffer = ByteArray(BUFFER_SIZE)
            var amountRead: Int

            while ((inStream.read(buffer).also { amountRead = it }) != -1) {
                outStream.write(buffer, 0, amountRead)
            }

            outStream.flush()

            return true
        } catch (e: IOException) {
            return false
        }
    }
}
