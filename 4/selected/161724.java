package syntelos.lang;

import alto.io.Input;
import alto.io.Output;
import alto.lang.InputStream;
import alto.lang.OutputStream;

/**
 * <p> A multiprotocol Socket abstracts all socket classes, TCP, UDP,
 * MDP and SRV.  The SRV socket has no I/O streams, but accepts TCP
 * sockets.  The TCP socket is a (unicast) connection to a remote
 * host.  Note that the UDP (Unicast &amp; Broadcast) and MDP
 * (Multicast) sockets are senders and receivers.  </p>
 * 
 * <p> This socket will also wrap an SHM connection.  Although in this
 * case the use needs to employ the {@link
 * alto.sys.Lock$Semaphore} locking protocol defined here.
 * Note that the SHM connection has a size boundary, default 2048
 * bytes. </p>
 * 
 * @author jdp
 * @since 1.5
 */
public class Socket extends java.lang.Object implements alto.lang.Socket {

    private static volatile javax.net.ssl.SSLContext sslDefaultContext;

    public static final void SInit() {
    }

    static {
        SInit();
    }

    protected final int type;

    protected final java.net.ServerSocket srv;

    protected final java.net.Socket tcp;

    protected final java.net.DatagramSocket udp;

    protected final java.net.MulticastSocket mdp;

    protected final syntelos.net.shm.Connection shm;

    protected final boolean secure;

    protected alto.sys.Lock.Semaphore lock;

    protected volatile boolean acceptWaitfor = false;

    protected final java.lang.Object accept = new java.lang.Object();

    protected volatile InputStream in;

    protected volatile OutputStream out;

    /**
     * Permits subclasses to define new types.
     */
    protected Socket(int type) {
        super();
        this.type = type;
        this.srv = null;
        this.tcp = null;
        this.udp = null;
        this.mdp = null;
        this.shm = null;
        this.secure = false;
    }

    /**
     * @param shm Shared memory IPC connection
     */
    public Socket(syntelos.net.shm.Connection shm) throws java.io.IOException {
        super();
        if (null != shm) {
            this.type = TYPE_SHM;
            this.srv = null;
            this.tcp = null;
            this.udp = null;
            this.mdp = null;
            this.shm = shm;
            this.lock = shm;
            this.secure = false;
        } else throw new java.lang.IllegalArgumentException();
    }

    /**
     * @param socket TCP service
     */
    public Socket(java.net.ServerSocket socket) {
        super();
        if (null != socket) {
            this.type = TYPE_SRV;
            this.srv = socket;
            this.tcp = null;
            this.udp = null;
            this.mdp = null;
            this.shm = null;
            this.secure = false;
        } else throw new java.lang.IllegalArgumentException();
    }

    /**
     * @param socket TCP service
     */
    public Socket(javax.net.ssl.SSLServerSocket socket) {
        super();
        if (null != socket) {
            this.type = TYPE_SRV;
            this.srv = socket;
            this.tcp = null;
            this.udp = null;
            this.mdp = null;
            this.shm = null;
            this.secure = true;
        } else throw new java.lang.IllegalArgumentException();
    }

    /**
     * @param socket TCP connection to remote host
     */
    public Socket(java.net.Socket socket) {
        super();
        if (null != socket) {
            this.type = TYPE_TCP;
            this.srv = null;
            this.tcp = socket;
            this.udp = null;
            this.mdp = null;
            this.shm = null;
            this.secure = false;
        } else throw new java.lang.IllegalArgumentException();
    }

    /**
     * @param socket TCP connection to remote host
     */
    public Socket(javax.net.ssl.SSLSocket socket) {
        super();
        if (null != socket) {
            this.type = TYPE_TCP;
            this.srv = null;
            this.tcp = socket;
            this.udp = null;
            this.mdp = null;
            this.shm = null;
            this.secure = true;
        } else throw new java.lang.IllegalArgumentException();
    }

    /**
     * Client and server.
     * @param socket Sender and receiver
     */
    public Socket(java.net.DatagramSocket socket) {
        super();
        if (null != socket) {
            this.type = TYPE_UDP;
            this.srv = null;
            this.tcp = null;
            this.udp = socket;
            this.mdp = null;
            this.shm = null;
            this.secure = false;
        } else throw new java.lang.IllegalArgumentException();
    }

