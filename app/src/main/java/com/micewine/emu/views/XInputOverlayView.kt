package com.micewine.emu.views

import android.annotation.SuppressLint
import android.content.Context
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
import com.micewine.emu.LorieView
import com.micewine.emu.activities.MainActivity.Companion.getNativeResolution
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_DOWN
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_LEFT
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_LEFT_DOWN
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_LEFT_UP
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_RIGHT
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_RIGHT_DOWN
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_RIGHT_UP
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.DPAD_UP
import com.micewine.emu.controller.ControllerUtils.GamePadServer.Companion.connectedControllers
import com.micewine.emu.input.InputStub.BUTTON_UNDEFINED
import com.micewine.emu.views.OverlayView.Companion.SHAPE_CIRCLE
import com.micewine.emu.views.OverlayView.Companion.SHAPE_DPAD
import com.micewine.emu.views.OverlayView.Companion.SHAPE_RECTANGLE
import com.micewine.emu.views.OverlayView.Companion.detectClick
import kotlin.math.sqrt
import androidx.core.graphics.createBitmap

class XInputOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
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
    private val dpadUp: Path = Path()
    private val dpadDown: Path = Path()
    private val dpadLeft: Path = Path()
    private val dpadRight: Path = Path()
    private val dpadList: MutableList<VirtualXInputDPad> = mutableListOf()
    private val buttonList: MutableList<VirtualXInputButton> = mutableListOf()
    private val analogList: MutableList<VirtualXInputAnalog> = mutableListOf()

    private fun getBitmapFromVectorDrawable(
        context: Context,
        drawableId: Int,
        width: Int,
        height: Int,
    ): Bitmap {
        val drawable = ContextCompat.getDrawable(context, drawableId) as VectorDrawable
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun textAsBitmap(
        text: String,
        textSize: Float,
        textColor: Int,
        width: Int,
        height: Int,
        context: Context,
    ): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.textSize = textSize
            this.color = textColor
            this.textAlign = Paint.Align.CENTER
            typeface = context.resources.getFont(R.font.quicksand)
        }

        val image = createBitmap(width, height)
        val canvas = Canvas(image)

        val xPos = width / 2f
        val yPos = (height / 2f) - ((paint.descent() + paint.ascent()) / 2)

        canvas.drawText(text, xPos, yPos, paint)
        return image
    }

    private fun adjustButtons() {
        val nativeResolution = getNativeResolution(context)
        val baseResolution = "2400x1080" // My Device Resolution

        if (baseResolution != nativeResolution) {
            val nativeSplit = nativeResolution.split("x").map { it.toFloat() }
            val processedSplit = baseResolution.split("x").map { it.toFloat() }

            val multiplierX = nativeSplit[0] / processedSplit[0] * 100F
            val multiplierY = nativeSplit[1] / processedSplit[1] * 100F

            buttonList.forEach {
                it.x = (it.x / 100F * multiplierX)
                it.y = (it.y / 100F * multiplierY)
            }
            analogList.forEach {
                it.x = (it.x / 100F * multiplierX)
                it.y = (it.y / 100F * multiplierY)
            }
            dpadList.forEach {
                it.x = (it.x / 100F * multiplierX)
                it.y = (it.y / 100F * multiplierY)
            }
        }
    }

    init {
        buttonList.add(
            VirtualXInputButton(
                A_BUTTON,
                2065F,
                910F,
                180F,
                0,
                false,
                SHAPE_CIRCLE,
                textAsBitmap("A", 150F, Color.WHITE, 180, 180, context),
            ),
        )
        buttonList.add(
            VirtualXInputButton(
                B_BUTTON,
                2205F,
                735F,
                180F,
                0,
                false,
                SHAPE_CIRCLE,
                textAsBitmap("B", 150F, Color.WHITE, 180, 180, context),
            ),
        )
        buttonList.add(
            VirtualXInputButton(
                X_BUTTON,
                1925F,
                735F,
                180F,
                0,
                false,
                SHAPE_CIRCLE,
                textAsBitmap("X", 150F, Color.WHITE, 180, 180, context),
            ),
        )
        buttonList.add(
            VirtualXInputButton(
                Y_BUTTON,
                2065F,
                560F,
                180F,
                0,
                false,
                SHAPE_CIRCLE,
                textAsBitmap("Y", 150F, Color.WHITE, 180, 180, context),
            ),
        )
        buttonList.add(
            VirtualXInputButton(
                START_BUTTON,
                1330F,
                980F,
                100F,
                0,
                false,
                SHAPE_CIRCLE,
                getBitmapFromVectorDrawable(context, R.drawable.start_button, 100, 100),
            ),
        )
        buttonList.add(
            VirtualXInputButton(
                SELECT_BUTTON,
                1120F,
                980F,
                100F,
                0,
                false,
                SHAPE_CIRCLE,
                getBitmapFromVectorDrawable(context, R.drawable.select_button, 100, 100),
            ),
        )
        buttonList.add(
            VirtualXInputButton(
                LB_BUTTON,
                280F,
                280F,
                180F,
                0,
                false,
                SHAPE_RECTANGLE,
                textAsBitmap("LB", 80F, Color.WHITE, 180, 180, context),
            ),
        )
        buttonList.add(
            VirtualXInputButton(
                LT_BUTTON,
                280F,
                140F,
                180F,
                0,
                false,
                SHAPE_RECTANGLE,
                textAsBitmap("LT", 80F, Color.WHITE, 180, 180, context),
            ),
        )
        buttonList.add(
            VirtualXInputButton(
                RB_BUTTON,
                2065F,
                280F,
                180F,
                0,
                false,
                SHAPE_RECTANGLE,
                textAsBitmap("RB", 80F, Color.WHITE, 180, 180, context),
            ),
        )
        buttonList.add(
            VirtualXInputButton(
                RT_BUTTON,
                2065F,
                140F,
                180F,
                0,
                false,
                SHAPE_RECTANGLE,
                textAsBitmap("RT", 80F, Color.WHITE, 180, 180, context),
            ),
        )
        buttonList.add(
            VirtualXInputButton(
                LS_BUTTON,
                940F,
                980F,
                140F,
                0,
                false,
                SHAPE_CIRCLE,
                textAsBitmap("LS", 80F, Color.WHITE, 140, 140, context),
            ),
        )
        buttonList.add(
            VirtualXInputButton(
                RS_BUTTON,
                1500F,
                980F,
                140F,
                0,
                false,
                SHAPE_CIRCLE,
                textAsBitmap("RS", 80F, Color.WHITE, 140, 140, context),
            ),
        )
        analogList.add(
            VirtualXInputAnalog(
                LEFT_ANALOG,
                280F,
                840F,
                275F,
                false,
                0,
                0F,
                0F,
            ),
        )
        analogList.add(
            VirtualXInputAnalog(
                RIGHT_ANALOG,
                1750F,
                210F,
                275F,
                false,
                0,
                0F,
                0F,
            ),
        )
        dpadList.add(
            VirtualXInputDPad(
                dpadList.count() + 1,
                640F,
                260F,
                250F,
                0,
                false,
                0F,
                0F,
                0,
            ),
        )

        adjustButtons()
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

            canvas.drawBitmap(it.bitmap, it.x - it.radius / 2, it.y - it.radius / 2, buttonPaint)
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
                    lineTo(
                        it.x - 20 - it.radius / 4 - it.radius / 2,
                        it.y - it.radius / 4 + it.radius / 2,
                    )
                    lineTo(it.x - 20 - it.radius / 4, it.y - it.radius / 4 + it.radius / 2)
                    lineTo(it.x - 20, it.y)
                    close()
                }

                dpadUp.apply {
                    reset()
                    moveTo(it.x, it.y - 20)
                    lineTo(it.x - it.radius / 4, it.y - 20 - it.radius / 4)
                    lineTo(it.x - it.radius / 4, it.y - 20 - it.radius / 4 - it.radius / 2)
                    lineTo(
                        it.x - it.radius / 4 + it.radius / 2,
                        it.y - 20 - it.radius / 4 - it.radius / 2,
                    )
                    lineTo(it.x - it.radius / 4 + it.radius / 2, it.y - 20 - it.radius / 4)
                    lineTo(it.x, it.y - 20)
                    close()
                }

                dpadRight.apply {
                    reset()
                    moveTo(it.x + 20, it.y)
                    lineTo(it.x + 20 + it.radius / 4, it.y - it.radius / 4)
                    lineTo(it.x + 20 + it.radius / 4 + it.radius / 2, it.y - it.radius / 4)
                    lineTo(
                        it.x + 20 + it.radius / 4 + it.radius / 2,
                        it.y - it.radius / 4 + it.radius / 2,
                    )
                    lineTo(it.x + 20 + it.radius / 4, it.y - it.radius / 4 + it.radius / 2)
                    lineTo(it.x + 20, it.y)
                    close()
                }

                dpadDown.apply {
                    reset()
                    moveTo(it.x, it.y + 20)
                    lineTo(it.x - it.radius / 4, it.y + 20 + it.radius / 4)
                    lineTo(it.x - it.radius / 4, it.y + 20 + it.radius / 4 + it.radius / 2)
                    lineTo(
                        it.x - it.radius / 4 + it.radius / 2,
                        it.y + 20 + it.radius / 4 + it.radius / 2,
                    )
                    lineTo(it.x - it.radius / 4 + it.radius / 2, it.y + 20 + it.radius / 4)
                    lineTo(it.x, it.y + 20)
                    close()
                }

                drawDPad(
                    dpadUp,
                    it.dpadStatus in listOf(DPAD_UP, DPAD_RIGHT_UP, DPAD_LEFT_UP),
                    canvas,
                )

                drawDPad(
                    dpadDown,
                    it.dpadStatus in listOf(DPAD_DOWN, DPAD_RIGHT_DOWN, DPAD_LEFT_DOWN),
                    canvas,
                )

                drawDPad(
                    dpadLeft,
                    it.dpadStatus in listOf(DPAD_LEFT, DPAD_LEFT_DOWN, DPAD_LEFT_UP),
                    canvas,
                )

                drawDPad(
                    dpadRight,
                    it.dpadStatus in listOf(DPAD_RIGHT, DPAD_RIGHT_DOWN, DPAD_RIGHT_UP),
                    canvas,
                )
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (virtualXInputControllerId != -1) {
            when (event.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_DOWN -> {
                    buttonList.forEach {
                        if (detectClick(
                                event,
                                event.actionIndex,
                                it.x,
                                it.y,
                                it.radius,
                                it.shape,
                            )
                        ) {
                            it.isPressed = true
                            it.fingerId = event.actionIndex

                            handleButton(it, true)

                            return@forEach
                        }
                    }
                    analogList.forEach {
                        if (detectClick(
                                event,
                                event.actionIndex,
                                it.x,
                                it.y,
                                it.radius,
                                SHAPE_CIRCLE,
                            )
                        ) {
                            val posX = event.getX(event.actionIndex) - it.x
                            val posY = event.getY(event.actionIndex) - it.y

                            it.fingerX = posX
                            it.fingerY = posY
                            it.isPressed = true
                            it.fingerId = event.actionIndex

                            it.fingerX = posX
                            it.fingerY = posY

                            when (it.id) {
                                LEFT_ANALOG -> {
                                    virtualAxis(
                                        posX / (it.radius / 4),
                                        posY / (it.radius / 4),
                                        connectedControllers[virtualXInputControllerId].lx,
                                        connectedControllers[virtualXInputControllerId].ly,
                                    )
                                }

                                RIGHT_ANALOG -> {
                                    virtualAxis(
                                        posX / (it.radius / 4),
                                        posY / (it.radius / 4),
                                        connectedControllers[virtualXInputControllerId].rx,
                                        connectedControllers[virtualXInputControllerId].ry,
                                    )
                                }
                            }

                            return@forEach
                        }
                    }
                    dpadList.forEach {
                        if (detectClick(
                                event,
                                event.actionIndex,
                                it.x,
                                it.y,
                                it.radius,
                                SHAPE_DPAD,
                            )
                        ) {
                            val posX = event.getX(event.actionIndex) - it.x
                            val posY = event.getY(event.actionIndex) - it.y

                            it.fingerX = posX
                            it.fingerY = posY
                            it.isPressed = true
                            it.fingerId = event.actionIndex

                            it.fingerX = posX
                            it.fingerY = posY

                            when {
                                (posX / it.radius > 0.25) && !(posY / it.radius < -0.25 || posY / it.radius > 0.25) -> it.dpadStatus =
                                    DPAD_RIGHT

                                (posX / it.radius < -0.25) && !(posY / it.radius < -0.25 || posY / it.radius > 0.25) -> it.dpadStatus =
                                    DPAD_LEFT

                                (posY / it.radius > 0.25) && !(posX / it.radius < -0.25 || posX / it.radius > 0.25) -> it.dpadStatus =
                                    DPAD_DOWN

                                (posY / it.radius < -0.25) && !(posX / it.radius < -0.25 || posX / it.radius > 0.25) -> it.dpadStatus =
                                    DPAD_UP

                                (posX / it.radius > 0.25) && (posY / it.radius > 0.25) -> it.dpadStatus =
                                    DPAD_RIGHT_DOWN

                                (posX / it.radius > 0.25) && (posY / it.radius < -0.25) -> it.dpadStatus =
                                    DPAD_RIGHT_UP

                                (posX / it.radius < -0.25) && (posY / it.radius > 0.25) -> it.dpadStatus =
                                    DPAD_LEFT_DOWN

                                (posX / it.radius < -0.25) && (posY / it.radius < -0.25) -> it.dpadStatus =
                                    DPAD_LEFT_UP

                                else -> it.dpadStatus = 0
                            }

                            connectedControllers[virtualXInputControllerId].dpadStatus = it.dpadStatus

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

                                when (it.id) {
                                    LEFT_ANALOG -> {
                                        virtualAxis(
                                            posX / (it.radius / 4),
                                            posY / (it.radius / 4),
                                            connectedControllers[virtualXInputControllerId].lx,
                                            connectedControllers[virtualXInputControllerId].ly,
                                        )
                                    }

                                    RIGHT_ANALOG -> {
                                        virtualAxis(
                                            posX / (it.radius / 4),
                                            posY / (it.radius / 4),
                                            connectedControllers[virtualXInputControllerId].rx,
                                            connectedControllers[virtualXInputControllerId].ry,
                                        )
                                    }
                                }

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
                                    (posX / it.radius > 0.25) && !(posY / it.radius < -0.25 || posY / it.radius > 0.25) -> it.dpadStatus =
                                        DPAD_RIGHT

                                    (posX / it.radius < -0.25) && !(posY / it.radius < -0.25 || posY / it.radius > 0.25) -> it.dpadStatus =
                                        DPAD_LEFT

                                    (posY / it.radius > 0.25) && !(posX / it.radius < -0.25 || posX / it.radius > 0.25) -> it.dpadStatus =
                                        DPAD_DOWN

                                    (posY / it.radius < -0.25) && !(posX / it.radius < -0.25 || posX / it.radius > 0.25) -> it.dpadStatus =
                                        DPAD_UP

                                    (posX / it.radius > 0.25) && (posY / it.radius > 0.25) -> it.dpadStatus =
                                        DPAD_RIGHT_DOWN

                                    (posX / it.radius > 0.25) && (posY / it.radius < -0.25) -> it.dpadStatus =
                                        DPAD_RIGHT_UP

                                    (posX / it.radius < -0.25) && (posY / it.radius > 0.25) -> it.dpadStatus =
                                        DPAD_LEFT_DOWN

                                    (posX / it.radius < -0.25) && (posY / it.radius < -0.25) -> it.dpadStatus =
                                        DPAD_LEFT_UP

                                    else -> it.dpadStatus = 0
                                }

                                connectedControllers[virtualXInputControllerId].dpadStatus = it.dpadStatus
                            }
                        }

                        if (!isFingerPressingButton) {
                            if (event.historySize > 0) {
                                val deltaX = event.getX(i) - event.getHistoricalX(i, 0)
                                val deltaY = event.getY(i) - event.getHistoricalY(i, 0)

                                if ((deltaX > 0.08 || deltaX < -0.08) && (deltaY > 0.08 || deltaY < -0.08)) {
                                    lorieView.sendMouseEvent(
                                        deltaX,
                                        deltaY,
                                        BUTTON_UNDEFINED,
                                        false,
                                        true,
                                    )
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
                        if (detectClick(
                                event,
                                event.actionIndex,
                                it.x,
                                it.y,
                                it.radius,
                                it.shape,
                            )
                        ) {
                            handleButton(it, false)
                        }
                    }
                    analogList.forEach {
                        if (it.fingerId == event.actionIndex) {
                            it.fingerId = -1
                            it.fingerX = 0F
                            it.fingerY = 0F

                            it.isPressed = false

                            when (it.id) {
                                LEFT_ANALOG -> {
                                    virtualAxis(
                                        0F,
                                        0F,
                                        connectedControllers[virtualXInputControllerId].lx,
                                        connectedControllers[virtualXInputControllerId].ly,
                                    )
                                }

                                RIGHT_ANALOG -> {
                                    virtualAxis(
                                        0F,
                                        0F,
                                        connectedControllers[virtualXInputControllerId].rx,
                                        connectedControllers[virtualXInputControllerId].ry,
                                    )
                                }
                            }
                        }
                    }
                    dpadList.forEach {
                        if (it.fingerId == event.actionIndex) {
                            it.fingerId = -1
                            it.fingerX = 0F
                            it.fingerY = 0F

                            it.isPressed = false
                            it.dpadStatus = 0

                            connectedControllers[virtualXInputControllerId].dpadStatus = 0
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

                        when (it.id) {
                            LEFT_ANALOG -> {
                                virtualAxis(
                                    0F,
                                    0F,
                                    connectedControllers[virtualXInputControllerId].lx,
                                    connectedControllers[virtualXInputControllerId].ly,
                                )
                            }

                            RIGHT_ANALOG -> {
                                virtualAxis(
                                    0F,
                                    0F,
                                    connectedControllers[virtualXInputControllerId].rx,
                                    connectedControllers[virtualXInputControllerId].ry,
                                )
                            }
                        }
                    }
                    dpadList.forEach {
                        it.fingerX = 0F
                        it.fingerY = 0F
                        it.isPressed = false
                        it.dpadStatus = 0

                        connectedControllers[virtualXInputControllerId].dpadStatus = 0
                    }

                    invalidate()
                }
            }
        }

        return true
    }

    private fun virtualAxis(
        axisX: Float,
        axisY: Float,
        x: ByteArray,
        y: ByteArray,
    ) {
        val lxStr = ((axisX.coerceIn(-1F, 1F) + 1) / 2 * 255).toInt().toString().padStart(3, '0')
        val lyStr = ((-axisY.coerceIn(-1F, 1F) + 1) / 2 * 255).toInt().toString().padStart(3, '0')

        x[0] = lxStr[0].digitToInt().toByte()
        x[1] = lxStr[1].digitToInt().toByte()
        x[2] = lxStr[2].digitToInt().toByte()

        y[0] = lyStr[0].digitToInt().toByte()
        y[1] = lyStr[1].digitToInt().toByte()
        y[2] = lyStr[2].digitToInt().toByte()
    }

    private fun handleButton(button: VirtualXInputButton, pressed: Boolean) {
        button.isPressed = pressed

        when (button.id) {
            A_BUTTON -> {
                connectedControllers[virtualXInputControllerId].aPressed = pressed
            }
            B_BUTTON -> {
                connectedControllers[virtualXInputControllerId].bPressed = pressed
            }
            X_BUTTON -> {
                connectedControllers[virtualXInputControllerId].xPressed = pressed
            }
            Y_BUTTON -> {
                connectedControllers[virtualXInputControllerId].yPressed = pressed
            }
            START_BUTTON -> {
                connectedControllers[virtualXInputControllerId].startPressed = pressed
            }
            SELECT_BUTTON -> {
                connectedControllers[virtualXInputControllerId].selectPressed = pressed
            }
            LB_BUTTON -> {
                connectedControllers[virtualXInputControllerId].lbPressed = pressed
            }
            LT_BUTTON -> {
                connectedControllers[virtualXInputControllerId].lt[0] = if (pressed) 2 else 0
                connectedControllers[virtualXInputControllerId].lt[1] = if (pressed) 5 else 0
                connectedControllers[virtualXInputControllerId].lt[2] = if (pressed) 5 else 0
            }
            RB_BUTTON -> {
                connectedControllers[virtualXInputControllerId].rbPressed = pressed
            }
            RT_BUTTON -> {
                connectedControllers[virtualXInputControllerId].rt[0] = if (pressed) 2 else 0
                connectedControllers[virtualXInputControllerId].rt[1] = if (pressed) 5 else 0
                connectedControllers[virtualXInputControllerId].rt[2] = if (pressed) 5 else 0
            }
            LS_BUTTON -> {
                connectedControllers[virtualXInputControllerId].lsPressed = pressed
            }
            RS_BUTTON -> {
                connectedControllers[virtualXInputControllerId].rsPressed = pressed
            }
        }
    }

    class VirtualXInputButton(
        var id: Int,
        var x: Float,
        var y: Float,
        var radius: Float,
        var fingerId: Int,
        var isPressed: Boolean,
        var shape: Int,
        var bitmap: Bitmap,
    )

    class VirtualXInputDPad(
        var id: Int,
        var x: Float,
        var y: Float,
        var radius: Float,
        var fingerId: Int,
        var isPressed: Boolean,
        var fingerX: Float,
        var fingerY: Float,
        var dpadStatus: Int,
    )

    class VirtualXInputAnalog(
        var id: Int,
        var x: Float,
        var y: Float,
        var radius: Float,
        var isPressed: Boolean,
        var fingerId: Int,
        var fingerX: Float,
        var fingerY: Float,
    )

    companion object {
        const val A_BUTTON = 1
        const val B_BUTTON = 2
        const val X_BUTTON = 3
        const val Y_BUTTON = 4
        const val START_BUTTON = 5
        const val SELECT_BUTTON = 6
        const val LB_BUTTON = 7
        const val LT_BUTTON = 8
        const val RB_BUTTON = 9
        const val RT_BUTTON = 10
        const val LEFT_ANALOG = 11
        const val RIGHT_ANALOG = 12
        const val LS_BUTTON = 13
        const val RS_BUTTON = 14

        var virtualXInputControllerId = -1
    }
}
