package naru.aweb.handler.ws;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import org.apache.log4j.Logger;
import naru.async.AsyncBuffer;
import naru.async.BufferGetter;
import naru.async.cache.CacheBuffer;
import naru.async.pool.BuffersUtil;
import naru.async.pool.PoolBase;
import naru.async.pool.PoolManager;
import naru.async.store.DataUtil;
import naru.aweb.http.HeaderParser;

public class WsHybi10 extends WsProtocol implements BufferGetter {

    private static Logger logger = Logger.getLogger(WsHybi10.class);

    private static final String SPEC = "hybi10";

    private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private static final int SUPPORT_VERSION = 13;

    private boolean isSendClose = false;

    private byte continuePcode;

    private int continuePayloadLength = 0;

    private CacheBuffer payloadBuffer;

    private WsHybiFrame frame = new WsHybiFrame();

    @Override
    public void recycle() {
        isSendClose = false;
        continuePcode = -1;
        continuePayloadLength = 0;
        if (payloadBuffer != null) {
            payloadBuffer.unref();
            payloadBuffer = null;
        }
        frame.init();
        if (curReq != null) {
            curReq.clean();
            curReq = null;
        }
        super.recycle();
    }

    @Override
    public boolean onHandshake(HeaderParser requestHeader, String subProtocol) {
        logger.debug("WsHybi10#onHandshake cid:" + handler.getChannelId());
        if (!isUseSpec(SPEC)) {
            handler.completeResponse("400");
            return false;
        }
        String version = requestHeader.getHeader(SEC_WEBSOCKET_VERSION);
        logger.debug("WebSocket version:" + version);
        int v = Integer.parseInt(version);
        if (v > SUPPORT_VERSION) {
            logger.debug("WsHybi10#version error:" + v);
            handler.setHeader(SEC_WEBSOCKET_VERSION, Integer.toString(SUPPORT_VERSION));
            handler.completeResponse("400");
            return false;
        }
        if (subProtocol != null) {
            handler.setHeader(SEC_WEBSOCKET_PROTOCOL, subProtocol);
        }
        String key = requestHeader.getHeader(SEC_WEBSOCKET_KEY);
        String accept = DataUtil.digestBase64Sha1((key + GUID).getBytes());
        handler.setHttpVersion("HTTP/1.1");
        handler.setStatusCode("101", "Switching Protocols");
        handler.setHeader("Upgrade", "WebSocket");
        handler.setHeader("Connection", "Upgrade");
        handler.setHeader(SEC_WEBSOCKET_ACCEPT, accept);
        handler.flushHeaderForWebSocket(SPEC, subProtocol);
        handler.onWsOpen(subProtocol);
        handler.setReadTimeout(getWebSocketPingInterval());
        handler.asyncRead(null);
        return true;
    }

    private void sendClose(short code, String reason) {
        if (isSendClose) {
            handler.asyncClose(null);
            return;
        }
        isSendClose = true;
        traceClose(code, reason);
        ByteBuffer[] closeBuffer = WsHybiFrame.createCloseFrame(isWebSocketResponseMask(), code, reason);
        handler.asyncWrite(null, closeBuffer);
    }

    private void doBinaryTrace(byte pcode, boolean isTop, boolean isFin, ByteBuffer[] payloadBuffers) {
        if (pcode != WsHybiFrame.PCODE_BINARY && continuePcode != WsHybiFrame.PCODE_BINARY) {
            return;
        }
        traceOnMessage(isTop, isFin, payloadBuffers);
    }

