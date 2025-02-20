package com.winlator.xenvironment.components

import com.winlator.sysvshm.SysVSHMConnectionHandler
import com.winlator.sysvshm.SysVSHMRequestHandler
import com.winlator.sysvshm.SysVSharedMemory
import com.winlator.xconnector.UnixSocketConfig
import com.winlator.xconnector.XConnectorEpoll
import com.winlator.xenvironment.EnvironmentComponent
import com.winlator.xserver.SHMSegmentManager
import com.winlator.xserver.XServer

class SysVSharedMemoryComponent(
    private val xServer: XServer,
    val socketConfig: UnixSocketConfig,
) : EnvironmentComponent() {

    private var connector: XConnectorEpoll? = null

    private var sysVSharedMemory: SysVSharedMemory? = null

    override fun start() {
        if (connector != null) {
            return
        }

        sysVSharedMemory = SysVSharedMemory()
        connector = XConnectorEpoll(socketConfig, SysVSHMConnectionHandler(sysVSharedMemory!!), SysVSHMRequestHandler()).apply {
            start()
        }

        xServer.shmSegmentManager = SHMSegmentManager(sysVSharedMemory!!)
    }

    override fun stop() {
        connector?.stop()
        connector = null

        sysVSharedMemory?.deleteAll()
    }
}
