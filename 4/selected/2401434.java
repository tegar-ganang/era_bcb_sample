package gov.sns.apps.lossviewer2;

import gov.sns.ca.*;
import java.util.logging.*;

public class EpicsDispatcher extends Dispatcher implements ConnectionListener, IEventSinkValTime {

    private ChannelFactory cf;

    private Channel chan;

    private Monitor triggerMonitor;

    private String triggerName;

    public EpicsDispatcher(String triggerName) {
        super();
        this.triggerName = triggerName;
    }

    public void startTrigger() {
        if (cf == null) cf = ChannelFactory.defaultFactory();
        chan = cf.getChannel(triggerName);
        chan.addConnectionListener(this);
        chan.requestConnection();
    }

    public void eventValue(ChannelTimeRecord record, Channel chan) {
        fireUpdate();
    }

    public void close() {
        chan.disconnect();
        super.close();
    }

    public void connectionMade(Channel channel) {
        try {
            Logger.getLogger(this.getClass().getCanonicalName()).log(Level.INFO, ("Trigger connected " + channel.channelName()));
            if (triggerMonitor == null) triggerMonitor = channel.addMonitorValTime(this, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connectionDropped(Channel channel) {
        Logger.getLogger(this.getClass().getCanonicalName()).log(Level.WARNING, ("Trigger connection dropped " + channel.channelName()));
    }
}