    /**
     * Client and server.
     * @param socket Sender and receiver
     */
    public Socket(java.net.MulticastSocket socket) {
        super();
        if (null != socket) {
            this.type = TYPE_MDP;
            this.srv = null;
            this.tcp = null;
            this.udp = socket;
            this.mdp = socket;
            this.shm = null;
            this.secure = false;
        } else throw new java.lang.IllegalArgumentException();
    }

    /**
     * TCP client.
     */
    public Socket(java.net.InetAddress addr, int port) throws java.io.IOException {
        this(addr, port, false);
    }

    /**
     * TCP client may connect using SSL/TLS.
     */
    public Socket(java.net.InetAddress addr, int port, boolean ssl) throws java.io.IOException {
        super();
        if (ssl) {
            this.type = TYPE_TCP;
            this.srv = null;
            try {
                javax.net.ssl.SSLSocket sock = this.createSocketSecure(addr, port);
                this.tcp = sock;
                this.udp = null;
                this.mdp = null;
                this.shm = null;
                this.secure = true;
            } catch (java.security.NoSuchAlgorithmException exc) {
                throw new alto.sys.Error.Bug(exc);
            } catch (java.security.KeyStoreException exc) {
                throw new alto.sys.Error.Bug(exc);
            } catch (java.security.cert.CertificateException exc) {
                throw new alto.sys.Error.Bug(exc);
            } catch (java.security.UnrecoverableKeyException exc) {
                throw new alto.sys.Error.Bug(exc);
            } catch (java.security.KeyManagementException exc) {
                throw new alto.sys.Error.Bug(exc);
            }
        } else {
            this.type = TYPE_TCP;
            this.srv = null;
            this.tcp = this.createSocket(addr, port);
            this.udp = null;
            this.mdp = null;
            this.shm = null;
            this.secure = false;
        }
    }

    public alto.io.Uri getUri() {
        return UriDefault;
    }

    public long lastModified() {
        return 0L;
    }

    public java.lang.String lastModifiedString() {
        return null;
    }

    public long getLastModified() {
        return this.lastModified();
    }

    public java.lang.String getLastModifiedString() {
        return null;
    }

    public boolean setLastModified(long last) {
        return false;
    }

    public final int getType() {
        return this.type;
    }

    public final boolean isService() {
        switch(this.type) {
            case TYPE_SRV:
            case TYPE_UDP:
            case TYPE_MDP:
                return true;
            default:
                return false;
        }
    }

    public final boolean isNotService() {
        return (!this.isService());
    }

    public final boolean isTcpServer() {
        return (TYPE_SRV == this.type);
    }

    public final boolean isNotTcpServer() {
        return (!this.isTcpServer());
    }

    public final boolean isTcpClient() {
        return (TYPE_TCP == this.type);
    }

    public final boolean isNotTcpClient() {
        return (!this.isTcpClient());
    }

    public final boolean isConnection() {
        switch(this.type) {
            case TYPE_SRV:
            case TYPE_TCP:
                return true;
            default:
                return false;
        }
    }

    public final boolean isNotConnection() {
        return (!this.isConnection());
    }

    public final boolean isUdp() {
        return (TYPE_UDP == this.type);
    }

    public final boolean isNotUdp() {
        return (!this.isUdp());
    }

    public final boolean isPeer() {
        switch(this.type) {
            case TYPE_UDP:
            case TYPE_MDP:
                return true;
            default:
                return false;
        }
    }

    public final boolean isNotPeer() {
        return (!this.isPeer());
    }

    public final boolean isMdp() {
        return (TYPE_MDP == this.type);
    }

    public final boolean isNotMdp() {
        return (!this.isMdp());
    }

    public final boolean isMulticast() {
        return (TYPE_MDP == this.type);
    }

    public final boolean isNotMulticast() {
        return (!this.isMulticast());
    }

    /**
     * When true, requires use of {@link
     * alto.sys.Lock$Semaphore} operations, defined here.
     */
    public final boolean isShm() {
        return (TYPE_SHM == this.type);
    }

    public final boolean isNotShm() {
        return (!this.isShm());
    }

    public final boolean isSecure() {
        return this.secure;
    }

    /**
     * @return This is secure, and the cipher suite employs a
     * connection privacy stage.
     */
    public final boolean isSecurePrivate() {
        if (this.isSecure()) {
            javax.net.ssl.SSLSession session = this.getSecureSession();
            if (session.isValid()) return (0 > session.getProtocol().indexOf("_WITH_NULL_"));
        }
        return false;
    }

