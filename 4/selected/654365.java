package php.java.servlet.fastcgi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import php.java.bridge.Util;
import php.java.bridge.http.AbstractChannelName;
import php.java.bridge.http.FCGIConnectException;
import php.java.bridge.http.FCGIConnectionException;
import php.java.bridge.http.FCGIConnectionPool;
import php.java.bridge.http.FCGIInputStream;
import php.java.bridge.http.FCGIOutputStream;
import php.java.bridge.http.FCGIUtil;
import php.java.bridge.http.IContextFactory;
import php.java.servlet.ContextLoaderListener;
import php.java.servlet.PhpJavaServlet;
import php.java.servlet.ServletContextFactory;
import php.java.servlet.ServletUtil;

/**
 * A CGI Servlet which connects to a FastCGI server. If allowed by the
 * administrator and if a fast cgi binary is installed in the JavaBridge web application or
 * DEFAULT_CGI_LOCATIONS, the bridge can automatically start one FCGI
 * server on the computer. Default is Autostart.  
 * <p>The admin may start a FCGI
 * server for all users with the command:<br><code> cd /tmp<br>
 * REDIRECT_STATUS=200 X_JAVABRIDGE_OVERRIDE_HOSTS="/" PHP_FCGI_CHILDREN="5"
 * PHP_FCGI_MAX_REQUESTS="5000" /usr/bin/php-cgi -b 127.0.0.1:9667<br>
 * </code>
 * </p>
 * <p>When the program <code>/bin/sh</code> does not exist, a program called <code>launcher.exe</code>
 * is called instead:
 * <code>launcher.exe -a "path_to_php-cgi.exe" -b 9667</code>.</p>
 * @see php.java.bridge.Util#DEFAULT_CGI_LOCATIONS
 * @author jostb
 */
public class FastCGIServlet extends HttpServlet {

    protected static final String _80 = "80";

    protected static final String _443 = "443";

    private static final long serialVersionUID = 3545800996174312757L;

    protected String documentRoot;

    protected String serverSignature;

    protected static class Environment {

        public IContextFactory ctx;

        public String contextPath;

        public String pathInfo;

        public String servletPath;

        public String queryString;

        public String requestUri;

        public HashMap environment;

        public boolean includedJava;

        public ArrayList allHeaders;
    }

    protected ServletContext context;

    protected ContextLoaderListener contextLoaderListener;

    protected String serverInfo;

    protected FCGIConnectionPool connectionPool;

    protected boolean phpRequestURIisUnique;

    /**
     * Create a new FastCGI servlet which connects to a PHP FastCGI server using a connection pool.
     * 
     * If the JavaBridge context exists and the JavaBridge context can
     * start a FastCGI server and the current context is configured to
     * connect to a FastCGI server, the current context connects to
     * the JavaBridge context to start the server and then uses this
     * server for all subsequent requests until the server is
     * stopped. When FastCGI is not available (anymore), the parent
     * CGI servlet is used instead.
     * @param config The servlet config
     * @throws ServletException 
     * @see php.java.bridge.http.FCGIConnectionPool
     * @see #destroy()
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        context = config.getServletContext();
        String value = (String) config.getInitParameter("php_request_uri_is_unique");
        if ("on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value)) phpRequestURIisUnique = true;
        contextLoaderListener = (ContextLoaderListener) context.getAttribute(ContextLoaderListener.CONTEXT_LOADER_LISTENER);
        serverInfo = config.getServletName();
        if (serverInfo == null) serverInfo = "FastCGIServlet";
        documentRoot = ServletUtil.getRealPath(context, "");
        serverSignature = context.getServerInfo();
        connectionPool = contextLoaderListener.getConnectionPool();
        if (connectionPool == null) {
            try {
                contextLoaderListener.getChannelName().test();
            } catch (FCGIConnectException e) {
                throw new ServletException(e);
            }
            throw new ServletException("No connection pool");
        }
    }

    /**
     * Destroys the FastCGI connection pool, if it exists.
     */
    public void destroy() {
        super.destroy();
    }

