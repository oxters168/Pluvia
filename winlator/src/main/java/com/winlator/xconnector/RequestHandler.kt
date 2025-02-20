package com.winlator.xconnector

import java.io.IOException

fun interface RequestHandler {
    @Throws(IOException::class)
    fun handleRequest(client: Client?): Boolean
}
