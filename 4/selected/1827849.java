package naru.aweb.handler;

import java.nio.ByteBuffer;
import naru.async.AsyncBuffer;
import naru.async.cache.CacheBuffer;
import naru.aweb.auth.LogoutEvent;
import naru.aweb.handler.ws.WsHybiFrame;
import naru.aweb.handler.ws.WsProtocol;
import naru.aweb.http.HeaderParser;
import naru.aweb.http.RequestContext;
import naru.aweb.http.WebServerHandler;
import naru.aweb.mapping.MappingResult;
import org.apache.log4j.Logger;

/**
 * WebSocket���󂯕t�����ꍇ�ɂ́AWebSocket�v���g�R��������
 * �w�łȂ��ꍇ�́Ahttp���N�G�X�g�������ł���悤�ɂ���WebServerHandler�Ɠ����̓���
 * 
 * @author Naru
 *
 */
public abstract class WebSocketHandler extends WebServerHandler implements LogoutEvent {

    private static Logger logger = Logger.getLogger(WebSocketHandler.class);

    protected boolean isWs;

    private boolean isHandshaked;

    private WsProtocol wsProtocol;

    protected synchronized void postMessage(String message) {
        wsProtocol.postMessage(message);
    }

    protected synchronized void postMessage(ByteBuffer[] message) {
        wsProtocol.postMessage(message);
    }

    protected synchronized void postMessage(AsyncBuffer message) {
        wsProtocol.postMessage(message);
    }

    /**
	 * statusCode �ڑ��O�������ꍇ�A�u���E�U�ɕԋp����statusCode
	 */
    protected void closeWebSocket(String statusCode, short code, String reason) {
        if (isHandshaked) {
            wsProtocol.onClose(code, reason);
        } else {
            completeResponse(statusCode);
        }
    }

    protected void closeWebSocket(String statusCode) {
        closeWebSocket(statusCode, WsHybiFrame.CLOSE_NORMAL, "OK");
    }

    public abstract void onWsOpen(String subprotocol);

    public abstract void onWsClose(short code, String reason);

    public abstract void onMessage(String msgs);

    public abstract void onMessage(CacheBuffer msgs);

    /**
	 * WebSocket�ڑ����ɃZ�V�������ꂽ�ꍇ�̒ʒm
	 */
    public void onLogout() {
        closeWebSocket("500");
    }

    public void startResponse() {
        logger.debug("#doResponse.cid:" + getChannelId());
        HeaderParser requestHeader = getRequestHeader();
        if (!requestHeader.isWs()) {
            super.startResponse();
            return;
        }
        isWs = true;
        MappingResult mapping = getRequestMapping();
        RequestContext requestContext = getRequestContext();
        requestContext.registerLogoutEvnet(this);
        wsProtocol = WsProtocol.createWsProtocol(requestHeader, getRequestMapping());
        if (wsProtocol == null) {
            completeResponse("400");
            logger.warn("not found WebSocket Protocol");
            return;
        }
        logger.debug("wsProtocol class:" + wsProtocol.getClass().getName());
        String selectSubprotocol = null;
        String reqSubprotocols = wsProtocol.getRequestSubProtocols(requestHeader);
        if (reqSubprotocols == null) {
            if (wsProtocol.isUseSubprotocol()) {
                completeResponse("400");
                return;
            }
        } else {
            selectSubprotocol = wsProtocol.checkSubprotocol(reqSubprotocols);
            if (selectSubprotocol == null) {
                logger.debug("WsHybi10#suprotocol error.webSocketProtocol:" + reqSubprotocols);
                completeResponse("400");
                return;
            }
        }
        startWebSocketResponse(requestHeader, selectSubprotocol);
    }

    public void startWebSocketResponse(HeaderParser requestHeader, String subprotocol) {
        doHandshake(subprotocol);
    }

    public final boolean doHandshake(String subProtocol) {
        HeaderParser requestHeader = getRequestHeader();
        wsProtocol.setup(this);
        isHandshaked = wsProtocol.onHandshake(requestHeader, subProtocol);
        return isHandshaked;
    }

    public void onReadPlain(Object userContext, ByteBuffer[] buffers) {
        logger.debug("#read.cid:" + getChannelId());
        if (!isWs) {
            super.onReadPlain(userContext, buffers);
            return;
        }
        wsProtocol.onBuffer(buffers);
    }

    public void onFailure(Object userContext, Throwable t) {
        logger.debug("#failer.cid:" + getChannelId() + ":" + t.getMessage());
        closeWebSocket("500");
        super.onFailure(userContext, t);
    }

    public void onReadTimeout(Object userContext) {
        logger.debug("#readTimeout.cid:" + getChannelId());
        wsProtocol.onReadTimeout();
    }

    public void onTimeout(Object userContext) {
        logger.debug("#timeout.cid:" + getChannelId());
        closeWebSocket("500");
        super.onTimeout(userContext);
    }

    public void onClosed(Object userContext) {
        logger.debug("#closed client.cid:" + getChannelId());
        super.onClosed(userContext);
    }

    @Override
    public void onFinished() {
        logger.debug("#finished client.cid:" + getChannelId());
        if (wsProtocol != null) {
            wsProtocol.onClose(WsHybiFrame.CLOSE_UNKOWN, null);
        }
        super.onFinished();
    }

    @Override
    public void recycle() {
        isWs = false;
        isHandshaked = false;
        if (wsProtocol != null) {
            wsProtocol.unref(true);
            wsProtocol = null;
        }
        super.recycle();
    }

    public void onPosted() {
    }

    @Override
    public void onWrittenPlain(Object userContext) {
        if (wsProtocol != null) {
            wsProtocol.onWrittenPlain(userContext);
        }
        super.onWrittenPlain(userContext);
    }
}
