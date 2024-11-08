package gwtupload.server;

import static gwtupload.shared.UConsts.*;
import gwtupload.server.exceptions.UploadCanceledException;
import gwtupload.server.exceptions.UploadException;
import gwtupload.server.exceptions.UploadSizeLimitException;
import gwtupload.server.exceptions.UploadTimeoutException;
import gwtupload.shared.UConsts;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Vector;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadBase.SizeLimitExceededException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;

/**
 * <p>
 * Upload servlet for the GwtUpload library.
 * </p>
 * 
 * <ul>
 * <li>For customizable application actions, it's better to extend the UloadAction
 * class instead of this.</li>
 * 
 * <li>
 * This servlet supports to be deployed in google application engine. It is able to
 * detect this environment and in this case it does:
 * <ul>
 * <li>Set the request size to 512 KB which is the maximal size allowed</li>
 * <li>Store received data in memory and cache instead of file system</li>
 * <li>Uses memcache for session tracking instead of normal session objects, 
 * because objects stored in session seem not to be available until the thread finishes</li>
 * </ul>
 * </li>
 * </ul>
 * 
 * 
 * <p>
 * <b>Example of web.xml</b>
 * </p>
 * 
 * <pre>
 * &lt;context-param&gt;
 *     &lt;!-- max size of the upload request --&gt;
 *     &lt;param-name&gt;maxSize&lt;/param-name&gt;
 *     &lt;param-value&gt;3145728&lt;/param-value&gt;
 *   &lt;/context-param&gt;
 *   
 *   &lt;context-param&gt;
 *     &lt;!-- useful in development mode to see the upload progress bar in fast networks. (sleep time in milliseconds) --&gt;
 *     &lt;param-name&gt;slowUploads&lt;/param-name&gt;
 *     &lt;param-value&gt;200&lt;/param-value&gt;
 *   &lt;/context-param&gt;
 * 
 *   &lt;servlet&gt;
 *     &lt;servlet-name&gt;uploadServlet&lt;/servlet-name&gt;
 *     &lt;servlet-class&gt;gwtupload.server.UploadServlet&lt;/servlet-class&gt;
 *   &lt;/servlet&gt;
 *   
 *   &lt;servlet-mapping&gt;
 *     &lt;servlet-name&gt;uploadServlet&lt;/servlet-name&gt;
 *     &lt;url-pattern&gt;*.gupld&lt;/url-pattern&gt;
 *   &lt;/servlet-mapping&gt;
 * 
 * 
 * </pre>
 * 
 * @author Manolo Carrasco Moñino
 * 
 */
public class UploadServlet extends HttpServlet implements Servlet {

    protected static final String SESSION_FILES = "FILES";

    protected static final String SESSION_LAST_FILES = "LAST_FILES";

    protected static final int DEFAULT_REQUEST_LIMIT_KB = 5 * 1024 * 1024;

    protected static final int DEFAULT_SLOW_DELAY_MILLIS = 300;

    protected static final String XML_CANCELED_TRUE = "<" + TAG_CANCELED + ">true</" + TAG_CANCELED + ">";

    protected static final String XML_DELETED_TRUE = "<" + TAG_DELETED + ">true</" + TAG_DELETED + ">";

    protected static final String XML_ERROR_ITEM_NOT_FOUND = "<" + TAG_ERROR + ">item not found</" + TAG_ERROR + ">";

    protected static final String XML_ERROR_TIMEOUT = "<" + TAG_ERROR + ">timeout receiving file</" + TAG_ERROR + ">";

    protected static final String XML_FINISHED_OK = "<" + TAG_FINISHED + ">OK</" + TAG_FINISHED + ">";

    protected static UploadLogger logger = UploadLogger.getLogger(UploadServlet.class);

    protected static final ThreadLocal<HttpServletRequest> perThreadRequest = new ThreadLocal<HttpServletRequest>();

    private static boolean appEngine = false;

    private static final long serialVersionUID = 2740693677625051632L;

