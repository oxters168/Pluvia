package com.winlator.math

import kotlin.math.ceil
import kotlin.math.floor

// TODO: Maybe these can be `infix`?
object Mathf {
    @JvmStatic
    fun clamp(x: Float, min: Float, max: Float): Float = if (x < min) min else (if (x > max) max else x)

    @JvmStatic
    fun clamp(x: Int, min: Int, max: Int): Int = if (x < min) min else (if (x > max) max else x)

    @JvmStatic
    fun roundTo(x: Float, step: Float): Float = (floor((x / step).toDouble()) * step).toFloat()

    @JvmStatic
    fun roundPoint(x: Float): Int = (if (x <= 0) floor(x.toDouble()) else ceil(x.toDouble())).toInt()

    @JvmStatic
    fun sign(x: Float): Byte = (if (x < 0) -1 else (if (x > 0) 1 else 0)).toByte()

    @JvmStatic
    fun lengthSq(x: Float, y: Float): Float = x * x + y * y
}
