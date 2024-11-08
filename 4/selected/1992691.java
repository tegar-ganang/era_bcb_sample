package spread;

import java.net.*;
import java.io.*;
import java.util.*;

/**
 * A SpreadConnection object is used to establish a connection to a spread daemon.
 * To connect to a spread daemon, first create a new SpreadConnection object, and then
 * call {@link SpreadConnection#connect(InetAddress, int, String, boolean, boolean)}:
 * <p><blockquote><pre>
 * SpreadConnection connection = new SpreadConnection();
 * connection.connect(null, 0, "name", false, false);
 * </pre></blockquote><p>
 * The only methods that can be called before
 * {@link SpreadConnection#connect(InetAddress, int, String, boolean, boolean)} are the add
 * ({@link SpreadConnection#add(BasicMessageListener)}, {@link SpreadConnection#add(AdvancedMessageListener)})
 * and remove ({@link SpreadConnection#remove(BasicMessageListener)}, {@link SpreadConnection#remove(AdvancedMessageListener)})
 * methods.  If any other methods are called, a SpreadException is thrown, except for
 * {@link SpreadConnection#getPrivateGroup()}, which returns null.
 * <p>
 * To disconnect from the daemon, call {@link SpreadConnection#disconnect()}:
 * <p><blockquote><pre>
 * connection.disconnect();
 * </pre></blockquote><p>
 * To send a message on this connection, call {@link SpreadConnection#multicast(SpreadMessage)}:
 * <p><blockquote><pre>
 * connection.multicast(message);
 * </pre></blockquote><p>
 * To receive a message sent to this connection, call {@link SpreadConnection#receive()}:
 * <p><blockquote><pre>
 * SpreadMessage message = connection.receive();
 * </pre></blockquote><p>
 */
public class SpreadConnection {

    private static final int DEFAULT_SPREAD_PORT = 4803;

    private static final int MAX_PRIVATE_NAME = 10;

    private static final int MAX_MESSAGE_LENGTH = 140000;

    protected static final int MAX_GROUP_NAME = 32;

    private static final int SP_MAJOR_VERSION = 4;

    private static final int SP_MINOR_VERSION = 0;

    private static final int SP_PATCH_VERSION = 0;

    private static final String DEFAULT_AUTH_NAME = "NULL";

    private static final String DEFAULT_AUTHCLASS_NAME = "spread.NULLAuth";

    private static final int MAX_AUTH_NAME = 30;

    private static final int MAX_AUTH_METHODS = 3;

    private static final int ACCEPT_SESSION = 1;

    private static final int ENDIAN_TYPE = 0x80000080;

    private boolean connected;

    private Boolean rsynchro;

    private Boolean wsynchro;

    private Boolean listenersynchro;

    private boolean callingListeners;

    private Listener listener;

    protected Vector basicListeners;

    protected Vector advancedListeners;

    private InetAddress address;

    private int port;

    private boolean priority;

    private boolean groupMembership;

    private String authName;

    private String authClassName;

    private Object authObj;

    private java.lang.reflect.Method authMethodAuthenticate;

    private Socket socket;

    private InputStream socketInput;

    private OutputStream socketOutput;

    private SpreadGroup group;

    private Vector listenerBuffer;

    private static final Object BUFFER_DISCONNECT = new Object();

    private static final Object BUFFER_ADD_BASIC = new Object();

    private static final Object BUFFER_ADD_ADVANCED = new Object();

    private static final Object BUFFER_REMOVE_BASIC = new Object();

    private static final Object BUFFER_REMOVE_ADVANCED = new Object();

    private static boolean sameEndian(int i) {
        return ((i & ENDIAN_TYPE) == 0);
    }

    private static int clearEndian(int i) {
        return (i & ~ENDIAN_TYPE);
    }

    protected static int flip(int i) {
        return (((i >> 24) & 0x000000ff) | ((i >> 8) & 0x0000ff00) | ((i << 8) & 0x00ff0000) | ((i << 24) & 0xff000000));
    }

