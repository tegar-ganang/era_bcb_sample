package naru.aweb.http;

import java.nio.ByteBuffer;
import javax.net.ssl.SSLEngine;
import org.apache.log4j.Logger;
import naru.async.Timer;
import naru.async.pool.BuffersUtil;
import naru.async.pool.PoolBase;
import naru.async.pool.PoolManager;
import naru.async.ssl.SslHandler;
import naru.async.timer.TimerManager;
import naru.aweb.config.Config;
import naru.aweb.robot.CallScheduler;

public class WebClientHandler extends SslHandler implements Timer {

    private static final int STAT_INIT = 0;

    private static final int STAT_CONNECT = 1;

    private static final int STAT_SSL_PROXY = 2;

    private static final int STAT_SSL_HANDSHAKE = 3;

    private static final int STAT_REQUEST_HEADER = 4;

    private static final int STAT_REQUEST_BODY = 5;

    private static final int STAT_RESPONSE_HEADER = 6;

    private static final int STAT_RESPONSE_BODY = 7;

    private static final int STAT_KEEP_ALIVE = 8;

    private static final int STAT_END = 9;

    public static final String CONTEXT_HEADER = "contextHeader";

    public static final String CONTEXT_BODY = "contextBody";

    public static final String CONTEXT_SSL_PROXY_CONNECT = "contextSslProxyConnect";

    public static final Throwable FAILURE_CONNECT = new Throwable("WebClientHandler connect");

    public static final Throwable FAILURE_TIMEOUT = new Throwable("WebClientHandler timeout");

    private static Logger logger = Logger.getLogger(WebClientHandler.class);

    private static Config config = Config.getConfig();

    private int stat;

    private boolean isKeepAlive;

    private boolean isCallerKeepAlive;

    private long keepAliveTimeout = 15000;

    private WebClientConnection webClientConnection;

    private CallScheduler scheduler = null;

    private long sslProxyActualWriteTime;

    private long headerActualWriteTime;

    private long bodyActualWriteTime;

    private ByteBuffer[] requestHeaderBuffer;

    private ByteBuffer[] requestBodyBuffer;

    private long requestContentLength;

    private long requestContentWriteLength;

    private HeaderParser responseHeader = new HeaderParser();

    private ChunkContext responseChunk = new ChunkContext();

    private GzipContext gzipContext = new GzipContext();

    private boolean isReadableCallback;

    private boolean isGzip;

    private long requestHeaderLength;

    private long responseHeaderLength;

    private WebClient webClient;

    private Object userContext;

    public static WebClientHandler create(WebClientConnection webClientConnection) {
        logger.debug("create:" + webClientConnection);
        WebClientHandler webClientHandler = (WebClientHandler) PoolManager.getInstance(WebClientHandler.class);
        webClientHandler.setWebClientConnection(webClientConnection);
        return webClientHandler;
    }

    /**
	 * webClientConnection�̃��C�t�T�C�N���́A�쐬�����WebClientHandler�ƈ�v����
	 * @param isHttps
	 * @param targetServer
	 * @param targetPort
	 * @return
	 */
    public static WebClientHandler create(boolean isHttps, String targetServer, int targetPort) {
        WebClientHandler webClientHandler = (WebClientHandler) PoolManager.getInstance(WebClientHandler.class);
        webClientHandler.webClientConnection = (WebClientConnection) PoolManager.getInstance(WebClientConnection.class);
        webClientHandler.webClientConnection.init(isHttps, targetServer, targetPort);
        return webClientHandler;
    }

    public SSLEngine getSSLEngine() {
        return config.getSslEngine(null);
    }

    private void setWebClient(WebClient webClient) {
        PoolBase poolBase = null;
        if (webClient != null) {
            if (webClient instanceof PoolBase) {
                poolBase = (PoolBase) webClient;
                poolBase.ref();
            }
        }
        if (this.webClient != null) {
            if (this.webClient instanceof PoolBase) {
                poolBase = (PoolBase) this.webClient;
                poolBase.unref();
            }
        }
        this.webClient = webClient;
    }

