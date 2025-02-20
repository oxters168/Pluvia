package com.winlator.xserver.events

import com.winlator.xconnector.XOutputStream
import java.io.IOException

class RawEvent(private val data: ByteArray) : Event(data[0].toInt()) {
    @Throws(IOException::class)
    override fun send(sequenceNumber: Short, outputStream: XOutputStream) {
        outputStream.lock().use {
            outputStream.write(data)
        }
    }
}
