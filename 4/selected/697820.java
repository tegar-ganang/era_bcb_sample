package com.chazaqdev.etei;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 *
 * @author mike
 */
public class JettyServerHandler {

    private Server server;

    private SelectChannelConnector connector;

    private boolean allowRemoteConnections = true;

    private int port = 8080;

    private WebAppContext webAppContext;

    public void start(String war) {
        String jetty_home = System.getProperty("jetty.home", AppDataDir.getTheApplicationsDataDir());
        System.out.println("Jetty home:" + jetty_home);
        System.setProperty("jetty.home", jetty_home);
        server = new Server();
        addConnector();
        webAppContext = new WebAppContext();
        String s = AppDataDir.getFirstWarYouCanFind();
        System.out.println("#" + s);
        File f = new File(s.substring(0, s.length() - 4));
        if (!f.exists()) f.mkdir();
        webAppContext.setTempDirectory(f);
        webAppContext.setContextPath("/");
        System.out.println("Found \"" + s + "\"");
        webAppContext.setWar(s);
        server.setHandler(webAppContext);
        new Thread(new Runnable() {

            public void run() {
                try {
                    server.start();
                    server.join();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public boolean isStarted() {
        return server.isStarted();
    }

    public boolean isStarting() {
        return server.isStarting();
    }

    public boolean isRunning() {
        return server.isRunning();
    }

    public boolean isFailed() {
        return server.isFailed();
    }

    public boolean isStopping() {
        return server.isStopping();
    }

    public boolean isStopped() {
        return server.isStopped();
    }

    public WebAppContext getWebAppContext() {
        return webAppContext;
    }

    public Handler[] getHandlers() {
        return server.getHandlers();
    }

    public SelectChannelConnector getChannelConnector() {
        return connector;
    }

    public int getPort() {
        return port;
    }

    public void stop() {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception ex) {
                Logger.getLogger(JettyServerHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void addConnector() {
        connector = new SelectChannelConnector();
        connector.setPort(port);
        if (!allowRemoteConnections) connector.setHost("127.0.0.1");
        server.addConnector(connector);
    }
}
