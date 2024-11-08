package axt.db;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import axt.config.Configuration;

/**
 * AXT database transaction class.
 * <p>
 * Uses the hibernate util to connect to the AXT database.
 * <p>
 * This class also uses the hibernate DAO java objects that have been created for each table.
 * The DAO objects are cached using EHCache and the EHCache is enabled through the
 * inclusion in the necessary methods within this class.
 * <p>
 * @author Colin Cheline
 */
public class GeneralDAO {

    private static Log log = LogFactory.getLog("axt.db");

    /**
	 * Adds a record for a new stage file and returns the stage file ID.
	 * 
	 * @param messageID
	 * @return int
	 * @throws Exception
	 */
    public static int setStageFile(String messageID) throws Exception {
        log.debug("Getting a New Stage File for message: " + messageID);
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = session.beginTransaction();
        Stage stageFile = new Stage();
        stageFile.setMessageid(messageID);
        try {
            session.saveOrUpdate(stageFile);
            tx.commit();
        } catch (Exception e) {
            log.debug("Rolled Back Transaction: " + e);
            tx.rollback();
            throw new Exception("Error Getting a Stage File ID: " + e.getCause().getMessage());
        }
        return stageFile.getFileId();
    }

    /**
	 * Updates a stage file record in the database, adding the file digest (MD5, SHA, etc) as defined in the properties.
	 * This information is currently optional, but should be populated for future uses.
	 * 
	 * @param fileID
	 * @param digest
	 * @throws Exception
	 */
    public static void setStageFileDigest(int fileID, String digest) throws Exception {
        log.debug("Setting the Stage File Digest");
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = session.beginTransaction();
        Stage stageFile = (Stage) session.load(Stage.class, fileID);
        stageFile.setDigest(digest);
        try {
            session.update(stageFile);
            tx.commit();
        } catch (Exception e) {
            log.debug("Rolled Back Transaction: " + e);
            tx.rollback();
            throw new Exception("Error Getting Setting the Stage File Digest: " + e.getCause().getMessage());
        }
        return;
    }

    /**
	 * Updates a stage file record in the database, adding the file size.
	 * This information is required by some transfer connectors and all transfer connector pull functions must update this information.
	 * 
	 * @param fileID
	 * @param fileSize
	 * @throws Exception
	 */
    public static void setStageFileSize(int fileID, Long fileSize) throws Exception {
        log.debug("Setting the Stage File Size");
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = session.beginTransaction();
        Stage stageFile = (Stage) session.load(Stage.class, fileID);
        stageFile.setFilesize(fileSize);
        try {
            session.update(stageFile);
            tx.commit();
        } catch (Exception e) {
            log.debug("Rolled Back Transaction: " + e);
            tx.rollback();
            throw new Exception("Error Getting Setting the Stage File Size: " + e.getCause().getMessage());
        }
        return;
    }

    /**
	 * Gets the stage file size from the row defined by the fileID.
	 * Returns the file size.
	 * 
	 * @param fileID
	 * @return Long
	 * @throws Exception
	 */
    public static Long getStageFileSize(int fileID) throws Exception {
        log.debug("Getting the Stage File Size");
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = session.beginTransaction();
        Stage stageFile = (Stage) session.load(Stage.class, fileID);
        Long fileSize = stageFile.getFilesize();
        try {
            session.update(stageFile);
            tx.commit();
        } catch (Exception e) {
            log.debug("Rolled Back Transaction: " + e);
            tx.rollback();
            throw new Exception("Error Getting a Stage File ID: " + e.getCause().getMessage());
        }
        return fileSize;
    }

    /**
	 * Gets an existing stage file ID through lookup using the message ID.
	 *  
	 * @param messageID
	 * @return int
	 * @throws Exception
	 */
    public static int getStageFile(String messageID) throws Exception {
        log.debug("Getting a Stage File ID");
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        Query q = session.createQuery("SELECT s.fileid FROM Stage as s " + "WHERE s.messageid = :messageid ");
        q.setString("messageid", messageID);
        log.debug("Getting Result");
        Integer fileID = (Integer) q.list().get(0);
        log.debug("Returning Staging ID: " + fileID);
        return fileID;
    }

