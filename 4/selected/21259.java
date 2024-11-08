package org.mobicents.media.server.ctrl.rtsp.stack;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

/**
 * 
 * @author amit.bhayani
 * 
 */
public class RtspClientStackImpl implements RtspStack {

    private static Logger logger = Logger.getLogger(RtspClientStackImpl.class);

    private final String address;

    private final int port;

    private final InetAddress inetAddress;

    private Channel channel = null;

    private ClientBootstrap bootstrap;

    private RtspListener listener = null;

    public RtspClientStackImpl(String address, int port) throws UnknownHostException {
        this.address = address;
        this.port = port;
        this.inetAddress = InetAddress.getByName(this.address);
    }

    public String getAddress() {
        return this.address;
    }

    public int getPort() {
        return this.port;
    }

    public void start() {
        InetSocketAddress bindAddress = new InetSocketAddress(this.inetAddress, this.port);
        bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
        bootstrap.setOption("localAddress", bindAddress);
        bootstrap.setPipelineFactory(new RtspClientPipelineFactory(this));
        logger.info("Mobicents RTSP Client started and bound to " + bindAddress.toString());
    }

    protected void processRtspResponse(HttpResponse rtspResponse) {
        synchronized (this.listener) {
            listener.onRtspResponse(rtspResponse);
        }
    }

    protected void processRtspRequest(HttpRequest rtspRequest, Channel channel) {
        synchronized (this.listener) {
            listener.onRtspRequest(rtspRequest, channel);
        }
    }

    public void stop() {
        ChannelFuture cf = channel.getCloseFuture();
        cf.addListener(new ClientChannelFutureListener());
        channel.close();
        cf.awaitUninterruptibly();
        bootstrap.getFactory().releaseExternalResources();
    }

    public void setRtspListener(RtspListener listener) {
        this.listener = listener;
    }

    private class ClientChannelFutureListener implements ChannelFutureListener {

        public void operationComplete(ChannelFuture arg0) throws Exception {
            logger.info("Mobicents RTSP Client Stop complete");
        }
    }

    public void sendRquest(HttpRequest rtspRequest, String host, int port) {
        ChannelFuture future = null;
        if (channel == null || (channel != null && !channel.isConnected())) {
            future = bootstrap.connect(new InetSocketAddress(host, port));
        }
        channel = future.awaitUninterruptibly().getChannel();
        if (!future.isSuccess()) {
            future.getCause().printStackTrace();
            return;
        }
        channel.write(rtspRequest);
    }
}
