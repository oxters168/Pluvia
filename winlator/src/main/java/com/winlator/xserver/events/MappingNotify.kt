package com.winlator.xserver.events

import com.winlator.xconnector.XOutputStream
import java.io.IOException

class MappingNotify(private val request: Request, private val firstKeycode: Byte, count: Int) : Event(34) {

    enum class Request {
        MODIFIER,
        KEYBOARD,
        POINTER,
    }

    private val count = count.toByte()

    @Throws(IOException::class)
    override fun send(sequenceNumber: Short, outputStream: XOutputStream) {
        outputStream.lock().use {
            outputStream.writeByte(code)
            outputStream.writeByte(0.toByte())
            outputStream.writeShort(sequenceNumber)
            outputStream.writeByte(request.ordinal.toByte())
            outputStream.writeByte(firstKeycode)
            outputStream.writeByte(count)
            outputStream.writePad(25)
        }
    }
}
