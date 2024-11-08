package org.opennms.netmgt.collectd;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.apache.commons.net.telnet.EchoOptionHandler;
import org.apache.commons.net.telnet.InvalidTelnetOptionException;
import org.apache.commons.net.telnet.SuppressGAOptionHandler;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.commons.net.telnet.TelnetNotificationHandler;
import org.apache.commons.net.telnet.TerminalTypeOptionHandler;
import org.apache.log4j.Category;
import org.apache.log4j.Priority;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.Marshaller;
import org.exolab.castor.xml.ValidationException;
import org.opennms.core.resource.db.DbConnectionFactory;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.EventConstants;
import org.opennms.netmgt.config.TelnetCollectionConfigFactory;
import org.opennms.netmgt.config.DatabaseConnectionFactory;
import org.opennms.netmgt.config.TelnetPeerFactory;
import org.opennms.netmgt.config.collectd.ItemMapping;
import org.opennms.netmgt.config.collectd.TelnetCollection;
import org.opennms.netmgt.poller.monitors.NetworkInterface;
import org.opennms.netmgt.utils.EventProxy;
import org.opennms.netmgt.utils.EventProxyException;
import org.opennms.netmgt.utils.ParameterMap;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.config.collectd.*;
import org.opennms.netmgt.config.inventory.parser.Item;

/**
 * <P>
 * The TelnetCollector class ...
 * </P>
 * @author <A HREF="mailto:">Maurizio Migliore</A>
 * @author <A HREF="mailto:mike@opennms.org">Mike Davidson </A>
 * @author <A HREF="http://www.opennms.org/">OpenNMS </A>
 *  
 */
final class TelnetCollector implements ServiceCollector {

    /**
	 * Name of monitored service.
	 */
    private static final String SERVICE_NAME = "Telnet";

    private static String TELNET_STORAGE_ALL = "all";

    /**
	 * Path to Telnet Data file repository.
	 */
    private String m_inventoryPath;

    /**
	 * Local host name
	 */
    private String m_host;

    private static final String SELECT_PATHTOFILE = "SELECT inventory.pathtofile from inventory, ipinterface where  inventory.name=? and inventory.status='A' and  inventory.nodeid=ipInterface.nodeId and ipInterface.ipAddr = ?;";

    private static final String SELECT_NODEID_BY_INTERFACE = "SELECT nodeId FROM ipInterface WHERE ipAddr = ?";

    /**
	 * <P>
	 * Returns the name of the service that the plug-in collects ("Telnet").
	 * </P>
	 * 
	 * @return The service that the plug-in collects.
	 */
    public String serviceName() {
        return SERVICE_NAME;
    }

