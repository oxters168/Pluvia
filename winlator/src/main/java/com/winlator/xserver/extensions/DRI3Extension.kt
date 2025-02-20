package com.winlator.xserver.extensions

import com.winlator.core.Callback
import com.winlator.sysvshm.SysVSharedMemory
import com.winlator.xconnector.XConnectorEpoll
import com.winlator.xconnector.XInputStream
import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.Drawable
import com.winlator.xserver.XClient
import com.winlator.xserver.XClientRequestHandler
import com.winlator.xserver.XServer
import com.winlator.xserver.errors.BadAlloc
import com.winlator.xserver.errors.BadDrawable
import com.winlator.xserver.errors.BadIdChoice
import com.winlator.xserver.errors.BadImplementation
import com.winlator.xserver.errors.BadWindow
import com.winlator.xserver.errors.XRequestError
import java.io.IOException

class DRI3Extension : Extension {

    companion object {
        const val MAJOR_OPCODE: Byte = -102
    }

    private val onDestroyDrawableListener = Callback { drawable: Drawable ->
        val data = drawable.data
        SysVSharedMemory.unmapSHMSegment(data, data.capacity().toLong())
    }

    private object ClientOpcodes {
        const val QUERY_VERSION: Byte = 0
        const val OPEN: Byte = 1
        const val PIXMAP_FROM_BUFFER: Byte = 2
        const val PIXMAP_FROM_BUFFERS: Byte = 7
    }

    override val name: String
        get() = "DRI3"

    override val majorOpcode: Byte
        get() = MAJOR_OPCODE

    override val firstErrorId: Byte
        get() = 0

    override val firstEventId: Byte
        get() = 0

    @Throws(IOException::class, XRequestError::class)
    private fun queryVersion(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        inputStream.skip(8)

        outputStream.lock().use {
            outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
            outputStream.writeByte(0.toByte())
            outputStream.writeShort(client.sequenceNumber)
            outputStream.writeInt(0)
            outputStream.writeInt(1)
            outputStream.writeInt(0)
            outputStream.writePad(16)
        }
    }

    @Throws(IOException::class, XRequestError::class)
    private fun open(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        val drawableId = inputStream.readInt()
        inputStream.skip(4)

        client.xServer.drawableManager.getDrawable(drawableId) ?: throw BadDrawable(drawableId)

        outputStream.lock().use {
            outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
            outputStream.writeByte(0.toByte())
            outputStream.writeShort(client.sequenceNumber)
            outputStream.writeInt(0)
            outputStream.writePad(24)
        }
    }

    @Throws(IOException::class, XRequestError::class)
    private fun pixmapFromBuffer(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        val pixmapId = inputStream.readInt()
        val windowId = inputStream.readInt()
        val size = inputStream.readInt()
        val width = inputStream.readShort()
        val height = inputStream.readShort()
        val stride = inputStream.readShort()
        val depth = inputStream.readByte()

        inputStream.skip(1)

        client.xServer.windowManager.getWindow(windowId) ?: throw BadWindow(windowId)

        val pixmap = client.xServer.pixmapManager.getPixmap(pixmapId)
        if (pixmap != null) {
            throw BadIdChoice(pixmapId)
        }

        val fd = inputStream.ancillaryFd
        pixmapFromFd(client, pixmapId, width, height, stride.toInt(), 0, depth, fd, size.toLong())
    }

    @Throws(IOException::class, XRequestError::class)
    private fun pixmapFromBuffers(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        val pixmapId = inputStream.readInt()
        val windowId = inputStream.readInt()

        inputStream.skip(4)

        val width = inputStream.readShort()
        val height = inputStream.readShort()
        val stride = inputStream.readInt()
        val offset = inputStream.readInt()

        inputStream.skip(24)

        val depth = inputStream.readByte()

        inputStream.skip(11)

        client.xServer.windowManager.getWindow(windowId) ?: throw BadWindow(windowId)

        val pixmap = client.xServer.pixmapManager.getPixmap(pixmapId)
        if (pixmap != null) {
            throw BadIdChoice(pixmapId)
        }

        val fd = inputStream.ancillaryFd
        val size = stride.toLong() * height
        pixmapFromFd(client, pixmapId, width, height, stride, offset, depth, fd, size)
    }

    @Throws(IOException::class, XRequestError::class)
    private fun pixmapFromFd(
        client: XClient,
        pixmapId: Int,
        width: Short,
        height: Short,
        stride: Int,
        offset: Int,
        depth: Byte,
        fd: Int,
        size: Long,
    ) {
        try {
            val buffer = SysVSharedMemory.mapSHMSegment(fd, size, offset, true) ?: throw BadAlloc()

            val totalWidth = (stride / 4).toShort()
            val drawable = client.xServer.drawableManager.createDrawable(pixmapId, totalWidth, height, depth)

            drawable.data = buffer
            drawable.texture = null
            drawable.onDestroyListener = onDestroyDrawableListener

            client.xServer.pixmapManager.createPixmap(drawable)
        } finally {
            XConnectorEpoll.closeFd(fd)
        }
    }

    @Throws(IOException::class, XRequestError::class)
    override fun handleRequest(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        val opcode = client.requestData.toInt()
        when (opcode) {
            ClientOpcodes.QUERY_VERSION.toInt() -> queryVersion(client, inputStream, outputStream)
            ClientOpcodes.OPEN.toInt() -> client.xServer.lock(XServer.Lockable.DRAWABLE_MANAGER).use {
                open(client, inputStream, outputStream)
            }

            ClientOpcodes.PIXMAP_FROM_BUFFER.toInt() -> client.xServer.lock(
                XServer.Lockable.WINDOW_MANAGER,
                XServer.Lockable.PIXMAP_MANAGER,
                XServer.Lockable.DRAWABLE_MANAGER,
            ).use {
                pixmapFromBuffer(client, inputStream, outputStream)
            }

            ClientOpcodes.PIXMAP_FROM_BUFFERS.toInt() -> client.xServer.lock(
                XServer.Lockable.WINDOW_MANAGER,
                XServer.Lockable.PIXMAP_MANAGER,
                XServer.Lockable.DRAWABLE_MANAGER,
            ).use {
                pixmapFromBuffers(client, inputStream, outputStream)
            }

            else -> throw BadImplementation()
        }
    }
}
