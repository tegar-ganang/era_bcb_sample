package org.jmantis.server.adapter;

import static org.jboss.netty.handler.codec.http.HttpHeaders.is100ContinueExpected;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import java.io.ByteArrayOutputStream;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.WriteCompletionEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jmantis.web.Dispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebAdapter extends SimpleChannelUpstreamHandler {

    private static final Logger log = LoggerFactory.getLogger(WebAdapter.class);

    private static final Dispatcher dispatcher = Dispatcher.getInstance();

    private boolean readingChunks;

    private ByteArrayOutputStream baos = new ByteArrayOutputStream();

    @Override
    public void messageReceived(final ChannelHandlerContext chc, final MessageEvent evt) throws Exception {
        if (!readingChunks) {
            HttpRequest request = (HttpRequest) evt.getMessage();
            if (is100ContinueExpected(request)) {
                send100Continue(evt);
            }
            if (request.isChunked()) {
                readingChunks = true;
            } else {
                ChannelBuffer content = request.getContent();
                if (content.readable()) {
                    baos.write(content.array());
                }
                service(chc, evt);
            }
        } else {
            HttpChunk chunk = (HttpChunk) evt.getMessage();
            if (chunk.isLast()) {
                service(chc, evt);
            } else {
                ChannelBuffer content = chunk.getContent();
                baos.write(content.array());
            }
        }
    }

    private final void service(final ChannelHandlerContext chc, final MessageEvent evt) throws Exception {
        baos.flush();
        byte[] b = baos.toByteArray();
        baos.reset();
        dispatcher.service(chc, evt, b);
    }

    private void send100Continue(MessageEvent me) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, CONTINUE);
        me.getChannel().write(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext chc, ExceptionEvent e) throws Exception {
        log.error("Channel错误", e.getCause());
        e.getChannel().close();
    }

    @Override
    public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e) throws Exception {
        super.writeComplete(ctx, e);
        Object o = ctx.getAttachment();
        if (o instanceof org.jmantis.web.HttpResponse) {
            org.jmantis.web.HttpResponse resp = (org.jmantis.web.HttpResponse) o;
            if (resp.isClose()) {
                log.debug("writeComplete close");
            }
        }
        log.info("writeComplete");
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext chc, ChannelStateEvent e) throws Exception {
        super.channelDisconnected(chc, e);
        log.info("channelDisconnected");
    }
}
