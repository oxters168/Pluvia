package com.winlator.xserver.events

import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.Window
import java.io.IOException

class ConfigureNotify(
    private val event: Window,
    private val window: Window,
    private val aboveSibling: Window?,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    borderWidth: Int,
    private val overrideRedirect: Boolean,
) : Event(22) {
    private val x = x.toShort()
    private val y = y.toShort()
    private val height = height.toShort()

    private val width = width.toShort()
    private val borderWidth = borderWidth.toShort()

    @Throws(IOException::class)
    override fun send(sequenceNumber: Short, outputStream: XOutputStream) {
        outputStream.lock().use {
            outputStream.writeByte(code)
            outputStream.writeByte(0.toByte())
            outputStream.writeShort(sequenceNumber)
            outputStream.writeInt(event.id)
            outputStream.writeInt(window.id)
            outputStream.writeInt(aboveSibling?.id ?: 0)
            outputStream.writeShort(x)
            outputStream.writeShort(y)
            outputStream.writeShort(width)
            outputStream.writeShort(height)
            outputStream.writeShort(borderWidth)
            outputStream.writeByte((if (overrideRedirect) 1 else 0).toByte())
            outputStream.writePad(5)
        }
    }
}
