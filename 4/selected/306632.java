package net.sourceforge.jcoupling2.adapter.yawl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import net.sourceforge.jcoupling2.adapter.MessageEvent;
import net.sourceforge.jcoupling2.adapter.MessageReceivedListener;
import net.sourceforge.jcoupling2.control.JCouplingImporter;
import net.sourceforge.jcoupling2.control.Util;
import net.sourceforge.jcoupling2.exception.JCouplingException;
import net.sourceforge.jcoupling2.peer.property.ChooseClause;
import net.sourceforge.jcoupling2.persistence.DataMapper;
import net.sourceforge.jcoupling2.persistence.FilterInstance;
import net.sourceforge.jcoupling2.persistence.Mapping;
import net.sourceforge.jcoupling2.persistence.Message;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.yawlfoundation.yawl.elements.data.YParameter;
import org.yawlfoundation.yawl.engine.interfce.WorkItemRecord;
import org.yawlfoundation.yawl.engine.interfce.interfaceB.InterfaceBWebsideController;
import org.yawlfoundation.yawl.exceptions.YAWLException;
import org.yawlfoundation.yawl.resourcing.datastore.WorkItemCache;
import org.yawlfoundation.yawl.resourcing.rsInterface.ConnectionCache;
import org.yawlfoundation.yawl.util.JDOMUtil;

/**
 * Bridge between JCoupling and YAWL Engine
 * have to be registered as YAWL service and receives messages and requests from YAWL
 * 
 * @author jan
 * @version $Id: JCouplingService.java 17682 2010-04-13 11:14:49Z tbe $
 */
public class JCouplingService extends InterfaceBWebsideController implements MessageReceivedListener {

    private static Logger logger = Logger.getLogger(JCouplingService.class);

    protected XMLOutputter fmt;

    protected DataMapper dataMapper;

    private JCouplingImporter jcImporter;

    public static WorkItemCache workItemCache = new WorkItemCache();

    protected ConnectionCache connections = ConnectionCache.getInstance();

    private static final String YAWL_ERROR_NO_WORK_ITEM1 = "<failure><reason>No work item with id ";

    private static final String YAWL_ERROR_NO_WORK_ITEM2 = "<failure><reason>WorkItem with ID ";

    /**
	 * TODO@tbe: evntl. restored YAWL das work item wieder?
	 */
    private static final boolean REMOVE_MAPPING_NOT_IN_YAWL = false;

    protected String engineSessionHandle;

    protected static Object MUTEX = null;

    static {
        new JCouplingService().processCachedWorkItemsTask(null);
    }

    /**
	 * 
	 * @throws JCouplingServiceDAOException
	 * @throws SimpleDAOException
	 * @throws TransferException
	 */
    public JCouplingService() {
        logger.info("JCouplingService 1 starting...");
        if (MUTEX == null) {
            MUTEX = new Object();
        }
        workItemCache.setPersist(true);
        dataMapper = new DataMapper();
        fmt = new XMLOutputter();
        fmt.setFormat(Format.getPrettyFormat());
        jcImporter = new JCouplingImporter(this);
    }

    /**
	 * ********** inherited by InterfaceBWebsideController *************
	 * receives messages and requests from YAWL work items
	 * Saves or updates a mapping to the database for recovery of failed YAWL requests
	 */
    public void handleEnabledWorkItemEvent(WorkItemRecord wirParent) {
        logger.debug("wirParentID: " + (wirParent == null ? null : wirParent.getID()));
        Mapping mapping = null;
        try {
            mapping = new Mapping(wirParent, null, Mapping.WORKITEM_STATUS_PARENT);
            dataMapper.saveMapping(mapping);
            processMapping(mapping);
        } catch (Throwable e) {
            logger.error("cannot execute work item: " + (wirParent == null ? null : wirParent.getID()), e);
            dataMapper.unlockMapping(mapping);
        }
    }

