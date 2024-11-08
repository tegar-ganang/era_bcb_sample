package org.dreamspeak.lib.data;

import java.nio.ByteBuffer;

/**
 * TODO: Proper documentation
 * 
 * @author avithan
 */
public class ChannelList {

    public static final int CHANNEL_AT_ROOTLEVEL = 0xFFFFFFFF;

    public class RootChannel extends Channel {

        private RootChannel() {
            super(-1, "root");
        }
    }

    private final Channel root;

    public Channel[] getToplevelChannels() {
        return root.getSubChannels();
    }

    public void clear() {
        root.clearSubchannelsRecursive();
    }

    public Channel getChannelById(int id) {
        return root.getChannelByIdRecursive(id);
    }

    private Channel defaultChannel;

    public Channel getDefaultChannel() {
        return defaultChannel;
    }

    public void admCreateChannel(Channel parent, String name) throws SecurityException {
        throw new RuntimeException("Not yet implemented o.x *drop*");
    }

    public void addToplevelChannel(Channel channel) {
        root.addChannel(channel);
        if (channel.getFlags().contains(ChannelAttributeSet.Flag.IsDefault)) {
            defaultChannel = channel;
        }
    }

    public void removeToplevelChannel(Channel channel) {
        root.removeChannel(channel);
    }

    public void sortAll() {
        root.sortRecursive();
    }

    /**
	 * Creates a new, empty ChannelList
	 */
    public ChannelList() {
        root = new RootChannel();
    }

    int waitingForFrame = -1;

    ByteBuffer reassembler;
}
