package com.winlator.xconnector

import com.winlator.xserver.XServer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

class XInputStream(val clientSocket: ClientSocket?, initialCapacity: Int) {

    private var activeBuffer: ByteBuffer? = null

    private var buffer: ByteBuffer = ByteBuffer.allocateDirect(initialCapacity)

    constructor(initialCapacity: Int) : this(null, initialCapacity)

    @Throws(IOException::class)
    fun readMoreData(canReceiveAncillaryMessages: Boolean): Int {
        if (activeBuffer != null) {
            if (!activeBuffer!!.hasRemaining()) {
                buffer.clear()
            } else if (activeBuffer!!.position() > 0) {
                val newLimit = buffer.position()
                buffer.position(activeBuffer!!.position()).limit(newLimit)
                buffer.compact()
            }

            activeBuffer = null
        }

        growInputBufferIfNecessary()

        val bytesRead = if (canReceiveAncillaryMessages) {
            clientSocket!!.recvAncillaryMsg(buffer)
        } else {
            clientSocket!!.read(buffer)
        }

        if (bytesRead > 0) {
            val position = buffer.position()

            buffer.flip()
            activeBuffer = buffer.slice().order(buffer.order())
            buffer.limit(buffer.capacity()).position(position)
        }

        return bytesRead
    }

    val ancillaryFd: Int
        get() = clientSocket!!.ancillaryFd

    private fun growInputBufferIfNecessary() {
        if (buffer.position() == buffer.capacity()) {
            val newBuffer = ByteBuffer.allocateDirect(buffer.capacity() * 2).order(buffer.order())
            buffer.rewind()
            newBuffer.put(buffer)
            buffer = newBuffer
        }
    }

    fun setByteOrder(byteOrder: ByteOrder) {
        buffer.order(byteOrder)

        if (activeBuffer != null) {
            activeBuffer!!.order(byteOrder)
        }
    }

    var activePosition: Int
        get() = activeBuffer!!.position()
        set(activePosition) {
            activeBuffer!!.position(activePosition)
        }

    fun available(): Int = activeBuffer!!.remaining()

    fun readByte(): Byte = activeBuffer!!.get()

    fun readUnsignedByte(): Int = activeBuffer!!.get().toInt() and 0xFF

    fun readShort(): Short = activeBuffer!!.getShort()

    fun readUnsignedShort(): Int = activeBuffer!!.getShort().toInt() and 0xFFFF

    fun readInt(): Int = activeBuffer!!.getInt()

    fun readUnsignedInt(): Long = Integer.toUnsignedLong(activeBuffer!!.getInt())

    fun readLong(): Long = activeBuffer!!.getLong()

    fun read(result: ByteArray) {
        activeBuffer!!.get(result)
    }

    fun readByteBuffer(length: Int): ByteBuffer {
        val newBuffer = activeBuffer!!.slice().order(activeBuffer!!.order())
        newBuffer.limit(length)
        activeBuffer!!.position(activeBuffer!!.position() + length)

        return newBuffer
    }

    fun readString8(length: Int): String {
        val bytes = ByteArray(length)
        read(bytes)

        val str = String(bytes, XServer.LATIN1_CHARSET)

        if ((-length and 3) > 0) {
            skip(-length and 3)
        }

        return str
    }

    fun skip(length: Int) {
        activeBuffer!!.position(activeBuffer!!.position() + length)
    }
}
