package com.winlator.inputcontrols

import android.view.KeyEvent
import android.view.MotionEvent
import org.json.JSONException
import org.json.JSONObject

class ExternalControllerBinding {

    private var keyCode: Short = 0

    var binding: Binding = Binding.NONE

    val keyCodeForAxis: Int
        get() = keyCode.toInt()

    fun setKeyCode(keyCode: Int) {
        this.keyCode = keyCode.toShort()
    }

    fun toJSONObject(): JSONObject? = try {
        val controllerBindingJSONObject = JSONObject()
        controllerBindingJSONObject.put("keyCode", keyCode.toInt())
        controllerBindingJSONObject.put("binding", binding.name)

        controllerBindingJSONObject
    } catch (e: JSONException) {
        null
    }

    override fun toString(): String {
        return when (keyCode) {
            AXIS_X_NEGATIVE.toShort() -> "AXIS X-"
            AXIS_X_POSITIVE.toShort() -> "AXIS X+"
            AXIS_Y_NEGATIVE.toShort() -> "AXIS Y-"
            AXIS_Y_POSITIVE.toShort() -> "AXIS Y+"
            AXIS_Z_NEGATIVE.toShort() -> "AXIS Z-"
            AXIS_Z_POSITIVE.toShort() -> "AXIS Z+"
            AXIS_RZ_NEGATIVE.toShort() -> "AXIS RZ-"
            AXIS_RZ_POSITIVE.toShort() -> "AXIS RZ+"
            else -> KeyEvent.keyCodeToString(keyCode.toInt()).replace("KEYCODE_", "").replace("_", " ")
        }
    }

    companion object {
        const val AXIS_X_NEGATIVE: Byte = -1
        const val AXIS_X_POSITIVE: Byte = -2
        const val AXIS_Y_NEGATIVE: Byte = -3
        const val AXIS_Y_POSITIVE: Byte = -4
        const val AXIS_Z_NEGATIVE: Byte = -5
        const val AXIS_Z_POSITIVE: Byte = -6
        const val AXIS_RZ_NEGATIVE: Byte = -7
        const val AXIS_RZ_POSITIVE: Byte = -8

        fun getKeyCodeForAxis(axis: Int, sign: Byte): Int = when (axis) {
            MotionEvent.AXIS_X -> (if (sign > 0) AXIS_X_POSITIVE else AXIS_X_NEGATIVE).toInt()
            MotionEvent.AXIS_Y -> (if (sign > 0) AXIS_Y_NEGATIVE else AXIS_Y_POSITIVE).toInt()
            MotionEvent.AXIS_Z -> (if (sign > 0) AXIS_Z_POSITIVE else AXIS_Z_NEGATIVE).toInt()
            MotionEvent.AXIS_RZ -> (if (sign > 0) AXIS_RZ_NEGATIVE else AXIS_RZ_POSITIVE).toInt()
            MotionEvent.AXIS_HAT_X -> if (sign > 0) KeyEvent.KEYCODE_DPAD_RIGHT else KeyEvent.KEYCODE_DPAD_LEFT
            MotionEvent.AXIS_HAT_Y -> if (sign > 0) KeyEvent.KEYCODE_DPAD_DOWN else KeyEvent.KEYCODE_DPAD_UP
            else -> KeyEvent.KEYCODE_UNKNOWN
        }
    }
}
