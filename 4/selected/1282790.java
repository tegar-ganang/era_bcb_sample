package org.apache.http.impl.nio.reactor;

import java.nio.channels.SocketChannel;

/**
 * {@link SocketChannel} entry maintained by the I/O reactor. If the channel
 *  represents an outgoing client connection, this entry also contains the
 *  original {@link SessionRequestImpl} used to request it.
 *  
 *
 * @version $Revision: 744539 $
 *
 * @since 4.0
 */
public class ChannelEntry {

    private final SocketChannel channel;

    private final SessionRequestImpl sessionRequest;

    /**
     * Creates new ChannelEntry.
     * 
     * @param channel the channel
     * @param sessionRequest original session request. Can be <code>null</code>
     *   if the channel represents an incoming server-side connection.
     */
    public ChannelEntry(final SocketChannel channel, final SessionRequestImpl sessionRequest) {
        super();
        if (channel == null) {
            throw new IllegalArgumentException("Socket channel may not be null");
        }
        this.channel = channel;
        this.sessionRequest = sessionRequest;
    }

    /**
     * Creates new ChannelEntry.
     * 
     * @param channel the channel.
     */
    public ChannelEntry(final SocketChannel channel) {
        this(channel, null);
    }

    /**
     * Returns the original session request, if available. If the channel
     * entry represents an incoming server-side connection, returns 
     * <code>null</code>.
     * 
     * @return the original session request, if client-side channel,
     *  <code>null</code> otherwise. 
     */
    public SessionRequestImpl getSessionRequest() {
        return this.sessionRequest;
    }

    /**
     * Returns the original session request attachment, if available.
     * 
     * @return the original session request attachment, if available,
     *  <code>null</code> otherwise. 
     */
    public Object getAttachment() {
        if (this.sessionRequest != null) {
            return this.sessionRequest.getAttachment();
        } else {
            return null;
        }
    }

    /**
     * Returns the channel.
     * 
     * @return the channel.
     */
    public SocketChannel getChannel() {
        return this.channel;
    }
}
