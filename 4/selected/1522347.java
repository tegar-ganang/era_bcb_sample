package naru.aweb.admin;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import naru.async.cache.FileCache;
import naru.async.core.ChannelContext;
import naru.async.pool.BuffersUtil;
import naru.async.pool.PoolManager;
import naru.async.store.DataUtil;
import naru.async.store.Store;
import naru.async.store.StoreManager;
import naru.aweb.auth.Authenticator;
import naru.aweb.auth.Authorizer;
import naru.aweb.auth.LogoutEvent;
import naru.aweb.config.AccessLog;
import naru.aweb.config.Config;
import naru.aweb.config.Mapping;
import naru.aweb.core.Main;
import naru.aweb.handler.ws.WsProtocol;
import naru.aweb.http.HeaderParser;
import naru.aweb.http.ParameterParser;
import naru.aweb.http.RequestContext;
import naru.aweb.http.WebServerHandler;
import naru.aweb.mapping.MappingResult;
import naru.aweb.queue.QueueManager;
import naru.aweb.robot.Browser;
import naru.aweb.util.ServerParser;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.log4j.Logger;

public class AdminHandler extends WebServerHandler {

    private static Logger logger = Logger.getLogger(AdminHandler.class);

    private static Config config = Config.getConfig();

    private File replayRootDir = new File(config.getString("path.replayDocroot"));

    private void replayDelete() {
        Iterator<File> itr = replayUploadFiles.iterator();
        while (itr.hasNext()) {
            File uploadFile = itr.next();
            uploadFile.delete();
            itr.remove();
        }
    }

    private static List<File> replayUploadFiles = new ArrayList<File>();

    private static final String REPLAY_UPLOAD_PATH = "/replayUpload";

    private void replayUpload(ParameterParser parameter) {
        String path = parameter.getParameter("replaypath");
        path = path.replace("://", "/");
        DiskFileItem fileItem = parameter.getItem("replayFile");
        File file = fileItem.getStoreLocation();
        File target = new File(replayRootDir, path);
        String targetCanonPath;
        String rootCanonPath;
        try {
            targetCanonPath = target.getCanonicalPath();
            rootCanonPath = replayRootDir.getCanonicalPath();
        } catch (IOException e) {
            logger.warn("fail to getCanonicalPath.", e);
            completeResponse("404", "getCanonicalPath error:");
            return;
        }
        if (!targetCanonPath.startsWith(rootCanonPath)) {
            logger.warn("traversal error. target:" + targetCanonPath + " root:" + rootCanonPath);
            completeResponse("404", "traversal error:" + targetCanonPath);
            return;
        }
        File parent = target.getParentFile();
        if (!parent.exists()) {
            parent.mkdirs();
        }
        if (replayUploadFiles.contains(target)) {
            target.delete();
        }
        file.renameTo(target);
        replayUploadFiles.add(target);
        redirectAdmin();
    }

    private byte[] bytes(String text, String encode) throws UnsupportedEncodingException {
        if (encode == null || "".equals(encode)) {
            encode = "utf8";
        }
        return text.getBytes(encode);
    }

    private static final String HEX_0A = new String(new byte[] { (byte) 0x0a });

    private static final String HEX_0D0A = new String(new byte[] { (byte) 0x0d, (byte) 0x0a });

    private static final byte[] HEADER_END_BYTE = new byte[] { (byte) 0x0d, (byte) 0x0a, (byte) 0x0d, (byte) 0x0a };

    private HeaderParser createHeader(ByteBuffer[] buffers) {
        if (buffers == null) {
            return null;
        }
        HeaderParser header = (HeaderParser) PoolManager.getInstance(HeaderParser.class);
        for (int i = 0; i < buffers.length; i++) {
            header.parse(buffers[i]);
        }
        PoolManager.poolArrayInstance(buffers);
        if (!header.isParseEnd()) {
            header.parse(ByteBuffer.wrap(HEADER_END_BYTE));
        }
        return header;
    }

    private HeaderParser getPartHeader(ParameterParser parameter, String part, String digest) throws UnsupportedEncodingException {
        String text = parameter.getParameter(part);
        String encode = parameter.getParameter(part + "Encode");
        if (text != null) {
            text = text.replaceAll(HEX_0A, HEX_0D0A);
        }
        ByteBuffer[] buffers;
        if (text == null) {
            if (digest == null) {
                return null;
            }
            buffers = DataUtil.toByteBuffers(digest);
            ;
        } else {
            byte[] data = null;
            data = bytes(text, encode);
            buffers = BuffersUtil.toByteBufferArray(ByteBuffer.wrap(data));
        }
        return createHeader(buffers);
    }

