package org.opennms.netmgt.capsd;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.ParseException;
import java.util.Date;
import org.apache.log4j.Category;
import org.opennms.core.utils.DBUtils;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.EventConstants;
import org.opennms.netmgt.config.DataSourceFactory;

/**
 * 
 * <p>
 * Once loaded or create, the class tracks any changes and will write those
 * changes to the database whenever the <code>store</code> method is invoked.
 * If a database conneciton is not passed to the store method, then a temporary
 * one is allocated to write the results.
 * </p>
 * 
 * <p>
 * NOTE: if the connection is passed in and is not in auto commit mode, then the
 * caller must call <code>commit</code> to inform the database that the
 * transaction is complete.
 * 
 * @author <a href="mailto:weave@oculan.com">Weave </a>
 * @author <a href="http://www.opennms.org/">OpenNMS </a>
 * 
 */
public final class DbIfServiceEntry {

    public static final char STATUS_UNMANAGED = 'U';

    public static final char STATUS_ACTIVE = 'A';

    public static final char STATUS_DELETED = 'D';

    public static final char STATUS_FORCED = 'F';

    public static final char STATUS_NOT_POLLED = 'N';

    public static final char STATUS_UNKNOWN = ' ';

    public static final char STATUS_SUSPEND = 'S';

    public static final char STATUS_RESUME = 'R';

    public static final char STATUS_REMOTE = 'X';

    static final char SOURCE_PLUGIN = 'P';

    static final char SOURCE_FORCED = 'F';

    static final char SOURCE_UNKNOWN = ' ';

    static final char NOTIFY_ON = 'Y';

    static final char NOTIFY_OFF = 'N';

    static final char NOTIFY_UNKNOWN = ' ';

    /**
     * The SQL statement used to read a node from the database. This record is
     * keyed by the node identifier and distributed poller name.
     */
    private static final String SQL_LOAD_REC = "SELECT ifIndex, lastGood, lastFail, qualifier, status, source, notify FROM ifServices WHERE nodeID = ? AND ipAddr = ? AND serviceID = ? AND status != 'D'";

    /**
     * True if this recored was loaded from the database. False if it's new.
     */
    private boolean m_fromDb;

    /**
     * The node identifier
     */
    private int m_nodeId;

    /**
     * The IP address.
     */
    private InetAddress m_ipAddr;

    /**
     * The integer service id
     */
    private int m_serviceId;

    /**
     * The SNMP ifIndex
     */
    private int m_ifIndex;

    /**
     * The status of the interface
     */
    private char m_status;

    /**
     * The managed status, if any
     */
    private char m_source;

    /**
     * The last time the interface failed.
     */
    private Timestamp m_lastFail;

    /**
     * The last time the interface succeeded.
     */
    private Timestamp m_lastGood;

    /**
     * The notification state.
     */
    private char m_notify;

    /**
     * The qualifier string
     */
    private String m_qualifier;

    /**
     * The bit map used to determine which elements have changed since the
     * record was created.
     */
    private int m_changed;

    private static final int CHANGED_IFINDEX = 1 << 0;

    private static final int CHANGED_LASTGOOD = 1 << 1;

    private static final int CHANGED_LASTFAIL = 1 << 2;

    private static final int CHANGED_STATUS = 1 << 3;

    private static final int CHANGED_SOURCE = 1 << 4;

    private static final int CHANGED_NOTIFY = 1 << 5;

    private static final int CHANGED_QUALIFIER = 1 << 6;

