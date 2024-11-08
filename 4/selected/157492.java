package com.bitgate.util.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Vector;
import java.util.zip.ZipEntry;
import com.bitgate.server.Server;
import com.bitgate.util.cache.FileCache;
import com.bitgate.util.debug.Debug;
import com.bitgate.util.filesystem.FileAccess;
import com.bitgate.util.memory.Objects;
import com.bitgate.util.mime.MimeTypes;
import com.bitgate.util.node.NodeUtil;
import com.bitgate.util.services.engine.DocumentEngine;
import com.bitgate.util.services.engine.Encoder;
import com.bitgate.util.services.engine.RenderEngine;
import com.bitgate.util.services.protocol.WebResponseCodes;
import com.bitgate.util.socket.SocketTools;

class FileSorter implements Comparator {

    public int compare(Object o1, Object o2) {
        return Collator.getInstance().compare(((File) o1).toString(), ((File) o2).toString());
    }
}

/**
 * This class handles the sending of static data to the specified client.
 *
 * @author Kenji Hollis &lt;kenji@nuklees.com&gt;
 * @version $Id: //depot/nuklees/util/services/StaticContentWorker.java#46 $
 */
public class StaticContentWorker {

    private WorkerContext wContext;

    /**
     * Constructor.
     *
     * @param wContext The currently active <code>WorkerContext</code> object.
     */
    public StaticContentWorker(WorkerContext wContext) {
        this.wContext = wContext;
    }

