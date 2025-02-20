package com.winlator.xconnector

import com.winlator.xserver.XServer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.locks.ReentrantLock

class XOutputStream(val clientSocket: ClientSocket?, initialCapacity: Int) {

    companion object {
        private val ZERO = ByteArray(64)
    }

    private var buffer: ByteBuffer = ByteBuffer.allocateDirect(initialCapacity)

    private val lock = ReentrantLock()

    private var ancillaryFd = -1

    constructor(initialCapacity: Int) : this(null, initialCapacity)

    fun setByteOrder(byteOrder: ByteOrder) {
        buffer.order(byteOrder)
    }

    fun setAncillaryFd(ancillaryFd: Int) {
        this.ancillaryFd = ancillaryFd
    }

    fun writeByte(value: Byte) {
        ensureSpaceIsAvailable(1)
        buffer.put(value)
    }

    fun writeShort(value: Short) {
        ensureSpaceIsAvailable(2)
        buffer.putShort(value)
    }

    fun writeInt(value: Int) {
        ensureSpaceIsAvailable(4)
        buffer.putInt(value)
    }

    fun writeLong(value: Long) {
        ensureSpaceIsAvailable(8)
        buffer.putLong(value)
    }

    fun writeString8(str: String) {
        val bytes = str.toByteArray(XServer.LATIN1_CHARSET)
        val length = -str.length and 3

        ensureSpaceIsAvailable(bytes.size + length)
        buffer.put(bytes)

        if (length > 0) {
            writePad(length)
        }
    }

    @JvmOverloads
    fun write(data: ByteArray, offset: Int = 0, length: Int = data.size) {
        ensureSpaceIsAvailable(length)
        buffer.put(data, offset, length)
    }

    fun write(data: ByteBuffer) {
        ensureSpaceIsAvailable(data.remaining())
        buffer.put(data)
    }

    fun writePad(length: Int) {
        write(XOutputStream.Companion.ZERO, 0, length)
    }

    @Throws(IOException::class)
    private fun flush() {
        if (buffer.position() != 0) {
            buffer.flip()

            if (ancillaryFd != -1) {
                clientSocket?.sendAncillaryMsg(buffer, ancillaryFd)
                ancillaryFd = -1
            } else {
                clientSocket?.write(buffer)
            }

            buffer.clear()
        }
    }

    fun lock(): XStreamLock = OutputStreamLock()

    private fun ensureSpaceIsAvailable(length: Int) {
        val position = buffer.position()

        if ((buffer.capacity() - position) >= length) {
            return
        }

        val newBuffer = ByteBuffer.allocateDirect(buffer.capacity() + length).order(buffer.order())

        buffer.rewind()
        newBuffer.put(buffer).position(position)

        buffer = newBuffer
    }

    private inner class OutputStreamLock : XStreamLock {
        init {
            lock.lock()
        }

        @Throws(IOException::class)
        override fun close() {
            try {
                flush()
            } finally {
                lock.unlock()
            }
        }
    }
}
