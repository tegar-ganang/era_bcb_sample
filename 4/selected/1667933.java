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
@ResponsibleForPacketTypes(0x0072)
public class ChannelDescriptionChange extends ChannelListUpdate {

    public ChannelDescriptionChange(short packetType, ByteBuffer data) throws ProtocolException {
        super(packetType, data);
        byte[] raw = data.array();
        channelId = data.getInt(0x00);
        userId = data.getInt(0x04);
        int pos = 0x08;
        StringBuilder rawNewDescription = new StringBuilder();
        while (pos < raw.length && raw[pos] != 0x00) {
            rawNewDescription.append((char) raw[pos]);
            pos++;
        }
        newDescription = rawNewDescription.toString();
    }

    final int userId;

    final int channelId;

    final String newDescription;

    public int getChannelId() {
        return channelId;
    }

    public int getUserId() {
        return userId;
    }

    public String getNewDescription() {
        return newDescription;
    }

    /**
	 * Applies changes in this update to the channelList
	 * 
	 * @param channelList
	 */
    public void processUpdate(ChannelList channelList) throws ProtocolException {
        Channel channel = channelList.getChannelById(getChannelId());
        if (channel == null) {
            throw new ProtocolException("ChannelDescriptionChange for unknown channel with id " + getChannelId());
        }
        channel.setTopic(getNewDescription());
    }
}
