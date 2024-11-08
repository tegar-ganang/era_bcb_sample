package gov.sns.apps.diagnostics.corede.corede;

import gov.sns.ca.*;

public class PVInput extends AbstractDataInput implements IEventSinkValTime {

    protected static ChannelFactory channelFactory;

    static {
        channelFactory = Utilities.getChannelFactory();
    }

    private Channel chan;

    private Object lock = new Object();

    private double theValue;

    private double lockedValue;

    private long lockedTst;

    private long theTst;

    /**
	 * Method eventValue
	 *
	 * @param    p1                  a  ChannelTimeRecord
	 * @param    p2                  a  Channel
	 *
	 */
    public void eventValue(ChannelTimeRecord chrec, Channel chan) {
        synchronized (lock) {
            theValue = chrec.doubleValue();
            theTst = chrec.getTimestamp().getTime() * 1000000;
        }
    }

    @Override
    protected void propagateValues() {
        synchronized (lock) {
            lockedValue = theValue;
            lockedTst = theTst;
        }
    }

    @Override
    public void processData() throws CoredeException {
        outputs[0].setValue(lockedValue, lockedTst);
    }

    public PVInput(String s) {
        super(s);
        try {
            chan = channelFactory.getChannel(s);
            chan.connect();
            chan.addMonitorValTime(this, 1);
        } catch (ConnectionException e) {
            e.printStackTrace();
        } catch (MonitorException e) {
            e.printStackTrace();
        }
    }
}
