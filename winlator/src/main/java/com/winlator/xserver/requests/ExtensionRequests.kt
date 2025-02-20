package com.winlator.xserver.requests

import com.winlator.xconnector.XInputStream
import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.XClient
import com.winlator.xserver.XClientRequestHandler
import com.winlator.xserver.errors.XRequestError
import java.io.IOException

object ExtensionRequests {
    @Throws(IOException::class, XRequestError::class)
    fun queryExtension(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        val length = inputStream.readShort()

        inputStream.skip(2)

        val name = inputStream.readString8(length.toInt())
        val extension = client.xServer.getExtensionByName(name)

        outputStream.lock().use {
            outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
            outputStream.writeByte(0.toByte())
            outputStream.writeShort(client.sequenceNumber)
            outputStream.writeInt(0)
            if (extension != null) {
                outputStream.writeByte(1.toByte())
                outputStream.writeByte(extension.majorOpcode)
                outputStream.writeByte(extension.firstEventId)
                outputStream.writeByte(extension.firstErrorId)
                outputStream.writePad(20)
            } else {
                outputStream.writeByte(0.toByte())
                outputStream.writePad(23)
            }
        }
    }
}
