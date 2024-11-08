package org.opennms.netmgt.outage;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Category;
import org.exolab.castor.xml.MarshalException;
import org.exolab.castor.xml.ValidationException;
import org.opennms.core.concurrent.RunnableConsumerThreadPool;
import org.opennms.core.fiber.PausableFiber;
import org.opennms.core.utils.ThreadCategory;
import org.opennms.netmgt.config.DatabaseConnectionFactory;
import org.opennms.netmgt.config.OutageManagerConfigFactory;

/**
 * The OutageManager receives events selectively and maintains a historical
 * archive of each outage for all devices in the database
 *
 * @author 	<A HREF="mailto:sowmya@opennms.org">Sowmya Nataraj</A>
 * @author 	<A HREF="mailto:mike@opennms.org">Mike Davidson</A>
 * @author	<A HREF="http://www.opennms.org">OpenNMS.org</A>
 */
public final class OutageManager implements PausableFiber {

    /** 
	 * The log4j category used to log debug messsages
	 * and statements.
	 */
    private static final String LOG4J_CATEGORY = "OpenNMS.Outage";

    /**
	 * The singleton instance of this class
	 */
    private static final OutageManager m_singleton = new OutageManager();

    /**
	 * The number of threads that must be started.
	 */
    private static final int NUM_THREADS = 2;

    /**
	 * The service table map
	 */
    private Map m_serviceTableMap;

    /**
	 * The events receiver
	 */
    private BroadcastEventProcessor m_eventReceiver;

    /**
	 * The RunnableConsumerThreadPool that runs writers
	 * that update the database
	 */
    private RunnableConsumerThreadPool m_writerPool;

    /**
	 * Current status of this fiber
	 */
    private int m_status;

    /**
	 * Build the servicename to serviceid map - this map is used so as to
	 * avoid a database lookup for each incoming event
	 */
    private void buildServiceTableMap(java.sql.Connection dbConn) throws SQLException {
        m_serviceTableMap = Collections.synchronizedMap(new HashMap());
        PreparedStatement stmt = dbConn.prepareStatement(OutageConstants.SQL_DB_SVC_TABLE_READ);
        ResultSet rset = stmt.executeQuery();
        while (rset.next()) {
            long svcid = rset.getLong(1);
            String svcname = rset.getString(2);
            m_serviceTableMap.put(svcname, new Long(svcid));
        }
        rset.close();
        stmt.close();
    }

    /**
	 * Close the currently open outages for services and interfaces that are
	 * currently unmanaged
	 */
    private void closeOutages(java.sql.Connection dbConn) throws SQLException {
        Category log = ThreadCategory.getInstance(getClass());
        if (log.isDebugEnabled()) log.debug("OutageManager getting ready to close currently open outages for unmanaged services and interfaces ..");
        Timestamp closeTime = new Timestamp((new java.util.Date()).getTime());
        PreparedStatement closeForServicesStmt = dbConn.prepareStatement(OutageConstants.DB_CLOSE_OUTAGES_FOR_UNMANAGED_SERVICES);
        PreparedStatement closeForInterfacesStmt = dbConn.prepareStatement(OutageConstants.DB_CLOSE_OUTAGES_FOR_UNMANAGED_INTERFACES);
        closeForServicesStmt.setTimestamp(1, closeTime);
        closeForInterfacesStmt.setTimestamp(1, closeTime);
        int num = closeForServicesStmt.executeUpdate();
        if (log.isDebugEnabled()) log.debug("OutageManager closed " + num + " outages for unmanaged services");
        num = closeForInterfacesStmt.executeUpdate();
        if (log.isDebugEnabled()) log.debug("OutageManager closed " + num + " outages for unmanaged interfaces");
        closeForInterfacesStmt.close();
        closeForServicesStmt.close();
        if (log.isDebugEnabled()) log.debug("OutageManager closed currently open outages for unmanaged services and interfaces");
    }

    /**
	 * The constructor for the OutageManager 
	 * 
	 */
    public OutageManager() {
        m_status = START_PENDING;
    }

    /**
	 * Returns a name/id for this process
	 */
    public String getName() {
        return "OpenNMS.OutageManager";
    }

    /**
	 * Returns the current status
	 */
    public int getStatus() {
        return m_status;
    }

