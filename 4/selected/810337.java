package org.scohen.juploadr.upload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

public class BandwidthThrottlingSocket extends Socket {

    private Socket wrapped;

    private int maxBytesPerSecond;

    public BandwidthThrottlingSocket(Socket wrapped, int maxBytesPerSecond) {
        this.wrapped = wrapped;
        this.maxBytesPerSecond = maxBytesPerSecond;
    }

    public OutputStream getOutputStream() throws IOException {
        return new BandwidthThrottlingOutputStream(wrapped.getOutputStream(), maxBytesPerSecond);
    }

    public void bind(SocketAddress bindpoint) throws IOException {
        wrapped.bind(bindpoint);
    }

    public void close() throws IOException {
        wrapped.close();
    }

    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        wrapped.connect(endpoint, timeout);
    }

    public void connect(SocketAddress endpoint) throws IOException {
        wrapped.connect(endpoint);
    }

    public boolean equals(Object obj) {
        return wrapped.equals(obj);
    }

    public SocketChannel getChannel() {
        return wrapped.getChannel();
    }

    public InetAddress getInetAddress() {
        return wrapped.getInetAddress();
    }

    public InputStream getInputStream() throws IOException {
        return wrapped.getInputStream();
    }

    public boolean getKeepAlive() throws SocketException {
        return wrapped.getKeepAlive();
    }

    public InetAddress getLocalAddress() {
        return wrapped.getLocalAddress();
    }

    public int getLocalPort() {
        return wrapped.getLocalPort();
    }

    public SocketAddress getLocalSocketAddress() {
        return wrapped.getLocalSocketAddress();
    }

    public boolean getOOBInline() throws SocketException {
        return wrapped.getOOBInline();
    }

    public int getPort() {
        return wrapped.getPort();
    }

    public int getReceiveBufferSize() throws SocketException {
        return wrapped.getReceiveBufferSize();
    }

    public SocketAddress getRemoteSocketAddress() {
        return wrapped.getRemoteSocketAddress();
    }

    public boolean getReuseAddress() throws SocketException {
        return wrapped.getReuseAddress();
    }

    public int getSendBufferSize() throws SocketException {
        return wrapped.getSendBufferSize();
    }

    public int getSoLinger() throws SocketException {
        return wrapped.getSoLinger();
    }

    public int getSoTimeout() throws SocketException {
        return wrapped.getSoTimeout();
    }

    public boolean getTcpNoDelay() throws SocketException {
        return wrapped.getTcpNoDelay();
    }

    public int getTrafficClass() throws SocketException {
        return wrapped.getTrafficClass();
    }

    public int hashCode() {
        return wrapped.hashCode();
    }

    public boolean isBound() {
        return wrapped.isBound();
    }

    public boolean isClosed() {
        return wrapped.isClosed();
    }

    public boolean isConnected() {
        return wrapped.isConnected();
    }

    public boolean isInputShutdown() {
        return wrapped.isInputShutdown();
    }

    public boolean isOutputShutdown() {
        return wrapped.isOutputShutdown();
    }

    public void sendUrgentData(int data) throws IOException {
        wrapped.sendUrgentData(data);
    }

    public void setKeepAlive(boolean on) throws SocketException {
        wrapped.setKeepAlive(on);
    }

    public void setOOBInline(boolean on) throws SocketException {
        wrapped.setOOBInline(on);
    }

    public void setReceiveBufferSize(int size) throws SocketException {
        wrapped.setReceiveBufferSize(size);
    }

    public void setReuseAddress(boolean on) throws SocketException {
        wrapped.setReuseAddress(on);
    }

    public void setSendBufferSize(int size) throws SocketException {
        wrapped.setSendBufferSize(size);
    }

    public void setSoLinger(boolean on, int linger) throws SocketException {
        wrapped.setSoLinger(on, linger);
    }

    public void setSoTimeout(int timeout) throws SocketException {
        wrapped.setSoTimeout(timeout);
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        wrapped.setTcpNoDelay(on);
    }

    public void setTrafficClass(int tc) throws SocketException {
        wrapped.setTrafficClass(tc);
    }

    public void shutdownInput() throws IOException {
        wrapped.shutdownInput();
    }

    public void shutdownOutput() throws IOException {
        wrapped.shutdownOutput();
    }

    public String toString() {
        return wrapped.toString();
    }
}