    /**
	 * Adds an audit record.
	 * Should be called only once at the start of a new message.
	 * 
	 * @param messageid
	 * @param message
	 * @throws Exception
	 */
    public static void setAuditRecord(String messageid, String message) throws Exception {
        log.debug("Adding audit  message");
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = session.beginTransaction();
        Audit auditTable = new Audit();
        auditTable.setMessageid(messageid);
        auditTable.setAction(message);
        try {
            session.saveOrUpdate(auditTable);
            tx.commit();
        } catch (Exception e) {
            log.debug("Rolled Back Transaction: " + e);
            tx.rollback();
            throw new Exception(e);
        }
        return;
    }

    /**
	 * Retrieves an audit record.
	 * 
	 * @param messageid
	 * @param message
	 * @throws Exception
	 */
    public static void getAuditRecord(String messageid) throws Exception {
        log.debug("Getting audit  message");
        return;
    }

    /**
	 * Adds an audit record.
	 * Should be called only once at the start of a new message.
	 * 
	 * @param messageid
	 * @param message
	 * @throws Exception
	 */
    public static void addAudit(String messageid, String message) throws Exception {
        log.debug("Adding audit  message");
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = session.beginTransaction();
        Audit auditTable = new Audit();
        auditTable.setMessageid(messageid);
        auditTable.setAction(message);
        try {
            session.saveOrUpdate(auditTable);
            tx.commit();
        } catch (Exception e) {
            log.debug("Rolled Back Transaction: " + e);
            tx.rollback();
            throw new Exception(e);
        }
        return;
    }

    /**
	 * Return the full audit list.
	 * 
	 * @param limit
	 * @return List
	 */
    public static List getAuditList(int limit) {
        log.debug("Getting Audit List");
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        Query q = session.createQuery("SELECT a.time, a.messageid, a.action FROM Audit as a " + "ORDER BY a.time DESC").setMaxResults(limit);
        List audits = q.list();
        return audits;
    }

    /**
	 * Gets the message text from the audit table for the defined message.
	 * 
	 * @param messageid
	 * @return String
	 */
    public static String getMessage(String messageid) {
        log.debug("Getting the message based on the messageid");
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        Audit auditTable = (Audit) session.load(Audit.class, messageid);
        String messageText = auditTable.getAction();
        return messageText;
    }

    /**
	 * Get the log for a specified message.
	 * 
	 * @param messageid
	 * @return List
	 */
    public static List getLog(String messageid) {
        log.debug("Getting log messages from messageid: " + messageid);
        StatelessSession session = HibernateUtil.getSessionFactory().openStatelessSession();
        Query q = session.createQuery("SELECT l.time, l.log FROM TxnLog as l " + "WHERE l.messageid = :messageid " + "ORDER BY l.pkey ASC");
        q.setString("messageid", messageid);
        List logs = q.list();
        log.debug("Got \"" + logs.size() + "\" logs");
        return logs;
    }

