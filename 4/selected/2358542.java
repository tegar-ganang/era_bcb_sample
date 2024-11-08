package gov.sns.tools.pvlogger;

import gov.sns.tools.database.ConnectionDictionary;
import gov.sns.tools.database.DatabaseAdaptor;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class BufferSPSqlStateStore extends SqlStateStore {

    protected static final String PV_SNAPSHOT_PV_SP_COL = "set_pt_sgnl_id";

    protected static final String PV_SNAPSHOT_VALUE_SP_COL = "set_pt_sgnl_val";

    /** Description of the Field */
    protected static final String SNAPSHOT_TYPE_PV_SP_COL = "set_pt_sgnl_id";

    public BufferSPSqlStateStore(DatabaseAdaptor adaptor, Connection connection) {
        super(adaptor, connection);
    }

    public BufferSPSqlStateStore(Connection connection) {
        super(connection);
    }

    public BufferSPSqlStateStore(DatabaseAdaptor adaptor, String urlSpec, String user, String password) throws StateStoreException {
        super(adaptor, urlSpec, user, password);
    }

    public BufferSPSqlStateStore(String urlSpec, String user, String password) throws StateStoreException {
        super(urlSpec, user, password);
    }

    public BufferSPSqlStateStore(DatabaseAdaptor adaptor, ConnectionDictionary dictionary) throws StateStoreException {
        super(adaptor, dictionary);
    }

    public BufferSPSqlStateStore(ConnectionDictionary dictionary) throws StateStoreException {
        super(dictionary);
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
    @Override
    public ChannelGroup fetchGroup(final String type) throws StateStoreException {
        System.out.println("/////// BufferSqlStateStore::fetchGroup called");
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
            return new ChannelBufferGroup(type, description, pvArray, loggingPeriod);
        } catch (SQLException exception) {
            throw new StateStoreException("Error fetching pvlogger group for the specified type.", exception);
        }
    }

    /**
	 * Create the prepared statement if it does not already exist.
	 *
	 * @return                        the prepared statement for inserting a new channel snapshot
	 * @exception SQLException        Description of the Exception
	 * @throws java.sql.SQLException  if an exception occurs during a SQL evaluation
	 */
    @Override
    protected PreparedStatement getChannelSnapshotInsertStatement() throws SQLException {
        if (CHANNEL_SNAPSHOT_INSERT == null) {
            CHANNEL_SNAPSHOT_INSERT = _connection.prepareStatement("INSERT INTO " + PV_SNAPSHOT_TABLE + "(" + PV_SNAPSHOT_MACHINE_SNAPSHOT_COL + ", " + PV_SNAPSHOT_PV_COL + ", " + PV_SNAPSHOT_TIMESTAMP_COL + ", " + PV_SNAPSHOT_VALUE_COL + ", " + PV_SNAPSHOT_STATUS_COL + ", " + PV_SNAPSHOT_SEVERITY_COL + ", " + PV_SNAPSHOT_PV_SP_COL + ", " + PV_SNAPSHOT_VALUE_SP_COL + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        }
        return CHANNEL_SNAPSHOT_INSERT;
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
    public void publish(final ChannelSnapshot snapshot, final ChannelSnapshot snapshotSP, final long machineId) throws StateStoreException {
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
            CHANNEL_SNAPSHOT_INSERT.setString(7, snapshotSP.getPV());
            Array valueArraySP = _databaseAdaptor.getArray(SGNL_VALUE_ARRAY_TYPE, _connection, snapshotSP.getValue());
            CHANNEL_SNAPSHOT_INSERT.setArray(8, valueArraySP);
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
    public void publish(final MachineSnapshotSP machineSnapshot) throws StateStoreException {
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
            final ChannelSnapshot[] channelSnapshotsSP = machineSnapshot.getChannelSnapshotsSP();
            int count = 0;
            for (int index = 0; index < channelSnapshots.length; index++) {
                ChannelSnapshot channelSnapshot = channelSnapshots[index];
                ChannelSnapshot channelSnapshotSP = channelSnapshotsSP[index];
                if (channelSnapshot != null) {
                    publish(channelSnapshot, channelSnapshotSP, id);
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
    protected ChannelSnapshot[] fetchChannelSnapshotsForMachineSP(final long id) throws StateStoreException {
        try {
            List<ChannelSnapshot> snapshots = new ArrayList<ChannelSnapshot>();
            getChannelSnapshotQueryByMachineSnapshotStatement();
            CHANNEL_SNAPSHOT_QUERY_BY_MACHINE_SNAPSHOT.setLong(1, id);
            ResultSet snapshotResult = CHANNEL_SNAPSHOT_QUERY_BY_MACHINE_SNAPSHOT.executeQuery();
            commit();
            while (snapshotResult.next()) {
                String pv = snapshotResult.getString(PV_SNAPSHOT_PV_SP_COL);
                Timestamp timestamp = snapshotResult.getTimestamp(PV_SNAPSHOT_TIMESTAMP_COL);
                BigDecimal[] bigValue = (BigDecimal[]) snapshotResult.getArray(PV_SNAPSHOT_VALUE_SP_COL).getArray();
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
	 * Fetch the machine snapshot associated with the unique machine snapshot identifier.
	 *
	 * @param id                                           The unique machine snapshot identifier
	 * @return                                             The machine snapshop read from the
	 *      persistent store.
	 * @exception StateStoreException                      Description of the Exception
	 * @throws gov.sns.tools.pvlogger.StateStoreException  if a SQL exception is thrown
	 */
    @Override
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
                ChannelSnapshot[] channelSnapshotsSP = fetchChannelSnapshotsForMachineSP(id);
                return new MachineSnapshotSP(id, type, timestamp, comment, channelSnapshots, channelSnapshotsSP);
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
    public MachineSnapshot loadChannelSnapshotsInto(final MachineSnapshotSP machineSnapshotSP) throws StateStoreException {
        ChannelSnapshot[] snapshots = fetchChannelSnapshotsForMachine(machineSnapshotSP.getId());
        ChannelSnapshot[] snapshotsSP = fetchChannelSnapshotsForMachineSP(machineSnapshotSP.getId());
        machineSnapshotSP.setChannelSnapshots(snapshots);
        machineSnapshotSP.setChannelSnapshotsSP(snapshotsSP);
        return machineSnapshotSP;
    }
}
