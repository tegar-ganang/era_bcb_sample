package naru.aweb.core;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;
import javax.net.ssl.SSLEngine;
import org.apache.log4j.Logger;
import naru.async.pool.BuffersUtil;
import naru.async.pool.PoolManager;
import naru.async.ssl.SslAdapter;
import naru.async.store.Page;
import naru.async.store.Store;
import naru.aweb.auth.AuthHandler;
import naru.aweb.auth.AuthSession;
import naru.aweb.auth.Authorizer;
import naru.aweb.auth.MappingAuth;
import naru.aweb.auth.SessionId;
import naru.aweb.config.AccessLog;
import naru.aweb.config.Config;
import naru.aweb.config.Mapping;
import naru.aweb.config.User;
import naru.aweb.http.HeaderParser;
import naru.aweb.http.KeepAliveContext;
import naru.aweb.http.RequestContext;
import naru.aweb.http.WebServerHandler;
import naru.aweb.mapping.Mapper;
import naru.aweb.mapping.MappingResult;
import naru.aweb.util.ServerParser;
import net.sf.json.JSONObject;

/**
 * DispatchHander�́A���N�G�X�g�w�b�_�̉�͂���ɍs��
 * WebServerHandler���p������ƁA���ꂪ���{�ł��Ȃ����߁AServerBaseHandler���p�����Ă���B
 * ���̂��Ƃ���ADispatchHander���璼�ڃ��X�|���X��ԋp���鎖�͂ł��Ȃ��B
 * 
 * @author naru
 * 
 */
public class DispatchHandler extends ServerBaseHandler {

    static Logger logger = Logger.getLogger(DispatchHandler.class);

    private static byte[] ProxyOkResponse = "HTTP/1.0 200 Connection established\r\n\r\n".getBytes();

    private static Config config = null;

    private static Mapper mapper = null;

    private static Authorizer authorizer = null;

    private static int limitRequestFieldSize = -1;

    private static Config getConfig() {
        if (config == null) {
            config = Config.getConfig();
        }
        return config;
    }

    private static Mapper getMapper() {
        if (mapper == null) {
            mapper = getConfig().getMapper();
        }
        return mapper;
    }

    private static Authorizer getAuthorizer() {
        if (authorizer == null) {
            authorizer = getConfig().getAuthorizer();
        }
        return authorizer;
    }

    private static int getLimitRequestFieldSize() {
        if (limitRequestFieldSize == -1) {
            limitRequestFieldSize = getConfig().getInt("limitRequestFieldSize", 8192);
        }
        return limitRequestFieldSize;
    }

    private Date startTime;

    private long handshakeTime = -1;

    private long connectTime = -1;

    private boolean isFirstRead;

    private long connectHeaderLength;

    private Page headerPage = new Page();

    public void recycle() {
        connectHeaderLength = 0;
        startTime = null;
        handshakeTime = connectTime = -1;
        headerPage.recycle();
        isFirstRead = false;
        super.recycle();
    }

    private long startTotalReadLength;

    private long startTotalWriteLength;

    public void onStartRequest() {
        logger.debug("#startRequest.cid:" + getChannelId());
        headerPage.recycle();
        startTotalReadLength = getTotalReadLength();
        startTotalWriteLength = getTotalWriteLength();
        asyncRead(null);
    }

    public void onAccepted(Object userContext) {
        setReadTimeout(getConfig().getAcceptTimeout());
        logger.debug("#accepted.cid:" + getChannelId());
        isFirstRead = true;
        getKeepAliveContext(true);
        startTime = new Date();
        onStartRequest();
    }

    public void onFinished() {
        logger.debug("#finished.cid:" + getChannelId());
        super.onFinished();
    }

    public void onTimeout(Object userContext) {
        logger.debug("#timeout.cid:" + getChannelId());
        asyncClose(null);
    }

    public void onFailure(Object userContext, Throwable t) {
        logger.debug("Dispatcher failure.poolId:" + getPoolId(), t);
        asyncClose(null);
    }

    /**
	 * ssl�m����A���f�[�^��v������B(return true)
	 */
    public boolean onHandshaked() {
        logger.debug("#handshaked.cid:" + getChannelId());
        handshakeTime = System.currentTimeMillis() - startTime.getTime();
        return true;
    }