    private String digest(ByteBuffer[] data) {
        Store store = Store.open(true);
        store.putBuffer(data);
        store.close();
        return store.getDigest();
    }

    private ByteBuffer[] getPartBuffer(ParameterParser parameter, String part, String digest) throws UnsupportedEncodingException {
        String body = parameter.getParameter(part);
        String bodyEncode = parameter.getParameter(part + "Encode");
        if (body == null) {
            if (digest == null) {
                return null;
            }
            return DataUtil.toByteBuffers(digest);
        }
        byte[] data = bytes(body, bodyEncode);
        return BuffersUtil.toByteBufferArray(ByteBuffer.wrap(data));
    }

    private void updateResolveDigest(AccessLog accessLog, HeaderParser requestHeader) {
        if (accessLog.getResponseBodyDigest() == null || accessLog.getResponseHeaderDigest() == null) {
            return;
        }
        boolean isHttps = (accessLog.getDestinationType() == AccessLog.DESTINATION_TYPE_HTTPS);
        String resolveDigest = AccessLog.calcResolveDigest(requestHeader.getMethod(), isHttps, accessLog.getResolveOrigin(), requestHeader.getPath(), requestHeader.getQuery());
        accessLog.setResolveDigest(resolveDigest);
    }

    private void storeRef(String digest) {
        StoreManager.ref(digest);
    }

    private AccessLog getEditedAccessLog(ParameterParser parameter) throws UnsupportedEncodingException {
        String accessLogId = parameter.getParameter("accessLogId");
        String requestHeader = parameter.getParameter("requestHeader");
        String requestBody = parameter.getParameter("requestBody");
        String responseHeader = parameter.getParameter("responseHeader");
        String responseBody = parameter.getParameter("responseBody");
        AccessLog accessLog = AccessLog.getById(Long.parseLong(accessLogId));
        accessLog.setSourceType(AccessLog.SOURCE_TYPE_EDIT);
        HeaderParser requestHeaderParser = null;
        ByteBuffer[] requestBodyBuffer = null;
        if (requestBody != null) {
            requestHeaderParser = getPartHeader(parameter, "requestHeader", accessLog.getRequestHeaderDigest());
            requestBodyBuffer = getPartBuffer(parameter, "requestBody", accessLog.getRequestBodyDigest());
            requestHeaderParser.removeHeader(HeaderParser.TRANSFER_ENCODING_HEADER);
            requestHeaderParser.setContentLength(BuffersUtil.remaining(requestBodyBuffer));
        } else if (requestHeader != null) {
            requestHeaderParser = getPartHeader(parameter, "requestHeader", accessLog.getRequestHeaderDigest());
        }
        if (requestHeaderParser != null) {
            requestHeaderParser.setHost(accessLog.getResolveOrigin());
            updateResolveDigest(accessLog, requestHeaderParser);
            accessLog.setRequestLine(requestHeaderParser.getRequestLine());
            String requestHeaderDigest = digest(requestHeaderParser.getHeaderBuffer());
            accessLog.setRequestHeaderDigest(requestHeaderDigest);
        } else {
            storeRef(accessLog.getRequestHeaderDigest());
        }
        if (requestBodyBuffer != null) {
            String requestBodyDigest = digest(requestBodyBuffer);
            accessLog.setRequestBodyDigest(requestBodyDigest);
        } else {
            storeRef(accessLog.getRequestBodyDigest());
        }
        HeaderParser responseHeaderParser = null;
        ByteBuffer[] responseBodyBuffer = null;
        if (responseBody != null) {
            responseHeaderParser = getPartHeader(parameter, "responseHeader", accessLog.getResponseHeaderDigest());
            responseBodyBuffer = getPartBuffer(parameter, "responseBody", accessLog.getResponseBodyDigest());
            accessLog.setTransferEncoding(null);
            accessLog.setContentEncoding(null);
            responseHeaderParser.removeHeader(HeaderParser.TRANSFER_ENCODING_HEADER);
            responseHeaderParser.removeHeader(HeaderParser.CONTENT_ENCODING_HEADER);
            long contentLength = BuffersUtil.remaining(responseBodyBuffer);
            responseHeaderParser.setContentLength(contentLength);
            accessLog.setResponseLength(contentLength);
        } else if (responseHeader != null) {
            responseHeaderParser = getPartHeader(parameter, "responseHeader", accessLog.getResponseHeaderDigest());
        }
        if (responseHeaderParser != null) {
            accessLog.setTransferEncoding(responseHeaderParser.getHeader(HeaderParser.TRANSFER_ENCODING_HEADER));
            accessLog.setContentEncoding(responseHeaderParser.getHeader(HeaderParser.CONTENT_ENCODING_HEADER));
            accessLog.setContentType(responseHeaderParser.getContentType());
            String responseHeaderDigest = digest(responseHeaderParser.getHeaderBuffer());
            accessLog.setResponseHeaderDigest(responseHeaderDigest);
            responseHeaderParser.unref(true);
        } else {
            storeRef(accessLog.getResponseHeaderDigest());
        }
        if (responseBodyBuffer != null) {
            String responseBodyDigest = digest(responseBodyBuffer);
            accessLog.setResponseBodyDigest(responseBodyDigest);
        } else {
            storeRef(accessLog.getResponseBodyDigest());
        }
        return accessLog;
    }

