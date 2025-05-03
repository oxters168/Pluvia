package com.micewine.emu.controller

import android.content.Context
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.micewine.emu.LorieView
import com.micewine.emu.MiceWineUtils
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_DOWN
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_LEFT
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_LEFT_DOWN
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_LEFT_UP
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_RIGHT
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_RIGHT_DOWN
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_RIGHT_UP
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_UP
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.connectController
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.connectedControllers
import com.micewine.emu.controller.XKeyCodes.getXKeyScanCodes
import com.micewine.emu.input.InputStub
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.nio.ByteBuffer
import kotlin.math.absoluteValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber

object ControllerUtils {
    const val KEYBOARD = 0
    const val MOUSE = 1

    private var virtualMouseMovingState: Int? = null
    private var axisXVelocity: Float = 0F
    private var axisYVelocity: Float = 0F
    private var lastUsedControllerIndex = 0
    private lateinit var lorieView: LorieView

    private const val LEFT = 1
    private const val RIGHT = 2
    private const val UP = 3
    private const val DOWN = 4
    private const val LEFT_UP = 5
    private const val LEFT_DOWN = 6
    private const val RIGHT_UP = 7
    private const val RIGHT_DOWN = 8

    fun initialize(context: Context) {
        lorieView = LorieView(context)
    }

    suspend fun controllerMouseEmulation() {
        withContext(Dispatchers.IO) {
            while (true) {
                val mouseSensibility = MiceWineUtils.ControllerPresetManager.getMouseSensibility(
                    MiceWineUtils.Shortcuts.getControllerPreset(MiceWineUtils.Game.selectedGameName, lastUsedControllerIndex),
                ).toFloat() / 100

                when (virtualMouseMovingState) {
                    LEFT -> {
                        lorieView.sendMouseEvent(
                            -10F * (axisXVelocity * mouseSensibility),
                            0F,
                            InputStub.BUTTON_UNDEFINED,
                            false,
                            true,
                        )
                    }

                    RIGHT -> {
                        lorieView.sendMouseEvent(
                            10F * (axisXVelocity * mouseSensibility),
                            0F,
                            InputStub.BUTTON_UNDEFINED,
                            false,
                            true,
                        )
                    }

                    UP -> {
                        lorieView.sendMouseEvent(
                            0F,
                            -10F * (axisYVelocity * mouseSensibility),
                            InputStub.BUTTON_UNDEFINED,
                            false,
                            true,
                        )
                    }

                    DOWN -> {
                        lorieView.sendMouseEvent(
                            0F,
                            10F * (axisYVelocity * mouseSensibility),
                            InputStub.BUTTON_UNDEFINED,
                            false,
                            true,
                        )
                    }

                    LEFT_UP -> {
                        lorieView.sendMouseEvent(
                            -10F * (axisXVelocity * mouseSensibility),
                            -10F * (axisYVelocity * mouseSensibility),
                            InputStub.BUTTON_UNDEFINED,
                            false,
                            true,
                        )
                    }

                    LEFT_DOWN -> {
                        lorieView.sendMouseEvent(
                            -10F * (axisXVelocity * mouseSensibility),
                            10F * (axisYVelocity * mouseSensibility),
                            InputStub.BUTTON_UNDEFINED,
                            false,
                            true,
                        )
                    }

                    RIGHT_UP -> {
                        lorieView.sendMouseEvent(
                            10F * (axisXVelocity * mouseSensibility),
                            -10F * (axisYVelocity * mouseSensibility),
                            InputStub.BUTTON_UNDEFINED,
                            false,
                            true,
                        )
                    }

                    RIGHT_DOWN -> {
                        lorieView.sendMouseEvent(
                            10F * (axisXVelocity * mouseSensibility),
                            10F * (axisYVelocity * mouseSensibility),
                            InputStub.BUTTON_UNDEFINED,
                            false,
                            true,
                        )
                    }
                }

                delay(20)
            }
        }
    }

