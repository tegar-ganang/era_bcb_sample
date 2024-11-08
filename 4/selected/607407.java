package jp.jparc.apps.monsampler;

import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;

public class BPMWERNewController extends MonitorController {

    private boolean debug;

    private Channel xRangeCh;

    private Channel xTimeCh;

    private Channel yRangeCh;

    private Channel yTimeCh;

    private String xRangeRec;

    private String xTimeRec;

    private String yRangeRec;

    private String yTimeRec;

    private static final int reclen = 2048;

    private String intervalRBRec;

    private String timeDelayRBRec;

    private CaMonitorScalar monIntervalRB;

    private CaMonitorScalar monTimeDelayRB;

    private boolean cagetFlag;

    private boolean caputFlag;

    public BPMWERNewController(String ID, DigitizerController digi, boolean caget, boolean caput, double reld) {
        super(ID, digi, reld);
        debug = false;
        xRangeRec = id + ":SET:X_VOLT_H_DT";
        xTimeRec = id + ":SET:X_VOLT_H_T0";
        yRangeRec = id + ":SET:Y_VOLT_H_DT";
        yTimeRec = id + ":SET:Y_VOLT_H_T0";
        intervalRBRec = id + ":RB:X_VOLT_WF_DT";
        timeDelayRBRec = id + ":RB:X_VOLT_WF_DELAY";
        cagetFlag = caget;
        caputFlag = caput;
        if (cagetFlag) {
            prepareRecords();
        }
    }

    protected void prepareRecords() {
        monIntervalRB = new CaMonitorScalar(intervalRBRec);
        monTimeDelayRB = new CaMonitorScalar(timeDelayRBRec);
    }

    @Override
    public double getTime(int sample) {
        return sampleToTime(sample);
    }

    @Override
    public int getSample(double time) {
        return timeToSample(time);
    }

    public double getInterval() {
        if (!cagetFlag) {
            return 5.0e-8;
        }
        double interval = monIntervalRB.getValue();
        return interval;
    }

    public int getRecLen() {
        return reclen;
    }

    public double getTimeDelay() {
        if (!cagetFlag) {
            return 0;
        }
        double timedelay = monTimeDelayRB.getValue();
        return timedelay;
    }

    public double sampleToTime(int sample) {
        double delay = getTimeDelay();
        double interval = getInterval();
        double time = sample * interval + delay;
        return time;
    }

    public int timeToSample(double time) {
        double delay = getTimeDelay();
        double interval = getInterval();
        double sample = (time - delay) / interval;
        return (int) Math.round(sample);
    }

    @Override
    public void setSignalRange(double time) {
        System.out.println("set " + xRangeRec + "to " + time);
        System.out.println("set " + yRangeRec + "to " + time);
        if (!caputFlag) {
            return;
        }
        xRangeCh = ChannelFactory.defaultFactory().getChannel(xRangeRec);
        yRangeCh = ChannelFactory.defaultFactory().getChannel(yRangeRec);
        CaMonitorScalar.setChannel(xRangeCh, time);
        CaMonitorScalar.setChannel(yRangeCh, time);
    }

    @Override
    public void setSignalTime(double time) {
        System.out.println("set " + xTimeRec + "to " + (time + relDelay));
        System.out.println("set " + yTimeRec + "to " + (time + relDelay));
        if (!caputFlag) {
            return;
        }
        xTimeCh = ChannelFactory.defaultFactory().getChannel(xTimeRec);
        yTimeCh = ChannelFactory.defaultFactory().getChannel(yTimeRec);
        CaMonitorScalar.setChannel(xTimeCh, time + relDelay);
        CaMonitorScalar.setChannel(yTimeCh, time + relDelay);
    }

    public void setBGRange(double time) {
    }

    public void setBGTime(double time) {
    }
}
