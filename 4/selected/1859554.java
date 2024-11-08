package gov.sns.tools.apputils.pvlogbrowser;

import gov.sns.tools.pvlogger.*;
import gov.sns.tools.database.*;
import gov.sns.tools.messaging.MessageCenter;
import java.sql.*;

/**
 * BrowserModel is the main document model.
 *
 * @author  tap
 */
public class BrowserModel {

    protected MessageCenter _messageCenter;

    protected BrowserModelListener _proxy;

    protected boolean _hasConnected = false;

    protected StateStore _loggerStore;

    protected String[] _loggerTypes;

    protected MachineSnapshot[] _snapshots;

    protected ChannelGroup _group;

    /**
	 * Constructor
	 */
    public BrowserModel() {
        _messageCenter = new MessageCenter("Browser Model");
        _proxy = _messageCenter.registerSource(this, BrowserModelListener.class);
        _snapshots = new MachineSnapshot[0];
        _group = null;
    }

    /**
	 * Add a listener of model events from this model.
	 * @param listener the listener to add for receiving model events.
	 */
    public void addBrowserModelListener(BrowserModelListener listener) {
        _messageCenter.registerTarget(listener, this, BrowserModelListener.class);
    }

    /**
	 * Remove the listener from receiving model events from this model.
	 * @param listener the listener to remove from receiving model events.
	 */
    public void removeBrowserModelListener(BrowserModelListener listener) {
        _messageCenter.removeTarget(listener, this, BrowserModelListener.class);
    }

    /**
	 * Set the database connection to the one specified.
	 * @param connection the new database connection
	 */
    public void setDatabaseConnection(final Connection connection, final ConnectionDictionary dictionary) {
        _hasConnected = false;
        _group = null;
        _snapshots = new MachineSnapshot[0];
        _loggerTypes = null;
        _loggerStore = new SqlStateStore(dictionary.getDatabaseAdaptor(), connection);
        _hasConnected = true;
        _proxy.connectionChanged(this);
    }

    /**
	 * Connect to the database with the default connection dictionary
	 * @throws DatabaseException if the connection or schema fetch fails
	 */
    public void connect() throws DatabaseException {
        connect(ConnectionDictionary.defaultDictionary());
    }

    /**
	 * Connect to the database with the specified connection dictionary
	 * @param dictionary The connection dictionary
	 * @throws DatabaseException if the connection or schema fetch fails
	 */
    public void connect(ConnectionDictionary dictionary) throws DatabaseException {
        Connection connection = dictionary.getDatabaseAdaptor().getConnection(dictionary);
        setDatabaseConnection(connection, dictionary);
    }

    /**
	 * Determine if we have successfully connected to the database.  Note that this does
	 * not mean that the database connection is still valid.
	 * @return true if we have successfully connected to the database and false if not
	 */
    public boolean hasConnected() {
        return _hasConnected;
    }

    /**
	 * Fetch the available logger types from the data store.
	 * @return an array of available logger types.
	 */
    protected String[] fetchLoggerTypes() {
        _loggerTypes = _loggerStore.fetchTypes();
        return _loggerTypes;
    }

    /**
	 * Get the array of available logger types.
	 * @return the array of available logger types.
	 */
    public String[] getLoggerTypes() {
        return (_hasConnected && _loggerTypes == null) ? fetchLoggerTypes() : _loggerTypes;
    }

    /**
	 * Select the specified channel group corresponding to the logger type.
	 * @param type the logger type identifying the channel group
	 * @return the channel group
	 */
    public ChannelGroup selectGroup(final String type) {
        if (type == null) {
            _group = null;
            _proxy.selectedChannelGroupChanged(this, _group);
        } else if (_group == null || !_group.getLabel().equals(type)) {
            _group = _loggerStore.fetchGroup(type);
            _proxy.selectedChannelGroupChanged(this, _group);
        }
        return _group;
    }

    /**
	 * Get the selected channel group
	 * @return the selected channel group
	 */
    public ChannelGroup getSelectedGroup() {
        return _group;
    }

    /**
	 * Get the array of machine snapshots that had been fetched.
	 * @return the array of machine snapshots
	 */
    public MachineSnapshot[] getSnapshots() {
        return _snapshots;
    }

    /**
	 * Fetch the machine snapshots that were taken between the selected times.  Only identifier
	 * data is fetched into the machine snapshot.
	 * @param startTime the start time of the range
	 * @param endTime the end time of the range
	 */
    public void fetchMachineSnapshots(java.util.Date startTime, java.util.Date endTime) {
        _snapshots = _loggerStore.fetchMachineSnapshotsInRange(_group.getLabel(), startTime, endTime);
        System.out.println("Found " + _snapshots.length + " snapshots...");
        _proxy.machineSnapshotsFetched(this, _snapshots);
    }

    /**
	 * Populate all fetched machine snapshots with all of their data.
	 */
    public void populateSnapshots() {
        for (int index = 0; index < _snapshots.length; index++) {
            populateSnapshot(_snapshots[index]);
        }
    }

    /**
	 * Populate the machine snapshot with all of its data.
	 * @param snapshot the machine snapshot to populate
	 * @return the machine snapshot that was populated (same object as the parameter)
	 */
    public MachineSnapshot populateSnapshot(final MachineSnapshot snapshot) {
        return (snapshot.getChannelCount() == 0) ? _loggerStore.loadChannelSnapshotsInto(snapshot) : snapshot;
    }
}
