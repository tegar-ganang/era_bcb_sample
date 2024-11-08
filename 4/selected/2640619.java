package org.spellwind.net;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.spellwind.task.TaskQueue;
import org.spellwind.task.impl.SessionCloseTask;
import org.spellwind.task.impl.SessionMessageTask;
import org.spellwind.task.impl.SessionOpenTask;

/**
 * A class which handles incoming events from Netty, and passes them on to the
 * appropriate parts of the server.
 * @author Graham Edgecombe
 */
@ChannelPipelineCoverage("all")
public final class MudServerHandler extends SimpleChannelUpstreamHandler {

    /**
	 * The logger instance.
	 */
    private static final Logger logger = Logger.getLogger(MudServerHandler.class.getName());

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent evt) throws Exception {
        MudSession session = new MudSession(ctx.getChannel());
        ctx.setAttachment(session);
        TaskQueue.enqueue(new SessionOpenTask(session));
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent evt) throws Exception {
        MudSession session = (MudSession) ctx.getAttachment();
        TaskQueue.enqueue(new SessionCloseTask(session));
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent evt) throws Exception {
        MudSession session = (MudSession) ctx.getAttachment();
        String message = (String) evt.getMessage();
        TaskQueue.enqueue(new SessionMessageTask(session, message));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent evt) throws Exception {
        logger.log(Level.WARNING, "Exception caught, closing channel...", evt.getCause());
        ctx.getChannel().close();
    }
}
