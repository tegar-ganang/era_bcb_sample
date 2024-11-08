package com.clanwts.w3gs.client;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import edu.cmu.ece.agora.futures.FutureListener;
import edu.cmu.ece.agora.futures.FutureManager;

public class InternetLinkManager extends AbstractLinkManager<InternetEndpoint> {

    private final ChannelFactory server_cf;

    private final ChannelFactory client_cf;

    private Channel server_ch;

    public InternetLinkManager(InternetEndpoint local, Executor exec) {
        super(local, exec);
        this.server_cf = new NioServerSocketChannelFactory(exec, exec);
        this.client_cf = new NioClientSocketChannelFactory(exec, exec);
        setupServer();
        doConnect();
    }

    private void setupClientPipeline(ChannelPipeline p, FutureManager<Link<InternetEndpoint>> fm) {
        p.addLast("clientHandler", new ClientChannelHandler(fm));
    }

    private void setupServerPipeline(ChannelPipeline p) {
        p.addLast("serverHandler", new ServerChannelHandler());
    }

    private void setupServer() {
        ServerBootstrap sbs = new ServerBootstrap(server_cf);
        sbs.setOption("reuseAddress", true);
        sbs.setOption("child.tcpNoDelay", true);
        sbs.setOption("child.keepAlive", true);
        setupServerPipeline(sbs.getPipeline());
        this.server_ch = sbs.bind(getLocalEndpoint().getAddress());
    }

    private void doConnect() {
        pollConnect().addListener(new FutureListener<ConnectRequest>() {

            @Override
            public void onCancellation(Throwable cause) {
                server_ch.close();
            }

            @Override
            public void onCompletion(final ConnectRequest req) {
                ClientBootstrap cbs = new ClientBootstrap(client_cf);
                cbs.setOption("tcpNoDelay", true);
                cbs.setOption("keepAlive", true);
                setupClientPipeline(cbs.getPipeline(), req.fm);
                cbs.connect(req.ep.getAddress()).addListener(new ChannelFutureListener() {

                    @Override
                    public void operationComplete(ChannelFuture f) throws Exception {
                        if (!f.isSuccess()) req.fm.cancelFuture(f.getCause());
                    }
                });
                doConnect();
            }
        });
    }

    @ChannelPipelineCoverage("all")
    private class ClientChannelHandler extends SimpleChannelHandler {

        private final FutureManager<Link<InternetEndpoint>> fm;

        public ClientChannelHandler(FutureManager<Link<InternetEndpoint>> fm) {
            this.fm = fm;
        }

        @Override
        public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            Channel c = ctx.getChannel();
            c.setReadable(false);
            c.getPipeline().remove(ClientChannelHandler.class);
            InternetEndpoint remote_ep = new InternetEndpoint((InetSocketAddress) c.getRemoteAddress());
            InternetLink link = new InternetLink(InternetLinkManager.this, getLocalEndpoint(), remote_ep, c, getExecutor());
            fm.completeFuture(link);
        }
    }

    @ChannelPipelineCoverage("all")
    private class ServerChannelHandler extends SimpleChannelHandler {

        @Override
        public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            Channel c = ctx.getChannel();
            c.setReadable(false);
            c.getPipeline().remove(ServerChannelHandler.class);
            InternetEndpoint remote_ep = new InternetEndpoint((InetSocketAddress) c.getRemoteAddress());
            InternetLink link = new InternetLink(InternetLinkManager.this, getLocalEndpoint(), remote_ep, c, getExecutor());
            acceptCompleted(link);
        }

        @Override
        public void channelUnbound(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            acceptClosed(new Exception("Channel unbound."));
            server_ch.close();
        }
    }
}
