package com.winlator.xserver.extensions

import android.util.SparseArray
import androidx.core.util.size
import com.winlator.renderer.GPUImage
import com.winlator.xconnector.XInputStream
import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.Bitmask
import com.winlator.xserver.Pixmap
import com.winlator.xserver.Window
import com.winlator.xserver.XClient
import com.winlator.xserver.XClientRequestHandler
import com.winlator.xserver.XServer
import com.winlator.xserver.errors.BadImplementation
import com.winlator.xserver.errors.BadMatch
import com.winlator.xserver.errors.BadPixmap
import com.winlator.xserver.errors.BadWindow
import com.winlator.xserver.errors.XRequestError
import com.winlator.xserver.events.PresentCompleteNotify
import com.winlator.xserver.events.PresentIdleNotify
import java.io.IOException

class PresentExtension : Extension {

    companion object {
        const val MAJOR_OPCODE: Byte = -103
        private const val FAKE_INTERVAL = 1000000 / 60

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
    }

    enum class Kind {
        PIXMAP,
        MSC_NOTIFY,
    }

    enum class Mode {
        COPY,
        FLIP,
        SKIP,
    }

    private val events = SparseArray<Event>()

    private var syncExtension: SyncExtension? = null

    private object ClientOpcodes {
        const val QUERY_VERSION: Byte = 0
        const val PRESENT_PIXMAP: Byte = 1
        const val SELECT_INPUT: Byte = 3
    }

    private class Event {
        var window: Window? = null
        var client: XClient? = null
        var id: Int = 0
        var mask: Bitmask? = null
    }

    override val name: String
        get() = "Present"

    override val majorOpcode: Byte
        get() = MAJOR_OPCODE

    override val firstErrorId: Byte
        get() = 0

    override val firstEventId: Byte
        get() = 0

    private fun sendIdleNotify(window: Window, pixmap: Pixmap, serial: Int, idleFence: Int) {
        if (idleFence != 0) syncExtension!!.setTriggered(idleFence)

        synchronized(events) {
            for (i in 0..<events.size) {
                val event = events.valueAt(i)
                if (event.window == window && event.mask!!.isSet(PresentIdleNotify.eventMask)) {
                    event.client!!.sendEvent(PresentIdleNotify(event.id, window, pixmap, serial, idleFence))
                }
            }
        }
    }

    private fun sendCompleteNotify(window: Window, serial: Int, kind: Kind, mode: Mode, ust: Long, msc: Long) {
        synchronized(events) {
            for (i in 0..<events.size) {
                val event = events.valueAt(i)
                if (event.window == window && event.mask!!.isSet(PresentCompleteNotify.eventMask)) {
                    event.client!!.sendEvent(PresentCompleteNotify(event.id, window, serial, kind, mode, ust, msc))
                }
            }
        }
    }

    @Throws(IOException::class, XRequestError::class)
    private fun presentPixmap(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        val windowId = inputStream.readInt()
        val pixmapId = inputStream.readInt()
        val serial = inputStream.readInt()

        inputStream.skip(8)

        val xOff = inputStream.readShort()
        val yOff = inputStream.readShort()

        inputStream.skip(8)

        val idleFence = inputStream.readInt()

        inputStream.skip(client.remainingRequestLength)

        val window = client.xServer.windowManager.getWindow(windowId) ?: throw BadWindow(windowId)

        val pixmap = client.xServer.pixmapManager.getPixmap(pixmapId) ?: throw BadPixmap(pixmapId)

        val content = window.content!!
        if (content.visual?.depth != pixmap.drawable.visual?.depth) throw BadMatch()

        val ust = System.nanoTime() / 1000
        val msc = ust / FAKE_INTERVAL

        synchronized(content.renderLock) {
            content.copyArea(0.toShort(), 0.toShort(), xOff, yOff, pixmap.drawable.width, pixmap.drawable.height, pixmap.drawable)
            sendIdleNotify(window, pixmap, serial, idleFence)
            sendCompleteNotify(window, serial, Kind.PIXMAP, Mode.COPY, ust, msc)
        }
    }

    @Throws(IOException::class, XRequestError::class)
    private fun selectInput(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        val eventId = inputStream.readInt()
        val windowId = inputStream.readInt()
        val mask = Bitmask(inputStream.readInt())

        val window = client.xServer.windowManager.getWindow(windowId) ?: throw BadWindow(windowId)

        if (GPUImage.isSupported && !mask.isEmpty) {
            val content = window.content!!
            val oldTexture = content.texture

            client.xServer.renderer?.xServerView?.queueEvent { oldTexture?.destroy() }

            content.texture = GPUImage(content.width, content.height)
        }

        synchronized(events) {
            var event = events[eventId]
            if (event != null) {
                if (event.window != window || event.client != client) {
                    throw BadMatch()
                }

                if (!mask.isEmpty) {
                    event.mask = mask
                } else {
                    events.remove(eventId)
                }
            } else {
                event = Event()
                event.id = eventId
                event.window = window
                event.client = client
                event.mask = mask
                events.put(eventId, event)
            }
        }
    }

    @Throws(IOException::class, XRequestError::class)
    override fun handleRequest(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        val opcode = client.requestData.toInt()

        if (syncExtension == null) {
            syncExtension = client.xServer.getExtension(SyncExtension.MAJOR_OPCODE.toInt())
        }

        when (opcode) {
            ClientOpcodes.QUERY_VERSION.toInt() -> queryVersion(client, inputStream, outputStream)
            ClientOpcodes.PRESENT_PIXMAP.toInt() ->
                client.xServer.lock(XServer.Lockable.WINDOW_MANAGER, XServer.Lockable.PIXMAP_MANAGER).use {
                    presentPixmap(client, inputStream, outputStream)
                }

            ClientOpcodes.SELECT_INPUT.toInt() -> client.xServer.lock(XServer.Lockable.WINDOW_MANAGER).use {
                selectInput(client, inputStream, outputStream)
            }

            else -> throw BadImplementation()
        }
    }
}
