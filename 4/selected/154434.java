package gov.sns.tools.pvlogger;

import gov.sns.tools.database.*;
import java.util.*;
import java.math.BigDecimal;
import java.sql.*;

/**
 * SqlStateStore is an implementation of StateStore that provides persistent storage of machine
 * state in a SQL database.
 *
 * @author   tap
 */
public class SqlStateStore implements StateStore {

    protected final int commitFrequency = 100;

    private static final boolean autoCommit = false;

    /** Description of the Field */
    protected static final String PV_TABLE = "epics.sgnl_rec";

    /** Description of the Field */
    protected static final String MACHINE_SNAPSHOT_TABLE = "epics.mach_snapshot";

    /** Description of the Field */
    protected static final String PV_SNAPSHOT_TABLE = "epics.mach_snapshot_sgnl";

    /** Description of the Field */
    protected static final String SNAPSHOT_TYPE_TABLE = "epics.mach_snapshot_type";

    /** Description of the Field */
    protected static final String SNAPSHOT_TYPE_PV_TABLE = "epics.mach_snapshot_type_sgnl";

    /** Description of the Field */
    protected static final String SGNL_VALUE_ARRAY_TYPE = "EPICS.SGNL_VAL_TYP";

    /** Description of the Field */
    protected static final String PV_PKEY = "sgnl_id";

    /** Description of the Field */
    protected static final String PV_SNAPSHOT_MACHINE_SNAPSHOT_COL = "snapshot_id";

    /** Description of the Field */
    protected static final String PV_SNAPSHOT_PV_COL = "sgnl_id";

    /** Description of the Field */
    protected static final String PV_SNAPSHOT_TIMESTAMP_COL = "sgnl_timestp";

    /** Description of the Field */
    protected static final String PV_SNAPSHOT_VALUE_COL = "sgnl_val";

    /** Description of the Field */
    protected static final String PV_SNAPSHOT_STATUS_COL = "sgnl_stat";

    /** Description of the Field */
    protected static final String PV_SNAPSHOT_SEVERITY_COL = "sgnl_svrty";

    /** Description of the Field */
    protected static final String MACHINE_SNAPSHOT_PKEY = "snapshot_id";

    /** Description of the Field */
    protected static final String MACHINE_SNAPSHOT_TIME_COL = "snapshot_dte";

    /** Description of the Field */
    protected static final String MACHINE_SNAPSHOT_TYPE_COL = "snapshot_type_nm";

    /** Description of the Field */
    protected static final String MACHINE_SNAPSHOT_COMMENT_COL = "cmnt";

    /** Description of the Field */
    protected static final String SNAPSHOT_TYPE_PV_NAME_PKEY = "snapshot_type_nm";

    /** Description of the Field */
    protected static final String SNAPSHOT_TYPE_PV_PV_PKEY = "sgnl_id";

    /** Description of the Field */
    protected static final String SNAPSHOT_TYPE_PKEY = "snapshot_type_nm";

    /** Description of the Field */
    protected static final String SNAPSHOT_TYPE_DESC_COL = "snapshot_type_desc";

    /** Description of the Field */
    protected static final String SNAPSHOT_TYPE_PERIOD_COL = "snapshot_per";

    /** Description of the Field */
    protected static final String MACHINE_SNAPSHOT_SEQ = "epics.mach_snapshot_id_seq";

    /** Description of the Field */
    protected DatabaseAdaptor _databaseAdaptor;

    /** Description of the Field */
    protected PreparedStatement MACHINE_SNAPSHOT_NEXT_PKEY;

    /** Description of the Field */
    protected PreparedStatement MACHINE_SNAPSHOT_INSERT;

    /** Description of the Field */
    protected PreparedStatement CHANNEL_SNAPSHOT_INSERT;

    /** Description of the Field */
    protected PreparedStatement PV_QUERY;

    /** Description of the Field */
    protected PreparedStatement MACHINE_SNAPSHOT_QUERY;

    /** Description of the Field */
    protected PreparedStatement MACHINE_SNAPSHOT_QUERY_BY_PKEY;

    /** Description of the Field */
    protected PreparedStatement MACHINE_SNAPSHOT_QUERY_BY_TYPE_TIMERANGE;

    /** Description of the Field */
    protected PreparedStatement MACHINE_SNAPSHOT_QUERY_BY_TIMERANGE;

    /** Description of the Field */
    protected PreparedStatement CHANNEL_SNAPSHOT_QUERY_BY_MACHINE_SNAPSHOT;

