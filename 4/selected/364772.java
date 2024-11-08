package com.googlecode.protobuf.netty;

import java.net.SocketAddress;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;

public class NettyRpcClient {

    private final ClientBootstrap bootstrap;

    private final ChannelUpstreamHandlerFactory handlerFactory = new ChannelUpstreamHandlerFactory() {

        public ChannelUpstreamHandler getChannelUpstreamHandler() {
            return new NettyRpcClientChannelUpstreamHandler();
        }
    };

    private final ChannelPipelineFactory pipelineFactory = new NettyRpcPipelineFactory(handlerFactory, NettyRpcProto.RpcResponse.getDefaultInstance());

    public NettyRpcClient(ChannelFactory channelFactory) {
        bootstrap = new ClientBootstrap(channelFactory);
        bootstrap.setPipelineFactory(pipelineFactory);
    }

    public NettyRpcChannel blockingConnect(SocketAddress sa) {
        return new NettyRpcChannel(bootstrap.connect(sa).awaitUninterruptibly().getChannel());
    }

    public void shutdown() {
        bootstrap.releaseExternalResources();
    }
}
