package org.opengts.util;

import java.io.*;
import java.util.*;
import java.net.*;
import java.awt.event.*;
import javax.net.ssl.*;
import javax.net.*;

public class ServerSocketThread extends Thread {

    public static final int PACKET_LEN_ASCII_LINE_TERMINATOR = -1;

    public static final int PACKET_LEN_END_OF_STREAM = -2;

    private int listenPort = 0;

    private int clientPort = -1;

    private DatagramSocket datagramSocket = null;

    private ServerSocket serverSocket = null;

    private java.util.List<ServerSessionThread> clientThreadPool = null;

    private ClientPacketHandler clientPacketHandler = null;

    private Class clientPacketHandlerClass = null;

    private long sessionTimeoutMS = -1L;

    private long idleTimeoutMS = -1L;

    private long packetTimeoutMS = -1L;

    private int lingerTimeoutSec = 4;

    private int maxReadLength = -1;

    private int minReadLength = -1;

    private boolean terminateOnTimeout = true;

    private boolean isTextPackets = true;

    private int lineTerminatorChar[] = new int[] { '\n' };

    private int backspaceChar[] = new int[] { '\b' };

    private int ignoreChar[] = new int[] { '\r' };

    private byte prompt[] = null;

    private int promptIndex = -1;

    private boolean autoPrompt = false;

    private java.util.List<ActionListener> actionListeners = null;

    /**
    *** Constructor
    **/
    private ServerSocketThread() {
        this.clientThreadPool = new Vector<ServerSessionThread>();
        this.actionListeners = new Vector<ActionListener>();
    }

    /**
    *** Constructor for UDP connections
    **/
    public ServerSocketThread(DatagramSocket ds) {
        this();
        this.datagramSocket = ds;
        this.listenPort = (ds != null) ? ds.getLocalPort() : -1;
    }

    /**
    *** Constructor for TCP connections
    *** @param ss  The ServerSocket containing the 'listen' port information
    **/
    public ServerSocketThread(ServerSocket ss) {
        this();
        this.serverSocket = ss;
        this.listenPort = (ss != null) ? ss.getLocalPort() : -1;
    }

    /**
    *** Constructor for TCP connections
    *** @param port  The port on which to listen for incoming connections
    **/
    public ServerSocketThread(int port) throws IOException {
        this(new ServerSocket(port));
        this.listenPort = port;
    }

    /**
    *** Constructor for TCP connections
    *** @param port  The port on which to listen for incoming connections
    *** @param useSSL  True to enable an SSL
    **/
    public ServerSocketThread(int port, boolean useSSL) throws IOException {
        this(useSSL ? SSLServerSocketFactory.getDefault().createServerSocket(port) : ServerSocketFactory.getDefault().createServerSocket(port));
        this.listenPort = port;
    }

    /**
    *** Gets the local port to which this socket is bound
    *** @returns the local port to which this socket is bound
    **/
    public int getLocalPort() {
        return this.listenPort;
    }

    /**
    *** Listens for incoming connections and dispatches them to a handler thread
    **/
    public void run() {
        while (true) {
            ClientSocket clientSocket = null;
            try {
                if (this.serverSocket != null) {
                    clientSocket = new ClientSocket(this.serverSocket.accept());
                } else if (this.datagramSocket != null) {
                    byte b[] = new byte[ServerSocketThread.this.getMaximumPacketLength()];
                    DatagramPacket dp = new DatagramPacket(b, b.length);
                    this.datagramSocket.receive(dp);
                    clientSocket = new ClientSocket(dp);
                } else {
                    Print.logStackTrace("ServerSocketThread has not been properly initialized");
                    break;
                }
            } catch (SocketException se) {
                if (this.serverSocket != null) {
                    int port = this.serverSocket.getLocalPort();
                    if (port <= 0) {
                        port = this.getLocalPort();
                    }
                    String portStr = (port <= 0) ? "?" : String.valueOf(port);
                    Print.logInfo("Shutdown TCP server on port " + portStr);
                } else if (this.datagramSocket != null) {
                    int port = this.datagramSocket.getLocalPort();
                    if (port <= 0) {
                        port = this.getLocalPort();
                    }
                    String portStr = (port <= 0) ? "?" : String.valueOf(port);
                    Print.logInfo("Shutdown UDP server on port " + portStr);
                } else {
                    Print.logInfo("Shutdown must have been called");
                }
                break;
            } catch (IOException ioe) {
                Print.logError("Connection - " + ioe);
                continue;
            }
            String ipAddr;
            try {
                InetAddress inetAddr = clientSocket.getInetAddress();
                ipAddr = (inetAddr != null) ? inetAddr.getHostAddress() : "?";
            } catch (Throwable t) {
                ipAddr = "?";
            }
            boolean foundThread = false;
            for (Iterator i = this.clientThreadPool.iterator(); i.hasNext() && !foundThread; ) {
                ServerSessionThread sst = (ServerSessionThread) i.next();
                foundThread = sst.setClientIfAvailable(clientSocket);
            }
            if (!foundThread) {
                ServerSessionThread sst = new ServerSessionThread(clientSocket);
                this.clientThreadPool.add(sst);
            } else {
            }
        }
    }

