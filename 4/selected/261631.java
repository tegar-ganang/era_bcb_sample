package goldengate.commandexec.ssl.client;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.handler.ssl.SslHandler;
import goldengate.commandexec.client.LocalExecClientHandler;
import goldengate.commandexec.client.LocalExecClientPipelineFactory;
import goldengate.common.logging.GgInternalLogger;
import goldengate.common.logging.GgInternalLoggerFactory;

/**
 * @author Frederic Bregier
 *
 */
public class LocalExecSslClientHandler extends LocalExecClientHandler {

    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory.getLogger(LocalExecSslClientHandler.class);

    /**
     * @param factory
     */
    public LocalExecSslClientHandler(LocalExecClientPipelineFactory factory) {
        super(factory);
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        initExecClient();
        SslHandler sslHandler = ctx.getPipeline().get(SslHandler.class);
        ChannelFuture handshakeFuture = sslHandler.handshake();
        handshakeFuture.addListener(new ChannelFutureListener() {

            public void operationComplete(ChannelFuture future) throws Exception {
                logger.debug("Handshake: " + future.isSuccess(), future.getCause());
                if (future.isSuccess()) {
                    factory.addChannel(future.getChannel());
                } else {
                    future.getChannel().close();
                }
            }
        });
    }
}
