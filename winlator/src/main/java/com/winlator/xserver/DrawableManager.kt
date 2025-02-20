package com.winlator.xserver

import android.util.SparseArray
import com.winlator.xserver.XResourceManager.OnResourceLifecycleListener

class DrawableManager(private val xServer: XServer) : XResourceManager(), OnResourceLifecycleListener {

    private val drawables = SparseArray<Drawable>()

    val visual: Visual?
        get() = xServer.pixmapManager.visual

    init {
        xServer.pixmapManager.addOnResourceLifecycleListener(this)
    }

    fun getDrawable(id: Int): Drawable? = drawables.get(id)

    fun createDrawable(id: Int, width: Short, height: Short, depth: Byte): Drawable? =
        createDrawable(id, width, height, xServer.pixmapManager.getVisualForDepth(depth))

    fun createDrawable(id: Int, width: Short, height: Short, visual: Visual?): Drawable? {
        if (id == 0) {
            return Drawable(id, width.toInt(), height.toInt(), visual)
        }

        if (drawables.indexOfKey(id) >= 0) {
            return null
        }

        val drawable = Drawable(id, width.toInt(), height.toInt(), visual)

        drawables.put(id, drawable)

        return drawable
    }

    fun removeDrawable(id: Int) {
        val drawable = drawables.get(id)

        val texture = drawable.texture
        xServer.renderer?.xServerView?.queueEvent(Runnable { texture?.destroy() })

        drawable.onDestroyListener?.call(drawable)
        drawable.onDrawListener = null

        drawables.remove(id)
    }

    override fun onCreateResource(resource: XResource?) {
    }

    override fun onFreeResource(resource: XResource?) {
        if (resource is Pixmap) {
            removeDrawable(resource.drawable.id)
        }
    }
}
