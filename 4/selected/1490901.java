package gov.sns.tools.pvlogger;

import gov.sns.tools.messaging.MessageCenter;
import gov.sns.ca.*;
import javax.swing.Timer;
import java.util.*;
import java.util.logging.*;

/**
 * LoggerSession manages a session of logging machine state.  One can create an instance to log the current machine
 * state either on demand or periodically.
 *
 * @author  tap
 */
public class LoggerSession {

    protected final int INITIAL_DELAY = 1000;

    /** default logging period in seconds */
    protected static final double DEFAULT_LOGGING_PERIOD = 5.0;

    protected MessageCenter messageCenter;

    protected LoggerChangeListener changeProxy;

    protected ChannelGroup _group;

    protected Timer logTimer;

    protected StateStore _stateStore;

    protected boolean _enabled;

    protected int iter;

    /**
	 * LoggerSession constructor
	 * @param group Group of channels to log. 
	 * @param stateStore The persistent storage to which to log machine state.
	 */
    public LoggerSession(final ChannelGroup group, final StateStore stateStore) {
        _stateStore = stateStore;
        _group = group;
        _group.requestConnections();
        _enabled = false;
        messageCenter = new MessageCenter("PV Logger");
        changeProxy = messageCenter.registerSource(this, LoggerChangeListener.class);
        iter = 0;
    }

    /**
	 * Add a logger change listener to receive logger change events.
	 * @param listener The listener of the logger change events.
	 */
    public void addLoggerChangeListener(LoggerChangeListener listener) {
        messageCenter.registerTarget(listener, this, LoggerChangeListener.class);
    }

    /**
	 * Remove a logger change listener from receiving logger change events.
	 * @param listener The listener of the logger change events.
	 */
    public void removeLoggerChangeListener(LoggerChangeListener listener) {
        messageCenter.removeTarget(listener, this, LoggerChangeListener.class);
    }

    /**
	 * Resume periodic logging with the most recent settings.
	 */
    public void resumeLogging() {
        if (isEnabled()) {
            logTimer.start();
            changeProxy.stateChanged(this, LoggerChangeListener.LOGGING_CHANGED);
        }
    }

    /**
	 * Start periodically logging machine state to the persistent storage.
	 * @param period The period in seconds between events where we take and store machine snapshots.
	 */
    public void startLogging() {
        final String message = "Start logging \"" + _group.getLabel() + "\" with period " + _group.getDefaultLoggingPeriod();
        System.out.println(message);
        Logger.getLogger("global").log(Level.INFO, message);
        startLogging(_group.getDefaultLoggingPeriod());
    }

    /**
	 * Start periodically logging machine state to the persistent storage.
	 * @param period The period in seconds between events where we take and store machine snapshots.
	 */
    public void startLogging(double period) {
        setLoggingPeriod(period);
        resumeLogging();
    }

    /** 
	 * Make a new log timer
	 * @param period The timer period in seconds.
	 */
    protected void makeLogTimer(double period) {
        int msecPeriod = (int) (1000 * period);
        System.out.println("** period, msecPeriod = " + period + " " + msecPeriod);
        logTimer = new Timer(msecPeriod, new java.awt.event.ActionListener() {

            public final void actionPerformed(java.awt.event.ActionEvent event) {
                MessageCenter.dispose();
                try {
                    MachineSnapshot snapshot = takeSnapshot();
                    publishSnapshot(snapshot);
                } catch (Exception exception) {
                    Logger.getLogger("global").log(Level.WARNING, "Error publishing snapshot: ", exception);
                    System.err.println(exception);
                }
                if (iter % 10 == 0) {
                    System.out.println("System.gc(), iter = " + iter);
                    System.gc();
                }
                iter++;
            }
        });
        logTimer.setRepeats(true);
        logTimer.setInitialDelay(INITIAL_DELAY);
    }

    /**
	 * Stop the periodic machine state logging.
	 */
    public void stopLogging() {
        logTimer.stop();
        changeProxy.stateChanged(this, LoggerChangeListener.LOGGING_CHANGED);
    }

    /**
	 * Reveal whether the logger is scheduled to run periodically
	 * @return true if the logger is scheduled to run periodically or false otherwise
	 */
    public boolean isLogging() {
        return (logTimer == null) ? false : logTimer.isRunning();
    }

    /**
	 * Set the period between events where we take and store machine snapshots.
	 * @param period The period in seconds between events where we take and store machine snapshots.
	 */
    public void setLoggingPeriod(double period) {
        setEnabled(period > 0);
        if (logTimer == null) {
            makeLogTimer(period);
        } else {
            int msecPeriod = (int) (1000 * period);
            logTimer.setDelay(msecPeriod);
        }
        changeProxy.stateChanged(this, LoggerChangeListener.LOGGING_PERIOD_CHANGED);
    }

