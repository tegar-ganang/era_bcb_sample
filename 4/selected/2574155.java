package symbiosis.message;

import symbiosis.BaseManager;
import symbiosis.util.Util;
import java.util.HashMap;

public class ChannelManager extends BaseManager {

    private HashMap channelMap = null;

    public ChannelManager() {
        channelMap = new HashMap();
    }

    public void init() {
        addChannel(MessageConstants.CONSOLE_CHANNEL);
    }

    public synchronized MessageChannel addChannel(String name) {
        if (channelExists(name)) {
            return null;
        } else {
            MessageChannel channel = new MessageChannel(name);
            channelMap.put(name, channel);
            Util.debug("MessageChannel Added => " + name);
            return channel;
        }
    }

    public MessageChannel getChannel(String name) {
        return (MessageChannel) channelMap.get(name);
    }

    public synchronized MessageChannel removeChannel(String name) {
        if (channelExists(name)) {
            return (MessageChannel) channelMap.remove(name);
        } else {
            return null;
        }
    }

    public boolean channelExists(String name) {
        if (channelMap.get(name) != null) {
            return true;
        }
        return false;
    }
}