    public void onRead(Object userContext, ByteBuffer[] buffers) {
        logger.debug("#onRead.cid:" + getChannelId() + ":buffers.hashCode:" + buffers.hashCode());
        if (isFirstRead) {
            if (startTime == null) {
                startTime = new Date();
            }
            connectTime = System.currentTimeMillis() - startTime.getTime();
            isFirstRead = false;
            setReadTimeout(getConfig().getReadTimeout());
            if (SslAdapter.isSsl(buffers[0])) {
                if (!sslOpenWithBuffer(false, buffers)) {
                    asyncClose(null);
                }
                return;
            }
        }
        super.onRead(userContext, buffers);
    }

    public void onReadPlain(Object userContext, ByteBuffer[] buffers) {
        logger.debug("#onReadPlain.cid:" + getChannelId() + ":buffers.hashCode:" + buffers.hashCode());
        if (startTime == null) {
            startTime = new Date();
        }
        headerPage.putBuffer(PoolManager.duplicateBuffers(buffers), true);
        HeaderParser headerParser = getRequestHeader();
        for (int i = 0; i < buffers.length; i++) {
            headerParser.parse(buffers[i]);
        }
        PoolManager.poolArrayInstance(buffers);
        if (headerParser.isParseEnd()) {
            if (headerParser.isParseError()) {
                logger.warn("http header error");
                asyncClose(null);
            } else {
                if (headerParser.isProxy() && getConfig().getRealHost(headerParser.getServer()) != null) {
                    headerParser.forceWebRequest();
                }
                mappingHandler();
            }
        } else {
            if (getLimitRequestFieldSize() <= headerPage.getBufferLength()) {
                logger.warn("too long header size." + headerPage.getBufferLength());
                asyncClose(null);
                return;
            }
            asyncRead(null);
        }
    }

    private AccessLog setupTraceLog(String realHostName, HeaderParser requestHeader, MappingResult mapping, User user, boolean isWs) {
        AccessLog accessLog = getAccessLog();
        accessLog.setStartTime(startTime);
        accessLog.setConnectTime(connectTime);
        accessLog.setHandshakeTime(handshakeTime);
        accessLog.setTimeCheckPint(AccessLog.TimePoint.requestHeader);
        accessLog.setIp(getRemoteIp());
        if (user != null) {
            accessLog.setUserId(user.getLoginId());
        }
        if (Boolean.TRUE.equals(mapping.getOption("skipPhlog"))) {
            accessLog.setSkipPhlog(true);
        }
        if (Boolean.TRUE.equals(mapping.getOption("shortFormatLog"))) {
            accessLog.setShortFormat(true);
        }
        Mapping.SecureType secureType = mapping.getTargetSecureType();
        if (secureType != null) {
            switch(mapping.getTargetSecureType()) {
                case PLAIN:
                    if (mapping.isSourceTypeProxy()) {
                        accessLog.setSourceType(AccessLog.SOURCE_TYPE_PLAIN_PROXY);
                    } else if (isWs) {
                        accessLog.setSourceType(AccessLog.SOURCE_TYPE_WS);
                    } else {
                        accessLog.setSourceType(AccessLog.SOURCE_TYPE_PLAIN_WEB);
                    }
                    break;
                case SSL:
                    if (mapping.isSourceTypeProxy()) {
                        accessLog.setSourceType(AccessLog.SOURCE_TYPE_SSL_PROXY);
                    } else if (isWs) {
                        accessLog.setSourceType(AccessLog.SOURCE_TYPE_WSS);
                    } else {
                        accessLog.setSourceType(AccessLog.SOURCE_TYPE_SSL_WEB);
                    }
                    break;
            }
        }
        accessLog.setRealHost(realHostName);
        Mapping.DestinationType destinationType = mapping.getDestinationType();
        if (destinationType != null) {
            String origin = null;
            switch(mapping.getDestinationType()) {
                case HTTP:
                    accessLog.setDestinationType(AccessLog.DESTINATION_TYPE_HTTP);
                    origin = mapping.getResolveServer().toString();
                    break;
                case HTTPS:
                    accessLog.setDestinationType(AccessLog.DESTINATION_TYPE_HTTPS);
                    origin = mapping.getResolveServer().toString();
                    break;
                case FILE:
                    accessLog.setDestinationType(AccessLog.DESTINATION_TYPE_FILE);
                    origin = mapping.getDestinationFile().getAbsolutePath();
                    break;
                case HANDLER:
                    accessLog.setDestinationType(AccessLog.DESTINATION_TYPE_HANDLER);
                    origin = mapping.getHandlerClass().getName();
                    break;
                case WS:
                    accessLog.setDestinationType(AccessLog.DESTINATION_TYPE_WS);
                    origin = mapping.getResolveServer().toString();
                    break;
                case WSS:
                    accessLog.setDestinationType(AccessLog.DESTINATION_TYPE_WSS);
                    origin = mapping.getResolveServer().toString();
                    break;
            }
            accessLog.setResolveOrigin(origin);
        }
        accessLog.setRequestLine(requestHeader.getRequestLine());
        accessLog.setRequestHeaderLength(connectHeaderLength + requestHeader.getHeaderLength());
        accessLog.setChannelId(getChannelId());
        accessLog.setLocalIp(getLocalIp());
        logger.debug("cid:" + getChannelId() + ":requestLine:" + accessLog.getRequestLine());
        switch(mapping.getLogType()) {
            case NONE:
                headerPage.recycle();
                accessLog.setPersist(false);
                return accessLog;
            case TRACE:
            case REQUEST_TRACE:
                Store readPeekStore = Store.open(true);
                ByteBuffer[] buffers = headerPage.getBuffer();
                BuffersUtil.cut(buffers, connectHeaderLength + requestHeader.getHeaderLength());
                readPeekStore.putBuffer(buffers);
                logger.debug("#setupTraceLog" + readPeekStore.getStoreId());
                accessLog.incTrace();
                readPeekStore.close(accessLog, readPeekStore);
                accessLog.setRequestHeaderDigest(readPeekStore.getDigest());
                if (isWs) {
                    break;
                }
                readPeekStore = Store.open(true);
                ByteBuffer[] bufs = requestHeader.peekBodyBuffer();
                if (bufs != null) {
                    readPeekStore.putBuffer(bufs);
                }
                pushReadPeekStore(readPeekStore);
                break;
        }
        accessLog.setPersist(true);
        return accessLog;
    }

