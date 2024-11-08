package org.opennms.netmgt.outage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.Enumeration;
import org.apache.log4j.Category;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.EventConstants;
import org.opennms.netmgt.config.DatabaseConnectionFactory;
import org.opennms.netmgt.config.OutageManagerConfigFactory;
import org.opennms.netmgt.xml.event.Event;
import org.opennms.netmgt.xml.event.Parm;
import org.opennms.netmgt.xml.event.Parms;
import org.opennms.netmgt.xml.event.Value;

/**
 * When a 'nodeLostService' is received, it is made sure that there is no
 * 'open' outage record in the 'outages' table for this nodeid/ipaddr/serviceid
 * - i.e that there is not already a record for this n/i/s where the 'lostService'
 * time is known and the 'regainedService' time is NULL - if there is, the
 * current 'lostService' event is ignored else a new outage is created.
 *
 * The 'interfaceDown' is similar to the 'nodeLostService' except that it acts
 * relevant to a nodeid/ipaddr combination and a 'nodeDown' acts on a nodeid.
 *
 * When a 'nodeRegainedService' is received and there is an 'open' outage for
 * the nodeid/ipaddr/serviceid, the outage is cleared. If not, the event is placed
 * in the event cache in case a race condition has occurred that puts the "up"
 * event in before the "down" event. (currently inactive).
 *
 * The 'interfaceUp' is similar to the 'nodeRegainedService' except that it acts
 * relevant to a nodeid/ipaddr combination and a 'nodeUp' acts on a nodeid.
 *
 * When a 'deleteService' is received, the appropriate entry is marked for
 * deletion is the 'ifservices' table - if this entry is the only entry for
 * a node/ip combination, the corresponding entry in the 'ipinterface' table
 * is marked for deletion and this is then cascaded to the node table
 * All deletions are followed by an appropriate event(serviceDeleted or
 * interfaceDeleted or..) being generated and sent to eventd.
 *
 * When an 'interfaceReparented' event is received, 'outages' table entries
 * associated with the old nodeid/interface pairing are changed so that those outage
 * entries will be associated with the new nodeid/interface pairing.
 *
 * The nodeLostService, interfaceDown, nodeDown, nodeUp, interfaceUp,
 * nodeRegainedService, deleteService events update the svcLostEventID and the 
 * svcRegainedEventID fields as approppriate. The interfaceReparented event has
 * no impact on these eventid reference fields.
 * @author 	<A HREF="mailto:sowmya@opennms.org">Sowmya Nataraj</A>
 * @author 	<A HREF="mailto:mike@opennms.org">Mike Davidson</A>
 * @author	<A HREF="http://www.opennms.org">OpenNMS.org</A>
 */
public final class OutageWriter implements Runnable {

    private static final String SNMP_SVC = "SNMP";

    private static final String SNMPV2_SVC = "SNMPv2";

    /**
	 * The event from which data is to be read.
	 */
    private Event m_event;

    private boolean m_generateNodeDeletedEvent;

    /**
	 * A class to hold SNMP/SNMPv2 entries for a node from the ifservices table.
	 * A list of this class is maintained on SNMP delete so as to be able to
	 * generate a series of serviceDeleted for all entries marked as 'D'
	 */
    private static class IfSvcSnmpEntry {

        private long m_nodeID;

        private String m_ipAddr;

        private String m_svcName;

        IfSvcSnmpEntry(long nodeid, String ip, String sname) {
            m_nodeID = nodeid;
            m_ipAddr = ip;
            m_svcName = sname;
        }

        long getNodeID() {
            return m_nodeID;
        }

        String getIP() {
            return m_ipAddr;
        }

        String getService() {
            return m_svcName;
        }
    }

    /**
         * Convert event time into timestamp
         */
    private java.sql.Timestamp convertEventTimeIntoTimestamp(String eventTime) {
        java.sql.Timestamp timestamp = null;
        try {
            java.util.Date date = EventConstants.parseToDate(eventTime);
            timestamp = new java.sql.Timestamp(date.getTime());
        } catch (ParseException e) {
            ThreadCategory.getInstance(OutageWriter.class).warn("Failed to convert event time " + eventTime + " to timestamp.", e);
            timestamp = new java.sql.Timestamp((new java.util.Date()).getTime());
        }
        return timestamp;
    }