    /**
     * Inserts the new interface into the ipInterface table of the OpenNMS database.
     * 
     * @param c
     *            The connection to the database.
     * 
     * @throws java.sql.SQLException
     *             Thrown if an error occurs with the connection
     */
    private void insert(Connection c) throws SQLException {
        if (m_fromDb) throw new IllegalStateException("The record already exists in the database");
        StringBuffer names = new StringBuffer("INSERT INTO ifServices (nodeID,ipAddr,serviceID");
        StringBuffer values = new StringBuffer("?,?,?");
        if ((m_changed & CHANGED_IFINDEX) == CHANGED_IFINDEX) {
            values.append(",?");
            names.append(",ifIndex");
        }
        if ((m_changed & CHANGED_STATUS) == CHANGED_STATUS) {
            values.append(",?");
            names.append(",status");
        }
        if ((m_changed & CHANGED_LASTGOOD) == CHANGED_LASTGOOD) {
            values.append(",?");
            names.append(",lastGood");
        }
        if ((m_changed & CHANGED_LASTFAIL) == CHANGED_LASTFAIL) {
            values.append(",?");
            names.append(",lastFail");
        }
        if ((m_changed & CHANGED_SOURCE) == CHANGED_SOURCE) {
            values.append(",?");
            names.append(",source");
        }
        if ((m_changed & CHANGED_NOTIFY) == CHANGED_NOTIFY) {
            values.append(",?");
            names.append(",notify");
        }
        if ((m_changed & CHANGED_QUALIFIER) == CHANGED_QUALIFIER) {
            values.append(",?");
            names.append(",qualifier");
        }
        names.append(") VALUES (").append(values).append(')');
        if (log().isDebugEnabled()) log().debug("DbIfServiceEntry.insert: SQL insert statment = " + names.toString());
        PreparedStatement stmt = null;
        PreparedStatement delStmt = null;
        final DBUtils d = new DBUtils(getClass());
        try {
            stmt = c.prepareStatement(names.toString());
            d.watch(stmt);
            names = null;
            int ndx = 1;
            stmt.setInt(ndx++, m_nodeId);
            stmt.setString(ndx++, m_ipAddr.getHostAddress());
            stmt.setInt(ndx++, m_serviceId);
            if ((m_changed & CHANGED_IFINDEX) == CHANGED_IFINDEX) stmt.setInt(ndx++, m_ifIndex);
            if ((m_changed & CHANGED_STATUS) == CHANGED_STATUS) stmt.setString(ndx++, new String(new char[] { m_status }));
            if ((m_changed & CHANGED_LASTGOOD) == CHANGED_LASTGOOD) {
                stmt.setTimestamp(ndx++, m_lastGood);
            }
            if ((m_changed & CHANGED_LASTFAIL) == CHANGED_LASTFAIL) {
                stmt.setTimestamp(ndx++, m_lastFail);
            }
            if ((m_changed & CHANGED_SOURCE) == CHANGED_SOURCE) stmt.setString(ndx++, new String(new char[] { m_source }));
            if ((m_changed & CHANGED_NOTIFY) == CHANGED_NOTIFY) stmt.setString(ndx++, new String(new char[] { m_notify }));
            if ((m_changed & CHANGED_QUALIFIER) == CHANGED_QUALIFIER) stmt.setString(ndx++, m_qualifier);
            int rc;
            try {
                rc = stmt.executeUpdate();
            } catch (SQLException e) {
                log().warn("ifServices DB insert got exception; will retry after " + "deletion of any existing records for this ifService " + "that are marked for deletion.", e);
                c.rollback();
                String delCmd = "DELETE FROM ifServices WHERE status = 'D' " + "AND nodeid = ? AND ipAddr = ? AND serviceID = ?";
                delStmt = c.prepareStatement(delCmd);
                d.watch(delStmt);
                delStmt.setInt(1, m_nodeId);
                delStmt.setString(2, m_ipAddr.getHostAddress());
                delStmt.setInt(3, m_serviceId);
                rc = delStmt.executeUpdate();
                rc = stmt.executeUpdate();
            }
            log().debug("insert(): SQL update result = " + rc);
        } finally {
            d.cleanUp();
        }
        m_fromDb = true;
        m_changed = 0;
    }