    private void viewAccessLog(ParameterParser parameter) {
        String accessLogId = parameter.getParameter("accessLogId");
        AccessLog accessLog = AccessLog.getById(Long.parseLong(accessLogId));
        if (accessLog == null) {
            completeResponse("404", "not found accessLog:" + accessLogId);
            return;
        }
        String responsHeaderDigest = accessLog.getResponseHeaderDigest();
        if (responsHeaderDigest == null) {
            completeResponse("404", "not found response header data.");
            return;
        }
        ByteBuffer[] headerBuffer = DataUtil.toByteBuffers(responsHeaderDigest);
        if (headerBuffer == null) {
            completeResponse("404", "not found response header data.digest:" + responsHeaderDigest);
            return;
        }
        if (!parseResponseHeader(headerBuffer)) {
            parseResponseHeader(BuffersUtil.toByteBufferArray(ByteBuffer.wrap(HEADER_END_BYTE)));
        }
        String responsBodyDigest = accessLog.getResponseBodyDigest();
        if (responsBodyDigest == null) {
            responseEnd();
            return;
        }
        Store body = Store.open(responsBodyDigest);
        if (body == null) {
            responseEnd();
            return;
        }
        setAttribute("Store", body);
        forwardHandler(Mapping.STORE_HANDLER);
    }

    private String runAccessLog(ParameterParser parameter) throws UnsupportedEncodingException {
        String accessLogId = parameter.getParameter("accessLogId");
        String requestBody = parameter.getParameter("requestBody");
        AccessLog accessLog = AccessLog.getById(Long.parseLong(accessLogId));
        if (accessLog.getDestinationType() != AccessLog.DESTINATION_TYPE_HTTP && accessLog.getDestinationType() != AccessLog.DESTINATION_TYPE_HTTPS) {
            return null;
        }
        HeaderParser requestHeaderParser = getPartHeader(parameter, "requestHeader", accessLog.getRequestHeaderDigest());
        if (requestHeaderParser == null) {
            return null;
        }
        if (HeaderParser.CONNECT_METHOD.equalsIgnoreCase(requestHeaderParser.getMethod())) {
            ByteBuffer[] realHeader = requestHeaderParser.getBodyBuffer();
            requestHeaderParser.recycle();
            for (ByteBuffer buffer : realHeader) {
                requestHeaderParser.parse(buffer);
            }
            PoolManager.poolArrayInstance(realHeader);
            if (!requestHeaderParser.isParseEnd()) {
                return null;
            }
        }
        ByteBuffer[] requestBodyBuffer = getPartBuffer(parameter, "requestBody", accessLog.getRequestBodyDigest());
        if (requestBody != null) {
            requestHeaderParser.removeHeader(HeaderParser.TRANSFER_ENCODING_HEADER);
            requestHeaderParser.setContentLength(BuffersUtil.remaining(requestBodyBuffer));
        }
        requestHeaderParser.setHost(accessLog.getResolveOrigin());
        boolean isHttps = (accessLog.getDestinationType() == AccessLog.DESTINATION_TYPE_HTTPS);
        Browser browser = Browser.create("run:" + accessLogId, isHttps, requestHeaderParser, requestBodyBuffer);
        QueueManager queueManager = QueueManager.getInstance();
        String chId = queueManager.createQueue();
        browser.start(chId);
        return chId;
    }

