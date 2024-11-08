package net.siuying.any2rss.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import de.nava.informa.exporters.RSS_1_0_Exporter;
import de.nava.informa.impl.basic.ChannelBuilder;
import net.siuying.any2rss.core.ContentProcessor;
import net.siuying.any2rss.core.ContentProcessorException;
import net.siuying.any2rss.handler.ContentHandlerException;
import net.siuying.any2rss.handler.MultiPatternContentHandler;
import net.siuying.any2rss.loader.HTTPLoader;
import net.siuying.any2rss.loader.LoaderException;
import org.apache.commons.configuration.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xrn.ant.JarHelper;

/**
 * @author Francis Chong
 * 
 */
public class Http2RssServlet extends HttpServlet {

    private static final long serialVersionUID = -757027187712206775L;

    private static String PROPERTIES_FILENAME = "http2rss-servlet.properties";

    private static Log log = LogFactory.getLog(Http2RssServlet.class);

    private static CompositeConfiguration config;

    private static Set<String> siteSet;

    private String propertiesFile;

    private HTTPLoader loader;

    private ChannelBuilder builder;

    private MultiPatternContentHandler handler;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        propertiesFile = config.getInitParameter("http2rss.config.file");
        loader = new HTTPLoader();
        handler = new MultiPatternContentHandler();
        builder = new ChannelBuilder();
        handler.setChannelBuilder(builder);
        try {
            setUp();
        } catch (ConfigurationException e) {
            log.warn("Error loading configuration ... " + e.getMessage());
            throw new RuntimeException("Configuration cannot be loaded! " + e.getMessage());
        }
    }

    protected void setUp() throws ConfigurationException {
        if (propertiesFile == null || propertiesFile.equals("")) {
            log.warn("Servlet init parameter http2rss.config.file " + "is not set! Using default configuration file: " + PROPERTIES_FILENAME);
            Properties prop = new Properties();
            InputStream in = getServletContext().getResourceAsStream("/WEB-INF/" + PROPERTIES_FILENAME);
            boolean copyJarCompleted = false;
            File defaultPropertiesFile = new File(PROPERTIES_FILENAME);
            try {
                if (!defaultPropertiesFile.exists()) {
                    log.warn("Default configuration file not exist, creating: " + PROPERTIES_FILENAME);
                    defaultPropertiesFile.createNewFile();
                    copyJarCompleted = JarHelper.copyFileFromJar("/WEB-INF/" + PROPERTIES_FILENAME, "", defaultPropertiesFile);
                }
            } catch (IOException e) {
                new ConfigurationException("Cannot create default properties file: " + PROPERTIES_FILENAME);
            }
            config = new CompositeConfiguration();
            config.addConfiguration(new SystemConfiguration());
            config.addConfiguration(new PropertiesConfiguration(defaultPropertiesFile));
        } else {
            config = new CompositeConfiguration();
            config.addConfiguration(new SystemConfiguration());
            config.addConfiguration(new PropertiesConfiguration(propertiesFile));
        }
        if (config != null) {
            loader.configure(config);
            handler.configure(config);
            siteSet = handler.getSites();
        }
    }

    /**
     * 
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        this.doPost(req, res);
    }

    /**
     * 
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String action = req.getParameter("a");
        String id = req.getParameter("id");
        String url = config.getString(MultiPatternContentHandler.KEY_PREFIX + id + ".url");
        String encoding = config.getString("encoding", "UTF-8");
        req.setCharacterEncoding(encoding);
        res.setCharacterEncoding(encoding);
        PrintWriter out = res.getWriter();
        if (action == null) {
            action = "list";
        }
        if (propertiesFile == null || config == null) {
            printSimpleMsg(out, "No Configuration", "Configuration is not completed, or problem occurs, please check server log.");
            return;
        }
        if (action.equals("get")) {
            if (id == null) {
                res.setContentType("text/html; charset=" + encoding);
                printSimpleMsg(out, "Parameter Missing", "id has not been set!");
                return;
            }
            if (!siteSet.contains(id)) {
                res.setContentType("text/html; charset=" + encoding);
                printSimpleMsg(out, "No configuration", "No configuration for specified ID!");
                return;
            }
            try {
                ContentProcessor processor = new ContentProcessor(config);
                processor.setLoader(loader);
                processor.setHandler(handler);
                handler.setHandler(id);
                processor.setContentUrl(new URL(url));
                processor.process();
                res.setContentType("text/xml; charset=" + encoding);
                RSS_1_0_Exporter exporter = new RSS_1_0_Exporter(out, encoding);
                exporter.write(processor.getChannel());
                return;
            } catch (ConfigurationException ce) {
                res.setContentType("text/html; charset=" + encoding);
                printSimpleMsg(out, "Configuration Error", "Error setup configuration: " + ce.getCause().getMessage());
                return;
            } catch (MalformedURLException e) {
                res.setContentType("text/html; charset=" + encoding);
                printSimpleMsg(out, "Invalid URL", "The input URL is invalid: " + url + ", details:" + e.getMessage());
                return;
            } catch (LoaderException e) {
                res.setContentType("text/html; charset=" + encoding);
                printSimpleMsg(out, "Error Loading URL", "Requested URL cannot be loaded: " + e.getCause().getMessage());
                return;
            } catch (ContentHandlerException e) {
                res.setContentType("text/html; charset=" + encoding);
                printSimpleMsg(out, "Error Parsing URL", "Requested URL cannot be parsed: " + e.getCause().getMessage());
                return;
            } catch (ContentProcessorException e) {
                res.setContentType("text/html; charset=" + encoding);
                printSimpleMsg(out, "Error Parsing URL", "An error has occured while converting content: " + e.getCause().getMessage());
                return;
            }
        } else if (action.equals("reset")) {
            try {
                setUp();
            } catch (ConfigurationException ce) {
                printSimpleMsg(out, "Configuration Error", "Error setup configuration: " + ce.getCause().getMessage());
            }
            return;
        } else if (action.equals("list")) {
            printSimpleMsg(out, "Avaliable Site", "TODO: List");
            return;
        }
    }

    private void printSimpleMsg(PrintWriter out, String title, String desc) {
        out.println("<html><head><title>" + title + "</title></head>" + "<body><h3>" + title + "</h3>" + "<p>" + desc + "</p>" + "</body></html>");
        out.flush();
    }
}
