package naru.aweb.handler;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;
import naru.async.pool.PoolManager;
import naru.async.store.Store;
import naru.aweb.config.AccessLog;
import naru.aweb.config.Config;
import naru.aweb.config.Mapping;
import naru.aweb.http.HeaderParser;
import naru.aweb.http.WebServerHandler;
import naru.aweb.mapping.MappingResult;
import naru.aweb.util.ServerParser;
import org.apache.log4j.Logger;

public class ReplayHelper {

    private static Logger logger = Logger.getLogger(ReplayHelper.class);

    private static Config config = Config.getConfig();

    private File defaultRootDir = new File(config.getString("path.replayDocroot"));

    private String getResolveDigest(HeaderParser requestHeader, MappingResult mapping) {
        ServerParser targetHostServer = mapping.getResolveServer();
        String path = mapping.getResolvePath();
        String resolveDigest = AccessLog.calcResolveDigest(requestHeader.getMethod(), mapping.isResolvedHttps(), targetHostServer.toString(), path, requestHeader.getQuery());
        return resolveDigest;
    }

    private AccessLog searchAccessLog(AccessLog myAccessLog, HeaderParser requestHeader, MappingResult mapping) {
        String resolveDigest = getResolveDigest(requestHeader, mapping);
        Collection<AccessLog> accessLogs = AccessLog.query("WHERE destinationType!='R' && statusCode!=null && responseBodyDigest!=null && resolveDigest=='" + resolveDigest + "' ORDER BY id DESC");
        Iterator<AccessLog> itr = accessLogs.iterator();
        AccessLog accessLog = null;
        while (itr.hasNext()) {
            accessLog = itr.next();
            myAccessLog.setOriginalLogId(accessLog.getId());
            return accessLog;
        }
        return null;
    }

    private File existsFile(File base, String path) {
        File file = new File(base, path);
        if (file.exists()) {
            return file;
        }
        return null;
    }

    private File searchFile(HeaderParser requestHeader, MappingResult mapping) {
        String protocol = null;
        if (!"GET".equals(requestHeader.getMethod())) {
            return null;
        }
        switch(mapping.getDestinationType()) {
            case HTTP:
                protocol = "http/";
                break;
            case HTTPS:
                protocol = "https/";
                break;
            default:
                return null;
        }
        String replayDocRoot = (String) mapping.getOption("replayDocroot");
        File seachRoot = defaultRootDir;
        if (replayDocRoot != null) {
            seachRoot = new File(replayDocRoot);
        }
        if (!seachRoot.exists()) {
            return null;
        }
        ServerParser resolveServer = mapping.getResolveServer();
        String path = mapping.getResolvePath();
        File file;
        file = existsFile(seachRoot, protocol + resolveServer.getHost() + path);
        if (file != null && file.isFile()) {
            return file;
        }
        file = existsFile(seachRoot, path);
        if (file != null && file.isFile()) {
            return file;
        }
        int pos = path.lastIndexOf('/');
        file = existsFile(seachRoot, path.substring(pos));
        if (file != null && file.isFile()) {
            return file;
        }
        return null;
    }

    /**
	 */
    public boolean doReplay(ProxyHandler handler, ByteBuffer[] body) {
        logger.debug("#doReplay cid:" + handler.getChannelId());
        AccessLog accessLog = handler.getAccessLog();
        HeaderParser requestHeader = handler.getRequestHeader();
        MappingResult mapping = handler.getRequestMapping();
        AccessLog recodeLog = searchAccessLog(accessLog, requestHeader, mapping);
        if (recodeLog != null) {
            accessLog.setDestinationType(AccessLog.DESTINATION_TYPE_REPLAY);
            handler.setStatusCode(recodeLog.getStatusCode());
            String contentType = recodeLog.getContentType();
            if (contentType != null) {
                handler.setContentType(contentType);
            }
        }
        File file = searchFile(requestHeader, mapping);
        if (file != null) {
            accessLog.setDestinationType(AccessLog.DESTINATION_TYPE_REPLAY);
            logger.debug("response from file.file:" + file.getAbsolutePath());
            handler.setRequestAttribute(ProxyHandler.ATTRIBUTE_RESPONSE_FILE, file);
            WebServerHandler response = (WebServerHandler) handler.forwardHandler(Mapping.FILE_SYSTEM_HANDLER);
            PoolManager.poolBufferInstance(body);
            return true;
        } else if (recodeLog != null) {
            String bodyDigest = recodeLog.getResponseBodyDigest();
            Store store = Store.open(bodyDigest);
            if (store != null) {
                logger.debug("response from trace.bodyDigest:" + bodyDigest);
                String contentEncoding = recodeLog.getContentEncoding();
                if (contentEncoding != null) {
                    handler.setHeader(HeaderParser.CONTENT_ENCODING_HEADER, contentEncoding);
                }
                String transferEncoding = recodeLog.getTransferEncoding();
                if (transferEncoding != null) {
                    handler.setHeader(HeaderParser.TRANSFER_ENCODING_HEADER, transferEncoding);
                } else {
                    long contentLength = recodeLog.getResponseLength();
                    handler.setContentLength(contentLength);
                }
                handler.setAttribute("Store", store);
                handler.forwardHandler(Mapping.STORE_HANDLER);
                PoolManager.poolBufferInstance(body);
                return true;
            }
        }
        return false;
    }
}