    /**
     * Updates an existing record in the OpenNMS ifServices table.
     * 
     * @param c
     *            The connection used for the update.
     * 
     * @throws java.sql.SQLException
     *             Thrown if an error occurs with the connection
     */
    private void update(Connection c) throws SQLException {
        if (!m_fromDb) throw new IllegalStateException("The record does not exists in the database");
        StringBuffer sqlText = new StringBuffer("UPDATE ifServices SET ");
        char comma = ' ';
        if ((m_changed & CHANGED_IFINDEX) == CHANGED_IFINDEX) {
            sqlText.append(comma).append("ifIndex = ?");
            comma = ',';
        }
        if ((m_changed & CHANGED_STATUS) == CHANGED_STATUS) {
            sqlText.append(comma).append("status = ?");
            comma = ',';
        }
        if ((m_changed & CHANGED_LASTGOOD) == CHANGED_LASTGOOD) {
            sqlText.append(comma).append("lastGood = ?");
            comma = ',';
        }
        if ((m_changed & CHANGED_LASTFAIL) == CHANGED_LASTFAIL) {
            sqlText.append(comma).append("lastFail = ?");
            comma = ',';
        }
        if ((m_changed & CHANGED_SOURCE) == CHANGED_SOURCE) {
            sqlText.append(comma).append("source = ?");
            comma = ',';
        }
        if ((m_changed & CHANGED_NOTIFY) == CHANGED_NOTIFY) {
            sqlText.append(comma).append("notify = ?");
            comma = ',';
        }
        if ((m_changed & CHANGED_QUALIFIER) == CHANGED_QUALIFIER) {
            sqlText.append(comma).append("qualifier = ?");
            comma = ',';
        }
        sqlText.append(" WHERE nodeID = ? AND ipAddr = ? AND serviceID = ? and status <> 'D'");
        log().debug("DbIfServiceEntry.update: SQL update statment = " + sqlText.toString());
        PreparedStatement stmt = null;
        final DBUtils d = new DBUtils(getClass());
        try {
            stmt = c.prepareStatement(sqlText.toString());
            d.watch(stmt);
            sqlText = null;
            int ndx = 1;
            if ((m_changed & CHANGED_IFINDEX) == CHANGED_IFINDEX) {
                if (m_ifIndex == -1) stmt.setNull(ndx++, Types.INTEGER); else stmt.setInt(ndx++, m_ifIndex);
            }
            if ((m_changed & CHANGED_STATUS) == CHANGED_STATUS) {
                if (m_status != STATUS_UNKNOWN) stmt.setString(ndx++, new String(new char[] { m_status })); else stmt.setNull(ndx++, Types.CHAR);
            }
            if ((m_changed & CHANGED_LASTGOOD) == CHANGED_LASTGOOD) {
                if (m_lastGood != null) {
                    stmt.setTimestamp(ndx++, m_lastGood);
                } else stmt.setNull(ndx++, Types.TIMESTAMP);
            }
            if ((m_changed & CHANGED_LASTFAIL) == CHANGED_LASTFAIL) {
                if (m_lastFail != null) {
                    stmt.setTimestamp(ndx++, m_lastFail);
                } else stmt.setNull(ndx++, Types.TIMESTAMP);
            }
            if ((m_changed & CHANGED_SOURCE) == CHANGED_SOURCE) {
                if (m_source == SOURCE_UNKNOWN) stmt.setNull(ndx++, Types.CHAR); else stmt.setString(ndx++, new String(new char[] { m_source }));
            }
            if ((m_changed & CHANGED_NOTIFY) == CHANGED_NOTIFY) {
                if (m_notify == NOTIFY_UNKNOWN) stmt.setNull(ndx++, Types.CHAR); else stmt.setString(ndx++, new String(new char[] { m_notify }));
            }
            stmt.setInt(ndx++, m_nodeId);
            stmt.setString(ndx++, m_ipAddr.getHostAddress());
            stmt.setInt(ndx++, m_serviceId);
            int rc = stmt.executeUpdate();
            log().debug("DbIfServiceEntry.update: update result = " + rc);
        } finally {
            d.cleanUp();
        }
        m_changed = 0;
    }

