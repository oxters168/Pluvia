package com.winlator.xconnector

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import timber.log.Timber

class Client(private val connector: XConnectorEpoll, val clientSocket: ClientSocket?) {

    var inputStream: XInputStream? = null
        private set

    var outputStream: XOutputStream? = null
        private set

    var tag: Any? = null

    var pollThread: Thread? = null

    var shutdownFd: Int = 0

    var connected: Boolean = false

    fun createIOStreams() {
        if (inputStream != null || outputStream != null) {
            return
        }

        inputStream = XInputStream(clientSocket, connector.initialInputBufferCapacity).apply {
            setByteOrder(ByteOrder.LITTLE_ENDIAN)
        }
        outputStream = XOutputStream(clientSocket, connector.initialOutputBufferCapacity).apply {
            setByteOrder(ByteOrder.LITTLE_ENDIAN)
        }
    }

    fun requestShutdown() {
        try {
            val data = ByteBuffer.allocateDirect(8)
            data.asLongBuffer().put(1)
            (ClientSocket(shutdownFd)).write(data)
        } catch (e: IOException) {
            Timber.w(e)
        }
    }
}