    protected void setupRequestVariables(HttpServletRequest req, Environment env) {
        env.allHeaders = new ArrayList();
        env.includedJava = contextLoaderListener.getPhpIncludeJava() && PhpJavaServlet.getHeader(Util.X_JAVABRIDGE_INCLUDE, req) == null;
        env.contextPath = (String) req.getAttribute("javax.servlet.include.context_path");
        if (env.contextPath == null) env.contextPath = req.getContextPath();
        env.pathInfo = (String) req.getAttribute("javax.servlet.include.path_info");
        if (env.pathInfo == null) env.pathInfo = req.getPathInfo();
        env.servletPath = (String) req.getAttribute("javax.servlet.include.servlet_path");
        if (env.servletPath == null) env.servletPath = req.getServletPath();
        env.queryString = (String) req.getAttribute("javax.servlet.include.query_string");
        if (env.queryString == null) env.queryString = req.getQueryString();
        if (phpRequestURIisUnique) {
            env.requestUri = (String) req.getAttribute("javax.servlet.include.request_uri");
        } else {
            env.requestUri = (String) req.getAttribute("javax.servlet.forward.request_uri");
        }
        if (env.requestUri == null) env.requestUri = req.getRequestURI();
    }

    /** calculate PATH_INFO, PATH_TRANSLATED and SCRIPT_FILENAME */
    protected void setPathInfo(HttpServletRequest req, HashMap envp, Environment env) {
        String pathInfo = env.pathInfo;
        if (pathInfo != null) {
            envp.put("PATH_INFO", pathInfo);
            envp.put("PATH_TRANSLATED", documentRoot + pathInfo);
        }
        if (env.includedJava) envp.put("SCRIPT_FILENAME", ServletUtil.getRealPath(context, "java/JavaProxy.php")); else envp.put("SCRIPT_FILENAME", ServletUtil.getRealPath(context, env.servletPath));
    }

    protected void setupCGIEnvironment(HttpServletRequest req, HttpServletResponse res, Environment env) throws ServletException {
        HashMap envp = (HashMap) contextLoaderListener.getEnvironment().clone();
        envp.put("SERVER_SOFTWARE", serverInfo);
        envp.put("SERVER_NAME", ServletUtil.nullsToBlanks(req.getServerName()));
        envp.put("GATEWAY_INTERFACE", "CGI/1.1");
        envp.put("SERVER_PROTOCOL", ServletUtil.nullsToBlanks(req.getProtocol()));
        int port = ServletUtil.getLocalPort(req);
        Integer iPort = (port == 0 ? new Integer(-1) : new Integer(port));
        envp.put("SERVER_PORT", iPort.toString());
        envp.put("REQUEST_METHOD", ServletUtil.nullsToBlanks(req.getMethod()));
        envp.put("SCRIPT_NAME", env.contextPath + env.servletPath);
        envp.put("QUERY_STRING", ServletUtil.nullsToBlanks(env.queryString));
        envp.put("REMOTE_HOST", ServletUtil.nullsToBlanks(req.getRemoteHost()));
        envp.put("REMOTE_ADDR", ServletUtil.nullsToBlanks(req.getRemoteAddr()));
        envp.put("AUTH_TYPE", ServletUtil.nullsToBlanks(req.getAuthType()));
        envp.put("REMOTE_USER", ServletUtil.nullsToBlanks(req.getRemoteUser()));
        envp.put("REMOTE_IDENT", "");
        envp.put("CONTENT_TYPE", ServletUtil.nullsToBlanks(req.getContentType()));
        setPathInfo(req, envp, env);
        int contentLength = req.getContentLength();
        String sContentLength = (contentLength <= 0 ? "" : (new Integer(contentLength)).toString());
        envp.put("CONTENT_LENGTH", sContentLength);
        Enumeration headers = req.getHeaderNames();
        String header = null;
        StringBuffer buffer = new StringBuffer();
        while (headers.hasMoreElements()) {
            header = ((String) headers.nextElement()).toUpperCase();
            if ("AUTHORIZATION".equalsIgnoreCase(header) || "PROXY_AUTHORIZATION".equalsIgnoreCase(header)) {
            } else if ("HOST".equalsIgnoreCase(header)) {
                String host = req.getHeader(header);
                int idx = host.indexOf(":");
                if (idx < 0) idx = host.length();
                envp.put("HTTP_" + header.replace('-', '_'), host.substring(0, idx));
            } else if (header.startsWith("X_")) {
                envp.put(header, req.getHeader(header));
            } else {
                envp.put("HTTP_" + header.replace('-', '_'), ServletUtil.getHeaders(buffer, req.getHeaders(header)));
            }
        }
        env.environment = envp;
        if (env.includedJava) {
            env.environment.put("X_JAVABRIDGE_INCLUDE_ONLY", "1");
            env.environment.put("X_JAVABRIDGE_INCLUDE", ServletUtil.getRealPath(getServletContext(), env.servletPath));
        }
        env.environment.put("REDIRECT_STATUS", "200");
        env.environment.put("SERVER_SOFTWARE", Util.EXTENSION_NAME);
        String sPort = (String) env.environment.get("SERVER_PORT");
        String standardPort = req.isSecure() ? _443 : _80;
        StringBuffer httpHost = new StringBuffer((String) env.environment.get("SERVER_NAME"));
        if (!standardPort.equals(sPort)) {
            httpHost.append(":");
            httpHost.append(sPort);
        }
        env.environment.put("HTTP_HOST", httpHost.toString());
        String remotePort = null;
        try {
            remotePort = String.valueOf(req.getRemotePort());
        } catch (Throwable t) {
            remotePort = String.valueOf(t);
        }
        env.environment.put("REMOTE_PORT", remotePort);
        String query = env.queryString;
        if (query != null) env.environment.put("REQUEST_URI", ServletUtil.nullsToBlanks(env.requestUri + "?" + query)); else env.environment.put("REQUEST_URI", ServletUtil.nullsToBlanks(env.requestUri));
        env.environment.put("SERVER_ADDR", req.getServerName());
        env.environment.put("SERVER_SIGNATURE", serverSignature);
        env.environment.put("DOCUMENT_ROOT", documentRoot);
        if (req.isSecure()) env.environment.put("HTTPS", "On");
        String id = PhpJavaServlet.getHeader(Util.X_JAVABRIDGE_CONTEXT, req);
        if (id == null) {
            id = (env.ctx = ServletContextFactory.addNew(contextLoaderListener.getContextServer(), this, getServletContext(), req, req, res)).getId();
            AbstractChannelName channelName = contextLoaderListener.getContextServer().getChannelName(env.ctx);
            if (channelName != null) {
                env.environment.put(Util.X_JAVABRIDGE_REDIRECT, channelName.getName());
                env.ctx.getBridge();
                contextLoaderListener.getContextServer().start(channelName, contextLoaderListener.getLogger());
            }
        }
        env.environment.put(Util.X_JAVABRIDGE_CONTEXT, id);
    }

