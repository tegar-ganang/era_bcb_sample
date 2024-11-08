package org.nightlabs.rmissl.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import org.apache.log4j.Logger;

/**
 * @author marco schulze - marco at nightlabs dot de
 */
public class SSLCompressionSocket extends SSLSocket {

    private static final Logger logger = Logger.getLogger(SSLCompressionSocket.class);

    private SSLSocket delegateSocket;

    private boolean compressionEnabled;

    public SSLCompressionSocket(SSLSocket delegateSocket, boolean compressionEnabled) {
        this.delegateSocket = delegateSocket;
        this.compressionEnabled = compressionEnabled;
    }

    @Override
    public void addHandshakeCompletedListener(HandshakeCompletedListener listener) {
        delegateSocket.addHandshakeCompletedListener(listener);
    }

    @Override
    public boolean getEnableSessionCreation() {
        return delegateSocket.getEnableSessionCreation();
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
    public boolean getNeedClientAuth() {
        return delegateSocket.getNeedClientAuth();
    }

    @Override
    public SSLSession getSession() {
        return delegateSocket.getSession();
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
    public void removeHandshakeCompletedListener(HandshakeCompletedListener listener) {
        delegateSocket.removeHandshakeCompletedListener(listener);
    }

    @Override
    public void bind(SocketAddress bindpoint) throws IOException {
        delegateSocket.bind(bindpoint);
    }

    @Override
    public void close() throws IOException {
        delegateSocket.close();
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        delegateSocket.connect(endpoint, timeout);
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        delegateSocket.connect(endpoint);
    }

    @Override
    public boolean equals(Object obj) {
        return delegateSocket.equals(obj);
    }

    @Override
    public SocketChannel getChannel() {
        return delegateSocket.getChannel();
    }

    @Override
    public InetAddress getInetAddress() {
        return delegateSocket.getInetAddress();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (compressionEnabled) {
            if (logger.isTraceEnabled()) logger.trace("getInputStream(): compression is enabled");
            return new CompressionInputStream(delegateSocket.getInputStream());
        }
        if (logger.isTraceEnabled()) logger.trace("getInputStream(): compression is disabled");
        return delegateSocket.getInputStream();
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
        return delegateSocket.getKeepAlive();
    }

    @Override
    public InetAddress getLocalAddress() {
        return delegateSocket.getLocalAddress();
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
    public boolean getOOBInline() throws SocketException {
        return delegateSocket.getOOBInline();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        if (compressionEnabled) {
            if (logger.isTraceEnabled()) logger.trace("getOutputStream(): compression is enabled");
            return new CompressionOutputStream(delegateSocket.getOutputStream());
        }
        if (logger.isTraceEnabled()) logger.trace("getOutputStream(): compression is disabled");
        return delegateSocket.getOutputStream();
    }

    @Override
    public int getPort() {
        return delegateSocket.getPort();
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
        return delegateSocket.getReceiveBufferSize();
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return delegateSocket.getRemoteSocketAddress();
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return delegateSocket.getReuseAddress();
    }

    @Override
    public int getSendBufferSize() throws SocketException {
        return delegateSocket.getSendBufferSize();
    }

    @Override
    public int getSoLinger() throws SocketException {
        return delegateSocket.getSoLinger();
    }

    @Override
    public int getSoTimeout() throws SocketException {
        return delegateSocket.getSoTimeout();
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        return delegateSocket.getTcpNoDelay();
    }

    @Override
    public int getTrafficClass() throws SocketException {
        return delegateSocket.getTrafficClass();
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
    public boolean isConnected() {
        return delegateSocket.isConnected();
    }

    @Override
    public boolean isInputShutdown() {
        return delegateSocket.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        return delegateSocket.isOutputShutdown();
    }

    @Override
    public void sendUrgentData(int data) throws IOException {
        delegateSocket.sendUrgentData(data);
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
    public void setKeepAlive(boolean on) throws SocketException {
        delegateSocket.setKeepAlive(on);
    }

    @Override
    public void setNeedClientAuth(boolean need) {
        delegateSocket.setNeedClientAuth(need);
    }

    @Override
    public void setOOBInline(boolean on) throws SocketException {
        delegateSocket.setOOBInline(on);
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
    public void setSendBufferSize(int size) throws SocketException {
        delegateSocket.setSendBufferSize(size);
    }

    @Override
    public void setSoLinger(boolean on, int linger) throws SocketException {
        delegateSocket.setSoLinger(on, linger);
    }

    @Override
    public void setSoTimeout(int timeout) throws SocketException {
        delegateSocket.setSoTimeout(timeout);
    }

    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
        delegateSocket.setTcpNoDelay(on);
    }

    @Override
    public void setTrafficClass(int tc) throws SocketException {
        delegateSocket.setTrafficClass(tc);
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
    public void shutdownInput() throws IOException {
        delegateSocket.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        delegateSocket.shutdownOutput();
    }

    @Override
    public void startHandshake() throws IOException {
        delegateSocket.startHandshake();
    }

    @Override
    public String toString() {
        return delegateSocket.toString();
    }
}
