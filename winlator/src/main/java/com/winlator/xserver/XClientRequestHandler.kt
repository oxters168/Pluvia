package com.winlator.xserver

import com.winlator.xconnector.Client
import com.winlator.xconnector.RequestHandler
import com.winlator.xconnector.XInputStream
import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.errors.XRequestError
import com.winlator.xserver.requests.AtomRequests.internAtom
import com.winlator.xserver.requests.CursorRequests.createCursor
import com.winlator.xserver.requests.CursorRequests.freeCursor
import com.winlator.xserver.requests.DrawRequests.copyArea
import com.winlator.xserver.requests.DrawRequests.getImage
import com.winlator.xserver.requests.DrawRequests.polyFillRectangle
import com.winlator.xserver.requests.DrawRequests.polyLine
import com.winlator.xserver.requests.DrawRequests.putImage
import com.winlator.xserver.requests.ExtensionRequests.queryExtension
import com.winlator.xserver.requests.FontRequests.listFonts
import com.winlator.xserver.requests.FontRequests.openFont
import com.winlator.xserver.requests.GrabRequests.grabPointer
import com.winlator.xserver.requests.GrabRequests.ungrabPointer
import com.winlator.xserver.requests.GraphicsContextRequests.changeGC
import com.winlator.xserver.requests.GraphicsContextRequests.createGC
import com.winlator.xserver.requests.GraphicsContextRequests.freeGC
import com.winlator.xserver.requests.KeyboardRequests.getKeyboardMapping
import com.winlator.xserver.requests.KeyboardRequests.getModifierMapping
import com.winlator.xserver.requests.PixmapRequests.createPixmap
import com.winlator.xserver.requests.PixmapRequests.freePixmap
import com.winlator.xserver.requests.SelectionRequests.getSelectionOwner
import com.winlator.xserver.requests.SelectionRequests.setSelectionOwner
import com.winlator.xserver.requests.WindowRequests.changeProperty
import com.winlator.xserver.requests.WindowRequests.changeWindowAttributes
import com.winlator.xserver.requests.WindowRequests.configureWindow
import com.winlator.xserver.requests.WindowRequests.createWindow
import com.winlator.xserver.requests.WindowRequests.deleteProperty
import com.winlator.xserver.requests.WindowRequests.destroyWindow
import com.winlator.xserver.requests.WindowRequests.getGeometry
import com.winlator.xserver.requests.WindowRequests.getInputFocus
import com.winlator.xserver.requests.WindowRequests.getProperty
import com.winlator.xserver.requests.WindowRequests.getScreenSaver
import com.winlator.xserver.requests.WindowRequests.getWindowAttributes
import com.winlator.xserver.requests.WindowRequests.mapWindow
import com.winlator.xserver.requests.WindowRequests.queryPointer
import com.winlator.xserver.requests.WindowRequests.queryTree
import com.winlator.xserver.requests.WindowRequests.reparentWindow
import com.winlator.xserver.requests.WindowRequests.sendEvent
import com.winlator.xserver.requests.WindowRequests.setInputFocus
import com.winlator.xserver.requests.WindowRequests.translateCoordinates
import com.winlator.xserver.requests.WindowRequests.unmapWindow
import com.winlator.xserver.requests.WindowRequests.warpPointer
import java.io.IOException
import java.nio.ByteOrder

class XClientRequestHandler : RequestHandler {

    companion object {
        const val RESPONSE_CODE_ERROR: Byte = 0
        const val RESPONSE_CODE_SUCCESS: Byte = 1
        const val MAX_REQUEST_LENGTH: Int = 65535
    }

    @Throws(IOException::class)
    override fun handleRequest(client: Client?): Boolean {
        val xClient = client!!.tag as XClient
        val inputStream = client.inputStream
        val outputStream = client.outputStream

        return if (xClient.isAuthenticated) {
            handleNormalRequest(xClient, inputStream!!, outputStream!!)
        } else {
            handleAuthRequest(xClient, inputStream!!, outputStream!!)
        }
    }

