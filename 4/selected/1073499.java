package edu.cmu.ece.agora.link.internet;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import com.google.protobuf.ByteString;
import com.google.protobuf.ExtensionRegistry;
import edu.cmu.ece.agora.link.AbstractLinkManager;
import edu.cmu.ece.agora.link.internet.InternetLinkProtocol.InternetLinkMessage;
import edu.cmu.ece.agora.link.internet.InternetLinkProtocol.InternetLinkMessage.ChannelInfo;

public class InternetLinkManager extends AbstractLinkManager<InternetEndpoint, Void> {

    private static final int MAX_MESSAGE_LENGTH = 1048576;

    private static final int LENGTH_FIELD_LENGTH = 4;

    private static final ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();

    static {
        InternetLinkProtocol.registerAllExtensions(extensionRegistry);
    }

    private final Executor receive_exec;

    private final Executor send_exec;

    private final ChannelFactory server_cf;

    private final ChannelFactory client_cf;

    public InternetLinkManager(InternetEndpoint local, Executor accept_exec, Executor connect_exec, Executor receive_exec, Executor send_exec, Executor boss_exec, Executor worker_exec) {
        super(local, accept_exec, connect_exec);
        if (boss_exec == null) boss_exec = Executors.newCachedThreadPool();
        if (worker_exec == null) worker_exec = Executors.newCachedThreadPool();
        this.receive_exec = receive_exec;
        this.send_exec = send_exec;
        this.server_cf = new NioServerSocketChannelFactory(boss_exec, worker_exec);
        this.client_cf = new NioClientSocketChannelFactory(boss_exec, worker_exec);
        setupServer();
    }

    private void setupProtobufPipeline(ChannelPipeline p) {
        p.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(MAX_MESSAGE_LENGTH, 0, LENGTH_FIELD_LENGTH, 0, LENGTH_FIELD_LENGTH));
        p.addLast("protobufDecoder", new ProtobufDecoder(InternetLinkMessage.getDefaultInstance(), extensionRegistry));
        p.addLast("frameEncoder", new LengthFieldPrepender(LENGTH_FIELD_LENGTH));
        p.addLast("protobufEncoder", new ProtobufEncoder());
    }

    private void setupClientPipeline(ChannelPipeline p) {
        setupProtobufPipeline(p);
        p.addLast("clientHandler", new ClientChannelHandler());
    }

    private void setupServerPipeline(ChannelPipeline p) {
        setupProtobufPipeline(p);
        p.addLast("serverHandler", new ServerChannelHandler());
    }

    private void setupServer() {
        ServerBootstrap sbs = new ServerBootstrap(server_cf);
        sbs.setOption("reuseAddress", true);
        sbs.setOption("child.tcpNoDelay", true);
        sbs.setOption("child.keepAlive", true);
        setupServerPipeline(sbs.getPipeline());
        sbs.bind(getLocalEndpoint().getAddress());
    }

    @Override
    protected void connectRequested(final InternetEndpoint endpoint, Void context) {
        ClientBootstrap cbs = new ClientBootstrap(client_cf);
        cbs.setOption("tcpNoDelay", true);
        cbs.setOption("keepAlive", true);
        setupClientPipeline(cbs.getPipeline());
        cbs.connect(endpoint.getAddress()).addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture arg0) throws Exception {
                if (!arg0.isSuccess()) connectFailed(endpoint, arg0.getCause());
            }
        });
    }

    @ChannelPipelineCoverage("all")
    private class ClientChannelHandler extends SimpleChannelHandler {

        @Override
        public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            ChannelInfo ci = ChannelInfo.newBuilder().setSourceAddress(ByteString.copyFrom(getLocalEndpoint().getAddress().getAddress().getAddress())).setSourcePort(getLocalEndpoint().getAddress().getPort()).build();
            InternetLinkMessage lm = InternetLinkMessage.newBuilder().setExtension(ChannelInfo.extension, ci).build();
            Channel c = ctx.getChannel();
            c.write(lm);
            c.setReadable(false);
            c.getPipeline().remove(ClientChannelHandler.class);
            InternetEndpoint remote_ep = new InternetEndpoint((InetSocketAddress) c.getRemoteAddress());
            InternetLink link = new InternetLink(InternetLinkManager.this, getLocalEndpoint(), remote_ep, c, receive_exec, send_exec);
            connectCompleted(remote_ep, link);
        }
    }

    @ChannelPipelineCoverage("all")
    private class ServerChannelHandler extends SimpleChannelHandler {

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            InternetLinkMessage msg = (InternetLinkMessage) e.getMessage();
            if (!msg.hasExtension(ChannelInfo.extension)) throw new IllegalStateException();
            ChannelInfo ci = msg.getExtension(ChannelInfo.extension);
            byte[] remote_addr_bytes = ci.getSourceAddress().toByteArray();
            InetAddress remote_addr = InetAddress.getByAddress(remote_addr_bytes);
            int remote_port = ci.getSourcePort();
            InetSocketAddress remote_saddr = new InetSocketAddress(remote_addr, remote_port);
            InternetEndpoint remote_ep = new InternetEndpoint(remote_saddr);
            Channel c = ctx.getChannel();
            c.setReadable(false);
            c.getPipeline().remove(ServerChannelHandler.class);
            InternetLink link = new InternetLink(InternetLinkManager.this, getLocalEndpoint(), remote_ep, c, receive_exec, send_exec);
            acceptCompleted(link);
        }
    }
}