    private void doFrame() {
        logger.debug("WsHybi10#doFrame cid:" + handler.getChannelId());
        byte pcode = frame.getPcode();
        ByteBuffer[] payloadBuffers = frame.getPayloadBuffers();
        doBinaryTrace(pcode, payloadBuffer == null, frame.isFin(), payloadBuffers);
        if (payloadBuffer == null) {
            payloadBuffer = CacheBuffer.open();
        }
        if (!frame.isFin()) {
            logger.debug("WsHybi10#doFrame not isFin");
            if (pcode != WsHybiFrame.PCODE_CONTINUE) {
                continuePcode = pcode;
            }
            continuePayloadLength += BuffersUtil.remaining(payloadBuffers);
            payloadBuffer.putBuffer(payloadBuffers);
            if (continuePayloadLength >= getWebSocketMessageLimit()) {
                logger.debug("WsHybi10#doFrame too long frame.continuePayloadLength:" + continuePayloadLength);
                sendClose(WsHybiFrame.CLOSE_MESSAGE_TOO_BIG, "too long frame");
            }
            return;
        }
        if (pcode == WsHybiFrame.PCODE_CONTINUE) {
            logger.debug("WsHybi10#doFrame pcode CONTINUE");
            pcode = continuePcode;
            continuePayloadLength = 0;
            continuePcode = -1;
        }
        payloadBuffer.putBuffer(payloadBuffers);
        payloadBuffer.flip();
        switch(pcode) {
            case WsHybiFrame.PCODE_TEXT:
                logger.debug("WsHybi10#doFrame pcode TEXT");
                if (!payloadBuffer.isInTopBuffer()) {
                    throw new UnsupportedOperationException("unsuppert big text");
                }
                payloadBuffers = payloadBuffer.popTopBuffer();
                for (ByteBuffer buffer : payloadBuffers) {
                    convertPutBuffer(buffer);
                }
                PoolManager.poolArrayInstance(payloadBuffers);
                String textMessage = convertToString();
                traceOnMessage(textMessage);
                callOnMessage(textMessage);
                break;
            case WsHybiFrame.PCODE_BINARY:
                logger.debug("WsHybi10#doFrame pcode BINARY");
                callBinaryOnMessage(payloadBuffer);
                payloadBuffer = null;
                break;
            case WsHybiFrame.PCODE_CLOSE:
                logger.debug("WsHybi10#doFrame pcode CLOSE");
                PoolManager.poolBufferInstance(payloadBuffers);
                traceOnClose(frame.getCloseCode(), frame.getCloseReason());
                sendClose(WsHybiFrame.CLOSE_NORMAL, null);
                break;
            case WsHybiFrame.PCODE_PING:
                logger.debug("WsHybi10#doFrame pcode PING");
                tracePingPong("PING");
                ByteBuffer[] pongBuffer = WsHybiFrame.createPongFrame(isWebSocketResponseMask(), payloadBuffers);
                handler.asyncWrite(null, pongBuffer);
                break;
            case WsHybiFrame.PCODE_PONG:
                logger.debug("WsHybi10#doFrame pcode PONG");
                PoolManager.poolBufferInstance(payloadBuffers);
                tracePingPong("PONG");
                break;
        }
        if (payloadBuffer != null) {
            payloadBuffer.unref();
            payloadBuffer = null;
        }
        if (frame.parseNextFrame()) {
            doFrame();
        }
    }

    @Override
    public void onBuffer(ByteBuffer[] buffers) {
        logger.debug("WsHybi10#onBuffer cid:" + handler.getChannelId());
        try {
            for (ByteBuffer buffer : buffers) {
                if (frame.parse(buffer)) {
                    doFrame();
                }
            }
            PoolManager.poolArrayInstance(buffers);
            if (frame.getPayloadLength() > getWebSocketMessageLimit()) {
                logger.debug("WsHybi10#doFrame too long frame.frame.getPayloadLength():" + frame.getPayloadLength());
                sendClose(WsHybiFrame.CLOSE_MESSAGE_TOO_BIG, "too long frame");
            }
            handler.asyncRead(null);
        } catch (RuntimeException e) {
            logger.error("Hybi10 parse error.", e);
            handler.asyncClose(null);
        }
    }

    @Override
    public void onClose(short code, String reason) {
        logger.debug("WsHybi10#onClose cid:" + handler.getChannelId());
        if (handler == null) {
            return;
        }
        logger.debug("WsHybi10#onClose cid:" + handler.getChannelId());
        callOnWsClose(code, reason);
        if (code != WsHybiFrame.CLOSE_UNKOWN) {
            sendClose((short) code, reason);
        }
    }

    @Override
    public void onReadTimeout() {
        logger.debug("WsHybi10#onReadTimeout cid:" + handler.getChannelId());
        ByteBuffer[] buffers = WsHybiFrame.createPingFrame(isWebSocketResponseMask(), "ping:" + System.currentTimeMillis());
        handler.asyncWrite(null, buffers);
        handler.asyncRead(null);
    }

    @Override
    public void postMessage(String message) {
        logger.debug("WsHybi10#postMessage(txt) cid:" + handler.getChannelId());
        postMessage(new PostRequest(message));
    }

