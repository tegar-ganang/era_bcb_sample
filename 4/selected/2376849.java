package org.jpos.iso;

import org.jpos.core.Configurable;
import org.jpos.core.Configuration;
import org.jpos.core.ConfigurationException;
import org.jpos.iso.ISOFilter.VetoException;
import org.jpos.iso.header.BaseHeader;
import org.jpos.util.LogEvent;
import org.jpos.util.LogSource;
import org.jpos.util.Logger;
import org.jpos.util.NameRegistrar;
import java.io.*;
import java.net.*;
import java.util.Collection;
import java.util.Iterator;
import java.util.Observable;
import java.util.Vector;

/**
 * ISOChannel is an abstract class that provides functionality that
 * allows the transmision and reception of ISO 8583 Messages
 * over a TCP/IP session.
 * <p>
 * It is not thread-safe, ISOMUX takes care of the
 * synchronization details
 * <p>
 * ISOChannel is Observable in order to suport GUI components
 * such as ISOChannelPanel.
 * <br>
 * It now support the new Logger architecture so we will
 * probably setup ISOChannelPanel to be a LogListener insteado
 * of being an Observer in future releases.
 * 
 * @author Alejandro P. Revilla
 * @author Bharavi Gade
 * @version $Revision: 2999 $ $Date: 2010-10-14 19:01:39 -0400 (Thu, 14 Oct 2010) $
 * @see ISOMsg
 * @see MUX
 * @see ISOException
 * @see org.jpos.iso.channel.CSChannel
 * @see Logger
 *
 */
public abstract class BaseChannel extends Observable implements FilteredChannel, ClientChannel, ServerChannel, FactoryChannel, LogSource, Configurable, BaseChannelMBean, Cloneable {

    private Socket socket;

    private String host, localIface;

    private String[] hosts;

    int[] ports;

    private int port, timeout, connectTimeout, localPort;

    private int maxPacketLength = 100000;

    private boolean keepAlive;

    private Configuration cfg;

    protected boolean usable;

    protected boolean overrideHeader;

    private String name;

    protected DataInputStream serverIn;

    protected DataOutputStream serverOut;

    protected Object serverInLock = new Object();

    protected Object serverOutLock = new Object();

    protected ISOPackager packager;

    protected ServerSocket serverSocket = null;

    protected Vector incomingFilters, outgoingFilters;

    protected ISOClientSocketFactory socketFactory = null;

    protected int[] cnt;

    protected Logger logger = null;

    protected String realm = null;

    protected String originalRealm = null;

    protected byte[] header = null;

    /**
     * constructor shared by server and client
     * ISOChannels (which have different signatures)
     */
    public BaseChannel() {
        super();
        cnt = new int[SIZEOF_CNT];
        name = "";
        incomingFilters = new Vector();
        outgoingFilters = new Vector();
        setHost(null, 0);
    }

    /**
     * constructs a client ISOChannel
     * @param host  server TCP Address
     * @param port  server port number
     * @param p     an ISOPackager
     * @see ISOPackager
     */
    public BaseChannel(String host, int port, ISOPackager p) {
        this();
        setHost(host, port);
        setPackager(p);
    }

    /**
     * constructs a server ISOChannel
     * @param p     an ISOPackager
     * @exception IOException on error
     * @see ISOPackager
     */
    public BaseChannel(ISOPackager p) throws IOException {
        this();
        setPackager(p);
    }

    /**
     * constructs a server ISOChannel associated with a Server Socket
     * @param p     an ISOPackager
     * @param serverSocket where to accept a connection
     * @exception IOException on error
     * @see ISOPackager
     */
    public BaseChannel(ISOPackager p, ServerSocket serverSocket) throws IOException {
        this();
        setPackager(p);
        setServerSocket(serverSocket);
    }

    /**
     * initialize an ISOChannel
     * @param host  server TCP Address
     * @param port  server port number
     */
    public void setHost(String host, int port) {
        this.host = host;
        this.port = port;
        this.hosts = new String[] { host };
        this.ports = new int[] { port };
    }

