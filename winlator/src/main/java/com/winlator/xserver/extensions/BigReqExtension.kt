package com.winlator.xserver.extensions

import com.winlator.xconnector.XInputStream
import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.XClient
import com.winlator.xserver.XClientRequestHandler
import java.io.IOException

class BigReqExtension : Extension {

    companion object {
        const val MAJOR_OPCODE: Byte = -100
        private const val MAX_REQUEST_LENGTH: Int = 4194303
    }

    override val name: String
        get() = "BIG-REQUESTS"

    override val majorOpcode: Byte
        get() = MAJOR_OPCODE

    override val firstErrorId: Byte
        get() = 0

    override val firstEventId: Byte
        get() = 0

    @Throws(IOException::class)
    override fun handleRequest(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        outputStream.lock().use {
            outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
            outputStream.writeByte(0.toByte())
            outputStream.writeShort(client.sequenceNumber)
            outputStream.writeInt(0)
            outputStream.writeInt(MAX_REQUEST_LENGTH)
            outputStream.writePad(20)
        }
    }
}
