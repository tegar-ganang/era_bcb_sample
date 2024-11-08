package charismata.broadcast;

public class ChannelEntry {

    private Container container;

    private ChannelProgram channelProgram;

    public ChannelEntry(Container container, ChannelProgram channelProgram) {
        super();
        this.container = container;
        this.channelProgram = channelProgram;
    }

    public Container getContainer() {
        return container;
    }

    public void setContainer(Container container) {
        this.container = container;
    }

    public ChannelProgram getChannelProgram() {
        return channelProgram;
    }

    public void setChannelProgram(ChannelProgram channelProgram) {
        this.channelProgram = channelProgram;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Container) {
            return (container == obj);
        }
        if (obj instanceof ChannelProgram) {
            return (channelProgram == obj);
        }
        return (this == obj);
    }
}
