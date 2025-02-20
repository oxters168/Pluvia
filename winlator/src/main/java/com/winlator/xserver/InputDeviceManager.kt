package com.winlator.xserver

import com.winlator.winhandler.MouseEventFlags.getFlagFor
import com.winlator.xserver.Keyboard.OnKeyboardListener
import com.winlator.xserver.Pointer.OnPointerMotionListener
import com.winlator.xserver.WindowManager.OnWindowModificationListener
import com.winlator.xserver.XResourceManager.OnResourceLifecycleListener
import com.winlator.xserver.events.ButtonPress
import com.winlator.xserver.events.ButtonRelease
import com.winlator.xserver.events.EnterNotify
import com.winlator.xserver.events.Event
import com.winlator.xserver.events.KeyPress
import com.winlator.xserver.events.KeyRelease
import com.winlator.xserver.events.LeaveNotify
import com.winlator.xserver.events.MappingNotify
import com.winlator.xserver.events.MotionNotify
import com.winlator.xserver.events.PointerWindowEvent
import com.winlator.xserver.events.PointerWindowEvent.Detail

class InputDeviceManager(private val xServer: XServer) :
    OnPointerMotionListener,
    OnKeyboardListener,
    OnWindowModificationListener,
    OnResourceLifecycleListener {

    companion object {
        private const val MOUSE_WHEEL_DELTA: Byte = 120
    }

    var pointWindow: Window?
        private set

    init {
        pointWindow = xServer.windowManager.rootWindow

        xServer.windowManager.addOnWindowModificationListener(this)
        xServer.windowManager.addOnResourceLifecycleListener(this)
        xServer.pointer.addOnPointerMotionListener(this)
        xServer.keyboard.addOnKeyboardListener(this)
    }

    override fun onMapWindow(window: Window) {
        updatePointWindow()
    }

    override fun onUnmapWindow(window: Window) {
        updatePointWindow()
    }

    override fun onChangeWindowZOrder(window: Window) {
        updatePointWindow()
    }

    override fun onUpdateWindowGeometry(window: Window?, resized: Boolean) {
        updatePointWindow()
    }

    override fun onCreateResource(resource: XResource?) {
        updatePointWindow()
    }

    override fun onFreeResource(resource: XResource?) {
        updatePointWindow()
    }

    private fun updatePointWindow() {
        val pointWindow = xServer.windowManager.findPointWindow(xServer.pointer.clampedX, xServer.pointer.clampedY)
        this.pointWindow = pointWindow ?: xServer.windowManager.rootWindow
    }

    private fun sendEvent(window: Window?, eventId: Int, event: Event) {
        val grabWindow = xServer.grabManager.window
        if (grabWindow != null && grabWindow.attributes.isEnabled) {
            val eventListener = xServer.grabManager.eventListener
            if (xServer.grabManager.isOwnerEvents && window != null) {
                window.sendEvent(eventId, event, xServer.grabManager.client)
            } else if (eventListener!!.isInterestedIn(eventId)) {
                eventListener.sendEvent(event)
            }
        } else if (window != null && window.attributes.isEnabled) {
            window.sendEvent(eventId, event)
        }
    }

    private fun sendEvent(window: Window?, eventMask: Bitmask, event: Event) {
        val grabWindow = xServer.grabManager.window
        if (grabWindow != null && grabWindow.attributes.isEnabled) {
            val eventListener = xServer.grabManager.eventListener
            if (xServer.grabManager.isOwnerEvents && window != null) {
                window.sendEvent(eventMask, event, eventListener!!.client)
            } else if (eventListener!!.isInterestedIn(eventMask)) {
                eventListener.sendEvent(event)
            }
        } else if (window != null && window.attributes.isEnabled) {
            window.sendEvent(eventMask, event)
        }
    }

    fun sendEnterLeaveNotify(windowA: Window, windowB: Window, mode: PointerWindowEvent.Mode) {
        if (windowA == windowB) return
        val x = xServer.pointer.x
        val y = xServer.pointer.y

        val localPointA = windowA.rootPointToLocal(x, y)
        val localPointB = windowB.rootPointToLocal(x, y)

        val sameScreenAndFocus = windowB.isAncestorOf(xServer.windowManager.focusedWindow)
        var detailA = Detail.NONLINEAR
        var detailB = Detail.NONLINEAR

        if (windowA.isAncestorOf(windowB)) {
            detailA = Detail.ANCESTOR
            detailB = Detail.INFERIOR
        } else if (windowB.isAncestorOf(windowA)) {
            detailB = Detail.ANCESTOR
            detailA = Detail.INFERIOR
        }

        val keyButMask = this.keyButMask
        sendEvent(
            window = windowA,
            eventId = Event.LEAVE_WINDOW,
            event = LeaveNotify(
                detail = detailA,
                root = xServer.windowManager.rootWindow,
                event = windowA,
                child = null,
                rootX = x,
                rootY = y,
                eventX = localPointA[0],
                eventY = localPointA[1],
                state = keyButMask,
                mode = mode,
                sameScreenAndFocus = sameScreenAndFocus,
            ),
        )
        sendEvent(
            window = windowB,
            eventId = Event.ENTER_WINDOW,
            event = EnterNotify(
                detail = detailB,
                root = xServer.windowManager.rootWindow,
                event = windowB,
                child = null,
                rootX = x,
                rootY = y,
                eventX = localPointB[0],
                eventY = localPointB[1],
                state = keyButMask,
                mode = mode,
                sameScreenAndFocus = sameScreenAndFocus,
            ),
        )
    }

    override fun onPointerButtonPress(button: Pointer.Button) {
        if (xServer.isRelativeMouseMovement) {
            val winHandler = xServer.winHandler
            val wheelDelta = if (button == Pointer.Button.BUTTON_SCROLL_UP) {
                MOUSE_WHEEL_DELTA.toInt()
            } else {
                if (button == Pointer.Button.BUTTON_SCROLL_DOWN) {
                    -MOUSE_WHEEL_DELTA
                } else {
                    0
                }
            }
            winHandler!!.mouseEvent(getFlagFor(button, true), 0, 0, wheelDelta)
        } else {
            var grabWindow = xServer.grabManager.window
            if (grabWindow == null) {
                grabWindow = pointWindow!!.getAncestorWithEventId(Event.BUTTON_PRESS)
                if (grabWindow != null) {
                    xServer.grabManager.activatePointerGrab(grabWindow)
                }
            }

            if (grabWindow != null && grabWindow.attributes.isEnabled) {
                val eventMask = createPointerEventMask()

                eventMask.unset(button.flag())

                val x = xServer.pointer.x
                val y = xServer.pointer.y
                val localPoint = grabWindow.rootPointToLocal(x, y)

                val child = if (grabWindow.isAncestorOf(pointWindow)) pointWindow else null
                grabWindow.sendEvent(
                    eventId = Event.BUTTON_PRESS,
                    event = ButtonPress(
                        detail = button.code(),
                        root = xServer.windowManager.rootWindow,
                        event = grabWindow,
                        child = child,
                        rootX = x,
                        rootY = y,
                        eventX = localPoint[0],
                        eventY = localPoint[1],
                        state = eventMask,
                    ),
                )
            }
        }
    }

    override fun onPointerButtonRelease(button: Pointer.Button) {
        if (xServer.isRelativeMouseMovement) {
            val winHandler = xServer.winHandler
            winHandler!!.mouseEvent(getFlagFor(button, false), 0, 0, 0)
        } else {
            val eventMask = createPointerEventMask()
            val grabWindow = xServer.grabManager.window
            val window =
                if (grabWindow == null || xServer.grabManager.isOwnerEvents) {
                    pointWindow!!.getAncestorWithEventMask(
                        eventMask,
                    )
                } else {
                    null
                }

            if (grabWindow != null || window != null) {
                val eventWindow: Window = (window ?: grabWindow)!!

                val x = xServer.pointer.x
                val y = xServer.pointer.y
                val localPoint = eventWindow.rootPointToLocal(x, y)

                val child = if (eventWindow.isAncestorOf(pointWindow)) {
                    pointWindow
                } else {
                    null
                }
                val buttonRelease = ButtonRelease(
                    detail = button.code(),
                    root = xServer.windowManager.rootWindow,
                    event = eventWindow,
                    child = child,
                    rootX = x,
                    rootY = y,
                    eventX = localPoint[0],
                    eventY = localPoint[1],
                    state = eventMask,
                )

                sendEvent(window, eventMask, buttonRelease)
            }

            if (xServer.pointer.buttonMask.isEmpty && xServer.grabManager.isReleaseWithButtons) {
                xServer.grabManager.deactivatePointerGrab()
            }
        }
    }

    override fun onPointerMove(x: Short, y: Short) {
        updatePointWindow()

        val eventMask = createPointerEventMask()
        val grabWindow = xServer.grabManager.window

        val window =
            if (grabWindow == null || xServer.grabManager.isOwnerEvents) {
                pointWindow!!.getAncestorWithEventMask(eventMask)
            } else {
                null
            }

        if (grabWindow != null || window != null) {
            val eventWindow: Window = (window ?: grabWindow)!!
            val localPoint = eventWindow.rootPointToLocal(x, y)

            val child = if (eventWindow.isAncestorOf(pointWindow)) {
                pointWindow
            } else {
                null
            }

            sendEvent(
                window, eventMask,
                MotionNotify(
                    detail = false,
                    root = xServer.windowManager.rootWindow,
                    event = eventWindow,
                    child = child,
                    rootX = x,
                    rootY = y,
                    eventX = localPoint[0],
                    eventY = localPoint[1],
                    state = this.keyButMask,
                ),
            )
        }
    }

    override fun onKeyPress(keycode: Byte, keysym: Int) {
        val focusedWindow = xServer.windowManager.focusedWindow
        if (focusedWindow == null) {
            return
        }

        updatePointWindow()

        var eventWindow: Window? = null
        var child: Window? = null

        if (focusedWindow.isAncestorOf(pointWindow)) {
            eventWindow = pointWindow!!.getAncestorWithEventId(Event.KEY_PRESS, focusedWindow)
            child = if (eventWindow!!.isAncestorOf(pointWindow)) {
                pointWindow
            } else {
                null
            }
        }

        if (eventWindow == null) {
            if (!focusedWindow.hasEventListenerFor(Event.KEY_PRESS)) {
                return
            }

            eventWindow = focusedWindow
        }

        if (!eventWindow.attributes.isEnabled) {
            return
        }

        val keyButMask = this.keyButMask
        val x = xServer.pointer.x
        val y = xServer.pointer.y
        val localPoint = eventWindow.rootPointToLocal(x, y)

        if (keysym != 0 && !xServer.keyboard.hasKeysym(keycode, keysym)) {
            xServer.keyboard.setKeysyms(keycode, keysym, keysym)
            eventWindow.sendEvent(MappingNotify(MappingNotify.Request.KEYBOARD, keycode, 1))
        }

        eventWindow.sendEvent(
            Event.KEY_PRESS,
            KeyPress(
                keycode = keycode,
                root = xServer.windowManager.rootWindow,
                event = eventWindow,
                child = child,
                rootX = x,
                rootY = y,
                eventX = localPoint[0],
                eventY = localPoint[1],
                state = keyButMask,
            ),
        )
    }

    override fun onKeyRelease(keycode: Byte) {
        val focusedWindow = xServer.windowManager.focusedWindow
        if (focusedWindow == null) {
            return
        }

        updatePointWindow()

        var eventWindow: Window? = null
        var child: Window? = null

        if (focusedWindow.isAncestorOf(pointWindow)) {
            eventWindow = pointWindow!!.getAncestorWithEventId(Event.KEY_RELEASE, focusedWindow)
            child = if (eventWindow!!.isAncestorOf(pointWindow)) {
                pointWindow
            } else {
                null
            }
        }

        if (eventWindow == null) {
            if (!focusedWindow.hasEventListenerFor(Event.KEY_RELEASE)) {
                return
            }

            eventWindow = focusedWindow
        }

        if (!eventWindow.attributes.isEnabled) return

        val keyButMask = this.keyButMask
        val x = xServer.pointer.x
        val y = xServer.pointer.y
        val localPoint = eventWindow.rootPointToLocal(x, y)

        eventWindow.sendEvent(
            Event.KEY_RELEASE,
            KeyRelease(
                keycode = keycode,
                root = xServer.windowManager.rootWindow,
                event = eventWindow,
                child = child,
                rootX = x,
                rootY = y,
                eventX = localPoint[0],
                eventY = localPoint[1],
                state = keyButMask,
            ),
        )
    }

    private fun createPointerEventMask(): Bitmask {
        val eventMask = Bitmask()
        eventMask.set(Event.POINTER_MOTION)

        val buttonMask = xServer.pointer.buttonMask
        if (!buttonMask.isEmpty) {
            eventMask.set(Event.BUTTON_MOTION)

            if (buttonMask.isSet(Pointer.Button.BUTTON_LEFT.flag())) {
                eventMask.set(Event.BUTTON1_MOTION)
            }
            if (buttonMask.isSet(Pointer.Button.BUTTON_MIDDLE.flag())) {
                eventMask.set(Event.BUTTON2_MOTION)
            }
            if (buttonMask.isSet(Pointer.Button.BUTTON_RIGHT.flag())) {
                eventMask.set(Event.BUTTON3_MOTION)
            }
            if (buttonMask.isSet(Pointer.Button.BUTTON_SCROLL_UP.flag())) {
                eventMask.set(Event.BUTTON4_MOTION)
            }
            if (buttonMask.isSet(Pointer.Button.BUTTON_SCROLL_DOWN.flag())) {
                eventMask.set(Event.BUTTON5_MOTION)
            }
        }

        return eventMask
    }

    val keyButMask: Bitmask
        get() {
            val keyButMask = Bitmask()
            keyButMask.join(xServer.pointer.buttonMask)
            keyButMask.join(xServer.keyboard.modifiersMask)
            return keyButMask
        }
}