    /**
	 * checkout work item from YAWL engine
	 * get their children work items, cache them, process them
	 * check them back into the engine
	 * remove the mapping for successful processed work items
	 * updates the mapping continuously
	 * @param mapping: contained work item can be parent or child
	 */
    private void processMapping(Mapping mapping) throws IOException, JCouplingException {
        connected();
        if (mapping.getWorkItemStatus().equals(Mapping.WORKITEM_STATUS_PARENT)) {
            checkOutWorkItem(mapping);
            mapping.setWorkItemStatus(Mapping.WORKITEM_STATUS_CHECKOUT);
            dataMapper.saveMapping(mapping);
        }
        if (mapping.getWorkItemStatus().equals(Mapping.WORKITEM_STATUS_CHECKOUT)) {
            for (WorkItemRecord wir : getChildren(mapping.getWorkItemId())) {
                Mapping mappingChild = null;
                try {
                    workItemCache.add(wir);
                    logger.info("work item ID: '" + wir.getID() + " cached successful");
                    mappingChild = new Mapping(wir, null, Mapping.WORKITEM_STATUS_CACHED);
                    dataMapper.saveMapping(mappingChild);
                    processMapping(mappingChild);
                } catch (Throwable e) {
                    logger.error("cannot execute work item: " + (wir == null ? null : wir.getID()), e);
                    dataMapper.unlockMapping(mappingChild);
                }
            }
            dataMapper.removeMapping(mapping);
        }
        if (mapping.getWorkItemStatus().equals(Mapping.WORKITEM_STATUS_CACHED)) {
            WorkItemRecord wir = mapping.getWorkItem();
            if (wir.getTimerTrigger() != null) {
                logger.info("work item ID: '" + wir.getID() + "wait for timeout");
                dataMapper.unlockMapping(mapping);
                return;
            }
            Element msgElement = wir.getDataList();
            String msgText = fmt.outputString(msgElement);
            String taskType = msgElement.getChildText("TaskType");
            logger.debug("msgText:\r\n" + msgText + ", taskType: " + taskType);
            if (taskType != null && taskType.equals("Message")) {
                processMessageTask(mapping, msgText);
            } else if (taskType != null && taskType.equals("Event")) {
                FilterInstance fInstance = new FilterInstance(mapping, new ChooseClause());
                processEventTask(fInstance);
            } else {
                throw new JCouplingException("unknown TaskType: " + taskType);
            }
        }
    }

    /**
	 * ********** inherited by InterfaceBWebsideController ************* 
	 * Removes (if necessary) the work items from the YAWL Engine
	 * 
	 */
    public void handleCancelledWorkItemEvent(WorkItemRecord workItemRecord) {
        logger.warn("is not implemented!");
    }

    /**
	 * inherited by InterfaceBWebsideController describes the input parameters for YAWL Engine
	 */
    public YParameter[] describeRequiredParams() {
        logger.warn("is not implemented!");
        return null;
    }

    public void checkInWorkItem(Mapping mapping, Element message) throws JCouplingException {
        try {
            WorkItemRecord wir = mapping.getWorkItem();
            checkCacheForWorkItem(wir);
            String result = checkInWorkItem(wir.getID(), wir.getDataList(), getOutputData(wir.getTaskName(), message), engineSessionHandle);
            if (!successful(result)) {
                if (REMOVE_MAPPING_NOT_IN_YAWL && result.startsWith(YAWL_ERROR_NO_WORK_ITEM2)) {
                    dataMapper.removeMapping(mapping);
                    throw new JCouplingException(result + ", remove it and cancel task");
                } else {
                    throw new JCouplingException("not successful: " + result);
                }
            }
            logger.info("work item " + wir.getID() + " successfully processed and checked back into the engine");
        } catch (JCouplingException e) {
            throw e;
        } catch (Throwable e) {
            throw new JCouplingException(e);
        }
    }

