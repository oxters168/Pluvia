package com.winlator.xserver

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import java.nio.ByteBuffer

class Pixmap(val drawable: Drawable) : XResource(drawable.id) {
    fun toBitmap(maskPixmap: Pixmap?): Bitmap {
        val maskData = maskPixmap?.drawable?.data
        val bitmap = createBitmap(drawable.width.toInt(), drawable.height.toInt())

        toBitmap(drawable.data, maskData, bitmap)

        return bitmap
    }

    companion object {
        /**
         * Native Methods
         */
        private external fun toBitmap(colorData: ByteBuffer?, maskData: ByteBuffer?, bitmap: Bitmap?)
    }
}
