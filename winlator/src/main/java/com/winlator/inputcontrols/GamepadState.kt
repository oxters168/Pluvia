package com.winlator.inputcontrols

import java.nio.ByteBuffer

class GamepadState {

    var thumbLX: Float = 0f
    var thumbLY: Float = 0f
    var thumbRX: Float = 0f
    var thumbRY: Float = 0f
    val dpad: BooleanArray = BooleanArray(4)
    var buttons: Short = 0

    val povHat: Byte
        get() {
            var povHat: Byte = -1
            if (dpad[0] && dpad[1]) {
                povHat = 1
            } else if (dpad[1] && dpad[2]) {
                povHat = 3
            } else if (dpad[2] && dpad[3]) {
                povHat = 5
            } else if (dpad[3] && dpad[0]) {
                povHat = 7
            } else if (dpad[0]) {
                povHat = 0
            } else if (dpad[1]) {
                povHat = 2
            } else if (dpad[2]) {
                povHat = 4
            } else if (dpad[3]) {
                povHat = 6
            }
            return povHat
        }

    val dPadX: Byte
        get() = (if (dpad[1]) 1 else (if (dpad[3]) -1 else 0)).toByte()

    val dPadY: Byte
        get() = (if (dpad[0]) -1 else (if (dpad[2]) 1 else 0)).toByte()

    fun writeTo(buffer: ByteBuffer) {
        buffer.putShort(buttons)
        buffer.put(povHat)
        buffer.putShort((thumbLX * Short.MAX_VALUE).toInt().toShort())
        buffer.putShort((thumbLY * Short.MAX_VALUE).toInt().toShort())
        buffer.putShort((thumbRX * Short.MAX_VALUE).toInt().toShort())
        buffer.putShort((thumbRY * Short.MAX_VALUE).toInt().toShort())
    }

    fun setPressed(buttonIdx: Int, pressed: Boolean) {
        val flag = 1 shl buttonIdx
        buttons = if (pressed) {
            (buttons.toInt() or flag).toShort()
        } else {
            (buttons.toInt() and flag.inv()).toShort()
        }
    }

    fun isPressed(buttonIdx: Int): Boolean {
        return (buttons.toInt() and (1 shl buttonIdx)) != 0
    }

    fun copy(other: GamepadState) {
        this.thumbLX = other.thumbLX
        this.thumbLY = other.thumbLY
        this.thumbRX = other.thumbRX
        this.thumbRY = other.thumbRY
        this.buttons = other.buttons

        System.arraycopy(other.dpad, 0, this.dpad, 0, 4)
    }
}
