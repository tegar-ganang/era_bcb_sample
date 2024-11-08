package engine.clients;

import engine.QueueIF;
import engine.clients.client.PipelineFactory;
import engine.clients.client.Handler;
import engine.clients.client.ClientQueue;
import engine.clients.forwarder.ForwarderHandler;
import engine.clients.forwarder.ForwarderQueue;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

/**
 * The bocked loop thread object.
 * In each loop iteration bocked in q untill new data portion arrived,
 * then sent it.
 */
public class Client extends Thread {

    private volatile Thread self = null;

    private QueueIF q;

    private String host = "127.0.0.1";

    private Integer port = 8080;

    private Channel channel;

    private static Logger log = Logger.getLogger(Client.class.getName());

    public Client(QueueIF q) {
        this.q = q;
    }

    @Override
    public final void start() {
        if (self == null) {
            self = new Thread(this);
            self.start();
        }
    }

    public final void shutdown() {
        self = null;
    }

    @Override
    public void run() {
        Thread thisThread = Thread.currentThread();
        ClientBootstrap bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
        if (q instanceof ClientQueue) {
            Handler handler = new Handler();
            bootstrap.setPipelineFactory(new PipelineFactory(handler));
        }
        if (q instanceof ForwarderQueue) {
            ForwarderHandler handler = new ForwarderHandler();
            bootstrap.setPipelineFactory(new PipelineFactory(handler));
        }
        ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port));
        channel = future.awaitUninterruptibly().getChannel();
        if (!future.isSuccess()) {
            bootstrap.releaseExternalResources();
            self = null;
            return;
        }
        ChannelFuture lastWriteFuture = null;
        while (self == thisThread) {
            String s = "";
            final ChannelBuffer buf = q.get();
            if (buf.capacity() >= 5) {
                s = buf.toString(0, 5, "utf8");
            }
            if (s.equalsIgnoreCase("<bye>")) {
                break;
            } else {
                buf.readerIndex(0);
            }
            lastWriteFuture = channel.write(buf);
            lastWriteFuture.addListener(new ChannelFutureListener() {

                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        fireDataSentFromQueue(buf);
                    }
                }
            });
            try {
                Thread.sleep(1);
            } catch (InterruptedException ex) {
                log.error(ex.getMessage());
            }
            yield();
        }
        if (lastWriteFuture != null) {
            lastWriteFuture.awaitUninterruptibly();
        }
        channel.close().awaitUninterruptibly();
        bootstrap.releaseExternalResources();
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    protected javax.swing.event.EventListenerList CustomEventsListenerList = new javax.swing.event.EventListenerList();

    public void addListener(ClientEventsListenerIF listener) {
        CustomEventsListenerList.add(ClientEventsListenerIF.class, listener);
    }

    public void removeListener(ClientEventsListenerIF listener) {
        CustomEventsListenerList.remove(ClientEventsListenerIF.class, listener);
    }

    public void fireDataArrived(ChannelHandlerContext ctx, MessageEvent e) {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ClientEventsListenerIF.class) {
                ((ClientEventsListenerIF) listeners[i + 1]).DataArrived(ctx, e);
            }
        }
    }

    public void fireConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ClientEventsListenerIF.class) {
                ((ClientEventsListenerIF) listeners[i + 1]).Connected(ctx, e);
            }
        }
    }

    public void fireDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ClientEventsListenerIF.class) {
                ((ClientEventsListenerIF) listeners[i + 1]).Disconnected(ctx, e);
            }
        }
    }

    public void fireExceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ClientEventsListenerIF.class) {
                ((ClientEventsListenerIF) listeners[i + 1]).ExceptionCaught(ctx, e);
            }
        }
    }

    public void fireDataSentFromQueue(ChannelBuffer bufferedMessage) {
        Object[] listeners = CustomEventsListenerList.getListenerList();
        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ClientEventsListenerIF.class) {
                ((ClientEventsListenerIF) listeners[i + 1]).DataSentFromQueue(bufferedMessage);
            }
        }
    }
}
