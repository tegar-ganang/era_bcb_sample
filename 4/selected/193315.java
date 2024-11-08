package org.nightlabs.ssltest.socket;

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

    public void bind(SocketAddress bindpoint) throws IOException {
        delegateSocket.bind(bindpoint);
    }

    public void close() throws IOException {
        delegateSocket.close();
    }

    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        delegateSocket.connect(endpoint, timeout);
    }

    public void connect(SocketAddress endpoint) throws IOException {
        delegateSocket.connect(endpoint);
    }

    public boolean equals(Object obj) {
        return delegateSocket.equals(obj);
    }

    public SocketChannel getChannel() {
        return delegateSocket.getChannel();
    }

    public InetAddress getInetAddress() {
        return delegateSocket.getInetAddress();
    }

    public InputStream getInputStream() throws IOException {
        if (compressionEnabled) {
            logger.info("getInputStream(): compression is enabled");
            return new CompressionInputStream(delegateSocket.getInputStream());
        }
        logger.info("getInputStream(): compression is disabled");
        return delegateSocket.getInputStream();
    }

    public boolean getKeepAlive() throws SocketException {
        return delegateSocket.getKeepAlive();
    }

    public InetAddress getLocalAddress() {
        return delegateSocket.getLocalAddress();
    }

    public int getLocalPort() {
        return delegateSocket.getLocalPort();
    }

    public SocketAddress getLocalSocketAddress() {
        return delegateSocket.getLocalSocketAddress();
    }

    public boolean getOOBInline() throws SocketException {
        return delegateSocket.getOOBInline();
    }

    public OutputStream getOutputStream() throws IOException {
        if (compressionEnabled) {
            logger.info("getOutputStream(): compression is enabled");
            return new CompressionOutputStream(delegateSocket.getOutputStream());
        }
        logger.info("getOutputStream(): compression is disabled");
        return delegateSocket.getOutputStream();
    }

    public int getPort() {
        return delegateSocket.getPort();
    }

    public int getReceiveBufferSize() throws SocketException {
        return delegateSocket.getReceiveBufferSize();
    }

    public SocketAddress getRemoteSocketAddress() {
        return delegateSocket.getRemoteSocketAddress();
    }

    public boolean getReuseAddress() throws SocketException {
        return delegateSocket.getReuseAddress();
    }

    public int getSendBufferSize() throws SocketException {
        return delegateSocket.getSendBufferSize();
    }

    public int getSoLinger() throws SocketException {
        return delegateSocket.getSoLinger();
    }

    public int getSoTimeout() throws SocketException {
        return delegateSocket.getSoTimeout();
    }

    public boolean getTcpNoDelay() throws SocketException {
        return delegateSocket.getTcpNoDelay();
    }

    public int getTrafficClass() throws SocketException {
        return delegateSocket.getTrafficClass();
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

    public boolean isConnected() {
        return delegateSocket.isConnected();
    }

    public boolean isInputShutdown() {
        return delegateSocket.isInputShutdown();
    }

    public boolean isOutputShutdown() {
        return delegateSocket.isOutputShutdown();
    }

    public void sendUrgentData(int data) throws IOException {
        delegateSocket.sendUrgentData(data);
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

    public void setKeepAlive(boolean on) throws SocketException {
        delegateSocket.setKeepAlive(on);
    }

    public void setNeedClientAuth(boolean need) {
        delegateSocket.setNeedClientAuth(need);
    }

    public void setOOBInline(boolean on) throws SocketException {
        delegateSocket.setOOBInline(on);
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

    public void setSendBufferSize(int size) throws SocketException {
        delegateSocket.setSendBufferSize(size);
    }

    public void setSoLinger(boolean on, int linger) throws SocketException {
        delegateSocket.setSoLinger(on, linger);
    }

    public void setSoTimeout(int timeout) throws SocketException {
        delegateSocket.setSoTimeout(timeout);
    }

    public void setTcpNoDelay(boolean on) throws SocketException {
        delegateSocket.setTcpNoDelay(on);
    }

    public void setTrafficClass(int tc) throws SocketException {
        delegateSocket.setTrafficClass(tc);
    }

    public void setUseClientMode(boolean mode) {
        delegateSocket.setUseClientMode(mode);
    }

    public void setWantClientAuth(boolean want) {
        delegateSocket.setWantClientAuth(want);
    }

    public void shutdownInput() throws IOException {
        delegateSocket.shutdownInput();
    }

    public void shutdownOutput() throws IOException {
        delegateSocket.shutdownOutput();
    }

    public void startHandshake() throws IOException {
        delegateSocket.startHandshake();
    }

    public String toString() {
        return delegateSocket.toString();
    }
}
