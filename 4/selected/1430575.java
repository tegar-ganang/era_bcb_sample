package gov.sns.ca;

import gov.sns.tools.messaging.MessageCenter;
import java.util.logging.Level;
import java.util.logging.Logger;

/** 
 * Creates a monitor for a channel when the channel is connected and dispatches monitor and channel connection 
 * events to registered listeners. 
 * @author t6p
 */
public class MonitorController {

    /** the monitor mask to use when initializing the monitor (Monitor.VALUE, Monitor.LOG, Monitor.ALARM) */
    protected final int monitorMask;

    /** synchronization lock */
    protected final Object _eventLock;

    protected final int count;

    /** event message center */
    protected MessageCenter _messageCenter;

    /** proxy for posting channel events */
    protected MonitorEventListener _eventProxy;

    /** The channel to wrap */
    protected Channel _channel;

    /** The monitor for the channel */
    protected Monitor _monitor;

    /** last record captured */
    protected ChannelTimeRecord _lastRecord;

    /** connection listener */
    protected ConnectionListener _connectionListener;

    /**
     * Primary constructor.
     * @param channel  The channel to wrap.
     * @param monitorMask The monitor mask to apply when instantiating the monitor.
     */
    public MonitorController(final Channel channel, final int monitorMask) {
        this(channel, monitorMask, -1);
    }

    public MonitorController(final Channel channel, final int monitorMask, int count) {
        this.monitorMask = monitorMask;
        _eventLock = new Object();
        _channel = channel;
        _messageCenter = new MessageCenter();
        _eventProxy = _messageCenter.registerSource(this, MonitorEventListener.class);
        _lastRecord = null;
        this.count = count;
    }

    /**
     * Constructor using the default monitor mask (Monitor.VALUE).
     * @param channel  The channel to wrap.
     */
    public MonitorController(final Channel channel) {
        this(channel, Monitor.VALUE);
    }

    /**
     * Constructor creating a channel from the specified PV and applying the specified monitor mask.
     * @param pv  The PV for which to create a channel.
     * @param monitorMask The monitor mask to apply when instantiating the monitor.
     */
    public MonitorController(final String pv, final int monitorMask) {
        this(ChannelFactory.defaultFactory().getChannel(pv), monitorMask);
    }

    /**
     * Constructor creating a channel from the specified PV and using the default monitor mask (Monitor.VALUE).
     * @param pv  The PV for which to create a channel.
     */
    public MonitorController(final String pv) {
        this(pv, Monitor.VALUE);
    }

    /**
     * Register the listener as a receiver of channel events from this controller.
     * @param listener  The listener to receive channel events
     */
    public void addMonitorEventListener(final MonitorEventListener listener) {
        synchronized (_eventLock) {
            _messageCenter.registerTarget(listener, this, MonitorEventListener.class);
            if (_channel != null) {
                listener.connectionChanged(_channel, isConnected());
                if (_lastRecord != null) {
                    listener.valueChanged(_channel, _lastRecord);
                }
            }
        }
    }

    /**
     * Remove the listener as a receiver of channel events from this controller.
     * @param listener  The listener to remove from receiving channel events
     */
    public void removeMonitorEventListener(MonitorEventListener listener) {
        _messageCenter.removeTarget(listener, this, MonitorEventListener.class);
    }

    /**
     * Get the PV for the controlled channel.
     * @return   the PV
     */
    public String getPV() {
        return _channel.channelName();
    }

    /**
     * Get this instance's controlled channel.
     * @return   the wrapped channel
     */
    public Channel getChannel() {
        return _channel;
    }

    /**
     * Get this instance's desired count of elements in returned value.
     */
    public int getCount() {
        return count;
    }

    /**
     * Determine if the channel is connected.
     * @return   true if the channel is connected and false if not.
     */
    public boolean isConnected() {
        return _channel.isConnected();
    }

    /**
     * Get the latest channel record.
     * @return the latest channel record or null if none has been published or the channel is not connected.
     */
    public ChannelTimeRecord getLatestRecord() {
        synchronized (_eventLock) {
            return _lastRecord;
        }
    }

    /** Request that the channel be connected. When the channel connection occurs, create a monitor. */
    public void requestMonitor() {
        if (_connectionListener == null) {
            _connectionListener = new ConnectionListener() {

                /**
                 * Indicates that a connection to the specified channel has been established.
                 * @param channel  The channel which has been connected.
                 */
                public void connectionMade(Channel channel) {
                    synchronized (_eventLock) {
                        _lastRecord = null;
                        if (_monitor == null) {
                            makeMonitor();
                        }
                        _eventProxy.connectionChanged(channel, true);
                    }
                }

                /**
                 * Indicates that a connection to the specified channel has been dropped.
                 * @param channel  The channel which has been disconnected.
                 */
                public void connectionDropped(Channel channel) {
                    synchronized (_eventLock) {
                        _lastRecord = null;
                        _eventProxy.connectionChanged(channel, false);
                    }
                }
            };
            _channel.addConnectionListener(_connectionListener);
        }
        if (!_channel.isConnected()) {
            _channel.requestConnection();
        }
    }

    /**
     * Create a monitor to listen for new channel records. An instance of an internal, anonymous
     * class is the listener of the monitor events and caches the latest channel record.
     */
    protected void makeMonitor() {
        try {
            _monitor = _channel.addMonitorValTime(new IEventSinkValTime() {

                /**
                 * Handle the monitor event by caching the latest channel record.
                 * @param record   the monitor's posted data for the channel
                 * @param channel  the channel whose monitor has fired
                 */
                public void eventValue(final ChannelTimeRecord record, final Channel channel) {
                    synchronized (_eventLock) {
                        _lastRecord = record;
                        if (_eventProxy != null) {
                            _eventProxy.valueChanged(channel, record);
                        }
                    }
                }
            }, monitorMask, count);
        } catch (ConnectionException exception) {
            Logger.global.log(Level.SEVERE, "Connection exception.", exception);
            exception.printStackTrace();
        } catch (MonitorException exception) {
            Logger.global.log(Level.SEVERE, "Monitor exception.", exception);
            exception.printStackTrace();
        }
    }

    /**
     * Dispose of the channel wrapper resources by clearing the monitor (if any) and disposing of
     * of the messaging resources.
     */
    public void dispose() {
        synchronized (_eventLock) {
            if (_connectionListener != null) {
                _channel.removeConnectionListener(_connectionListener);
                _connectionListener = null;
            }
            if (_monitor != null) {
                _monitor.clear();
            }
            _eventProxy = null;
            _messageCenter = null;
            _monitor = null;
            _channel = null;
        }
    }
}
