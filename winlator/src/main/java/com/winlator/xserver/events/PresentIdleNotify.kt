package com.winlator.xserver.events

import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.Pixmap
import com.winlator.xserver.Window
import com.winlator.xserver.extensions.PresentExtension
import java.io.IOException

class PresentIdleNotify(
    private val eventId: Int,
    private val window: Window,
    private val pixmap: Pixmap,
    private val serial: Int,
    private val idleFence: Int,
) : Event(35) {

    companion object {
        val eventType: Short
            get() = 2

        val eventMask: Int
            get() = 1 shl eventType.toInt()
    }

    @Throws(IOException::class)
    override fun send(sequenceNumber: Short, outputStream: XOutputStream) {
        outputStream.lock().use {
            outputStream.writeByte(code)
            outputStream.writeByte(PresentExtension.MAJOR_OPCODE)
            outputStream.writeShort(sequenceNumber)
            outputStream.writeInt(0)
            outputStream.writeShort(eventType)
            outputStream.writeShort(0.toShort())
            outputStream.writeInt(eventId)
            outputStream.writeInt(window.id)
            outputStream.writeInt(serial)
            outputStream.writeInt(pixmap.id)
            outputStream.writeInt(idleFence)
        }
    }
}
