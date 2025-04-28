package com.micewine.emu

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.text.InputType
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.annotation.Keep
import com.OxGames.Pluvia.PrefManager
import com.micewine.emu.activities.EmulationActivity
import com.micewine.emu.activities.MainActivity.Companion.selectedResolution
import com.micewine.emu.input.InputStub
import com.micewine.emu.input.TouchInputHandler
import dalvik.annotation.optimization.CriticalNative
import dalvik.annotation.optimization.FastNative
import timber.log.Timber
import java.nio.charset.StandardCharsets

@Keep
@SuppressLint("WrongConstant")
@Suppress("deprecation")
class LorieView : SurfaceView, InputStub {
    interface Callback {
        fun changed(
            sfc: Surface?,
            surfaceWidth: Int,
            surfaceHeight: Int,
            screenWidth: Int,
            screenHeight: Int,
        )
    }

    internal interface PixelFormat {
        companion object {
            const val BGRA_8888: Int = 5 // Stands for HAL_PIXEL_FORMAT_BGRA_8888
        }
    }

    private var clipboard: ClipboardManager? = null
    private var lastClipboardTimestamp = System.currentTimeMillis()
    private var mCallback: Callback? = null
    private val p = Point()
    private val mSurfaceCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            holder.setFormat(PixelFormat.BGRA_8888)
        }

        override fun surfaceChanged(holder: SurfaceHolder, f: Int, width: Int, height: Int) {
            Timber.tag("SurfaceChangedListener").d("Surface was changed: $measuredWidth x $measuredHeight")
            if (mCallback == null) return

            this@LorieView.dimensionsFromSettings
            if (mCallback != null) mCallback!!.changed(holder.surface, measuredWidth, measuredHeight, p.x, p.y)
            this@LorieView.surfaceChanged(holder.surface)
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            if (mCallback != null) mCallback!!.changed(holder.surface, 0, 0, 0, 0)
            this@LorieView.surfaceChanged(holder.surface)
        }
    }

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr,
    ) {
        init()
    }

    @Suppress("unused")
    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int,
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init()
    }

    private fun init() {
        holder.addCallback(mSurfaceCallback)
        clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        nativeInit()
    }

    fun setCallback(callback: Callback?) {
        mCallback = callback
        triggerCallback()
    }

    fun regenerate() {
        val callback = mCallback
        mCallback = null
        holder.setFormat(android.graphics.PixelFormat.RGBA_8888)
        mCallback = callback

        triggerCallback()
    }

    fun triggerCallback() {
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()

        background =
            object : ColorDrawable(Color.TRANSPARENT) {
                override fun isStateful(): Boolean {
                    return true
                }

                override fun hasFocusStateSpecified(): Boolean {
                    return true
                }
            }

        val r = holder.surfaceFrame
        activity.runOnUiThread {
            mSurfaceCallback.surfaceChanged(
                holder,
                PixelFormat.BGRA_8888,
                r.width(),
                r.height(),
            )
        }
    }

    private val activity: Activity
        get() {
            var context = context
            while (context is ContextWrapper) {
                if (context is Activity) {
                    return context
                }
                context = context.baseContext
            }

            throw NullPointerException()
        }

    private val dimensionsFromSettings: Unit
        get() {
            val width = measuredWidth
            val height = measuredHeight
            val resolution = (selectedResolution ?: "1280x720" ).split("x".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val w = resolution[0].toInt()
            val h = resolution[1].toInt()

            if ((width < height && w > h) || (width > height && w < h)) {
                p[h] = w
            } else {
                p[w] = h
            }
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        // val preferences = PreferenceManager.getDefaultSharedPreferences(activity)
        if (PrefManager.getBoolean("displayStretch", false)) {
            holder.setSizeFromLayout()
            return
        }

        dimensionsFromSettings

        if (p.x <= 0 || p.y <= 0) return

        var width = measuredWidth
        var height = measuredHeight

        if ((width < height && p.x > p.y) || (width > height && p.x < p.y))
            p[p.y] = p.x

        if (width > height * p.x / p.y) width = height * p.x / p.y
        else height = width * p.y / p.x

        holder.setFixedSize(p.x, p.y)
        setMeasuredDimension(width, height)

        // In the case if old fixed surface size equals new fixed surface size windowChanged will not be called.
        // We should force it.
        regenerate()
    }

    override fun sendMouseWheelEvent(deltaX: Float, deltaY: Float) {
        sendMouseEvent(deltaX, deltaY, InputStub.BUTTON_SCROLL, false, true)
    }

    override fun dispatchKeyEventPreIme(event: KeyEvent): Boolean {
        if (hardwareKbdScancodesWorkaround) return false
        val a = activity
        return (a is EmulationActivity) && a.handleKey(event)
    }

    private var clipboardListener: ClipboardManager.OnPrimaryClipChangedListener = ClipboardManager.OnPrimaryClipChangedListener { this.handleClipboardChange() }

    fun reloadPreferences() {
        hardwareKbdScancodesWorkaround = false
        clipboardSyncEnabled = true
        setClipboardSyncEnabled(true, true)
        TouchInputHandler.refreshInputDevices()
    }

    // It is used in native code
    fun setClipboardText(text: String?) {
        clipboard!!.setPrimaryClip(ClipData.newPlainText("X11 clipboard", text))

        // Android does not send PrimaryClipChanged event to the window which posted event
        // But in the case we are owning focus and clipboard is unchanged it will be replaced by the same value on X server side.
        // Not cool in the case if user installed some clipboard manager, clipboard content will be doubled.
        lastClipboardTimestamp = System.currentTimeMillis() + 150
    }

    /** @noinspection unused
     */
    // It is used in native code
    fun requestClipboard() {
        if (!clipboardSyncEnabled) {
            sendClipboardEvent("".toByteArray(StandardCharsets.UTF_8))
            return
        }

        val clip = clipboard!!.text
        if (clip != null) {
            val text = clipboard!!.text.toString()
            sendClipboardEvent(text.toByteArray(StandardCharsets.UTF_8))
            Timber.tag("CLIP").d("sending clipboard contents: $text")
        }
    }

    private fun handleClipboardChange() {
        checkForClipboardChange()
    }

    private fun checkForClipboardChange() {
        val desc = clipboard!!.primaryClipDescription
        if (clipboardSyncEnabled && desc != null && lastClipboardTimestamp < desc.timestamp && desc.mimeTypeCount == 1 &&
            (desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) ||
                    desc.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML))
        ) {
            lastClipboardTimestamp = desc.timestamp
            sendClipboardAnnounce()
            Timber.tag("CLIP").d("sending clipboard announce")
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) regenerate()

        requestFocus()

        if (clipboardSyncEnabled && hasFocus) {
            clipboard!!.addPrimaryClipChangedListener(clipboardListener)
            checkForClipboardChange()
        } else clipboard!!.removePrimaryClipChangedListener(clipboardListener)

        TouchInputHandler.refreshInputDevices()
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

        // Note that IME_ACTION_NONE cannot be used as that makes it impossible to input newlines using the on-screen
        // keyboard on Android TV (see https://github.com/termux/termux-app/issues/221).
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_FULLSCREEN

        return super.onCreateInputConnection(outAttrs)
    }

    @FastNative
    private external fun nativeInit()

    @FastNative
    private external fun surfaceChanged(surface: Surface)

    @FastNative
    external fun sendClipboardAnnounce()

    @FastNative
    external fun sendClipboardEvent(text: ByteArray?)

    @FastNative
    external override fun sendMouseEvent(
        x: Float,
        y: Float,
        whichButton: Int,
        buttonDown: Boolean,
        relative: Boolean,
    )

    @FastNative
    external override fun sendTouchEvent(action: Int, id: Int, x: Int, y: Int)

    @FastNative
    external override fun sendStylusEvent(
        x: Float,
        y: Float,
        pressure: Int,
        tiltX: Int,
        tiltY: Int,
        orientation: Int,
        buttons: Int,
        eraser: Boolean,
        mouseMode: Boolean,
    )

    @FastNative
    external override fun sendKeyEvent(scanCode: Int, keyCode: Int, keyDown: Boolean): Boolean

    @FastNative
    external override fun sendTextEvent(utf8Bytes: ByteArray?)

    companion object {
        private var clipboardSyncEnabled = false
        private var hardwareKbdScancodesWorkaround = false

        @JvmStatic
        external fun renderingInActivity(): Boolean

        @JvmStatic @FastNative
        external fun connect(fd: Int)

        @JvmStatic @CriticalNative
        external fun connected(): Boolean

        @JvmStatic @FastNative
        external fun startLogcat(fd: Int)

        @JvmStatic @FastNative
        external fun setClipboardSyncEnabled(enabled: Boolean, ignored: Boolean)

        @JvmStatic @FastNative
        external fun sendWindowChange(width: Int, height: Int, framerate: Int, name: String?)

        @JvmStatic @FastNative
        external fun requestStylusEnabled(enabled: Boolean)

        @JvmStatic @CriticalNative
        external fun requestConnection()

        init {
            System.loadLibrary("Xlorie")
        }
    }
}
