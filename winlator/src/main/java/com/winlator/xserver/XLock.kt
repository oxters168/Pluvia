package com.winlator.xserver

fun interface XLock : AutoCloseable {
    override fun close()
}