    /**
	 * <P>This method is used to convert the service name into
	 * a service id. It first looks up the information from a
	 * service map in OutagesManager and if no match is found, by performing
	 * a lookup in the database. If the conversion is successful then the
	 * corresponding integer identifier will be returned to the caller.</P>
	 *
	 * @param name		The name of the service
	 *
	 * @return The integer identifier for the service name.
	 *
	 * @throws java.sql.SQLException if there is an error accessing
	 * 	the stored data, the SQL text is malformed, or the result 
	 * 	cannot be obtained.
	 *
	 * @see org.opennms.netmgt.outage.OutageConstants#DB_GET_SVC_ID DB_GET_SVC_ID
	 */
    private long getServiceID(String name) throws SQLException {
        if (name == null) throw new NullPointerException("The service name was null");
        long id = OutageManager.getInstance().getServiceID(name);
        if (id != -1) return id;
        Connection dbConn = null;
        try {
            dbConn = DatabaseConnectionFactory.getInstance().getConnection();
            PreparedStatement serviceStmt = dbConn.prepareStatement(OutageConstants.DB_GET_SVC_ID);
            serviceStmt.setString(1, name);
            ResultSet rset = serviceStmt.executeQuery();
            if (rset.next()) {
                id = rset.getLong(1);
            }
            rset.close();
            if (serviceStmt != null) serviceStmt.close();
        } finally {
            try {
                if (dbConn != null) dbConn.close();
            } catch (SQLException e) {
                ThreadCategory.getInstance(getClass()).warn("Exception closing JDBC connection", e);
            }
        }
        if (id != -1) OutageManager.getInstance().addServiceMapping(name, id);
        return id;
    }

    /**
         * This method checks the outage table and determines if an open outage
         * entry exists for the specified node id.
         *
         * @throws SQLException if database error encountered.
         */
    private boolean openOutageExists(Connection dbConn, long nodeId) throws SQLException {
        return openOutageExists(dbConn, nodeId, null, -1);
    }

    /**
         * This method checks the outage table and determines if an open outage
         * entry exists for the specified node/ip pair.
         *
         * @throws SQLException if database error encountered.
         */
    private boolean openOutageExists(Connection dbConn, long nodeId, String ipAddr) throws SQLException {
        return openOutageExists(dbConn, nodeId, ipAddr, -1);
    }

    /**
         * This method checks the outage table and determines if an open outage
         * entry exists for the specified node/ip/service tuple.
         *
         * @throws SQLException if database error encountered.
         */
    private boolean openOutageExists(Connection dbConn, long nodeId, String ipAddr, long serviceId) throws SQLException {
        int numOpenOutages = -1;
        PreparedStatement openStmt = null;
        if (ipAddr != null && serviceId > 0) {
            openStmt = dbConn.prepareStatement(OutageConstants.DB_OPEN_RECORD);
            openStmt.setLong(1, nodeId);
            openStmt.setString(2, ipAddr);
            openStmt.setLong(3, serviceId);
        } else if (ipAddr != null) {
            openStmt = dbConn.prepareStatement(OutageConstants.DB_OPEN_RECORD_2);
            openStmt.setLong(1, nodeId);
            openStmt.setString(2, ipAddr);
        } else {
            openStmt = dbConn.prepareStatement(OutageConstants.DB_OPEN_RECORD_3);
            openStmt.setLong(1, nodeId);
        }
        ResultSet rs = openStmt.executeQuery();
        if (rs.next()) {
            numOpenOutages = rs.getInt(1);
        }
        rs.close();
        openStmt.close();
        if (numOpenOutages > 0) return true; else return false;
    }

