package org.mobicents.tools.http.balancer;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * @author Vladimir Ralev (vladimir.ralev@jboss.org)
 *
 */
@ChannelPipelineCoverage("one")
public class HttpResponseHandler extends SimpleChannelUpstreamHandler {

    private static final Logger logger = Logger.getLogger(HttpResponseHandler.class.getCanonicalName());

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Channel channel = HttpChannelAssociations.channels.get(e.getChannel());
        if (channel != null) {
            channel.write(e.getMessage());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        logger.log(Level.SEVERE, "Error", e.getCause());
    }
}