    /** Description of the Field */
    protected PreparedStatement SNAPSHOT_TYPE_QUERY;

    /** Description of the Field */
    protected PreparedStatement SNAPSHOT_TYPE_QUERY_BY_NAME;

    /** Description of the Field */
    protected PreparedStatement SNAPSHOT_TYPE_PV_QUERY_BY_NAME;

    /** Description of the Field */
    protected Connection _connection;

    /**
	 * Primary constructor
	 *
	 * @param adaptor     The database adaptor
	 * @param connection  A database connection
	 */
    public SqlStateStore(final DatabaseAdaptor adaptor, final Connection connection) {
        _databaseAdaptor = (adaptor != null) ? adaptor : DatabaseAdaptor.getInstance();
        _connection = connection;
    }

    /**
	 * Construct an SQL state store from the specified connection and use the default database
	 * adaptor.
	 *
	 * @param connection  A database connection
	 */
    public SqlStateStore(final Connection connection) {
        this(null, connection);
    }

    /**
	 * Construct a SqlStateStore and connect it to a database specified by the url and with user
	 * and password access.
	 *
	 * @param adaptor                                      The database adaptor
	 * @param urlSpec                                      The url of the database
	 * @param user                                         The user to login
	 * @param password                                     The user's password for login
	 * @exception StateStoreException                      Description of the Exception
	 * @throws gov.sns.tools.pvlogger.StateStoreException  if a SQL exception is thrown
	 */
    public SqlStateStore(DatabaseAdaptor adaptor, String urlSpec, String user, String password) throws StateStoreException {
        this(adaptor, newConnection(adaptor, urlSpec, user, password));
    }

    /**
	 * Construct a SqlStateStore and connect it to a database specified by the url and with user
	 * and password access.
	 *
	 * @param urlSpec                                      The url of the database
	 * @param user                                         The user to login
	 * @param password                                     The user's password for login
	 * @exception StateStoreException                      Description of the Exception
	 * @throws gov.sns.tools.pvlogger.StateStoreException  if a SQL exception is thrown
	 */
    public SqlStateStore(String urlSpec, String user, String password) throws StateStoreException {
        this(DatabaseAdaptor.getInstance(), urlSpec, user, password);
    }

    /**
	 * Construct a SqlStateStore and connect it to a database specified by the url and with user
	 * and password access.
	 *
	 * @param adaptor                                      The database adaptor
	 * @param dictionary                                   The dictionary containing the
	 *      connection information
	 * @exception StateStoreException                      Description of the Exception
	 * @throws gov.sns.tools.pvlogger.StateStoreException  if a SQL exception is thrown
	 */
    public SqlStateStore(DatabaseAdaptor adaptor, ConnectionDictionary dictionary) throws StateStoreException {
        this(adaptor, dictionary.getURLSpec(), dictionary.getUser(), dictionary.getPassword());
    }

    /**
	 * Construct a SqlStateStore and connect it to a database specified by the url and with user
	 * and password access.
	 *
	 * @param dictionary                                   The dictionary containing the
	 *      connection information
	 * @exception StateStoreException                      Description of the Exception
	 * @throws gov.sns.tools.pvlogger.StateStoreException  if a SQL exception is thrown
	 */
    public SqlStateStore(ConnectionDictionary dictionary) throws StateStoreException {
        this(dictionary.getDatabaseAdaptor(), dictionary.getURLSpec(), dictionary.getUser(), dictionary.getPassword());
    }

    /**
	 * Create a database connection to the persistent data storage.
	 *
	 * @param urlSpec                                      The url of the database
	 * @param user                                         The user to login
	 * @param password                                     The user's password for login
	 * @exception StateStoreException                      Description of the Exception
	 * @throws gov.sns.tools.pvlogger.StateStoreException  if a SQL exception is thrown
	 */
    protected void connect(String urlSpec, String user, String password) throws StateStoreException {
        try {
            _connection = newConnection(_databaseAdaptor, urlSpec, user, password);
        } catch (DatabaseException exception) {
            throw new StateStoreException("Error while connecting to the data source and preparing statements.", exception);
        }
    }

