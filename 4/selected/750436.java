package jp.jparc.apps.monsampler;

import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;

public class BPMController extends MonitorController {

    private boolean debug;

    private Channel xRangeCh;

    private Channel xTimeCh;

    private Channel yRangeCh;

    private Channel yTimeCh;

    private String xRangeRec;

    private String xTimeRec;

    private String yRangeRec;

    private String yTimeRec;

    private boolean cagetFlag;

    private boolean caputFlag;

    public BPMController(String ID, DigitizerController digi, boolean caget, boolean caput, double reld) {
        super(ID, digi, reld);
        debug = false;
        xRangeRec = id + ":SET:X_SIG_RANGE";
        xTimeRec = id + ":SET:X_SIG_START";
        yRangeRec = id + ":SET:Y_SIG_RANGE";
        yTimeRec = id + ":SET:Y_SIG_START";
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
        System.out.println("set " + xRangeRec + "to " + sample);
        System.out.println("set " + yRangeRec + "to " + sample);
        if (!caputFlag) {
            return;
        }
        xRangeCh = ChannelFactory.defaultFactory().getChannel(xRangeRec);
        yRangeCh = ChannelFactory.defaultFactory().getChannel(yRangeRec);
        CaMonitorScalar.setChannel(xRangeCh, sample);
        CaMonitorScalar.setChannel(yRangeCh, sample);
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
        System.out.println("set " + xTimeRec + "to " + sample);
        System.out.println("set " + yTimeRec + "to " + sample);
        if (!caputFlag) {
            return;
        }
        xTimeCh = ChannelFactory.defaultFactory().getChannel(xTimeRec);
        yTimeCh = ChannelFactory.defaultFactory().getChannel(yTimeRec);
        CaMonitorScalar.setChannel(xTimeCh, sample);
        CaMonitorScalar.setChannel(yTimeCh, sample);
    }
}
