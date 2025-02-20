package com.winlator.alsaserver

import com.winlator.xconnector.Client
import com.winlator.xconnector.ConnectionHandler

class ALSAClientConnectionHandler : ConnectionHandler {
    override fun handleNewConnection(client: Client?) {
        client?.createIOStreams()
        client?.tag = ALSAClient()
    }

    override fun handleConnectionShutdown(client: Client?) {
        (client?.tag as ALSAClient).release()
    }
}
