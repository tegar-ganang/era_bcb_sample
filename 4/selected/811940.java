package org.amse.bomberman.common.net.netty.handlers;

import java.nio.charset.Charset;
import java.util.List;
import org.amse.bomberman.protocol.impl.ProtocolMessage;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

/**
 *
 * @author Kirilchuk V.E.
 */
public class ProtocolMessageEncoder extends OneToOneEncoder {

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        if (!(msg instanceof ProtocolMessage)) {
            return msg;
        }
        ProtocolMessage message = (ProtocolMessage) msg;
        List<String> data = message.getData();
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        writeHeader(buffer, message.getMessageId(), data.size());
        writeData(buffer, data);
        return buffer;
    }

    private void writeHeader(ChannelBuffer buffer, int messageId, int size) {
        buffer.writeInt(messageId);
        buffer.writeInt(size);
    }

    private void writeData(ChannelBuffer buffer, List<String> data) {
        for (String string : data) {
            ChannelBuffer buff = ChannelBuffers.copiedBuffer(string, Charset.forName("UTF-8"));
            buffer.writeShort(buff.readableBytes());
            buffer.writeBytes(buff);
        }
    }
}
