package net.yura.mobile.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import net.yura.mobile.logging.Logger;
import net.yura.mobile.util.QueueProcessorThread;

/**
 * @author Yura Mamyrin
 */
public abstract class SocketClient implements Runnable {

    public static final int DISCONNECTED = 1;

    public static final int CONNECTED = 2;

    public static final int CONNECTING = 3;

    public static final int COMMUNICATING = 4;

    public static final int DISCONNECTED_AND_PAUSED = 5;

    protected QueueProcessorThread writeThread;

    private Thread readThread;

    /**
     * not sure how useful this is, as sometimes write does NOT throw any exception
     * when trying to send something, even though it was not able to send, and the
     * connection is shut down right after, that message would have been lost, and
     * will not end up in the offline inbox
     */
    protected Vector offlineBox = new Vector();

    protected StreamConnection conn;

    protected OutputStream out;

    protected InputStream in;

    private boolean disconnected = false;

    public void setDisconnected(boolean b) {
        this.disconnected = b;
    }

    protected int maxRetries = 3;

    protected int retryWaitMultiplier = 2;

    protected int initialWaitValue = 1000;

    protected int maxWaitTimeMillis = 30000;

    boolean pauseReconnectOnFailure;

    private int retryCount;

    protected String protocol = "socket://";

    private final String server;

    /**
     * you can pass null into this method, but then you need to Override {@link #getNextServer()}
     * @param server
     */
    public SocketClient(String server) {
        this.server = server;
    }

