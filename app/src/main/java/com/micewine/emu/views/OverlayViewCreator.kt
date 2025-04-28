package com.micewine.emu.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.VectorDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.micewine.emu.R
import com.micewine.emu.activities.MainActivity.Companion.getNativeResolution
import com.micewine.emu.activities.VirtualControllerOverlayMapper.Companion.ACTION_EDIT_VIRTUAL_BUTTON
import com.micewine.emu.adapters.AdapterPreset.Companion.clickedPresetName
import com.micewine.emu.fragments.EditVirtualButtonFragment.Companion.selectedAnalogDownKeyName
import com.micewine.emu.fragments.EditVirtualButtonFragment.Companion.selectedAnalogLeftKeyName
import com.micewine.emu.fragments.EditVirtualButtonFragment.Companion.selectedAnalogRightKeyName
import com.micewine.emu.fragments.EditVirtualButtonFragment.Companion.selectedAnalogUpKeyName
import com.micewine.emu.fragments.EditVirtualButtonFragment.Companion.selectedButtonKeyName
import com.micewine.emu.fragments.EditVirtualButtonFragment.Companion.selectedButtonRadius
import com.micewine.emu.fragments.EditVirtualButtonFragment.Companion.selectedButtonShape
import com.micewine.emu.fragments.VirtualControllerPresetManagerFragment.Companion.getMapping
import com.micewine.emu.fragments.VirtualControllerPresetManagerFragment.Companion.putMapping
import com.micewine.emu.views.OverlayView.Companion.SHAPE_CIRCLE
import com.micewine.emu.views.OverlayView.Companion.SHAPE_RECTANGLE
import com.micewine.emu.views.OverlayView.Companion.SHAPE_SQUARE
import com.micewine.emu.views.OverlayView.Companion.analogList
import com.micewine.emu.views.OverlayView.Companion.buttonList
import com.micewine.emu.views.OverlayView.Companion.detectClick
import com.micewine.emu.views.OverlayView.Companion.dpadList
import kotlin.math.roundToInt
import androidx.core.graphics.createBitmap

class OverlayViewCreator @JvmOverloads constructor (context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0): View(context, attrs, defStyleAttr) {
    private val editButton: CircleButton = CircleButton(0F, 0F, 150F)
    private val removeButton: CircleButton = CircleButton(0F, 0F, 150F)

    private var editIcon: Bitmap = getBitmapFromVectorDrawable(context, R.drawable.ic_edit, (editButton.radius / 2).toInt(), (editButton.radius / 2).toInt())
    private var removeIcon: Bitmap = getBitmapFromVectorDrawable(context, R.drawable.ic_delete, (removeButton.radius / 2).toInt(), (removeButton.radius / 2).toInt())

    private fun getBitmapFromVectorDrawable(context: Context, drawableId: Int, width: Int, height: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableId) as VectorDrawable
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private val paint = Paint().apply {
        color = Color.BLACK
        alpha = 200
    }

    private val whitePaint = Paint().apply {
        color = Color.WHITE
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

    private val gridPaint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 10F
        isAntiAlias = true
        alpha = 200
    }

    private var selectedButton = 0
    private var selectedVAxis = 0
    private var selectedDPad = 0
    private val dpadUp: Path = Path()
    private val dpadDown: Path = Path()
    private val dpadLeft: Path = Path()
    private val dpadRight: Path = Path()

    private fun loadFromPreferences() {
        val mapping = getMapping(clickedPresetName)

        buttonList.clear()
        analogList.clear()
        dpadList.clear()

        mapping?.buttons?.forEach {
            buttonList.add(it)
        }
        mapping?.analogs?.forEach {
            analogList.add(it)
        }
        mapping?.dpads?.forEach {
            dpadList.add(it)
        }

        reorderButtonsAnalogsIDs()
    }

    fun saveOnPreferences() {
        putMapping(clickedPresetName, getNativeResolution(context), buttonList, analogList, dpadList)
    }

    init {
        loadFromPreferences()
    }

