package com.winlator.renderer

import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.winlator.xserver.Drawable
import java.nio.ByteBuffer

open class Texture {

    var textureId: Int = 0
        protected set

    var wrapS: Int = GLES20.GL_CLAMP_TO_EDGE
    var wrapT: Int = GLES20.GL_CLAMP_TO_EDGE
    var magFilter: Int = GLES20.GL_LINEAR
    var minFilter: Int = GLES20.GL_LINEAR
    var format: Int = GLES11Ext.GL_BGRA
    var isNeedsUpdate: Boolean = true

    open fun allocateTexture(width: Short, height: Short, data: ByteBuffer?) {
        val textureIds = IntArray(1)

        GLES20.glGenTextures(1, textureIds, 0)

        textureId = textureIds[0]

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 4)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        if (data != null) {
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format, width.toInt(), height.toInt(), 0, format, GLES20.GL_UNSIGNED_BYTE, data)
        }

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, wrapS)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, wrapT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, magFilter)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, minFilter)

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    open fun updateFromDrawable(drawable: Drawable) {
        val data = drawable.data ?: return

        if (!isAllocated) {
            allocateTexture(drawable.width, drawable.height, data)
        } else if (isNeedsUpdate) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glTexSubImage2D(
                GLES20.GL_TEXTURE_2D,
                0,
                0,
                0,
                drawable.width.toInt(),
                drawable.height.toInt(),
                format,
                GLES20.GL_UNSIGNED_BYTE,
                data,
            )
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

            isNeedsUpdate = false
        }
    }

    val isAllocated: Boolean
        get() = textureId > 0

    fun copyFromFramebuffer(framebuffer: Int, width: Short, height: Short) {
        if (!isAllocated) {
            allocateTexture(width, height, null)
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, framebuffer)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glCopyTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 0, 0, width.toInt(), height.toInt(), 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    open fun destroy() {
        if (textureId > 0) {
            val textureIds = intArrayOf(textureId)

            GLES20.glDeleteTextures(textureIds.size, textureIds, 0)

            textureId = 0
        }
    }
}
