package com.google.gwt.dev.shell.jetty;

import com.google.gwt.core.ext.ServletContainer;
import com.google.gwt.core.ext.ServletContainerLauncher;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.InstalledHelpInfo;
import com.google.gwt.dev.util.Util;
import org.mortbay.component.AbstractLifeCycle;
import org.mortbay.jetty.AbstractConnector;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.RequestLog;
import org.mortbay.jetty.Response;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.HttpFields.Field;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.security.SslSocketConnector;
import org.mortbay.jetty.webapp.WebAppClassLoader;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.log.Log;
import org.mortbay.log.Logger;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * A {@link ServletContainerLauncher} for an embedded Jetty server.
 */
public class JettyLauncher extends ServletContainerLauncher {

    /**
   * Log jetty requests/responses to TreeLogger.
   */
    public static class JettyRequestLogger extends AbstractLifeCycle implements RequestLog {

        private final TreeLogger logger;

        private final TreeLogger.Type normalLogLevel;

        public JettyRequestLogger(TreeLogger logger, TreeLogger.Type normalLogLevel) {
            this.logger = logger;
            assert (normalLogLevel != null);
            this.normalLogLevel = normalLogLevel;
        }

        /**
     * Log an HTTP request/response to TreeLogger.
     */
        @SuppressWarnings("unchecked")
        public void log(Request request, Response response) {
            int status = response.getStatus();
            if (status < 0) {
                status = 404;
            }
            TreeLogger.Type logStatus, logHeaders;
            if (status >= 500) {
                logStatus = TreeLogger.ERROR;
                logHeaders = TreeLogger.INFO;
            } else if (status == 404) {
                if ("/favicon.ico".equals(request.getRequestURI()) && request.getQueryString() == null) {
                    logStatus = TreeLogger.TRACE;
                    logHeaders = TreeLogger.DEBUG;
                } else {
                    logStatus = TreeLogger.WARN;
                    logHeaders = TreeLogger.INFO;
                }
            } else if (status >= 400) {
                logStatus = TreeLogger.WARN;
                logHeaders = TreeLogger.INFO;
            } else {
                logStatus = normalLogLevel;
                logHeaders = TreeLogger.DEBUG;
            }
            String userString = request.getRemoteUser();
            if (userString == null) {
                userString = "";
            } else {
                userString += "@";
            }
            String bytesString = "";
            if (response.getContentCount() > 0) {
                bytesString = " " + response.getContentCount() + " bytes";
            }
            if (logger.isLoggable(logStatus)) {
                TreeLogger branch = logger.branch(logStatus, String.valueOf(status) + " - " + request.getMethod() + ' ' + request.getUri() + " (" + userString + request.getRemoteHost() + ')' + bytesString);
                if (branch.isLoggable(logHeaders)) {
                    TreeLogger headers = branch.branch(logHeaders, "Request headers");
                    Iterator<Field> headerFields = request.getConnection().getRequestFields().getFields();
                    while (headerFields.hasNext()) {
                        Field headerField = headerFields.next();
                        headers.log(logHeaders, headerField.getName() + ": " + headerField.getValue());
                    }
                    headers = branch.branch(logHeaders, "Response headers");
                    headerFields = response.getHttpFields().getFields();
                    while (headerFields.hasNext()) {
                        Field headerField = headerFields.next();
                        headers.log(logHeaders, headerField.getName() + ": " + headerField.getValue());
                    }
                }
            }
        }
    }

    /**
   * An adapter for the Jetty logging system to GWT's TreeLogger. This
   * implementation class is only public to allow {@link Log} to instantiate it.
   * 
   * The weird static data / default construction setup is a game we play with
   * {@link Log}'s static initializer to prevent the initial log message from
   * going to stderr.
   */
    public static class JettyTreeLogger implements Logger {

        private final TreeLogger logger;

        public JettyTreeLogger(TreeLogger logger) {
            if (logger == null) {
                throw new NullPointerException();
            }
            this.logger = logger;
        }

