package jkad.controller.io;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.util.Observable;

public class JKadDatagramSocket extends Observable {

    private DatagramSocket socket;

    public enum Action {

        CLOSE_SOCKET
    }

    public JKadDatagramSocket() throws SocketException {
        socket = new DatagramSocket();
    }

    public JKadDatagramSocket(int port, InetAddress laddr) throws SocketException {
        socket = new DatagramSocket(port, laddr);
    }

    public JKadDatagramSocket(int port) throws SocketException {
        socket = new DatagramSocket(port);
    }

    public JKadDatagramSocket(SocketAddress bindaddr) throws SocketException {
        socket = new DatagramSocket(bindaddr);
    }

    public void bind(SocketAddress addr) throws SocketException {
        socket.bind(addr);
    }

    public void close() {
        socket.close();
    }

    public void connect(InetAddress address, int port) {
        socket.connect(address, port);
    }

    public void connect(SocketAddress addr) throws SocketException {
        socket.connect(addr);
    }

    public void disconnect() {
        socket.disconnect();
    }

    public boolean getBroadcast() throws SocketException {
        return socket.getBroadcast();
    }

    public DatagramChannel getChannel() {
        return socket.getChannel();
    }

    public InetAddress getInetAddress() {
        return socket.getInetAddress();
    }

    public InetAddress getLocalAddress() {
        return socket.getLocalAddress();
    }

    public int getLocalPort() {
        return socket.getLocalPort();
    }

    public SocketAddress getLocalSocketAddress() {
        return socket.getLocalSocketAddress();
    }

    public int getPort() {
        return socket.getPort();
    }

    public int getReceiveBufferSize() throws SocketException {
        return socket.getReceiveBufferSize();
    }

    public SocketAddress getRemoteSocketAddress() {
        return socket.getRemoteSocketAddress();
    }

    public boolean getReuseAddress() throws SocketException {
        return socket.getReuseAddress();
    }

    public int getSendBufferSize() throws SocketException {
        return socket.getSendBufferSize();
    }

    public int getSoTimeout() throws SocketException {
        return socket.getSoTimeout();
    }

    public int getTrafficClass() throws SocketException {
        return socket.getTrafficClass();
    }

    public boolean isBound() {
        return socket.isBound();
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    public boolean isConnected() {
        return socket.isConnected();
    }

    public void receive(DatagramPacket p) throws IOException {
        socket.receive(p);
    }

    public void send(DatagramPacket p) throws IOException {
        socket.send(p);
    }

    public void setBroadcast(boolean on) throws SocketException {
        socket.setBroadcast(on);
    }

    public void setReceiveBufferSize(int size) throws SocketException {
        socket.setReceiveBufferSize(size);
    }

    public void setReuseAddress(boolean on) throws SocketException {
        socket.setReuseAddress(on);
    }

    public void setSendBufferSize(int size) throws SocketException {
        socket.setSendBufferSize(size);
    }

    public void setSoTimeout(int timeout) throws SocketException {
        socket.setSoTimeout(timeout);
    }

    public void setTrafficClass(int tc) throws SocketException {
        socket.setTrafficClass(tc);
    }
}