    /**
	 * Handles node lost service events.
	 * Record the 'nodeLostService' event in the outages table - create
	 * a new outage entry if the service is not already down.
	 */
    private void handleNodeLostService(long eventID, long nodeID, String ipAddr, long serviceID, String eventTime) {
        Category log = ThreadCategory.getInstance(OutageWriter.class);
        if (eventID == -1 || nodeID == -1 || ipAddr == null || serviceID == -1) {
            log.warn(EventConstants.NODE_REGAINED_SERVICE_EVENT_UEI + " ignored - info incomplete - eventid/nodeid/ip/svc: " + eventID + "/" + nodeID + "/" + ipAddr + "/" + serviceID);
            return;
        }
        Connection dbConn = null;
        try {
            dbConn = DatabaseConnectionFactory.getInstance().getConnection();
            if (openOutageExists(dbConn, nodeID, ipAddr, serviceID)) {
                log.warn("\'" + EventConstants.NODE_LOST_SERVICE_EVENT_UEI + "\' for " + nodeID + "/" + ipAddr + "/" + serviceID + " ignored - table already  has an open record ");
            } else {
                PreparedStatement getNextOutageIdStmt = dbConn.prepareStatement(OutageManagerConfigFactory.getInstance().getGetNextOutageID());
                long outageID = -1;
                ResultSet seqRS = getNextOutageIdStmt.executeQuery();
                if (seqRS.next()) {
                    outageID = seqRS.getLong(1);
                }
                seqRS.close();
                try {
                    dbConn.setAutoCommit(false);
                } catch (SQLException sqle) {
                    log.error("Unable to change database AutoCommit to FALSE", sqle);
                    return;
                }
                PreparedStatement newOutageWriter = null;
                if (log.isDebugEnabled()) log.debug("handleNodeLostService: creating new outage entry...");
                newOutageWriter = dbConn.prepareStatement(OutageConstants.DB_INS_NEW_OUTAGE);
                newOutageWriter.setLong(1, outageID);
                newOutageWriter.setLong(2, eventID);
                newOutageWriter.setLong(3, nodeID);
                newOutageWriter.setString(4, ipAddr);
                newOutageWriter.setLong(5, serviceID);
                newOutageWriter.setTimestamp(6, convertEventTimeIntoTimestamp(eventTime));
                newOutageWriter.executeUpdate();
                newOutageWriter.close();
                try {
                    dbConn.commit();
                    if (log.isDebugEnabled()) log.debug("nodeLostService : " + nodeID + "/" + ipAddr + "/" + serviceID + " recorded in DB");
                } catch (SQLException se) {
                    log.warn("Rolling back transaction, nodeLostService could not be recorded  for nodeid/ipAddr/service: " + nodeID + "/" + ipAddr + "/" + serviceID, se);
                    try {
                        dbConn.rollback();
                    } catch (SQLException sqle) {
                        log.warn("SQL exception during rollback, reason", sqle);
                    }
                }
            }
        } catch (SQLException sqle) {
            log.warn("SQL exception while handling \'nodeLostService\'", sqle);
        } finally {
            try {
                if (dbConn != null) dbConn.close();
            } catch (SQLException e) {
                log.warn("Exception closing JDBC connection", e);
            }
        }
    }

