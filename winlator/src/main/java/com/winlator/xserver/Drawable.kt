package com.winlator.xserver

import android.graphics.Bitmap
import com.winlator.core.Callback
import com.winlator.math.Mathf.clamp
import com.winlator.renderer.GPUImage
import com.winlator.renderer.Texture
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Drawable(id: Int, width: Int, height: Int, val visual: Visual?) : XResource(id) {

    companion object {

        init {
            System.loadLibrary("winlator")
        }

        fun fromBitmap(bitmap: Bitmap): Drawable {
            val drawable = Drawable(0, bitmap.getWidth(), bitmap.getHeight(), null)
            fromBitmap(bitmap, drawable.data)
            return drawable
        }

        /**
         * Native Methods
         */

        private external fun drawBitmap(width: Short, height: Short, srcData: ByteBuffer?, dstData: ByteBuffer?)

        private external fun drawAlphaMaskedBitmap(
            foreRed: Byte,
            foreGreen: Byte,
            foreBlue: Byte,
            backRed: Byte,
            backGreen: Byte,
            backBlue: Byte,
            srcData: ByteBuffer?,
            maskData: ByteBuffer?,
            dstData: ByteBuffer?,
        )

        private external fun copyArea(
            srcX: Short,
            srcY: Short,
            dstX: Short,
            dstY: Short,
            width: Short,
            height: Short,
            srcStride: Short,
            dstStride: Short,
            srcData: ByteBuffer?,
            dstData: ByteBuffer?,
        )

        private external fun copyAreaOp(
            srcX: Short,
            srcY: Short,
            dstX: Short,
            dstY: Short,
            width: Short,
            height: Short,
            srcStride: Short,
            dstStride: Short,
            srcData: ByteBuffer?,
            dstData: ByteBuffer?,
            gcFunction: Int,
        )

        private external fun fillRect(x: Short, y: Short, width: Short, height: Short, color: Int, stride: Short, data: ByteBuffer?)

        private external fun drawLine(
            x0: Short,
            y0: Short,
            x1: Short,
            y1: Short,
            color: Int,
            lineWidth: Short,
            stride: Short,
            data: ByteBuffer?,
        )

        private external fun fromBitmap(bitmap: Bitmap?, data: ByteBuffer?)
    }

    val width: Short

    val height: Short

    var texture: Texture? = Texture()
        internal set

    var data: ByteBuffer?

    var onDrawListener: Runnable? = null

    var onDestroyListener: Callback<Drawable>? = null

    val renderLock: Any = Any()

    private val stride: Short
        get() = if (texture is GPUImage) (texture as GPUImage).stride else width

    init {
        this.width = width.toShort()
        this.height = height.toShort()
        this.data = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.LITTLE_ENDIAN)
    }

    fun setTexture(texture: Texture) {
        if (texture is GPUImage) {
            data = texture.virtualData
        }

        this.texture = texture
    }

    fun drawImage(
        srcX: Short,
        srcY: Short,
        dstX: Short,
        dstY: Short,
        width: Short,
        height: Short,
        depth: Byte,
        data: ByteBuffer,
        totalWidth: Short,
        totalHeight: Short,
    ) {
        var dstX = dstX
        var dstY = dstY
        var width = width
        var height = height
        if (depth.toInt() == 1) {
            drawBitmap(width, height, data, this.data)
        } else if (depth.toInt() == 24 || depth.toInt() == 32) {
            dstX = clamp(dstX.toInt(), 0, this.width - 1).toShort()
            dstY = clamp(dstY.toInt(), 0, this.height - 1).toShort()

            if ((dstX + width) > this.width) {
                width = ((this.width - dstX)).toShort()
            }

            if ((dstY + height) > this.height) {
                height = ((this.height - dstY)).toShort()
            }

            copyArea(
                srcX = srcX,
                srcY = srcY,
                dstX = dstX,
                dstY = dstY,
                width = width,
                height = height,
                srcStride = totalWidth,
                dstStride = this.stride,
                srcData = data,
                dstData = this.data,
            )
        }

        this.data!!.rewind()
        data.rewind()

        texture?.isNeedsUpdate = true
        onDrawListener?.run()
    }

    fun getImage(x: Short, y: Short, width: Short, height: Short): ByteBuffer {
        var x = x
        var y = y
        var width = width
        var height = height
        val dstData = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.LITTLE_ENDIAN)

        x = clamp(x.toInt(), 0, this.width - 1).toShort()
        y = clamp(y.toInt(), 0, this.height - 1).toShort()

        if ((x + width) > this.width) {
            width = (this.width - x).toShort()
        }

        if ((y + height) > this.height) {
            height = (this.height - y).toShort()
        }

        copyArea(
            srcX = x,
            srcY = y,
            dstX = 0.toShort(),
            dstY = 0.toShort(),
            width = width,
            height = height,
            srcStride = this.stride,
            dstStride = width,
            srcData = this.data,
            dstData = dstData,
        )

        this.data!!.rewind()
        dstData.rewind()

        return dstData
    }

    @JvmOverloads
    fun copyArea(
        srcX: Short,
        srcY: Short,
        dstX: Short,
        dstY: Short,
        width: Short,
        height: Short,
        drawable: Drawable,
        gcFunction: GraphicsContext.Function = GraphicsContext.Function.COPY,
    ) {
        var dstX = dstX
        var dstY = dstY
        var width = width
        var height = height

        dstX = clamp(dstX.toInt(), 0, this.width - 1).toShort()
        dstY = clamp(dstY.toInt(), 0, this.height - 1).toShort()

        if ((dstX + width) > this.width) {
            width = (this.width - dstX).toShort()
        }

        if ((dstY + height) > this.height) {
            height = (this.height - dstY).toShort()
        }

        if (gcFunction == GraphicsContext.Function.COPY) {
            copyArea(
                srcX = srcX,
                srcY = srcY,
                dstX = dstX,
                dstY = dstY,
                width = width,
                height = height,
                srcStride = drawable.stride,
                dstStride = this.stride,
                srcData = drawable.data,
                dstData = this.data,
            )
        } else {
            copyAreaOp(
                srcX = srcX,
                srcY = srcY,
                dstX = dstX,
                dstY = dstY,
                width = width,
                height = height,
                srcStride = drawable.stride,
                dstStride = this.stride,
                srcData = drawable.data,
                dstData = this.data,
                gcFunction = gcFunction.ordinal,
            )
        }

        this.data!!.rewind()
        drawable.data!!.rewind()

        texture?.isNeedsUpdate = true
        onDrawListener?.run()
    }

    fun fillColor(color: Int) {
        fillRect(0, 0, width.toInt(), height.toInt(), color)
    }

    fun fillRect(x: Int, y: Int, width: Int, height: Int, color: Int) {
        var x = x
        var y = y
        var width = width
        var height = height

        x = clamp(x, 0, this.width - 1).toShort().toInt()
        y = clamp(y, 0, this.height - 1).toShort().toInt()

        if ((x + width) > this.width) {
            width = ((this.width - x)).toShort().toInt()
        }

        if ((y + height) > this.height) {
            height = ((this.height - y)).toShort().toInt()
        }

        fillRect(
            x = x.toShort(),
            y = y.toShort(),
            width = width.toShort(),
            height = height.toShort(),
            color = color,
            stride = this.stride,
            data = this.data,
        )

        this.data!!.rewind()

        texture?.isNeedsUpdate = true
        onDrawListener?.run()
    }

    fun drawLines(color: Int, lineWidth: Int, vararg points: Short) {
        var i = 2

        while (i < points.size) {
            drawLine(
                x0 = points[i - 2].toInt(),
                y0 = points[i - 1].toInt(),
                x1 = points[i + 0].toInt(),
                y1 = points[i + 1].toInt(),
                color = color,
                lineWidth = lineWidth.toShort().toInt(),
            )

            i += 2
        }
    }

    fun drawLine(x0: Int, y0: Int, x1: Int, y1: Int, color: Int, lineWidth: Int) {
        var x0 = x0
        var y0 = y0
        var x1 = x1
        var y1 = y1

        x0 = clamp(x0, 0, width - lineWidth)
        y0 = clamp(y0, 0, height - lineWidth)
        x1 = clamp(x1, 0, width - lineWidth)
        y1 = clamp(y1, 0, height - lineWidth)

        drawLine(
            x0 = x0.toShort(),
            y0 = y0.toShort(),
            x1 = x1.toShort(),
            y1 = y1.toShort(),
            color = color,
            lineWidth = lineWidth.toShort(),
            stride = this.stride,
            data = this.data,
        )

        this.data!!.rewind()

        texture?.isNeedsUpdate = true
        onDrawListener?.run()
    }

    fun drawAlphaMaskedBitmap(
        foreRed: Byte,
        foreGreen: Byte,
        foreBlue: Byte,
        backRed: Byte,
        backGreen: Byte,
        backBlue: Byte,
        srcDrawable: Drawable,
        maskDrawable: Drawable,
    ) {
        drawAlphaMaskedBitmap(
            foreRed = foreRed,
            foreGreen = foreGreen,
            foreBlue = foreBlue,
            backRed = backRed,
            backGreen = backGreen,
            backBlue = backBlue,
            srcData = srcDrawable.data,
            maskData = maskDrawable.data,
            dstData = this.data,
        )

        this.data!!.rewind()

        texture?.isNeedsUpdate = true
        onDrawListener?.run()
    }
}
