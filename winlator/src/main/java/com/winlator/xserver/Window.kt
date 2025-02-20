package com.winlator.xserver

import android.util.SparseArray
import androidx.core.util.size
import com.winlator.xserver.Atom.getId
import com.winlator.xserver.events.Event
import com.winlator.xserver.events.PropertyNotify
import java.util.Collections
import java.util.Stack

class Window(
    id: Int,
    var content: Drawable?,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    val originClient: XClient?,
) : XResource(id) {

    companion object {
        const val FLAG_X: Int = 1
        const val FLAG_Y: Int = 1 shl 1
        const val FLAG_WIDTH: Int = 1 shl 2
        const val FLAG_HEIGHT: Int = 1 shl 3
        const val FLAG_BORDER_WIDTH: Int = 1 shl 4
        const val FLAG_SIBLING: Int = 1 shl 5
        const val FLAG_STACK_MODE: Int = 1 shl 6
    }

    enum class StackMode {
        ABOVE,
        BELOW,
        TOP_IF,
        BOTTOM_IF,
        OPPOSITE,
    }

    enum class MapState {
        UNMAPPED,
        UNVIEWABLE,
        VIEWABLE,
    }

    enum class WMHints {
        FLAGS,
        INPUT,
        INITIAL_STATE,
        ICON_PIXMAP,
        ICON_WINDOW,
        ICON_X,
        ICON_Y,
        ICON_MASK,
        WINDOW_GROUP,
    }

    var x: Short

    var y: Short

    var width: Short

    var height: Short

    var borderWidth: Short = 0

    var parent: Window? = null
        private set

    val attributes: WindowAttributes = WindowAttributes(this)

    private val properties = SparseArray<Property>()

    val children = ArrayList<Window>()

    private val immutableChildren: MutableList<Window?> = Collections.unmodifiableList<Window?>(children)

    private val eventListeners = ArrayList<EventListener>()

    init {
        this.x = x.toShort()
        this.y = y.toShort()
        this.width = width.toShort()
        this.height = height.toShort()
    }

    fun getProperty(id: Int): Property? = properties.get(id)

    fun addProperty(property: Property) {
        properties.put(property.name, property)
    }

    fun removeProperty(id: Int) {
        properties.remove(id)
        sendEvent(Event.PROPERTY_CHANGE, PropertyNotify(this, id, true))
    }

    fun modifyProperty(atom: Int, type: Int, format: Property.Format, mode: Property.Mode?, data: ByteArray): Property? {
        var property = getProperty(atom)
        var modified = false
        if (property == null) {
            addProperty((Property(atom, type, format, data).also { property = it }))
            modified = true
        } else if (mode == Property.Mode.REPLACE) {
            if (property.format == format) {
                property.replace(data)
            } else {
                properties.put(atom, Property(atom, type, format, data))
            }
            modified = true
        } else if (property.format == format && property.type == type) {
            if (mode == Property.Mode.PREPEND) {
                property.prepend(data)
            } else if (mode == Property.Mode.APPEND) {
                property.append(data)
            }
            modified = true
        }

        if (modified) {
            sendEvent(Event.PROPERTY_CHANGE, PropertyNotify(this, atom, false))
            return property
        } else {
            return null
        }
    }

    val name: String
        get() {
            val property = getProperty(getId("WM_NAME"))
            return property?.toString() ?: ""
        }

    val className: String
        get() {
            val property = getProperty(getId("WM_CLASS"))
            return property?.toString() ?: ""
        }

    fun getWMHintsValue(wmHints: WMHints): Int {
        val property = getProperty(getId("WM_HINTS"))
        return property?.getInt(wmHints.ordinal) ?: 0
    }

    val processId: Int
        get() {
            val property = getProperty(getId("_NET_WM_PID"))
            return property?.getInt(0) ?: 0
        }

    val isWoW64: Boolean
        get() {
            val property = getProperty(getId("_NET_WM_WOW64"))
            return property != null && property.data!!.get(0).toInt() == 1
        }

    val handle: Long
        get() {
            val property = getProperty(getId("_NET_WM_HWND"))
            return property?.getLong(0) ?: 0
        }

    val isApplicationWindow: Boolean
        get() {
            val windowGroup = getWMHintsValue(WMHints.WINDOW_GROUP)
            return attributes.isMapped && !this.name.isEmpty() && windowGroup == id && width > 1 && height > 1
        }

    val isInputOutput: Boolean
        get() = content != null

    fun addChild(child: Window?) {
        if (child == null || child.parent == this) return
        child.parent = this
        children.add(child)
    }

    fun removeChild(child: Window?) {
        if (child == null || child.parent != this) return
        child.parent = null
        children.remove(child)
    }

    fun previousSibling(): Window? {
        if (parent == null) return null
        val index = parent!!.children.indexOf(this)
        return if (index > 0) parent!!.children.get(index - 1) else null
    }

    fun moveChildAbove(child: Window?, sibling: Window?) {
        children.remove(child)
        if (sibling != null && children.contains(sibling)) {
            children.add(children.indexOf(sibling) + 1, child!!)
            return
        }
        children.add(child!!)
    }

    fun moveChildBelow(child: Window?, sibling: Window?) {
        children.remove(child)
        if (sibling != null && children.contains(sibling)) {
            children.add(children.indexOf(sibling), child!!)
            return
        }
        children.add(0, child!!)
    }

    fun getChildren(): MutableList<Window?> {
        return immutableChildren
    }

    val childCount: Int
        get() = children.size

    fun addEventListener(eventListener: EventListener?) {
        eventListeners.add(eventListener!!)
    }

    fun removeEventListener(eventListener: EventListener?) {
        eventListeners.remove(eventListener)
    }

    fun hasEventListenerFor(eventId: Int): Boolean {
        for (eventListener in eventListeners) {
            if (eventListener.isInterestedIn(eventId)) {
                return true
            }
        }

        return false
    }

    fun hasEventListenerFor(mask: Bitmask): Boolean {
        for (eventListener in eventListeners) {
            if (eventListener.isInterestedIn(mask)) {
                return true
            }
        }

        return false
    }

    fun sendEvent(eventId: Int, event: Event) {
        for (eventListener in eventListeners) {
            if (eventListener.isInterestedIn(eventId)) {
                eventListener.sendEvent(event)
            }
        }
    }

    fun sendEvent(eventMask: Bitmask, event: Event) {
        eventListeners.forEach { eventListener ->
            if (eventListener.isInterestedIn(eventMask)) {
                eventListener.sendEvent(event)
            }
        }
    }

    fun sendEvent(eventId: Int, event: Event, client: XClient?) {
        eventListeners.forEach { eventListener ->
            if (eventListener.isInterestedIn(eventId) && eventListener.client == client) {
                eventListener.sendEvent(event)
            }
        }
    }

    fun sendEvent(eventMask: Bitmask, event: Event, client: XClient?) {
        eventListeners.forEach { eventListener ->
            if (eventListener.isInterestedIn(eventMask) && eventListener.client == client) {
                eventListener.sendEvent(event)
            }
        }
    }

    fun sendEvent(event: Event) {
        eventListeners.forEach { eventListener ->
            eventListener.sendEvent(event)
        }
    }

    fun containsPoint(rootX: Short, rootY: Short): Boolean {
        val localPoint = rootPointToLocal(rootX, rootY)
        return localPoint[0] >= 0 && localPoint[1] >= 0 && localPoint[0] < width && localPoint[1] < height
    }

    fun rootPointToLocal(x: Short, y: Short): ShortArray {
        var x = x
        var y = y
        var window: Window? = this

        while (window != null) {
            x = (x - window.x).toShort()
            y = (y - window.y).toShort()
            window = window.parent
        }

        return shortArrayOf(x, y)
    }

    fun localPointToRoot(x: Short, y: Short): ShortArray? {
        var x = x
        var y = y
        var window: Window? = this

        while (window != null) {
            x = (x + window.x).toShort()
            y = (y + window.y).toShort()
            window = window.parent
        }

        return shortArrayOf(x, y)
    }

    val rootX: Short
        get() {
            var rootX = x
            var window = parent

            while (window != null) {
                rootX = (rootX + window.x).toShort()
                window = window.parent
            }

            return rootX
        }

    val rootY: Short
        get() {
            var rootY = y
            var window = parent

            while (window != null) {
                rootY = (rootY + window.y).toShort()
                window = window.parent
            }

            return rootY
        }

    fun getAncestorWithEventMask(eventMask: Bitmask): Window? {
        var window: Window? = this

        while (window != null) {
            if (window.hasEventListenerFor(eventMask)) {
                return window
            }

            if (window.attributes.doNotPropagateMask.intersects(eventMask)) {
                return null
            }

            window = window.parent
        }

        return null
    }

    fun getAncestorWithEventId(eventId: Int): Window? = getAncestorWithEventId(eventId, null)

    fun getAncestorWithEventId(eventId: Int, endWindow: Window?): Window? {
        var window: Window? = this

        while (window != null) {
            if (window.hasEventListenerFor(eventId)) {
                return window
            }

            if (window == endWindow || window.attributes.doNotPropagateMask.isSet(eventId)) {
                return null
            }

            window = window.parent
        }

        return null
    }

    fun isAncestorOf(window: Window?): Boolean {
        var window = window

        if (window == this) {
            return false
        }

        while (window != null) {
            if (window == this) {
                return true
            }

            window = window.parent
        }

        return false
    }

    fun getChildByCoords(x: Short, y: Short): Window? {
        children.indices.reversed().forEach {
            val child = children[it]

            if (child.attributes.isMapped && child.containsPoint(x, y)) {
                return child
            }
        }

        return null
    }

    val mapState: MapState
        get() {
            if (!attributes.isMapped) {
                return MapState.UNMAPPED
            }

            var window: Window? = this

            do {
                window = window!!.parent
                if (window == null) {
                    return MapState.VIEWABLE
                }
            } while (window.attributes.isMapped)

            return MapState.UNVIEWABLE
        }

    val allEventMasks: Bitmask
        get() {
            val eventMask = Bitmask()

            eventListeners.forEach { eventListener ->
                eventMask.join(eventListener.eventMask)
            }

            return eventMask
        }

    val buttonPressListener: EventListener?
        get() {
            eventListeners.forEach { eventListener ->
                if (eventListener.isInterestedIn(Event.BUTTON_PRESS)) {
                    return eventListener
                }
            }

            return null
        }

    fun disableAllDescendants() {
        val stack = Stack<Window>()
        stack.push(this)
        while (!stack.isEmpty()) {
            val window = stack.pop()
            window.attributes.isEnabled = false
            stack.addAll(window.children)
        }
    }

    fun serializeProperties(): String {
        var result = ""
        for (i in 0..<properties.size) {
            val property = properties.valueAt(i)
            result += property.nameAsString() + "=" + property + "\n"
        }

        return result
    }
}
