package com.winlator.xserver

import androidx.collection.ArrayMap
import com.winlator.xconnector.XInputStream
import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.XResourceManager.OnResourceLifecycleListener
import com.winlator.xserver.events.Event
import java.io.IOException
import timber.log.Timber

class XClient(
    val xServer: XServer,
    val inputStream: XInputStream,
    val outputStream: XOutputStream,
) : OnResourceLifecycleListener {

    var isAuthenticated: Boolean = false

    var resourceIDBase: Int = -1

    var sequenceNumber: Short = 0
        private set

    var requestLength = 0
        private set

    var requestData: Byte = 0

    private var initialLength = 0

    private val eventListeners = ArrayMap<Window?, EventListener?>()

    private val resources = ArrayList<XResource?>()

    val remainingRequestLength: Int
        get() {
            val actualLength = initialLength - inputStream.available()
            return requestLength - actualLength
        }

    init {
        xServer.lockAll().use {
            resourceIDBase = xServer.resourceIDs.get()
            xServer.windowManager.addOnResourceLifecycleListener(this)
            xServer.pixmapManager.addOnResourceLifecycleListener(this)
            xServer.graphicsContextManager.addOnResourceLifecycleListener(this)
            xServer.cursorManager.addOnResourceLifecycleListener(this)
        }
    }

    fun registerAsOwnerOfResource(resource: XResource?) {
        resources.add(resource)
    }

    fun setEventListenerForWindow(window: Window, eventMask: Bitmask) {
        var eventListener = eventListeners.get(window)

        if (eventListener != null) {
            window.removeEventListener(eventListener)
        }

        if (eventMask.isEmpty) {
            return
        }

        eventListener = EventListener(this, eventMask)

        eventListeners.put(window, eventListener)

        window.addEventListener(eventListener)
    }

    fun sendEvent(event: Event) {
        try {
            event.send(sequenceNumber, outputStream)
        } catch (e: IOException) {
            Timber.e(e)
        }
    }

    fun isInterestedIn(eventId: Int, window: Window?): Boolean {
        val eventListener = eventListeners[window]
        return eventListener != null && eventListener.isInterestedIn(eventId)
    }

    fun freeResources() {
        xServer.lockAll().use {
            while (!resources.isEmpty()) {
                val resource = resources.removeAt(resources.size - 1)

                if (resource is Window) {
                    xServer.windowManager.destroyWindow(resource.id)
                } else if (resource is Pixmap) {
                    xServer.pixmapManager.freePixmap(resource.id)
                } else if (resource is GraphicsContext) {
                    xServer.graphicsContextManager.freeGraphicsContext(resource.id)
                } else if (resource is Cursor) {
                    xServer.cursorManager.freeCursor(resource.id)
                }
            }

            while (!eventListeners.isEmpty()) {
                val i: Int = eventListeners.size - 1
                eventListeners.keyAt(i)!!.removeEventListener(eventListeners.removeAt(i))
            }

            xServer.windowManager.removeOnResourceLifecycleListener(this)
            xServer.pixmapManager.removeOnResourceLifecycleListener(this)
            xServer.graphicsContextManager.removeOnResourceLifecycleListener(this)
            xServer.cursorManager.removeOnResourceLifecycleListener(this)
            xServer.resourceIDs.free(resourceIDBase)
        }
    }

    fun generateSequenceNumber() {
        sequenceNumber++
    }

    fun setRequestLength(requestLength: Int) {
        this.requestLength = requestLength
        initialLength = inputStream.available()
    }

    fun skipRequest() {
        inputStream.skip(this.remainingRequestLength)
    }

    fun getEventMaskForWindow(window: Window?): Bitmask {
        val eventListener = eventListeners.get(window)
        return eventListener?.eventMask ?: Bitmask()
    }

    override fun onCreateResource(resource: XResource?) {
    }

    override fun onFreeResource(resource: XResource?) {
        if (resource is Window) {
            eventListeners.remove(resource)
        }

        resources.remove(resource)
    }

    fun isValidResourceId(id: Int): Boolean = xServer.resourceIDs.isInInterval(id, resourceIDBase)
}
