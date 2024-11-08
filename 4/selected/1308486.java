package com.googlecode.protobuf.netty;

import java.net.SocketAddress;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import com.google.protobuf.BlockingService;
import com.google.protobuf.Service;
import com.googlecode.protobuf.netty.NettyRpcProto.RpcRequest;

public class NettyRpcServer {

    private static final Logger logger = Logger.getLogger(NettyRpcServer.class);

    private final ServerBootstrap bootstrap;

    private final NettyRpcServerChannelUpstreamHandler handler = new NettyRpcServerChannelUpstreamHandler();

    private final ChannelUpstreamHandlerFactory handlerFactory = new ChannelUpstreamHandlerFactory() {

        public ChannelUpstreamHandler getChannelUpstreamHandler() {
            return handler;
        }
    };

    private final ChannelPipelineFactory pipelineFactory = new NettyRpcPipelineFactory(handlerFactory, RpcRequest.getDefaultInstance());

    public NettyRpcServer(ChannelFactory channelFactory) {
        bootstrap = new ServerBootstrap(channelFactory);
        bootstrap.setPipelineFactory(pipelineFactory);
    }

    public void registerService(Service service) {
        handler.registerService(service);
    }

    public void unregisterService(Service service) {
        handler.unregisterService(service);
    }

    public void registerBlockingService(BlockingService service) {
        handler.registerBlockingService(service);
    }

    public void unregisterBlockingService(BlockingService service) {
        handler.unregisterBlockingService(service);
    }

    public void serve() {
        logger.info("Serving...");
        bootstrap.bind();
    }

    public void serve(SocketAddress sa) {
        logger.info("Serving on: " + sa);
        bootstrap.bind(sa);
    }
}
