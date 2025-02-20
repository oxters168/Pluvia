package com.winlator.xserver

import android.util.SparseArray
import androidx.core.util.size
import com.winlator.xconnector.XInputStream
import com.winlator.xserver.IDGenerator.generate
import com.winlator.xserver.Window.StackMode
import com.winlator.xserver.WindowAttributes.WindowClass
import com.winlator.xserver.errors.BadIdChoice
import com.winlator.xserver.errors.BadMatch
import com.winlator.xserver.errors.XRequestError
import com.winlator.xserver.events.ConfigureNotify
import com.winlator.xserver.events.ConfigureRequest
import com.winlator.xserver.events.DestroyNotify
import com.winlator.xserver.events.Event
import com.winlator.xserver.events.Expose
import com.winlator.xserver.events.MapNotify
import com.winlator.xserver.events.MapRequest
import com.winlator.xserver.events.ResizeRequest
import com.winlator.xserver.events.UnmapNotify

class WindowManager(
    screenInfo: ScreenInfo,
    val drawableManager: DrawableManager,
) : XResourceManager() {

    enum class FocusRevertTo {
        NONE,
        POINTER_ROOT,
        PARENT,
    }

    val rootWindow: Window

    private val windows = SparseArray<Window?>()

    var focusedWindow: Window? = null
        private set

    var focusRevertTo: FocusRevertTo = FocusRevertTo.NONE
        private set

    private val onWindowModificationListeners = ArrayList<OnWindowModificationListener?>()

    interface OnWindowModificationListener {
        fun onMapWindow(window: Window) {}

        fun onUnmapWindow(window: Window) {}

        fun onChangeWindowZOrder(window: Window) {}

        fun onUpdateWindowContent(window: Window) {}

        fun onUpdateWindowGeometry(window: Window?, resized: Boolean) {}

        fun onUpdateWindowAttributes(window: Window, mask: Bitmask) {}

        fun onModifyWindowProperty(window: Window, property: Property) {}
    }

    init {
        val id = generate()
        val drawable = drawableManager.createDrawable(
            id = id,
            width = screenInfo.width,
            height = screenInfo.height,
            visual = drawableManager.visual,
        )

        rootWindow = Window(id, drawable, 0, 0, screenInfo.width.toInt(), screenInfo.height.toInt(), null)
        rootWindow.attributes.isMapped = true

        windows.put(id, rootWindow)
    }

    fun getWindow(id: Int): Window? = windows.get(id)

    fun findWindowWithProcessId(processId: Int): Window? {
        for (i in 0..<windows.size) {
            val window = windows.valueAt(i)
            if (window != null && window.processId == processId) {
                return window
            }
        }

        return null
    }

    fun destroyWindow(id: Int) {
        val window = getWindow(id)
        if (window != null && rootWindow.id != id) {
            unmapWindow(window)
            removeAllSubwindowsAndWindow(window)
        }
    }

    private fun removeAllSubwindowsAndWindow(window: Window) {
        val children = window.getChildren()

        children.forEach { child ->
            child?.let { removeAllSubwindowsAndWindow(it) }
        }

        val parent = window.parent

        window.sendEvent(Event.STRUCTURE_NOTIFY, DestroyNotify(window, window))
        parent!!.sendEvent(Event.SUBSTRUCTURE_NOTIFY, DestroyNotify(parent, window))

        windows.remove(window.id)

        if (window.isInputOutput) {
            drawableManager.removeDrawable(window.content!!.id)
        }

        triggerOnFreeResourceListener(window)

        if (window == focusedWindow) {
            revertFocus()
        }

        parent.removeChild(window)
    }

    fun mapWindow(window: Window) {
        if (!window.attributes.isMapped) {
            val parent = window.parent
            if (!parent!!.hasEventListenerFor(Event.SUBSTRUCTURE_REDIRECT) || window.attributes.isOverrideRedirect) {
                window.attributes.isMapped = true
                window.sendEvent(Event.STRUCTURE_NOTIFY, MapNotify(window, window))
                parent.sendEvent(Event.SUBSTRUCTURE_NOTIFY, MapNotify(parent, window))
                window.sendEvent(Event.EXPOSURE, Expose(window))

                triggerOnMapWindow(window)
            } else {
                parent.sendEvent(Event.SUBSTRUCTURE_REDIRECT, MapRequest(parent, window))
            }
        }
    }

    fun unmapWindow(window: Window) {
        if (rootWindow.id != window.id && window.attributes.isMapped) {
            window.attributes.isMapped = false

            val parent = window.parent

            window.sendEvent(Event.STRUCTURE_NOTIFY, UnmapNotify(window, window))
            parent!!.sendEvent(Event.SUBSTRUCTURE_NOTIFY, UnmapNotify(parent, window))

            if (window == focusedWindow) {
                revertFocus()
            }

            triggerOnUnmapWindow(window)
        }
    }

    fun revertFocus() {
        when (focusRevertTo) {
            FocusRevertTo.NONE -> focusedWindow = null
            FocusRevertTo.POINTER_ROOT -> focusedWindow = rootWindow
            FocusRevertTo.PARENT -> if (focusedWindow!!.parent != null) {
                focusedWindow = focusedWindow!!.parent
            }
        }
    }

    fun setFocus(focusedWindow: Window?, focusRevertTo: FocusRevertTo) {
        this.focusedWindow = focusedWindow
        this.focusRevertTo = focusRevertTo
    }

    @Throws(XRequestError::class)
    fun createWindow(
        id: Int,
        parent: Window,
        x: Short,
        y: Short,
        width: Short,
        height: Short,
        windowClass: WindowClass,
        visual: Visual?,
        depth: Byte,
        client: XClient?,
    ): Window {
        var visual = visual
        var depth = depth

        if (windows.indexOfKey(id) >= 0) {
            throw BadIdChoice(id)
        }

        var isInputOutput = false
        when (windowClass) {
            WindowClass.COPY_FROM_PARENT -> {
                depth = if (depth.toInt() != 0 || !parent.isInputOutput) {
                    depth
                } else {
                    parent.content!!.visual!!.depth
                }
                isInputOutput = parent.isInputOutput
            }

            WindowClass.INPUT_OUTPUT -> if (parent.isInputOutput) {
                depth = if (depth.toInt() == 0) parent.content!!.visual!!.depth else depth
                isInputOutput = true
            } else {
                throw BadMatch()
            }

            WindowClass.INPUT_ONLY -> isInputOutput = false
        }

        if (isInputOutput) {
            visual = visual ?: parent.content!!.visual

            if (depth != visual!!.depth) {
                throw BadMatch()
            }
        }

        var drawable: Drawable? = null
        if (isInputOutput) {
            drawable = drawableManager.createDrawable(id, width, height, visual)
            if (drawable == null) {
                throw BadIdChoice(id)
            }
        }

        val window = Window(id, drawable, x.toInt(), y.toInt(), width.toInt(), height.toInt(), client)
        window.attributes.windowClass = windowClass

        if (drawable != null) drawable.onDrawListener = Runnable { triggerOnUpdateWindowContent(window) }

        windows.put(id, window)
        parent.addChild(window)

        triggerOnCreateResourceListener(window)

        return window
    }

    private fun changeWindowGeometry(window: Window, x: Short, y: Short, width: Short, height: Short) {
        var width = width
        var height = height
        var resized = window.width != width || window.height != height

        if (resized && window.hasEventListenerFor(Event.RESIZE_REDIRECT)) {
            window.sendEvent(Event.SUBSTRUCTURE_REDIRECT, ResizeRequest(window, width, height))
            width = window.width
            height = window.height
            resized = false
        }

        if (resized && window.isInputOutput) {
            val oldContent = window.content
            drawableManager.removeDrawable(oldContent!!.id)
            val newContent = drawableManager.createDrawable(oldContent.id, width, height, oldContent.visual)
            newContent!!.onDrawListener = Runnable { triggerOnUpdateWindowContent(window) }
            window.content = newContent
        }

        if (resized || window.x != x || window.y != y) {
            window.x = x
            window.y = y
            window.width = width
            window.height = height
            triggerOnUpdateWindowGeometry(window, resized)
        }

        if (resized && window.isInputOutput && window.attributes.isMapped) {
            window.sendEvent(Expose(window))
        }
    }

    private fun changeWindowZOrder(stackMode: StackMode, window: Window, sibling: Window?) {
        val parent = window.parent

        when (stackMode) {
            StackMode.ABOVE -> parent!!.moveChildAbove(window, sibling)
            StackMode.BELOW -> parent!!.moveChildBelow(window, sibling)
            else -> Unit
        }

        triggerOnChangeWindowZOrder(window)
    }

    fun configureWindow(window: Window, valueMask: Bitmask, inputStream: XInputStream) {
        var x = window.x
        var y = window.y
        var width = window.width
        var height = window.height
        var borderWidth = window.borderWidth
        var sibling: Window? = null
        var stackMode: StackMode? = null

        for (index in valueMask) {
            when (index) {
                Window.FLAG_X -> x = inputStream.readInt().toShort()
                Window.FLAG_Y -> y = inputStream.readInt().toShort()
                Window.FLAG_WIDTH -> width = inputStream.readInt().toShort()
                Window.FLAG_HEIGHT -> height = inputStream.readInt().toShort()
                Window.FLAG_BORDER_WIDTH -> borderWidth = inputStream.readInt().toShort()
                Window.FLAG_SIBLING -> sibling = getWindow(inputStream.readInt())
                Window.FLAG_STACK_MODE -> stackMode = StackMode.entries.toTypedArray()[inputStream.readInt()]
            }
        }

        val parent = window.parent
        val overrideRedirect = window.attributes.isOverrideRedirect
        if (!parent!!.hasEventListenerFor(Event.SUBSTRUCTURE_REDIRECT) || overrideRedirect) {
            changeWindowGeometry(window, x, y, width, height)

            window.borderWidth = borderWidth
            if (stackMode != null) {
                changeWindowZOrder(stackMode, window, sibling)
            }

            val previousSibling = window.previousSibling()
            window.sendEvent(
                Event.STRUCTURE_NOTIFY,
                ConfigureNotify(
                    event = window,
                    window = window,
                    aboveSibling = previousSibling,
                    x = x.toInt(),
                    y = y.toInt(),
                    width = width.toInt(),
                    height = height.toInt(),
                    borderWidth = borderWidth.toInt(),
                    overrideRedirect = overrideRedirect,
                ),
            )
            parent.sendEvent(
                Event.SUBSTRUCTURE_NOTIFY,
                ConfigureNotify(
                    event = parent,
                    window = window,
                    aboveSibling = previousSibling,
                    x = x.toInt(),
                    y = y.toInt(),
                    width = width.toInt(),
                    height = height.toInt(),
                    borderWidth = borderWidth.toInt(),
                    overrideRedirect = overrideRedirect,
                ),
            )
        } else {
            parent.sendEvent(
                Event.SUBSTRUCTURE_REDIRECT,
                ConfigureRequest(parent, window, window.previousSibling(), x, y, width, height, borderWidth, stackMode, valueMask),
            )
        }
    }

    fun reparentWindow(window: Window, newParent: Window) {
        val oldParent = window.parent
        oldParent?.removeChild(window)
        newParent.addChild(window)
    }

    fun findPointWindow(rootX: Short, rootY: Short): Window? = findPointWindow(rootWindow, rootX, rootY)

    private fun findPointWindow(window: Window, rootX: Short, rootY: Short): Window? {
        if (!(window.attributes.isMapped && window.containsPoint(rootX, rootY))) {
            return null
        }

        val child = window.getChildByCoords(rootX, rootY)

        return if (child != null) {
            findPointWindow(child, rootX, rootY)
        } else {
            window
        }
    }

    fun addOnWindowModificationListener(onWindowModificationListener: OnWindowModificationListener?) {
        onWindowModificationListeners.add(onWindowModificationListener)
    }

    fun removeOnWindowModificationListener(onWindowModificationListener: OnWindowModificationListener?) {
        onWindowModificationListeners.remove(onWindowModificationListener)
    }

    private fun triggerOnMapWindow(window: Window) {
        onWindowModificationListeners.indices.reversed().forEach {
            onWindowModificationListeners[it]!!.onMapWindow(window)
        }
    }

    private fun triggerOnUnmapWindow(window: Window) {
        onWindowModificationListeners.indices.reversed().forEach {
            onWindowModificationListeners[it]!!.onUnmapWindow(window)
        }
    }

    private fun triggerOnChangeWindowZOrder(window: Window) {
        onWindowModificationListeners.indices.reversed().forEach {
            onWindowModificationListeners[it]!!.onChangeWindowZOrder(window)
        }
    }

    protected fun triggerOnUpdateWindowContent(window: Window) {
        onWindowModificationListeners.indices.reversed().forEach {
            onWindowModificationListeners[it]!!.onUpdateWindowContent(window)
        }
    }

    protected fun triggerOnUpdateWindowGeometry(window: Window, resized: Boolean) {
        onWindowModificationListeners.indices.reversed().forEach {
            onWindowModificationListeners[it]!!.onUpdateWindowGeometry(window, resized)
        }
    }

    fun triggerOnUpdateWindowAttributes(window: Window, mask: Bitmask) {
        onWindowModificationListeners.indices.reversed().forEach {
            onWindowModificationListeners[it]!!.onUpdateWindowAttributes(window, mask)
        }
    }

    fun triggerOnModifyWindowProperty(window: Window, property: Property) {
        onWindowModificationListeners.indices.reversed().forEach {
            onWindowModificationListeners[it]!!.onModifyWindowProperty(window, property)
        }
    }
}