    @Override
    public void postMessage(ByteBuffer[] message) {
        logger.debug("WsHybi10#postMessage(bin) cid:" + handler.getChannelId());
        postMessage(new PostRequest(message));
    }

    @Override
    public String getWsProtocolName() {
        return "Hibi10";
    }

    @Override
    public String getRequestSubProtocols(HeaderParser requestHeader) {
        return requestHeader.getHeader(SEC_WEBSOCKET_PROTOCOL);
    }

    private static class PostRequest {

        public PostRequest(AsyncBuffer asyncMessage) {
            this.asyncMessage = asyncMessage;
            position = 0;
            endPosition = asyncMessage.bufferLength();
            isTop = true;
            strMessage = null;
            byteMessage = null;
        }

        public PostRequest(String message) {
            this.strMessage = message;
        }

        public PostRequest(ByteBuffer[] message) {
            this.byteMessage = message;
            this.strMessage = null;
        }

        public void clean() {
            if (asyncMessage != null && asyncMessage instanceof PoolBase) {
                ((PoolBase) asyncMessage).unref();
            }
            asyncMessage = null;
        }

        String strMessage;

        ByteBuffer[] byteMessage;

        AsyncBuffer asyncMessage;

        long position;

        long endPosition;

        boolean isTop = true;
    }

    private PostRequest curReq = null;

    private LinkedList<PostRequest> postQueue = new LinkedList<PostRequest>();

    private synchronized void postMessage(PostRequest req) {
        if (req == null) {
            return;
        }
        if (curReq != null) {
            postQueue.add(req);
            return;
        }
        curReq = req;
        if (curReq.strMessage != null) {
            tracePostMessage(curReq.strMessage);
            ByteBuffer[] buffers = WsHybiFrame.createTextFrame(isWebSocketResponseMask(), curReq.strMessage);
            handler.asyncWrite(curReq, buffers);
            return;
        }
        if (curReq.byteMessage != null) {
            tracePostMessage(true, true, curReq.byteMessage);
            ByteBuffer[] buffers = WsHybiFrame.createBinaryFrame(true, true, isWebSocketResponseMask(), curReq.byteMessage);
            handler.asyncWrite(curReq, buffers);
            return;
        }
        curReq.asyncMessage.asyncBuffer(this, curReq);
    }

    @Override
    public void onWrittenPlain(Object userContext) {
        boolean isPostEnd = false;
        synchronized (this) {
            if (curReq == null || curReq != userContext) {
                return;
            }
            if (curReq.position == curReq.endPosition) {
                isPostEnd = true;
            }
        }
        if (!isPostEnd) {
            curReq.asyncMessage.asyncBuffer(this, curReq);
            return;
        }
        curReq.clean();
        handler.onPosted();
        synchronized (this) {
            curReq = null;
            if (postQueue.size() == 0) {
                return;
            }
            PostRequest req = postQueue.removeFirst();
            if (req != null) {
                postMessage(req);
            }
        }
    }

    @Override
    public void postMessage(AsyncBuffer message) {
        logger.debug("WsHybi10#postMessage(bin) cid:" + handler.getChannelId());
        postMessage(new PostRequest(message));
    }

    @Override
    public boolean onBuffer(Object ctx, ByteBuffer[] buffers) {
        long len = BuffersUtil.remaining(buffers);
        if (curReq.endPosition <= (curReq.position + len)) {
            BuffersUtil.cut(buffers, curReq.endPosition - curReq.position);
            curReq.position = curReq.endPosition;
        } else {
            curReq.position += len;
        }
        boolean isFin = false;
        if (curReq.position == curReq.endPosition) {
            isFin = true;
        }
        tracePostMessage(curReq.isTop, isFin, buffers);
        buffers = WsHybiFrame.createBinaryFrame(curReq.isTop, isFin, isWebSocketResponseMask(), buffers);
        curReq.isTop = false;
        handler.asyncWrite(curReq, buffers);
        return false;
    }

    @Override
    public void onBufferEnd(Object ctx) {
        throw new IllegalStateException("called onBufferEnd");
    }

    @Override
    public void onBufferFailure(Object ctx, Throwable failure) {
        synchronized (this) {
            if (curReq == null) {
                return;
            }
            curReq.clean();
            curReq = null;
        }
        handler.onFailure(failure);
    }
}
