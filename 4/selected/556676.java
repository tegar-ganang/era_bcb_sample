package net.sf.opensmus.io;

import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.buffer.ChannelBuffer;
import net.sf.opensmus.*;

@ChannelHandler.Sharable
public class UDPIOHandler extends IOHandler {

    MUSServer m_server;

    ChannelGroup channels;

    public UDPIOHandler(MUSServer srv, ChannelGroup cg) {
        m_server = srv;
        channels = cg;
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
        channels.add(e.getChannel());
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        MUSUser whatUser = ((SMUSPipeline) ctx.getPipeline()).user;
        MUSLog.Log("UDP message arrived from " + whatUser.name(), MUSLog.kDeb);
        MUSMessage msg = new MUSLogonMessage();
        ChannelBuffer buffer = (ChannelBuffer) e.getMessage();
        msg.extractMUSMessage(buffer);
        msg.m_senderID = new MUSMsgHeaderString(whatUser.name());
        msg.m_udp = true;
        if (whatUser.m_udpcookie != msg.m_timeStamp) {
            MUSLog.Log("UDP cookie mismatch for " + whatUser.name(), MUSLog.kDeb);
            return;
        }
        whatUser.postMessage(msg);
    }
}
