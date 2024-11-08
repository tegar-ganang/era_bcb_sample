package edu.cmu.ece.agora.kernel.router;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.LengthFieldBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.LengthFieldPrepender;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.ssl.SslHandler;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.GeneratedMessage;
import edu.cmu.ece.agora.codecs.Tokens;
import edu.cmu.ece.agora.futures.Future;
import edu.cmu.ece.agora.futures.FutureManager;
import edu.cmu.ece.agora.futures.FuturePackage;
import edu.cmu.ece.agora.futures.Futures;
import edu.cmu.ece.agora.kernel.router.Protocol.RouterMessage;
import edu.cmu.ece.agora.kernel.router.Protocol.RouterMessage.DomainInfo;
import edu.cmu.ece.agora.kernel.router.Protocol.RouterMessage.DomainLogonRequest;
import edu.cmu.ece.agora.kernel.router.Protocol.RouterMessage.DomainLogonResponse;
import edu.cmu.ece.agora.kernel.router.Protocol.RouterMessage.DomainProof;
import edu.cmu.ece.agora.kernel.router.Protocol.RouterMessage.FindNodeRequest;
import edu.cmu.ece.agora.kernel.router.Protocol.RouterMessage.FindNodeResponse;
import edu.cmu.ece.agora.kernel.router.Protocol.RouterMessage.FindValueRequest;
import edu.cmu.ece.agora.kernel.router.Protocol.RouterMessage.FindValueResponse;
import edu.cmu.ece.agora.kernel.router.Protocol.RouterMessage.InternalKeyRequest;
import edu.cmu.ece.agora.kernel.router.Protocol.RouterMessage.InternalKeyResponse;
import edu.cmu.ece.agora.kernel.router.Protocol.RouterMessage.StoreValueRequest;
import edu.cmu.ece.agora.kernel.router.Protocol.RouterMessage.StoreValueResponse;
import edu.cmu.ece.agora.kernel.router.Protocol.RouterMessage.TerminationNotice;

public class LinkManager {

    private static final XLogger log = XLoggerFactory.getXLogger("LinkManager");

    private static final ExtensionRegistry extensionRegistry = ExtensionRegistry.newInstance();

    static {
        Protocol.registerAllExtensions(extensionRegistry);
    }

    private final LinkListener listener;

    private final ServerBootstrap sbs;

    private final ClientBootstrap cbs;

    private Channel listenChannel;

    private long inMsg = 0;

    public LinkManager(LinkListener listener) {
        this.listener = listener;
        ChannelFactory scf = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        this.sbs = new ServerBootstrap(scf);
        this.sbs.setPipelineFactory(new RouterPipelineFactory(false));
        this.sbs.setOption("child.tcpNoDelay", true);
        this.sbs.setOption("child.keepAlive", true);
        ChannelFactory ccf = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
        this.cbs = new ClientBootstrap(ccf);
        this.cbs.setPipelineFactory(new RouterPipelineFactory(true));
        this.cbs.setOption("tcpNoDelay", true);
        this.cbs.setOption("keepAlive", true);
    }

    public InetSocketAddress bind(InetSocketAddress addr) {
        listenChannel = this.sbs.bind(addr);
        return (InetSocketAddress) listenChannel.getLocalAddress();
    }

    public void unbind() {
        log.info("*** UNBIND CALLED ***");
        listenChannel.close();
        System.out.println("INMSG: " + inMsg);
    }

    private class RouterPipelineFactory implements ChannelPipelineFactory {

        private final boolean client;

        public RouterPipelineFactory(boolean client) {
            this.client = client;
        }

