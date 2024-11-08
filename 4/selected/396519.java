package fulmine.distribution.events;

import fulmine.distribution.channel.Channel;
import fulmine.distribution.channel.IChannel;
import fulmine.event.IEventManager;
import fulmine.event.system.AbstractSystemEvent;

/**
 * Raised when a {@link Channel} has completed its destroy sequence.
 * 
 * @author Ramon Servadei
 */
public class ChannelDestroyedEvent extends AbstractSystemEvent {

    /** The channel that was destroyed */
    private final IChannel channel;

    /**
     * Standard constructor to encapsulate the destroyed channel
     * 
     * @param context
     *            the context for event operations
     * @param channel
     *            the channel that was destroyed
     */
    public ChannelDestroyedEvent(IEventManager context, IChannel channel) {
        super(context);
        this.channel = channel;
    }

    /**
     * Get the channel that was destroyed
     * 
     * @return the channel that was destroyed
     */
    public IChannel getChannel() {
        return this.channel;
    }

    @Override
    protected String getAdditionalToString() {
        return "channel=" + getChannel();
    }
}
