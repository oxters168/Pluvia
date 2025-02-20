package com.winlator.xserver.requests

import com.winlator.xconnector.XInputStream
import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.Keyboard
import com.winlator.xserver.XClient
import com.winlator.xserver.XClientRequestHandler
import com.winlator.xserver.errors.XRequestError
import java.io.IOException

object KeyboardRequests {
    @Throws(IOException::class, XRequestError::class)
    fun getKeyboardMapping(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        val firstKeycode = inputStream.readByte()
        var count = inputStream.readUnsignedByte()

        inputStream.skip(2)

        outputStream.lock().use {
            outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
            outputStream.writeByte(Keyboard.KEYSYMS_PER_KEYCODE)
            outputStream.writeShort(client.sequenceNumber)
            outputStream.writeInt(count)
            outputStream.writePad(24)

            var i = firstKeycode - Keyboard.MIN_KEYCODE
            while (count != 0) {
                outputStream.writeInt(client.xServer.keyboard.keysyms[i])
                count--
                i++
            }
        }
    }

    @Throws(IOException::class, XRequestError::class)
    fun getModifierMapping(
        client: XClient,
        inputStream: XInputStream?,
        outputStream: XOutputStream,
    ) {
        outputStream.lock().use { lock ->
            outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
            outputStream.writeByte(1.toByte())
            outputStream.writeShort(client.sequenceNumber)
            outputStream.writeInt(2)
            outputStream.writePad(24)
            outputStream.writePad(8)
        }
    }
}
