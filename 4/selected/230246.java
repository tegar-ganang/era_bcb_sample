package org.gomba.contrib;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.gomba.AbstractServlet;
import org.gomba.Expression;
import org.gomba.ParameterResolver;

/**
 * Perform a file system operation on a file. This servlet inherits the
 * init-params of {@link org.gomba.AbstractServlet}, plus:
 * <dl>
 * <dt>name</dt>
 * <dd>The name for the file to operate on. May contain ${} parameters. The
 * path must begin with a "/" and is interpreted as relative to the current
 * context root. (Required)</dd>
 * <dt>http-method</dt>
 * <dd>The value can be GET, PUT or DELETE. (Required)</dd>
 * </dl>
 * 
 * Note about HTTP method usage. The GET method is normally used for file
 * retrieval operations. The PUT method is normally used for file write
 * operations. The DELETE method is normally used for file remove operations.
 * 
 * @author Patrick Dreyer
 * @version $Id: FileSystemFileServlet.java,v 1.1 2007/05/16 13:16:37 flaviotordini Exp $
 */
public class FileSystemFileServlet extends AbstractServlet {

    private static final String INIT_PARAM_HTTP_METHOD = "http-method";

    private static final String INIT_PARAM_NAME = "name";

    /** <code>true</code> if this servlet supports the GET HTTP method. */
    private boolean supportGet;

    /** <code>true</code> if this servlet supports the PUT HTTP method. */
    private boolean supportPut;

    /** <code>true</code> if this servlet supports the DELETE HTTP method. */
    private boolean supportDelete;

    private Expression name;

    /**
     * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String httpMethod = config.getInitParameter(INIT_PARAM_HTTP_METHOD);
        if (httpMethod == null) {
            throw new ServletException("Missing init-param: " + INIT_PARAM_HTTP_METHOD);
        }
        if (httpMethod.equals("GET")) this.supportGet = true; else if (httpMethod.equals("PUT")) this.supportPut = true; else if (httpMethod.equals("DELETE")) this.supportDelete = true; else throw new ServletException("Unsupported HTTP method: " + httpMethod);
        String name = config.getInitParameter(INIT_PARAM_NAME);
        if (name == null) throw new ServletException("Missing init-param: " + INIT_PARAM_NAME);
        try {
            if (new File(name).isAbsolute()) this.name = new Expression(name); else this.name = new Expression(getServletContext().getRealPath("/") + name);
        } catch (Exception e) {
            throw new ServletException("Error parsing name.", e);
        }
    }

    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!this.supportGet) response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        final long startTime = System.currentTimeMillis();
        final ParameterResolver parameterResolver = new ParameterResolver(request);
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new BufferedInputStream(new FileInputStream(resolveName(parameterResolver)));
            os = response.getOutputStream();
            byte[] buffer = new byte[response.getBufferSize()];
            int l;
            while ((l = is.read(buffer)) >= 0) os.write(buffer, 0, l);
            response.setStatus(getHttpStatusCode());
        } finally {
            if (os != null) os.close();
            if (is != null) is.close();
            if (isDebugMode()) log(getProfilingMessage(request, startTime));
        }
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPut(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!this.supportPut) response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        final long startTime = System.currentTimeMillis();
        final ParameterResolver parameterResolver = new ParameterResolver(request);
        final File file = resolveName(parameterResolver);
        int httpStatusCode = getHttpStatusCode();
        if (!file.exists()) httpStatusCode = HttpServletResponse.SC_CREATED;
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new BufferedInputStream(request.getInputStream());
            os = new BufferedOutputStream(new FileOutputStream(file));
            byte[] buffer = new byte[16384];
            int l;
            while ((l = is.read(buffer)) >= 0) os.write(buffer, 0, l);
            response.setStatus(httpStatusCode);
        } finally {
            if (os != null) os.close();
            if (is != null) is.close();
            if (isDebugMode()) log(getProfilingMessage(request, startTime));
        }
    }

    /**
     * @see javax.servlet.http.HttpServlet#doDelete(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if (!this.supportDelete) response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        final long startTime = System.currentTimeMillis();
        final ParameterResolver parameterResolver = new ParameterResolver(request);
        final File file = resolveName(parameterResolver);
        if (!file.exists()) response.setStatus(HttpServletResponse.SC_NOT_FOUND); else if (!file.delete()) response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Could not delete file."); else response.setStatus(getHttpStatusCode());
        String msg = getProfilingMessage(request, startTime);
        if (msg != null) {
            log(msg);
        }
    }

    protected final File resolveName(ParameterResolver parameterResolver) throws ServletException {
        try {
            return new File(this.name.replaceParameters(parameterResolver).toString());
        } catch (Exception e) {
            throw new ServletException("Error setting name.", e);
        }
    }
}