    /**
	 * Based on the specified number of days:
	 *  Purge the rows from the audit, log, and stage tables.
	 *  Remove the staged files.
	 * 
	 * @param history
	 * @return String
	 * @throws Exception
	 */
    public static String purgeHistory(int history) throws Exception {
        log.debug("Purging log messages");
        Configuration config = new Configuration();
        String STAGINGDIR = config.getProperty("stagingDirectory");
        int PurgedLog = 0;
        int PurgedAudit = 0;
        int PurgedStage = 0;
        int PurgedStageFile = 0;
        Calendar cal = new GregorianCalendar();
        cal.add(Calendar.DATE, -history);
        java.util.Date historyDate = cal.getTime();
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = session.beginTransaction();
        Query q = session.createQuery("SELECT a.messageid FROM Audit as a " + "WHERE a.time <= :historyDate ").setDate("historyDate", historyDate);
        Iterator messageidIterator = q.list().iterator();
        String purgeLog = "";
        while (messageidIterator.hasNext()) {
            String purgeMessage = (String) messageidIterator.next();
            PurgedLog += session.createQuery("delete TxnLog as l " + "WHERE l.messageid = :messageid").setString("messageid", purgeMessage).executeUpdate();
            PurgedAudit += session.createQuery("delete Audit as a " + "WHERE a.messageid = :messageid").setString("messageid", purgeMessage).executeUpdate();
            Query stageQ = session.createQuery("SELECT s.fileid FROM Stage as s " + "WHERE s.messageid = :messageid ").setString("messageid", purgeMessage);
            Iterator stageIterator = stageQ.list().iterator();
            while (stageIterator.hasNext()) {
                Integer stageFileID = (Integer) stageIterator.next();
                File stageFile = new File(STAGINGDIR + "/" + stageFileID);
                try {
                    stageFile.delete();
                    PurgedStageFile++;
                } catch (Exception rmError) {
                    log.error("--Could not purge stage file: " + STAGINGDIR + "/" + stageFileID);
                    purgeLog += "--ERROR: Could not purge stage file: " + STAGINGDIR + "/" + stageFileID;
                }
            }
            PurgedStage += session.createQuery("delete Stage as s " + "WHERE s.messageid = :messageid").setString("messageid", purgeMessage).executeUpdate();
            session.flush();
            session.clear();
        }
        try {
            tx.commit();
            log.info("-Purged \"" + PurgedAudit + "\" audit logs.");
            purgeLog += "-Purged \"" + PurgedAudit + "\" audit logs.\n";
            log.info("-Purged \"" + PurgedLog + "\" transaction logs.");
            purgeLog += "-Purged \"" + PurgedLog + "\" transaction logs.\n";
            log.info("-Purged \"" + PurgedStage + "\" stage logs.");
            purgeLog += "-Purged \"" + PurgedStage + "\" stage logs.\n";
            log.info("-Purged \"" + PurgedStageFile + "\" stage files.");
            purgeLog += "-Purged \"" + PurgedStageFile + "\" stage files.";
        } catch (Exception e) {
            log.debug("Rolled Back Transaction: " + e);
            tx.rollback();
            throw new Exception(e);
        }
        return purgeLog;
    }

    /**
	 * Determine the action class for a specific node based on the action type.
	 * An example would be to get the FTP Pull class name for node isp101.
	 *  
	 * @param nodeName
	 * @param actionType
	 * @return List
	 */
    public static List getActionClass(String nodeName, String actionType) {
        log.debug("Getting Transfer Classes");
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        Query q = session.createQuery("SELECT c.classname FROM NodeConnector as nc, " + "Node as n, Connector as c, " + "Connectorfunction as cf " + "WHERE nc.node = n.node " + "and n.node = :nodename " + "and n.active = 1 " + "and nc.connector = c.connector " + "and c.connectorfunction = cf.function " + "and cf.function = :function " + "and c.active = 1 " + "ORDER BY nc.priority ASC").setCacheable(true);
        q.setString("function", actionType);
        q.setString("nodename", nodeName);
        List classList = q.list();
        log.debug("Retrieved " + classList.size() + " classes");
        return classList;
    }

    /**
	 * Get the tags for a specified transformation.
	 * 
	 * @param transformer
	 * @return Iterator
	 */
    public static List getTransformTags(String transformer) {
        log.debug("Getting Transformer Tags");
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        List tagList = session.createQuery("select t.tag," + " t.newvalue" + " from Transformation as t" + " where t.transformation = :transformation").setString("transformation", transformer).setCacheable(true).list();
        return tagList;
    }

    /**
	 * Get a list of all transformations and their tags.
	 * 
	 * @return List
	 */
    public static List getTransformationList(String transformer) {
        log.debug("Getting Transformer Tags");
        if ((transformer == null) || (transformer.equals(""))) {
            transformer = "%";
        }
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        List transformationList = session.createQuery("select t.transformation, t.tag," + " t.newvalue " + " from Transformation as t " + " where t.transformation like :transformation " + " ORDER by t.transformation, t.tag ").setString("transformation", transformer).setCacheable(true).list();
        return transformationList;
    }

