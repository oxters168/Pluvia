package com.winlator.xenvironment.components

import com.winlator.alsaserver.ALSAClientConnectionHandler
import com.winlator.alsaserver.ALSARequestHandler
import com.winlator.xconnector.UnixSocketConfig
import com.winlator.xconnector.XConnectorEpoll
import com.winlator.xenvironment.EnvironmentComponent

class ALSAServerComponent(private val socketConfig: UnixSocketConfig) : EnvironmentComponent() {

    private var connector: XConnectorEpoll? = null

    override fun start() {
        if (connector != null) {
            return
        }

        connector = XConnectorEpoll(socketConfig, ALSAClientConnectionHandler(), ALSARequestHandler()).apply {
            isMultithreadedClients = true
            start()
        }
    }

    override fun stop() {
        connector?.stop()
        connector = null
    }
}