    /**
	 * <P>
	 * Initialize the service collector.
	 * </P>
	 * 
	 * <P>
	 * During initialization the Telnet collector: - Initializes various
	 * configuration factories. - Verifies access to the database - Verifies
	 * access to inventory file repository.
	 * </P>
	 * 
	 * @param parameters
	 *            Not currently used.
	 * 
	 * @exception RuntimeException
	 *                Thrown if an unrecoverable error occurs that prevents the
	 *                plug-in from functioning.
	 *  
	 */
    public void initialize(Map parameters) {
        Category log = ThreadCategory.getInstance(getClass());
        try {
            m_host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            if (log.isEnabledFor(Priority.WARN)) log.warn("initialize: Unable to resolve local host name.", e);
            m_host = "unresolved.host";
        }
        try {
            TelnetPeerFactory.init();
        } catch (MarshalException ex) {
            if (log.isEnabledFor(Priority.FATAL)) log.fatal("initialize: Failed to load telnet data collection configuration", ex);
            throw new UndeclaredThrowableException(ex);
        } catch (ValidationException ex) {
            if (log.isEnabledFor(Priority.FATAL)) log.fatal("initialize: Failed to load telnet data collection configuration", ex);
            throw new UndeclaredThrowableException(ex);
        } catch (IOException ex) {
            if (log.isEnabledFor(Priority.FATAL)) log.fatal("initialize: Failed to load telnet data collection configuration", ex);
            throw new UndeclaredThrowableException(ex);
        }
        try {
            TelnetCollectionConfigFactory.init();
        } catch (MarshalException ex) {
            if (log.isEnabledFor(Priority.FATAL)) log.fatal("initialize: Failed to load telnet data collection configuration", ex);
            throw new UndeclaredThrowableException(ex);
        } catch (ValidationException ex) {
            if (log.isEnabledFor(Priority.FATAL)) log.fatal("initialize: Failed to load telnet data collection configuration", ex);
            throw new UndeclaredThrowableException(ex);
        } catch (IOException ex) {
            if (log.isEnabledFor(Priority.FATAL)) log.fatal("initialize: Failed to load telnet data collection configuration", ex);
            throw new UndeclaredThrowableException(ex);
        }
        java.sql.Connection ctest = null;
        try {
            DatabaseConnectionFactory.init();
            ctest = DatabaseConnectionFactory.getInstance().getConnection();
        } catch (IOException ie) {
            if (log.isEnabledFor(Priority.FATAL)) log.fatal("initialize: IOException getting database connection", ie);
            throw new UndeclaredThrowableException(ie);
        } catch (MarshalException me) {
            if (log.isEnabledFor(Priority.FATAL)) log.fatal("initialize: Marshall Exception getting database connection", me);
            throw new UndeclaredThrowableException(me);
        } catch (ValidationException ve) {
            if (log.isEnabledFor(Priority.FATAL)) log.fatal("initialize: Validation Exception getting database connection", ve);
            throw new UndeclaredThrowableException(ve);
        } catch (SQLException sqlE) {
            if (log.isEnabledFor(Priority.FATAL)) log.fatal("initialize: Failed getting connection to the database.", sqlE);
            throw new UndeclaredThrowableException(sqlE);
        } catch (ClassNotFoundException cnfE) {
            if (log.isEnabledFor(Priority.FATAL)) log.fatal("initialize: Failed loading database driver.", cnfE);
            throw new UndeclaredThrowableException(cnfE);
        } finally {
            if (ctest != null) {
                try {
                    ctest.close();
                } catch (Throwable t) {
                    if (log.isEnabledFor(Priority.WARN)) log.warn("initialize: an exception occured while closing the JDBC connection", t);
                }
            }
        }
        m_inventoryPath = TelnetCollectionConfigFactory.getInstance().getInventoryRepository();
        if (m_inventoryPath == null) throw new RuntimeException("Configuration error, failed to retrieve path to inventory repository.");
        if (m_inventoryPath.endsWith(File.separator)) {
            m_inventoryPath = m_inventoryPath.substring(0, (m_inventoryPath.length() - File.separator.length()));
        }
        if (log.isDebugEnabled()) log.debug("initialize: Telnet inventory file repository path: " + m_inventoryPath);
        log.info("Creating inventory repository path");
        File f = new File(m_inventoryPath);
        if (!f.isDirectory()) if (!f.mkdirs()) throw new RuntimeException("Unable to create inventory file repository, path: " + m_inventoryPath);
        return;
    }

    /**
	 * Responsible for freeing up any resources held by the collector.
	 */
    public void release() {
    }

    /**
	 * Responsible for performing all necessary initialization for the specified
	 * interface in preparation for data collection.
	 * 
	 * @param iface
	 *            Network interface to be prepped for collection.
	 * @param parameters
	 *            Key/value pairs associated with the package to which the
	 *            interface belongs..
	 *  
	 */
    public void initialize(NetworkInterface iface, Map parameters) {
        Category log = ThreadCategory.getInstance(getClass());
        if (iface.getType() != NetworkInterface.TYPE_IPV4) throw new RuntimeException("Unsupported interface type, only TYPE_IPV4 currently supported");
        InetAddress ipAddr = (InetAddress) iface.getAddress();
        String collectionName = ParameterMap.getKeyedString(parameters, "collection", "default");
        TelnetCollection telnetColl = TelnetCollectionConfigFactory.getInstance().getTelnetCollection(collectionName);
        if (telnetColl == null) {
            if (log.isEnabledFor(Priority.WARN)) log.warn("initialize: Configuration error, failed to retrieve telnet info for collection: " + collectionName);
        }
        java.sql.Connection dbConn = null;
        try {
            DatabaseConnectionFactory.init();
        } catch (Exception e) {
            if (log.isEnabledFor(Priority.ERROR)) log.error("initialize: init database connection factory Failed .", e.getCause());
            throw new UndeclaredThrowableException(e);
        }
        return;
    }

