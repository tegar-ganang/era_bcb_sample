package net.sf.opensmus.io;

import org.jboss.netty.channel.*;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.handler.timeout.IdleStateEvent;
import org.jboss.netty.handler.timeout.IdleStateAwareChannelUpstreamHandler;
import net.sf.opensmus.*;
import java.nio.channels.ClosedChannelException;
import java.io.IOException;

@ChannelHandler.Sharable
public class IOHandler extends IdleStateAwareChannelUpstreamHandler {

    public IOHandler() {
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        ctx.getChannel().close().addListener(MUSUser.REPORT_CLOSE);
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        MUSUser whatUser = ((SMUSPipeline) ctx.getPipeline()).user;
        whatUser.killMUSUser();
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object incoming = e.getMessage();
        if (!(incoming instanceof ChannelBuffer)) {
            System.out.println("Illegal messageReceived: " + e + e.getMessage());
            return;
        }
        ChannelBuffer buffer = (ChannelBuffer) incoming;
        MUSMessage msg = new MUSMessage(buffer);
        MUSUser whatUser = ((SMUSPipeline) ctx.getPipeline()).user;
        msg.m_senderID = new MUSMsgHeaderString(whatUser.name());
        whatUser.postMessage(msg);
    }

    @Override
    public void channelIdle(ChannelHandlerContext ctx, IdleStateEvent e) throws Exception {
        MUSUser whatUser = ((SMUSPipeline) ctx.getPipeline()).user;
        whatUser.deleteUser();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        Channel ch = e.getChannel();
        MUSUser whatUser = ((SMUSPipeline) ch.getPipeline()).user;
        Throwable cause = e.getCause();
        if (!(cause instanceof ClosedChannelException || cause instanceof IOException)) {
            MUSLog.Log("Netty Exception " + cause + " for " + whatUser, MUSLog.kDeb);
            cause.printStackTrace();
        }
        ch.close().addListener(MUSUser.REPORT_CLOSE);
    }
}
