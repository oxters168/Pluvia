package com.winlator.xserver.events

import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.Window
import java.io.IOException

class CreateNotify(private val parent: Window, private val window: Window) : Event(16) {
    @Throws(IOException::class)
    override fun send(sequenceNumber: Short, outputStream: XOutputStream) {
        outputStream.lock().use {
            outputStream.writeByte(code)
            outputStream.writeByte(0.toByte())
            outputStream.writeShort(sequenceNumber)
            outputStream.writeInt(parent.id)
            outputStream.writeInt(window.id)
            outputStream.writeShort(window.x)
            outputStream.writeShort(window.y)
            outputStream.writeShort(window.width)
            outputStream.writeShort(window.height)
            outputStream.writeShort(window.borderWidth)
            outputStream.writeByte((if (window.attributes.isOverrideRedirect) 1 else 0).toByte())
            outputStream.writePad(9)
        }
    }
}
