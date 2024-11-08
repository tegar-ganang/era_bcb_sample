package net.kano.joustsim.oscar.oscar.service.icbm.ft.controllers;

import net.kano.joustsim.oscar.oscar.service.icbm.ft.state.SocketStreamInfo;
import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public abstract class AbstractOutgoingConnectionController extends AbstractConnectionController {

    protected abstract class DefaultOutgoingConnector implements OutgoingConnector {

        public SocketStreamInfo createStream() throws IOException {
            handleResolvingState();
            InetAddress ip = getIpAddress();
            if (ip == null) {
                throw new IllegalStateException("no IP address");
            }
            handleConnectingState();
            SocketFactory factory = getRvConnection().getSettings().getProxyInfo().getSocketFactory();
            int port = getConnectionPort();
            Socket socket;
            if (factory == null) {
                socket = SocketChannel.open(new InetSocketAddress(ip, port)).socket();
            } else {
                socket = factory.createSocket(ip, port);
            }
            return new SocketStreamInfo(socket.getChannel());
        }

        public void checkConnectionInfo() throws Exception {
        }

        public void prepareStream() throws IOException {
        }
    }
}
