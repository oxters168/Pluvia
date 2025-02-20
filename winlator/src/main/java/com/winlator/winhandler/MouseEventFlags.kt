package com.winlator.winhandler

import com.winlator.xserver.Pointer

object MouseEventFlags {
    const val MOVE: Int = 0x0001
    const val LEFTDOWN: Int = 0x0002
    const val LEFTUP: Int = 0x0004
    const val RIGHTDOWN: Int = 0x0008
    const val RIGHTUP: Int = 0x0010
    const val MIDDLEDOWN: Int = 0x0020
    const val MIDDLEUP: Int = 0x0040
    const val XDOWN: Int = 0x0080
    const val XUP: Int = 0x0100
    const val WHEEL: Int = 0x0800
    const val VIRTUALDESK: Int = 0x4000
    const val ABSOLUTE: Int = 0x8000

    @JvmStatic
    fun getFlagFor(button: Pointer.Button, isActionDown: Boolean): Int {
        return when (button) {
            Pointer.Button.BUTTON_LEFT -> if (isActionDown) LEFTDOWN else LEFTUP
            Pointer.Button.BUTTON_MIDDLE -> if (isActionDown) MIDDLEDOWN else MIDDLEUP
            Pointer.Button.BUTTON_RIGHT -> if (isActionDown) RIGHTDOWN else RIGHTUP
            Pointer.Button.BUTTON_SCROLL_DOWN,
            Pointer.Button.BUTTON_SCROLL_UP,
            -> WHEEL
            else -> 0
        }
    }
}
