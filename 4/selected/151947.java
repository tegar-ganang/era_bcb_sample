package goldengate.commandexec.ssl.server;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.handler.ssl.SslHandler;
import goldengate.commandexec.server.LocalExecServerHandler;
import goldengate.commandexec.server.LocalExecServerPipelineFactory;
import goldengate.common.logging.GgInternalLogger;
import goldengate.common.logging.GgInternalLoggerFactory;

/**
 * @author Frederic Bregier
 *
 */
public class LocalExecSslServerHandler extends LocalExecServerHandler {

    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory.getLogger(LocalExecSslServerHandler.class);

    /**
     * @param factory
     * @param newdelay
     */
    public LocalExecSslServerHandler(LocalExecServerPipelineFactory factory, long newdelay) {
        super(factory, newdelay);
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        SslHandler sslHandler = ctx.getPipeline().get(SslHandler.class);
        ChannelFuture handshakeFuture = sslHandler.handshake();
        handshakeFuture.addListener(new ChannelFutureListener() {

            public void operationComplete(ChannelFuture future) throws Exception {
                logger.debug("Handshake: " + future.isSuccess(), future.getCause());
                if (future.isSuccess()) {
                    if (isShutdown(future.getChannel())) {
                        answered = true;
                        return;
                    }
                    answered = false;
                    factory.addChannel(future.getChannel());
                } else {
                    answered = true;
                    future.getChannel().close();
                }
            }
        });
    }
}
