package com.winlator.xserver.requests

import com.winlator.xconnector.XInputStream
import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.Bitmask
import com.winlator.xserver.Window
import com.winlator.xserver.XClient
import com.winlator.xserver.XClientRequestHandler
import com.winlator.xserver.errors.BadWindow
import com.winlator.xserver.errors.XRequestError
import java.io.IOException

object GrabRequests {
    @Throws(IOException::class, XRequestError::class)
    fun grabPointer(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        if (client.xServer.isRelativeMouseMovement) {
            client.skipRequest()

            outputStream.lock().use {
                outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
                outputStream.writeByte(Status.ALREADY_GRABBED.ordinal.toByte())
                outputStream.writeShort(client.sequenceNumber)
                outputStream.writeInt(0)
                outputStream.writePad(24)
            }

            return
        }

        val ownerEvents = client.requestData.toInt() == 1
        val windowId = inputStream.readInt()

        val window = client.xServer.windowManager.getWindow(windowId)
        if (window == null) {
            throw BadWindow(windowId)
        }

        val eventMask = Bitmask(inputStream.readShort().toInt())

        inputStream.skip(14)

        val status: Status?
        if (client.xServer.grabManager.window != null && client.xServer.grabManager.client != client) {
            status = Status.ALREADY_GRABBED
        } else if (window.mapState != Window.MapState.VIEWABLE) {
            status = Status.NOT_VIEWABLE
        } else {
            status = Status.SUCCESS
            client.xServer.grabManager.activatePointerGrab(window, ownerEvents, eventMask, client)
        }

        outputStream.lock().use {
            outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
            outputStream.writeByte(status.ordinal.toByte())
            outputStream.writeShort(client.sequenceNumber)
            outputStream.writeInt(0)
            outputStream.writePad(24)
        }
    }

    fun ungrabPointer(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        inputStream.skip(4)
        client.xServer.grabManager.deactivatePointerGrab()
    }

    private enum class Status {
        SUCCESS,
        ALREADY_GRABBED,
        INVALID_TIME,
        NOT_VIEWABLE,
        FROZEN,
    }
}
