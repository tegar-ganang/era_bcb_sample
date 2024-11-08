package org.perfmon4j.extras.tomcat7;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.perfmon4j.SnapShotData;
import org.perfmon4j.SnapShotSQLWriter;
import org.perfmon4j.instrument.SnapShotGauge;
import org.perfmon4j.instrument.SnapShotInstanceDefinition;
import org.perfmon4j.instrument.SnapShotProvider;
import org.perfmon4j.instrument.SnapShotString;
import org.perfmon4j.util.JDBCHelper;
import org.perfmon4j.util.Logger;
import org.perfmon4j.util.LoggerFactory;
import org.perfmon4j.util.MiscHelper;

@SnapShotProvider(type = SnapShotProvider.Type.INSTANCE_PER_MONITOR, dataInterface = ThreadPoolMonitor.class, sqlWriter = ThreadPoolMonitorImpl.SQLWriter.class)
public class ThreadPoolMonitorImpl extends JMXMonitorBase {

    private static final Logger logger = LoggerFactory.initLogger(ThreadPoolMonitorImpl.class);

    private static String buildBaseObjectName() {
        String result = "Catalina:type=ThreadPool";
        if (MiscHelper.isRunningInJBossAppServer()) {
            result = "jboss.web:type=ThreadPool";
        }
        return result;
    }

    public ThreadPoolMonitorImpl() {
        super(buildBaseObjectName(), "name", null);
    }

    public ThreadPoolMonitorImpl(String instanceName) {
        super(buildBaseObjectName(), "name", instanceName);
    }

    @SnapShotInstanceDefinition
    public static String[] getInstanceNames() throws MalformedObjectNameException, NullPointerException {
        MBeanServer mBeanServer = MiscHelper.findMBeanServer(MiscHelper.isRunningInJBossAppServer() ? "jboss" : null);
        return MiscHelper.getAllObjectName(mBeanServer, new ObjectName(buildBaseObjectName()), "name");
    }

    @SnapShotString(isInstanceName = true)
    public String getInstanceName() {
        return MiscHelper.getInstanceNames(getMBeanServer(), getQueryObjectName(), "name");
    }

    @SnapShotGauge
    public long getCurrentThreadsBusy() {
        return MiscHelper.sumMBeanAttributes(getMBeanServer(), getQueryObjectName(), "currentThreadsBusy");
    }

    @SnapShotGauge
    public long getCurrentThreadCount() {
        return MiscHelper.sumMBeanAttributes(getMBeanServer(), getQueryObjectName(), "currentThreadCount");
    }

    public static class SQLWriter implements SnapShotSQLWriter {

        public void writeToSQL(Connection conn, String schema, SnapShotData data) throws SQLException {
            writeToSQL(conn, schema, (ThreadPoolMonitor) data);
        }

        public void writeToSQL(Connection conn, String schema, ThreadPoolMonitor data) throws SQLException {
            schema = (schema == null) ? "" : (schema + ".");
            final String SQL = "INSERT INTO " + schema + "P4JThreadPoolMonitor " + "(ThreadPoolOwner, InstanceName, StartTime, EndTime, Duration,  " + "CurrentThreadsBusy, CurrentThreadCount) " + "VALUES(?, ?, ?, ?, ?, ?, ?)";
            PreparedStatement stmt = null;
            try {
                stmt = conn.prepareStatement(SQL);
                stmt.setString(1, "Apache/Tomcat");
                stmt.setString(2, data.getInstanceName());
                stmt.setTimestamp(3, new Timestamp(data.getStartTime()));
                stmt.setTimestamp(4, new Timestamp(data.getEndTime()));
                stmt.setLong(5, data.getDuration());
                stmt.setLong(6, data.getCurrentThreadsBusy());
                stmt.setLong(7, data.getCurrentThreadCount());
                int count = stmt.executeUpdate();
                if (count != 1) {
                    throw new SQLException("ThreadPoolMonitor failed to insert row");
                }
            } finally {
                JDBCHelper.closeNoThrow(stmt);
            }
        }
    }
}
