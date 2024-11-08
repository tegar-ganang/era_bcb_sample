package gov.sns.apps.slacs;

import gov.sns.ca.*;
import gov.sns.xal.smf.impl.SCLCavity;

public abstract class ChannelHandler implements IEventSinkValue, ConnectionListener {

    /** The channel object */
    protected Channel channel;

    /** the cavity */
    protected SCLCavity cavity;

    /** the controller to send action commands to */
    protected Controller controller;

    /** the value that this channel has */
    protected double value;

    /** the monitor for this channel */
    protected Monitor monitor;

    /** the constructor */
    public ChannelHandler(String name, SCLCavity cav, Controller cont) {
        cavity = cav;
        controller = cont;
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
    protected void makeMonitor() {
        try {
            monitor = channel.addMonitorValue(this, Monitor.VALUE);
        } catch (ConnectionException exc) {
        } catch (MonitorException exc) {
        }
    }

    /** The Connection Listener interface */
    public abstract void connectionMade(Channel chan);

    /** ConnectionListener interface */
    public void connectionDropped(Channel aChannel) {
        System.out.println("Channel dropped " + aChannel.channelName());
    }

    /** interface method for IEventSinkVal 
	* customize for each type PV */
    public abstract void eventValue(ChannelRecord newRecord, Channel chan);
}
