package gov.sns.apps.pvlogscore;

import gov.sns.ca.*;

public class ChannelWrapper implements IEventSinkValTime, ConnectionListener {

    /** The channel object */
    private Channel channel;

    /** the value that this channel has */
    private double value = Double.NaN;

    /** stat **/
    private int stat = 0;

    /** svrty **/
    private int svrty = 0;

    /** the monitor for this channel */
    private Monitor monitor;

    /** has the channel delivered a value since the last call to getReady ?  - to be used if the monitor does not work out and we switch to a callback get */
    protected boolean gotValue = false;

    /** the constructor */
    public ChannelWrapper(Channel chan) {
        channel = chan;
        channel.addConnectionListener(this);
        channel.requestConnection();
    }

    /** the constructor */
    public ChannelWrapper(String name) {
        channel = ChannelFactory.defaultFactory().getChannel(name);
        channel.addConnectionListener(this);
        channel.requestConnection();
    }

    private void requestNewConnection(String name) {
        channel = ChannelFactory.defaultFactory().getChannel(name);
        channel.addConnectionListener(this);
        channel.requestConnection();
    }

    /** whether this channel is connected */
    protected boolean isConnected() {
        return channel.isConnected();
    }

    /** return the channel */
    protected Channel getChannel() {
        return channel;
    }

    /** returns the latest value from this Channel */
    protected double getValDbl() {
        return value;
    }

    /** the name of the Channel */
    protected String getId() {
        return channel.getId();
    }

    /** make a monitor connection to a channel */
    private void makeMonitor() {
        try {
            monitor = channel.addMonitorValTime(this, Monitor.VALUE);
        } catch (ConnectionException exc) {
        } catch (MonitorException exc) {
        }
    }

    /** The Connection Listener interface */
    public void connectionMade(Channel chan) {
        if (monitor == null) makeMonitor();
    }

    /** ConnectionListener interface */
    public void connectionDropped(Channel aChannel) {
        System.out.println("Channel dropped " + aChannel.channelName());
        value = Double.NaN;
        stat = 0;
        svrty = 0;
        System.out.println("requested reconnection");
        requestNewConnection(aChannel.channelName());
    }

    /** interface method for IEventSinkVal */
    public void eventValue(ChannelTimeRecord newRecord, Channel chan) {
        value = newRecord.doubleValue();
        stat = newRecord.status();
        svrty = newRecord.severity();
    }
}
