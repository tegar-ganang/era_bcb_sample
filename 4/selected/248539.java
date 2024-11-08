package org.traccar;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

/**
 * Tracker server
 */
public class TrackerServer extends ServerBootstrap {

    /**
     * Initialization
     */
    private void init(Integer port, Integer threadPoolSize) {
        setPort(port);
        setFactory(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
    }

    public TrackerServer(Integer port) {
        init(port, 1);
    }

    /**
     * Server port
     */
    private Integer port;

    public Integer getPort() {
        return port;
    }

    private void setPort(Integer newPort) {
        port = newPort;
    }

    /**
     * Opened channels
     */
    private ChannelGroup allChannels = new DefaultChannelGroup();

    public ChannelGroup getChannelGroup() {
        return allChannels;
    }

    /**
     * Start server
     */
    public void start() {
        Channel channel = bind(new InetSocketAddress(getPort()));
        getChannelGroup().add(channel);
    }

    /**
     * Stop server
     */
    public void stop() {
        ChannelGroupFuture future = getChannelGroup().close();
        future.awaitUninterruptibly();
        getFactory().releaseExternalResources();
    }
}
