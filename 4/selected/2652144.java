package org.jboss.netty.handler.ipfilter;

import java.net.InetSocketAddress;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;

/**
 * General class that handle Ip Filtering
 * @author frederic bregier
 *
 */
public abstract class IpFilteringHandler implements ChannelUpstreamHandler {

    /**
     * Called when the channel is connected. It returns True if the corresponding connection
     * is to be allowed. Else it returns False.
     * @param ctx
     * @param e
     * @param inetSocketAddress the remote {@link InetSocketAddress} from client
     * @return True if the corresponding connection is allowed, else False.
     * @throws Exception
     */
    protected abstract boolean accept(ChannelHandlerContext ctx, ChannelEvent e, InetSocketAddress inetSocketAddress) throws Exception;

    /**
     * Called when the channel has the CONNECTED status and the channel was refused by a previous call to accept().
     * This method enables your implementation to send a message back to the client before closing
     * or whatever you need. This method returns a ChannelFuture on which the implementation
     * will wait uninterruptibly before closing the channel.<br>
     * For instance, If a message is sent back, the corresponding ChannelFuture has to be returned.
     * @param ctx
     * @param e
     * @param inetSocketAddress the remote {@link InetSocketAddress} from client
     * @return the associated ChannelFuture to be waited for before closing the channel. Null is allowed.
     * @throws Exception
     */
    protected abstract ChannelFuture handleRefusedChannel(ChannelHandlerContext ctx, ChannelEvent e, InetSocketAddress inetSocketAddress) throws Exception;

    /**
     * Internal method to test if the current channel is blocked. Should not be overridden.
     * @param ctx
     * @return True if the current channel is blocked, else False
     */
    protected boolean isBlocked(ChannelHandlerContext ctx) {
        return ctx.getAttachment() != null;
    }

    /**
     * Called in handleUpstream, if this channel was previously blocked,
     * to check if whatever the event, it should be passed to the next entry in the pipeline.<br>
     * If one wants to not block events, just overridden this method by returning always true.<br><br>
     * <b>Note that OPENED and BOUND events are still passed to the next entry in the pipeline since
     * those events come out before the CONNECTED event and so the possibility to filter the connection.</b>
     * @param ctx
     * @param e
     * @return True if the event should continue, False if the event should not continue
     *          since this channel was blocked by this filter
     * @throws Exception
     */
    protected abstract boolean continues(ChannelHandlerContext ctx, ChannelEvent e) throws Exception;

    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        if (e instanceof ChannelStateEvent) {
            ChannelStateEvent evt = (ChannelStateEvent) e;
            switch(evt.getState()) {
                case OPEN:
                case BOUND:
                    if (isBlocked(ctx) && !continues(ctx, evt)) {
                        return;
                    } else {
                        ctx.sendUpstream(e);
                        return;
                    }
                case CONNECTED:
                    if (evt.getValue() != null) {
                        InetSocketAddress inetSocketAddress = (InetSocketAddress) e.getChannel().getRemoteAddress();
                        if (!accept(ctx, e, inetSocketAddress)) {
                            ctx.setAttachment(Boolean.TRUE);
                            ChannelFuture future = handleRefusedChannel(ctx, e, inetSocketAddress);
                            if (future != null) {
                                future.addListener(ChannelFutureListener.CLOSE);
                            } else {
                                Channels.close(e.getChannel());
                            }
                            if (isBlocked(ctx) && !continues(ctx, evt)) {
                                return;
                            }
                        }
                        ctx.setAttachment(null);
                    } else {
                        if (isBlocked(ctx) && !continues(ctx, evt)) {
                            return;
                        }
                    }
                    break;
            }
        }
        if (isBlocked(ctx) && !continues(ctx, e)) {
            return;
        }
        ctx.sendUpstream(e);
    }
}
