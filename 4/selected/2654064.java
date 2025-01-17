package org.mobicents.servlet.sip.alerting.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.spi.Driver;
import javax.media.mscontrol.spi.DriverManager;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.log4j.Logger;
import org.mobicents.javax.media.mscontrol.spi.DriverImpl;

/**
 * @author <A HREF="mailto:jean.deruelle@gmail.com">Jean Deruelle</A> 
 * 
 */
public class InitializationListener implements ServletContextListener {

    private static Logger logger = Logger.getLogger(InitializationListener.class);

    private static final String AUDIO_DIR = "/audio";

    private static final String FILE_PROTOCOL = "file://";

    private static final String[] AUDIO_FILES = new String[] {};

    Properties properties = null;

    private static final String MS_CONTROL_FACTORY = "MsControlFactory";

    public static final String PR_JNDI_NAME = "media/trunk/PacketRelay/$";

    public static final String MGCP_STACK_NAME = "mgcp.stack.name";

    public static final String MGCP_STACK_IP = "mgcp.server.address";

    public static final String MGCP_STACK_PORT = "mgcp.local.port";

    public static final String MGCP_PEER_IP = "mgcp.bind.address";

    public static final String MGCP_PEER_PORT = "mgcp.server.port";

    /**
	 * In this case MGW and CA are on same local host
	 */
    public static final String LOCAL_ADDRESS = System.getProperty("jboss.bind.address", "127.0.0.1");

    protected static final String CA_PORT = "2828";

    public static final String PEER_ADDRESS = System.getProperty("jboss.bind.address", "127.0.0.1");

    protected static final String MGW_PORT = "2427";

    public void contextDestroyed(ServletContextEvent event) {
        Iterator<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasNext()) {
            Driver driver = drivers.next();
            DriverManager.deregisterDriver(driver);
            DriverImpl impl = (DriverImpl) driver;
            impl.shutdown();
        }
    }

    public void contextInitialized(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();
        File tempWriteDir = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
        String audioFilePath = FILE_PROTOCOL + tempWriteDir.getAbsolutePath() + File.separatorChar;
        servletContext.setAttribute("audioFilePath", audioFilePath);
        logger.info("Setting audioFilePath param to " + audioFilePath);
        for (int i = 0; i < AUDIO_FILES.length; i++) {
            String audioFile = AUDIO_FILES[i];
            logger.info("Writing " + audioFile + " to webapp temp dir : " + tempWriteDir);
            InputStream is = servletContext.getResourceAsStream(AUDIO_DIR + "/" + audioFile);
            copyToTempDir(is, tempWriteDir, audioFile);
        }
        Enumeration<String> initParamNames = servletContext.getInitParameterNames();
        logger.info("Setting init Params into application context");
        while (initParamNames.hasMoreElements()) {
            String initParamName = (String) initParamNames.nextElement();
            servletContext.setAttribute(initParamName, servletContext.getInitParameter(initParamName));
            logger.info("Param key=" + initParamName + ", value = " + servletContext.getInitParameter(initParamName));
        }
        servletContext.setAttribute("registeredUsersMap", new HashMap<String, String>());
        if (servletContextEvent.getServletContext().getAttribute(MS_CONTROL_FACTORY) == null) {
            DriverImpl d = new DriverImpl();
            properties = new Properties();
            properties.setProperty(MGCP_STACK_NAME, "SipServlets");
            properties.setProperty(MGCP_PEER_IP, PEER_ADDRESS);
            properties.setProperty(MGCP_PEER_PORT, MGW_PORT);
            properties.setProperty(MGCP_STACK_IP, LOCAL_ADDRESS);
            properties.setProperty(MGCP_STACK_PORT, CA_PORT);
            try {
                final MsControlFactory msControlFactory = new DriverImpl().getFactory(properties);
                MMSUtil.msControlFactory = msControlFactory;
                servletContextEvent.getServletContext().setAttribute(MS_CONTROL_FACTORY, msControlFactory);
                logger.info("started MGCP Stack on " + LOCAL_ADDRESS + "and port " + CA_PORT + " obj: " + MMSUtil.msControlFactory);
            } catch (Exception e) {
                logger.error("couldn't start the underlying MGCP Stack", e);
            }
        } else {
            logger.info("MGCP Stack already started on " + LOCAL_ADDRESS + "and port " + CA_PORT);
        }
    }

    private void copyToTempDir(InputStream is, File tempWriteDir, String fileName) {
        File file = new File(tempWriteDir, fileName);
        final int bufferSize = 1000;
        BufferedOutputStream fout = null;
        BufferedInputStream fin = null;
        try {
            fout = new BufferedOutputStream(new FileOutputStream(file));
            fin = new BufferedInputStream(is);
            byte[] buffer = new byte[bufferSize];
            int readCount = 0;
            while ((readCount = fin.read(buffer)) != -1) {
                if (readCount < bufferSize) {
                    fout.write(buffer, 0, readCount);
                } else {
                    fout.write(buffer);
                }
            }
        } catch (IOException e) {
            logger.error("An unexpected exception occured while copying audio files", e);
        } finally {
            try {
                if (fout != null) {
                    fout.flush();
                    fout.close();
                }
            } catch (IOException e) {
                logger.error("An unexpected exception while closing stream", e);
            }
            try {
                if (fin != null) {
                    fin.close();
                }
            } catch (IOException e) {
                logger.error("An unexpected exception while closing stream", e);
            }
        }
    }
}
