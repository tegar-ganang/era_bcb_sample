package org.jboss.netty.handler.ipfilter;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelState;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChannelHandler.Sharable;

/**
 * Handler that block any new connection if there are already a currently active
 * channel connected with the same InetAddress (IP).<br>
 * <br>
 *
 * Take care to not change isBlocked method except if you know what you are doing
 * since it is used to test if the current closed connection is to be removed
 * or not from the map of currently connected channel.
 *
 * @author frederic bregier
 *
 */
@Sharable
public class OneIpFilterHandler extends IpFilteringHandler {

    /**
     * HashMap of current remote connected InetAddress
     */
    private final ConcurrentMap<InetAddress, Boolean> connectedSet = new ConcurrentHashMap<InetAddress, Boolean>();

    @Override
    protected boolean accept(ChannelHandlerContext ctx, ChannelEvent e, InetSocketAddress inetSocketAddress) throws Exception {
        InetAddress inetAddress = inetSocketAddress.getAddress();
        if (connectedSet.containsKey(inetAddress)) {
            return false;
        }
        connectedSet.put(inetAddress, Boolean.TRUE);
        return true;
    }

    @Override
    protected ChannelFuture handleRefusedChannel(ChannelHandlerContext ctx, ChannelEvent e, InetSocketAddress inetSocketAddress) throws Exception {
        return null;
    }

    @Override
    protected boolean continues(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        return false;
    }

    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        super.handleUpstream(ctx, e);
        if (e instanceof ChannelStateEvent) {
            ChannelStateEvent evt = (ChannelStateEvent) e;
            if (evt.getState() == ChannelState.CONNECTED) {
                if (evt.getValue() == null) {
                    if (isBlocked(ctx)) {
                        InetSocketAddress inetSocketAddress = (InetSocketAddress) e.getChannel().getRemoteAddress();
                        connectedSet.remove(inetSocketAddress.getAddress());
                    }
                }
            }
        }
    }
}
