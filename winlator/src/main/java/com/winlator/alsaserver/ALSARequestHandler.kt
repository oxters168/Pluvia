package com.winlator.alsaserver

import com.winlator.sysvshm.SysVSharedMemory
import com.winlator.xconnector.Client
import com.winlator.xconnector.RequestHandler
import com.winlator.xconnector.XConnectorEpoll
import com.winlator.xconnector.XOutputStream
import java.io.IOException

class ALSARequestHandler : RequestHandler {

    private var maxSHMemoryId = 0

    @Throws(IOException::class)
    override fun handleRequest(client: Client?): Boolean {
        val alsaClient = client!!.tag as ALSAClient
        val inputStream = client.inputStream!!
        val outputStream = client.outputStream!!

        if (inputStream.available() < 5) return false

        val requestCode = inputStream.readByte()
        val requestLength = inputStream.readInt()

        when (requestCode) {
            RequestCodes.CLOSE.code -> alsaClient.release()
            RequestCodes.START.code -> alsaClient.start()
            RequestCodes.STOP.code -> alsaClient.stop()
            RequestCodes.PAUSE.code -> alsaClient.pause()
            RequestCodes.PREPARE.code -> {
                if (inputStream.available() < requestLength) return false

                alsaClient.channelCount = inputStream.readByte().toInt().toByte() // :^)
                alsaClient.dataType = ALSAClient.DataType.entries.toTypedArray()[inputStream.readByte().toInt()]

                alsaClient.sampleRate = inputStream.readInt()
                alsaClient.bufferSize = inputStream.readInt()
                alsaClient.prepare()

                createSharedMemory(alsaClient, outputStream)
            }

            RequestCodes.WRITE.code -> {
                val buffer = alsaClient.sharedBuffer
                if (buffer != null) {
                    buffer.limit(requestLength)
                    alsaClient.writeDataToStream(buffer)
                } else {
                    if (inputStream.available() < requestLength) return false
                    alsaClient.writeDataToStream(inputStream.readByteBuffer(requestLength))
                }
            }

            RequestCodes.DRAIN.code -> alsaClient.drain()
            RequestCodes.POINTER.code -> outputStream.lock().use { lock ->
                outputStream.writeInt(alsaClient.pointer())
            }
        }

        return true
    }

    @Throws(IOException::class)
    private fun createSharedMemory(alsaClient: ALSAClient, outputStream: XOutputStream) {
        val size = alsaClient.getBufferSizeInBytes()
        val fd = SysVSharedMemory.createMemoryFd("alsa-shm" + (++maxSHMemoryId), size)

        if (fd >= 0) {
            SysVSharedMemory.mapSHMSegment(fd, size.toLong(), 0, true)?.let { buffer ->
                alsaClient.sharedBuffer = buffer
            }
        }

        try {
            outputStream.lock().use {
                outputStream.writeByte(0.toByte())
                outputStream.setAncillaryFd(fd)
            }
        } finally {
            if (fd >= 0) XConnectorEpoll.closeFd(fd)
        }
    }
}
