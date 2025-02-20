package com.winlator.renderer

import kotlin.math.ceil
import kotlin.math.min

class ViewTransformation {
    @JvmField
    var viewOffsetX: Int = 0

    @JvmField
    var viewOffsetY: Int = 0

    @JvmField
    var viewWidth: Int = 0

    @JvmField
    var viewHeight: Int = 0

    @JvmField
    var aspect: Float = 0f

    @JvmField
    var sceneScaleX: Float = 0f

    @JvmField
    var sceneScaleY: Float = 0f

    @JvmField
    var sceneOffsetX: Float = 0f

    @JvmField
    var sceneOffsetY: Float = 0f

    fun update(outerWidth: Int, outerHeight: Int, innerWidth: Int, innerHeight: Int) {
        aspect = min((outerWidth.toFloat() / innerWidth).toDouble(), (outerHeight.toFloat() / innerHeight).toDouble()).toFloat()

        viewWidth = ceil((innerWidth * aspect).toDouble()).toInt()
        viewHeight = ceil((innerHeight * aspect).toDouble()).toInt()
        viewOffsetX = ((outerWidth - innerWidth * aspect) * 0.5f).toInt()
        viewOffsetY = ((outerHeight - innerHeight * aspect) * 0.5f).toInt()

        sceneScaleX = (innerWidth * aspect) / outerWidth
        sceneScaleY = (innerHeight * aspect) / outerHeight
        sceneOffsetX = (innerWidth - innerWidth * sceneScaleX) * 0.5f
        sceneOffsetY = (innerHeight - innerHeight * sceneScaleY) * 0.5f
    }
}
