package com.aionengine.chatserver.network.aion.serverpackets;

import org.jboss.netty.buffer.ChannelBuffer;
import com.aionengine.chatserver.model.channel.Channel;
import com.aionengine.chatserver.network.aion.AbstractServerPacket;
import com.aionengine.chatserver.network.netty.handler.ClientChannelHandler;

/**
 * 
 * @author ATracer
 *
 */
public class SM_CHANNEL_RESPONSE extends AbstractServerPacket {

    private Channel channel;

    private int channelIndex;

    public SM_CHANNEL_RESPONSE(Channel channel, int channelIndex) {
        super(0x11);
        this.channel = channel;
        this.channelIndex = channelIndex;
    }

    @Override
    protected void writeImpl(ClientChannelHandler cHandler, ChannelBuffer buf) {
        writeC(buf, getOpCode());
        writeC(buf, 0x40);
        writeH(buf, channelIndex);
        writeH(buf, 0x00);
        writeD(buf, channel.getChannelId());
    }
}
