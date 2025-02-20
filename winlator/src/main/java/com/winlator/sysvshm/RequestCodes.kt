package com.winlator.sysvshm

enum class RequestCodes(val code: Byte) {
    SHMGET(0),
    GET_FD(1),
    DELETE(2),
}