    /**
	 * Adds transformation tags to the transformation table.
	 * 
	 * @param transformation
	 * @param tag
	 * @param newvalue
	 * @throws Exception
	 */
    public static void addTransformTag(String transformation, String tag, String newvalue) throws Exception {
        log.debug("Adding transformation tag");
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = session.beginTransaction();
        Transformation transformTable = new Transformation();
        transformTable.setTransformation(transformation);
        transformTable.setTag(tag);
        transformTable.setNewvalue(newvalue);
        try {
            session.save(transformTable);
            tx.commit();
        } catch (Exception e) {
            log.debug("Rolled Back Transaction: " + e);
            tx.rollback();
            throw new Exception(e);
        }
        return;
    }

    /**
	 * Removes a transformation tag fro mthe transformation table that exactly matches the specified values.
	 * This will return the number of rows that were removed.
	 * 
	 * @param transformation
	 * @param tag
	 * @param newvalue
	 * @return int
	 */
    public static int removeTransformTag(String transformation, String tag, String newvalue) {
        log.debug("Removing Transformation Tag");
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = session.beginTransaction();
        int deletedEntries = session.createQuery("delete transformation as t " + "WHERE t.transformation = :transformation " + "and t.tag like :tag " + "and t.newvalue like :newvalue ").setString("transformation", transformation).setString("tag", tag).setString("newvalue", newvalue).executeUpdate();
        try {
            tx.commit();
        } catch (Exception e) {
            log.debug("Rolled Back Transaction: " + e);
            tx.rollback();
        }
        return deletedEntries;
    }

    /**
	 * Get a list of node values based on the specified node and attribute.
	 * This can return multiple rows (eg: attribute "group" for node "X" = 3 groups that node X is a member of) 
	 * 
	 * @param nodeName
	 * @param attribute
	 * @return List
	 */
    public static List getNodeValue(String nodeName, String attribute) {
        log.debug("Getting Node Values for: " + attribute + " on " + nodeName);
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        List nodeValue = session.createQuery("select nv.value " + "  from Nodevalue as nv, Node as n" + " where n.node = nv.node" + " and n.node = :nodename" + " and nv.attribute = :attribute").setString("nodename", nodeName).setString("attribute", attribute).setCacheable(true).list();
        return nodeValue;
    }

    /**
	 * Get the nodes that are in group "X".
	 * Returns a List of nodes in the group based on the nodevalues table group attribute.
	 * 
	 * @param groupName
	 * @return List
	 */
    public static List getNodeByGroup(String groupName) {
        log.debug("Getting Nodes in group: " + groupName);
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        List nodeValue = session.createQuery("select n.node " + "from Nodevalue as nv, Node as n " + "where nv.node = n.node " + " and n.active = 1 " + "and nv.attribute = 'group' " + "and nv.value = :groupName").setString("groupName", groupName).setCacheable(true).list();
        log.debug("Found \"" + nodeValue.size() + "\" nodes in the group.");
        return nodeValue;
    }

    /**
	 * Gets a list of all node values.
	 * 
	 * @return List
	 */
    public static List getNodeValueList(String node, String attribute, String value) {
        log.debug("Getting Node Value List");
        if ((node == null) || (node.equals(""))) {
            node = "%";
        }
        if ((attribute == null) || (attribute.equals(""))) {
            attribute = "%";
        }
        if ((value == null) || (value.equals(""))) {
            value = "%";
        }
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        List nodeValueList = session.createQuery("select n.node, nv.attribute, nv.value " + "  from Nodevalue as nv, Node as n" + " where n.node = nv.node " + " and nv.node like :node " + " and nv.attribute like :attribute " + " and nv.value like :value " + "ORDER by n.node ASC").setString("node", node).setString("attribute", attribute).setString("value", value).setCacheable(true).list();
        return nodeValueList;
    }

