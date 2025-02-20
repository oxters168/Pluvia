package com.winlator.xserver.events

import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.Window
import java.io.IOException

class Expose(private val window: Window) : Event(12) {

    private val width = window.width
    private val height = window.height
    private val x: Short = 0
    private val y: Short = 0

    @Throws(IOException::class)
    override fun send(sequenceNumber: Short, outputStream: XOutputStream) {
        outputStream.lock().use {
            outputStream.writeByte(code)
            outputStream.writeByte(0.toByte())
            outputStream.writeShort(sequenceNumber)
            outputStream.writeInt(window.id)
            outputStream.writeShort(x)
            outputStream.writeShort(y)
            outputStream.writeShort(width)
            outputStream.writeShort(height)
            outputStream.writeShort(0.toShort())
            outputStream.writePad(14)
        }
    }
}
