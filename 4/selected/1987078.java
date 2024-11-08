package org.knopferfish.bundle.jetty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.knopflerfish.service.log.LogRef;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.xml.XmlConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.webapp.WarDeployer;

/**
 * Jetty-based HTTP service. Can be used as a replacement for the internal HTTP
 * server.
 */
public class JettyHttpService implements WarDeployer, HttpContext {

    /** System property used to configure Jetty from an external file */
    public static final String CFG_JETTY_KEY = "org.osgi.service.http.config_file";

    /** System property used to configure Jetty from an external file */
    public static final String CFG_JETTY_HOME_DIR = "org.osgi.service.http.home_dir";

    /** System Property pointing to the keystore location - used only if SSL is
	 * enabled */
    public static final String CFG_KEYSTORE_FILE = "org.osgi.service.http.keystore_location";

    /** System property containing the keystore password */
    public static final String CFG_KEYSTORE_PWD = "org.osgi.service.http.keystore_password";

    /** System property containing the key password */
    public static final String CFG_KEY_PWD = "org.osgi.service.http.key_password";

    /** Configuration file for Jetty contained in the JAR */
    public static final String DEF_JETTY_FILE = "/jetty-cfg.xml";

    /** Default keystore included in the jar */
    public static final String DEF_KEYSTORE_FILE = "/keystore";

    /** logger */
    private LogRef log;

    /** Jetty server */
    private Server server;

    /** The context collection */
    private ContextHandlerCollection hcoll;

    /** local mapping alias - context */
    private Map<String, ContextHandler> cMap;

    /** the jetty home dir */
    private File jhome;

    /** the storage directory */
    private File jstorage;

    /**
	 * Creates the service.
	 * @param ctx
	 * 		The bundle context used to set up jetty working environment
	 * @param _log
	 * 		The Logger.
	 * @throws IOException
	 * 		If an error happens setting up the working environment.
	 */
    public JettyHttpService(BundleContext ctx, LogRef _log) throws IOException {
        log = _log;
        server = new Server();
        cMap = new HashMap<String, ContextHandler>();
        jhome = ctx.getDataFile("");
        String cfgHomeDir = System.getProperty(CFG_JETTY_HOME_DIR, null);
        if (cfgHomeDir != null) {
            jhome = new File(cfgHomeDir);
            if ((!jhome.exists() && !jhome.mkdirs())) {
                throw new IOException("Invalid home_dir, cannot create: " + cfgHomeDir);
            }
        }
        if (jhome == null) {
            throw new IOException("Can't allocate a jetty working directory, file support not present in OSGi");
        }
        if (!jhome.canWrite()) {
            throw new IOException("Invalid home_dir, read-only: " + jhome.getAbsolutePath());
        }
        log.info("Jetty base dir: " + jhome.getAbsolutePath());
        System.setProperty("jetty.home", jhome.getAbsolutePath());
        File tmp = new File(jhome, "logs");
        if (!tmp.exists() && !tmp.mkdirs()) {
            throw new IOException("Cannot configure Jetty, unable to create logs dir: " + tmp.getAbsolutePath());
        }
        System.setProperty("jetty.logs", tmp.getAbsolutePath());
        tmp = new File(jhome, "work");
        if (!tmp.exists() && !tmp.mkdirs()) {
            throw new IOException("Cannot configure Jetty, unable to create work dir: " + tmp.getAbsolutePath());
        }
        tmp = new File(jhome, "storage");
        if (!tmp.exists() && !tmp.mkdirs()) {
            throw new IOException("Cannot configure Jetty, unable to create storage dir: " + tmp.getAbsolutePath());
        }
        jstorage = tmp;
        String cname = System.getProperty("jetty.context.path", null);
        if (cname == null) {
            cname = new File(jhome, "contexts").getAbsolutePath();
            System.setProperty("jetty.context.path", cname);
            log.info("Jetty Context directory: " + cname);
        }
        tmp = new File(cname);
        if (!tmp.exists() && !tmp.mkdirs()) {
            throw new IOException("Cannot configure Jetty, unable to create context dir: " + tmp.getAbsolutePath());
        }
        String kloc = System.getProperty(CFG_KEYSTORE_FILE, null);
        if (kloc == null) {
            kloc = new File(jhome, "keystore").getAbsolutePath();
            System.setProperty(CFG_KEYSTORE_FILE, kloc);
        }
        if (System.getProperty(CFG_KEYSTORE_PWD, null) == null) {
            System.setProperty(CFG_KEYSTORE_PWD, "storepass");
        }
        if (System.getProperty(CFG_KEY_PWD, null) == null) {
            System.setProperty(CFG_KEY_PWD, "storepass");
        }
        File f = new File(kloc);
        if (!f.exists()) {
            log.warn("Copying default keystore at: " + kloc);
            FileOutputStream fo = new FileOutputStream(f);
            InputStream in = this.getClass().getResourceAsStream(DEF_KEYSTORE_FILE);
            byte[] buf = new byte[1024];
            int len = 0;
            while ((len = in.read(buf)) != -1) {
                fo.write(buf, 0, len);
            }
            fo.close();
        }
    }

    /**
	 * Configures and starts the server
	 * @throws Exception
	 * 		if an error occurs during the configuration process.
	 */
    public void configure() throws Exception {
        String cfgName = System.getProperty(CFG_JETTY_KEY, null);
        log.info("Configuring Jetty web server from: " + (cfgName != null ? cfgName : "<default config file>"));
        InputStream is = null;
        if (cfgName == null) {
            is = getClass().getResourceAsStream(DEF_JETTY_FILE);
        } else {
            is = new FileInputStream(cfgName);
        }
        XmlConfiguration cfg = new XmlConfiguration(is);
        cfg.configure(server);
        hcoll = new ContextHandlerCollection();
        Handler hdr = server.getHandler();
        hcoll.addHandler(hdr);
        server.setHandler(hcoll);
    }