    /**
     * initialize an ISOChannel
     * @param iface server TCP Address
     * @param port  server port number
     */
    public void setLocalAddress(String iface, int port) {
        this.localIface = iface;
        this.localPort = port;
    }

    /**
     * @param host to connect (client ISOChannel)
     */
    public void setHost(String host) {
        this.host = host;
        this.hosts = new String[] { host };
    }

    /**
     * @param port to connect (client ISOChannel)
     */
    public void setPort(int port) {
        this.port = port;
        this.ports = new int[] { port };
    }

    /**
     * @return hostname (may be null)
     */
    public String getHost() {
        return host;
    }

    /**
     * @return port number
     */
    public int getPort() {
        return port;
    }

    /**
     * set Packager for channel
     * @param p     an ISOPackager
     * @see ISOPackager
     */
    public void setPackager(ISOPackager p) {
        this.packager = p;
    }

    /**
     * @return current packager
     */
    public ISOPackager getPackager() {
        return packager;
    }

    /**
     * Associates this ISOChannel with a server socket
     * @param sock where to accept a connection
     */
    public void setServerSocket(ServerSocket sock) {
        setHost(null, 0);
        this.serverSocket = sock;
        name = "";
    }

    /**
     * reset stat info
     */
    public void resetCounters() {
        for (int i = 0; i < SIZEOF_CNT; i++) cnt[i] = 0;
    }

    /**
    * @return counters
    */
    public int[] getCounters() {
        return cnt;
    }

    /**
     * @return the connection state
     */
    public boolean isConnected() {
        return socket != null && usable;
    }