    private static short flip(short s) {
        return ((short) (((s >> 8) & 0x00ff) | ((s << 8) & 0xff00)));
    }

    private static void toBytes(SpreadGroup group, byte buffer[], int bufferIndex) {
        byte name[];
        try {
            name = group.toString().getBytes("ISO8859_1");
        } catch (UnsupportedEncodingException e) {
            name = new byte[0];
        }
        int len = name.length;
        if (len > MAX_GROUP_NAME) len = MAX_GROUP_NAME;
        System.arraycopy(name, 0, buffer, bufferIndex, len);
        for (; len < MAX_GROUP_NAME; len++) buffer[bufferIndex + len] = 0;
    }

    private static void toBytes(int i, byte buffer[], int bufferIndex) {
        buffer[bufferIndex++] = (byte) ((i >> 24) & 0xFF);
        buffer[bufferIndex++] = (byte) ((i >> 16) & 0xFF);
        buffer[bufferIndex++] = (byte) ((i >> 8) & 0xFF);
        buffer[bufferIndex++] = (byte) ((i) & 0xFF);
    }

    protected static int toInt(byte buffer[], int bufferIndex) {
        int i0 = (buffer[bufferIndex++] & 0xFF);
        int i1 = (buffer[bufferIndex++] & 0xFF);
        int i2 = (buffer[bufferIndex++] & 0xFF);
        int i3 = (buffer[bufferIndex++] & 0xFF);
        return ((i0 << 24) | (i1 << 16) | (i2 << 8) | (i3));
    }

    private void readBytesFromSocket(byte buffer[], String bufferTypeString) throws SpreadException {
        int byteIndex;
        int rcode;
        try {
            for (byteIndex = 0; byteIndex < buffer.length; byteIndex += rcode) {
                rcode = socketInput.read(buffer, byteIndex, buffer.length - byteIndex);
                if (rcode == -1) {
                    throw new SpreadException("Connection closed while reading " + bufferTypeString);
                }
            }
        } catch (InterruptedIOException e) {
            throw new SpreadException("readBytesFromSocket(): InterruptedIOException " + e);
        } catch (IOException e) {
            throw new SpreadException("readBytesFromSocket(): read() " + e);
        }
    }

    protected SpreadGroup toGroup(byte buffer[], int bufferIndex) {
        try {
            for (int end = bufferIndex; end < buffer.length; end++) {
                if (buffer[end] == 0) {
                    String name = new String(buffer, bufferIndex, end - bufferIndex, "ISO8859_1");
                    return new SpreadGroup(this, name);
                }
            }
        } catch (UnsupportedEncodingException e) {
        }
        return null;
    }

    private void setBufferSizes() throws SpreadException {
    }

    private void sendConnect(String privateName) throws SpreadException {
        int len = (privateName == null ? 0 : privateName.length());
        if (len > MAX_PRIVATE_NAME) {
            privateName = privateName.substring(0, MAX_PRIVATE_NAME);
            len = MAX_PRIVATE_NAME;
        }
        byte buffer[] = new byte[len + 5];
        buffer[0] = (byte) SP_MAJOR_VERSION;
        buffer[1] = (byte) SP_MINOR_VERSION;
        buffer[2] = (byte) SP_PATCH_VERSION;
        buffer[3] = 0;
        if (groupMembership) {
            buffer[3] |= 0x01;
        }
        if (priority) {
            buffer[3] |= 0x10;
        }
        buffer[4] = (byte) len;
        if (len > 0) {
            byte nameBytes[] = privateName.getBytes();
            for (int src = 0, dest = 5; src < len; src++, dest++) {
                buffer[dest] = nameBytes[src];
            }
        }
        try {
            socketOutput.write(buffer);
        } catch (IOException e) {
            throw new SpreadException("write(): " + e);
        }
    }

    private void readAuthMethods() throws SpreadException {
        int len;
        try {
            len = socketInput.read();
        } catch (IOException e) {
            throw new SpreadException("read(): " + e);
        }
        if (len == -1) {
            throw new SpreadException("Connection closed during connect attempt to read authlen");
        }
        if (len >= 128) {
            throw new SpreadException("Connection attempt rejected=" + (0xffffff00 | len));
        }
        byte buffer[] = new byte[len];
        readBytesFromSocket(buffer, "authname");
    }

