package com.winlator.xserver.requests

import com.winlator.xconnector.XInputStream
import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.GraphicsContext
import com.winlator.xserver.XClient
import com.winlator.xserver.XClientRequestHandler
import com.winlator.xserver.errors.BadDrawable
import com.winlator.xserver.errors.BadGraphicsContext
import com.winlator.xserver.errors.BadMatch
import com.winlator.xserver.errors.XRequestError
import java.io.IOException

object DrawRequests {
    @Throws(XRequestError::class)
    fun putImage(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        val format = Format.entries[client.requestData.toInt()]
        val drawableId = inputStream.readInt()
        val gcId = inputStream.readInt()
        val width = inputStream.readShort()
        val height = inputStream.readShort()
        val dstX = inputStream.readShort()
        val dstY = inputStream.readShort()
        val leftPad = inputStream.readByte()
        val depth = inputStream.readByte()

        inputStream.skip(2)

        val length = client.remainingRequestLength
        val data = inputStream.readByteBuffer(length)

        val drawable = client.xServer.drawableManager.getDrawable(drawableId)
        if (drawable == null) {
            throw BadDrawable(drawableId)
        }

        val graphicsContext = client.xServer.graphicsContextManager.getGraphicsContext(gcId)
        if (graphicsContext == null) {
            throw BadGraphicsContext(gcId)
        }

        if (!(graphicsContext.function == GraphicsContext.Function.COPY || format == Format.Z_PIXMAP)) {
            throw UnsupportedOperationException("GC Function other than COPY is not supported.")
        }

        when (format) {
            Format.BITMAP -> {
                if (leftPad.toInt() != 0) {
                    throw UnsupportedOperationException("PutImage.leftPad cannot be != 0.")
                }
                if (depth.toInt() == 1) {
                    drawable.drawImage(0.toShort(), 0.toShort(), dstX, dstY, width, height, 1.toByte(), data, width, height)
                } else {
                    throw BadMatch()
                }
            }

            Format.XY_PIXMAP -> if (drawable.visual!!.depth != depth) {
                throw BadMatch()
            }

            Format.Z_PIXMAP -> if (leftPad.toInt() == 0) {
                drawable.drawImage(0.toShort(), 0.toShort(), dstX, dstY, width, height, depth, data, width, height)
            } else {
                throw BadMatch()
            }
        }
    }

    @Throws(IOException::class, XRequestError::class)
    fun getImage(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        val format: Format? = Format.entries[client.requestData.toInt()]
        val drawableId = inputStream.readInt()
        val x = inputStream.readShort()
        val y = inputStream.readShort()
        val width = inputStream.readShort()
        val height = inputStream.readShort()

        inputStream.skip(4)

        if (format != Format.Z_PIXMAP) {
            throw UnsupportedOperationException("Only Z_PIXMAP is supported.")
        }

        val drawable = client.xServer.drawableManager.getDrawable(drawableId)
        if (drawable == null) {
            throw BadDrawable(drawableId)
        }

        val visualId = if (client.xServer.pixmapManager.getPixmap(drawableId) == null) drawable.visual!!.id else 0
        val data = drawable.getImage(x, y, width, height)
        val length = data.limit()

        outputStream.lock().use {
            outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
            outputStream.writeByte(drawable.visual!!.depth)
            outputStream.writeShort(client.sequenceNumber)
            outputStream.writeInt((length + 3) / 4)
            outputStream.writeInt(visualId)
            outputStream.writePad(20)
            outputStream.write(data)

            if ((-length and 3) > 0) {
                outputStream.writePad(-length and 3)
            }
        }
    }

    @Throws(XRequestError::class)
    fun copyArea(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        val srcDrawableId = inputStream.readInt()
        val dstDrawableId = inputStream.readInt()
        val gcId = inputStream.readInt()
        val srcX = inputStream.readShort()
        val srcY = inputStream.readShort()
        val dstX = inputStream.readShort()
        val dstY = inputStream.readShort()
        val width = inputStream.readShort()
        val height = inputStream.readShort()

        val srcDrawable = client.xServer.drawableManager.getDrawable(srcDrawableId)
        if (srcDrawable == null) {
            throw BadDrawable(srcDrawableId)
        }

        val dstDrawable = client.xServer.drawableManager.getDrawable(dstDrawableId)
        if (dstDrawable == null) {
            throw BadDrawable(dstDrawableId)
        }

        val graphicsContext = client.xServer.graphicsContextManager.getGraphicsContext(gcId)
        if (graphicsContext == null) {
            throw BadGraphicsContext(gcId)
        }

        if (srcDrawable.visual!!.depth != dstDrawable.visual!!.depth) {
            throw BadMatch()
        }

        dstDrawable.copyArea(srcX, srcY, dstX, dstY, width, height, srcDrawable, graphicsContext.function!!)
    }

    @Throws(XRequestError::class)
    fun polyLine(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        val coordinateMode: CoordinateMode? = CoordinateMode.entries[client.requestData.toInt()]
        val drawableId = inputStream.readInt()
        val gcId = inputStream.readInt()

        val drawable = client.xServer.drawableManager.getDrawable(drawableId)
        if (drawable == null) {
            throw BadDrawable(drawableId)
        }

        val graphicsContext = client.xServer.graphicsContextManager.getGraphicsContext(gcId)
        if (graphicsContext == null) {
            throw BadGraphicsContext(gcId)
        }

        var length = client.remainingRequestLength

        val points = ShortArray(length / 2)
        var i = 0
        while (length != 0) {
            points[i++] = inputStream.readShort()
            points[i++] = inputStream.readShort()
            length -= 4
        }

        if (coordinateMode == CoordinateMode.ORIGIN && graphicsContext.lineWidth > 0) {
            drawable.drawLines(graphicsContext.foreground, graphicsContext.lineWidth, *points)
        }
    }

    @Throws(XRequestError::class)
    fun polyFillRectangle(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        val drawableId = inputStream.readInt()
        val gcId = inputStream.readInt()

        val drawable = client.xServer.drawableManager.getDrawable(drawableId)
        if (drawable == null) {
            throw BadDrawable(drawableId)
        }

        val graphicsContext = client.xServer.graphicsContextManager.getGraphicsContext(gcId)
        if (graphicsContext == null) {
            throw BadGraphicsContext(gcId)
        }

        var length = client.remainingRequestLength

        while (length != 0) {
            val x = inputStream.readShort()
            val y = inputStream.readShort()
            val width = inputStream.readShort()
            val height = inputStream.readShort()

            drawable.fillRect(x.toInt(), y.toInt(), width.toInt(), height.toInt(), graphicsContext.background)

            length -= 8
        }
    }

    enum class Format {
        BITMAP,
        XY_PIXMAP,
        Z_PIXMAP,
    }

    private enum class CoordinateMode {
        ORIGIN,
        PREVIOUS,
    }
}