    public void recycle() {
        stat = STAT_INIT;
        setScheduler(null);
        setWebClient(null);
        isKeepAlive = false;
        if (requestHeaderBuffer != null) {
            PoolManager.poolBufferInstance(requestHeaderBuffer);
            requestHeaderBuffer = null;
        }
        if (requestBodyBuffer != null) {
            PoolManager.poolBufferInstance(requestBodyBuffer);
            requestBodyBuffer = null;
        }
        requestContentLength = requestContentWriteLength = 0;
        requestHeaderLength = responseHeaderLength = 0;
        setScheduler(null);
        responseHeader.recycle();
        gzipContext.recycle();
        isReadableCallback = false;
        setWebClientConnection(null);
        sslProxyActualWriteTime = headerActualWriteTime = bodyActualWriteTime = -1;
        super.recycle();
    }

    public void setScheduler(CallScheduler scheduler) {
        if (scheduler != null) {
            scheduler.ref();
        }
        if (this.scheduler != null) {
            this.scheduler.unref();
        }
        this.scheduler = scheduler;
    }

    private void internalStartRequest() {
        synchronized (this) {
            stat = STAT_REQUEST_HEADER;
            logger.debug("startRequest requestHeaderBuffer length:" + BuffersUtil.remaining(requestHeaderBuffer) + ":" + getPoolId() + ":cid:" + getChannelId());
            requestHeaderLength = BuffersUtil.remaining(requestHeaderBuffer);
            if (scheduler != null) {
                scheduler.scheduleWrite(CONTEXT_HEADER, requestHeaderBuffer);
            } else {
                headerActualWriteTime = System.currentTimeMillis();
                asyncWrite(CONTEXT_HEADER, requestHeaderBuffer);
            }
            requestHeaderBuffer = null;
            if (requestBodyBuffer != null) {
                stat = STAT_REQUEST_BODY;
                long length = BuffersUtil.remaining(requestBodyBuffer);
                requestContentWriteLength += length;
                if (scheduler != null) {
                    scheduler.scheduleWrite(CONTEXT_BODY, requestBodyBuffer);
                } else {
                    bodyActualWriteTime = System.currentTimeMillis();
                    asyncWrite(CONTEXT_BODY, requestBodyBuffer);
                }
                requestBodyBuffer = null;
            }
        }
        if (requestContentWriteLength >= requestContentLength) {
        }
    }

    public void onConnected(Object userContext) {
        logger.debug("#connected.id:" + getChannelId());
        onWebConnected();
        if (webClientConnection.isHttps()) {
            if (webClientConnection.isUseProxy()) {
                stat = STAT_SSL_PROXY;
                StringBuffer sb = new StringBuffer(512);
                sb.append("CONNECT ");
                sb.append(webClientConnection.getTargetServer());
                sb.append(":");
                sb.append(webClientConnection.getTargetPort());
                sb.append(" HTTP/1.0\r\nHost: ");
                sb.append(webClientConnection.getTargetServer());
                sb.append(":");
                sb.append(webClientConnection.getTargetPort());
                sb.append("\r\nContent-Length: 0\r\n\r\n");
                ByteBuffer buf = ByteBuffer.wrap(sb.toString().getBytes());
                if (scheduler != null) {
                    scheduler.scheduleWrite(CONTEXT_SSL_PROXY_CONNECT, BuffersUtil.toByteBufferArray(buf));
                } else {
                    sslProxyActualWriteTime = System.currentTimeMillis();
                    asyncWrite(CONTEXT_SSL_PROXY_CONNECT, BuffersUtil.toByteBufferArray(buf));
                }
                asyncRead(CONTEXT_SSL_PROXY_CONNECT);
                return;
            } else {
                stat = STAT_SSL_HANDSHAKE;
                sslOpen(true);
                return;
            }
        }
        asyncRead(CONTEXT_HEADER);
        internalStartRequest();
    }

    public boolean onHandshaked() {
        logger.debug("#handshaked.cid:" + getChannelId());
        onWebHandshaked();
        asyncRead(CONTEXT_HEADER);
        internalStartRequest();
        return false;
    }

    public final void onWrittenPlain(Object userContext) {
        logger.debug("#writtenPlain.cid:" + getChannelId());
        if (userContext == CONTEXT_HEADER) {
            onWrittenRequestHeader();
        } else if (userContext == CONTEXT_BODY) {
            onWrittenRequestBody();
        }
    }

