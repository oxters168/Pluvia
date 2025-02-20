package com.winlator.sysvshm

import com.winlator.xconnector.Client
import com.winlator.xconnector.ConnectionHandler

class SysVSHMConnectionHandler(private val sysVSharedMemory: SysVSharedMemory) : ConnectionHandler {

    override fun handleNewConnection(client: Client?) {
        client?.createIOStreams()
        client?.tag = sysVSharedMemory
    }

    override fun handleConnectionShutdown(client: Client?) {}
}
