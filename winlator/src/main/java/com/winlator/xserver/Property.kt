package com.winlator.xserver

import com.winlator.core.ArrayUtils
import com.winlator.core.StringUtils
import com.winlator.xserver.Atom.getName
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

class Property(val name: Int, val type: Int, val format: Format, data: ByteArray?) {

    enum class Mode {
        REPLACE,
        PREPEND,
        APPEND,
    }

    enum class Format(value: Int) {
        BYTE_ARRAY(8),
        SHORT_ARRAY(16),
        INT_ARRAY(32),
        ;

        val value: Byte = value.toByte()

        companion object {
            fun valueOf(format: Int): Format? {
                when (format) {
                    8 -> return BYTE_ARRAY
                    16 -> return SHORT_ARRAY
                    32 -> return INT_ARRAY
                }

                return null
            }
        }
    }

    var data: ByteBuffer? = null

    init {
        replace(data)
    }

    fun replace(data: ByteArray?) {
        this.data = ByteBuffer.wrap(data ?: ByteArray(0)).order(ByteOrder.LITTLE_ENDIAN)
    }

    fun prepend(values: ByteArray) {
        replace(ArrayUtils.concat(values, this.data!!.array()))
    }

    fun append(values: ByteArray?) {
        replace(ArrayUtils.concat(this.data!!.array(), values!!))
    }

    override fun toString(): String {
        val type = getName(this.type)
        data!!.rewind()
        when (type) {
            "UTF8_STRING" -> return StringUtils.fromANSIString(data!!.array(), StandardCharsets.UTF_8)
            "STRING" -> return StringUtils.fromANSIString(data!!.array(), XServer.LATIN1_CHARSET)
            "ATOM" -> return getName(data!!.getInt(0))!!
            else -> {
                val sb = StringBuilder()
                var i = 0
                val size = data!!.capacity() / (format.value.toInt() shr 3)

                while (i < size) {
                    if (i > 0) sb.append(",")
                    when (format) {
                        Format.BYTE_ARRAY -> sb.append(data!!.get().toInt())
                        Format.SHORT_ARRAY -> sb.append(data!!.getShort().toInt())
                        Format.INT_ARRAY -> sb.append(data!!.getInt())
                    }

                    i++
                }

                data!!.rewind()

                return sb.toString()
            }
        }
    }

    fun getInt(index: Int): Int = data!!.getInt(index * 4)

    fun getLong(index: Int): Long = data!!.getLong(index * 8)

    fun nameAsString(): String? = getName(name)
}
