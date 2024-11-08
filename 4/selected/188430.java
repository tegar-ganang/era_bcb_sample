package naru.aweb.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.Date;
import javax.net.ssl.SSLEngine;
import org.apache.log4j.Logger;
import naru.async.ChannelHandler;
import naru.async.pool.BuffersUtil;
import naru.async.pool.PoolManager;
import naru.async.store.Store;
import naru.aweb.auth.AuthSession;
import naru.aweb.config.AccessLog;
import naru.aweb.config.Config;
import naru.aweb.core.DispatchHandler;
import naru.aweb.core.ServerBaseHandler;
import naru.aweb.mapping.MappingResult;
import naru.aweb.util.ServerParser;

/**
 * HTTP�v���g�R������{�ɁA��Ƀ��X�|���X���n���h�����O����B HTTP�v���g�R���̃X�L�[���ɓ��Ȃ��v���g�R�����n���h�����O����ꍇ�ɂ́A
 * onRead���\�b�h���I�[�o�[���C�h����
 * 
 * @author Naru
 * 
 */
public class WebServerHandler extends ServerBaseHandler {

    public static final String ENCODE = "utf-8";

    private static final String WRITE_CONTEXT_BODY = "writeContextBody";

    private static final String WRITE_CONTEXT_BODY_INTERNAL = "writeContextBodyInternal";

    private static final String WRITE_CONTEXT_HEADER = "writeContextHeader";

    private static final String WRITE_CONTEXT_LAST_HEADER = "writeContextLastHeader";

    private static Logger logger = Logger.getLogger(WebServerHandler.class);

    private static Config config = Config.getConfig();

    private static final String NON_SERVER_HEADER = "$$NON_SERVER_HEADER$$";

    private static String serverHeader = null;

    private static String getServerHeader() {
        if (serverHeader == NON_SERVER_HEADER) {
            return null;
        } else if (serverHeader != null) {
            return serverHeader;
        }
        serverHeader = config.getString("phantomServerHeader", null);
        if (serverHeader == null) {
            serverHeader = NON_SERVER_HEADER;
            return null;
        }
        return serverHeader;
    }

    private HeaderParser responseHeader = new HeaderParser();

    private long requestContentLength;

    private long requestReadBody;

    private long responseHeaderLength;

    private long responseContentLengthApl;

    private long responseWriteBodyApl;

    private long responseWriteBody;

    private boolean isFlushFirstResponse;

    private ByteBuffer[] firstBody;

    private boolean isResponseEnd;

    public SSLEngine getSSLEngine() {
        KeepAliveContext keepAliveContext = getKeepAliveContext();
        ServerParser sslServer = keepAliveContext.getProxyTargetServer();
        return config.getSslEngine(sslServer);
    }

    public void recycle() {
        requestContentLength = requestReadBody = 0;
        responseWriteBody = responseHeaderLength = responseWriteBodyApl = responseContentLengthApl = 0;
        responseHeader.recycle();
        responseBodyStream = null;
        responseBodyWriter = null;
        isFlushFirstResponse = false;
        isResponseEnd = false;
        firstBody = null;
        super.recycle();
    }

    /**
	 * proxy��������reverse proxy�̎��A�o�b�N�T�[�o���ԋp����buffer���p�[�X���邽�߂̃��\�b�h
	 * ���̂܂܁A�u���E�U�̃��X�|���X�ɗ��p�ł���B
	 * 
	 * @param buffers
	 * @return
	 */
    public final boolean parseResponseHeader(ByteBuffer[] buffers) {
        for (int i = 0; i < buffers.length; i++) {
            responseHeader.parse(buffers[i]);
        }
        PoolManager.poolArrayInstance(buffers);
        return responseHeader.isParseEnd();
    }

    public final boolean isReponseParseError() {
        return responseHeader.isParseError();
    }

    public final void setStatusCode(String statusCode) {
        responseHeader.setStatusCode(statusCode);
    }

    public final void setStatusCode(String statusCode, String reasonPhrase) {
        responseHeader.setStatusCode(statusCode, reasonPhrase);
    }

    public final String getStatusCode() {
        return responseHeader.getStatusCode();
    }