    private void doCommand(ParameterParser parameter) {
        String cmd = parameter.getParameter("command");
        String callback = parameter.getParameter("callback");
        if ("setTimeouts".equals(cmd)) {
            String connectTimeout = parameter.getParameter("connectTimeout");
            String acceptTimeout = parameter.getParameter("acceptTimeout");
            String readTimeout = parameter.getParameter("readTimeout");
            String writeTimeout = parameter.getParameter("writeTimeout");
            config.setConnectTimeout(Long.parseLong(connectTimeout));
            config.setAcceptTimeout(Long.parseLong(acceptTimeout));
            config.setReadTimeout(Long.parseLong(readTimeout));
            config.setWriteTimeout(Long.parseLong(writeTimeout));
            responseJson(true);
        } else if ("setProxy".equals(cmd)) {
            String pacUrl = parameter.getParameter("pacUrl");
            if ("".equals(pacUrl)) {
                pacUrl = null;
            }
            String proxyServer = parameter.getParameter("proxyServer");
            if ("".equals(proxyServer)) {
                proxyServer = null;
            }
            String sslProxyServer = parameter.getParameter("sslProxyServer");
            if ("".equals(sslProxyServer)) {
                sslProxyServer = null;
            }
            String exceptProxyDomains = parameter.getParameter("exceptProxyDomains");
            if ("".equals(exceptProxyDomains)) {
                exceptProxyDomains = null;
            }
            boolean result = config.updateProxyFinder(pacUrl, proxyServer, sslProxyServer, exceptProxyDomains);
            JSONObject json = new JSONObject();
            json.put("pacUrl", config.getProperty("pacUrl"));
            json.put("proxyServer", config.getProperty("proxyServer"));
            json.put("sslProxyServer", config.getProperty("sslProxyServer"));
            json.put("exceptProxyDomains", config.getProperty("exceptProxyDomains"));
            json.put("result", result);
            responseJson(json);
        } else if ("setKeepAlive".equals(cmd)) {
            String isWebKeepAlive = parameter.getParameter("isWebKeepAlive");
            String isProxyKeepAlive = parameter.getParameter("isProxyKeepAlive");
            String maxKeepAliveRequests = parameter.getParameter("maxKeepAliveRequests");
            String keepAliveTimeout = parameter.getParameter("keepAliveTimeout");
            String contentEncoding = parameter.getParameter("contentEncoding");
            String allowChunked = parameter.getParameter("allowChunked");
            config.setWebKeepAlive("true".equalsIgnoreCase(isWebKeepAlive));
            config.setProxyKeepAlive("true".equalsIgnoreCase(isProxyKeepAlive));
            config.setMaxKeepAliveRequests(Integer.parseInt(maxKeepAliveRequests));
            config.setKeepAliveTimeout(Integer.parseInt(keepAliveTimeout));
            config.setContentEncoding(contentEncoding);
            config.setAllowChaned("true".equalsIgnoreCase(allowChunked));
            responseJson(true);
        } else if ("setHtml5".equals(cmd)) {
            String websocketSpecs = parameter.getParameter("websocketSpecs");
            String webSocketMessageLimit = parameter.getParameter("webSocketMessageLimit");
            String webSocketPingInterval = parameter.getParameter("webSocketPingInterval");
            String isUseSessionStorage = parameter.getParameter("isUseSessionStorage");
            String isUseCrossDomain = parameter.getParameter("isUseCrossDomain");
            WsProtocol.setWebSocketSpecs(websocketSpecs);
            WsProtocol.setWebSocketMessageLimit(Integer.parseInt(webSocketMessageLimit));
            WsProtocol.setWebSocketPingInterval(Integer.parseInt(webSocketPingInterval));
            config.setProperty("isUseSessionStorage", isUseSessionStorage);
            config.setProperty("isUseCrossDomain", isUseCrossDomain);
            responseJson(true);
        } else if ("setFileCache".equals(cmd)) {
            String isChache = parameter.getParameter("isChache");
            String isFlush = parameter.getParameter("isFlush");
            FileCache fileCache = FileCache.getInstance();
            fileCache.setUseCache("true".equalsIgnoreCase(isChache));
            fileCache.setFlushCache("true".equalsIgnoreCase(isFlush));
            responseJson(true);
        } else if ("setAuth".equals(cmd)) {
            String scheme = parameter.getParameter("scheme");
            Authenticator authenticator = config.getAuthenticator();
            authenticator.setScheme(scheme);
            String sessionTimeout = parameter.getParameter("sessionTimeout");
            Authorizer authorizer = config.getAuthorizer();
            authorizer.setSessionTimeout(Long.parseLong(sessionTimeout));
            responseJson(true);
        } else if ("setBroadcaster".equals(cmd)) {
            String interval = parameter.getParameter("interval");
            config.setProperty("broardcastInterval", interval);
            responseJson(true);
        } else if ("setSelfDomain".equals(cmd)) {
            String domain = parameter.getParameter("domain");
            config.setProperty(Config.SELF_DOMAIN, domain);
            String port = config.getString(Config.SELF_PORT);
            config.setProperty(Config.SELF_URL, "http://" + domain + ":" + port);
            responseJson(true);
        } else if ("getStastics".equals(cmd)) {
            responseJson(JSONObject.fromObject(config.getStasticsObject()));
        } else if ("debugTrace".equals(cmd)) {
            String debugTrace = parameter.getParameter("debugTrace");
            config.setDebugTrace("true".equalsIgnoreCase(debugTrace));
            responseJson(config.isDebugTrace());
        } else if ("runAccessLog".equals(cmd)) {
            try {
                String chId = runAccessLog(parameter);
                responseJson(chId, callback);
            } catch (Exception e) {
                logger.error("runAccessLog error.", e);
                completeResponse("500");
            }
        } else if ("saveAccessLog".equals(cmd)) {
            try {
                AccessLog accessLog = getEditedAccessLog(parameter);
                accessLog.setOriginalLogId(accessLog.getId());
                accessLog.setId(null);
                accessLog.setPersist(true);
                accessLog.persist();
                responseJson(accessLog.getId(), callback);
            } catch (Exception e) {
                logger.error("getEditedAccessLog error.", e);
                completeResponse("500");
            }
        } else if ("phlog".equals(cmd)) {
            String phlogNumber = parameter.getParameter("phlogNumber");
            String logName;
            if ("0".equals(phlogNumber)) {
                logName = "ph.log";
            } else {
                logName = "ph.log." + phlogNumber;
            }
            setRequestAttribute(ATTRIBUTE_RESPONSE_CONTENT_DISPOSITION, "attachment; filename=\"" + logName + "\"");
            setRequestAttribute(ATTRIBUTE_RESPONSE_FILE, new File(config.getPhantomHome(), "/log/" + logName));
            setRequestAttribute(ATTRIBUTE_RESPONSE_FILE_NOT_USE_CACHE, ATTRIBUTE_RESPONSE_FILE_NOT_USE_CACHE);
            forwardHandler(Mapping.FILE_SYSTEM_HANDLER);
        } else if ("checkPool".equals(cmd)) {
            System.gc();
            ChannelContext.dumpChannelContexts();
            PoolManager.dump();
            completeResponse("205");
        } else if ("terminate".equals(cmd)) {
            String isRestartValue = parameter.getParameter("isRestart");
            String isCleanupValue = parameter.getParameter("isCleanup");
            String javaHeapSizeValue = parameter.getParameter("javaHeapSize");
            boolean isRestart = "true".equals(isRestartValue);
            boolean isCleanup = "true".equals(isCleanupValue);
            int javaHeapSize;
            if (javaHeapSizeValue == null) {
                javaHeapSize = 0;
            } else {
                javaHeapSize = Integer.parseInt(javaHeapSizeValue);
            }
            LogoutEvent logoutEvent = new LogoutEvent() {

                boolean isRestart;

                boolean isCleanup;

                int javaHeapSize;

                public LogoutEvent init(boolean isRestart, boolean isCleanup, int javaHeapSize) {
                    this.isRestart = isRestart;
                    this.isCleanup = isCleanup;
                    this.javaHeapSize = javaHeapSize;
                    return this;
                }

                @Override
                public void onLogout() {
                    if (!isRestart) {
                        Main.terminate();
                    } else {
                        Main.terminate(true, isCleanup, javaHeapSize);
                    }
                }
            }.init(isRestart, isCleanup, javaHeapSize);
            boolean addEvent = getRequestContext().getAuthSession().addLogoutEvent(logoutEvent);
            responseJson(addEvent);
        } else if ("replayDelete".equals(cmd)) {
            replayDelete();
            completeResponse("205");
        } else if ("dumpStore".equals(cmd)) {
            try {
                StoreManager.dumpStore();
                completeResponse("205");
            } catch (IOException e) {
                logger.warn("fail to dumpStore", e);
                completeResponse("500");
            }
        } else if ("forceDown".equals(cmd)) {
        } else if ("outOfMemory".equals(cmd)) {
        }
    }

