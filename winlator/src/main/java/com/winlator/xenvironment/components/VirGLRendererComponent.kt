package com.winlator.xenvironment.components

import androidx.annotation.Keep
import com.winlator.xconnector.Client
import com.winlator.xconnector.ConnectionHandler
import com.winlator.xconnector.RequestHandler
import com.winlator.xconnector.UnixSocketConfig
import com.winlator.xconnector.XConnectorEpoll
import com.winlator.xenvironment.EnvironmentComponent
import com.winlator.xserver.XServer
import java.io.IOException

class VirGLRendererComponent(
    private val xServer: XServer,
    private val socketConfig: UnixSocketConfig,
) : EnvironmentComponent(),
    ConnectionHandler,
    RequestHandler {

    companion object {
        init {
            System.loadLibrary("virglrenderer")
        }
    }

    private var connector: XConnectorEpoll? = null

    private var sharedEGLContextPtr: Long = 0

    override fun start() {
        if (connector != null) {
            return
        }

        connector = XConnectorEpoll(socketConfig, this, this).apply {
            start()
        }
    }

    override fun stop() {
        connector?.stop()
        connector = null
    }

    @Keep
    private fun killConnection(fd: Int) {
        connector?.killConnection(connector!!.getClient(fd))
    }

    @Keep
    private fun getSharedEGLContext(): Long {
        if (sharedEGLContextPtr != 0L) return sharedEGLContextPtr
        val thread = Thread.currentThread()
        try {
            val renderer = xServer.renderer

            renderer.xServerView.queueEvent {
                sharedEGLContextPtr = getCurrentEGLContextPtr()

                synchronized(thread) {
                    (thread as Object).notify()
                }
            }

            synchronized(thread) {
                (thread as Object).wait()
            }
        } catch (e: Exception) {
            return 0
        }

        return sharedEGLContextPtr
    }

    override fun handleConnectionShutdown(client: Client) {
        val clientPtr = client.tag as Long

        destroyClient(clientPtr)
    }

    override fun handleNewConnection(client: Client) {
        getSharedEGLContext()

        val clientPtr = handleNewConnection(client.clientSocket.fd)

        client.tag = clientPtr
    }

    @Throws(IOException::class)
    override fun handleRequest(client: Client): Boolean {
        val clientPtr = client.tag as Long

        handleRequest(clientPtr)

        return true
    }

    @Keep
    private fun flushFrontbuffer(drawableId: Int, framebuffer: Int) {
        val drawable = xServer.drawableManager.getDrawable(drawableId) ?: return

        synchronized(drawable.renderLock) {
            drawable.data = null

            drawable.texture.copyFromFramebuffer(framebuffer, drawable.width, drawable.height)
        }

        drawable.onDrawListener?.run()
    }

    /**
     * Native Methods
     */

    private external fun handleNewConnection(fd: Int): Long

    private external fun handleRequest(clientPtr: Long)

    private external fun getCurrentEGLContextPtr(): Long

    private external fun destroyClient(clientPtr: Long)

    private external fun destroyRenderer(clientPtr: Long)
}
