package gov.sns.apps.mpscheckstatus;

import gov.sns.ca.*;
import gov.sns.tools.messaging.MessageCenter;

public class ChannelWrapper implements ConnectionListener {

    /** message center for distributing events */
    private MessageCenter _messageCenter;

    /** proxy for forwarding events */
    private CAValueListener _eventProxy;

    /** The channel to wrap */
    protected Channel channel;

    /** The monitor for the channel */
    protected Monitor _monitor;

    /** the value that this channel has */
    private volatile int value;

    /** indicates valid value */
    private boolean isValid;

    /** the constructor */
    public ChannelWrapper(final Channel chan) {
        _messageCenter = new MessageCenter("Channel Wrapper");
        _eventProxy = _messageCenter.registerSource(this, CAValueListener.class);
        channel = chan;
        isValid = false;
        channel.addConnectionListener(this);
        chan.requestConnection();
    }

    /** the constructor */
    public ChannelWrapper(final String name) {
        this(ChannelFactory.defaultFactory().getChannel(name));
    }

    /** add the listener as a receiver of CAValueListener events */
    public void addCAValueListener(final CAValueListener listener) {
        _messageCenter.registerTarget(listener, this, CAValueListener.class);
        if (isValid) {
            listener.newValue(this, value, isValid);
        }
    }

    /** remove the listener from receiving CAValueListener events */
    public void removeCAValueListener(final CAValueListener listener) {
        _messageCenter.removeTarget(listener, this, CAValueListener.class);
    }

    /** whether this channel is connected */
    public boolean isConnected() {
        return channel.isConnected();
    }

    /** return the channel */
    public Channel getChannel() {
        return channel;
    }

    /** returns the latest value from this Channel */
    public int getValue() {
        return value;
    }

    /** Determine if valid */
    public boolean isValid() {
        return isValid;
    }

    /** the name of the Channel */
    public String getName() {
        return channel.channelName();
    }

    /** The Connection Listener interface */
    public void connectionMade(Channel chan) {
        if (_monitor == null) makeMonitor();
    }

    /** ConnectionListener interface */
    public void connectionDropped(Channel aChannel) {
        System.out.println("Channel dropped " + aChannel.channelName());
        value = 0;
        isValid = false;
        _eventProxy.newValue(this, value, isValid);
    }

    /**
	* Create a monitor to listen for new channel records.  An instance of an internal anonymous class
	* is the listener of the monitor events and caches the latest channel record.
	*/
    protected void makeMonitor() {
        try {
            _monitor = channel.addMonitorValTime(new IEventSinkValTime() {

                /** handle the monitor event by caching the latest channel record */
                public void eventValue(ChannelTimeRecord record, Channel chan) {
                    value = record.intValue();
                    isValid = true;
                    _eventProxy.newValue(ChannelWrapper.this, value, isValid);
                }
            }, Monitor.VALUE);
        } catch (ConnectionException exception) {
            exception.printStackTrace();
        } catch (MonitorException exception) {
            exception.printStackTrace();
        }
    }
}
