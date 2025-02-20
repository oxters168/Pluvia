package com.winlator.xserver.events

import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.Window
import java.io.IOException

class DestroyNotify(private val event: Window, private val window: Window) : Event(17) {
    @Throws(IOException::class)
    override fun send(sequenceNumber: Short, outputStream: XOutputStream) {
        outputStream.lock().use {
            outputStream.writeByte(code)
            outputStream.writeByte(0.toByte())
            outputStream.writeShort(sequenceNumber)
            outputStream.writeInt(event.id)
            outputStream.writeInt(window.id)
            outputStream.writePad(20)
        }
    }
}
