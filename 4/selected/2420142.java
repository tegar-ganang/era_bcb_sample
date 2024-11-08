package fulmine.distribution.channel;

import fulmine.event.IEventManager;
import fulmine.event.system.AbstractSystemEvent;

/**
 * Raised when an {@link IChannel} is ready - this means the channel can now be
 * used. A channel can exist but not ready for use; only when it has
 * synchronised readiness with the peer channel can it be considered available
 * for use.
 * 
 * @author Ramon Servadei
 * 
 */
public class ChannelReadyEvent extends AbstractSystemEvent {

    /** The channel that is available */
    private final IChannel channel;

    /**
     * Standard constructor
     * 
     * @param context
     *            the context for event operations
     * @param channel
     *            the channel that has been created
     */
    public ChannelReadyEvent(IEventManager context, IChannel channel) {
        super(context);
        this.channel = channel;
    }

    /**
     * Get the channel that has become ready
     * 
     * @return the {@link IChannel} encapsulated by this event
     */
    public IChannel getChannel() {
        return channel;
    }

    @Override
    protected String getAdditionalToString() {
        return "channel=" + this.channel;
    }
}
