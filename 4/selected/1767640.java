package ch.iserver.ace.net.impl.protocol;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import org.apache.log4j.Logger;
import org.beepcore.beep.core.Channel;
import org.beepcore.beep.core.OutputDataStream;
import org.beepcore.beep.core.ReplyListener;
import org.beepcore.beep.lib.NullReplyListener;
import org.beepcore.beep.util.BufferSegment;

/**
 * AbstractConnection contains all common functionality for a 
 * connection. A connection is basically a wrapper around a 
 * {@link org.beepcore.beep.core.Channel}.
 */
public abstract class AbstractConnection {

    protected Logger LOG = Logger.getLogger(AbstractConnection.class);

    public static final int STATE_INITIALIZED = 0;

    public static final int STATE_ACTIVE = 1;

    public static final int STATE_DIRTY = 2;

    public static final int STATE_ABORTED = 3;

    public static final int STATE_CLOSED = 4;

    private Channel channel;

    private ReplyListener listener;

    private int state = -1;

    /**
	 * Constructor.
	 * 
	 * @param channel the channel around which this connection is wrapped
	 */
    public AbstractConnection(Channel channel) {
        this.channel = channel;
    }

    /**
	 * Sends a message to the peer with whom this connection is established.
	 * 
	 * @param message 			the message to be sent
	 * @param data				additional in-process data for this channel, not sent to peer
	 * @param listener			the listener for responses sent by the peer
	 * @throws ProtocolException	thrown if an error occurs
	 */
    public synchronized void send(byte[] message, Object data, ReplyListener listener) throws ProtocolException {
        try {
            if (getState() == STATE_ACTIVE) {
                OutputDataStream output = DataStreamHelper.prepare(message);
                if (data != null) channel.setAppData(data);
                LOG.debug("--> sendMSG() with " + message.length + " bytes");
                ReplyListener actualListener = (listener != null) ? listener : NullReplyListener.getListener();
                channel.sendMSG(output, actualListener);
                LOG.debug("<-- sendMSG()");
            } else {
                LOG.warn("cannot send data, channel not in STATE_ACTIVE but in [" + getStateString() + "]");
            }
        } catch (Exception e) {
            state = STATE_ABORTED;
            String trace = getStackTrace(e);
            LOG.debug("caught exception [" + e + ", " + trace + "]");
            throw new ProtocolException(e.getMessage());
        }
    }

    /**
	 * Sets the channel for this connection.
	 * 
	 * @param channel the channel for this connection
	 */
    public synchronized void setChannel(Channel channel) {
        this.channel = channel;
    }

    /**
	 * Gets the channel for this connection.
	 * 
	 * @return the channel for this connection
	 */
    public Channel getChannel() {
        return channel;
    }

    /**
	 * Sets the ReplyListener for this connection
	 * 
	 * @param listener the ReplyListener for this connection
	 * @see ReplyListener
	 */
    public void setReplyListener(ReplyListener listener) {
        this.listener = listener;
    }

    /**
	 * Gets the ReplyListener for this connection.
	 * 
	 * @return the ReplyListener for this connection
	 */
    public ReplyListener getReplyListener() {
        return listener;
    }

    /**
	 * Returns the state in which this connection is.
	 * 
	 * @return the state of this connection
	 */
    public int getState() {
        return state;
    }

    /**
	 * Sets the state of this connection.
	 * 
	 * @param newState the state to be set for this connection
	 */
    public synchronized void setState(int newState) {
        this.state = newState;
    }

    /**
	 * Returns a state string for this connection.
	 * 
	 * @return the state string for this connection
	 */
    public String getStateString() {
        switch(state) {
            case STATE_ABORTED:
                return "aborted";
            case STATE_ACTIVE:
                return "active";
            case STATE_CLOSED:
                return "closed";
            case STATE_DIRTY:
                return "dirty";
            case STATE_INITIALIZED:
                return "initialized";
            default:
                return "unkown";
        }
    }

    /**
	 * Cleans up this connection and frees any resources.
	 *
	 */
    public abstract void cleanup();

    /**
	 * 
	 * @param e
	 * @return
	 */
    private String getStackTrace(Exception e) {
        ByteArrayOutputStream trace = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(trace);
        e.printStackTrace(pw);
        pw.close();
        return new String(trace.toByteArray());
    }
}
