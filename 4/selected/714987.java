package engine.server;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import util.Globals;
import org.apache.log4j.*;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.group.ChannelGroupFuture;

public class Server {

    /**
    DefaultChannelGroup requires the name of the group as a constructor parameter. The group
    name is solely used to distinguish one group from others.
     */
    private static final ChannelGroup allChannels = new DefaultChannelGroup("hercules-server");

    private static final HashMap<Integer, String> channelsArray = new HashMap<Integer, String>();

    private static Logger log = Logger.getLogger(Server.class.getName());

    private ChannelFactory factory = null;

    public boolean startListening(int port) {
        log.setLevel(Globals.getInstance().isAllowSTDOUTMessages() ? Level.DEBUG : Level.OFF);
        setFactory(new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
        ServerBootstrap bootstrap = new ServerBootstrap(getFactory());
        bootstrap.setPipelineFactory(new PipelineFactory());
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);
        bootstrap.setOption("child.reuseAddress", true);
        Channel channel = null;
        try {
            channel = bootstrap.bind(new InetSocketAddress(port));
            log.debug("Server started...");
            getAllChannels().add(channel);
            return true;
        } catch (org.jboss.netty.channel.ChannelException ex) {
            log.error("Bind failed...");
            return false;
        }
    }

    public void gracefullyClose() {
        try {
            ChannelGroupFuture future = Server.getAllChannels().close();
            future.awaitUninterruptibly();
            if (getFactory() != null) {
                getFactory().releaseExternalResources();
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ex) {
                log.error(ex);
            }
            log.debug("Server stopped...");
        } catch (java.lang.NullPointerException ex) {
            log.error(ex);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        gracefullyClose();
    }

    public static ChannelGroup getAllChannels() {
        return allChannels;
    }

    public ChannelFactory getFactory() {
        return factory;
    }

    public void setFactory(ChannelFactory factory) {
        this.factory = factory;
    }

    public static HashMap<Integer, String> getChannelsArray() {
        return channelsArray;
    }

    protected javax.swing.event.EventListenerList CustomEventsListenerList = new javax.swing.event.EventListenerList();

    public void addListener(ServerEventsListenerIF listener) {
        CustomEventsListenerList.add(ServerEventsListenerIF.class, listener);
    }

    public void removeListener(ServerEventsListenerIF listener) {
        CustomEventsListenerList.remove(ServerEventsListenerIF.class, listener);
    }

    public void fireDataArrived(ChannelHandlerContext ctx, MessageEvent e) {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ServerEventsListenerIF.class) {
                ((ServerEventsListenerIF) listeners[i + 1]).DataArrived(ctx, e);
            }
        }
    }

    public void fireChannelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ServerEventsListenerIF.class) {
                ((ServerEventsListenerIF) listeners[i + 1]).ChannelOpen(ctx, e);
            }
        }
    }

    public void fireChannelClose(ChannelHandlerContext ctx, ChannelStateEvent e) {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ServerEventsListenerIF.class) {
                ((ServerEventsListenerIF) listeners[i + 1]).ChannelClose(ctx, e);
            }
        }
    }

    public void fireExceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ServerEventsListenerIF.class) {
                ((ServerEventsListenerIF) listeners[i + 1]).ExceptionCaught(ctx, e);
            }
        }
    }
}
