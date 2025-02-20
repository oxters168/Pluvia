package com.winlator.inputcontrols

import android.view.InputDevice
import android.view.MotionEvent
import com.winlator.core.AppUtils.screenHeight
import com.winlator.core.AppUtils.screenWidth
import com.winlator.math.Mathf.roundPoint
import com.winlator.math.XForm.instance
import com.winlator.math.XForm.makeScale
import com.winlator.math.XForm.makeTranslation
import com.winlator.math.XForm.scale
import com.winlator.math.XForm.transformPoint
import com.winlator.renderer.ViewTransformation
import com.winlator.winhandler.MouseEventFlags
import com.winlator.xserver.Pointer
import com.winlator.xserver.XServer
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

class TouchMouse(private val xServer: XServer) {
    private val fingers: Array<Finger?> = arrayOfNulls<Finger>(MAX_FINGERS.toInt())
    private val xform = instance
    private var fingerPointerButtonLeft: Finger? = null
    private var fingerPointerButtonRight: Finger? = null
    private var fourFingersTapCallback: Runnable? = null
    private var numFingers: Byte = 0
    private var scrollAccumY = 0f
    private var scrolling = false
    private var sensitivity = 1.0f

    var isPointerButtonLeftEnabled: Boolean = true
    var isPointerButtonRightEnabled: Boolean = true

    init {
        updateXform(
            screenWidth,
            screenHeight,
            xServer.screenInfo.width.toInt(),
            xServer.screenInfo.height.toInt(),
        )
    }

    private fun updateXform(outerWidth: Int, outerHeight: Int, innerWidth: Int, innerHeight: Int) {
        val viewTransformation = ViewTransformation()
        viewTransformation.update(outerWidth, outerHeight, innerWidth, innerHeight)

        val invAspect = 1.0f / viewTransformation.aspect
        if (!xServer.renderer!!.isFullscreen) {
            makeTranslation(
                xform,
                -viewTransformation.viewOffsetX.toFloat(),
                -viewTransformation.viewOffsetY.toFloat(),
            )

            scale(xform, invAspect, invAspect)
        } else {
            makeScale(xform, invAspect, invAspect)
        }
    }

    private inner class Finger(x: Float, y: Float) {
        var x: Int
            private set
        var y: Int
            private set
        private val startX: Int
        private val startY: Int
        var lastX: Int
            private set
        var lastY: Int
            private set
        private val touchTime: Long

        init {
            val transformedPoint = transformPoint(xform, x, y)

            this.lastX = transformedPoint[0].toInt()
            this.startX = this.lastX
            this.x = this.startX
            this.lastY = transformedPoint[1].toInt()
            this.startY = this.lastY
            this.y = this.startY

            touchTime = System.currentTimeMillis()
        }

        fun update(x: Float, y: Float) {
            lastX = this.x
            lastY = this.y

            val transformedPoint = transformPoint(xform, x, y)

            this.x = transformedPoint[0].toInt()
            this.y = transformedPoint[1].toInt()
        }

        fun deltaX(): Int {
            var dx = (x - lastX) * sensitivity

            if (abs(dx.toDouble()) > CURSOR_ACCELERATION_THRESHOLD) {
                dx *= CURSOR_ACCELERATION
            }

            return roundPoint(dx)
        }

        fun deltaY(): Int {
            var dy = (y - lastY) * sensitivity

            if (abs(dy.toDouble()) > CURSOR_ACCELERATION_THRESHOLD) {
                dy *= CURSOR_ACCELERATION
            }

            return roundPoint(dy)
        }

        val isTap: Boolean
            get() = (System.currentTimeMillis() - touchTime) < MAX_TAP_MILLISECONDS && travelDistance() < MAX_TAP_TRAVEL_DISTANCE

        fun travelDistance(): Float = hypot((x - startX).toDouble(), (y - startY).toDouble()).toFloat()
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        val actionIndex = event.actionIndex
        val pointerId = event.getPointerId(actionIndex)
        val actionMasked = event.actionMasked

        if (pointerId >= MAX_FINGERS) {
            return true
        }

        when (actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
                    return true
                }

                scrollAccumY = 0f
                scrolling = false

                fingers[pointerId] = Finger(event.getX(actionIndex), event.getY(actionIndex))

                numFingers++
            }

