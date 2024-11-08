package jp.jparc.apps.chopperfb;

import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;

public class ChopperWatcher {

    private boolean caputMode;

    private boolean debugMode;

    private ChopperFBProperties properties;

    private int counter;

    private int fbperiod;

    private double sumpower;

    private double averagepower;

    private CaMonitorScalar powerPV;

    private CaMonitorScalar ampRBPV;

    private CaMonitorScalar statusPV;

    private CaMonitorScalar LLRFRegister1PV;

    private int ampMax;

    private int ampMin;

    private int ampDelta;

    private double powerNominal;

    private double powerTolerance;

    private Channel ampSetChannel;

    private Channel ampStrobeChannel;

    private int remoteValue;

    private int slowStartValue;

    public ChopperWatcher(boolean caput, boolean debug) {
        caputMode = caput;
        debugMode = debug;
        properties = new ChopperFBProperties();
        counter = 0;
        fbperiod = (int) (properties.getFeedbackPeriod() / properties.getSamplePeriod());
        counter = 0;
        averagepower = 0;
        sumpower = 0;
        powerPV = new CaMonitorScalar(properties.getPowerRecord());
        ampRBPV = new CaMonitorScalar(properties.getAmpRBRecord());
        statusPV = new CaMonitorScalar(properties.getStatusRecord());
        LLRFRegister1PV = new CaMonitorScalar(properties.getLLRFRegister1Record());
        ampMax = properties.getAmpMax();
        ampMin = properties.getAmpMin();
        ampDelta = properties.getAmpDelta();
        powerNominal = properties.getPowerNominal();
        powerTolerance = properties.getPowerTolerance();
        remoteValue = (int) Math.pow(2, properties.getLLRFRegister1RemoteBit());
        System.out.println("remoteValue = " + remoteValue);
        slowStartValue = (int) Math.pow(2, properties.getLLRFRegister1SlowStartBit());
    }

    public void run() {
        sample();
        feedback();
    }

    private synchronized void sample() {
        double power = powerPV.getValue();
        counter++;
        if (counter > 0) {
            sumpower += power;
            averagepower = sumpower / counter;
        } else {
            System.err.println("ChopperWatcher.sampe(): counter=0. aborting");
            System.exit(-1);
        }
        if (debugMode) {
            System.out.println("*** sample ****");
            System.out.println("counter = " + counter);
            System.out.println("sumpower, averagepower, powerNominal = " + sumpower + " " + averagepower + " " + powerNominal);
        }
    }

    private synchronized void feedback() {
        if (counter >= fbperiod) {
            int ampRB = (int) ampRBPV.getValue();
            int status = (int) statusPV.getValue();
            int register = (int) LLRFRegister1PV.getValue();
            if (debugMode) {
                System.out.println("*** feedback count = " + counter);
                System.out.println("ampRB = " + ampRB);
                System.out.println("status = " + status);
                System.out.println("register = " + register);
            }
            boolean ampOK = false;
            if ((ampRB >= ampMin) && (ampRB <= ampMax)) {
                ampOK = true;
            }
            boolean statusOK = false;
            if (status == properties.getStatusRunValue()) {
                statusOK = true;
            }
            boolean remoteOK = false;
            if ((register & remoteValue) == remoteValue) {
                remoteOK = true;
            }
            boolean slowStartOK = false;
            if ((register & slowStartValue) == 0) {
                slowStartOK = true;
            }
            if (debugMode) {
                System.out.println("ampOK = " + ampOK);
                System.out.println("statusOK = " + statusOK);
                System.out.println("remoteOK = " + remoteOK);
                System.out.println("slowStartOK = " + slowStartOK);
            }
            if (ampOK & statusOK & remoteOK & slowStartOK) {
                int ampCor = ampRB;
                boolean ampCorFlag = false;
                double powerdiff = averagepower - powerNominal;
                if (debugMode) {
                    System.out.println("powerdiff, powerTolerance = " + powerdiff + " " + powerTolerance);
                    System.out.println("Math.abs(powerdiff) = " + Math.abs(powerdiff));
                }
                if (Math.abs(powerdiff) > powerTolerance) {
                    if ((powerdiff > 0) && ((ampRB - ampDelta) >= ampMin)) {
                        ampCor = ampRB - ampDelta;
                        ampCorFlag = true;
                    } else if ((powerdiff < 0) && ((ampRB + ampDelta) <= ampMax)) {
                        ampCor = ampRB + ampDelta;
                        ampCorFlag = true;
                    }
                }
                if (ampCorFlag) {
                    if (debugMode) {
                        System.out.println("ampCorFlag = true, oldAMp, newAmp = " + ampRB + " " + ampCor);
                    }
                    if (caputMode) {
                        if (ampSetChannel == null) {
                            ampSetChannel = ChannelFactory.defaultFactory().getChannel(properties.getAmpSetRecord());
                        }
                        if (ampStrobeChannel == null) {
                            ampStrobeChannel = ChannelFactory.defaultFactory().getChannel(properties.getAmpStrobeRecord());
                        }
                        if ((ampSetChannel != null) && (ampStrobeChannel != null)) {
                            if (debugMode) {
                                System.out.println("caput " + ampSetChannel.channelName() + " " + ampCor);
                                System.out.println("caput " + ampStrobeChannel.channelName() + " " + properties.getAmpStrobeValue());
                            }
                        } else {
                            System.err.println("ChooperWatcher> Error. channels " + ampSetChannel.channelName() + " or " + ampStrobeChannel.channelName() + " are not connected");
                        }
                    }
                }
            }
            counter = 0;
            averagepower = 0;
            sumpower = 0;
        }
    }

    public double getSamplePeriod() {
        return properties.getSamplePeriod();
    }
}
