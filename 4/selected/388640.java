package org.dreamspeak.lib.protocol.packets.outbound.reliablecontent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.dreamspeak.lib.protocol.packets.PacketType;

/**
 * TODO: Proper documentation
 * 
 * @author avithan
 */
@PacketType(0x0005)
public final class SendLoginChannelContent extends SendReliableContent {

    @Override
    public byte[] getPayload() {
        ByteBuffer b = ByteBuffer.allocate(96);
        b.order(ByteOrder.LITTLE_ENDIAN);
        b.putShort(0, (short) 1);
        b.position(2);
        b.put((byte) channel.length());
        b.put(channel.getBytes());
        b.position(32);
        b.put((byte) subchannel.length());
        b.put(subchannel.getBytes());
        b.position(62);
        b.put((byte) channelPassword.length());
        b.put(channelPassword.getBytes());
        return b.array();
    }

    final String channel;

    final String subchannel;

    final String channelPassword;

    public SendLoginChannelContent(String channel, String subchannel, String channelPassword) {
        if (channel.length() > 30) throw new IllegalArgumentException("Param channel can not be longer than 29 chars.");
        if (subchannel.length() > 30) throw new IllegalArgumentException("Param subchannel can not be longer than 29 chars.");
        if (channelPassword.length() > 30) throw new IllegalArgumentException("Param channelPassword can not be longer than 29 chars.");
        this.channel = channel;
        this.subchannel = subchannel;
        this.channelPassword = channelPassword;
    }

    public SendLoginChannelContent() {
        this("", "", "");
    }

    public String getChannel() {
        return channel;
    }

    public String getSubchannel() {
        return subchannel;
    }

    public String getChannelPassword() {
        return channelPassword;
    }
}
