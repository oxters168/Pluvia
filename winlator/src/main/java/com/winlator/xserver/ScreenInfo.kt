package com.winlator.xserver

class ScreenInfo {

    val width: Short

    val height: Short

    val widthInMillimeters: Short
        get() = (width / 10).toShort()

    val heightInMillimeters: Short
        get() = (height / 10).toShort()

    constructor(value: String) {
        val parts = value.split("x".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        width = parts[0].toShort()
        height = parts[1].toShort()
    }

    constructor(width: Int, height: Int) {
        this.width = width.toShort()
        this.height = height.toShort()
    }

    override fun toString(): String = width.toString() + "x" + height
}
