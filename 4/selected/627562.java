package gov.sns.apps.diagtiming;

import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;
import gov.sns.ca.ConnectionException;
import gov.sns.ca.GetException;
import gov.sns.ca.PutException;
import java.text.NumberFormat;
import java.text.ParseException;

/**
 * Calculate Linac BPM timing
 * 
 * @author chu
 * 
 */
public class CalcLinacTiming implements Runnable {

    int bpmInd;

    String bpmName;

    double triggerDelay = 2620.;

    double bpmWidthSet = 0.0001;

    double bpmAvgWidthSet = 10.;

    double bpmChopFreqSet = 1.057501e6;

    ChannelFactory caF = ChannelFactory.defaultFactory();

    Channel bpmAvgStartCh;

    Channel bpmAvgStartSetCh;

    Channel bpmTDelayCh;

    Channel bpmTDelaySetCh;

    Channel bpmAvgStopCh;

    Channel bpmAvgStopSetCh;

    Channel bpmChopFreqCh;

    Channel bpmChopFreqSetCh;

    Channel bpmTDelay00Ch;

    Channel bpmTDelay00SetCh;

    BPMPane thePane;

    NumberFormat nf = NumberFormat.getNumberInstance();

    public CalcLinacTiming(int ind, String name, BPMPane bpmPane) {
        bpmInd = ind;
        bpmName = name;
        thePane = bpmPane;
        bpmAvgStartCh = caF.getChannel(bpmName + ":avgStart_Rb");
        bpmAvgStartSetCh = caF.getChannel(bpmName + ":avgStart");
        bpmTDelayCh = caF.getChannel(bpmName + ":Delay00_Rb");
        bpmTDelaySetCh = caF.getChannel(bpmName + ":Delay00");
        bpmAvgStopCh = caF.getChannel(bpmName + ":avgStop_Rb");
        bpmAvgStopSetCh = caF.getChannel(bpmName + ":avgStop");
        bpmChopFreqCh = caF.getChannel(bpmName + ":chopFreq_Rb");
        bpmChopFreqSetCh = caF.getChannel(bpmName + ":chopFreq");
        bpmTDelay00Ch = caF.getChannel(bpmName + ":TurnsDelay00_Rb");
        bpmTDelay00SetCh = caF.getChannel(bpmName + ":TurnsDelay00");
    }

    protected void setBPM(int ind) {
        bpmInd = ind;
        bpmName = thePane.bpmNames[bpmInd];
    }

    protected void setTimings() {
        try {
            if (!((String) thePane.bpmTableModel.getValueAt(bpmInd, 6)).equals("null")) bpmTDelaySetCh.putVal(nf.parse((String) thePane.bpmTableModel.getValueAt(bpmInd, 6)).doubleValue());
            if (!((String) thePane.bpmTableModel.getValueAt(bpmInd, 7)).equals("null")) bpmAvgStartSetCh.putVal(nf.parse((String) thePane.bpmTableModel.getValueAt(bpmInd, 7)).doubleValue());
            if (!((String) thePane.bpmTableModel.getValueAt(bpmInd, 8)).equals("null")) bpmAvgStopSetCh.putVal(nf.parse((String) thePane.bpmTableModel.getValueAt(bpmInd, 8)).doubleValue());
            if (!((String) thePane.bpmTableModel.getValueAt(bpmInd, 9)).equals("null")) bpmChopFreqSetCh.putVal(nf.parse((String) thePane.bpmTableModel.getValueAt(bpmInd, 9)).doubleValue());
            if (!((String) thePane.bpmTableModel.getValueAt(bpmInd, 10)).equals("null")) bpmTDelay00SetCh.putVal(nf.parse((String) thePane.bpmTableModel.getValueAt(bpmInd, 10)).doubleValue());
        } catch (ConnectionException ce) {
            System.out.println(ce);
        } catch (PutException pe) {
            System.out.println(pe);
        } catch (ParseException pe) {
            System.out.println(pe);
        }
    }

