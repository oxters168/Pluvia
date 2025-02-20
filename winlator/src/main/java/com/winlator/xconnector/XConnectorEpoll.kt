package com.winlator.xconnector

import android.util.SparseArray
import androidx.annotation.Keep
import androidx.core.util.isNotEmpty
import androidx.core.util.size
import java.io.IOException
import java.nio.ByteBuffer
import timber.log.Timber

class XConnectorEpoll(
    socketConfig: UnixSocketConfig,
    private val connectionHandler: ConnectionHandler,
    private val requestHandler: RequestHandler,
) : Runnable {

    companion object {
        init {
            System.loadLibrary("winlator")
        }

        external fun closeFd(fd: Int)
    }

    private val connectedClients = SparseArray<Client>()

    private val epollFd: Int

    private val serverFd: Int

    private val shutdownFd: Int

    private var epollThread: Thread?

    private var running = false

    var initialInputBufferCapacity: Int = 4096

    var initialOutputBufferCapacity: Int = 4096

    var isCanReceiveAncillaryMessages: Boolean = false

    var isMultithreadedClients: Boolean = false

    init {
        serverFd = createAFUnixSocket(socketConfig.path)
        if (serverFd < 0) {
            throw RuntimeException("Failed to create an AF_UNIX socket.")
        }

        epollFd = createEpollFd()
        if (epollFd < 0) {
            closeFd(serverFd)
            throw RuntimeException("Failed to create epoll fd.")
        }

        if (!addFdToEpoll(epollFd, serverFd)) {
            closeFd(serverFd)
            closeFd(epollFd)
            throw RuntimeException("Failed to add server fd to epoll.")
        }

        shutdownFd = createEventFd()
        if (!addFdToEpoll(epollFd, shutdownFd)) {
            closeFd(serverFd)
            closeFd(shutdownFd)
            closeFd(epollFd)
            throw RuntimeException("Failed to add shutdown fd to epoll.")
        }

        epollThread = Thread(this)
    }

    @Synchronized
    fun start() {
        if (running || epollThread == null) {
            return
        }

        running = true
        epollThread!!.start()
    }

    @Synchronized
    fun stop() {
        if (!running || epollThread == null) {
            return
        }

        running = false
        requestShutdown()

        while (epollThread!!.isAlive) {
            try {
                epollThread!!.join()
            } catch (_: InterruptedException) {
            }
        }

        epollThread = null
    }

    override fun run() {
        while (running && doEpollIndefinitely(epollFd, serverFd, !this.isMultithreadedClients)) {
            shutdown()
        }
    }

    @Keep
    private fun handleNewConnection(fd: Int) {
        val client = Client(this, ClientSocket(fd))
        client.connected = true
        if (this.isMultithreadedClients) {
            client.shutdownFd = createEventFd()
            client.pollThread = Thread(
                Runnable {
                    connectionHandler.handleNewConnection(client)
                    while (client.connected && waitForSocketRead(client.clientSocket!!.fd, client.shutdownFd));
                },
            )

            client.pollThread!!.start()
        } else {
            connectionHandler.handleNewConnection(client)
        }

        connectedClients.put(fd, client)
    }

    @Keep
    private fun handleExistingConnection(fd: Int) {
        val client = connectedClients.get(fd)

        if (client == null) {
            return
        }

        val inputStream = client.inputStream
        try {
            if (inputStream != null) {
                if (inputStream.readMoreData(this.isCanReceiveAncillaryMessages) > 0) {
                    var activePosition = 0

                    while (running && requestHandler.handleRequest(client)) {
                        activePosition = inputStream.activePosition
                    }

                    inputStream.activePosition = activePosition
                } else {
                    killConnection(client)
                }
            } else {
                requestHandler.handleRequest(client)
            }
        } catch (e: IOException) {
            Timber.w(e)
            killConnection(client)
        }
    }

    fun getClient(fd: Int): Client? = connectedClients.get(fd)

    fun killConnection(client: Client) {
        client.connected = false
        connectionHandler.handleConnectionShutdown(client)

        if (this.isMultithreadedClients) {
            if (Thread.currentThread() != client.pollThread) {
                client.requestShutdown()

                while (client.pollThread!!.isAlive) {
                    try {
                        client.pollThread!!.join()
                    } catch (_: InterruptedException) {
                    }
                }

                client.pollThread = null
            }

            closeFd(client.shutdownFd)
        } else {
            removeFdFromEpoll(epollFd, client.clientSocket!!.fd)
        }

        closeFd(client.clientSocket!!.fd)
        connectedClients.remove(client.clientSocket.fd)
    }

    private fun shutdown() {
        while (connectedClients.isNotEmpty()) {
            val client = connectedClients.valueAt(connectedClients.size - 1)
            killConnection(client)
        }

        removeFdFromEpoll(epollFd, serverFd)
        removeFdFromEpoll(epollFd, shutdownFd)

        closeFd(serverFd)
        closeFd(shutdownFd)
        closeFd(epollFd)
    }

    private fun requestShutdown() {
        try {
            val data = ByteBuffer.allocateDirect(8)
            data.asLongBuffer().put(1)
            (ClientSocket(shutdownFd)).write(data)
        } catch (e: IOException) {
            Timber.e(e)
        }
    }

    /**
     * Native Methods
     */

    private external fun createEpollFd(): Int

    private external fun createEventFd(): Int

    private external fun doEpollIndefinitely(epollFd: Int, serverFd: Int, addClientToEpoll: Boolean): Boolean

    private external fun addFdToEpoll(epollFd: Int, fd: Int): Boolean

    private external fun removeFdFromEpoll(epollFd: Int, fd: Int)

    private external fun waitForSocketRead(clientFd: Int, shutdownFd: Int): Boolean

    private external fun createAFUnixSocket(path: String?): Int
}
