package server;

import message.ClientMessage;
import message.ServerMessage;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;

@ChannelPipelineCoverage("all")
public class NettyServerHandler extends SimpleChannelHandler {

    private Channel channel;

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
        this.channel = e.getChannel();
        e.getChannel().getRemoteAddress();
        this.channel.getPipeline().addFirst("encoder", new ObjectEncoder());
        this.channel.getPipeline().addFirst("decoder", new ObjectDecoder());
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        this.channel.write(new ClientMessage("Welcome to mudtwenty, deployed on Netty using Java NIO", SystemColor.DEFAULT));
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {
        ServerMessage req = (ServerMessage) e.getMessage();
        ClientMessage res = new ClientMessage("Echo: " + req.getPayload(), SystemColor.ERROR);
        ctx.getChannel().write(res);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        e.getCause().printStackTrace();
        e.getChannel().close();
    }

    public void sendMessage(ClientMessage message) {
        try {
            this.channel.write(message);
        } catch (NullPointerException e) {
        }
    }
}
