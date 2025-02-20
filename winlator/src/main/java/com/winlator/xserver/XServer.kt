package com.winlator.xserver

import android.util.SparseArray
import androidx.core.util.size
import com.winlator.core.CursorLocker
import com.winlator.renderer.GLRenderer
import com.winlator.winhandler.WinHandler
import com.winlator.xserver.DesktopHelper.attachTo
import com.winlator.xserver.extensions.BigReqExtension
import com.winlator.xserver.extensions.DRI3Extension
import com.winlator.xserver.extensions.Extension
import com.winlator.xserver.extensions.MITSHMExtension
import com.winlator.xserver.extensions.PresentExtension
import com.winlator.xserver.extensions.SyncExtension
import java.nio.charset.Charset
import java.util.EnumMap
import java.util.concurrent.locks.ReentrantLock

class XServer(val screenInfo: ScreenInfo) {

    companion object {
        const val VERSION: Short = 11
        const val VENDOR_NAME: String = "Elbrus Technologies, LLC"
        val LATIN1_CHARSET: Charset = Charset.forName("latin1")
    }

    enum class Lockable {
        WINDOW_MANAGER,
        PIXMAP_MANAGER,
        DRAWABLE_MANAGER,
        GRAPHIC_CONTEXT_MANAGER,
        INPUT_DEVICE,
        CURSOR_MANAGER,
        SHMSEGMENT_MANAGER,
    }

    val extensions: SparseArray<Extension> = SparseArray<Extension>()
    val pixmapManager: PixmapManager
    val resourceIDs: ResourceIDs = ResourceIDs(128)
    val graphicsContextManager: GraphicsContextManager = GraphicsContextManager()
    val selectionManager: SelectionManager
    val drawableManager: DrawableManager
    val windowManager: WindowManager
    val cursorManager: CursorManager
    val keyboard: Keyboard = Keyboard.createKeyboard(this)
    val pointer: Pointer = Pointer(this)
    val inputDeviceManager: InputDeviceManager
    val grabManager: GrabManager
    val cursorLocker: CursorLocker = CursorLocker(this)
    var shmSegmentManager: SHMSegmentManager? = null
    var renderer: GLRenderer? = null
    var winHandler: WinHandler? = null

    private val locks = EnumMap<Lockable?, ReentrantLock?>(Lockable::class.java)

    var isRelativeMouseMovement: Boolean = false
        set(relativeMouseMovement) {
            cursorLocker.setEnabled(!relativeMouseMovement)
            field = relativeMouseMovement
        }

    init {
        Lockable.entries.forEach { lockable ->
            locks.put(lockable, ReentrantLock())
        }

        pixmapManager = PixmapManager()
        drawableManager = DrawableManager(this)
        cursorManager = CursorManager(drawableManager)
        windowManager = WindowManager(screenInfo, drawableManager)
        selectionManager = SelectionManager(windowManager)
        inputDeviceManager = InputDeviceManager(this)
        grabManager = GrabManager(this)

        attachTo(this)
        setupExtensions()
    }

    private inner class SingleXLock(lockable: Lockable?) : XLock {
        private val lock: ReentrantLock? = locks[lockable]

        init {
            lock!!.lock()
        }

        override fun close() {
            lock!!.unlock()
        }
    }

    private inner class MultiXLock(private val lockables: Array<out Lockable>) : XLock {
        init {
            lockables.forEach { lockable ->
                locks.get(lockable)!!.lock()
            }
        }

        override fun close() {
            lockables.indices.reversed().forEach {
                locks.get(lockables[it])!!.unlock()
            }
        }
    }

    fun lock(lockable: Lockable?): XLock = SingleXLock(lockable)

    fun lock(vararg lockables: Lockable): XLock = MultiXLock(lockables)

    fun lockAll(): XLock = MultiXLock(Lockable.entries.toTypedArray())

    fun getExtensionByName(name: String?): Extension? {
        for (i in 0..<extensions.size) {
            val extension = extensions.valueAt(i)
            if (extension.name == name) {
                return extension
            }
        }

        return null
    }

    fun injectPointerMove(x: Int, y: Int) {
        lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE).use {
            pointer.setPosition(x, y)
        }
    }

    fun injectPointerMoveDelta(dx: Int, dy: Int) {
        lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE).use {
            pointer.setPosition(pointer.x + dx, pointer.y + dy)
        }
    }

    fun injectPointerButtonPress(buttonCode: Pointer.Button) {
        lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE).use {
            pointer.setButton(buttonCode, true)
        }
    }

    fun injectPointerButtonRelease(buttonCode: Pointer.Button) {
        lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE).use {
            pointer.setButton(buttonCode, false)
        }
    }

    @JvmOverloads
    fun injectKeyPress(xKeycode: XKeycode, keysym: Int = 0) {
        lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE).use {
            keyboard.setKeyPress(xKeycode.id, keysym)
        }
    }

    fun injectKeyRelease(xKeycode: XKeycode) {
        lock(Lockable.WINDOW_MANAGER, Lockable.INPUT_DEVICE).use {
            keyboard.setKeyRelease(xKeycode.id)
        }
    }

    private fun setupExtensions() {
        extensions.put(BigReqExtension.MAJOR_OPCODE.toInt(), BigReqExtension())
        extensions.put(MITSHMExtension.MAJOR_OPCODE.toInt(), MITSHMExtension())
        extensions.put(DRI3Extension.MAJOR_OPCODE.toInt(), DRI3Extension())
        extensions.put(PresentExtension.MAJOR_OPCODE.toInt(), PresentExtension())
        extensions.put(SyncExtension.MAJOR_OPCODE.toInt(), SyncExtension())
    }

    fun <T : Extension> getExtension(opcode: Int): T? = extensions.get(opcode) as T?
}
