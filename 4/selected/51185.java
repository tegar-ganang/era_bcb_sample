package com.aionemu.chatserver.network.aion.clientpackets;

import java.util.Arrays;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import com.aionemu.chatserver.model.channel.Channel;
import com.aionemu.chatserver.model.channel.Channels;
import com.aionemu.chatserver.model.message.Message;
import com.aionemu.chatserver.network.aion.AbstractClientPacket;
import com.aionemu.chatserver.network.netty.handler.ClientChannelHandler;
import com.aionemu.chatserver.service.BroadcastService;

/**
 * 
 * @author ATracer
 */
public class CM_CHANNEL_MESSAGE extends AbstractClientPacket {

    @SuppressWarnings("unused")
    private static final Logger log = Logger.getLogger(CM_CHANNEL_MESSAGE.class);

    private int channelId;

    private byte[] content;

    /**
	 * 
	 * @param channelBuffer
	 * @param gameChannelHandler
	 * @param opCode
	 */
    public CM_CHANNEL_MESSAGE(ChannelBuffer channelBuffer, ClientChannelHandler gameChannelHandler) {
        super(channelBuffer, gameChannelHandler, 0x18);
    }

    @Override
    protected void readImpl() {
        readH();
        readC();
        readD();
        readD();
        channelId = readD();
        int lenght = readH() * 2;
        content = readB(lenght);
    }

    @Override
    protected void runImpl() {
        Channel channel = Channels.getChannelById(channelId);
        Message message = new Message(channel, content, clientChannelHandler.getChatClient());
        BroadcastService.getInstance().broadcastMessage(message);
    }

    @Override
    public String toString() {
        return "CM_CHANNEL_MESSAGE [channelId=" + channelId + ", content=" + Arrays.toString(content) + "]";
    }
}