    public final void setHttpVersion(String httpVersion) {
        responseHeader.setResHttpVersion(httpVersion);
    }

    public final void setHeader(String name, String value) {
        responseHeader.setHeader(name, value);
    }

    public final void removeHeader(String name) {
        responseHeader.removeHeader(name);
    }

    public final void removeContentLength() {
        responseHeader.removeContentLength();
    }

    public final void setContentLength(long contentLength) {
        responseHeader.setContentLength(contentLength);
    }

    public final void setContentType(String contentType) {
        responseHeader.setContentType(contentType);
    }

    public final String getHeader(String name) {
        return responseHeader.getHeader(name);
    }

    public final String getResponseStatusCode() {
        return responseHeader.getStatusCode();
    }

    public final void setNoCacheResponseHeaders() {
        responseHeader.setHeader("Pragma", "no-cache");
        responseHeader.setHeader("Cache-Control", "no-cache");
        responseHeader.setHeader("Expires", "Thu, 01 Dec 1994 16:00:00 GMT");
    }

    public final void setResponseHeader(HeaderParser header) {
        responseHeader.setStatusCode(header.getStatusCode(), header.getReasonPhrase());
        responseHeader.setResHttpVersion(header.getResHttpVersion());
        responseHeader.setAllHeaders(header);
    }

    /**
	 * �{�f�B�̉�͏������J�n���܂��B doResponse�Ăяo�����ɂ́Aread�v�����o���Ă��Ȃ��̂ŁA�ق��Ă����body�͓������Ȃ��B
	 */
    public final void startParseRequestBody() {
        HeaderParser requestHeader = getRequestHeader();
        requestReadBody = 0;
        requestContentLength = requestHeader.getContentLength();
        ParameterParser parameterParser = getParameterParser();
        parameterParser.init(requestHeader.getMethod(), requestHeader.getContentType(), requestContentLength);
        String query = requestHeader.getQuery();
        if (query != null && !"".equals(query)) {
            parameterParser.parseQuery(query);
        }
        if (requestContentLength <= 0) {
            getAccessLog().setTimeCheckPint(AccessLog.TimePoint.requestBody);
            startResponseReqBody();
            return;
        }
        String transferEncoding = requestHeader.getHeader(HeaderParser.TRANSFER_ENCODING_HEADER);
        ChunkContext requestChunkContext = getKeepAliveContext().getRequestContext().getRequestChunkContext();
        if (HeaderParser.TRANSFER_ENCODING_CHUNKED.equalsIgnoreCase(transferEncoding)) {
            requestChunkContext.decodeInit(true, -1);
        } else {
            requestChunkContext.decodeInit(false, requestContentLength);
        }
        ByteBuffer[] body = requestHeader.getBodyBuffer();
        onReadPlain(null, body);
    }

    /**
	 * �Ǝ��Ƀ��X�|���X��ԋp�������l�́A���̃��\�b�h���I�[�o���C�h����
	 * ���̃��\�b�h�Ăяo�����_�ł́A�w�b�_��͎��ɓǂݍ���ł��܂���body������requestHeader�����Ɏc���Ă���_�ɒ���
	 * startParseBody���\�b�h�ł́A���̕����ɂ��Ė����I��onReadPlain���\�b�h���Ăяo���B
	 * 
	 * @param requestParser
	 */
    public void startResponse() {
        startParseRequestBody();
    }

    /**
	 * ���N�G�X�gbody�������������l�͂��̃��\�b�h���I�[�o���C�h���Ďg�� �f�t�H���g�ł́Aparameter�Ƃ��ĉ�͂��鏈��
	 * 
	 * @param buffers
	 */
    public void requestBody(ByteBuffer[] buffers) {
        ParameterParser parameterParser = getParameterParser();
        try {
            for (int i = 0; i < buffers.length; i++) {
                parameterParser.parse(buffers[i]);
            }
            PoolManager.poolArrayInstance(buffers);
        } catch (IOException e) {
            logger.warn("fail to parse body", e);
            completeResponse("500", "wrong body");
        }
    }

