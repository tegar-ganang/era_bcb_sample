package org.dreamspeak.lib.protocol.packets.inbound.reliablecontent;

import java.nio.ByteBuffer;
import org.dreamspeak.lib.data.Channel;
import org.dreamspeak.lib.data.ChannelList;
import org.dreamspeak.lib.protocol.ProtocolException;
import org.dreamspeak.lib.protocol.packets.ResponsibleForPacketTypes;

/**
 * TODO: Proper documentation
 * 
 * @author avithan
 */
@ResponsibleForPacketTypes(0x0073)
public class ChannelDelete extends ChannelListUpdate {

    public ChannelDelete(short packetType, ByteBuffer data) throws ProtocolException {
        super(packetType, data);
        channelId = (int) data.getShort(0x00);
        creatorId = data.getInt(0x02);
    }

    final int channelId;

    final int creatorId;

    public int getCreatorId() {
        return creatorId;
    }

    public int getChannelId() {
        return channelId;
    }

    /**
	 * Applies changes in this update to the channelList
	 * 
	 * @param channelList
	 */
    public void processUpdate(ChannelList channelList) throws ProtocolException {
        Channel channel = channelList.getChannelById(getChannelId());
        if (channel == null) {
            throw new ProtocolException("ChannelDelete for unknown channel with id " + getChannelId());
        }
        channel.removeChannel(channel);
    }
}
