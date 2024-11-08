package identifierMessageBus.bus;

/**
 * This message is distributed to all receivers registered to a distribution
 * channel. It is initialized with a channel identifier.
 * 
 * @author Moritz Hoffmann
 * 
 */
public class DistributorMessage extends MessageType {

    private final long channel;

    public DistributorMessage(long channel) {
        this.channel = channel;
    }

    public long getChannel() {
        return channel;
    }
}
