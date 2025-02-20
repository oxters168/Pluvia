package com.winlator.xenvironment.components

import com.winlator.steampipeserver.SteamPipeServer
import com.winlator.xconnector.Client
import com.winlator.xconnector.ConnectionHandler
import com.winlator.xconnector.RequestHandler
import com.winlator.xenvironment.EnvironmentComponent
import timber.log.Timber

class SteamClientComponent : EnvironmentComponent(), ConnectionHandler, RequestHandler {

    // public abstract static class RequestCodes {
    //     public static final byte INIT = 0;
    //     public static final byte GET_TICKET = 1;
    //     public static final byte AUTH_SESSION = 2;
    // }

    private var connector: SteamPipeServer? = null

    // private XConnectorEpoll connector;

    // private final UnixSocketConfig socketConfig;

    // public SteamClientComponent(UnixSocketConfig socketConfig) {
    //     this.socketConfig = socketConfig;
    // }

    override fun start() {
        Timber.d("Starting...")
        if (connector != null) {
            return
        }

        connector = SteamPipeServer() // new XConnectorEpoll(socketConfig, this, this);

        // set up the socket file to be accessible by processes executed within wine
        // File socketFile = new File(socketConfig.path);

        // while (socketFile != null && socketFile.exists() && socketFile.getAbsolutePath().contains(ImageFs.WINEPREFIX)) {
        //     FileUtils.chmod(socketFile, 0771);
        //     socketFile = socketFile.getParentFile();
        // }

        // connector.setMultithreadedClients(true);
        connector!!.start()
    }

    override fun stop() {
        Timber.d("Stopping...")
        if (connector != null) {
            connector!!.stop()
            connector = null
        }
    }

    override fun handleNewConnection(client: Client?) {
        Timber.d("New connection")
        client?.createIOStreams()
        // client.setTag(new ALSAClient());
    }

    override fun handleConnectionShutdown(client: Client?) {
        Timber.d("Connection shutdown")
        // ((ALSAClient)client.getTag()).release();
    }

    override fun handleRequest(client: Client?): Boolean {
        // XInputStream input = client.getInputStream();
        // if (input == null) return false;
        //
        // int cmdType = input.readInt();
        // Log.d("SteamClientComponent", "Received " + cmdType);
        //
        // switch (cmdType) {
        //     case RequestCodes.INIT:
        //         Log.d("SteamClientComponent", "Received INIT");
        //         break;
        //     case RequestCodes.GET_TICKET:
        //         Log.d("SteamClientComponent", "Received GET_TICKET");
        //         break;
        //     case RequestCodes.AUTH_SESSION:
        //         Log.d("SteamClientComponent", "Received AUTH_SESSION");
        //         break;
        // }

        return true
    }
}
