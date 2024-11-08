package gov.sns.apps.sclsetcm;

import gov.sns.ca.*;
import gov.sns.ca.correlator.*;
import gov.sns.tools.correlator.*;

/**
 *
 * @author y32
 */
public class SCLCmTune implements CorrelationNotice, Runnable {

    SCLCmDocument myDoc;

    protected ChannelCorrelator correlator;

    private PeriodicPoster poster;

    private Double dwellTime;

    private Double deltaT;

    private int endPt;

    private int startPt;

    private int nocorrelate;

    private int trys;

    protected boolean correlatorRunning;

    boolean nottuned;

    protected String[] pv1;

    protected String pv3;

    double[][] phaseRecord;

    ChannelFactory cf = ChannelFactory.defaultFactory();

    /** Creates a new instance of SCLCmMeasure */
    public SCLCmTune(SCLCmDocument doc) {
        myDoc = doc;
        deltaT = new Double(0.1);
        dwellTime = new Double(1.01);
        correlatorRunning = false;
        nottuned = true;
        correlator = new ChannelCorrelator(deltaT.doubleValue());
        poster = new PeriodicPoster(correlator, dwellTime.doubleValue());
        poster.addCorrelationNoticeListener(this);
        phaseRecord = new double[myDoc.numberOfCav][512];
        trys = 0;
    }

    public void run() {
        Channel[] ca = new Channel[myDoc.numberOfCav];
        try {
            for (int i = 0; i < myDoc.numberOfCav; i++) {
                ca[i] = cf.getChannel("SCL_HPRF:Tun" + myDoc.cav[i].channelSuite().getChannel("cavAmpSet").getId().substring(12, 16) + "Tun_Ctl");
                ca[i].putVal("Off");
            }
        } catch (ConnectionException ce) {
            myDoc.errormsg("Error in connection to PV");
        } catch (PutException pe) {
            myDoc.errormsg("Error in writing to PV");
        }
        while (nottuned) {
            startCorrelator();
            if (cavtuned()) {
                nottuned = false;
                try {
                    for (int i = 0; i < myDoc.numberOfCav; i++) {
                        ca[i].putVal("Auto-Tune");
                    }
                } catch (ConnectionException ce) {
                    myDoc.errormsg("Error in connection to PV");
                } catch (PutException pe) {
                    myDoc.errormsg("Error in writing to PV!");
                }
                return;
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                myDoc.errormsg("Tune thread was interruped!");
            }
            if (trys > 7) {
                myDoc.errormsg("Failed, need to tune menually");
                return;
            }
        }
        return;
    }

    protected boolean setPV1(String[] name) {
        if (correlatorRunning) stopCorrelator();
        try {
            if (pv1 == name) {
                if (!checkConnection(name)) return false;
                return true;
            }
            for (int i = 0; i < myDoc.numberOfCav; i++) if (pv1[i] != null) correlator.removeSource(pv1[i]);
        } catch (NullPointerException ne) {
        } catch (ArrayIndexOutOfBoundsException ae) {
        }
        pv1 = name;
        if (!checkConnection(name)) return false;
        for (int i = 0; i < myDoc.numberOfCav; i++) {
            correlator.addChannel(name[i]);
        }
        return true;
    }

    protected boolean setPV3(String name) {
        String[] nam = new String[1];
        nam[0] = name;
        if (correlatorRunning) stopCorrelator();
        if (pv3 == name) {
            if (!checkConnection(nam)) return false;
            return true;
        }
        if (pv3 != null) correlator.removeSource(pv3);
        if (!checkConnection(nam)) return false;
        pv3 = name;
        correlator.addChannel(name);
        return true;
    }

    /** check to see if we can connect to this PV: */
    private boolean checkConnection(String[] name) {
        for (int i = 0; i < name.length; i++) {
            Channel tempChannel = ChannelFactory.defaultFactory().getChannel(name[i]);
            try {
                tempChannel.checkConnection();
            } catch (ConnectionException e) {
                return false;
            }
        }
        return true;
    }

