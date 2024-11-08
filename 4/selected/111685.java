package org.dreamspeak.lib.protocol.packets.inbound.reliablecontent;

import java.nio.ByteBuffer;
import org.dreamspeak.lib.data.Channel;
import org.dreamspeak.lib.data.ChannelAttributeSet;
import org.dreamspeak.lib.data.ChannelList;
import org.dreamspeak.lib.protocol.ProtocolException;
import org.dreamspeak.lib.protocol.packets.ResponsibleForPacketTypes;

/**
 * TODO: Proper documentation
 * 
 * @author avithan
 */
@ResponsibleForPacketTypes(0x006e)
public class ChannelAdded extends ChannelListUpdate {

    public ChannelAdded(short packetType, ByteBuffer data) throws ProtocolException {
        super(packetType, data);
        byte[] raw = data.array();
        creatorId = data.getInt(0x00);
        channelId = data.getInt(0x04);
        rawChannelAttributes = data.get(0x08);
        rawCodec = data.get(0x0A);
        parentChannelId = data.getInt(0x0C);
        order = data.getShort(0x10);
        maxUsers = data.getShort(0x12);
        int pos = 0x14;
        StringBuilder rawName = new StringBuilder();
        StringBuilder rawTopic = new StringBuilder();
        StringBuilder rawDescription = new StringBuilder();
        while (pos < raw.length && raw[pos] != 0x00) {
            rawName.append((char) raw[pos]);
            pos++;
        }
        pos++;
        while (pos < raw.length && raw[pos] != 0x00) {
            rawTopic.append((char) raw[pos]);
            pos++;
        }
        pos++;
        while (pos < raw.length && raw[pos] != 0x00) {
            rawDescription.append((char) raw[pos]);
            pos++;
        }
        name = rawName.toString();
        topic = rawName.toString();
        description = rawName.toString();
    }

    final byte rawChannelAttributes;

    final byte rawCodec;

    final int creatorId;

    final int channelId;

    final int parentChannelId;

    final short order;

    final short maxUsers;

    final String name;

    final String topic;

    final String description;

    public byte getRawChannelAttributes() {
        return rawChannelAttributes;
    }

    public int getCreatorId() {
        return creatorId;
    }

    public int getParentChannelId() {
        return parentChannelId;
    }

    public short getOrder() {
        return order;
    }

    public short getMaxUsers() {
        return maxUsers;
    }

    public String getName() {
        return name;
    }

    public String getTopic() {
        return topic;
    }

    public String getDescription() {
        return description;
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
        Channel channel = new Channel(getChannelId(), getName());
        channel.setTopic(getDescription());
        channel.setDescription(getDescription());
        channel.setMaxUsers(getMaxUsers());
        channel.setOrder(getOrder());
        channel.getFlags().addAll(ChannelAttributeSet.fromByte(getRawChannelAttributes()));
        Channel parentChannel = channelList.getChannelById(getChannelId());
        if (parentChannel == null) {
            channelList.addToplevelChannel(channel);
        } else {
            parentChannel.addChannel(channel);
        }
    }
}
