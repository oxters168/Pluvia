package com.winlator.xserver.requests

import com.winlator.xconnector.XInputStream
import com.winlator.xconnector.XOutputStream
import com.winlator.xserver.Atom
import com.winlator.xserver.XClient
import com.winlator.xserver.XClientRequestHandler
import com.winlator.xserver.errors.BadAtom
import com.winlator.xserver.errors.BadWindow
import com.winlator.xserver.errors.XRequestError
import java.io.IOException

object SelectionRequests {
    @Throws(IOException::class, XRequestError::class)
    fun setSelectionOwner(client: XClient, inputStream: XInputStream, outputStream: XOutputStream?) {
        val windowId = inputStream.readInt()
        val atom = inputStream.readInt()
        val timestamp = inputStream.readInt()

        val owner = client.xServer.windowManager.getWindow(windowId)
        if (owner == null) {
            throw BadWindow(windowId)
        }

        if (!Atom.isValid(atom)) {
            throw BadAtom(atom)
        }

        client.xServer.selectionManager.setSelection(atom, owner, client, timestamp)
    }

    @Throws(IOException::class, XRequestError::class)
    fun getSelectionOwner(client: XClient, inputStream: XInputStream, outputStream: XOutputStream) {
        val atom = inputStream.readInt()
        if (!Atom.isValid(atom)) {
            throw BadAtom(atom)
        }

        val owner = client.xServer.selectionManager.getSelection(atom).owner

        outputStream.lock().use {
            outputStream.writeByte(XClientRequestHandler.RESPONSE_CODE_SUCCESS)
            outputStream.writeByte(0.toByte())
            outputStream.writeShort(client.sequenceNumber)
            outputStream.writeInt(0)
            outputStream.writeInt(owner?.id ?: 0)
            outputStream.writePad(20)
        }
    }
}