    private void redirectAdmin() {
        HeaderParser requestHeader = getRequestHeader();
        ServerParser selfServer = requestHeader.getServer();
        StringBuilder sb = new StringBuilder();
        if (isSsl()) {
            sb.append("https://");
        } else {
            sb.append("http://");
        }
        sb.append(selfServer.toString());
        sb.append("/admin");
        sb.append("/admin.vsp");
        setHeader(HeaderParser.LOCATION_HEADER, sb.toString());
        completeResponse("302");
        return;
    }

    private boolean checkToken(ParameterParser parameter) {
        RequestContext requestContext = getRequestContext();
        String token = requestContext.getAuthSession().getToken();
        String paramToken = parameter.getParameter("token");
        if (paramToken != null) {
            if (token.equals(paramToken)) {
                return true;
            }
            return false;
        }
        JSON json = parameter.getJsonObject();
        if (json instanceof JSONObject) {
            JSONObject jsonObj = (JSONObject) json;
            String jsonToken = jsonObj.getString("token");
            if (token.equals(jsonToken)) {
                return true;
            }
        }
        return false;
    }

    public void startResponseReqBody() {
        try {
            doAdmin();
        } catch (RuntimeException e) {
            logger.warn("fail to doAdmin.", e);
            completeResponse("500");
        }
    }

