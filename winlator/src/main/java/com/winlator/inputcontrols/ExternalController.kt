package com.winlator.inputcontrols

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import kotlin.math.abs
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class ExternalController {

    @JvmField
    var name: String? = null

    var id: String? = null

    private var deviceId = -1

    private val controllerBindings = ArrayList<ExternalControllerBinding>()

    @JvmField
    val state: GamepadState = GamepadState()

    fun getDeviceId(): Int {
        if (this.deviceId == -1) {
            for (deviceId in InputDevice.getDeviceIds()) {
                val device = InputDevice.getDevice(deviceId)
                if (device != null && device.descriptor == id) {
                    this.deviceId = deviceId
                    break
                }
            }
        }
        return this.deviceId
    }

    val isConnected: Boolean
        get() {
            for (deviceId in InputDevice.getDeviceIds()) {
                val device = InputDevice.getDevice(deviceId)
                if (device != null && device.descriptor == id) return true
            }
            return false
        }

    fun getControllerBinding(keyCode: Int): ExternalControllerBinding? {
        for (controllerBinding in controllerBindings) {
            if (controllerBinding.keyCodeForAxis == keyCode) return controllerBinding
        }
        return null
    }

    fun getControllerBindingAt(index: Int): ExternalControllerBinding {
        return controllerBindings[index]
    }

    fun addControllerBinding(controllerBinding: ExternalControllerBinding) {
        if (getControllerBinding(controllerBinding.keyCodeForAxis) == null) {
            controllerBindings.add(
                controllerBinding,
            )
        }
    }

    fun getPosition(controllerBinding: ExternalControllerBinding): Int {
        return controllerBindings.indexOf(controllerBinding)
    }

    fun removeControllerBinding(controllerBinding: ExternalControllerBinding) {
        controllerBindings.remove(controllerBinding)
    }

    val controllerBindingCount: Int
        get() = controllerBindings.size

    fun toJSONObject(): JSONObject? {
        try {
            if (controllerBindings.isEmpty()) return null
            val controllerJSONObject = JSONObject()
            controllerJSONObject.put("id", id)
            controllerJSONObject.put("name", name)

            val controllerBindingsJSONArray = JSONArray()

            for (controllerBinding in controllerBindings) {
                controllerBindingsJSONArray.put(controllerBinding.toJSONObject())
            }

            controllerJSONObject.put("controllerBindings", controllerBindingsJSONArray)

            return controllerJSONObject
        } catch (e: JSONException) {
            return null
        }
    }

    override fun equals(other: Any?): Boolean = if (other is ExternalController) (other.id == this.id) else super.equals(other)

    private fun processJoystickInput(event: MotionEvent, historyPos: Int) {
        state.thumbLX = getCenteredAxis(event, MotionEvent.AXIS_X, historyPos)
        state.thumbLY = getCenteredAxis(event, MotionEvent.AXIS_Y, historyPos)
        state.thumbRX = getCenteredAxis(event, MotionEvent.AXIS_Z, historyPos)
        state.thumbRY = getCenteredAxis(event, MotionEvent.AXIS_RZ, historyPos)

        if (historyPos == -1) {
            val axisX = getCenteredAxis(event, MotionEvent.AXIS_HAT_X, historyPos)
            val axisY = getCenteredAxis(event, MotionEvent.AXIS_HAT_Y, historyPos)

            state.dpad[0] = axisY == -1.0f && abs(state.thumbLY.toDouble()) < ControlElement.STICK_DEAD_ZONE
            state.dpad[1] = axisX == 1.0f && abs(state.thumbLX.toDouble()) < ControlElement.STICK_DEAD_ZONE
            state.dpad[2] = axisY == 1.0f && abs(state.thumbLY.toDouble()) < ControlElement.STICK_DEAD_ZONE
            state.dpad[3] = axisX == -1.0f && abs(state.thumbLX.toDouble()) < ControlElement.STICK_DEAD_ZONE
        }
    }

    private fun processTriggerButton(event: MotionEvent) {
        state.setPressed(
            buttonIdx = IDX_BUTTON_L2.toInt(),
            pressed = event.getAxisValue(MotionEvent.AXIS_LTRIGGER) == 1.0f || event.getAxisValue(MotionEvent.AXIS_BRAKE) == 1.0f,
        )
        state.setPressed(
            buttonIdx = IDX_BUTTON_R2.toInt(),
            pressed = event.getAxisValue(MotionEvent.AXIS_RTRIGGER) == 1.0f || event.getAxisValue(MotionEvent.AXIS_GAS) == 1.0f,
        )
    }

    fun updateStateFromMotionEvent(event: MotionEvent): Boolean {
        if (isJoystickDevice(event)) {
            processTriggerButton(event)

            val historySize = event.historySize

            for (i in 0..<historySize) {
                processJoystickInput(event, i)
            }

            processJoystickInput(event, -1)

            return true
        }

        return false
    }

    fun updateStateFromKeyEvent(event: KeyEvent): Boolean {
        val pressed = event.action == KeyEvent.ACTION_DOWN
        val keyCode = event.keyCode
        val buttonIdx = getButtonIdxByKeyCode(keyCode)

        if (buttonIdx != -1) {
            state.setPressed(buttonIdx, pressed)
            return true
        }

        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                state.dpad[0] = pressed && abs(state.thumbLY.toDouble()) < ControlElement.STICK_DEAD_ZONE
                return true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                state.dpad[1] = pressed && abs(state.thumbLX.toDouble()) < ControlElement.STICK_DEAD_ZONE
                return true
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                state.dpad[2] = pressed && abs(state.thumbLY.toDouble()) < ControlElement.STICK_DEAD_ZONE
                return true
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                state.dpad[3] = pressed && abs(state.thumbLX.toDouble()) < ControlElement.STICK_DEAD_ZONE
                return true
            }
        }

        return false
    }

    companion object {
        const val IDX_BUTTON_A: Byte = 0
        const val IDX_BUTTON_B: Byte = 1
        const val IDX_BUTTON_X: Byte = 2
        const val IDX_BUTTON_Y: Byte = 3
        const val IDX_BUTTON_L1: Byte = 4
        const val IDX_BUTTON_R1: Byte = 5
        const val IDX_BUTTON_SELECT: Byte = 6
        const val IDX_BUTTON_START: Byte = 7
        const val IDX_BUTTON_L3: Byte = 8
        const val IDX_BUTTON_R3: Byte = 9
        const val IDX_BUTTON_L2: Byte = 10
        const val IDX_BUTTON_R2: Byte = 11

        val controllers: ArrayList<ExternalController>
            get() {
                val deviceIds = InputDevice.getDeviceIds()
                val controllers = ArrayList<ExternalController>()

                for (i in deviceIds.indices.reversed()) {
                    val device = InputDevice.getDevice(deviceIds[i])

                    if (isGameController(device)) {
                        ExternalController().apply {
                            id = device!!.descriptor
                            name = device.name
                        }.also(controllers::add)
                    }
                }

                return controllers
            }

        fun getController(id: String): ExternalController? {
            for (controller in controllers) {
                if (controller.id == id) {
                    return controller
                }
            }

            return null
        }

        @JvmStatic
        fun getController(deviceId: Int): ExternalController? {
            val deviceIds = InputDevice.getDeviceIds()

            for (i in deviceIds.indices.reversed()) {
                if (deviceIds[i] == deviceId || deviceId == 0) {
                    val device = InputDevice.getDevice(deviceIds[i])

                    if (isGameController(device)) {
                        val controller = ExternalController().apply {
                            this.id = device!!.descriptor
                            this.name = device.name
                            this.deviceId = deviceIds[i]
                        }

                        return controller
                    }
                }
            }

            return null
        }

        @JvmStatic
        fun isGameController(device: InputDevice?): Boolean {
            if (device == null) {
                return false
            }

            val sources = device.sources

            return !device.isVirtual &&
                (
                    (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                        (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                    )
        }

        fun getCenteredAxis(event: MotionEvent, axis: Int, historyPos: Int): Float {
            if (axis == MotionEvent.AXIS_HAT_X || axis == MotionEvent.AXIS_HAT_Y) {
                val value = event.getAxisValue(axis)
                if (abs(value.toDouble()) == 1.0) {
                    return value
                }
            } else {
                val device = event.device
                val range = device.getMotionRange(axis, event.source)

                if (range != null) {
                    val flat = range.flat
                    val value = if (historyPos < 0) {
                        event.getAxisValue(axis)
                    } else {
                        event.getHistoricalAxisValue(axis, historyPos)
                    }

                    if (abs(value.toDouble()) > flat) {
                        return value
                    }
                }
            }

            return 0f
        }

        fun isJoystickDevice(event: MotionEvent): Boolean =
            (event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK && event.action == MotionEvent.ACTION_MOVE

        fun getButtonIdxByKeyCode(keyCode: Int): Int = when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> IDX_BUTTON_A.toInt()
            KeyEvent.KEYCODE_BUTTON_B -> IDX_BUTTON_B.toInt()
            KeyEvent.KEYCODE_BUTTON_X -> IDX_BUTTON_X.toInt()
            KeyEvent.KEYCODE_BUTTON_Y -> IDX_BUTTON_Y.toInt()
            KeyEvent.KEYCODE_BUTTON_L1 -> IDX_BUTTON_L1.toInt()
            KeyEvent.KEYCODE_BUTTON_R1 -> IDX_BUTTON_R1.toInt()
            KeyEvent.KEYCODE_BUTTON_SELECT -> IDX_BUTTON_SELECT.toInt()
            KeyEvent.KEYCODE_BUTTON_START -> IDX_BUTTON_START.toInt()
            KeyEvent.KEYCODE_BUTTON_THUMBL -> IDX_BUTTON_L3.toInt()
            KeyEvent.KEYCODE_BUTTON_THUMBR -> IDX_BUTTON_R3.toInt()
            KeyEvent.KEYCODE_BUTTON_L2 -> IDX_BUTTON_L2.toInt()
            KeyEvent.KEYCODE_BUTTON_R2 -> IDX_BUTTON_R2.toInt()
            else -> -1
        }

        fun getButtonIdxByName(name: String): Int = when (name) {
            "A" -> IDX_BUTTON_A.toInt()
            "B" -> IDX_BUTTON_B.toInt()
            "X" -> IDX_BUTTON_X.toInt()
            "Y" -> IDX_BUTTON_Y.toInt()
            "L1" -> IDX_BUTTON_L1.toInt()
            "R1" -> IDX_BUTTON_R1.toInt()
            "SELECT" -> IDX_BUTTON_SELECT.toInt()
            "START" -> IDX_BUTTON_START.toInt()
            "L3" -> IDX_BUTTON_L3.toInt()
            "R3" -> IDX_BUTTON_R3.toInt()
            "L2" -> IDX_BUTTON_L2.toInt()
            "R2" -> IDX_BUTTON_R2.toInt()
            else -> -1
        }
    }
}