    /**
	 * ermittelt Children eines Parent-WorkItems, dieses muss dazu bereits ausgecheckt sein
	 * offenbar wird bei aufruf von InterfaceBWebsideController.getChildren() das work item 'wirParent'
	 * aus dem YAWL cache entfernt, auch wenn getChildren() fehlschl�gt
	 * 
	 * @param workItemID
	 * @return
	 * @throws IOException
	 */
    public List<WorkItemRecord> getChildren(String workItemID) throws IOException {
        WorkItemRecord wirParent = null;
        try {
            wirParent = workItemCache.get(workItemID);
            wirParent = wirParent == null ? null : wirParent.clone();
            logger.debug("---------notice wirParent=" + wirParent);
        } catch (CloneNotSupportedException e1) {
            logger.warn("cannot clone work item " + workItemID);
        }
        try {
            List<WorkItemRecord> list = getChildren(workItemID, engineSessionHandle);
            logger.debug("return " + list.size() + " children of parentID " + workItemID);
            return list;
        } catch (IOException e) {
            if (wirParent != null) {
                logger.debug("---------add wirParent=" + wirParent + " to workItemCache");
                workItemCache.add(wirParent);
            } else {
                logger.debug("---------dont add wirParent=" + wirParent + " to workItemCache");
            }
            throw e;
        }
    }

    /**
	 * Check the work item out of the engine
	 * InterfaceBWebsideController.checkOut() adds work item with ID=wirID to workItemCache 
	 * 
	 * @param mapping
	 *          - contains the work item ID to check out
	 */
    protected void checkOutWorkItem(Mapping mapping) throws JCouplingException {
        String wirID = null;
        try {
            wirID = mapping.getWorkItemId();
            String text = "work item ID: '" + wirID + "' ";
            WorkItemRecord wir = checkOut(wirID, engineSessionHandle);
            if (wir == null) {
                throw new YAWLException("work item " + wirID + " not found in YAWL");
            } else {
                logger.info(text + "checkout successful");
            }
        } catch (YAWLException e) {
            if (REMOVE_MAPPING_NOT_IN_YAWL && e.getMessage().startsWith(YAWL_ERROR_NO_WORK_ITEM1)) {
                dataMapper.removeMapping(mapping);
                throw new JCouplingException(e.getMessage() + ", remove it and cancel task");
            } else {
                throw new JCouplingException(e);
            }
        } catch (Throwable e) {
            throw new JCouplingException(e);
        }
    }

    public void receivedMessage(MessageEvent msgEvent) {
        logger.debug("receive MessageEvent for Channel: " + msgEvent.getChannelName());
        processCachedWorkItemsTask(msgEvent.getChannelName());
    }

    /**
	 * Checks if there is a connection to the engine, and if there isn't, attempts to connect
	 * 
	 * @return true if connected to the engine
	 */
    private void connected() throws JCouplingException {
        String backEndURI = null;
        long waitSec = 0;
        try {
            if ((engineSessionHandle == null) || (engineSessionHandle.length() == 0) || (!checkConnection(engineSessionHandle))) {
                Properties props = new Properties();
                props.load(this.getClass().getResourceAsStream("/yawl.properties"));
                waitSec = Long.parseLong(props.getProperty("waitSec", String.valueOf(waitSec)));
                backEndURI = props.getProperty("InterfaceBClient.backEndURI");
                String user = props.getProperty("user", DEFAULT_ENGINE_USERNAME);
                String password = props.getProperty("password", DEFAULT_ENGINE_PASSWORD);
                setUpInterfaceBClient(backEndURI);
                engineSessionHandle = connect(user, password);
                if (!successful(engineSessionHandle)) {
                    throw new IOException("connect not successful");
                }
                workItemCache.clear();
                workItemCache.restore();
                logger.info("successfully connected to YAWL Engine " + backEndURI + " and restore work item cache");
                logger.debug("workItemCache: " + Util.toString((Map) workItemCache));
            }
        } catch (Throwable e) {
            throw new JCouplingException("cannot connect to YAWL Engine " + backEndURI, e);
        }
    }

    /**
	 * re-adds checkedout item to local cache after a restore (if required)
	 */
    protected void checkCacheForWorkItem(WorkItemRecord wir) {
        WorkItemRecord wirCached = getCachedWorkItem(wir.getID());
        if (wirCached == null) {
            logger.debug("cache work item " + wir.getID());
            getModel().addWorkItem(wir);
        }
    }

