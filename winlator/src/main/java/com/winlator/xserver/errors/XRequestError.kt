package com.winlator.xserver.errors

import com.winlator.xserver.XClient
import com.winlator.xserver.XClientRequestHandler
import java.io.IOException

open class XRequestError(code: Int, val data: Int) : Exception() {

    val code: Byte = code.toByte()

    @Throws(IOException::class)
    fun sendError(client: XClient, opcode: Byte) {
        val outputStream = client.outputStream

        outputStream.lock().use {
            outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_ERROR)
            outputStream.writeByte(code)
            outputStream.writeShort(client.sequenceNumber)
            outputStream.writeInt(data)
            outputStream.writeShort(client.requestData.toShort())
            outputStream.writeByte(opcode)
            outputStream.writePad(21)
        }
    }
}
