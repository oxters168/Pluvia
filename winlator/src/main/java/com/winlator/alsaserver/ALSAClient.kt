package com.winlator.alsaserver

import com.winlator.sysvshm.SysVSharedMemory
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ALSAClient {

    enum class DataType(byteCount: Int) {
        U8(1),
        S16LE(2),
        S16BE(2),
        FLOATLE(4),
        FLOATBE(4),
        ;

        val byteCount: Byte = byteCount.toByte()
    }

    companion object {
        init {
            System.loadLibrary("winlator")
        }
    }

    var dataType: DataType = DataType.U8
        internal set

    var channelCount: Byte = 2
        internal set

    var sampleRate: Int = 0
        internal set

    private var position = 0

    var bufferSize: Int = 0
        internal set

    private var frameBytes = 0

    var sharedBuffer: ByteBuffer? = null
        internal set

    private var playing = false

    private var streamPtr: Long = 0

    fun release() {
        sharedBuffer?.let { buffer ->
            SysVSharedMemory.unmapSHMSegment(buffer, buffer.capacity().toLong())
            sharedBuffer = null
        }

        stop(streamPtr)
        close(streamPtr)

        playing = false
        streamPtr = 0
    }

    fun prepare() {
        position = 0
        frameBytes = channelCount * dataType.byteCount

        release()

        if (!isValidBufferSize()) return

        streamPtr = create(dataType.ordinal, channelCount, sampleRate, bufferSize)

        if (streamPtr > 0) start()
    }

    fun start() {
        if (streamPtr > 0 && !playing) {
            start(streamPtr)
            playing = true
        }
    }

    fun stop() {
        if (streamPtr > 0 && playing) {
            stop(streamPtr)
            playing = false
        }
    }

    fun pause() {
        if (streamPtr > 0) {
            pause(streamPtr)
            playing = false
        }
    }

    fun drain() {
        if (streamPtr > 0) flush(streamPtr)
    }

    fun writeDataToStream(data: ByteBuffer) {
        if (dataType == DataType.S16LE || dataType == DataType.FLOATLE) {
            data.order(ByteOrder.LITTLE_ENDIAN)
        } else if (dataType == DataType.S16BE || dataType == DataType.FLOATBE) {
            data.order(ByteOrder.BIG_ENDIAN)
        }

        if (playing) {
            val numFrames = data.limit() / frameBytes
            val framesWritten = write(streamPtr, data, numFrames)

            if (framesWritten > 0) position += framesWritten

            data.rewind()
        }
    }

    fun pointer(): Int = position

    fun getBufferSizeInBytes(): Int = bufferSize * frameBytes

    fun computeLatencyMillis(): Int = ((bufferSize.toFloat() / sampleRate) * 1000).toInt()

    private fun isValidBufferSize(): Boolean = (getBufferSizeInBytes() % frameBytes == 0) && bufferSize > 0

    /**
     * Native Calls
     */

    private external fun create(format: Int, channelCount: Byte, sampleRate: Int, bufferSize: Int): Long

    private external fun write(streamPtr: Long, buffer: ByteBuffer?, numFrames: Int): Int

    private external fun start(streamPtr: Long)

    private external fun stop(streamPtr: Long)

    private external fun pause(streamPtr: Long)

    private external fun flush(streamPtr: Long)

    private external fun close(streamPtr: Long)
}
