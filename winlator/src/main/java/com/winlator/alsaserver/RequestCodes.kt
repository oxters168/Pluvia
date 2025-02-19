package com.winlator.alsaserver

enum class RequestCodes(val code: Byte) {
    CLOSE(0),
    START(1),
    STOP(2),
    PAUSE(3),
    PREPARE(4),
    WRITE(5),
    DRAIN(6),
    POINTER(7),
}
