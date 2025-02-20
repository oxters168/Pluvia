package com.winlator.xserver

import android.view.InputDevice
import android.view.KeyEvent
import androidx.collection.ArraySet
import com.winlator.inputcontrols.ExternalController.Companion.isGameController

class Keyboard(private val xServer: XServer) {

    companion object {
        const val KEYSYMS_PER_KEYCODE: Byte = 2
        const val KEYS_COUNT: Short = 248
        const val MAX_KEYCODE: Short = 255
        const val MIN_KEYCODE: Short = 8

        fun isKeyboardDevice(device: InputDevice?): Boolean {
            if (device == null) {
                return false
            }

            val sources = device.sources

            return !device.isVirtual && ((sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD)
        }

        private fun createKeycodeMap(): Array<XKeycode?> {
            return arrayOfNulls<XKeycode>(KeyEvent.getMaxKeyCode() + 1).apply {
                this[KeyEvent.KEYCODE_ENTER] = XKeycode.KEY_ENTER
                this[KeyEvent.KEYCODE_DPAD_LEFT] = XKeycode.KEY_LEFT
                this[KeyEvent.KEYCODE_DPAD_RIGHT] = XKeycode.KEY_RIGHT
                this[KeyEvent.KEYCODE_DPAD_UP] = XKeycode.KEY_UP
                this[KeyEvent.KEYCODE_DPAD_DOWN] = XKeycode.KEY_DOWN
                this[KeyEvent.KEYCODE_DEL] = XKeycode.KEY_BKSP
                this[KeyEvent.KEYCODE_INSERT] = XKeycode.KEY_INSERT
                this[KeyEvent.KEYCODE_FORWARD_DEL] = XKeycode.KEY_DEL
                this[KeyEvent.KEYCODE_MOVE_HOME] = XKeycode.KEY_HOME
                this[KeyEvent.KEYCODE_MOVE_END] = XKeycode.KEY_END
                this[KeyEvent.KEYCODE_PAGE_UP] = XKeycode.KEY_PRIOR
                this[KeyEvent.KEYCODE_PAGE_DOWN] = XKeycode.KEY_NEXT
                this[KeyEvent.KEYCODE_SHIFT_LEFT] = XKeycode.KEY_SHIFT_L
                this[KeyEvent.KEYCODE_SHIFT_RIGHT] = XKeycode.KEY_SHIFT_R
                this[KeyEvent.KEYCODE_CTRL_LEFT] = XKeycode.KEY_CTRL_L
                this[KeyEvent.KEYCODE_CTRL_RIGHT] = XKeycode.KEY_CTRL_R
                this[KeyEvent.KEYCODE_ALT_LEFT] = XKeycode.KEY_ALT_L
                this[KeyEvent.KEYCODE_ALT_RIGHT] = XKeycode.KEY_ALT_R
                this[KeyEvent.KEYCODE_TAB] = XKeycode.KEY_TAB
                this[KeyEvent.KEYCODE_SPACE] = XKeycode.KEY_SPACE
                this[KeyEvent.KEYCODE_A] = XKeycode.KEY_A
                this[KeyEvent.KEYCODE_B] = XKeycode.KEY_B
                this[KeyEvent.KEYCODE_C] = XKeycode.KEY_C
                this[KeyEvent.KEYCODE_D] = XKeycode.KEY_D
                this[KeyEvent.KEYCODE_E] = XKeycode.KEY_E
                this[KeyEvent.KEYCODE_F] = XKeycode.KEY_F
                this[KeyEvent.KEYCODE_G] = XKeycode.KEY_G
                this[KeyEvent.KEYCODE_H] = XKeycode.KEY_H
                this[KeyEvent.KEYCODE_I] = XKeycode.KEY_I
                this[KeyEvent.KEYCODE_J] = XKeycode.KEY_J
                this[KeyEvent.KEYCODE_K] = XKeycode.KEY_K
                this[KeyEvent.KEYCODE_L] = XKeycode.KEY_L
                this[KeyEvent.KEYCODE_M] = XKeycode.KEY_M
                this[KeyEvent.KEYCODE_N] = XKeycode.KEY_N
                this[KeyEvent.KEYCODE_O] = XKeycode.KEY_O
                this[KeyEvent.KEYCODE_P] = XKeycode.KEY_P
                this[KeyEvent.KEYCODE_Q] = XKeycode.KEY_Q
                this[KeyEvent.KEYCODE_R] = XKeycode.KEY_R
                this[KeyEvent.KEYCODE_S] = XKeycode.KEY_S
                this[KeyEvent.KEYCODE_T] = XKeycode.KEY_T
                this[KeyEvent.KEYCODE_U] = XKeycode.KEY_U
                this[KeyEvent.KEYCODE_V] = XKeycode.KEY_V
                this[KeyEvent.KEYCODE_W] = XKeycode.KEY_W
                this[KeyEvent.KEYCODE_X] = XKeycode.KEY_X
                this[KeyEvent.KEYCODE_Y] = XKeycode.KEY_Y
                this[KeyEvent.KEYCODE_Z] = XKeycode.KEY_Z
                this[KeyEvent.KEYCODE_0] = XKeycode.KEY_0
                this[KeyEvent.KEYCODE_1] = XKeycode.KEY_1
                this[KeyEvent.KEYCODE_2] = XKeycode.KEY_2
                this[KeyEvent.KEYCODE_3] = XKeycode.KEY_3
                this[KeyEvent.KEYCODE_4] = XKeycode.KEY_4
                this[KeyEvent.KEYCODE_5] = XKeycode.KEY_5
                this[KeyEvent.KEYCODE_6] = XKeycode.KEY_6
                this[KeyEvent.KEYCODE_7] = XKeycode.KEY_7
                this[KeyEvent.KEYCODE_8] = XKeycode.KEY_8
                this[KeyEvent.KEYCODE_9] = XKeycode.KEY_9
                this[KeyEvent.KEYCODE_STAR] = XKeycode.KEY_8
                this[KeyEvent.KEYCODE_POUND] = XKeycode.KEY_3
                this[KeyEvent.KEYCODE_COMMA] = XKeycode.KEY_COMMA
                this[KeyEvent.KEYCODE_PERIOD] = XKeycode.KEY_PERIOD
                this[KeyEvent.KEYCODE_SEMICOLON] = XKeycode.KEY_SEMICOLON
                this[KeyEvent.KEYCODE_APOSTROPHE] = XKeycode.KEY_APOSTROPHE
                this[KeyEvent.KEYCODE_LEFT_BRACKET] = XKeycode.KEY_BRACKET_LEFT
                this[KeyEvent.KEYCODE_RIGHT_BRACKET] = XKeycode.KEY_BRACKET_RIGHT
                this[KeyEvent.KEYCODE_GRAVE] = XKeycode.KEY_GRAVE
                this[KeyEvent.KEYCODE_MINUS] = XKeycode.KEY_MINUS
                this[KeyEvent.KEYCODE_PLUS] = XKeycode.KEY_EQUAL
                this[KeyEvent.KEYCODE_EQUALS] = XKeycode.KEY_EQUAL
                this[KeyEvent.KEYCODE_SLASH] = XKeycode.KEY_SLASH
                this[KeyEvent.KEYCODE_AT] = XKeycode.KEY_2
                this[KeyEvent.KEYCODE_BACKSLASH] = XKeycode.KEY_BACKSLASH
                this[KeyEvent.KEYCODE_NUMPAD_DIVIDE] = XKeycode.KEY_KP_DIVIDE
                this[KeyEvent.KEYCODE_NUMPAD_MULTIPLY] = XKeycode.KEY_KP_MULTIPLY
                this[KeyEvent.KEYCODE_NUMPAD_SUBTRACT] = XKeycode.KEY_KP_SUBTRACT
                this[KeyEvent.KEYCODE_NUMPAD_ADD] = XKeycode.KEY_KP_ADD
                this[KeyEvent.KEYCODE_NUMPAD_DOT] = XKeycode.KEY_KP_DEL
                this[KeyEvent.KEYCODE_NUMPAD_0] = XKeycode.KEY_KP_0
                this[KeyEvent.KEYCODE_NUMPAD_1] = XKeycode.KEY_KP_1
                this[KeyEvent.KEYCODE_NUMPAD_2] = XKeycode.KEY_KP_2
                this[KeyEvent.KEYCODE_NUMPAD_3] = XKeycode.KEY_KP_3
                this[KeyEvent.KEYCODE_NUMPAD_4] = XKeycode.KEY_KP_4
                this[KeyEvent.KEYCODE_NUMPAD_5] = XKeycode.KEY_KP_5
                this[KeyEvent.KEYCODE_NUMPAD_6] = XKeycode.KEY_KP_6
                this[KeyEvent.KEYCODE_NUMPAD_7] = XKeycode.KEY_KP_7
                this[KeyEvent.KEYCODE_NUMPAD_8] = XKeycode.KEY_KP_8
                this[KeyEvent.KEYCODE_NUMPAD_9] = XKeycode.KEY_KP_9
                this[KeyEvent.KEYCODE_F1] = XKeycode.KEY_F1
                this[KeyEvent.KEYCODE_F2] = XKeycode.KEY_F2
                this[KeyEvent.KEYCODE_F3] = XKeycode.KEY_F3
                this[KeyEvent.KEYCODE_F4] = XKeycode.KEY_F4
                this[KeyEvent.KEYCODE_F5] = XKeycode.KEY_F5
                this[KeyEvent.KEYCODE_F6] = XKeycode.KEY_F6
                this[KeyEvent.KEYCODE_F7] = XKeycode.KEY_F7
                this[KeyEvent.KEYCODE_F8] = XKeycode.KEY_F8
                this[KeyEvent.KEYCODE_F9] = XKeycode.KEY_F9
                this[KeyEvent.KEYCODE_F10] = XKeycode.KEY_F10
                this[KeyEvent.KEYCODE_F11] = XKeycode.KEY_F11
                this[KeyEvent.KEYCODE_F12] = XKeycode.KEY_F12
                this[KeyEvent.KEYCODE_NUM_LOCK] = XKeycode.KEY_NUM_LOCK
                this[KeyEvent.KEYCODE_CAPS_LOCK] = XKeycode.KEY_CAPS_LOCK
            }
        }

        fun createKeyboard(xServer: XServer): Keyboard {
            return Keyboard(xServer).apply {
                setKeysyms(XKeycode.KEY_ESC.id, 65307, 0)
                setKeysyms(XKeycode.KEY_ENTER.id, 65293, 0)
                setKeysyms(XKeycode.KEY_RIGHT.id, 65363, 0)
                setKeysyms(XKeycode.KEY_UP.id, 65362, 0)
                setKeysyms(XKeycode.KEY_LEFT.id, 65361, 0)
                setKeysyms(XKeycode.KEY_DOWN.id, 65364, 0)
                setKeysyms(XKeycode.KEY_DEL.id, 65535, 0)
                setKeysyms(XKeycode.KEY_BKSP.id, 65288, 0)
                setKeysyms(XKeycode.KEY_INSERT.id, 65379, 0)
                setKeysyms(XKeycode.KEY_PRIOR.id, 65365, 0)
                setKeysyms(XKeycode.KEY_NEXT.id, 65366, 0)
                setKeysyms(XKeycode.KEY_HOME.id, 65360, 0)
                setKeysyms(XKeycode.KEY_END.id, 65367, 0)
                setKeysyms(XKeycode.KEY_SHIFT_L.id, 65505, 0)
                setKeysyms(XKeycode.KEY_SHIFT_R.id, 65506, 0)
                setKeysyms(XKeycode.KEY_CTRL_L.id, 65507, 0)
                setKeysyms(XKeycode.KEY_CTRL_R.id, 65508, 0)
                setKeysyms(XKeycode.KEY_ALT_L.id, 65511, 0)
                setKeysyms(XKeycode.KEY_ALT_R.id, 65512, 0)
                setKeysyms(XKeycode.KEY_TAB.id, 65289, 0)
                setKeysyms(XKeycode.KEY_SPACE.id, 32, 32)
                setKeysyms(XKeycode.KEY_A.id, 97, 65)
                setKeysyms(XKeycode.KEY_B.id, 98, 66)
                setKeysyms(XKeycode.KEY_C.id, 99, 67)
                setKeysyms(XKeycode.KEY_D.id, 100, 68)
                setKeysyms(XKeycode.KEY_E.id, 101, 69)
                setKeysyms(XKeycode.KEY_F.id, 102, 70)
                setKeysyms(XKeycode.KEY_G.id, 103, 71)
                setKeysyms(XKeycode.KEY_H.id, 104, 72)
                setKeysyms(XKeycode.KEY_I.id, 105, 73)
                setKeysyms(XKeycode.KEY_J.id, 106, 74)
                setKeysyms(XKeycode.KEY_K.id, 107, 75)
                setKeysyms(XKeycode.KEY_L.id, 108, 76)
                setKeysyms(XKeycode.KEY_M.id, 109, 77)
                setKeysyms(XKeycode.KEY_N.id, 110, 78)
                setKeysyms(XKeycode.KEY_O.id, 111, 79)
                setKeysyms(XKeycode.KEY_P.id, 112, 80)
                setKeysyms(XKeycode.KEY_Q.id, 113, 81)
                setKeysyms(XKeycode.KEY_R.id, 114, 82)
                setKeysyms(XKeycode.KEY_S.id, 115, 83)
                setKeysyms(XKeycode.KEY_T.id, 116, 84)
                setKeysyms(XKeycode.KEY_U.id, 117, 85)
                setKeysyms(XKeycode.KEY_V.id, 118, 86)
                setKeysyms(XKeycode.KEY_W.id, 119, 87)
                setKeysyms(XKeycode.KEY_X.id, 120, 88)
                setKeysyms(XKeycode.KEY_Y.id, 121, 89)
                setKeysyms(XKeycode.KEY_Z.id, 122, 90)
                setKeysyms(XKeycode.KEY_1.id, 49, 33)
                setKeysyms(XKeycode.KEY_2.id, 50, 64)
                setKeysyms(XKeycode.KEY_3.id, 51, 35)
                setKeysyms(XKeycode.KEY_4.id, 52, 36)
                setKeysyms(XKeycode.KEY_5.id, 53, 37)
                setKeysyms(XKeycode.KEY_6.id, 54, 94)
                setKeysyms(XKeycode.KEY_7.id, 55, 38)
                setKeysyms(XKeycode.KEY_8.id, 56, 42)
                setKeysyms(XKeycode.KEY_9.id, 57, 40)
                setKeysyms(XKeycode.KEY_0.id, 48, 41)
                setKeysyms(XKeycode.KEY_COMMA.id, 44, 60)
                setKeysyms(XKeycode.KEY_PERIOD.id, 46, 62)
                setKeysyms(XKeycode.KEY_SEMICOLON.id, 59, 58)
                setKeysyms(XKeycode.KEY_APOSTROPHE.id, 39, 34)
                setKeysyms(XKeycode.KEY_BRACKET_LEFT.id, 91, 123)
                setKeysyms(XKeycode.KEY_BRACKET_RIGHT.id, 93, 125)
                setKeysyms(XKeycode.KEY_GRAVE.id, 96, 126)
                setKeysyms(XKeycode.KEY_MINUS.id, 45, 95)
                setKeysyms(XKeycode.KEY_EQUAL.id, 61, 43)
                setKeysyms(XKeycode.KEY_SLASH.id, 47, 63)
                setKeysyms(XKeycode.KEY_BACKSLASH.id, 92, 124)
                setKeysyms(XKeycode.KEY_KP_DIVIDE.id, 65455, 65455)
                setKeysyms(XKeycode.KEY_KP_MULTIPLY.id, 65450, 65450)
                setKeysyms(XKeycode.KEY_KP_SUBTRACT.id, 65453, 65453)
                setKeysyms(XKeycode.KEY_KP_ADD.id, 65451, 65451)
                setKeysyms(XKeycode.KEY_KP_0.id, 65456, 65438)
                setKeysyms(XKeycode.KEY_KP_1.id, 65457, 65436)
                setKeysyms(XKeycode.KEY_KP_2.id, 65458, 65433)
                setKeysyms(XKeycode.KEY_KP_3.id, 65459, 65459)
                setKeysyms(XKeycode.KEY_KP_4.id, 65460, 65430)
                setKeysyms(XKeycode.KEY_KP_5.id, 65461, 65461)
                setKeysyms(XKeycode.KEY_KP_6.id, 65462, 65432)
                setKeysyms(XKeycode.KEY_KP_7.id, 65463, 65429)
                setKeysyms(XKeycode.KEY_KP_8.id, 65464, 65431)
                setKeysyms(XKeycode.KEY_KP_9.id, 65465, 65465)
                setKeysyms(XKeycode.KEY_KP_DEL.id, 65439, 0)
                setKeysyms(XKeycode.KEY_F1.id, 65470, 0)
                setKeysyms(XKeycode.KEY_F2.id, 65471, 0)
                setKeysyms(XKeycode.KEY_F3.id, 65472, 0)
                setKeysyms(XKeycode.KEY_F4.id, 65473, 0)
                setKeysyms(XKeycode.KEY_F5.id, 65474, 0)
                setKeysyms(XKeycode.KEY_F6.id, 65475, 0)
                setKeysyms(XKeycode.KEY_F7.id, 65476, 0)
                setKeysyms(XKeycode.KEY_F8.id, 65477, 0)
                setKeysyms(XKeycode.KEY_F9.id, 65478, 0)
                setKeysyms(XKeycode.KEY_F10.id, 65479, 0)
                setKeysyms(XKeycode.KEY_F11.id, 65480, 0)
                setKeysyms(XKeycode.KEY_F12.id, 65481, 0)
            }
        }

        fun isModifier(keycode: Byte): Boolean = keycode == XKeycode.KEY_SHIFT_L.id ||
            keycode == XKeycode.KEY_SHIFT_R.id ||
            keycode == XKeycode.KEY_CTRL_L.id ||
            keycode == XKeycode.KEY_CTRL_R.id ||
            keycode == XKeycode.KEY_ALT_L.id ||
            keycode == XKeycode.KEY_ALT_R.id ||
            keycode == XKeycode.KEY_CAPS_LOCK.id ||
            keycode == XKeycode.KEY_NUM_LOCK.id

        fun getModifierFlag(keycode: Byte): Int {
            if (keycode == XKeycode.KEY_SHIFT_L.id || keycode == XKeycode.KEY_SHIFT_R.id) {
                return 1
            } else if (keycode == XKeycode.KEY_CAPS_LOCK.id) {
                return 2
            } else if (keycode == XKeycode.KEY_CTRL_L.id || keycode == XKeycode.KEY_CTRL_R.id) {
                return 4
            } else if (keycode == XKeycode.KEY_ALT_L.id || keycode == XKeycode.KEY_ALT_R.id) {
                return 8
            } else if (keycode == XKeycode.KEY_NUM_LOCK.id) {
                return 16
            }

            return 0
        }

        fun isModifierSticky(keycode: Byte): Boolean = keycode == XKeycode.KEY_CAPS_LOCK.id || keycode == XKeycode.KEY_NUM_LOCK.id
    }

    val keysyms: IntArray = IntArray(KEYS_COUNT.toInt())

    val modifiersMask: Bitmask = Bitmask()

    private val keycodeMap: Array<XKeycode?> = createKeycodeMap()

    private val pressedKeys = ArraySet<Byte?>()

    private val onKeyboardListeners = ArrayList<OnKeyboardListener?>()

    interface OnKeyboardListener {
        fun onKeyPress(keycode: Byte, keysym: Int)

        fun onKeyRelease(keycode: Byte)
    }

    fun setKeysyms(keycode: Byte, minKeysym: Int, majKeysym: Int) {
        val index = keycode - 8

        keysyms[index * KEYSYMS_PER_KEYCODE + 0] = minKeysym
        keysyms[index * KEYSYMS_PER_KEYCODE + 1] = majKeysym
    }

    fun hasKeysym(keycode: Byte, keysym: Int): Boolean {
        val index = keycode - 8
        return keysyms[index * KEYSYMS_PER_KEYCODE + 0] == keysym || keysyms[index * KEYSYMS_PER_KEYCODE + 1] == keysym
    }

    fun setKeyPress(keycode: Byte, keysym: Int) {
        if (isModifierSticky(keycode)) {
            if (pressedKeys.contains(keycode)) {
                pressedKeys.remove(keycode)
                modifiersMask.unset(getModifierFlag(keycode))
                triggerOnKeyRelease(keycode)
            } else {
                pressedKeys.add(keycode)
                modifiersMask.set(getModifierFlag(keycode))
                triggerOnKeyPress(keycode, keysym)
            }
        } else if (!pressedKeys.contains(keycode)) {
            pressedKeys.add(keycode)

            if (isModifier(keycode)) {
                modifiersMask.set(getModifierFlag(keycode))
            }

            triggerOnKeyPress(keycode, keysym)
        }
    }

    fun setKeyRelease(keycode: Byte) {
        if (!isModifierSticky(keycode) && pressedKeys.contains(keycode)) {
            pressedKeys.remove(keycode)

            if (isModifier(keycode)) {
                modifiersMask.unset(getModifierFlag(keycode))
            }

            triggerOnKeyRelease(keycode)
        }
    }

    fun addOnKeyboardListener(onKeyboardListener: OnKeyboardListener?) {
        onKeyboardListeners.add(onKeyboardListener)
    }

    fun removeOnKeyboardListener(onKeyboardListener: OnKeyboardListener?) {
        onKeyboardListeners.remove(onKeyboardListener)
    }

    private fun triggerOnKeyPress(keycode: Byte, keysym: Int) {
        onKeyboardListeners.indices.reversed().forEach {
            onKeyboardListeners[it]!!.onKeyPress(keycode, keysym)
        }
    }

    private fun triggerOnKeyRelease(keycode: Byte) {
        onKeyboardListeners.indices.reversed().forEach {
            onKeyboardListeners[it]!!.onKeyRelease(keycode)
        }
    }

    fun onKeyEvent(event: KeyEvent): Boolean {
        if (isGameController(event.device)) {
            return false
        }

        val action = event.action
        if (action == KeyEvent.ACTION_DOWN || action == KeyEvent.ACTION_UP) {
            val keyCode = event.keyCode
            val xKeycode = keycodeMap[keyCode]

            if (xKeycode == null) {
                return false
            }

            if (action == KeyEvent.ACTION_DOWN) {
                val shiftPressed =
                    event.isShiftPressed ||
                        keyCode == KeyEvent.KEYCODE_AT ||
                        keyCode == KeyEvent.KEYCODE_STAR ||
                        keyCode == KeyEvent.KEYCODE_POUND ||
                        keyCode == KeyEvent.KEYCODE_PLUS

                if (shiftPressed) {
                    xServer.injectKeyPress(XKeycode.KEY_SHIFT_L)
                }

                xServer.injectKeyPress(xKeycode, if (xKeycode != XKeycode.KEY_ENTER) event.unicodeChar else 0)
            } else if (action == KeyEvent.ACTION_UP) {
                xServer.injectKeyRelease(XKeycode.KEY_SHIFT_L)
                xServer.injectKeyRelease(xKeycode)
            }
        }

        return true
    }
}
