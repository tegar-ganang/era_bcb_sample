package openr66.protocol.networkhandler.ssl;

import goldengate.common.logging.GgInternalLogger;
import goldengate.common.logging.GgInternalLoggerFactory;
import java.util.concurrent.ConcurrentHashMap;
import openr66.protocol.configuration.Configuration;
import openr66.protocol.exception.OpenR66ProtocolNetworkException;
import openr66.protocol.networkhandler.NetworkServerHandler;
import openr66.protocol.utils.R66Future;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.handler.ssl.SslHandler;

/**
 * @author Frederic Bregier
 *
 */
public class NetworkSslServerHandler extends NetworkServerHandler {

    /**
     * @param isServer
     */
    public NetworkSslServerHandler(boolean isServer) {
        super(isServer);
    }

    /**
     * Internal Logger
     */
    private static final GgInternalLogger logger = GgInternalLoggerFactory.getLogger(NetworkSslServerHandler.class);

    /**
     * Waiter for SSL handshake is finished
     */
    private static final ConcurrentHashMap<Integer, R66Future> waitForSsl = new ConcurrentHashMap<Integer, R66Future>();

    /**
     * Remover from SSL HashMap
     */
    private static final ChannelFutureListener remover = new ChannelFutureListener() {

        public void operationComplete(ChannelFuture future) {
            logger.debug("SSL remover");
            waitForSsl.remove(future.getChannel().getId());
        }
    };

    /**
     * Add the Channel as SSL handshake is over
     * @param channel
     */
    private static void addSslConnectedChannel(Channel channel) {
        R66Future futureSSL = new R66Future(true);
        waitForSsl.put(channel.getId(), futureSSL);
        channel.getCloseFuture().addListener(remover);
    }

    /**
     * Set the future of SSL handshake to status
     * @param channel
     * @param status
     */
    private static void setStatusSslConnectedChannel(Channel channel, boolean status) {
        R66Future futureSSL = waitForSsl.get(channel.getId());
        if (futureSSL != null) {
            if (status) {
                futureSSL.setSuccess();
            } else {
                futureSSL.cancel();
            }
        }
    }

    /**
     *
     * @param channel
     * @return True if the SSL handshake is over and OK, else False
     */
    public static boolean isSslConnectedChannel(Channel channel) {
        R66Future futureSSL = waitForSsl.get(channel.getId());
        if (futureSSL == null) {
            for (int i = 0; i < Configuration.RETRYNB; i++) {
                futureSSL = waitForSsl.get(channel.getId());
                if (futureSSL != null) break;
                try {
                    Thread.sleep(Configuration.RETRYINMS);
                } catch (InterruptedException e) {
                }
            }
        }
        if (futureSSL == null) {
            logger.debug("No wait For SSL found");
            return false;
        } else {
            try {
                futureSSL.await(Configuration.configuration.TIMEOUTCON);
            } catch (InterruptedException e) {
            }
            if (futureSSL.isDone()) {
                logger.debug("Wait For SSL: " + futureSSL.isSuccess());
                return futureSSL.isSuccess();
            }
            logger.error("Out of time for wait For SSL");
            return false;
        }
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        Channel channel = e.getChannel();
        logger.debug("Add channel to ssl");
        addSslConnectedChannel(channel);
        isSSL = true;
        super.channelOpen(ctx, e);
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws OpenR66ProtocolNetworkException {
        final SslHandler sslHandler = ctx.getPipeline().get(SslHandler.class);
        if (sslHandler != null) {
            ChannelFuture handshakeFuture;
            handshakeFuture = sslHandler.handshake();
            handshakeFuture.addListener(new ChannelFutureListener() {

                public void operationComplete(ChannelFuture future) throws Exception {
                    logger.debug("Handshake: " + future.isSuccess(), future.getCause());
                    if (future.isSuccess()) {
                        setStatusSslConnectedChannel(future.getChannel(), true);
                    } else {
                        if (Configuration.configuration.r66Mib != null) {
                            String error2 = future.getCause() != null ? future.getCause().getMessage() : "During Handshake";
                            Configuration.configuration.r66Mib.notifyError("SSL Connection Error", error2);
                        }
                        setStatusSslConnectedChannel(future.getChannel(), false);
                        future.getChannel().close();
                    }
                }
            });
        } else {
            logger.error("SSL Not found");
        }
        super.channelConnected(ctx, e);
    }
}
