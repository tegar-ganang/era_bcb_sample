package org.nightlabs.ssltest.socket;

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

    public Socket accept() throws IOException {
        return new SSLCompressionSocket((SSLSocket) delegateSocket.accept(), compressionEnabled);
    }

    public void bind(SocketAddress endpoint, int backlog) throws IOException {
        delegateSocket.bind(endpoint, backlog);
    }

    public void bind(SocketAddress endpoint) throws IOException {
        delegateSocket.bind(endpoint);
    }

    public void close() throws IOException {
        delegateSocket.close();
    }

    public boolean equals(Object obj) {
        return delegateSocket.equals(obj);
    }

    public ServerSocketChannel getChannel() {
        return delegateSocket.getChannel();
    }

    public String[] getEnabledCipherSuites() {
        return delegateSocket.getEnabledCipherSuites();
    }

    public String[] getEnabledProtocols() {
        return delegateSocket.getEnabledProtocols();
    }

    public boolean getEnableSessionCreation() {
        return delegateSocket.getEnableSessionCreation();
    }

    public InetAddress getInetAddress() {
        return delegateSocket.getInetAddress();
    }

    public int getLocalPort() {
        return delegateSocket.getLocalPort();
    }

    public SocketAddress getLocalSocketAddress() {
        return delegateSocket.getLocalSocketAddress();
    }

    public boolean getNeedClientAuth() {
        return delegateSocket.getNeedClientAuth();
    }

    public int getReceiveBufferSize() throws SocketException {
        return delegateSocket.getReceiveBufferSize();
    }

    public boolean getReuseAddress() throws SocketException {
        return delegateSocket.getReuseAddress();
    }

    public int getSoTimeout() throws IOException {
        return delegateSocket.getSoTimeout();
    }

    public String[] getSupportedCipherSuites() {
        return delegateSocket.getSupportedCipherSuites();
    }

    public String[] getSupportedProtocols() {
        return delegateSocket.getSupportedProtocols();
    }

    public boolean getUseClientMode() {
        return delegateSocket.getUseClientMode();
    }

    public boolean getWantClientAuth() {
        return delegateSocket.getWantClientAuth();
    }

    public int hashCode() {
        return delegateSocket.hashCode();
    }

    public boolean isBound() {
        return delegateSocket.isBound();
    }

    public boolean isClosed() {
        return delegateSocket.isClosed();
    }

    public void setEnabledCipherSuites(String[] suites) {
        delegateSocket.setEnabledCipherSuites(suites);
    }

    public void setEnabledProtocols(String[] protocols) {
        delegateSocket.setEnabledProtocols(protocols);
    }

    public void setEnableSessionCreation(boolean flag) {
        delegateSocket.setEnableSessionCreation(flag);
    }

    public void setNeedClientAuth(boolean need) {
        delegateSocket.setNeedClientAuth(need);
    }

    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        delegateSocket.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    public void setReceiveBufferSize(int size) throws SocketException {
        delegateSocket.setReceiveBufferSize(size);
    }

    public void setReuseAddress(boolean on) throws SocketException {
        delegateSocket.setReuseAddress(on);
    }

    public void setSoTimeout(int timeout) throws SocketException {
        delegateSocket.setSoTimeout(timeout);
    }

    public void setUseClientMode(boolean mode) {
        delegateSocket.setUseClientMode(mode);
    }

    public void setWantClientAuth(boolean want) {
        delegateSocket.setWantClientAuth(want);
    }

    public String toString() {
        return delegateSocket.toString();
    }
}
