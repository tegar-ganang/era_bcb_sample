import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;

public class ClientConnectionHandler implements Runnable {

    private static final byte ZERO = (byte) '0';

    private static final int LENGTH_MAX_DIGITS = 5;

    private static final int MAX_MESSAGE_LENGTH = 65536 - LENGTH_MAX_DIGITS;

    private final ClientConnectionHandlerListener listener;

    private final Hashtable sendMessages = new Hashtable();

    private StreamConnection connection;

    private InputStream in;

    private OutputStream out;

    private volatile boolean aborting;

    /**
     * Constructor used to create an object of class ClientConnectionHandler
     */
    public ClientConnectionHandler(StreamConnection connection, ClientConnectionHandlerListener listener) {
        this.connection = connection;
        this.listener = listener;
        aborting = false;
        in = null;
        out = null;
    }

    /**
     * Function to start the thread.
     */
    public synchronized void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    /**
     * Function to close the thread.
     */
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

    /**
     * Function to queue messages for sending.
     */
    public void queueMessageForSending(Integer id, byte[] data) {
        if (data.length > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message too long: limit is " + MAX_MESSAGE_LENGTH + " bytes");
        }
        synchronized (sendMessages) {
            sendMessages.put(id, data);
            sendMessages.notify();
        }
    }

    /**
     * Function for sending messages.
     */
    private void sendMessage(byte[] data) throws IOException {
        byte[] buf = new byte[LENGTH_MAX_DIGITS + data.length];
        writeLength(data.length, buf);
        System.arraycopy(data, 0, buf, LENGTH_MAX_DIGITS, data.length);
        out.write(buf);
        out.flush();
    }

    /**
     * Thread function for this class.
     */
    public void run() {
        try {
            in = connection.openInputStream();
            out = connection.openOutputStream();
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

    /**
     * Function for reading the data from inputstream.
     */
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

    /**
     * Function for calculating read length from the byte data.
     */
    private static int readLength(byte[] buffer) {
        int value = 0;
        for (int i = 0; i < LENGTH_MAX_DIGITS; ++i) {
            value *= 10;
            value += buffer[i] - ZERO;
        }
        return value;
    }

    /**
     * Function for writing byte data on the output stream.
     */
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

    /**
     * Function for calculating write length fro the byte data.
     */
    private static void writeLength(int value, byte[] buffer) {
        for (int i = LENGTH_MAX_DIGITS - 1; i >= 0; --i) {
            buffer[i] = (byte) (ZERO + value % 10);
            value = value / 10;
        }
    }

    /**
     * Inner class for writing data in a separate thread.
     */
    private class Writer implements Runnable {

        private final ClientConnectionHandler handler;

        Writer(ClientConnectionHandler handler) {
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
