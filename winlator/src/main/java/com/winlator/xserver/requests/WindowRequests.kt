package com.winlator.xserver.requests

import com.winlator.xconnector.XInputStream
import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.Bitmask
import com.winlator.xserver.Property
import com.winlator.xserver.Window
import com.winlator.xserver.WindowAttributes
import com.winlator.xserver.WindowAttributes.WindowClass
import com.winlator.xserver.WindowManager.FocusRevertTo
import com.winlator.xserver.XClient
import com.winlator.xserver.XClientRequestHandler
import com.winlator.xserver.errors.BadAccess
import com.winlator.xserver.errors.BadDrawable
import com.winlator.xserver.errors.BadIdChoice
import com.winlator.xserver.errors.BadMatch
import com.winlator.xserver.errors.BadValue
import com.winlator.xserver.errors.BadWindow
import com.winlator.xserver.errors.XRequestError
import com.winlator.xserver.events.CreateNotify
import com.winlator.xserver.events.Event
import com.winlator.xserver.events.RawEvent
import java.io.IOException
import kotlin.math.min

object WindowRequests {
    @Throws(XRequestError::class)
    fun createWindow(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        val depth = client.requestData
        val windowId = inputStream.readInt()
        val parentId = inputStream.readInt()

        if (!client.isValidResourceId(windowId)) {
            throw BadIdChoice(windowId)
        }

        val parent = client.xServer.windowManager.getWindow(parentId)
        if (parent == null) {
            throw BadWindow(parentId)
        }

        val x = inputStream.readShort()
        val y = inputStream.readShort()
        val width = inputStream.readShort()
        val height = inputStream.readShort()
        val borderWidth = inputStream.readShort()
        val windowClass = WindowClass.entries.toTypedArray()[inputStream.readShort().toByte().toInt()]
        val visual = client.xServer.pixmapManager.getVisual(inputStream.readInt())
        val valueMask = Bitmask(inputStream.readInt())

        val window = client.xServer.windowManager.createWindow(windowId, parent, x, y, width, height, windowClass, visual, depth, client)
        window.borderWidth = borderWidth

        if (!valueMask.isEmpty) {
            window.attributes.update(valueMask, inputStream, client)
        }

        client.setEventListenerForWindow(window, window.attributes.eventMask)
        client.registerAsOwnerOfResource(window)

        parent.sendEvent(Event.SUBSTRUCTURE_NOTIFY, CreateNotify(parent, window))
    }

    @Throws(IOException::class, XRequestError::class)
    fun getWindowAttributes(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        val windowId = inputStream.readInt()
        val window = client.xServer.windowManager.getWindow(windowId)
        if (window == null) {
            throw BadWindow(windowId)
        }

        outputStream.lock().use {
            outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
            outputStream.writeByte(window.attributes.backingStore!!.ordinal.toByte())
            outputStream.writeShort(client.sequenceNumber)
            outputStream.writeInt(3)
            outputStream.writeInt(if (window.isInputOutput) window.content!!.visual!!.id else 0)
            outputStream.writeShort(window.attributes.windowClass!!.ordinal.toShort())
            outputStream.writeByte(window.attributes.bitGravity!!.ordinal.toByte())
            outputStream.writeByte(window.attributes.winGravity!!.ordinal.toByte())
            outputStream.writeInt(window.attributes.backingPlanes)
            outputStream.writeInt(window.attributes.backingPixel)
            outputStream.writeByte((if (window.attributes.isSaveUnder) 1 else 0).toByte())
            outputStream.writeByte(1.toByte())
            outputStream.writeByte(window.mapState.ordinal.toByte())
            outputStream.writeByte((if (window.attributes.isOverrideRedirect) 1 else 0).toByte())
            outputStream.writeInt(0)
            outputStream.writeInt(window.allEventMasks.bits)
            outputStream.writeInt(client.getEventMaskForWindow(window).bits)
            outputStream.writeShort(window.attributes.doNotPropagateMask.bits.toShort())
            outputStream.writeShort(0.toShort())
        }
    }

    @Throws(XRequestError::class)
    fun changeWindowAttributes(
        client: XClient,
        inputStream: XInputStream,
        outputStream: XOutputStream?,
    ) {
        val windowId = inputStream.readInt()
        val valueMask = Bitmask(inputStream.readInt())
        val window = client.xServer.windowManager.getWindow(windowId)
        if (window == null) {
            throw BadWindow(windowId)
        }

        if (!valueMask.isEmpty) {
            window.attributes.update(valueMask, inputStream, client)

            if (valueMask.isSet(WindowAttributes.FLAG_EVENT_MASK)) {
                if (isClientCanSelectFor(Event.SUBSTRUCTURE_REDIRECT, window, client) &&
                    isClientCanSelectFor(Event.RESIZE_REDIRECT, window, client) &&
                    isClientCanSelectFor(Event.BUTTON_PRESS, window, client)
                ) {
                    client.setEventListenerForWindow(window, window.attributes.eventMask)
                } else {
                    throw BadAccess()
                }
            }
        }
    }