    private fun detectKey(presetName: String?, key: String): List<Int> {
        var mapping = MiceWineUtils.ControllerPresetManager.getMapping(presetName ?: "default", key)

        if (presetName == "--") {
            mapping = MiceWineUtils.ControllerPresetManager.getMapping("default", key)
        }

        val keyList: List<Int>

        when (mapping[0]) {
            "M_Left" -> {
                keyList = listOf(InputStub.BUTTON_LEFT, InputStub.BUTTON_LEFT, MOUSE)
            }

            "M_Middle" -> {
                keyList = listOf(InputStub.BUTTON_MIDDLE, InputStub.BUTTON_MIDDLE, MOUSE)
            }

            "M_Right" -> {
                keyList = listOf(InputStub.BUTTON_RIGHT, InputStub.BUTTON_RIGHT, MOUSE)
            }

            "Mouse" -> {
                keyList = listOf(MOUSE, MOUSE, MOUSE)
            }

            else -> {
                keyList = getXKeyScanCodes(mapping[0])
            }
        }

        return keyList
    }

    fun prepareButtonsAxisValues() {
        connectedPhysicalControllers.forEachIndexed { index, it ->
            val presetName = MiceWineUtils.Shortcuts.getControllerPreset(MiceWineUtils.Game.selectedGameName, index)

            if (MiceWineUtils.Shortcuts.getControllerXInput(MiceWineUtils.Game.selectedGameName, index)) {
                it.mappingType = MAPPING_TYPE_XINPUT

                if (it.virtualXInputId == -1) {
                    it.virtualXInputId = connectController()
                }
            } else {
                it.mappingType = MAPPING_TYPE_KEYBOARD_MOUSE

                it.keyboardMapping.aButton = detectKey(presetName, MiceWineUtils.PresetManager.BUTTON_A_KEY)
                it.keyboardMapping.bButton = detectKey(presetName, MiceWineUtils.PresetManager.BUTTON_B_KEY)
                it.keyboardMapping.xButton = detectKey(presetName, MiceWineUtils.PresetManager.BUTTON_X_KEY)
                it.keyboardMapping.yButton = detectKey(presetName, MiceWineUtils.PresetManager.BUTTON_Y_KEY)

                it.keyboardMapping.rbButton = detectKey(presetName, MiceWineUtils.PresetManager.BUTTON_R1_KEY)
                it.keyboardMapping.rtButton = detectKey(presetName, MiceWineUtils.PresetManager.BUTTON_R2_KEY)

                it.keyboardMapping.lbButton = detectKey(presetName, MiceWineUtils.PresetManager.BUTTON_L1_KEY)
                it.keyboardMapping.ltButton = detectKey(presetName, MiceWineUtils.PresetManager.BUTTON_L2_KEY)

                it.keyboardMapping.lsButton = detectKey(presetName, MiceWineUtils.PresetManager.BUTTON_THUMBL_KEY)
                it.keyboardMapping.rsButton = detectKey(presetName, MiceWineUtils.PresetManager.BUTTON_THUMBR_KEY)

                it.keyboardMapping.startButton = detectKey(presetName, MiceWineUtils.PresetManager.BUTTON_START_KEY)
                it.keyboardMapping.selectButton = detectKey(presetName, MiceWineUtils.PresetManager.BUTTON_SELECT_KEY)

                it.keyboardMapping.leftAnalog.up = detectKey(presetName, MiceWineUtils.PresetManager.AXIS_Y_MINUS_KEY)
                it.keyboardMapping.leftAnalog.down = detectKey(presetName, MiceWineUtils.PresetManager.AXIS_Y_PLUS_KEY)
                it.keyboardMapping.leftAnalog.left = detectKey(presetName, MiceWineUtils.PresetManager.AXIS_X_MINUS_KEY)
                it.keyboardMapping.leftAnalog.right = detectKey(presetName, MiceWineUtils.PresetManager.AXIS_X_PLUS_KEY)

                it.keyboardMapping.leftAnalog.isMouseMapping = listOf(
                    detectKey(presetName, MiceWineUtils.PresetManager.AXIS_Y_MINUS_KEY),
                    detectKey(presetName, MiceWineUtils.PresetManager.AXIS_Y_PLUS_KEY),
                    detectKey(presetName, MiceWineUtils.PresetManager.AXIS_X_MINUS_KEY),
                    detectKey(presetName, MiceWineUtils.PresetManager.AXIS_X_PLUS_KEY),
                ).any { it.first() == MOUSE }

                it.keyboardMapping.rightAnalog.up = detectKey(presetName, MiceWineUtils.PresetManager.AXIS_Z_MINUS_KEY)
                it.keyboardMapping.rightAnalog.down = detectKey(presetName, MiceWineUtils.PresetManager.AXIS_Z_PLUS_KEY)
                it.keyboardMapping.rightAnalog.left = detectKey(presetName, MiceWineUtils.PresetManager.AXIS_RZ_MINUS_KEY)
                it.keyboardMapping.rightAnalog.right = detectKey(presetName, MiceWineUtils.PresetManager.AXIS_RZ_PLUS_KEY)

                it.keyboardMapping.rightAnalog.isMouseMapping = listOf(
                    detectKey(presetName, MiceWineUtils.PresetManager.AXIS_Z_MINUS_KEY),
                    detectKey(presetName, MiceWineUtils.PresetManager.AXIS_Z_PLUS_KEY),
                    detectKey(presetName, MiceWineUtils.PresetManager.AXIS_RZ_MINUS_KEY),
                    detectKey(presetName, MiceWineUtils.PresetManager.AXIS_RZ_PLUS_KEY),
                ).any { it.first() == MOUSE }

                it.keyboardMapping.dPad.up = detectKey(presetName, MiceWineUtils.PresetManager.AXIS_HAT_Y_MINUS_KEY)
                it.keyboardMapping.dPad.down = detectKey(presetName, MiceWineUtils.PresetManager.AXIS_HAT_Y_PLUS_KEY)
                it.keyboardMapping.dPad.left = detectKey(presetName, MiceWineUtils.PresetManager.AXIS_HAT_X_MINUS_KEY)
                it.keyboardMapping.dPad.right = detectKey(presetName, MiceWineUtils.PresetManager.AXIS_HAT_X_PLUS_KEY)

                it.keyboardMapping.dPad.isMouseMapping = listOf(
                    detectKey(presetName, MiceWineUtils.PresetManager.AXIS_HAT_Y_MINUS_KEY),
                    detectKey(presetName, MiceWineUtils.PresetManager.AXIS_HAT_Y_PLUS_KEY),
                    detectKey(presetName, MiceWineUtils.PresetManager.AXIS_HAT_X_MINUS_KEY),
                    detectKey(presetName, MiceWineUtils.PresetManager.AXIS_HAT_X_PLUS_KEY),
                ).any { it.first() == MOUSE }

                it.deadZone = MiceWineUtils.ControllerPresetManager.getDeadZone(presetName).toFloat() / 100
                it.mouseSensibility = MiceWineUtils.ControllerPresetManager.getMouseSensibility(presetName).toFloat() / 100
            }
        }
    }

