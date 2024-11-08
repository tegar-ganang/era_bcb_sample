package gumbo.net.msg.impl;

import gumbo.core.util.AssertUtils;
import gumbo.net.impl.NetConnectorImpl;
import gumbo.net.msg.MessageConnector;
import gumbo.net.msg.MessageIOReader;
import gumbo.net.msg.MessageIOWriter;
import gumbo.net.msg.MessageRouter;
import gumbo.net.msg.MessageRouterRunner;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Default abstract implementation for a MessageConnector. Before use, must call
 * setHostPort(), then call run().
 * <p>
 * Note that socket streams can block and need to be forcibly closed to assure
 * that the socket will close. However, closing a socket's stream also closes the socket.
 * Therefore, closing a stream only "shuts down" the stream, but closing the socket
 * also closes the reader/writer and the streams to assure socket closure.
 * @author Jon Barrilleaux (jonb@jmbaai.com) of JMB and Associates Inc.
 * @param <T> Message object type.
 */
public abstract class MessageConnectorImpl<T> extends NetConnectorImpl implements MessageConnector<T> {

    /**
	 * Creates an instance.
	 */
    public MessageConnectorImpl() {
    }

    /**
	 * Called by the system each time a new connection is established. Returns a
	 * new runner for the connector's message router.
	 * <p>
	 * Default implementation returns a default MessageRouterRunner.
	 * @return Ceded runner. Never null.
	 */
    protected MessageRouterRunner<T> newMessageRouterRunner(MessageRouter<T> router, MessageIOReader<T> reader, MessageIOWriter<T> writer) {
        return new MessageRouterRunner<T>(router, reader, writer);
    }

    /**
	 * Called by the system each time a new connection is established. Returns a
	 * new message reader for the current connection's input stream.
	 * <p>
	 * Note that on some systems an ObjectMessageWriter must be created for the
	 * socket before the ObjectMessageReader, otherwise the thread will hang.
	 * Also, in general, the socket input stream should be wrapped in a
	 * DataInputStream to assure machine-independence (see
	 * NetUtils.socketReader()).
	 * @param stream Shared exposed data socket stream. Never null.
	 * @return Ceded message writer. Never null.
	 * @throws IOException if there is a problem creating the reader.
	 */
    protected abstract MessageIOReader<T> newMessageReader(InputStream stream) throws IOException;

    /**
	 * Called by the system each time a new connection is established. Returns a
	 * new message writer for current connection's output stream.
	 * <p>
	 * Note that on some systems an ObjectMessageWriter must be created for the
	 * socket before the ObjectMessageReader, otherwise the thread will hang.
	 * Also, in general, the socket output stream should be wrapped in a
	 * DataOutputStream to assure machine-independence.
	 * @param stream Shared exposed data socket stream. Never null.
	 * @return Ceded message writer. Never null.
	 * @throws IOException if there is a problem creating the writer.
	 */
    protected abstract MessageIOWriter<T> newMessageWriter(OutputStream stream) throws IOException;

    protected final void setDataSocket(Socket socket) {
        _dataSocket = socket;
    }

    protected final void setMessageWriter(MessageIOWriter<T> writer) {
        AssertUtils.assertNonNullArg(writer);
        _writer = writer;
    }

    protected final void setMessageReader(MessageIOReader<T> reader) {
        AssertUtils.assertNonNullArg(reader);
        _reader = reader;
    }

    @Override
    public final void closeDataInput() {
        if (_dataSocket != null) {
            try {
                _dataSocket.shutdownInput();
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public final void closeDataOutput() {
        if (_dataSocket != null) {
            try {
                _dataSocket.shutdownOutput();
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public final void closeConnection() {
        try {
            if (_writer != null) {
                _writer.close();
            }
            if (_reader != null) {
                _reader.close();
            }
            if (_dataSocket != null) {
                try {
                    _dataSocket.close();
                } catch (Exception ex) {
                }
            }
        } finally {
            _writer = null;
            _reader = null;
            _dataSocket = null;
        }
    }

    @Override
    public final Socket getDataSocket() {
        return _dataSocket;
    }

    @Override
    public final boolean isDataInputOpen() {
        if (_dataSocket == null) return false;
        return !_dataSocket.isInputShutdown();
    }

    @Override
    public final boolean isDataOutputOpen() {
        if (_dataSocket == null) return false;
        return !_dataSocket.isOutputShutdown();
    }

    @Override
    public final boolean isDataSocketOpen() {
        if (_dataSocket == null) {
            return false;
        } else {
            if (_dataSocket.isClosed()) throw new IllegalStateException("Socket exists but is closed.  Should never happen!");
            if (!isDataInputOpen() && !isDataOutputOpen()) throw new IllegalStateException("Socket exists but both streams are closed.  Should never happen!");
            return true;
        }
    }

    @Override
    public final MessageIOReader<T> getMessageReader() {
        if (!isDataInputOpen()) return null;
        return _reader;
    }

    @Override
    public final MessageIOWriter<T> getMessageWriter() {
        if (!isDataOutputOpen()) return null;
        return _writer;
    }

    private volatile Socket _dataSocket = null;

    private volatile MessageIOReader<T> _reader = null;

    private volatile MessageIOWriter<T> _writer = null;
}
