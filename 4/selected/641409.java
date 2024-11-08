package chatserver.network.aion.clientpackets;

import java.util.Arrays;
import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import chatserver.model.channel.Channel;
import chatserver.model.channel.Channels;
import chatserver.model.message.Message;
import chatserver.network.aion.AbstractClientPacket;
import chatserver.network.netty.handler.ClientChannelHandler;
import chatserver.service.BroadcastService;

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