    fun getGameControllerNames(): MutableList<PhysicalController> {
        val deviceIds = InputDevice.getDeviceIds()
        val devices = mutableListOf<PhysicalController>()

        deviceIds.forEach { deviceId ->
            InputDevice.getDevice(deviceId)?.let { device ->
                if ((device.sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) ||
                    (device.sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)
                ) {
                    devices.add(
                        PhysicalController(
                            device.name,
                            deviceId,
                            -1,
                            -1,
                        ),
                    )
                }
            }
        }

        return devices
    }

    private fun handleKey(pressed: Boolean, mapping: List<Int>) {
        when (mapping[2]) {
            KEYBOARD -> lorieView.sendKeyEvent(mapping[0], mapping[1], pressed)
            MOUSE -> lorieView.sendMouseEvent(0F, 0F, mapping[0], pressed, true)
        }
    }

    fun checkControllerButtons(e: KeyEvent): Boolean {
        val pressed = e.action == KeyEvent.ACTION_DOWN
        val physicalController = connectedPhysicalControllers.firstOrNull { it.id == e.deviceId } ?: return false

        when (e.keyCode) {
            KeyEvent.KEYCODE_BUTTON_Y -> {
                when (physicalController.mappingType) {
                    MAPPING_TYPE_XINPUT -> {
                        val index = physicalController.virtualXInputId
                        if (index != -1) {
                            connectedControllers[index].yPressed = pressed
                        }
                    }

                    MAPPING_TYPE_KEYBOARD_MOUSE -> {
                        val mapping = physicalController.keyboardMapping.yButton
                        handleKey(pressed, mapping)
                    }
                }
            }

            KeyEvent.KEYCODE_BUTTON_A -> {
                when (physicalController.mappingType) {
                    MAPPING_TYPE_XINPUT -> {
                        val index = physicalController.virtualXInputId
                        if (index != -1) {
                            connectedControllers[index].aPressed = pressed
                        }
                    }

                    MAPPING_TYPE_KEYBOARD_MOUSE -> {
                        val mapping = physicalController.keyboardMapping.aButton
                        handleKey(pressed, mapping)
                    }
                }
            }

            KeyEvent.KEYCODE_BUTTON_B -> {
                when (physicalController.mappingType) {
                    MAPPING_TYPE_XINPUT -> {
                        val index = physicalController.virtualXInputId
                        if (index != -1) {
                            connectedControllers[index].bPressed = pressed
                        }
                    }

                    MAPPING_TYPE_KEYBOARD_MOUSE -> {
                        val mapping = physicalController.keyboardMapping.bButton
                        handleKey(pressed, mapping)
                    }
                }
            }

            KeyEvent.KEYCODE_BUTTON_X -> {
                when (physicalController.mappingType) {
                    MAPPING_TYPE_XINPUT -> {
                        val index = physicalController.virtualXInputId
                        if (index != -1) {
                            connectedControllers[index].xPressed = pressed
                        }
                    }

                    MAPPING_TYPE_KEYBOARD_MOUSE -> {
                        val mapping = physicalController.keyboardMapping.xButton
                        handleKey(pressed, mapping)
                    }
                }
            }

            KeyEvent.KEYCODE_BUTTON_START -> {
                when (physicalController.mappingType) {
                    MAPPING_TYPE_XINPUT -> {
                        val index = physicalController.virtualXInputId
                        if (index != -1) {
                            connectedControllers[index].startPressed = pressed
                        }
                    }

                    MAPPING_TYPE_KEYBOARD_MOUSE -> {
                        val mapping = physicalController.keyboardMapping.startButton
                        handleKey(pressed, mapping)
                    }
                }
            }

            KeyEvent.KEYCODE_BUTTON_SELECT -> {
                when (physicalController.mappingType) {
                    MAPPING_TYPE_XINPUT -> {
                        val index = physicalController.virtualXInputId
                        if (index != -1) {
                            connectedControllers[index].selectPressed = pressed
                        }
                    }

                    MAPPING_TYPE_KEYBOARD_MOUSE -> {
                        val mapping = physicalController.keyboardMapping.selectButton
                        handleKey(pressed, mapping)
                    }
                }
            }

            KeyEvent.KEYCODE_BUTTON_R1 -> {
                when (physicalController.mappingType) {
                    MAPPING_TYPE_XINPUT -> {
                        val index = physicalController.virtualXInputId
                        if (index != -1) {
                            connectedControllers[index].rbPressed = pressed
                        }
                    }

                    MAPPING_TYPE_KEYBOARD_MOUSE -> {
                        val mapping = physicalController.keyboardMapping.rbButton
                        handleKey(pressed, mapping)
                    }
                }
            }

            KeyEvent.KEYCODE_BUTTON_R2 -> {
                when (physicalController.mappingType) {
                    MAPPING_TYPE_XINPUT -> {
                        val index = physicalController.virtualXInputId
                        if (index != -1) {
                            connectedControllers[index].rt[0] = if (pressed) 2 else 0
                            connectedControllers[index].rt[1] = if (pressed) 5 else 0
                            connectedControllers[index].rt[2] = if (pressed) 5 else 0
                        }
                    }

                    MAPPING_TYPE_KEYBOARD_MOUSE -> {
                        val mapping = physicalController.keyboardMapping.rtButton
                        handleKey(pressed, mapping)
                    }
                }
            }

            KeyEvent.KEYCODE_BUTTON_L1 -> {
                when (physicalController.mappingType) {
                    MAPPING_TYPE_XINPUT -> {
                        val index = physicalController.virtualXInputId
                        if (index != -1) {
                            connectedControllers[index].lbPressed = pressed
                        }
                    }

                    MAPPING_TYPE_KEYBOARD_MOUSE -> {
                        val mapping = physicalController.keyboardMapping.lbButton
                        handleKey(pressed, mapping)
                    }
                }
            }

            KeyEvent.KEYCODE_BUTTON_L2 -> {
                when (physicalController.mappingType) {
                    MAPPING_TYPE_XINPUT -> {
                        val index = physicalController.virtualXInputId
                        if (index != -1) {
                            connectedControllers[index].lt[0] = if (pressed) 2 else 0
                            connectedControllers[index].lt[1] = if (pressed) 5 else 0
                            connectedControllers[index].lt[2] = if (pressed) 5 else 0
                        }
                    }

                    MAPPING_TYPE_KEYBOARD_MOUSE -> {
                        val mapping = physicalController.keyboardMapping.ltButton
                        handleKey(pressed, mapping)
                    }
                }
            }

            KeyEvent.KEYCODE_BUTTON_THUMBR -> {
                when (physicalController.mappingType) {
                    MAPPING_TYPE_XINPUT -> {
                        val index = physicalController.virtualXInputId
                        if (index != -1) {
                            connectedControllers[index].rsPressed = pressed
                        }
                    }

                    MAPPING_TYPE_KEYBOARD_MOUSE -> {
                        val mapping = physicalController.keyboardMapping.rsButton
                        handleKey(pressed, mapping)
                    }
                }
            }

            KeyEvent.KEYCODE_BUTTON_THUMBL -> {
                when (physicalController.mappingType) {
                    MAPPING_TYPE_XINPUT -> {
                        val index = physicalController.virtualXInputId
                        if (index != -1) {
                            connectedControllers[index].lsPressed = pressed
                        }
                    }

                    MAPPING_TYPE_KEYBOARD_MOUSE -> {
                        val mapping = physicalController.keyboardMapping.lsButton
                        handleKey(pressed, mapping)
                    }
                }
            }
        }

        return true
    }

