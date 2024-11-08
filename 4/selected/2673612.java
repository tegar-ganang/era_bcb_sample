package org.amse.bomberman.client.net.impl.netty;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.amse.bomberman.protocol.impl.ProtocolMessage;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

public class ClientHandler extends SimpleChannelUpstreamHandler {

    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

    private final ClientHandlerListener listener;

    public ClientHandler(ClientHandlerListener listener) {
        this.listener = listener;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (!(e.getMessage() instanceof ProtocolMessage)) {
            throw new RuntimeException("Wrong type of message");
        }
        listener.received((ProtocolMessage) e.getMessage());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        logger.log(Level.SEVERE, "Unexpected exception from downStream", e.getCause());
        e.getChannel().close().awaitUninterruptibly();
    }
}
