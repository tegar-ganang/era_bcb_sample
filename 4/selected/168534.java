package org.net.stbp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import org.engine.GameEngine;
import org.net.NetworkClient;
import org.net.NetworkServer;

public class STBPNetworkClient implements NetworkClient {

    protected STBPGameEngineProxy stbpproxy;

    protected Socket sock;

    protected String host;

    protected int port;

    private boolean isConnected = false;

    public void closeConnection() throws IOException, IllegalStateException {
        stbpproxy.terminateGame();
        System.exit(0);
    }

    public void connect(String chost, int cport) throws IOException, IllegalStateException {
        ProtocolReader reader;
        ProtocolWriter writer;
        if (isConnected) {
            throw new IllegalStateException();
        }
        if (cport == NetworkServer.DEFAULT_PORT || cport == NetworkServer.ANY_PORT) {
            cport = STBPNetworkServer.STBP_DEFAULT_PORT;
        }
        this.host = chost;
        this.port = cport;
        this.sock = new Socket(chost, cport);
        reader = new ProtocolReader(new BufferedReader(new InputStreamReader(sock.getInputStream(), "UTF-8")));
        writer = new ProtocolWriter(sock.getOutputStream());
        this.stbpproxy = initConnection(reader, writer);
        isConnected = true;
    }

    protected STBPGameEngineProxy initConnection(ProtocolReader reader, ProtocolWriter writer) {
        return new STBPGameEngineProxy(reader, writer);
    }

    public GameEngine getProxyEngine() throws IOException, IllegalStateException {
        if (!isConnected) {
            throw new IllegalStateException();
        }
        return stbpproxy;
    }

    public boolean isConnected() {
        return isConnected;
    }
}