    private fun isClientCanSelectFor(eventId: Int, window: Window, client: XClient): Boolean {
        return !window.attributes.eventMask.isSet(eventId) ||
            !(window.hasEventListenerFor(eventId) && !client.isInterestedIn(eventId, window))
    }

    fun destroyWindow(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        client.xServer.windowManager.destroyWindow(inputStream.readInt())
    }

    @Throws(XRequestError::class)
    fun reparentWindow(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        val windowId = inputStream.readInt()
        val parentId = inputStream.readInt()

        inputStream.skip(4)

        val window = client.xServer.windowManager.getWindow(windowId)
        if (window == null) {
            throw BadWindow(windowId)
        }

        val parent = client.xServer.windowManager.getWindow(parentId)
        if (parent == null) {
            throw BadWindow(parentId)
        }

        client.xServer.windowManager.reparentWindow(window, parent)
    }

    @Throws(XRequestError::class)
    fun mapWindow(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        val windowId = inputStream.readInt()
        val window = client.xServer.windowManager.getWindow(windowId)
        if (window == null) {
            throw BadWindow(windowId)
        }

        client.xServer.windowManager.mapWindow(window)
    }

    @Throws(XRequestError::class)
    fun unmapWindow(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        val windowId = inputStream.readInt()
        val window = client.xServer.windowManager.getWindow(windowId)
        if (window == null) {
            throw BadWindow(windowId)
        }

        client.xServer.windowManager.unmapWindow(window)
    }

    @Throws(XRequestError::class)
    fun changeProperty(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        val mode: Property.Mode? = Property.Mode.entries.toTypedArray()[client.requestData.toInt()]

        val windowId = inputStream.readInt()
        val window = client.xServer.windowManager.getWindow(windowId)
        if (window == null) {
            throw BadWindow(windowId)
        }

        val atom = inputStream.readInt()
        val type = inputStream.readInt()
        val format = inputStream.readByte()

        inputStream.skip(3)

        val length = inputStream.readInt()
        val totalSize = length * (format.toInt() shr 3)

        var data: ByteArray? = null
        if (totalSize > 0) {
            data = ByteArray(totalSize)
            inputStream.read(data)
            inputStream.skip(-totalSize and 3)
        }

        val property = window.modifyProperty(atom, type, Property.Format.valueOf(format.toInt())!!, mode, data!!)
        if (property == null) {
            throw BadMatch()
        }

        client.xServer.windowManager.triggerOnModifyWindowProperty(window, property)
    }

    @Throws(XRequestError::class)
    fun deleteProperty(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        val windowId = inputStream.readInt()

        val window = client.xServer.windowManager.getWindow(windowId)
        if (window == null) {
            throw BadWindow(windowId)
        }

        window.removeProperty(inputStream.readInt())
    }

    @Throws(IOException::class, XRequestError::class)
    fun getProperty(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        val delete = client.requestData.toInt() == 1
        val sequenceNumber = client.sequenceNumber
        val windowId = inputStream.readInt()

        val window = client.xServer.windowManager.getWindow(windowId)
        if (window == null) {
            throw BadWindow(windowId)
        }

        val atom = inputStream.readInt()
        val type = inputStream.readInt()
        val longOffset = inputStream.readInt()
        val longLength = inputStream.readInt()
        val property = window.getProperty(atom)

        var bytesAfter = 0
        outputStream.lock().use {
            if (property == null) {
                outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
                outputStream.writeByte(0.toByte())
                outputStream.writeShort(sequenceNumber)
                outputStream.writeInt(0)
                outputStream.writeInt(0)
                outputStream.writeInt(0)
                outputStream.writeInt(0)
                outputStream.writePad(12)
            } else if (property.type != type && type != 0) {
                outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
                outputStream.writeByte(property.format.value)
                outputStream.writeShort(sequenceNumber)
                outputStream.writeInt(0)
                outputStream.writeInt(property.type)
                outputStream.writeInt(0)
                outputStream.writeInt(0)
                outputStream.writePad(12)
            } else {
                val data = property.data!!.array()
                val offset = longOffset * 4

                val length = min((data.size - offset).toDouble(), (longLength * 4).toDouble()).toInt()
                if (length < 0) {
                    throw BadValue(longOffset)
                }

                bytesAfter = data.size - (offset + length)

                outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
                outputStream.writeByte(property.format.value)
                outputStream.writeShort(sequenceNumber)
                outputStream.writeInt((length + 3) / 4)
                outputStream.writeInt(property.type)
                outputStream.writeInt(bytesAfter)
                outputStream.writeInt(length / (property.format.value / 8))
                outputStream.writePad(12)
                outputStream.write(data, offset, length)

                if ((-length and 3) > 0) {
                    outputStream.writePad(-length and 3)
                }
            }
        }

        if (delete && property != null && bytesAfter == 0) {
            window.removeProperty(atom)
        }
    }