    /**
     * @return The value from {@link #getSecureRemote()} is not null.
     */
    public final boolean isSecureAuthenticated() {
        if (this.isSecure()) return (null != this.getSecureRemote()); else return false;
    }

    /**
     * @return Never null, otherwise Bug exception.
     * @see #isSecure()
     */
    public javax.net.ssl.SSLSession getSecureSession() {
        return this.getSecureSession(true);
    }

    public javax.net.ssl.SSLSession getSecureSession(boolean exc) {
        if (this.isSecure()) {
            javax.net.ssl.SSLSocket ssl = (javax.net.ssl.SSLSocket) this.tcp;
            javax.net.ssl.SSLSession session = ssl.getSession();
            if (null != session) return session;
        }
        if (exc) throw new alto.sys.Error.Bug(); else return null;
    }

    /**
     * @return Null for unauthenticated client.
     * @see #isSecure()
     */
    public java.security.Principal getSecureRemote() {
        javax.net.ssl.SSLSession session = this.getSecureSession(false);
        if (null != session) {
            try {
                return session.getPeerPrincipal();
            } catch (javax.net.ssl.SSLPeerUnverifiedException unauthenticated_peer) {
            }
        }
        return null;
    }

    /**
     * @return Null for unauthenticated client.
     * @see #isSecure()
     */
    public java.security.cert.Certificate[] getSecureRemoteCertificates() {
        javax.net.ssl.SSLSession session = this.getSecureSession(false);
        if (null != session) {
            try {
                return session.getPeerCertificates();
            } catch (javax.net.ssl.SSLPeerUnverifiedException unauthenticated_peer) {
            }
        }
        return null;
    }

    /**
     * @return Never null, otherwise Bug exception.
     * @see #isSecure()
     */
    public java.security.Principal getSecureHost() {
        javax.net.ssl.SSLSession session = this.getSecureSession();
        return session.getLocalPrincipal();
    }

    public java.net.InetAddress getInetAddress() {
        switch(this.type) {
            case TYPE_SRV:
                return this.srv.getInetAddress();
            case TYPE_TCP:
                return this.tcp.getInetAddress();
            case TYPE_UDP:
                return this.udp.getInetAddress();
            case TYPE_MDP:
                return this.mdp.getInetAddress();
            case TYPE_SHM:
                return this.shm.getLocalHost();
            default:
                throw new alto.sys.Error.Bug();
        }
    }

    public java.net.InetAddress getLocalAddress() {
        switch(this.type) {
            case TYPE_SRV:
                return this.srv.getInetAddress();
            case TYPE_TCP:
                return this.tcp.getLocalAddress();
            case TYPE_UDP:
                return this.udp.getLocalAddress();
            case TYPE_MDP:
                return this.mdp.getLocalAddress();
            case TYPE_SHM:
                return this.shm.getLocalHost();
            default:
                throw new alto.sys.Error.Bug();
        }
    }

    public int getPort() {
        switch(this.type) {
            case TYPE_SRV:
                return this.srv.getLocalPort();
            case TYPE_TCP:
                return this.tcp.getPort();
            case TYPE_UDP:
                return this.udp.getPort();
            case TYPE_MDP:
                return this.mdp.getPort();
            case TYPE_SHM:
                return -1;
            default:
                throw new alto.sys.Error.Bug();
        }
    }

    public int getLocalPort() {
        switch(this.type) {
            case TYPE_SRV:
                return this.srv.getLocalPort();
            case TYPE_TCP:
                return this.tcp.getLocalPort();
            case TYPE_UDP:
                return this.udp.getLocalPort();
            case TYPE_MDP:
                return this.mdp.getLocalPort();
            case TYPE_SHM:
                return -1;
            default:
                throw new alto.sys.Error.Bug();
        }
    }

    public java.net.SocketAddress getRemoteSocketAddress() {
        switch(this.type) {
            case TYPE_TCP:
                return this.tcp.getRemoteSocketAddress();
            case TYPE_UDP:
                return this.udp.getRemoteSocketAddress();
            case TYPE_MDP:
                return this.mdp.getRemoteSocketAddress();
            default:
                throw new alto.sys.Error.Bug();
        }
    }

    public java.net.SocketAddress getLocalSocketAddress() {
        switch(this.type) {
            case TYPE_SRV:
                return this.srv.getLocalSocketAddress();
            case TYPE_TCP:
                return this.tcp.getLocalSocketAddress();
            case TYPE_UDP:
                return this.udp.getLocalSocketAddress();
            case TYPE_MDP:
                return this.mdp.getLocalSocketAddress();
            default:
                throw new alto.sys.Error.Bug();
        }
    }

