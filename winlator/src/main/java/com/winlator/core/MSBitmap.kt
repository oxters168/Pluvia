package com.winlator.core

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import com.winlator.core.FileUtils.read
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder

object MSBitmap {
    fun open(targetFile: File): Bitmap? {
        if (!targetFile.isFile) {
            return null
        }

        val bytes = read(targetFile) ?: return null

        val data = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        if (data.getShort().toInt() != 0x4d42) {
            return null
        }

        val fileSize = data.getInt()

        if (fileSize > targetFile.length()) {
            return null
        }

        data.getInt()

        val dataOffset = data.getInt()
        val infoHeaderSize = data.getInt()
        val width = data.getInt()
        var height = data.getInt()
        val planes = data.getShort()
        val bitCount = data.getShort()
        val compression = data.getInt()
        val imageSize = data.getInt()
        val hr = data.getInt()
        val vr = data.getInt()
        val colorsUsed = data.getInt()
        val colorsImportant = data.getInt()

        if (width == 0 || height == 0) {
            return null
        }

        var invertY = true
        if (height < 0) {
            height *= -1
            invertY = false
        }

        val pixels = ByteBuffer.allocate(width * height * 4)
        var r1: Byte = 0
        var g1: Byte = 0
        var b1: Byte = 0
        var r2: Byte = 0
        var g2: Byte = 0
        var b2: Byte = 0
        var started = false
        var blank = true
        var y = height - 1
        var i = data.position()
        var j: Int
        var line: Int

        while (y >= 0) {
            line = if (invertY) {
                y
            } else {
                height - 1 - y
            }

            for (x in 0..<width) {
                j = line * width * 4 + x * 4
                b1 = data[i++]
                g1 = data[i++]
                r1 = data[i++]
                pixels.put(j + 2, b1)
                pixels.put(j + 1, g1)
                pixels.put(j + 0, r1)
                pixels.put(j + 3, 255.toByte())

                if (!started) {
                    b2 = b1
                    g2 = g1
                    r2 = r1
                    started = true
                } else if (r1 != r2 || b1 != b2 || g1 != g2) {
                    blank = false
                }
            }

            i += width % 4
            y--
        }

        if (blank) return null

        val bitmap = createBitmap(width, height)
        bitmap.copyPixelsFromBuffer(pixels)

        return bitmap
    }

    fun create(bitmap: Bitmap, outputFile: File?): Boolean {
        val width = bitmap.width
        val height = bitmap.height

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val extraBytes = width % 4
        val imageSize = height * (3 * width + extraBytes)
        val infoHeaderSize = 40
        val dataOffset = 54
        val bitCount = 24
        val planes = 1
        val compression = 0
        val hr = 0
        val vr = 0
        val colorsUsed = 0
        val colorsImportant = 0

        val buffer = ByteBuffer.allocate(dataOffset + imageSize).order(ByteOrder.LITTLE_ENDIAN)

        buffer.putShort(0x4d42.toShort()) // "BM"
        buffer.putInt(dataOffset + imageSize)
        buffer.putInt(0)
        buffer.putInt(dataOffset)

        buffer.putInt(infoHeaderSize)
        buffer.putInt(width)
        buffer.putInt(height)
        buffer.putShort(planes.toShort())
        buffer.putShort(bitCount.toShort())
        buffer.putInt(compression)
        buffer.putInt(imageSize)
        buffer.putInt(hr)
        buffer.putInt(vr)
        buffer.putInt(colorsUsed)
        buffer.putInt(colorsImportant)

        val rowBytes = 3 * width + extraBytes
        var y = height - 1
        var i = 0
        var j: Int

        while (y >= 0) {
            for (x in 0..<width) {
                j = dataOffset + y * rowBytes + x * 3

                val pixel = pixels[i++]

                buffer.put(j + 0, Color.blue(pixel).toByte())
                buffer.put(j + 1, Color.green(pixel).toByte())
                buffer.put(j + 2, Color.red(pixel).toByte())
            }

            if (extraBytes > 0) {
                val fillOffset = dataOffset + y * rowBytes + width * 3

                j = fillOffset

                while (j < fillOffset + extraBytes) {
                    buffer.put(j, 255.toByte())
                    j++
                }
            }

            y--
        }

        try {
            FileOutputStream(outputFile).use { fos ->
                fos.write(buffer.array())
                return true
            }
        } catch (e: IOException) {
            return false
        }
    }
}