    private fun setVirtualMouseState(axisX: Float, axisY: Float, state: Int) {
        virtualMouseMovingState = state

        axisXVelocity = axisX.absoluteValue
        axisYVelocity = axisY.absoluteValue
    }

    private fun getDPadStatus(axisX: Float, axisY: Float, deadZone: Float): Int {
        val axisXNeutral = axisX < deadZone && axisX > -deadZone
        val axisYNeutral = axisY < deadZone && axisY > -deadZone

        return when {
            axisY < -deadZone && axisXNeutral -> DPAD_UP
            axisX > deadZone && axisY < -deadZone -> DPAD_RIGHT_UP
            axisX > deadZone && axisYNeutral -> DPAD_RIGHT
            axisX > deadZone && axisY > deadZone -> DPAD_RIGHT_DOWN
            axisY > deadZone && axisXNeutral -> DPAD_DOWN
            axisX < -deadZone && axisY > deadZone -> DPAD_LEFT_DOWN
            axisX < -deadZone && axisYNeutral -> DPAD_LEFT
            axisX < -deadZone && axisY < -deadZone -> DPAD_LEFT_UP

            else -> 0
        }
    }

    fun handleAxis(axisX: Float, axisY: Float, analog: Analog, deadZone: Float) {
        val axisXNeutral = axisX < deadZone && axisX > -deadZone
        val axisYNeutral = axisY < deadZone && axisY > -deadZone

        when {
            // Left
            axisX < -deadZone && axisYNeutral -> {
                if (analog.isMouseMapping) {
                    setVirtualMouseState(axisX, axisY, LEFT)
                } else {
                    lorieView.sendKeyEvent(analog.right[0], analog.right[1], false)
                    lorieView.sendKeyEvent(analog.left[0], analog.left[1], true)
                    lorieView.sendKeyEvent(analog.up[0], analog.up[1], false)
                    lorieView.sendKeyEvent(analog.down[0], analog.down[1], false)
                }
            }

            // Right
            axisX > deadZone && axisYNeutral -> {
                if (analog.isMouseMapping) {
                    setVirtualMouseState(axisX, axisY, RIGHT)
                } else {
                    lorieView.sendKeyEvent(analog.right[0], analog.right[1], true)
                    lorieView.sendKeyEvent(analog.left[0], analog.left[1], false)
                    lorieView.sendKeyEvent(analog.up[0], analog.up[1], false)
                    lorieView.sendKeyEvent(analog.down[0], analog.down[1], false)
                }
            }

            // Up
            axisY < -deadZone && axisXNeutral -> {
                if (analog.isMouseMapping) {
                    setVirtualMouseState(axisX, axisY, UP)
                } else {
                    lorieView.sendKeyEvent(analog.right[0], analog.right[1], false)
                    lorieView.sendKeyEvent(analog.left[0], analog.left[1], false)
                    lorieView.sendKeyEvent(analog.up[0], analog.up[1], true)
                    lorieView.sendKeyEvent(analog.down[0], analog.down[1], false)
                }
            }

            // Down
            axisY > deadZone && axisXNeutral -> {
                if (analog.isMouseMapping) {
                    setVirtualMouseState(axisX, axisY, DOWN)
                } else {
                    lorieView.sendKeyEvent(analog.right[0], analog.right[1], false)
                    lorieView.sendKeyEvent(analog.left[0], analog.left[1], false)
                    lorieView.sendKeyEvent(analog.up[0], analog.up[1], false)
                    lorieView.sendKeyEvent(analog.down[0], analog.down[1], true)
                }
            }

            // Left/Up
            axisX < -deadZone && axisY < -deadZone -> {
                if (analog.isMouseMapping) {
                    setVirtualMouseState(axisX, axisY, LEFT_UP)
                } else {
                    lorieView.sendKeyEvent(analog.right[0], analog.right[1], false)
                    lorieView.sendKeyEvent(analog.left[0], analog.left[1], true)
                    lorieView.sendKeyEvent(analog.up[0], analog.up[1], true)
                    lorieView.sendKeyEvent(analog.down[0], analog.down[1], false)
                }
            }

            // Left/Down
            axisX < -deadZone && axisY > deadZone -> {
                if (analog.isMouseMapping) {
                    setVirtualMouseState(axisX, axisY, LEFT_DOWN)
                } else {
                    lorieView.sendKeyEvent(analog.right[0], analog.right[1], false)
                    lorieView.sendKeyEvent(analog.left[0], analog.left[1], true)
                    lorieView.sendKeyEvent(analog.up[0], analog.up[1], false)
                    lorieView.sendKeyEvent(analog.down[0], analog.down[1], true)
                }
            }

            // Right/Up
            axisX > deadZone && axisY < -deadZone -> {
                if (analog.isMouseMapping) {
                    setVirtualMouseState(axisX, axisY, RIGHT_UP)
                } else {
                    lorieView.sendKeyEvent(analog.right[0], analog.right[1], true)
                    lorieView.sendKeyEvent(analog.left[0], analog.left[1], false)
                    lorieView.sendKeyEvent(analog.up[0], analog.up[1], true)
                    lorieView.sendKeyEvent(analog.down[0], analog.down[1], false)
                }
            }

            // Right/Down
            axisX > deadZone && axisY > deadZone -> {
                if (analog.isMouseMapping) {
                    setVirtualMouseState(axisX, axisY, RIGHT_DOWN)
                } else {
                    lorieView.sendKeyEvent(analog.right[0], analog.right[1], true)
                    lorieView.sendKeyEvent(analog.left[0], analog.left[1], false)
                    lorieView.sendKeyEvent(analog.up[0], analog.up[1], false)
                    lorieView.sendKeyEvent(analog.down[0], analog.down[1], true)
                }
            }

            else -> {
                if (analog.isMouseMapping) {
                    virtualMouseMovingState = null
                } else {
                    lorieView.sendKeyEvent(analog.right[0], analog.right[1], false)
                    lorieView.sendKeyEvent(analog.left[0], analog.left[1], false)
                    lorieView.sendKeyEvent(analog.up[0], analog.up[1], false)
                    lorieView.sendKeyEvent(analog.down[0], analog.down[1], false)
                }
            }
        }
    }

