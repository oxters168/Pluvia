package com.winlator.xserver.events

import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.Bitmask
import com.winlator.xserver.Window
import com.winlator.xserver.Window.StackMode
import java.io.IOException

class ConfigureRequest(
    private val parent: Window,
    private val window: Window,
    private val sibling: Window?,
    private val x: Short,
    private val y: Short,
    private val width: Short,
    private val height: Short,
    private val borderWidth: Short,
    stackMode: StackMode?,
    private val valueMask: Bitmask,
) : Event(23) {
    private val stackMode = stackMode ?: StackMode.ABOVE

    @Throws(IOException::class)
    override fun send(sequenceNumber: Short, outputStream: XOutputStream) {
        outputStream.lock().use {
            outputStream.writeByte(code)
            outputStream.writeByte(stackMode.ordinal.toByte())
            outputStream.writeShort(sequenceNumber)
            outputStream.writeInt(parent.id)
            outputStream.writeInt(window.id)
            outputStream.writeInt(sibling?.id ?: 0)
            outputStream.writeShort(x)
            outputStream.writeShort(y)
            outputStream.writeShort(width)
            outputStream.writeShort(height)
            outputStream.writeShort(borderWidth)
            outputStream.writeShort(valueMask.bits.toShort())
            outputStream.writePad(4)
        }
    }
}
