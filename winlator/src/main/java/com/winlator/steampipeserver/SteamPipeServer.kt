package com.winlator.steampipeserver

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import timber.log.Timber

class SteamPipeServer {

    companion object {
        private const val PORT = 34865
    }

    private var serverSocket: ServerSocket? = null

    private var running = false

    @Throws(IOException::class)
    private fun readNetworkInt(input: DataInputStream): Int {
        return Integer.reverseBytes(input.readInt())
    }

    @Throws(IOException::class)
    private fun writeNetworkInt(output: DataOutputStream, value: Int) {
        output.writeInt(Integer.reverseBytes(value))
    }

    fun start() {
        running = true
        Thread(
            Runnable {
                try {
                    serverSocket = ServerSocket()
                    serverSocket!!.setReuseAddress(true)
                    serverSocket!!.bind(InetSocketAddress(PORT))
                    Timber.d("Server started on port %s", PORT)

                    while (running) {
                        val clientSocket: Socket = serverSocket!!.accept()
                        // clientSocket.setTcpNoDelay(true);
                        // clientSocket.setSoTimeout(5000);  // 5 second timeout
                        handleClient(clientSocket)
                    }
                } catch (e: IOException) {
                    Timber.e(e, "Server error")
                }
            },
        ).start()
    }

    private fun handleClient(clientSocket: Socket) {
        Thread(
            Runnable {
                try {
                    val input = DataInputStream(BufferedInputStream(clientSocket.getInputStream()))
                    val output = DataOutputStream(BufferedOutputStream(clientSocket.getOutputStream()))

                    while (running && !clientSocket.isClosed) {
                        if (input.available() > 0) {
                            val messageType = readNetworkInt(input)

                            // Log.d("SteamPipeServer", "Received message: " + messageType);
                            when (messageType) {
                                RequestCodes.MSG_INIT -> {
                                    Timber.d("Received MSG_INIT")
                                    writeNetworkInt(output, 1)
                                    output.flush()
                                }

                                RequestCodes.MSG_SHUTDOWN -> {
                                    Timber.d("Received MSG_SHUTDOWN")
                                    clientSocket.close()
                                }

                                RequestCodes.MSG_RESTART_APP -> {
                                    Timber.d("Received MSG_RESTART_APP")
                                    // val appId = input.readInt()
                                    writeNetworkInt(output, 0) // Send restart not needed
                                }

                                RequestCodes.MSG_IS_RUNNING -> {
                                    Timber.d("Received MSG_IS_RUNNING")
                                    writeNetworkInt(output, 1) // Send Steam running status
                                }

                                RequestCodes.MSG_REGISTER_CALLBACK -> Timber.d("Received MSG_REGISTER_CALLBACK")

                                RequestCodes.MSG_UNREGISTER_CALLBACK -> Timber.d("Received MSG_UNREGISTER_CALLBACK")

                                RequestCodes.MSG_RUN_CALLBACKS -> Timber.d("Received MSG_RUN_CALLBACKS")

                                else -> Timber.w("Unknown message type: %s", messageType)
                            }
                        }
                    }
                } catch (e: IOException) {
                    Timber.e(e, "Client handler error")
                }
            },
        ).start()
    }

    fun stop() {
        running = false

        try {
            serverSocket?.close()
        } catch (e: IOException) {
            Timber.w(e)
        }
    }
}