    /**
	 * ���N�G�X�g�f�[�^��ʒm������������ʒm �p�����^������҂��ď����������l�͂��̃��\�b�h���I�[�o���C�h���Ďg��
	 * 
	 * @param buffers
	 */
    public void startResponseReqBody() {
    }

    /**
	 * ���̃��\�b�h���ĂԂƕK�����X�|���X������������A������ύX�����Ƃ���Ȃ̂�final��
	 * 
	 * @param requestParser
	 * @param statusCode
	 */
    public final void completeResponse(String statusCode) {
        completeResponse(statusCode, (ByteBuffer) null);
    }

    /**
	 * ���̃��\�b�h���ĂԂƕK�����X�|���X������������A������ύX�����Ƃ���Ȃ̂�final��
	 * 
	 * @param requestParser
	 * @param statusCode
	 */
    public final void completeResponse(String statusCode, String body) {
        try {
            completeResponse(statusCode, body.getBytes(ENCODE));
        } catch (UnsupportedEncodingException e) {
            logger.error("fail to getBytes().", e);
        }
    }

    /**
	 * ���̃��\�b�h���ĂԂƕK�����X�|���X������������A������ύX�����Ƃ���Ȃ̂�final��
	 * 
	 * @param requestParser
	 * @param statusCode
	 */
    public final void completeResponse(String statusCode, byte[] body) {
        completeResponse(statusCode, ByteBuffer.wrap(body));
    }

    /**
	 * ���̃��\�b�h���ĂԂƕK�����X�|���X������������A������ύX�����Ƃ���Ȃ̂�final��
	 * 
	 * @param requestParser
	 * @param statusCode
	 */
    public final void completeResponse(String statusCode, ByteBuffer body) {
        if (statusCode != null) {
            setStatusCode(statusCode);
        }
        if (body != null) {
            responseContentLengthApl = (long) body.remaining();
            setContentLength(responseContentLengthApl);
            setHeader(HeaderParser.CONTENT_LENGTH_HEADER, Long.toString(responseContentLengthApl));
            responseBody(body);
        }
        responseEnd();
    }

    public final void responseHeaderAndRestBody() {
        String statusCode = responseHeader.getStatusCode();
        if ("304".equals(statusCode) || "204".equals(statusCode)) {
            responseContentLengthApl = 0;
        } else {
            responseContentLengthApl = responseHeader.getContentLength();
        }
        ByteBuffer[] body = responseHeader.getBodyBuffer();
        if (body != null) {
            responseBody(body);
        }
    }

    /**
	 * �R���e���c�������X�|���X������false�𕜋A�B
	 * 
	 * @return
	 */
    public final boolean needMoreResponse() {
        if (responseContentLengthApl < 0) {
            return true;
        }
        if (responseContentLengthApl > responseWriteBodyApl) {
            return true;
        }
        return false;
    }

    /**
	 * ���X�|���X�w�b�_���m�肵�Ă��炶��Ȃ���gzip�͊m�肵�Ȃ�
	 * 
	 * @param isAllResponse
	 *            �S���X�|���X����������ɂ��邩�ۂ�
	 */
    private void setupResponseHeader() {
        String httpVersion = responseHeader.getResHttpVersion();
        if (httpVersion == null) {
            responseHeader.setResHttpVersion(HeaderParser.HTTP_VESION_11);
            String serverHeader = getServerHeader();
            if (serverHeader != null) {
                responseHeader.setHeader("Server", serverHeader);
            }
            responseHeader.setHeader("Date", HeaderParser.fomatDateHeader(new Date()));
        }
        String statusCode = responseHeader.getStatusCode();
        if ("304".equals(statusCode) || "204".equals(statusCode)) {
            responseContentLengthApl = 0;
        } else {
            responseContentLengthApl = responseHeader.getContentLength();
        }
        if (setupGzip()) {
            logger.debug("contents gzip response.id:" + getPoolId());
        }
        return;
    }

    public boolean isCommitted() {
        return !isFlushFirstResponse;
    }