    private void forwardMapping(String realHostName, HeaderParser requestHeader, MappingResult mapping, AuthSession auth, boolean isWs) {
        User user = null;
        if (auth != null) {
            user = auth.getUser();
        }
        setRequestAttribute(ServerBaseHandler.ATTRIBUTE_USER, user);
        setupTraceLog(realHostName, requestHeader, mapping, user, isWs);
        setRequestMapping(mapping);
        Class<WebServerHandler> responseClass = mapping.getHandlerClass();
        WebServerHandler response = (WebServerHandler) forwardHandler(responseClass);
        if (response == null) {
            logger.warn("fail to forwardHandler:cid:" + getChannelId() + ":" + this);
            return;
        }
        logger.debug("responseObject:cid:" + getChannelId() + ":" + response + ":" + this);
        response.startResponse();
    }

    private static final String SSL_PROXY_OK_CONTEXT = "sslProxyOkContext";

    private MappingResult sslProxyMapping(Mapper mapper, KeepAliveContext keepAliveContext, String realHost, ServerParser server) {
        ServerParser.resolveLocalhost(server, getRemoteIp());
        boolean isPeek = mapper.isPeekSslProxyServer(realHost, server);
        if (isPeek == false) {
            MappingResult mappingResult = mapper.resolveSslProxy(realHost, server);
            if (mappingResult != null) {
                return mappingResult;
            }
            return DispatchResponseHandler.forbidden();
        }
        keepAliveContext.setProxyTargetServer(server);
        HeaderParser requestHeader = getRequestHeader();
        connectHeaderLength = requestHeader.getHeaderLength();
        requestHeader.recycle();
        asyncWrite(SSL_PROXY_OK_CONTEXT, BuffersUtil.toByteBufferArray(ByteBuffer.wrap(ProxyOkResponse)));
        return null;
    }

    private MappingResult proxyMapping(Mapper mapper, String realHost, ServerParser server, String path) {
        ServerParser.resolveLocalhost(server, getRemoteIp());
        MappingResult mappingResult = mapper.resolveProxy(realHost, server, path);
        if (mappingResult != null) {
            return mappingResult;
        }
        return DispatchResponseHandler.forbidden();
    }

    private MappingResult wsMapping(Mapper mapper, KeepAliveContext keepAliveContext, String realHost, ServerParser server, String path) {
        ServerParser targetServer = keepAliveContext.getProxyTargetServer();
        boolean isProxy = (targetServer != null);
        return mapper.resolveWs(realHost, isSsl(), isProxy, server, path);
    }

    private MappingResult webMapping(Mapper mapper, KeepAliveContext keepAliveContext, String realHost, ServerParser server, String path) {
        ServerParser sslServer = keepAliveContext.getProxyTargetServer();
        if (getConfig().getRealHost(sslServer) == null && sslServer != null) {
            MappingResult mappingResult = mapper.resolvePeekSslProxy(realHost, sslServer, path);
            if (mappingResult != null) {
                return mappingResult;
            }
            return DispatchResponseHandler.forbidden();
        }
        MappingResult mapping = mapper.resolveWeb(realHost, isSsl(), server, path);
        return mapping;
    }