    @Throws(IOException::class)
    private fun sendServerInformation(client: XClient, outputStream: XOutputStream) {
        val vendorNameLength = XServer.VENDOR_NAME.length.toShort()
        val pixmapFormatCount = client.xServer.pixmapManager.supportedPixmapFormats.size.toByte()
        val additionalDataLength = (
            8 + (2 * pixmapFormatCount) + ((vendorNameLength + 3) / 4) +
                ((40 + 8 * client.xServer.pixmapManager.supportedVisuals.size + 24) + 3) / 4
            ).toShort()

        outputStream.lock().use {
            outputStream.writeByte(RESPONSE_CODE_SUCCESS)
            outputStream.writeByte(0.toByte())
            outputStream.writeShort(XServer.VERSION)
            outputStream.writeShort(0.toShort())
            outputStream.writeShort(additionalDataLength)
            outputStream.writeInt(1)
            outputStream.writeInt(client.resourceIDBase)
            outputStream.writeInt(client.xServer.resourceIDs.idMask)
            outputStream.writeInt(256)
            outputStream.writeShort(vendorNameLength)
            outputStream.writeShort(MAX_REQUEST_LENGTH.toShort())
            outputStream.writeByte(1.toByte())
            outputStream.writeByte(pixmapFormatCount)
            outputStream.writeByte(0.toByte())
            outputStream.writeByte(0.toByte())
            outputStream.writeByte(32.toByte())
            outputStream.writeByte(32.toByte())
            outputStream.writeByte(Keyboard.MIN_KEYCODE.toByte())
            outputStream.writeByte(Keyboard.MAX_KEYCODE.toByte())
            outputStream.writeInt(0)
            outputStream.writeString8(XServer.VENDOR_NAME)

            client.xServer.pixmapManager.supportedPixmapFormats.forEach { pixmapFormat ->
                outputStream.writeByte(pixmapFormat.depth)
                outputStream.writeByte(pixmapFormat.bitsPerPixel)
                outputStream.writeByte(pixmapFormat.scanlinePad)
                outputStream.writePad(5)
            }

            val rootVisual = client.xServer.windowManager.rootWindow.content?.visual

            outputStream.writeInt(client.xServer.windowManager.rootWindow.id)
            outputStream.writeInt(0)
            outputStream.writeInt(0xffffff)
            outputStream.writeInt(0x000000)
            outputStream.writeInt(client.xServer.windowManager.rootWindow.allEventMasks.bits)
            outputStream.writeShort(client.xServer.screenInfo.width)
            outputStream.writeShort(client.xServer.screenInfo.height)
            outputStream.writeShort(client.xServer.screenInfo.widthInMillimeters)
            outputStream.writeShort(client.xServer.screenInfo.heightInMillimeters)
            outputStream.writeShort(1.toShort())
            outputStream.writeShort(1.toShort())
            outputStream.writeInt(rootVisual!!.id)
            outputStream.writeByte(0.toByte())
            outputStream.writeByte(0.toByte())
            outputStream.writeByte(rootVisual.depth)
            outputStream.writeByte(client.xServer.pixmapManager.supportedVisuals.size.toByte())

            client.xServer.pixmapManager.supportedVisuals.forEach { visual ->
                outputStream.writeByte(visual.depth)
                outputStream.writeByte(0.toByte())
                outputStream.writeShort((if (visual.displayable) 1 else 0).toShort())
                outputStream.writeInt(0)

                if (visual.displayable) {
                    outputStream.writeInt(visual.id)
                    outputStream.writeByte(visual.visualClass)
                    outputStream.writeByte(visual.bitsPerRGBValue)
                    outputStream.writeShort(visual.colormapEntries)
                    outputStream.writeInt(visual.redMask)
                    outputStream.writeInt(visual.greenMask)
                    outputStream.writeInt(visual.blueMask)
                    outputStream.writeInt(0)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun handleAuthRequest(client: XClient, inputStream: XInputStream, outputStream: XOutputStream): Boolean {
        if (inputStream.available() < 12) {
            return false
        }

        val byteOrder = inputStream.readByte()
        if (byteOrder.toInt() == 66) {
            inputStream.setByteOrder(ByteOrder.BIG_ENDIAN)
            outputStream.setByteOrder(ByteOrder.BIG_ENDIAN)
        } else if (byteOrder.toInt() == 108) {
            inputStream.setByteOrder(ByteOrder.LITTLE_ENDIAN)
            outputStream.setByteOrder(ByteOrder.LITTLE_ENDIAN)
        }

        inputStream.skip(1)

        val majorVersion = inputStream.readShort()
        if (majorVersion.toInt() != 11) {
            throw UnsupportedOperationException("Unsupported major X protocol version $majorVersion.")
        }

        inputStream.skip(2)

        val nameLength = inputStream.readShort().toInt()
        val dataLength = inputStream.readShort().toInt()

        inputStream.skip(2)

        if (nameLength > 0) {
            inputStream.readString8(nameLength)
        }
        if (dataLength > 0) {
            inputStream.readString8(dataLength)
        }

        client.xServer.lock(XServer.Lockable.WINDOW_MANAGER).use {
            sendServerInformation(client, outputStream)
        }
        client.isAuthenticated = true

        return true
    }

    @Throws(IOException::class)
    private fun handleNormalRequest(client: XClient, inputStream: XInputStream, outputStream: XOutputStream): Boolean {
        if (inputStream.available() < 4) {
            return false
        }

        val opcode = inputStream.readByte()
        val requestData = inputStream.readByte()

        var requestLength = inputStream.readUnsignedShort()

        requestLength = if (requestLength != 0) {
            requestLength * 4 - 4
        } else if (inputStream.available() < 4) {
            return false
        } else {
            inputStream.readInt() * 4 - 8
        }

        if (inputStream.available() < requestLength) {
            return false
        }

        client.generateSequenceNumber()
        client.requestData = requestData
        client.setRequestLength(requestLength)

        try {
            when (opcode) {
                ClientOpcodes.CREATE_WINDOW ->
                    client.xServer.lock(
                        XServer.Lockable.WINDOW_MANAGER,
                        XServer.Lockable.DRAWABLE_MANAGER,
                        XServer.Lockable.INPUT_DEVICE,
                        XServer.Lockable.CURSOR_MANAGER,
                    ).use {
                        createWindow(client, inputStream, outputStream)
                    }

                ClientOpcodes.CHANGE_WINDOW_ATTRIBUTES ->
                    client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.CURSOR_MANAGER).use {
                        changeWindowAttributes(client, inputStream, outputStream)
                    }

                ClientOpcodes.GET_WINDOW_ATTRIBUTES ->
                    client.xServer.lock(XServer.Lockable.WINDOW_MANAGER).use {
                        getWindowAttributes(client, inputStream, outputStream)
                    }

                ClientOpcodes.DESTROY_WINDOW ->
                    client.xServer.lock(
                        XServer.Lockable.WINDOW_MANAGER,
                        XServer.Lockable.DRAWABLE_MANAGER,
                        XServer.Lockable.INPUT_DEVICE,
                    ).use {
                        destroyWindow(client, inputStream, outputStream)
                    }

                ClientOpcodes.REPARENT_WINDOW ->
                    client.xServer.lock(XServer.Lockable.WINDOW_MANAGER).use {
                        reparentWindow(client, inputStream, outputStream)
                    }

                ClientOpcodes.MAP_WINDOW ->
                    client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE).use {
                        mapWindow(client, inputStream, outputStream)
                    }

                ClientOpcodes.UNMAP_WINDOW ->
                    client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE).use {
                        unmapWindow(client, inputStream, outputStream)
                    }

                ClientOpcodes.CONFIGURE_WINDOW ->
                    client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE).use {
                        configureWindow(client, inputStream, outputStream)
                    }

                ClientOpcodes.GET_GEOMETRY ->
                    client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.DRAWABLE_MANAGER).use {
                        getGeometry(client, inputStream, outputStream)
                    }

                ClientOpcodes.QUERY_TREE ->
                    client.xServer.lock(XServer.Lockable.WINDOW_MANAGER).use {
                        queryTree(client, inputStream, outputStream)
                    }

                ClientOpcodes.INTERN_ATOM ->
                    internAtom(client, inputStream, outputStream)

                ClientOpcodes.CHANGE_PROPERTY ->
                    client.xServer.lock(XServer.Lockable.WINDOW_MANAGER).use {
                        changeProperty(client, inputStream, outputStream)
                    }

                ClientOpcodes.DELETE_PROPERTY ->
                    client.xServer.lock(XServer.Lockable.WINDOW_MANAGER).use { lock ->
                        deleteProperty(client, inputStream, outputStream)
                    }

                ClientOpcodes.GET_PROPERTY ->
                    client.xServer.lock(XServer.Lockable.WINDOW_MANAGER).use {
                        getProperty(client, inputStream, outputStream)
                    }

                ClientOpcodes.SET_SELECTION_OWNER ->
                    client.xServer.lock(XServer.Lockable.WINDOW_MANAGER).use {
                        setSelectionOwner(client, inputStream, outputStream)
                    }

                ClientOpcodes.GET_SELECTION_OWNER ->
                    client.xServer.lock(XServer.Lockable.WINDOW_MANAGER).use {
                        getSelectionOwner(client, inputStream, outputStream)
                    }

                ClientOpcodes.SEND_EVENT ->
                    client.xServer.lockAll().use {
                        sendEvent(client, inputStream, outputStream)
                    }

                ClientOpcodes.GRAB_POINTER ->
                    client.xServer.lock(
                        XServer.Lockable.WINDOW_MANAGER,
                        XServer.Lockable.INPUT_DEVICE,
                        XServer.Lockable.CURSOR_MANAGER,
                    ).use {
                        grabPointer(client, inputStream, outputStream)
                    }

                ClientOpcodes.UNGRAB_POINTER ->
                    client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE).use {
                        ungrabPointer(client, inputStream, outputStream)
                    }

                ClientOpcodes.QUERY_POINTER ->
                    client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE).use {
                        queryPointer(client, inputStream, outputStream)
                    }

