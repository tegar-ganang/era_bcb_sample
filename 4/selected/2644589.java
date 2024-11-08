package edu.cmu.ece.agora.network.internet;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
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
import edu.cmu.ece.agora.network.NetworkAcceptEvent;
import edu.cmu.ece.agora.network.NetworkBindRequest;
import edu.cmu.ece.agora.network.NetworkBindResponse;
import edu.cmu.ece.agora.network.NetworkControlMessage;
import edu.cmu.ece.agora.network.NetworkController;
import edu.cmu.ece.agora.network.NetworkLinkRequest;
import edu.cmu.ece.agora.network.NetworkLinkResponse;
import edu.cmu.ece.agora.network.internet.InternetLinkProtocol.InternetLinkMessage;
import edu.cmu.ece.agora.network.internet.InternetLinkProtocol.InternetLinkMessage.ChannelInfo;

public class InternetController extends NetworkController<InternetEndpoint> {

    private static final int MAX_MESSAGE_LENGTH = 1048576;

    private static final int LENGTH_FIELD_LENGTH = 4;

    private static final ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();

    static {
        InternetLinkProtocol.registerAllExtensions(extensionRegistry);
    }

    private final ExecutorService pipeline_exec;

    private final ExecutorService boss_exec;

    private final ExecutorService worker_exec;

    private final ChannelFactory server_cf;

    private final ChannelFactory client_cf;

    private InternetEndpoint bind;

    public InternetController(ExecutorService exec) {
        super(exec);
        pipeline_exec = exec;
        boss_exec = Executors.newCachedThreadPool();
        worker_exec = Executors.newCachedThreadPool();
        server_cf = new NioServerSocketChannelFactory(boss_exec, worker_exec);
        client_cf = new NioClientSocketChannelFactory(boss_exec, worker_exec);
    }

    ExecutorService getPipeExecutor() {
        return pipeline_exec;
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

    private void setupServer(InternetEndpoint local) {
        ServerBootstrap sbs = new ServerBootstrap(server_cf);
        sbs.setOption("reuseAddress", true);
        sbs.setOption("child.tcpNoDelay", true);
        sbs.setOption("child.keepAlive", true);
        setupServerPipeline(sbs.getPipeline());
        sbs.bind(local.getAddress());
    }

    private ChannelFuture setupClient(InternetEndpoint remote) {
        ClientBootstrap cbs = new ClientBootstrap(client_cf);
        cbs.setOption("tcpNoDelay", true);
        cbs.setOption("keepAlive", true);
        setupClientPipeline(cbs.getPipeline());
        return cbs.connect(remote.getAddress());
    }

    @Override
    protected void handleDownstream(NetworkControlMessage<InternetEndpoint> message) {
        if (message instanceof NetworkBindRequest<?>) {
            NetworkBindRequest<InternetEndpoint> nbrq = (NetworkBindRequest<InternetEndpoint>) message;
            NetworkBindResponse<InternetEndpoint> nbrp;
            if (bind == null) {
                nbrp = new NetworkBindResponse<InternetEndpoint>(nbrq, new IllegalStateException("This InternetController is already bound to " + bind + "."));
            } else {
                try {
                    setupServer(nbrq.getEndpoint());
                    bind = nbrq.getEndpoint();
                    nbrp = new NetworkBindResponse<InternetEndpoint>(nbrq);
                } catch (Throwable t) {
                    nbrp = new NetworkBindResponse<InternetEndpoint>(nbrq, t);
                }
            }
            feedUpstream(nbrp);
        } else if (message instanceof NetworkLinkRequest<?>) {
            final NetworkLinkRequest<InternetEndpoint> nlrq = (NetworkLinkRequest<InternetEndpoint>) message;
            try {
                setupClient(nlrq.getEndpoint()).addListener(new ChannelFutureListener() {

                    @Override
                    public void operationComplete(ChannelFuture arg0) throws Exception {
                        if (!arg0.isSuccess()) {
                            feedUpstream(new NetworkLinkResponse<InternetEndpoint>(nlrq, arg0.getCause()));
                        } else {
                            Channel c = arg0.getChannel();
                            ChannelInfo.Builder cib = ChannelInfo.newBuilder();
                            if (bind != null) {
                                cib.setSourceAddress(ByteString.copyFrom(bind.getAddress().getAddress().getAddress()));
                                cib.setSourcePort(bind.getAddress().getPort());
                            }
                            InternetLinkMessage lm = InternetLinkMessage.newBuilder().setExtension(ChannelInfo.extension, cib.build()).build();
                            c.write(lm);
                            c.setReadable(false);
                            c.getPipeline().remove(ClientChannelHandler.class);
                            InternetEndpoint remote_ep = new InternetEndpoint((InetSocketAddress) c.getRemoteAddress());
                            InternetLink link = new InternetLink(InternetController.this, bind, remote_ep, c);
                            feedUpstream(new NetworkLinkResponse<InternetEndpoint>(nlrq, bind, remote_ep, link));
                        }
                    }
                });
            } catch (Throwable t) {
                feedUpstream(new NetworkLinkResponse<InternetEndpoint>(nlrq, t));
            }
        }
    }

    @ChannelPipelineCoverage("all")
    private class ClientChannelHandler extends SimpleChannelHandler {

        @Override
        public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        }
    }

    @ChannelPipelineCoverage("all")
    private class ServerChannelHandler extends SimpleChannelHandler {

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            InternetLinkMessage msg = (InternetLinkMessage) e.getMessage();
            if (!msg.hasExtension(ChannelInfo.extension)) throw new IllegalStateException();
            ChannelInfo ci = msg.getExtension(ChannelInfo.extension);
            InternetEndpoint remote_ep = null;
            if (ci.hasSourceAddress() && ci.hasSourcePort()) {
                byte[] remote_addr_bytes = ci.getSourceAddress().toByteArray();
                InetAddress remote_addr = InetAddress.getByAddress(remote_addr_bytes);
                int remote_port = ci.getSourcePort();
                InetSocketAddress remote_saddr = new InetSocketAddress(remote_addr, remote_port);
                remote_ep = new InternetEndpoint(remote_saddr);
            }
            Channel c = ctx.getChannel();
            c.setReadable(false);
            c.getPipeline().remove(ServerChannelHandler.class);
            InternetLink link = new InternetLink(InternetController.this, bind, remote_ep, c);
            feedUpstream(new NetworkAcceptEvent<InternetEndpoint>(bind, remote_ep, link));
        }
    }
}