    public void responseEnd() {
        synchronized (this) {
            if (isResponseEnd || getChannelId() == -1) {
                return;
            }
            logger.debug("responseEnd called.handler:" + toString());
            isResponseEnd = true;
            if (isFlushFirstResponse == false) {
                flushFirstResponse(null);
                isFlushFirstResponse = true;
            }
            endOfResponse();
        }
    }

    private void endOfResponse() {
        boolean isReadWrite = false;
        GzipContext gzipContext = getGzipContext();
        if (gzipContext != null) {
            ByteBuffer[] zipdBuffer = gzipContext.getZipedBuffer(true);
            if (zipdBuffer != null && BuffersUtil.remaining(zipdBuffer) != 0) {
                isReadWrite = internalWriteBody(true, false, zipdBuffer);
            } else {
                isReadWrite = internalWriteBody(true, false, null);
            }
        } else {
            isReadWrite = internalWriteBody(true, false, null);
        }
        KeepAliveContext keepAliveContext = getKeepAliveContext();
        AccessLog accessLog = getAccessLog();
        accessLog.endProcess();
        accessLog.setStatusCode(responseHeader.getStatusCode());
        accessLog.setResponseHeaderLength(responseHeaderLength);
        accessLog.setContentType(responseHeader.getContentType());
        accessLog.setTransferEncoding(responseHeader.getHeader(HeaderParser.TRANSFER_ENCODING_HEADER));
        accessLog.setPlainResponseLength(responseWriteBodyApl);
        accessLog.setResponseLength(responseWriteBodyApl);
        accessLog.setContentEncoding(responseHeader.getHeader(HeaderParser.CONTENT_ENCODING_HEADER));
        accessLog.setRawRead(getTotalReadLength());
        accessLog.setRawWrite(getTotalWriteLength());
        Store readPeek = popReadPeekStore();
        if (readPeek != null && readPeek.getPutLength() >= 0) {
            logger.debug("#endOfResponse" + readPeek.getStoreId());
            accessLog.incTrace();
            readPeek.close(accessLog, readPeek);
            accessLog.setRequestBodyDigest(readPeek.getDigest());
        } else {
            if (readPeek != null) {
                readPeek.close();
            }
        }
        Store writePeek = popWritePeekStore();
        if (writePeek != null && writePeek.getPutLength() > 0) {
            accessLog.incTrace();
            writePeek.close(accessLog, writePeek);
            accessLog.setResponseBodyDigest(writePeek.getDigest());
        } else {
            if (writePeek != null) {
                writePeek.close();
            }
        }
        if (logger.isDebugEnabled()) {
            accessLog.log(true);
        }
        keepAliveContext.endOfResponse();
        if (!isReadWrite) {
            doneKeepAlive();
        }
    }

    public final void responseBodyLength(long length) {
        responseWriteBodyApl += length;
    }

    public final void responseBody(ByteBuffer buffer) {
        responseBody(BuffersUtil.toByteBufferArray(buffer));
    }

    /**
	 * keepAlive���邩�ۂ��𔻒f
	 * 
	 * @return
	 */
    private void prepareKeepAlive(long commitContentLength) {
        KeepAliveContext keepAliveContext = getKeepAliveContext();
        keepAliveContext.prepareResponse(this, responseHeader, commitContentLength);
    }

    protected boolean doneKeepAlive() {
        KeepAliveContext keepAliveContext = getKeepAliveContext();
        if (keepAliveContext != null) {
            boolean done = keepAliveContext.commitResponse(this);
            return done;
        }
        return false;
    }