    /**
	 * Adds a row to the nodevalue table based on the arguments provided.
	 * 
	 * @param nodeName
	 * @param attribute
	 * @param value
	 * @throws Exception
	 */
    public static void addNodeValue(String nodeName, String attribute, String value) throws Exception {
        log.debug("Adding Node Value \"" + attribute + "=" + value + "\" to " + nodeName);
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = session.beginTransaction();
        Nodevalue nodeTable = new Nodevalue();
        nodeTable.setNode(new Node(nodeName));
        nodeTable.setAttribute(attribute);
        nodeTable.setValue(value);
        try {
            session.saveOrUpdate(nodeTable);
            tx.commit();
        } catch (Exception e) {
            log.debug("Rolled Back Transaction: " + e);
            tx.rollback();
            throw new Exception("Error Setting Attribute on \"" + nodeName + "\": " + e.getCause().getMessage());
        }
        return;
    }

    /**
	 * Removes all rows from the nodevalue table that match the supplied arguments exactly.
	 * This returns the number of rows removed.
	 *  
	 * @param nodeName
	 * @param attribute
	 * @param value
	 * @return int
	 */
    public static int removeNodeValue(String nodeName, String attribute, String value) {
        log.debug("Removing Node Value");
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = session.beginTransaction();
        int deletedEntries = session.createQuery("delete nodevalue as nv " + "WHERE nv.node like :node " + "and nv.attribute like :attribute " + "and nv.value like :value ").setString("node", nodeName).setString("attribute", attribute).setString("value", value).executeUpdate();
        try {
            tx.commit();
        } catch (Exception e) {
            log.debug("Rolled Back Transaction: " + e);
            tx.rollback();
        }
        return deletedEntries;
    }

    /**
	 * Determines if the node is active or inactive and returns the value as true (active) or false (inactive).
	 * 
	 * @param nodeName
	 * @return Boolean
	 */
    public static Boolean getNodeActive(String nodeName) {
        log.debug("Getting Active Status on " + nodeName);
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        List nodeValue = session.createQuery("select n.active " + "  from Node as n" + " where n.node = :nodename").setString("nodename", nodeName).setCacheable(true).list();
        Boolean activeValue = false;
        if (nodeValue.size() == 1) {
            activeValue = (Boolean) nodeValue.get(0);
        }
        log.debug("Returning Active: " + activeValue);
        return activeValue;
    }

    /**
	 * Sets the specified node to active or inactive based on the provided value.
	 * 
	 * @param nodeName
	 * @param activeValue
	 * @throws Exception
	 */
    public static void setNodeActive(String nodeName, Boolean activeValue) throws Exception {
        log.debug("Setting Active Status on " + nodeName);
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = session.beginTransaction();
        Node nodeTable = new Node(nodeName);
        nodeTable.setActive(activeValue);
        try {
            session.saveOrUpdate(nodeTable);
            tx.commit();
        } catch (Exception e) {
            log.debug("Rolled Back Transaction: " + e);
            tx.rollback();
            throw new Exception("Error Setting Active Status on \"" + nodeName + "\": " + e.getCause().getMessage());
        }
        return;
    }

    /**
	 * Returns a list of all nodes and whether they are active or not.
	 * 
	 * @return List
	 */
    public static List<String> getNodeList(String nodename) {
        log.debug("Getting Node List");
        if ((nodename == null) || (nodename.trim().equals(""))) {
            log.debug("Nodename is null. Getting all values.");
            nodename = "%";
        }
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        Query q = session.createQuery("SELECT n.node, n.active FROM Node as n " + " where n.node like :nodename " + " ORDER BY n.node ASC ").setString("nodename", nodename).setCacheable(true);
        List<String> nodes = q.list();
        return nodes;
    }

