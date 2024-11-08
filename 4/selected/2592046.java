package gumbo.wip.net.msg.impl;

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
 * Note that the socket streams can block and need to be forcibly closed to asure
 * that the socket will close. However, closing a socket's stream also closes the socket.
 * Therefore, closing a stream only "shuts down" the stream, but closing the socket
 * also closes the reader/writer and the streams to assure socket closure.
 * 
 * @author Jon Barrilleaux (jonb@jmbaai.com) of JMB and Associates Inc.
 * @param <T> Message object type.
 */
public abstract class XXXMessageConnectorImpl<T> extends NetConnectorImpl implements MessageConnector<T> {

    /**
	 * Creates an instance.
	 * @param router Shared exposed router. Never null.
	 */
    public XXXMessageConnectorImpl(MessageRouter<T> router) {
        AssertUtils.assertNonNullArg(router);
        _router = router;
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

    protected final void setRouterRunner(MessageRouterRunner<T> runner) {
        _routerRunner = runner;
    }

    protected final MessageRouterRunner<T> getRouterRunner() {
        return _routerRunner;
    }

    protected final void setMessageWriter(MessageIOWriter<T> writer) {
        _writer = writer;
    }

    protected final void setMessageReader(MessageIOReader<T> reader) {
        _reader = reader;
    }

    @Override
    public final void pleaseStop() {
        if (_routerRunner != null) {
            _routerRunner.pleaseStop();
        }
        super.pleaseStop();
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
    public final void closeDataSocket() {
        if (_writer != null) {
            _writer.close();
            _writer = null;
        }
        if (_reader != null) {
            _reader.close();
            _reader = null;
        }
        if (_dataSocket != null) {
            try {
                _dataSocket.close();
            } catch (Exception ex) {
            }
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
            AssertUtils.assertValidState(!_dataSocket.isClosed(), "Socket is non-null but closed.  Should never happen!");
            AssertUtils.assertValidState(isDataInputOpen() || isDataOutputOpen(), "Socket is non-null but both streams are closed.  Should never happen!");
            return true;
        }
    }

    @Override
    public final MessageIOReader<T> getMessageReader() {
        if (!isDataOutputOpen()) return null;
        return _reader;
    }

    @Override
    public final MessageIOWriter<T> getMessageWriter() {
        if (!isDataInputOpen()) return null;
        return _writer;
    }

    @Override
    public final MessageRouter<T> getMessageRouter() {
        return _router;
    }

    private volatile Socket _dataSocket = null;

    private volatile MessageIOReader<T> _reader = null;

    private volatile MessageIOWriter<T> _writer = null;

    private volatile MessageRouter<T> _router;

    private volatile MessageRouterRunner<T> _routerRunner = null;
}
