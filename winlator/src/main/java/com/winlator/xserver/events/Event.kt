package com.winlator.xserver.events

import com.winlator.xconnector.XOutputStream
import java.io.IOException

abstract class Event(code: Int) {

    companion object {
        const val KEY_PRESS: Int = 1 shl 0
        const val KEY_RELEASE: Int = 1 shl 1
        const val BUTTON_PRESS: Int = 1 shl 2
        const val BUTTON_RELEASE: Int = 1 shl 3
        const val ENTER_WINDOW: Int = 1 shl 4
        const val LEAVE_WINDOW: Int = 1 shl 5
        const val POINTER_MOTION: Int = 1 shl 6
        const val POINTER_MOTION_HINT: Int = 1 shl 7
        const val BUTTON1_MOTION: Int = 1 shl 8
        const val BUTTON2_MOTION: Int = 1 shl 9
        const val BUTTON3_MOTION: Int = 1 shl 10
        const val BUTTON4_MOTION: Int = 1 shl 11
        const val BUTTON5_MOTION: Int = 1 shl 12
        const val BUTTON_MOTION: Int = 1 shl 13
        const val KEYMAP_STATE: Int = 1 shl 14
        const val EXPOSURE: Int = 1 shl 15
        const val VISIBILITY_CHANGE: Int = 1 shl 16
        const val STRUCTURE_NOTIFY: Int = 1 shl 17
        const val RESIZE_REDIRECT: Int = 1 shl 18
        const val SUBSTRUCTURE_NOTIFY: Int = 1 shl 19
        const val SUBSTRUCTURE_REDIRECT: Int = 1 shl 20
        const val FOCUS_CHANGE: Int = 1 shl 21
        const val PROPERTY_CHANGE: Int = 1 shl 22
        const val COLORMAP_CHANGE: Int = 1 shl 23
        const val OWNER_GRAB_BUTTON: Int = 1 shl 24
    }

    protected val code: Byte = code.toByte()

    @Throws(IOException::class)
    abstract fun send(sequenceNumber: Short, outputStream: XOutputStream)
}