    /**
	 * Adds a node to the node table using the specified arguments.
	 * 
	 * @param nodeName
	 * @param activeValue
	 * @throws Exception
	 */
    public static void addNode(String nodeName, Boolean activeValue) throws Exception {
        log.debug("Adding/Altering Node: " + nodeName);
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = session.beginTransaction();
        Node nodeTable = new Node();
        nodeTable.setNode(nodeName);
        nodeTable.setActive(activeValue);
        try {
            session.saveOrUpdate(nodeTable);
            tx.commit();
        } catch (Exception e) {
            log.debug("Rolled Back Transaction: " + e);
            tx.rollback();
            throw new Exception("Error Adding Node \"" + nodeName + "\": " + e.getCause().getMessage());
        }
        return;
    }

    /**
	 * Adds a row to the nodeconnector table effectively connecting a node to an action class.
	 * 
	 * @param nodeName
	 * @param connector
	 * @param priority
	 * @throws Exception
	 */
    public static void addNodeConnector(String nodeName, String connector, int priority) throws Exception {
        log.debug("Adding Node Connector: " + nodeName);
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = session.beginTransaction();
        Nodeconnector nodeTable = new Nodeconnector();
        nodeTable.setNode(new Node(nodeName));
        nodeTable.setConnector(new Connector(connector, null, null));
        nodeTable.setPriority(priority);
        try {
            session.saveOrUpdate(nodeTable);
            tx.commit();
        } catch (Exception e) {
            log.debug("Rolled Back Transaction: " + e);
            tx.rollback();
            throw new Exception("Error Adding Node \"" + nodeName + "\": " + e.getCause().getMessage());
        }
        return;
    }

    /**
	 * Alters the priority of the specified connector to the specified value.
	 * Returns the number of rows updated.
	 * 
	 * @param nodeName
	 * @param connector
	 * @param priority
	 * @return int
	 * @throws Exception
	 */
    public static int setNodeConnectorPriority(String nodeName, String connector, String priority) throws Exception {
        log.debug("Adding Node Connector: " + nodeName);
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = session.beginTransaction();
        int updateNumber = session.createQuery("update nodeconnector as nc " + "set nc.priority = :priority " + " where nc.node = :node " + "and nc.Connector = :connector ").setString("priority", priority).setString("node", nodeName).setString("connector", connector).executeUpdate();
        try {
            tx.commit();
        } catch (Exception e) {
            log.debug("Rolled Back Transaction: " + e);
            tx.rollback();
            throw new Exception("Error Adding Node \"" + nodeName + "\": " + e.getCause().getMessage());
        }
        return updateNumber;
    }

    /**
	 * Removes a row from the nodeconnector table based on an exact match of the supplied properties.
	 * Returns the number of rows that were removed.
	 * 
	 * @param nodeName
	 * @param connector
	 * @return int
	 */
    public static int removeNodeConnector(String nodeName, String connector) {
        log.debug("Removing Node Connector");
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = session.beginTransaction();
        int deletedEntries = session.createQuery("delete nodeconnector as nc " + "WHERE nc.node like :node " + "and nc.connector like :connector ").setString("node", nodeName).setString("connector", connector).executeUpdate();
        try {
            tx.commit();
        } catch (Exception e) {
            log.debug("Rolled Back Transaction: " + e);
            tx.rollback();
        }
        return deletedEntries;
    }

    /**
	 * Gets a list of all node connectors from the nodeconnector table.
	 * 
	 * @return List
	 */
    public static List getNodeConnectorList(String node, String connector, String priority) {
        if ((node == null) || (node.equals(""))) {
            node = "%";
        }
        if ((connector == null) || (connector.equals(""))) {
            connector = "%";
        }
        if ((priority == null) || (priority.equals(""))) {
            priority = "%";
        }
        log.debug("Getting Node Connector List");
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        List connectorList = session.createQuery("select c.connector, n.node, nc.priority " + " FROM Nodeconnector as nc, " + " Connector as c, " + " Node as n " + " WHERE nc.connector = c.connector " + " and nc.node = n.node " + " and c.connector like :connector " + " and n.node like :node " + "ORDER by n.node ASC").setString("node", node).setString("connector", connector).setCacheable(true).list();
        return connectorList;
    }
}