    /**
	 * Responsible for releasing any resources associated with the specified
	 * interface.
	 * 
	 * @param iface
	 *            Network interface to be released.
	 */
    public void release(NetworkInterface iface) {
    }

    /**
	 * Perform data collection.
	 * 
	 * @param iface
	 *            Network interface to be data collected.
	 * @param eproxy
	 *            Eventy proxy for sending events.
	 * @param parameters
	 *            Key/value pairs from the package to which the interface
	 *            belongs.
	 */
    public int collect(NetworkInterface iface, EventProxy eproxy, Map parameters) {
        Category log = ThreadCategory.getInstance(getClass());
        int collectionResult = COLLECTION_UNKNOWN;
        InetAddress ipaddr = (InetAddress) iface.getAddress();
        log.info("Collecting Telnet data for interface " + ipaddr.getHostAddress());
        java.sql.Connection dbConn = null;
        try {
            dbConn = DatabaseConnectionFactory.getInstance().getConnection();
            dbConn.setAutoCommit(false);
        } catch (SQLException s) {
            log.error("Unable to connect to DB");
            return COLLECTION_FAILED;
        }
        String collectionName = ParameterMap.getKeyedString(parameters, "collection", "default");
        try {
            TelnetCollectionConfigFactory.init();
        } catch (Exception ex) {
            log.error("Error while init telnetcollectionconfigfactory " + ex.getCause());
            return COLLECTION_FAILED;
        }
        TelnetCollection telnetColl = TelnetCollectionConfigFactory.getInstance().getTelnetCollection(collectionName);
        log.info("Collection : " + collectionName + " with " + telnetColl.getItemMappingCount() + " ItemMappings");
        TelnetBroker tb = null;
        List fileToCancel = null;
        if (telnetColl.getItemMappingCount() > 0) {
            Iterator itemMappIter = telnetColl.getItemMappingCollection().iterator();
            try {
                tb = new TelnetBroker(ipaddr.getHostAddress(), parameters);
                log.info("TelnetBroker instantiated");
            } catch (Exception ex) {
                log.error("Telnet Broker instantiation error: " + ex);
                return COLLECTION_FAILED;
            }
            int timeout = Integer.parseInt((String) parameters.get("timeout"));
            int retry = Integer.parseInt((String) parameters.get("retry"));
            int saveResult = COLLECTION_UNKNOWN;
            log.debug("Parameters loaded: timeout=" + timeout + " retry=" + retry);
            log.info("Getting peer exchanges...");
            Exchange[] excgs = TelnetPeerFactory.getInstance().getPeer(ipaddr);
            for (int c = 0; c < excgs.length; c++) {
                log.debug("prompt:" + excgs[c].getPrompt() + " command:" + excgs[c].getCommand() + " errorprompt" + excgs[c].getErrorprompt());
            }
            ATTEMPTFOR: for (int attempt = 1; attempt <= retry; attempt++) {
                log.debug("Attempt " + attempt + " of " + retry);
                try {
                    if (tb.openConnection(excgs, timeout)) {
                        log.info("Telnet Connection with peer opened");
                    } else {
                        log.info("Telnet Connection with peer NOT opened");
                        collectionResult = COLLECTION_FAILED;
                        continue ATTEMPTFOR;
                    }
                } catch (IOException io) {
                    log.error("Telnet open connection error for attempt " + attempt + " \n" + io.getCause());
                    collectionResult = COLLECTION_FAILED;
                    continue ATTEMPTFOR;
                }
                int nodeId = 0;
                ResultSet rs = null;
                try {
                    PreparedStatement stmt = dbConn.prepareStatement(SELECT_NODEID_BY_INTERFACE);
                    stmt.setString(1, ipaddr.getHostAddress());
                    rs = stmt.executeQuery();
                    if (rs.next()) {
                        nodeId = rs.getInt(1);
                    }
                    log.debug("Retrieved nodeid for " + ipaddr.getHostAddress() + " :" + nodeId);
                    stmt.close();
                } catch (SQLException s) {
                    log.error("Unable to read from DB for attempt " + attempt + " \n" + s.getCause());
                    collectionResult = COLLECTION_FAILED;
                    continue ATTEMPTFOR;
                }
                fileToCancel = new java.util.ArrayList();
                log.info("itering on ItemMappings...");
                while (itemMappIter.hasNext()) {
                    ItemMapping im = (ItemMapping) itemMappIter.next();
                    CommandLine cmdLine = im.getCommandLine();
                    String response = null;
                    try {
                        response = tb.doCommand(cmdLine, timeout);
                        log.info("Response for '" + cmdLine.getCommand() + "' = " + response);
                    } catch (Exception ex) {
                        log.error("Telnet command error for attempt " + attempt + " \n" + ex.getCause());
                        try {
                            tb.closeConnection();
                        } catch (IOException io) {
                            log.error("Telnet Close connection error  for attempt " + attempt + " \n" + io.getCause());
                        }
                        collectionResult = COLLECTION_FAILED;
                        continue ATTEMPTFOR;
                    }
                    if (response != null) {
                        org.opennms.netmgt.config.collectd.Correspondence[] corrs = im.getCorrespondence();
                        log.debug("itering on correspondences...");
                        for (int i = 0; i < corrs.length; i++) {
                            Correspondence corr = corrs[i];
                            if (corr.getAssetField() == null && (corr.hasSaveXmlData() && corr.getSaveXmlData() == false)) {
                                log.error("Correspondence must have either asset-field setted or savexmldata!=false");
                                try {
                                    tb.closeConnection();
                                } catch (IOException io) {
                                    log.error("Telnet close connection error: " + io);
                                    continue ATTEMPTFOR;
                                }
                                return COLLECTION_FAILED;
                            }
                            log.info("Handling response...");
                            String handledRsp = handleCorrespondence(response, corr, collectionName);
                            log.debug("Handled response is: " + handledRsp);
                            String oldPath = null;
                            try {
                                PreparedStatement stmt = dbConn.prepareStatement(SELECT_PATHTOFILE);
                                stmt.setString(1, corr.getName());
                                stmt.setString(2, ipaddr.getHostAddress());
                                rs = stmt.executeQuery();
                                while (rs.next()) {
                                    oldPath = rs.getString(1);
                                }
                            } catch (SQLException s) {
                                log.error("Unable to read from DB for attempt " + attempt + " \n" + s.getCause());
                                collectionResult = COLLECTION_FAILED;
                                continue ATTEMPTFOR;
                            }
                            String inventoryRepository = TelnetCollectionConfigFactory.getInstance().getInventoryRepository();
                            log.debug("Inventory repository loaded from config file: " + inventoryRepository);
                            String nodeDirectory_repository = "";
                            String newPathToFile = null;
                            if (!corr.hasSaveXmlData() || corr.getSaveXmlData() == true) {
                                if (inventoryRepository.endsWith("/") == false && inventoryRepository.endsWith(File.separator) == false) {
                                    inventoryRepository += File.separator;
                                }
                                nodeDirectory_repository = inventoryRepository + nodeId + File.separator;
                                long time = System.currentTimeMillis();
                                Timestamp currTime = new Timestamp(time);
                                java.util.Date currTimeDate = new java.util.Date(currTime.getTime());
                                SimpleDateFormat ObjectformatDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                newPathToFile = nodeDirectory_repository + corr.getName() + "[" + ObjectformatDate.format(currTimeDate) + "].xml";
                                log.debug("Path to save file:" + newPathToFile);
                            }
                            InventoryComparator icomp = new InventoryComparator(oldPath, handledRsp);
                            int compareResult = 0;
                            try {
                                log.info("Comparing old and new telnet data-collections");
                                compareResult = icomp.compare();
                                log.debug("Compare result=" + compareResult);
                            } catch (Exception ex) {
                                log.error("Error while comparing inventories  for attempt " + attempt + " \n" + icomp.getCompareMessage() + "\n" + ex.getCause());
                                collectionResult = COLLECTION_FAILED;
                                continue ATTEMPTFOR;
                            }
                            switch(compareResult) {
                                case InventoryComparator.FIRST_ACTIVE_CONFIGURATION_DOWNLOAD:
                                    {
                                        break;
                                    }
                                case InventoryComparator.CONFIGURATION_CHANGED:
                                    {
                                        try {
                                            log.info("Configuration changed... generating Inventory-Changed-Event");
                                            generateInventoryChangedEvent(icomp.getCompareMessage(), ipaddr.getHostAddress(), eproxy);
                                        } catch (EventProxyException e) {
                                            log.error("Error while sending Inventory Changed Event " + e.getCause());
                                        }
                                        break;
                                    }
                                case InventoryComparator.CONFIGURATION_NOT_CHANGED:
                                    {
                                        break;
                                    }
                            }
                            InventorySaver isaver = new InventorySaver(iface);
                            log.info("Saving telnet collection data");
                            int tmpSaveResult = isaver.save(handledRsp, corr.getName(), newPathToFile, compareResult, false, dbConn);
                            log.debug("Save result=" + tmpSaveResult);
                            log.debug("Save Msg :" + isaver.getSaveMessage());
                            if (newPathToFile != null && tmpSaveResult == COLLECTION_SUCCEEDED) {
                                fileToCancel.add(newPathToFile);
                            }
                            log.debug("File to cancel list: " + fileToCancel);
                            if (tmpSaveResult == COLLECTION_FAILED) {
                                log.info("Collection failed for interface " + ipaddr.getHostAddress() + " for attempt " + attempt + "\n" + isaver.getSaveMessage());
                                log.debug("Deleting files created during save..." + fileToCancel);
                                deleteCreatedFiles(fileToCancel);
                                fileToCancel = new java.util.ArrayList();
                                log.debug("Files deleted.");
                                log.debug("Rollbacking db operations...");
                                try {
                                    dbConn.rollback();
                                    dbConn.close();
                                    dbConn = DatabaseConnectionFactory.getInstance().getConnection();
                                    dbConn.setAutoCommit(false);
                                } catch (SQLException sqle) {
                                    log.fatal("Unable to rollback on db. " + sqle.getCause());
                                    return COLLECTION_FAILED;
                                }
                                collectionResult = COLLECTION_FAILED;
                                continue ATTEMPTFOR;
                            } else {
                                collectionResult = COLLECTION_SUCCEEDED;
                            }
                        }
                    } else {
                        collectionResult = COLLECTION_FAILED;
                    }
                }
            }
            if (collectionResult == COLLECTION_SUCCEEDED) {
                try {
                    log.debug("Committing changes on db");
                    dbConn.commit();
                } catch (SQLException s) {
                    log.error("Unable to commit to DB " + s.getCause());
                    try {
                        log.debug("Rollbacking DB and Deleting Temporary files created...");
                        dbConn.rollback();
                        deleteCreatedFiles(fileToCancel);
                    } catch (SQLException sqle) {
                        log.fatal("Unable to rollback on db. " + sqle.getCause());
                        return COLLECTION_FAILED;
                    }
                    collectionResult = COLLECTION_FAILED;
                }
            }
            try {
                tb.closeConnection();
                log.info("Telnet connection closed.");
            } catch (IOException io) {
                log.warn("Error while closing connection with telnet peer " + io.getCause());
            }
        }
        try {
            dbConn.close();
        } catch (SQLException se) {
            log.warn("Error while closing connection with db " + se.getCause());
        }
        return collectionResult;
    }

