package jp.jparc.apps.monsampler;

import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;

public class WE7118Controller extends DigitizerController {

    private String intervalRBRec;

    private String intervalSetRec;

    private String recLenRBRec;

    private String recLenSetRec;

    private String timeDelayRBRec;

    private String timeDelaySetRec;

    private String daqSetRec;

    Channel intervalSetCh;

    Channel recLenSetCh;

    Channel timeDelaySetCh;

    Channel daqSetCh;

    private CaMonitorScalar monIntervalRB;

    private CaMonitorScalar monRecLenRB;

    private CaMonitorScalar monTimeDelayRB;

    boolean cagetFlag;

    boolean caputFlag;

    public WE7118Controller(String ID, boolean caget, boolean caput) {
        super(ID);
        intervalRBRec = id + ":RB:INTERVAL";
        intervalSetRec = id + ":SET:INTERVAL";
        timeDelayRBRec = id + ":RB:TRGDELAY";
        timeDelaySetRec = id + ":SET:TRGDELAY";
        recLenRBRec = id + ":RB:RECLEN";
        recLenSetRec = id + ":SET:RECLEN";
        daqSetRec = id + ":OPE:DAQ";
        cagetFlag = caget;
        caputFlag = caput;
        if (cagetFlag) {
            prepareRecords();
        }
    }

    @Override
    public void daqOn() {
        daqSetCh = ChannelFactory.defaultFactory().getChannel(daqSetRec);
        CaMonitorScalar.setChannel(daqSetCh, 1);
    }

    @Override
    public void daqOff() {
        daqSetCh = ChannelFactory.defaultFactory().getChannel(daqSetRec);
        CaMonitorScalar.setChannel(daqSetCh, 0);
    }

    public void setInterval(double interval) {
        System.out.println("set digi " + id + " interval to " + interval);
        if (!caputFlag) {
            return;
        }
        daqOff();
        intervalSetCh = ChannelFactory.defaultFactory().getChannel(intervalSetRec);
        CaMonitorScalar.setChannel(intervalSetCh, interval);
        daqOn();
    }

    public void setTimeDelay(double delay) {
        System.out.println("set digi " + id + " timedelay to " + delay);
        if (!caputFlag) {
            return;
        }
        daqOff();
        timeDelaySetCh = ChannelFactory.defaultFactory().getChannel(timeDelaySetRec);
        CaMonitorScalar.setChannel(timeDelaySetCh, delay);
    }

    @Override
    protected void prepareRecords() {
        monIntervalRB = new CaMonitorScalar(intervalRBRec);
        monRecLenRB = new CaMonitorScalar(recLenRBRec);
        monTimeDelayRB = new CaMonitorScalar(timeDelayRBRec);
    }

    @Override
    public double getInterval() {
        if (!cagetFlag) {
            return 5.0e-7;
        }
        double interval = monIntervalRB.getValue();
        return interval;
    }

    @Override
    public int getRecLen() {
        if (!cagetFlag) {
            return 2000;
        }
        int reclen = (int) monRecLenRB.getValue();
        return reclen;
    }

    @Override
    public double getTimeDelay() {
        if (!cagetFlag) {
            return 0;
        }
        double timedelay = monTimeDelayRB.getValue();
        return timedelay;
    }
}
