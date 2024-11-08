package org.jboss.netty.handler.codec.http2;

import static org.jboss.netty.channel.Channels.*;
import static org.jboss.netty.handler.codec.http2.HttpHeaders.*;
import java.util.List;
import java.util.Map.Entry;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.util.CharsetUtil;

/**
 * A {@link ChannelHandler} that aggregates an {@link HttpMessage}
 * and its following {@link HttpChunk}s into a single {@link HttpMessage} with
 * no following {@link HttpChunk}s.  It is useful when you don't want to take
 * care of HTTP messages whose transfer encoding is 'chunked'.  Insert this
 * handler after {@link HttpMessageDecoder} in the {@link ChannelPipeline}:
 * <pre>
 * {@link ChannelPipeline} p = ...;
 * ...
 * p.addLast("decoder", new {@link HttpRequestDecoder}());
 * p.addLast("aggregator", <b>new {@link HttpChunkAggregator}(1048576)</b>);
 * ...
 * p.addLast("encoder", new {@link HttpResponseEncoder}());
 * p.addLast("handler", new HttpRequestHandler());
 * </pre>
 *
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
 * @version $Rev: 1107 $, $Date: 2012-04-15 13:00:57 -0400 (Sun, 15 Apr 2012) $
 *
 * @apiviz.landmark
 * @apiviz.has org.jboss.netty.handler.codec.http2.HttpChunk oneway - - filters out
 */
public class HttpChunkAggregator extends SimpleChannelUpstreamHandler {

    private static final ChannelBuffer CONTINUE = ChannelBuffers.copiedBuffer("HTTP/1.1 100 Continue\r\n\r\n", CharsetUtil.US_ASCII);

    private final int maxContentLength;

    private HttpMessage currentMessage;

    /**
     * Creates a new instance.
     *
     * @param maxContentLength
     *        the maximum length of the aggregated content.
     *        If the length of the aggregated content exceeds this value,
     *        a {@link TooLongFrameException} will be raised.
     */
    public HttpChunkAggregator(int maxContentLength) {
        if (maxContentLength <= 0) {
            throw new IllegalArgumentException("maxContentLength must be a positive integer: " + maxContentLength);
        }
        this.maxContentLength = maxContentLength;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        HttpMessage currentMessage = this.currentMessage;
        if (msg instanceof HttpMessage) {
            HttpMessage m = (HttpMessage) msg;
            if (is100ContinueExpected(m)) {
                write(ctx, succeededFuture(ctx.getChannel()), CONTINUE.duplicate());
            }
            if (m.isChunked()) {
                List<String> encodings = m.getHeaders(HttpHeaders.Names.TRANSFER_ENCODING);
                encodings.remove(HttpHeaders.Values.CHUNKED);
                if (encodings.isEmpty()) {
                    m.removeHeader(HttpHeaders.Names.TRANSFER_ENCODING);
                }
                m.setChunked(false);
                m.setContent(ChannelBuffers.dynamicBuffer(e.getChannel().getConfig().getBufferFactory()));
                this.currentMessage = m;
            } else {
                this.currentMessage = null;
                ctx.sendUpstream(e);
            }
        } else if (msg instanceof HttpChunk) {
            if (currentMessage == null) {
                throw new IllegalStateException("received " + HttpChunk.class.getSimpleName() + " without " + HttpMessage.class.getSimpleName());
            }
            HttpChunk chunk = (HttpChunk) msg;
            ChannelBuffer content = currentMessage.getContent();
            if (content.readableBytes() > maxContentLength - chunk.getContent().readableBytes()) {
                throw new TooLongFrameException("HTTP content length exceeded " + maxContentLength + " bytes.");
            }
            content.writeBytes(chunk.getContent());
            if (chunk.isLast()) {
                this.currentMessage = null;
                if (chunk instanceof HttpChunkTrailer) {
                    HttpChunkTrailer trailer = (HttpChunkTrailer) chunk;
                    for (Entry<String, String> header : trailer.getHeaders()) {
                        currentMessage.setHeader(header.getKey(), header.getValue());
                    }
                }
                currentMessage.setHeader(HttpHeaders.Names.CONTENT_LENGTH, String.valueOf(content.readableBytes()));
                Channels.fireMessageReceived(ctx, currentMessage, e.getRemoteAddress());
            }
        } else {
            ctx.sendUpstream(e);
        }
    }
}
