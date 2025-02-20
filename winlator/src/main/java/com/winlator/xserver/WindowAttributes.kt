package com.winlator.xserver

import com.winlator.xconnector.XInputStream

class WindowAttributes(val window: Window) {

    companion object {
        const val FLAG_BACKGROUND_PIXMAP: Int = 1 shl 0
        const val FLAG_BACKGROUND_PIXEL: Int = 1 shl 1
        const val FLAG_BORDER_PIXMAP: Int = 1 shl 2
        const val FLAG_BORDER_PIXEL: Int = 1 shl 3
        const val FLAG_BIT_GRAVITY: Int = 1 shl 4
        const val FLAG_WIN_GRAVITY: Int = 1 shl 5
        const val FLAG_BACKING_STORE: Int = 1 shl 6
        const val FLAG_BACKING_PLANES: Int = 1 shl 7
        const val FLAG_BACKING_PIXEL: Int = 1 shl 8
        const val FLAG_OVERRIDE_REDIRECT: Int = 1 shl 9
        const val FLAG_SAVE_UNDER: Int = 1 shl 10
        const val FLAG_EVENT_MASK: Int = 1 shl 11
        const val FLAG_DO_NOT_PROPAGATE_MASK: Int = 1 shl 12
        const val FLAG_COLORMAP: Int = 1 shl 13
        const val FLAG_CURSOR: Int = 1 shl 14
    }

    enum class BackingStore {
        NOT_USEFUL,
        WHEN_MAPPED,
        ALWAYS,
    }

    enum class WindowClass {
        COPY_FROM_PARENT,
        INPUT_OUTPUT,
        INPUT_ONLY,
    }

    enum class BitGravity {
        FORGET,
        NORTH_WEST,
        NORTH,
        NORTH_EAST,
        WEST,
        CENTER,
        EAST,
        SOUTH_WEST,
        SOUTH,
        SOUTH_EAST,
        STATIC,
    }

    enum class WinGravity {
        UNMAP,
        NORTH_WEST,
        NORTH,
        NORTH_EAST,
        WEST,
        CENTER,
        EAST,
        SOUTH_WEST,
        SOUTH,
        SOUTH_EAST,
        STATIC,
    }

    var backingPixel: Int = 0
        private set

    var backingPlanes: Int = 1
        private set

    var backingStore: BackingStore? = BackingStore.NOT_USEFUL
        private set

    var bitGravity: BitGravity? = BitGravity.CENTER
        private set

    private var cursor: Cursor? = null

    var doNotPropagateMask: Bitmask = Bitmask(0)
        private set

    var eventMask: Bitmask = Bitmask(0)
        private set

    var isMapped: Boolean = false

    var isOverrideRedirect: Boolean = false
        private set

    var isSaveUnder: Boolean = false
        private set

    var isEnabled: Boolean = true

    var winGravity: WinGravity? = WinGravity.CENTER
        private set

    var windowClass: WindowClass? = WindowClass.INPUT_OUTPUT

    fun getCursor(): Cursor? {
        val parent = window.parent
        return if (cursor == null && parent != null) {
            parent.attributes.getCursor()
        } else {
            cursor
        }
    }

    fun update(valueMask: Bitmask, inputStream: XInputStream, client: XClient) {
        for (index in valueMask) {
            when (index) {
                FLAG_BACKGROUND_PIXEL -> window.content?.fillColor(inputStream.readInt())
                FLAG_BACKING_PIXEL -> backingPixel = inputStream.readInt()
                FLAG_BACKING_PLANES -> backingPlanes = inputStream.readInt()
                FLAG_BIT_GRAVITY -> bitGravity = BitGravity.entries[inputStream.readInt()]
                FLAG_WIN_GRAVITY -> winGravity = WinGravity.entries[inputStream.readInt()]
                FLAG_BACKING_STORE -> backingStore = BackingStore.entries[inputStream.readInt()]
                FLAG_SAVE_UNDER -> this.isSaveUnder = inputStream.readInt() == 1
                FLAG_OVERRIDE_REDIRECT -> this.isOverrideRedirect = inputStream.readInt() == 1
                FLAG_EVENT_MASK -> eventMask = Bitmask(inputStream.readInt())
                FLAG_DO_NOT_PROPAGATE_MASK -> doNotPropagateMask = Bitmask(inputStream.readInt())
                FLAG_CURSOR -> cursor = client.xServer.cursorManager.getCursor(inputStream.readInt())
                FLAG_BACKGROUND_PIXMAP,
                FLAG_BORDER_PIXMAP,
                FLAG_BORDER_PIXEL,
                FLAG_COLORMAP,
                -> inputStream.skip(4)
            }
        }

        client.xServer.windowManager.triggerOnUpdateWindowAttributes(window, valueMask)
    }
}