    protected Element getOutputData(String taskName, Element message) {
        String strMessage = message == null ? "" : JDOMUtil.elementToString(message).trim();
        strMessage = "<" + taskName + ">" + strMessage + "</" + taskName + ">";
        return JDOMUtil.stringToElement(strMessage);
    }

    /**
	 * TODO@tbe: evntl. Taskz�hler einbauen und begrenzte anzahl Tasks parallel abarbeiten,
	 * sonst m�ssen Message-Tasks in processMapping() immer warten bis processEventTask() fertig ist
	 */
    private void processEventTask(FilterInstance fInstance) {
        ReceiveMessageHandler rMessageHandler = new ReceiveMessageHandler(dataMapper, fInstance);
        rMessageHandler.run();
    }

    private void processMessageTask(Mapping mapping, String msgText) {
        SendMessageHandler sMessageHandler = new SendMessageHandler(mapping, msgText);
        sMessageHandler.start();
    }

    /**
	 * TODO@tbe: dont abort processCachedWorkItems if requested process can handle more than the
	 * processing/waiting process, z.B.
	 * 
	 * processCachedWorkItems(testchannel) -> processing
	 * processCachedWorkItems(testchannel) -> waiting
	 * processCachedWorkItems(null) -> requesting -> aborting, but its not correct
	 * 
	 * @param channelName
	 */
    private void processCachedWorkItemsTask(String channelName) {
        CachedWorkItemsHandler cwih = new CachedWorkItemsHandler(channelName);
        cwih.run();
    }

    /**
	 * registers a YAWL request and look for matching messages in data base
	 * 
	 * @author jpr
	 * 
	 */
    private class ReceiveMessageHandler extends Thread {

        private DataMapper dataMapper = null;

        private FilterInstance fInstance = null;

        public ReceiveMessageHandler(DataMapper dMapper, FilterInstance fInstance) {
            this.fInstance = fInstance;
            this.dataMapper = dMapper;
        }

        public void run() {
            WorkItemRecord wir = null;
            Mapping mapping = fInstance.getMapping();
            try {
                wir = mapping.getWorkItem();
                logger.info("process event work item " + wir.getID() + ", FilterModelName=" + fInstance.getFilterModelName());
                checkCacheForWorkItem(wir);
                dataMapper.registerYAWLRequest(fInstance);
                logger.debug("work item ID=" + wir.getID() + " has RequestKey=" + mapping.getRequestKey());
                processFilter(fInstance);
            } catch (Throwable e) {
                logger.error("cannot process event work item: " + (wir == null ? null : wir.getID()), e);
                dataMapper.unlockMapping(mapping);
            }
        }

        /**
		 * look for matching message ids in data base and reserves them for later consume
		 * @param fInstance
		 * @throws JCouplingException
		 */
        private void processFilter(FilterInstance fInstance) throws JCouplingException {
            Mapping mapping = fInstance.getMapping();
            List<Integer> messageIDs = dataMapper.executeFilterInstance(fInstance);
            if (messageIDs.size() > 0) {
                processMessages(mapping, messageIDs);
            } else {
                dataMapper.unlockMapping(mapping);
            }
        }

        /**
		 * returns message(s) to YAWL engine and removes the YAWL request
		 * The default is to choose the first and delete all messages.
		 * This will occur unless you explicitly set the choose clause to be something else.
		 * @param fInstance
		 * @throws JCouplingException
		 */
        private void processMessages(Mapping mapping, List<Integer> messageIDs) throws JCouplingException {
            List<Message> messages = dataMapper.retrieveMessages(messageIDs);
            String body = "";
            for (Message message : messages) {
                body += message.getBody() + "\r\n";
            }
            body = body.substring(0, body.length() - 2);
            logger.info("send " + messages.size() + " message IDs (" + Util.getCSV(messages, 20) + ") for work item " + mapping.getWorkItemId() + ", message body:\r\n" + body);
            checkInWorkItem(mapping, JDOMUtil.stringToElement(body));
            try {
                dataMapper.removeYAWLRequest(fInstance);
            } catch (Throwable e) {
                logger.error("cannot cleanup request work item", e);
            }
        }
    }