        @Override
        public ChannelPipeline getPipeline() throws Exception {
            ChannelPipeline p = Channels.pipeline();
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, null, null);
            SSLEngine engine = context.createSSLEngine();
            engine.setEnabledCipherSuites(new String[] { "SSL_DH_anon_WITH_RC4_128_MD5" });
            engine.setUseClientMode(client);
            p.addLast("SSL", new SslHandler(engine));
            p.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(1048576, 0, 4, 0, 4));
            p.addLast("protobufDecoder", new ProtobufDecoder(Protocol.RouterMessage.getDefaultInstance(), extensionRegistry));
            p.addLast("frameEncoder", new LengthFieldPrepender(4));
            p.addLast("protobufEncoder", new ProtobufEncoder());
            p.addLast("executor", new ExecutionHandler(new OrderedMemoryAwareThreadPoolExecutor(16, 1048576, 1048576)));
            p.addLast("managerHandler", new LinkChannelHandler(client));
            return p;
        }
    }

    @ChannelPipelineCoverage("one")
    public class LinkChannelHandler extends SimpleChannelHandler {

        private final boolean client;

        public final FuturePackage<Link> connectfp;

        private Link link;

        private String termReason = "Link terminated manually by local node.";

        public LinkChannelHandler(boolean client) {
            this.client = client;
            this.connectfp = Futures.newFuturePackage();
        }

        @Override
        public void channelConnected(final ChannelHandlerContext ctx, final ChannelStateEvent e) throws Exception {
            link = new Link(ctx.getChannel());
            final SslHandler sslHandler = ctx.getPipeline().get(SslHandler.class);
            sslHandler.handshake(e.getChannel()).addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(ChannelFuture f) throws Exception {
                    if (!f.isSuccess()) {
                        log.warn("SSL handshake with " + e.getChannel().getRemoteAddress() + " failed!  Closing channel.");
                        e.getChannel().close();
                        return;
                    }
                    listener.linkEstablished(link, client);
                    connectfp.getManager().completeFuture(link);
                }
            });
        }

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
            RouterMessage rm = (RouterMessage) e.getMessage();
            if (rm.hasExtension(TerminationNotice.extension)) {
                TerminationNotice tn = rm.getExtension(TerminationNotice.extension);
                termReason = tn.getReason();
                ctx.getChannel().close();
            } else {
                receiveMessage(link, rm);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
            e.getCause().printStackTrace();
            termReason = e.getCause().toString();
            if (ctx.getChannel().isWritable()) {
                TerminationNotice tn = TerminationNotice.newBuilder().setReason(termReason).build();
                ctx.getChannel().write(tn).addListener(ChannelFutureListener.CLOSE);
            } else {
                ctx.getChannel().close();
            }
        }

        @Override
        public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
            listener.linkTerminated(link, client, termReason);
        }
    }

    private void receiveMessage(Link link, RouterMessage rm) {
        inMsg++;
        if (rm.hasExtension(DomainInfo.extension)) handleDomainInfo(link, rm.getExtension(DomainInfo.extension)); else if (rm.hasExtension(DomainProof.extension)) handleDomainProof(link, rm.getExtension(DomainProof.extension)); else if (rm.hasExtension(DomainLogonRequest.extension)) handleDomainLogonRequest(link, rm.getExtension(DomainLogonRequest.extension)); else if (rm.hasExtension(DomainLogonResponse.extension)) handleDomainLogonResponse(link, rm.getExtension(DomainLogonResponse.extension)); else if (rm.hasExtension(FindNodeRequest.extension)) handleFindNodeRequest(link, rm.getExtension(FindNodeRequest.extension), rm.getToken()); else if (rm.hasExtension(FindNodeResponse.extension)) handleFindNodeResponse(link, rm.getExtension(FindNodeResponse.extension), rm.getToken()); else if (rm.hasExtension(InternalKeyRequest.extension)) handleInternalKeyRequest(link, rm.getExtension(InternalKeyRequest.extension), rm.getToken()); else if (rm.hasExtension(InternalKeyResponse.extension)) handleInternalKeyResponse(link, rm.getExtension(InternalKeyResponse.extension), rm.getToken()); else if (rm.hasExtension(FindValueRequest.extension)) handleFindValueRequest(link, rm.getExtension(FindValueRequest.extension), rm.getToken()); else if (rm.hasExtension(FindValueResponse.extension)) handleFindValueResponse(link, rm.getExtension(FindValueResponse.extension), rm.getToken()); else if (rm.hasExtension(StoreValueRequest.extension)) handleStoreValueRequest(link, rm.getExtension(StoreValueRequest.extension), rm.getToken()); else if (rm.hasExtension(StoreValueResponse.extension)) handleStoreValueResponse(link, rm.getExtension(StoreValueResponse.extension), rm.getToken());
    }

    private void sendMessage(Link link, RouterMessage rm) {
        Channel c = link.getChannel();
        if (c == null) {
            receiveMessage(link, rm);
        } else {
            c.write(rm);
        }
    }

    private <REQUEST, RESPONSE> Future<RESPONSE> sendRequest(GeneratedMessage.GeneratedExtension<RouterMessage, REQUEST> extension, REQUEST request, Map<Long, FutureManager<RESPONSE>> futures, Link link) {
        RouterMessage.Builder b = RouterMessage.newBuilder();
        FuturePackage<RESPONSE> fp = Futures.newFuturePackage();
        Long token;
        synchronized (futures) {
            token = Tokens.generateToken(futures.keySet());
            futures.put(token, fp.getManager());
        }
        b.setToken(token);
        b.setExtension(extension, request);
        sendMessage(link, b.build());
        return fp.getFuture();
    }

    private <RESPONSE> void sendResponse(GeneratedMessage.GeneratedExtension<RouterMessage, RESPONSE> extension, RESPONSE response, Long token, Link link) {
        if (response == null) return;
        RouterMessage.Builder b = RouterMessage.newBuilder();
        b.setToken(token);
        b.setExtension(extension, response);
        sendMessage(link, b.build());
    }

    private <RESPONSE> void handleResponse(RESPONSE response, Map<Long, FutureManager<RESPONSE>> futures, Long token) {
        FutureManager<RESPONSE> fm;
        synchronized (futures) {
            fm = futures.remove(token);
        }
        if (fm == null) throw new IllegalArgumentException("Token not found.");
        fm.completeFuture(response);
    }

    public Link createLocalLink() {
        return new Link(null);
    }

    public Future<Link> beginLink(InetSocketAddress address) {
        final FuturePackage<Link> fp;
        if (address == null) {
            fp = Futures.newFuturePackage();
            fp.getManager().completeFuture(new Link(null));
        } else {
            ChannelFuture cf = cbs.connect(address);
            LinkChannelHandler lch = cf.getChannel().getPipeline().get(LinkChannelHandler.class);
            fp = lch.connectfp;
            cf.addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(ChannelFuture arg0) throws Exception {
                    if (!arg0.isSuccess()) {
                        fp.getManager().cancelFuture(arg0.getCause());
                    }
                }
            });
        }
        return fp.getFuture();
    }

    private Map<Long, FutureManager<InternalKeyResponse>> publicKeyResponseFutures = new HashMap<Long, FutureManager<InternalKeyResponse>>();

    public Future<InternalKeyResponse> sendInternalKeyRequest(Link link, InternalKeyRequest request) {
        return sendRequest(InternalKeyRequest.extension, request, publicKeyResponseFutures, link);
    }

    private void sendInternalKeyResponse(Link link, InternalKeyResponse response, Long token) {
        sendResponse(InternalKeyResponse.extension, response, token, link);
    }

    private void handleInternalKeyRequest(Link link, InternalKeyRequest request, Long token) {
        sendInternalKeyResponse(link, listener.publicKeyRequestReceived(link, request), token);
    }

    private void handleInternalKeyResponse(Link link, InternalKeyResponse response, Long token) {
        listener.publicKeyResponseReceived(link, response);
        handleResponse(response, publicKeyResponseFutures, token);
    }

    private Map<Long, FutureManager<FindNodeResponse>> findNodeResponseFutures = new HashMap<Long, FutureManager<FindNodeResponse>>();

    public Future<FindNodeResponse> sendFindNodeRequest(Link link, FindNodeRequest request) {
        return sendRequest(FindNodeRequest.extension, request, findNodeResponseFutures, link);
    }

    private void sendFindNodeResponse(Link link, FindNodeResponse response, Long token) {
        sendResponse(FindNodeResponse.extension, response, token, link);
    }

    private void handleFindNodeRequest(Link link, FindNodeRequest request, Long token) {
        sendFindNodeResponse(link, listener.findNodeRequestReceived(link, request), token);
    }

    private void handleFindNodeResponse(Link link, FindNodeResponse response, Long token) {
        listener.findNodeResponseReceived(link, response);
        handleResponse(response, findNodeResponseFutures, token);
    }

    private Map<Long, FutureManager<FindValueResponse>> findValueResponseFutures = new HashMap<Long, FutureManager<FindValueResponse>>();

    public Future<FindValueResponse> sendFindValueRequest(Link link, FindValueRequest request) {
        return sendRequest(FindValueRequest.extension, request, findValueResponseFutures, link);
    }

    private void sendFindValueResponse(Link link, FindValueResponse response, Long token) {
        sendResponse(FindValueResponse.extension, response, token, link);
    }

    private void handleFindValueRequest(Link link, FindValueRequest request, Long token) {
        sendFindValueResponse(link, listener.findValueRequestReceived(link, request), token);
    }

    private void handleFindValueResponse(Link link, FindValueResponse response, Long token) {
        listener.findValueResponseReceived(link, response);
        handleResponse(response, findValueResponseFutures, token);
    }

    private Map<Long, FutureManager<StoreValueResponse>> storeValueResponseFutures = new HashMap<Long, FutureManager<StoreValueResponse>>();

    public Future<StoreValueResponse> sendStoreValueRequest(Link link, StoreValueRequest request) {
        return sendRequest(StoreValueRequest.extension, request, storeValueResponseFutures, link);
    }

    private void sendStoreValueResponse(Link link, StoreValueResponse response, Long token) {
        sendResponse(StoreValueResponse.extension, response, token, link);
    }

    private void handleStoreValueRequest(Link link, StoreValueRequest request, Long token) {
        sendStoreValueResponse(link, listener.storeValueRequestReceived(link, request), token);
    }

    private void handleStoreValueResponse(Link link, StoreValueResponse response, Long token) {
        listener.storeValueResponseReceived(link, response);
        handleResponse(response, storeValueResponseFutures, token);
    }

    public void sendDomainProof(Link link, DomainProof domainInfo) {
        this.sendResponse(DomainProof.extension, domainInfo, 0L, link);
    }

    private void handleDomainProof(Link link, DomainProof domainInfo) {
        listener.domainProofReceived(link, domainInfo);
    }

    public void sendDomainInfo(Link link, DomainInfo domainInfo) {
        this.sendResponse(DomainInfo.extension, domainInfo, 0L, link);
    }

    private void handleDomainInfo(Link link, DomainInfo domainInfo) {
        listener.domainInfoReceived(link, domainInfo);
    }

    public void sendDomainLogonRequest(Link link, DomainLogonRequest request) {
        this.sendResponse(DomainLogonRequest.extension, request, 0L, link);
    }

    private void handleDomainLogonRequest(Link link, DomainLogonRequest request) {
        listener.domainLogonRequestReceived(link, request);
    }

    public void sendDomainLogonResponse(Link link, DomainLogonResponse response) {
        sendResponse(DomainLogonResponse.extension, response, 0L, link);
    }

    private void handleDomainLogonResponse(Link link, DomainLogonResponse response) {
        listener.domainLogonResponseReceived(link, response);
    }
}
