package net.wsnware.network.proxy.tcp;

import net.wsnware.core.Message;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import org.jboss.netty.bootstrap.ClientBootstrap;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client socket for WSNWARE Network I/O.
 * Standard config-flow:
 *  - setMessageListener( .. )
 * 
 * Standard use-flow:
 *  - init()
 *  - connectTo(127.0.0.1", 9989)
 *  - disconnect()
 *  - deinit()
 * 
 * Note that NetworkIO component implement a standard generic socket
 * (may act as client or server) and it follows standard WSNWARE architecture.
 *
 * @author  Alessandro Polo <contact@alessandropolo.name>
 * @version 1.0.0
 * @date    2011-10-03
 */
public class NettyClient extends NettyHandler {

    protected final Logger logger;

    public NettyClient() {
        logger = Logger.getLogger(this.toString());
        initDefaults();
    }

    public NettyClient(final Logger handlerLog) {
        logger = handlerLog;
        initDefaults();
    }

    private void initDefaults() {
    }

    protected ClientBootstrap clientBootstrap;

    protected NettyMessageHandler nettyMessageHandler;

    @Override
    protected void init() {
        if (clientBootstrap != null) {
            logger.severe("Alreay Initialized");
            throw new RuntimeException("Alreay Initialized!");
        }
        logger.log(Level.FINE, "Initializing Client..");
        clientBootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
        nettyMessageHandler = new NettyMessageHandler(logger) {

            @Override
            protected void channelOpened(Channel channel) {
            }

            @Override
            protected void channelClosed(Channel channel) {
                disconnect();
            }

            @Override
            public void messageReceived(Message msg, String remoteAddress) {
                incomingMessages.incrementAndGet();
                synchronized (NettyClient.this) {
                    if (messageListener != null) messageListener.messageReceived(msg, remoteAddress);
                }
            }
        };
        clientBootstrap.setPipelineFactory(new ChannelPipelineFactory() {

            @Override
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(new ObjectEncoder(), new ObjectDecoder(), nettyMessageHandler);
            }
        });
    }

    @Override
    protected void deinit() {
        logger.log(Level.FINE, "Deinitializing Client..");
        if (clientBootstrap != null) {
            clientBootstrap.getFactory().releaseExternalResources();
            clientBootstrap = null;
        }
        nettyMessageHandler = null;
    }

    protected Channel channel;

    public synchronized boolean connectTo(String hostname, int port) {
        if (clientBootstrap == null) {
            logger.fine("Initialization during connectin..");
            init();
        }
        if (channel != null) {
            logger.severe("Already Connected!");
            return false;
        }
        logger.log(Level.INFO, "Cconnecting client to {0} at port {1}", new Object[] { hostname, port });
        ChannelFuture channelFuture = clientBootstrap.connect(new InetSocketAddress(hostname, port));
        if (!channelFuture.awaitUninterruptibly().isSuccess()) {
            clientBootstrap.releaseExternalResources();
            return false;
        }
        channel = channelFuture.getChannel();
        return channel.isConnected();
    }

    public synchronized void disconnect() {
        if (channel != null) {
            logger.log(Level.INFO, "Disconnecting client..");
            channel.close().awaitUninterruptibly();
            channel = null;
        }
    }

    @Override
    protected void close() {
        disconnect();
    }

    public String getRemoteName() {
        if (channel == null) return "";
        return channel.getRemoteAddress().toString();
    }

    public boolean isConnected() {
        if (channel == null) return false;
        return channel.isConnected();
    }

    public NettyMessageHandler getMessageHandler() {
        return nettyMessageHandler;
    }

    @Override
    public synchronized void deliverMessage(Message msg) {
        if (channel == null) {
            logger.log(Level.SEVERE, "Discarded (since not connected) Message# {0}", msg.msg_id);
            return;
        }
        try {
            channel.write(msg);
            outgoingMessages.incrementAndGet();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Discarded (unexpected) Message# " + msg.msg_id, ex);
        }
    }

    public static void main(String args[]) throws Exception {
        NettyClient client = new NettyClient();
        client.getLogger().setLevel(Level.ALL);
        client.connectTo("localhost", 9989);
        client.disconnect();
        client.deinit();
    }
}
