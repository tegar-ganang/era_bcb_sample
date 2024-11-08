package gov.sns.apps.lossviewer2.signals;

import gov.sns.ca.*;

public class CASignal extends AbstractSignal implements ConnectionListener, IEventSinkValTime {

    public void start() {
        if (cf == null) {
            cf = ChannelFactory.defaultFactory();
        }
        chan = cf.getChannel(getName());
        chan.addConnectionListener(this);
        chan.requestConnection();
    }

    private Channel chan = null;

    private int count = 0;

    public boolean setValue(double hv) {
        boolean result = false;
        try {
            result = chan.writeAccess();
            if (result) chan.putVal(hv);
        } catch (Exception ex) {
            result = false;
        }
        return result;
    }

    private double value = 0;

    Object lock = new Object();

    public void eventValue(ChannelTimeRecord record, Channel chan) {
        long tst = (long) (record.getTimestamp().getSeconds() * 1000);
        double v = record.doubleValue();
        dispatcher.processNewValue(new ScalarSignalValue(this, tst, v));
    }

    public double doubleValue() {
        synchronized (lock) {
            return value;
        }
    }

    Monitor monitor = null;

    public void connectionMade(Channel channel) {
        try {
            count = channel.elementCount();
            if (monitor == null) {
                monitor = channel.addMonitorValTime(this, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void connectionDropped(Channel channel) {
    }

    private ChannelFactory cf = null;

    public CASignal() {
        this(null);
    }

    public CASignal(String n) {
        setName(n);
    }

    public void close() {
        if (chan != null) {
            chan.disconnect();
            chan.flushIO();
        }
    }
}
