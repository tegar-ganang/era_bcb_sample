package org.apache.roller.presentation.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.Date;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.model.RollerFactory;

/**
 * Resources servlet.  Acts as a gateway to files uploaded by users.
 *
 * Since we keep uploaded resources in a location outside of the webapp
 * context we need a way to serve them up.  This servlet assumes that
 * resources are stored on a filesystem in the "uploads.dir" directory.
 *
 * @author Allen Gilliland
 *
 * @web.servlet name="ResourcesServlet"
 * @web.servlet-mapping url-pattern="/resources/*"
 */
public class ResourceServlet extends HttpServlet {

    private static Log mLogger = LogFactory.getLog(ResourceServlet.class);

    private String upload_dir = null;

    private ServletContext context = null;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.context = config.getServletContext();
        try {
            this.upload_dir = RollerFactory.getRoller().getFileManager().getUploadDir();
            mLogger.debug("upload dir is [" + this.upload_dir + "]");
        } catch (Exception e) {
            mLogger.warn(e);
        }
    }

    /** 
     * Handles requests for user uploaded resources.
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String context = request.getContextPath();
        String servlet = request.getServletPath();
        String reqURI = request.getRequestURI();
        reqURI = reqURI.replaceAll("\\+", "%2B");
        reqURI = URLDecoder.decode(reqURI, "UTF-8");
        String reqResource = reqURI.substring(servlet.length() + context.length());
        String resource_path = this.upload_dir + reqResource;
        File resource = new File(resource_path);
        mLogger.debug("Resource requested [" + reqURI + "]");
        mLogger.debug("Real path is [" + resource.getAbsolutePath() + "]");
        if (!resource.exists() || !resource.canRead() || resource.isDirectory()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        File uploadDir = new File(this.upload_dir);
        if (!resource.getCanonicalPath().startsWith(uploadDir.getCanonicalPath())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        Date ifModDate = new Date(request.getDateHeader("If-Modified-Since"));
        Date lastMod = new Date(resource.lastModified());
        if (lastMod.compareTo(ifModDate) <= 0) {
            mLogger.debug("Resource unmodified ... sending 304");
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }
        response.addDateHeader("Last-Modified", (new Date()).getTime());
        response.setContentType(this.context.getMimeType(resource.getAbsolutePath()));
        byte[] buf = new byte[8192];
        int length = 0;
        OutputStream out = response.getOutputStream();
        InputStream resource_file = new FileInputStream(resource);
        while ((length = resource_file.read(buf)) > 0) out.write(buf, 0, length);
        out.close();
        resource_file.close();
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }
}
