package com.winlator.xserver.events

import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.Window
import java.io.IOException

class PropertyNotify(
    private val window: Window,
    private val atom: Int,
    private val deleted: Boolean,
) : Event(28) {

    private val timestamp = System.currentTimeMillis().toInt()

    @Throws(IOException::class)
    override fun send(sequenceNumber: Short, outputStream: XOutputStream) {
        outputStream.lock().use {
            outputStream.writeByte(code)
            outputStream.writeByte(0.toByte())
            outputStream.writeShort(sequenceNumber)
            outputStream.writeInt(window.id)
            outputStream.writeInt(atom)
            outputStream.writeInt(timestamp)
            outputStream.writeByte((if (deleted) 1 else 0).toByte())
            outputStream.writePad(15)
        }
    }
}
