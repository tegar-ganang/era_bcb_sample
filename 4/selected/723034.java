package com.bambamboo.st.socket.server.netty;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.ChildChannelStateEvent;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.WriteCompletionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.bambamboo.st.logging.Markers;
import com.bambamboo.st.util.HexString;

/**
 * DOCME
 * 
 * @author Nick.Tan
 * @since 2.2
 */
@ChannelPipelineCoverage("all")
public class HexDumpFilter implements ChannelUpstreamHandler, ChannelDownstreamHandler {

    private static final Logger logger = LoggerFactory.getLogger(HexDumpFilter.class);

    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        log(ctx, e);
        ctx.sendUpstream(e);
    }

    public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        log(ctx, e);
        ctx.sendDownstream(e);
    }

    protected void log(ChannelHandlerContext ctx, ChannelEvent e) {
        if (e instanceof MessageEvent) {
            Object msg = ((MessageEvent) e).getMessage();
            if (msg instanceof ChannelBuffer) {
                ChannelBuffer buffer = (ChannelBuffer) msg;
                if (e instanceof DownstreamMessageEvent) {
                    logger.info(Markers.STATUS, "outbound data to <{}>, data length: [{}]", ctx.getChannel(), buffer.readableBytes());
                    logger.debug(Markers.DUMP, "outbound data\n{}", HexString.dump(buffer));
                } else {
                    logger.info(Markers.STATUS, "inbound data from <{}>, data length: [{}]", ctx.getChannel(), buffer.readableBytes());
                    logger.debug(Markers.DUMP, "inbound data\n{}", HexString.dump(buffer));
                }
            } else {
                logger.info(Markers.STATUS, "recv msg <{}>", ctx.getChannel());
                logger.debug(Markers.DUMP, "recv msg {}", msg);
            }
        } else if (e instanceof WriteCompletionEvent) {
            WriteCompletionEvent evt = (WriteCompletionEvent) e;
            logger.debug(Markers.STATUS, "writeComplete: {}", evt.getWrittenAmount());
        } else if (e instanceof ChildChannelStateEvent) {
            ChildChannelStateEvent evt = (ChildChannelStateEvent) e;
            if (evt.getChildChannel().isOpen()) {
                logger.debug(Markers.STATUS, "childChannelOpen: {}", ctx.getChannel());
            } else {
                logger.debug(Markers.STATUS, "childChannelClosed: {}", ctx.getChannel());
            }
        } else if (e instanceof ChannelStateEvent) {
            ChannelStateEvent evt = (ChannelStateEvent) e;
            switch(evt.getState()) {
                case OPEN:
                    if (Boolean.TRUE.equals(evt.getValue())) {
                        logger.info(Markers.STATUS, "channelOpen: {}", ctx.getChannel());
                    } else {
                        logger.info(Markers.STATUS, "channelClosed: {}", ctx.getChannel());
                    }
                    break;
                case BOUND:
                    if (evt.getValue() != null) {
                        logger.debug(Markers.STATUS, "channelBound: {}", ctx.getChannel());
                    } else {
                        logger.debug(Markers.STATUS, "channelUnbound: {}", ctx.getChannel());
                    }
                    break;
                case CONNECTED:
                    if (evt.getValue() != null) {
                        logger.info(Markers.STATUS, "channelConnected: {}", ctx.getChannel());
                    } else {
                        logger.info(Markers.STATUS, "channelDisconnected: {}", ctx.getChannel());
                    }
                    break;
                case INTEREST_OPS:
                    logger.debug(Markers.STATUS, "channelInterestChanged");
                    break;
                default:
            }
        } else if (e instanceof ExceptionEvent) {
            logger.error(Markers.STATUS, "EXCEPTION on channel: {}", ((ExceptionEvent) e).getChannel());
            Throwable t = ((ExceptionEvent) e).getCause();
            logger.error(Markers.STATUS, t.getMessage(), t);
        }
    }
}
