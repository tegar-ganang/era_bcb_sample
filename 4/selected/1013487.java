package org.jboss.netty.example.factorial;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

/**
 * Sends a sequence of integers to a {@link FactorialServer} to calculate
 * the factorial of the specified integer.
 *
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
 *
 * @version $Rev: 2080 $, $Date: 2010-01-26 18:04:19 +0900 (Tue, 26 Jan 2010) $
 */
public class FactorialClient {

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: " + FactorialClient.class.getSimpleName() + " <host> <port> <count>");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        int count = Integer.parseInt(args[2]);
        if (count <= 0) {
            throw new IllegalArgumentException("count must be a positive integer.");
        }
        ClientBootstrap bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()));
        bootstrap.setPipelineFactory(new FactorialClientPipelineFactory(count));
        ChannelFuture connectFuture = bootstrap.connect(new InetSocketAddress(host, port));
        Channel channel = connectFuture.awaitUninterruptibly().getChannel();
        FactorialClientHandler handler = (FactorialClientHandler) channel.getPipeline().getLast();
        System.err.format("Factorial of %,d is: %,d", count, handler.getFactorial());
        bootstrap.releaseExternalResources();
    }
}
