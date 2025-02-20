package com.winlator.xserver.extensions

import com.winlator.xconnector.XInputStream
import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.XClient
import com.winlator.xserver.errors.XRequestError
import java.io.IOException

interface Extension {
    val name: String

    val majorOpcode: Byte

    val firstErrorId: Byte

    val firstEventId: Byte

    @Throws(IOException::class, XRequestError::class)
    fun handleRequest(client: XClient, inputStream: XInputStream, outputStream: XOutputStream)
}
