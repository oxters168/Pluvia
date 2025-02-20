package com.winlator.inputcontrols

import android.graphics.Paint

class ControlElement {
    enum class Type {
        BUTTON,
        D_PAD,
        RANGE_BUTTON,
        STICK,
        TRACKPAD,
        ;

        companion object {
            fun names(): Array<String?> {
                val types = entries.toTypedArray()
                val names = arrayOfNulls<String>(types.size)
                for (i in types.indices) names[i] = types[i].name.replace("_", "-")
                return names
            }
        }
    }

    enum class Shape {
        CIRCLE,
        RECT,
        ROUND_RECT,
        SQUARE,
        ;

        companion object {
            fun names(): Array<String?> {
                val shapes = entries.toTypedArray()
                val names = arrayOfNulls<String>(shapes.size)
                for (i in shapes.indices) names[i] = shapes[i].name.replace("_", " ")
                return names
            }
        }
    }

    enum class Range(max: Int) {
        FROM_A_TO_Z(26),
        FROM_0_TO_9(10),
        FROM_F1_TO_F12(12),
        FROM_NP0_TO_NP9(10),
        ;

        val max: Byte = max.toByte()

        companion object {
            fun names(): Array<String?> {
                val ranges = entries.toTypedArray()
                val names = arrayOfNulls<String>(ranges.size)
                for (i in ranges.indices) names[i] = ranges[i].name.replace("_", " ")
                return names
            }
        }
    }

    companion object {
        const val STICK_DEAD_ZONE: Float = 0.15f
        const val DPAD_DEAD_ZONE: Float = 0.3f
        const val STICK_SENSITIVITY: Float = 3.0f
        const val TRACKPAD_MIN_SPEED: Float = 0.8f
        const val TRACKPAD_MAX_SPEED: Float = 20.0f
        const val TRACKPAD_ACCELERATION_THRESHOLD: Byte = 4
        const val BUTTON_MIN_TIME_TO_KEEP_PRESSED: Short = 300
        private fun getTextSizeForWidth(paint: Paint, text: String, desiredWidth: Float): Float {
            val testTextSize: Byte = 48
            paint.textSize = testTextSize.toFloat()
            return testTextSize * desiredWidth / paint.measureText(text)
        }

        private fun getRangeTextForIndex(range: Range, index: Int): String {
            var text = ""
            text = when (range) {
                Range.FROM_A_TO_Z -> (65 + index).toChar()
                    .toString()

                Range.FROM_0_TO_9 -> ((index + 1) % 10).toString()
                Range.FROM_F1_TO_F12 -> "F" + (index + 1)
                Range.FROM_NP0_TO_NP9 -> "NP" + ((index + 1) % 10)
            }
            return text
        }
    }
}
