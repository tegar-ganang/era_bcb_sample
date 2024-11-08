package com.juant.market.source.socket;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.*;
import java.util.*;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.buffer.*;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import com.juant.market.*;
import com.juant.util.*;

/**
 * Specific implementation for juantIntegration.mq4 libraries.
 *
 * See code into mq4 folder.
 */
public class MQ4SocketServer extends SocketServer {

    private final MarketListener marketListener;

    private static final Logger log = Logger.getLogger(MQ4SocketServer.class.getPackage().getName());

    /**
     * Creates a socket server to receive messages from juantIntegration.mq4
     * library.
     *
     * @param marketListener market where incoming prices will be notified.
     * @param port server listening port.
     */
    public MQ4SocketServer(int port, final MarketListener marketListener) {
        super(port);
        this.marketListener = marketListener;
    }

    /**
     * Creates specific org.jboss.netty.channel.SimpleChannelUpstreamHandler
     * class to manipulate incoming messages.
     */
    protected SimpleChannelUpstreamHandler createHandler() {
        return new Handler(this.marketListener);
    }

    /**
     * Process messages in the format: <code/>
     *      Timestamp,Symbol,Bid,Ask</code>.
     *
     * Messages with lower timestamp than the last received will be discarded.
     */
    private static class Handler extends SimpleChannelUpstreamHandler {

        private final MarketListener marketListener;

        private long lastMoment;

        public Handler(final MarketListener marketListener) {
            this.marketListener = marketListener;
            this.lastMoment = -1;
        }

        @Override
        public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e) {
            final byte[] buffer = new byte[((ChannelBuffer) e.getMessage()).readableBytes()];
            ((ChannelBuffer) e.getMessage()).getBytes(0, buffer, 0, buffer.length);
            final String parts[] = new String(buffer).split(",");
            final long currentMoment = Long.parseLong(parts[0]);
            if (currentMoment <= lastMoment) {
                log.info("Received data out of expected time");
                return;
            }
            final Calendar cal = CalUtils.getZeroCalendar(currentMoment);
            this.marketListener.setBid(cal, parts[1], Double.parseDouble(parts[2]));
            this.marketListener.setAsk(cal, parts[1], Double.parseDouble(parts[3]));
        }

        @Override
        public void exceptionCaught(final ChannelHandlerContext ctx, final ExceptionEvent e) {
            System.err.println(e.getCause());
            e.getChannel().close();
        }
    }
}
