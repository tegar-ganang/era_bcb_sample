package ch.iserver.ace.net.impl.protocol;

import org.apache.log4j.Logger;
import org.beepcore.beep.core.BEEPException;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.transport.tcp.TCPSession;

/**
 * Wrapper/Decorator class around a <code>Channel</code>. The MainConnection
 * is the main connection between two peers. All basic communication except the
 * document editing goes over the main connection.
 * 
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
	 * Closes the main connection.
	 *
	 */
    public void close() {
        try {
            if (getState() == STATE_ACTIVE) {
                getChannel().close();
            }
        } catch (BEEPException be) {
            LOG.warn("could not close channel [" + be.getMessage() + "]");
        }
        setChannel(null);
        setReplyListener(null);
        setState(STATE_CLOSED);
    }

    public void cleanup() {
        LOG.debug("not used yet.");
    }

    public String toString() {
        return "MainConnection( " + ((TCPSession) getChannel().getSession()).getSocket() + " )";
    }
}
