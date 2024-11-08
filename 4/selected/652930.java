package org.jboss.netty.handler.codec.http2;

import java.util.Queue;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.embedder.EncoderEmbedder;
import org.jboss.netty.util.internal.QueueFactory;

/**
 * Encodes the content of the outbound {@link HttpResponse} and {@link HttpChunk}.
 * The original content is replaced with the new content encoded by the
 * {@link EncoderEmbedder}, which is created by {@link #newContentEncoder(String)}.
 * Once encoding is finished, the value of the <tt>'Content-Encoding'</tt> header
 * is set to the target content encoding, as returned by {@link #getTargetContentEncoding(String)}.
 * Also, the <tt>'Content-Length'</tt> header is updated to the length of the
 * encoded content.  If there is no supported encoding in the
 * corresponding {@link HttpRequest}'s {@code "Accept-Encoding"} header,
 * {@link #newContentEncoder(String)} should return {@code null} so that no
 * encoding occurs (i.e. pass-through).
 * <p>
 * Please note that this is an abstract class.  You have to extend this class
 * and implement {@link #newContentEncoder(String)} and {@link #getTargetContentEncoding(String)}
 * properly to make this class functional.  For example, refer to the source
 * code of {@link HttpContentCompressor}.
 * <p>
 * This handler must be placed after {@link HttpMessageEncoder} in the pipeline
 * so that this handler can intercept HTTP responses before {@link HttpMessageEncoder}
 * converts them into {@link ChannelBuffer}s.
 *
 * @author <a href="http://www.jboss.org/netty/">The Netty Project</a>
 * @author <a href="http://gleamynode.net/">Trustin Lee</a>
 * @version $Rev: 1107 $, $Date: 2012-04-15 13:00:57 -0400 (Sun, 15 Apr 2012) $
 */
public abstract class HttpContentEncoder extends SimpleChannelHandler {

    private final Queue<String> acceptEncodingQueue = QueueFactory.createQueue(String.class);

    private volatile EncoderEmbedder<ChannelBuffer> encoder;

    /**
     * Creates a new instance.
     */
    protected HttpContentEncoder() {
        super();
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (!(msg instanceof HttpMessage)) {
            ctx.sendUpstream(e);
            return;
        }
        HttpMessage m = (HttpMessage) msg;
        String acceptedEncoding = m.getHeader(HttpHeaders.Names.ACCEPT_ENCODING);
        if (acceptedEncoding == null) {
            acceptedEncoding = HttpHeaders.Values.IDENTITY;
        }
        boolean offered = acceptEncodingQueue.offer(acceptedEncoding);
        assert offered;
        ctx.sendUpstream(e);
    }

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof HttpResponse && ((HttpResponse) msg).getStatus().getCode() == 100) {
            ctx.sendDownstream(e);
        } else if (msg instanceof HttpMessage) {
            HttpMessage m = (HttpMessage) msg;
            encoder = null;
            String contentEncoding = m.getHeader(HttpHeaders.Names.CONTENT_ENCODING);
            if (contentEncoding != null && !HttpHeaders.Values.IDENTITY.equalsIgnoreCase(contentEncoding)) {
                ctx.sendDownstream(e);
            } else {
                String acceptEncoding = acceptEncodingQueue.poll();
                if (acceptEncoding == null) {
                    throw new IllegalStateException("cannot send more responses than requests");
                }
                boolean hasContent = m.isChunked() || m.getContent().readable();
                if (hasContent && (encoder = newContentEncoder(acceptEncoding)) != null) {
                    m.setHeader(HttpHeaders.Names.CONTENT_ENCODING, getTargetContentEncoding(acceptEncoding));
                    if (!m.isChunked()) {
                        ChannelBuffer content = m.getContent();
                        content = ChannelBuffers.wrappedBuffer(encode(content), finishEncode());
                        m.setContent(content);
                        if (m.containsHeader(HttpHeaders.Names.CONTENT_LENGTH)) {
                            m.setHeader(HttpHeaders.Names.CONTENT_LENGTH, Integer.toString(content.readableBytes()));
                        }
                    }
                }
                ctx.sendDownstream(e);
            }
        } else if (msg instanceof HttpChunk) {
            HttpChunk c = (HttpChunk) msg;
            ChannelBuffer content = c.getContent();
            if (encoder != null) {
                if (!c.isLast()) {
                    content = encode(content);
                    if (content.readable()) {
                        c.setContent(content);
                        ctx.sendDownstream(e);
                    }
                } else {
                    ChannelBuffer lastProduct = finishEncode();
                    if (lastProduct.readable()) {
                        Channels.write(ctx, Channels.succeededFuture(e.getChannel()), new DefaultHttpChunk(lastProduct), e.getRemoteAddress());
                    }
                    ctx.sendDownstream(e);
                }
            } else {
                ctx.sendDownstream(e);
            }
        } else {
            ctx.sendDownstream(e);
        }
    }

    /**
     * Returns a new {@link EncoderEmbedder} that encodes the HTTP message
     * content.
     *
     * @param acceptEncoding
     *        the value of the {@code "Accept-Encoding"} header
     *
     * @return a new {@link EncoderEmbedder} if there is a supported encoding
     *         in {@code acceptEncoding}.  {@code null} otherwise.
     */
    protected abstract EncoderEmbedder<ChannelBuffer> newContentEncoder(String acceptEncoding) throws Exception;

    /**
     * Returns the expected content encoding of the encoded content.
     *
     * @param acceptEncoding the value of the {@code "Accept-Encoding"} header
     * @return the expected content encoding of the new content
     */
    protected abstract String getTargetContentEncoding(String acceptEncoding) throws Exception;

    private ChannelBuffer encode(ChannelBuffer buf) {
        encoder.offer(buf);
        return ChannelBuffers.wrappedBuffer(encoder.pollAll(new ChannelBuffer[encoder.size()]));
    }

    private ChannelBuffer finishEncode() {
        ChannelBuffer result;
        if (encoder.finish()) {
            result = ChannelBuffers.wrappedBuffer(encoder.pollAll(new ChannelBuffer[encoder.size()]));
        } else {
            result = ChannelBuffers.EMPTY_BUFFER;
        }
        encoder = null;
        return result;
    }
}
