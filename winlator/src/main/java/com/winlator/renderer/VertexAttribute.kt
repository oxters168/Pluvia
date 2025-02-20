package com.winlator.renderer

import android.opengl.GLES20
import java.nio.Buffer
import java.nio.FloatBuffer

class VertexAttribute(private val name: String, itemSize: Int) {

    private var buffer: Buffer? = null
    private var bufferId = 0
    private val itemSize = itemSize.toByte()
    private var needsUpdate = true

    var location: Int = -1
        private set

    fun put(array: FloatArray?) {
        buffer = FloatBuffer.wrap(array)
        needsUpdate = true
    }

    fun update() {
        if (!needsUpdate || buffer == null) {
            return
        }

        if (bufferId == 0) {
            val bufferIds = IntArray(1)
            GLES20.glGenBuffers(1, bufferIds, 0)
            bufferId = bufferIds[0]
        }

        val size = buffer!!.limit() * 4

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId)
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, size, buffer, GLES20.GL_STATIC_DRAW)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        needsUpdate = false
    }

    fun bind(programId: Int) {
        update()

        if (location == -1) {
            location = GLES20.glGetAttribLocation(programId, name)
        }

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bufferId)
        GLES20.glEnableVertexAttribArray(location)
        GLES20.glVertexAttribPointer(location, itemSize.toInt(), GLES20.GL_FLOAT, false, 0, 0)
    }

    fun disable() {
        if (location == -1) {
            return
        }

        GLES20.glDisableVertexAttribArray(location)
    }

    fun destroy() {
        clear()

        if (bufferId > 0) {
            val bufferIds = intArrayOf(bufferId)
            GLES20.glDeleteBuffers(bufferIds.size, bufferIds, 0)

            bufferId = 0
        }
    }

    fun clear() {
        buffer?.let {
            buffer!!.limit(0)
            buffer = null
        }
    }

    fun count(): Int = if (buffer != null) buffer!!.limit() / itemSize else 0
}
