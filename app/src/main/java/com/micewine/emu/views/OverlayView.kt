package com.micewine.emu.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.OxGames.Pluvia.PrefManager
import com.micewine.emu.LorieView
import com.micewine.emu.activities.PresetManagerActivity.Companion.SELECTED_VIRTUAL_CONTROLLER_PRESET_KEY
import com.micewine.emu.controller.ControllerUtils
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_DOWN
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_LEFT
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_LEFT_DOWN
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_LEFT_UP
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_RIGHT
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_RIGHT_DOWN
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_RIGHT_UP
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_UP
import com.micewine.emu.controller.ControllerUtils.KEYBOARD
import com.micewine.emu.controller.ControllerUtils.MOUSE
import com.micewine.emu.controller.ControllerUtils.handleAxis
import com.micewine.emu.controller.XKeyCodes.getXKeyScanCodes
import com.micewine.emu.fragments.VirtualControllerPresetManagerFragment.Companion.getMapping
import com.micewine.emu.input.InputStub.BUTTON_LEFT
import com.micewine.emu.input.InputStub.BUTTON_MIDDLE
import com.micewine.emu.input.InputStub.BUTTON_RIGHT
import com.micewine.emu.input.InputStub.BUTTON_UNDEFINED
import kotlin.math.sqrt

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paint: Paint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val buttonPaint: Paint = Paint().apply {
        strokeWidth = 8F
        color = Color.WHITE
        style = Paint.Style.STROKE
    }
    private val textPaint: Paint = Paint().apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 40F
    }

    private var lorieView: LorieView = LorieView(context)
    // private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val dpadUp: Path = Path()
    private val dpadDown: Path = Path()
    private val dpadLeft: Path = Path()
    private val dpadRight: Path = Path()

    fun loadPreset(name: String?) {
        var mapping = getMapping(name ?: PrefManager.getString(SELECTED_VIRTUAL_CONTROLLER_PRESET_KEY, "default")!!)

        if (name == "--") mapping = getMapping(PrefManager.getString(SELECTED_VIRTUAL_CONTROLLER_PRESET_KEY, "default")!!)

        buttonList.clear()
        analogList.clear()
        dpadList.clear()

        mapping?.buttons?.forEach {
            when (it.keyName) {
                "M_Left" -> {
                    it.keyCodes = listOf(BUTTON_LEFT, BUTTON_LEFT, MOUSE)
                }

                "M_Middle" -> {
                    it.keyCodes = listOf(BUTTON_MIDDLE, BUTTON_MIDDLE, MOUSE)
                }

                "M_Right" -> {
                    it.keyCodes = listOf(BUTTON_RIGHT, BUTTON_RIGHT, MOUSE)
                }

                "Mouse" -> {
                    it.keyCodes = listOf(MOUSE, MOUSE, MOUSE)
                }

                else -> {
                    it.keyCodes = getXKeyScanCodes(it.keyName)
                }
            }

            buttonList.add(it)
        }
        mapping?.analogs?.forEach {
            it.upKeyCodes = getXKeyScanCodes(it.upKeyName)
            it.downKeyCodes = getXKeyScanCodes(it.downKeyName)
            it.leftKeyCodes = getXKeyScanCodes(it.leftKeyName)
            it.rightKeyCodes = getXKeyScanCodes(it.rightKeyName)

            analogList.add(it)
        }
        mapping?.dpads?.forEach {
            it.upKeyCodes = getXKeyScanCodes(it.upKeyName)
            it.downKeyCodes = getXKeyScanCodes(it.downKeyName)
            it.leftKeyCodes = getXKeyScanCodes(it.leftKeyName)
            it.rightKeyCodes = getXKeyScanCodes(it.rightKeyName)

            dpadList.add(it)
        }
    }

    private fun drawDPad(path: Path, pressed: Boolean, canvas: Canvas) {
        buttonPaint.color = if (pressed) Color.GRAY else Color.WHITE
        buttonPaint.alpha = 150
        textPaint.color = buttonPaint.color

        canvas.drawPath(path, buttonPaint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        buttonList.forEach {
            buttonPaint.color = if (it.isPressed) Color.GRAY else Color.WHITE
            buttonPaint.alpha = 150

            textPaint.color = if (it.isPressed) Color.GRAY else Color.WHITE

            paint.textSize = it.radius / 4

            when (it.shape) {
                SHAPE_CIRCLE -> {
                    canvas.drawCircle(it.x, it.y, it.radius / 2, buttonPaint)
                }
                SHAPE_SQUARE -> {
                    canvas.drawRect(
                        it.x - it.radius / 2,
                        it.y - it.radius / 2,
                        it.x + it.radius / 2,
                        it.y + it.radius / 2,
                        buttonPaint
                    )
                }
                SHAPE_RECTANGLE -> {
                    canvas.drawRect(
                        it.x - it.radius / 2,
                        it.y - it.radius / 4,
                        it.x + it.radius / 2,
                        it.y + it.radius / 4,
                        buttonPaint
                    )
                }
            }

            canvas.drawText(it.keyName, it.x, it.y + 10, textPaint)
        }
        analogList.forEach {
            paint.color = if (it.isPressed) Color.GRAY else Color.WHITE
            buttonPaint.color = if (it.isPressed) Color.GRAY else Color.WHITE

            paint.alpha = 150
            buttonPaint.alpha = 150

            canvas.drawCircle(it.x, it.y, it.radius / 2, buttonPaint)

            var analogX = it.x + it.fingerX
            var analogY = it.y + it.fingerY

            val distanceSquared = (it.fingerX * it.fingerX) + (it.fingerY * it.fingerY)
            val maxRadiusSquared = (it.radius / 4) * (it.radius / 4)

            if (distanceSquared > maxRadiusSquared) {
                val scale = (it.radius / 4) / sqrt(distanceSquared)
                analogX = it.x + (it.fingerX * scale)
                analogY = it.y + (it.fingerY * scale)
            }

            canvas.drawCircle(analogX, analogY, it.radius / 4, paint)
        }
        dpadList.forEach {
            canvas.apply {
                dpadLeft.apply {
                    reset()
                    moveTo(it.x - 20, it.y)
                    lineTo(it.x - 20 - it.radius / 4, it.y - it.radius / 4)
                    lineTo(it.x - 20 - it.radius / 4 - it.radius / 2, it.y - it.radius / 4)
                    lineTo(it.x - 20 - it.radius / 4 - it.radius / 2, it.y - it.radius / 4 + it.radius / 2)
                    lineTo(it.x - 20 - it.radius / 4, it.y - it.radius / 4 + it.radius / 2)
                    lineTo(it.x - 20, it.y)
                    close()
                }

                dpadUp.apply {
                    reset()
                    moveTo(it.x, it.y - 20)
                    lineTo(it.x - it.radius / 4, it.y - 20 - it.radius / 4)
                    lineTo(it.x - it.radius / 4, it.y - 20 - it.radius / 4 - it.radius / 2)
                    lineTo(it.x - it.radius / 4 + it.radius / 2, it.y - 20 - it.radius / 4 - it.radius / 2)
                    lineTo(it.x - it.radius / 4 + it.radius / 2, it.y - 20 - it.radius / 4)
                    lineTo(it.x, it.y - 20)
                    close()
                }

                dpadRight.apply {
                    reset()
                    moveTo(it.x + 20, it.y)
                    lineTo(it.x + 20 + it.radius / 4, it.y - it.radius / 4)
                    lineTo(it.x + 20 + it.radius / 4 + it.radius / 2, it.y - it.radius / 4)
                    lineTo(it.x + 20 + it.radius / 4 + it.radius / 2, it.y - it.radius / 4 + it.radius / 2)
                    lineTo(it.x + 20 + it.radius / 4, it.y - it.radius / 4 + it.radius / 2)
                    lineTo(it.x + 20, it.y)
                    close()
                }

                dpadDown.apply {
                    reset()
                    moveTo(it.x, it.y + 20)
                    lineTo(it.x - it.radius / 4, it.y + 20 + it.radius / 4)
                    lineTo(it.x - it.radius / 4, it.y + 20 + it.radius / 4 + it.radius / 2)
                    lineTo(it.x - it.radius / 4 + it.radius / 2, it.y + 20 + it.radius / 4 + it.radius / 2)
                    lineTo(it.x - it.radius / 4 + it.radius / 2, it.y + 20 + it.radius / 4)
                    lineTo(it.x, it.y + 20)
                    close()
                }

                drawDPad(dpadUp, it.dpadStatus in listOf(DPAD_UP, DPAD_RIGHT_UP, DPAD_LEFT_UP), canvas)
                drawText(it.upKeyName, it.x, it.y - it.radius / 2, textPaint)

                drawDPad(dpadDown, it.dpadStatus in listOf(DPAD_DOWN, DPAD_RIGHT_DOWN, DPAD_LEFT_DOWN), canvas)
                drawText(it.downKeyName, it.x, it.y + it.radius / 2 + 20, textPaint)

                drawDPad(dpadLeft, it.dpadStatus in listOf(DPAD_LEFT, DPAD_LEFT_DOWN, DPAD_LEFT_UP), canvas)
                drawText(it.leftKeyName, it.x - it.radius / 2 - 20, it.y + 10, textPaint)

                drawDPad(dpadRight, it.dpadStatus in listOf(DPAD_RIGHT, DPAD_RIGHT_DOWN, DPAD_RIGHT_UP), canvas)
                drawText(it.rightKeyName, it.x + it.radius / 2 + 20, it.y + 10, textPaint)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_DOWN -> {
                buttonList.forEach {
                    if (detectClick(event, event.actionIndex, it.x, it.y, it.radius, it.shape)) {
                        it.isPressed = true
                        it.fingerId = event.actionIndex

                        handleButton(it, true)

                        return@forEach
                    }
                }
                analogList.forEach {
                    if (detectClick(event, event.actionIndex, it.x, it.y, it.radius, SHAPE_CIRCLE)) {
                        val posX = event.getX(event.actionIndex) - it.x
                        val posY = event.getY(event.actionIndex) - it.y

                        it.fingerX = posX
                        it.fingerY = posY
                        it.isPressed = true
                        it.fingerId = event.actionIndex

                        it.fingerX = posX
                        it.fingerY = posY

                        virtualAxis(posX / (it.radius / 4), posY / (it.radius / 4),
                            it.upKeyCodes, it.downKeyCodes, it.leftKeyCodes, it.rightKeyCodes, it.deadZone)

                        return@forEach
                    }
                }
                dpadList.forEach {
                    if (detectClick(event, event.actionIndex, it.x, it.y, it.radius, SHAPE_DPAD)) {
                        val posX = event.getX(event.actionIndex) - it.x
                        val posY = event.getY(event.actionIndex) - it.y

                        it.fingerX = posX
                        it.fingerY = posY
                        it.isPressed = true
                        it.fingerId = event.actionIndex

                        it.fingerX = posX
                        it.fingerY = posY

                        when {
                            (posX / it.radius > 0.25) && !(posY / it.radius < -0.25 || posY / it.radius > 0.25) -> it.dpadStatus = DPAD_RIGHT
                            (posX / it.radius < -0.25) && !(posY / it.radius < -0.25 || posY / it.radius > 0.25) -> it.dpadStatus = DPAD_LEFT
                            (posY / it.radius > 0.25) && !(posX / it.radius < -0.25 || posX / it.radius > 0.25) -> it.dpadStatus = DPAD_DOWN
                            (posY / it.radius < -0.25) && !(posX / it.radius < -0.25 || posX / it.radius > 0.25) -> it.dpadStatus = DPAD_UP
                            (posX / it.radius > 0.25) && (posY / it.radius > 0.25) -> it.dpadStatus = DPAD_RIGHT_DOWN
                            (posX / it.radius > 0.25) && (posY / it.radius < -0.25) -> it.dpadStatus = DPAD_RIGHT_UP
                            (posX / it.radius < -0.25) && (posY / it.radius > 0.25) -> it.dpadStatus = DPAD_LEFT_DOWN
                            (posX / it.radius < -0.25) && (posY / it.radius < -0.25) -> it.dpadStatus = DPAD_LEFT_UP

                            else -> it.dpadStatus = -1
                        }

                        virtualAxis(posX / it.radius, posY / it.radius, it.upKeyCodes, it.downKeyCodes, it.leftKeyCodes, it.rightKeyCodes, 0.25F)

                        return@forEach
                    }
                }

                invalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    var isFingerPressingButton = false

                    buttonList.forEach {
                        if (it.fingerId == i) {
                            it.isPressed = true
                            handleButton(it, true)

                            isFingerPressingButton = true
                        }
                    }
                    analogList.forEach {
                        if (it.isPressed && it.fingerId == i) {
                            val posX = event.getX(i) - it.x
                            val posY = event.getY(i) - it.y

                            it.fingerX = posX
                            it.fingerY = posY

                            virtualAxis(posX / (it.radius / 4), posY / (it.radius / 4), it.upKeyCodes, it.downKeyCodes, it.leftKeyCodes, it.rightKeyCodes, it.deadZone)

                            isFingerPressingButton = true
                        }
                    }
                    dpadList.forEach {
                        if (it.isPressed && it.fingerId == i) {
                            val posX = event.getX(i) - it.x
                            val posY = event.getY(i) - it.y

                            it.fingerX = posX
                            it.fingerY = posY

                            isFingerPressingButton = true

                            when {
                                (posX / it.radius > 0.25) && !(posY / it.radius < -0.25 || posY / it.radius > 0.25) -> it.dpadStatus = DPAD_RIGHT
                                (posX / it.radius < -0.25) && !(posY / it.radius < -0.25 || posY / it.radius > 0.25) -> it.dpadStatus = DPAD_LEFT
                                (posY / it.radius > 0.25) && !(posX / it.radius < -0.25 || posX / it.radius > 0.25) -> it.dpadStatus = DPAD_DOWN
                                (posY / it.radius < -0.25) && !(posX / it.radius < -0.25 || posX / it.radius > 0.25) -> it.dpadStatus = DPAD_UP
                                (posX / it.radius > 0.25) && (posY / it.radius > 0.25) -> it.dpadStatus = DPAD_RIGHT_DOWN
                                (posX / it.radius > 0.25) && (posY / it.radius < -0.25) -> it.dpadStatus = DPAD_RIGHT_UP
                                (posX / it.radius < -0.25) && (posY / it.radius > 0.25) -> it.dpadStatus = DPAD_LEFT_DOWN
                                (posX / it.radius < -0.25) && (posY / it.radius < -0.25) -> it.dpadStatus = DPAD_LEFT_UP

                                else -> it.dpadStatus = -1
                            }

                            virtualAxis(posX / it.radius, posY / it.radius, it.upKeyCodes, it.downKeyCodes, it.leftKeyCodes, it.rightKeyCodes, 0.25F)
                        }
                    }

                    if (!isFingerPressingButton) {
                        if (event.historySize > 0) {
                            val deltaX = event.getX(i) - event.getHistoricalX(i, 0)
                            val deltaY = event.getY(i) - event.getHistoricalY(i, 0)

                            if ((deltaX > 0.08 || deltaX < -0.08) && (deltaY > 0.08 || deltaY < -0.08)) {
                                lorieView.sendMouseEvent(deltaX, deltaY, BUTTON_UNDEFINED, false, true)
                            }
                        }
                    }
                }

                invalidate()
            }

            MotionEvent.ACTION_POINTER_UP -> {
                buttonList.forEach {
                    if (it.fingerId == event.actionIndex) {
                        it.fingerId = -1
                    }
                    if (detectClick(event, event.actionIndex, it.x, it.y, it.radius, it.shape)) {
                        handleButton(it, false)
                    }
                }
                analogList.forEach {
                    if (it.fingerId == event.actionIndex) {
                        it.fingerId = -1
                        it.fingerX = 0F
                        it.fingerY = 0F

                        it.isPressed = false

                        virtualAxis(0F, 0F, it.upKeyCodes, it.downKeyCodes, it.leftKeyCodes, it.rightKeyCodes, it.deadZone)
                    }
                }
                dpadList.forEach {
                    if (it.fingerId == event.actionIndex) {
                        it.fingerId = -1
                        it.fingerX = 0F
                        it.fingerY = 0F

                        it.isPressed = false
                        it.dpadStatus = -1

                        virtualAxis(0F, 0F, it.upKeyCodes, it.downKeyCodes, it.leftKeyCodes, it.rightKeyCodes, 0.25F)
                    }
                }

                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                buttonList.forEach {
                    it.fingerId = -1
                    handleButton(it, false)
                }
                analogList.forEach {
                    it.fingerX = 0F
                    it.fingerY = 0F
                    it.isPressed = false

                    virtualAxis(0F, 0F, it.upKeyCodes, it.downKeyCodes, it.leftKeyCodes, it.rightKeyCodes, it.deadZone)
                }
                dpadList.forEach {
                    it.fingerX = 0F
                    it.fingerY = 0F
                    it.isPressed = false
                    it.dpadStatus = -1

                    virtualAxis(0F, 0F, it.upKeyCodes, it.downKeyCodes, it.leftKeyCodes, it.rightKeyCodes, 0.25F)
                }

                invalidate()
            }
        }

        return true
    }

    private fun virtualAxis(axisX: Float, axisY: Float, upKeyCodes: List<Int>, downKeyCodes: List<Int>, leftKeyCodes: List<Int>, rightKeyCodes: List<Int>, deadZone: Float) {
        handleAxis(axisX, axisY, ControllerUtils.Analog(false, upKeyCodes, downKeyCodes, leftKeyCodes, rightKeyCodes), deadZone)
    }

    private fun handleButton(button: VirtualButton, pressed: Boolean) {
        button.isPressed = pressed

        when (button.keyCodes!![2]) {
            KEYBOARD -> lorieView.sendKeyEvent(button.keyCodes!![0], button.keyCodes!![1], pressed)
            MOUSE -> lorieView.sendMouseEvent(0F, 0F, button.keyCodes!![0], pressed, true)
        }
    }

    class VirtualButton(
        var id: Int,
        var x: Float,
        var y: Float,
        var radius: Float,
        var keyName: String,
        var keyCodes: List<Int>?,
        var fingerId: Int,
        var isPressed: Boolean,
        var shape: Int
    )

    class VirtualDPad(
        var id: Int,
        var x: Float,
        var y: Float,
        var radius: Float,
        var upKeyName: String,
        var upKeyCodes: List<Int>,
        var downKeyName: String,
        var downKeyCodes: List<Int>,
        var leftKeyName: String,
        var leftKeyCodes: List<Int>,
        var rightKeyName: String,
        var rightKeyCodes: List<Int>,
        var fingerId: Int,
        var isPressed: Boolean,
        var fingerX: Float,
        var fingerY: Float,
        var dpadStatus: Int
    )

    class VirtualAnalog(
        var id: Int,
        var x: Float,
        var y: Float,
        var fingerX: Float,
        var fingerY: Float,
        var radius: Float,
        var upKeyName: String,
        var upKeyCodes: List<Int>,
        var downKeyName: String,
        var downKeyCodes: List<Int>,
        var leftKeyName: String,
        var leftKeyCodes: List<Int>,
        var rightKeyName: String,
        var rightKeyCodes: List<Int>,
        var isPressed: Boolean,
        var fingerId: Int,
        var deadZone: Float
    )

    companion object {
        const val SHAPE_CIRCLE = 0
        const val SHAPE_SQUARE = 1
        const val SHAPE_RECTANGLE = 2
        const val SHAPE_DPAD = 3

        val buttonList = mutableListOf<VirtualButton>()
        val analogList = mutableListOf<VirtualAnalog>()
        val dpadList = mutableListOf<VirtualDPad>()

        fun detectClick(event: MotionEvent, index: Int, x: Float, y: Float, radius: Float, shape: Int): Boolean {
            return when (shape) {
                SHAPE_RECTANGLE -> {
                    (event.getX(index) >= x - radius / 2 && event.getX(index) <= (x + (radius / 2))) &&
                            (event.getY(index) >= y - radius / 4 && event.getY(index) <= (y + (radius / 4)))
                }

                SHAPE_DPAD -> {
                    (event.getX(index) >= x - radius - 20 && event.getX(index) <= (x + (radius - 20))) &&
                            (event.getY(index) >= y - radius - 20 && event.getY(index) <= (y + (radius - 20)))
                }

                else -> (event.getX(index) >= x - radius / 2 && event.getX(index) <= (x + (radius / 2))) &&
                        (event.getY(index) >= y - radius / 2 && event.getY(index) <= (y + (radius / 2)))
            }
        }
    }
}
