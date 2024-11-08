package org.iosgi.outpost;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sven Schulz
 */
public class ServerHandler extends SimpleChannelUpstreamHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerHandler.class.getName());

    @SuppressWarnings("unchecked")
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent me) {
        Object obj = me.getMessage();
        if (!(obj instanceof Operation<?>)) {
            me.getChannel().close();
        }
        Channel c = me.getChannel();
        Operation<Object> op = (Operation<Object>) obj;
        try {
            Object result = op.perform();
            if (result == null) {
                c.write(Null.NULL);
            } else {
                c.write(result);
            }
        } catch (Exception e) {
            OperationExecutionException oee = new OperationExecutionException(e);
            c.write(oee);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        LOGGER.warn("Unexpected exception from downstream.", e.getCause());
        e.getChannel().close();
    }
}