    /**
	 * gzip encoding���\�����f���A�\�ȏꍇ�AgzipContext��p�ӂ���
	 * 
	 * @return
	 */
    private boolean setupGzip() {
        GzipContext gzipContext = getGzipContext();
        if (responseContentLengthApl == 0 || gzipContext != null) {
            return false;
        }
        String settingContentEncoding = config.getContentEncoding();
        if (!HeaderParser.CONTENT_ENCODING_GZIP.equalsIgnoreCase(settingContentEncoding)) {
            return false;
        }
        String transferEncoding = responseHeader.getHeader(HeaderParser.TRANSFER_ENCODING_HEADER);
        if (transferEncoding != null) {
            return false;
        }
        String contentEncoding = responseHeader.getHeader(HeaderParser.CONTENT_ENCODING_HEADER);
        if (contentEncoding != null) {
            return false;
        }
        String contentType = responseHeader.getHeader(HeaderParser.CONTENT_TYPE_HEADER);
        if (contentType != null && contentType.indexOf("zip") >= 0) {
            return false;
        }
        HeaderParser requestHeader = getRequestHeader();
        String acceptEncoding = requestHeader.getHeader(HeaderParser.ACCEPT_ENCODING_HEADER);
        if (acceptEncoding != null) {
            String[] entry = acceptEncoding.split(",");
            for (int i = 0; i < entry.length; i++) {
                if (HeaderParser.CONTENT_ENCODING_GZIP.equalsIgnoreCase(entry[i].trim())) {
                    responseHeader.setHeader(HeaderParser.CONTENT_ENCODING_HEADER, HeaderParser.CONTENT_ENCODING_GZIP);
                    responseHeader.removeContentLength();
                    gzipContext = (GzipContext) PoolManager.getInstance(GzipContext.class);
                    setGzipContext(gzipContext);
                    return true;
                }
            }
        }
        return false;
    }

    private ByteBuffer[] zipedIfNeed(boolean isLast, ByteBuffer[] buffers) {
        GzipContext gzipContext = getGzipContext();
        if (gzipContext == null || buffers == null) {
            return buffers;
        }
        return gzipContext.getZipedBuffer(isLast, buffers);
    }

    /**
	 * @param isLast
	 *            �ŏI�f�[�^���ۂ�
	 * @param neadCallback
	 *            onWriteBody��callback���K�v���ۂ�
	 * @param buffers
	 *            ���M�f�[�^
	 * @return ��write�������ۂ��H
	 */
    private boolean internalWriteBody(boolean isLast, boolean needCallback, ByteBuffer[] buffers) {
        KeepAliveContext keepAliveContext = getKeepAliveContext();
        buffers = keepAliveContext.chunkedIfNeed(isLast, buffers);
        if (buffers == null) {
            if (needCallback) {
                onWrittenBody();
            }
            return false;
        }
        String writeContext;
        if (needCallback) {
            writeContext = WRITE_CONTEXT_BODY;
        } else {
            writeContext = WRITE_CONTEXT_BODY_INTERNAL;
        }
        if (responseWriteBody == 0) {
            getAccessLog().setTimeCheckPint(AccessLog.TimePoint.responseBody);
        }
        long length = BuffersUtil.remaining(buffers);
        if (asyncWrite(writeContext, buffers)) {
            responseWriteBody += length;
            return true;
        }
        return false;
    }

