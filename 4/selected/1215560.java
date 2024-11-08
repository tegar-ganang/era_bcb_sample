package example.btl2capecho.server;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Enumeration;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.L2CAPConnection;
import javax.bluetooth.L2CAPConnectionNotifier;
import javax.microedition.io.Connector;
import example.btl2capecho.MIDletApplication;
import example.btl2capecho.LogScreen;

public class ServerConnectionHandler implements Runnable {

    private static final int WAIT_MILLIS = 250;

    private final ServiceRecord serviceRecord;

    private final int requiredSecurity;

    private final ServerConnectionHandlerListener listener;

    private final Hashtable sendMessages = new Hashtable();

    private L2CAPConnection connection;

    private int transmitMTU;

    private volatile boolean aborting;

    private Writer writer;

    public ServerConnectionHandler(ServerConnectionHandlerListener listener, ServiceRecord serviceRecord, int requiredSecurity) {
        this.listener = listener;
        this.serviceRecord = serviceRecord;
        this.requiredSecurity = requiredSecurity;
        aborting = false;
        connection = null;
        transmitMTU = 0;
        listener = null;
    }

    public ServiceRecord getServiceRecord() {
        return serviceRecord;
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
        String url = null;
        try {
            url = serviceRecord.getConnectionURL(requiredSecurity, false);
            connection = (L2CAPConnection) Connector.open(url);
            transmitMTU = connection.getTransmitMTU();
            LogScreen.log("Opened connection to: '" + url + "'\n");
            Writer writer = new Writer(this);
            Thread writeThread = new Thread(writer);
            writeThread.start();
            LogScreen.log("Started a reader & writer for: '" + url + "'\n");
            listener.handleOpen(this);
        } catch (IOException e) {
            LogScreen.log("Failed to open " + "connection for '" + url + "' , Error: " + e.getMessage());
            close();
            listener.handleOpenError(this, "IOException :'" + e.getMessage() + "'");
            return;
        } catch (SecurityException e) {
            LogScreen.log("Failed to open " + "connection for '" + url + "' , Error: " + e.getMessage());
            close();
            listener.handleOpenError(this, "SecurityException: '" + e.getMessage() + "'");
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

        private final ServerConnectionHandler handler;

        Writer(ServerConnectionHandler handler) {
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
