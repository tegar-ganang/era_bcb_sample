package gumbo.net.msg;

import gumbo.core.debug.Debug;
import gumbo.core.util.AssertUtils;
import gumbo.net.NetDataSocketFailedException;
import gumbo.net.NetDataStreamFailedException;
import gumbo.net.NetProps;

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
public class MessageRouterRunner<T> implements Runnable {

    /**
	 * Creates an instance.
	 * @param router Shared exposed router. Null if none. Note that messages
	 * received while there is no router will be lost.
	 * @param reader Shared exposed message reader, used to receive messages
	 * from the remote node. Never null.
	 * @param writer Shared exposed message writer, used by message handlers to
	 * send response messages to the remote node. Null if none (in which case it
	 * is assumed that the message handlers can get the writer through some
	 * other means).
	 */
    public MessageRouterRunner(MessageRouter<T> router, MessageIOReader<? extends T> reader, MessageIOWriter<T> writer) {
        AssertUtils.assertNonNullArg(reader);
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
            Debug.getDebugger(NetProps.DEBUG_NET_ROUTE).getReporter().pushTrace("MessageRouterRunner.run:" + " route loop begin. thread=" + Thread.currentThread());
            T msg;
            while (!_stop) {
                try {
                    try {
                        Debug.getDebugger(NetProps.DEBUG_NET_ROUTE).getReporter().pushTrace("MessageRouterRunner.run:" + " read wait begin. thread=" + Thread.currentThread());
                        msg = _reader.readMessage();
                    } finally {
                        Debug.getDebugger(NetProps.DEBUG_NET_ROUTE).getReporter().popTrace("MessageRouterRunner.run:" + " read wait end. thread=" + Thread.currentThread());
                    }
                    if (_router != null) {
                        try {
                            Debug.getDebugger(NetProps.DEBUG_NET_ROUTE).getReporter().pushTrace("MessageRouterRunner.run:" + " read route begin. thread=" + Thread.currentThread());
                            _router.routeMessage(msg, _writer);
                        } catch (RuntimeException ex) {
                            System.err.println("MessageRouterThread: Problem in message handler.  Keep going...");
                            ex.printStackTrace();
                        } finally {
                            Debug.getDebugger(NetProps.DEBUG_NET_ROUTE).getReporter().popTrace("MessageRouterRunner.run:" + " read route end. thread=" + Thread.currentThread());
                        }
                    }
                } catch (MessageIOException ex) {
                    System.err.println("MessageRouterThread: Bad message format.  Keep going...");
                    ex.printStackTrace();
                }
            }
        } finally {
            Debug.getDebugger(NetProps.DEBUG_NET_ROUTE).getReporter().popTrace("MessageRouterRunner.run:" + " route loop end. thread=" + Thread.currentThread());
            pleaseStop();
        }
    }

    private MessageIOReader<? extends T> _reader;

    private MessageIOWriter<T> _writer;

    private MessageRouter<T> _router = null;

    private boolean _stop = false;
}
