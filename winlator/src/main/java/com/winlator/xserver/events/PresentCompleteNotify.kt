package com.winlator.xserver.events

import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.Window
import com.winlator.xserver.extensions.PresentExtension
import java.io.IOException

class PresentCompleteNotify(
    private val eventId: Int,
    private val window: Window,
    private val serial: Int,
    private val kind: PresentExtension.Kind,
    private val mode: PresentExtension.Mode,
    private val ust: Long,
    private val msc: Long,
) : Event(35) {

    companion object {
        val eventType: Short
            get() = 1

        val eventMask: Int
            get() = 1 shl eventType.toInt()
    }

    @Throws(IOException::class)
    override fun send(sequenceNumber: Short, outputStream: XOutputStream) {
        outputStream.lock().use {
            outputStream.writeByte(code)
            outputStream.writeByte(PresentExtension.MAJOR_OPCODE)
            outputStream.writeShort(sequenceNumber)
            outputStream.writeInt(2)
            outputStream.writeShort(eventType)
            outputStream.writeByte(kind.ordinal.toByte())
            outputStream.writeByte(mode.ordinal.toByte())
            outputStream.writeInt(eventId)
            outputStream.writeInt(window.id)
            outputStream.writeInt(serial)
            outputStream.writeLong(ust)
            outputStream.writeLong(msc)
        }
    }
}