    fun checkControllerAxis(event: MotionEvent) {
        lastUsedControllerIndex = connectedPhysicalControllers.indexOfFirst { it.id == event.deviceId }
        if (lastUsedControllerIndex == -1) return

        val physicalController = connectedPhysicalControllers[lastUsedControllerIndex]

        val axisX = event.getAxisValue(MotionEvent.AXIS_X)
        val axisY = event.getAxisValue(MotionEvent.AXIS_Y)

        val axisZ = event.getAxisValue(MotionEvent.AXIS_Z)
        val axisRZ = event.getAxisValue(MotionEvent.AXIS_RZ)

        val axisHatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
        val axisHatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

        when (physicalController.mappingType) {
            MAPPING_TYPE_XINPUT -> {
                val index = physicalController.virtualXInputId
                if (index != -1) {
                    val lxStr = ((axisX + 1) / 2 * 255).toInt().toString().padStart(3, '0')
                    val lyStr = ((-axisY + 1) / 2 * 255).toInt().toString().padStart(3, '0')

                    connectedControllers[index].lx[0] = lxStr[0].digitToInt().toByte()
                    connectedControllers[index].lx[1] = lxStr[1].digitToInt().toByte()
                    connectedControllers[index].lx[2] = lxStr[2].digitToInt().toByte()

                    connectedControllers[index].ly[0] = lyStr[0].digitToInt().toByte()
                    connectedControllers[index].ly[1] = lyStr[1].digitToInt().toByte()
                    connectedControllers[index].ly[2] = lyStr[2].digitToInt().toByte()

                    val rxStr = ((axisZ + 1) / 2 * 255).toInt().toString().padStart(3, '0')
                    val ryStr = ((-axisRZ + 1) / 2 * 255).toInt().toString().padStart(3, '0')

                    connectedControllers[index].rx[0] = rxStr[0].digitToInt().toByte()
                    connectedControllers[index].rx[1] = rxStr[1].digitToInt().toByte()
                    connectedControllers[index].rx[2] = rxStr[2].digitToInt().toByte()

                    connectedControllers[index].ry[0] = ryStr[0].digitToInt().toByte()
                    connectedControllers[index].ry[1] = ryStr[1].digitToInt().toByte()
                    connectedControllers[index].ry[2] = ryStr[2].digitToInt().toByte()

                    connectedControllers[index].dpadStatus = getDPadStatus(axisHatX, axisHatY, 0.25F)
                }
            }

            MAPPING_TYPE_KEYBOARD_MOUSE -> {
                val index = connectedPhysicalControllers.indexOfFirst { it.id == event.deviceId }
                if (index == -1) return

                handleAxis(
                    axisX,
                    axisY,
                    connectedPhysicalControllers[index].keyboardMapping.leftAnalog,
                    connectedPhysicalControllers[index].deadZone,
                )
                handleAxis(
                    axisZ,
                    axisRZ,
                    connectedPhysicalControllers[index].keyboardMapping.rightAnalog,
                    connectedPhysicalControllers[index].deadZone,
                )
                handleAxis(
                    axisHatX,
                    axisHatY,
                    connectedPhysicalControllers[index].keyboardMapping.dPad,
                    connectedPhysicalControllers[index].deadZone,
                )
            }
        }
    }

