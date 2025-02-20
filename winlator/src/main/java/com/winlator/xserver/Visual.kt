package com.winlator.xserver

class Visual(
    val id: Int,
    val displayable: Boolean,
    depth: Int,
    bitsPerRGBValue: Int,
    val redMask: Int,
    val greenMask: Int,
    val blueMask: Int,
) {
    val visualClass: Byte = 4
    val depth: Byte = depth.toByte()
    val bitsPerRGBValue: Byte = bitsPerRGBValue.toByte()
    val colormapEntries: Short = 256
}
