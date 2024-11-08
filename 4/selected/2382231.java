package gumbo.wip.net.msg;

import gumbo.net.NetDataSocketFailedException;
import gumbo.net.NetDataStreamFailedException;
import gumbo.net.msg.MessageIOException;
import gumbo.net.msg.MessageIOReader;
import gumbo.net.msg.MessageIOWriter;
import gumbo.net.msg.MessageRouter;

/**
 * A Runnable that asynchronously receives messages and routes them to message
 * handlers registered with the delegate MessageRouter. The delegate
 * MessageReader is used to receive whole messages from the remote network node.
 * The delegate MessageWriter is passed to the router's message handlers, to
 * send a response, as needed. Call run() to begin listening to the message
 * reader and routing its messages.
 * <p>
 * Once running, the router will continue until pleaseStop() is called, or a
 * fatal exception occurs. A fatal exception occurs if the MessageReader throws
 * anything but MessageIOException. Non-fatal exceptions are ignored, such as if
 * the MessageReader throws MessageIOException, signifying a message format
 * error, or a message handler throws a RuntimeException.
 * <p>
 * Note that a router is separate from its runner since the router and its
 * listeners are meant to persist between network connections, but the runner
 * and its network resources are not.
 * @author Jon Barrilleaux (jonb@jmbaai.com) of JMB and Associates Inc.
 * @see MessageRouter
 * @param <T> Message object type.
 */
public class XXXMessageRouterRunner<T> implements Runnable {

    /**
	 * Creates an instance.
	 * @param router Shared exposed router. Null if none. Note that messages
	 * received while there is no router will be lost.
	 * @param reader Shared exposed message reader, used to receive messages
	 * from the remote node. Never null. To assure thread safety all entities
	 * reading messages should use this reader (i.e. it is the thread lock).
	 * @param writer Shared exposed message writer, used by message handlers to
	 * send response messages to the remote node. Null if none (in which case it
	 * is assumed that the message handlers can get the writer through some
	 * other means). To assure thread safety all entities writing messages
	 * should use this writer (i.e. it is the thread lock).
	 */
    public XXXMessageRouterRunner(MessageRouter<T> router, MessageIOReader<? extends T> reader, MessageIOWriter<T> writer) {
        setRouter(router);
        _reader = reader;
        _writer = writer;
    }

    /**
	 * Called by the system to set the router hosting this router thread (the
	 * thread may die but the router will persist).
	 * @param router Shared exposed message router. If null, any received
	 * messages will be lost.
	 */
    public void setRouter(MessageRouter<T> router) {
        _router = router;
    }

    /**
	 * Gracefully stops this thread from running by setting a stop flag (see
	 * isStopping()) and closing the reader to break any blocked read.
	 */
    public final void pleaseStop() {
        _stop = true;
        try {
            _reader.close();
        } catch (Exception ex) {
        }
    }

    /**
	 * Returns true if pleaseStop() has been called. Used by thread implementors
	 * to determine when they should stop this thread.
	 */
    public final boolean isStopping() {
        return _stop;
    }

    /**
	 * Runs until the remote breaks the connection or the local closes the
	 * socket, by calling pleaseStop(). Catches all message handler and message
	 * format errors, and tries to keep going.
	 * @throws NetDataSocketFailedException if the connection breaks (e.g. local or remote
	 * node closed the socket).
	 * @throws NetDataStreamFailedException if the remote node shuts down the
	 * remote-to-local half of the socket.
	 */
    public void run() {
        try {
            T msg;
            while (!_stop) {
                try {
                    msg = _reader.readMessage();
                    if (_router != null) {
                        try {
                            _router.routeMessage(msg, _writer);
                        } catch (RuntimeException ex) {
                            System.err.println("MessageRouterThread: Problem in message handler.  Keep going...");
                            ex.printStackTrace();
                        }
                    }
                } catch (MessageIOException ex) {
                    System.err.println("MessageRouterThread: Bad message format.  Keep going...");
                    ex.printStackTrace();
                }
            }
        } finally {
            pleaseStop();
        }
    }

    private MessageIOReader<? extends T> _reader;

    private MessageIOWriter<T> _writer;

    private MessageRouter<T> _router = null;

    private boolean _stop = false;
}