    public void onRead(Object userContext, ByteBuffer[] buffers) {
        if (userContext == CONTEXT_SSL_PROXY_CONNECT) {
            for (int i = 0; i < buffers.length; i++) {
                responseHeader.parse(buffers[i]);
            }
            PoolManager.poolArrayInstance(buffers);
            if (responseHeader.isParseEnd()) {
                onWebProxyConnected();
                if (responseHeader.isParseError()) {
                    logger.warn("ssl proxy header error");
                    asyncClose(null);
                    return;
                }
            } else {
                asyncRead(CONTEXT_SSL_PROXY_CONNECT);
                return;
            }
            if (!"200".equals(responseHeader.getStatusCode())) {
                logger.warn("ssl proxy fail to connect.statusCode;" + responseHeader.getStatusCode());
                onResponseHeader(responseHeader);
                asyncClose(null);
                return;
            }
            responseHeader.recycle();
            stat = STAT_SSL_HANDSHAKE;
            sslOpen(true);
            return;
        }
        super.onRead(userContext, buffers);
    }

    public void onReadPlain(Object userContext, ByteBuffer[] buffers) {
        logger.debug("#readPlain.cid:" + getChannelId());
        if (userContext == CONTEXT_BODY) {
            stat = STAT_RESPONSE_BODY;
            boolean isLast;
            if (isReadableCallback) {
                buffers = responseChunk.decodeChunk(buffers);
                isLast = responseChunk.isEndOfData();
                if (isGzip) {
                    gzipContext.putZipedBuffer(buffers);
                    buffers = gzipContext.getPlainBuffer();
                }
            } else {
                isLast = responseChunk.isEndOfData(buffers);
            }
            onResponseBody(buffers);
            if (isLast) {
                endOfResponse();
            } else {
                logger.debug("asyncRead(CONTEXT_BODY) cid:" + getChannelId());
                asyncRead(CONTEXT_BODY);
            }
            return;
        }
        stat = STAT_RESPONSE_HEADER;
        for (int i = 0; i < buffers.length; i++) {
            responseHeader.parse(buffers[i]);
        }
        PoolManager.poolArrayInstance(buffers);
        if (!responseHeader.isParseEnd()) {
            logger.debug("asyncRead(CONTEXT_HEADER) cid:" + getChannelId());
            asyncRead(CONTEXT_HEADER);
            return;
        }
        if (responseHeader.isParseError()) {
            logger.warn("http header error");
            asyncClose(null);
            return;
        }
        responseHeaderLength = responseHeader.getHeaderLength();
        String statusCode = responseHeader.getStatusCode();
        String transfer = responseHeader.getHeader(HeaderParser.TRANSFER_ENCODING_HEADER);
        String encoding = responseHeader.getHeader(HeaderParser.CONTENT_ENCODING_HEADER);
        isGzip = false;
        if (HeaderParser.CONTENT_ENCODING_GZIP.equalsIgnoreCase(encoding)) {
            isGzip = true;
        }
        onResponseHeader(responseHeader);
        if ("304".equals(statusCode) || "204".equals(statusCode)) {
            endOfResponse();
            return;
        }
        long responseContentLength = responseHeader.getContentLength();
        if (responseContentLength < 0) {
            responseContentLength = Long.MAX_VALUE;
        }
        boolean isChunked = HeaderParser.TRANSFER_ENCODING_CHUNKED.equalsIgnoreCase(transfer);
        ByteBuffer[] body = responseHeader.getBodyBuffer();
        responseChunk.decodeInit(isChunked, responseContentLength);
        boolean isLast;
        if (isReadableCallback) {
            body = responseChunk.decodeChunk(body);
            isLast = responseChunk.isEndOfData();
            if (isGzip) {
                gzipContext.putZipedBuffer(body);
                body = gzipContext.getPlainBuffer();
            }
        } else {
            isLast = responseChunk.isEndOfData(body);
        }
        if (body != null) {
            stat = STAT_RESPONSE_BODY;
            onResponseBody(body);
        }
        if (isLast) {
            onResponseBody(null);
            endOfResponse();
        } else {
            logger.debug("asyncRead(CONTEXT_BODY) cid:" + getChannelId());
            asyncRead(CONTEXT_BODY);
        }
    }

