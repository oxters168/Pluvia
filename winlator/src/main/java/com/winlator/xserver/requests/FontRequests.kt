package com.winlator.xserver.requests

import com.winlator.xconnector.XInputStream
import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.XClient
import com.winlator.xserver.XClientRequestHandler
import com.winlator.xserver.errors.XRequestError
import java.io.IOException

object FontRequests {
    @Throws(XRequestError::class)
    fun openFont(client: XClient?, inputStream: XInputStream, outputStream: XOutputStream?) {
        inputStream.skip(4)

        val length = inputStream.readShort().toInt()

        inputStream.skip(2)

        val name = inputStream.readString8(length)

        if (name != "cursor") {
            throw UnsupportedOperationException("OpenFont supports only name: cursor.")
        }
    }

    @Throws(IOException::class, XRequestError::class)
    fun listFonts(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        inputStream.skip(2)

        val patternLength = inputStream.readShort()

        inputStream.readString8(patternLength.toInt())

        outputStream.lock().use {
            outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
            outputStream.writeByte(0.toByte())
            outputStream.writeShort(client.sequenceNumber)
            outputStream.writeInt(0)
            outputStream.writeShort(0.toShort())
            outputStream.writePad(22)
        }
    }
}
