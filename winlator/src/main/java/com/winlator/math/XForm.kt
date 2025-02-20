package com.winlator.math

import kotlin.math.cos
import kotlin.math.sin

object XForm {

    private val tmpXForm = instance

    @JvmStatic
    val instance: FloatArray
        get() = identity(FloatArray(6))

    @JvmStatic
    fun identity(xform: FloatArray): FloatArray = set(xform, 1f, 0f, 0f, 1f, 0f, 0f)

    fun set(xform: FloatArray, n11: Float, n12: Float, n21: Float, n22: Float, dx: Float, dy: Float): FloatArray {
        xform[0] = n11
        xform[1] = n12
        xform[2] = n21
        xform[3] = n22
        xform[4] = dx
        xform[5] = dy

        return xform
    }

    @JvmStatic
    fun set(xform: FloatArray, tx: Float, ty: Float, sx: Float, sy: Float): FloatArray {
        xform[0] = sx
        xform[1] = 0f
        xform[2] = 0f
        xform[3] = sy
        xform[4] = tx
        xform[5] = ty

        return xform
    }

    @JvmStatic
    fun makeTransform(xform: FloatArray, tx: Float, ty: Float, sx: Float, sy: Float, angle: Float): FloatArray {
        val c = cos(angle.toDouble()).toFloat()
        val s = sin(angle.toDouble()).toFloat()

        return set(xform, sx * c, sy * s, sx * -s, sy * c, tx, ty)
    }

    fun makeTranslation(xform: FloatArray, x: Float, y: Float): FloatArray = set(xform, 1f, 0f, 0f, 1f, x, y)

    fun makeScale(xform: FloatArray, x: Float, y: Float): FloatArray = set(xform, x, 0f, 0f, y, 0f, 0f)

    fun makeRotation(xform: FloatArray, angle: Float): FloatArray {
        val c = cos(angle.toDouble()).toFloat()
        val s = sin(angle.toDouble()).toFloat()

        return set(xform, c, s, -s, c, 0f, 0f)
    }

    @Synchronized
    fun translate(xform: FloatArray, x: Float, y: Float): FloatArray = multiply(xform, xform, makeTranslation(tmpXForm, x, y))

    @Synchronized
    fun scale(xform: FloatArray, x: Float, y: Float): FloatArray = multiply(xform, xform, makeScale(tmpXForm, x, y))

    @Synchronized
    fun rotate(xform: FloatArray, angle: Float): FloatArray = multiply(xform, xform, makeRotation(tmpXForm, angle))

    @JvmStatic
    fun multiply(result: FloatArray, ta: FloatArray, tb: FloatArray): FloatArray {
        val a0 = ta[0]
        val a3 = ta[3]
        val a1 = ta[1]
        val a4 = ta[4]
        val a2 = ta[2]
        val a5 = ta[5]
        val b0 = tb[0]
        val b3 = tb[3]
        val b1 = tb[1]
        val b4 = tb[4]
        val b2 = tb[2]
        val b5 = tb[5]

        result[0] = a0 * b0 + a1 * b2
        result[1] = a0 * b1 + a1 * b3
        result[2] = a2 * b0 + a3 * b2
        result[3] = a2 * b1 + a3 * b3
        result[4] = a4 * b0 + a5 * b2 + b4
        result[5] = a4 * b1 + a5 * b3 + b5

        return result
    }

    @JvmOverloads
    fun transformPoint(xform: FloatArray, x: Float, y: Float, result: FloatArray = FloatArray(2)): FloatArray {
        result[0] = xform[0] * x + xform[2] * y + xform[4]
        result[1] = xform[1] * x + xform[3] * y + xform[5]

        return result
    }
}
