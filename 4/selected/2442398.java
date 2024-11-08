package net.pesahov.remote.socket;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.net.UnknownHostException;

/**
 * @author Pesahov Dmitry
 * @since 2.0
 */
public class RemoteSocket extends Socket {

    /**
     * Underling proxy of current socket.
     */
    private UnderlyingClientSocketProxy proxy;

    /**
     * Creates an unconnected socket, with the system-default type of SocketImpl.
     * @param factory UnderlyingSocketFactory instance.
     * @throws SocketException
     */
    public RemoteSocket(UnderlyingSocketFactory factory) throws SocketException {
        super(new UnderlyingSocketImplementation());
        this.proxy = factory.createUnderlyingClientSocketProxy();
        this.proxy.initSocket();
    }

    /**
     * Creates an unconnected socket, specifying the type of proxy, if any,
     * that should be used regardless of any other settings.
     * <P>
     * If there is a security manager, its <code>checkConnect</code> method
     * is called with the proxy host address and port number
     * as its arguments. This could result in a SecurityException.
     * <P>
     * Examples: 
     * <UL> <LI><code>Socket s = new Socket(Proxy.NO_PROXY);</code> will create
     * a plain socket ignoring any other proxy configuration.</LI>
     * <LI><code>Socket s = new Socket(new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("socks.mydom.com", 1080)));</code>
     * will create a socket connecting through the specified SOCKS proxy
     * server.</LI>
     * </UL>
     * @param factory UnderlyingSocketFactory instance.
     * @param remoteProxy a {@link RemoteProxy} object specifying what kind
     *          of proxying should be used.
     * @throws IllegalArgumentException if the proxy is of an invalid type 
     *      or <code>null</code>.
     * @throws SecurityException if a security manager is present and
     *               permission to connect to the proxy is
     *               denied.
     * @throws SocketException
     * @see java.net.ProxySelector
     * @see java.net.Proxy
     */
    public RemoteSocket(UnderlyingSocketFactory factory, RemoteProxy remoteProxy) throws SocketException {
        super(new UnderlyingSocketImplementation());
        this.proxy = factory.createUnderlyingClientSocketProxy();
        this.proxy.initSocket(remoteProxy);
    }

    /**
     * Creates an unconnected Socket with a user-specified
     * SocketImpl.
     * <P>
     * @param impl an instance of a <B>SocketImpl</B>
     * the subclass wishes to use on the Socket.
     *
     * @exception SocketException if there is an error in the underlying protocol,     
     * such as a TCP error. 
     */
    protected RemoteSocket(SocketImpl impl) throws SocketException {
        super(new UnderlyingSocketImplementation());
        if (impl != null) new IllegalStateException("Unsupported");
    }

    /**
     * Creates a stream socket and connects it to the specified port
     * number on the named host.
     * <p>
     * If the specified host is <tt>null</tt> it is the equivalent of
     * specifying the address as <tt>{@link java.net.InetAddress#getByName InetAddress.getByName}(null)</tt>.
     * In other words, it is equivalent to specifying an address of the 
     * loopback interface. </p>
     * <p>
     * If the application has specified a server socket factory, that
     * factory's <code>createSocketImpl</code> method is called to create
     * the actual socket implementation. Otherwise a "plain" socket is created.
     * <p>
     * If there is a security manager, its
     * <code>checkConnect</code> method is called
     * with the host address and <code>port</code> 
     * as its arguments. This could result in a SecurityException.
     * @param factory UnderlyingSocketFactory instance.
     * @param      host   the host name, or <code>null</code> for the loopback address.
     * @param      port   the port number.
     *
     * @exception  UnknownHostException if the IP address of 
     * the host could not be determined.
     *
     * @exception  IOException  if an I/O error occurs when creating the socket.
     * @exception  SecurityException  if a security manager exists and its  
     *             <code>checkConnect</code> method doesn't allow the operation.
     * @see        java.net.Socket#setSocketImplFactory(java.net.SocketImplFactory)
     * @see        java.net.SocketImpl
     * @see        java.net.SocketImplFactory#createSocketImpl()
     * @see        SecurityManager#checkConnect
     */
    public RemoteSocket(UnderlyingSocketFactory factory, String host, int port) throws UnknownHostException, IOException {
        super(new UnderlyingSocketImplementation());
        this.proxy = factory.createUnderlyingClientSocketProxy();
        this.proxy.initSocket(host, port);
    }