    /**
	 * process Message-Task-WorkItems
	 * 
	 * @author tbe
	 * 
	 */
    private class SendMessageHandler extends Thread {

        private Mapping mapping = null;

        private String msgText = null;

        public SendMessageHandler(Mapping mapping, String msgText) {
            this.mapping = mapping;
            this.msgText = msgText;
        }

        public void run() {
            WorkItemRecord wir = null;
            try {
                wir = mapping.getWorkItem();
                logger.info("process message work item " + wir.getID());
                checkCacheForWorkItem(wir);
                jcImporter.importText(msgText);
                checkInWorkItem(mapping, null);
                try {
                    dataMapper.removeMapping(mapping);
                    jcImporter.notifyMessageReceivedListener(msgText);
                } catch (Throwable e) {
                    logger.error("cannot cleanup message work item", e);
                }
            } catch (Throwable e) {
                logger.error("cannot process message work item: " + (wir == null ? null : wir.getID()), e);
                dataMapper.unlockMapping(mapping);
            }
        }
    }

    /**
	 *  count of processes which already waiting for method
	 *  @see processCachedWorkItems(String channelName)
	 */
    private static int countProcessCachedWorkItems = 0;

    /**
	 * 
	 * @author tbe
	 * 
	 */
    private class CachedWorkItemsHandler extends Thread {

        private String channelName = null;

        public CachedWorkItemsHandler(String channelName) {
            this.channelName = channelName;
        }

        public void run() {
            countProcessCachedWorkItems++;
            if (countProcessCachedWorkItems > 1) {
                logger.debug(countProcessCachedWorkItems + " process(es) are already processing/waiting/requesting, abort...");
                countProcessCachedWorkItems--;
                return;
            }
            synchronized (MUTEX) {
                if (countProcessCachedWorkItems > 1) {
                    logger.error(countProcessCachedWorkItems + " process(es) are active in MUTEX3, this should not occur, abort");
                    return;
                }
                try {
                    process();
                } finally {
                    countProcessCachedWorkItems--;
                }
            }
        }

        private void process() {
            ArrayList<Mapping> mappings = null;
            try {
                logger.debug("load cached work items for channel: " + channelName + "...");
                mappings = dataMapper.retrieveMappings(channelName);
                logger.info("found " + mappings.size() + " cached work items for channel: " + channelName);
                if (mappings.size() > 0) {
                    connected();
                    Collections.sort(mappings, new Comparator<Mapping>() {

                        public int compare(Mapping m1, Mapping m2) {
                            return norm(m1.getWorkItemId()).compareTo(norm(m2.getWorkItemId()));
                        }

                        final String praefix = "0000";

                        private String norm(String wirID) {
                            int idx = wirID.indexOf(".");
                            if (idx >= 0) {
                                String s = praefix + wirID.substring(0, idx);
                                s = s.substring(s.length() - praefix.length(), s.length());
                                return s + "." + norm(wirID.substring(idx + 1));
                            } else {
                                return wirID;
                            }
                        }
                    });
                }
                for (Mapping mapping : mappings) {
                    try {
                        WorkItemRecord wir = workItemCache.get(mapping.getWorkItemId());
                        if (wir == null && !mapping.getWorkItemStatus().equals(Mapping.WORKITEM_STATUS_PARENT)) {
                            String text = "";
                            if (REMOVE_MAPPING_NOT_IN_YAWL) {
                                dataMapper.removeMapping(mapping);
                                text = ", remove it and cancel task";
                            }
                            throw new JCouplingException("work item " + mapping.getWorkItemId() + " is not in YAWL cache" + text);
                        } else {
                            mapping.setWorkItem(wir);
                            processMapping(mapping);
                        }
                    } catch (Throwable e) {
                        logger.error("cannot execute cached work item: " + mapping.getWorkItemId(), e);
                        dataMapper.unlockMapping(mapping);
                    }
                }
            } catch (Throwable e) {
                logger.error("cannot retrieve mappings for channel: " + channelName, e);
                dataMapper.unlockMappings(mappings);
            }
        }
    }
}
