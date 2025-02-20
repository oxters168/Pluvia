package com.winlator.winhandler

internal object RequestCodes {
    const val EXIT: Byte = 0
    const val INIT: Byte = 1
    const val EXEC: Byte = 2
    const val KILL_PROCESS: Byte = 3
    const val LIST_PROCESSES: Byte = 4
    const val GET_PROCESS: Byte = 5
    const val SET_PROCESS_AFFINITY: Byte = 6
    const val MOUSE_EVENT: Byte = 7
    const val GET_GAMEPAD: Byte = 8
    const val GET_GAMEPAD_STATE: Byte = 9
    const val RELEASE_GAMEPAD: Byte = 10
    const val KEYBOARD_EVENT: Byte = 11
    const val BRING_TO_FRONT: Byte = 12
    const val CURSOR_POS_FEEDBACK: Byte = 13
}
