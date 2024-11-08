package example.btsppecho.server;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Enumeration;
import javax.bluetooth.ServiceRecord;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import example.btsppecho.MIDletApplication;
import example.btsppecho.LogScreen;

public class ServerConnectionHandler implements Runnable {

    private static final byte ZERO = (byte) '0';

    private static final int LENGTH_MAX_DIGITS = 5;

    private static final int MAX_MESSAGE_LENGTH = 65536 - LENGTH_MAX_DIGITS;

    private final ServiceRecord serviceRecord;

    private final int requiredSecurity;

    private final ServerConnectionHandlerListener listener;

    private final Hashtable sendMessages = new Hashtable();

    private StreamConnection connection;

    private OutputStream out;

    private InputStream in;

    private volatile boolean aborting;

    private Writer writer;

    public ServerConnectionHandler(ServerConnectionHandlerListener listener, ServiceRecord serviceRecord, int requiredSecurity) {
        this.listener = listener;
        this.serviceRecord = serviceRecord;
        this.requiredSecurity = requiredSecurity;
        aborting = false;
        connection = null;
        out = null;
        in = null;
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
            if (out != null) {
                try {
                    out.close();
                    synchronized (this) {
                        out = null;
                    }
                } catch (IOException e) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                    synchronized (this) {
                        in = null;
                    }
                } catch (IOException e) {
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                    synchronized (this) {
                        connection = null;
                    }
                } catch (IOException e) {
                }
            }
        }
    }

    public void queueMessageForSending(Integer id, byte[] data) {
        if (data.length > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message too long: limit is " + MAX_MESSAGE_LENGTH + " bytes");
        }
        synchronized (sendMessages) {
            sendMessages.put(id, data);
            sendMessages.notify();
        }
    }

    private void sendMessage(byte[] data) throws IOException {
        byte[] buf = new byte[LENGTH_MAX_DIGITS + data.length];
        writeLength(data.length, buf);
        System.arraycopy(data, 0, buf, LENGTH_MAX_DIGITS, data.length);
        out.write(buf);
        out.flush();
    }

    public void run() {
        String url = null;
        try {
            url = serviceRecord.getConnectionURL(requiredSecurity, false);
            connection = (StreamConnection) Connector.open(url);
            in = connection.openInputStream();
            out = connection.openOutputStream();
            LogScreen.log("Opened connection & streams to: '" + url + "'\n");
            Writer writer = new Writer(this);
            Thread writeThread = new Thread(writer);
            writeThread.start();
            LogScreen.log("Started a reader & writer for: '" + url + "'\n");
            listener.handleOpen(this);
        } catch (IOException e) {
            LogScreen.log("Failed to open " + "connection or streams for '" + url + "' , Error: " + e.getMessage());
            close();
            listener.handleOpenError(this, "IOException: '" + e.getMessage() + "'");
            return;
        } catch (SecurityException e) {
            LogScreen.log("Failed to open " + "connection or streams for '" + url + "' , Error: " + e.getMessage());
            close();
            listener.handleOpenError(this, "SecurityException: '" + e.getMessage() + "'");
            return;
        }
        while (!aborting) {
            int length = 0;
            try {
                byte[] lengthBuf = new byte[LENGTH_MAX_DIGITS];
                readFully(in, lengthBuf);
                length = readLength(lengthBuf);
                byte[] temp = new byte[length];
                readFully(in, temp);
                listener.handleReceivedMessage(this, temp);
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

    private static void readFully(InputStream in, byte[] buffer) throws IOException {
        int bytesRead = 0;
        while (bytesRead < buffer.length) {
            int count = in.read(buffer, bytesRead, buffer.length - bytesRead);
            if (count == -1) {
                throw new IOException("Input stream closed");
            }
            bytesRead += count;
        }
    }

    private static int readLength(byte[] buffer) {
        int value = 0;
        for (int i = 0; i < LENGTH_MAX_DIGITS; ++i) {
            value *= 10;
            value += buffer[i] - ZERO;
        }
        return value;
    }

    private void sendMessage(OutputStream out, byte[] data) throws IOException {
        if (data.length > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message too long: limit is: " + MAX_MESSAGE_LENGTH + " bytes");
        }
        byte[] buf = new byte[LENGTH_MAX_DIGITS + data.length];
        writeLength(data.length, buf);
        System.arraycopy(data, 0, buf, LENGTH_MAX_DIGITS, data.length);
        out.write(buf);
        out.flush();
    }

    private static void writeLength(int value, byte[] buffer) {
        for (int i = LENGTH_MAX_DIGITS - 1; i >= 0; --i) {
            buffer[i] = (byte) (ZERO + value % 10);
            value = value / 10;
        }
    }

    private class Writer implements Runnable {

        private final ServerConnectionHandler handler;

        Writer(ServerConnectionHandler handler) {
            this.handler = handler;
        }

        public void run() {
            while (!aborting) {
                synchronized (sendMessages) {
                    Enumeration e = sendMessages.keys();
                    if (e.hasMoreElements()) {
                        Integer id = (Integer) e.nextElement();
                        byte[] sendData = (byte[]) sendMessages.get(id);
                        try {
                            sendMessage(out, sendData);
                            sendMessages.remove(id);
                            listener.handleQueuedMessageWasSent(handler, id);
                        } catch (IOException ex) {
                            close();
                            listener.handleErrorClose(handler, ex.getMessage());
                        }
                    }
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
