package naru.aweb.handler;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import naru.async.pool.PoolManager;
import naru.async.store.Page;
import naru.aweb.config.AccessLog;
import naru.aweb.config.Config;
import naru.aweb.config.Mapping;
import naru.aweb.http.HeaderParser;
import naru.aweb.http.KeepAliveContext;
import naru.aweb.http.WebClient;
import naru.aweb.http.WebClientHandler;
import naru.aweb.http.WebServerHandler;
import naru.aweb.mapping.MappingResult;
import naru.aweb.util.ServerParser;
import org.apache.log4j.Logger;

/**
 * reversProxy�Ƃ��ē���
 * 1)reverse�p�X�ƈ�v�����ꍇ
 * 2)sslProxy��peek����ꍇ
 * @author Naru
 *
 */
public class ProxyHandler extends WebServerHandler implements WebClient {

    private static Logger logger = Logger.getLogger(ProxyHandler.class);

    private static Config config = Config.getConfig();

    private static String OPTION_INJECTOR = "injector";

    private static String OPTION_REPLAY = "replay";

    private static String OPTION_FILTER = "filter";

    private boolean isReplay = false;

    private boolean isTryAgain = false;

    private boolean isReplace = false;

    private ProxyInjector injector;

    private Page bodyPage = new Page();

    private WebClientHandler webClientHandler;

    private List<String> removeResponseHeaders = new ArrayList<String>();

    private Map<String, String> addResponseHeaders = new HashMap<String, String>();

    public void recycle() {
        isTryAgain = isReplay = isReplace = false;
        webClientHandler = null;
        bodyPage.recycle();
        if (injector != null) {
            injector.term();
            injector = null;
        }
        addResponseHeaders.clear();
        removeResponseHeaders.clear();
        super.recycle();
    }

    public void removeResponseHeader(String name) {
        removeResponseHeaders.add(name);
    }

    public void addResponseHeader(String name, String value) {
        addResponseHeaders.put(name, value);
    }

    private WebClientHandler getWebClientHandler(KeepAliveContext keepAliveContext, HeaderParser requestHeader) {
        MappingResult mapping = getRequestMapping();
        ServerParser targetHostServer = mapping.getResolveServer();
        String path = mapping.getResolvePath();
        requestHeader.setPath(path);
        return keepAliveContext.getWebClientHandler(mapping.isResolvedHttps(), targetHostServer.getHost(), targetHostServer.getPort());
    }

    private void calcResolveDigest(HeaderParser requestHeader) {
        MappingResult mapping = getRequestMapping();
        Mapping.LogType logType = mapping.getLogType();
        if (logType != Mapping.LogType.RESPONSE_TRACE && logType != Mapping.LogType.TRACE) {
            return;
        }
        ServerParser targetHostServer = mapping.getResolveServer();
        String path = mapping.getResolvePath();
        String resolveDigest = AccessLog.calcResolveDigest(requestHeader.getMethod(), mapping.isResolvedHttps(), targetHostServer.toString(), path, requestHeader.getQuery());
        AccessLog accessLog = getAccessLog();
        accessLog.setResolveDigest(resolveDigest);
    }

    private void editRequestHeader(HeaderParser requestHeader) {
        MappingResult mapping = getRequestMapping();
        Mapping.LogType logType = mapping.getLogType();
        if (logType == Mapping.LogType.RESPONSE_TRACE || logType == Mapping.LogType.TRACE) {
            requestHeader.removeHeader(HeaderParser.IF_MODIFIED_SINCE_HEADER);
            requestHeader.removeHeader(HeaderParser.IF_NONE_MATCH);
        }
    }

    private boolean doProxy() {
        logger.debug("#startResponse.cid:" + getChannelId());
        HeaderParser requestHeader = getRequestHeader();
        boolean isCallerKeepAlive = false;
        long keepAliveTimeout = 0;
        KeepAliveContext keepAliveContext = getKeepAliveContext();
        if (keepAliveContext != null) {
            isCallerKeepAlive = keepAliveContext.isKeepAlive();
            keepAliveTimeout = keepAliveContext.getKeepAliveTimeout();
        }
        calcResolveDigest(requestHeader);
        webClientHandler = getWebClientHandler(keepAliveContext, requestHeader);
        editRequestHeader(requestHeader);
        long connectTimeout = config.getConnectTimeout();
        boolean rc = webClientHandler.startRequest(this, null, connectTimeout, requestHeader, isCallerKeepAlive, keepAliveTimeout);
        if (rc == false) {
            completeResponse("500", "fail to request backserver");
            webClientHandler = null;
            return false;
        }
        logger.debug("client cid:" + getChannelId() + " server cid:" + webClientHandler.getChannelId());
        webClientHandler.setReadTimeout(getReadTimeout());
        webClientHandler.setWriteTimeout(getWriteTimeout());
        return true;
    }

    public void startResponse() {
        MappingResult mapping = getRequestMapping();
        if (Boolean.TRUE.equals(mapping.getOption(OPTION_FILTER))) {
            FilterHelper helper = config.getFilterHelper();
            if (helper.doFilter(this) == false) {
                logger.debug("filter blocked");
                return;
            }
        }
        InjectionHelper helper = config.getInjectionHelper();
        injector = helper.getInjector((String) mapping.getOption(OPTION_INJECTOR));
        if (injector != null) {
            injector.init(this);
            injector.onRequestHeader(getRequestHeader());
        }
        if (Boolean.TRUE.equals(mapping.getOption(OPTION_REPLAY))) {
            isReplay = true;
        } else {
            if (doProxy() == false) {
                return;
            }
        }
        startParseRequestBody();
    }