    private void endOfResponse() {
        if (stat == STAT_END) {
            isKeepAlive = false;
        }
        if (isKeepAlive) {
            String connectionHeader = null;
            String httpVersion = responseHeader.getResHttpVersion();
            if (!webClientConnection.isHttps() && webClientConnection.isUseProxy()) {
                connectionHeader = responseHeader.getHeader(HeaderParser.PROXY_CONNECTION_HEADER);
            } else {
                connectionHeader = responseHeader.getHeader(HeaderParser.CONNECTION_HEADER);
            }
            if (HeaderParser.HTTP_VESION_10.equalsIgnoreCase(httpVersion)) {
                if (!HeaderParser.CONNECION_KEEP_ALIVE.equalsIgnoreCase(connectionHeader)) {
                    isKeepAlive = false;
                }
            }
            if (HeaderParser.HTTP_VESION_11.equalsIgnoreCase(httpVersion)) {
                if (HeaderParser.CONNECION_CLOSE.equalsIgnoreCase(connectionHeader)) {
                    isKeepAlive = false;
                }
            }
        }
        if (isKeepAlive == false) {
            asyncClose(null);
            return;
        }
        onRequestEnd(STAT_KEEP_ALIVE);
        if (requestHeaderBuffer != null) {
            PoolManager.poolBufferInstance(requestHeaderBuffer);
            requestHeaderBuffer = null;
        }
        if (requestBodyBuffer != null) {
            PoolManager.poolBufferInstance(requestBodyBuffer);
            requestBodyBuffer = null;
        }
        responseHeaderLength = requestHeaderLength = requestContentLength = requestContentWriteLength = 0;
        responseHeader.recycle();
        setReadTimeout(keepAliveTimeout);
        asyncRead(CONTEXT_HEADER);
        logger.debug("WebClientHandler keepAlive.cid:" + getChannelId());
    }

    public void onFailure(Object userContext, Throwable t) {
        logger.debug("#failure.cid:" + getChannelId(), t);
        isKeepAlive = false;
        asyncClose(userContext);
        onRequestFailure(stat, t);
        super.onFailure(userContext, t);
    }

    public void onTimeout(Object userContext) {
        logger.debug("#timeout.cid:" + getChannelId());
        asyncClose(userContext);
        if (isKeepAlive == false) {
            onRequestFailure(stat, FAILURE_TIMEOUT);
        }
        isKeepAlive = false;
        super.onTimeout(userContext);
    }

    public void onClosed(Object userContext) {
        logger.debug("#closed.cid:" + getChannelId());
        isKeepAlive = false;
        onRequestEnd(STAT_END);
        super.onClosed(userContext);
    }

    public void onFinished() {
        logger.debug("#finished.cid:" + getChannelId());
        isKeepAlive = false;
        onRequestEnd(STAT_END);
        super.onFinished();
    }

    public boolean isSameConnection(boolean isHttps, String targetServer, int targetPort) {
        if (stat != STAT_KEEP_ALIVE) {
            logger.debug("isSameConnection not keepAlive stat:" + stat);
            return false;
        }
        return webClientConnection.equalsConnection(isHttps, targetServer, targetPort);
    }

    /**
	 * Caller����͒��ڌĂяo�����
	 * error�����������ꍇ�Atimer�o�R�i�ʃX���b�h�j�ŃC�x���g�ɃG���[��ʒm����
	 * 
	 * @param webClient
	 * @param userContext
	 * @param connectTimeout
	 * @param requestHeaderBuffer
	 * @param requestContentLength
	 * @param isCallerkeepAlive
	 * @param keepAliveTimeout
	 * @return
	 */
    public final boolean startRequest(WebClient webClient, Object userContext, long connectTimeout, ByteBuffer[] requestHeaderBuffer, long requestContentLength, boolean isCallerkeepAlive, long keepAliveTimeout) {
        synchronized (this) {
            if (this.webClient != null) {
                throw new IllegalStateException("aleardy had webClient:" + this.webClient);
            }
            setWebClient(webClient);
            this.userContext = userContext;
        }
        this.gzipContext.recycle();
        this.isCallerKeepAlive = isCallerkeepAlive;
        this.keepAliveTimeout = keepAliveTimeout;
        this.isKeepAlive = isCallerKeepAlive;
        this.requestHeaderBuffer = requestHeaderBuffer;
        this.requestContentLength = requestContentLength;
        Throwable error;
        if (stat == STAT_KEEP_ALIVE) {
            setReadTimeout(config.getReadTimeout());
            internalStartRequest();
            return true;
        } else if (stat == STAT_INIT) {
            synchronized (this) {
                if (asyncConnect(this, webClientConnection.getRemoteServer(), webClientConnection.getRemotePort(), connectTimeout)) {
                    stat = STAT_CONNECT;
                    setReadTimeout(config.getReadTimeout());
                    return true;
                }
            }
            logger.warn("fail to asyncConnect.");
            error = FAILURE_CONNECT;
        } else {
            logger.error("fail to doRequest.cid=" + getChannelId() + ":stat:" + stat);
            error = new Throwable("fail to doRequest.cid=" + getChannelId() + ":stat:" + stat);
        }
        TimerManager.setTimeout(0L, this, error);
        return false;
    }