    public java.nio.channels.WritableByteChannel openChannelWritable() {
        return null;
    }

    public java.nio.channels.WritableByteChannel getChannelWritable() {
        switch(this.type) {
            case TYPE_TCP:
                return this.tcp.getChannel();
            case TYPE_UDP:
                return this.udp.getChannel();
            case TYPE_MDP:
                return this.mdp.getChannel();
            case TYPE_SHM:
                return this.shm.getChannel();
            default:
                throw new alto.sys.Error.Bug();
        }
    }

    public java.nio.channels.ReadableByteChannel openChannelReadable() {
        return null;
    }

    public java.nio.channels.ReadableByteChannel getChannelReadable() {
        switch(this.type) {
            case TYPE_TCP:
                return this.tcp.getChannel();
            case TYPE_UDP:
                return this.udp.getChannel();
            case TYPE_MDP:
                return this.mdp.getChannel();
            case TYPE_SHM:
                return this.shm.getChannel();
            default:
                throw new alto.sys.Error.Bug();
        }
    }

    public final java.nio.channels.SocketChannel getChannelTcp() {
        switch(this.type) {
            case TYPE_TCP:
                return this.tcp.getChannel();
            default:
                throw new alto.sys.Error.Bug();
        }
    }

    public final java.nio.channels.DatagramChannel getChannelUdp() {
        switch(this.type) {
            case TYPE_UDP:
                return this.udp.getChannel();
            case TYPE_MDP:
                return this.mdp.getChannel();
            default:
                throw new alto.sys.Error.Bug();
        }
    }

    public final java.lang.String[] getEnabledCiphers() {
        if (this.secure) {
            switch(this.type) {
                case TYPE_SRV:
                    return ((javax.net.ssl.SSLServerSocket) this.srv).getEnabledCipherSuites();
                case TYPE_TCP:
                    return ((javax.net.ssl.SSLSocket) this.tcp).getEnabledCipherSuites();
            }
        }
        return null;
    }

    public final java.lang.String getEnabledCiphersString() {
        java.lang.String[] list = this.getEnabledCiphers();
        if (null != list) {
            java.lang.StringBuilder strbuf = new java.lang.StringBuilder();
            for (int cc = 0, count = list.length; cc < count; cc++) {
                if (0 < strbuf.length()) strbuf.append(',');
                strbuf.append(list[cc]);
            }
            return strbuf.toString();
        }
        return null;
    }

    /**
     * Overridden in {@link org.syntels.sx.Socket} to return a new
     * instance of that class.
     */
    public alto.lang.Socket acceptNewSocket(java.net.Socket client) throws java.io.IOException {
        if (client instanceof javax.net.ssl.SSLSocket) return new Socket((javax.net.ssl.SSLSocket) client); else return new Socket(client);
    }

    public alto.lang.Socket connect() throws java.io.IOException {
        switch(this.type) {
            case TYPE_SHM:
                this.shm.connect();
                return this;
            default:
                if (this.isNotClosed()) return this; else throw new java.net.SocketException("closed");
        }
    }

    public void handshake() throws java.io.IOException {
        java.net.Socket tcp = this.tcp;
        if (tcp instanceof javax.net.ssl.SSLSocket) {
            javax.net.ssl.SSLSocket ssl = (javax.net.ssl.SSLSocket) tcp;
            ssl.startHandshake();
        }
    }

    public final boolean isQueued() {
        return this.acceptWaitfor;
    }

    public final alto.lang.Socket enqueue() {
        this.acceptWaitfor = true;
        return this;
    }

    public alto.lang.Socket dequeue() {
        return this;
    }

    public alto.lang.Socket release() {
        switch(this.type) {
            case TYPE_SHM:
                this.shm.release();
                this.acceptWaitfor = false;
                synchronized (this.accept) {
                    this.accept.notify();
                }
                return this;
            default:
                try {
                    this.close();
                } catch (java.io.IOException ignore) {
                }
                return this;
        }
    }

