package org.bing.balancer.impl.netty;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

public class HttpProxyHandler extends SimpleChannelHandler {

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof ChannelBuffer) {
            ChannelBuffer buf = (ChannelBuffer) msg;
            byte[] bs = buf.array();
            System.out.println(new String(bs));
            ctx.getChannel().write(buf);
            e.getChannel().close();
        }
    }
}