    /**
	 * delete files created during save operation
	 * @param fileToCancel the list of path of files to delete
	 */
    private void deleteCreatedFiles(List fileToDelete) {
        Category log = ThreadCategory.getInstance(getClass());
        log.info("Deleting files temporally created " + fileToDelete.toString());
        Iterator it = fileToDelete.iterator();
        while (it.hasNext()) {
            String tmpPathToDelete = (String) it.next();
            log.debug("Deleting " + tmpPathToDelete);
            File fileToDel = new File(tmpPathToDelete);
            if (fileToDel.exists()) {
                fileToDel.delete();
            }
        }
    }

    /**
	 * Generate an Inventory Changed Event
	 * @param descr
	 * @param nodeId
	 * @param eventProxy
	 * @throws EventProxyException
	 */
    private void generateInventoryChangedEvent(String descr, String ifAddress, EventProxy eventProxy) throws EventProxyException {
        Category log = ThreadCategory.getInstance(getClass());
        Event newEvent = new Event();
        newEvent.setUei(EventConstants.CONFIGURATION_CHANGED_EVENT_UEI);
        newEvent.setSource("TelnetServiceMonitor");
        newEvent.setInterface(ifAddress);
        newEvent.setDescr(descr);
        newEvent.setService(SERVICE_NAME);
        if (m_host != null) newEvent.setHost(m_host);
        newEvent.setTime(EventConstants.formatToString(new java.util.Date()));
        log.info("Sending event " + newEvent);
        eventProxy.send(newEvent);
    }