    private void sendAuthMethod() throws SpreadException {
        int len = authName.length();
        byte buffer[] = new byte[MAX_AUTH_NAME * MAX_AUTH_METHODS];
        try {
            System.arraycopy(authName.getBytes("ISO8859_1"), 0, buffer, 0, len);
        } catch (UnsupportedEncodingException e) {
        }
        for (; len < (MAX_AUTH_NAME * MAX_AUTH_METHODS); len++) buffer[len] = 0;
        try {
            socketOutput.write(buffer);
        } catch (IOException e) {
            throw new SpreadException("write(): " + e);
        }
    }

    private void instantiateAuthMethod() throws SpreadException {
        Class authclass;
        try {
            authclass = Class.forName(authClassName);
        } catch (ClassNotFoundException e) {
            throw new SpreadException("class " + authClassName + " not found.\n");
        }
        try {
            authObj = authclass.newInstance();
        } catch (Exception e) {
            throw new SpreadException("class " + authClassName + " error getting instance.\n" + e);
        }
        try {
            authMethodAuthenticate = authclass.getMethod("authenticate", new Class[] {});
        } catch (NoSuchMethodException e) {
            System.out.println("Failed to find auth method authenticate()");
            System.exit(1);
        } catch (SecurityException e) {
            System.out.println("security exception for method authenticate()");
            System.exit(1);
        }
    }

    private void checkAccept() throws SpreadException {
        int accepted;
        try {
            accepted = socketInput.read();
        } catch (IOException e) {
            throw new SpreadException("read(): " + e);
        }
        if (accepted == -1) {
            throw new SpreadException("Connection closed during connect attempt");
        }
        if (accepted != ACCEPT_SESSION) {
            throw new SpreadException("Connection attempt rejected=" + (0xffffff00 | accepted));
        }
    }

    private void checkVersion() throws SpreadException {
        int majorVersion;
        try {
            majorVersion = socketInput.read();
        } catch (IOException e) {
            throw new SpreadException("read(): " + e);
        }
        int minorVersion;
        try {
            minorVersion = socketInput.read();
        } catch (IOException e) {
            throw new SpreadException("read(): " + e);
        }
        int patchVersion;
        try {
            patchVersion = socketInput.read();
        } catch (IOException e) {
            throw new SpreadException("read(): " + e);
        }
        if ((majorVersion == -1) || (minorVersion == -1) || (patchVersion == -1)) {
            throw new SpreadException("Connection closed during connect attempt");
        }
        int version = ((majorVersion * 10000) + (minorVersion * 100) + patchVersion);
        if (version < 30100) {
            throw new SpreadException("Old version " + majorVersion + "." + minorVersion + "." + patchVersion + " not supported");
        }
        if ((version < 30800) && (priority)) {
            throw new SpreadException("Old version " + majorVersion + "." + minorVersion + "." + patchVersion + " does not support priority");
        }
    }

    private void readGroup() throws SpreadException {
        int len;
        try {
            len = socketInput.read();
        } catch (IOException e) {
            throw new SpreadException("read(): " + e);
        }
        if (len == -1) {
            throw new SpreadException("Connection closed during connect attempt");
        }
        byte buffer[] = new byte[len];
        readBytesFromSocket(buffer, "group name");
        group = new SpreadGroup(this, new String(buffer));
    }

    /**
	 * Initializes a new SpreadConnection object.  To connect to a daemon with this
	 * object, use {@link SpreadConnection#connect(InetAddress, int, String, boolean, boolean)}.
	 * 
	 * @see  SpreadConnection#connect(InetAddress, int, String, boolean, boolean)
	 */
    public SpreadConnection() {
        connected = false;
        rsynchro = new Boolean(false);
        wsynchro = new Boolean(false);
        listenersynchro = new Boolean(false);
        basicListeners = new Vector();
        advancedListeners = new Vector();
        listenerBuffer = new Vector();
        authName = DEFAULT_AUTH_NAME;
        authClassName = DEFAULT_AUTHCLASS_NAME;
    }

