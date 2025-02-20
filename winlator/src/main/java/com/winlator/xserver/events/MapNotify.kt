package com.winlator.xserver.events

import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.Window
import java.io.IOException

class MapNotify(private val event: Window, private val window: Window) : Event(19) {
    @Throws(IOException::class)
    override fun send(sequenceNumber: Short, outputStream: XOutputStream) {
        outputStream.lock().use {
            outputStream.writeByte(code)
            outputStream.writeByte(0.toByte())
            outputStream.writeShort(sequenceNumber)
            outputStream.writeInt(event.id)
            outputStream.writeInt(window.id)
            outputStream.writeByte((if (window.attributes.isOverrideRedirect) 1 else 0).toByte())
            outputStream.writePad(19)
        }
    }
}