    /**
	 * Get the loggin period.
	 * @return The period in seconds between events where we take and store machine snapshots.
	 */
    public double getLoggingPeriod() {
        if (logTimer == null) {
            makeLogTimer(DEFAULT_LOGGING_PERIOD);
            return DEFAULT_LOGGING_PERIOD;
        } else {
            return (logTimer.getDelay()) / 1000.0;
        }
    }

    /**
	 * Determine whether this logger session is enabled
	 * @return true if this logger session is enabled and false if not
	 */
    public boolean isEnabled() {
        return _enabled;
    }

    /**
	 * Set whether this session should be enabled
	 * @param enable true to enable this session and false to disable it 
	 */
    protected void setEnabled(boolean enable) {
        _enabled = enable;
        if (isLogging()) stopLogging();
        changeProxy.stateChanged(this, LoggerChangeListener.ENABLE_CHANGED);
    }

    /**
	 * Get the active channel group for this session
	 * @return the channel group
	 */
    public ChannelGroup getChannelGroup() {
        return _group;
    }

    /**
	 * Get the state store for this logger session
	 * @return the state store
	 */
    public StateStore getStateStore() {
        return _stateStore;
    }

    /**
	 * Set the channel group for this logger session
	 * @param group the new channel group for this logger session
	 */
    public void setChannelGroup(ChannelGroup group) {
        boolean shouldLog = isLogging();
        if (shouldLog) stopLogging();
        if (group != null && group != _group) group.dispose();
        _group = group;
        if (shouldLog) resumeLogging();
        changeProxy.stateChanged(this, LoggerChangeListener.GROUP_CHANGED);
    }

    /**
	 * Get the channels which we are attempting to monitor and log
	 * @return a collection of the channels we wish to monitor and log
	 */
    public Collection<Channel> getChannels() {
        return _group.getChannels();
    }

    public MachineSnapshot takeSnapshot() {
        if (_group instanceof ChannelBufferGroup) {
            return takeSnapShotBuffer();
        } else {
            return takeSnapShotNoBuffer();
        }
    }

    public synchronized MachineSnapshot takeSnapShotBuffer() {
        final ChannelWrapper[] channelWrappers = _group.getChannelWrappers();
        ArrayList<ChannelSnapshot> snapshots = new ArrayList<ChannelSnapshot>();
        int ichannel = 0;
        for (int index = 0; index < channelWrappers.length; index++) {
            ChannelBufferWrapper channelWrapper = (ChannelBufferWrapper) channelWrappers[index];
            List<ChannelTimeRecord> records = channelWrapper.getRecords();
            for (int j = 0; j < records.size(); j++) {
                ChannelTimeRecord record = records.get(j);
                if (record == null) continue;
                ChannelSnapshot snapshot = new ChannelSnapshot(channelWrapper.getPV(), record);
                if (records.size() > 1) {
                    System.out.println("update " + ichannel + " : " + snapshot.getPV() + " value = " + snapshot.getValue()[0] + " time = " + snapshot.getTimestamp());
                }
                snapshots.add(snapshot);
                ichannel++;
            }
            channelWrapper.clear();
        }
        System.out.println("LoggerSession::takeSnapshotBuffer number of update records = " + ichannel);
        if (snapshots.size() == 0) {
            return null;
        }
        ChannelSnapshot[] channelSnapshots = snapshots.toArray(new ChannelSnapshot[snapshots.size()]);
        MachineSnapshot machineSnapshot = new MachineSnapshot(new Date(), "", channelSnapshots);
        machineSnapshot.setType(_group.getLabel());
        changeProxy.snapshotTaken(this, machineSnapshot);
        System.out.println("current time (ms) = " + System.currentTimeMillis());
        return machineSnapshot;
    }

    public MachineSnapshot takeSnapShotNoBuffer() {
        final ChannelWrapper[] channelWrappers = _group.getChannelWrappers();
        MachineSnapshot machineSnapshot = new MachineSnapshot(channelWrappers.length);
        machineSnapshot.setType(_group.getLabel());
        for (int index = 0; index < channelWrappers.length; index++) {
            ChannelWrapper channelWrapper = channelWrappers[index];
            if (channelWrapper == null) continue;
            ChannelTimeRecord record = channelWrapper.getRecord();
            if (record == null) continue;
            ChannelSnapshot snapshot = new ChannelSnapshot(channelWrapper.getPV(), record);
            machineSnapshot.setChannelSnapshot(index, snapshot);
        }
        changeProxy.snapshotTaken(this, machineSnapshot);
        System.out.println("current time (ms) = " + System.currentTimeMillis());
        return machineSnapshot;
    }

    /**
	 * Publish the machine snapshot to the persistent storage.
	 * @param machineSnapshot The machine snapshot to publish.
	 */
    public final void publishSnapshot(MachineSnapshot machineSnapshot) {
        _stateStore.publish(machineSnapshot);
        changeProxy.snapshotPublished(this, machineSnapshot);
    }
}
