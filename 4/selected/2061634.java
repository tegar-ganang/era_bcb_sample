package com.mapbased.sfw.common;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import com.mapbased.sfw.util.concurrent.JMXConfigurableThreadPoolExecutor;
import com.mapbased.sfw.util.logging.ESLogger;
import com.mapbased.sfw.util.logging.Loggers;

/**
 * <p>
 * Title: MA LUCENE
 * </p>
 * 
 * <p>
 * Description:
 * </p>
 * 
 * <p>
 * Copyright: Copyright (c) 2010
 * </p>
 * 
 * <p>
 * Company: woyo.com
 * </p>
 * 
 * @author changhuanyou
 * @version 1.0
 */
public abstract class BaseNioServer implements Server {

    final ChannelGroup allChannels = new DefaultChannelGroup("nio-server");

    protected ESLogger log;

    protected ServerBootstrap bootstrap;

    protected ChannelFactory channelFactory = null;

    public BaseNioServer() {
    }

    /**
	 * init
	 * 
	 * @todo Implement this com.woyo.search.common.Server method
	 */
    public void init() {
        log = Loggers.getLogger(this.serverName());
    }

    protected abstract ChannelPipelineFactory getChannelPipelineFactory();

    protected final SocketAddress getSocketAddress() {
        return new InetSocketAddress(this.defaultPort());
    }

    protected int defaultPort() {
        return 1122;
    }

    protected ChannelFactory createChannelFactory() {
        return new NioServerSocketChannelFactory(JMXConfigurableThreadPoolExecutor.newCachedThreadPool(this.serverName() + "-boss"), JMXConfigurableThreadPoolExecutor.newCachedThreadPool(this.serverName() + "-work"));
    }

    /**
	 * start
	 * 
	 * @todo Implement this com.woyo.search.common.Server method
	 */
    public void start() {
        this.channelFactory = this.createChannelFactory();
        bootstrap = new ServerBootstrap(channelFactory);
        bootstrap.setPipelineFactory(getChannelPipelineFactory());
        bootstrap.setOption("child.tcpNoDelay", Config.get().getBoolean(this.serverName() + "child.tcpNoDelay", true));
        bootstrap.setOption("child.keepAlive", Config.get().getBoolean(this.serverName() + "child.keepAlive", true));
        Channel serverChannel = bootstrap.bind(this.getSocketAddress());
        allChannels.add(serverChannel);
        log.info("server started at:" + this.getSocketAddress());
    }

    /**
	 * stop
	 * 
	 * @todo Implement this com.woyo.search.common.Server method
	 */
    public void stop() {
        allChannels.close().awaitUninterruptibly();
        if (channelFactory != null) {
            new Thread(this.serverName() + "-destory") {

                public void run() {
                    channelFactory.releaseExternalResources();
                }
            }.start();
        }
    }
}
