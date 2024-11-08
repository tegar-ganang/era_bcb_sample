package net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers;

import net.kano.joscar.rvcmd.RvConnectionInfo;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.EventPost;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.events.WaitingForConnectionEvent;
import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.SocketStreamInfo;
import javax.net.ServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.channels.ServerSocketChannel;
import java.util.Arrays;

public abstract class PassiveConnectionController extends AbstractConnectionController {

    {
        setConnector(new MyConnector());
    }

    public PassiveConnector getConnector() {
        return (PassiveConnector) super.getConnector();
    }

    protected void initializeBeforeStarting() throws IOException {
    }

    protected abstract void sendRequest() throws IOException;

    protected void handleResolvingState() {
    }

    protected void handleConnectingState() {
        EventPost post = getRvConnection().getEventPost();
        RvConnectionInfo connInfo = getRvSessionInfo().getConnectionInfo();
        post.fireEvent(new WaitingForConnectionEvent(connInfo.getInternalIP(), connInfo.getPort()));
    }

    protected void prepareStream() throws IOException {
        super.prepareStream();
        sendRequest();
    }

    private class MyConnector implements PassiveConnector {

        private ServerSocket serverSocket;

        private int localPort = -1;

        public int getLocalPort() {
            return localPort;
        }

        public InetAddress getLocalHost() {
            InetAddress addr = serverSocket.getInetAddress();
            if (Arrays.equals(addr.getAddress(), new byte[] { 0, 0, 0, 0 })) {
                try {
                    return InetAddress.getLocalHost();
                } catch (UnknownHostException ignored) {
                }
            }
            return addr;
        }

        public SocketStreamInfo createStream() throws IOException {
            handleConnectingState();
            return new SocketStreamInfo(serverSocket.accept().getChannel());
        }

        public void prepareStream() throws IOException {
            ServerSocketFactory ssf = getRvConnection().getSettings().getProxyInfo().getServerSocketFactory();
            if (ssf == null) {
                serverSocket = ServerSocketChannel.open().socket();
            } else {
                serverSocket = ssf.createServerSocket();
            }
            serverSocket.bind(null);
            localPort = serverSocket.getLocalPort();
        }

        public void checkConnectionInfo() throws Exception {
        }
    }
}
