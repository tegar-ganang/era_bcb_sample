package coopnetserver.data.channel;

import coopnetserver.data.player.Player;
import java.util.HashMap;

public class ChannelData {

    private static HashMap<String, Channel> channels = new HashMap<String, Channel>();

    public static HashMap<String, Channel> getChannels() {
        return channels;
    }

    public static Channel getChannel(String ID) {
        return channels.get(ID);
    }

    public static void addChannel(Channel channel) {
        channels.put(channel.ID, channel);
    }

    public static void removePlayerFromAllChannels(Player player) {
        for (Channel ch : channels.values().toArray(new Channel[channels.size()])) {
            ch.removePlayer(player);
        }
    }

    public static void removeChannel(String channel) {
        Channel ch = channels.get(channel);
        channels.remove(ch.ID);
    }
}
