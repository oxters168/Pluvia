package com.winlator.xserver

class GraphicsContext(id: Int, val drawable: Drawable?) : XResource(id) {

    companion object {
        const val FLAG_FUNCTION: Int = 1 shl 0
        const val FLAG_PLANE_MASK: Int = 1 shl 1
        const val FLAG_FOREGROUND: Int = 1 shl 2
        const val FLAG_BACKGROUND: Int = 1 shl 3
        const val FLAG_LINE_WIDTH: Int = 1 shl 4
        const val FLAG_LINE_STYLE: Int = 1 shl 5
        const val FLAG_CAP_STYLE: Int = 1 shl 6
        const val FLAG_JOIN_STYLE: Int = 1 shl 7
        const val FLAG_FILL_STYLE: Int = 1 shl 8
        const val FLAG_FILL_RULE: Int = 1 shl 9
        const val FLAG_TILE: Int = 1 shl 10
        const val FLAG_STIPPLE: Int = 1 shl 11
        const val FLAG_TILE_STIPPLE_X_ORIGIN: Int = 1 shl 12
        const val FLAG_TILE_STIPPLE_Y_ORIGIN: Int = 1 shl 13
        const val FLAG_FONT: Int = 1 shl 14
        const val FLAG_SUBWINDOW_MODE: Int = 1 shl 15
        const val FLAG_GRAPHICS_EXPOSURES: Int = 1 shl 16
        const val FLAG_CLIP_X_ORIGIN: Int = 1 shl 17
        const val FLAG_CLIP_Y_ORIGIN: Int = 1 shl 18
        const val FLAG_CLIP_MASK: Int = 1 shl 19
        const val FLAG_DASH_OFFSET: Int = 1 shl 20
        const val FLAG_DASHES: Int = 1 shl 21
        const val FLAG_ARC_MODE: Int = 1 shl 22
    }

    enum class Function {
        CLEAR,
        AND,
        AND_REVERSE,
        COPY,
        AND_INVERTED,
        NO_OP,
        XOR,
        OR,
        NOR,
        EQUIV,
        INVERT,
        OR_REVERSE,
        COPY_INVERTED,
        OR_INVERTED,
        NAND,
        SET,
    }

    enum class SubwindowMode {
        CLIP_BY_CHILDREN,
        INCLUDE_INFERIORS,
    }

    var function: Function? = Function.COPY
    var background: Int = 0xffffff
    var foreground: Int = 0x000000
    var lineWidth: Int = 1
    var planeMask: Int = -1
    var subwindowMode: SubwindowMode? = SubwindowMode.CLIP_BY_CHILDREN
}