    /**
         * Sets the authentication name and class string for the client side authentication method.
         * An authentication method can only be registered before connect is called. 
         * The authentication method registered will then be used whenever
         * {@link SpreadConnection#connect(InetAddress, int, String, boolean, boolean)} is called.
         *
         * @param  authName  the short official "name" of the method begin registered.
         * @param  authClassName  the complete class name for the method (including package)
         * @throws SpreadException if the connection is already established
         */
    public synchronized void registerAuthentication(String authName, String authClassName) throws SpreadException {
        if (connected == true) {
            throw new SpreadException("Already connected.");
        }
        this.authClassName = authClassName;
        try {
            this.authName = authName.substring(0, MAX_AUTH_NAME);
        } catch (IndexOutOfBoundsException e) {
            this.authName = authName;
        }
    }

    /**
	 * Establishes a connection to a spread daemon.  Groups can be joined, and messages can be
	 * sent or received once the connection has been established.
	 * 
	 * @param  address  the daemon's address, or null to connect to the localhost
	 * @param  port  the daemon's port, or 0 for the default port (4803)
	 * @param  privateName  the private name to use for this connection
	 * @param  priority  if true, this is a priority connection
	 * @param  groupMembership  if true, membership messages will be received on this connection
	 * @throws SpreadException  if the connection cannot be established
	 * @see  SpreadConnection#disconnect()
	 */
    public synchronized void connect(InetAddress address, int port, String privateName, boolean priority, boolean groupMembership) throws SpreadException {
        if (connected == true) {
            throw new SpreadException("Already connected.");
        }
        try {
            new String("ASCII/ISO8859_1 encoding test").getBytes("ISO8859_1");
        } catch (UnsupportedEncodingException e) {
            throw new SpreadException("ISO8859_1 encoding is not supported.");
        }
        this.address = address;
        this.port = port;
        this.priority = priority;
        this.groupMembership = groupMembership;
        if (address == null) {
            try {
                address = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                throw new SpreadException("Error getting local host");
            }
        }
        if (port == 0) {
            port = DEFAULT_SPREAD_PORT;
        }
        if ((port < 0) || (port > (32 * 1024))) {
            throw new SpreadException("Bad port (" + port + ").");
        }
        try {
            socket = new Socket(address, port);
        } catch (IOException e) {
            throw new SpreadException("Socket(): " + e);
        }
        setBufferSizes();
        try {
            socketInput = socket.getInputStream();
            socketOutput = socket.getOutputStream();
        } catch (IOException e) {
            throw new SpreadException("getInput/OutputStream(): " + e);
        }
        sendConnect(privateName);
        readAuthMethods();
        sendAuthMethod();
        try {
            instantiateAuthMethod();
        } catch (SpreadException e) {
            System.out.println("Failed to create authMethod instance" + e.toString());
            System.exit(1);
        }
        try {
            authMethodAuthenticate.invoke(authObj, new Object[] {});
        } catch (IllegalAccessException e) {
            System.out.println("error calling authenticate" + e.toString());
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.out.println("error calling authenticate" + e.toString());
            System.exit(1);
        } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable thr = e.getTargetException();
            if (thr.getClass().equals(SpreadException.class)) {
                throw new SpreadException("Connection Rejected: Authentication failed");
            }
        }
        checkAccept();
        checkVersion();
        readGroup();
        connected = true;
        if ((basicListeners.size() != 0) || (advancedListeners.size() != 0)) {
            startListener();
        }
    }

    /**
	 * Disconnects the connection to the daemon.  Nothing else should be done with this connection
	 * after disconnecting it.
	 * 
	 * @throws  SpreadException  if there is no connection or there is an error disconnecting
	 * @see  SpreadConnection#connect(InetAddress, int, String, boolean, boolean)
	 */
    public synchronized void disconnect() throws SpreadException {
        if (connected == false) {
            throw new SpreadException("Not connected.");
        }
        if (callingListeners) {
            listenerBuffer.addElement(BUFFER_DISCONNECT);
            return;
        }
        SpreadMessage killMessage = new SpreadMessage();
        killMessage.addGroup(group);
        killMessage.setServiceType(SpreadMessage.KILL_MESS);
        multicast(killMessage);
        if (listener != null) {
            stopListener();
        }
        try {
            socket.close();
        } catch (IOException e) {
            throw new SpreadException("close(): " + e);
        }
        connected = false;
    }

    /**
	 * Gets the private group for this connection.
	 * 
	 * @return  the SpreadGroup representing this connection's private group, or null if there is no connection
	 */
    public SpreadGroup getPrivateGroup() {
        if (connected == false) {
            return null;
        }
        return group;
    }

    /**
	 * Receives the next message waiting on this connection.  If there are no messages
	 * waiting, the call will block until a message is ready to be received.
	 * 
	 * @return  the message that has just been received
	 * @throws  SpreadException  if there is no connection or there is any error reading a new message
	 */
    public SpreadMessage receive() throws SpreadException, InterruptedIOException {
        synchronized (rsynchro) {
            if ((basicListeners.isEmpty() == false) || (advancedListeners.isEmpty() == false)) {
                throw new SpreadException("Tried to receive while there are listeners");
            }
            return internal_receive();
        }
    }

    private SpreadMessage internal_receive() throws SpreadException, InterruptedIOException {
        if (connected == false) {
            throw new SpreadException("Not connected.");
        }
        byte header[] = new byte[MAX_GROUP_NAME + 16];
        int headerIndex;
        int rcode;
        try {
            for (headerIndex = 0; headerIndex < header.length; headerIndex += rcode) {
                rcode = socketInput.read(header, headerIndex, header.length - headerIndex);
                if (rcode == -1) {
                    throw new SpreadException("Connection closed while reading header");
                }
            }
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            throw new SpreadException("read(): " + e);
        }
        headerIndex = 0;
        int serviceType = toInt(header, headerIndex);
        headerIndex += 4;
        SpreadGroup sender = toGroup(header, headerIndex);
        headerIndex += MAX_GROUP_NAME;
        int numGroups = toInt(header, headerIndex);
        headerIndex += 4;
        int hint = toInt(header, headerIndex);
        headerIndex += 4;
        int dataLen = toInt(header, headerIndex);
        headerIndex += 4;
        boolean daemonEndianMismatch;
        if (sameEndian(serviceType) == false) {
            serviceType = flip(serviceType);
            numGroups = flip(numGroups);
            dataLen = flip(dataLen);
            daemonEndianMismatch = true;
        } else {
            daemonEndianMismatch = false;
        }
        if ((numGroups < 0) || (dataLen < 0)) {
            throw new SpreadException("Illegal Message: Message Dropped");
        }
        boolean endianMismatch;
        short type;
        if (SpreadMessage.isRegular(serviceType) || SpreadMessage.isReject(serviceType)) {
            if (sameEndian(hint) == false) {
                hint = flip(hint);
                endianMismatch = true;
            } else {
                endianMismatch = false;
            }
            hint = clearEndian(hint);
            hint >>= 8;
            hint &= 0x0000FFFF;
            type = (short) hint;
        } else {
            type = -1;
            endianMismatch = false;
        }
        if (SpreadMessage.isReject(serviceType)) {
            byte oldtypeBuffer[] = new byte[4];
            try {
                for (int oldtypeIndex = 0; oldtypeIndex < oldtypeBuffer.length; ) {
                    rcode = socketInput.read(oldtypeBuffer, oldtypeIndex, oldtypeBuffer.length - oldtypeIndex);
                    if (rcode == -1) {
                        throw new SpreadException("Connection closed while reading groups");
                    }
                    oldtypeIndex += rcode;
                }
            } catch (InterruptedIOException e) {
                throw e;
            } catch (IOException e) {
                throw new SpreadException("read(): " + e);
            }
            int oldType = toInt(oldtypeBuffer, 0);
            if (sameEndian(serviceType) == false) oldType = flip(oldType);
            serviceType = (SpreadMessage.REJECT_MESS | oldType);
        }
        byte buffer[] = new byte[numGroups * MAX_GROUP_NAME];
        try {
            for (int bufferIndex = 0; bufferIndex < buffer.length; ) {
                rcode = socketInput.read(buffer, bufferIndex, buffer.length - bufferIndex);
                if (rcode == -1) {
                    throw new SpreadException("Connection closed while reading groups");
                }
                bufferIndex += rcode;
            }
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            throw new SpreadException("read(): " + e);
        }
        serviceType = clearEndian(serviceType);
        Vector groups = new Vector(numGroups);
        for (int bufferIndex = 0; bufferIndex < buffer.length; bufferIndex += MAX_GROUP_NAME) {
            groups.addElement(toGroup(buffer, bufferIndex));
        }
        byte data[] = new byte[dataLen];
        try {
            for (int dataIndex = 0; dataIndex < dataLen; ) {
                rcode = socketInput.read(data, dataIndex, dataLen - dataIndex);
                if (rcode == -1) {
                    throw new SpreadException("Connection close while reading data");
                }
                dataIndex += rcode;
            }
        } catch (InterruptedIOException e) {
            throw e;
        } catch (IOException e) {
            throw new SpreadException("read():" + e);
        }
        MembershipInfo membershipInfo;
        if (SpreadMessage.isMembership(serviceType)) {
            membershipInfo = new MembershipInfo(this, serviceType, groups, sender, data, daemonEndianMismatch);
            if (membershipInfo.isRegularMembership()) {
                type = (short) groups.indexOf(group);
            }
        } else {
            membershipInfo = null;
        }
        return new SpreadMessage(serviceType, groups, sender, data, type, endianMismatch, membershipInfo);
    }

    /**
	 * Receives <code>numMessages</code> messages on the connection and returns them in an array.
	 * If there are not <code>numMessages</code> messages waiting, the call will block until there are
	 * enough messages available.
	 * 
	 * @param  numMessages  the number of messages to receive
	 * @return an array of messages
	 * @throws  SpreadException  if there is no connection or if there is any error reading the messages
	 */
    public SpreadMessage[] receive(int numMessages) throws SpreadException, InterruptedIOException {
        SpreadMessage[] messages = new SpreadMessage[numMessages];
        synchronized (rsynchro) {
            if ((basicListeners.isEmpty() == false) || (advancedListeners.isEmpty() == false)) {
                throw new SpreadException("Tried to receive while there are listeners");
            }
            for (int i = 0; i < numMessages; i++) {
                messages[i] = internal_receive();
            }
        }
        return messages;
    }

    /**
	 * Returns true if there are any messages waiting on this connection.
	 * 
	 * @return true if there is at least one message that can be received
	 * @throws  SpreadException  if there is no connection or if there is an error checking for messages
	 */
    public boolean poll() throws SpreadException {
        if (connected == false) {
            throw new SpreadException("Not connected.");
        }
        try {
            if (socketInput.available() == 0) {
                return false;
            }
        } catch (IOException e) {
            throw new SpreadException("available(): " + e);
        }
        return true;
    }

    private void startListener() {
        listener = new Listener(this);
        listener.start();
    }

    /**
	 * Adds the BasicMessageListener to this connection.  If there are no other listeners, this call will
	 * start a thread to listen for new messages.  From the time this function is called until
	 * this listener is removed, {@link BasicMessageListener#messageReceived(SpreadMessage)} will
	 * be called every time a message is received.
	 * 
	 * @param  listener  a BasicMessageListener to add to this connection
	 * @see  SpreadConnection#remove(BasicMessageListener)
	 */
    public void add(BasicMessageListener listener) {
        synchronized (listenersynchro) {
            if (callingListeners) {
                listenerBuffer.addElement(BUFFER_ADD_BASIC);
                listenerBuffer.addElement(listener);
                return;
            }
            basicListeners.addElement(listener);
            if (connected == true) {
                if (this.listener == null) {
                    startListener();
                }
            }
        }
    }

    /**
	 * Adds the AdvancedMessageListener to this connection.  If there are no other listeners, this call will
	 * start a thread to listen for new messages.  From the time this function is called until
	 * this listener is removed, {@link AdvancedMessageListener#regularMessageReceived(SpreadMessage)} will
	 * be called every time a regular message is received, and 
	 * {@link AdvancedMessageListener#membershipMessageReceived(SpreadMessage)} will be called every time
	 * a membership message is received.
	 * 
	 * @param  listener an AdvancedMessageListener to add to this connection
	 * @see  SpreadConnection#remove(AdvancedMessageListener)
	 */
    public void add(AdvancedMessageListener listener) {
        synchronized (listenersynchro) {
            if (callingListeners) {
                listenerBuffer.addElement(BUFFER_ADD_ADVANCED);
                listenerBuffer.addElement(listener);
                return;
            }
            advancedListeners.addElement(listener);
            if (connected == true) {
                if (this.listener == null) {
                    startListener();
                }
            }
        }
    }

    private void stopListener() {
        listener.signal = true;
        try {
            listener.join();
        } catch (InterruptedException e) {
        }
        listener = null;
    }

    /**
	 * Removes the BasicMessageListener from this connection.  If this is the only listener on this
	 * connection, the listener thread will be stopped.
	 * 
	 * @param  listener  the listener to remove
	 * @see  SpreadConnection#add(BasicMessageListener)
	 */
    public void remove(BasicMessageListener listener) {
        synchronized (listenersynchro) {
            if (callingListeners) {
                listenerBuffer.addElement(BUFFER_REMOVE_BASIC);
                listenerBuffer.addElement(listener);
                return;
            }
            basicListeners.removeElement(listener);
            if (connected == true) {
                if ((basicListeners.size() == 0) && (advancedListeners.size() == 0)) {
                    stopListener();
                }
            }
        }
    }

    /**
	 * Removes the AdvancedMessageListener from this connection.  If this is the only listener on this
	 * connection, the listener thread will be stopped.
	 * 
	 * @param  listener  the listener to remove
	 * @see SpreadConnection#add(AdvancedMessageListener)
	 */
    public void remove(AdvancedMessageListener listener) {
        synchronized (listenersynchro) {
            if (callingListeners) {
                listenerBuffer.addElement(BUFFER_REMOVE_ADVANCED);
                listenerBuffer.addElement(listener);
                return;
            }
            advancedListeners.removeElement(listener);
            if (connected == true) {
                if ((basicListeners.size() == 0) && (advancedListeners.size() == 0)) {
                    stopListener();
                }
            }
        }
    }

    private class Listener extends Thread {

        private SpreadConnection connection;

        protected boolean signal;

        public Listener(SpreadConnection connection) {
            this.connection = connection;
            this.signal = false;
            this.setDaemon(true);
        }

        public void run() {
            SpreadMessage message;
            BasicMessageListener basicListener;
            AdvancedMessageListener advancedListener;
            Object command;
            int previous_socket_timeout = 100;
            try {
                try {
                    previous_socket_timeout = connection.socket.getSoTimeout();
                    connection.socket.setSoTimeout(100);
                } catch (SocketException e) {
                    System.out.println("socket error setting timeout" + e.toString());
                }
                while (true) {
                    synchronized (connection) {
                        if (signal == true) {
                            System.out.println("LISTENER: told to exit so returning");
                            try {
                                connection.socket.setSoTimeout(previous_socket_timeout);
                            } catch (SocketException e) {
                                System.out.println("socket error setting timeout" + e.toString());
                            }
                            return;
                        }
                        try {
                            synchronized (rsynchro) {
                                message = connection.internal_receive();
                            }
                            callingListeners = true;
                            for (int i = 0; i < basicListeners.size(); i++) {
                                basicListener = (BasicMessageListener) basicListeners.elementAt(i);
                                basicListener.messageReceived(message);
                            }
                            for (int i = 0; i < advancedListeners.size(); i++) {
                                advancedListener = (AdvancedMessageListener) advancedListeners.elementAt(i);
                                if (message.isRegular()) {
                                    advancedListener.regularMessageReceived(message);
                                } else {
                                    advancedListener.membershipMessageReceived(message);
                                }
                            }
                            callingListeners = false;
                        } catch (InterruptedIOException e) {
                        }
                        while (listenerBuffer.isEmpty() == false) {
                            command = listenerBuffer.firstElement();
                            listenerBuffer.removeElementAt(0);
                            if (command == BUFFER_DISCONNECT) {
                                connection.disconnect();
                                listenerBuffer.removeAllElements();
                            } else if (command == BUFFER_ADD_BASIC) {
                                basicListener = (BasicMessageListener) listenerBuffer.firstElement();
                                connection.add(basicListener);
                                listenerBuffer.removeElementAt(0);
                            } else if (command == BUFFER_ADD_ADVANCED) {
                                advancedListener = (AdvancedMessageListener) listenerBuffer.firstElement();
                                connection.add(advancedListener);
                                listenerBuffer.removeElementAt(0);
                            } else if (command == BUFFER_REMOVE_BASIC) {
                                basicListener = (BasicMessageListener) listenerBuffer.firstElement();
                                connection.remove(basicListener);
                                listenerBuffer.removeElementAt(0);
                            } else if (command == BUFFER_REMOVE_ADVANCED) {
                                advancedListener = (AdvancedMessageListener) listenerBuffer.firstElement();
                                connection.remove(advancedListener);
                                listenerBuffer.removeElementAt(0);
                            }
                        }
                    }
                    yield();
                }
            } catch (SpreadException e) {
                System.out.println("SpreadException: " + e.toString());
            }
        }
    }

    /**
	 * Multicasts a message.  The message will be sent to all the groups specified in
	 * the message.
	 * 
	 * @param  message  the message to multicast
	 * @throws  SpreadException  if there is no connection or if there is any error sending the message
	 */
    public void multicast(SpreadMessage message) throws SpreadException {
        if (connected == false) {
            throw new SpreadException("Not connected.");
        }
        SpreadGroup groups[] = message.getGroups();
        byte data[] = message.getData();
        int numBytes = 16;
        numBytes += MAX_GROUP_NAME;
        numBytes += (MAX_GROUP_NAME * groups.length);
        if (numBytes + data.length > MAX_MESSAGE_LENGTH) {
            throw new SpreadException("Message is too long for a Spread Message");
        }
        byte buffer[] = new byte[numBytes];
        int bufferIndex = 0;
        toBytes(message.getServiceType(), buffer, bufferIndex);
        bufferIndex += 4;
        toBytes(group, buffer, bufferIndex);
        bufferIndex += MAX_GROUP_NAME;
        toBytes(groups.length, buffer, bufferIndex);
        bufferIndex += 4;
        toBytes(((int) message.getType() << 8) & 0x00FFFF00, buffer, bufferIndex);
        bufferIndex += 4;
        toBytes(data.length, buffer, bufferIndex);
        bufferIndex += 4;
        for (int i = 0; i < groups.length; i++) {
            toBytes(groups[i], buffer, bufferIndex);
            bufferIndex += MAX_GROUP_NAME;
        }
        synchronized (wsynchro) {
            try {
                socketOutput.write(buffer);
                socketOutput.write(data);
            } catch (IOException e) {
                throw new SpreadException("write(): " + e.toString());
            }
        }
    }

    /**
	 * Multicasts an array of messages.  Each message will be sent to all the groups specified in
	 * the message.
	 * 
	 * @param  messages  the messages to multicast
	 * @throws  SpreadException  if there is no connection or if there is any error sending the messages
	 */
    public void multicast(SpreadMessage messages[]) throws SpreadException {
        for (int i = 0; i < messages.length; i++) {
            multicast(messages[i]);
        }
    }
}
