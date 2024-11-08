package net.wsnware.network.proxy.tcp;

import net.wsnware.core.Message;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.netty.channel.Channel;

/**
 * Upstream handler for WSNWARE messages.
 * (De)Serialization is performed by Netty Object coder and Java itself.
 * 
 * Used by NettyServer, NettyClient.
 *
 * @author  Alessandro Polo <contact@alessandropolo.name>
 * @version 1.0.0
 * @date    2011-10-03
 */
public abstract class NettyMessageHandler extends SimpleChannelUpstreamHandler {

    protected final Logger logger;

    public NettyMessageHandler() {
        logger = Logger.getLogger(this.toString());
    }

    public NettyMessageHandler(final Logger handlerLog) {
        logger = handlerLog;
    }

    public abstract void messageReceived(Message msg, String remoteAddress);

    protected abstract void channelOpened(Channel channel);

    protected abstract void channelClosed(Channel channel);

    @Override
    public synchronized void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        if (e instanceof ChannelStateEvent && ((ChannelStateEvent) e).getState() != ChannelState.INTEREST_OPS) {
            logger.info(e.toString());
        }
        super.handleUpstream(ctx, e);
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        super.channelOpen(ctx, e);
        channelOpened(e.getChannel());
    }

    @Override
    public synchronized void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        logger.log(Level.FINEST, "Got Packet");
        try {
            if (e.getMessage() instanceof Message) {
                Message msg = (Message) e.getMessage();
                logger.log(Level.FINE, "Got Message #{0}", msg.msg_id);
                messageReceived(msg, e.getRemoteAddress().toString());
            } else if (e.getMessage() instanceof String) {
                logger.log(Level.SEVERE, "unmarshalling string data");
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "unmarshalling data", ex);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        logger.log(Level.WARNING, "Unexpected exception from downstream.", e.getCause());
        channelClosed(e.getChannel());
    }

    public Logger getLogger() {
        return logger;
    }
}
