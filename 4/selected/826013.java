package charismata.broadcast;

public class ChannelProgramEntry {

    private Container container;

    private Channel channel;

    private ChannelProgram channelProgram;

    public ChannelProgramEntry(Container container, Channel channel, ChannelProgram channelProgram) {
        this.container = container;
        this.channel = channel;
        this.channelProgram = channelProgram;
    }

    public Container getContainer() {
        return container;
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public ChannelProgram getChannelProgram() {
        return channelProgram;
    }

    public void setChannelProgram(ChannelProgram channelProgram) {
        this.channelProgram = channelProgram;
    }
}
