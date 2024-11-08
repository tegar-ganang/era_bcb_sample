package org.jboss.netty.example.local;

import java.util.concurrent.Executor;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;
import org.jboss.netty.handler.execution.ExecutionHandler;

/**
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author Andy Taylor (andy.taylor@jboss.org)
 * @author Frederic Bregier (fredbregier@free.fr)
 * @version $Rev: 2235 $, $Date: 2010-04-06 18:40:35 +0900 (Tue, 06 Apr 2010) $
 */
public class LocalServerPipelineFactory implements ChannelPipelineFactory {

    private final ExecutionHandler executionHandler;

    public LocalServerPipelineFactory(Executor eventExecutor) {
        executionHandler = new ExecutionHandler(eventExecutor);
    }

    public ChannelPipeline getPipeline() throws Exception {
        final ChannelPipeline pipeline = Channels.pipeline();
        pipeline.addLast("decoder", new StringDecoder());
        pipeline.addLast("encoder", new StringEncoder());
        pipeline.addLast("executor", executionHandler);
        pipeline.addLast("handler", new EchoCloseServerHandler());
        return pipeline;
    }

    static class EchoCloseServerHandler implements ChannelUpstreamHandler, ChannelDownstreamHandler {

        public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
            if (e instanceof MessageEvent) {
                final MessageEvent evt = (MessageEvent) e;
                String msg = (String) evt.getMessage();
                if (msg.equalsIgnoreCase("quit")) {
                    Channels.close(e.getChannel());
                    return;
                }
            }
            ctx.sendUpstream(e);
        }

        public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) {
            if (e instanceof MessageEvent) {
                final MessageEvent evt = (MessageEvent) e;
                String msg = (String) evt.getMessage();
                if (msg.equalsIgnoreCase("quit")) {
                    Channels.close(e.getChannel());
                    return;
                }
                System.err.println("SERVER:" + msg);
                Channels.write(e.getChannel(), msg);
            }
            ctx.sendDownstream(e);
        }
    }
}
