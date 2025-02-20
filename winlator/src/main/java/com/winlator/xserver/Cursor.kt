package com.winlator.xserver

class Cursor(
    id: Int,
    val hotSpotX: Int,
    val hotSpotY: Int,
    val cursorImage: Drawable?,
    val sourceImage: Drawable?,
    val maskImage: Drawable?,
) : XResource(id) {
    var isVisible: Boolean = true
}
