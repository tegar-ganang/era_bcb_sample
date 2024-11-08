package com.psycho.rtb;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <pre>
 *           new()
 *             |
 *     ADD env.connected
 *             |
 *    Y--&ltfrom server&gt--N
 *    |                 |
 * NThread              |
 *    |                 |
 *    +------Init-------+
 *            |
 *           Loop
 * </pre>
 * <ul>
 * <li> For decode, rEntryMaps must stay null until decoding is ready</li>
 * <li> Disconnect when loop is not start </li>
 * </ul>
 * 
 * @author psycho
 * @version 1.0
 */
public abstract class AbstractConnection implements RtbConstants {

    static final Log LOG = LogFactory.getLog(AbstractConnection.class);

    public final RtbAdress address;

    RtbAdress aka;

    Thread writeConnection = null;

    boolean connecting = true;

    boolean running = false;

    protected AbstractConnection(RtbAdress add) {
        address = add;
    }

    protected abstract void unregister(Throwable cause);

    protected boolean confirmRegister() {
        return true;
    }

    protected void readConnectHeader(DataInputStream is) throws IOException {
    }

    protected abstract void writeConnectHeader(DataOutputStream os) throws IOException;

    public void connect() {
        Socket call = null;
        try {
            call = address.createMessage();
            DataOutputStream os = new DataOutputStream(call.getOutputStream());
            os.write(Message.CONNECT.ordinal());
            aka.writeAddress(os);
            writeConnectHeader(os);
            DataInputStream is = new DataInputStream(call.getInputStream());
            init(call, os, is);
        } catch (IOException ioe) {
            if (!(ioe instanceof ConnectException)) {
                LOG.error("Error while calling " + address + ": unregistered");
                ioe.printStackTrace();
            } else {
                LOG.warn("Host not reachable or stopped:" + address);
            }
            unregister(ioe);
        } finally {
            if (call != null) {
                try {
                    call.close();
                } catch (IOException ignore) {
                    ignore.printStackTrace();
                }
            }
        }
    }

    public boolean receiveConnect(Socket call, DataInputStream is) {
        try {
            readConnectHeader(is);
            return init(call, new DataOutputStream(call.getOutputStream()), is);
        } catch (IOException ioe) {
            LOG.error("Error while being called by " + address + ": unregistered");
            ioe.printStackTrace();
            unregister(ioe);
            return false;
        }
    }

    boolean keepSocket() {
        return false;
    }

    protected boolean init(final Socket call, final DataOutputStream os, DataInputStream is) throws IOException {
        final Throwable[] syncho = new Throwable[] { null };
        writeConnection = new Thread() {

            public void run() {
                writeConnectionMessage(syncho, call, os);
            }
        };
        writeConnection.start();
        readConnectionMessage(syncho, call, is);
        boolean linked = connectContent();
        connecting = false;
        if (linked) {
            running = true;
            if (confirmRegister()) {
                runSendingLoop();
                return true;
            }
        } else {
            unregister(null);
        }
        if (!keepSocket()) {
            call.close();
        }
        return false;
    }

    protected abstract void writeConnectionContent(DataOutputStream out) throws IOException;

    protected abstract void readConnectionContent(DataInputStream is) throws IOException;

    void writeConnectionMessage(Throwable[] lock, Socket call, DataOutputStream out) {
        try {
            writeConnectionContent(out);
            out.flush();
        } catch (Throwable t) {
            lock[0] = t;
        } finally {
            synchronized (lock) {
                writeConnection = null;
                lock.notifyAll();
            }
        }
    }

    void readConnectionMessage(Throwable[] lock, Socket call, DataInputStream in) throws IOException {
        readConnectionContent(in);
        synchronized (lock) {
            if (writeConnection != null) {
                try {
                    lock.wait(5 * 1000);
                } catch (InterruptedException ie) {
                    throw new IOException("Interrupted");
                }
            }
            if (writeConnection != null) {
                throw new IOException("Write timeout (" + 5000 + " ms)");
            }
            if (lock[0] != null) {
                if (lock[0] instanceof IOException) {
                    throw (IOException) lock[0];
                }
                IOException ioe = new IOException();
                ioe.initCause(lock[0]);
                throw ioe;
            }
        }
    }

    /**
         * Do something TODO.
         * <p>Details of the function.</p>
         *
         */
    protected boolean connectContent() {
        return true;
    }

    protected void runSendingLoop() {
    }

    public void stop() {
    }

    /**
     * Do something TODO.
     * <p>Details of the function.</p>
     *
     * @return
     */
    public boolean isConnecting() {
        return connecting;
    }

    /**
     * Do something TODO.
     * <p>Details of the function.</p>
     *
     */
    public void disconnect() {
        Socket message = null;
        try {
            message = address.createMessage();
            DataOutputStream os = new DataOutputStream(message.getOutputStream());
            writeDisconnect(os);
        } catch (IOException ignore) {
            LOG.error("Error while disconnecting : ignored", ignore);
        } finally {
            if (message != null) {
                try {
                    message.close();
                } catch (IOException ignore) {
                    LOG.warn("Error while closing call : ignored", ignore);
                }
            }
        }
    }

    /**
     * Do something TODO.
     * <p>Details of the function.</p>
     *
     * @param os
     * @throws IOException
     */
    protected abstract void writeDisconnect(DataOutputStream os) throws IOException;
}
