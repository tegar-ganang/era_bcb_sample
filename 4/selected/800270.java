package naru.aweb.handler;

import naru.async.cache.CacheBuffer;
import naru.aweb.config.Config;
import naru.aweb.http.HeaderParser;
import naru.aweb.http.WsClient;
import naru.aweb.http.WsClientHandler;
import naru.aweb.mapping.MappingResult;
import naru.aweb.util.ServerParser;
import org.apache.log4j.Logger;

/**
 * websocket proxy�Ƃ��ē���
 * 1)�u���E�U���Aph��WebSocket�T�[�o�Ƃ݂Ȃ��Ă���ꍇ
 * 2)�u���E�U���Aph��Proxy�Ƃ݂Ȃ��Ă���ꍇ
 * @author Naru
 *
 */
public class WsProxyHandler extends WebSocketHandler implements WsClient {

    private static Logger logger = Logger.getLogger(WsProxyHandler.class);

    private static Config config = Config.getConfig();

    private WsClientHandler wsClientHandler;

    @Override
    public void startWebSocketResponse(HeaderParser requestHeader, String subProtocols) {
        MappingResult mapping = getRequestMapping();
        ServerParser targetHostServer = mapping.getResolveServer();
        String path = mapping.getResolvePath();
        requestHeader.setPath(path);
        wsClientHandler = WsClientHandler.create(mapping.isResolvedHttps(), targetHostServer.getHost(), targetHostServer.getPort());
        wsClientHandler.ref();
        String uri = requestHeader.getRequestUri();
        String origin = requestHeader.getHeader("Origin");
        wsClientHandler.startRequest(this, null, 10000, uri, subProtocols, origin);
    }

    @Override
    public void onMessage(String message) {
        wsClientHandler.postMessage(message);
    }

    @Override
    public void onMessage(CacheBuffer message) {
        wsClientHandler.postMessage(message);
    }

    @Override
    public void onWsClose(short code, String reason) {
        wsClientHandler.doClose(code, reason);
    }

    @Override
    public void onWsOpen(String subprotocol) {
        ref();
    }

    @Override
    public void onWcSslHandshaked(Object userContext) {
    }

    @Override
    public void onWcConnected(Object userContext) {
    }

    @Override
    public void onWcClose(Object userContext, int stat, short closeCode, String closeReason) {
        logger.debug("#onWcClose cid:" + getChannelId());
        closeWebSocket("500", closeCode, closeReason);
        unref();
    }

    @Override
    public void onWcFailure(Object userContext, int stat, Throwable t) {
        logger.debug("#wcFailure cid:" + getChannelId());
        closeWebSocket("500");
        unref();
    }

    @Override
    public void onWcHandshaked(Object userContext, String subprotocol) {
        logger.debug("#wcHandshaked cid:" + getChannelId() + " subprotocol:" + subprotocol);
        doHandshake(subprotocol);
    }

    @Override
    public void onWcMessage(Object userContext, String message) {
        postMessage(message);
    }

    @Override
    public void onWcMessage(Object userContext, CacheBuffer message) {
        postMessage(message);
    }

    @Override
    public void onWcProxyConnected(Object userContext) {
    }

    @Override
    public void onWcWrittenHeader(Object userContext) {
    }

    @Override
    public void onWcResponseHeader(Object userContext, HeaderParser responseHeader) {
    }

    @Override
    public void recycle() {
        if (wsClientHandler != null) {
            wsClientHandler.unref();
            wsClientHandler = null;
        }
        super.recycle();
    }
}
