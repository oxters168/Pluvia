package com.winlator.xserver.requests

import com.winlator.xconnector.XInputStream
import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.Bitmask
import com.winlator.xserver.XClient
import com.winlator.xserver.errors.BadDrawable
import com.winlator.xserver.errors.BadGraphicsContext
import com.winlator.xserver.errors.BadIdChoice
import com.winlator.xserver.errors.XRequestError

object GraphicsContextRequests {
    @Throws(XRequestError::class)
    fun createGC(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        val gcId = inputStream.readInt()
        val drawableId = inputStream.readInt()
        val valueMask = Bitmask(inputStream.readInt())

        if (!client.isValidResourceId(gcId)) throw BadIdChoice(gcId)

        val drawable = client.xServer.drawableManager.getDrawable(drawableId)
        if (drawable == null) {
            throw BadDrawable(drawableId)
        }

        val graphicsContext = client.xServer.graphicsContextManager.createGraphicsContext(gcId, drawable)
        if (graphicsContext == null) {
            throw BadIdChoice(gcId)
        }

        client.registerAsOwnerOfResource(graphicsContext)
        if (!valueMask.isEmpty) {
            client.xServer.graphicsContextManager.updateGraphicsContext(graphicsContext, valueMask, inputStream)
        }
    }

    @Throws(XRequestError::class)
    fun changeGC(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        val gcId = inputStream.readInt()
        val valueMask = Bitmask(inputStream.readInt())

        val graphicsContext = client.xServer.graphicsContextManager.getGraphicsContext(gcId)
        if (graphicsContext == null) {
            throw BadGraphicsContext(gcId)
        }

        if (!valueMask.isEmpty) {
            client.xServer.graphicsContextManager.updateGraphicsContext(graphicsContext, valueMask, inputStream)
        }
    }

    @Throws(XRequestError::class)
    fun freeGC(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        client.xServer.graphicsContextManager.freeGraphicsContext(inputStream.readInt())
    }
}
