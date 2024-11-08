package com.emailer4j.thread;

import org.apache.log4j.Logger;
import com.emailer4j.db.JDBSchema;
import com.emailer4j.db.JDBUser;
import com.emailer4j.sys.Common;
import com.emailer4j.util.JUnique;
import com.emailer4j.util.JUtility;

public class InterfaceThread extends Thread {

    private String hostID = "";

    private Boolean shutdown = false;

    private String sessionID = "";

    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    private OutboundEmailThread outboundEmailThread;

    private boolean threadsRunning = false;

    private final Logger logger = Logger.getLogger(InterfaceThread.class);

    private boolean abortThread = false;

    public InterfaceThread(String host, String session) {
        setHostID(host);
        setSessionID(session);
        abortThread = false;
    }

    public InterfaceThread(String[] args) {
        JUtility.initLogging("");
        Common.base_dir = System.getProperty("user.dir");
        String uniqueID = "";
        try {
            uniqueID = args[1];
            if (uniqueID.equals("")) {
                uniqueID = "emailer";
            }
        } catch (Exception ex) {
            uniqueID = "emailer";
        }
        logger.debug("Checking if parameter [" + uniqueID + "] is a Unique Host ID");
        Common.hostList.loadHosts();
        hostID = Common.hostList.getHostIDforUniqueId(uniqueID);
        if (hostID.length() > 0) {
            logger.debug("Unique ID = Host ID [" + hostID + "]");
        } else {
            logger.debug("Unique Ref not found, assume parameter is actual Host ID [" + hostID + "] (not recommended)");
            try {
                hostID = args[1];
            } catch (Exception ex) {
                hostID = "";
            }
            if (Common.hostList.isValidSite(hostID) == false) {
                logger.debug("Parameter [" + hostID + "] is not a valid Host ID - Interface thread aborting.");
                abortThread = true;
            }
        }
    }

    public Boolean isThreadAbortingStartup() {
        return abortThread;
    }

    public void requestStop() {
        shutdown = true;
    }

    private void setHostID(String host) {
        hostID = host;
    }

    private String getHostID() {
        return hostID;
    }

    public void run() {
        startupInterface();
    }

    public void shutdownInterface() {
    }

    public void startupInterface() {
        setSessionID(JUnique.getUniqueID());
        Common.sessionID = getSessionID();
        Common.sd.setData(getSessionID(), "silentExceptions", "Yes", true);
        Common.applicationMode = "SwingClient";
        JUtility.initLogging("");
        if (JUtility.isValidJavaVersion(Common.requiredJavaVersion) == true) {
            Common.hostList.loadHosts();
            Common.selectedHostID = getHostID();
            while (shutdown == false) {
                logger.debug("Connecting to database.");
                while ((Common.hostList.getHost(getHostID()).isConnected(getSessionID()) == false) && (shutdown == false)) {
                    Common.hostList.getHost(getHostID()).connect(getSessionID(), getHostID());
                }
                if ((Common.hostList.getHost(getHostID()).isConnected(getSessionID()) == true) && (shutdown == false)) {
                    JDBSchema schema = new JDBSchema(getSessionID(), Common.hostList.getHost(getHostID()));
                    schema.validate(false);
                    JDBUser user = new JDBUser(getHostID(), getSessionID());
                    user.setUserId("INTERFACE");
                    user.setPassword("INTERFACE");
                    Common.userList.addUser(getSessionID(), user);
                    if (user.login()) {
                        logger.debug("Starting Threads....");
                        startupThreads();
                        while (shutdown == false) {
                            com.emailer4j.util.JWait.milliSec(1000);
                        }
                        logger.debug("Stopping Threads....");
                        shutdownThreads();
                        user.logout();
                    }
                    logger.debug("Disconnecting from database.");
                    Common.hostList.getHost(getHostID()).disconnectAll();
                }
            }
        }
    }

    public void startupThreads() {
        logger.debug("Starting Email Thread.....");
        com.emailer4j.util.JWait.milliSec(100);
        outboundEmailThread = new OutboundEmailThread(getHostID());
        outboundEmailThread.setName("Email Interface");
        outboundEmailThread.start();
        com.emailer4j.util.JWait.milliSec(100);
        threadsRunning = true;
    }

    public void shutdownThreads() {
        if (threadsRunning) {
            com.emailer4j.util.JWait.milliSec(100);
            threadsRunning = false;
            logger.debug("Stopping Email Interface Thread.....");
            com.emailer4j.util.JWait.milliSec(100);
            outboundEmailThread.allDone = true;
            try {
                while (outboundEmailThread.isAlive()) {
                    outboundEmailThread.allDone = true;
                    com.emailer4j.util.JWait.milliSec(100);
                }
            } catch (Exception ex) {
            }
            logger.debug("Email Interface Thread Stopped.");
        }
    }
}
