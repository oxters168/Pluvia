package com.winlator.xconnector

import androidx.annotation.Keep
import java.io.IOException
import java.nio.ByteBuffer
import java.util.ArrayDeque

class ClientSocket(val fd: Int) {

    companion object {
        init {
            System.loadLibrary("winlator")
        }
    }

    private val ancillaryFds = ArrayDeque<Int?>()

    val ancillaryFd: Int
        get() = if (hasAncillaryFds()) ancillaryFds.poll()!! else -1

    fun hasAncillaryFds(): Boolean = ancillaryFds.isNotEmpty()

    @Keep
    fun addAncillaryFd(ancillaryFd: Int) {
        ancillaryFds.add(ancillaryFd)
    }

    @Throws(IOException::class)
    fun read(data: ByteBuffer): Int {
        val position = data.position()
        val bytesRead = read(fd, data, position, data.remaining())
        if (bytesRead > 0) {
            data.position(position + bytesRead)
            return bytesRead
        } else if (bytesRead == 0) {
            return -1
        } else {
            throw IOException("Failed to read data.")
        }
    }

    @Throws(IOException::class)
    fun write(data: ByteBuffer) {
        val bytesWritten = write(fd, data, data.limit())
        if (bytesWritten >= 0) {
            data.position(bytesWritten)
        } else {
            throw IOException("Failed to write data.")
        }
    }

    @Throws(IOException::class)
    fun recvAncillaryMsg(data: ByteBuffer): Int {
        val position = data.position()
        val bytesRead = recvAncillaryMsg(fd, data, position, data.remaining())
        if (bytesRead > 0) {
            data.position(position + bytesRead)
            return bytesRead
        } else if (bytesRead == 0) {
            return -1
        } else {
            throw IOException("Failed to receive ancillary messages.")
        }
    }

    @Throws(IOException::class)
    fun sendAncillaryMsg(data: ByteBuffer, ancillaryFd: Int) {
        val bytesSent = sendAncillaryMsg(fd, data, data.limit(), ancillaryFd)
        if (bytesSent >= 0) {
            data.position(bytesSent)
        } else {
            throw IOException("Failed to send ancillary messages.")
        }
    }

    /**
     * Native Methods
     */

    private external fun read(fd: Int, data: ByteBuffer?, offset: Int, length: Int): Int

    private external fun write(fd: Int, data: ByteBuffer?, length: Int): Int

    private external fun recvAncillaryMsg(clientFd: Int, data: ByteBuffer?, offset: Int, length: Int): Int

    private external fun sendAncillaryMsg(clientFd: Int, data: ByteBuffer?, length: Int, ancillaryFd: Int): Int
}
