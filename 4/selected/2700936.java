package gov.sns.apps.pvlogger;

import gov.sns.services.pvloggertool.*;
import gov.sns.tools.Lock;
import gov.sns.tools.services.*;
import gov.sns.tools.messaging.MessageCenter;
import java.util.*;

/**
 * LoggerSessionHandler handles requests for a single remote logger session model.
 *
 * @author  tap
 * @since Jun 01, 2004
 */
public class LoggerSessionHandler {

    protected String _groupType;

    protected LoggerPortal _remoteProxy;

    protected final Lock _lock;

    protected final MessageCenter _messageCenter;

    protected final LoggerSessionListener _postProxy;

    protected String _latestSnapshotDump;

    protected Date _latestSnapshotTimestamp;

    protected List _channelRefs;

    protected volatile double _loggingPeriod;

    protected volatile boolean _isLogging;

    protected Date _lastLoggerEventTime;

    protected Date _lastChannelEventTime;

    /**
	 * Constructor
	 */
    public LoggerSessionHandler(String groupType, LoggerPortal remoteProxy) {
        _groupType = groupType;
        _remoteProxy = remoteProxy;
        _lock = new Lock();
        _messageCenter = new MessageCenter("Logger Session");
        _postProxy = _messageCenter.registerSource(this, LoggerSessionListener.class);
        _latestSnapshotTimestamp = null;
        _latestSnapshotDump = "";
        _lastLoggerEventTime = new Date(0);
        _lastChannelEventTime = new Date(0);
        _channelRefs = new ArrayList();
        _isLogging = false;
        _loggingPeriod = 0;
    }

    /**
	 * Add a listener of logger session handler events from this handler
	 * @param listener the listener to add
	 */
    public void addLoggerSessionListener(LoggerSessionListener listener) {
        _messageCenter.registerTarget(listener, this, LoggerSessionListener.class);
    }

    /**
	 * Remove the listener of logger session handler events from this handler
	 * @param listener the listener to remove
	 */
    public void removeLoggerSessionListener(LoggerSessionListener listener) {
        _messageCenter.removeTarget(listener, this, LoggerSessionListener.class);
    }

    /**
	 * Get the remote proxy managed by this handler
	 * @return the remote proxy
	 */
    public LoggerPortal getRemoteProxy() {
        return _remoteProxy;
    }

    /**
	 * Update the record with the current information from the remote application.  Try to get a lock
	 * for updating data from the remote application.  If the lock is unsuccessful simply return 
	 * false, otherwise perform the update.
	 * @return true if the data was successfully updated and false if not.
	 */
    public boolean update() {
        if (_lock.tryLock()) {
            try {
                Date lastLoggerEventTime = _remoteProxy.getLastLoggerEventTime(_groupType);
                if (!lastLoggerEventTime.equals(_lastLoggerEventTime)) {
                    boolean hasUpdate = false;
                    final boolean isLogging = _remoteProxy.isLogging(_groupType);
                    if (isLogging != _isLogging) {
                        _isLogging = isLogging;
                        hasUpdate = true;
                    }
                    final double loggingPeriod = _remoteProxy.getLoggingPeriod(_groupType);
                    if (loggingPeriod != _loggingPeriod) {
                        _loggingPeriod = loggingPeriod;
                        hasUpdate = true;
                    }
                    if (hasUpdate) {
                        _postProxy.loggerSessionUpdated(this);
                    }
                    _latestSnapshotTimestamp = _remoteProxy.getTimestampOfLastPublishedSnapshot(_groupType);
                    _latestSnapshotDump = _remoteProxy.getLastPublishedSnapshotDump(_groupType);
                    _lastLoggerEventTime = lastLoggerEventTime;
                    _postProxy.snapshotPublished(this, _latestSnapshotTimestamp, _latestSnapshotDump);
                }
                Date lastChannelEventTime = _remoteProxy.getLastChannelEventTime(_groupType);
                if (!lastChannelEventTime.equals(_lastChannelEventTime)) {
                    List channelTables = _remoteProxy.getChannels(_groupType);
                    _lastChannelEventTime = lastChannelEventTime;
                    processChannels(channelTables);
                }
                _postProxy.loggerSessionUpdated(this);
                return true;
            } catch (Exception exception) {
                System.err.println("Got an update exception...");
                System.err.println(exception);
                return true;
            } finally {
                _lock.unlock();
            }
        }
        return false;
    }

    /**
	 * Process the channel information.  The channelTables is a list of tables each of which
	 * provides the channel PV and channel connection status.  For convenience a channel ref 
	 * is constructed for each such channel table.
	 * @param channelTables the list of channel information about the remote logger's channels
	 */
    protected void processChannels(List channelTables) {
        _channelRefs = new ArrayList(channelTables.size());
        for (Iterator iter = channelTables.iterator(); iter.hasNext(); ) {
            Map channelTable = (Map) iter.next();
            String pv = (String) channelTable.get(LoggerPortal.CHANNEL_SIGNAL);
            Boolean connected = (Boolean) channelTable.get(LoggerPortal.CHANNEL_CONNECTED);
            _channelRefs.add(new ChannelRef(pv, connected));
        }
        _postProxy.channelsChanged(this, _channelRefs);
    }

    /**
	 * Get the list of channel references which carry the channel PV and channel connection
	 * statuse information about the remote logger's channels.
	 * @return the list of channel references
	 */
    public List getChannelRefs() {
        return _channelRefs;
    }

    /**
	 * Get the dump of the last published snapshot
	 * @return the dump of the last published snapshot
	 */
    public String getLastPublishedSnapshotDump() {
        return _latestSnapshotDump;
    }

    /**
	 * Get the timestamp of the last published snapshot
	 * @return the timestamp of the last published snapshot
	 */
    public Date getTimestampOfLastPublishedSnapshot() {
        return _latestSnapshotTimestamp;
    }

    /**
	 * Determine if the logger is running.
	 * @return true if the logger is running and false if not.
	 */
    public boolean isLogging() {
        return _isLogging;
    }

    /**
	 * Get the logging period.
	 * @return the logging period in units of seconds
	 */
    public double getLoggingPeriod() {
        return _loggingPeriod;
    }

    /**
	 * Set the logging period of the remote logger
	 * @param period The period in seconds to set for remote logging
	 */
    public void setLoggingPeriod(double period) {
        if (_remoteProxy != null) {
            try {
                _remoteProxy.setLoggingPeriod(_groupType, period);
            } catch (RemoteMessageException exception) {
                System.err.println("Remote exception while trying to set the logging period...");
                exception.printStackTrace();
            }
        }
    }
}