    /**
     * Creates a stream socket and connects it to the specified port
     * number at the specified IP address.
     * <p>
     * If the application has specified a socket factory, that factory's
     * <code>createSocketImpl</code> method is called to create the
     * actual socket implementation. Otherwise a "plain" socket is created.
     * <p>
     * If there is a security manager, its
     * <code>checkConnect</code> method is called
     * with the host address and <code>port</code> 
     * as its arguments. This could result in a SecurityException.
     * @param factory UnderlyingSocketFactory instance.
     * @param      address   the IP address.
     * @param      port      the port number.
     * @exception  IOException  if an I/O error occurs when creating the socket.
     * @exception  SecurityException  if a security manager exists and its  
     *             <code>checkConnect</code> method doesn't allow the operation.
     * @see        java.net.Socket#setSocketImplFactory(java.net.SocketImplFactory)
     * @see        java.net.SocketImpl
     * @see        java.net.SocketImplFactory#createSocketImpl()
     * @see        SecurityManager#checkConnect
     */
    public RemoteSocket(UnderlyingSocketFactory factory, InetAddress address, int port) throws IOException {
        super(new UnderlyingSocketImplementation());
        this.proxy = factory.createUnderlyingClientSocketProxy();
        this.proxy.initSocket(address, port);
    }

    /**
     * Creates a socket and connects it to the specified remote host on
     * the specified remote port. The Socket will also bind() to the local
     * address and port supplied.
     * <p>
     * If the specified host is <tt>null</tt> it is the equivalent of
     * specifying the address as <tt>{@link java.net.InetAddress#getByName InetAddress.getByName}(null)</tt>.
     * In other words, it is equivalent to specifying an address of the 
     * loopback interface. </p>
     * <p>
     * If there is a security manager, its
     * <code>checkConnect</code> method is called
     * with the host address and <code>port</code> 
     * as its arguments. This could result in a SecurityException.
     * @param factory UnderlyingSocketFactory instance.
     * @param host the name of the remote host, or <code>null</code> for the loopback address.
     * @param port the remote port
     * @param localAddr the local address the socket is bound to
     * @param localPort the local port the socket is bound to
     * @exception  IOException  if an I/O error occurs when creating the socket.
     * @exception  SecurityException  if a security manager exists and its  
     *             <code>checkConnect</code> method doesn't allow the operation.
     * @see        SecurityManager#checkConnect
     */
    public RemoteSocket(UnderlyingSocketFactory factory, String host, int port, InetAddress localAddr, int localPort) throws IOException {
        super(new UnderlyingSocketImplementation());
        this.proxy = factory.createUnderlyingClientSocketProxy();
        this.proxy.initSocket(host, port, localAddr, localPort);
    }

    /**
     * Creates connected {@link RemoteSocket} instance created  by {@link RemoteServerSocket} accept() method. 
     * @param proxy {@link UnderlyingClientSocketProxy} instance.
     * @throws SocketException if there is an error in the underlying protocol, such as a TCP error. 
     */
    protected RemoteSocket(UnderlyingClientSocketProxy proxy) throws SocketException {
        super(new UnderlyingSocketImplementation());
        this.proxy = proxy;
    }

    /**
     * Creates a socket and connects it to the specified remote address on
     * the specified remote port. The Socket will also bind() to the local
     * address and port supplied.
     * <p>
     * If there is a security manager, its
     * <code>checkConnect</code> method is called
     * with the host address and <code>port</code> 
     * as its arguments. This could result in a SecurityException.
     * @param factory UnderlyingSocketFactory instance.
     * @param address the remote address
     * @param port the remote port
     * @param localAddr the local address the socket is bound to
     * @param localPort the local port the socket is bound to
     * @exception  IOException  if an I/O error occurs when creating the socket.
     * @exception  SecurityException  if a security manager exists and its  
     *             <code>checkConnect</code> method doesn't allow the operation.
     * @see        SecurityManager#checkConnect
     */
    public RemoteSocket(UnderlyingSocketFactory factory, InetAddress address, int port, InetAddress localAddr, int localPort) throws IOException {
        super(new UnderlyingSocketImplementation());
        this.proxy = factory.createUnderlyingClientSocketProxy();
        this.proxy.initSocket(address, port, localAddr, localPort);
    }