    /**
	 * This method is responsible for building a Capsd forceRescan event object
	 * and sending it out over the EventProxy. (not currently used)
	 * 
	 * @param ifAddress
	 *            interface address to which this event pertains
	 * @param eventProxy
	 *            proxy over which an event may be sent to eventd
	 */
    private void generateForceRescanEvent(String ifAddress, EventProxy eventProxy) {
        Category log = ThreadCategory.getInstance(getClass());
        if (log.isDebugEnabled()) log.debug("generateForceRescanEvent: interface = " + ifAddress);
        Event newEvent = new Event();
        newEvent.setUei(EventConstants.FORCE_RESCAN_EVENT_UEI);
        newEvent.setSource("TelnetServiceMonitor");
        newEvent.setInterface(ifAddress);
        newEvent.setService(SERVICE_NAME);
        if (m_host != null) newEvent.setHost(m_host);
        newEvent.setTime(EventConstants.formatToString(new java.util.Date()));
        try {
            eventProxy.send(newEvent);
        } catch (EventProxyException e) {
            if (log.isEnabledFor(Priority.ERROR)) log.error("generateForceRescanEvent: Unable to send forceRescan event.", e);
        }
    }

    /**
	 * handle the String strToHandle creating an org.opennms.netmgt.config.inventory.parser.Inventory 
	 * instance using Correspondence attributes and setting to the new Inventory created the name 'inventoryName'
	 * @param strToHandle
	 * @param corr
	 * @param inventoryName
	 * @return the Inventory created in String form
	 * @throws IllegalStateException
	 */
    private String handleCorrespondence(String strToHandle, Correspondence corr, String inventoryName) throws IllegalStateException {
        if (strToHandle == null) {
            throw new IllegalStateException("Unable to handle null String.");
        }
        boolean imappFound = false;
        org.opennms.netmgt.config.inventory.parser.Inventory inv = new org.opennms.netmgt.config.inventory.parser.Inventory();
        inv.setName(inventoryName);
        String name = corr.getName();
        String assetField = corr.getAssetField();
        String regExpr = corr.getFilterRegexpr();
        String replaceWith = "";
        if (regExpr != null) {
            String action = corr.getFilterAction();
            if (action.equals("match")) {
                strToHandle = StringHandler.match(strToHandle, regExpr);
            }
            if (action.equals("replace")) {
                replaceWith = corr.getFilterReplacewith();
                strToHandle = StringHandler.replaceAll(strToHandle, regExpr, replaceWith);
            }
            if (action.equals("delete")) {
                strToHandle = StringHandler.delete(strToHandle, regExpr);
            }
        }
        Item newItem = new Item();
        newItem.setName(name);
        newItem.setDataitem(strToHandle);
        if (assetField != null) {
            newItem.setAssetField(assetField);
        }
        inv.addItem(newItem);
        StringWriter sw = new StringWriter();
        try {
            Marshaller.marshal(inv, sw);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }
        try {
            sw.close();
        } catch (IOException io) {
            throw new IllegalStateException(io.getMessage());
        }
        return sw.toString();
    }