    private fun drawText(text: String, x: Float, y: Float, c: Canvas) {
        textPaint.style = Paint.Style.FILL
        textPaint.color = Color.WHITE

        c.drawText(text, x, y, textPaint)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (i in 0..width) {
            if (i % GRID_SIZE == 0) {
                canvas.drawLine(i.toFloat(), 0F, i.toFloat(), height.toFloat(), gridPaint)
            }
        }

        for (i in 0..height) {
            if (i % GRID_SIZE == 0) {
                canvas.drawLine(0F, i.toFloat(), width.toFloat(), i.toFloat(), gridPaint)
            }
        }

        buttonList.forEach {
            buttonPaint.color = if (lastSelectedButton == it.id && lastSelectedType == BUTTON) Color.GRAY else Color.WHITE
            textPaint.color = buttonPaint.color

            buttonPaint.alpha = 220

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
                        buttonPaint,
                    )
                }
                SHAPE_RECTANGLE -> {
                    canvas.drawRect(
                        it.x - it.radius / 2,
                        it.y - it.radius / 4,
                        it.x + it.radius / 2,
                        it.y + it.radius / 4,
                        buttonPaint,
                    )
                }
            }

            drawText(it.keyName, it.x, it.y + 10, canvas)
        }

        analogList.forEach {
            buttonPaint.color = if (lastSelectedButton == it.id && lastSelectedType == ANALOG) Color.GRAY else Color.WHITE
            whitePaint.color = buttonPaint.color

            whitePaint.alpha = 220
            buttonPaint.alpha = 220

            canvas.apply {
                drawCircle(it.x, it.y, it.radius / 2, buttonPaint)
                drawCircle(it.x, it.y, it.radius / 4, whitePaint)
            }
        }

        if (lastSelectedButton > 0) {
            editButton.x = width - 20F - editButton.radius / 2
            editButton.y = 20F + editButton.radius / 2

            removeButton.x = editButton.x - removeButton.radius
            removeButton.y = 20F + removeButton.radius / 2

            canvas.apply {
                drawCircle(editButton.x, editButton.y, editButton.radius / 2, paint)
                drawCircle(removeButton.x, removeButton.y, removeButton.radius / 2, paint)
                drawBitmap(editIcon, editButton.x - editButton.radius / 4, editButton.y - editButton.radius / 4, whitePaint)
                drawBitmap(removeIcon, removeButton.x - removeButton.radius / 4, removeButton.y - removeButton.radius / 4, whitePaint)
            }
        }

        dpadList.forEach {
            buttonPaint.color = if (lastSelectedButton == it.id && lastSelectedType == DPAD) Color.GRAY else Color.WHITE
            whitePaint.color = buttonPaint.color

            whitePaint.alpha = 220
            buttonPaint.alpha = 220

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

                drawPath(dpadLeft, buttonPaint)
                drawPath(dpadRight, buttonPaint)
                drawPath(dpadUp, buttonPaint)
                drawPath(dpadDown, buttonPaint)

                drawText(it.upKeyName, it.x, it.y - it.radius / 2, canvas)
                drawText(it.downKeyName, it.x, it.y + it.radius / 2 + 20, canvas)
                drawText(it.leftKeyName, it.x - it.radius / 2 - 20, it.y + 10, canvas)
                drawText(it.rightKeyName, it.x + it.radius / 2 + 20, it.y + 10, canvas)
            }
        }
    }

    fun addButton(buttonData: OverlayView.VirtualButton) {
        buttonList.add(buttonData)
        invalidate()
    }

    fun addAnalog(buttonData: OverlayView.VirtualAnalog) {
        analogList.add(buttonData)
        invalidate()
    }

    fun addDPad(buttonData: OverlayView.VirtualDPad) {
        dpadList.add(buttonData)
        invalidate()
    }

    private fun reorderButtonsAnalogsIDs() {
        buttonList.forEachIndexed { i, button ->
            button.id = i + 1
        }
        analogList.forEachIndexed { i, analog ->
            analog.id = i + 1
        }
        dpadList.forEachIndexed { i, virtualDPad ->
            virtualDPad.id = i + 1
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
         when (event.actionMasked) {
             MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                 if (!detectClick(event, event.actionIndex, editButton.x, editButton.y, editButton.radius, SHAPE_CIRCLE) && !detectClick(event, event.actionIndex, removeButton.x, removeButton.y, removeButton.radius, SHAPE_CIRCLE)) {
                     lastSelectedButton = 0
                 }

                 buttonList.forEach {
                     if (detectClick(event, event.actionIndex, it.x, it.y, it.radius, it.shape)) {
                         if (selectedButton == 0) {
                             selectedButton = it.id
                             lastSelectedType = BUTTON
                             lastSelectedButton = it.id
                         }
                     }
                 }
                 analogList.forEach {
                     if (detectClick(event, event.actionIndex, it.x, it.y, it.radius, SHAPE_SQUARE)) {
                         if (selectedVAxis == 0) {
                             selectedVAxis = it.id
                             lastSelectedType = ANALOG
                             lastSelectedButton = it.id
                         }
                     }
                 }
                 dpadList.forEach {
                     if (detectClick(event, event.actionIndex, it.x, it.y, it.radius, SHAPE_SQUARE)) {
                         if (selectedDPad == 0) {
                             selectedDPad = it.id
                             lastSelectedType = DPAD
                             lastSelectedButton = it.id
                         }
                     }
                 }

                 invalidate()
             }

             MotionEvent.ACTION_MOVE -> {
                 buttonList.forEach {
                     if (detectClick(event, event.actionIndex, it.x, it.y, it.radius, it.shape)) {
                         if (selectedButton > 0) {
                             buttonList[
                                 buttonList.indexOfFirst { i ->
                                     i.id == selectedButton
                                 },
                             ].apply {
                                 x = (event.getX(event.actionIndex) / GRID_SIZE).roundToInt() * GRID_SIZE.toFloat()
                                 y = (event.getY(event.actionIndex) / GRID_SIZE).roundToInt() * GRID_SIZE.toFloat()
                             }
                         }
                     }
                 }

                 analogList.forEach {
                     if (detectClick(event, event.actionIndex, it.x, it.y, it.radius, SHAPE_CIRCLE)) {
                         if (selectedVAxis > 0) {
                             analogList[
                                 analogList.indexOfFirst { i ->
                                     i.id == selectedVAxis
                                 },
                             ].apply {
                                 x = (event.getX(event.actionIndex) / GRID_SIZE).roundToInt() * GRID_SIZE.toFloat()
                                 y = (event.getY(event.actionIndex) / GRID_SIZE).roundToInt() * GRID_SIZE.toFloat()
                             }
                         }
                     }
                 }

                 dpadList.forEach {
                     if (detectClick(event, event.actionIndex, it.x, it.y, it.radius, SHAPE_SQUARE)) {
                         if (selectedDPad > 0) {
                             dpadList[
                                 dpadList.indexOfFirst { i ->
                                     i.id == selectedDPad
                                 },
                             ].apply {
                                 x = (event.getX(event.actionIndex) / GRID_SIZE).roundToInt() * GRID_SIZE.toFloat()
                                 y = (event.getY(event.actionIndex) / GRID_SIZE).roundToInt() * GRID_SIZE.toFloat()
                             }
                         }
                     }
                 }

                 invalidate()
             }

             MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_UP -> {
                 buttonList.forEach {
                     if (detectClick(event, event.actionIndex, it.x, it.y, it.radius, it.shape)) {
                         if (selectedButton == it.id) {
                             selectedButton = 0
                         }
                     }
                 }

                 analogList.forEach {
                     if (detectClick(event, event.actionIndex, it.x, it.y, it.radius, SHAPE_CIRCLE)) {
                         if (selectedVAxis == it.id) {
                             selectedVAxis = 0
                         }
                     }
                 }

                 dpadList.forEach {
                     if (detectClick(event, event.actionIndex, it.x, it.y, it.radius, SHAPE_SQUARE)) {
                         if (selectedDPad == it.id) {
                             selectedDPad = 0
                         }
                     }
                 }

                 if (detectClick(event, event.actionIndex, editButton.x, editButton.y, editButton.radius, SHAPE_CIRCLE) && lastSelectedButton > 0) {
                     if (buttonList.isNotEmpty() && lastSelectedType == BUTTON) {
                         selectedButtonKeyName = buttonList[lastSelectedButton - 1].keyName
                         selectedButtonRadius = buttonList[lastSelectedButton - 1].radius.toInt()
                         selectedButtonShape = buttonList[lastSelectedButton - 1].shape
                     }

                     if (analogList.isNotEmpty() && lastSelectedType == ANALOG) {
                         selectedAnalogUpKeyName = analogList[lastSelectedButton - 1].upKeyName
                         selectedAnalogDownKeyName = analogList[lastSelectedButton - 1].downKeyName
                         selectedAnalogLeftKeyName = analogList[lastSelectedButton - 1].leftKeyName
                         selectedAnalogRightKeyName = analogList[lastSelectedButton - 1].rightKeyName
                         selectedButtonRadius = analogList[lastSelectedButton - 1].radius.toInt()
                     }

                     if (dpadList.isNotEmpty() && lastSelectedType == DPAD) {
                         selectedAnalogUpKeyName = dpadList[lastSelectedButton - 1].upKeyName
                         selectedAnalogDownKeyName = dpadList[lastSelectedButton - 1].downKeyName
                         selectedAnalogLeftKeyName = dpadList[lastSelectedButton - 1].leftKeyName
                         selectedAnalogRightKeyName = dpadList[lastSelectedButton - 1].rightKeyName
                         selectedButtonRadius = dpadList[lastSelectedButton - 1].radius.toInt()
                     }

                     context.sendBroadcast(
                         Intent(ACTION_EDIT_VIRTUAL_BUTTON),
                     )
                 }

                 if (detectClick(event, event.actionIndex, removeButton.x, removeButton.y, removeButton.radius, SHAPE_CIRCLE) && lastSelectedButton > 0) {
                     if (buttonList.isNotEmpty() && lastSelectedType == BUTTON) {
                         buttonList.removeAt(lastSelectedButton - 1)
                     }

                     if (analogList.isNotEmpty() && lastSelectedType == ANALOG) {
                         analogList.removeAt(lastSelectedButton - 1)
                     }

                     if (dpadList.isNotEmpty() && lastSelectedType == DPAD) {
                         dpadList.removeAt(lastSelectedButton - 1)
                     }

                     lastSelectedButton = 0

                     reorderButtonsAnalogsIDs()
                     invalidate()
                 }
             }
         }

        return true
    }

    class CircleButton(
        var x: Float,
        var y: Float,
        var radius: Float,
    )

    companion object {
        const val BUTTON = 0
        const val ANALOG = 1
        const val DPAD = 2
        const val GRID_SIZE = 35

        var lastSelectedButton = 0
        var lastSelectedType = BUTTON
    }
}