    /**
     * Load the current interface from the database. If the interface was
     * modified, the modifications are lost. The nodeid and ip address must be
     * set prior to this call.
     * 
     * @param c
     *            The connection used to load the data.
     * 
     * @throws java.sql.SQLException
     *             Thrown if an error occurs with the connection
     */
    private boolean load(Connection c) throws SQLException {
        if (!m_fromDb) throw new IllegalStateException("The record does not exists in the database");
        PreparedStatement stmt = null;
        ResultSet rset = null;
        final DBUtils d = new DBUtils(getClass());
        try {
            stmt = c.prepareStatement(SQL_LOAD_REC);
            d.watch(stmt);
            stmt.setInt(1, m_nodeId);
            stmt.setString(2, m_ipAddr.getHostAddress());
            stmt.setInt(3, m_serviceId);
            rset = stmt.executeQuery();
            d.watch(rset);
            if (!rset.next()) {
                return false;
            }
            int ndx = 1;
            m_ifIndex = rset.getInt(ndx++);
            if (rset.wasNull()) m_ifIndex = -1;
            m_lastGood = rset.getTimestamp(ndx++);
            m_lastFail = rset.getTimestamp(ndx++);
            m_qualifier = rset.getString(ndx++);
            if (rset.wasNull()) m_qualifier = null;
            String str = rset.getString(ndx++);
            if (str != null && !rset.wasNull()) m_status = str.charAt(0); else m_status = STATUS_UNKNOWN;
            str = rset.getString(ndx++);
            if (str != null && !rset.wasNull()) m_source = str.charAt(0); else m_source = SOURCE_UNKNOWN;
            str = rset.getString(ndx++);
            if (str != null && !rset.wasNull()) m_notify = str.charAt(0); else m_notify = NOTIFY_UNKNOWN;
        } finally {
            d.cleanUp();
        }
        m_changed = 0;
        return true;
    }

    /**
     * Default constructor.
     * 
     */
    private DbIfServiceEntry() {
        throw new UnsupportedOperationException("Default constructor not supported!");
    }

    /**
     * Constructs a new interface.
     * 
     * @param nid
     *            The node identifier.
     * @param address
     *            The target interface address.
     * @param sid
     *            The service identifier
     * @param exists
     *            True if the interface already exists.
     * 
     */
    private DbIfServiceEntry(int nid, InetAddress address, int sid, boolean exists) {
        m_fromDb = exists;
        m_nodeId = nid;
        m_ipAddr = address;
        m_serviceId = sid;
        m_ifIndex = -1;
        m_status = STATUS_UNKNOWN;
        m_lastGood = null;
        m_lastFail = null;
        m_source = SOURCE_UNKNOWN;
        m_notify = NOTIFY_UNKNOWN;
        m_qualifier = null;
        m_changed = 0;
    }

    /**
     * Returns the node entry's unique identifier. This is a non-mutable
     * element. If the record does not yet exist in the database then a -1 is
     * returned.
     * 
     */
    int getNodeId() {
        return m_nodeId;
    }

    /**
     * Returns the name of the distributed poller for the entry. This is a
     * non-mutable element of the record.
     * 
     */
    InetAddress getIfAddress() {
        return m_ipAddr;
    }

    /**
     * Returns the service id of this service entry.
     */
    int getServiceId() {
        return m_serviceId;
    }

    /**
     * Gets the last good poll time of the record
     */
    String getLastGoodString() {
        String result = null;
        if (m_lastGood != null) {
            result = m_lastGood.toString();
        }
        return result;
    }

    /**
     * Gets the last good poll time of the record
     */
    Timestamp getLastGood() {
        return m_lastGood;
    }

    /**
     * Sets the current last good poll time
     * 
     * @param time
     *            The poll time.
     * 
     */
    void setLastGood(String time) throws ParseException {
        if (time == null) {
            m_lastGood = null;
        } else {
            Date tmpDate = EventConstants.parseToDate(time);
            m_lastGood = new Timestamp(tmpDate.getTime());
        }
        m_changed |= CHANGED_LASTGOOD;
    }

    /**
     * Sets the current last good poll time.
     * 
     * @param time
     *            The poll time.
     * 
     */
    void setLastGood(Date time) {
        m_lastGood = new Timestamp(time.getTime());
        m_changed |= CHANGED_LASTGOOD;
    }

    /**
     * Sets the current last good poll time.
     * 
     * @param time
     *            The poll time.
     * 
     */
    void setLastGood(Timestamp time) {
        m_lastGood = time;
        m_changed |= CHANGED_LASTGOOD;
    }

    /**
     * Gets the last fail poll time of the record
     */
    String getLastFailString() {
        String result = null;
        if (m_lastFail != null) {
            result = m_lastFail.toString();
        }
        return result;
    }

    /**
     * Gets the last fail poll time of the record
     */
    Timestamp getLastFail() {
        return m_lastFail;
    }

    /**
     * Sets the current last fail poll time
     * 
     * @param time
     *            The poll time.
     * 
     */
    void setLastFail(String time) throws ParseException {
        if (time == null) {
            m_lastFail = null;
        } else {
            Date tmpDate = EventConstants.parseToDate(time);
            m_lastFail = new Timestamp(tmpDate.getTime());
        }
        m_changed |= CHANGED_LASTFAIL;
    }

