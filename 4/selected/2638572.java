package org.jwebsocket.netty.connectors;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.handler.codec.http.websocket.DefaultWebSocketFrame;
import org.jwebsocket.api.WebSocketEngine;
import org.jwebsocket.api.WebSocketPacket;
import org.jwebsocket.async.IOFuture;
import org.jwebsocket.connectors.BaseConnector;
import org.jwebsocket.kit.CloseReason;
import org.jwebsocket.logging.Logging;
import org.jwebsocket.netty.engines.NettyEngineHandler;

/**
 * Netty based implementation of the {@code BaseConnector}.
 * 
 * @author puran
 * @version $Id: NettyConnector.java 612 2010-06-29 17:24:04Z fivefeetfurther $
 */
public class NettyConnector extends BaseConnector {

    private static Logger log = Logging.getLogger(NettyConnector.class);

    private NettyEngineHandler handler = null;

    /**
   * The private constructor, netty connector objects are created using static
   * factory method:
   * <tt>getNettyConnector({@code WebSocketEngine}, {@code ChannelHandlerContext})</tt>
   * 
   * @param theEngine the websocket engine object
   * @param theHandlerContext the netty engine handler context
   */
    public NettyConnector(WebSocketEngine theEngine, NettyEngineHandler theHandler) {
        super(theEngine);
        this.handler = theHandler;
    }

    /**
   * {@inheritDoc}
   */
    @Override
    public void startConnector() {
        if (log.isDebugEnabled()) {
            log.debug("Starting Netty connector...");
        }
        if (log.isInfoEnabled()) {
            log.info("Started Netty connector on port" + getRemotePort() + ".");
        }
    }

    /**
   * {@inheritDoc}
   */
    @Override
    public void stopConnector(CloseReason aCloseReason) {
        if (log.isDebugEnabled()) {
            log.debug("Stopping Netty connector (" + aCloseReason.name() + ")...");
        }
        getEngine().connectorStopped(this, aCloseReason);
        if (handler.getChannelHandlerContext().getChannel().isConnected() && getEngine().isAlive()) {
            handler.getChannelHandlerContext().getChannel().close();
        }
        if (log.isInfoEnabled()) {
            log.info("Stopped Netty connector (" + aCloseReason.name() + ") on port " + getRemotePort());
        }
    }

    /**
   * {@inheritDoc}
   */
    @Override
    public int getRemotePort() {
        InetSocketAddress address = (InetSocketAddress) handler.getChannelHandlerContext().getChannel().getRemoteAddress();
        return address.getPort();
    }

    /**
   * {@inheritDoc}
   */
    @Override
    public InetAddress getRemoteHost() {
        InetSocketAddress address = (InetSocketAddress) handler.getChannelHandlerContext().getChannel().getRemoteAddress();
        return address.getAddress();
    }

    /**
   * {@inheritDoc}
   */
    @Override
    public void processPacket(WebSocketPacket aDataPacket) {
        getEngine().processPacket(this, aDataPacket);
    }

    /**
   * {@inheritDoc}
   */
    @Override
    public void sendPacket(WebSocketPacket aDataPacket) {
        if (handler.getChannelHandlerContext().getChannel().isConnected() && getEngine().isAlive()) {
            handler.getChannelHandlerContext().getChannel().write(new DefaultWebSocketFrame(aDataPacket.getString()));
        }
    }

    public IOFuture sendPacketAsync(WebSocketPacket aDataPacket) {
        if (handler.getChannelHandlerContext().getChannel().isConnected() && getEngine().isAlive()) {
            ChannelFuture internalFuture = handler.getChannelHandlerContext().getChannel().write(new DefaultWebSocketFrame(aDataPacket.getString()));
            return new NIOFuture(this, internalFuture);
        } else {
            return null;
        }
    }

    /**
   * {@inheritDoc}
   */
    @Override
    public String toString() {
        String lRes = getRemoteHost().getHostAddress() + ":" + getRemotePort();
        String lUsername = getString("org.jwebsocket.plugins.system.username");
        if (lUsername != null) {
            lRes += " (" + lUsername + ")";
        }
        return lRes;
    }
}