    public alto.lang.Socket accept() throws java.io.IOException {
        switch(this.type) {
            case TYPE_SHM:
                if (this.acceptWaitfor) {
                    try {
                        synchronized (this.accept) {
                            this.accept.wait();
                        }
                    } catch (java.lang.InterruptedException exc) {
                        throw new alto.sys.Error.State(exc);
                    }
                }
                if (this.shm.accept()) return this; else throw new alto.sys.Error.Bug();
            case TYPE_SRV:
                while (true) {
                    java.net.Socket netsock = this.srv.accept();
                    if (null != netsock) return this.acceptNewSocket(netsock); else continue;
                }
            default:
                throw new alto.sys.Error.Bug();
        }
    }

    public void send(java.net.DatagramPacket p) throws java.io.IOException {
        if (null != p) {
            switch(this.type) {
                case TYPE_TCP:
                    throw new alto.sys.Error.Bug();
                case TYPE_UDP:
                    this.udp.send(p);
                    return;
                case TYPE_MDP:
                    this.mdp.send(p);
                    return;
                default:
                    throw new alto.sys.Error.Bug();
            }
        }
    }

    public java.net.DatagramPacket receive(java.net.DatagramPacket p) throws java.io.IOException {
        if (null != p) {
            switch(this.type) {
                case TYPE_TCP:
                    throw new alto.sys.Error.Bug();
                case TYPE_UDP:
                    this.udp.receive(p);
                    return p;
                case TYPE_MDP:
                    this.mdp.receive(p);
                    return p;
                default:
                    throw new alto.sys.Error.Bug();
            }
        } else throw new alto.sys.Error.Argument();
    }

    public java.io.InputStream openInputStream() throws java.io.IOException {
        return (java.io.InputStream) this.openInput();
    }

    public Input openInput() throws java.io.IOException {
        switch(this.type) {
            case TYPE_SRV:
                throw new alto.sys.Error.Bug();
            default:
                this.in = new InputStream(this);
                return this.in;
        }
    }

    public java.io.InputStream getInputStream() throws java.io.IOException {
        return (java.io.InputStream) this.getInput();
    }

    public Input getInput() throws java.io.IOException {
        switch(this.type) {
            case TYPE_SRV:
                throw new alto.sys.Error.Bug();
            default:
                InputStream in = this.in;
                if (null == in) {
                    in = new InputStream(this);
                    this.in = in;
                }
                return in;
        }
    }

    public java.io.OutputStream openOutputStream() throws java.io.IOException {
        return (java.io.OutputStream) this.openOutput();
    }

    public Output openOutput() throws java.io.IOException {
        switch(this.type) {
            case TYPE_SRV:
                throw new alto.sys.Error.Bug();
            default:
                this.out = new OutputStream(this);
                return this.out;
        }
    }

    public java.io.OutputStream getOutputStream() throws java.io.IOException {
        return (java.io.OutputStream) this.getOutput();
    }

    public Output getOutput() throws java.io.IOException {
        switch(this.type) {
            case TYPE_SRV:
                throw new alto.sys.Error.Bug();
            default:
                OutputStream out = this.out;
                if (null == out) {
                    out = new OutputStream(this);
                    this.out = out;
                }
                return out;
        }
    }

    public final java.io.InputStream getInputStreamTcp() throws java.io.IOException {
        switch(this.type) {
            case TYPE_TCP:
                if (this.isConnected()) return this.tcp.getInputStream(); else throw new java.io.IOException("not connected");
            default:
                throw new alto.sys.Error.Bug();
        }
    }

    public final java.io.OutputStream getOutputStreamTcp() throws java.io.IOException {
        switch(this.type) {
            case TYPE_TCP:
                if (this.isConnected()) return this.tcp.getOutputStream(); else throw new java.io.IOException("not connected");
            default:
                throw new alto.sys.Error.Bug();
        }
    }

    public final java.io.InputStream getInputStreamShm() throws java.io.IOException {
        switch(this.type) {
            case TYPE_SHM:
                if (this.isConnected()) return this.shm.getInputStream(); else throw new java.io.IOException("not connected");
            default:
                throw new alto.sys.Error.Bug();
        }
    }

    public final java.io.OutputStream getOutputStreamShm() throws java.io.IOException {
        switch(this.type) {
            case TYPE_SHM:
                if (this.isConnected()) return this.shm.getOutputStream(); else throw new java.io.IOException("not connected");
            default:
                throw new alto.sys.Error.Bug();
        }
    }

    public void setTcpNoDelay(boolean on) throws java.net.SocketException {
        switch(this.type) {
            case TYPE_TCP:
                this.tcp.setTcpNoDelay(on);
                return;
            default:
                return;
        }
    }

