package org.activision.net;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.activision.io.InStream;
import org.activision.model.World;
import org.activision.net.codec.ConnectionHandler;
import org.activision.net.codec.ConnectionWorker;
import org.activision.util.Logger;
import org.activision.net.packet.Packets;

@ChannelPipelineCoverage("all")
public class ServerChannelHandler extends SimpleChannelHandler {

    private ServerBootstrap bootstrap;

    public ServerChannelHandler() {
        bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
        bootstrap.getPipeline().addLast("handler", this);
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.bind(new InetSocketAddress(43595));
        Logger.log(this, "Server binded to port - 43595");
    }

    @Override
    public final void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        Channel channel = e.getChannel();
        ConnectionHandler connection = new ConnectionHandler(channel);
        ctx.setAttachment(connection);
    }

    @Override
    public final void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        ConnectionHandler p = (ConnectionHandler) ctx.getAttachment();
        if (p != null) {
            World.unRegisterConnection(p);
            ctx.setAttachment(null);
        }
    }

    @Override
    public final void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent ee) throws Exception {
    }

    @Override
    public final void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        ConnectionHandler p = (ConnectionHandler) ctx.getAttachment();
        ChannelBuffer buf = (ChannelBuffer) e.getMessage();
        buf.markReaderIndex();
        int avail = buf.readableBytes();
        if (avail > 5000) {
            e.getChannel().close();
            return;
        }
        byte[] b = new byte[avail];
        buf.readBytes(b);
        InStream in = new InStream(b);
        if (p.getPlayer() == null) {
            ConnectionWorker.run(p, in);
        } else {
            Packets.run(p, in);
        }
    }
}