    private class TelnetBroker implements TelnetNotificationHandler {

        private TelnetClient tc = null;

        private Vector exchangeVec = new Vector();

        private String command = null;

        private String prompt = null;

        private final int DEFAULT_PORT = 23;

        private final int DEFAULT_TIMEOUT = 1000;

        private int timeout = DEFAULT_TIMEOUT;

        private int port = DEFAULT_PORT;

        public TelnetBroker(String ipAddress, Map parameters) throws IOException, MarshalException, ValidationException {
            Category log = ThreadCategory.getInstance(getClass());
            String strPort = (String) parameters.get("port");
            if (strPort != null) {
                port = Integer.parseInt(strPort);
            }
            String strTimout = (String) parameters.get("timeout");
            if (strTimout != null) {
                timeout = Integer.parseInt(strTimout);
            }
            tc = new TelnetClient();
            TerminalTypeOptionHandler ttopt = new TerminalTypeOptionHandler("VT100", false, false, true, false);
            EchoOptionHandler echoopt = new EchoOptionHandler(true, false, true, false);
            SuppressGAOptionHandler gaopt = new SuppressGAOptionHandler(true, true, true, true);
            try {
                tc.addOptionHandler(ttopt);
                tc.addOptionHandler(echoopt);
                tc.addOptionHandler(gaopt);
            } catch (InvalidTelnetOptionException e) {
                log.error("Error registering option handlers: " + e.getMessage());
            }
            try {
                tc.connect(ipAddress, port);
                tc.registerNotifHandler(this);
            } catch (Exception e) {
                log.error("Exception while connecting:" + e.getMessage());
            }
        }