    private MappingResult sslProxyHandler(HeaderParser requestHeader, KeepAliveContext keepAliveContext) {
        ServerParser server = requestHeader.getServer();
        keepAliveContext.setProxyTargetServer(server);
        String realHost = keepAliveContext.getRealHost().getName();
        return sslProxyMapping(getMapper(), keepAliveContext, realHost, server);
    }

    private MappingResult proxyHandler(HeaderParser requestHeader, KeepAliveContext keepAliveContext, RequestContext requestContext) {
        ServerParser server = requestHeader.getServer();
        keepAliveContext.setProxyTargetServer(server);
        String realHost = keepAliveContext.getRealHost().getName();
        String path = requestHeader.getPath();
        MappingResult mapping = proxyMapping(getMapper(), realHost, server, path);
        mapping = checkPhAuth(requestHeader, keepAliveContext, requestContext, mapping);
        return mapping;
    }

    private MappingResult webHandler(HeaderParser requestHeader, KeepAliveContext keepAliveContext, RequestContext requestContext) {
        String realHost = keepAliveContext.getRealHost().getName();
        String path = requestHeader.getPath();
        ServerParser server = requestHeader.getServer();
        MappingResult mapping = webMapping(getMapper(), keepAliveContext, realHost, server, path);
        if (mapping == null) {
            mapping = DispatchResponseHandler.notfound("not found mapping");
            return mapping;
        }
        mapping = checkPhAuth(requestHeader, keepAliveContext, requestContext, mapping);
        return mapping;
    }

    private MappingResult wsHandler(HeaderParser requestHeader, KeepAliveContext keepAliveContext, RequestContext requestContext) {
        String realHost = keepAliveContext.getRealHost().getName();
        String path = requestHeader.getPath();
        ServerParser server = requestHeader.getServer();
        MappingResult mapping = wsMapping(getMapper(), keepAliveContext, realHost, server, path);
        if (mapping == null) {
            mapping = DispatchResponseHandler.notfound("not found mapping");
            return mapping;
        }
        mapping = checkPhAuth(requestHeader, keepAliveContext, requestContext, mapping);
        return mapping;
    }

    private enum AUTH_STAT {

        SUCCESS, FAIL, PUBLIC
    }

    private MappingResult authMarkResponse(String authMark, AUTH_STAT stat, AuthSession authSession) {
        JSONObject response = new JSONObject();
        response.element("authUrl", config.getAuthUrl());
        if (AuthHandler.AUTH_CD_SET.equals(authMark)) {
            response.element("result", false);
            response.element("reason", "seequence error");
        } else {
            switch(stat) {
                case SUCCESS:
                    response.element("result", true);
                    response.element("appId", authSession.getAppId());
                    break;
                case PUBLIC:
                    response.element("result", true);
                    response.element("appId", "public");
                    break;
                case FAIL:
                    response.element("result", false);
                    response.element("reason", "lack of right");
                    break;
            }
        }
        return DispatchResponseHandler.crossDomainFrame(response);
    }

    private MappingResult checkPhAuth(HeaderParser requestHeader, KeepAliveContext keepAliveContext, RequestContext requestContext, MappingResult mapping) {
        String authMark = (String) getRequestAttribute(AuthHandler.AUTH_MARK);
        String cookieId = (String) getRequestAttribute(SessionId.SESSION_ID);
        if (cookieId != null) {
            ServerParser domain = requestHeader.getServer();
            boolean isSsl = isSsl();
            domain.setupPortIfNeed(isSsl);
            Authorizer authorizer = getAuthorizer();
            AuthSession authSession = authorizer.getAuthSessionBySecondaryId(cookieId, mapping.getMapping(), isSsl, domain);
            if (authSession != null) {
                if (!authorizer.authorize(mapping.getMapping(), authSession)) {
                    authSession.unref();
                    mapping.unref();
                    if (authMark != null) {
                        return authMarkResponse(authMark, AUTH_STAT.FAIL, authSession);
                    }
                    mapping = DispatchResponseHandler.forbidden("lack of right");
                    return mapping;
                }
                requestContext.registerAuthSession(authSession);
                if (authMark != null) {
                    mapping.unref();
                    return authMarkResponse(authMark, AUTH_STAT.SUCCESS, authSession);
                }
                return mapping;
            }
        }
        List<String> mappingRoles = mapping.getRolesList();
        if (mappingRoles.size() == 0) {
            if (authMark != null) {
                return authMarkResponse(authMark, AUTH_STAT.PUBLIC, null);
            }
            return mapping;
        }
        if (authMark == null) {
            authMark = AuthHandler.AUTHORIZE_MARK;
        }
        setRequestAttribute(AuthHandler.AUTHORIZE_MARK, authMark);
        mapping.forwardAuth();
        return mapping;
    }

