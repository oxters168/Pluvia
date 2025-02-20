package com.winlator.xserver.events

import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.Window
import java.io.IOException

class ResizeRequest(
    private val window: Window,
    private val width: Short,
    private val height: Short,
) : Event(25) {
    @Throws(IOException::class)
    override fun send(sequenceNumber: Short, outputStream: XOutputStream) {
        outputStream.lock().use {
            outputStream.writeByte(code)
            outputStream.writeByte(0.toByte())
            outputStream.writeShort(sequenceNumber)
            outputStream.writeInt(window.id)
            outputStream.writeShort(width)
            outputStream.writeShort(height)
            outputStream.writePad(20)
        }
    }
}
