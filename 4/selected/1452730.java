package ca.etsmtl.latis.network;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import org.apache.log4j.Logger;

public class MyServerSocket extends java.net.ServerSocket {

    private static final Logger logger = Logger.getLogger(MyServerSocket.class);

    public MyServerSocket() throws IOException {
        super();
        logger.debug("Entering MyServerSocket().");
        logger.debug("Entering MyServerSocket().");
    }

    public MyServerSocket(int port) throws IOException {
        super(port);
        if (logger.isDebugEnabled()) {
            logger.debug("Entering MyServerSocket(" + port + ")");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting MyServerSocket(" + port + ")");
        }
    }

    public MyServerSocket(int port, int backlog) throws IOException {
        super(port, backlog);
        if (logger.isDebugEnabled()) {
            logger.debug("Entering MyServerSocket(" + port + ", " + backlog + ")");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting MyServerSocket(" + port + ", " + backlog + ")");
        }
    }

    public MyServerSocket(int port, int backlog, InetAddress bindAddr) throws IOException {
        super(port, backlog, bindAddr);
        if (logger.isDebugEnabled()) {
            logger.debug("Entering MyServerSocket(" + port + ", " + backlog + ", " + bindAddr + ")");
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting MyServerSocket(" + port + ", " + backlog + ", " + bindAddr + ")");
        }
    }

    public synchronized int getReceiveBufferSize() throws SocketException {
        logger.debug("Entering getReceiveBufferSize().");
        int receiveBufferSize = super.getReceiveBufferSize();
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting getReceiveBufferSize(); RV = [" + receiveBufferSize + "]");
        }
        return receiveBufferSize;
    }

    public boolean getReuseAddress() throws SocketException {
        logger.debug("Entering getReusedAddress().");
        boolean reusedAddress = super.getReuseAddress();
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting getReusedAddress(); RV = [" + reusedAddress + "].");
        }
        return reusedAddress;
    }

    public synchronized int getSoTimeout() throws IOException {
        logger.debug("Entering getSoTimeout().");
        int soTimeout = super.getSoTimeout();
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting getSoTimeout(); RV = [" + soTimeout + "].");
        }
        return soTimeout;
    }

    public boolean isBound() {
        logger.debug("Entering isBound().");
        boolean isBound = super.isBound();
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting isBound(); RV = [" + isBound + "].");
        }
        return isBound;
    }

    public boolean isClosed() {
        logger.debug("Entering isClosed().");
        boolean isClosed = super.isClosed();
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting isClosed(); RV = [" + isClosed + "].");
        }
        return isClosed;
    }

    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        if (logger.isDebugEnabled()) {
            logger.debug("Entering setPerformancePreferences(" + connectionTime + ", " + latency + ", " + bandwidth + ").");
        }
        super.setPerformancePreferences(connectionTime, latency, bandwidth);
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting setPerformancePreferences(" + connectionTime + ", " + latency + ", " + bandwidth + ").");
        }
    }

    public synchronized void setReceiveBufferSize(int size) throws SocketException {
        if (logger.isDebugEnabled()) {
            logger.debug("Entering setReceivedBufferSize(" + size + ").");
        }
        super.setReceiveBufferSize(size);
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting setReceivedBufferSize(" + size + ").");
        }
    }

    public void setReuseAddress(boolean on) throws SocketException {
        if (logger.isDebugEnabled()) {
            logger.debug("Entering setReusedAddress(" + on + ").");
        }
        super.setReuseAddress(on);
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting setReusedAddress(" + on + ").");
        }
    }

    public synchronized void setSoTimeout(int timeout) throws SocketException {
        if (logger.isDebugEnabled()) {
            logger.debug("Entering setSoTimeout(" + timeout + ").");
        }
        super.setSoTimeout(timeout);
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting setSoTimeout(" + timeout + ").");
        }
    }

    public String toString() {
        logger.debug("Entering toString().");
        String toString = super.toString();
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting toString(); RV = [\"" + toString + "\"].");
        }
        return toString;
    }

    public Socket accept() throws IOException {
        logger.debug("Entering accept().");
        Socket socket = super.accept();
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting accept(); RV = [" + socket + "].");
        }
        return socket;
    }

    public void bind(SocketAddress endPoint) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Entering bind(" + endPoint + ").");
        }
        super.bind(endPoint);
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting bind(" + endPoint + ").");
        }
    }

    public void bind(SocketAddress endPoint, int backlog) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Entering bind(" + endPoint + ", " + backlog + ").");
        }
        super.bind(endPoint, backlog);
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting bind(" + endPoint + ", " + backlog + ").");
        }
    }

    public void close() throws IOException {
        logger.debug("Entering close().");
        super.close();
        logger.debug("Exiting close().");
    }

    public ServerSocketChannel getChannel() {
        logger.debug("Entering getChannel().");
        ServerSocketChannel serverSocketChannel = super.getChannel();
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting getChannel(); RV = [" + serverSocketChannel + "].");
        }
        return serverSocketChannel;
    }

    public InetAddress getInetAddress() {
        logger.debug("Entering getInetAddress().");
        InetAddress inetAddress = super.getInetAddress();
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting getInetAddress(); RV = [" + inetAddress + "].");
        }
        return inetAddress;
    }

    public int getLocalPort() {
        logger.debug("Entering getLocalPort().");
        int localPort = super.getLocalPort();
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting getLocalPort(); RV = [" + localPort + "].");
        }
        return localPort;
    }

    public SocketAddress getLocalSocketAddress() {
        logger.debug("Entering getLocalSocketAddress().");
        SocketAddress socketAddress = super.getLocalSocketAddress();
        if (logger.isDebugEnabled()) {
            logger.debug("Exiting getLocalSocketAddress(); RV = [" + socketAddress + "].");
        }
        return socketAddress;
    }
}
