package com.micewine.emu.core

enum class HighlightState {
    HIGHLIGHT_SHORTCUTS,
    HIGHLIGHT_SETTINGS,
    HIGHLIGHT_FILES,
    HIGHLIGHT_DONE;

    companion object {
        const val HIGHLIGHT_PREFERENCE_KEY = "tutorialState"

        fun fromOrdinal(int: Int?): HighlightState {
            return entries.getOrNull(int ?: -1) ?: HIGHLIGHT_DONE
        }
    }
}
