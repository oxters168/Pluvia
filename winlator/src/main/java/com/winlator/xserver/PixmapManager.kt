package com.winlator.xserver

import android.graphics.Bitmap
import android.util.SparseArray
import com.winlator.xserver.IDGenerator.generate

class PixmapManager : XResourceManager() {

    val visual: Visual = Visual(generate(), true, 32, 24, 0xff0000, 0x00ff00, 0x0000ff)

    val supportedVisuals: Array<Visual> = arrayOf<Visual>(visual, Visual(generate(), false, 1, 1, 0, 0, 0))

    val supportedPixmapFormats: Array<PixmapFormat> = arrayOf<PixmapFormat>(
        PixmapFormat(1, 1, 32),
        PixmapFormat(24, 32, 32),
        PixmapFormat(32, 32, 32),
    )

    private val pixmaps = SparseArray<Pixmap>()

    fun getPixmap(id: Int): Pixmap? = pixmaps.get(id)

    fun createPixmap(drawable: Drawable): Pixmap? {
        if (pixmaps.indexOfKey(drawable.id) >= 0) {
            return null
        }

        val pixmap = Pixmap(drawable)

        pixmaps.put(drawable.id, pixmap)
        triggerOnCreateResourceListener(pixmap)

        return pixmap
    }

    fun freePixmap(id: Int) {
        triggerOnFreeResourceListener(pixmaps.get(id))
        pixmaps.remove(id)
    }

    fun getVisualForDepth(depth: Byte): Visual? {
        if (depth == visual.depth) {
            return visual
        }

        for (visual in supportedVisuals) {
            if (depth == visual.depth) {
                return visual
            }
        }

        return null
    }

    fun getVisual(id: Int): Visual? {
        if (id == visual.id) {
            return visual
        }

        for (visual in supportedVisuals) {
            if (id == visual.id && visual.displayable) {
                return visual
            }
        }

        return null
    }

    fun getWindowIcon(window: Window): Bitmap? {
        val colorPixmapId = window.getWMHintsValue(Window.WMHints.ICON_PIXMAP)
        val maskPixmapId = window.getWMHintsValue(Window.WMHints.ICON_MASK)
        val colorPixmap = if (colorPixmapId != 0) getPixmap(colorPixmapId) else null
        val maskPixmap = if (maskPixmapId != 0) getPixmap(maskPixmapId) else null

        return colorPixmap?.toBitmap(maskPixmap)
    }
}