    /**
	 * WebSocket�p��header�𑦍���flush���郁�\�b�h
	 */
    public void flushHeaderForWebSocket(String spec, String subprotocol) {
        ByteBuffer[] headerBuffer = responseHeader.getHeaderBuffer();
        if (headerBuffer == null) {
            logger.warn("flushHeader fail to getHeaderBuffer.cid:" + getChannelId());
            asyncClose(null);
            return;
        }
        getAccessLog().setTimeCheckPint(AccessLog.TimePoint.responseHeader);
        responseHeaderLength = BuffersUtil.remaining(headerBuffer);
        Store responsePeek = null;
        MappingResult mapping = getRequestMapping();
        AccessLog accessLog = getAccessLog();
        if (mapping != null) {
            switch(mapping.getLogType()) {
                case RESPONSE_TRACE:
                case TRACE:
                    responsePeek = Store.open(true);
                    ByteBuffer[] headerDup = PoolManager.duplicateBuffers(headerBuffer);
                    responsePeek.putBuffer(headerDup);
                    logger.debug("#flushHeader" + responsePeek.getStoreId());
                    accessLog.incTrace();
                    responsePeek.close(accessLog, responsePeek);
                    accessLog.setResponseHeaderDigest(responsePeek.getDigest());
                case REQUEST_TRACE:
                case ACCESS:
                    AccessLog wsAccessLog = accessLog.copyForWs();
                    StringBuffer sb = new StringBuffer();
                    switch(mapping.getDestinationType()) {
                        case WS:
                            sb.append("ws://");
                            sb.append(mapping.getResolveServer());
                            sb.append(mapping.getResolvePath());
                            break;
                        case WSS:
                            sb.append("wss://");
                            sb.append(mapping.getResolveServer());
                            sb.append(mapping.getResolvePath());
                            break;
                        case HANDLER:
                            if (isSsl()) {
                                sb.append("wss://");
                            } else {
                                sb.append("ws://");
                            }
                            sb.append(config.getSelfDomain());
                            sb.append(':');
                            sb.append(config.getProperty(Config.SELF_PORT));
                            sb.append(mapping.getSourcePath());
                            break;
                    }
                    sb.append('[');
                    sb.append(spec);
                    sb.append(':');
                    if (subprotocol != null) {
                        sb.append(subprotocol);
                    }
                    sb.append(':');
                    sb.append(getChannelId());
                    sb.append(']');
                    wsAccessLog.setRequestLine(sb.toString());
                    wsAccessLog.setStatusCode("B=S");
                    wsAccessLog.endProcess();
                    wsAccessLog.setSourceType(AccessLog.SOURCE_TYPE_WS_HANDSHAKE);
                    wsAccessLog.setPersist(true);
                    wsAccessLog.decTrace();
            }
        }
        asyncWrite(WRITE_CONTEXT_HEADER, headerBuffer);
        isFlushFirstResponse = true;
        if (firstBody != null) {
            logger.error("flushHeader use only websocket.");
            asyncClose(null);
        }
    }

    /**
	 * 1���N�G�X�g��1��A����body���������ݎ��ɌĂяo�����
	 * 
	 * @param secondBody
	 *            null�̏ꍇ�A���X�|���X�I��
	 */
    private void flushFirstResponse(ByteBuffer[] secondBody) {
        setupResponseHeader();
        ByteBuffer[] bodyBuffers = BuffersUtil.concatenate(firstBody, secondBody);
        long commitContentLength = -1;
        if (secondBody == null) {
            bodyBuffers = zipedIfNeed(true, bodyBuffers);
            commitContentLength = BuffersUtil.remaining(bodyBuffers);
            responseHeader.setContentLength(commitContentLength);
        } else {
            bodyBuffers = zipedIfNeed(false, bodyBuffers);
        }
        prepareKeepAlive(commitContentLength);
        ByteBuffer[] headerBuffer = responseHeader.getHeaderBuffer();
        if (headerBuffer == null) {
            logger.warn("flushFirstResponse fail to getHeaderBuffer.cid:" + getChannelId());
            logger.warn("firstBody:" + firstBody + ":secondBody:" + secondBody);
            asyncClose(null);
            return;
        }
        responseHeaderLength = BuffersUtil.remaining(headerBuffer);
        Store responsePeek = null;
        MappingResult mapping = getRequestMapping();
        if (mapping != null) {
            switch(mapping.getLogType()) {
                case RESPONSE_TRACE:
                case TRACE:
                    responsePeek = Store.open(true);
                    ByteBuffer[] headerDup = PoolManager.duplicateBuffers(headerBuffer);
                    responsePeek.putBuffer(headerDup);
                    AccessLog accessLog = getAccessLog();
                    logger.debug("#flushFirstResponse" + responsePeek.getStoreId());
                    accessLog.incTrace();
                    responsePeek.close(accessLog, responsePeek);
                    accessLog.setResponseHeaderDigest(responsePeek.getDigest());
                    responsePeek = Store.open(true);
            }
        }
        getAccessLog().setTimeCheckPint(AccessLog.TimePoint.responseHeader);
        if (firstBody == null && secondBody == null) {
            getAccessLog().setTimeCheckPint(AccessLog.TimePoint.responseBody);
            asyncWrite(WRITE_CONTEXT_LAST_HEADER, headerBuffer);
            if (responsePeek != null) {
                responsePeek.close();
            }
            return;
        }
        firstBody = null;
        logger.debug("flushFirstResponse cid:" + getChannelId() + ":header[0]:" + headerBuffer[0]);
        asyncWrite(WRITE_CONTEXT_HEADER, headerBuffer);
        if (responsePeek != null) {
            pushWritePeekStore(responsePeek);
        }
        if (secondBody == null) {
            internalWriteBody(true, true, bodyBuffers);
        } else {
            internalWriteBody(false, true, bodyBuffers);
        }
    }

