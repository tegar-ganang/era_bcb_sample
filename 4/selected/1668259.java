package example.runtime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.mortbay.jetty.handler.RequestLogHandler;
import org.mortbay.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.access.jetty.RequestLogImpl;

/**
 * Manages an embedded Jetty instance which uses a subfolder named {@code war} of the project
 * directory as it's web application context. This class is intended specifically to be
 * used inside of a JAR file, which contains the mentioned {@code war} directory.
 *
 * <p>The default host is {@code 0.0.0.0}, you may override it by setting an
 * environment variable {@code http.host}
 *
 * <p>The default HTTP port is {@code 8080}, you may override it by setting an environment variable
 * {@code http.port}.
 *
 * <p>The default HTTPS port is {@code 8443}, you may override it by setting an environment variable
 * {@code https.port}.
 */
public final class Standalone extends Development {

    private static final Logger log = LoggerFactory.getLogger(Standalone.class);

    /**
   * Creates a new instance of this class and calls it's {@link #startup()} method.
   */
    public static void main(final String[] args) {
        new Standalone().startup();
    }

    @Override
    protected List<WebAppContext> createWebAppContexts() {
        List<WebAppContext> contexts = new ArrayList<WebAppContext>(2);
        WebAppContext wac = new WebAppContext();
        wac.setContextPath("/");
        wac.setWar(Thread.currentThread().getContextClassLoader().getResource("war/").toExternalForm());
        contexts.add(wac);
        return contexts;
    }

    @Override
    protected RequestLogHandler createRequestLogHandler() {
        try {
            File logbackConf = File.createTempFile("logback-access", ".xml");
            IOUtils.copy(Thread.currentThread().getContextClassLoader().getResourceAsStream("logback-access.xml"), new FileOutputStream(logbackConf));
            RequestLogHandler requestLogHandler = new RequestLogHandler();
            RequestLogImpl requestLog = new RequestLogImpl();
            requestLog.setFileName(logbackConf.getPath());
            requestLogHandler.setRequestLog(requestLog);
        } catch (FileNotFoundException e) {
            log.error("Could not create request log handler", e);
        } catch (IOException e) {
            log.error("Could not create request log handler", e);
        }
        return null;
    }
}