    public SocketClient(String server, int maxRetries, int retryWaitMultiplier, int initialWaitValue, int maxWaitTimeMillis, boolean pauseReconnectOnFailure) {
        this.server = server;
        this.maxRetries = maxRetries;
        this.retryWaitMultiplier = retryWaitMultiplier;
        this.initialWaitValue = initialWaitValue;
        this.maxWaitTimeMillis = maxWaitTimeMillis;
        this.pauseReconnectOnFailure = pauseReconnectOnFailure;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    protected int getRetryCount() {
        return retryCount;
    }

    protected int getMaxRetries() {
        return maxRetries;
    }

    public static String connectAppend;

    protected StreamConnection openConnection(String serv) throws IOException {
        if (connectAppend != null && serv.indexOf(";") < 0) {
            serv += connectAppend;
        }
        return (StreamConnection) Connector.open(protocol + serv);
    }

    public void addToOutbox(Object obj) {
        if (writeThread == null) {
            writeThread = new QueueProcessorThread("SocketClient-WriteThread") {

                public void process(Object object) {
                    if (conn == null) {
                        int wait = initialWaitValue;
                        while (out == null || in == null) {
                            if (!isRunning()) return;
                            try {
                                String serv = getNextServer();
                                retryCount++;
                                Logger.info("[SocketClient] Trying to connect to: " + serv + " to send: " + object);
                                if (disconnected) throw new IOException();
                                conn = openConnection(serv);
                                out = conn.openOutputStream();
                                in = conn.openInputStream();
                                updateState(CONNECTING);
                                retryCount = 0;
                                wait = initialWaitValue;
                            } catch (SecurityException s) {
                                close(conn, in, out);
                                updateState(DISCONNECTED);
                                Logger.info(s);
                                securityException();
                                return;
                            } catch (Exception x) {
                                close(conn, in, out);
                                if (x instanceof IOException) {
                                    Logger.info(x.toString());
                                } else {
                                    Logger.info(x);
                                }
                                if (pauseReconnectOnFailure && retryCount > maxRetries) {
                                    updateState(DISCONNECTED_AND_PAUSED);
                                    synchronized (this) {
                                        try {
                                            wait();
                                        } catch (Exception e) {
                                            Logger.info(e);
                                        }
                                    }
                                    updateState(DISCONNECTED);
                                    retryCount = 0;
                                } else {
                                    updateState(DISCONNECTED);
                                    try {
                                        Thread.sleep(wait);
                                        wait = wait * retryWaitMultiplier;
                                        if (wait > maxWaitTimeMillis) {
                                            wait = maxWaitTimeMillis;
                                        }
                                    } catch (InterruptedException ex) {
                                        Logger.info(ex);
                                    }
                                }
                            }
                        }
                        connected(in, out);
                        readThread = new Thread(SocketClient.this, "SocketClient-ReadThread");
                        readThread.start();
                    }
                    try {
                        Logger.info("[SocketClient] sending object: " + object);
                        updateState(COMMUNICATING);
                        if (disconnected) throw new IOException();
                        write(out, object);
                        out.flush();
                        updateState(CONNECTED);
                    } catch (Exception ex) {
                        Logger.info("[SocketClient] Exception during a write to socket");
                        Logger.info(ex);
                        addToOfflineBox(object, true);
                        shutdownConnection();
                    }
                }
            };
            updateState(CONNECTING);
            writeThread.start();
        }
        writeThread.addToInbox(obj);
    }

    /**
     * if we are in the DISCONNECTED_AND_PAUSED state, wake us up
     */
    public void addToOfflineBox(Object t, boolean isFront) {
        if (!offlineBox.contains(t)) {
            if (isFront) {
                offlineBox.insertElementAt(t, 0);
            } else {
                offlineBox.addElement(t);
            }
        }
        wake();
    }

    /**
     * if we are in the DISCONNECTED_AND_PAUSED state, wake us up
     */
    public void wake() {
        QueueProcessorThread obj = writeThread;
        if (obj != null) {
            synchronized (obj) {
                obj.notify();
            }
        }
    }

    public Vector getOfflineBox() {
        return offlineBox;
    }

    protected void sendOfflineInboxMessages() {
        Logger.info("[SocketClient] sending offline messages: " + offlineBox);
        while (!offlineBox.isEmpty()) {
            Object task = offlineBox.elementAt(0);
            offlineBox.removeElementAt(0);
            addToOutbox(task);
        }
    }

    public void disconnect() {
        QueueProcessorThread old = writeThread;
        writeThread = null;
        shutdownConnection();
        if (old != null) {
            old.kill();
        }
    }

    private synchronized void shutdownConnection() {
        if (conn == null && in == null && out == null) return;
        updateState(DISCONNECTED);
        close(conn, in, out);
        in = null;
        out = null;
        conn = null;
        if (writeThread != null) {
            Vector inbox = writeThread.getInbox();
            for (int c = 0; c < inbox.size(); c++) {
                addToOfflineBox(inbox.elementAt(c), false);
            }
            writeThread.clearInbox();
            disconnected();
        }
    }

    private void close(final StreamConnection connection, final InputStream inputStream, final OutputStream outputStream) {
        new Thread() {

            public void run() {
                FileUtil.close(inputStream);
                FileUtil.close(outputStream);
                FileUtil.close(connection);
            }
        }.start();
    }

    /**
     * This is the run() method of the Socket read thread
     * this thread can sometimes block and stick around even after a socket is closed and a new one reopened
     */
    public final void run() {
        try {
            String name = readThread.getName();
            int id = System.identityHashCode(readThread);
            Logger.info("[SocketClient] STARTING " + name + " " + id);
            if (net.yura.mobile.util.QueueProcessorThread.CHANGE_PRIORITY) {
                Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            }
            InputStream myin = in;
            try {
                while (true) {
                    if (disconnected) throw new IOException();
                    Object task = read(myin);
                    updateState(COMMUNICATING);
                    Logger.info("[SocketClient] got object: " + task + " " + id);
                    try {
                        Thread.yield();
                        Thread.sleep(0);
                        handleObject(task);
                        Thread.yield();
                        Thread.sleep(0);
                    } catch (Exception x) {
                        Logger.warn("[SocketClient] CAN NOT HANDLE! Task: " + task + " " + x.toString());
                        x.printStackTrace();
                    }
                    updateState(CONNECTED);
                }
            } catch (Exception ex) {
                Logger.info("[SocketClient] Disconnect (Exception) during read from socket " + ex.toString() + " " + id);
                boolean normal = myin == in;
                if (!(ex instanceof IOException) || (!normal && writeThread != null)) {
                    Logger.info("[SocketClient] strange disconnect in=" + in + " myin=" + myin);
                    ex.printStackTrace();
                }
                if (normal) {
                    shutdownConnection();
                }
            }
            Logger.info("[SocketClient] ENDING " + name + " " + id);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    protected String getNextServer() {
        return server;
    }

    protected void securityException() {
        Logger.warn("[SocketClient] Socket connections are not allowed.");
    }

    /**
     * this is called when a object is recieved from the server
     */
    protected abstract void handleObject(Object task);

    /**
     * this is used to update a connection indicator
     */
    protected abstract void updateState(int c);

    protected abstract void write(OutputStream out, Object object) throws IOException;

    protected abstract Object read(InputStream in) throws IOException;

    /**
     * this method is called when a connection is established
     */
    protected abstract void connected(InputStream in, OutputStream out);

    /**
     * This method is called when a connection is lost
     *
     * this should send a new message if you want to make a connection again
     * the message you send will be something like 'hello' or 'login'
     */
    protected abstract void disconnected();
}
