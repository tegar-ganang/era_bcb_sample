package com.ohua.clustering.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import javax.net.SocketFactory;

public class OhuaSocketFactory extends SocketFactory {

    private static OhuaSocketFactory _instance = null;

    public static OhuaSocketFactory getInstance() {
        if (_instance == null) {
            _instance = new OhuaSocketFactory();
        }
        return _instance;
    }

    private OhuaSocketFactory() {
    }

    /**
   * {@inheritDoc}
   */
    @Override
    public Socket createSocket() throws IOException {
        SocketChannel newChannel = SocketChannel.open();
        return newChannel.socket();
    }

    /**
   * {@inheritDoc}
   */
    @Override
    public Socket createSocket(String arg0, int arg1) throws IOException, UnknownHostException {
        Socket newSocket = createSocket();
        InetSocketAddress inetSocketAddress = new InetSocketAddress(arg0, arg1);
        System.out.println("trying to connect to server " + inetSocketAddress);
        newSocket.connect(inetSocketAddress);
        newSocket.getChannel().configureBlocking(false);
        return newSocket;
    }

    /**
   * {@inheritDoc}
   */
    @Override
    public Socket createSocket(InetAddress arg0, int arg1) throws IOException {
        Socket newSocket = createSocket();
        newSocket.connect(new InetSocketAddress(arg0, arg1));
        return newSocket;
    }

    /**
   * {@inheritDoc}
   */
    @Override
    public Socket createSocket(String arg0, int arg1, InetAddress arg2, int arg3) throws IOException, UnknownHostException {
        Socket newSocket = createSocket();
        newSocket.bind(new InetSocketAddress(arg2, arg3));
        InetSocketAddress inetSocketAddress = new InetSocketAddress(arg0, arg1);
        System.out.println("trying to connect to server " + inetSocketAddress);
        newSocket.connect(inetSocketAddress);
        return newSocket;
    }

    /**
   * {@inheritDoc}
   */
    @Override
    public Socket createSocket(InetAddress arg0, int arg1, InetAddress arg2, int arg3) throws IOException {
        Socket newSocket = createSocket();
        newSocket.bind(new InetSocketAddress(arg2, arg3));
        newSocket.connect(new InetSocketAddress(arg0, arg1));
        return newSocket;
    }
}
