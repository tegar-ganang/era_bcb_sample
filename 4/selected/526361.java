package org.jaffa.applications.jaffa.modules.admin.components.fileexplorer.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

/** Servlet needed if you want to use the FileExplorer to download file
 * <p>Include in web.xml with the following configuration
<code><pre>
&lt;servlet&gt;
&lt;servlet-name&gt;FileExplorerDownload&lt;/servlet-name&gt;
&lt;servlet-class&gt;org.jaffa.applications.jaffa.modules.admin.components.fileexplorer.ui.FileExplorerDownload&lt;/servlet-class&gt;
&lt;/servlet&gt;

&lt;servlet-mapping&gt;
&lt;servlet-name&gt;FileExplorerDownload&lt;/servlet-name&gt;
&lt;url-pattern&gt;/jaffa/admin/fileexplorer/download&lt;/url-pattern&gt;
&lt;/servlet-mapping&gt;
</pre><code>
 *
 * @author PaulE
 * @version 1.0
 */
public class FileExplorerDownload extends HttpServlet {

    private static Logger log = Logger.getLogger(FileExplorerDownload.class);

    public static final String SESSION_ATTR_FILE_PREFIX = "org.jaffa.applications.jaffa.modules.admin.components.fileexplorer.";

    private static final String PARAM_UUID = "UUID";

    private static final String ATTR_FILE = "org.jaffa.applications.jaffa.modules.admin.components.fileexplorer";

    private static final String ATTR_FILE_NAME = "org.jaffa.applications.jaffa.modules.admin.components.fileexplorer.fileName";

    private static final String ATTR_CONTENT_TYPE = "org.jaffa.applications.jaffa.modules.admin.components.fileexplorer.contentType";

    protected static final Map c_fileTypes = new HashMap();

    static {
        c_fileTypes.put("csv", "application/vnd.ms-excel ");
        c_fileTypes.put("doc", "application/msword");
        c_fileTypes.put("htm", "text/html");
        c_fileTypes.put("html", "text/html");
        c_fileTypes.put("pdf", "application/pdf");
        c_fileTypes.put("ppt", "application/vnd.ms-powerpoint");
        c_fileTypes.put("txt", "text/plain");
        c_fileTypes.put("xls", "application/vnd.ms-excel ");
        c_fileTypes.put("xml", "text/xml");
        c_fileTypes.put("zip", "application/zip");
    }

    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
        File file;
        String fileName;
        String contentType;
        String UUID = request.getParameter(PARAM_UUID);
        if (UUID != null) {
            String sessionAttribute = SESSION_ATTR_FILE_PREFIX + UUID;
            file = (File) request.getSession().getAttribute(sessionAttribute);
            request.getSession().removeAttribute(sessionAttribute);
            if (file == null || !file.exists()) {
                String s = "Download of file " + (file == null ? "NULL" : file.getAbsolutePath()) + " failed.";
                log.error(s);
                throw new ServletException(s);
            }
            if (log.isDebugEnabled()) log.debug("Downloading file cached in the session: " + file.getAbsolutePath());
            fileName = file.getName();
            contentType = determineContentType(fileName);
        } else {
            file = (File) request.getAttribute(ATTR_FILE);
            request.removeAttribute(ATTR_FILE);
            if (file == null || !file.exists()) {
                String s = "Download of file " + (file == null ? "NULL" : file.getAbsolutePath()) + " failed.";
                log.error(s);
                throw new ServletException(s);
            }
            if (log.isDebugEnabled()) log.debug("Downloading file " + file.getAbsolutePath());
            fileName = (String) request.getAttribute(ATTR_FILE_NAME);
            request.removeAttribute(ATTR_FILE_NAME);
            if (fileName != null) {
                int pos = fileName.lastIndexOf('/');
                if (pos >= 0) fileName = fileName.substring(pos + 1); else {
                    pos = fileName.lastIndexOf('\\');
                    if (pos >= 0) fileName = fileName.substring(pos + 1);
                }
            } else fileName = file.getName();
            contentType = (String) request.getAttribute(ATTR_CONTENT_TYPE);
            request.removeAttribute(ATTR_CONTENT_TYPE);
            if (contentType == null) determineContentType(fileName);
        }
        if (log.isDebugEnabled()) {
            log.debug("Downloading file as " + fileName);
            log.debug("ContentType is " + contentType);
        }
        response.setContentType(contentType);
        try {
            response.setBufferSize(2048);
        } catch (IllegalStateException e) {
        }
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        InputStream in = new FileInputStream(file);
        ServletOutputStream out = response.getOutputStream();
        try {
            byte[] buffer = new byte[1000];
            while (in.available() > 0) out.write(buffer, 0, in.read(buffer));
            out.flush();
        } catch (IOException e) {
            log.error("Problem Serving Resource " + file.getAbsolutePath(), e);
        } finally {
            out.close();
            in.close();
        }
        if (log.isDebugEnabled()) log.debug("Downloaded file " + file.getAbsolutePath());
    }

    /** Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
        processRequest(request, response);
    }

    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
        processRequest(request, response);
    }

    /** determines the content type based on the input fileName. */
    private String determineContentType(String fileName) {
        String contentType = getServletConfig().getServletContext().getMimeType(fileName);
        if (contentType == null) {
            int pos = fileName.lastIndexOf(".");
            if (pos >= 0) contentType = (String) c_fileTypes.get(fileName.substring(pos + 1));
            if (contentType == null) contentType = "application/x-download";
        }
        return contentType;
    }
}
