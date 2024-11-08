package example.btl2capecho.client;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.L2CAPConnectionNotifier;

public class ClientConnectionHandler implements Runnable {

    private static final int WAIT_MILLIS = 250;

    private final ConnectionService ConnectionService;

    private final ClientConnectionHandlerListener listener;

    private final Hashtable sendMessages = new Hashtable();

    private L2CAPConnection connection;

    int transmitMTU = 0;

    private volatile boolean aborting;

    public ClientConnectionHandler(ConnectionService ConnectionService, L2CAPConnection connection, ClientConnectionHandlerListener listener) {
        this.ConnectionService = ConnectionService;
        this.connection = connection;
        this.listener = listener;
        aborting = false;
    }

    ClientConnectionHandlerListener getListener() {
        return listener;
    }

    public synchronized void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    public void close() {
        if (!aborting) {
            synchronized (this) {
                aborting = true;
            }
            synchronized (sendMessages) {
                sendMessages.notify();
            }
            if (connection != null) {
                try {
                    connection.close();
                    synchronized (this) {
                        connection = null;
                        transmitMTU = 0;
                    }
                } catch (IOException e) {
                }
            }
        }
    }

    public void queueMessageForSending(Integer id, byte[] data) {
        if (data.length > transmitMTU) {
            throw new IllegalArgumentException("Message too long: limit is " + transmitMTU + " bytes");
        }
        synchronized (sendMessages) {
            sendMessages.put(id, data);
            sendMessages.notify();
        }
    }

    public void run() {
        try {
            transmitMTU = connection.getTransmitMTU();
            Writer writer = new Writer(this);
            Thread writeThread = new Thread(writer);
            writeThread.start();
            listener.handleStreamsOpen(this);
        } catch (IOException e) {
            close();
            listener.handleStreamsOpenError(this, e.getMessage());
            return;
        }
        while (!aborting) {
            boolean ready = false;
            try {
                ready = connection.ready();
            } catch (IOException e) {
                close();
                listener.handleClose(this);
            }
            int length = 0;
            try {
                if (ready) {
                    int mtuLength = connection.getReceiveMTU();
                    if (mtuLength > 0) {
                        byte[] buffer = new byte[mtuLength];
                        length = connection.receive(buffer);
                        byte[] readData = new byte[length];
                        System.arraycopy(buffer, 0, readData, 0, length);
                        listener.handleReceivedMessage(this, readData);
                    }
                } else {
                    try {
                        synchronized (this) {
                            wait(WAIT_MILLIS);
                        }
                    } catch (InterruptedException e) {
                    }
                }
            } catch (IOException e) {
                close();
                if (length == 0) {
                    listener.handleClose(this);
                } else {
                    listener.handleErrorClose(this, e.getMessage());
                }
            }
        }
    }

    private class Writer implements Runnable {

        private final ClientConnectionHandler handler;

        Writer(ClientConnectionHandler handler) {
            this.handler = handler;
        }

        public void run() {
            while (!aborting) {
                Enumeration e = sendMessages.keys();
                if (e.hasMoreElements()) {
                    Integer id = (Integer) e.nextElement();
                    byte[] sendData = (byte[]) sendMessages.get(id);
                    try {
                        connection.send(sendData);
                        sendMessages.remove(id);
                        listener.handleQueuedMessageWasSent(handler, id);
                    } catch (IOException ex) {
                        close();
                        listener.handleErrorClose(handler, ex.getMessage());
                    }
                }
                synchronized (sendMessages) {
                    if (sendMessages.isEmpty()) {
                        try {
                            sendMessages.wait();
                        } catch (InterruptedException ex) {
                        }
                    }
                }
            }
        }
    }
}