    @Throws(IOException::class, XRequestError::class)
    fun queryPointer(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        val windowId = inputStream.readInt()

        val window = client.xServer.windowManager.getWindow(windowId)
        if (window == null) {
            throw BadWindow(windowId)
        }

        val rootX = client.xServer.pointer.clampedX
        val rootY = client.xServer.pointer.clampedY
        val child = window.getChildByCoords(rootX, rootY)
        val localPoint = window.rootPointToLocal(rootX, rootY)

        outputStream.lock().use {
            outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
            outputStream.writeByte((if (!client.xServer.isRelativeMouseMovement) 1 else 0).toByte())
            outputStream.writeShort(client.sequenceNumber)
            outputStream.writeInt(0)
            outputStream.writeInt(client.xServer.windowManager.rootWindow.id)
            outputStream.writeInt(child?.id ?: 0)
            outputStream.writeShort(rootX)
            outputStream.writeShort(rootY)
            outputStream.writeShort(localPoint[0])
            outputStream.writeShort(localPoint[1])
            outputStream.writeShort(client.xServer.inputDeviceManager.keyButMask.bits.toShort())
            outputStream.writePad(6)
        }
    }

    @Throws(IOException::class, XRequestError::class)
    fun translateCoordinates(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        val srcWindowId = inputStream.readInt()
        val dstWindowId = inputStream.readInt()
        val srcX = inputStream.readShort()
        val srcY = inputStream.readShort()

        val srcWindow = client.xServer.windowManager.getWindow(srcWindowId)
        if (srcWindow == null) {
            throw BadWindow(srcWindowId)
        }

        val dstWindow = client.xServer.windowManager.getWindow(dstWindowId)
        if (dstWindow == null) {
            throw BadWindow(dstWindowId)
        }

        val rootPoint = srcWindow.localPointToRoot(srcX, srcY)
        val localPoint = dstWindow.rootPointToLocal(rootPoint!![0], rootPoint[1])
        val child = dstWindow.getChildByCoords(rootPoint[0], rootPoint[1])

        outputStream.lock().use {
            outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
            outputStream.writeByte(1.toByte())
            outputStream.writeShort(client.sequenceNumber)
            outputStream.writeInt(0)
            outputStream.writeInt(child?.id ?: 0)
            outputStream.writeShort(localPoint[0])
            outputStream.writeShort(localPoint[1])
            outputStream.writePad(16)
        }
    }

    @Throws(IOException::class, XRequestError::class)
    fun warpPointer(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        if (client.xServer.isRelativeMouseMovement) {
            client.skipRequest()
            return
        }

        val srcWindow = client.xServer.windowManager.getWindow(inputStream.readInt())
        val dstWindow = client.xServer.windowManager.getWindow(inputStream.readInt())
        val srcX = inputStream.readShort()
        val srcY = inputStream.readShort()
        var srcWidth = inputStream.readShort()
        var srcHeight = inputStream.readShort()
        val dstX = inputStream.readShort()
        val dstY = inputStream.readShort()

        if (srcWindow != null) {
            if (srcWidth.toInt() == 0) {
                srcWidth = (srcWindow.width - srcX).toShort()
            }

            if (srcHeight.toInt() == 0) {
                srcHeight = (srcWindow.height - srcY).toShort()
            }

            val localPoint = srcWindow.rootPointToLocal(client.xServer.pointer.x, client.xServer.pointer.y)
            val isContained = localPoint[0] >= srcX &&
                localPoint[1] >= srcY &&
                localPoint[0] < (srcX + srcWidth) &&
                localPoint[1] < (srcY + srcHeight)
            if (!isContained) {
                return
            }
        }

        if (dstWindow == null) {
            client.xServer.pointer.setX(client.xServer.pointer.x + dstX)
            client.xServer.pointer.setY(client.xServer.pointer.y + dstY)
        } else {
            val localPoint = dstWindow.localPointToRoot(dstX, dstY)
            client.xServer.pointer.setX(localPoint!![0].toInt())
            client.xServer.pointer.setY(localPoint[1].toInt())
        }
    }