    /**
	 * Stops the server
	 * @throws Exception
	 * 		if the server stop operation fails
	 */
    public void shutdown() throws Exception {
        if (server == null) {
            log.warn("Shutdown on unconfigured Jetty Server");
            return;
        }
        server.stop();
    }

    /**
	 * Starts the server.
	 * @throws Exception
	 * 		if the server start operation fails
	 */
    public void start() throws Exception {
        server.start();
    }

    public HttpContext createDefaultHttpContext() {
        log.debug("Creating default HttpContext (this returned)");
        return this;
    }

    public void registerResources(String alias, String path, HttpContext httpCtx) throws NamespaceException {
        if (alias == null || alias.length() == 0) {
            throw new NamespaceException("Invalid alias on registerResources: " + alias);
        }
        if (alias.length() > 0 && alias.charAt(alias.length() - 1) == '/') {
            alias = alias.substring(0, alias.length() - 1);
        }
        if (cMap.containsKey(alias)) {
            throw new NamespaceException("Alias already registered on registerResources: " + alias);
        }
        if (path == null || path.length() == 0) {
            throw new NamespaceException("Invalid path on registerResources: " + path);
        }
        ContextHandler ctx = new ContextHandler(hcoll, alias);
        ctx.setResourceBase(path);
        ctx.setHandler(new ResourceHandler());
        try {
            ctx.start();
        } catch (Exception ex) {
            log.error("registerResources, Unable to start context: " + ex.getMessage());
            hcoll.removeHandler(ctx);
            throw new NamespaceException("registerResources, unable to start context: " + ex.getMessage(), ex);
        }
        cMap.put(alias, ctx);
        log.info("Registered resources at " + alias + " for " + path);
    }

    @SuppressWarnings("unchecked")
    public void registerServlet(String alias, Servlet servlet, Dictionary params, HttpContext httpCtx) throws ServletException, NamespaceException {
        if (alias == null || alias.length() == 0) {
            throw new NamespaceException("Invalid alias on registerServlet: " + alias);
        }
        if (alias.length() > 0 && alias.charAt(alias.length() - 1) == '/') {
            alias = alias.substring(0, alias.length() - 1);
        }
        if (cMap.containsKey(alias)) {
            throw new NamespaceException("Alias already registered on registerServlet: " + alias);
        }
        Context ctx = new Context(null, alias, Context.SESSIONS);
        ctx.addServlet(new ServletHolder(servlet), "/*");
        if (params != null) {
            Enumeration e = params.keys();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                ctx.setAttribute(key, params.get(key));
            }
        }
        ctx.setServer(hcoll.getServer());
        try {
            ctx.start();
        } catch (Exception ex) {
            log.error("registerServlet, unable to start context: " + ex.getMessage());
            throw new ServletException("registerServlet, unable to start context: " + ex.getMessage(), ex);
        }
        cMap.put(alias, ctx);
        hcoll.addHandler(ctx);
        log.info("Registered servlet at " + alias + " for " + servlet.toString());
    }

    public void unregister(String alias) {
        if (!cMap.containsKey(alias)) {
            log.warn("Alias doesn't exist: " + alias);
            return;
        }
        try {
            ContextHandler ch = cMap.remove(alias);
            hcoll.removeHandler(ch);
            ch.stop();
        } catch (Exception ex) {
            log.error("Error stopping handler for alias: " + alias + ": " + ex.getMessage(), ex);
        }
        log.info("Unregistered " + alias);
    }

    public void registerWar(String alias, InputStream war, ClassLoader cl) throws ServletException, NamespaceException {
        if (alias == null || alias.length() == 0) {
            throw new NamespaceException("Invalid alias on registerWar: " + alias);
        }
        if (alias.length() > 0 && alias.charAt(alias.length() - 1) == '/') {
            alias = alias.substring(0, alias.length() - 1);
        }
        if (cMap.containsKey(alias)) {
            throw new NamespaceException("Alias already registered on registerWar: " + alias);
        }
        if (war == null) {
            throw new ServletException("Null war on registerWar");
        }
        String fname = alias + ".war";
        if (fname.lastIndexOf('/') != -1) {
            fname = fname.substring(fname.lastIndexOf('/'));
        }
        File tmp = new File(jstorage, fname);
        if (tmp.exists() && !tmp.delete()) {
            throw new ServletException("Temporary file exists and can't be deleted: " + tmp.getAbsolutePath());
        }
        byte[] buf = new byte[4096];
        try {
            FileOutputStream tf = new FileOutputStream(tmp);
            do {
                int read = war.read(buf, 0, buf.length);
                if (read == -1) {
                    break;
                }
                tf.write(buf, 0, read);
            } while (true);
            tf.close();
        } catch (IOException iox) {
            log.error("Error copying war file: " + iox.getMessage(), iox);
            tmp.delete();
            throw new ServletException("Cannot copy war file: " + iox.getMessage());
        }
        try {
            WebAppContext wac = new WebAppContext(tmp.getAbsolutePath(), alias);
            wac.setServer(hcoll.getServer());
            wac.start();
            cMap.put(alias, wac);
            hcoll.addHandler(wac);
        } catch (Exception ex) {
            throw new ServletException("Error deploying war file: " + ex.getMessage());
        }
        log.info("Registered war for " + alias);
    }

    public String getMimeType(String name) {
        return null;
    }

    public URL getResource(String name) {
        return null;
    }

    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return true;
    }
}
