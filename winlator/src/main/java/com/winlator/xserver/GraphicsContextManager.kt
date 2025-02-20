package com.winlator.xserver

import android.util.SparseArray
import com.winlator.xconnector.XInputStream

class GraphicsContextManager : XResourceManager() {
    private val graphicsContexts = SparseArray<GraphicsContext?>()

    fun getGraphicsContext(id: Int): GraphicsContext? {
        return graphicsContexts.get(id)
    }

    fun createGraphicsContext(id: Int, drawable: Drawable?): GraphicsContext? {
        if (graphicsContexts.indexOfKey(id) >= 0) return null
        val graphicsContext = GraphicsContext(id, drawable)
        graphicsContexts.put(id, graphicsContext)
        triggerOnCreateResourceListener(graphicsContext)
        return graphicsContext
    }

    fun freeGraphicsContext(id: Int) {
        triggerOnFreeResourceListener(graphicsContexts.get(id))
        graphicsContexts.remove(id)
    }

    fun updateGraphicsContext(
        graphicsContext: GraphicsContext,
        valueMask: Bitmask,
        inputStream: XInputStream,
    ) {
        for (index in valueMask) {
            when (index) {
                GraphicsContext.FLAG_FUNCTION ->
                    graphicsContext.function = GraphicsContext.Function.entries.toTypedArray()[inputStream.readInt()]

                GraphicsContext.FLAG_PLANE_MASK ->
                    graphicsContext.planeMask = inputStream.readInt()

                GraphicsContext.FLAG_FOREGROUND ->
                    graphicsContext.foreground = inputStream.readInt()

                GraphicsContext.FLAG_BACKGROUND ->
                    graphicsContext.background = inputStream.readInt()

                GraphicsContext.FLAG_LINE_WIDTH ->
                    graphicsContext.lineWidth = inputStream.readInt()

                GraphicsContext.FLAG_SUBWINDOW_MODE ->
                    graphicsContext.subwindowMode = GraphicsContext.SubwindowMode.entries.toTypedArray()[inputStream.readInt()]

                GraphicsContext.FLAG_LINE_STYLE,
                GraphicsContext.FLAG_CAP_STYLE,
                GraphicsContext.FLAG_JOIN_STYLE,
                GraphicsContext.FLAG_FILL_STYLE,
                GraphicsContext.FLAG_FILL_RULE,
                GraphicsContext.FLAG_GRAPHICS_EXPOSURES,
                GraphicsContext.FLAG_DASHES,
                GraphicsContext.FLAG_ARC_MODE,
                GraphicsContext.FLAG_TILE,
                GraphicsContext.FLAG_STIPPLE,
                GraphicsContext.FLAG_FONT,
                GraphicsContext.FLAG_CLIP_MASK,
                GraphicsContext.FLAG_TILE_STIPPLE_X_ORIGIN,
                GraphicsContext.FLAG_TILE_STIPPLE_Y_ORIGIN,
                GraphicsContext.FLAG_CLIP_X_ORIGIN,
                GraphicsContext.FLAG_CLIP_Y_ORIGIN,
                GraphicsContext.FLAG_DASH_OFFSET,
                -> inputStream.skip(4)
            }
        }
    }
}