    /**
	 * Handles interface down events.
	 * Record the 'interfaceDown' event in the outages table - create
	 * a new outage entry for each active service of the nodeid/ip if
	 * service not already down.
	 */
    private void handleInterfaceDown(long eventID, long nodeID, String ipAddr, String eventTime) {
        Category log = ThreadCategory.getInstance(OutageWriter.class);
        if (eventID == -1 || nodeID == -1 || ipAddr == null) {
            log.warn(EventConstants.INTERFACE_DOWN_EVENT_UEI + " ignored - info incomplete - eventid/nodeid/ip: " + eventID + "/" + nodeID + "/" + ipAddr);
            return;
        }
        Connection dbConn = null;
        try {
            dbConn = DatabaseConnectionFactory.getInstance().getConnection();
            try {
                dbConn.setAutoCommit(false);
            } catch (SQLException sqle) {
                log.error("Unable to change database AutoCommit to FALSE", sqle);
                return;
            }
            PreparedStatement activeSvcsStmt = dbConn.prepareStatement(OutageConstants.DB_GET_ACTIVE_SERVICES_FOR_INTERFACE);
            PreparedStatement openStmt = dbConn.prepareStatement(OutageConstants.DB_OPEN_RECORD);
            PreparedStatement newOutageWriter = dbConn.prepareStatement(OutageConstants.DB_INS_NEW_OUTAGE);
            PreparedStatement getNextOutageIdStmt = dbConn.prepareStatement(OutageManagerConfigFactory.getInstance().getGetNextOutageID());
            newOutageWriter = dbConn.prepareStatement(OutageConstants.DB_INS_NEW_OUTAGE);
            if (log.isDebugEnabled()) log.debug("handleInterfaceDown: creating new outage entries...");
            activeSvcsStmt.setLong(1, nodeID);
            activeSvcsStmt.setString(2, ipAddr);
            ResultSet activeSvcsRS = activeSvcsStmt.executeQuery();
            while (activeSvcsRS.next()) {
                long serviceID = activeSvcsRS.getLong(1);
                if (openOutageExists(dbConn, nodeID, ipAddr, serviceID)) {
                    if (log.isDebugEnabled()) log.debug("handleInterfaceDown: " + nodeID + "/" + ipAddr + "/" + serviceID + " already down");
                } else {
                    long outageID = -1;
                    ResultSet seqRS = getNextOutageIdStmt.executeQuery();
                    if (seqRS.next()) {
                        outageID = seqRS.getLong(1);
                    }
                    seqRS.close();
                    newOutageWriter.setLong(1, outageID);
                    newOutageWriter.setLong(2, eventID);
                    newOutageWriter.setLong(3, nodeID);
                    newOutageWriter.setString(4, ipAddr);
                    newOutageWriter.setLong(5, serviceID);
                    newOutageWriter.setTimestamp(6, convertEventTimeIntoTimestamp(eventTime));
                    newOutageWriter.executeUpdate();
                    if (log.isDebugEnabled()) log.debug("handleInterfaceDown: Recording new outage for " + nodeID + "/" + ipAddr + "/" + serviceID);
                }
            }
            activeSvcsRS.close();
            try {
                dbConn.commit();
                if (log.isDebugEnabled()) log.debug("Outage recorded for all active services for " + nodeID + "/" + ipAddr);
            } catch (SQLException se) {
                log.warn("Rolling back transaction, interfaceDown could not be recorded  for nodeid/ipAddr: " + nodeID + "/" + ipAddr, se);
                try {
                    dbConn.rollback();
                } catch (SQLException sqle) {
                    log.warn("SQL exception during rollback, reason", sqle);
                }
            }
            activeSvcsStmt.close();
            openStmt.close();
            newOutageWriter.close();
        } catch (SQLException sqle) {
            log.warn("SQL exception while handling \'interfaceDown\'", sqle);
        } finally {
            try {
                if (dbConn != null) dbConn.close();
            } catch (SQLException e) {
                log.warn("Exception closing JDBC connection", e);
            }
        }
    }

    /**
	 * Handles node down events.
	 * Record the 'nodeDown' event in the outages table - create a new
	 * outage entry for each active service of the nodeid if service is
	 * not already down.
	 */
    private void handleNodeDown(long eventID, long nodeID, String eventTime) {
        Category log = ThreadCategory.getInstance(OutageWriter.class);
        if (eventID == -1 || nodeID == -1) {
            log.warn(EventConstants.NODE_DOWN_EVENT_UEI + " ignored - info incomplete - eventid/nodeid: " + eventID + "/" + nodeID);
            return;
        }
        Connection dbConn = null;
        try {
            dbConn = DatabaseConnectionFactory.getInstance().getConnection();
            try {
                dbConn.setAutoCommit(false);
            } catch (SQLException sqle) {
                log.error("Unable to change database AutoCommit to FALSE", sqle);
                return;
            }
            PreparedStatement activeSvcsStmt = dbConn.prepareStatement(OutageConstants.DB_GET_ACTIVE_SERVICES_FOR_NODE);
            PreparedStatement openStmt = dbConn.prepareStatement(OutageConstants.DB_OPEN_RECORD);
            PreparedStatement newOutageWriter = dbConn.prepareStatement(OutageConstants.DB_INS_NEW_OUTAGE);
            PreparedStatement getNextOutageIdStmt = dbConn.prepareStatement(OutageManagerConfigFactory.getInstance().getGetNextOutageID());
            newOutageWriter = dbConn.prepareStatement(OutageConstants.DB_INS_NEW_OUTAGE);
            if (log.isDebugEnabled()) log.debug("handleNodeDown: creating new outage entries...");
            activeSvcsStmt.setLong(1, nodeID);
            ResultSet activeSvcsRS = activeSvcsStmt.executeQuery();
            while (activeSvcsRS.next()) {
                String ipAddr = activeSvcsRS.getString(1);
                long serviceID = activeSvcsRS.getLong(2);
                if (openOutageExists(dbConn, nodeID, ipAddr, serviceID)) {
                    if (log.isDebugEnabled()) log.debug("handleNodeDown: " + nodeID + "/" + ipAddr + "/" + serviceID + " already down");
                } else {
                    long outageID = -1;
                    ResultSet seqRS = getNextOutageIdStmt.executeQuery();
                    if (seqRS.next()) {
                        outageID = seqRS.getLong(1);
                    }
                    seqRS.close();
                    newOutageWriter.setLong(1, outageID);
                    newOutageWriter.setLong(2, eventID);
                    newOutageWriter.setLong(3, nodeID);
                    newOutageWriter.setString(4, ipAddr);
                    newOutageWriter.setLong(5, serviceID);
                    newOutageWriter.setTimestamp(6, convertEventTimeIntoTimestamp(eventTime));
                    newOutageWriter.executeUpdate();
                    if (log.isDebugEnabled()) log.debug("handleNodeDown: Recording outage for " + nodeID + "/" + ipAddr + "/" + serviceID);
                }
            }
            activeSvcsRS.close();
            try {
                dbConn.commit();
                if (log.isDebugEnabled()) log.debug("Outage recorded for all active services for " + nodeID);
            } catch (SQLException se) {
                log.warn("Rolling back transaction, nodeDown could not be recorded  for nodeId: " + nodeID, se);
                try {
                    dbConn.rollback();
                } catch (SQLException sqle) {
                    log.warn("SQL exception during rollback, reason", sqle);
                }
            }
            activeSvcsStmt.close();
            openStmt.close();
            newOutageWriter.close();
        } catch (SQLException sqle) {
            log.warn("SQL exception while handling \'nodeDown\'", sqle);
        } finally {
            try {
                if (dbConn != null) dbConn.close();
            } catch (SQLException e) {
                log.warn("Exception closing JDBC connection", e);
            }
        }
    }

