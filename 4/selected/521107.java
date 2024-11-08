package gov.sns.services.pvloggertool;

import gov.sns.tools.services.*;
import gov.sns.tools.pvlogger.*;
import gov.sns.ca.Channel;
import java.util.*;

/**
 * LoggerService is the implementation of LoggerPortal that responds to
 * requests from remote clients on behalf of the logger model.
 *
 * @author  tap
 */
public class LoggerService implements LoggerPortal {

    protected final String IDENTITY = "PV Logger";

    protected final LoggerModel _model;

    /**
	 * LoggerService constructor
	 */
    public LoggerService(final LoggerModel model) {
        _model = model;
        broadcast();
    }

    /**
	 * Begin broadcasting the service
	 */
    public void broadcast() {
        ServiceDirectory.defaultDirectory().registerService(LoggerPortal.class, IDENTITY, this);
        System.out.println("broadcasting...");
    }

    /**
	 * Set the period between events where we take and store machine snapshots.
	 * @param groupType identifies the group by type 
	 * @param period The period in seconds between events where we take and store machine snapshots.
	 * @return 0 Services must always return something
	 */
    public int setLoggingPeriod(String groupType, double period) {
        final LoggerSession session = _model.getLoggerSession(groupType);
        if (session != null) {
            session.setLoggingPeriod(period);
        }
        return 0;
    }

    /**
	 * Get the logging period.
	 * @param groupType identifies the group by type 
	 * @return The period in seconds between events where we take and store machine snapshots.
	 */
    public double getLoggingPeriod(String groupType) {
        final LoggerSession session = _model.getLoggerSession(groupType);
        if (session != null) {
            return session.getLoggingPeriod();
        } else {
            return 0;
        }
    }

    /**
	 * Determine if the logger is presently logging
	 * @param groupType identifies the group by type 
	 * @return true if the logger is logging and false if not
	 */
    public boolean isLogging(String groupType) {
        final LoggerSession session = _model.getLoggerSession(groupType);
        if (session != null) {
            return session.isLogging();
        } else {
            return false;
        }
    }

    /**
	 * Restart the logger.
	 * Stop logging, reload groups from the database and resume logging.
	 * @return 0 Services must always return something
	 */
    public int restartLogger() {
        _model.restartLogger();
        return 0;
    }

    /**
	 * Resume the logger logging.
	 * @return 0 Services must always return something
	 */
    public int resumeLogging() {
        _model.resumeLogging();
        return 0;
    }

    /**
	 * Stop the logger.
	 * @return 0 Services must always return something
	 */
    public int stopLogging() {
        _model.stopLogging();
        return 0;
    }

    /**
	 * Shutdown the process.
	 * @param code The shutdown code which is normally just 0.
	 * @return 0 Services must always return something
	 */
    public int shutdown(int code) {
        _model.shutdown(code);
        return 0;
    }

    /**
	 * Get the name of the host where the application is running.
	 * @return The name of the host where the application is running.
	 */
    public String getHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (java.net.UnknownHostException exception) {
            return "";
        }
    }

    /**
	 * Get the launch time of the service.
	 * @return the launch time in seconds since the Java epoch of January 1, 1970.
	 */
    public double getLaunchTime() {
        return LoggerModel.getLaunchTime();
    }

    /**
	 * Get the timestamp of the last channel event (e.g. channel connected/disconnected event)
	 * @param groupType identifies the group by type 
	 * @return the wall clock timestamp of the last channel event
	 */
    public Date getLastChannelEventTime(final String groupType) {
        final LoggerSession session = _model.getLoggerSession(groupType);
        if (session != null) {
            return session.getChannelGroup().getLastChannelEventTime();
        } else {
            return new Date(0);
        }
    }

    /**
	 * Get the timestamp of the last logger event
	 * @param groupType identifies the group by type 
	 * @return the wall clock timestamp of the last logger event
	 */
    public Date getLastLoggerEventTime(final String groupType) {
        return _model.getSessionModel(groupType).getLastLoggerEventTime();
    }

    /**
	 * Get a vector of group types
	 * @return a vector of group types
	 */
    public Vector getGroupTypes() {
        return new Vector(_model.getSessionTypes());
    }

    /**
	 * Get the number of channels we wish to log.
	 * @param groupType identifies the group by type 
	 * @return the number of channels we wish to log
	 */
    public int getChannelCount(final String groupType) {
        return _model.getLoggerSession(groupType).getChannelGroup().getChannelCount();
    }

    /**
	 * Get the list of channel info tables.  Each channel info table contains
	 * the PV signal name and the channel connection status.
	 * @param groupType identifies the group by type 
	 * @return The list channel info tables corresponding to the channels we wish to log
	 */
    public Vector getChannels(final String groupType) {
        final LoggerSession session = _model.getLoggerSession(groupType);
        if (session != null) {
            final Collection channels = session.getChannels();
            Vector channelInfoList = new Vector(channels.size());
            for (Iterator iter = channels.iterator(); iter.hasNext(); ) {
                Hashtable info = new Hashtable();
                Channel channel = (Channel) iter.next();
                info.put(CHANNEL_SIGNAL, channel.channelName());
                info.put(CHANNEL_CONNECTED, new Boolean(channel.isConnected()));
                channelInfoList.add(info);
            }
            return channelInfoList;
        } else {
            return new Vector();
        }
    }

    /**
	 * Get the timestamp of the last published snapshot
	 * @param groupType identifies the group by type 
	 * @return the timestamp of the last published snapshot
	 */
    public Date getTimestampOfLastPublishedSnapshot(String groupType) {
        MachineSnapshot snapshot = _model.getSessionModel(groupType).getLastPublishedSnapshot();
        return (snapshot != null) ? snapshot.getTimestamp() : new Date(0);
    }

    /**
	 * Get the textual dump of the last published snapshot
	 * @param groupType identifies the group by type 
	 * @return the textual dump of the last published snapshot or null if none exists
	 */
    public String getLastPublishedSnapshotDump(String groupType) {
        Object snapshot = _model.getSessionModel(groupType).getLastPublishedSnapshot();
        return (snapshot != null) ? snapshot.toString() : "";
    }
}