    public void init() {
        ThreadCategory.setPrefix(LOG4J_CATEGORY);
        Category log = ThreadCategory.getInstance(getClass());
        int numWriters = 1;
        try {
            OutageManagerConfigFactory.reload();
            OutageManagerConfigFactory oFactory = OutageManagerConfigFactory.getInstance();
            numWriters = oFactory.getWriters();
        } catch (MarshalException ex) {
            log.error("Failed to load outage configuration", ex);
            throw new UndeclaredThrowableException(ex);
        } catch (ValidationException ex) {
            log.error("Failed to load outage configuration", ex);
            throw new UndeclaredThrowableException(ex);
        } catch (IOException ex) {
            log.error("Failed to load outage configuration", ex);
            throw new UndeclaredThrowableException(ex);
        }
        java.sql.Connection conn = null;
        try {
            DatabaseConnectionFactory.init();
            conn = DatabaseConnectionFactory.getInstance().getConnection();
            closeOutages(conn);
            buildServiceTableMap(conn);
        } catch (IOException ie) {
            log.fatal("IOException getting database connection", ie);
            throw new UndeclaredThrowableException(ie);
        } catch (MarshalException me) {
            log.fatal("Marshall Exception getting database connection", me);
            throw new UndeclaredThrowableException(me);
        } catch (ValidationException ve) {
            log.fatal("Validation Exception getting database connection", ve);
            throw new UndeclaredThrowableException(ve);
        } catch (SQLException sqlE) {
            log.fatal("Error closing outages for unmanaged services and interfaces or building servicename to serviceid mapping", sqlE);
            throw new UndeclaredThrowableException(sqlE);
        } catch (ClassNotFoundException cnfE) {
            log.fatal("Failed to load database driver", cnfE);
            throw new UndeclaredThrowableException(cnfE);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {
                }
            }
        }
        m_writerPool = new RunnableConsumerThreadPool("Outage Writer Pool", 0.6f, 1.0f, numWriters);
        if (log.isDebugEnabled()) log.debug("Created writer pool");
        m_eventReceiver = new BroadcastEventProcessor(m_writerPool.getRunQueue());
        if (log.isDebugEnabled()) log.debug("Created event receiver");
        log.info("OutageManager ready to accept events");
    }

    /** 
	 * Read the configuration xml, create and start all the subthreads.
	 */
    public void start() {
        ThreadCategory.setPrefix(LOG4J_CATEGORY);
        Category log = ThreadCategory.getInstance(getClass());
        m_status = STARTING;
        if (log.isDebugEnabled()) {
            log.debug("Starting writer pool");
        }
        m_writerPool.start();
        try {
            m_eventReceiver.start();
        } catch (Throwable t) {
            m_writerPool.stop();
            if (log.isDebugEnabled()) log.debug("Writer pool shutdown");
            throw new UndeclaredThrowableException(t);
        }
        m_status = RUNNING;
        if (log.isDebugEnabled()) {
            log.debug("Outage Writer threads started");
        }
        log.info("OutageManager ready to accept events");
    }

    /**
	 * Pauses all the threads
	 */
    public void pause() {
        if (m_status != RUNNING) return;
        m_status = PAUSE_PENDING;
        Category log = ThreadCategory.getInstance(getClass());
        m_status = PAUSED;
        if (log.isDebugEnabled()) log.debug("Finished pausing all threads");
    }

    /**
	 * Resumes all the threads
	 */
    public void resume() {
        if (m_status != PAUSED) return;
        m_status = RESUME_PENDING;
        Category log = ThreadCategory.getInstance(getClass());
        m_status = RUNNING;
        if (log.isDebugEnabled()) log.debug("Finished resuming ");
    }

    /**
	 * Stops all the threads
	 */
    public void stop() {
        m_status = STOP_PENDING;
        Category log = ThreadCategory.getInstance(getClass());
        try {
            if (log.isDebugEnabled()) log.debug("Beginning shutdown process");
            m_eventReceiver.close();
            if (log.isDebugEnabled()) log.debug("sending shutdown to writers");
            m_writerPool.stop();
            if (log.isDebugEnabled()) log.debug("Outage Writers shutdown");
            m_status = STOPPED;
            log.info("OutageManager shutdown complete");
        } catch (Exception e) {
            log.error(e.getLocalizedMessage(), e);
        }
    }

    /**
	 * Return the service id for the name passed
	 *
	 * @param svcname	the service name whose service id is required
	 *
	 * @return the service id for the name passed, -1 if not found
	 */
    public synchronized long getServiceID(String svcname) {
        Long val = (Long) m_serviceTableMap.get(svcname);
        if (val != null) {
            return val.longValue();
        } else {
            return -1;
        }
    }

    /**
	 * Add the svcname/svcid mapping to the servicetable map
	 */
    public synchronized void addServiceMapping(String svcname, long serviceid) {
        m_serviceTableMap.put(svcname, new Long(serviceid));
    }

    /**
	 * Return a handle to the OutageManager
	 */
    public static OutageManager getInstance() {
        return m_singleton;
    }
}
