package edu.psu.its.lionshare.security;

import java.net.*;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.channels.SocketChannel;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;

public class SocketWrapper extends Socket {

    private String id;

    private Socket filling;

    public SocketWrapper(Socket filling, String id) {
        super();
        this.filling = filling;
        this.id = id;
    }

    public void connect(SocketAddress endpoint) throws IOException {
        filling.connect(endpoint);
    }

    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        filling.connect(endpoint, timeout);
    }

    public void bind(SocketAddress bindpoint) throws IOException {
        filling.bind(bindpoint);
    }

    public InetAddress getInetAddress() {
        return filling.getInetAddress();
    }

    public InetAddress getLocalAddress() {
        return filling.getLocalAddress();
    }

    public int getPort() {
        return filling.getPort();
    }

    public int getLocalPort() {
        return filling.getLocalPort();
    }

    public SocketAddress getRemoteSocketAddress() {
        return filling.getRemoteSocketAddress();
    }

    public SocketAddress getLocalSocketAddress() {
        return filling.getLocalSocketAddress();
    }

    public SocketChannel getChannel() {
        return filling.getChannel();
    }

    public InputStream getInputStream() throws IOException {
        return filling.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return filling.getOutputStream();
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        filling.setTcpNoDelay(on);
    }

    public boolean getTcpNoDelay() throws SocketException {
        return filling.getTcpNoDelay();
    }

    public void setSoLinger(boolean on, int linger) throws SocketException {
        filling.setSoLinger(on, linger);
    }

    public int getSoLinger() throws SocketException {
        return filling.getSoLinger();
    }

    public void sendUrgentData(int data) throws IOException {
        filling.sendUrgentData(data);
    }

    public void setOOBInline(boolean on) throws SocketException {
        filling.setOOBInline(on);
    }

    public boolean getOOBInline() throws SocketException {
        return filling.getOOBInline();
    }

    public synchronized void setSoTimeout(int timeout) throws SocketException {
        filling.setSoTimeout(timeout);
    }

    public synchronized int getSoTimeout() throws SocketException {
        return filling.getSoTimeout();
    }

    public synchronized void setSendBufferSize(int size) throws SocketException {
        filling.setSendBufferSize(size);
    }

    public synchronized int getSendBufferSize() throws SocketException {
        return filling.getSendBufferSize();
    }

    public synchronized void setReceiveBufferSize(int size) throws SocketException {
        filling.setReceiveBufferSize(size);
    }

    public synchronized int getReceiveBufferSize() throws SocketException {
        return filling.getReceiveBufferSize();
    }

    public void setKeepAlive(boolean on) throws SocketException {
        filling.setKeepAlive(on);
    }

    public boolean getKeepAlive() throws SocketException {
        return filling.getKeepAlive();
    }

    public void setTrafficClass(int tc) throws SocketException {
        filling.setTrafficClass(tc);
    }

    public int getTrafficClass() throws SocketException {
        return filling.getTrafficClass();
    }

    public void setReuseAddress(boolean on) throws SocketException {
        filling.setReuseAddress(on);
    }

    public boolean getReuseAddress() throws SocketException {
        return filling.getReuseAddress();
    }

    public synchronized void close() throws IOException {
        filling.close();
    }

    public void shutdownInput() throws IOException {
        filling.shutdownInput();
    }

    public void shutdownOutput() throws IOException {
        filling.shutdownOutput();
    }

    public String toString() {
        return this.id;
    }

    public boolean isConnected() {
        return filling.isConnected();
    }

    public boolean isBound() {
        return filling.isBound();
    }

    public boolean isClosed() {
        return filling.isClosed();
    }

    public boolean isInputShutdown() {
        return filling.isInputShutdown();
    }

    public boolean isOutputShutdown() {
        return filling.isOutputShutdown();
    }

    public static synchronized void setSocketImplFactory(SocketImplFactory fac) throws IOException {
        Socket.setSocketImplFactory(fac);
    }

    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
    }
}
