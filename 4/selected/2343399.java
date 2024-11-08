package com.aionemu.chatserver.network.aion.serverpackets;

import org.jboss.netty.buffer.ChannelBuffer;
import com.aionemu.chatserver.model.ChatClient;
import com.aionemu.chatserver.model.channel.Channel;
import com.aionemu.chatserver.network.aion.AbstractServerPacket;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler;

/**
 * 
 * @author ATracer
 *
 */
public class SM_CHANNEL_RESPONSE extends AbstractServerPacket {

    private Channel channel;

    private ChatClient chatClient;

    public SM_CHANNEL_RESPONSE(ChatClient chatClient, Channel channel) {
        super(0x11);
        this.chatClient = chatClient;
        this.channel = channel;
    }

    @Override
    protected void writeImpl(ClientChannelHandler cHandler, ChannelBuffer buf) {
        writeC(buf, getOpCode());
        writeC(buf, 0x40);
        writeH(buf, chatClient.nextIndex());
        writeH(buf, 0x00);
        writeD(buf, channel.getChannelId());
    }
}
