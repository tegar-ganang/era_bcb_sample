package charismata.broadcast;

public abstract class ChannelProgram {

    public abstract void hook(Container container, Channel channel);

    public abstract void unhook(Container container, Channel channel);

    public abstract void listen(Container container, Channel channel, BroadcastInfo bi);

    public boolean equals(Object obj) {
        if (obj instanceof ChannelEntry) {
            ChannelEntry channelEntry = (ChannelEntry) obj;
            return channelEntry.getChannelProgram() == this;
        }
        return this == obj;
    }
}
