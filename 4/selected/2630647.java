package jp.jparc.apps.monsampler;

import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;
import java.util.HashMap;

public class WE7111Controller extends DigitizerController {

    private String timeDivRBRec;

    private String timeDivSetRec;

    private String recLenRBRec;

    private String recLenSetRec;

    private String timeDelayRBRec;

    private String timeDelaySetRec;

    private String daqSetRec;

    private Channel timeDivSetCh;

    private Channel recLenSetCh;

    private Channel timeDelaySetCh;

    private Channel daqSetCh;

    private CaMonitorScalar monTimeDivRB;

    private CaMonitorScalar monRecLenRB;

    private CaMonitorScalar monTimeDelayRB;

    private HashMap<Integer, Double> rawToTimeDiv;

    private HashMap<Integer, Integer> rawToRecLen;

    private boolean cagetFlag;

    private boolean caputFlag;

    public WE7111Controller(String ID, boolean caget, boolean caput) {
        super(ID);
        timeDivRBRec = id + ":RB:TIMDIV";
        timeDivSetRec = id + ":SET:TIMDIV";
        timeDelayRBRec = id + ":RB:TRGDELAY";
        timeDelaySetRec = id + ":SET:TRGDELAY";
        recLenRBRec = id + ":RB:RECLEN";
        recLenSetRec = id + ":SET:RECLEN";
        daqSetRec = id + ":OPE:DAQ";
        cagetFlag = caget;
        caputFlag = caput;
        if (cagetFlag) {
            prepareRecords();
            prepareTables();
        }
    }

    @Override
    public int timeToSample(double time) {
        double delay = getTimeDelay();
        double interval = getInterval();
        int reclen = getRecLen();
        double internalDelaySample = reclen / 2.;
        double sample = (time - delay) / interval + internalDelaySample;
        return (int) Math.round(sample);
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

    public void setTimeDiv(int index) {
        System.out.println("set digi " + id + " timediv to " + index);
        if (!caputFlag) {
            return;
        }
        daqOff();
        timeDivSetCh = ChannelFactory.defaultFactory().getChannel(timeDivSetRec);
        CaMonitorScalar.setChannel(timeDivSetCh, index);
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
        daqOn();
    }

    @Override
    protected void prepareRecords() {
        monTimeDivRB = new CaMonitorScalar(timeDivRBRec);
        monRecLenRB = new CaMonitorScalar(recLenRBRec);
        monTimeDelayRB = new CaMonitorScalar(timeDelayRBRec);
    }

    @Override
    public double getInterval() {
        if (!cagetFlag) {
            return 1.0e-6;
        }
        int timeDivRaw = (int) monTimeDivRB.getValue();
        System.out.println("timeDivRaw = " + timeDivRaw);
        double timediv = rawToTimeDiv.get(new Integer(timeDivRaw)).doubleValue();
        System.out.println("timediv = " + timediv);
        double interval = timediv / (getRecLen() / 10);
        System.out.println("id, timeDiv, recLen = " + id + " " + timediv + " " + getRecLen());
        return interval;
    }

    @Override
    public int getRecLen() {
        if (!cagetFlag) {
            return 1000;
        }
        int reclenRaw = (int) monRecLenRB.getValue();
        return rawToRecLen.get(new Integer(reclenRaw)).intValue();
    }

    @Override
    public double getTimeDelay() {
        if (!cagetFlag) {
            return 0;
        }
        double timedelay = monTimeDelayRB.getValue();
        return timedelay;
    }

    private void prepareTables() {
        rawToTimeDiv = new HashMap<Integer, Double>();
        rawToTimeDiv.put(new Integer(0), new Double(0.1));
        rawToTimeDiv.put(new Integer(1), new Double(0.05));
        rawToTimeDiv.put(new Integer(2), new Double(0.02));
        rawToTimeDiv.put(new Integer(3), new Double(0.01));
        rawToTimeDiv.put(new Integer(4), new Double(0.005));
        rawToTimeDiv.put(new Integer(5), new Double(0.002));
        rawToTimeDiv.put(new Integer(6), new Double(0.001));
        rawToTimeDiv.put(new Integer(7), new Double(0.0005));
        rawToTimeDiv.put(new Integer(8), new Double(0.0002));
        rawToTimeDiv.put(new Integer(9), new Double(0.0001));
        rawToTimeDiv.put(new Integer(10), new Double(0.00005));
        rawToTimeDiv.put(new Integer(11), new Double(0.00002));
        rawToTimeDiv.put(new Integer(12), new Double(0.00001));
        rawToTimeDiv.put(new Integer(13), new Double(0.000005));
        rawToTimeDiv.put(new Integer(14), new Double(0.000002));
        rawToTimeDiv.put(new Integer(15), new Double(0.000001));
        rawToRecLen = new HashMap<Integer, Integer>();
        rawToRecLen.put(new Integer(0), new Integer(1000));
        rawToRecLen.put(new Integer(1), new Integer(5000));
        rawToRecLen.put(new Integer(2), new Integer(10000));
    }
}
