package net.pesahov.remote.socket.wrapper;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import net.pesahov.remote.socket.RemoteServerSocket;
import net.pesahov.remote.socket.RemoteServerSocketChannel;
import net.pesahov.remote.socket.RemoteSocket;

/**
 * @author Pesahov Dmitry
 * @since 2.0
 */
public class RemoteServerSocketWrapper extends RemoteServerSocket implements Serializable {

    /**
     * Use serialVersionUID from JDK 1.0.2 for interoperability
     */
    static final long serialVersionUID = 5555603636296694605L;

    /**
     * Wrapped ServerSocket instance.
     */
    private ServerSocket serverSocket;

    /**
     * Creates new wrapper instance by given parameter.
     * @param serverSocket ServerSocket instance to wrap.
     * @throws IOException IO error when opening the socket.
     */
    public RemoteServerSocketWrapper(ServerSocket serverSocket) throws IOException {
        if (serverSocket == null) throw new IllegalArgumentException("ServerSocket instance is null!");
        this.serverSocket = serverSocket;
    }

    @Override
    public RemoteSocket accept() throws IOException {
        Socket socket = serverSocket.accept();
        if (socket instanceof RemoteSocket) return (RemoteSocket) socket;
        return new RemoteSocketWrapper(socket);
    }

    @Override
    public void bind(SocketAddress endpoint, int backlog) throws IOException {
        serverSocket.bind(endpoint, backlog);
    }

    @Override
    public void bind(SocketAddress endpoint) throws IOException {
        serverSocket.bind(endpoint);
    }

    @Override
    public void close() throws IOException {
        serverSocket.close();
    }

    @Override
    public RemoteServerSocketChannel getChannel() {
        ServerSocketChannel serverSocketChannel = serverSocket.getChannel();
        if (serverSocketChannel instanceof RemoteServerSocketChannel) return (RemoteServerSocketChannel) serverSocketChannel;
        return new RemoteServerSocketChannelWrapper(serverSocketChannel);
    }

    @Override
    public InetAddress getInetAddress() {
        return serverSocket.getInetAddress();
    }

    @Override
    public int getLocalPort() {
        return serverSocket.getLocalPort();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return serverSocket.getLocalSocketAddress();
    }

    @Override
    public synchronized int getReceiveBufferSize() throws SocketException {
        return serverSocket.getReceiveBufferSize();
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return serverSocket.getReuseAddress();
    }

    @Override
    public synchronized int getSoTimeout() throws IOException {
        return serverSocket.getSoTimeout();
    }

    @Override
    public boolean isBound() {
        return serverSocket.isBound();
    }

    @Override
    public boolean isClosed() {
        return serverSocket.isClosed();
    }

    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        serverSocket.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    @Override
    public synchronized void setReceiveBufferSize(int size) throws SocketException {
        serverSocket.setReceiveBufferSize(size);
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        serverSocket.setReuseAddress(on);
    }

    @Override
    public synchronized void setSoTimeout(int timeout) throws SocketException {
        serverSocket.setSoTimeout(timeout);
    }

    @Override
    public String toString() {
        return serverSocket.toString();
    }
}