    private static String XML_TPL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<response>%%MESSAGE%%</response>\n";

    /**
   * Copy the content of an input stream to an output one.
   * 
   * @param in
   * @param out
   * @throws IOException
   */
    public static void copyFromInputStreamToOutputStream(InputStream in, OutputStream out) throws IOException {
        IOUtils.copy(in, out);
    }

    /**
   * Utility method to get a fileItem of type file from a vector using either 
   * the file name or the attribute name.
   * 
   * @param sessionFiles
   * @param parameter 
   * @return fileItem of the file found or null
   */
    public static FileItem findFileItem(List<FileItem> sessionFiles, String parameter) {
        if (sessionFiles == null || parameter == null) {
            return null;
        }
        FileItem item = findItemByFieldName(sessionFiles, parameter);
        if (item == null) {
            item = findItemByFileName(sessionFiles, parameter);
        }
        if (item != null && !item.isFormField()) {
            return item;
        }
        return null;
    }

    /**
   * Utility method to get a fileItem from a vector using the attribute name.
   * 
   * @param sessionFiles
   * @param attrName
   * @return fileItem found or null
   */
    public static FileItem findItemByFieldName(List<FileItem> sessionFiles, String attrName) {
        if (sessionFiles != null) {
            for (FileItem fileItem : sessionFiles) {
                if (fileItem.getFieldName().equalsIgnoreCase(attrName)) {
                    return fileItem;
                }
            }
        }
        return null;
    }

    /**
   * Utility method to get a fileItem from a vector using the file name It
   * only returns items of type file.
   * 
   * @param sessionFiles
   * @param fileName
   * @return fileItem of the file found or null
   */
    public static FileItem findItemByFileName(List<FileItem> sessionFiles, String fileName) {
        if (sessionFiles != null) {
            for (FileItem fileItem : sessionFiles) {
                if (fileItem.isFormField() == false && fileItem.getName().equalsIgnoreCase(fileName)) {
                    return fileItem;
                }
            }
        }
        return null;
    }

    /**
   * Return the list of FileItems stored in session.
   * 
   * @param request
   * @return FileItems stored in session
   */
    @SuppressWarnings("unchecked")
    public static List<FileItem> getSessionFileItems(HttpServletRequest request) {
        return (Vector<FileItem>) request.getSession().getAttribute(SESSION_FILES);
    }

    /**
   * @deprecated use getSessionFileItems
   */
    public static List<FileItem> getSessionItems(HttpServletRequest request) {
        return getSessionFileItems(request);
    }

    /**
   * Returns the localized text of a key.
   */
    public static String getMessage(String key, Object... pars) {
        ResourceBundle res = ResourceBundle.getBundle(UploadServlet.class.getName(), getThreadLocalRequest().getLocale());
        return new MessageFormat(res.getString(key), getThreadLocalRequest().getLocale()).format(pars);
    }

    public static final HttpServletRequest getThreadLocalRequest() {
        return perThreadRequest.get();
    }

    /**
   * Just a method to detect whether the web container is running with appengine
   * restrictions.
   *  
   * @return true if the case of the application is running in appengine
   */
    public boolean isAppEngine() {
        return appEngine;
    }

    /**
   * Removes all FileItems stored in session and the temporary data.
   * 
   * @param request
   */
    public static void removeSessionFileItems(HttpServletRequest request) {
        removeSessionFileItems(request, true);
    }

    /**
   * Removes all FileItems stored in session, but in this case 
   * the user can specify whether the temporary data is removed from disk.
   * 
   * @param request
   * @param removeData 
   *                    true: the file data is deleted.
   *                    false: use it when you are referencing file items 
   *                    instead of copying them.
   */
    public static void removeSessionFileItems(HttpServletRequest request, boolean removeData) {
        logger.debug("UPLOAD-SERVLET (" + request.getSession().getId() + ") removeSessionFileItems: removeData=" + removeData);
        @SuppressWarnings("unchecked") Vector<FileItem> sessionFiles = (Vector<FileItem>) request.getSession().getAttribute(SESSION_FILES);
        if (removeData && sessionFiles != null) {
            for (FileItem fileItem : sessionFiles) {
                if (fileItem != null && !fileItem.isFormField()) {
                    fileItem.delete();
                }
            }
        }
        request.getSession().removeAttribute(SESSION_FILES);
    }