        public void debug(String msg, Object arg0, Object arg1) {
            if (logger.isLoggable(TreeLogger.SPAM)) {
                logger.log(TreeLogger.SPAM, format(msg, arg0, arg1));
            }
        }

        public void debug(String msg, Throwable th) {
            logger.log(TreeLogger.SPAM, msg, th);
        }

        public Logger getLogger(String name) {
            return this;
        }

        public void info(String msg, Object arg0, Object arg1) {
            if (logger.isLoggable(TreeLogger.TRACE)) {
                logger.log(TreeLogger.TRACE, format(msg, arg0, arg1));
            }
        }

        public boolean isDebugEnabled() {
            return logger.isLoggable(TreeLogger.SPAM);
        }

        public void setDebugEnabled(boolean enabled) {
        }

        public void warn(String msg, Object arg0, Object arg1) {
            if (logger.isLoggable(TreeLogger.WARN)) {
                logger.log(TreeLogger.WARN, format(msg, arg0, arg1));
            }
        }

        public void warn(String msg, Throwable th) {
            logger.log(TreeLogger.WARN, msg, th);
        }

        /**
     * Copied from org.mortbay.log.StdErrLog.
     */
        private String format(String msg, Object arg0, Object arg1) {
            int i0 = msg.indexOf("{}");
            int i1 = i0 < 0 ? -1 : msg.indexOf("{}", i0 + 2);
            if (arg1 != null && i1 >= 0) {
                msg = msg.substring(0, i1) + arg1 + msg.substring(i1 + 2);
            }
            if (arg0 != null && i0 >= 0) {
                msg = msg.substring(0, i0) + arg0 + msg.substring(i0 + 2);
            }
            return msg;
        }
    }

    /**
   * The resulting {@link ServletContainer} this is launched.
   */
    protected static class JettyServletContainer extends ServletContainer {

        private final int actualPort;

        private final File appRootDir;

        private final TreeLogger logger;

        private final Server server;

        private final WebAppContext wac;

        public JettyServletContainer(TreeLogger logger, Server server, WebAppContext wac, int actualPort, File appRootDir) {
            this.logger = logger;
            this.server = server;
            this.wac = wac;
            this.actualPort = actualPort;
            this.appRootDir = appRootDir;
        }

        @Override
        public int getPort() {
            return actualPort;
        }

        @Override
        public void refresh() throws UnableToCompleteException {
            String msg = "Reloading web app to reflect changes in " + appRootDir.getAbsolutePath();
            TreeLogger branch = logger.branch(TreeLogger.INFO, msg);
            Log.setLog(new JettyTreeLogger(branch));
            try {
                wac.stop();
                server.stop();
                wac.start();
                server.start();
                branch.log(TreeLogger.INFO, "Reload completed successfully");
            } catch (Exception e) {
                branch.log(TreeLogger.ERROR, "Unable to restart embedded Jetty server", e);
                throw new UnableToCompleteException();
            } finally {
                Log.setLog(new JettyTreeLogger(logger));
            }
        }

        @Override
        public void stop() throws UnableToCompleteException {
            TreeLogger branch = logger.branch(TreeLogger.INFO, "Stopping Jetty server");
            Log.setLog(new JettyTreeLogger(branch));
            try {
                server.stop();
                server.setStopAtShutdown(false);
                branch.log(TreeLogger.TRACE, "Stopped successfully");
            } catch (Exception e) {
                branch.log(TreeLogger.ERROR, "Unable to stop embedded Jetty server", e);
                throw new UnableToCompleteException();
            } finally {
                Log.setLog(new JettyTreeLogger(logger));
            }
        }
    }

    /**
   * A {@link WebAppContext} tailored to GWT hosted mode. Features hot-reload
   * with a new {@link WebAppClassLoader} to pick up disk changes. The default
   * Jetty {@code WebAppContext} will create new instances of servlets, but it
   * will not create a brand new {@link ClassLoader}. By creating a new {@code
   * ClassLoader} each time, we re-read updated classes from disk.
   * 
   * Also provides special class filtering to isolate the web app from the GWT
   * hosting environment.
   */
    protected static final class WebAppContextWithReload extends WebAppContext {

