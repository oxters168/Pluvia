package com.winlator.xenvironment.components

import com.winlator.xconnector.UnixSocketConfig
import com.winlator.xconnector.XConnectorEpoll
import com.winlator.xenvironment.EnvironmentComponent
import com.winlator.xserver.XClientConnectionHandler
import com.winlator.xserver.XClientRequestHandler
import com.winlator.xserver.XServer

class XServerComponent(
    val xServer: XServer,
    private val socketConfig: UnixSocketConfig,
) : EnvironmentComponent() {

    private var connector: XConnectorEpoll? = null

    override fun start() {
        if (connector != null) {
            return
        }

        connector = XConnectorEpoll(socketConfig, XClientConnectionHandler(xServer), XClientRequestHandler()).apply {
            initialInputBufferCapacity = 262144
            isCanReceiveAncillaryMessages = true
            start()
        }
    }

    override fun stop() {
        connector?.stop()
        connector = null
    }
}