    public boolean getTcpNoDelay() throws java.net.SocketException {
        switch(this.type) {
            case TYPE_TCP:
                return this.tcp.getTcpNoDelay();
            default:
                return false;
        }
    }

    public void setSoLinger(boolean on, int linger) throws java.net.SocketException {
        switch(this.type) {
            case TYPE_TCP:
                this.tcp.setSoLinger(on, linger);
                return;
            default:
                return;
        }
    }

    public int getSoLinger() throws java.net.SocketException {
        switch(this.type) {
            case TYPE_TCP:
                return this.tcp.getSoLinger();
            default:
                return 0;
        }
    }

    public void setSoTimeout(int timeout) throws java.net.SocketException {
        switch(this.type) {
            case TYPE_SRV:
                this.srv.setSoTimeout(timeout);
                return;
            case TYPE_TCP:
                this.tcp.setSoTimeout(timeout);
                return;
            case TYPE_UDP:
                this.udp.setSoTimeout(timeout);
                return;
            case TYPE_MDP:
                this.mdp.setSoTimeout(timeout);
                return;
            default:
                return;
        }
    }

    public int getSoTimeout() throws java.io.IOException {
        switch(this.type) {
            case TYPE_SRV:
                return this.srv.getSoTimeout();
            case TYPE_TCP:
                return this.tcp.getSoTimeout();
            case TYPE_UDP:
                return this.udp.getSoTimeout();
            case TYPE_MDP:
                return this.mdp.getSoTimeout();
            default:
                return 0;
        }
    }

    public void setSendBufferSize(int size) throws java.net.SocketException {
        switch(this.type) {
            case TYPE_TCP:
                this.tcp.setSendBufferSize(size);
                return;
            case TYPE_UDP:
                this.udp.setSendBufferSize(size);
                return;
            case TYPE_MDP:
                this.mdp.setSendBufferSize(size);
                return;
            default:
                return;
        }
    }

    public int getSendBufferSize() throws java.net.SocketException {
        switch(this.type) {
            case TYPE_TCP:
                return this.tcp.getSendBufferSize();
            case TYPE_UDP:
                return this.udp.getSendBufferSize();
            case TYPE_MDP:
                return this.mdp.getSendBufferSize();
            default:
                return 0;
        }
    }

    public void setReceiveBufferSize(int size) throws java.net.SocketException {
        switch(this.type) {
            case TYPE_SRV:
                this.srv.setReceiveBufferSize(size);
                return;
            case TYPE_TCP:
                this.tcp.setReceiveBufferSize(size);
                return;
            case TYPE_UDP:
                this.udp.setReceiveBufferSize(size);
                return;
            case TYPE_MDP:
                this.mdp.setReceiveBufferSize(size);
                return;
            default:
                return;
        }
    }

    public int getReceiveBufferSize() throws java.net.SocketException {
        switch(this.type) {
            case TYPE_SRV:
                return this.srv.getReceiveBufferSize();
            case TYPE_TCP:
                return this.tcp.getReceiveBufferSize();
            case TYPE_UDP:
                return this.udp.getReceiveBufferSize();
            case TYPE_MDP:
                return this.mdp.getReceiveBufferSize();
            default:
                return 0;
        }
    }

    public void setKeepAlive(boolean on) throws java.net.SocketException {
        switch(this.type) {
            case TYPE_TCP:
                this.tcp.setKeepAlive(on);
                return;
            default:
                return;
        }
    }

    public boolean getKeepAlive() throws java.net.SocketException {
        switch(this.type) {
            case TYPE_TCP:
                return this.tcp.getKeepAlive();
            default:
                return false;
        }
    }

    public void setTrafficClass(int tc) throws java.net.SocketException {
        switch(this.type) {
            case TYPE_TCP:
                this.tcp.setTrafficClass(tc);
                return;
            case TYPE_UDP:
                this.udp.setTrafficClass(tc);
                return;
            case TYPE_MDP:
                this.mdp.setTrafficClass(tc);
                return;
            default:
                return;
        }
    }

    public int getTrafficClass() throws java.net.SocketException {
        switch(this.type) {
            case TYPE_TCP:
                return this.tcp.getTrafficClass();
            case TYPE_UDP:
                return this.udp.getTrafficClass();
            case TYPE_MDP:
                return this.mdp.getTrafficClass();
            default:
                return 0;
        }
    }

