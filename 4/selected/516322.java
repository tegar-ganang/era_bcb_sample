package edu.cmu.ece.agora.codecs;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

@ChannelPipelineCoverage("all")
public class MessageEncoder extends SimpleChannelHandler {

    @SuppressWarnings("unused")
    private static final XLogger log = XLoggerFactory.getXLogger("MessageEncoder");

    private static final int sliceLength = 1024;

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (!(e.getMessage() instanceof Message)) {
            ctx.sendDownstream(e);
            return;
        }
        Message m = (Message) e.getMessage();
        ChannelBuffer buf = ChannelBuffers.dynamicBuffer(ChannelBuffers.LITTLE_ENDIAN, m.size());
        m.writeHeaderTo(buf);
        m.writeTo(buf);
        m.writeFooterTo(buf);
        buf.markReaderIndex();
        byte[] msg_bytes = new byte[buf.readableBytes()];
        buf.readBytes(msg_bytes);
        buf.resetReaderIndex();
        log.debug("Sending message:");
        Strings.prettyPrintHex(log, msg_bytes);
        int fullSlices = buf.readableBytes() / sliceLength;
        boolean exact = (fullSlices * sliceLength == buf.readableBytes());
        for (int i = 0; i < fullSlices; i++) {
            ChannelFuture future = (exact && (i == fullSlices - 1)) ? e.getFuture() : Channels.future(e.getChannel());
            Channels.write(ctx, e.getChannel(), future, buf.readSlice(sliceLength), e.getRemoteAddress());
        }
        if (!exact) {
            buf.discardReadBytes();
            Channels.write(ctx, e.getChannel(), e.getFuture(), buf, e.getRemoteAddress());
        }
    }
}