    public void doAdmin() {
        HeaderParser requestHeader = getRequestHeader();
        MappingResult mapping = getRequestMapping();
        String selfPath = requestHeader.getRequestUri();
        String path = mapping.getResolvePath();
        if ("/".equals(path)) {
            if (!selfPath.endsWith("/")) {
                redirectAdmin();
                return;
            }
            path = "/admin.vsp";
            mapping.setResolvePath(path);
        }
        ParameterParser parameter = getParameterParser();
        if (path.endsWith(".vsp") || path.endsWith(".vsf")) {
            mapping.setDesitinationFile(config.getAdminDocumentRoot());
            forwardHandler(Mapping.VELOCITY_PAGE_HANDLER);
            return;
        } else if (path.startsWith("/storeDownload")) {
            forwardHandler(Mapping.STORE_HANDLER);
            return;
        } else if (path.startsWith("/viewAccessLog")) {
            viewAccessLog(parameter);
            return;
        }
        if (!checkToken(parameter)) {
            if (requestHeader.getMethod().equalsIgnoreCase(HeaderParser.POST_METHOD)) {
                logger.error("CSRF check error.path:" + path + ":cid:" + getChannelId());
                completeResponse("403", "token error.");
            } else {
                mapping.setOption(MappingResult.PARAMETER_VELOCITY_USE, "false");
                mapping.setDesitinationFile(config.getPublicDocumentRoot());
                forwardHandler(Mapping.FILE_SYSTEM_HANDLER);
            }
            return;
        }
        if ("/admin".equals(path)) {
            doCommand(parameter);
            return;
        } else if ("/accessLog".equals(path)) {
            forwardHandler(AdminAccessLogHandler.class);
            return;
        } else if (REPLAY_UPLOAD_PATH.equals(path)) {
            replayUpload(parameter);
            return;
        } else if (path.startsWith("/mapping")) {
            forwardHandler(AdminMappingHandler.class);
            return;
        } else if (path.startsWith("/realHost")) {
            forwardHandler(AdminRealHostHandler.class);
            return;
        } else if (path.startsWith("/user")) {
            forwardHandler(AdminUserHandler.class);
            return;
        } else if (path.startsWith("/perf")) {
            forwardHandler(AdminPerfHandler.class);
            return;
        } else if (path.startsWith("/filter")) {
            forwardHandler(AdminFilterHandler.class);
            return;
        } else if (path.startsWith("/commissionAuth")) {
            forwardHandler(AdminCommissionAuthHandler.class);
            return;
        }
        logger.error("admin not found error.path:" + path + ":cid:" + getChannelId());
        completeResponse("404");
    }
}