    public void requestBody(ByteBuffer[] buffers) {
        if (isReplay) {
            bodyPage.putBuffer(PoolManager.duplicateBuffers(buffers), true);
            super.requestBody(buffers);
            return;
        }
        webClientHandler.requestBody(buffers);
    }

    /**
	 * body�����ׂēǂݍ���ł���Ăяo�����,
	 * replay�̏ꍇ�A�������J�n����
	 * 
	 */
    public void startResponseReqBody() {
        if (!isReplay) {
            return;
        }
        ReplayHelper helper = config.getReplayHelper();
        ByteBuffer[] body = bodyPage.getBuffer();
        if (helper.doReplay(this, body)) {
            return;
        }
        if (doProxy() == false) {
            return;
        }
        if (body != null) {
            webClientHandler.requestBody(body);
        }
    }

    public void onTimeout(Object userContext) {
        logger.debug("#timeout.cid:" + getChannelId());
        asyncClose(userContext);
        responseEnd();
    }

    public void onFailure(Object userContext, Throwable t) {
        logger.debug("#failure.cid:" + getChannelId(), t);
        asyncClose(userContext);
        responseEnd();
    }

    public void onClosed(Object userContext) {
        logger.debug("#closed.cid:" + getChannelId());
        super.onClosed(userContext);
    }

    public void onFinished() {
        logger.debug("#finished.cid:" + getChannelId());
        responseEnd();
        super.onFinished();
    }

    public void onResponseBody(Object userContext, ByteBuffer[] buffer) {
        logger.debug("#responseBody.cid:" + getChannelId());
        if (isTryAgain || isReplace) {
            PoolManager.poolBufferInstance(buffer);
            return;
        }
        if (injector != null) {
            logger.debug("inject add contents cid:" + getChannelId());
            buffer = injector.onResponseBody(buffer);
        }
        responseBody(buffer);
    }

    private void rewriteLocation(HeaderParser responseHeader) {
        String location = responseHeader.getHeader(HeaderParser.LOCATION_HEADER);
        if (location == null) {
            return;
        }
        MappingResult mapping = getRequestMapping();
        location = mapping.reverseResolve(location);
        responseHeader.setHeader(HeaderParser.LOCATION_HEADER, location);
    }

    public void onResponseHeader(Object userContext, HeaderParser responseHeader) {
        logger.debug("#responseHeader.cid:" + getChannelId());
        long injectLength = 0;
        boolean isInject = false;
        if (injector != null) {
            injector.onResponseHeader(responseHeader);
            isInject = injector.isInject();
            if (isInject) {
                webClientHandler.setReadableCallback(true);
                injectLength = injector.getInjectLength();
                logger.debug("inject cid:" + getChannelId() + ":injectLength:" + injectLength + ":ContentsLength:" + responseHeader.getContentLength());
            }
        }
        String statusCode = responseHeader.getStatusCode();
        if ("301".equals(statusCode) || "302".equals(statusCode) || "303".equals(statusCode)) {
            rewriteLocation(responseHeader);
        }
        setResponseHeader(responseHeader);
        for (String removeHeader : removeResponseHeaders) {
            removeHeader(removeHeader);
        }
        for (String name : addResponseHeaders.keySet()) {
            String value = addResponseHeaders.get(name);
            setHeader(name, value);
        }
        if (isInject) {
            String contentEncoding = responseHeader.getHeader(HeaderParser.CONTENT_ENCODING_HEADER);
            long contentLength = 0;
            if (!isReplace) {
                contentLength = responseHeader.getContentLength();
            }
            if (contentEncoding != null) {
                removeHeader(HeaderParser.CONTENT_ENCODING_HEADER);
                removeContentLength();
            } else if (contentLength >= 0) {
                setContentLength(contentLength + injectLength);
                logger.debug("inject change contentLength cid:" + getChannelId() + ":contentLength:" + (contentLength + injectLength));
            }
            removeHeader(HeaderParser.TRANSFER_ENCODING_HEADER);
        }
    }

    public void onRequestEnd(Object userContext, int stat) {
        logger.debug("#webClientEnd.cid:" + getChannelId());
        if (isTryAgain) {
            isTryAgain = false;
            startResponse();
            return;
        }
        if (injector != null) {
            ByteBuffer[] buffers = injector.onResponseBody(null);
            if (buffers != null) {
                logger.debug("inject add contents last cid:" + getChannelId());
                responseBody(buffers);
            }
        }
        if (getResponseStatusCode() == null) {
            completeResponse("500", "no response");
        } else {
            responseEnd();
        }
    }

    public void onRequestFailure(Object userContext, int stat, Throwable t) {
        logger.debug("#webClientFailure.cid:" + getChannelId() + ":" + stat, t);
        String statusCode = getResponseStatusCode();
        if (statusCode != null) {
            responseEnd();
        } else {
            completeResponse("500", "proxyHandler error.stat:" + stat);
        }
    }

    public void onWrittenRequestHeader(Object userContext) {
    }

    public void onWrittenRequestBody(Object userContext) {
    }

    public void setTryAgain(boolean isTryAgain) {
        this.isTryAgain = isTryAgain;
    }

    public void setReplace(boolean isReplace) {
        this.isReplace = isReplace;
    }

    public void onWebConnected(Object userContext) {
    }

    public void onWebHandshaked(Object userContext) {
    }

    public void onWebProxyConnected(Object userContext) {
    }
}
