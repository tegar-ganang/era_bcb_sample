package jelb.common;

import java.util.ArrayList;
import jelb.netio.Uint32;
import jelb.netio.Uint8;
import jelb.netio.Protocol.Channel;

public class OpenChannels {

    private static final int MAX_OPEN_CHANNELS = 3;

    private long[] openChannels;

    private int activeChannelIndex;

    public OpenChannels(ArrayList<Uint32> openChannels, Uint8 activeChannelIndex) {
        this.openChannels = new long[openChannels.size()];
        for (int index = 0; index < openChannels.size(); index++) {
            this.openChannels[index] = openChannels.get(index).toLong();
        }
        this.activeChannelIndex = activeChannelIndex.toInt();
    }

    public long[] getOpenChannels() {
        return this.openChannels;
    }

    public long getActiveChannel() {
        return this.openChannels[this.activeChannelIndex];
    }

    public boolean hasFreeChannelToOpen() {
        for (int i = 0; i < this.openChannels.length; i++) if (this.openChannels[i] == 0) return true;
        return this.openChannels.length >= MAX_OPEN_CHANNELS;
    }

    public boolean contains(Channel channel) {
        return this.getChannelIndex(channel) != null;
    }

    public Integer getChannelIndex(Channel channel) {
        for (int i = 0; i < this.openChannels.length; i++) if (channel == Channel.Guild && this.openChannels[i] > 1000000000) return i; else if (this.openChannels[i] == channel.getId()) return i;
        return null;
    }
}
