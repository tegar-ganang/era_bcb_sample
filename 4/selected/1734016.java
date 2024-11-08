package jp.jparc.apps.monsampler;

import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;

public class SCTController extends MonitorController {

    private boolean debug;

    private Channel sigRangeCh;

    private Channel sigTimeCh;

    private Channel bgRangeCh;

    private Channel bgTimeCh;

    private String sigRangeRec;

    private String sigTimeRec;

    private String bgRangeRec;

    private String bgTimeRec;

    private boolean cagetFlag;

    private boolean caputFlag;

    public SCTController(String ID, DigitizerController digi, boolean caget, boolean caput, double reld) {
        super(ID, digi, reld);
        debug = false;
        sigRangeRec = id + ":SET:SIG_RANGE";
        sigTimeRec = id + ":SET:SIG_START";
        bgRangeRec = id + ":SET:BG_RANGE";
        bgTimeRec = id + ":SET:BG_START";
        cagetFlag = caget;
        caputFlag = caput;
    }

    @Override
    public void setSignalRange(double time) {
        int sample = this.getSampleNoDelay(time);
        if (debug) {
            System.out.println("setting " + id + " signal range to " + time + " sec (" + sample + ")(sample)");
        } else {
            setSignalRangeInSample(sample);
        }
    }

    private void setSignalRangeInSample(int sample) {
        System.out.println("set " + sigRangeRec + "to " + sample);
        if (!caputFlag) {
            return;
        }
        sigRangeCh = ChannelFactory.defaultFactory().getChannel(sigRangeRec);
        CaMonitorScalar.setChannel(sigRangeCh, sample);
    }

    @Override
    public void setSignalTime(double time) {
        int sample = this.getSample(time + relDelay);
        if (debug) {
            System.out.println("setting " + id + " signal time to " + time + " sec (" + sample + ")(sample)");
        } else {
            setSignalTimeInSample(sample);
        }
    }

    private void setSignalTimeInSample(int sample) {
        System.out.println("set " + sigTimeRec + "to " + sample);
        if (!caputFlag) {
            return;
        }
        sigTimeCh = ChannelFactory.defaultFactory().getChannel(sigTimeRec);
        CaMonitorScalar.setChannel(sigTimeCh, sample);
    }

    public void setBGRange(double time) {
        int sample = this.getSampleNoDelay(time);
        if (debug) {
            System.out.println("setting " + id + " bg range to " + time + " sec (" + sample + ")(sample)");
        } else {
            setBGRangeInSample(sample);
        }
    }

    private void setBGRangeInSample(int sample) {
        System.out.println("set " + bgRangeRec + "to " + sample);
        if (!caputFlag) {
            return;
        }
        bgRangeCh = ChannelFactory.defaultFactory().getChannel(bgRangeRec);
        CaMonitorScalar.setChannel(bgRangeCh, sample);
    }

    public void setBGTime(double time) {
        int sample = this.getSample(time);
        if (debug) {
            System.out.println("setting " + id + " bg time to " + time + " sec (" + sample + ")(sample)");
        } else {
            setBGTimeInSample(sample);
        }
    }

    private void setBGTimeInSample(int sample) {
        System.out.println("set " + bgTimeRec + "to " + sample);
        if (!caputFlag) {
            return;
        }
        bgTimeCh = ChannelFactory.defaultFactory().getChannel(bgTimeRec);
        CaMonitorScalar.setChannel(bgTimeCh, sample);
    }
}