    @Throws(XRequestError::class)
    fun setInputFocus(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        val focusRevertTo: FocusRevertTo = FocusRevertTo.entries.toTypedArray()[client.requestData.toInt()]

        val windowId = inputStream.readInt()

        inputStream.skip(4)

        when (focusRevertTo) {
            FocusRevertTo.NONE -> client.xServer.windowManager.setFocus(null, focusRevertTo)
            FocusRevertTo.POINTER_ROOT -> client.xServer.windowManager.setFocus(client.xServer.windowManager.rootWindow, focusRevertTo)

            FocusRevertTo.PARENT -> {
                val window = client.xServer.windowManager.getWindow(windowId)
                if (window == null) {
                    throw BadWindow(windowId)
                }

                client.xServer.windowManager.setFocus(window, focusRevertTo)
            }
        }
    }

    @Throws(IOException::class, XRequestError::class)
    fun getInputFocus(client: XClient, inputStream: XInputStream?, outputStream: XOutputStream) {
        val focusedWindow = client.xServer.windowManager.focusedWindow

        outputStream.lock().use {
            outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
            outputStream.writeByte(client.xServer.windowManager.focusRevertTo.ordinal.toByte())
            outputStream.writeShort(client.sequenceNumber)
            outputStream.writeInt(0)
            outputStream.writeInt(focusedWindow?.id ?: 0)
            outputStream.writePad(20)
        }
    }

    @Throws(XRequestError::class)
    fun configureWindow(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        val windowId = inputStream.readInt()

        val window = client.xServer.windowManager.getWindow(windowId)
        if (window == null) {
            throw BadWindow(windowId)
        }

        val valueMask = Bitmask(inputStream.readShort().toInt())

        inputStream.skip(2)

        if (!valueMask.isEmpty) {
            client.xServer.windowManager.configureWindow(window, valueMask, inputStream)
        }
    }

    @Throws(IOException::class, XRequestError::class)
    fun getGeometry(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        val drawableId = inputStream.readInt()

        val drawable = client.xServer.drawableManager.getDrawable(drawableId)
        if (drawable == null) {
            throw BadDrawable(drawableId)
        }

        val window = client.xServer.windowManager.getWindow(drawableId)
        val x = window?.x ?: 0
        val y = window?.y ?: 0
        val borderWidth = window?.borderWidth ?: 0

        outputStream.lock().use { lock ->
            outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
            outputStream.writeByte(drawable.visual!!.depth)
            outputStream.writeShort(client.sequenceNumber)
            outputStream.writeInt(0)
            outputStream.writeInt(client.xServer.windowManager.rootWindow.id)
            outputStream.writeShort(x)
            outputStream.writeShort(y)
            outputStream.writeShort(drawable.width)
            outputStream.writeShort(drawable.height)
            outputStream.writeShort(borderWidth)
            outputStream.writePad(10)
        }
    }

    @Throws(IOException::class, XRequestError::class)
    fun queryTree(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        val windowId = inputStream.readInt()

        val window = client.xServer.windowManager.getWindow(windowId)
        if (window == null) {
            throw BadWindow(windowId)
        }

        val parent = window.parent
        val children = window.children

        outputStream.lock().use { lock ->
            outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
            outputStream.writeByte(0.toByte())
            outputStream.writeShort(client.sequenceNumber)
            outputStream.writeInt(children.size)
            outputStream.writeInt(client.xServer.windowManager.rootWindow.id)
            outputStream.writeInt(parent?.id ?: 0)
            outputStream.writeShort(children.size.toShort())
            outputStream.writePad(14)

            for (i in children.indices.reversed()) {
                outputStream.writeInt(children[i].id)
            }
        }
    }

    @Throws(IOException::class, XRequestError::class)
    fun sendEvent(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        val windowId = inputStream.readInt()

        if (windowId == 0 || windowId == 1) {
            client.skipRequest()
            return
        }

        val destination = client.xServer.windowManager.getWindow(windowId)
        if (destination == null) {
            throw BadWindow(windowId)
        }

        val eventMask = Bitmask(inputStream.readInt())

        val data = ByteArray(32)

        inputStream.read(data)

        val event: Event = RawEvent(data)

        if (eventMask.isEmpty) {
            destination.originClient?.sendEvent(event)
        } else {
            destination.sendEvent(eventMask, event)
        }
    }

    @Throws(IOException::class, XRequestError::class)
    fun getScreenSaver(client: XClient, inputStream: XInputStream?, outputStream: XOutputStream) {
        outputStream.lock().use {
            outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
            outputStream.writeByte(0.toByte())
            outputStream.writeShort(client.sequenceNumber)
            outputStream.writeInt(0)
            outputStream.writeShort(600.toShort())
            outputStream.writeShort(600.toShort())
            outputStream.writeByte(1.toByte())
            outputStream.writeByte(1.toByte())
            outputStream.writePad(18)
        }
    }
}