        /***********************************************************************
		 * Callback method called when TelnetClient receives an option
		 * negotiation command.
		 * <p>
		 * 
		 * @param negotiation_code -
		 *            type of negotiation command received (RECEIVED_DO,
		 *            RECEIVED_DONT, RECEIVED_WILL, RECEIVED_WONT)
		 *            <p>
		 * @param option_code -
		 *            code of the option negotiated
		 *            <p>
		 **********************************************************************/
        public void receivedNegotiation(int negotiation_code, int option_code) {
            String command = null;
            if (negotiation_code == TelnetNotificationHandler.RECEIVED_DO) {
                command = "DO";
            } else if (negotiation_code == TelnetNotificationHandler.RECEIVED_DONT) {
                command = "DONT";
            } else if (negotiation_code == TelnetNotificationHandler.RECEIVED_WILL) {
                command = "WILL";
            } else if (negotiation_code == TelnetNotificationHandler.RECEIVED_WONT) {
                command = "WONT";
            }
        }

        /**
		 * open a telnet connection
		 * 
		 * @param excs
		 *            Exchange[] array containing the exchange for the
		 *            authentication
		 * @param timeoutSec
		 *            timeout in seconds beetwen two exchanges of command with
		 *            the telnet peer
		 * @return true if the connection was been opened correctly, false
		 *         otherwise
		 */
        public boolean openConnection(Exchange[] excs, int timeoutMillisec) throws IOException {
            Category log = ThreadCategory.getInstance(getClass());
            InputStream instr = tc.getInputStream();
            String lastCommand = null;
            log.debug("Opening telnet connnection... " + excs.length + " Exchanges");
            try {
                String tmpCommand = "";
                for (int i = 0; i < excs.length; ) {
                    Exchange ex = excs[i];
                    int numBytesRead = 0;
                    log.debug("Sleeping for " + timeoutMillisec + " millisecs...");
                    Thread.sleep(timeoutMillisec);
                    log.debug("End sleeping");
                    String strRecv = null;
                    strRecv = getResponse(instr);
                    log.debug("Received: " + strRecv);
                    if (strRecv == null) {
                        if (tc.isConnected()) {
                            tc.disconnect();
                        }
                        log.info("Telnet Connection open error");
                        return false;
                    }
                    strRecv = strRecv.trim();
                    if (strRecv.endsWith(ex.getPrompt())) {
                        tmpCommand = ex.getCommand();
                        i++;
                    } else if (strRecv.equals("") || strRecv.endsWith(ex.getErrorprompt())) {
                        if (tc.isConnected()) {
                            tc.disconnect();
                        }
                        log.info("Telnet Connection open error");
                        return false;
                    }
                    prompt = strRecv;
                    lastCommand = tmpCommand;
                    sendCommand(tmpCommand);
                    log.debug("Sending command " + tmpCommand);
                }
                Thread.sleep(timeoutMillisec);
                prompt = getResponse(instr);
            } catch (Exception e) {
                throw new IOException("Unable to read/write with target machine.");
            }
            log.info("Telnet Conneciton opened successfully");
            return true;
        }