    private MappingResult checkMappingAuth(HeaderParser requestHeader, KeepAliveContext keepAliveContext, RequestContext requestContext, MappingResult mapping) {
        if (requestContext.getAuthSession() != null) {
            return mapping;
        }
        MappingAuth mappingAuth = mapping.getMappingAuth();
        if (mappingAuth == null) {
            return mapping;
        }
        AuthSession authSession = mappingAuth.authorize(requestHeader);
        if (authSession != null) {
            authSession.ref();
            requestContext.registerAuthSession(authSession);
            return mapping;
        }
        mapping.unref();
        mapping = DispatchResponseHandler.authenticate(mappingAuth.isProxy(), mappingAuth.createAuthenticateHeader());
        return mapping;
    }

    private void mappingHandler() {
        HeaderParser requestHeader = getRequestHeader();
        KeepAliveContext keepAliveContext = getKeepAliveContext();
        RequestContext requestContext = getRequestContext();
        boolean isSslProxy = requestHeader.isSslProxy();
        boolean isProxy = requestHeader.isProxy();
        boolean isWs = requestHeader.isWs();
        String cookieId = requestHeader.getAndRemoveCookieHeader(SessionId.SESSION_ID);
        if (cookieId != null) {
            setRequestAttribute(SessionId.SESSION_ID, cookieId);
        }
        String query = requestHeader.getQuery();
        if (isWs == false && query != null) {
            if (query.startsWith(AuthHandler.QUERY_CD_CHECK)) {
                setRequestAttribute(AuthHandler.AUTH_MARK, AuthHandler.AUTH_CD_CHECK);
            } else if (query.startsWith(AuthHandler.QUERY_CD_WS_CHECK)) {
                setRequestAttribute(AuthHandler.AUTH_MARK, AuthHandler.AUTH_CD_WS_CHECK);
                isWs = true;
            } else if (query.startsWith(AuthHandler.QUERY_CD_SET)) {
                setRequestAttribute(AuthHandler.AUTH_MARK, AuthHandler.AUTH_CD_SET);
            } else if (query.startsWith(AuthHandler.QUERY_CD_WS_SET)) {
                setRequestAttribute(AuthHandler.AUTH_MARK, AuthHandler.AUTH_CD_WS_SET);
                isWs = true;
            }
        }
        MappingResult mapping = null;
        if (isSslProxy) {
            mapping = sslProxyHandler(requestHeader, keepAliveContext);
            if (mapping == null) {
                return;
            }
        } else if (isProxy) {
            mapping = proxyHandler(requestHeader, keepAliveContext, requestContext);
        } else if (isWs) {
            mapping = wsHandler(requestHeader, keepAliveContext, requestContext);
        } else {
            if (keepAliveContext.isSslProxy()) {
                mapping = webHandler(requestHeader, keepAliveContext, requestContext);
            } else {
                mapping = webHandler(requestHeader, keepAliveContext, requestContext);
            }
        }
        if (!isWs) {
            mapping = checkMappingAuth(requestHeader, keepAliveContext, requestContext, mapping);
        }
        if (requestContext.getAuthSession() == null) {
            AuthSession.UNAUTH_SESSION.ref();
            requestContext.registerAuthSession(AuthSession.UNAUTH_SESSION);
        }
        keepAliveContext.startRequest(requestHeader);
        String realHost = keepAliveContext.getRealHost().getName();
        AuthSession auth = requestContext.getAuthSession();
        keepAliveContext.getRequestContext().allocAccessLog();
        forwardMapping(realHost, requestHeader, mapping, auth, isWs);
    }

    public SSLEngine getSSLEngine() {
        KeepAliveContext keepAliveContext = getKeepAliveContext();
        ServerParser sslServer = keepAliveContext.getProxyTargetServer();
        return getConfig().getSslEngine(sslServer);
    }

    /**
	 * proxy�ւ�OK���N�G�X�g�𑗐M�����ꍇ�A���M���m�F����sslOpen���Ă� �iSSL�̑w��context��counter�̂���)
	 */
    @Override
    public void onWrittenPlain(Object userContext) {
        logger.debug("#WrittenPlain cid:" + getChannelId());
        if (userContext == SSL_PROXY_OK_CONTEXT) {
            isFirstRead = true;
            asyncRead(null);
        }
    }
}