    /**
    *** Shuts down the server 
    **/
    public void shutdown() {
        try {
            if (this.serverSocket != null) {
                this.serverSocket.close();
            }
            if (this.datagramSocket != null) {
                this.datagramSocket.close();
            }
            Iterator it = this.clientThreadPool.iterator();
            while (it.hasNext()) {
                ServerSessionThread sst = (ServerSessionThread) it.next();
                if (sst != null) {
                    sst.close();
                }
            }
        } catch (Exception e) {
            Print.logError("Error shutting down ServerSocketThread " + e);
        }
    }

    public void setRemotePort(int remotePort) {
        this.clientPort = remotePort;
    }

    public int getRemotePort() {
        return this.clientPort;
    }

    public boolean hasListeners() {
        return (this.actionListeners.size() > 0);
    }

    public void addActionListener(ActionListener al) {
        if (!this.actionListeners.contains(al)) {
            this.actionListeners.add(al);
        }
    }

    public void removeActionListener(ActionListener al) {
        this.actionListeners.remove(al);
    }

    protected boolean invokeListeners(byte msgBytes[]) throws Exception {
        if (msgBytes != null) {
            String msg = StringTools.toStringValue(msgBytes);
            for (Iterator i = this.actionListeners.iterator(); i.hasNext(); ) {
                Object alObj = i.next();
                if (alObj instanceof ActionListener) {
                    ActionListener al = (ActionListener) i.next();
                    ActionEvent ae = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, msg);
                    al.actionPerformed(ae);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public void setClientPacketHandler(ClientPacketHandler cph) {
        this.clientPacketHandler = cph;
    }

    public void setClientPacketHandlerClass(Class cphc) {
        if ((cphc == null) || ClientPacketHandler.class.isAssignableFrom(cphc)) {
            this.clientPacketHandlerClass = cphc;
            this.clientPacketHandler = null;
        } else {
            throw new ClassCastException("Invalid ClientPacketHandler class");
        }
    }

    public ClientPacketHandler getClientPacketHandler() {
        if (this.clientPacketHandler != null) {
            return this.clientPacketHandler;
        } else if (this.clientPacketHandlerClass != null) {
            try {
                return (ClientPacketHandler) this.clientPacketHandlerClass.newInstance();
            } catch (Throwable t) {
                Print.logException("ClientPacketHandler", t);
                return null;
            }
        } else {
            return null;
        }
    }

    public void setSessionTimeout(long timeoutMS) {
        this.sessionTimeoutMS = timeoutMS;
    }

    public long getSessionTimeout() {
        return this.sessionTimeoutMS;
    }

    public void setIdleTimeout(long timeoutMS) {
        this.idleTimeoutMS = timeoutMS;
    }

    public long getIdleTimeout() {
        return this.idleTimeoutMS;
    }

    public void setPacketTimeout(long timeoutMS) {
        this.packetTimeoutMS = timeoutMS;
    }

    public long getPacketTimeout() {
        return this.packetTimeoutMS;
    }

    public void setTerminateOnTimeout(boolean timeoutQuit) {
        this.terminateOnTimeout = timeoutQuit;
    }

    public boolean getTerminateOnTimeout() {
        return this.terminateOnTimeout;
    }

    public void setLingerTimeoutSec(int timeoutSec) {
        this.lingerTimeoutSec = timeoutSec;
    }

    public int getLingerTimeoutSec() {
        return this.lingerTimeoutSec;
    }

    public void setTextPackets(boolean isText) {
        this.isTextPackets = isText;
        if (!this.isTextPackets()) {
            this.setBackspaceChar(null);
            this.setIgnoreChar(null);
        }
    }

    public boolean isTextPackets() {
        return this.isTextPackets;
    }

    /**
    *** Sets the maximum packet length
    *** @param len  The maximum packet length
    **/
    public void setMaximumPacketLength(int len) {
        this.maxReadLength = len;
    }

    /**
    *** Gets the maximum packet length
    *** @return  The maximum packet length
    **/
    public int getMaximumPacketLength() {
        if (this.maxReadLength > 0) {
            return this.maxReadLength;
        } else if (this.isTextPackets()) {
            return 2048;
        } else {
            return 1024;
        }
    }

    /**
    *** Sets the minimum packet length
    *** @param len  The minimum packet length
    **/
    public void setMinimumPacketLength(int len) {
        this.minReadLength = len;
    }

    /**
    *** Gets the minimum packet length
    *** @return  The minimum packet length
    **/
    public int getMinimumPacketLength() {
        if (this.minReadLength > 0) {
            return this.minReadLength;
        } else if (this.isTextPackets()) {
            return 1;
        } else {
            return this.getMaximumPacketLength();
        }
    }

    public void setLineTerminatorChar(int term) {
        this.setLineTerminatorChar(new int[] { term });
    }

    public void setLineTerminatorChar(int term[]) {
        this.lineTerminatorChar = term;
    }

    public int[] getLineTerminatorChar() {
        return this.lineTerminatorChar;
    }

    public boolean isLineTerminatorChar(int ch) {
        int termChar[] = this.getLineTerminatorChar();
        if ((termChar != null) && (ch >= 0)) {
            for (int i = 0; i < termChar.length; i++) {
                if (termChar[i] == ch) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setBackspaceChar(int bs) {
        this.setBackspaceChar(new int[] { bs });
    }

    public void setBackspaceChar(int bs[]) {
        this.backspaceChar = bs;
    }

    public int[] getBackspaceChar() {
        return this.backspaceChar;
    }

    public boolean isBackspaceChar(int ch) {
        if (this.hasPrompt() && (this.backspaceChar != null) && (ch >= 0)) {
            for (int i = 0; i < this.backspaceChar.length; i++) {
                if (this.backspaceChar[i] == ch) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setIgnoreChar(int bs[]) {
        this.ignoreChar = bs;
    }

    public int[] getIgnoreChar() {
        return this.ignoreChar;
    }

    public boolean isIgnoreChar(int ch) {
        if ((this.ignoreChar != null) && (ch >= 0)) {
            for (int i = 0; i < this.ignoreChar.length; i++) {
                if (this.ignoreChar[i] == ch) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setAutoPrompt(boolean auto) {
        if (auto) {
            this.prompt = null;
            this.autoPrompt = true;
        } else {
            this.autoPrompt = false;
        }
    }

    public void setPrompt(byte prompt[]) {
        this.prompt = prompt;
        this.autoPrompt = false;
    }

    public void setPrompt(String prompt) {
        this.setPrompt(StringTools.getBytes(prompt));
    }

    protected byte[] getPrompt(int ndx) {
        this.promptIndex = ndx;
        if (this.prompt != null) {
            return this.prompt;
        } else if (this.autoPrompt && this.isTextPackets()) {
            return StringTools.getBytes("" + (this.promptIndex + 1) + "> ");
        } else {
            return null;
        }
    }

    public boolean hasPrompt() {
        return (this.prompt != null) || (this.autoPrompt && this.isTextPackets());
    }

    protected int getPromptIndex() {
        return this.promptIndex;
    }

    private class ClientSocket {

        private Socket tcpClient = null;

        private DatagramPacket udpClient = null;

        private InputStream bais = null;

        public ClientSocket(Socket tcpClient) {
            this.tcpClient = tcpClient;
        }

        public ClientSocket(DatagramPacket udpClient) {
            this.udpClient = udpClient;
        }

        public boolean isTCP() {
            return (this.tcpClient != null);
        }

        public boolean isUDP() {
            return (this.udpClient != null);
        }

        public int available() {
            try {
                return this.getInputStream().available();
            } catch (Throwable t) {
                return 0;
            }
        }

        public InetAddress getInetAddress() {
            if (this.tcpClient != null) {
                return this.tcpClient.getInetAddress();
            } else if (this.udpClient != null) {
                SocketAddress sa = this.udpClient.getSocketAddress();
                if (sa instanceof InetSocketAddress) {
                    return ((InetSocketAddress) sa).getAddress();
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        public int getPort() {
            if (this.tcpClient != null) {
                return this.tcpClient.getPort();
            } else if (this.udpClient != null) {
                return this.udpClient.getPort();
            } else {
                return -1;
            }
        }

        public int getLocalPort() {
            return ServerSocketThread.this.getLocalPort();
        }

        public OutputStream getOutputStream() throws IOException {
            if (this.tcpClient != null) {
                return this.tcpClient.getOutputStream();
            } else {
                return null;
            }
        }

        public InputStream getInputStream() throws IOException {
            if (this.tcpClient != null) {
                return this.tcpClient.getInputStream();
            } else if (this.udpClient != null) {
                if (bais == null) {
                    bais = new ByteArrayInputStream(this.udpClient.getData(), 0, this.udpClient.getLength());
                }
                return bais;
            } else {
                return null;
            }
        }

        public void setSoTimeout(int timeoutSec) throws SocketException {
            if (this.tcpClient != null) {
                this.tcpClient.setSoTimeout(timeoutSec);
            }
        }

        public void setSoLinger(int timeoutSec) throws SocketException {
            if (this.tcpClient != null) {
                if (timeoutSec <= 0) {
                    this.tcpClient.setSoLinger(false, 0);
                } else {
                    this.tcpClient.setSoLinger(true, timeoutSec);
                }
            }
        }

        public void setSoLinger(boolean on, int timeoutSec) throws SocketException {
            if (this.tcpClient != null) {
                if (timeoutSec <= 0) {
                    on = false;
                }
                this.tcpClient.setSoLinger(on, timeoutSec);
            }
        }

        public void close() throws IOException {
            if (this.tcpClient != null) {
                this.tcpClient.close();
            }
        }
    }

    public interface SessionInfo {

        public int getLocalPort();

        public boolean isTCP();

        public boolean isUDP();

        public int getAvailableBytes();

        public long getReadByteCount();

        public long getWriteByteCount();

        public InetAddress getInetAddress();
    }

    public class ServerSessionThread extends Thread implements SessionInfo {

        private Object runLock = new Object();

        private ClientSocket client = null;

        private long readByteCount = 0L;

        private long writeByteCount = 0L;

        public ServerSessionThread(ClientSocket client) {
            super("ClientSession");
            this.client = client;
            this.start();
        }

        public boolean setClientIfAvailable(ClientSocket clientSocket) {
            boolean rtn = false;
            synchronized (this.runLock) {
                if (this.client != null) {
                    rtn = false;
                } else {
                    this.client = clientSocket;
                    this.runLock.notify();
                    rtn = true;
                }
            }
            return rtn;
        }

        public int getLocalPort() {
            return ServerSocketThread.this.getLocalPort();
        }

        public boolean isTCP() {
            return this.client.isTCP();
        }

        public boolean isUDP() {
            return this.client.isUDP();
        }

        public int getMinimumPacketLength() {
            return ServerSocketThread.this.getMinimumPacketLength();
        }

        public int getMaximumPacketLength() {
            return ServerSocketThread.this.getMaximumPacketLength();
        }

        public InetAddress getInetAddress() {
            return this.client.getInetAddress();
        }

        public int getAvailableBytes() {
            return this.client.available();
        }

        public long getReadByteCount() {
            return this.readByteCount;
        }

        public long getWriteByteCount() {
            return this.writeByteCount;
        }

        public void run() {
            while (true) {
                synchronized (this.runLock) {
                    while (this.client == null) {
                        try {
                            this.runLock.wait();
                        } catch (InterruptedException ie) {
                        }
                    }
                }
                this.readByteCount = 0L;
                this.writeByteCount = 0L;
                InetAddress inetAddr = this.client.getInetAddress();
                Print.logInfo("Remote client port: " + inetAddr + ":" + this.client.getPort() + "[" + this.client.getLocalPort() + "]");
                long sessionStartTime = DateTime.getCurrentTimeMillis();
                long sessionTimeoutMS = ServerSocketThread.this.getSessionTimeout();
                long sessionTimeoutAt = (sessionTimeoutMS > 0L) ? (sessionStartTime + sessionTimeoutMS) : -1L;
                ClientPacketHandler clientHandler = ServerSocketThread.this.getClientPacketHandler();
                if (clientHandler != null) {
                    if (clientHandler instanceof AbstractClientPacketHandler) {
                        ((AbstractClientPacketHandler) clientHandler).setSessionInfo(this);
                    }
                    clientHandler.sessionStarted(inetAddr, this.client.isTCP(), ServerSocketThread.this.isTextPackets());
                }
                OutputStream output = null;
                Throwable termError = null;
                try {
                    output = this.client.getOutputStream();
                    if (clientHandler != null) {
                        byte initialPacket[] = clientHandler.getInitialPacket();
                        if ((initialPacket != null) && (initialPacket.length > 0)) {
                            if (this.client.isTCP()) {
                                this.writeBytes(output, initialPacket);
                            } else {
                            }
                        }
                    }
                    for (int i = 0; ; i++) {
                        if (sessionTimeoutAt > 0L) {
                            long currentTimeMS = DateTime.getCurrentTimeMillis();
                            if (currentTimeMS >= sessionTimeoutAt) {
                                throw new SSSessionTimeoutException("Session timeout");
                            }
                        }
                        if (this.client.isTCP()) {
                            byte prompt[] = ServerSocketThread.this.getPrompt(i);
                            if ((prompt != null) && (prompt.length > 0)) {
                                this.writeBytes(output, prompt);
                            }
                        }
                        byte line[] = null;
                        if (ServerSocketThread.this.isTextPackets()) {
                            line = this.readLine(this.client, clientHandler);
                        } else {
                            line = this.readPacket(this.client, clientHandler);
                        }
                        if ((line != null) && ServerSocketThread.this.hasListeners()) {
                            try {
                                ServerSocketThread.this.invokeListeners(line);
                            } catch (Throwable t) {
                                break;
                            }
                        }
                        if ((line != null) && (clientHandler != null)) {
                            try {
                                byte response[] = clientHandler.getHandlePacket(line);
                                if ((response != null) && (response.length > 0)) {
                                    if (this.client.isTCP()) {
                                        this.writeBytes(output, response);
                                    } else {
                                        int rPort = 0;
                                        if (rPort <= 0) {
                                            rPort = clientHandler.getResponsePort();
                                        }
                                        if (rPort <= 0) {
                                            rPort = ServerSocketThread.this.getRemotePort();
                                        }
                                        if (rPort <= 0) {
                                            rPort = this.client.getPort();
                                        }
                                        if (rPort > 0) {
                                            int retry = 1;
                                            DatagramSocket dgSocket = new DatagramSocket();
                                            DatagramPacket respPkt = new DatagramPacket(response, response.length, inetAddr, rPort);
                                            for (; retry > 0; retry--) {
                                                Print.logInfo("Sending Datagram [%s:%d]: %s", inetAddr.toString(), rPort, StringTools.toHexString(response));
                                                dgSocket.send(respPkt);
                                            }
                                        } else {
                                            Print.logWarn("Unable to send response Datagram: unknown port");
                                        }
                                    }
                                } else {
                                }
                                if (clientHandler.terminateSession()) {
                                    break;
                                }
                            } catch (Throwable t) {
                                Print.logException("Unexpected exception: ", t);
                                break;
                            }
                        }
                        if (this.client.isUDP()) {
                            int avail = this.client.available();
                            if (avail <= 0) {
                                break;
                            } else {
                                Print.logInfo("UDP: bytes remaining - %d", avail);
                            }
                        }
                    }
                } catch (SSSessionTimeoutException ste) {
                    Print.logError(ste.getMessage());
                    termError = ste;
                } catch (SSReadTimeoutException rte) {
                    Print.logError(rte.getMessage());
                    termError = rte;
                } catch (SSEndOfStreamException eos) {
                    if (this.client.isTCP()) {
                        Print.logError(eos.getMessage());
                        termError = eos;
                    } else {
                    }
                } catch (SocketException se) {
                    Print.logError("Connection closed");
                    termError = se;
                } catch (Throwable t) {
                    Print.logException("?", t);
                    termError = t;
                }
                if (clientHandler != null) {
                    try {
                        byte finalPacket[] = clientHandler.getFinalPacket(termError != null);
                        if ((finalPacket != null) && (finalPacket.length > 0)) {
                            if (this.client.isTCP()) {
                                this.writeBytes(output, finalPacket);
                            } else {
                                int rPort = 0;
                                if (rPort <= 0) {
                                    rPort = clientHandler.getResponsePort();
                                }
                                if (rPort <= 0) {
                                    rPort = ServerSocketThread.this.getRemotePort();
                                }
                                if (rPort <= 0) {
                                    rPort = this.client.getPort();
                                }
                                if (rPort > 0) {
                                    int retry = 1;
                                    DatagramSocket dgSocket = new DatagramSocket();
                                    DatagramPacket respPkt = new DatagramPacket(finalPacket, finalPacket.length, inetAddr, rPort);
                                    for (; retry > 0; retry--) {
                                        Print.logInfo("Sending Datagram [%s:%d]: %s", inetAddr.toString(), rPort, StringTools.toHexString(finalPacket));
                                        dgSocket.send(respPkt);
                                    }
                                } else {
                                    Print.logWarn("Unable to send final packet Datagram: unknown port");
                                }
                            }
                        }
                    } catch (Throwable t) {
                        Print.logException("Final packet transmission", t);
                    }
                    clientHandler.sessionTerminated(termError, this.readByteCount, this.writeByteCount);
                    if (clientHandler instanceof AbstractClientPacketHandler) {
                        ((AbstractClientPacketHandler) clientHandler).setSessionInfo(null);
                    }
                }
                if (output != null) {
                    try {
                        output.flush();
                    } catch (IOException ioe) {
                        Print.logException("Flush", ioe);
                    } catch (Throwable t) {
                        Print.logException("?", t);
                    }
                }
                try {
                    this.client.setSoLinger(ServerSocketThread.this.getLingerTimeoutSec());
                } catch (SocketException se) {
                    Print.logException("setSoLinger", se);
                } catch (Throwable t) {
                    Print.logException("?", t);
                }
                try {
                    this.client.close();
                } catch (IOException ioe) {
                }
                synchronized (this.runLock) {
                    this.client = null;
                }
            }
        }

        private void writeBytes(OutputStream output, byte cmd[]) throws IOException {
            if ((output != null) && (cmd != null) && (cmd.length > 0)) {
                try {
                    output.write(cmd);
                    output.flush();
                    this.writeByteCount += cmd.length;
                } catch (IOException t) {
                    Print.logError("writeBytes error - " + t);
                    throw t;
                }
            }
        }

        private int readByte(ClientSocket client, long timeoutAt, int byteNdx) throws IOException {
            int ch;
            InputStream input = client.getInputStream();
            while (true) {
                if (timeoutAt > 0L) {
                    long currentTimeMS = DateTime.getCurrentTimeMillis();
                    if (currentTimeMS >= timeoutAt) {
                        throw new SSReadTimeoutException("Read timeout [@ " + byteNdx + "]");
                    }
                    int timeout = (int) (timeoutAt - currentTimeMS);
                    client.setSoTimeout(timeout);
                }
                try {
                    ch = input.read();
                    if (ch < 0) {
                        throw new SSEndOfStreamException("End of stream [@ " + byteNdx + "]");
                    }
                    this.readByteCount++;
                    return ch;
                } catch (InterruptedIOException ie) {
                    continue;
                } catch (SocketException se) {
                    throw se;
                } catch (IOException ioe) {
                    throw ioe;
                }
            }
        }

        private byte[] readLine(ClientSocket client, ClientPacketHandler clientHandler) throws IOException {
            long idleTimeoutMS = ServerSocketThread.this.getIdleTimeout();
            long pcktTimeoutMS = ServerSocketThread.this.getPacketTimeout();
            long pcktTimeoutAt = (idleTimeoutMS > 0L) ? (DateTime.getCurrentTimeMillis() + idleTimeoutMS) : -1L;
            int maxLen = this.getMaximumPacketLength();
            byte buff[] = new byte[maxLen];
            int buffLen = 0;
            boolean isIdle = true;
            long readStartTime = DateTime.getCurrentTimeMillis();
            try {
                while (true) {
                    int ch = this.readByte(client, pcktTimeoutAt, buffLen);
                    if (isIdle) {
                        isIdle = false;
                        if (pcktTimeoutMS > 0L) {
                            pcktTimeoutAt = DateTime.getCurrentTimeMillis() + pcktTimeoutMS;
                        }
                    }
                    if (ServerSocketThread.this.isLineTerminatorChar(ch)) {
                        break;
                    } else if (ServerSocketThread.this.isIgnoreChar(ch)) {
                        continue;
                    } else if (ServerSocketThread.this.isBackspaceChar(ch)) {
                        if (buffLen > 0) {
                            buffLen--;
                        }
                        continue;
                    } else if (ch < ' ') {
                        continue;
                    }
                    if (buffLen >= buff.length) {
                        byte newBuff[] = new byte[buff.length * 2];
                        System.arraycopy(buff, 0, newBuff, 0, buff.length);
                        buff = newBuff;
                    }
                    buff[buffLen++] = (byte) ch;
                    if ((maxLen > 0) && (buffLen >= maxLen)) {
                        break;
                    }
                }
            } catch (SSReadTimeoutException te) {
                if (buffLen > 0) {
                    String s = StringTools.toStringValue(buff, 0, buffLen);
                    Print.logWarn("Timeout: " + s);
                }
                if (ServerSocketThread.this.getTerminateOnTimeout()) {
                    throw te;
                }
            } catch (SSEndOfStreamException eos) {
                if (buffLen > 0) {
                    String s = StringTools.toStringValue(buff, 0, buffLen);
                    Print.logWarn("EOS: " + s);
                }
                if (client.isTCP()) {
                    Print.logError(eos.getMessage());
                    throw eos;
                } else {
                }
            } catch (IOException ioe) {
                Print.logError("ReadLine error - " + ioe);
                throw ioe;
            }
            long readEndTime = DateTime.getCurrentTimeMillis();
            if (buff.length == buffLen) {
                return buff;
            } else {
                byte newBuff[] = new byte[buffLen];
                System.arraycopy(buff, 0, newBuff, 0, buffLen);
                return newBuff;
            }
        }

        private byte[] readPacket(ClientSocket client, ClientPacketHandler clientHandler) throws IOException {
            long idleTimeoutMS = ServerSocketThread.this.getIdleTimeout();
            long pcktTimeoutMS = ServerSocketThread.this.getPacketTimeout();
            long pcktTimeoutAt = (idleTimeoutMS > 0L) ? (DateTime.getCurrentTimeMillis() + idleTimeoutMS) : -1L;
            int maxLen = this.getMaximumPacketLength();
            int minLen = this.getMinimumPacketLength();
            int actualLen = 0;
            byte packet[] = new byte[maxLen];
            int packetLen = 0;
            boolean isIdle = true;
            boolean isTextLine = false;
            try {
                while (true) {
                    int lastByte = this.readByte(client, pcktTimeoutAt, packetLen);
                    if (isIdle) {
                        isIdle = false;
                        if (pcktTimeoutMS > 0L) {
                            pcktTimeoutAt = DateTime.getCurrentTimeMillis() + pcktTimeoutMS;
                        }
                    }
                    if (isTextLine) {
                        if (ServerSocketThread.this.isLineTerminatorChar(lastByte)) {
                            break;
                        } else if (ServerSocketThread.this.isIgnoreChar(lastByte)) {
                            continue;
                        } else {
                            packet[packetLen++] = (byte) lastByte;
                        }
                    } else {
                        packet[packetLen++] = (byte) lastByte;
                    }
                    if (packetLen >= maxLen) {
                        break;
                    } else if ((actualLen > 0) && (packetLen >= actualLen)) {
                        break;
                    } else if ((clientHandler != null) && (actualLen <= 0) && (packetLen >= minLen)) {
                        actualLen = clientHandler.getActualPacketLength(packet, packetLen);
                        if (actualLen == PACKET_LEN_ASCII_LINE_TERMINATOR) {
                            if (ServerSocketThread.this.isLineTerminatorChar(lastByte)) {
                                packetLen--;
                                break;
                            } else {
                                actualLen = maxLen;
                                isTextLine = true;
                            }
                        } else if (actualLen <= PACKET_LEN_END_OF_STREAM) {
                            actualLen = maxLen;
                        } else if (actualLen > maxLen) {
                            Print.logStackTrace("Actual length [" + actualLen + "] > Maximum length [" + maxLen + "]");
                            actualLen = maxLen;
                        } else {
                        }
                        if (actualLen == packetLen) {
                            break;
                        }
                    }
                }
            } catch (SSReadTimeoutException t) {
                if (packetLen > 0) {
                    String h = StringTools.toHexString(packet, 0, packetLen);
                    Print.logWarn("Timeout: " + h);
                }
                if (ServerSocketThread.this.getTerminateOnTimeout()) {
                    throw t;
                }
            } catch (SSEndOfStreamException eos) {
                if (packetLen > 0) {
                    String h = StringTools.toHexString(packet, 0, packetLen);
                    Print.logWarn("EOS: " + h);
                }
                if (client.isTCP()) {
                    Print.logError(eos.getMessage());
                    throw eos;
                } else {
                }
            } catch (SocketException se) {
                Print.logError("ReadPacket error - " + se);
                throw se;
            } catch (IOException ioe) {
                Print.logError("ReadPacket error - " + ioe);
                throw ioe;
            }
            if (packet.length == packetLen) {
                return packet;
            } else {
                byte newPacket[] = new byte[packetLen];
                System.arraycopy(packet, 0, newPacket, 0, packetLen);
                return newPacket;
            }
        }

        public void close() throws IOException {
            IOException rethrowIOE = null;
            synchronized (this.runLock) {
                if (this.client != null) {
                    try {
                        this.client.close();
                    } catch (IOException ioe) {
                        rethrowIOE = ioe;
                    }
                    this.client = null;
                }
            }
            if (rethrowIOE != null) {
                throw rethrowIOE;
            }
        }
    }

    public static class SSSessionTimeoutException extends IOException {

        public SSSessionTimeoutException(String msg) {
            super(msg);
        }
    }

    public static class SSReadTimeoutException extends IOException {

        public SSReadTimeoutException(String msg) {
            super(msg);
        }
    }

    public static class SSEndOfStreamException extends IOException {

        public SSEndOfStreamException(String msg) {
            super(msg);
        }
    }

    /**
    *** Sends a datagram to the specified host:port
    *** @param host  The destination host
    *** @param port  The destination port
    *** @param data  The data to send
    *** @throws IOException  if an IO error occurs
    **/
    public static void sendDatagram(InetAddress host, int port, byte data[]) throws IOException {
        if (host == null) {
            throw new IOException("Invalid destination host");
        } else if (data == null) {
            throw new IOException("Data buffer is null");
        } else {
            DatagramPacket sendPacket = new DatagramPacket(data, data.length, host, port);
            DatagramSocket datagramSocket = new DatagramSocket();
            datagramSocket.send(sendPacket);
        }
    }
}
