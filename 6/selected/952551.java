package com.commander4j.thread;

import java.util.LinkedList;
import org.apache.commons.beanutils.converters.ArrayConverter;
import org.apache.commons.beanutils.converters.StringConverter;
import org.apache.log4j.Logger;
import com.commander4j.app.JVersion;
import com.commander4j.db.JDBControl;
import com.commander4j.db.JDBInterface;
import com.commander4j.db.JDBSchema;
import com.commander4j.db.JDBUser;
import com.commander4j.email.JeMail;
import com.commander4j.messages.GenericMessageHeader;
import com.commander4j.sys.Common;
import com.commander4j.sys.JLaunchReport;
import com.commander4j.util.JPrint;
import com.commander4j.util.JUnique;
import com.commander4j.util.JUtility;

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

    private OutboundMessageThread outboundThread;

    private ReportingThread reportingThread;

    private InboundMessageCollectionThread fileCollectThread;

    private InboundMessageThread inboundThread;

    private boolean threadsRunning = false;

    private boolean houseKeeping = false;

    private int secondsBeforeHousekeeping = 86400;

    private Boolean enableEnterfaceStatusEmails = false;

    private String siteName = "";

    private String interfaceEmailAddresses = "";

    private int secondsRemaining = secondsBeforeHousekeeping;

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
        } catch (Exception ex) {
            uniqueID = "undefined";
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
        JPrint.init();
        if (JUtility.isValidJavaVersion(Common.requiredJavaVersion) == true) {
            Common.hostList.loadHosts();
            Common.selectedHostID = getHostID();
            houseKeeping = false;
            while (shutdown == false) {
                secondsRemaining = secondsBeforeHousekeeping;
                logger.debug("Connecting to database.");
                while ((Common.hostList.getHost(getHostID()).isConnected(getSessionID()) == false) && (shutdown == false)) {
                    Common.hostList.getHost(getHostID()).connect(getSessionID(), getHostID());
                }
                if ((Common.hostList.getHost(getHostID()).isConnected(getSessionID()) == true) && (shutdown == false)) {
                    JDBSchema schema = new JDBSchema(getSessionID(), Common.hostList.getHost(getHostID()));
                    schema.validate(false);
                    JUtility.initEANBarcode();
                    JLaunchReport.init();
                    Common.init();
                    JDBUser user = new JDBUser(getHostID(), getSessionID());
                    JDBControl ctrl = new JDBControl(getHostID(), getSessionID());
                    JeMail mail = new JeMail(getHostID(), getSessionID());
                    user.setUserId("INTERFACE");
                    user.setPassword("INTERFACE");
                    Common.userList.addUser(getSessionID(), user);
                    enableEnterfaceStatusEmails = Boolean.parseBoolean(ctrl.getKeyValueWithDefault("INTERFACE EMAIL NOTIFY", "false", "Email startup and shutdown events :- true or false"));
                    interfaceEmailAddresses = ctrl.getKeyValueWithDefault("INTERFACE ADMIN EMAIL", "someone@somewhere.com", "Email address for startup and shutdown events.");
                    StringConverter stringConverter = new StringConverter();
                    ArrayConverter arrayConverter = new ArrayConverter(String[].class, stringConverter);
                    arrayConverter.setDelimiter(';');
                    arrayConverter.setAllowedChars(new char[] { '@', '_' });
                    String[] emailList = (String[]) arrayConverter.convert(String[].class, interfaceEmailAddresses);
                    siteName = Common.hostList.getHost(getHostID()).getSiteDescription();
                    if (user.login()) {
                        if (enableEnterfaceStatusEmails == true) {
                            try {
                                String subject = "";
                                if (houseKeeping == true) {
                                    subject = "Commander4j " + JVersion.getProgramVersion() + " Interface maintenance restart for [" + siteName + "] on " + JUtility.getClientName();
                                } else {
                                    subject = "Commander4j " + JVersion.getProgramVersion() + " Interface startup for [" + siteName + "] on " + JUtility.getClientName();
                                }
                                mail.postMail(emailList, subject, "Interface service has started.", "", "");
                            } catch (Exception ex) {
                                logger.error("InterfaceThread Unable to send emails");
                            }
                        }
                        houseKeeping = false;
                        logger.debug("Interface Logged on successfully");
                        logger.debug("Starting Threads....");
                        secondsBeforeHousekeeping = Integer.valueOf(ctrl.getKeyValueWithDefault("INTERFACE HOUSEKEEPING INTERVAL", "86400", "Frequency in seconds."));
                        secondsRemaining = secondsBeforeHousekeeping;
                        startupThreads();
                        while ((shutdown == false) & (houseKeeping == false)) {
                            com.commander4j.util.JWait.milliSec(1000);
                            secondsRemaining--;
                            if (secondsRemaining == 0) {
                                houseKeeping = true;
                            }
                        }
                        logger.debug("Stopping Threads....");
                        shutdownThreads();
                        user.logout();
                        logger.debug("Interface Logged out successfully");
                        if (enableEnterfaceStatusEmails == true) {
                            try {
                                String subject = "";
                                if (houseKeeping == true) {
                                    subject = "Commander4j " + JVersion.getProgramVersion() + " Interface maintenance shutdown for [" + siteName + "] on " + JUtility.getClientName();
                                } else {
                                    subject = "Commander4j " + JVersion.getProgramVersion() + " Interface shutdown for [" + siteName + "] on " + JUtility.getClientName();
                                }
                                mail.postMail(emailList, subject, "Interface service has stopped.", "", "");
                            } catch (Exception ex) {
                                logger.error("InterfaceThread Unable to send emails");
                            }
                        }
                    } else {
                        logger.debug("Interface routine failed to logon to application using account INTERFACE");
                    }
                    logger.debug("Disconnecting from database.");
                    Common.hostList.getHost(getHostID()).disconnectAll();
                    if (houseKeeping == true) {
                        logger.debug("HOUSEKEEPING START");
                        String memoryBefore = "Memory used before garbage collection = " + String.valueOf((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024) + "k";
                        System.gc();
                        String memoryAfter = "Memory used after garbage collection  = " + String.valueOf((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024) + "k";
                        String stats = GenericMessageHeader.getStats();
                        GenericMessageHeader.clearStats();
                        if (enableEnterfaceStatusEmails == true) {
                            try {
                                mail.postMail(emailList, "Commander4j " + JVersion.getProgramVersion() + " Interface maintenance for [" + siteName + "] on " + JUtility.getClientName(), memoryBefore + "\n\n" + memoryAfter + "\n\n" + "Maintenance is scheduled to occur every " + String.valueOf(secondsBeforeHousekeeping) + " seconds.\n\n\n\n" + stats, "", "");
                            } catch (Exception ex) {
                                logger.error("InterfaceThread Unable to send emails");
                            }
                        }
                        logger.debug("Interface Garbage Collection.");
                        logger.debug("HOUSEKEEPING END");
                    }
                }
            }
        }
    }

    public void startupThreads() {
        logger.debug("Starting Inbound Interface Thread.....");
        com.commander4j.util.JWait.milliSec(100);
        inboundThread = new InboundMessageThread(getHostID(), Common.interface_recovery_path, Common.interface_error_path, Common.interface_backup_path);
        inboundThread.setName("C4J Inbound Interface");
        inboundThread.start();
        logger.debug("Starting Outbound Interface Thread.....");
        com.commander4j.util.JWait.milliSec(100);
        outboundThread = new OutboundMessageThread(getHostID());
        outboundThread.setName("C4J Outbound Interface");
        outboundThread.start();
        com.commander4j.util.JWait.milliSec(100);
        LinkedList<String> test1 = new LinkedList<String>();
        JDBInterface interfaces = new JDBInterface(getHostID(), getSessionID());
        test1 = interfaces.getInputPaths();
        logger.debug("Starting File Collection Thread.....");
        fileCollectThread = new InboundMessageCollectionThread(test1, Common.interface_recovery_path);
        fileCollectThread.setName("C4J Recovery Thread");
        fileCollectThread.start();
        logger.debug("Starting Reporting Thread.....");
        com.commander4j.util.JWait.milliSec(100);
        reportingThread = new ReportingThread(getHostID());
        reportingThread.setName("C4J Reporting Thread");
        reportingThread.start();
        com.commander4j.util.JWait.milliSec(100);
        threadsRunning = true;
    }

    public void shutdownThreads() {
        if (threadsRunning) {
            logger.debug("Stopping Reporting Thread.....");
            com.commander4j.util.JWait.milliSec(100);
            reportingThread.allDone = true;
            try {
                while (reportingThread.isAlive()) {
                    reportingThread.allDone = true;
                    com.commander4j.util.JWait.milliSec(100);
                }
            } catch (Exception ex1) {
            }
            com.commander4j.util.JWait.milliSec(5000);
            logger.debug("Reporting Thread Stopped.");
            com.commander4j.util.JWait.milliSec(100);
            threadsRunning = false;
            logger.debug("Stopping Inbound Interface Thread.....");
            com.commander4j.util.JWait.milliSec(100);
            inboundThread.allDone = true;
            try {
                while (inboundThread.isAlive()) {
                    inboundThread.allDone = true;
                    com.commander4j.util.JWait.milliSec(100);
                }
            } catch (Exception ex) {
            }
            logger.debug("Inbound Interface Thread Stopped.");
            logger.debug("Stopping Outbound Interface Thread.....");
            com.commander4j.util.JWait.milliSec(100);
            outboundThread.allDone = true;
            try {
                while (outboundThread.isAlive()) {
                    outboundThread.allDone = true;
                    com.commander4j.util.JWait.milliSec(100);
                }
            } catch (Exception ex1) {
            }
            logger.debug("Outbound Interface Thread Stopped.");
            logger.debug("Stopping File Collection Thread.....");
            com.commander4j.util.JWait.milliSec(100);
            fileCollectThread.allDone = true;
            try {
                while (fileCollectThread.isAlive()) {
                    fileCollectThread.allDone = true;
                    com.commander4j.util.JWait.milliSec(100);
                }
            } catch (Exception ex2) {
            }
            logger.debug("File Collection Thread Stopped.");
        }
    }
}
