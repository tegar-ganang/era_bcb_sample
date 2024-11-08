package org.snova.framework.httpserver;

import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicInteger;
import org.arch.common.KeyValuePair;
import org.arch.common.Pair;
import org.arch.event.Event;
import org.arch.event.EventDispatcher;
import org.arch.event.http.HTTPChunkEvent;
import org.arch.event.http.HTTPConnectionEvent;
import org.arch.event.http.HTTPRequestEvent;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpLocalProxyRequestHandler extends SimpleChannelUpstreamHandler {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    private Channel localChannel = null;

    private Integer id;

    private static AtomicInteger seed = new AtomicInteger(1);

    public HttpLocalProxyRequestHandler() {
        id = seed.getAndIncrement();
        if (logger.isDebugEnabled()) {
            logger.debug("HttpLocalProxyRequestHandler instance created with ID:" + id);
        }
    }

    private boolean dispatchEvent(Event event) {
        Pair<Channel, Integer> attach = new Pair<Channel, Integer>(localChannel, id);
        event.setAttachment(attach);
        try {
            EventDispatcher.getSingletonInstance().dispatch(event);
            return true;
        } catch (Exception ex) {
            logger.error("Failed to dispatch event.", ex);
            return false;
        }
    }

    private HTTPRequestEvent buildEvent(HttpRequest request) {
        HTTPRequestEvent event = new HTTPRequestEvent();
        event.method = request.getMethod().getName();
        event.url = request.getUri();
        event.version = request.getProtocolVersion().getText();
        event.setHash(id);
        ChannelBuffer content = request.getContent();
        if (null != content) {
            int buflen = content.readableBytes();
            event.content.ensureWritableBytes(content.readableBytes());
            content.readBytes(event.content.getRawBuffer(), event.content.getWriteIndex(), content.readableBytes());
            event.content.advanceWriteIndex(buflen);
        }
        for (String name : request.getHeaderNames()) {
            for (String value : request.getHeaders(name)) {
                event.headers.add(new KeyValuePair<String, String>(name, value));
            }
        }
        return event;
    }

    private void handleChunks(MessageEvent e) {
        HTTPChunkEvent event = new HTTPChunkEvent();
        ChannelBuffer content = null;
        if (e.getMessage() instanceof HttpChunk) {
            HttpChunk chunk = (HttpChunk) e.getMessage();
            content = chunk.getContent();
        } else if (e.getMessage() instanceof ChannelBuffer) {
            content = (ChannelBuffer) e.getMessage();
        }
        if (null != content && content.readable()) {
            event.content = new byte[content.readableBytes()];
            content.readBytes(event.content);
            event.setHash(id);
            dispatchEvent(event);
        }
    }

    private void handleHttpRequest(HttpRequest request, MessageEvent e) {
        if (logger.isDebugEnabled()) {
            logger.debug("Local server received a request:" + request);
        }
        HTTPRequestEvent event = buildEvent(request);
        if (!dispatchEvent(event)) {
            localChannel.close();
        }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e) throws Exception {
        localChannel = e.getChannel();
        if (e.getMessage() instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) e.getMessage();
            handleHttpRequest(request, e);
        } else {
            handleChunks(e);
        }
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Browser connection[" + id + "]  closed");
        }
        close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        if (e.getCause() instanceof ClosedChannelException) {
            if (logger.isDebugEnabled()) {
                logger.error("Browser connection[" + id + "] exceptionCaught.", e.getCause());
            }
        } else {
            logger.error("Browser connection[" + id + "] exceptionCaught.", e.getCause());
        }
    }

    public void close() {
        Pair<Channel, Integer> attach = new Pair<Channel, Integer>(localChannel, id);
        HTTPConnectionEvent event = new HTTPConnectionEvent(HTTPConnectionEvent.CLOSED);
        event.setAttachment(attach);
        dispatchEvent(event);
        if (localChannel != null && localChannel.isConnected()) {
            localChannel.close();
            localChannel = null;
        }
    }
}
