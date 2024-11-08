package org.nightlabs.rmissl.socket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.ServerSocketChannel;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

/**
 * @author marco schulze - marco at nightlabs dot de
 */
public class SSLCompressionServerSocket extends SSLServerSocket {

    private SSLServerSocket delegateSocket;

    private boolean compressionEnabled;

    public SSLCompressionServerSocket(SSLServerSocket delegateSocket, boolean compressionEnabled) throws IOException {
        this.delegateSocket = delegateSocket;
        this.compressionEnabled = compressionEnabled;
    }

    @Override
    public Socket accept() throws IOException {
        return new SSLCompressionSocket((SSLSocket) delegateSocket.accept(), compressionEnabled);
    }

    @Override
    public void bind(SocketAddress endpoint, int backlog) throws IOException {
        delegateSocket.bind(endpoint, backlog);
    }

    @Override
    public void bind(SocketAddress endpoint) throws IOException {
        delegateSocket.bind(endpoint);
    }

    @Override
    public void close() throws IOException {
        delegateSocket.close();
    }

    @Override
    public boolean equals(Object obj) {
        return delegateSocket.equals(obj);
    }

    @Override
    public ServerSocketChannel getChannel() {
        return delegateSocket.getChannel();
    }

    @Override
    public String[] getEnabledCipherSuites() {
        return delegateSocket.getEnabledCipherSuites();
    }

    @Override
    public String[] getEnabledProtocols() {
        return delegateSocket.getEnabledProtocols();
    }

    @Override
    public boolean getEnableSessionCreation() {
        return delegateSocket.getEnableSessionCreation();
    }

    @Override
    public InetAddress getInetAddress() {
        return delegateSocket.getInetAddress();
    }

    @Override
    public int getLocalPort() {
        return delegateSocket.getLocalPort();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return delegateSocket.getLocalSocketAddress();
    }

    @Override
    public boolean getNeedClientAuth() {
        return delegateSocket.getNeedClientAuth();
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
        return delegateSocket.getReceiveBufferSize();
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return delegateSocket.getReuseAddress();
    }

    @Override
    public int getSoTimeout() throws IOException {
        return delegateSocket.getSoTimeout();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegateSocket.getSupportedCipherSuites();
    }

    @Override
    public String[] getSupportedProtocols() {
        return delegateSocket.getSupportedProtocols();
    }

    @Override
    public boolean getUseClientMode() {
        return delegateSocket.getUseClientMode();
    }

    @Override
    public boolean getWantClientAuth() {
        return delegateSocket.getWantClientAuth();
    }

    @Override
    public int hashCode() {
        return delegateSocket.hashCode();
    }

    @Override
    public boolean isBound() {
        return delegateSocket.isBound();
    }

    @Override
    public boolean isClosed() {
        return delegateSocket.isClosed();
    }

    @Override
    public void setEnabledCipherSuites(String[] suites) {
        delegateSocket.setEnabledCipherSuites(suites);
    }

    @Override
    public void setEnabledProtocols(String[] protocols) {
        delegateSocket.setEnabledProtocols(protocols);
    }

    @Override
    public void setEnableSessionCreation(boolean flag) {
        delegateSocket.setEnableSessionCreation(flag);
    }

    @Override
    public void setNeedClientAuth(boolean need) {
        delegateSocket.setNeedClientAuth(need);
    }

    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        delegateSocket.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    @Override
    public void setReceiveBufferSize(int size) throws SocketException {
        delegateSocket.setReceiveBufferSize(size);
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        delegateSocket.setReuseAddress(on);
    }

    @Override
    public void setSoTimeout(int timeout) throws SocketException {
        delegateSocket.setSoTimeout(timeout);
    }

    @Override
    public void setUseClientMode(boolean mode) {
        delegateSocket.setUseClientMode(mode);
    }

    @Override
    public void setWantClientAuth(boolean want) {
        delegateSocket.setWantClientAuth(want);
    }

    @Override
    public String toString() {
        return delegateSocket.toString();
    }
}
