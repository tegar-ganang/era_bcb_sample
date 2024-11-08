package org.frameworkset.netty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.frameworkset.netty.Client.TestObject;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import org.jboss.netty.util.internal.ExecutorUtil;

/**
 * <p>Title: Server.java</p> 
 * <p>Description: </p>
 * <p>bboss workgroup</p>
 * <p>Copyright (c) 2008</p>
 * @Date 2010-4-17
 * @author biaoping.yin
 * @version 1.0
 */
public class Server {

    private ExecutorService executor;

    /**
	 * @param args
	 * @throws Throwable 
	 */
    public static void main(String[] args) throws Throwable {
        Server server = new Server();
        server.init();
        server.startup();
    }

    public void startup() throws Throwable {
        ServerBootstrap sb = new ServerBootstrap(newServerSocketChannelFactory(executor));
        EchoHandler sh = new EchoHandler();
        sb.getPipeline().addLast("decoder", new ObjectDecoder());
        sb.getPipeline().addLast("encoder", new ObjectEncoder());
        sb.getPipeline().addLast("handler", sh);
        Channel sc = sb.bind(new InetSocketAddress(3344));
        if (sh.exception.get() != null && !(sh.exception.get() instanceof IOException)) {
            throw sh.exception.get();
        }
        if (sh.exception.get() != null) {
            throw sh.exception.get();
        }
    }

    private class EchoHandler extends SimpleChannelUpstreamHandler {

        final AtomicReference<Throwable> exception = new AtomicReference<Throwable>();

        volatile int counter;

        EchoHandler() {
            super();
        }

        @Override
        public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
            executor.execute(new Runnable() {

                public void run() {
                    Object m = null;
                    if (e.getMessage() instanceof String) {
                        m = (String) e.getMessage();
                    }
                    if (e.getMessage() instanceof TestObject) {
                        TestObject m_ = (TestObject) e.getMessage();
                        m = m_.getId() + ":" + m_.getName();
                        System.out.println(m_.getId());
                        System.out.println(m_.getName());
                    }
                    e.getChannel().write("receive:" + m);
                    counter++;
                }
            });
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            if (exception.compareAndSet(null, e.getCause())) {
                e.getChannel().close();
            }
        }
    }

    protected ChannelFactory newServerSocketChannelFactory(Executor executor) {
        return new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
    }

    public void init() {
        executor = Executors.newCachedThreadPool();
    }

    public void destroy() {
        ExecutorUtil.terminate(executor);
    }
}
