package gov.sns.services.pvloggertool;

import gov.sns.tools.pvlogger.*;
import java.util.*;

/**
 * SessionModel manages a single logger session.
 *
 * @author  tap
 * @since Jun 01, 2004
 */
public class SessionModel {

    /** state store from which to load the group */
    protected final StateStore STATE_STORE;

    /** Channel Group type */
    protected final String GROUP_TYPE;

    /** Logger Session */
    protected LoggerSession _loggerSession;

    /** channel group to log */
    protected ChannelGroup _group;

    /** Last snapshot published */
    protected MachineSnapshot _lastPublishedSnapshot;

    /** wall clock time of the last MPS event */
    private Date _lastLoggerEventTime;

    /**
	 * Constructor
	 */
    public SessionModel(final String groupType, final StateStore store) {
        STATE_STORE = store;
        GROUP_TYPE = groupType;
        _lastLoggerEventTime = new Date();
        reloadGroup();
    }

    /** Start logging */
    public void startLogging() {
        if (_loggerSession != null) {
            _loggerSession.addLoggerChangeListener(new LoggerChangeAdapter() {

                @Override
                public void snapshotPublished(final LoggerSession logger, final MachineSnapshot snapshot) {
                    _lastPublishedSnapshot = snapshot;
                    _lastLoggerEventTime = new Date();
                }
            });
            _loggerSession.startLogging();
        } else {
            System.err.println("Attempting to start logging a non-existent session for group:  " + GROUP_TYPE);
        }
    }

    /**
	 * Reload the logger session's channel group from the state store.  This method may be used to update from the state store the signals associated with the
	 * channel group in use.
	 */
    public void reloadGroup() {
        if (_group != null) {
            _group.dispose();
        }
        _group = fetchGroup(STATE_STORE);
        if (_group.getDefaultLoggingPeriod() > 0) {
            if (_loggerSession == null) {
                _loggerSession = new LoggerSession(_group, STATE_STORE);
            } else {
                _loggerSession.setChannelGroup(fetchGroup(STATE_STORE));
            }
        } else if (_loggerSession != null) {
            _loggerSession.stopLogging();
            _loggerSession = null;
        }
    }

    /**
	 * Fetch the channel group from the state store
	 * @param store the store from which to fetch the channel group
	 * @return the channel group fetched from the state store
	 */
    protected ChannelGroup fetchGroup(final StateStore store) {
        return store.fetchGroup(GROUP_TYPE);
    }

    /**
	 * Resume logging.
	 */
    public void resumeLogging() {
        if (_loggerSession != null) {
            _loggerSession.resumeLogging();
        } else {
            System.err.println("Attempting to resume a non-existent logging session for group:  " + GROUP_TYPE);
        }
    }

    /**
	 * Stop logging
	 */
    public void stopLogging() {
        if (_loggerSession != null) {
            _loggerSession.stopLogging();
        }
    }

    /**
	 * Get the logger session
	 * @return the logger session
	 */
    public LoggerSession getLoggerSession() {
        return _loggerSession;
    }

    /**
	 * Get the channel group type that is managed by this session model.
	 * @return the label of the channel group managed by this model
	 */
    public String getChannelGroupType() {
        return GROUP_TYPE;
    }

    /**
	 * Get the most recently published machine snapshot.
	 * @return The most recently published machine snapshot.
	 */
    public MachineSnapshot getLastPublishedSnapshot() {
        return _lastPublishedSnapshot;
    }

    /**
	 * Get the timestamp of the last logger event
	 * @return the wall clock timestamp of the last logger event
	 */
    public Date getLastLoggerEventTime() {
        return _lastLoggerEventTime;
    }
}
