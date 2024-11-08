package net.pesahov.remote.socket.direct;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import net.pesahov.remote.socket.RemoteChannel;
import net.pesahov.remote.socket.RemoteServerSocketChannel;
import net.pesahov.remote.socket.RemoteSocketChannel;
import net.pesahov.remote.socket.UnderlyingSocketProxy;
import net.pesahov.remote.socket.wrapper.RemoteServerSocketChannelWrapper;
import net.pesahov.remote.socket.wrapper.RemoteSocketChannelWrapper;

/**
 * @author Pesahov Dmitry
 * @since 2.0
 */
public class DirectUnderlyingSocketWrapper implements UnderlyingSocketProxy {

    /**
     * Wrapped Socket instance.
     */
    private Socket socket;

    /**
     * Wrapped ServerSocket instance.
     */
    private ServerSocket serverSocket;

    /**
     * Creates DirectUnderlyingSocketWrapper for Socket instance.
     * @param socket Socket instance to wrap.
     */
    public DirectUnderlyingSocketWrapper(Socket socket) {
        if (socket == null) throw new IllegalArgumentException("Socket instance is null!");
        this.socket = socket;
    }

    /**
     * Creates DirectUnderlyingSocketWrapper for ServerSocket instance.
     * @param serverSocket Socket instance to wrap.
     */
    public DirectUnderlyingSocketWrapper(ServerSocket serverSocket) {
        if (serverSocket == null) throw new IllegalArgumentException("ServerSocket instance is null!");
        this.serverSocket = serverSocket;
    }

    public void close() throws IOException {
        if (socket != null) socket.close(); else serverSocket.close();
    }

    public RemoteChannel getChannel() {
        if (socket != null) {
            SocketChannel socketChannel = socket.getChannel();
            if (socketChannel instanceof RemoteSocketChannel) return (RemoteSocketChannel) socketChannel;
            return new RemoteSocketChannelWrapper(socketChannel);
        } else {
            ServerSocketChannel serverSocketChannel = serverSocket.getChannel();
            if (serverSocketChannel instanceof RemoteServerSocketChannel) return (RemoteServerSocketChannel) serverSocketChannel;
            return new RemoteServerSocketChannelWrapper(serverSocketChannel);
        }
    }

    public InetAddress getInetAddress() {
        if (socket != null) return socket.getInetAddress(); else return serverSocket.getInetAddress();
    }

    public int getLocalPort() {
        if (socket != null) return socket.getLocalPort(); else return serverSocket.getLocalPort();
    }

    public SocketAddress getLocalSocketAddress() {
        if (socket != null) return socket.getLocalSocketAddress(); else return serverSocket.getLocalSocketAddress();
    }

    public int getReceiveBufferSize() throws SocketException {
        if (socket != null) return socket.getReceiveBufferSize(); else return serverSocket.getReceiveBufferSize();
    }

    public boolean getReuseAddress() throws SocketException {
        if (socket != null) return socket.getReuseAddress(); else return serverSocket.getReuseAddress();
    }

    public boolean isBound() {
        if (socket != null) return socket.isBound(); else return serverSocket.isBound();
    }

    public boolean isClosed() {
        if (socket != null) return socket.isClosed(); else return serverSocket.isClosed();
    }

    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        if (socket != null) socket.setPerformancePreferences(connectionTime, latency, bandwidth); else serverSocket.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    public void setReceiveBufferSize(int size) throws SocketException {
        if (socket != null) socket.setReceiveBufferSize(size); else serverSocket.setReceiveBufferSize(size);
    }

    public void setReuseAddress(boolean on) throws SocketException {
        if (socket != null) socket.setReuseAddress(on); else serverSocket.setReuseAddress(on);
    }

    public void setSoTimeout(int timeout) throws SocketException {
        if (socket != null) socket.setSoTimeout(timeout); else serverSocket.setSoTimeout(timeout);
    }

    @Override
    public String toString() {
        if (socket != null) return socket.toString(); else return serverSocket.toString();
    }
}