    public final void responseBody(ByteBuffer[] buffers) {
        responseWriteBodyApl += BuffersUtil.remaining(buffers);
        boolean isCallbackOnWrittenBody = false;
        synchronized (this) {
            if (getChannelId() == -1) {
                return;
            }
            if (isFlushFirstResponse == false && firstBody != null) {
                flushFirstResponse(buffers);
                isFlushFirstResponse = true;
                return;
            } else if (isFlushFirstResponse == false && firstBody == null) {
                firstBody = buffers;
                isCallbackOnWrittenBody = true;
            }
        }
        if (isCallbackOnWrittenBody) {
            onWrittenBody();
            return;
        }
        if (isFlushFirstResponse) {
            buffers = zipedIfNeed(false, buffers);
            if (buffers == null) {
                onWrittenBody();
            } else {
                internalWriteBody(false, true, buffers);
            }
        }
        if (needMoreResponse()) {
            return;
        }
        responseEnd();
    }

    /**
	 * ���̃N���X�́A�S�w�b�_���ǂݍ��܂�Ă���Ăяo�����̂ŁAbody�f�[�^������������ʉ߂���B
	 * �w�b�_��͎��ɓǂݍ���ł��܂���body�����́A�����I�ɌĂяo���Ă���B
	 * 
	 * @param buffers
	 */
    public void onReadPlain(Object userContext, ByteBuffer[] buffers) {
        logger.debug("#onReadPlain cid:" + getChannelId());
        ChunkContext requestChunkContext = getRequestContext().getRequestChunkContext();
        if (requestChunkContext.isEndOfData()) {
            PoolManager.poolBufferInstance(buffers);
            return;
        }
        if (buffers != null) {
            requestReadBody += BuffersUtil.remaining(buffers);
            requestBody(requestChunkContext.decodeChunk(buffers));
        }
        if (!requestChunkContext.isEndOfData()) {
            asyncRead(null);
            return;
        }
        getAccessLog().setTimeCheckPint(AccessLog.TimePoint.requestBody);
        startResponseReqBody();
        return;
    }

    public ChannelHandler forwardHandler(Class handlerClass) {
        return forwardHandler(handlerClass, true);
    }

    public ChannelHandler forwardHandler(Class handlerClass, boolean callStartMethod) {
        logger.debug("#forwardHandler cid:" + getChannelId() + ":" + handlerClass.getName());
        WebServerHandler handler = (WebServerHandler) super.allocHandler(handlerClass);
        handler.responseHeader.setAllHeaders(responseHeader);
        handler.requestContentLength = requestContentLength;
        handler.requestReadBody = requestReadBody;
        super.forwardHandler(handler);
        if (callStartMethod) {
            if (handler.requestContentLength > 0 && handler.requestContentLength <= handler.requestReadBody) {
                handler.startResponseReqBody();
            } else {
                handler.startResponse();
            }
        }
        return handler;
    }

    public void waitForNextRequest() {
        logger.debug("#waitForNextRequest cid:" + getChannelId());
        DispatchHandler handler = (DispatchHandler) super.forwardHandler(DispatchHandler.class);
        if (handler == null) {
            logger.warn("fail to forward Dispatcher.Can't keepAlive.");
            return;
        }
        handler.onStartRequest();
    }

    public void onFinished() {
        logger.debug("#onFinished cid:" + getChannelId());
        responseEnd();
        KeepAliveContext keepAliveContext = getKeepAliveContext();
        if (keepAliveContext != null) {
            keepAliveContext.finishedOfServerHandler();
        }
        super.onFinished();
    }