    /**
     * Handles the request made by a client, sending the requested document to the client in the form of a
     * <code>ResponseBuffer</code>.
     *
     * @param request The request that was made.
     * @param vfstype The type of data to be sent.
     * @return <code>ResponseBuffer</code> containing the data.
     * @throws KeepaliveException on any errors.
     */
    public ResponseBuffer handleRequest(String request, int vfstype) throws KeepaliveException {
        ResponseBuffer response = new ResponseBuffer();
        String requestedDocument = request;
        String physicalRequestDirectory = wContext.getRequestDirectory();
        String bareFile = null;
        String docRoot = wContext.getDocRoot();
        String rewrittenDocument = null;
        String pathinfo = null;
        long currentUsedBandwidth = wContext.getCurrentUsedBandwidth();
        long currentMaxBandwidth = wContext.getCurrentMaxBandwidth();
        long ifModifiedSince = wContext.getClientContext().getIfModifiedSince();
        boolean isProtected = false, bandwidthExceeded = false, allowGzip = wContext.getClientContext().getAllowGzip();
        ArrayList filterList = wContext.getVendContext().getVend().getFilter(wContext.getClientContext().getMatchedHost());
        int filterListSize = filterList.size();
        int type = vfstype;
        if (type == ClientHandler.PAGE_NOT_FOUND_MAPPED || type == (ClientHandler.PAGE_VFS_BASED + ClientHandler.PAGE_NOT_FOUND_MAPPED)) {
            rewrittenDocument = wContext.getErrorPage(request);
            type = wContext.getVendContext().getFileAccess().getVFSType(rewrittenDocument, wContext.getClientContext().getMatchedHost());
            if (type != FileAccess.TYPE_UNKNOWN) {
                type = ClientHandler.PAGE_VFS_BASED + ClientHandler.PAGE_STATIC_DATA;
                Debug.debug("Filename '" + rewrittenDocument + "' matches a VFS lookup.");
            }
        }
        int periodIndex = request.indexOf(".");
        if (periodIndex != -1) {
            if (request.indexOf("/", periodIndex) != -1) {
                String extension = request.substring(periodIndex + 1, request.indexOf("/", periodIndex));
                if (BinarySupport.isBinary(extension)) {
                    pathinfo = request.substring(request.indexOf("/", periodIndex + 1));
                    request = request.substring(0, request.indexOf("/", periodIndex));
                }
            }
        }
        requestedDocument = request;
        Debug.debug("Handling request: request='" + request + "' origtype='" + vfstype + "' type='" + type + "' rewrittenDocument='" + rewrittenDocument + "' pathinfo='" + pathinfo + "'");
        int requestLastSlash = request.lastIndexOf("/");
        if (requestLastSlash != -1) {
            bareFile = request.substring(requestLastSlash + 1);
        } else {
            bareFile = request;
        }
        for (int i = 0; i < filterListSize; i++) {
            String fileFilter = (String) filterList.get(i);
            if (bareFile.equalsIgnoreCase(fileFilter) || requestedDocument.indexOf(fileFilter) != -1) {
                isProtected = true;
                break;
            }
        }
        if (isProtected) {
            StringBuffer body = WebResponseCodes.createGenericResponse(wContext, WebResponseCodes.HTTP_NOT_FOUND, "The requested resource '" + requestedDocument + "' was not found on this server.");
            response.setBody(body, allowGzip);
            response.setHeaders(wContext.createHeader(null, WebResponseCodes.HTTP_NOT_FOUND, response.getBodySize(), 0L, "text/html", false, allowGzip));
            response.setResponse(WebResponseCodes.HTTP_NOT_FOUND);
            return response;
        }
        boolean cachable = true;
        try {
            if (rewrittenDocument != null) {
                cachable = FileCache.getDefault().isCachable(rewrittenDocument);
                Debug.debug("Data is cachable for request '" + rewrittenDocument + "'");
            } else {
                cachable = FileCache.getDefault().isCachable(wContext.getDocRoot() + "/" + requestedDocument);
                Debug.debug("Data is cachable for request '" + wContext.getDocRoot() + "/" + requestedDocument + "'");
            }
        } catch (IOException e) {
        }
        if (cachable) {
            byte[] fileData = null;
            long lastModified = 0L;
            try {
                if (type > ClientHandler.PAGE_VFS_BASED) {
                    if (rewrittenDocument != null) {
                        fileData = wContext.getVendContext().getVend().getFileAccess().getFile(wContext, rewrittenDocument, wContext.getClientContext().getMatchedHost(), wContext.getVendContext().getVend().getRenderExtension(wContext.getClientContext().getMatchedHost()), wContext.getVendContext().getVend().getServerpageExtensions(), true);
                        if (lastModified == 0) {
                            lastModified = wContext.getVendContext().getVend().getFileAccess().lastModified(wContext, rewrittenDocument, wContext.getClientContext().getMatchedHost());
                        }
                        Debug.debug("File data for request '" + rewrittenDocument + "' size='" + fileData.length + "' lastModified='" + lastModified + "'");
                    } else {
                        fileData = wContext.getVendContext().getVend().getFileAccess().getFile(wContext, requestedDocument, wContext.getClientContext().getMatchedHost(), wContext.getVendContext().getVend().getRenderExtension(wContext.getClientContext().getMatchedHost()), wContext.getVendContext().getVend().getServerpageExtensions(), true);
                        if (lastModified == 0) {
                            lastModified = wContext.getVendContext().getVend().getFileAccess().lastModified(wContext, requestedDocument, wContext.getClientContext().getMatchedHost());
                        }
                        Debug.debug("File data for request '" + requestedDocument + "' size='" + fileData.length + "' lastModified='" + lastModified + "'");
                    }
                } else {
                    fileData = FileCache.getDefault().read(null, wContext.getDocRoot() + "/" + requestedDocument, wContext.getVendContext().getVend().getRenderExtension(wContext.getClientContext().getMatchedHost()), wContext.getVendContext().getVend().getServerpageExtensions(), false);
                    lastModified = FileCache.getDefault().lastModified(null, requestedDocument);
                    Debug.debug("File data for request '" + requestedDocument + "' size='" + fileData.length + "' lastModified='" + lastModified + "'");
                }
            } catch (Exception e) {
                StringBuffer body = WebResponseCodes.createGenericResponse(wContext, WebResponseCodes.HTTP_NOT_FOUND, "The requested resource '" + requestedDocument + "' was not found on this server.");
                response.setBody(body, allowGzip);
                response.setHeaders(wContext.createHeader(null, WebResponseCodes.HTTP_NOT_FOUND, response.getBodySize(), 0L, "text/html", false, allowGzip));
                response.setResponse(WebResponseCodes.HTTP_NOT_FOUND);
                return response;
            }
            currentUsedBandwidth += fileData.length;
            if (currentMaxBandwidth != 0 && (currentUsedBandwidth + fileData.length) > currentMaxBandwidth) {
                bandwidthExceeded = true;
            }
            if (bandwidthExceeded) {
                Debug.debug("Connection Failed: Bandwidth exceeded.");
                StringBuffer body = WebResponseCodes.createGenericResponse(wContext, WebResponseCodes.HTTP_BANDWIDTH_EXCEEDED, "The server is temporarily unable to service your request due to the site owner reaching his/her " + "bandwidth limit.  If you feel you have received this message in error, please contact the site " + "administrator at '" + wContext.getEmail() + "'");
                response.setBody(body, allowGzip);
                response.setHeaders(wContext.createHeader(null, WebResponseCodes.HTTP_BANDWIDTH_EXCEEDED, response.getBodySize(), 0L, "text/html", false, allowGzip));
                response.setResponse(WebResponseCodes.HTTP_BANDWIDTH_EXCEEDED);
                return response;
            }
            if (ifModifiedSince != 0L && ifModifiedSince >= lastModified) {
                response.setHeaders(wContext.createHeader(null, WebResponseCodes.HTTP_NOT_MODIFIED, 0, 0L, "text/html", false, allowGzip));
                response.setResponse(WebResponseCodes.HTTP_NOT_MODIFIED);
                return response;
            }
            BinaryContext bContext = null;
            if (BinarySupport.isBinary(bareFile)) {
                try {
                    bContext = BinarySupport.handleBinary(wContext.getDocRoot() + "/" + requestedDocument, request, pathinfo, wContext);
                } catch (Exception e) {
                    StringBuffer body = WebResponseCodes.createGenericResponse(wContext, WebResponseCodes.HTTP_INTERNAL_ERROR, "The requested resource '" + requestedDocument + "' failed to process: " + e.getMessage());
                    response.setBody(body, allowGzip);
                    response.setHeaders(wContext.createHeader(null, WebResponseCodes.HTTP_INTERNAL_ERROR, response.getBodySize(), 0L, "text/html", false, allowGzip));
                    response.setResponse(WebResponseCodes.HTTP_INTERNAL_ERROR);
                    return response;
                }
            }
            if (bContext != null) {
                response.setBody(bContext.getBody());
            } else {
                response.setBody(fileData);
            }
            if (bContext != null && bContext.getHeaders().length() != 0) {
                response.setHeaders(bContext.getHeaders());
            } else {
                response.setHeaders(wContext.createHeader(requestedDocument, WebResponseCodes.HTTP_OK, (int) fileData.length, lastModified, null, true, false));
            }
            response.setResponse(WebResponseCodes.HTTP_OK);
            return response;
        } else {
            File sendFile = new File(wContext.getDocRoot() + "/" + requestedDocument);
            if (!sendFile.exists()) {
                StringBuffer body = WebResponseCodes.createGenericResponse(wContext, WebResponseCodes.HTTP_NOT_FOUND, "The requested resource '" + requestedDocument + "' was not found on this server.");
                response.setBody(body, allowGzip);
                response.setHeaders(wContext.createHeader(null, WebResponseCodes.HTTP_NOT_FOUND, response.getBodySize(), 0L, "text/html", false, allowGzip));
                response.setResponse(WebResponseCodes.HTTP_NOT_FOUND);
                return response;
            }
            if (currentMaxBandwidth != 0 && (currentUsedBandwidth + sendFile.length()) > currentMaxBandwidth) {
                bandwidthExceeded = true;
            }
            if (bandwidthExceeded) {
                Debug.debug("Connection Failed: Bandwidth exceeded.");
                StringBuffer body = WebResponseCodes.createGenericResponse(wContext, WebResponseCodes.HTTP_BANDWIDTH_EXCEEDED, "The server is temporarily unable to service your request due to the site owner reaching his/her " + "bandwidth limit.  If you feel you have received this message in error, please contact the site " + "administrator at '" + wContext.getEmail() + "'");
                response.setBody(body, allowGzip);
                response.setHeaders(wContext.createHeader(null, WebResponseCodes.HTTP_BANDWIDTH_EXCEEDED, response.getBodySize(), 0L, "text/html", false, allowGzip));
                response.setResponse(WebResponseCodes.HTTP_BANDWIDTH_EXCEEDED);
                return response;
            }
            if (ifModifiedSince != 0L && ifModifiedSince >= sendFile.lastModified()) {
                response.setHeaders(wContext.createHeader(null, WebResponseCodes.HTTP_NOT_MODIFIED, 0, 0L, "text/html", false, allowGzip));
                response.setResponse(WebResponseCodes.HTTP_NOT_MODIFIED);
                return response;
            }
            if (MimeTypes.getDefault().getBinaryAssociation(wContext.getDocRoot() + "/" + requestedDocument) != null && (new File(MimeTypes.getDefault().getBinaryAssociation(wContext.getDocRoot() + "/" + requestedDocument)).exists())) {
                String processorBinary = MimeTypes.getDefault().getBinaryAssociation(wContext.getDocRoot() + "/" + requestedDocument);
                String unknownContent = "text/html";
                ArrayList environmentVariables = new ArrayList();
                String envp[] = null;
                String unknownString = NodeUtil.walkNodeTree(Server.getConfig(), "//configuration/object[@type='engine.default']/property[@type='engine.binaryunknowncontent']/@value");
                byte fileData[] = null;
                long lastModified = 0L;
                Debug.debug("File '" + requestedDocument + "' matches a binary file handler '" + processorBinary + "'");
                try {
                    fileData = FileCache.getDefault().read(null, wContext.getDocRoot() + "/" + requestedDocument, wContext.getVendContext().getVend().getRenderExtension(wContext.getClientContext().getMatchedHost()), wContext.getVendContext().getVend().getServerpageExtensions(), false);
                    lastModified = FileCache.getDefault().lastModified(null, wContext.getDocRoot() + "/" + requestedDocument);
                } catch (Exception e) {
                    StringBuffer body = WebResponseCodes.createGenericResponse(wContext, WebResponseCodes.HTTP_NOT_FOUND, "The requested resource '" + requestedDocument + "' was not found on this server.");
                    response.setBody(body, allowGzip);
                    response.setHeaders(wContext.createHeader(null, WebResponseCodes.HTTP_NOT_FOUND, response.getBodySize(), 0L, "text/html", false, allowGzip));
                    response.setResponse(WebResponseCodes.HTTP_NOT_FOUND);
                    return response;
                }
                if (unknownString != null) {
                    unknownContent = unknownString;
                }
                Process proc = null;
                StringBuffer output = new StringBuffer();
                String str;
                int n;
                String binaryString = NodeUtil.walkNodeTree(Server.getConfig(), "//configuration/object[@type='engine.default']/property[@type='engine.binaryapachesupport']/@value");
                StringBuffer ps = new StringBuffer();
                StringBuffer headersPs = new StringBuffer();
                if (binaryString != null && binaryString.equalsIgnoreCase("true")) {
                    if (wContext.getClientContext() != null) {
                        environmentVariables.add("SERVER_ADDR=" + wContext.getClientContext().getHost());
                        environmentVariables.add("SERVER_PORT=" + wContext.getClientContext().getPort());
                        environmentVariables.add("REMOTE_ADDR=" + wContext.getClientContext().getHost());
                        environmentVariables.add("REMOTE_PORT=" + wContext.getClientContext().getPort());
                        environmentVariables.add("DOCUMENT_ROOT=" + wContext.getDocRoot());
                        environmentVariables.add("REQUEST_URI=" + request);
                        int requestedDocumentQuestion = request.indexOf("?");
                        if (requestedDocumentQuestion != -1) {
                            environmentVariables.add("QUERY_STRING=" + request.substring(requestedDocumentQuestion + 1));
                        }
                        environmentVariables.add("REQUEST_METHOD=GET");
                        int envVarSize = environmentVariables.size();
                        if (envVarSize > 0) {
                            envp = new String[envVarSize];
                            for (int counter = 0; counter < envVarSize; counter++) {
                                envp[counter] = (String) environmentVariables.get(counter);
                            }
                        }
                        environmentVariables = null;
                    }
                    try {
                        proc = Runtime.getRuntime().exec(processorBinary, envp);
                        if (proc == null) {
                            StringBuffer body = WebResponseCodes.createGenericResponse(wContext, WebResponseCodes.HTTP_INTERNAL_ERROR, "Unable to process this requested file through the appropriate binary.");
                            response.setBody(body, allowGzip);
                            response.setHeaders(wContext.createHeader(null, WebResponseCodes.HTTP_INTERNAL_ERROR, response.getBodySize(), 0L, "text/html", false, allowGzip));
                            response.setResponse(WebResponseCodes.HTTP_NOT_FOUND);
                            return response;
                        }
                        OutputStream ostr = proc.getOutputStream();
                        BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                        ostr.write(fileData, 0, fileData.length);
                        ostr.flush();
                        ostr.close();
                        boolean firstLine = true, returnSeen = false, contentTypeSeen = false, contentLengthSeen = false;
                        while ((str = br.readLine()) != null) {
                            if (firstLine) {
                                if (!str.startsWith("HTTP")) {
                                    headersPs.append("HTTP/1.1 200 OK\r\n");
                                } else {
                                    headersPs.append("\r\n");
                                }
                                firstLine = false;
                                continue;
                            } else if (!returnSeen && str.indexOf(":") != -1 && str.substring(0, str.indexOf(":")).equalsIgnoreCase("content-type")) {
                                contentTypeSeen = true;
                                headersPs.append(str + "\n");
                                continue;
                            } else if (!returnSeen && str.indexOf(":") != -1 && str.substring(0, str.indexOf(":")).equalsIgnoreCase("content-length")) {
                                contentLengthSeen = true;
                                headersPs.append(str + "\n");
                                continue;
                            } else if (!returnSeen && (str.equals("") || str.equals("\r") || str.equals("\n"))) {
                                returnSeen = true;
                                if (!contentTypeSeen) {
                                    headersPs.append("Content-Type: " + unknownContent + "\r\n\r\n");
                                    contentTypeSeen = true;
                                    continue;
                                }
                            } else if (!returnSeen) {
                                headersPs.append(str + "\n");
                                continue;
                            }
                            ps.append(str + "\n");
                        }
                        br.close();
                        if (!contentLengthSeen) {
                            headersPs = new StringBuffer(headersPs.toString().trim());
                            headersPs.append("\n");
                            headersPs.append("Content-Length: " + ps.length() + "\n");
                        }
                    } catch (IOException e) {
                        StringBuffer body = WebResponseCodes.createGenericResponse(wContext, WebResponseCodes.HTTP_INTERNAL_ERROR, "The requested resource '" + request + "' failed to run:<br/>" + Debug.getStackTrace(e) + "<p></p>\nPlease refer to server logs for information regarding why.");
                        response.setBody(body, allowGzip);
                        response.setHeaders(wContext.createHeader(null, WebResponseCodes.HTTP_INTERNAL_ERROR, response.getBodySize(), 0L, "text/html", false, allowGzip));
                        response.setResponse(WebResponseCodes.HTTP_INTERNAL_ERROR);
                        return response;
                    }
                }
                fileData = ps.toString().getBytes();
                response.setBody(fileData);
                if (headersPs.length() > 0) {
                    response.setHeaders(headersPs);
                } else {
                    response.setHeaders(wContext.createHeader(requestedDocument, WebResponseCodes.HTTP_OK, (int) fileData.length, lastModified, null, true, false));
                }
                response.setResponse(WebResponseCodes.HTTP_OK);
                return response;
            } else {
                FileChannel roChannel = null;
                try {
                    roChannel = new FileInputStream(sendFile).getChannel();
                } catch (FileNotFoundException e) {
                    StringBuffer body = WebResponseCodes.createGenericResponse(wContext, WebResponseCodes.HTTP_NOT_FOUND, "The requested resource '" + requestedDocument + "' was not found on this server.");
                    response.setBody(body, allowGzip);
                    response.setHeaders(wContext.createHeader(null, WebResponseCodes.HTTP_NOT_FOUND, response.getBodySize(), 0L, "text/html", false, allowGzip));
                    response.setResponse(WebResponseCodes.HTTP_NOT_FOUND);
                    return response;
                }
                StringBuffer headers = wContext.createHeader(requestedDocument, WebResponseCodes.HTTP_OK, (int) sendFile.length(), sendFile.lastModified(), null, true, false);
                ByteBuffer headersBuf = ByteBuffer.allocateDirect(headers.length());
                ByteBuffer buf = ByteBuffer.allocateDirect(2);
                headersBuf.put(headers.toString().getBytes());
                headersBuf.flip();
                buf.put(new String("\r\n").getBytes());
                buf.flip();
                SocketTools.sendBufferToSocket(headersBuf, wContext.getClientContext().getSocketChannel());
                SocketTools.sendBufferToSocket(buf, wContext.getClientContext().getSocketChannel());
                try {
                    Objects.free(headersBuf);
                } catch (Exception e) {
                    Debug.inform("Unable to free headers buffer: " + e.getMessage());
                }
                try {
                    Objects.free(buf);
                } catch (Exception e) {
                    Debug.inform("Unable to free data buffer: " + e.getMessage());
                }
                try {
                    RequestLogger.getDefault().log(wContext, WebResponseCodes.HTTP_OK, (int) roChannel.size());
                    if (wContext.getLabel() != null) {
                        BandwidthCache.getDefault().add(wContext.getLabel(), (int) roChannel.size());
                    }
                } catch (Exception e) {
                    Debug.inform("Unable to log request: " + e);
                }
                ByteBuffer roBuf = null;
                int fileLength = 0, sendChunks = Server.getMapBufferSize(), sendBlocks = Server.getMapBufferSize();
                try {
                    fileLength = (int) roChannel.size();
                } catch (IOException e) {
                    Debug.inform("Unable to get file length: " + e.getMessage());
                }
                if (sendChunks > fileLength) {
                    sendChunks = fileLength;
                }
                for (int position = 0; position < fileLength; ) {
                    if ((position + sendChunks) > fileLength) {
                        sendChunks = (fileLength - position);
                    }
                    try {
                        roBuf = roChannel.map(FileChannel.MapMode.READ_ONLY, position, sendChunks);
                    } catch (IOException e) {
                        break;
                    }
                    try {
                        SocketTools.sendBufferToSocket(roBuf, wContext.getClientContext().getSocketChannel());
                    } catch (KeepaliveException e) {
                        break;
                    }
                    try {
                        Objects.free(roBuf);
                    } catch (Exception e) {
                        Debug.inform("Unable to free memory mapped object: " + e.getMessage() + " (" + e + ")");
                    }
                    position += sendBlocks;
                }
                try {
                    Objects.free(roBuf);
                } catch (Exception e) {
                    Debug.inform("Unable to free memory mapped object: " + e.getMessage() + " (" + e + ")");
                }
                try {
                    roChannel.close();
                } catch (Exception e) {
                    Debug.debug("Unable to close mapped file: " + e);
                }
                headersBuf = null;
                buf = null;
                roBuf = null;
                roChannel = null;
                return null;
            }
        }
    }

