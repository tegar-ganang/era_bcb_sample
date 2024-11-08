package jp.jparc.apps.monsampler;

import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;

public class WERBLMController extends DigitizerController {

    private Channel sigRangeCh;

    private Channel sigTimeCh;

    private Channel bgRangeCh;

    private Channel bgTimeCh;

    private String sigRangeRec;

    private String sigTimeRec;

    private String bgRangeRec;

    private String bgTimeRec;

    boolean cagetFlag;

    boolean caputFlag;

    public WERBLMController(String ID, boolean caget, boolean caput) {
        super(ID);
        sigRangeRec = ID + ":SET:H_DT";
        sigTimeRec = ID + ":SET:H_T0";
        bgRangeRec = ID + ":SET:L_DT";
        bgTimeRec = ID + ":SET:L_T0";
        cagetFlag = caget;
        caputFlag = caput;
        if (caputFlag) {
            prepareRecords();
        }
    }

    public void setSignalRange(double time) {
        System.out.println("set " + sigRangeRec + "to " + time);
        if (!caputFlag) {
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

    public void setBGRange(double time) {
        System.out.println("set " + bgRangeRec + "to " + time);
        if (!caputFlag) {
            return;
        }
        bgRangeCh = ChannelFactory.defaultFactory().getChannel(bgRangeRec);
        CaMonitorScalar.setChannel(bgRangeCh, time);
    }

    public void setBGTime(double time) {
        System.out.println("set " + bgTimeRec + "to " + time);
        if (!caputFlag) {
            return;
        }
        bgTimeCh = ChannelFactory.defaultFactory().getChannel(bgTimeRec);
        CaMonitorScalar.setChannel(bgTimeCh, time);
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
