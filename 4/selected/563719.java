package gov.sns.services.tripmonitor;

import java.util.logging.*;
import gov.sns.ca.*;
import gov.sns.tools.messaging.MessageCenter;

/** monitor a single channel */
public class ChannelMonitor {

    /** synchronization lock */
    protected Object EVENT_LOCK;

    /** event message center */
    protected MessageCenter MESSAGE_CENTER;

    /** proxy for posting channel events */
    protected ChannelEventListener EVENT_PROXY;

    /** PV channel */
    protected final Channel CHANNEL;

    /** event monitor */
    protected Monitor _monitor;

    /** last record captured */
    protected ChannelTimeRecord _lastRecord;

    /** trip count */
    protected int _tripCount;

    /** connection listener */
    protected ConnectionListener _connectionListener;

    /** constructor */
    public ChannelMonitor(final String pv) {
        MESSAGE_CENTER = new MessageCenter("Channel Monitor");
        EVENT_PROXY = MESSAGE_CENTER.registerSource(this, ChannelEventListener.class);
        EVENT_LOCK = new Object();
        CHANNEL = ChannelFactory.defaultFactory().getChannel(pv);
        _lastRecord = null;
        _connectionListener = null;
        clearTripCount();
    }

    /**
	 * Register the listener as a receiver of channel events from the wrapped channel
	 * @param listener  The listener to receive channel events
	 */
    public void addChannelEventListener(ChannelEventListener listener) {
        MESSAGE_CENTER.registerTarget(listener, this, ChannelEventListener.class);
        if (CHANNEL != null) {
            boolean isConnected;
            ChannelTimeRecord lastRecord;
            synchronized (EVENT_LOCK) {
                isConnected = isConnected();
                lastRecord = _lastRecord;
            }
            listener.connectionChanged(this, isConnected);
            if (lastRecord != null) {
                listener.valueChanged(this, lastRecord);
            }
        }
    }

    /**
	 * Unregister the listener as a receiver of channel events from the wrapped channel
	 * @param listener  The listener to unregister from receiving channel events
	 */
    public void removeChannelEventListener(ChannelEventListener listener) {
        MESSAGE_CENTER.removeTarget(listener, this, ChannelEventListener.class);
    }

    /**
		* Get the PV for the channel being wrapped.
	 *
	 * @return   the PV
	 */
    public String getPV() {
        return CHANNEL.channelName();
    }

    /**
	 * Get the wrapped channel.
	 * @return   the wrapped channel
	 */
    public Channel getChannel() {
        return CHANNEL;
    }

    /** get a description of this channel monitor */
    @Override
    public String toString() {
        return getPV();
    }

    /**
	 * Determine if the channel is connected.
	 * @return   true if the channel is connected and false if not.
	 */
    public boolean isConnected() {
        return CHANNEL.isConnected();
    }

    /**
		* Get the latest record
	 * @return the latest record
	 */
    public ChannelTimeRecord getLatestRecord() {
        synchronized (EVENT_LOCK) {
            return _lastRecord;
        }
    }

    /**
	 * Get the latest value as an integer.
	 * @return the latest value or -1 if there is none.
	 */
    public int getTripCount() {
        return _tripCount;
    }

    /**
	 * Update the trip count with the given record
	 * @param record the record from which to get the latest trip count
	 */
    protected void updateTripCount(final ChannelTimeRecord record) {
        boolean postTrip;
        int tripCount;
        synchronized (EVENT_LOCK) {
            final int initialTripCount = _tripCount;
            tripCount = record.intValue();
            postTrip = initialTripCount >= 0 && tripCount > initialTripCount && tripCount > 0;
            _tripCount = tripCount;
        }
        if (postTrip) {
            EVENT_PROXY.handleTrip(this, new TripRecord(getPV(), record.getTimestamp(), tripCount));
        }
    }

    /** clear the trip count */
    protected void clearTripCount() {
        synchronized (EVENT_LOCK) {
            _tripCount = -1;
        }
    }

    /** Request that the channel be connected. When the channel connection occurs, create a monitor. */
    public void requestConnection() {
        if (Main.isVerbose()) {
            System.out.println("Request connection for channel:  " + CHANNEL.channelName());
        }
        if (_connectionListener == null) {
            _connectionListener = new ConnectionListener() {

                /**
				 * Indicates that a connection to the specified channel has been established.
				 * @param channel  The channel which has been connected.
				 */
                public void connectionMade(final Channel channel) {
                    synchronized (EVENT_LOCK) {
                        _lastRecord = null;
                        if (_monitor == null) {
                            makeMonitor();
                        }
                    }
                    Main.printlnIfVerbose(channel + " is connected.");
                    EVENT_PROXY.connectionChanged(ChannelMonitor.this, true);
                }

                /**
				 * Indicates that a connection to the specified channel has been dropped.
				 * @param channel  The channel which has been disconnected.
				 */
                public void connectionDropped(final Channel channel) {
                    synchronized (EVENT_LOCK) {
                        _lastRecord = null;
                    }
                    Main.printlnIfVerbose(channel + " is disconnected.");
                    EVENT_PROXY.connectionChanged(ChannelMonitor.this, false);
                }
            };
            CHANNEL.addConnectionListener(_connectionListener);
        }
        if (!CHANNEL.isConnected()) {
            CHANNEL.requestConnection();
        }
    }

    /**
	 * Create a monitor to listen for new channel records. An instance of an internal anonymous
	 * class is the listener of the monitor events and caches the latest channel record.
	 */
    protected void makeMonitor() {
        try {
            _monitor = CHANNEL.addMonitorValTime(new IEventSinkValTime() {

                /**
				* handle the monitor event by caching the latest channel record
				* @param record   Description of the Parameter
				* @param channel  Description of the Parameter
				*/
                public void eventValue(final ChannelTimeRecord record, final Channel channel) {
                    synchronized (EVENT_LOCK) {
                        _lastRecord = record;
                    }
                    if (EVENT_PROXY != null) {
                        EVENT_PROXY.valueChanged(ChannelMonitor.this, record);
                    }
                    updateTripCount(record);
                }
            }, Monitor.VALUE);
            Main.printlnIfVerbose(CHANNEL.channelName() + " monitor made.");
        } catch (ConnectionException exception) {
            Logger.global.log(Level.SEVERE, "Connection exception.", exception);
            exception.printStackTrace();
        } catch (MonitorException exception) {
            Logger.global.log(Level.SEVERE, "Monitor exception.", exception);
            exception.printStackTrace();
        }
    }

    /** Dispose of the channel wrapper resources by clearing the monitor (if any) and disposing of the messaging resources. */
    public void dispose() {
        synchronized (EVENT_LOCK) {
            if (_connectionListener != null) {
                CHANNEL.removeConnectionListener(_connectionListener);
                _connectionListener = null;
            }
            if (_monitor != null) {
                _monitor.clear();
            }
            _monitor = null;
        }
    }
}
