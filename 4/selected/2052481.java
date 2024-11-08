package ch.iserver.ace.net.protocol;

import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.transport.tcp.TCPSession;

/**
 * Wrapper/Decorator class around a <code>Channel</code>. The MainConnection
 * is the main connection between two peers. All basic communication except the
 * document editing session goes over the main connection. Note that all instances
 * of MainConnection use the same MainRequestHandler instance to handle the received
 * requests.
 * 
 * @see ch.iserver.ace.net.protocol.MainRequestHandler
 * @see org.beepcore.beep.core.Channel
 */
public class MainConnection extends AbstractConnection {

    /**
	 * Constructor.
	 * 
	 * @param channel the channel belonging to this MainConnection
	 * @see Channel
	 */
    public MainConnection(Channel channel) {
        super(channel);
        setState((channel == null) ? STATE_INITIALIZED : STATE_ACTIVE);
        super.LOG = Logger.getLogger(MainConnection.class);
    }

    /**
	 * Closes the main connection. The state of the channel must be
	 * STATE_ACTIVE in order to close it. Otherwise it is ignored.
	 *
	 */
    public void close() {
        LOG.debug("--> close()");
        try {
            if (getState() == STATE_ACTIVE && getChannel().getRequestHandler() != null) {
                getChannel().close();
            }
        } catch (BEEPException be) {
            LOG.warn("could not close channel [" + be.getMessage() + "]");
        }
        setChannel(null);
        setReplyListener(null);
        setState(STATE_CLOSED);
        LOG.debug("<-- close()");
    }

    /**
	 * {@inheritDoc}
	 */
    public void cleanup() {
        LOG.debug("not used yet.");
    }

    /**
	 * {@inheritDoc}
	 */
    public void recover() throws RecoveryException {
        throw new RecoveryException();
    }

    /**
	 * {@inheritDoc}
	 */
    public String toString() {
        return "MainConnection( " + ((TCPSession) getChannel().getSession()).getSocket() + " )";
    }
}
