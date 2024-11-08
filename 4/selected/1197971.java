package com.clanwts.w3gs.server.test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import com.clanwts.bncs.bot.BattleNetChatBot;
import com.clanwts.bncs.bot.SimpleBattleNetChatBot;
import com.clanwts.bncs.client.BattleNetChatClientFactory;
import com.clanwts.bncs.codec.standard.messages.Platform;
import com.clanwts.bncs.codec.standard.messages.Product;
import com.clanwts.w3gs.codec.AbstractMessage;
import com.clanwts.w3gs.server.ClientConnectionEvent;
import com.clanwts.w3gs.server.ClientDisconnectionEvent;
import com.clanwts.w3gs.server.ClientMessageEvent;
import com.clanwts.w3gs.server.Gateway;
import com.clanwts.w3gs.server.GatewayListener;
import com.clanwts.w3gs.server.Warcraft3GameServer;
import edu.cmu.ece.agora.codecs.Message;

public class GatewayTest {

    private static final XLogger log = XLoggerFactory.getXLogger(GatewayTest.class);

    private static final int gamePort = 6666;

    /**
   * @param args
   */
    public static void main(String[] args) {
        BattleNetChatClientFactory fact124;
        fact124 = new BattleNetChatClientFactory();
        fact124.setPlatform(Platform.X86);
        fact124.setProduct(Product.W3TFT_1_24B);
        fact124.setKeys("XXXXXXXXXXXXXXXXXXXXXXXXXX", "XXXXXXXXXXXXXXXXXXXXXXXXXX");
        BattleNetChatBot bot = new TestBot(fact124, "clanwts.com", 6112, "TestBot", "password", true, "W3");
        bot.start();
    }

    private static class TestBot extends SimpleBattleNetChatBot {

        protected TestBot(BattleNetChatClientFactory fact, String host, int port, String user, String pass, boolean pvpgn, String channel) {
            super(fact, host, port, user, pass, pvpgn, channel);
        }

        @Override
        protected void onChannelChatReceived(String user, String message) {
            if (message.equalsIgnoreCase("!test")) {
                Warcraft3GameServer w3gs = new Warcraft3GameServer((byte) 10);
                Gateway gw = new W3gsGateway();
                w3gs.addGateway(gw);
                this.beginLobby("w00t");
            }
        }
    }

    private static class W3gsGateway extends Gateway {

        private Map<EventListener, Object> listeners;

        private Map<SocketAddress, Channel> channelsByAddress;

        public W3gsGateway() {
            super(gamePort);
            log.info("Starting W3GS gateway on port " + gamePort + ".");
            this.listeners = new HashMap<EventListener, Object>();
            this.channelsByAddress = new HashMap<SocketAddress, Channel>();
            ChannelFactory factory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
            ServerBootstrap b = new ServerBootstrap(factory);
            b.setOption("localAddress", new InetSocketAddress(port));
            b.setOption("reuseAddress", true);
            b.setOption("child.tcpNoDelay", true);
            b.setOption("child.receiveBufferSize", 1048576);
            ChannelPipeline cp = b.getPipeline();
            cp.addLast("decoder", new com.clanwts.w3gs.codec.standard.MessageDecoder(true));
            cp.addLast("encoder", new edu.cmu.ece.agora.codecs.MessageEncoder());
            cp.addLast("executor", new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(16, 1048576, 1048576)));
            cp.addLast("handler", new W3gsGateway.ChannelHandler());
            b.bind();
        }

        @Override
        public void sendMessage(InetSocketAddress address, AbstractMessage message) {
            Channel c = channelsByAddress.get(address);
            if (c == null) {
                return;
            }
            log.info("Sending W3GS message to " + address + " with id " + message.id());
            c.write(message);
        }

        @Override
        public void addListener(EventListener l) {
            listeners.put(l, null);
        }

        @Override
        public boolean isListener(EventListener l) {
            return listeners.containsKey(l);
        }

        @Override
        public void removeListener(EventListener l) {
            listeners.remove(l);
        }

        @ChannelPipelineCoverage("all")
        private class ChannelHandler extends SimpleChannelHandler {

            @Override
            public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
                InetSocketAddress remote = (InetSocketAddress) e.getChannel().getRemoteAddress();
                if (channelsByAddress.containsKey(remote)) {
                    return;
                }
                log.info("New W3GS connection from " + remote + ".");
                channelsByAddress.put(remote, e.getChannel());
                for (EventListener l : listeners.keySet()) {
                    if (l instanceof GatewayListener) {
                        ((GatewayListener) l).clientConnected(new ClientConnectionEvent(W3gsGateway.this, listeners.get(l), remote));
                    }
                }
            }

            @Override
            public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
                handleClientGone((InetSocketAddress) e.getChannel().getRemoteAddress());
            }

            private void handleClientGone(InetSocketAddress remote) {
                if (!channelsByAddress.containsKey(remote)) {
                    return;
                }
                log.info("Closed W3GS connection from " + remote + ".");
                channelsByAddress.remove(remote);
                for (EventListener l : listeners.keySet()) {
                    if (l instanceof GatewayListener) {
                        ((GatewayListener) l).clientDisconnected(new ClientDisconnectionEvent(W3gsGateway.this, listeners.get(l), remote));
                    }
                }
            }

            @Override
            public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
                log.warn("Exception in gateway: " + e.getCause().toString());
                log.warn("Closing channel.");
                e.getChannel().close();
            }

            @Override
            public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
                InetSocketAddress remote = (InetSocketAddress) e.getChannel().getRemoteAddress();
                if (!channelsByAddress.containsKey(remote)) {
                    return;
                }
                if (!(e.getMessage() instanceof AbstractMessage)) {
                    return;
                }
                log.info("Received W3GS message from " + remote + " with id " + ((AbstractMessage) e.getMessage()).id());
                for (EventListener l : listeners.keySet()) {
                    if (l instanceof GatewayListener) {
                        ((GatewayListener) l).clientMessage(new ClientMessageEvent(W3gsGateway.this, listeners.get(l), remote, (Message) e.getMessage()));
                    }
                }
            }
        }
    }
}
