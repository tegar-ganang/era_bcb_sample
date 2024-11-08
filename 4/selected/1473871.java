package com.fusteeno.gnutella.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;
import com.fusteeno.gnutella.io.DataInputStream;
import com.fusteeno.gnutella.io.DataOutputStream;
import com.fusteeno.gnutella.util.Debug;
import com.fusteeno.gnutella.util.Constants;
import com.fusteeno.gnutella.util.ProviderServentsAddress;
import com.fusteeno.gnutella.util.SendQueue;
import com.fusteeno.util.event.StatusChangeEvent;
import com.fusteeno.util.event.StatusChangeListener;

/**
 * @author aGO!
 * 
 * I servent vengono creati per rispondere ad una richiesta esterna di connessione o per 
 * stabilire una connessione con un servent per mia iniziativa
 * 
 * TODO: in un secondo passaggio rimuovere gli accessori (get..., set...) e mettere gli
 * attributi interessati a visibilita' protected
 * 
 * CONTROLLARE!!!
 */
public class Servent {

    protected static final int STATUS_CONNECTING = 1;

    protected static final int STATUS_CONNECTED = 2;

    protected static final int STATUS_DISCONNECTED = 3;

    protected static final int STATUS_NEW_STATS = 4;

    private static final String CONNECT = " CONNECT/";

    private static final String LOGINMSG_04 = " CONNECT/0.4";

    private static final String LOGINMSG_06 = " CONNECT/0.6";

    private static final String LOGIN_OK = " OK";

    private static final String NETWORK_NAME = "GNUTELLA";

    private static final String LOGIN_FORBIDDEN = " FORBIDDEN\n\n";

    private static final String USER_AGENT = "User-Agent";

    private static final String VENDORCODE = Constants.FUSTEENO_NAME + "/" + Constants.FUSTEENO_VERSION;

    private static final String X_TRY = "X-Try";

    private static final String VERSION = "0.6";

    private int status;

    private Vector statusChangeListeners = new Vector();

    private WriterThread writerThread;

    private ReaderThread readerThread;

    private ProviderServentsAddress providerServentsAddress = null;

    private String firstLine;

    private Socket socket;

    private int port;

    private String ip;

    private DataInputStream in;

    private DataOutputStream out;

    private boolean incomingConnection;

    private SendQueue sendQueue;

    /**
     * Constructs a new servent to accept the connection coming in from socket.
     */
    public Servent(String request, Socket sock) {
        firstLine = request;
        socket = sock;
        ip = socket.getInetAddress().getHostAddress();
        port = socket.getPort();
        incomingConnection = true;
        incomingConnection = true;
        init();
    }

    /**
     * Creates a new Servent with given ip and port.
     */
    public Servent(String ip, int port) {
        this.ip = ip;
        this.port = port;
        incomingConnection = false;
        init();
    }

    /**
     * Inizializzazione del Servent
     */
    private void init() {
        providerServentsAddress = ProviderServentsAddress.getInstance();
        sendQueue = new SendQueue();
        writerThread = new WriterThread(this);
        readerThread = new ReaderThread(this);
        status = STATUS_DISCONNECTED;
    }

    public SendQueue getSendQueue() {
        return sendQueue;
    }

    /**
	 * @return Returns the port.
	 */
    public int getPort() {
        return port;
    }

    /**
     * Returns ip address of remote host.
     */
    public String getIp() {
        return ip;
    }

    public String getAddress() {
        return ip + ":" + port;
    }

    /**
	 * @return Returns the status.
	 */
    public int getStatus() {
        return status;
    }

    /**
	 * @param status The status to set.
	 */
    private void setStatus(int newStatus) {
        fireStatusChange(status, newStatus);
        this.status = newStatus;
    }

    private void sendInvalidLogin(DataOutputStream out, String reason) {
        try {
            out.write(NETWORK_NAME.getBytes());
            out.write(LOGIN_FORBIDDEN.getBytes());
            out.flush();
            Debug.log(reason);
        } catch (IOException ie) {
        }
    }

    /**
	 * Controlla se due sevent sono uguali
	 */
    public boolean equals(Servent o) {
        if ((o != null) && this.getIp().equals(o.getIp()) && this.port == o.port) return true; else return false;
    }

    private int compareVersion(String v1, String v2) {
        StringTokenizer t1 = new StringTokenizer(v1, ".");
        StringTokenizer t2 = new StringTokenizer(v2, ".");
        if ((t1.countTokens() != 2) && (t2.countTokens() != 2)) return -2;
        int major1 = Integer.parseInt(t1.nextToken());
        int minor1 = Integer.parseInt(t1.nextToken());
        int major2 = Integer.parseInt(t2.nextToken());
        int minor2 = Integer.parseInt(t2.nextToken());
        if (major1 > major2) return 1; else if (major1 < major2) return -1;
        if (minor1 > minor2) return 1; else if (minor1 < minor2) return -1;
        return 0;
    }

    /**
     * Behave as host cache and send some cached hosts.
     */
    private void sendServerHeaders(DataOutputStream os) throws IOException {
    }

