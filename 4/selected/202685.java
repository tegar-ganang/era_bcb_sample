package jp.jparc.apps.monsampler;

import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;

public class WERBPMController extends DigitizerController {

    private Channel sigRangeCh;

    private Channel sigTimeCh;

    private String sigRangeRec;

    private String sigTimeRec;

    boolean cagetFlag;

    boolean caputFlag;

    public WERBPMController(String ID, boolean caget, boolean caput) {
        super(ID);
        sigRangeRec = ID + ":SET:H_DT";
        sigTimeRec = ID + ":SET:H_T0";
        cagetFlag = caget;
        caputFlag = caput;
        if (caputFlag) {
            prepareRecords();
        }
    }

    public void setSignalRange(double time) {
        System.out.println("set " + sigRangeRec + "to " + time);
        if (!caputFlag) {
            System.out.println("set " + sigRangeRec + "to " + time);
            return;
        }
        sigRangeCh = ChannelFactory.defaultFactory().getChannel(sigRangeRec);
        CaMonitorScalar.setChannel(sigRangeCh, time);
    }

    public void setSignalTime(double time) {
        System.out.println("set " + sigTimeRec + "to " + time);
        if (!caputFlag) {
            return;
        }
        sigTimeCh = ChannelFactory.defaultFactory().getChannel(sigTimeRec);
        CaMonitorScalar.setChannel(sigTimeCh, time);
    }

    @Override
    protected void prepareRecords() {
    }

    @Override
    public double getInterval() {
        return 0;
    }

    @Override
    public int getRecLen() {
        return 0;
    }

    @Override
    public double getTimeDelay() {
        return 0;
    }
}