        /**
     * Specialized {@link WebAppClassLoader} that allows outside resources to be
     * brought in dynamically from the system path. A warning is issued when
     * this occurs.
     */
        private class WebAppClassLoaderExtension extends WebAppClassLoader {

            private static final String META_INF_SERVICES = "META-INF/services/";

            public WebAppClassLoaderExtension() throws IOException {
                super(bootStrapOnlyClassLoader, WebAppContextWithReload.this);
            }

            @Override
            public URL findResource(String name) {
                String checkName = name;
                if (checkName.startsWith(META_INF_SERVICES)) {
                    checkName = checkName.substring(META_INF_SERVICES.length());
                }
                URL found;
                if (isSystemPath(checkName)) {
                    found = systemClassLoader.getResource(name);
                    if (found != null) {
                        return found;
                    }
                }
                found = super.findResource(name);
                if (found != null) {
                    return found;
                }
                found = systemClassLoader.getResource(name);
                if (found == null) {
                    return null;
                }
                String warnMessage = "Server resource '" + name + "' could not be found in the web app, but was found on the system classpath";
                if (!addContainingClassPathEntry(warnMessage, found, name)) {
                    return null;
                }
                return super.findResource(name);
            }

            /**
       * Override to additionally consider the most commonly available JSP and
       * XML implementation as system resources. (In fact, Jasper is in gwt-dev
       * via embedded Tomcat, so we always hit this case.)
       */
            @Override
            public boolean isSystemPath(String name) {
                name = name.replace('/', '.');
                return super.isSystemPath(name) || name.startsWith("org.apache.jasper.") || name.startsWith("org.apache.xerces.");
            }

            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                if (isSystemPath(name)) {
                    try {
                        return systemClassLoader.loadClass(name);
                    } catch (ClassNotFoundException e) {
                    }
                }
                try {
                    return super.findClass(name);
                } catch (ClassNotFoundException e) {
                    if (isServerPath(name)) {
                        throw e;
                    }
                }
                String resourceName = name.replace('.', '/') + ".class";
                URL found = systemClassLoader.getResource(resourceName);
                if (found == null) {
                    return null;
                }
                String warnMessage = "Server class '" + name + "' could not be found in the web app, but was found on the system classpath";
                if (!addContainingClassPathEntry(warnMessage, found, resourceName)) {
                    throw new ClassNotFoundException(name);
                }
                return super.findClass(name);
            }

            private boolean addContainingClassPathEntry(String warnMessage, URL resource, String resourceName) {
                TreeLogger.Type logLevel = (System.getProperty(PROPERTY_NOWARN_WEBAPP_CLASSPATH) == null) ? TreeLogger.WARN : TreeLogger.DEBUG;
                TreeLogger branch = logger.branch(logLevel, warnMessage);
                String classPathURL;
                String foundStr = resource.toExternalForm();
                if (resource.getProtocol().equals("file")) {
                    assert foundStr.endsWith(resourceName);
                    classPathURL = foundStr.substring(0, foundStr.length() - resourceName.length());
                } else if (resource.getProtocol().equals("jar")) {
                    assert foundStr.startsWith("jar:");
                    assert foundStr.endsWith("!/" + resourceName);
                    classPathURL = foundStr.substring(4, foundStr.length() - (2 + resourceName.length()));
                } else {
                    branch.log(TreeLogger.ERROR, "Found resouce but unrecognized URL format: '" + foundStr + '\'');
                    return false;
                }
                branch = branch.branch(logLevel, "Adding classpath entry '" + classPathURL + "' to the web app classpath for this session", null, new InstalledHelpInfo("webAppClassPath.html"));
                try {
                    addClassPath(classPathURL);
                    return true;
                } catch (IOException e) {
                    branch.log(TreeLogger.ERROR, "Failed add container URL: '" + classPathURL + '\'', e);
                    return false;
                }
            }
        }

