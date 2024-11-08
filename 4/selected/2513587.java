package net.pesahov.remote.socket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.SocketOptions;

/**
 * @author Pesahov Dmitry
 * @since 2.0
 */
public class RemoteServerSocket extends ServerSocket {

    /**
     * Underling proxy of current server socket.
     */
    private UnderlyingServerSocketProxy proxy;

    /**
     * Creates an unbound server socket.
     *
     * @exception IOException IO error when opening the socket.
     * @revised 1.4
     */
    protected RemoteServerSocket() throws IOException {
        super();
    }

    /**
     * Creates an unbound server socket.
     * @param factory UnderlyingSocketFactory instance.
     * @exception IOException IO error when opening the socket.
     */
    public RemoteServerSocket(UnderlyingSocketFactory factory) throws IOException {
        this.proxy = factory.createUnderlyingServerSocketProxy();
        this.proxy.initServerSocket();
    }

    /**
     * Creates a server socket, bound to the specified port. A port of 
     * <code>0</code> creates a socket on any free port. 
     * <p>
     * The maximum queue length for incoming connection indications (a 
     * request to connect) is set to <code>50</code>. If a connection 
     * indication arrives when the queue is full, the connection is refused.
     * <p>
     * If the application has specified a server socket factory, that 
     * factory's <code>createSocketImpl</code> method is called to create 
     * the actual socket implementation. Otherwise a "plain" socket is created.
     * <p>
     * If there is a security manager, 
     * its <code>checkListen</code> method is called
     * with the <code>port</code> argument
     * as its argument to ensure the operation is allowed. 
     * This could result in a SecurityException.
     *
     * @param factory UnderlyingSocketFactory instance.
     * @param      port  the port number, or <code>0</code> to use any
     *                   free port.
     * 
     * @exception  IOException  if an I/O error occurs when opening the socket.
     * @exception  SecurityException
     * if a security manager exists and its <code>checkListen</code> 
     * method doesn't allow the operation.
     * 
     * @see        java.net.SocketImpl
     * @see        java.net.SocketImplFactory#createSocketImpl()
     * @see        java.net.ServerSocket#setSocketFactory(java.net.SocketImplFactory)
     * @see        SecurityManager#checkListen
     */
    public RemoteServerSocket(UnderlyingSocketFactory factory, int port) throws IOException {
        this(factory, port, 50, null);
    }

    /**
     * Creates a server socket and binds it to the specified local port 
     * number, with the specified backlog. 
     * A port number of <code>0</code> creates a socket on any 
     * free port. 
     * <p>
     * The maximum queue length for incoming connection indications (a 
     * request to connect) is set to the <code>backlog</code> parameter. If 
     * a connection indication arrives when the queue is full, the 
     * connection is refused. 
     * <p>
     * If the application has specified a server socket factory, that 
     * factory's <code>createSocketImpl</code> method is called to create 
     * the actual socket implementation. Otherwise a "plain" socket is created.
     * <p>
     * If there is a security manager, 
     * its <code>checkListen</code> method is called
     * with the <code>port</code> argument
     * as its argument to ensure the operation is allowed. 
     * This could result in a SecurityException.
     *
     * <P>The <code>backlog</code> argument must be a positive
     * value greater than 0. If the value passed if equal or less
     * than 0, then the default value will be assumed.
     * <P>
     * @param factory UnderlyingSocketFactory instance.
     * @param      port     the specified port, or <code>0</code> to use
     *                      any free port.
     * @param      backlog  the maximum length of the queue.
     * 
     * @exception  IOException  if an I/O error occurs when opening the socket.
     * @exception  SecurityException
     * if a security manager exists and its <code>checkListen</code> 
     * method doesn't allow the operation.
     * 
     * @see        java.net.SocketImpl
     * @see        java.net.SocketImplFactory#createSocketImpl()
     * @see        java.net.ServerSocket#setSocketFactory(java.net.SocketImplFactory)
     * @see        SecurityManager#checkListen
     */
    public RemoteServerSocket(UnderlyingSocketFactory factory, int port, int backlog) throws IOException {
        this(factory, port, backlog, null);
    }

    /** 
     * Create a server with the specified port, listen backlog, and 
     * local IP address to bind to.  The <i>bindAddr</i> argument
     * can be used on a multi-homed host for a ServerSocket that
     * will only accept connect requests to one of its addresses.
     * If <i>bindAddr</i> is null, it will default accepting
     * connections on any/all local addresses.
     * The port must be between 0 and 65535, inclusive.
     * 
     * <P>If there is a security manager, this method 
     * calls its <code>checkListen</code> method
     * with the <code>port</code> argument
     * as its argument to ensure the operation is allowed. 
     * This could result in a SecurityException.
     *
     * <P>The <code>backlog</code> argument must be a positive
     * value greater than 0. If the value passed if equal or less
     * than 0, then the default value will be assumed.
     * <P>
     * @param factory UnderlyingSocketFactory instance.
     * @param port the local TCP port
     * @param backlog the listen backlog
     * @param bindAddr the local InetAddress the server will bind to
     * 
     * @throws  SecurityException if a security manager exists and 
     * its <code>checkListen</code> method doesn't allow the operation.
     * 
     * @throws  IOException if an I/O error occurs when opening the socket.
     *
     * @see SocketOptions
     * @see SocketImpl
     * @see SecurityManager#checkListen
     */
    public RemoteServerSocket(UnderlyingSocketFactory factory, int port, int backlog, InetAddress bindAddr) throws IOException {
        this.proxy = factory.createUnderlyingServerSocketProxy();
        this.proxy.initServerSocket(port, backlog, bindAddr);
    }

    @Override
    public RemoteSocket accept() throws IOException {
        return proxy.accept();
    }

    @Override
    public void bind(SocketAddress endpoint, int backlog) throws IOException {
        proxy.bind(endpoint, backlog);
    }

    @Override
    public void bind(SocketAddress endpoint) throws IOException {
        proxy.bind(endpoint, 50);
    }

    @Override
    public void close() throws IOException {
        proxy.close();
    }

    @Override
    public RemoteServerSocketChannel getChannel() {
        return (RemoteServerSocketChannel) proxy.getChannel();
    }

    @Override
    public InetAddress getInetAddress() {
        return proxy.getInetAddress();
    }

    @Override
    public int getLocalPort() {
        return proxy.getLocalPort();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return proxy.getLocalSocketAddress();
    }

    @Override
    public synchronized int getReceiveBufferSize() throws SocketException {
        return proxy.getReceiveBufferSize();
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return proxy.getReuseAddress();
    }

    @Override
    public synchronized int getSoTimeout() throws IOException {
        return proxy.getSoTimeout();
    }

    @Override
    public boolean isBound() {
        return proxy.isBound();
    }

    @Override
    public boolean isClosed() {
        return proxy.isClosed();
    }

    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        proxy.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    @Override
    public synchronized void setReceiveBufferSize(int size) throws SocketException {
        proxy.setReceiveBufferSize(size);
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        proxy.setReuseAddress(on);
    }

    @Override
    public synchronized void setSoTimeout(int timeout) throws SocketException {
        proxy.setSoTimeout(timeout);
    }

    @Override
    public String toString() {
        return proxy.toString();
    }
}