    public void setReuseAddress(boolean on) throws java.net.SocketException {
        switch(this.type) {
            case TYPE_SRV:
                this.srv.setReuseAddress(on);
                return;
            case TYPE_TCP:
                this.tcp.setReuseAddress(on);
                return;
            case TYPE_UDP:
                this.udp.setReuseAddress(on);
                return;
            case TYPE_MDP:
                this.mdp.setReuseAddress(on);
                return;
            default:
                return;
        }
    }

    public boolean getReuseAddress() throws java.net.SocketException {
        switch(this.type) {
            case TYPE_SRV:
                return this.srv.getReuseAddress();
            case TYPE_TCP:
                return this.tcp.getReuseAddress();
            case TYPE_UDP:
                return this.udp.getReuseAddress();
            case TYPE_MDP:
                return this.mdp.getReuseAddress();
            default:
                return false;
        }
    }

    public void close() throws java.io.IOException {
        switch(this.type) {
            case TYPE_SRV:
                this.srv.close();
                this.in = null;
                this.out = null;
                return;
            case TYPE_TCP:
                this.tcp.close();
                this.in = null;
                this.out = null;
                return;
            case TYPE_UDP:
                this.udp.close();
                this.in = null;
                this.out = null;
                return;
            case TYPE_MDP:
                this.mdp.close();
                this.in = null;
                this.out = null;
                return;
            case TYPE_SHM:
                this.shm.disconnect();
                this.in = null;
                this.out = null;
                return;
            default:
                return;
        }
    }

    public boolean isConnected() {
        switch(this.type) {
            case TYPE_SRV:
                return (!this.srv.isClosed());
            case TYPE_TCP:
                return this.tcp.isConnected();
            case TYPE_UDP:
                return this.udp.isConnected();
            case TYPE_MDP:
                return this.mdp.isConnected();
            case TYPE_SHM:
                return this.shm.isOpen();
            default:
                return true;
        }
    }

    public final boolean isNotConnected() {
        return (!this.isConnected());
    }

    public boolean isBound() {
        switch(this.type) {
            case TYPE_SRV:
                return this.srv.isBound();
            case TYPE_TCP:
                return this.tcp.isBound();
            case TYPE_UDP:
                return this.udp.isBound();
            case TYPE_MDP:
                return this.mdp.isBound();
            default:
                return true;
        }
    }

    public boolean isClosed() {
        switch(this.type) {
            case TYPE_SRV:
                return this.srv.isClosed();
            case TYPE_TCP:
                return this.tcp.isClosed();
            case TYPE_UDP:
                return this.udp.isClosed();
            case TYPE_MDP:
                return this.mdp.isClosed();
            case TYPE_SHM:
                return this.shm.isNotOpen();
            default:
                return false;
        }
    }

    public final boolean isNotClosed() {
        return (!this.isClosed());
    }

    public final boolean lockReadEnterTry() {
        if (null != this.lock) return this.lock.lockReadEnterTry(); else return true;
    }

    public final void lockReadEnter() {
        if (null != this.lock) this.lock.lockReadEnter();
    }

    public final void lockReadExit() {
        if (null != this.lock) this.lock.lockReadExit();
    }

    public final boolean lockWriteEnterTry() {
        if (null != this.lock) return this.lock.lockWriteEnterTry(); else return true;
    }

    public final boolean lockWriteEnterTry(byte cur, byte nex) {
        if (null != this.lock) return this.lock.lockWriteEnterTry(cur, nex); else return true;
    }

    public final void lockWriteEnter() {
        if (null != this.lock) this.lock.lockWriteEnter();
    }

    /**
     * <p> This is not typically employed directly.  It's use is for
     * specialized locking protocol best implemented as in {@link
     * syntelos.net.shm.Connection} by an object that this class
     * uses. </p>
     * @see syntelos.net.shm.Connection
     */
    public final boolean lockWriteEnter(byte last, byte next) {
        if (null != this.lock) return this.lock.lockWriteEnter(last, next); else return true;
    }

    public final void lockWriteExit() {
        if (null != this.lock) this.lock.lockWriteExit();
    }

    private java.net.Socket createSocket(java.net.InetAddress addr, int port) throws java.io.IOException {
        java.net.Socket sock = new java.net.Socket(addr, port);
        return sock;
    }

