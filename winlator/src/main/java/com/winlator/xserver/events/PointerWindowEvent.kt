package com.winlator.xserver.events

import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.Bitmask
import com.winlator.xserver.Window
import java.io.IOException

abstract class PointerWindowEvent(
    code: Int,
    private val detail: Detail,
    private val root: Window,
    private val event: Window,
    private val child: Window?,
    private val rootX: Short,
    private val rootY: Short,
    private val eventX: Short,
    private val eventY: Short,
    private val state: Bitmask,
    private val mode: Mode,
    private val sameScreenAndFocus: Boolean,
) : Event(code) {

    enum class Detail {
        ANCESTOR,
        VIRTUAL,
        INFERIOR,
        NONLINEAR,
        NONLINEAR_VIRTUAL,
    }

    enum class Mode {
        NORMAL,
        GRAB,
        UNGRAB,
    }

    private val timestamp = System.currentTimeMillis().toInt()

    @Throws(IOException::class)
    override fun send(sequenceNumber: Short, outputStream: XOutputStream) {
        outputStream.lock().use {
            outputStream.writeByte(code)
            outputStream.writeByte(detail.ordinal.toByte())
            outputStream.writeShort(sequenceNumber)
            outputStream.writeInt(timestamp)
            outputStream.writeInt(root.id)
            outputStream.writeInt(event.id)
            outputStream.writeInt(child?.id ?: 0)
            outputStream.writeShort(rootX)
            outputStream.writeShort(rootY)
            outputStream.writeShort(eventX)
            outputStream.writeShort(eventY)
            outputStream.writeShort(state.bits.toShort())
            outputStream.writeByte(mode.ordinal.toByte())
            outputStream.writeByte((if (sameScreenAndFocus) 1 else 0).toByte())
        }
    }
}