    /**
     * Sets the current last fail poll time.
     * 
     * @param time
     *            The poll time.
     * 
     */
    void setLastFail(Date time) {
        m_lastFail = new Timestamp(time.getTime());
        m_changed |= CHANGED_LASTFAIL;
    }

    /**
     * Sets the current last fail poll time.
     * 
     * @param time
     *            The poll time.
     * 
     */
    void setLastFail(Timestamp time) {
        m_lastFail = time;
        m_changed |= CHANGED_LASTFAIL;
    }

    /**
     * Returns true if the ifIndex is defined.
     */
    boolean hasIfIndex() {
        return m_ifIndex != -1;
    }

    /**
     * Returns the current ifIndex
     */
    int getIfIndex() {
        return m_ifIndex;
    }

    /**
     * Sets the ifIndex value
     * 
     * @param ndx
     *            The new ifIndex.
     */
    void setIfIndex(int ndx) {
        m_ifIndex = ndx;
        m_changed |= CHANGED_IFINDEX;
    }

    boolean hasIfIndexChanged() {
        if ((m_changed & CHANGED_IFINDEX) == CHANGED_IFINDEX) return true; else return false;
    }

    boolean updateIfIndex(int newIfIndex) {
        if (newIfIndex != m_ifIndex) {
            setIfIndex(newIfIndex);
            return true;
        }
        return false;
    }

    /**
     * Gets the current operational status field
     */
    char getStatus() {
        return m_status;
    }

    /**
     * Sets the current status of the service
     * 
     * @param status
     *            The new status.
     * 
     */
    void setStatus(char status) {
        m_status = status;
        m_changed |= CHANGED_STATUS;
    }

    boolean hasStatusChanged() {
        if ((m_changed & CHANGED_STATUS) == CHANGED_STATUS) return true; else return false;
    }

    boolean updateStatus(char newStatus) {
        if (newStatus != m_status) {
            setStatus(newStatus);
            return true;
        }
        return false;
    }

    /**
     * Gets the source of the interface service.
     */
    char getSource() {
        return m_source;
    }

    /**
     * Sets the source of the interface service
     */
    void setSource(char src) {
        m_source = src;
        m_changed |= CHANGED_SOURCE;
    }

    boolean hasSourceChanged() {
        if ((m_changed & CHANGED_SOURCE) == CHANGED_SOURCE) return true; else return false;
    }

    boolean updateSource(char newSource) {
        if (newSource != m_source) {
            setSource(newSource);
            return true;
        }
        return false;
    }

    /**
     * Gets the notification state.
     */
    char getNotify() {
        return m_notify;
    }

    /**
     * Sets the notification state
     */
    void setNotify(char notify) {
        m_notify = notify;
        m_changed |= CHANGED_NOTIFY;
    }

    boolean hasNotifyChanged() {
        if ((m_changed & CHANGED_NOTIFY) == CHANGED_NOTIFY) return true; else return false;
    }

    boolean updateNotify(char newNotify) {
        if (newNotify != m_notify) {
            setNotify(newNotify);
            return true;
        }
        return false;
    }

    String getQualifier() {
        return m_qualifier;
    }

    void setQualifier(String qualifier) {
        m_qualifier = qualifier;
        m_changed |= CHANGED_QUALIFIER;
    }

    boolean hasQualifierChanged() {
        if ((m_changed & CHANGED_QUALIFIER) == CHANGED_QUALIFIER) return true; else return false;
    }

    boolean updateQualifier(String newQualifier) {
        boolean doUpdate = false;
        if (newQualifier != null && m_qualifier != null) {
            if (!newQualifier.equals(m_qualifier)) doUpdate = true;
        } else if (newQualifier == null && m_qualifier == null) {
        } else doUpdate = true;
        if (doUpdate) {
            setQualifier(newQualifier);
            return true;
        } else return false;
    }

