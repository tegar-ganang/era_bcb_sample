package org.jboss.netty.example.http.tunnel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.http.HttpTunnelingClientSocketChannelFactory;
import org.jboss.netty.channel.socket.oio.OioClientSocketChannelFactory;
import org.jboss.netty.example.securechat.SecureChatSslContextFactory;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.logging.InternalLogLevel;

/**
 * An HTTP tunneled version of the telnet client org.jboss.netty.example.  Please refer to the
 * API documentation of the <tt>org.jboss.netty.channel.socket.http</tt> package
 * for the detailed instruction on how to deploy the server-side HTTP tunnel in
 * your Servlet container.
 *
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author Andy Taylor (andy.taylor@jboss.org)
 * @version $Rev: 2080 $, $Date: 2010-01-26 18:04:19 +0900 (Tue, 26 Jan 2010) $
 */
public class HttpTunnelingClientExample {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("Usage: " + HttpTunnelingClientExample.class.getSimpleName() + " <URL>");
            System.err.println("Example: " + HttpTunnelingClientExample.class.getSimpleName() + " http://localhost:8080/netty-tunnel");
            return;
        }
        URI uri = new URI(args[0]);
        String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
        ClientBootstrap b = new ClientBootstrap(new HttpTunnelingClientSocketChannelFactory(new OioClientSocketChannelFactory(Executors.newCachedThreadPool())));
        b.setPipelineFactory(new ChannelPipelineFactory() {

            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(new StringDecoder(), new StringEncoder(), new LoggingHandler(InternalLogLevel.INFO));
            }
        });
        b.setOption("serverName", uri.getHost());
        b.setOption("serverPath", uri.getRawPath());
        if (scheme.equals("https")) {
            b.setOption("sslContext", SecureChatSslContextFactory.getClientContext());
        } else if (!scheme.equals("http")) {
            System.err.println("Only HTTP(S) is supported.");
            return;
        }
        ChannelFuture channelFuture = b.connect(new InetSocketAddress(uri.getHost(), uri.getPort()));
        channelFuture.awaitUninterruptibly();
        System.out.println("Enter text ('quit' to exit)");
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
        b.releaseExternalResources();
    }
}
