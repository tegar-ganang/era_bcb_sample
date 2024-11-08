package org.jboss.netty.example.local;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory;
import org.jboss.netty.channel.local.DefaultLocalServerChannelFactory;
import org.jboss.netty.channel.local.LocalAddress;
import org.jboss.netty.example.echo.EchoServerHandler;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;

/**
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author Andy Taylor (andy.taylor@jboss.org)
 * @version $Rev: 2080 $, $Date: 2010-01-26 18:04:19 +0900 (Tue, 26 Jan 2010) $
 */
public class LocalExample {

    public static void main(String[] args) throws Exception {
        LocalAddress socketAddress = new LocalAddress("1");
        ServerBootstrap sb = new ServerBootstrap(new DefaultLocalServerChannelFactory());
        EchoServerHandler handler = new EchoServerHandler();
        sb.getPipeline().addLast("handler", handler);
        sb.bind(socketAddress);
        ClientBootstrap cb = new ClientBootstrap(new DefaultLocalClientChannelFactory());
        cb.setPipelineFactory(new ChannelPipelineFactory() {

            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(new StringDecoder(), new StringEncoder(), new LoggingHandler(InternalLogLevel.INFO));
            }
        });
        ChannelFuture channelFuture = cb.connect(socketAddress);
        channelFuture.awaitUninterruptibly();
        System.out.println("Enter text (quit to end)");
        ChannelFuture lastWriteFuture = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        for (; ; ) {
            String line = in.readLine();
            if (line == null || "quit".equalsIgnoreCase(line)) {
                break;
            }
            lastWriteFuture = channelFuture.getChannel().write(line);
        }
        if (lastWriteFuture != null) {
            lastWriteFuture.awaitUninterruptibly();
        }
        channelFuture.getChannel().close();
        channelFuture.getChannel().getCloseFuture().awaitUninterruptibly();
        cb.releaseExternalResources();
        sb.releaseExternalResources();
    }
}
