package org.jboss.netty.example.discard;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.WriteCompletionEvent;

/**
 * Handles a client-side channel.
 *
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
 *
 * @version $Rev: 2121 $, $Date: 2010-02-02 09:38:07 +0900 (Tue, 02 Feb 2010) $
 */
public class DiscardClientHandler extends SimpleChannelUpstreamHandler {

    private static final Logger logger = Logger.getLogger(DiscardClientHandler.class.getName());

    private final AtomicLong transferredBytes = new AtomicLong();

    private final byte[] content;

    public DiscardClientHandler(int messageSize) {
        if (messageSize <= 0) {
            throw new IllegalArgumentException("messageSize: " + messageSize);
        }
        content = new byte[messageSize];
    }

    public long getTransferredBytes() {
        return transferredBytes.get();
    }

    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        if (e instanceof ChannelStateEvent) {
            if (((ChannelStateEvent) e).getState() != ChannelState.INTEREST_OPS) {
                logger.info(e.toString());
            }
        }
        super.handleUpstream(ctx, e);
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        generateTraffic(e);
    }

    @Override
    public void channelInterestChanged(ChannelHandlerContext ctx, ChannelStateEvent e) {
        generateTraffic(e);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
    }

    @Override
    public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e) {
        transferredBytes.addAndGet(e.getWrittenAmount());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        logger.log(Level.WARNING, "Unexpected exception from downstream.", e.getCause());
        e.getChannel().close();
    }

    private void generateTraffic(ChannelStateEvent e) {
        Channel channel = e.getChannel();
        while (channel.isWritable()) {
            ChannelBuffer m = nextMessage();
            if (m == null) {
                break;
            }
            channel.write(m);
        }
    }

    private ChannelBuffer nextMessage() {
        return ChannelBuffers.wrappedBuffer(content);
    }
}
