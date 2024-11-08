package jp.jparc.apps.monsampler;

import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;

public class BLMWERNewController extends MonitorController {

    private boolean debug;

    private Channel sigRangeCh;

    private Channel sigTimeCh;

    private Channel bgRangeCh;

    private Channel bgTimeCh;

    private String sigRangeRec;

    private String sigTimeRec;

    private String bgRangeRec;

    private String bgTimeRec;

    private static final int reclen = 2048;

    private String intervalRBRec;

    private String timeDelayRBRec;

    private CaMonitorScalar monIntervalRB;

    private CaMonitorScalar monTimeDelayRB;

    private boolean cagetFlag;

    private boolean caputFlag;

    public BLMWERNewController(String ID, DigitizerController digi, boolean caget, boolean caput, double reld) {
        super(ID, digi, reld);
        debug = false;
        sigRangeRec = id + ":SET:DIFF_VOLT_H_DT";
        sigTimeRec = id + ":SET:DIFF_VOLT_H_T0";
        bgRangeRec = id + ":SET:DIFF_VOLT_L_DT";
        bgTimeRec = id + ":SET:DIFF_VOLT_L_T0";
        intervalRBRec = id + ":RB:DIFF_VOLT_WF_DT";
        timeDelayRBRec = id + ":RB:DIFF_VOLT_WF_DELAY";
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
        System.out.println("set " + sigRangeRec + "to " + time);
        if (!caputFlag) {
            return;
        }
        sigRangeCh = ChannelFactory.defaultFactory().getChannel(sigRangeRec);
        CaMonitorScalar.setChannel(sigRangeCh, time);
    }

    @Override
    public void setSignalTime(double time) {
        System.out.println("set " + sigTimeRec + "to " + (time + relDelay));
        if (!caputFlag) {
            return;
        }
        sigTimeCh = ChannelFactory.defaultFactory().getChannel(sigTimeRec);
        CaMonitorScalar.setChannel(sigTimeCh, time + relDelay);
    }

    public void setBGRange(double time) {
        System.out.println("set " + bgRangeRec + "to " + time);
        if (!caputFlag) {
            return;
        }
        bgRangeCh = ChannelFactory.defaultFactory().getChannel(bgRangeRec);
        CaMonitorScalar.setChannel(bgRangeCh, time);
    }

    public void setBGTime(double time) {
        System.out.println("set " + bgTimeRec + "to " + (time + relDelay));
        if (!caputFlag) {
            return;
        }
        bgTimeCh = ChannelFactory.defaultFactory().getChannel(bgTimeRec);
        CaMonitorScalar.setChannel(bgTimeCh, time + relDelay);
    }
}
