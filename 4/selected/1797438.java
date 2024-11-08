package gov.sns.tools.pvlogger;

import gov.sns.ca.*;

/**
 * ChannelWrapper is a wrapper for a Channel that handles connecting to the channel and setting up a monitor
 * when its channel is connected.  Cache the latest channel record that has been found by the monitor.
 *
 * @author  tap
 */
public class ChannelWrapper {

    /** The channel to wrap */
    protected Channel _channel;

    /** The monitor for the channel */
    protected Monitor _monitor;

    /** The latest channel record found by the monitor */
    protected volatile ChannelTimeRecord _record;

    /** The handler handles channel connection events */
    protected ConnectionHandler _connectionHandler;

    /**
	 * ChannelWrapper constructor
	 * @param pv The PV for which to create a channel.
	 */
    public ChannelWrapper(String pv) {
        _channel = ChannelFactory.defaultFactory().getChannel(pv);
        _connectionHandler = new ConnectionHandler();
        _channel.addConnectionListener(_connectionHandler);
    }

    /**
	 * Dispose of the channel resources:  Shutdown the monitor if a monitor is active.
	 * Remove the connection handler as a connection listener of the channel.
	 */
    public void dispose() {
        _channel.removeConnectionListener(_connectionHandler);
        if (_channel.isConnected() && _monitor != null) {
            _monitor.clear();
            _monitor = null;
        }
    }

    /**
	 * Add the specified object as a listener of channel connection events of the
	 * channel that is wrapped.
	 * @param listener the object to add as a connection listener.
	 */
    public void addConnectionListener(ConnectionListener listener) {
        _channel.addConnectionListener(listener);
    }

    /**
	 * Remove the specified object from being a listener of channel connection events
	 * of the channel that is wrapped.
	 * @param listener the object to remove from being a connection listener
	 */
    public void removeConnectionListener(ConnectionListener listener) {
        _channel.removeConnectionListener(listener);
    }

    /**
	* Request that the channel be connected.  When the channel connection occurs, create a monitor.  If
	* a channel is dropped then clear the record.
	*/
    protected void requestConnection() {
        try {
            Channel.setSyncRequest(true);
            _channel.connect();
        } finally {
            Channel.setSyncRequest(false);
        }
    }

    /**
	* Create a monitor to listen for new channel records.  An instance of an internal anonymous class
	* is the listener of the monitor events and caches the latest channel record.
	*/
    protected void makeMonitor() {
        try {
            _monitor = _channel.addMonitorValTime(new IEventSinkValTime() {

                /** handle the monitor event by caching the latest channel record */
                public void eventValue(ChannelTimeRecord record, Channel chan) {
                    _record = record;
                }
            }, Monitor.VALUE);
        } catch (ConnectionException exception) {
            exception.printStackTrace();
        } catch (MonitorException exception) {
            exception.printStackTrace();
        }
    }

    /**
	* Get the PV for the channel being wrapped.
	* @return the PV
	*/
    public String getPV() {
        return _channel.channelName();
    }

    /**
	* Get the wrapped channel.
	* @return the wrapped channel
	*/
    public Channel getChannel() {
        return _channel;
    }

    /**
	* Get the latest channel record cached.
	* @return the latest channel record cached.
	*/
    public ChannelTimeRecord getRecord() {
        return _record;
    }

    /**
	 * Connection handler is a class whose instance listens for connection
	 * events of the wrapped channel.
	 */
    protected class ConnectionHandler implements ConnectionListener {

        /**
		 * Indicates that a connection to the specified channel has been established.
		 * If the monitor is null make a new monitor.
		 * @param channel The channel which has been connected.
		 */
        public void connectionMade(Channel channel) {
            if (_monitor == null) makeMonitor();
        }

        /**
		 * Indicates that a connection to the specified channel has been dropped.
		 * If the connection is dropped, clear the latest record.
		 * @param channel The channel which has been disconnected.
		 */
        public void connectionDropped(Channel channel) {
            _record = null;
        }
    }
}