    @Override
    public void bind(SocketAddress bindpoint) throws IOException {
        this.proxy.bind(bindpoint);
    }

    @Override
    public synchronized void close() throws IOException {
        this.proxy.close();
    }

    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        this.proxy.connect(endpoint, timeout);
    }

    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        this.proxy.connect(endpoint, 0);
    }

    @Override
    public RemoteSocketChannel getChannel() {
        return (RemoteSocketChannel) this.proxy.getChannel();
    }

    @Override
    public InetAddress getInetAddress() {
        return this.proxy.getInetAddress();
    }

    @Override
    public RemoteInputStream getInputStream() throws IOException {
        return this.proxy.getInputStream();
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
        return this.proxy.getKeepAlive();
    }

    @Override
    public InetAddress getLocalAddress() {
        return this.proxy.getLocalAddress();
    }

    @Override
    public int getLocalPort() {
        return this.proxy.getLocalPort();
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return this.proxy.getLocalSocketAddress();
    }

    @Override
    public boolean getOOBInline() throws SocketException {
        return this.proxy.getOOBInline();
    }

    @Override
    public RemoteOutputStream getOutputStream() throws IOException {
        return this.proxy.getOutputStream();
    }

    @Override
    public int getPort() {
        return this.proxy.getPort();
    }

    @Override
    public synchronized int getReceiveBufferSize() throws SocketException {
        return this.proxy.getReceiveBufferSize();
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return this.proxy.getRemoteSocketAddress();
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        return this.proxy.getReuseAddress();
    }

    @Override
    public synchronized int getSendBufferSize() throws SocketException {
        return this.proxy.getSendBufferSize();
    }

    @Override
    public int getSoLinger() throws SocketException {
        return this.proxy.getSoLinger();
    }

    @Override
    public synchronized int getSoTimeout() throws SocketException {
        return this.proxy.getSoTimeout();
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        return this.proxy.getTcpNoDelay();
    }

    @Override
    public int getTrafficClass() throws SocketException {
        return this.proxy.getTrafficClass();
    }

    @Override
    public boolean isBound() {
        return this.proxy.isBound();
    }

    @Override
    public boolean isClosed() {
        return this.proxy.isClosed();
    }

    @Override
    public boolean isConnected() {
        return this.proxy.isConnected();
    }

    @Override
    public boolean isInputShutdown() {
        return this.proxy.isInputShutdown();
    }

    @Override
    public boolean isOutputShutdown() {
        return this.proxy.isOutputShutdown();
    }

    @Override
    public void sendUrgentData(int data) throws IOException {
        this.proxy.sendUrgentData(data);
    }

    @Override
    public void setKeepAlive(boolean on) throws SocketException {
        this.proxy.setKeepAlive(on);
    }

    @Override
    public void setOOBInline(boolean on) throws SocketException {
        this.proxy.setOOBInline(on);
    }

    @Override
    public void setPerformancePreferences(int connectionTime, int latency, int bandwidth) {
        this.proxy.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    @Override
    public synchronized void setReceiveBufferSize(int size) throws SocketException {
        this.proxy.setReceiveBufferSize(size);
    }

    @Override
    public void setReuseAddress(boolean on) throws SocketException {
        this.proxy.setReuseAddress(on);
    }

    @Override
    public synchronized void setSendBufferSize(int size) throws SocketException {
        this.proxy.setSendBufferSize(size);
    }

    @Override
    public void setSoLinger(boolean on, int linger) throws SocketException {
        this.proxy.setSoLinger(on, linger);
    }

    @Override
    public synchronized void setSoTimeout(int timeout) throws SocketException {
        this.proxy.setSoTimeout(timeout);
    }

    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
        this.proxy.setTcpNoDelay(on);
    }

    @Override
    public void setTrafficClass(int tc) throws SocketException {
        this.proxy.setTrafficClass(tc);
    }

    @Override
    public void shutdownInput() throws IOException {
        this.proxy.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        this.proxy.shutdownOutput();
    }

    @Override
    public String toString() {
        return this.proxy.toString();
    }
}
