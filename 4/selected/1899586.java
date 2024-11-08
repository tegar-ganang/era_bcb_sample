package client.protocol;

import java.io.PrintStream;
import java.io.IOException;
import java.util.Vector;
import java.net.Socket;
import common.CampaignData;
import client.protocol.IConnectionListener;

/**
 * The keeper of the Socket on the client side.
 * Using deprecated JDK1.0.2 I/O methods on purpose, because this may be running in a crappy
 * browser.
 *
 * This method spawns two threads: One for reading and one for writing.  When new messages are read,
 * they are passed to the ChatServerLocal via it's incomingMessage() method
 * @see ChatServerLocal#incomingMessage
 */
public class ConnectionHandlerLocal implements IConnectionHandler {

    protected PrintStream _out;

    protected Socket _socket;

    protected IConnectionListener _listener;

    protected ReaderThread _reader;

    protected WriterThread _writer;

    static final boolean DEBUG = false;

    /**
     * Construct the ConnectionHandler and spawn the reader and writer threads.
     */
    public ConnectionHandlerLocal(Socket s) throws IOException {
        _socket = s;
        _out = new PrintStream(s.getOutputStream());
        _reader = new ReaderThread(this, _socket);
        _writer = new WriterThread(_out);
        _writer.start();
    }

    public void setListener(IConnectionListener listener) {
        _listener = listener;
        _reader.setListener(listener);
        _reader.start();
    }

    /**
     * Queue an outgoing message
     */
    public void queueMessage(String message) {
        _writer.queueMessage(message);
    }

    public void sendImmediately(String message) {
        _out.println(message);
        _out.flush();
    }

    /**
     * Try to stop the threads gracefully, close the socket, then call connectionLost() on the
     * ChatServerLocal.
     * This method is typically called by the ReaderThread when it has detected the the
     * connection died.
     */
    public void shutdown(boolean notify) {
        _reader.pleaseStop();
        _writer.pleaseStop();
        _writer.flushOutputQueue();
        try {
            _socket.close();
        } catch (IOException e) {
            CampaignData.mwlog.errLog("Error closing socket.");
            CampaignData.mwlog.errLog(e);
        }
        if (notify) {
            _listener.socketClosed();
        }
    }

    static final void DEBUG(String s) {
        if (DEBUG) {
            CampaignData.mwlog.errLog(s);
        }
    }
}

/**
 * Write the messages in the queue to the socket's output stream
 */
class WriterThread extends Thread {

    private boolean keepGoing = true;

    private Vector<String> outgoingMessages;

    private PrintStream _out;

    WriterThread(PrintStream out) {
        super("ConnectionHandler$WriterThread");
        _out = out;
        outgoingMessages = new Vector<String>(1, 1);
    }

    @Override
    public synchronized void run() {
        try {
            while (keepGoing) {
                flushOutputQueue();
                wait(1000);
            }
            CampaignData.mwlog.errLog("WriterThread: stopping gracefully.");
        } catch (InterruptedException e) {
            CampaignData.mwlog.errLog("ConnectionHandlerLocal$WriterThread.run(): Interrupted!");
        }
    }

    void flushOutputQueue() {
        while (outgoingMessages.size() > 0) {
            String message = (String) outgoingMessages.elementAt(0);
            outgoingMessages.removeElementAt(0);
            ConnectionHandlerLocal.DEBUG("> " + message);
            _out.print(message + "\r\n");
        }
        _out.flush();
    }

    synchronized void queueMessage(String s) {
        outgoingMessages.addElement(s);
        notify();
    }

    void pleaseStop() {
        keepGoing = false;
    }
}
