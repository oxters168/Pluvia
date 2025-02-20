package com.winlator.renderer

import com.winlator.xserver.Drawable

internal class RenderableWindow @JvmOverloads constructor(
    @JvmField val content: Drawable,
    rootX: Int,
    rootY: Int,
    @JvmField val forceFullscreen: Boolean = false,
) {
    @JvmField
    var rootX: Short = rootX.toShort()

    @JvmField
    var rootY: Short = rootY.toShort()
}
