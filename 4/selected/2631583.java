package gov.sns.ca;

import gov.sns.tools.messaging.MessageCenter;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Connect to, monitor and cache a channel's monitor events. */
public class MonitorCache {

    /** Message center for dispatching monitor events to registered listeners. */
    protected final MessageCenter _messageCenter;

    /** Proxy which forwards monitor events to registered listeners. */
    protected final IEventSinkValTime _eventProxy;

    /** the channel to wrap */
    protected final Channel _channel;

    /** listener to handle connection events */
    protected final ConnectionListener _connectionHandler;

    /** listener to handle monitor events */
    protected final IEventSinkValTime _monitorEventHandler;

    /** a channel monitor */
    protected Monitor _monitor;

    /** latest monitor event */
    protected volatile ChannelTimeRecord _latestRecord;

    /** Constructor */
    public MonitorCache(final Channel channel) {
        _channel = channel;
        _monitor = null;
        _latestRecord = null;
        _messageCenter = new MessageCenter("Monitor Event Cache");
        _eventProxy = _messageCenter.registerSource(this, IEventSinkValTime.class);
        _monitorEventHandler = new MonitorEventHandler();
        _connectionHandler = new ConnectionEventHandler();
        _channel.addConnectionListener(_connectionHandler);
    }

    /** Dispose of this wrapper's resources. */
    public void dispose() {
        if (_channel != null) {
            _channel.removeConnectionListener(_connectionHandler);
        }
        _messageCenter.removeSource(this, IEventSinkValTime.class);
        if (_monitor != null) {
            _monitor.clear();
            _monitor = null;
        }
    }

    /** Register the listener to receive IEventSinkValTime events from the monitor */
    public void addMonitorListener(final IEventSinkValTime listener) {
        _messageCenter.registerTarget(listener, this, IEventSinkValTime.class);
    }

    /** Remove the listener from receiving IEventSinkValTime events from this monitor */
    public void removeMonitorListener(final IEventSinkValTime listener) {
        _messageCenter.removeTarget(listener, this, IEventSinkValTime.class);
    }

    /** Request a connection and start the monitor upon connection. */
    public void requestMonitor() {
        _channel.requestConnection();
    }

    /** Get the channel. */
    public Channel getChannel() {
        return _channel;
    }

    /** Determine if the channel is connected. */
    public boolean isConnected() {
        return _channel.isConnected();
    }

    /** Get the latest record. */
    public ChannelTimeRecord getLatestRecord() {
        return _latestRecord;
    }

    /** Handle monitor events */
    protected class MonitorEventHandler implements IEventSinkValTime {

        /** Handle the monitor event. */
        public void eventValue(final ChannelTimeRecord record, final Channel channel) {
            _latestRecord = record;
            _eventProxy.eventValue(record, channel);
        }
    }

    /** Handle connection events */
    protected class ConnectionEventHandler implements ConnectionListener {

        /**
         * Indicates that a connection to the specified channel has been established.
         * @param channel The channel which has been connected.
         */
        public void connectionMade(final Channel channel) {
            if (_monitor == null) {
                try {
                    _monitor = channel.addMonitorValTime(_monitorEventHandler, Monitor.VALUE);
                } catch (Exception exception) {
                    Logger.getLogger("global").log(Level.SEVERE, "Exception attempting to make a monitor.", exception);
                }
            }
        }

        /**
         * Indicates that a connection to the specified channel has been dropped.
         * @param channel The channel which has been disconnected.
         */
        public void connectionDropped(final Channel channel) {
            _latestRecord = null;
        }
    }
}