    public synchronized void newCorrelation(Object Sender, Correlation correlation) {
        ChannelTimeRecord pvValue;
        for (int i = 0; i < myDoc.numberOfCav; i++) {
            pvValue = (ChannelTimeRecord) (correlation.getRecord(pv1[i]));
            phaseRecord[i] = pvValue.doubleArray();
        }
        stopCorrelator();
    }

    public synchronized void noCorrelationCaught(Object sender) {
        nocorrelate++;
        if (nocorrelate > 30) {
            stopCorrelator();
            myDoc.errormsg("Error, not correlation found.");
        }
    }

    protected void startCorrelator() {
        if (correlatorRunning) return;
        nocorrelate = 0;
        correlator.startMonitoring();
        poster.start();
        correlatorRunning = true;
    }

    /** the  method to handle stop button clicks */
    protected void stopCorrelator() {
        if (!correlatorRunning) return;
        poster.stop();
        correlatorRunning = false;
    }

    private boolean cavtuned() {
        int j = 0;
        trys++;
        double p1 = 0.;
        double p2 = 0.;
        double rev;
        double dt = 2.8;
        double endP = 1300.;
        try {
            for (int i = 0; i < myDoc.numberOfCav; i++) {
                Channel ca1 = cf.getChannel(myDoc.cav[i].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "Wf_Dt");
                Channel ca2 = cf.getChannel(myDoc.cav[i].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "CtlRFPW");
                try {
                    dt = ca1.getValDbl();
                    endP = ca2.getValDbl();
                } catch (ConnectionException ce) {
                    myDoc.errormsg("Error connect " + ce);
                } catch (GetException ge) {
                    myDoc.errormsg("Error read " + ge);
                }
                endPt = (int) Math.round((endP + 100.) / dt);
                startPt = (int) Math.round(endP / dt);
                for (int k = 0; k < endPt - startPt; k++) {
                    if (phaseRecord[i][startPt + k] - phaseRecord[i][startPt] > 180) phaseRecord[i][startPt + k] -= 360; else if (phaseRecord[i][startPt + k] - phaseRecord[i][startPt] < -180) phaseRecord[i][startPt + k] += 360;
                    if (phaseRecord[i][endPt + k] - phaseRecord[i][startPt] > 180) phaseRecord[i][endPt + k] -= 360; else if (phaseRecord[i][endPt + k] - phaseRecord[i][startPt] < -180) phaseRecord[i][endPt + k] += 360;
                    p1 += phaseRecord[i][k + startPt];
                    p2 += phaseRecord[i][k + endPt];
                }
                if (Math.abs(p1 - p2) > 12.0) {
                    j++;
                    try {
                        Channel ca3 = cf.getChannel(myDoc.cav[i].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "cavV");
                        if (ca3.getValDbl() > 2.5) {
                            myDoc.errormsg("Unsafe operation, tuning cavity with RF on!");
                            nottuned = false;
                            return false;
                        }
                        Channel ca4 = cf.getChannel("SCL_HPRF:Tun" + myDoc.cav[i].channelSuite().getChannel("cavAmpSet").getId().substring(12, 16) + "Mot");
                        rev = (p1 - p2) * myDoc.rate / myDoc.pts;
                        if (Math.abs(rev) > 15) {
                            myDoc.errormsg("Error, cavity detuned too far!");
                            return true;
                        } else if (trys % 2 == 1) ca4.putVal(ca1.getValDbl() - rev); else ca4.putVal(ca1.getValDbl() + rev);
                    } catch (ConnectionException ce) {
                        myDoc.errormsg("Error connection");
                        return true;
                    } catch (GetException ge) {
                        myDoc.errormsg("Error get PV value");
                        return true;
                    } catch (PutException pe) {
                        myDoc.errormsg("Error write to PV");
                        return true;
                    }
                }
            }
        } catch (NullPointerException ne) {
            return true;
        } catch (ArrayIndexOutOfBoundsException ae) {
            return true;
        }
        if (j > 0) return false; else return true;
    }
}
