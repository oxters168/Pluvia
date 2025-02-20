package com.winlator.xserver

import com.winlator.xconnector.Client
import com.winlator.xconnector.ConnectionHandler

class XClientConnectionHandler(private val xServer: XServer) : ConnectionHandler {
    override fun handleNewConnection(client: Client?) {
        client?.createIOStreams()
        client?.tag = XClient(xServer, client.inputStream!!, client.outputStream!!)
    }

    override fun handleConnectionShutdown(client: Client?) {
        (client?.tag as XClient).freeResources()
    }
}
