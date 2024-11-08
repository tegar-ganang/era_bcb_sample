package chatserver.network.aion.serverpackets;

import org.jboss.netty.buffer.ChannelBuffer;
import chatserver.model.channel.Channel;
import chatserver.network.aion.AbstractServerPacket;
import chatserver.network.netty.handler.ClientChannelHandler;

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
