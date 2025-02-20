package com.winlator.xserver.requests

import com.winlator.xconnector.XInputStream
import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.XClient
import com.winlator.xserver.errors.BadIdChoice
import com.winlator.xserver.errors.BadMatch
import com.winlator.xserver.errors.BadPixmap
import com.winlator.xserver.errors.XRequestError

object CursorRequests {
    @Throws(XRequestError::class)
    fun createCursor(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        val cursorId = inputStream.readInt()
        val sourcePixmapId = inputStream.readInt()
        val maskPixmapId = inputStream.readInt()

        if (!client.isValidResourceId(cursorId)) {
            throw BadIdChoice(cursorId)
        }

        val sourcePixmap = client.xServer.pixmapManager.getPixmap(sourcePixmapId)
        if (sourcePixmap == null) {
            throw BadPixmap(sourcePixmapId)
        }

        val maskPixmap = client.xServer.pixmapManager.getPixmap(maskPixmapId)
        if (maskPixmap != null &&
            (
                maskPixmap.drawable.visual?.depth?.toInt() != 1 ||
                    maskPixmap.drawable.width != sourcePixmap.drawable.width ||
                    maskPixmap.drawable.height != sourcePixmap.drawable.height
                )
        ) {
            throw BadMatch()
        }

        val foreRed = inputStream.readShort().toByte()
        val foreGreen = inputStream.readShort().toByte()
        val foreBlue = inputStream.readShort().toByte()
        val backRed = inputStream.readShort().toByte()
        val backGreen = inputStream.readShort().toByte()
        val backBlue = inputStream.readShort().toByte()
        val x = inputStream.readShort()
        val y = inputStream.readShort()

        val cursor = client.xServer.cursorManager.createCursor(cursorId, x, y, sourcePixmap, maskPixmap)
        if (cursor == null) {
            throw BadIdChoice(cursorId)
        }

        client.xServer.cursorManager.recolorCursor(cursor, foreRed, foreGreen, foreBlue, backRed, backGreen, backBlue)
        client.registerAsOwnerOfResource(cursor)
    }

    @Throws(XRequestError::class)
    fun freeCursor(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        client.xServer.cursorManager.freeCursor(inputStream.readInt())
    }
}