    public void run() {
        Channel ampWF = caF.getChannel(bpmName + ":beamIA");
        try {
            double bpmAvgWidth_orig = 10.;
            double bpmSampling_orig = 40.;
            try {
                System.out.println(bpmAvgStopCh.getId());
                System.out.println(bpmChopFreqCh.getId());
                bpmAvgWidth_orig = bpmAvgStopCh.getValDbl();
                bpmSampling_orig = bpmChopFreqCh.getValDbl();
            } catch (GetException ge) {
                System.out.println(ge);
            }
            double bpmWidth = bpmAvgStartCh.getValDbl() * 1000000.;
            if (thePane.beamWidth > 100.) {
                bpmAvgWidthSet = thePane.beamWidth / 5.;
                if (bpmWidth < thePane.beamWidth + 200.) {
                    if (thePane.beamWidth < 500.) {
                        bpmWidthSet = thePane.beamWidth * 2. / 1000000.;
                        try {
                            bpmAvgStartSetCh.putVal(bpmWidthSet);
                        } catch (PutException pe) {
                            System.out.println(pe);
                        }
                    } else {
                        bpmWidthSet = (thePane.beamWidth + 300.) / 1000000.;
                        try {
                            bpmAvgStartSetCh.putVal(bpmWidthSet);
                        } catch (PutException pe) {
                            System.out.println(pe);
                        }
                    }
                }
            } else {
                bpmWidthSet = 0.0001;
                if (thePane.beamWidth < 50.) bpmAvgWidthSet = Math.round(thePane.beamWidth / 5.); else bpmAvgWidthSet = 10.;
                try {
                    bpmAvgStartSetCh.putVal(0.0001);
                } catch (PutException pe) {
                    System.out.println(pe);
                }
            }
            try {
                bpmAvgStopSetCh.putVal(thePane.beamWidth);
                bpmChopFreqSetCh.putVal(40);
            } catch (PutException pe) {
                System.out.println(pe);
            }
            double tDelay_orig = 2620.;
            try {
                tDelay_orig = bpmTDelayCh.getValDbl();
            } catch (GetException ge) {
                System.out.println(ge);
            }
            double tDelay_init = 0.;
            try {
                bpmTDelaySetCh.putVal(tDelay_init);
            } catch (PutException pe) {
                System.out.println(pe);
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                System.out.println(ie);
            }
            double[] ampArray = ampWF.getArrDbl();
            int lastPt = 255;
            while (ampArray[lastPt] < 2.) {
                lastPt--;
            }
            System.out.println("The last non-noise point for " + bpmName + " is " + lastPt);
            triggerDelay = tDelay_init + lastPt - thePane.beamWidth / 5.;
            System.out.println("trigger delay = " + triggerDelay);
            if (thePane.bpmTableModel != null) {
                nf.setMaximumFractionDigits(1);
                thePane.bpmTableModel.setValueAt(nf.format(triggerDelay), bpmInd, 5);
                thePane.bpmTableModel.setValueAt(nf.format(bpmAvgWidthSet), bpmInd, 6);
                thePane.bpmTableModel.setValueAt(nf.format(bpmChopFreqSet), bpmInd, 8);
                nf.setMaximumFractionDigits(5);
                thePane.bpmTableModel.setValueAt(nf.format(bpmWidthSet), bpmInd, 7);
            }
            try {
                Thread.sleep(1500);
            } catch (InterruptedException ie) {
                System.out.println(ie);
            }
            try {
                bpmAvgStopSetCh.putVal(bpmAvgWidth_orig);
                bpmChopFreqSetCh.putVal(bpmSampling_orig);
                bpmTDelaySetCh.putVal(tDelay_orig);
            } catch (PutException pe) {
                System.out.println(pe);
            }
            thePane.setOne.setEnabled(true);
        } catch (ConnectionException ce) {
            System.out.println(ce);
        } catch (GetException ge) {
            System.out.println(ge);
        }
    }
}
