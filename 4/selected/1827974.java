package server.MWChatServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import common.CampaignData;
import common.util.ThreadManager;

/**
 * The keeper of the Socket on the server side. Spawns a thread for reading from
 * the socket.
 * 
 * As each line is read from the socket, the server is notified, via the
 * IConnectionListener interface
 * 
 * Outgoing messages are passed by ChatServer to the Dispatcher who queues them
 * up and calls flush on the different CHes in turn.
 */
public class ConnectionHandler extends AbstractConnectionHandler {

    protected Socket _socket = null;

    protected PrintWriter _out = null;

    protected ReaderThread _reader = null;

    protected WriterThread _writer = null;

    protected InputStream _inputStream = null;

    protected boolean _isShutDown = false;

    /**
     * Construct a ConnectionHandler for the given socket
     * 
     * @param s
     *            - the socket
     * @param listener
     *            - object that will be notified with incoming messages
     * @exception IOException
     *                - if there is a problem reading or writing to the socket
     */
    public ConnectionHandler(Socket socket, MWChatClient client) throws IOException {
        _client = client;
        _socket = socket;
        _out = new PrintWriter(new OutputStreamWriter(_socket.getOutputStream(), "UTF8"));
        _inputStream = socket.getInputStream();
    }

    /**
     * Called from MWChatClient. Start reading incoming chat and sending to an
     * associated dispatcher.
     */
    void init() {
        try {
            _reader = new ReaderThread(this, _client, _inputStream);
            ThreadManager.getInstance().runInThreadFromPool(_reader);
            _writer = new WriterThread(_socket, _out, _client.getHost());
            ThreadManager.getInstance().runInThreadFromPool(_writer);
        } catch (OutOfMemoryError OOM) {
            CampaignData.mwlog.errLog(OOM.getMessage());
            try {
                _out.close();
                _out = null;
                _socket.close();
                _socket = null;
                _inputStream.close();
                _inputStream = null;
                _client = null;
                _reader = null;
                _writer = null;
                System.gc();
                shutdown(true);
            } catch (Exception e) {
                CampaignData.mwlog.errLog(e);
            }
        } catch (Exception ex) {
            CampaignData.mwlog.errLog(ex);
        }
    }

    /**
     * Bypass the message queue to send something immediately. This is used for
     * pings and to kill clients (bad chars, banned folks, etc).
     */
    @Override
    public void queuePriorityMessage(String message) {
        synchronized (message) {
            _out.print(message + "\n");
            _out.flush();
        }
    }

    /**
     * @param notify
     *            to notify the ConnectionListener. Should be true for
     *            unexpected shutdowns (like if there is a socket error), and
     *            false otherwise (if client called this method on purpose)
     */
    @Override
    public synchronized void shutdown(boolean notify) {
        if (!_isShutDown) {
            _isShutDown = true;
            _reader.pleaseStop();
            _reader.interrupt();
            _writer.pleaseStop();
            _writer.interrupt();
            try {
                _socket.close();
            } catch (IOException e) {
                CampaignData.mwlog.errLog("connection shutdown due to error");
                CampaignData.mwlog.errLog(e);
            }
            super.shutdown(notify);
        }
    }

    @Override
    public void queueMessage(String message) {
        if (_writer != null) {
            _writer.queueMessage(message);
        }
    }
}
