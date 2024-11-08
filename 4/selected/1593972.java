package charismata.broadcast;

import java.util.HashMap;
import java.util.Map;

public class ChannelRepository {

    private Map<String, Channel> channelMap;

    private static ChannelRepository instance;

    private ChannelRepository() {
        channelMap = new HashMap();
    }

    public static synchronized ChannelRepository getInstance() {
        if (instance == null) {
            instance = new ChannelRepository();
        }
        return instance;
    }

    public synchronized void deregisterChannel(String channelName) {
    }

    public synchronized Channel getChannel(String channelName) {
        Channel curChannel = channelMap.get(channelName);
        if (curChannel == null) {
            curChannel = new Channel(channelName);
            channelMap.put(channelName, curChannel);
        }
        return curChannel;
    }
}