    val connectedPhysicalControllers: MutableList<PhysicalController> = getGameControllerNames()

    const val MAPPING_TYPE_KEYBOARD_MOUSE = 0
    const val MAPPING_TYPE_XINPUT = 1

    class PhysicalController(
        var name: String,
        var id: Int,
        var mappingType: Int = MAPPING_TYPE_KEYBOARD_MOUSE,
        var virtualXInputId: Int = -1,
        var deadZone: Float = 0.25F,
        var mouseSensibility: Float = 1F,
        var keyboardMapping: KeyboardMapping = KeyboardMapping(),
    )

    class KeyboardMapping(
        var leftAnalog: Analog = Analog(),
        var rightAnalog: Analog = Analog(),
        var dPad: Analog = Analog(),
        var aButton: List<Int> = listOf(),
        var bButton: List<Int> = listOf(),
        var xButton: List<Int> = listOf(),
        var yButton: List<Int> = listOf(),
        var startButton: List<Int> = listOf(),
        var selectButton: List<Int> = listOf(),
        var lbButton: List<Int> = listOf(),
        var rbButton: List<Int> = listOf(),
        var ltButton: List<Int> = listOf(),
        var rtButton: List<Int> = listOf(),
        var lsButton: List<Int> = listOf(),
        var rsButton: List<Int> = listOf(),
    )

