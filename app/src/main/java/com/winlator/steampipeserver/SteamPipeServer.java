package com.winlator.steampipeserver;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class SteamPipeServer {
    private static final int PORT = 34865;
    private ServerSocket serverSocket;
    private boolean running;

    private int readNetworkInt(DataInputStream input) throws IOException {
        return Integer.reverseBytes(input.readInt());
    }

    private void writeNetworkInt(DataOutputStream output, int value) throws IOException {
        output.writeInt(Integer.reverseBytes(value));
    }

    public void start() {
        running = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket();
                serverSocket.setReuseAddress(true);
                serverSocket.bind(new InetSocketAddress(PORT));
                Log.d("SteamPipeServer", "Server started on port " + PORT);

                while (running) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        handleClient(clientSocket);
                    } catch (IOException e) {
                        if (running) {
                            Log.e("SteamPipeServer", "Error accepting client connection", e);
                        } else {
                            Log.d("SteamPipeServer", "Server socket closed during shutdown");
                        }
                        break;
                    }
                }
            } catch (IOException e) {
                Log.e("SteamPipeServer", "Server error", e);
            } finally {
                Log.d("SteamPipeServer", "Server thread exiting");
            }
        }).start();
    }

    private void handleClient(Socket clientSocket) {
        new Thread(() -> {
            try {
                DataInputStream input = new DataInputStream(
                        new BufferedInputStream(clientSocket.getInputStream()));
                DataOutputStream output = new DataOutputStream(
                        new BufferedOutputStream(clientSocket.getOutputStream()));

                while (running && !clientSocket.isClosed()) {
                    try {
                        if (input.available() > 0) {
                            int messageType = readNetworkInt(input);

                            switch (messageType) {
                                case RequestCodes.MSG_INIT:
                                    Log.d("SteamPipeServer", "Received MSG_INIT");
                                    writeNetworkInt(output, 1);
                                    output.flush();
                                    break;
                                case RequestCodes.MSG_SHUTDOWN:
                                    Log.d("SteamPipeServer", "Received MSG_SHUTDOWN");
                                    clientSocket.close();
                                    break;
                                case RequestCodes.MSG_RESTART_APP:
                                    Log.d("SteamPipeServer", "Received MSG_RESTART_APP");
                                    int appId = input.readInt();
                                    writeNetworkInt(output, 0); // Send restart not needed
                                    break;
                                case RequestCodes.MSG_IS_RUNNING:
                                    Log.d("SteamPipeServer", "Received MSG_IS_RUNNING");
                                    writeNetworkInt(output, 1); // Send Steam running status
                                    break;
                                case RequestCodes.MSG_REGISTER_CALLBACK:
                                    Log.d("SteamPipeServer", "Received MSG_REGISTER_CALLBACK");
                                    break;
                                case RequestCodes.MSG_UNREGISTER_CALLBACK:
                                    Log.d("SteamPipeServer", "Received MSG_UNREGISTER_CALLBACK");
                                    break;
                                case RequestCodes.MSG_RUN_CALLBACKS:
                                    Log.d("SteamPipeServer", "Received MSG_RUN_CALLBACKS");
                                    break;
                                default:
                                    Log.w("SteamPipeServer", "Unknown message type: " + messageType);
                                    break;
                            }
                        }

                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Log.d("SteamPipeServer", "Client thread interrupted");
                        break;
                    } catch (IOException e) {
                        if (running) {
                            Log.e("SteamPipeServer", "Error reading from client", e);
                        }
                        break;
                    }
                }
            } catch (IOException e) {
                Log.e("SteamPipeServer", "Client handler error", e);
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    Log.e("SteamPipeServer", "Error closing client socket", e);
                }
                Log.d("SteamPipeServer", "Client thread exiting");
            }
        }).start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            Log.e("SteamPipeServer",e.getLocalizedMessage());
        }
    }
}

