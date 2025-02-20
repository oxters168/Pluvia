package com.winlator.xserver

import com.winlator.math.Mathf.clamp

class Pointer(private val xServer: XServer) {

    companion object {
        const val MAX_BUTTONS: Byte = 7
    }

    enum class Button {
        BUTTON_LEFT,
        BUTTON_MIDDLE,
        BUTTON_RIGHT,
        BUTTON_SCROLL_UP,
        BUTTON_SCROLL_DOWN,
        BUTTON_SCROLL_CLICK_LEFT,
        BUTTON_SCROLL_CLICK_RIGHT,
        ;

        fun code(): Byte = (ordinal + 1).toByte()

        fun flag(): Int = 1 shl (code() + MAX_BUTTONS)
    }

    private val onPointerMotionListeners = ArrayList<OnPointerMotionListener?>()

    val buttonMask: Bitmask = Bitmask()

    var x: Short = 0
        private set

    var y: Short = 0
        private set

    val clampedX: Short
        get() = clamp(x.toInt(), 0, xServer.screenInfo.width - 1).toShort()

    val clampedY: Short
        get() = clamp(y.toInt(), 0, xServer.screenInfo.height - 1).toShort()

    interface OnPointerMotionListener {
        fun onPointerButtonPress(button: Button) {}
        fun onPointerButtonRelease(button: Button) {}
        fun onPointerMove(x: Short, y: Short) {}
    }

    fun setX(x: Int) {
        this.x = x.toShort()
    }

    fun setY(y: Int) {
        this.y = y.toShort()
    }

    fun setPosition(x: Int, y: Int) {
        setX(x)
        setY(y)
        triggerOnPointerMove(this.x, this.y)
    }

    fun setButton(button: Button, pressed: Boolean) {
        val oldPressed = isButtonPressed(button)
        buttonMask.set(button.flag(), pressed)
        if (oldPressed != pressed) {
            if (pressed) {
                triggerOnPointerButtonPress(button)
            } else {
                triggerOnPointerButtonRelease(button)
            }
        }
    }

    fun isButtonPressed(button: Button): Boolean = buttonMask.isSet(button.flag())

    fun addOnPointerMotionListener(onPointerMotionListener: OnPointerMotionListener?) {
        onPointerMotionListeners.add(onPointerMotionListener)
    }

    fun removeOnPointerMotionListener(onPointerMotionListener: OnPointerMotionListener?) {
        onPointerMotionListeners.remove(onPointerMotionListener)
    }

    private fun triggerOnPointerButtonPress(button: Button) {
        onPointerMotionListeners.indices.reversed().forEach {
            onPointerMotionListeners[it]!!.onPointerButtonPress(button)
        }
    }

    private fun triggerOnPointerButtonRelease(button: Button) {
        onPointerMotionListeners.indices.reversed().forEach {
            onPointerMotionListeners[it]!!.onPointerButtonRelease(button)
        }
    }

    private fun triggerOnPointerMove(x: Short, y: Short) {
        onPointerMotionListeners.indices.reversed().forEach {
            onPointerMotionListeners[it]!!.onPointerMove(x, y)
        }
    }
}