    /**
	 * Handle node up events.
	 * Record the 'nodeUp' event in the outages table - close all open
	 * outage entries for the nodeid in the outages table.
	 */
    private void handleNodeUp(long eventID, long nodeID, String eventTime) {
        Category log = ThreadCategory.getInstance(OutageWriter.class);
        if (eventID == -1 || nodeID == -1) {
            log.warn(EventConstants.NODE_UP_EVENT_UEI + " ignored - info incomplete - eventid/nodeid: " + eventID + "/" + nodeID);
            return;
        }
        Connection dbConn = null;
        try {
            dbConn = DatabaseConnectionFactory.getInstance().getConnection();
            int count = 0;
            if (openOutageExists(dbConn, nodeID)) {
                try {
                    dbConn.setAutoCommit(false);
                } catch (SQLException sqle) {
                    log.error("Unable to change database AutoCommit to FALSE", sqle);
                    return;
                }
                PreparedStatement outageUpdater = dbConn.prepareStatement(OutageConstants.DB_UPDATE_OUTAGES_FOR_NODE);
                outageUpdater.setLong(1, eventID);
                outageUpdater.setTimestamp(2, convertEventTimeIntoTimestamp(eventTime));
                outageUpdater.setLong(3, nodeID);
                count = outageUpdater.executeUpdate();
                outageUpdater.close();
            } else {
                log.warn("\'" + EventConstants.NODE_UP_EVENT_UEI + "\' for " + nodeID + " no open record.");
            }
            try {
                dbConn.commit();
                if (log.isDebugEnabled()) log.debug("nodeUp closed " + count + " outages for nodeid " + nodeID + " in DB");
            } catch (SQLException se) {
                log.warn("Rolling back transaction, nodeUp could not be recorded  for nodeId: " + nodeID, se);
                try {
                    dbConn.rollback();
                } catch (SQLException sqle) {
                    log.warn("SQL exception during rollback, reason", sqle);
                }
            }
        } catch (SQLException se) {
            log.warn("SQL exception while handling \'nodeRegainedService\'", se);
        } finally {
            try {
                if (dbConn != null) dbConn.close();
            } catch (SQLException e) {
                log.warn("Exception closing JDBC connection", e);
            }
        }
    }