    /**
	 * Create a database connection to the persistent data storage.
	 *
	 * @param adaptor                                      the database adaptor
	 * @param urlSpec                                      The url of the database
	 * @param user                                         The user to login
	 * @param password                                     The user's password for login
	 * @return                                             a new database connection
	 * @exception StateStoreException                      Description of the Exception
	 * @throws gov.sns.tools.pvlogger.StateStoreException  if a SQL exception is thrown
	 */
    protected static Connection newConnection(DatabaseAdaptor adaptor, String urlSpec, String user, String password) throws StateStoreException {
        try {
            Connection c = adaptor.getConnection(urlSpec, user, password);
            try {
                c.setAutoCommit(autoCommit);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return c;
        } catch (DatabaseException exception) {
            throw new StateStoreException("Error while connecting to the data source and preparing statements.", exception);
        }
    }

    /** sako
	 *  sql command is different between oracle and postgresql for sequence
	 */
    protected PreparedStatement getMachineSnapshotNextPrimaryKeyStatement() throws SQLException {
        if (MACHINE_SNAPSHOT_NEXT_PKEY == null) {
            if (_databaseAdaptor instanceof PgsqlDatabaseAdaptor) {
                MACHINE_SNAPSHOT_NEXT_PKEY = _connection.prepareStatement("SELECT nextval('" + MACHINE_SNAPSHOT_SEQ + "')");
            } else {
                MACHINE_SNAPSHOT_NEXT_PKEY = _connection.prepareStatement("SELECT " + MACHINE_SNAPSHOT_SEQ + ".nextval FROM dual");
            }
        }
        return MACHINE_SNAPSHOT_NEXT_PKEY;
    }

    /**
	 * Create the prepared statement if it does not already exist.
	 *
	 * @return                        the prepared statement for inserting a new machine snapshot
	 * @exception SQLException        Description of the Exception
	 * @throws java.sql.SQLException  if an exception occurs during a SQL evaluation
	 */
    protected PreparedStatement getMachineSnapshotInsertStatement() throws SQLException {
        if (MACHINE_SNAPSHOT_INSERT == null) {
            MACHINE_SNAPSHOT_INSERT = _connection.prepareStatement("INSERT INTO " + MACHINE_SNAPSHOT_TABLE + "(" + MACHINE_SNAPSHOT_PKEY + ", " + MACHINE_SNAPSHOT_TIME_COL + ", " + MACHINE_SNAPSHOT_TYPE_COL + ", " + MACHINE_SNAPSHOT_COMMENT_COL + ")" + " VALUES (?, ?, ?, ?)");
        }
        return MACHINE_SNAPSHOT_INSERT;
    }

    /**
	 * Create the prepared statement if it does not already exist.
	 *
	 * @return                        the prepared statement for inserting a new channel snapshot
	 * @exception SQLException        Description of the Exception
	 * @throws java.sql.SQLException  if an exception occurs during a SQL evaluation
	 */
    protected PreparedStatement getChannelSnapshotInsertStatement() throws SQLException {
        if (CHANNEL_SNAPSHOT_INSERT == null) {
            CHANNEL_SNAPSHOT_INSERT = _connection.prepareStatement("INSERT INTO " + PV_SNAPSHOT_TABLE + "(" + PV_SNAPSHOT_MACHINE_SNAPSHOT_COL + ", " + PV_SNAPSHOT_PV_COL + ", " + PV_SNAPSHOT_TIMESTAMP_COL + ", " + PV_SNAPSHOT_VALUE_COL + ", " + PV_SNAPSHOT_STATUS_COL + ", " + PV_SNAPSHOT_SEVERITY_COL + ") VALUES (?, ?, ?, ?, ?, ?)");
        }
        return CHANNEL_SNAPSHOT_INSERT;
    }

    /**
	 * Create the prepared statement if it does not already exist.
	 *
	 * @return                        the prepared statement for querying the PV table for the PV
	 *      record
	 * @exception SQLException        Description of the Exception
	 * @throws java.sql.SQLException  if an exception occurs during a SQL evaluation
	 */
    protected PreparedStatement getPvQueryStatement() throws SQLException {
        if (PV_QUERY == null) {
            PV_QUERY = _connection.prepareStatement("SELECT * FROM " + PV_TABLE + " WHERE " + PV_PKEY + " = ?");
        }
        return PV_QUERY;
    }

    /**
	 * Create the prepared statement if it does not already exist.
	 *
	 * @return                        the prepared statement to query for all machine snapshots
	 * @exception SQLException        Description of the Exception
	 * @throws java.sql.SQLException  if an exception occurs during a SQL evaluation
	 */
    protected PreparedStatement getMachineSnapshotQueryStatement() throws SQLException {
        if (MACHINE_SNAPSHOT_QUERY == null) {
            MACHINE_SNAPSHOT_QUERY = _connection.prepareStatement("SELECT * FROM " + MACHINE_SNAPSHOT_TABLE);
        }
        return MACHINE_SNAPSHOT_QUERY;
    }

    /**
	 * Create the prepared statement if it does not already exist.
	 *
	 * @return                        the prepared statement to query for machine snapshots by
	 *      primary key
	 * @exception SQLException        Description of the Exception
	 * @throws java.sql.SQLException  if an exception occurs during a SQL evaluation
	 */
    protected PreparedStatement getMachineSnapshotQueryByPkeyStatement() throws SQLException {
        if (MACHINE_SNAPSHOT_QUERY_BY_PKEY == null) {
            MACHINE_SNAPSHOT_QUERY_BY_PKEY = _connection.prepareStatement("SELECT * FROM " + MACHINE_SNAPSHOT_TABLE + " WHERE " + MACHINE_SNAPSHOT_PKEY + " = ?");
        }
        return MACHINE_SNAPSHOT_QUERY_BY_PKEY;
    }

    /**
	 * Create the prepared statement if it does not already exist.
	 *
	 * @return                        the prepared statement to query for machine snapshots by
	 *      type and time range
	 * @exception SQLException        Description of the Exception
	 * @throws java.sql.SQLException  if an exception occurs during a SQL evaluation
	 */
    protected PreparedStatement getMachineSnapshotQueryByTypeTimerangeStatement() throws SQLException {
        if (MACHINE_SNAPSHOT_QUERY_BY_TYPE_TIMERANGE == null) {
            MACHINE_SNAPSHOT_QUERY_BY_TYPE_TIMERANGE = _connection.prepareStatement("SELECT * FROM " + MACHINE_SNAPSHOT_TABLE + " WHERE " + MACHINE_SNAPSHOT_TYPE_COL + " = ? AND " + MACHINE_SNAPSHOT_TIME_COL + " > ? AND " + MACHINE_SNAPSHOT_TIME_COL + " < ?");
        }
        return MACHINE_SNAPSHOT_QUERY_BY_TYPE_TIMERANGE;
    }

    /**
	 * Create the prepared statement if it does not already exist.
	 *
	 * @return                        the prepared statement to query for machine snapshots by
	 *      time range
	 * @exception SQLException        Description of the Exception
	 * @throws java.sql.SQLException  if an exception occurs during a SQL evaluation
	 */
    protected PreparedStatement getMachineSnapshotQueryByTimerangeStatement() throws SQLException {
        if (MACHINE_SNAPSHOT_QUERY_BY_TIMERANGE == null) {
            MACHINE_SNAPSHOT_QUERY_BY_TIMERANGE = _connection.prepareStatement("SELECT * FROM " + MACHINE_SNAPSHOT_TABLE + " WHERE " + MACHINE_SNAPSHOT_TIME_COL + " > ? AND " + MACHINE_SNAPSHOT_TIME_COL + " < ?");
        }
        return MACHINE_SNAPSHOT_QUERY_BY_TIMERANGE;
    }

    /**
	 * Create the prepared statement if it does not already exist.
	 *
	 * @return                        the prepared statement to query for channel snapshots by
	 *      machine snapshot
	 * @exception SQLException        Description of the Exception
	 * @throws java.sql.SQLException  if an exception occurs during a SQL evaluation
	 */
    protected PreparedStatement getChannelSnapshotQueryByMachineSnapshotStatement() throws SQLException {
        if (CHANNEL_SNAPSHOT_QUERY_BY_MACHINE_SNAPSHOT == null) {
            CHANNEL_SNAPSHOT_QUERY_BY_MACHINE_SNAPSHOT = _connection.prepareStatement("SELECT * FROM " + PV_SNAPSHOT_TABLE + " WHERE " + PV_SNAPSHOT_MACHINE_SNAPSHOT_COL + " = ?");
        }
        return CHANNEL_SNAPSHOT_QUERY_BY_MACHINE_SNAPSHOT;
    }

    /**
	 * Create the prepared statement if it does not already exist.
	 *
	 * @return                        the prepared statement to query for available snapshot types
	 * @exception SQLException        Description of the Exception
	 * @throws java.sql.SQLException  if an exception occurs during a SQL evaluation
	 */
    protected PreparedStatement getSnapshotTypeQueryStatement() throws SQLException {
        if (SNAPSHOT_TYPE_QUERY == null) {
            SNAPSHOT_TYPE_QUERY = _connection.prepareStatement("SELECT * FROM " + SNAPSHOT_TYPE_TABLE);
        }
        return SNAPSHOT_TYPE_QUERY;
    }

    /**
	 * Create the prepared statement if it does not already exist.
	 *
	 * @return                        the prepared statement to query for available snapshot types
	 * @exception SQLException        Description of the Exception
	 * @throws java.sql.SQLException  if an exception occurs during a SQL evaluation
	 */
    protected PreparedStatement getSnapshotTypeQueryByNameStatement() throws SQLException {
        if (SNAPSHOT_TYPE_QUERY_BY_NAME == null) {
            SNAPSHOT_TYPE_QUERY_BY_NAME = _connection.prepareStatement("SELECT * FROM " + SNAPSHOT_TYPE_TABLE + " WHERE " + SNAPSHOT_TYPE_PV_NAME_PKEY + " = ?");
        }
        return SNAPSHOT_TYPE_QUERY_BY_NAME;
    }

    /**
	 * Create the prepared statement if it does not already exist.
	 *
	 * @return                        the prepared statement to query for machine snapshot type-PV
	 *      record by type
	 * @exception SQLException        Description of the Exception
	 * @throws java.sql.SQLException  if an exception occurs during a SQL evaluation
	 */
    protected PreparedStatement getSnapshotTypePvQueryByNameStatement() throws SQLException {
        if (SNAPSHOT_TYPE_PV_QUERY_BY_NAME == null) {
            SNAPSHOT_TYPE_PV_QUERY_BY_NAME = _connection.prepareStatement("SELECT * FROM " + SNAPSHOT_TYPE_PV_TABLE + " WHERE " + SNAPSHOT_TYPE_PV_NAME_PKEY + " = ?");
        }
        return SNAPSHOT_TYPE_PV_QUERY_BY_NAME;
    }

    /**
	 * Publish the channel snapshot and associate it with the machine snapshot given by the
	 * machine snapshop id.
	 *
	 * @param snapshot                                     The channel snapshot to publish
	 * @param machineId                                    The unique id of the associated machine
	 *      snapshot
	 * @exception StateStoreException                      Description of the Exception
	 * @throws gov.sns.tools.pvlogger.StateStoreException  if a SQL exception is thrown
	 */
    public void publish(final ChannelSnapshot snapshot, final long machineId) throws StateStoreException {
        Timestamp time = snapshot.getTimestamp().getSQLTimestamp();
        try {
            getChannelSnapshotInsertStatement();
            CHANNEL_SNAPSHOT_INSERT.setLong(1, machineId);
            CHANNEL_SNAPSHOT_INSERT.setString(2, snapshot.getPV());
            CHANNEL_SNAPSHOT_INSERT.setTimestamp(3, time);
            Array valueArray = _databaseAdaptor.getArray(SGNL_VALUE_ARRAY_TYPE, _connection, snapshot.getValue());
            CHANNEL_SNAPSHOT_INSERT.setArray(4, valueArray);
            CHANNEL_SNAPSHOT_INSERT.setInt(5, snapshot.getStatus());
            CHANNEL_SNAPSHOT_INSERT.setInt(6, snapshot.getSeverity());
            CHANNEL_SNAPSHOT_INSERT.addBatch();
        } catch (SQLException exception) {
            throw new StateStoreException("Error publishing a channel snapshot.", exception);
        }
    }

    /**
	 * Publish the machine snapshot.
	 *
	 * @param machineSnapshot                              The machine snapshot to publish.
	 * @exception StateStoreException                      Description of the Exception
	 * @throws gov.sns.tools.pvlogger.StateStoreException  if a SQL exception is thrown
	 */
    public void publish(final MachineSnapshot machineSnapshot) throws StateStoreException {
        if (machineSnapshot == null) {
            System.out.println("no data to be saved. skipping");
            return;
        }
        Timestamp time = new Timestamp(machineSnapshot.getTimestamp().getTime());
        try {
            getMachineSnapshotNextPrimaryKeyStatement();
            getMachineSnapshotInsertStatement();
            getChannelSnapshotInsertStatement();
            ResultSet idResult = MACHINE_SNAPSHOT_NEXT_PKEY.executeQuery();
            commit();
            idResult.next();
            long id = idResult.getLong(1);
            MACHINE_SNAPSHOT_INSERT.setLong(1, id);
            MACHINE_SNAPSHOT_INSERT.setTimestamp(2, time);
            MACHINE_SNAPSHOT_INSERT.setString(3, machineSnapshot.getType());
            MACHINE_SNAPSHOT_INSERT.setString(4, machineSnapshot.getComment());
            MACHINE_SNAPSHOT_INSERT.executeUpdate();
            commit();
            final ChannelSnapshot[] channelSnapshots = machineSnapshot.getChannelSnapshots();
            int count = 0;
            for (int index = 0; index < channelSnapshots.length; index++) {
                ChannelSnapshot channelSnapshot = channelSnapshots[index];
                if (channelSnapshot != null) {
                    publish(channelSnapshot, id);
                    count++;
                    if ((count % commitFrequency) == 0) {
                        CHANNEL_SNAPSHOT_INSERT.executeBatch();
                        commit();
                        CHANNEL_SNAPSHOT_INSERT.clearBatch();
                    }
                }
            }
            CHANNEL_SNAPSHOT_INSERT.executeBatch();
            commit();
            CHANNEL_SNAPSHOT_INSERT.clearBatch();
            machineSnapshot.setId(id);
        } catch (SQLException exception) {
            exception.getNextException().printStackTrace();
            throw new StateStoreException("Error publishing a machine snapshot.", exception);
        }
    }

    protected void commit() {
        if (_databaseAdaptor instanceof PgsqlDatabaseAdaptor) {
            if (!autoCommit) {
                try {
                    _connection.commit();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
	 * Fetch an array of logger types
	 *
	 * @return                                             an array of available logger types
	 * @exception StateStoreException                      Description of the Exception
	 * @throws gov.sns.tools.pvlogger.StateStoreException  if a SQL exception is thrown
	 */
    public String[] fetchTypes() throws StateStoreException {
        try {
            getSnapshotTypeQueryStatement();
            List<String> types = new ArrayList<String>();
            ResultSet result = SNAPSHOT_TYPE_QUERY.executeQuery();
            commit();
            while (result.next()) {
                types.add(result.getString(SNAPSHOT_TYPE_PKEY));
            }
            return types.toArray(new String[types.size()]);
        } catch (SQLException exception) {
            throw new StateStoreException("Error fetching pvlogger types.", exception);
        }
    }

    /**
	 * Fetch a channel group for the specified logger type
	 *
	 * @param type                                         the logger type
	 * @return                                             a channel group for the logger type
	 *      which includes the type, description and the pvs to log
	 * @exception StateStoreException                      Description of the Exception
	 * @throws gov.sns.tools.pvlogger.StateStoreException  if a SQL exception is thrown
	 */
    public ChannelGroup fetchGroup(final String type) throws StateStoreException {
        try {
            getSnapshotTypeQueryByNameStatement();
            getSnapshotTypePvQueryByNameStatement();
            SNAPSHOT_TYPE_QUERY_BY_NAME.setString(1, type);
            ResultSet typeResult = SNAPSHOT_TYPE_QUERY_BY_NAME.executeQuery();
            commit();
            String description;
            double loggingPeriod;
            if (typeResult.next()) {
                description = typeResult.getString(SNAPSHOT_TYPE_DESC_COL);
                loggingPeriod = typeResult.getDouble(SNAPSHOT_TYPE_PERIOD_COL);
            } else {
                description = "";
                loggingPeriod = 0;
            }
            List<String> pvs = new ArrayList<String>();
            SNAPSHOT_TYPE_PV_QUERY_BY_NAME.setString(1, type);
            ResultSet result = SNAPSHOT_TYPE_PV_QUERY_BY_NAME.executeQuery();
            commit();
            while (result.next()) {
                pvs.add(result.getString(SNAPSHOT_TYPE_PV_PV_PKEY));
            }
            String[] pvArray = pvs.toArray(new String[pvs.size()]);
            return new ChannelGroup(type, description, pvArray, loggingPeriod);
        } catch (SQLException exception) {
            throw new StateStoreException("Error fetching pvlogger group for the specified type.", exception);
        }
    }

    /**
	 * Fetch the machine snapshot associated with the unique machine snapshot identifier.
	 *
	 * @param id                                           The unique machine snapshot identifier
	 * @return                                             The machine snapshop read from the
	 *      persistent store.
	 * @exception StateStoreException                      Description of the Exception
	 * @throws gov.sns.tools.pvlogger.StateStoreException  if a SQL exception is thrown
	 */
    public MachineSnapshot fetchMachineSnapshot(final long id) throws StateStoreException {
        try {
            getMachineSnapshotQueryByPkeyStatement();
            MACHINE_SNAPSHOT_QUERY_BY_PKEY.setLong(1, id);
            ResultSet snapshotResult = MACHINE_SNAPSHOT_QUERY_BY_PKEY.executeQuery();
            commit();
            if (snapshotResult.next()) {
                String type = snapshotResult.getString(MACHINE_SNAPSHOT_TYPE_COL);
                Timestamp timestamp = snapshotResult.getTimestamp(MACHINE_SNAPSHOT_TIME_COL);
                String comment = snapshotResult.getString(MACHINE_SNAPSHOT_COMMENT_COL);
                ChannelSnapshot[] channelSnapshots = fetchChannelSnapshotsForMachine(id);
                return new MachineSnapshot(id, type, timestamp, comment, channelSnapshots);
            }
        } catch (SQLException exception) {
            throw new StateStoreException("Error fetching the machine snapshot.", exception);
        }
        return null;
    }

    /**
	 * Fetch the channel snapshots from the data source and populate the machine snapshot
	 *
	 * @param machineSnapshot          The machine snapshot for which to fetch the channel
	 *      snapshots and load them
	 * @return                         the machineSnapshot which is the same as the parameter
	 *      returned for convenience
	 * @exception StateStoreException  Description of the Exception
	 */
    public MachineSnapshot loadChannelSnapshotsInto(final MachineSnapshot machineSnapshot) throws StateStoreException {
        ChannelSnapshot[] snapshots = fetchChannelSnapshotsForMachine(machineSnapshot.getId());
        machineSnapshot.setChannelSnapshots(snapshots);
        return machineSnapshot;
    }

    /**
	 * Fetch the machine snapshots within the specified time range. If the type is not null, then
	 * restrict the machine snapshots to those of the specified type. The machine snapshots do not
	 * include the channel snapshots. A complete snapshot can be obtained using the
	 * fetchMachineSnapshot(id) method.
	 *
	 * @param type                                         The type of machine snapshots to fetch
	 *      or null for no restriction
	 * @param startTime                                    The start time of the time range
	 * @param endTime                                      The end time of the time range
	 * @return                                             An array of machine snapshots meeting
	 *      the specified criteria
	 * @exception StateStoreException                      Description of the Exception
	 * @throws gov.sns.tools.pvlogger.StateStoreException  if a SQL exception is thrown
	 */
    public MachineSnapshot[] fetchMachineSnapshotsInRange(final String type, final java.util.Date startTime, final java.util.Date endTime) throws StateStoreException {
        if (type == null) {
            return fetchMachineSnapshotsInRange(startTime, endTime);
        }
        List<MachineSnapshot> snapshots = new ArrayList<MachineSnapshot>();
        try {
            getMachineSnapshotQueryByTypeTimerangeStatement();
            MACHINE_SNAPSHOT_QUERY_BY_TYPE_TIMERANGE.setString(1, type);
            System.out.println("startTime.getTime() = " + startTime.getTime());
            System.out.println("endTime.getTime() = " + endTime.getTime());
            MACHINE_SNAPSHOT_QUERY_BY_TYPE_TIMERANGE.setTimestamp(2, new Timestamp(startTime.getTime()));
            MACHINE_SNAPSHOT_QUERY_BY_TYPE_TIMERANGE.setTimestamp(3, new Timestamp(endTime.getTime()));
            ResultSet snapshotResult = MACHINE_SNAPSHOT_QUERY_BY_TYPE_TIMERANGE.executeQuery();
            commit();
            while (snapshotResult.next()) {
                long id = snapshotResult.getLong(MACHINE_SNAPSHOT_PKEY);
                String foundType = snapshotResult.getString(MACHINE_SNAPSHOT_TYPE_COL);
                Timestamp timestamp = snapshotResult.getTimestamp(MACHINE_SNAPSHOT_TIME_COL);
                String comment = snapshotResult.getString(MACHINE_SNAPSHOT_COMMENT_COL);
                snapshots.add(new MachineSnapshot(id, foundType, timestamp, comment, new ChannelSnapshot[0]));
            }
        } catch (SQLException exception) {
            throw new StateStoreException("Error fetching machine snapshots for the specified type and in the specified range.", exception);
        }
        return snapshots.toArray(new MachineSnapshot[snapshots.size()]);
    }

    /**
	 * Fetch the machine snapshots within the specified time range. The machine snapshots do not
	 * include the channel snapshots. A complete snapshot can be obtained using the
	 * fetchMachineSnapshot(id) method.
	 *
	 * @param startTime                                    The start time of the time range
	 * @param endTime                                      The end time of the time range
	 * @return                                             An array of machine snapshots meeting
	 *      the specified criteria
	 * @exception StateStoreException                      Description of the Exception
	 * @throws gov.sns.tools.pvlogger.StateStoreException  if a SQL exception is thrown
	 */
    protected MachineSnapshot[] fetchMachineSnapshotsInRange(final java.util.Date startTime, final java.util.Date endTime) throws StateStoreException {
        List<MachineSnapshot> snapshots = new ArrayList<MachineSnapshot>();
        try {
            getMachineSnapshotQueryByTimerangeStatement();
            MACHINE_SNAPSHOT_QUERY_BY_TIMERANGE.setTimestamp(1, new Timestamp(startTime.getTime()));
            MACHINE_SNAPSHOT_QUERY_BY_TIMERANGE.setTimestamp(2, new Timestamp(endTime.getTime()));
            ResultSet snapshotResult = MACHINE_SNAPSHOT_QUERY_BY_TIMERANGE.executeQuery();
            commit();
            while (snapshotResult.next()) {
                long id = snapshotResult.getLong(MACHINE_SNAPSHOT_PKEY);
                String foundType = snapshotResult.getString(MACHINE_SNAPSHOT_TYPE_COL);
                Timestamp timestamp = snapshotResult.getTimestamp(MACHINE_SNAPSHOT_TIME_COL);
                String comment = snapshotResult.getString(MACHINE_SNAPSHOT_COMMENT_COL);
                ChannelSnapshot[] channelSnapshots = fetchChannelSnapshotsForMachine(id);
                snapshots.add(new MachineSnapshot(id, foundType, timestamp, comment, new ChannelSnapshot[0]));
            }
        } catch (SQLException exception) {
            throw new StateStoreException("Error fetching machine snapshots in the specified range.", exception);
        }
        return snapshots.toArray(new MachineSnapshot[snapshots.size()]);
    }

    /**
	 * Fetch the channel snapshots associated with a machine snapshot given by the machine
	 * snapshot's unique identifier.
	 *
	 * @param id                                           The unique machine identifier.
	 * @return                                             The channel snapshots associated with
	 *      the machine snapshop
	 * @exception StateStoreException                      Description of the Exception
	 * @throws gov.sns.tools.pvlogger.StateStoreException  if a SQL exception is thrown
	 */
    protected ChannelSnapshot[] fetchChannelSnapshotsForMachine(final long id) throws StateStoreException {
        try {
            List<ChannelSnapshot> snapshots = new ArrayList<ChannelSnapshot>();
            getChannelSnapshotQueryByMachineSnapshotStatement();
            CHANNEL_SNAPSHOT_QUERY_BY_MACHINE_SNAPSHOT.setLong(1, id);
            ResultSet snapshotResult = CHANNEL_SNAPSHOT_QUERY_BY_MACHINE_SNAPSHOT.executeQuery();
            commit();
            while (snapshotResult.next()) {
                String pv = snapshotResult.getString(PV_SNAPSHOT_PV_COL);
                Timestamp timestamp = snapshotResult.getTimestamp(PV_SNAPSHOT_TIMESTAMP_COL);
                Array arrayValue = snapshotResult.getArray(PV_SNAPSHOT_VALUE_COL);
                BigDecimal[] bigValue = (BigDecimal[]) snapshotResult.getArray(PV_SNAPSHOT_VALUE_COL).getArray();
                double[] value = toDoubleArray(bigValue);
                short status = snapshotResult.getShort(PV_SNAPSHOT_STATUS_COL);
                short severity = snapshotResult.getShort(PV_SNAPSHOT_SEVERITY_COL);
                snapshots.add(new ChannelSnapshot(pv, value, status, severity, new gov.sns.ca.Timestamp(timestamp)));
            }
            return snapshots.toArray(new ChannelSnapshot[snapshots.size()]);
        } catch (SQLException exception) {
            throw new StateStoreException("Error fetching channel snapshots for the specified machine id.", exception);
        }
    }

    /**
	 * Convert an array of numbers to an array of double values.
	 *
	 * @param numbers  The array of numbers to convert
	 * @return         An array of double values corresponding to the input array of numbers.
	 */
    protected double[] toDoubleArray(Number[] numbers) {
        double[] array = new double[numbers.length];
        for (int index = 0; index < numbers.length; index++) {
            array[index] = numbers[index].doubleValue();
        }
        return array;
    }
}