        public void closeConnection() throws IOException {
            if (tc.isConnected()) tc.disconnect();
        }

        public String doCommand(CommandLine cmdLine, int timeoutMillisec) throws IOException, InterruptedException {
            Category log = ThreadCategory.getInstance(getClass());
            if (!tc.isConnected()) {
                log.error("Telnet client not connected! It must be open connection before");
                throw new IOException("Telnet client not connected! It must be open connection before");
            }
            InputStream instr = tc.getInputStream();
            String response = null;
            log.debug("Executing command " + cmdLine.getCommand() + " for prompt " + cmdLine.getPrompt() + " and error-prompt " + cmdLine.getErrorprompt());
            log.debug("Current prompt: " + prompt);
            if (cmdLine.getPrompt() != null) {
                if (!prompt.trim().endsWith(cmdLine.getPrompt())) {
                    if (tc.isConnected()) {
                        tc.disconnect();
                    }
                    throw new IOException("Telnet prompt (" + prompt + ") doesn't ends as configured: " + cmdLine.getPrompt());
                }
            }
            if (cmdLine.getErrorprompt() != null) {
                if (prompt.endsWith(cmdLine.getErrorprompt())) {
                    if (tc.isConnected()) {
                        tc.disconnect();
                    }
                    throw new IOException("Telnet prompt (" + prompt + ") ends with the error-prompt configured: " + cmdLine.getErrorprompt());
                }
            }
            sendCommand(cmdLine.getCommand());
            Thread.sleep(timeoutMillisec);
            response = getCommandResponse(instr, prompt);
            log.debug("Response for command " + cmdLine.getCommand() + " for prompt " + cmdLine.getPrompt() + " and error-prompt " + cmdLine.getErrorprompt() + " :\n" + response);
            if (response != null) {
                response = response.replaceAll(prompt, "");
                response = response.replaceAll(cmdLine.getCommand(), "");
            }
            return response;
        }

        private void sendCommand(String command) throws IOException {
            OutputStream outstr = tc.getOutputStream();
            command += "\n";
            byte[] buff = command.getBytes();
            outstr.write(buff, 0, buff.length);
            outstr.flush();
        }

        private String getResponse(InputStream is) throws IOException {
            int ret_read = 0;
            String retStr = "";
            while ((ret_read = is.available()) > 0) {
                byte[] buff = new byte[2048];
                is.read(buff, 0, ret_read);
                String tmpStr = new String(buff, 0, ret_read);
                retStr += tmpStr;
            }
            return retStr.equals("") ? null : retStr;
        }

        private String getCommandResponse(InputStream is, String end) throws IOException, InterruptedException {
            int ret_read = 0;
            String retStr = "";
            while (true) {
                byte[] buff = new byte[2048];
                ret_read = is.read(buff);
                String tmpStr = new String(buff, 0, ret_read);
                retStr += tmpStr;
                if (retStr.endsWith(end)) return retStr;
                Thread.sleep(50);
            }
        }
    }
}