    class Analog(
        var isMouseMapping: Boolean = false,
        var up: List<Int> = listOf(),
        var down: List<Int> = listOf(),
        var left: List<Int> = listOf(),
        var right: List<Int> = listOf(),
    )

    class GamePadServer {
        fun startServer() {
            val serverSocket = DatagramSocket(CLIENT_PORT)

            gamePadServerRunning = true

            Thread {
                while (gamePadServerRunning) {
                    try {
                        Timber.tag("GamePad").e("Server initialized on 127.0.0.1:${CLIENT_PORT}")

                        val buffer = ByteArray(BUFFER_SIZE)
                        val packet = DatagramPacket(buffer, buffer.size)

                        while (true) {
                            serverSocket.receive(packet)

                            val receivedData = ByteBuffer.wrap(buffer).get().toInt()
                            when (receivedData) {
                                GET_CONNECTION -> {
                                    val responsePacket = DatagramPacket(buffer, buffer.size, packet.address, packet.port)

                                    serverSocket.send(responsePacket)
                                }

                                GET_GAMEPAD_STATE -> {
                                    connectedControllers.forEachIndexed { index, virtualController ->
                                        buffer[0 + (index * 32)] = GET_GAMEPAD_STATE.toByte()
                                        buffer[1 + (index * 32)] = if (virtualController.connected || index == 0) 1 else 0
                                        buffer[2 + (index * 32)] = if (virtualController.aPressed) 1 else 0
                                        buffer[3 + (index * 32)] = if (virtualController.bPressed) 1 else 0
                                        buffer[4 + (index * 32)] = if (virtualController.xPressed) 1 else 0
                                        buffer[5 + (index * 32)] = if (virtualController.yPressed) 1 else 0
                                        buffer[6 + (index * 32)] = if (virtualController.lbPressed) 1 else 0
                                        buffer[7 + (index * 32)] = if (virtualController.rbPressed) 1 else 0
                                        buffer[8 + (index * 32)] = if (virtualController.selectPressed) 1 else 0
                                        buffer[9 + (index * 32)] = if (virtualController.startPressed) 1 else 0
                                        buffer[10 + (index * 32)] = if (virtualController.lsPressed) 1 else 0
                                        buffer[11 + (index * 32)] = if (virtualController.rsPressed) 1 else 0
                                        buffer[12 + (index * 32)] = 0
                                        buffer[13 + (index * 32)] = virtualController.dpadStatus.toByte()
                                        buffer[14 + (index * 32)] = virtualController.lx[0]
                                        buffer[15 + (index * 32)] = virtualController.lx[1]
                                        buffer[16 + (index * 32)] = virtualController.lx[2]
                                        buffer[17 + (index * 32)] = virtualController.ly[0]
                                        buffer[18 + (index * 32)] = virtualController.ly[1]
                                        buffer[19 + (index * 32)] = virtualController.ly[2]
                                        buffer[20 + (index * 32)] = virtualController.rx[0]
                                        buffer[21 + (index * 32)] = virtualController.rx[1]
                                        buffer[22 + (index * 32)] = virtualController.rx[2]
                                        buffer[23 + (index * 32)] = virtualController.ry[0]
                                        buffer[24 + (index * 32)] = virtualController.ry[1]
                                        buffer[25 + (index * 32)] = virtualController.ry[2]
                                        buffer[26 + (index * 32)] = virtualController.lt[0]
                                        buffer[27 + (index * 32)] = virtualController.lt[1]
                                        buffer[28 + (index * 32)] = virtualController.lt[2]
                                        buffer[29 + (index * 32)] = virtualController.rt[0]
                                        buffer[30 + (index * 32)] = virtualController.rt[1]
                                        buffer[31 + (index * 32)] = virtualController.rt[2]
                                    }

                                    val responsePacket = DatagramPacket(buffer, buffer.size, packet.address, packet.port)

                                    serverSocket.send(responsePacket)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Timber.tag("GamePad").e(e.toString())
                    } finally {
                        serverSocket.close()
                        gamePadServerRunning = false
                    }
                }
            }.start()
        }

        companion object {
            var gamePadServerRunning = false
            val connectedControllers: List<VirtualController> =
                listOf(VirtualController(), VirtualController(), VirtualController(), VirtualController())

            fun connectController(): Int {
                connectedControllers.forEachIndexed { index, it ->
                    if (!it.connected) {
                        it.connected = true

                        return index
                    }
                }

                return -1
            }

            fun disconnectController(index: Int) {
                if (index != -1) connectedControllers[index].connected = false
            }

            const val CLIENT_PORT = 7941
            const val BUFFER_SIZE = 128
            const val GET_CONNECTION = 1
            const val GET_GAMEPAD_STATE = 2
            const val DPAD_UP = 1
            const val DPAD_RIGHT_UP = 2
            const val DPAD_RIGHT = 3
            const val DPAD_RIGHT_DOWN = 4
            const val DPAD_DOWN = 5
            const val DPAD_LEFT_DOWN = 6
            const val DPAD_LEFT = 7
            const val DPAD_LEFT_UP = 8
        }
    }

    class VirtualController(
        var connected: Boolean = false,
        var aPressed: Boolean = false,
        var bPressed: Boolean = false,
        var xPressed: Boolean = false,
        var yPressed: Boolean = false,
        var startPressed: Boolean = false,
        var selectPressed: Boolean = false,
        var rbPressed: Boolean = false,
        var lbPressed: Boolean = false,
        var lsPressed: Boolean = false,
        var rsPressed: Boolean = false,
        var dpadStatus: Int = 0,
        var lx: ByteArray = byteArrayOf(1, 2, 7),
        var ly: ByteArray = byteArrayOf(1, 2, 7),
        var rx: ByteArray = byteArrayOf(1, 2, 7),
        var ry: ByteArray = byteArrayOf(1, 2, 7),
        var lt: ByteArray = byteArrayOf(0, 0, 0),
        var rt: ByteArray = byteArrayOf(0, 0, 0),
    )
}
