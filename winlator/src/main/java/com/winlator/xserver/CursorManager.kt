package com.winlator.xserver

import android.util.SparseArray

class CursorManager(private val drawableManager: DrawableManager) : XResourceManager() {
    private val cursors = SparseArray<Cursor?>()

    fun getCursor(id: Int): Cursor? = cursors.get(id)

    fun createCursor(id: Int, x: Short, y: Short, sourcePixmap: Pixmap, maskPixmap: Pixmap?): Cursor? {
        if (cursors.indexOfKey(id) >= 0) {
            return null
        }

        val drawable = drawableManager.createDrawable(0, sourcePixmap.drawable.width, sourcePixmap.drawable.height, sourcePixmap.drawable.visual)

        val cursor = Cursor(id, x.toInt(), y.toInt(), drawable, sourcePixmap.drawable, maskPixmap?.drawable)

        cursors.put(id, cursor)
        triggerOnCreateResourceListener(cursor)

        return cursor
    }

    fun freeCursor(id: Int) {
        triggerOnFreeResourceListener(cursors.get(id))
        cursors.remove(id)
    }

    fun recolorCursor(cursor: Cursor, foreRed: Byte, foreGreen: Byte, foreBlue: Byte, backRed: Byte, backGreen: Byte, backBlue: Byte) {
        if (cursor.maskImage != null) {
            val visible: Boolean = !isEmptyMaskImage(cursor.maskImage)
            cursor.isVisible = visible

            if (visible) {
                cursor.cursorImage!!.drawAlphaMaskedBitmap(
                    foreRed,
                    foreGreen,
                    foreBlue,
                    backRed,
                    backGreen,
                    backBlue,
                    cursor.sourceImage!!,
                    cursor.maskImage,
                )
            }
        }
    }

    companion object {
        private fun isEmptyMaskImage(maskImage: Drawable): Boolean {
            val maskData = maskImage.data!!.asIntBuffer()
            var result = true

            for (i in 0..<maskData.capacity()) {
                if (maskData.get(i) != 0x000000) {
                    result = false

                    break
                }
            }

            return result
        }
    }
}
