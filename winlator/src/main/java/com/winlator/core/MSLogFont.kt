package com.winlator.core

import java.nio.ByteBuffer
import java.nio.ByteOrder

class MSLogFont {
    var height: Int = -11
        private set
    var width: Int = 0
        private set
    var escapement: Int = 0
        private set
    var orientation: Int = 0
        private set
    var weight: Int = 400
        private set
    var italic: Byte = 0
        private set
    var underline: Byte = 0
        private set
    var strikeOut: Byte = 0
        private set
    var charSet: Byte = 0
        private set
    var outPrecision: Byte = 0
        private set
    var clipPrecision: Byte = 0
        private set
    var quality: Byte = 0
        private set
    var pitchAndFamily: Byte = 34
        private set
    var faceName: String = "Tahoma"
        private set

    fun setHeight(height: Int): MSLogFont {
        this.height = height
        return this
    }

    fun setWidth(width: Int): MSLogFont {
        this.width = width
        return this
    }

    fun setEscapement(escapement: Int): MSLogFont {
        this.escapement = escapement
        return this
    }

    fun setOrientation(orientation: Int): MSLogFont {
        this.orientation = orientation
        return this
    }

    fun setWeight(weight: Int): MSLogFont {
        this.weight = weight
        return this
    }

    fun setItalic(italic: Byte): MSLogFont {
        this.italic = italic
        return this
    }

    fun setUnderline(underline: Byte): MSLogFont {
        this.underline = underline
        return this
    }

    fun setStrikeOut(strikeOut: Byte): MSLogFont {
        this.strikeOut = strikeOut
        return this
    }

    fun setCharSet(charSet: Byte): MSLogFont {
        this.charSet = charSet
        return this
    }

    fun setOutPrecision(outPrecision: Byte): MSLogFont {
        this.outPrecision = outPrecision
        return this
    }

    fun setClipPrecision(clipPrecision: Byte): MSLogFont {
        this.clipPrecision = clipPrecision
        return this
    }

    fun setQuality(quality: Byte): MSLogFont {
        this.quality = quality
        return this
    }

    fun setPitchAndFamily(pitchAndFamily: Byte): MSLogFont {
        this.pitchAndFamily = pitchAndFamily
        return this
    }

    fun setFaceName(faceName: String): MSLogFont {
        this.faceName = faceName
        return this
    }

    fun toByteArray(): ByteArray {
        val data = ByteBuffer.allocate(92).order(ByteOrder.LITTLE_ENDIAN)
        data.putInt(height)
        data.putInt(width)
        data.putInt(escapement)
        data.putInt(orientation)
        data.putInt(weight)
        data.put(italic)
        data.put(underline)
        data.put(strikeOut)
        data.put(charSet)
        data.put(outPrecision)
        data.put(clipPrecision)
        data.put(quality)
        data.put(pitchAndFamily)

        for (element in faceName) {
            data.putChar(element)
        }

        return data.array()
    }
}