    /**
     * Optimized run method for FastCGI. Makes use of the large FCGI_BUF_SIZE and the specialized in.read(). 
     * It is a modified copy of the parseBody. 
     * @throws InterruptedException 
     * @see HeaderParser#parseBody(byte[], InputStream, OutputStream, HeaderParser)
     */
    protected void parseBody(HttpServletRequest req, HttpServletResponse res, Environment env) throws FCGIConnectionException, FCGIConnectException, IOException, ServletException {
        final byte[] buf = new byte[FCGIUtil.FCGI_BUF_SIZE];
        InputStream in = null;
        OutputStream out = null;
        FCGIInputStream natIn = null;
        FCGIOutputStream natOut = null;
        FCGIConnectionPool.Connection connection = null;
        try {
            connection = connectionPool.openConnection();
            natOut = (FCGIOutputStream) connection.getOutputStream();
            natIn = (FCGIInputStream) connection.getInputStream();
            in = req.getInputStream();
            out = ServletUtil.getServletOutputStream(res);
            natOut.writeBegin();
            natOut.writeParams(env.environment);
            String line = null;
            int i = 0, n, s = 0;
            boolean eoh = false;
            boolean rn = false;
            if (("chunked".equalsIgnoreCase(PhpJavaServlet.getHeader("Transfer-Encoding", req))) || ("upgrade".equalsIgnoreCase(PhpJavaServlet.getHeader("Connection", req)))) {
                final InputStream inputStream = in;
                in = null;
                final FCGIOutputStream natOutputStream = natOut;
                natOut = null;
                (new Thread() {

                    public void run() {
                        int n;
                        try {
                            while ((n = inputStream.read(buf)) != -1) {
                                natOutputStream.write(FCGIUtil.FCGI_STDIN, buf, n);
                            }
                            natOutputStream.write(FCGIUtil.FCGI_STDIN, FCGIUtil.FCGI_EMPTY_RECORD);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                natOutputStream.close();
                            } catch (IOException e) {
                            }
                        }
                    }
                }).start();
            } else {
                while ((n = in.read(buf)) != -1) {
                    natOut.write(FCGIUtil.FCGI_STDIN, buf, n);
                }
                natOut.write(FCGIUtil.FCGI_STDIN, FCGIUtil.FCGI_EMPTY_RECORD);
                natOut.close();
                natOut = null;
            }
            String remain = null;
            while ((n = natIn.read(buf)) != -1) {
                int N = i + n;
                while (!eoh && i < N) {
                    switch(buf[i++]) {
                        case '\n':
                            if (rn) {
                                eoh = true;
                            } else {
                                if (remain != null) {
                                    line = remain + new String(buf, s, i - s, Util.ASCII);
                                    line = line.substring(0, line.length() - 2);
                                    remain = null;
                                } else {
                                    line = new String(buf, s, i - s - 2, Util.ASCII);
                                }
                                addHeader(res, line, env);
                                s = i;
                            }
                            rn = true;
                            break;
                        case '\r':
                            break;
                        default:
                            rn = false;
                    }
                }
                if (eoh) {
                    if (i < N) out.write(buf, i, N - i);
                } else {
                    if (remain != null) {
                        remain += new String(buf, s, i - s, Util.ASCII);
                    } else {
                        remain = new String(buf, s, i - s, Util.ASCII);
                    }
                }
                s = i = 0;
            }
            natIn.close();
            String phpFatalError = natIn.checkError();
            StringBuffer phpError = natIn.getError();
            if ((phpError != null) && (Util.logLevel > 4)) Util.logDebug(phpError.toString());
            natIn = null;
            connection = null;
            if (phpFatalError != null) throw new RuntimeException(phpFatalError);
        } catch (InterruptedException e) {
            throw new ServletException(e);
        } finally {
            if (connection != null) connection.setIsClosed();
            if (natIn != null) try {
                natIn.close();
            } catch (IOException e) {
            }
            if (natOut != null) try {
                natOut.close();
            } catch (IOException e) {
            }
        }
    }

    protected Environment getEnvironment() {
        return new Environment();
    }

    protected void execute(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException, InterruptedException {
        Environment env = getEnvironment();
        setupRequestVariables(req, env);
        setupCGIEnvironment(req, res, env);
        try {
            parseBody(req, res, env);
        } catch (FCGIConnectException ex) {
            if (Util.logLevel > 1) {
                Util.logDebug("PHP FastCGI server failed: " + ex);
                Util.printStackTrace(ex);
            }
            IOException ex2 = new IOException("PHP FastCGI server failed: ");
            ex2.initCause(ex);
            throw ex2;
        } catch (FCGIConnectionException x) {
            Util.logError("PHP application terminated unexpectedly, have you started php-cgi with the environment setting PHP_FCGI_MAX_REQUESTS=" + contextLoaderListener.getPhpMaxRequests() + "?  Error: " + x);
            if (Util.logLevel > 1) {
                Util.logDebug("PHP FastCGI instance failed: " + x);
                Util.printStackTrace(x);
            }
            throw new ServletException("PHP FastCGI instance failed.", x);
        } catch (IOException e) {
            if (Util.logLevel > 4) Util.printStackTrace(e);
        } finally {
            if (env.ctx != null) env.ctx.releaseManaged();
            env.ctx = null;
        }
    }

    protected void addHeader(HttpServletResponse response, String line, Environment env) {
        try {
            if (line.startsWith("Status")) {
                line = line.substring(line.indexOf(":") + 1).trim();
                int i = line.indexOf(' ');
                if (i > 0) line = line.substring(0, i);
                response.setStatus(Integer.parseInt(line));
            } else {
                if (!env.allHeaders.contains(line)) {
                    response.addHeader(line.substring(0, line.indexOf(":")).trim(), line.substring(line.indexOf(":") + 1).trim());
                    env.allHeaders.add(line);
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
        } catch (StringIndexOutOfBoundsException e) {
        }
    }

    protected void handle(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        try {
            execute(req, res);
        } catch (IOException e) {
            try {
                res.reset();
            } catch (Exception ex) {
            }
            StringBuffer buf = new StringBuffer("PHP FastCGI server not running. Please see server log for details.");
            if (contextLoaderListener.getChannelName() != null && context != null) {
                buf.append(" Or start a PHP FastCGI server using the command:\n");
                buf.append(contextLoaderListener.getChannelName().getFcgiStartCommand(ServletUtil.getRealPath(context, ContextLoaderListener.CGI_DIR), contextLoaderListener.getPhpMaxRequests()));
            }
            IOException ex = new IOException(buf.toString());
            ex.initCause(e);
            throw ex;
        } catch (ServletException e) {
            try {
                res.reset();
            } catch (Exception ex) {
            }
            throw e;
        } catch (Throwable t) {
            try {
                res.reset();
            } catch (Exception ex) {
            }
            if (Util.logLevel > 4) Util.printStackTrace(t);
            throw new ServletException(t);
        }
    }

    protected void doPut(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        handle(req, res);
    }

    protected void doDelete(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        handle(req, res);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (Util.logLevel > 4) {
            if (req.getAttribute("javax.servlet.include.request_uri") != null) log("doGet (included):" + req.getAttribute("javax.servlet.include.request_uri"));
            log("doGet:" + req.getRequestURI());
        }
        handle(req, res);
    }
}
