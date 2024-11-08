package gov.sns.ca;

/**
 *
 * @author  CKAllen
 * @version 
 */
public class ConnectionException extends ChannelException {

    private Channel _channel;

    /**
     * Creates new <code>ConnectionException</code> without detail message.
     */
    public ConnectionException() {
    }

    /**
     * Constructs an <code>ConnectionException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public ConnectionException(Channel channel, String msg) {
        super(msg);
        _channel = channel;
    }

    /**
     * Get the channel for which the connection exception was thrown.
     * @return The channel for which the connection exception was thrown.
     */
    public Channel getChannel() {
        return _channel;
    }
}
