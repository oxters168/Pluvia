package com.micewine.emu.controller

import com.micewine.emu.controller.ControllerUtils.KEYBOARD

object XKeyCodes {
    private val scanKeyCodes = mapOf(
        "Up" to (103 to 19),
        "Down" to (108 to 20),
        "Left" to (105 to 21),
        "Right" to (106 to 22),
        "ESC" to (1 to 111),
        "Enter" to (28 to 66),
        "Space" to (57 to 62),
        "A" to (30 to 29),
        "B" to (48 to 30),
        "C" to (46 to 31),
        "D" to (32 to 32),
        "E" to (18 to 33),
        "F" to (33 to 34),
        "G" to (34 to 35),
        "H" to (35 to 36),
        "I" to (23 to 37),
        "J" to (36 to 38),
        "K" to (37 to 39),
        "L" to (38 to 40),
        "M" to (50 to 41),
        "N" to (49 to 42),
        "O" to (24 to 43),
        "P" to (25 to 44),
        "Q" to (16 to 45),
        "R" to (19 to 46),
        "S" to (31 to 47),
        "T" to (20 to 48),
        "U" to (22 to 49),
        "V" to (47 to 50),
        "W" to (17 to 51),
        "X" to (45 to 52),
        "Y" to (21 to 53),
        "Z" to (44 to 54),
        "'" to (40 to 75),
        "LCtrl" to (29 to 113),
        "RCtrl" to (97 to 114),
        "LShift" to (42 to 59),
        "RShift" to (54 to 60),
        "Tab" to (15 to 61),
        "AltLeft" to (56 to 57),
        "F1" to (59 to 131),
        "F2" to (60 to 132),
        "F3" to (61 to 133),
        "F4" to (62 to 134),
        "F5" to (63 to 135),
        "F6" to (64 to 136),
        "F7" to (65 to 137),
        "F8" to (66 to 138),
        "F9" to (67 to 139),
        "F10" to (68 to 140),
        "F11" to (87 to 141),
        "F12" to (88 to 142),
        "Insert" to (110 to 124),
        "Home" to (102 to 122),
        "PageUp" to (104 to 92),
        "Delete" to (111 to 112),
        "End" to (107 to 123),
        "PageDown" to (109 to 93),
        "BackSpace" to (14 to 67),
        "0" to (82 to 144),
        "1" to (79 to 145),
        "2" to (80 to 146),
        "3" to (81 to 147),
        "4" to (75 to 148),
        "5" to (76 to 149),
        "6" to (77 to 150),
        "7" to (71 to 151),
        "8" to (72 to 152),
        "9" to (73 to 153),
    )

    fun getKeyNames(getMouseButtons: Boolean): List<String> {
        val keyNames: MutableList<String> = mutableListOf("--")

        if (getMouseButtons) {
            keyNames.plusAssign("M_Left")
            keyNames.plusAssign("M_Middle")
            keyNames.plusAssign("M_Right")
        } else {
            keyNames.plusAssign("Mouse")
        }

        for (i in scanKeyCodes.keys) {
            keyNames.plusAssign(i)
        }

        return keyNames
    }

    fun getXKeyScanCodes(key: String): List<Int> {
        val scanCode = scanKeyCodes[key]?.first ?: 0
        val keyCode = scanKeyCodes[key]?.second ?: 0
        return listOf(scanCode, keyCode, KEYBOARD)
    }
}