    /**
     * setup I/O Streams from socket
     * @param socket a Socket (client or server)
     * @exception IOException on error
     */
    protected void connect(Socket socket) throws IOException {
        this.socket = socket;
        applyTimeout();
        setLogger(getLogger(), getOriginalRealm() + "/" + socket.getInetAddress().getHostAddress() + ":" + socket.getPort());
        synchronized (serverInLock) {
            serverIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        }
        synchronized (serverOutLock) {
            serverOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), 2048));
        }
        postConnectHook();
        usable = true;
        cnt[CONNECT]++;
        setChanged();
        notifyObservers();
    }

    protected void postConnectHook() throws IOException {
    }

    /**
     * factory method pattern (as suggested by Vincent.Greene@amo.com)
     * @param host remote host
     * @param port remote port
     * @throws IOException on error
     *
     * Use Socket factory if exists. If it is missing create a normal socket
     * @see ISOClientSocketFactory
     * @return newly created socket
     */
    protected Socket newSocket(String host, int port) throws IOException {
        try {
            if (socketFactory != null) return socketFactory.createSocket(host, port); else {
                if (connectTimeout > 0) {
                    Socket s = new Socket();
                    s.connect(new InetSocketAddress(host, port), connectTimeout);
                    return s;
                } else if (localIface == null && localPort == 0) {
                    return new Socket(host, port);
                } else {
                    InetAddress addr = (localIface == null) ? InetAddress.getLocalHost() : InetAddress.getByName(localIface);
                    return new Socket(host, port, addr, localPort);
                }
            }
        } catch (ISOException e) {
            throw new IOException(e.getMessage());
        }
    }

    protected Socket newSocket(String[] hosts, int[] ports, LogEvent evt) throws IOException {
        Socket s = null;
        for (int i = 0; i < hosts.length; i++) {
            try {
                evt.addMessage(hosts[i] + ":" + ports[i]);
                s = newSocket(hosts[i], ports[i]);
                break;
            } catch (IOException e) {
                evt.addMessage("  " + e.getMessage());
            }
        }
        if (s == null) throw new IOException("Unable to connect");
        return s;
    }

    /**
     * @return current socket
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * @return current serverSocket
     */
    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    /** 
     * sets socket timeout (as suggested by 
     * Leonard Thomas <leonard@rhinosystemsinc.com>)
     * @param timeout in milliseconds
     * @throws SocketException on error
     */
    public void setTimeout(int timeout) throws SocketException {
        this.timeout = timeout;
        applyTimeout();
    }

    public int getTimeout() {
        return timeout;
    }

    protected void applyTimeout() throws SocketException {
        if (timeout >= 0 && socket != null) socket.setSoTimeout(timeout);
    }

    /**
     * Connects client ISOChannel to server
     * @exception IOException
     */
    public void connect() throws IOException {
        LogEvent evt = new LogEvent(this, "connect");
        try {
            if (serverSocket != null) {
                accept(serverSocket);
                evt.addMessage("local port " + serverSocket.getLocalPort() + " remote host " + socket.getInetAddress());
            } else {
                connect(newSocket(hosts, ports, evt));
            }
            applyTimeout();
            if (socket != null) socket.setKeepAlive(keepAlive);
            Logger.log(evt);
            setChanged();
            notifyObservers();
        } catch (ConnectException e) {
            Logger.log(new LogEvent(this, "connection-refused", getHost() + ":" + getPort()));
        } catch (IOException e) {
            evt.addMessage(e.getMessage());
            Logger.log(evt);
            throw e;
        }
    }

    /**
     * Accepts connection 
     * @exception IOException
     */
    public void accept(ServerSocket s) throws IOException {
        Socket ss = s.accept();
        this.name = ss.getInetAddress().getHostAddress() + ":" + ss.getPort();
        connect(ss);
    }

    /**
     * @param b - new Usable state (used by ISOMUX internals to
     * flag as unusable in order to force a reconnection)
     */
    public void setUsable(boolean b) {
        Logger.log(new LogEvent(this, "usable", b));
        usable = b;
    }

    /**
    * allow subclasses to override default packager
    * on outgoing messages
    * @param m outgoing ISOMsg
    * @return ISOPackager
    */
    protected ISOPackager getDynamicPackager(ISOMsg m) {
        return packager;
    }

    /**
    * allow subclasses to override default packager
    * on outgoing messages
    * @param image incoming message image
    * @return ISOPackager
    */
    protected ISOPackager getDynamicPackager(byte[] image) {
        return packager;
    }

    /**
     * allow subclasses to override default packager
     * on outgoing messages
     * @param header message header
     * @param image incoming message image
     * @return ISOPackager
     */
    protected ISOPackager getDynamicPackager(byte[] header, byte[] image) {
        return getDynamicPackager(image);
    }

    /** 
     * Allow subclasses to override the Default header on
     * incoming messages.
     * @param image message image
     * @return ISOHeader instance
     */
    protected ISOHeader getDynamicHeader(byte[] image) {
        return image != null ? new BaseHeader(image) : null;
    }

    protected void sendMessageLength(int len) throws IOException {
    }

    protected void sendMessageHeader(ISOMsg m, int len) throws IOException {
        if (!isOverrideHeader() && m.getHeader() != null) serverOut.write(m.getHeader()); else if (header != null) serverOut.write(header);
    }

    /**
     * @deprecated use sendMessageTrailler(ISOMsg m, byte[] b) instead.
     * @param m a reference to the ISOMsg
     * @param len the packed image length
     * @throws IOException on error
     */
    protected void sendMessageTrailler(ISOMsg m, int len) throws IOException {
    }

    @SuppressWarnings("deprecation")
    protected void sendMessageTrailler(ISOMsg m, byte[] b) throws IOException {
        sendMessageTrailler(m, b.length);
    }

    protected void getMessageTrailler() throws IOException {
    }

    protected void getMessage(byte[] b, int offset, int len) throws IOException, ISOException {
        serverIn.readFully(b, offset, len);
    }

    protected int getMessageLength() throws IOException, ISOException {
        return -1;
    }

    protected int getHeaderLength() {
        return header != null ? header.length : 0;
    }

    protected int getHeaderLength(byte[] b) {
        return 0;
    }

    protected int getHeaderLength(ISOMsg m) {
        return (!overrideHeader && m.getHeader() != null) ? m.getHeader().length : getHeaderLength();
    }

    protected byte[] streamReceive() throws IOException {
        return new byte[0];
    }

    protected void sendMessage(byte[] b, int offset, int len) throws IOException {
        serverOut.write(b, offset, len);
    }

    /**
     * sends an ISOMsg over the TCP/IP session
     * @param m the Message to be sent
     * @exception IOException
     * @exception ISOException
     * @exception ISOFilter.VetoException;
     */
    public void send(ISOMsg m) throws IOException, ISOException {
        LogEvent evt = new LogEvent(this, "send");
        try {
            if (!isConnected()) throw new ISOException("unconnected ISOChannel");
            m.setDirection(ISOMsg.OUTGOING);
            ISOPackager p = getDynamicPackager(m);
            m.setPackager(p);
            m = applyOutgoingFilters(m, evt);
            evt.addMessage(m);
            m.setDirection(ISOMsg.OUTGOING);
            m.setPackager(p);
            byte[] b = m.pack();
            synchronized (serverOutLock) {
                sendMessageLength(b.length + getHeaderLength(m));
                sendMessageHeader(m, b.length);
                sendMessage(b, 0, b.length);
                sendMessageTrailler(m, b);
                serverOut.flush();
            }
            cnt[TX]++;
            setChanged();
            notifyObservers(m);
        } catch (VetoException e) {
            evt.addMessage(m);
            evt.addMessage(e);
            throw e;
        } catch (ISOException e) {
            evt.addMessage(e);
            throw e;
        } catch (IOException e) {
            evt.addMessage(e);
            throw e;
        } catch (Exception e) {
            evt.addMessage(e);
            throw new ISOException("unexpected exception", e);
        } finally {
            Logger.log(evt);
        }
    }

    /**
     * sends a byte[] over the TCP/IP session
     * @param b the byte array to be sent
     * @exception IOException
     * @exception ISOException
     * @exception ISOFilter.VetoException;
     */
    public void send(byte[] b) throws IOException, ISOException {
        LogEvent evt = new LogEvent(this, "send");
        try {
            if (!isConnected()) throw new ISOException("unconnected ISOChannel");
            synchronized (serverOutLock) {
                serverOut.write(b);
                serverOut.flush();
            }
            cnt[TX]++;
            setChanged();
        } catch (Exception e) {
            evt.addMessage(e);
            throw new ISOException("unexpected exception", e);
        } finally {
            Logger.log(evt);
        }
    }

    /**
     * Sends a high-level keep-alive message (zero length)
     * @throws IOException on exception
     */
    public void sendKeepAlive() throws IOException {
        synchronized (serverOutLock) {
            sendMessageLength(0);
            serverOut.flush();
        }
    }

    protected boolean isRejected(byte[] b) {
        return false;
    }

    protected boolean shouldIgnore(byte[] b) {
        return false;
    }

    /**
     * support old factory method name for backward compatibility
     * @return newly created ISOMsg
     */
    protected ISOMsg createMsg() {
        return createISOMsg();
    }

    protected ISOMsg createISOMsg() {
        return packager.createISOMsg();
    }

    /**
     * Reads in a message header.
     *
     * @param hLen The Length og the reader to read
     * @return The header bytes that were read in
     * @throws IOException on error
     */
    protected byte[] readHeader(int hLen) throws IOException {
        byte[] header = new byte[hLen];
        serverIn.readFully(header, 0, hLen);
        return header;
    }

    /**
     * Waits and receive an ISOMsg over the TCP/IP session
     * @return the Message received
     * @throws IOException
     * @throws ISOException
     */
    public ISOMsg receive() throws IOException, ISOException {
        byte[] b = null;
        byte[] header = null;
        LogEvent evt = new LogEvent(this, "receive");
        ISOMsg m = createMsg();
        m.setSource(this);
        try {
            if (!isConnected()) throw new ISOException("unconnected ISOChannel");
            synchronized (serverInLock) {
                int len = getMessageLength();
                int hLen = getHeaderLength();
                if (len == -1) {
                    if (hLen > 0) {
                        header = readHeader(hLen);
                    }
                    b = streamReceive();
                } else if (len > 0 && len <= getMaxPacketLength()) {
                    if (hLen > 0) {
                        header = readHeader(hLen);
                        len -= header.length;
                    }
                    b = new byte[len];
                    getMessage(b, 0, len);
                    getMessageTrailler();
                } else throw new ISOException("receive length " + len + " seems strange - maxPacketLength = " + getMaxPacketLength());
            }
            m.setPackager(getDynamicPackager(header, b));
            m.setHeader(getDynamicHeader(header));
            if (b.length > 0 && !shouldIgnore(header)) unpack(m, b);
            m.setDirection(ISOMsg.INCOMING);
            evt.addMessage(m);
            m = applyIncomingFilters(m, header, b, evt);
            m.setDirection(ISOMsg.INCOMING);
            cnt[RX]++;
            setChanged();
            notifyObservers(m);
        } catch (ISOException e) {
            evt.addMessage(e);
            if (header != null) {
                evt.addMessage("--- header ---");
                evt.addMessage(ISOUtil.hexdump(header));
            }
            if (b != null) {
                evt.addMessage("--- data ---");
                evt.addMessage(ISOUtil.hexdump(b));
            }
            throw e;
        } catch (EOFException e) {
            closeSocket();
            evt.addMessage("<peer-disconnect/>");
            throw e;
        } catch (SocketException e) {
            closeSocket();
            if (usable) evt.addMessage("<peer-disconnect>" + e.getMessage() + "</peer-disconnect>");
            throw e;
        } catch (InterruptedIOException e) {
            closeSocket();
            evt.addMessage("<io-timeout/>");
            throw e;
        } catch (IOException e) {
            closeSocket();
            if (usable) evt.addMessage(e);
            throw e;
        } catch (Exception e) {
            evt.addMessage(m);
            evt.addMessage(e);
            throw new ISOException("unexpected exception", e);
        } finally {
            Logger.log(evt);
        }
        return m;
    }

    /**
     * Low level receive
     * @param b byte array
     * @throws IOException on error
     * @return the total number of bytes read into the buffer,
     * or -1 if there is no more data because the end of the stream has been reached.
     */
    public int getBytes(byte[] b) throws IOException {
        return serverIn.read(b);
    }

    /**
     * disconnects the TCP/IP session. The instance is ready for
     * a reconnection. There is no need to create a new ISOChannel<br>
     * @exception IOException
     */
    public void disconnect() throws IOException {
        LogEvent evt = new LogEvent(this, "disconnect");
        if (serverSocket != null) evt.addMessage("local port " + serverSocket.getLocalPort() + " remote host " + serverSocket.getInetAddress()); else evt.addMessage(host + ":" + port);
        try {
            usable = false;
            setChanged();
            notifyObservers();
            closeSocket();
            if (serverIn != null) {
                try {
                    serverIn.close();
                } catch (IOException ex) {
                    evt.addMessage(ex);
                }
                serverIn = null;
            }
            if (serverOut != null) {
                try {
                    serverOut.close();
                } catch (IOException ex) {
                    evt.addMessage(ex);
                }
                serverOut = null;
            }
        } catch (IOException e) {
            evt.addMessage(e);
            Logger.log(evt);
            throw e;
        }
        socket = null;
    }

    /**
     * Issues a disconnect followed by a connect
     * @exception IOException
     */
    public void reconnect() throws IOException {
        disconnect();
        connect();
    }

    public void setLogger(Logger logger, String realm) {
        this.logger = logger;
        this.realm = realm;
        if (originalRealm == null) originalRealm = realm;
    }

    public String getRealm() {
        return realm;
    }

    public Logger getLogger() {
        return logger;
    }

    public String getOriginalRealm() {
        return originalRealm == null ? this.getClass().getName() : originalRealm;
    }

    /**
     * associates this ISOChannel with a name using NameRegistrar
     * @param name name to register
     * @see NameRegistrar
     */
    public void setName(String name) {
        this.name = name;
        NameRegistrar.register("channel." + name, this);
    }

    /**
     * @return this ISOChannel's name ("" if no name was set)
     */
    public String getName() {
        return this.name;
    }

    /**
     * @param filter filter to add
     * @param direction ISOMsg.INCOMING, ISOMsg.OUTGOING, 0 for both
     */
    @SuppressWarnings("unchecked")
    public void addFilter(ISOFilter filter, int direction) {
        switch(direction) {
            case ISOMsg.INCOMING:
                incomingFilters.add(filter);
                break;
            case ISOMsg.OUTGOING:
                outgoingFilters.add(filter);
                break;
            case 0:
                incomingFilters.add(filter);
                outgoingFilters.add(filter);
                break;
        }
    }

    /**
     * @param filter incoming filter to add
     */
    public void addIncomingFilter(ISOFilter filter) {
        addFilter(filter, ISOMsg.INCOMING);
    }

    /**
     * @param filter outgoing filter to add
     */
    public void addOutgoingFilter(ISOFilter filter) {
        addFilter(filter, ISOMsg.OUTGOING);
    }

    /**
     * @param filter filter to add (both directions, incoming/outgoing)
     */
    public void addFilter(ISOFilter filter) {
        addFilter(filter, 0);
    }

    /**
     * @param filter filter to remove
     * @param direction ISOMsg.INCOMING, ISOMsg.OUTGOING, 0 for both
     */
    public void removeFilter(ISOFilter filter, int direction) {
        switch(direction) {
            case ISOMsg.INCOMING:
                incomingFilters.remove(filter);
                break;
            case ISOMsg.OUTGOING:
                outgoingFilters.remove(filter);
                break;
            case 0:
                incomingFilters.remove(filter);
                outgoingFilters.remove(filter);
                break;
        }
    }

    /**
     * @param filter filter to remove (both directions)
     */
    public void removeFilter(ISOFilter filter) {
        removeFilter(filter, 0);
    }

    /**
     * @param filter incoming filter to remove
     */
    public void removeIncomingFilter(ISOFilter filter) {
        removeFilter(filter, ISOMsg.INCOMING);
    }

    /**
     * @param filter outgoing filter to remove
     */
    public void removeOutgoingFilter(ISOFilter filter) {
        removeFilter(filter, ISOMsg.OUTGOING);
    }

    protected ISOMsg applyOutgoingFilters(ISOMsg m, LogEvent evt) throws VetoException {
        Iterator iter = outgoingFilters.iterator();
        while (iter.hasNext()) m = ((ISOFilter) iter.next()).filter(this, m, evt);
        return m;
    }

    protected ISOMsg applyIncomingFilters(ISOMsg m, LogEvent evt) throws VetoException {
        return applyIncomingFilters(m, null, null, evt);
    }

    protected ISOMsg applyIncomingFilters(ISOMsg m, byte[] header, byte[] image, LogEvent evt) throws VetoException {
        Iterator iter = incomingFilters.iterator();
        while (iter.hasNext()) {
            ISOFilter f = (ISOFilter) iter.next();
            if (image != null && (f instanceof RawIncomingFilter)) m = ((RawIncomingFilter) f).filter(this, m, header, image, evt); else m = f.filter(this, m, evt);
        }
        return m;
    }

    protected void unpack(ISOMsg m, byte[] b) throws ISOException {
        m.unpack(b);
    }

    /**
    * Implements Configurable<br>
    * Properties:<br>
    * <ul>
    * <li>host - destination host (if ClientChannel)
    * <li>port - port number      (if ClientChannel)
    * <li>local-iface - local interfase to use (if ClientChannel)
    * <li>local-port - local port to bind (if ClientChannel)
    * </ul>
    * (host not present indicates a ServerChannel)
    *
    * @param cfg Configuration
    * @throws ConfigurationException
    */
    public void setConfiguration(Configuration cfg) throws ConfigurationException {
        this.cfg = cfg;
        String h = cfg.get("host");
        int port = cfg.getInt("port");
        maxPacketLength = cfg.getInt("max-packet-length", 100000);
        if (h != null && h.length() > 0) {
            if (port == 0) throw new ConfigurationException("invalid port for host '" + h + "'");
            setHost(h, port);
            setLocalAddress(cfg.get("local-iface", null), cfg.getInt("local-port"));
            String[] altHosts = cfg.getAll("alternate-host");
            int[] altPorts = cfg.getInts("alternate-port");
            hosts = new String[altHosts.length + 1];
            ports = new int[altPorts.length + 1];
            if (hosts.length != ports.length) {
                throw new ConfigurationException("alternate host/port misconfiguration");
            }
            hosts[0] = host;
            ports[0] = port;
            System.arraycopy(altHosts, 0, hosts, 1, altHosts.length);
            System.arraycopy(altPorts, 0, ports, 1, altPorts.length);
        }
        setOverrideHeader(cfg.getBoolean("override-header", false));
        keepAlive = cfg.getBoolean("keep-alive", false);
        if (socketFactory != this && socketFactory instanceof Configurable) ((Configurable) socketFactory).setConfiguration(cfg);
        try {
            setTimeout(cfg.getInt("timeout"));
            connectTimeout = cfg.getInt("connect-timeout", timeout);
        } catch (SocketException e) {
            throw new ConfigurationException(e);
        }
    }

    public Configuration getConfiguration() {
        return cfg;
    }

    public Collection getIncomingFilters() {
        return incomingFilters;
    }

    public Collection getOutgoingFilters() {
        return outgoingFilters;
    }

    public void setIncomingFilters(Collection filters) {
        incomingFilters = new Vector(filters);
    }

    public void setOutgoingFilters(Collection filters) {
        outgoingFilters = new Vector(filters);
    }

    public void setHeader(byte[] header) {
        this.header = header;
    }

    public void setHeader(String header) {
        setHeader(header.getBytes());
    }

    public byte[] getHeader() {
        return header;
    }

    public void setOverrideHeader(boolean overrideHeader) {
        this.overrideHeader = overrideHeader;
    }

    public boolean isOverrideHeader() {
        return overrideHeader;
    }

    /**
     * @param name the Channel's name (without the "channel." prefix)
     * @return ISOChannel instance with given name.
     * @throws NameRegistrar.NotFoundException;
     * @see NameRegistrar
     */
    public static ISOChannel getChannel(String name) throws NameRegistrar.NotFoundException {
        return (ISOChannel) NameRegistrar.get("channel." + name);
    }

    /**
    * Gets the ISOClientSocketFactory (may be null)
    * @see     ISOClientSocketFactory
    * @since 1.3.3    \
    * @return ISOClientSocketFactory
    */
    public ISOClientSocketFactory getSocketFactory() {
        return socketFactory;
    }

    /**
    * Sets the specified Socket Factory to create sockets
    * @param         socketFactory the ISOClientSocketFactory
    * @see           ISOClientSocketFactory
    * @since 1.3.3
    */
    public void setSocketFactory(ISOClientSocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    public int getMaxPacketLength() {
        return maxPacketLength;
    }

    public void setMaxPacketLength(int maxPacketLength) {
        this.maxPacketLength = maxPacketLength;
    }

    private void closeSocket() throws IOException {
        if (socket != null) {
            try {
                socket.setSoLinger(true, 5);
                socket.shutdownOutput();
            } catch (SocketException e) {
            }
            socket.close();
            socket = null;
        }
    }

    public Object clone() {
        try {
            BaseChannel channel = (BaseChannel) super.clone();
            channel.cnt = cnt.clone();
            channel.serverInLock = new Object();
            channel.serverOutLock = new Object();
            channel.serverIn = null;
            channel.serverOut = null;
            channel.usable = false;
            channel.socket = null;
            return channel;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }
}
