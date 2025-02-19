package com.winlator.box86_64

class Box86_64Preset(val id: String, val name: String) {
    companion object {
        const val STABILITY: String = "STABILITY"
        const val COMPATIBILITY: String = "COMPATIBILITY"
        const val INTERMEDIATE: String = "INTERMEDIATE"
        const val PERFORMANCE: String = "PERFORMANCE"
        const val CUSTOM: String = "CUSTOM"
    }

    val isCustom: Boolean
        get() = id.startsWith(CUSTOM)

    override fun toString(): String = name
}
