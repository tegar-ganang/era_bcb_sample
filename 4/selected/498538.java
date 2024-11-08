package charismata.broadcast;

public class BroadcastInfo {

    private Channel channel;

    private Object broadcastObj;

    public BroadcastInfo(String channelName, Object broadcastObj) {
        ChannelRepository channelRepository = ChannelRepository.getInstance();
        this.channel = channelRepository.getChannel(channelName);
        this.broadcastObj = broadcastObj;
    }

    public Channel getChannel() {
        return channel;
    }

    public Object getBroadcastObj() {
        return broadcastObj;
    }
}