    /**
	 * ���N�G�X�g�I�[�́Amethod��content-length�Ŕ��f���� �I�[�F����A���X�|���X�ncallback�n���h���������Ăяo�����B
	 * ProxyHandler����Ăяo�����
	 * @param connectTimeout TODO
	 * @param isCallerkeepAlive �Ăяo������keepAlive����]���邩�ۂ�
	 * @param keepAliveTimeout TODO
	 * @param clientIp �Ăяo����ip�A�h���X�i�ݒ�Q�Ɨp)
	 * @param targetServer �ڑ���T�[�o
	 * @param targetPort �ڑ���|�[�g
	 * @param webClient�@�C�x���g�ʒm�C���^�t�F�[�X
	 * @param requestHeader�@���N�G�X�g�w�b�_(uri��web�T�[�o��ł��邱��'/'����n�܂邱�ƑO��)
	 * @return
	 */
    public final boolean startRequest(WebClient webClient, Object userContext, long connectTimeout, HeaderParser requestHeader, boolean isCallerkeepAlive, long keepAliveTimeout) {
        String requestLine = webClientConnection.getRequestLine(requestHeader);
        ByteBuffer[] requestHeaderBuffer = webClientConnection.getRequestHeaderBuffer(requestLine, requestHeader, isCallerkeepAlive);
        long requestContentLength = requestHeader.getContentLength();
        if (requestContentLength < 0) {
            requestContentLength = 0;
        }
        return startRequest(webClient, userContext, connectTimeout, requestHeaderBuffer, requestContentLength, isCallerkeepAlive, keepAliveTimeout);
    }

    public final void requestBody(ByteBuffer[] buffers) {
        synchronized (this) {
            requestBodyBuffer = BuffersUtil.concatenate(requestBodyBuffer, buffers);
            if (stat == STAT_CONNECT) {
                return;
            }
            stat = STAT_REQUEST_BODY;
        }
        long length = BuffersUtil.remaining(requestBodyBuffer);
        requestContentWriteLength += length;
        if (scheduler != null) {
            scheduler.scheduleWrite(CONTEXT_BODY, requestBodyBuffer);
        } else {
            if (bodyActualWriteTime <= 0) {
                bodyActualWriteTime = System.currentTimeMillis();
            }
            asyncWrite(CONTEXT_BODY, requestBodyBuffer);
        }
        requestBodyBuffer = null;
        if (requestContentWriteLength >= requestContentLength) {
            asyncRead(CONTEXT_HEADER);
        }
    }

    public final void cancelRequest() {
        webClient = null;
        asyncClose(null);
    }

    public boolean isKeepAlive() {
        return stat == STAT_KEEP_ALIVE | stat == STAT_INIT;
    }

    private void onWebConnected() {
        logger.debug("#webConnected cid:" + getChannelId());
        if (webClient != null) {
            webClient.onWebConnected(userContext);
        }
    }

    private void onWebProxyConnected() {
        logger.debug("#webProxyConnected cid:" + getChannelId());
        if (webClient != null) {
            webClient.onWebProxyConnected(userContext);
        }
    }

    private void onWebHandshaked() {
        logger.debug("#webHandshaked cid:" + getChannelId());
        if (webClient != null) {
            webClient.onWebHandshaked(userContext);
        }
    }

    private void onWrittenRequestHeader() {
        logger.debug("#writtenRequestHeader cid:" + getChannelId());
        if (webClient != null) {
            webClient.onWrittenRequestHeader(userContext);
        }
    }