    /**
     * Updates the interface information in the configured database. If the
     * interfaca does not exist the a new row in the table is created. If the
     * element already exists then it's current row is updated as needed based
     * upon the current changes to the node.
     */
    void store() throws SQLException {
        if (m_changed != 0 || m_fromDb == false) {
            Connection db = null;
            try {
                db = DataSourceFactory.getInstance().getConnection();
                store(db);
                if (db.getAutoCommit() == false) db.commit();
            } finally {
                try {
                    if (db != null) db.close();
                } catch (SQLException e) {
                    log().warn("Exception closing JDBC connection", e);
                }
            }
        }
        return;
    }

    /**
     * Updates the interface information in the configured database. If the
     * interface does not exist the a new row in the table is created. If the
     * element already exists then it's current row is updated as needed based
     * upon the current changes to the node.
     * 
     * @param db
     *            The database connection used to write the record.
     */
    void store(Connection db) throws SQLException {
        if (m_changed != 0 || m_fromDb == false) {
            if (m_fromDb) {
                update(db);
            } else {
                insert(db);
            }
        }
    }

    /**
     * Creates a new entry. The entry is created in memory, but is not written
     * to the database until the first call to <code>store</code>.
     * 
     * @param address
     *            The address of the interface.
     * @param nid
     *            The node id of the interface.
     * @param sid
     *            The service id for the interface.
     * 
     * @return A new interface record.
     */
    static DbIfServiceEntry create(int nid, InetAddress address, int sid) {
        return new DbIfServiceEntry(nid, address, sid, false);
    }

    /**
     * Retrieves a current record from the database based upon the key fields of
     * <em>nodeID</em> and <em>ipAddr</em>. If the record cannot be found
     * then a null reference is returned.
     * 
     * @param nid
     *            The node id key
     * @param addr
     *            The ip address.
     * @param sid
     *            The service id.
     * 
     * @return The loaded entry or null if one could not be found.
     * 
     */
    static DbIfServiceEntry get(int nid, InetAddress addr, int sid) throws SQLException {
        Connection db = null;
        try {
            db = DataSourceFactory.getInstance().getConnection();
            return get(db, nid, addr, sid);
        } finally {
            try {
                if (db != null) {
                    db.close();
                }
            } catch (SQLException e) {
                ThreadCategory.getInstance(DbIfServiceEntry.class).warn("Exception closing JDBC connection", e);
            }
        }
    }

    /**
     * Retrieves a current record from the database based upon the key fields of
     * <em>nodeID</em> and <em>ipAddr</em>. If the record cannot be found
     * then a null reference is returned.
     * 
     * @param db
     *            The database connection used to load the entry.
     * @param nid
     *            The node id key
     * @param addr
     *            The IP address.
     * 
     * @return The loaded entry or null if one could not be found.
     * 
     */
    static DbIfServiceEntry get(Connection db, int nid, InetAddress addr, int sid) throws SQLException {
        DbIfServiceEntry entry = new DbIfServiceEntry(nid, addr, sid, true);
        if (!entry.load(db)) {
            entry = null;
        }
        return entry;
    }

    /**
     * Creates a string that displays the internal contents of the record. This
     * is mainly just used for debug output since the format is ad-hoc.
     * 
     */
    public String toString() {
        String sep = System.getProperty("line.separator");
        StringBuffer buf = new StringBuffer();
        buf.append("from db = ").append(m_fromDb).append(sep);
        buf.append("node id = ").append(m_nodeId).append(sep);
        buf.append("address = ").append(m_ipAddr).append(sep);
        buf.append("service id = ").append(m_serviceId).append(sep);
        buf.append("good time = ").append(m_lastGood).append(sep);
        buf.append("fail time = ").append(m_lastFail).append(sep);
        buf.append("status = ").append(m_status).append(sep);
        buf.append("ifIndex = ").append(m_ifIndex).append(sep);
        buf.append("source = ").append(m_source).append(sep);
        buf.append("notify = ").append(m_notify).append(sep);
        buf.append("qualifier = ").append(m_qualifier).append(sep);
        return buf.toString();
    }

    /**
     * For debugging only
     */
    public static void main(String[] args) {
        try {
            DbIfServiceEntry entry = DbIfServiceEntry.get(Integer.parseInt(args[0]), InetAddress.getByName(args[1]), Integer.parseInt(args[2]));
            System.out.println(entry.toString());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private Category log() {
        return ThreadCategory.getInstance(getClass());
    }
}
