package naru.aweb.handler;

import naru.async.AsyncBuffer;
import naru.async.cache.CacheBuffer;
import naru.async.pool.PoolBase;
import naru.aweb.config.Config;
import naru.aweb.http.HeaderParser;
import naru.aweb.mapping.MappingResult;
import org.apache.log4j.Logger;

/**
 * Connection������邽�߂�WebSocket�n���h��
 * 
 * @author Naru
 *
 */
public class WSConnectionHandler extends WebSocketHandler {

    private static Logger logger = Logger.getLogger(WebSocketHandler.class);

    private static Config config = Config.getConfig();

    @Override
    public void startWebSocketResponse(HeaderParser requestHeader, String subprotocol) {
        MappingResult mapping = getRequestMapping();
        String ip = (String) mapping.getOption("ip");
        if (ip != null) {
            String remoteIp = getRemoteIp();
            if (ip.equals(remoteIp)) {
                closeWebSocket("403");
                return;
            }
        }
        doHandshake(subprotocol);
    }

    @Override
    public void onMessage(String msgs) {
        logger.debug("#message text cid:" + getChannelId());
        if ("doClose".equals(msgs)) {
            closeWebSocket("500");
        } else {
            postMessage(msgs);
        }
    }

    @Override
    public void onMessage(CacheBuffer msgs) {
        logger.debug("#message bin cid:" + getChannelId());
        if (msgs instanceof PoolBase) {
            ((PoolBase) msgs).unref();
        }
    }

    @Override
    public void onWsClose(short code, String reason) {
        logger.debug("#wsClose cid:" + getChannelId());
    }

    @Override
    public void onWsOpen(String subprotocol) {
        logger.debug("#wsOpen cid:" + getChannelId());
        postMessage("OK");
    }
}
