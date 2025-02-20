package com.winlator.xconnector

interface ConnectionHandler {
    fun handleConnectionShutdown(client: Client?)

    fun handleNewConnection(client: Client?)
}
