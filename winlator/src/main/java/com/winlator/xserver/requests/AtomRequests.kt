package com.winlator.xserver.requests

import com.winlator.xconnector.XInputStream
import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.Atom
import com.winlator.xserver.XClient
import com.winlator.xserver.XClientRequestHandler
import com.winlator.xserver.errors.BadAtom
import com.winlator.xserver.errors.XRequestError
import java.io.IOException

object AtomRequests {
    @Throws(IOException::class, XRequestError::class)
    fun internAtom(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        val onlyIfExists = client.requestData.toInt() == 1
        val length = inputStream.readShort()

        inputStream.skip(2)

        val name = inputStream.readString8(length.toInt())

        val id = if (onlyIfExists) {
            Atom.getId(name)
        } else {
            Atom.internAtom(name)
        }

        if (id < 0) {
            throw BadAtom(id)
        }

        outputStream.lock().use {
            outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
            outputStream.writeByte(0.toByte())
            outputStream.writeShort(client.sequenceNumber)
            outputStream.writeInt(0)
            outputStream.writeInt(id)
            outputStream.writePad(20)
        }
    }
}
