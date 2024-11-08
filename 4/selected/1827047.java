package goldengate.common.future;

import org.jboss.netty.channel.Channel;

/**
 * Future that hold a channel as result
 *
 * @author Frederic Bregier
 *
 */
public class GgChannelFuture extends GgFuture {

    /**
     * Channel as result
     */
    private Channel channel = null;

    /**
     *
     */
    public GgChannelFuture() {
        super();
    }

    /**
     * @param cancellable
     */
    public GgChannelFuture(boolean cancellable) {
        super(cancellable);
    }

    /**
     * @return the channel as result
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * @param channel
     *            the channel to set
     */
    public void setChannel(Channel channel) {
        this.channel = channel;
    }
}