    private javax.net.ssl.SSLSocket createSocketSecure(java.net.InetAddress addr, int port) throws java.io.IOException, java.security.KeyStoreException, java.security.NoSuchAlgorithmException, java.security.cert.CertificateException, java.security.UnrecoverableKeyException, java.security.KeyManagementException {
        javax.net.ssl.SSLContext context = javax.net.ssl.SSLContext.getDefault();
        java.security.KeyStore keystore = this.createSocketSecureKeyStore();
        java.security.KeyStore truststore = this.createSocketSecureTrustStore();
        java.lang.String keyFormat = javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm();
        javax.net.ssl.KeyManager[] kms = this.createSocketSecureKeyManagers(keyFormat, keystore);
        javax.net.ssl.TrustManager[] tms = this.createSocketSecureTrustManagers(keyFormat, truststore);
        java.security.SecureRandom srg = this.createSocketSecureRNG();
        context.init(kms, tms, srg);
        javax.net.ssl.SSLSocketFactory factory = context.getSocketFactory();
        javax.net.ssl.SSLSocket sock = (javax.net.ssl.SSLSocket) factory.createSocket(addr, port);
        sock.setEnabledProtocols(SSL_PROT);
        return sock;
    }

    private final java.security.SecureRandom createSocketSecureRNG() {
        return new java.security.SecureRandom();
    }

    private java.security.KeyStore createSocketSecureKeyStore() throws java.io.IOException, java.security.KeyStoreException, java.security.NoSuchAlgorithmException, java.security.cert.CertificateException {
        java.lang.String type = System.getProperty("javax.net.ssl.keyStoreType", "JKS");
        java.lang.String sysFile = java.lang.System.getProperty("javax.net.ssl.keyStoreFile");
        java.lang.String file = (null != sysFile) ? (sysFile) : (java.lang.System.getProperty("user.home") + "/.keystore");
        java.lang.String sysPass = java.lang.System.getProperty("javax.net.ssl.keyStorePassword");
        java.lang.String pass = (null != sysPass) ? (sysPass) : ("l30l10n");
        return this.createSecureSocketStore(type, file, pass);
    }

    private final java.security.KeyStore createSocketSecureTrustStore() throws java.io.IOException, java.security.KeyStoreException, java.security.NoSuchAlgorithmException, java.security.cert.CertificateException {
        java.lang.String type = System.getProperty("javax.net.ssl.trustStoreType", "JKS");
        java.lang.String sysFile = java.lang.System.getProperty("javax.net.ssl.trustStoreFile");
        java.lang.String file = (null != sysFile) ? (sysFile) : (java.lang.System.getProperty("user.home") + "/.keystore");
        java.lang.String sysPass = java.lang.System.getProperty("javax.net.ssl.trustStorePassword");
        java.lang.String pass = (null != sysPass) ? (sysPass) : ("l30l10n");
        return this.createSecureSocketStore(type, file, pass);
    }

    private java.security.KeyStore createSecureSocketStore(java.lang.String type, java.lang.String file, java.lang.String pass) throws java.io.IOException, java.security.KeyStoreException, java.security.NoSuchAlgorithmException, java.security.cert.CertificateException {
        java.security.KeyStore keyStore = java.security.KeyStore.getInstance(type);
        try {
            java.io.InputStream in = new java.io.FileInputStream(file);
            try {
                keyStore.load(in, pass.toCharArray());
                return keyStore;
            } finally {
                in.close();
            }
        } catch (java.io.FileNotFoundException exc) {
            keyStore.load(null, pass.toCharArray());
            return keyStore;
        }
    }

    private final javax.net.ssl.KeyManager[] createSocketSecureKeyManagers(java.lang.String format, java.security.KeyStore store) throws java.io.IOException, java.security.NoSuchAlgorithmException, java.security.KeyStoreException, java.security.UnrecoverableKeyException {
        java.lang.String sysPass = java.lang.System.getProperty("javax.net.ssl.keyStoreKeyPassword");
        java.lang.String pass = (null != sysPass) ? (sysPass) : ("k30r10g");
        javax.net.ssl.KeyManagerFactory factory = javax.net.ssl.KeyManagerFactory.getInstance(format);
        factory.init(store, pass.toCharArray());
        return factory.getKeyManagers();
    }

    private final javax.net.ssl.TrustManager[] createSocketSecureTrustManagers(java.lang.String format, java.security.KeyStore store) throws java.io.IOException, java.security.NoSuchAlgorithmException, java.security.KeyStoreException {
        javax.net.ssl.TrustManagerFactory factory = javax.net.ssl.TrustManagerFactory.getInstance(format);
        factory.init(store);
        return factory.getTrustManagers();
    }
}