    /**
	 * Handles interface up events.
	 * Record the 'interfaceUp' event in the outages table - close all open
	 * outage entries for the nodeid/ip in the outages table.
	 */
    private void handleInterfaceUp(long eventID, long nodeID, String ipAddr, String eventTime) {
        Category log = ThreadCategory.getInstance(OutageWriter.class);
        if (eventID == -1 || nodeID == -1 || ipAddr == null) {
            log.warn(EventConstants.INTERFACE_UP_EVENT_UEI + " ignored - info incomplete - eventid/nodeid/ipAddr: " + eventID + "/" + nodeID + "/" + ipAddr);
            return;
        }
        Connection dbConn = null;
        try {
            dbConn = DatabaseConnectionFactory.getInstance().getConnection();
            if (openOutageExists(dbConn, nodeID, ipAddr)) {
                try {
                    dbConn.setAutoCommit(false);
                } catch (SQLException sqle) {
                    log.error("Unable to change database AutoCommit to FALSE", sqle);
                    return;
                }
                PreparedStatement outageUpdater = dbConn.prepareStatement(OutageConstants.DB_UPDATE_OUTAGES_FOR_INTERFACE);
                outageUpdater.setLong(1, eventID);
                outageUpdater.setTimestamp(2, convertEventTimeIntoTimestamp(eventTime));
                outageUpdater.setLong(3, nodeID);
                outageUpdater.setString(4, ipAddr);
                int count = outageUpdater.executeUpdate();
                outageUpdater.close();
                try {
                    dbConn.commit();
                    if (log.isDebugEnabled()) log.debug("handleInterfaceUp: interfaceUp closed " + count + " outages for nodeid/ip " + nodeID + "/" + ipAddr + " in DB");
                } catch (SQLException se) {
                    log.warn("Rolling back transaction, interfaceUp could not be recorded for nodeId/ipaddr: " + nodeID + "/" + ipAddr, se);
                    try {
                        dbConn.rollback();
                    } catch (SQLException sqle) {
                        log.warn("SQL exception during rollback, reason: ", sqle);
                    }
                }
            } else {
                log.warn("\'" + EventConstants.INTERFACE_UP_EVENT_UEI + "\' for " + nodeID + "/" + ipAddr + " ignored.");
            }
        } catch (SQLException se) {
            log.warn("SQL exception while handling \'interfaceUp\'", se);
        } finally {
            try {
                if (dbConn != null) dbConn.close();
            } catch (SQLException e) {
                log.warn("Exception closing JDBC connection", e);
            }
        }
    }

    /**
	 * Hanlde node regained service events.
	 * Record the 'nodeRegainedService' event in the outages table - close
	 * the outage entry in the table if the service is currently down.
	 */
    private void handleNodeRegainedService(long eventID, long nodeID, String ipAddr, long serviceID, String eventTime) {
        Category log = ThreadCategory.getInstance(OutageWriter.class);
        if (eventID == -1 || nodeID == -1 || ipAddr == null || serviceID == -1) {
            log.warn(EventConstants.NODE_REGAINED_SERVICE_EVENT_UEI + " ignored - info incomplete - eventid/nodeid/ip/svc: " + eventID + "/" + nodeID + "/" + ipAddr + "/" + serviceID);
            return;
        }
        Connection dbConn = null;
        try {
            dbConn = DatabaseConnectionFactory.getInstance().getConnection();
            if (openOutageExists(dbConn, nodeID, ipAddr, serviceID)) {
                try {
                    dbConn.setAutoCommit(false);
                } catch (SQLException sqle) {
                    log.error("Unable to change database AutoCommit to FALSE", sqle);
                    return;
                }
                PreparedStatement outageUpdater = dbConn.prepareStatement(OutageConstants.DB_UPDATE_OUTAGE_FOR_SERVICE);
                outageUpdater.setLong(1, eventID);
                outageUpdater.setTimestamp(2, convertEventTimeIntoTimestamp(eventTime));
                outageUpdater.setLong(3, nodeID);
                outageUpdater.setString(4, ipAddr);
                outageUpdater.setLong(5, serviceID);
                outageUpdater.executeUpdate();
                outageUpdater.close();
                try {
                    dbConn.commit();
                    if (log.isDebugEnabled()) log.debug("nodeRegainedService: closed outage for nodeid/ip/service " + nodeID + "/" + ipAddr + "/" + serviceID + " in DB");
                } catch (SQLException se) {
                    log.warn("Rolling back transaction, nodeRegainedService could not be recorded  for nodeId/ipAddr/service: " + nodeID + "/" + ipAddr + "/" + serviceID, se);
                    try {
                        dbConn.rollback();
                    } catch (SQLException sqle) {
                        log.warn("SQL exception during rollback, reason", sqle);
                    }
                }
            } else {
                log.warn("\'" + EventConstants.NODE_REGAINED_SERVICE_EVENT_UEI + "\' for " + nodeID + "/" + ipAddr + "/" + serviceID + " does not have open record.");
            }
        } catch (SQLException se) {
            log.warn("SQL exception while handling \'nodeRegainedService\'", se);
        } finally {
            try {
                if (dbConn != null) dbConn.close();
            } catch (SQLException e) {
                log.warn("Exception closing JDBC connection", e);
            }
        }
    }

