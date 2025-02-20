package com.winlator.xserver

class PixmapFormat(depth: Int, bitsPerPixel: Int, scanlinePad: Int) {
    val depth: Byte = depth.toByte()
    val bitsPerPixel: Byte = bitsPerPixel.toByte()
    val scanlinePad: Byte = scanlinePad.toByte()
}
