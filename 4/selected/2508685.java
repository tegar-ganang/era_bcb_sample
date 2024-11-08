package net.sf.opensmus.io;

import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.buffer.ChannelBuffer;
import net.sf.opensmus.*;
import java.net.InetSocketAddress;

@ChannelHandler.Sharable
public class LogonHandler extends IOHandler {

    MUSServer m_server;

    ChannelGroup channels;

    public LogonHandler(MUSServer srv, ChannelGroup cg) {
        m_server = srv;
        channels = cg;
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
        channels.add(e.getChannel());
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        String ip = ((InetSocketAddress) ctx.getChannel().getRemoteAddress()).getAddress().getHostAddress();
        MUSLog.Log("Client connection initialized : " + (m_server.m_clientlist.size() + 1) + " (" + ip + ")", MUSLog.kSrv);
        MUSUser newUser = new MUSUser(m_server, ctx.getChannel());
        ((SMUSPipeline) ctx.getPipeline()).user = newUser;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        MUSUser whatUser = ((SMUSPipeline) ctx.getPipeline()).user;
        MUSLogonMessage msg = new MUSLogonMessage();
        ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
        msg.extractMUSMessage(buffer);
        m_server.queueLogonMessage(msg, whatUser);
    }
}