    public void onWrittenPlain(Object userContext) {
        logger.debug("#onWrittenPlain cid:" + getChannelId() + ":userContext:" + userContext);
        if (userContext == WRITE_CONTEXT_BODY) {
            onWrittenBody();
        }
        if (userContext == WRITE_CONTEXT_BODY || userContext == WRITE_CONTEXT_BODY_INTERNAL || userContext == WRITE_CONTEXT_LAST_HEADER) {
            synchronized (this) {
                if (getChannelId() < 0) {
                    return;
                }
                if (isResponseEnd) {
                    if (doneKeepAlive()) {
                        return;
                    }
                }
            }
        }
        super.onWrittenPlain(userContext);
    }

    public void onWrittenBody() {
        logger.debug("#onWrittenBody cid:" + getChannelId());
    }

    private OutputStream responseBodyStream;

    private Writer responseBodyWriter;

    private class ResponseBodyStream extends OutputStream {

        private ByteBuffer buffer;

        private int capacity;

        private int limit;

        ResponseBodyStream() {
            buffer = null;
        }

        public void close() throws IOException {
            responseBodyStream = null;
            responseBodyWriter = null;
            flush();
        }

        public void flush() throws IOException {
            if (buffer != null) {
                buffer.flip();
                responseBody(BuffersUtil.toByteBufferArray(buffer));
            }
            buffer = null;
        }

        public void write(byte[] src, int offset, int length) throws IOException {
            if (buffer != null && capacity < (limit + length)) {
                flush();
            }
            if (buffer == null) {
                buffer = PoolManager.getBufferInstance();
                capacity = buffer.capacity();
                limit = 0;
                if (capacity < length) {
                    PoolManager.poolBufferInstance(buffer);
                    buffer = null;
                    ByteBuffer[] buffers = BuffersUtil.buffers(src, offset, length);
                    responseBody(buffers);
                    return;
                }
            }
            buffer.put(src, offset, length);
            limit += length;
        }

        public void write(byte[] src) throws IOException {
            write(src, 0, src.length);
        }

        public void write(int src) throws IOException {
            write(new byte[] { (byte) src }, 0, 1);
        }
    }

    public final OutputStream getResponseBodyStream() {
        if (responseBodyStream != null) {
            return responseBodyStream;
        }
        responseBodyStream = new ResponseBodyStream();
        return responseBodyStream;
    }

    public final Writer getResponseBodyWriter(String enc) throws UnsupportedEncodingException {
        if (responseBodyWriter != null) {
            return responseBodyWriter;
        }
        responseBodyWriter = new OutputStreamWriter(getResponseBodyStream(), enc);
        return responseBodyWriter;
    }

    /**
	 * json����������X�|���X����
	 * 
	 * @param json�@toString��json�ƂȂ�I�u�W�F�N�g
	 */
    public void responseJson(Object json) {
        responseJson(json, null);
    }

    /**
	 * json����������X�|���X����
	 * 
	 * @param json�@toString��json�ƂȂ�I�u�W�F�N�g
	 * @param callback callback���\�b�h��
	 */
    public void responseJson(Object json, String callback) {
        setNoCacheResponseHeaders();
        setContentType("text/javascript; charset=utf-8");
        setStatusCode("200");
        Writer out = null;
        try {
            out = getResponseBodyWriter("utf-8");
        } catch (UnsupportedEncodingException e) {
            completeResponse("500", "fail to getWriter.");
            logger.error("fail to getWriter.", e);
            return;
        }
        try {
            if (callback != null) {
                out.write(callback);
                out.write("(");
            }
            if (json == null) {
                out.write("null");
            } else {
                out.write(json.toString());
            }
            if (callback != null) {
                out.write(");");
            }
        } catch (IOException e) {
            logger.error("doJson IO error.", e);
        } catch (Throwable e) {
            logger.error("doJson IO error.!!", e);
        } finally {
            try {
                out.close();
            } catch (IOException ignore) {
            }
        }
        responseEnd();
    }

    public RequestContext getRequestContext() {
        KeepAliveContext keepAliveContext = getKeepAliveContext();
        if (keepAliveContext == null) {
            return null;
        }
        return keepAliveContext.getRequestContext();
    }

    public AuthSession getAuthSession() {
        RequestContext requestContext = getRequestContext();
        if (requestContext == null) {
            return null;
        }
        return requestContext.getAuthSession();
    }
}