    /**
   * 
   * @deprecated use removeSessionFileItems
   */
    public static void removeSessionFiles(HttpServletRequest request) {
        removeSessionFileItems(request);
    }

    /**
   * Delete an uploaded file.
   * 
   * @param request
   * @param response
   * @return FileItem
   * @throws IOException
   */
    protected static FileItem removeUploadedFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String parameter = request.getParameter(UConsts.PARAM_REMOVE);
        FileItem item = findFileItem(getSessionFileItems(request), parameter);
        if (item != null) {
            getSessionFileItems(request).remove(item);
            renderXmlResponse(request, response, XML_DELETED_TRUE);
            logger.debug("UPLOAD-SERVLET (" + request.getSession().getId() + ") removeUploadedFile: " + parameter + " " + item.getName() + " " + item.getSize());
        } else {
            renderXmlResponse(request, response, XML_ERROR_ITEM_NOT_FOUND);
            logger.info("UPLOAD-SERVLET (" + request.getSession().getId() + ") removeUploadedFile: " + parameter + " unable to delete file because it isn't in session.");
        }
        return item;
    }

    /**
   * Writes a response to the client.
   */
    protected static void renderMessage(HttpServletResponse response, String message, String contentType) throws IOException {
        response.setContentType(contentType + "; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        PrintWriter out = response.getWriter();
        out.print(message);
        out.flush();
        out.close();
    }

    /**
   * Writes an XML response to the client.
   */
    protected void renderHtmlMessage(HttpServletResponse response, String message) throws IOException {
        renderMessage(response, message, "text/html");
    }

    /**
   * Writes a XML response to the client. 
   * The message must be a text which will be wrapped in an XML structure.
   * 
   * Note: if the request is a POST, the response should set the content type 
   *  to text/html or text/plain in order to be able in the client side to
   *  read the iframe body (submitCompletEvent.getResults()), otherwise the
   *  method returns null 
   * 
   * @param request
   * @param response
   * @param message
   * @param post
   *        specify whether the request is post or not.   
   * @throws IOException
   */
    protected static void renderXmlResponse(HttpServletRequest request, HttpServletResponse response, String message, boolean post) throws IOException {
        String contentType = post ? "text/plain" : "text/html";
        String ctype = request.getParameter(PARAM_CTYPE);
        if (ctype != null) {
            if ("xml".equalsIgnoreCase(ctype)) {
                contentType = "text/xml";
            } else if ("html".equalsIgnoreCase(ctype)) {
                contentType = "text/html";
            } else if ("text".equalsIgnoreCase(ctype)) {
                contentType = "text/plain";
            }
        }
        String xml = XML_TPL.replace("%%MESSAGE%%", message != null ? message : "");
        if (post) {
            xml = TAG_MSG_START + xml.replaceAll("<", TAG_MSG_LT).replaceAll(">", TAG_MSG_GT) + TAG_MSG_END;
        }
        renderMessage(response, xml, contentType);
    }

    protected static void renderXmlResponse(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {
        renderXmlResponse(request, response, message, false);
    }

    protected static void setThreadLocalRequest(HttpServletRequest request) {
        perThreadRequest.set(request);
    }

    /**
   * Simple method to get a string from the exception stack.
   * 
   * @param e
   * @return string
   */
    protected static String stackTraceToString(Exception e) {
        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));
        return writer.getBuffer().toString();
    }

    protected long maxSize = DEFAULT_REQUEST_LIMIT_KB;

    protected int uploadDelay = 0;

    protected boolean useBlobstore = false;

    /**
   * Mark the current upload process to be canceled.
   * 
   * @param request
   */
    public void cancelUpload(HttpServletRequest request) {
        logger.debug("UPLOAD-SERVLET (" + request.getSession().getId() + ") cancelling Upload");
        AbstractUploadListener listener = getCurrentListener(request);
        if (listener != null && !listener.isCanceled()) {
            listener.setException(new UploadCanceledException());
        }
    }

    /**
   * Override this method if you want to check the request before it is passed 
   * to commons-fileupload parser.
   * 
   * @param request
   * @throws RuntimeException
   */
    public void checkRequest(HttpServletRequest request) {
        logger.debug("UPLOAD-SERVLET (" + request.getSession().getId() + ") procesing a request with size: " + request.getContentLength() + " bytes.");
        if (request.getContentLength() > maxSize) {
            throw new UploadSizeLimitException(maxSize, request.getContentLength());
        }
    }

    /**
   * Get an uploaded file item.
   * 
   * @param request
   * @param response
   * @throws IOException
   */
    public void getUploadedFile(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String parameter = request.getParameter(UConsts.PARAM_SHOW);
        FileItem item = findFileItem(getSessionFileItems(request), parameter);
        if (item != null) {
            logger.debug("UPLOAD-SERVLET (" + request.getSession().getId() + ") getUploadedFile: " + parameter + " returning: " + item.getContentType() + ", " + item.getName() + ", " + item.getSize() + " bytes");
            response.setContentType(item.getContentType());
            copyFromInputStreamToOutputStream(item.getInputStream(), response.getOutputStream());
        } else {
            logger.info("UPLOAD-SERVLET (" + request.getSession().getId() + ") getUploadedFile: " + parameter + " file isn't in session.");
            renderXmlResponse(request, response, XML_ERROR_ITEM_NOT_FOUND);
        }
    }

    @Override
    public String getInitParameter(String name) {
        String value = getServletContext().getInitParameter(name);
        if (value == null) {
            value = super.getInitParameter(name);
        }
        return value;
    }

    /**
   * Read configurable parameters during the servlet initialization.
   */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String size = getInitParameter("maxSize");
        if (size != null) {
            try {
                maxSize = Long.parseLong(size);
            } catch (NumberFormatException e) {
            }
        }
        String slow = getInitParameter("slowUploads");
        if (slow != null) {
            if ("true".equalsIgnoreCase(slow)) {
                uploadDelay = DEFAULT_SLOW_DELAY_MILLIS;
            } else {
                try {
                    uploadDelay = Integer.valueOf(slow);
                } catch (NumberFormatException e) {
                }
            }
        }
        String timeout = getInitParameter("noDataTimeout");
        if (timeout != null) {
            try {
                UploadListener.setNoDataTimeout(Integer.parseInt(timeout));
            } catch (NumberFormatException e) {
            }
        }
        String appe = getInitParameter("appEngine");
        if (appe != null) {
            appEngine = "true".equalsIgnoreCase(appe);
        } else {
            appEngine = isAppEngine();
        }
        logger.info("UPLOAD-SERVLET init: maxSize=" + maxSize + ", slowUploads=" + slow + ", isAppEngine=" + isAppEngine());
    }

    /**
   * Create a new listener for this session.
   * 
   * @param request
   * @return the appropriate listener 
   */
    protected AbstractUploadListener createNewListener(HttpServletRequest request) {
        if (isAppEngine()) {
            return new MemoryUploadListener(uploadDelay, request.getContentLength());
        } else {
            return new UploadListener(uploadDelay, request.getContentLength());
        }
    }

    /**
   * The get method is used to monitor the uploading process or to get the
   * content of the uploaded files.
   */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        perThreadRequest.set(request);
        if (request.getParameter(UConsts.PARAM_SESSION) != null) {
            logger.debug("UPLOAD-SERVLET (" + request.getSession().getId() + ") new session, blobstore=" + (isAppEngine() && useBlobstore));
            request.getSession();
            renderXmlResponse(request, response, "<" + TAG_BLOBSTORE + ">" + (isAppEngine() && useBlobstore) + "</" + TAG_BLOBSTORE + ">");
        } else if (isAppEngine() && (request.getParameter(UConsts.PARAM_BLOBSTORE) != null || request.getParameterMap().size() == 0)) {
            String blobStorePath = getBlobstorePath(request);
            logger.debug("UPLOAD-SERVLET (" + request.getSession().getId() + ") getBlobstorePath=" + blobStorePath);
            renderXmlResponse(request, response, "<" + TAG_BLOBSTORE_PATH + ">" + blobStorePath + "</" + TAG_BLOBSTORE_PATH + ">");
        } else if (request.getParameter(UConsts.PARAM_SHOW) != null) {
            getUploadedFile(request, response);
        } else if (request.getParameter(UConsts.PARAM_CANCEL) != null) {
            cancelUpload(request);
            renderXmlResponse(request, response, XML_CANCELED_TRUE);
        } else if (request.getParameter(UConsts.PARAM_REMOVE) != null) {
            removeUploadedFile(request, response);
        } else if (request.getParameter(UConsts.PARAM_CLEAN) != null) {
            logger.debug("UPLOAD-SERVLET (" + request.getSession().getId() + ") cleanListener");
            AbstractUploadListener listener = getCurrentListener(request);
            if (listener != null) {
                listener.remove();
            }
            renderXmlResponse(request, response, XML_FINISHED_OK);
        } else {
            String message = statusToString(getUploadStatus(request, request.getParameter(UConsts.PARAM_FILENAME), null));
            renderXmlResponse(request, response, message);
        }
        perThreadRequest.set(null);
    }

    protected String statusToString(Map<String, String> status) {
        String message = "";
        for (Entry<String, String> e : status.entrySet()) {
            if (e.getValue() != null) {
                String k = e.getKey();
                String v = e.getValue().replaceAll("</*pre>", "").replaceAll("&lt;", "<").replaceAll("&gt;", ">");
                message += "<" + k + ">" + v + "</" + k + ">\n";
            }
        }
        return message;
    }

    /**
   * The post method is used to receive the file and save it in the user
   * session. It returns a very XML page that the client receives in an
   * iframe.
   * 
   * The content of this xml document has a tag error in the case of error in
   * the upload process or the string OK in the case of success.
   * 
   */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        perThreadRequest.set(request);
        String error;
        try {
            error = parsePostRequest(request, response);
            finish(request);
            Map<String, String> stat = new HashMap<String, String>();
            if (error != null && error.length() > 0) {
                stat.put(TAG_ERROR, error);
            } else {
                getFileItemsSummary(request, stat);
            }
            renderXmlResponse(request, response, statusToString(stat), true);
        } catch (UploadCanceledException e) {
            renderXmlResponse(request, response, XML_CANCELED_TRUE, true);
        } catch (UploadTimeoutException e) {
            renderXmlResponse(request, response, XML_ERROR_TIMEOUT, true);
        } catch (UploadSizeLimitException e) {
            renderXmlResponse(request, response, "<" + TAG_ERROR + ">" + e.getMessage() + "</" + TAG_ERROR + ">", true);
        } catch (Exception e) {
            logger.error("UPLOAD-SERVLET (" + request.getSession().getId() + ") Exception -> " + e.getMessage() + "\n" + stackTraceToString(e));
            error = e.getMessage();
            renderXmlResponse(request, response, "<" + TAG_ERROR + ">" + error + "</" + TAG_ERROR + ">", true);
        } finally {
            perThreadRequest.set(null);
        }
    }

    protected Map<String, String> getFileItemsSummary(HttpServletRequest request, Map<String, String> ret) {
        if (ret == null) {
            ret = new HashMap<String, String>();
        }
        @SuppressWarnings("unchecked") List<FileItem> s = (List<FileItem>) request.getSession().getAttribute(SESSION_LAST_FILES);
        if (s != null) {
            for (FileItem i : s) {
                if (false == i.isFormField()) {
                    ret.put("ctype", i.getContentType() != null ? i.getContentType() : "unknown");
                    ret.put("size", "" + i.getSize());
                    ret.put("name", "" + i.getName());
                    ret.put("field", "" + i.getFieldName());
                }
            }
            ret.put(TAG_FINISHED, "ok");
        }
        return ret;
    }

    /**
   * Notify to the listener that the upload has finished.
   * 
   * @param request
   */
    protected void finish(HttpServletRequest request) {
        AbstractUploadListener listener = getCurrentListener(request);
        if (listener != null) {
            listener.setFinished(true);
        }
    }

    protected String getBlobstorePath(HttpServletRequest request) {
        return null;
    }

    /**
   * Get the listener active in this session.
   * 
   * @param request
   * @return the listener active
   */
    protected AbstractUploadListener getCurrentListener(HttpServletRequest request) {
        if (isAppEngine()) {
            return MemoryUploadListener.current(request.getSession().getId());
        } else {
            return UploadListener.current(request);
        }
    }

    /**
   * Override this method if you want to implement a different ItemFactory.
   * 
   * @return FileItemFactory
   */
    protected FileItemFactory getFileItemFactory(int requestSize) {
        return new DiskFileItemFactory();
    }

    /**
   * Method executed each time the client asks the server for the progress status.
   * It uses the listener to generate the adequate response
   * 
   * @param request
   * @param fieldname
   * @return a map of tag/values to be rendered 
   */
    protected Map<String, String> getUploadStatus(HttpServletRequest request, String fieldname, Map<String, String> ret) {
        perThreadRequest.set(request);
        HttpSession session = request.getSession();
        if (ret == null) {
            ret = new HashMap<String, String>();
        }
        long currentBytes = 0;
        long totalBytes = 0;
        long percent = 0;
        AbstractUploadListener listener = getCurrentListener(request);
        if (listener != null) {
            if (listener.getException() != null) {
                if (listener.getException() instanceof UploadCanceledException) {
                    ret.put(TAG_CANCELED, "true");
                    ret.put(TAG_FINISHED, TAG_CANCELED);
                    logger.error("UPLOAD-SERVLET (" + session.getId() + ") getUploadStatus: " + fieldname + " canceled by the user after " + listener.getBytesRead() + " Bytes");
                } else {
                    String errorMsg = getMessage("server_error", listener.getException().getMessage());
                    ret.put(TAG_ERROR, errorMsg);
                    ret.put(TAG_FINISHED, TAG_ERROR);
                    logger.error("UPLOAD-SERVLET (" + session.getId() + ") getUploadStatus: " + fieldname + " finished with error: " + listener.getException().getMessage());
                }
            } else {
                currentBytes = listener.getBytesRead();
                totalBytes = listener.getContentLength();
                percent = totalBytes != 0 ? currentBytes * 100 / totalBytes : 0;
                ret.put("percent", "" + percent);
                ret.put("currentBytes", "" + currentBytes);
                ret.put("totalBytes", "" + totalBytes);
                if (listener.isFinished()) {
                    ret.put(TAG_FINISHED, "ok");
                }
            }
        } else if (getSessionFileItems(request) != null) {
            if (fieldname == null) {
                ret.put(TAG_FINISHED, "ok");
                logger.debug("UPLOAD-SERVLET (" + session.getId() + ") getUploadStatus: " + request.getQueryString() + " finished with files: " + session.getAttribute(SESSION_FILES));
            } else {
                Vector<FileItem> sessionFiles = (Vector<FileItem>) getSessionFileItems(request);
                for (FileItem file : sessionFiles) {
                    if (file.isFormField() == false && file.getFieldName().equals(fieldname)) {
                        ret.put(TAG_FINISHED, "ok");
                        ret.put(UConsts.PARAM_FILENAME, fieldname);
                        logger.debug("UPLOAD-SERVLET (" + session.getId() + ") getUploadStatus: " + fieldname + " finished with files: " + session.getAttribute(SESSION_FILES));
                    }
                }
            }
        } else {
            logger.debug("UPLOAD-SERVLET (" + session.getId() + ") getUploadStatus: no listener in session");
            ret.put("wait", "listener is null");
        }
        if (ret.containsKey(TAG_FINISHED)) {
            removeCurrentListener(request);
        }
        perThreadRequest.set(null);
        return ret;
    }

    /**
   * This method parses the submit action, puts in session a listener where the
   * progress status is updated, and eventually stores the received data in
   * the user session.
   * 
   * returns null in the case of success or a string with the error
   * 
   */
    @SuppressWarnings("unchecked")
    protected String parsePostRequest(HttpServletRequest request, HttpServletResponse response) {
        try {
            String delay = request.getParameter(PARAM_DELAY);
            uploadDelay = Integer.parseInt(delay);
        } catch (Exception e) {
        }
        HttpSession session = request.getSession();
        logger.debug("UPLOAD-SERVLET (" + session.getId() + ") new upload request received.");
        AbstractUploadListener listener = getCurrentListener(request);
        if (listener != null) {
            if (listener.isFrozen() || listener.isCanceled() || listener.getPercent() >= 100) {
                removeCurrentListener(request);
            } else {
                String error = getMessage("busy");
                logger.error("UPLOAD-SERVLET (" + session.getId() + ") " + error);
                return error;
            }
        }
        listener = createNewListener(request);
        List<FileItem> uploadedItems;
        try {
            checkRequest(request);
            FileItemFactory factory = getFileItemFactory(request.getContentLength());
            ServletFileUpload uploader = new ServletFileUpload(factory);
            uploader.setSizeMax(maxSize);
            uploader.setProgressListener(listener);
            logger.debug("UPLOAD-SERVLET (" + session.getId() + ") parsing HTTP POST request ");
            uploadedItems = uploader.parseRequest(request);
            logger.debug("UPLOAD-SERVLET (" + session.getId() + ") parsed request, " + uploadedItems.size() + " items received.");
            Vector<FileItem> sessionFiles = (Vector<FileItem>) getSessionFileItems(request);
            if (sessionFiles == null) {
                sessionFiles = new Vector<FileItem>();
            }
            String error = "";
            session.setAttribute(SESSION_LAST_FILES, uploadedItems);
            if (uploadedItems.size() > 0) {
                sessionFiles.addAll(uploadedItems);
                String msg = "";
                for (FileItem i : sessionFiles) {
                    msg += i.getFieldName() + " => " + i.getName() + "(" + i.getSize() + " bytes),";
                }
                logger.debug("UPLOAD-SERVLET (" + session.getId() + ") puting items in session: " + msg);
                session.setAttribute(SESSION_FILES, sessionFiles);
            } else {
                logger.error("UPLOAD-SERVLET (" + session.getId() + ") error NO DATA received ");
                error += getMessage("no_data");
            }
            return error.length() > 0 ? error : null;
        } catch (SizeLimitExceededException e) {
            RuntimeException ex = new UploadSizeLimitException(e.getPermittedSize(), e.getActualSize());
            listener.setException(ex);
            throw ex;
        } catch (UploadSizeLimitException e) {
            listener.setException(e);
            throw e;
        } catch (UploadCanceledException e) {
            listener.setException(e);
            throw e;
        } catch (UploadTimeoutException e) {
            listener.setException(e);
            throw e;
        } catch (Exception e) {
            logger.error("UPLOAD-SERVLET (" + request.getSession().getId() + ") Unexpected Exception -> " + e.getMessage() + "\n" + stackTraceToString(e));
            e.printStackTrace();
            RuntimeException ex = new UploadException(e);
            listener.setException(ex);
            throw ex;
        }
    }

    /**
   * Remove the listener active in this session.
   * 
   * @param request
   */
    protected void removeCurrentListener(HttpServletRequest request) {
        AbstractUploadListener listener = getCurrentListener(request);
        if (listener != null) {
            listener.remove();
        }
    }
}