                ClientOpcodes.TRANSLATE_COORDINATES ->
                    client.xServer.lock(XServer.Lockable.WINDOW_MANAGER).use {
                        translateCoordinates(client, inputStream, outputStream)
                    }

                ClientOpcodes.WARP_POINTER ->
                    client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.INPUT_DEVICE).use {
                        warpPointer(client, inputStream, outputStream)
                    }

                ClientOpcodes.SET_INPUT_FOCUS ->
                    client.xServer.lock(XServer.Lockable.WINDOW_MANAGER).use {
                        setInputFocus(client, inputStream, outputStream)
                    }

                ClientOpcodes.GET_INPUT_FOCUS ->
                    client.xServer.lock(XServer.Lockable.WINDOW_MANAGER).use {
                        getInputFocus(client, inputStream, outputStream)
                    }

                ClientOpcodes.OPEN_FONT ->
                    openFont(client, inputStream, outputStream)

                ClientOpcodes.LIST_FONTS ->
                    listFonts(client, inputStream, outputStream)

                ClientOpcodes.CREATE_PIXMAP ->
                    client.xServer.lock(XServer.Lockable.PIXMAP_MANAGER, XServer.Lockable.DRAWABLE_MANAGER).use {
                        createPixmap(client, inputStream, outputStream)
                    }

                ClientOpcodes.FREE_PIXMAP ->
                    client.xServer.lock(XServer.Lockable.PIXMAP_MANAGER, XServer.Lockable.DRAWABLE_MANAGER).use {
                        freePixmap(client, inputStream, outputStream)
                    }

                ClientOpcodes.CREATE_GC -> client.xServer.lock(
                    XServer.Lockable.PIXMAP_MANAGER,
                    XServer.Lockable.DRAWABLE_MANAGER,
                    XServer.Lockable.GRAPHIC_CONTEXT_MANAGER,
                ).use { lock ->
                    createGC(client, inputStream, outputStream)
                }

                ClientOpcodes.CHANGE_GC ->
                    client.xServer.lock(
                        XServer.Lockable.PIXMAP_MANAGER,
                        XServer.Lockable.DRAWABLE_MANAGER,
                        XServer.Lockable.GRAPHIC_CONTEXT_MANAGER,
                    ).use {
                        changeGC(client, inputStream, outputStream)
                    }

                ClientOpcodes.SET_CLIP_RECTANGLES ->
                    client.skipRequest()

                ClientOpcodes.FREE_GC ->
                    client.xServer.lock(XServer.Lockable.GRAPHIC_CONTEXT_MANAGER).use {
                        freeGC(client, inputStream, outputStream)
                    }

                ClientOpcodes.COPY_AREA ->
                    client.xServer.lock(XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.GRAPHIC_CONTEXT_MANAGER).use {
                        copyArea(client, inputStream, outputStream)
                    }

                ClientOpcodes.POLY_LINE ->
                    client.xServer.lock(XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.GRAPHIC_CONTEXT_MANAGER).use {
                        polyLine(client, inputStream, outputStream)
                    }

                ClientOpcodes.POLY_SEGMENT ->
                    client.skipRequest()

                ClientOpcodes.POLY_RECTANGLE ->
                    client.skipRequest()

                ClientOpcodes.POLY_FILL_RECTANGLE ->
                    client.xServer.lock(XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.GRAPHIC_CONTEXT_MANAGER).use {
                        polyFillRectangle(client, inputStream, outputStream)
                    }

                ClientOpcodes.PUT_IMAGE ->
                    client.xServer.lock(XServer.Lockable.DRAWABLE_MANAGER, XServer.Lockable.GRAPHIC_CONTEXT_MANAGER).use {
                        putImage(client, inputStream, outputStream)
                    }

                ClientOpcodes.GET_IMAGE ->
                    client.xServer.lock(XServer.Lockable.PIXMAP_MANAGER, XServer.Lockable.DRAWABLE_MANAGER).use {
                        getImage(client, inputStream, outputStream)
                    }

                ClientOpcodes.CREATE_COLORMAP ->
                    client.skipRequest()

                ClientOpcodes.FREE_COLORMAP ->
                    client.skipRequest()

                ClientOpcodes.CREATE_CURSOR ->
                    client.xServer.lock(
                        XServer.Lockable.PIXMAP_MANAGER,
                        XServer.Lockable.DRAWABLE_MANAGER,
                        XServer.Lockable.CURSOR_MANAGER,
                    ).use {
                        createCursor(client, inputStream, outputStream)
                    }

                ClientOpcodes.CREATE_GLYPH_CURSOR ->
                    client.skipRequest()

                ClientOpcodes.FREE_CURSOR -> client.xServer.lock(
                    XServer.Lockable.PIXMAP_MANAGER,
                    XServer.Lockable.DRAWABLE_MANAGER,
                    XServer.Lockable.CURSOR_MANAGER,
                ).use {
                    freeCursor(client, inputStream, outputStream)
                }

                ClientOpcodes.QUERY_EXTENSION ->
                    queryExtension(client, inputStream, outputStream)

                ClientOpcodes.GET_KEYBOARD_MAPPING ->
                    client.xServer.lock(XServer.Lockable.INPUT_DEVICE).use {
                        getKeyboardMapping(client, inputStream, outputStream)
                    }

                ClientOpcodes.BELL ->
                    client.skipRequest()

                ClientOpcodes.SET_SCREEN_SAVER ->
                    client.skipRequest()

                ClientOpcodes.GET_SCREEN_SAVER ->
                    getScreenSaver(client, inputStream, outputStream)

                ClientOpcodes.FORCE_SCREEN_SAVER ->
                    client.skipRequest()

                ClientOpcodes.GET_MODIFIER_MAPPING ->
                    getModifierMapping(client, inputStream, outputStream)

                ClientOpcodes.NO_OPERATION -> client.skipRequest()

                else -> if (opcode < 0) {
                    val extension = client.xServer.extensions.get(opcode.toInt())
                    extension?.handleRequest(client, inputStream, outputStream)
                } else {
                    throw UnsupportedOperationException("Unsupported opcode $opcode.")
                }
            }
        } catch (e: XRequestError) {
            client.skipRequest()
            e.sendError(client, opcode)
        }

        return true
    }
}
