package com.winlator.xserver

import com.winlator.xserver.WindowManager.OnWindowModificationListener
import com.winlator.xserver.events.Event
import com.winlator.xserver.events.PointerWindowEvent

class GrabManager(private val xServer: XServer) : OnWindowModificationListener {

    var window: Window? = null
        private set

    var isOwnerEvents: Boolean = false
        private set

    var isReleaseWithButtons: Boolean = false
        private set

    var eventListener: EventListener? = null
        private set

    val client: XClient?
        get() = if (eventListener != null) eventListener!!.client else null

    init {
        xServer.windowManager.addOnWindowModificationListener(this)
    }

    override fun onUnmapWindow(window: Window) {
        if (window.mapState != Window.MapState.VIEWABLE) {
            deactivatePointerGrab()
        }
    }

    fun deactivatePointerGrab() {
        if (window != null) {
            xServer.inputDeviceManager.sendEnterLeaveNotify(window!!, xServer.inputDeviceManager.pointWindow!!, PointerWindowEvent.Mode.UNGRAB)
            window = null
            eventListener = null
        }
    }

    private fun activatePointerGrab(window: Window, eventListener: EventListener?, ownerEvents: Boolean, releaseWithButtons: Boolean) {
        if (this.window == null) {
            xServer.inputDeviceManager.sendEnterLeaveNotify(xServer.inputDeviceManager.pointWindow!!, window, PointerWindowEvent.Mode.GRAB)
        }
        this.window = window
        this.isReleaseWithButtons = releaseWithButtons
        this.isOwnerEvents = ownerEvents
        this.eventListener = eventListener
    }

    fun activatePointerGrab(window: Window, ownerEvents: Boolean, eventMask: Bitmask, client: XClient) {
        activatePointerGrab(window, EventListener(client, eventMask), ownerEvents, false)
    }

    fun activatePointerGrab(window: Window) {
        val eventListener = window.buttonPressListener
        activatePointerGrab(window, eventListener, eventListener!!.isInterestedIn(Event.OWNER_GRAB_BUTTON), true)
    }
}
