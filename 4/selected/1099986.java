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
@ResponsibleForPacketTypes(0x0070)
public class ChannelTopicChange extends ChannelListUpdate {

    public ChannelTopicChange(short packetType, ByteBuffer data) throws ProtocolException {
        super(packetType, data);
        byte[] raw = data.array();
        channelId = data.getInt(0x00);
        userId = data.getInt(0x04);
        int pos = 0x08;
        StringBuilder rawNewTopic = new StringBuilder();
        while (pos < raw.length && raw[pos] != 0x00) {
            rawNewTopic.append((char) raw[pos]);
            pos++;
        }
        newTopic = rawNewTopic.toString();
    }

    final int userId;

    final int channelId;

    final String newTopic;

    public int getChannelId() {
        return channelId;
    }

    public int getUserId() {
        return userId;
    }

    public String getNewTopic() {
        return newTopic;
    }

    /**
	 * Applies changes in this update to the channelList
	 * 
	 * @param channelList
	 */
    public void processUpdate(ChannelList channelList) throws ProtocolException {
        Channel channel = channelList.getChannelById(getChannelId());
        if (channel == null) {
            throw new ProtocolException("ChannelTopicChange for unknown channel with id " + getChannelId());
        }
        channel.setTopic(getNewTopic());
    }
}
