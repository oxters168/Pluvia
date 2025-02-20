package com.winlator.xserver.events

import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.Bitmask
import com.winlator.xserver.Window
import java.io.IOException

open class InputDeviceEvent(
    code: Int,
    private val detail: Byte,
    private val root: Window,
    private val event: Window,
    private val child: Window?,
    private val rootX: Short,
    private val rootY: Short,
    private val eventX: Short,
    private val eventY: Short,
    private val state: Bitmask,
) : Event(code) {

    private val timestamp = System.currentTimeMillis().toInt()

    @Throws(IOException::class)
    override fun send(sequenceNumber: Short, outputStream: XOutputStream) {
        outputStream.lock().use {
            outputStream.writeByte(code)
            outputStream.writeByte(detail)
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
            outputStream.writeByte(1.toByte())
            outputStream.writeByte(0.toByte())
        }
    }
}
