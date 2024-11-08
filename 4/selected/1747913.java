package k9;

public abstract class CommunicationChannel {

    private final String channelID;

    public CommunicationChannel(String channelID) {
        this.channelID = channelID;
    }

    public abstract Object AddToCommunicationChannel(Object toAdd);

    public abstract Object ReadCommunicationChannel();

    public abstract Object PeekCommunicationChannelReader();

    public abstract Object PeekCommunicationChannelWriter();

    public abstract void ClearCommunicationChannelReader();

    public abstract void ClearCommunicationChannelWriter();

    public abstract boolean EmptyCommunicationChannel();

    protected String getChannelID() {
        return channelID;
    }
}