    private boolean accept() {
        Debug.log("[Servent] Accept connection from " + ip + ":" + port);
        try {
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            writerThread.setOutputStream(out);
            readerThread.setInputStream(in);
        } catch (IOException ie) {
            Debug.log(ie.toString());
            return false;
        }
        StringTokenizer t = new StringTokenizer(firstLine, "/");
        if (t.countTokens() < 2) {
            sendInvalidLogin(out, "Wrong number of args");
            return false;
        }
        t.nextToken();
        String version = t.nextToken();
        if (compareVersion(version, "0.4") == -2) return false; else if (compareVersion(version, "0.4") == 0) {
            try {
                out.write(NETWORK_NAME.getBytes());
                out.write(LOGIN_OK.getBytes());
                out.write(10);
                out.write(10);
                out.flush();
            } catch (IOException ie) {
                return false;
            }
            return true;
        } else if (compareVersion(version, "0.4") > 0) {
            StringBuffer loginOk = new StringBuffer();
            loginOk.append(NETWORK_NAME);
            loginOk.append('/');
            loginOk.append(VERSION);
            loginOk.append(" 200 OK");
            try {
                out.write(loginOk.toString().getBytes());
                out.write(13);
                out.write(10);
                sendServerHeaders(out);
                out.flush();
            } catch (IOException ie) {
                return false;
            }
            ReadLineReader rlr = new ReadLineReader(in);
            String response = rlr.readLine();
            if (response != null && response.indexOf("200") != -1) sendInvalidLogin(out, "Wrong response" + response); else {
                sendInvalidLogin(out, "Wrong response");
                return false;
            }
        }
        return true;
    }

    /**
     * Called from <code>run()</code> when connection is initiated from us,
     * handles handshaking.
     */
    private boolean connect() {
        Debug.log("[Servent] Connection to " + ip + ":" + port);
        ReadLineReader rlr;
        try {
            socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(ip, port), Constants.CONNECT_SOCKET_TIMEOUT);
            socket.setSoTimeout(Constants.SOCKET_SO_TIMEOUT);
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            writerThread.setOutputStream(out);
            readerThread.setInputStream(in);
        } catch (Exception e) {
            return false;
        }
        LineWriter lw = new LineWriter(out);
        rlr = new ReadLineReader(in);
        try {
            lw.println(NETWORK_NAME + LOGINMSG_06);
            lw.println(USER_AGENT + ": " + VENDORCODE);
            lw.println("X-Ultrapeer: False");
            lw.println("X-Query-Routing: 0.1");
            lw.println();
            lw.flush();
        } catch (IOException ie) {
            Debug.log("gnutella: Could not handshake");
            return false;
        }
        String response = rlr.readLine();
        if ((response != null) && (response.toUpperCase().startsWith(NETWORK_NAME + "/" + VERSION))) {
            boolean result = (response.indexOf("200") != -1);
            Debug.log("Response: " + response);
            String line;
            while ((line = rlr.readLine()) != null) {
                Debug.log(line);
                if (line.toUpperCase().indexOf("X-TRY") != -1) {
                    String lineAddrs = line.substring(line.indexOf(':') + 1);
                    String addrs[] = lineAddrs.split(",");
                    for (int i = 0; i < addrs.length; i++) providerServentsAddress.pushAddress(addrs[i].trim());
                }
            }
            if (result) {
                try {
                    lw.println(NETWORK_NAME + "/" + VERSION + " 200 OK");
                    lw.println();
                    lw.flush();
                } catch (IOException ie) {
                    Debug.log("gnutella: Could not handshake");
                    return false;
                }
            }
            return result;
        } else {
            Debug.log("servent response: " + response);
            return false;
        }
    }

    private void readHeaders(String response) {
    }

    public void start() {
        boolean success = false;
        setStatus(STATUS_CONNECTING);
        if (incomingConnection) {
            success = accept();
        } else {
            success = connect();
        }
        if (!success) {
            close();
            setStatus(STATUS_DISCONNECTED);
        } else {
            setStatus(STATUS_CONNECTED);
            try {
                socket.setSoTimeout(Constants.DEFAULT_SOCKET_TIMEOUT);
            } catch (SocketException se) {
                Debug.log(se.toString());
            }
            writerThread.start();
            readerThread.start();
        }
    }

    /**
     * Closes socket if open.
     */
    private void close() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
            }
            socket = null;
        }
        writerThread.close();
        readerThread.close();
    }

    public synchronized void stop() {
        writerThread.stop();
        readerThread.stop();
        close();
        setStatus(STATUS_DISCONNECTED);
    }

    /**
     * Sends message to remote host. Message is queued in
     * <code>sendQueue</code> and sent by <code>writerThread</code>.
     * @see SendQueue
     * @see Writerthread
     */
    public void send(Message msg, int priority) {
        if (status == STATUS_CONNECTED) {
            sendQueue.add(msg, priority);
        }
    }

    public void addStatusChangeListener(StatusChangeListener l) {
        synchronized (statusChangeListeners) {
            statusChangeListeners.add(l);
        }
    }

    public void removeStatusChangeListener(StatusChangeListener l) {
        synchronized (statusChangeListeners) {
            statusChangeListeners.remove(l);
        }
    }

    private void fireStatusChange(int oldStatus, int newStatus) {
        Object[] listeners;
        synchronized (statusChangeListeners) {
            listeners = statusChangeListeners.toArray();
        }
        if (listeners != null) {
            StatusChangeEvent event = new StatusChangeEvent(this, oldStatus, newStatus);
            for (int i = listeners.length - 1; i >= 0; i--) {
                ((StatusChangeListener) listeners[i]).statusChange(event);
            }
        }
    }
}
