package com.winlator.xserver.events

import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.Window
import java.io.IOException

class SelectionClear(
    private val timestamp: Int,
    private val owner: Window,
    private val selection: Int,
) : Event(29) {
    @Throws(IOException::class)
    override fun send(sequenceNumber: Short, outputStream: XOutputStream) {
        outputStream.lock().use {
            outputStream.writeByte(code)
            outputStream.writeByte(0.toByte())
            outputStream.writeShort(sequenceNumber)
            outputStream.writeInt(timestamp)
            outputStream.writeInt(owner.id)
            outputStream.writeInt(selection)
            outputStream.writePad(16)
        }
    }
}
