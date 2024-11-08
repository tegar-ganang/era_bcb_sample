package genericirc.irccore;

import java.util.EventObject;

/**
 *
 * @author Steve "Uru" West <uruwolf@users.sourceforge.net>
 * @version 2011-07-22
 */
public class ChannelCreatedEvent extends EventObject {

    private Channel channel;

    public ChannelCreatedEvent(Object source, Channel channel) {
        super(source);
        this.channel = channel;
    }

    /**
     * @return the channel
     */
    public Channel getChannel() {
        return channel;
    }
}