    private void onWrittenRequestBody() {
        logger.debug("#writtenRequestBody cid:" + getChannelId());
        if (webClient != null) {
            webClient.onWrittenRequestBody(userContext);
        }
    }

    private void onResponseHeader(HeaderParser responseHeader) {
        logger.debug("#responseHeader cid:" + getChannelId());
        if (webClient != null) {
            webClient.onResponseHeader(userContext, responseHeader);
        }
    }

    private void onResponseBody(ByteBuffer[] buffer) {
        logger.debug("#responseBody cid:" + getChannelId());
        if (webClient != null) {
            webClient.onResponseBody(userContext, buffer);
        }
    }

    private synchronized void onRequestEnd(int stat) {
        logger.debug("#requestEnd cid:" + getChannelId() + ":webClient:" + webClient);
        int lastStat = this.stat;
        this.stat = stat;
        if (webClient == null) {
            return;
        }
        WebClient wkWebClient = webClient;
        Object wkUserContext = userContext;
        setWebClient(null);
        userContext = null;
        wkWebClient.onRequestEnd(wkUserContext, lastStat);
    }

    private void onRequestFailure(int stat, Throwable t) {
        logger.debug("#requestFailure cid:" + getChannelId());
        synchronized (this) {
            if (webClient == null) {
                return;
            }
            WebClient wkWebClient = webClient;
            Object wkUserContext = userContext;
            setWebClient(null);
            userContext = null;
            wkWebClient.onRequestFailure(wkUserContext, stat, t);
        }
        if (t == FAILURE_CONNECT) {
            logger.warn("#requestFailure.connect failure");
        } else {
            logger.warn("#requestFailure.", t);
        }
    }

    public long getRequestHeaderLength() {
        return requestHeaderLength;
    }

    public long getResponseHeaderLength() {
        return responseHeaderLength;
    }

    public boolean isConnect() {
        if (stat == STAT_INIT || stat == STAT_END) {
            return false;
        }
        return true;
    }

    public void setWebClientConnection(WebClientConnection webClientConnection) {
        logger.debug("#setWebClientConnection:" + webClientConnection);
        if (this.webClientConnection != null) {
            this.webClientConnection.unref();
        }
        if (webClientConnection != null) {
            webClientConnection.ref();
        }
        this.webClientConnection = webClientConnection;
    }

    public boolean unref() {
        if (stat == STAT_INIT) {
            logger.debug("stat INIT unref");
            stat = STAT_END;
            super.unref();
        }
        return super.unref();
    }

    public void setReadableCallback(boolean isReadableCallback) {
        this.isReadableCallback = isReadableCallback;
    }

    /**
	 * startRequest�̑O�ɐݒ肷�邱��
	 * @param sslProxyTime
	 * @param sslProxyLength
	 */
    public void setSslProxySchedule(long sslProxyTime, long sslProxyLength) {
        if (scheduler == null) {
            scheduler = CallScheduler.create(this);
        }
        scheduler.setSslProxySchedule(sslProxyTime, sslProxyLength);
    }

    /**
	 * startRequest�̑O�ɐݒ肷�邱��
	 * @param headerTime
	 * @param headerLength
	 */
    public void setHeaderSchedule(long headerTime, long headerLength) {
        if (scheduler == null) {
            scheduler = CallScheduler.create(this);
        }
        scheduler.setHeaderSchedule(headerTime, headerLength);
    }

    /**
	 * startRequest�̑O�ɐݒ肷�邱��
	 * @param bodyTime
	 * @param bodyLength
	 */
    public void setBodySchedule(long bodyTime, long bodyLength) {
        if (scheduler == null) {
            scheduler = CallScheduler.create(this);
        }
        scheduler.setBodySchedule(bodyTime, bodyLength);
    }

    public long getSslProxyActualWriteTime() {
        if (scheduler != null) {
            return scheduler.getSslProxyActualWriteTime();
        }
        return sslProxyActualWriteTime;
    }

    public long getHeaderActualWriteTime() {
        if (scheduler != null) {
            return scheduler.getHeaderActualWriteTime();
        }
        return headerActualWriteTime;
    }

    public long getBodyActualWriteTime() {
        if (scheduler != null) {
            return scheduler.getBodyActualWriteTime();
        }
        return bodyActualWriteTime;
    }

    public void onTimer(Object userContext) {
        onRequestFailure(stat, (Throwable) userContext);
    }
}