    /**
     * Handles the display of the files specified directory.
     *
     * @param requestDocument The directory containing the list of files to use.
     * @param renderAction The action to perform.
     * @return <code>ResponseBuffer</code> containing the result.
     */
    public ResponseBuffer handleDirectoryRequest(String requestDocument, int renderAction) {
        ResponseBuffer response = new ResponseBuffer();
        StringBuffer msg = new StringBuffer();
        StringBuffer header;
        boolean hasPreviousPath = false, showHidden = true, allowGzip = wContext.getClientContext().getAllowGzip();
        String path = Encoder.URLDecode(requestDocument);
        String prevPath = null;
        String hiddenString = NodeUtil.walkNodeTree(Server.getConfig(), "//configuration/object[@type='engine.tunable']/property[@type='engine.showhiddenfiles']/@value");
        String docRoot = wContext.getDocRoot();
        long lastModified = 0L;
        int docRootLength = docRoot.length();
        Debug.debug("Handling directory request of '" + requestDocument + "' renderAction='" + renderAction + "'");
        if (hiddenString != null && hiddenString.equalsIgnoreCase("true")) {
            showHidden = true;
        } else {
            showHidden = false;
        }
        path = path.replace('\\', '/');
        prevPath = new String(path);
        if (prevPath.endsWith("/")) {
            prevPath = prevPath.substring(0, prevPath.length() - 1);
        }
        int prevPathLast = prevPath.lastIndexOf("/");
        if ((prevPathLast != -1 && !prevPath.equals("/")) || (prevPath.length() > 1)) {
            if (prevPathLast != -1) {
                prevPath = prevPath.substring(0, prevPathLast);
            } else {
                prevPath = "/";
            }
            hasPreviousPath = true;
        } else {
            hasPreviousPath = false;
        }
        if (prevPath.equals("")) {
            prevPath = "/";
        }
        if (!path.endsWith("/")) {
            StringBuffer body = WebResponseCodes.createGenericResponse(wContext, WebResponseCodes.HTTP_MOVED_PERM, "Document moved to '" + path + "/'");
            response.setBody(body, allowGzip);
            StringBuffer myHeader = wContext.createHeader(null, WebResponseCodes.HTTP_MOVED_PERM, response.getBodySize(), 0L, "text/html", false, allowGzip);
            myHeader.append("Location: ");
            myHeader.append(path);
            myHeader.append('/');
            myHeader.append("\r\n");
            response.setHeaders(myHeader);
            response.setResponse(WebResponseCodes.HTTP_MOVED_PERM);
            return response;
        }
        ArrayList filterList = wContext.getVendContext().getVend().getFilter(wContext.getClientContext().getMatchedHost());
        int filterListSize = filterList.size();
        for (int fPos = 0; fPos < filterListSize; fPos++) {
            String filter = (String) filterList.get(fPos);
            if (filter.startsWith("/")) {
                filter = filter.substring(1);
            }
            if (path.indexOf("/" + filter + "/") != -1 || path.indexOf("/" + filter) != -1 || path.endsWith(filter)) {
                StringBuffer body = WebResponseCodes.createGenericResponse(wContext, WebResponseCodes.HTTP_NOT_FOUND, "The requested resource '" + path + "' was not found on this server.");
                response.setBody(body, allowGzip);
                response.setHeaders(wContext.createHeader(null, WebResponseCodes.HTTP_NOT_FOUND, response.getBodySize(), 0L, "text/html", false, allowGzip));
                response.setResponse(WebResponseCodes.HTTP_NOT_FOUND);
                return response;
            }
        }
        if (path.indexOf("/.proc/") != -1 || path.indexOf("/.proc") != -1 || path.indexOf("/procedures/") != -1 || path.indexOf("/procedures") != -1) {
            StringBuffer body = WebResponseCodes.createGenericResponse(wContext, WebResponseCodes.HTTP_NOT_FOUND, "The requested resource '" + path + "' was not found on this server.");
            response.setBody(body, allowGzip);
            response.setHeaders(wContext.createHeader(null, WebResponseCodes.HTTP_NOT_FOUND, response.getBodySize(), 0L, "text/html", false, allowGzip));
            response.setResponse(WebResponseCodes.HTTP_NOT_FOUND);
            return response;
        }
        if (renderAction == ClientHandler.PAGE_IS_DIRECTORY) {
            File files = new File(docRoot + path);
            boolean canListDirs = false, toggle = false;
            canListDirs = DirListAccess.getDefault().hasDirList(wContext.getClientContext().getMatchedHost(), false);
            if (files.isDirectory()) {
                if (canListDirs) {
                    File fileList[] = files.listFiles();
                    RenderEngine engine = new RenderEngine(wContext);
                    Arrays.sort(fileList, new FileSorter());
                    if (fileList != null || fileList.length > 0) {
                        int counter = 0;
                        for (int i = 0; i < fileList.length; i++) {
                            String filename;
                            String displayFilename;
                            String fileInfo = "";
                            String toggleMessage = "";
                            filename = fileList[i].toString();
                            filename = filename.replace('\\', '/');
                            filename = filename.substring(docRootLength);
                            displayFilename = filename;
                            displayFilename = displayFilename.substring(displayFilename.lastIndexOf("/") + 1);
                            boolean displayable = true;
                            for (int fPos = 0; fPos < filterListSize; fPos++) {
                                String filter = (String) filterList.get(fPos);
                                if (filter.startsWith("/")) {
                                    filter = filter.substring(1);
                                }
                                if (displayFilename.indexOf("/" + filter + "/") != -1 || displayFilename.indexOf("/" + filter) != -1 || displayFilename.endsWith(filter)) {
                                    displayable = false;
                                    break;
                                }
                            }
                            fileInfo = "";
                            if (!displayable) {
                                continue;
                            }
                            if ((fileList[i].isHidden() || displayFilename.startsWith(".")) && !showHidden) {
                                continue;
                            }
                            counter++;
                            engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(link)", false, Encoder.StaticListerEncode(filename));
                            engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(name)", false, Encoder.XMLEncode(displayFilename));
                            if (fileList[i].isDirectory()) {
                                engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(desc)", false, "Directory");
                            } else {
                                engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(desc)", false, MimeTypes.getDefault().get(filename));
                            }
                            String fileType = "";
                            fileType += (fileList[i].isAbsolute()) ? "A" : "-";
                            fileType += (fileList[i].isDirectory()) ? "D" : "-";
                            fileType += (fileList[i].isFile()) ? "F" : "-";
                            fileType += (fileList[i].isHidden()) ? "H" : "-";
                            engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(type)", false, fileType);
                            Date myDate = new Date();
                            myDate.setTime(fileList[i].lastModified());
                            engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(size)", false, "" + fileList[i].length());
                            engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(mod)", false, myDate.toString());
                        }
                        engine.getVariableContainer().setVariable(engine, "files(count)", false, "" + counter);
                    }
                    StringBuffer ret = null;
                    engine.getVariableContainer().setVariable("FILEPATH", Encoder.StaticListerEncode(path));
                    engine.getVariableContainer().setVariable("PREVPATH", Encoder.StaticListerEncode(prevPath));
                    try {
                        String transformFile = NodeUtil.walkNodeTree(Server.getConfig(), "//configuration/object[@type='engine.template']/property[@type='engine.filelist']/@value");
                        String str[] = { "*" };
                        String sspExtensions[] = { "ssp" };
                        DocumentEngine docEngine = new DocumentEngine(wContext, transformFile, str, sspExtensions);
                        engine.setDocumentEngine(docEngine);
                        engine.getRenderContext().setCurrentDocroot(".");
                        ret = docEngine.render(engine);
                    } catch (Exception e) {
                        StringBuffer body = WebResponseCodes.createGenericResponse(wContext, WebResponseCodes.HTTP_INTERNAL_ERROR, "Directory contents listing failed:<p>" + e.getMessage() + "</p>\n");
                        response.setBody(body, allowGzip);
                        response.setHeaders(wContext.createHeader(null, WebResponseCodes.HTTP_INTERNAL_ERROR, response.getBodySize(), 0L, "text/html", false, allowGzip));
                        response.setResponse(WebResponseCodes.HTTP_INTERNAL_ERROR);
                        return response;
                    }
                    int responseLength = ret.length();
                    response.setBody(ret);
                    response.setHeaders(wContext.createHeader(null, 200, responseLength, 0L, "text/html", false, false));
                    response.setResponse(WebResponseCodes.HTTP_OK);
                    return response;
                }
            }
        } else if (renderAction == (ClientHandler.PAGE_IS_DIRECTORY + ClientHandler.PAGE_VFS_BASED)) {
            boolean isDir = false;
            try {
                isDir = wContext.getVendContext().getFileAccess().isDirectory(path, wContext.getClientContext().getMatchedHost());
            } catch (FileNotFoundException e) {
            }
            if (isDir) {
                boolean toggle = false;
                boolean canListDirs = DirListAccess.getDefault().hasDirList(wContext.getClientContext().getMatchedHost(), false);
                if (canListDirs) {
                    String trueRequest = null;
                    String vfsRoot = wContext.getVendContext().getFileAccess().getVFSMatch(path, wContext.getClientContext().getMatchedHost());
                    String vfsRequest = null;
                    String vfsPreviousRequest = null;
                    String toggleMessage = "";
                    RenderEngine engine = new RenderEngine(wContext);
                    int counter = 0;
                    if (!vfsRoot.endsWith("/")) {
                        vfsRoot += "/";
                    }
                    trueRequest = path;
                    vfsRequest = path.replaceFirst(vfsRoot, "");
                    vfsPreviousRequest = path;
                    if (!vfsPreviousRequest.startsWith("/")) {
                        vfsPreviousRequest = "/" + vfsPreviousRequest;
                    }
                    if (vfsPreviousRequest.endsWith("/") && !vfsPreviousRequest.equals("/")) {
                        vfsPreviousRequest = vfsPreviousRequest.substring(0, vfsPreviousRequest.length() - 1);
                    }
                    if (vfsPreviousRequest.indexOf("/") != -1 && !vfsPreviousRequest.equals("/")) {
                        vfsPreviousRequest = vfsPreviousRequest.substring(0, vfsPreviousRequest.lastIndexOf("/"));
                    }
                    if (!vfsPreviousRequest.endsWith("/") && !vfsPreviousRequest.equals("/")) {
                        vfsPreviousRequest += "/";
                    }
                    Debug.debug("Directory root from VFS='" + vfsRoot + "', is now '" + vfsRequest + "'");
                    msg = new StringBuffer();
                    engine.getVariableContainer().setVariable("FILEPATH", Encoder.StaticListerEncode(path));
                    engine.getVariableContainer().setVariable("PREVPATH", Encoder.StaticListerEncode(vfsPreviousRequest));
                    Vector listFiles = wContext.getVendContext().getFileAccess().listFiles(path, wContext.getClientContext().getMatchedHost());
                    if (listFiles != null) {
                        Object[] list = listFiles.toArray();
                        if (wContext.getVendContext().getFileAccess().getVFSType(path, wContext.getClientContext().getMatchedHost()) == FileAccess.TYPE_JAR) {
                            for (int i = 0; i < list.length; i++) {
                                ZipEntry zEntry = (ZipEntry) list[i];
                                String filename = zEntry.getName();
                                if (filename.startsWith(vfsRequest)) {
                                    String displayFilename = filename.replaceFirst(vfsRequest, "");
                                    int depth = 0;
                                    int displayFilenameLength = displayFilename.length();
                                    for (int x = 0; x < displayFilenameLength; x++) {
                                        if (displayFilename.charAt(x) == '/') {
                                            depth++;
                                        }
                                    }
                                    if (displayFilename.startsWith(".") && !showHidden) {
                                        continue;
                                    }
                                    boolean displayable = true;
                                    for (int fPos = 0; fPos < filterListSize; fPos++) {
                                        String filter = (String) filterList.get(fPos);
                                        if (filter.startsWith("/")) {
                                            filter = filter.substring(1);
                                        }
                                        if (displayFilename.indexOf("/" + filter + "/") != -1 || displayFilename.indexOf("/" + filter) != -1 || displayFilename.endsWith(filter)) {
                                            displayable = false;
                                            break;
                                        }
                                    }
                                    if (!displayable) {
                                        continue;
                                    }
                                    if (depth == 0 && !displayFilename.equals("")) {
                                        counter++;
                                        Date myDate = new Date();
                                        myDate.setTime(zEntry.getTime());
                                        engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(link)", false, Encoder.StaticListerEncode(vfsRoot) + Encoder.StaticListerEncode(filename));
                                        engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(name)", false, Encoder.XMLEncode(displayFilename));
                                        engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(desc)", false, MimeTypes.getDefault().get(filename));
                                        engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(type)", false, "--F-");
                                        engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(size)", false, "" + zEntry.getSize());
                                        engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(mod)", false, myDate.toString());
                                    } else if (depth == 1 && zEntry.isDirectory()) {
                                        displayFilename = displayFilename.substring(0, displayFilename.length() - 1);
                                        counter++;
                                        Date myDate = new Date();
                                        myDate.setTime(zEntry.getTime());
                                        engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(link)", false, Encoder.StaticListerEncode(vfsRoot) + Encoder.StaticListerEncode(filename));
                                        engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(name)", false, Encoder.XMLEncode(displayFilename));
                                        engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(desc)", false, MimeTypes.getDefault().get(filename));
                                        engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(type)", false, "-D--");
                                        engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(size)", false, "" + zEntry.getSize());
                                        engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(mod)", false, myDate.toString());
                                    }
                                }
                            }
                        } else if (wContext.getVendContext().getFileAccess().getVFSType(path, wContext.getClientContext().getMatchedHost()) == FileAccess.TYPE_FS) {
                            File[] filesorted = new File[list.length];
                            for (int i = 0; i < list.length; i++) {
                                filesorted[i] = (File) list[i];
                            }
                            Arrays.sort(filesorted, new FileSorter());
                            for (int i = 0; i < filesorted.length; i++) {
                                File zEntry = (File) filesorted[i];
                                String filename = zEntry.getName();
                                String displayFilename = filename.replaceFirst(vfsRequest, "");
                                int depth = 0;
                                for (int x = 0; x < displayFilename.length(); x++) {
                                    if (displayFilename.charAt(x) == '/') {
                                        depth++;
                                    }
                                }
                                if (depth == 0 && !displayFilename.equals("")) {
                                    if ((zEntry.isHidden() || displayFilename.startsWith(".")) && !showHidden) {
                                        continue;
                                    }
                                    boolean displayable = true;
                                    for (int fPos = 0; fPos < filterListSize; fPos++) {
                                        String filter = (String) filterList.get(fPos);
                                        if (filter.startsWith("/")) {
                                            filter = filter.substring(1);
                                        }
                                        if (displayFilename.indexOf("/" + filter + "/") != -1 || displayFilename.indexOf("/" + filter) != -1 || displayFilename.endsWith(filter)) {
                                            displayable = false;
                                            break;
                                        }
                                    }
                                    if (!displayable) {
                                        continue;
                                    }
                                    counter++;
                                    engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(link)", false, Encoder.StaticListerEncode(path) + Encoder.StaticListerEncode(filename));
                                    engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(name)", false, Encoder.XMLEncode(displayFilename));
                                    if (zEntry.isDirectory()) {
                                        engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(desc)", false, "Directory");
                                    } else {
                                        engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(desc)", false, MimeTypes.getDefault().get(filename));
                                    }
                                    String fileType = "";
                                    fileType += (zEntry.isAbsolute()) ? "A" : "-";
                                    fileType += (zEntry.isDirectory()) ? "D" : "-";
                                    fileType += (zEntry.isFile()) ? "F" : "-";
                                    fileType += (zEntry.isHidden()) ? "H" : "-";
                                    engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(type)", false, fileType);
                                    if (zEntry.isDirectory()) {
                                        engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(size)", false, "0");
                                    } else {
                                        engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(size)", false, "" + zEntry.length());
                                    }
                                    Date myDate = new Date();
                                    myDate.setTime(zEntry.lastModified());
                                    engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(mod)", false, myDate.toString());
                                } else if (zEntry.isDirectory()) {
                                    displayFilename = displayFilename.substring(0, displayFilename.length() - 1);
                                    if ((zEntry.isHidden() || displayFilename.startsWith(".")) && !showHidden) {
                                        continue;
                                    }
                                    counter++;
                                    engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(link)", false, Encoder.StaticListerEncode(path) + Encoder.StaticListerEncode(filename));
                                    engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(name)", false, Encoder.XMLEncode(displayFilename));
                                    if (zEntry.isDirectory()) {
                                        engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(desc)", false, "Directory");
                                    } else {
                                        engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(desc)", false, MimeTypes.getDefault().get(filename));
                                    }
                                    String fileType = "";
                                    fileType += (zEntry.isAbsolute()) ? "A" : "-";
                                    fileType += (zEntry.isDirectory()) ? "D" : "-";
                                    fileType += (zEntry.isFile()) ? "F" : "-";
                                    fileType += (zEntry.isHidden()) ? "H" : "-";
                                    engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(type)", false, fileType);
                                    if (zEntry.isDirectory()) {
                                        engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(size)", false, "0");
                                    } else {
                                        engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(size)", false, "" + zEntry.length());
                                    }
                                    Date myDate = new Date();
                                    myDate.setTime(zEntry.lastModified());
                                    engine.getVariableContainer().setVariable(engine, "files(" + counter + ")(mod)", false, myDate.toString());
                                }
                            }
                        }
                        engine.getVariableContainer().setVariable(engine, "files(count)", false, "" + counter);
                    }
                    StringBuffer ret = null;
                    try {
                        String transformFile = NodeUtil.walkNodeTree(Server.getConfig(), "//configuration/object[@type='engine.template']/property[@type='engine.filelist']/@value");
                        String str[] = { "*" };
                        String sspExtensions[] = { "ssp" };
                        DocumentEngine docEngine = new DocumentEngine(wContext, transformFile, str, sspExtensions);
                        engine.setDocumentEngine(docEngine);
                        engine.getRenderContext().setCurrentDocroot(".");
                        ret = docEngine.render(engine);
                    } catch (Exception e) {
                        StringBuffer body = WebResponseCodes.createGenericResponse(wContext, WebResponseCodes.HTTP_INTERNAL_ERROR, "Directory contents listing failed:<p>" + e.getMessage() + "</p>\n");
                        response.setBody(body, allowGzip);
                        response.setHeaders(wContext.createHeader(null, WebResponseCodes.HTTP_INTERNAL_ERROR, response.getBodySize(), 0L, "text/html", false, allowGzip));
                        response.setResponse(WebResponseCodes.HTTP_INTERNAL_ERROR);
                        return response;
                    }
                    int responseLength = ret.length();
                    response.setBody(ret);
                    response.setHeaders(wContext.createHeader(null, 200, responseLength, 0L, "text/html", false, false));
                    response.setResponse(WebResponseCodes.HTTP_OK);
                    return response;
                }
            }
            return response;
        }
        return response;
    }
}
