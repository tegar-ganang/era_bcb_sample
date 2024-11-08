package jp.jparc.apps.monsampler;

import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;

public class FCTController extends MonitorController {

    private boolean debug;

    private Channel sigRangeCh;

    private Channel sigTimeCh;

    private String sigRangeRec;

    private String sigTimeRec;

    private boolean cagetFlag;

    private boolean caputFlag;

    public FCTController(String ID, DigitizerController digi, boolean caget, boolean caput, double reld) {
        super(ID, digi, reld);
        debug = false;
        sigRangeRec = id + ":SET:SIG_RANGE";
        sigTimeRec = id + ":SET:SIG_START";
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
}
