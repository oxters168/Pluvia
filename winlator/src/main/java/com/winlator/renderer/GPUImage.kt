package com.winlator.renderer

import androidx.annotation.Keep
import com.winlator.xserver.Drawable
import java.nio.ByteBuffer

class GPUImage(width: Short, height: Short) : Texture() {

    companion object {
        var isSupported: Boolean = false
            private set

        init {
            System.loadLibrary("winlator")
        }

        @JvmStatic
        fun checkIsSupported() {
            val size: Short = 8
            val gpuImage = GPUImage(size, size)

            gpuImage.allocateTexture(size, size, null)

            isSupported = gpuImage.hardwareBufferPtr != 0L && gpuImage.imageKHRPtr != 0L && gpuImage.virtualData != null

            gpuImage.destroy()
        }
    }

    private var hardwareBufferPtr: Long

    private var imageKHRPtr: Long = 0

    var virtualData: ByteBuffer? = null
        private set

    @set:Keep
    var stride: Short = 0
        private set

    init {
        hardwareBufferPtr = createHardwareBuffer(width, height)

        if (hardwareBufferPtr != 0L) {
            virtualData = lockHardwareBuffer(hardwareBufferPtr)
        }
    }

    override fun allocateTexture(width: Short, height: Short, data: ByteBuffer?) {
        if (isAllocated) {
            return
        }

        super.allocateTexture(width, height, null)

        imageKHRPtr = createImageKHR(hardwareBufferPtr, textureId)
    }

    override fun updateFromDrawable(drawable: Drawable) {
        if (!isAllocated) {
            allocateTexture(drawable.width, drawable.height, null)
        }

        isNeedsUpdate = false
    }

    override fun destroy() {
        destroyImageKHR(imageKHRPtr)
        destroyHardwareBuffer(hardwareBufferPtr)

        virtualData = null
        imageKHRPtr = 0
        hardwareBufferPtr = 0

        super.destroy()
    }

    /**
     * Native Methods
     */

    private external fun createHardwareBuffer(width: Short, height: Short): Long

    private external fun destroyHardwareBuffer(hardwareBufferPtr: Long)

    private external fun lockHardwareBuffer(hardwareBufferPtr: Long): ByteBuffer?

    private external fun createImageKHR(hardwareBufferPtr: Long, textureId: Int): Long

    private external fun destroyImageKHR(imageKHRPtr: Long)
}