    /**
	 * <p>Record the 'interfaceReparented' event in the outages table. 
	 * Change'outages' table entries associated with the old nodeid/interface
	 * pairing so that those outage entries will be associated with
	 * the new nodeid/interface pairing.</p>
	 *
	 * <p><strong>Note:</strong>This event has no impact on the event id reference
	 * fields</p>
	 */
    private void handleInterfaceReparented(String ipAddr, Parms eventParms) {
        Category log = ThreadCategory.getInstance(OutageWriter.class);
        if (log.isDebugEnabled()) log.debug("interfaceReparented event received...");
        if (ipAddr == null || eventParms == null) {
            log.warn(EventConstants.INTERFACE_REPARENTED_EVENT_UEI + " ignored - info incomplete - ip/parms: " + ipAddr + "/" + eventParms);
            return;
        }
        long oldNodeId = -1;
        long newNodeId = -1;
        String parmName = null;
        Value parmValue = null;
        String parmContent = null;
        Enumeration parmEnum = eventParms.enumerateParm();
        while (parmEnum.hasMoreElements()) {
            Parm parm = (Parm) parmEnum.nextElement();
            parmName = parm.getParmName();
            parmValue = parm.getValue();
            if (parmValue == null) continue; else parmContent = parmValue.getContent();
            if (parmName.equals(EventConstants.PARM_OLD_NODEID)) {
                try {
                    oldNodeId = Integer.valueOf(parmContent).intValue();
                } catch (NumberFormatException nfe) {
                    log.warn("Parameter " + EventConstants.PARM_OLD_NODEID + " cannot be non-numeric");
                    oldNodeId = -1;
                }
            } else if (parmName.equals(EventConstants.PARM_NEW_NODEID)) {
                try {
                    newNodeId = Integer.valueOf(parmContent).intValue();
                } catch (NumberFormatException nfe) {
                    log.warn("Parameter " + EventConstants.PARM_NEW_NODEID + " cannot be non-numeric");
                    newNodeId = -1;
                }
            }
        }
        if (newNodeId == -1 || oldNodeId == -1) {
            log.warn("Unable to process 'interfaceReparented' event, invalid event parm.");
            return;
        }
        Connection dbConn = null;
        try {
            dbConn = DatabaseConnectionFactory.getInstance().getConnection();
            try {
                dbConn.setAutoCommit(false);
            } catch (SQLException sqle) {
                log.error("Unable to change database AutoCommit to FALSE", sqle);
                return;
            }
            PreparedStatement reparentOutagesStmt = dbConn.prepareStatement(OutageConstants.DB_REPARENT_OUTAGES);
            reparentOutagesStmt.setLong(1, newNodeId);
            reparentOutagesStmt.setLong(2, oldNodeId);
            reparentOutagesStmt.setString(3, ipAddr);
            int count = reparentOutagesStmt.executeUpdate();
            try {
                dbConn.commit();
                if (log.isDebugEnabled()) log.debug("Reparented " + count + " outages - ip: " + ipAddr + " reparented from " + oldNodeId + " to " + newNodeId);
            } catch (SQLException se) {
                log.warn("Rolling back transaction, reparent outages failed for newNodeId/ipAddr: " + newNodeId + "/" + ipAddr);
                try {
                    dbConn.rollback();
                } catch (SQLException sqle) {
                    log.warn("SQL exception during rollback, reason", sqle);
                }
            }
            reparentOutagesStmt.close();
        } catch (SQLException se) {
            log.warn("SQL exception while handling \'interfaceReparented\'", se);
        } finally {
            try {
                if (dbConn != null) dbConn.close();
            } catch (SQLException e) {
                log.warn("Exception closing JDBC connection", e);
            }
        }
    }

