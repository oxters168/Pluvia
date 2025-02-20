package com.winlator.xserver.requests

import com.winlator.xconnector.XInputStream
import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.XClient
import com.winlator.xserver.errors.BadDrawable
import com.winlator.xserver.errors.BadIdChoice
import com.winlator.xserver.errors.XRequestError

object PixmapRequests {
    @Throws(XRequestError::class)
    fun createPixmap(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        val depth = client.requestData
        val pixmapId = inputStream.readInt()
        val drawableId = inputStream.readInt()
        val width = inputStream.readShort()
        val height = inputStream.readShort()

        if (!client.isValidResourceId(pixmapId)) {
            throw BadIdChoice(pixmapId)
        }

        val drawable = client.xServer.drawableManager.getDrawable(drawableId)
        if (drawable == null) {
            throw BadDrawable(drawableId)
        }

        val backingStore = client.xServer.drawableManager.createDrawable(pixmapId, width, height, depth)
        if (backingStore == null) {
            throw BadIdChoice(pixmapId)
        }

        val pixmap = client.xServer.pixmapManager.createPixmap(backingStore)
        if (pixmap == null) {
            throw BadIdChoice(pixmapId)
        }

        client.registerAsOwnerOfResource(pixmap)
    }

    @Throws(XRequestError::class)
    fun freePixmap(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        client.xServer.pixmapManager.freePixmap(inputStream.readInt())
    }
}