            MotionEvent.ACTION_MOVE -> if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
                val transformedPoint = transformPoint(xform, event.x, event.y)
                xServer.injectPointerMove(transformedPoint[0].toInt(), transformedPoint[1].toInt())
            } else {
                var i: Byte = 0
                while (i < MAX_FINGERS) {
                    if (fingers[i.toInt()] != null) {
                        val pointerIndex = event.findPointerIndex(i.toInt())
                        if (pointerIndex >= 0) {
                            fingers[i.toInt()]!!.update(
                                event.getX(pointerIndex),
                                event.getY(pointerIndex),
                            )
                            handleFingerMove(fingers[i.toInt()]!!)
                        } else {
                            handleFingerUp(fingers[i.toInt()]!!)
                            fingers[i.toInt()] = null
                            numFingers--
                        }
                    }
                    i++
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> if (fingers[pointerId] != null) {
                fingers[pointerId]!!.update(event.getX(actionIndex), event.getY(actionIndex))
                handleFingerUp(fingers[pointerId]!!)
                fingers[pointerId] = null
                numFingers--
            }

            MotionEvent.ACTION_CANCEL -> {
                var i: Byte = 0
                while (i < MAX_FINGERS) {
                    fingers[i.toInt()] = null
                    i++
                }
                numFingers = 0
            }
        }

        return true
    }

    private fun handleFingerUp(finger1: Finger) {
        when (numFingers.toInt()) {
            1 -> if (finger1.isTap) pressPointerButtonLeft(finger1)
            2 -> {
                val finger2 = findSecondFinger(finger1)
                if (finger2 != null && finger1.isTap) pressPointerButtonRight(finger1)
            }

            4 -> if (fourFingersTapCallback != null) {
                var i: Byte = 0
                while (i < 4) {
                    if (fingers[i.toInt()] != null && !fingers[i.toInt()]!!.isTap) return
                    i++
                }
                fourFingersTapCallback!!.run()
            }
        }

        releasePointerButtonLeft(finger1)
        releasePointerButtonRight(finger1)
    }

    private fun handleFingerMove(finger1: Finger) {
        var skipPointerMove = false

        val finger2 = if (numFingers.toInt() == 2) findSecondFinger(finger1) else null
        if (finger2 != null) {
            val resolutionScale: Float = 1000.0f / min(
                xServer.screenInfo.width.toDouble(),
                xServer.screenInfo.height.toDouble(),
            ).toFloat()
            val currDistance = hypot(
                (finger1.x - finger2.x).toDouble(),
                (finger1.y - finger2.y).toDouble(),
            ).toFloat() * resolutionScale

            if (currDistance < MAX_TWO_FINGERS_SCROLL_DISTANCE) {
                scrollAccumY += ((finger1.y + finger2.y) * 0.5f) - (finger1.lastY + finger2.lastY) * 0.5f

                if (scrollAccumY < -100) {
                    xServer.injectPointerButtonPress(Pointer.Button.BUTTON_SCROLL_DOWN)
                    xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_SCROLL_DOWN)
                    scrollAccumY = 0f
                } else if (scrollAccumY > 100) {
                    xServer.injectPointerButtonPress(Pointer.Button.BUTTON_SCROLL_UP)
                    xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_SCROLL_UP)
                    scrollAccumY = 0f
                }

                scrolling = true
            } else if (currDistance >= MAX_TWO_FINGERS_SCROLL_DISTANCE &&
                !xServer.pointer.isButtonPressed(Pointer.Button.BUTTON_LEFT) &&
                finger2.travelDistance() < MAX_TAP_TRAVEL_DISTANCE
            ) {
                pressPointerButtonLeft(finger1)
                skipPointerMove = true
            }
        }

        if (!scrolling && numFingers <= 2 && !skipPointerMove) {
            val dx = finger1.deltaX()
            val dy = finger1.deltaY()

            if (xServer.isRelativeMouseMovement) {
                val winHandler = xServer.winHandler
                winHandler!!.mouseEvent(MouseEventFlags.MOVE, dx, dy, 0)
            } else {
                xServer.injectPointerMoveDelta(dx, dy)
            }
        }
    }

    private fun findSecondFinger(finger: Finger?): Finger? {
        for (i in 0..<MAX_FINGERS) {
            if (fingers[i.toInt()] != null && fingers[i.toInt()] != finger) {
                return fingers[i.toInt()]
            }
        }

        return null
    }

    private fun pressPointerButtonLeft(finger: Finger?) {
        if (this.isPointerButtonLeftEnabled && !xServer.pointer.isButtonPressed(Pointer.Button.BUTTON_LEFT)) {
            xServer.injectPointerButtonPress(Pointer.Button.BUTTON_LEFT)
            fingerPointerButtonLeft = finger
        }
    }

    private fun pressPointerButtonRight(finger: Finger?) {
        if (this.isPointerButtonRightEnabled && !xServer.pointer.isButtonPressed(Pointer.Button.BUTTON_RIGHT)) {
            xServer.injectPointerButtonPress(Pointer.Button.BUTTON_RIGHT)
            fingerPointerButtonRight = finger
        }
    }

    private fun releasePointerButtonLeft(finger: Finger?) {
        if (this.isPointerButtonLeftEnabled &&
            finger == fingerPointerButtonLeft &&
            xServer.pointer.isButtonPressed(Pointer.Button.BUTTON_LEFT)
        ) {
            Executors.newSingleThreadScheduledExecutor().schedule(
                Runnable {
                    xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_LEFT)
                    fingerPointerButtonLeft = null
                },
                30,
                TimeUnit.MILLISECONDS,
            )
            // postDelayed(() -> {
            //     xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_LEFT);
            //     fingerPointerButtonLeft = null;
            // }, 30);
        }
    }

    private fun releasePointerButtonRight(finger: Finger?) {
        if (this.isPointerButtonRightEnabled &&
            finger == fingerPointerButtonRight &&
            xServer.pointer.isButtonPressed(Pointer.Button.BUTTON_RIGHT)
        ) {
            Executors.newSingleThreadScheduledExecutor().schedule(
                Runnable {
                    xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_RIGHT)
                    fingerPointerButtonRight = null
                },
                30,
                TimeUnit.MILLISECONDS,
            )
            // postDelayed(() -> {
            //     xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_RIGHT);
            //     fingerPointerButtonRight = null;
            // }, 30);
        }
    }

    fun setSensitivity(sensitivity: Float) {
        this.sensitivity = sensitivity
    }

    fun setFourFingersTapCallback(fourFingersTapCallback: Runnable?) {
        this.fourFingersTapCallback = fourFingersTapCallback
    }

    fun onExternalMouseEvent(event: MotionEvent): Boolean {
        var handled = false
        // if (event.isFromSource(InputDevice.SOURCE_MOUSE)) {
        if (isMouseDevice(event.device)) {
            val actionButton = event.actionButton
            when (event.action) {
                MotionEvent.ACTION_BUTTON_PRESS -> {
                    if (actionButton == MotionEvent.BUTTON_PRIMARY) {
                        xServer.injectPointerButtonPress(Pointer.Button.BUTTON_LEFT)
                    } else if (actionButton == MotionEvent.BUTTON_SECONDARY) {
                        xServer.injectPointerButtonPress(Pointer.Button.BUTTON_RIGHT)
                    }

                    handled = true
                }

                MotionEvent.ACTION_BUTTON_RELEASE -> {
                    if (actionButton == MotionEvent.BUTTON_PRIMARY) {
                        xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_LEFT)
                    } else if (actionButton == MotionEvent.BUTTON_SECONDARY) {
                        xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_RIGHT)
                    }

                    handled = true
                }

                MotionEvent.ACTION_HOVER_MOVE -> {
                    val transformedPoint = transformPoint(xform, event.x, event.y)

                    xServer.injectPointerMove(
                        transformedPoint[0].toInt(),
                        transformedPoint[1].toInt(),
                    )

                    handled = true
                }

                MotionEvent.ACTION_SCROLL -> {
                    val scrollY = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                    if (scrollY <= -1.0f) {
                        xServer.injectPointerButtonPress(Pointer.Button.BUTTON_SCROLL_DOWN)
                        xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_SCROLL_DOWN)
                    } else if (scrollY >= 1.0f) {
                        xServer.injectPointerButtonPress(Pointer.Button.BUTTON_SCROLL_UP)
                        xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_SCROLL_UP)
                    }

                    handled = true
                }
            }
        }

        return handled
    }

    fun computeDeltaPoint(lastX: Float, lastY: Float, x: Float, y: Float): FloatArray {
        var lastX = lastX
        var lastY = lastY
        var x = x
        var y = y
        val result = floatArrayOf(0f, 0f)

        transformPoint(xform, lastX, lastY, result)
        lastX = result[0]
        lastY = result[1]

        transformPoint(xform, x, y, result)
        x = result[0]
        y = result[1]

        result[0] = x - lastX
        result[1] = y - lastY

        return result
    }

    companion object {
        private const val MAX_FINGERS: Byte = 4
        private const val MAX_TWO_FINGERS_SCROLL_DISTANCE: Short = 350

        const val MAX_TAP_TRAVEL_DISTANCE: Byte = 10
        const val MAX_TAP_MILLISECONDS: Short = 200
        const val CURSOR_ACCELERATION: Float = 1.25f
        const val CURSOR_ACCELERATION_THRESHOLD: Byte = 6

        fun isMouseDevice(device: InputDevice?): Boolean {
            if (device == null) return false
            val sources = device.sources

            return !device.isVirtual &&
                (
                    (sources and InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE ||
                        (sources and InputDevice.SOURCE_TOUCHPAD) == InputDevice.SOURCE_TOUCHPAD ||
                        (sources and InputDevice.SOURCE_BLUETOOTH_STYLUS) == InputDevice.SOURCE_BLUETOOTH_STYLUS ||
                        (sources and InputDevice.SOURCE_STYLUS) == InputDevice.SOURCE_STYLUS
                    )
        }
    }
}