    /**
	 * This method creates an event for the passed parameters.
	 *
	 * @param uei		Event to generate and send
	 * @param eventDate	Time to be set for the event
	 * @param nodeID	Node identifier associated with this event
	 * @param ipAddr	Interface address associated with this event
	 * @param serviceName	Service name associated with this event
	 */
    private Event createEvent(String uei, java.util.Date eventDate, long nodeID, String ipAddr, String serviceName) {
        Event newEvent = new Event();
        newEvent.setUei(uei);
        newEvent.setSource("OutageManager");
        newEvent.setNodeid(nodeID);
        if (ipAddr != null) newEvent.setInterface(ipAddr);
        if (serviceName != null) newEvent.setService(serviceName);
        newEvent.setTime(EventConstants.formatToString(eventDate));
        return newEvent;
    }

    /**
         * Process an event.
	 * Read the event UEI, nodeid, interface and service - depending
	 * on the UEI, read event parms, if necessary, and process as appropriate.
	 */
    private void processEvent() {
        Category log = ThreadCategory.getInstance(OutageWriter.class);
        if (m_event == null) {
            if (log.isDebugEnabled()) log.debug("Event is null, nothing to process");
            return;
        }
        if (log.isDebugEnabled()) log.debug("About to process event: " + m_event.getUei());
        String uei = m_event.getUei();
        if (uei == null) {
            if (log.isDebugEnabled()) log.debug("Event received with null UEI, ignoring event");
            return;
        }
        long eventID = -1;
        if (m_event.hasDbid()) eventID = m_event.getDbid();
        long nodeID = -1;
        if (m_event.hasNodeid()) nodeID = m_event.getNodeid();
        String ipAddr = m_event.getInterface();
        String service = m_event.getService();
        String eventTime = m_event.getTime();
        if (log.isDebugEnabled()) log.debug("processEvent: Event\nuei\t\t" + uei + "\neventid\t\t" + eventID + "\nnodeid\t\t" + nodeID + "\nipaddr\t\t" + ipAddr + "\nservice\t\t" + service + "\neventtime\t" + (eventTime != null ? eventTime : "<null>"));
        long serviceID = -1;
        if (service != null) {
            try {
                serviceID = getServiceID(service);
            } catch (SQLException sqlE) {
                log.warn("Error converting service name \"" + service + "\" to an integer identifier, storing -1", sqlE);
            }
        }
        if (uei.equals(EventConstants.NODE_LOST_SERVICE_EVENT_UEI)) {
            handleNodeLostService(eventID, nodeID, ipAddr, serviceID, eventTime);
        } else if (uei.equals(EventConstants.INTERFACE_DOWN_EVENT_UEI)) {
            handleInterfaceDown(eventID, nodeID, ipAddr, eventTime);
        } else if (uei.equals(EventConstants.NODE_DOWN_EVENT_UEI)) {
            handleNodeDown(eventID, nodeID, eventTime);
        } else if (uei.equals(EventConstants.NODE_UP_EVENT_UEI)) {
            handleNodeUp(eventID, nodeID, eventTime);
        } else if (uei.equals(EventConstants.INTERFACE_UP_EVENT_UEI)) {
            handleInterfaceUp(eventID, nodeID, ipAddr, eventTime);
        } else if (uei.equals(EventConstants.NODE_REGAINED_SERVICE_EVENT_UEI)) {
            handleNodeRegainedService(eventID, nodeID, ipAddr, serviceID, eventTime);
        } else if (uei.equals(EventConstants.INTERFACE_REPARENTED_EVENT_UEI)) {
            handleInterfaceReparented(ipAddr, m_event.getParms());
        }
    }

    /**
	 * The constructor.
	 *
	 * @param event	the event for this outage writer.
	 */
    public OutageWriter(Event event) {
        m_event = event;
    }

    /**
	 * Process the event depending on the UEI.
	 */
    public void run() {
        try {
            processEvent();
        } catch (Throwable t) {
            Category log = ThreadCategory.getInstance(OutageWriter.class);
            log.warn("Unexpected exception processing event", t);
        }
    }
}