        /**
     * Parent ClassLoader for the Jetty web app, which can only load JVM
     * classes. We would just use <code>null</code> for the parent ClassLoader
     * except this makes Jetty unhappy.
     */
        private final ClassLoader bootStrapOnlyClassLoader = new ClassLoader(null) {
        };

        private final TreeLogger logger;

        /**
     * In the usual case of launching {@link com.google.gwt.dev.DevMode}, this
     * will always by the system app ClassLoader.
     */
        private final ClassLoader systemClassLoader = Thread.currentThread().getContextClassLoader();

        private WebAppClassLoaderExtension classLoader;

        @SuppressWarnings("unchecked")
        private WebAppContextWithReload(TreeLogger logger, String webApp, String contextPath) {
            super(webApp, contextPath);
            this.logger = logger;
            getInitParams().put("org.mortbay.jetty.servlet.Default.useFileMappedBuffer", "false");
            setParentLoaderPriority(true);
        }

        @Override
        protected void doStart() throws Exception {
            classLoader = new WebAppClassLoaderExtension();
            setClassLoader(classLoader);
            super.doStart();
        }

        @Override
        protected void doStop() throws Exception {
            super.doStop();
            Class<?> jdbcUnloader = classLoader.loadClass("com.google.gwt.dev.shell.jetty.JDBCUnloader");
            java.lang.reflect.Method unload = jdbcUnloader.getMethod("unload");
            unload.invoke(null);
            setClassLoader(null);
            classLoader.destroy();
        }
    }

    /**
   * Represents the type of SSL client certificate authentication desired.
   */
    private enum ClientAuth {

        NONE, WANT, REQUIRE
    }

    /**
   * System property to suppress warnings about loading web app classes from the
   * system classpath.
   */
    private static final String PROPERTY_NOWARN_WEBAPP_CLASSPATH = "gwt.nowarn.webapp.classpath";

    static {
        System.setProperty("org.mortbay.log.class", JettyNullLogger.class.getName());
        Log.getLog();
        String antJavaC = System.getProperty("build.compiler", "org.eclipse.jdt.core.JDTCompilerAdapter");
        System.setProperty("build.compiler", antJavaC);
    }

    /**
   * Setup a connector for the bind address/port.
   * 
   * @param connector
   * @param bindAddress 
   * @param port
   */
    private static void setupConnector(AbstractConnector connector, String bindAddress, int port) {
        if (bindAddress != null) {
            connector.setHost(bindAddress.toString());
        }
        connector.setPort(port);
        connector.setReuseAddress(false);
        connector.setSoLingerTime(0);
    }

    private TreeLogger.Type baseLogLevel = TreeLogger.INFO;

    private String bindAddress = null;

    private ClientAuth clientAuth;

    private String keyStore;

    private String keyStorePassword;

    private final Object privateInstanceLock = new Object();

    private boolean useSsl;

    @Override
    public String getName() {
        return "Jetty";
    }

    @Override
    public boolean isSecure() {
        return useSsl;
    }

    @Override
    public boolean processArguments(TreeLogger logger, String arguments) {
        if (arguments != null && arguments.length() > 0) {
            for (String arg : arguments.split(",")) {
                int equals = arg.indexOf('=');
                String tag;
                String value = null;
                if (equals < 0) {
                    tag = arg;
                } else {
                    tag = arg.substring(0, equals);
                    value = arg.substring(equals + 1);
                }
                if ("ssl".equals(tag)) {
                    useSsl = true;
                    URL keyStoreUrl = getClass().getResource("localhost.keystore");
                    if (keyStoreUrl == null) {
                        logger.log(TreeLogger.ERROR, "Default GWT keystore not found");
                        return false;
                    }
                    keyStore = keyStoreUrl.toExternalForm();
                    keyStorePassword = "localhost";
                } else if ("keystore".equals(tag)) {
                    useSsl = true;
                    keyStore = value;
                } else if ("password".equals(tag)) {
                    useSsl = true;
                    keyStorePassword = value;
                } else if ("pwfile".equals(tag)) {
                    useSsl = true;
                    keyStorePassword = Util.readFileAsString(new File(value)).trim();
                    if (keyStorePassword == null) {
                        logger.log(TreeLogger.ERROR, "Unable to read keystore password from '" + value + "'");
                        return false;
                    }
                } else if ("clientAuth".equals(tag)) {
                    useSsl = true;
                    try {
                        clientAuth = ClientAuth.valueOf(value);
                    } catch (IllegalArgumentException e) {
                        logger.log(TreeLogger.WARN, "Ignoring invalid clientAuth of '" + value + "'");
                    }
                } else {
                    logger.log(TreeLogger.ERROR, "Unexpected argument to " + JettyLauncher.class.getSimpleName() + ": " + arg);
                    return false;
                }
            }
            if (useSsl) {
                if (keyStore == null) {
                    logger.log(TreeLogger.ERROR, "A keystore is required to use SSL");
                    return false;
                }
                if (keyStorePassword == null) {
                    logger.log(TreeLogger.ERROR, "A keystore password is required to use SSL");
                    return false;
                }
            }
        }
        return true;
    }

    public void setBaseRequestLogLevel(TreeLogger.Type baseLogLevel) {
        synchronized (privateInstanceLock) {
            this.baseLogLevel = baseLogLevel;
        }
    }

    @Override
    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }

    @Override
    public ServletContainer start(TreeLogger logger, int port, File appRootDir) throws Exception {
        TreeLogger branch = logger.branch(TreeLogger.TRACE, "Starting Jetty on port " + port, null);
        checkStartParams(branch, port, appRootDir);
        Log.setLog(new JettyTreeLogger(branch));
        jreLeakPrevention(logger);
        System.setProperty("org.mortbay.xml.XmlParser.Validating", "false");
        Server server = new Server();
        AbstractConnector connector = getConnector(logger);
        setupConnector(connector, bindAddress, port);
        server.addConnector(connector);
        WebAppContext wac = createWebAppContext(logger, appRootDir);
        RequestLogHandler logHandler = new RequestLogHandler();
        logHandler.setRequestLog(new JettyRequestLogger(logger, getBaseLogLevel()));
        logHandler.setHandler(wac);
        server.setHandler(logHandler);
        server.start();
        server.setStopAtShutdown(true);
        Log.setLog(new JettyTreeLogger(logger));
        int connectorPort = connector.getLocalPort();
        if (connector.getLocalPort() < 0) {
            branch.log(TreeLogger.ERROR, String.format("Failed to connect to open channel with port %d (return value %d)", port, connectorPort));
            if (connector.getConnection() == null) {
                branch.log(TreeLogger.TRACE, "Connection is null");
            }
        }
        return createServletContainer(logger, appRootDir, server, wac, connectorPort);
    }

    protected JettyServletContainer createServletContainer(TreeLogger logger, File appRootDir, Server server, WebAppContext wac, int localPort) {
        return new JettyServletContainer(logger, server, wac, localPort, appRootDir);
    }

    protected WebAppContext createWebAppContext(TreeLogger logger, File appRootDir) {
        return new WebAppContextWithReload(logger, appRootDir.getAbsolutePath(), "/");
    }

    protected AbstractConnector getConnector(TreeLogger logger) {
        if (useSsl) {
            TreeLogger sslLogger = logger.branch(TreeLogger.INFO, "Listening for SSL connections");
            if (sslLogger.isLoggable(TreeLogger.TRACE)) {
                sslLogger.log(TreeLogger.TRACE, "Using keystore " + keyStore);
            }
            SslSocketConnector conn = new SslSocketConnector();
            if (clientAuth != null) {
                switch(clientAuth) {
                    case NONE:
                        conn.setWantClientAuth(false);
                        conn.setNeedClientAuth(false);
                        break;
                    case WANT:
                        sslLogger.log(TreeLogger.TRACE, "Requesting client certificates");
                        conn.setWantClientAuth(true);
                        conn.setNeedClientAuth(false);
                        break;
                    case REQUIRE:
                        sslLogger.log(TreeLogger.TRACE, "Requiring client certificates");
                        conn.setWantClientAuth(true);
                        conn.setNeedClientAuth(true);
                        break;
                }
            }
            conn.setKeystore(keyStore);
            conn.setTruststore(keyStore);
            conn.setKeyPassword(keyStorePassword);
            conn.setTrustPassword(keyStorePassword);
            return conn;
        }
        return new SelectChannelConnector();
    }

    private void checkStartParams(TreeLogger logger, int port, File appRootDir) {
        if (logger == null) {
            throw new NullPointerException("logger cannot be null");
        }
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be either 0 (for auto) or less than 65536");
        }
        if (appRootDir == null) {
            throw new NullPointerException("app root direcotry cannot be null");
        }
    }

    private TreeLogger.Type getBaseLogLevel() {
        synchronized (privateInstanceLock) {
            return this.baseLogLevel;
        }
    }

    /**
   * This is a modified version of JreMemoryLeakPreventionListener.java found
   * in the Apache Tomcat project at
   * 
   * http://svn.apache.org/repos/asf/tomcat/trunk/java/org/apache/catalina/core/
   * JreMemoryLeakPreventionListener.java
   * 
   * Relevant part of the Tomcat NOTICE, retrieved from
   * http://svn.apache.org/repos/asf/tomcat/trunk/NOTICE Apache Tomcat Copyright
   * 1999-2010 The Apache Software Foundation
   * 
   * This product includes software developed by The Apache Software Foundation
   * (http://www.apache.org/).
   */
    private void jreLeakPrevention(TreeLogger logger) {
        ImageIO.getCacheDirectory();
        try {
            Class<?> clazz = Class.forName("sun.misc.GC");
            Method method = clazz.getDeclaredMethod("requestLatency", new Class[] { long.class });
            method.invoke(null, Long.valueOf(3600000));
        } catch (ClassNotFoundException e) {
            logger.log(TreeLogger.ERROR, "jreLeakPrevention.gcDaemonFail", e);
        } catch (SecurityException e) {
            logger.log(TreeLogger.ERROR, "jreLeakPrevention.gcDaemonFail", e);
        } catch (NoSuchMethodException e) {
            logger.log(TreeLogger.ERROR, "jreLeakPrevention.gcDaemonFail", e);
        } catch (IllegalArgumentException e) {
            logger.log(TreeLogger.ERROR, "jreLeakPrevention.gcDaemonFail", e);
        } catch (IllegalAccessException e) {
            logger.log(TreeLogger.ERROR, "jreLeakPrevention.gcDaemonFail", e);
        } catch (InvocationTargetException e) {
            logger.log(TreeLogger.ERROR, "jreLeakPrevention.gcDaemonFail", e);
        }
        try {
            Class<?> policyClass = Class.forName("javax.security.auth.Policy");
            Method method = policyClass.getMethod("getPolicy");
            method.invoke(null);
        } catch (ClassNotFoundException e) {
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
            logger.log(TreeLogger.WARN, "jreLeakPrevention.authPolicyFail", e);
        } catch (IllegalArgumentException e) {
            logger.log(TreeLogger.WARN, "jreLeakPrevention.authPolicyFail", e);
        } catch (IllegalAccessException e) {
            logger.log(TreeLogger.WARN, "jreLeakPrevention.authPolicyFail", e);
        } catch (InvocationTargetException e) {
            logger.log(TreeLogger.WARN, "jreLeakPrevention.authPolicyFail", e);
        }
        java.security.Security.getProviders();
        try {
            URL url = new URL("jar:file://dummy.jar!/");
            URLConnection uConn = url.openConnection();
            uConn.setDefaultUseCaches(false);
        } catch (MalformedURLException e) {
            logger.log(TreeLogger.ERROR, "jreLeakPrevention.jarUrlConnCacheFail", e);
        } catch (IOException e) {
            logger.log(TreeLogger.ERROR, "jreLeakPrevention.jarUrlConnCacheFail", e);
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            logger.log(TreeLogger.ERROR, "jreLeakPrevention.xmlParseFail", e);
        }
    }
}
