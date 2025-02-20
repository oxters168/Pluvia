package com.winlator.xserver.extensions

import com.winlator.xconnector.XInputStream
import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.GraphicsContext
import com.winlator.xserver.XClient
import com.winlator.xserver.XClientRequestHandler
import com.winlator.xserver.XServer
import com.winlator.xserver.errors.BadDrawable
import com.winlator.xserver.errors.BadGraphicsContext
import com.winlator.xserver.errors.BadImplementation
import com.winlator.xserver.errors.BadSHMSegment
import com.winlator.xserver.errors.XRequestError
import java.io.IOException

class MITSHMExtension : Extension {

    companion object {
        const val MAJOR_OPCODE: Byte = -101

        @Throws(IOException::class, XRequestError::class)
        private fun queryVersion(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
            outputStream.lock().use {
                outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
                outputStream.writeByte(0.toByte())
                outputStream.writeShort(client.sequenceNumber)
                outputStream.writeInt(0)
                outputStream.writeShort(1.toShort())
                outputStream.writeShort(1.toShort())
                outputStream.writeShort(0.toShort())
                outputStream.writeShort(0.toShort())
                outputStream.writeByte(0.toByte())
            }
        }

        @Throws(IOException::class, XRequestError::class)
        private fun attach(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
            val xid = inputStream.readInt()
            val shmid = inputStream.readInt()

            inputStream.skip(4)

            client.xServer.shmSegmentManager?.attach(xid, shmid)
        }

        @Throws(IOException::class, XRequestError::class)
        private fun detach(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
            client.xServer.shmSegmentManager?.detach(inputStream.readInt())
        }

        @Throws(IOException::class, XRequestError::class)
        private fun putImage(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
            val drawableId = inputStream.readInt()
            val gcId = inputStream.readInt()
            val totalWidth = inputStream.readShort()
            val totalHeight = inputStream.readShort()
            val srcX = inputStream.readShort()
            val srcY = inputStream.readShort()
            val srcWidth = inputStream.readShort()
            val srcHeight = inputStream.readShort()
            val dstX = inputStream.readShort()
            val dstY = inputStream.readShort()
            val depth = inputStream.readByte()

            inputStream.skip(3)

            val shmseg = inputStream.readInt()

            inputStream.skip(4)

            val drawable = client.xServer.drawableManager.getDrawable(drawableId) ?: throw BadDrawable(drawableId)

            val graphicsContext = client.xServer.graphicsContextManager.getGraphicsContext(gcId) ?: throw BadGraphicsContext(gcId)

            val data = client.xServer.shmSegmentManager?.getData(shmseg) ?: throw BadSHMSegment(shmseg)

            if (graphicsContext.function != GraphicsContext.Function.COPY) {
                throw UnsupportedOperationException("GC Function other than COPY is not supported.")
            }

            drawable.drawImage(srcX, srcY, dstX, dstY, srcWidth, srcHeight, depth, data, totalWidth, totalHeight)
        }
    }

    private object ClientOpcodes {
        const val QUERY_VERSION: Byte = 0
        const val ATTACH: Byte = 1
        const val DETACH: Byte = 2
        const val PUT_IMAGE: Byte = 3
    }

    override val name: String
        get() = "MIT-SHM"

    override val majorOpcode: Byte
        get() = MAJOR_OPCODE

    override val firstErrorId: Byte
        get() = Byte.MIN_VALUE

    override val firstEventId: Byte
        get() = 64

    @Throws(IOException::class, XRequestError::class)
    override fun handleRequest(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        val opcode = client.requestData.toInt()
        when (opcode) {
            ClientOpcodes.QUERY_VERSION.toInt() -> queryVersion(client, inputStream, outputStream)
            ClientOpcodes.ATTACH.toInt() -> client.xServer.lock(XServer.Lockable.SHMSEGMENT_MANAGER).use {
                attach(client, inputStream, outputStream)
            }

            ClientOpcodes.DETACH.toInt() -> client.xServer.lock(XServer.Lockable.SHMSEGMENT_MANAGER).use {
                detach(client, inputStream, outputStream)
            }

            ClientOpcodes.PUT_IMAGE.toInt() -> client.xServer.lock(
                XServer.Lockable.SHMSEGMENT_MANAGER,
                XServer.Lockable.DRAWABLE_MANAGER,
                XServer.Lockable.GRAPHIC_CONTEXT_MANAGER,
            ).use {
                putImage(client, inputStream, outputStream)
            }

            else -> throw BadImplementation()
        }
    }
}
