package com.winlator.xconnector

import java.io.IOException

fun interface XStreamLock : AutoCloseable {
    @Throws(IOException::class)
    override fun close()
}
