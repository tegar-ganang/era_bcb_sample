package gov.sns.apps.sclsetcm;

import gov.sns.ca.*;
import gov.sns.ca.correlator.*;
import gov.sns.tools.correlator.*;
import gov.sns.xal.smf.impl.sclcavity.*;
import java.util.ArrayList;

/**
 *
 * @author y32
 */
public class SCLCmMeasure implements CorrelationNotice, Runnable {

    SCLCmDocument myDoc;

    int correlated;

    int ncorrelated;

    protected ChannelCorrelator correlator;

    private PeriodicPoster poster;

    private Double dwellTime;

    private Double deltaT;

    private Double endS;

    private Double llrfDt;

    protected boolean correlatorRunning;

    protected String[] pv1;

    protected String[] pv2;

    protected String pv3;

    double[] signalPhase;

    double[] signalAmplitude;

    double[] noisePhase;

    double[] noiseAmplitude;

    double[] beamPhase;

    double[] beamAmplitude;

    double[] amin;

    double[] amax;

    double[] pmin;

    double[] pmax;

    double[] totalphase;

    int[] beamcount;

    int[] noisecount;

    ArrayList phs;

    ArrayList amp;

    double[][] phaseRecord;

    double[][] ampRecord;

    double[] ampPeak;

    int[] beamIndx;

    double noise2signal = 0.47;

    /** Creates a new instance of SCLCmMeasure */
    public SCLCmMeasure(SCLCmDocument doc) {
        myDoc = doc;
        deltaT = new Double(0.01);
        dwellTime = new Double(0.502);
        correlatorRunning = false;
        ncorrelated = 0;
        correlated = 0;
        correlator = new ChannelCorrelator(deltaT.doubleValue());
        poster = new PeriodicPoster(correlator, dwellTime.doubleValue());
        poster.addCorrelationNoticeListener(this);
        phs = new ArrayList(60 * myDoc.numberOfCav);
        amp = new ArrayList(60 * myDoc.numberOfCav);
        phaseRecord = new double[60][512];
        ampRecord = new double[60][512];
        ampPeak = new double[60];
        beamIndx = new int[60];
    }

    public void run() {
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

    protected boolean setPV2(String[] name) {
        if (correlatorRunning) stopCorrelator();
        try {
            if (pv2 == name) {
                if (!checkConnection(name)) return false;
                return true;
            }
            for (int i = 0; i < myDoc.numberOfCav; i++) if (pv2[i] != null) correlator.removeSource(pv2[i]);
        } catch (NullPointerException ne) {
        } catch (ArrayIndexOutOfBoundsException ae) {
        }
        if (!checkConnection(name)) return false;
        pv2 = name;
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
        ChannelTimeRecord pvValue1, pvValue2;
        for (int i = 0; i < myDoc.numberOfCav; i++) {
            pvValue1 = (ChannelTimeRecord) (correlation.getRecord(pv1[i]));
            pvValue2 = (ChannelTimeRecord) (correlation.getRecord(pv2[i]));
            phs.add(i + correlated * myDoc.numberOfCav, pvValue1.doubleArray());
            amp.add(i + correlated * myDoc.numberOfCav, pvValue2.doubleArray());
        }
        correlated++;
        myDoc.getController().tfmon.setText(String.valueOf(correlated));
        if (correlated >= 60) stopCorrelator();
    }

    public synchronized void noCorrelationCaught(Object sender) {
        ncorrelated++;
        if (ncorrelated > 900) {
            stopCorrelator();
        }
    }

    protected void startCorrelator() {
        if (correlatorRunning) {
            return;
        }
        if (myDoc.stopped) myDoc.stopped = false;
        correlator.startMonitoring();
        poster.start();
        correlatorRunning = true;
    }

    /** the  method to handle stop button clicks */
    protected void stopCorrelator() {
        if (!correlatorRunning) return;
        poster.stop();
        correlatorRunning = false;
        myDoc.stopped = true;
        CavPhaseAvg();
    }

    protected void reset() {
        if (correlatorRunning) stopCorrelator();
        if (phs != null) phs.clear();
        if (amp != null) amp.clear();
        for (int i = 0; i < myDoc.numberOfCav; i++) {
            correlatorRunning = false;
            ncorrelated = 0;
            correlated = 0;
        }
    }

    protected void CavPhaseAvg() {
        noisecount = new int[myDoc.numberOfCav];
        beamcount = new int[myDoc.numberOfCav];
        signalPhase = new double[myDoc.numberOfCav];
        signalAmplitude = new double[myDoc.numberOfCav];
        noisePhase = new double[myDoc.numberOfCav];
        noiseAmplitude = new double[myDoc.numberOfCav];
        beamPhase = new double[myDoc.numberOfCav];
        beamAmplitude = new double[myDoc.numberOfCav];
        amin = new double[myDoc.numberOfCav];
        amax = new double[myDoc.numberOfCav];
        pmin = new double[myDoc.numberOfCav];
        pmax = new double[myDoc.numberOfCav];
        totalphase = new double[myDoc.numberOfCav];
        for (int k = 0; k < myDoc.numberOfCav; k++) {
            signalPhase[k] = 0.;
            signalAmplitude[k] = 0.;
            noisePhase[k] = 0.;
            noiseAmplitude[k] = 0.;
            beamPhase[k] = 0.;
            beamAmplitude[k] = 0.;
            Channel ca1 = ChannelFactory.defaultFactory().getChannel(myDoc.cav[k].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "Wf_Dt");
            Channel ca2 = ChannelFactory.defaultFactory().getChannel(myDoc.cav[k].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "CtlRFPW");
            try {
                llrfDt = ca1.getValDbl();
                endS = ca2.getValDbl() - 2.0;
            } catch (ConnectionException ce) {
                myDoc.errormsg("Error connect " + ce);
            } catch (GetException ge) {
                myDoc.errormsg("Error read " + ge);
            }
            myDoc.startPt = (int) Math.round(endS / llrfDt);
            myDoc.pts = (int) Math.round(10.0 / llrfDt);
            amin[k] = 1.E8;
            amax[k] = -1.E8;
            pmin[k] = 1.E8;
            pmax[k] = -1.E8;
            noisecount[k] = 0;
            beamcount[k] = 0;
            totalphase[k] = 0.;
            for (int i = 0; i < correlated; i++) {
                beamIndx[i] = 0;
                ampPeak[i] = -1.E8;
                ampRecord[i] = (double[]) amp.get(k + i * myDoc.numberOfCav);
                phaseRecord[i] = (double[]) phs.get(k + i * myDoc.numberOfCav);
                for (int j = myDoc.startPt; j < myDoc.startPt + myDoc.pts; j++) {
                    if (ampRecord[i][j] > ampPeak[i]) ampPeak[i] = ampRecord[i][j];
                    if ((phaseRecord[i][j] - phaseRecord[correlated - 1][myDoc.startPt]) > 180) phaseRecord[i][j] = phaseRecord[i][j] - 360; else if ((phaseRecord[i][j] - phaseRecord[correlated - 1][myDoc.startPt]) < -180) phaseRecord[i][j] = phaseRecord[i][j] + 360;
                }
                amax[k] = Math.max(amax[k], ampPeak[i]);
            }
            for (int i = 0; i < correlated; i++) {
                if (ampPeak[i] > 0.85 * amax[k]) {
                    beamIndx[i] = 2;
                    beamcount[k]++;
                    for (int j = myDoc.startPt; j < myDoc.startPt + myDoc.pts; j++) totalphase[k] = totalphase[k] + phaseRecord[i][j];
                } else if (ampPeak[i] < noise2signal * amax[k]) {
                    beamIndx[i] = 1;
                    noisecount[k]++;
                    for (int j = myDoc.startPt; j < myDoc.startPt + myDoc.pts; j++) {
                        noisePhase[k] = noisePhase[k] + phaseRecord[i][j];
                        noiseAmplitude[k] = noiseAmplitude[k] + ampRecord[i][j];
                    }
                }
            }
            if (beamcount[k] > 0) totalphase[k] = totalphase[k] / (myDoc.pts * beamcount[k]);
            for (int i = 0; i < correlated; i++) {
                if (beamIndx[i] == 2) {
                    for (int j = myDoc.startPt; j < myDoc.startPt + myDoc.pts; j++) {
                        if (Math.abs(phaseRecord[i][j] - totalphase[k]) > 30) {
                            beamIndx[i] = 0;
                            beamcount[k]--;
                            break;
                        }
                    }
                }
            }
            for (int i = 0; i < correlated; i++) {
                if (beamIndx[i] == 2) {
                    signalAmplitude[k] = signalAmplitude[k] + ampPeak[i];
                    for (int j = myDoc.startPt; j < myDoc.startPt + myDoc.pts; j++) {
                        pmin[k] = Math.min(pmin[k], phaseRecord[i][j]);
                        pmax[k] = Math.max(pmax[k], phaseRecord[i][j]);
                        amin[k] = Math.min(amin[k], ampRecord[i][j]);
                        signalPhase[k] = signalPhase[k] + phaseRecord[i][j];
                    }
                }
            }
            if (beamcount[k] > 0) {
                signalPhase[k] = signalPhase[k] / (myDoc.pts * beamcount[k]);
                signalAmplitude[k] = signalAmplitude[k] / beamcount[k];
            }
            if (noisecount[k] > 0) {
                noisePhase[k] = noisePhase[k] / (noisecount[k] * myDoc.pts);
                noiseAmplitude[k] = noiseAmplitude[k] / (noisecount[k] * myDoc.pts);
            }
            Phasor signal = new Phasor(signalAmplitude[k], signalPhase[k] * Constant.rad);
            Phasor noise = new Phasor(noiseAmplitude[k], noisePhase[k] * Constant.rad);
            signal.minus(noise);
            beamAmplitude[k] = signal.getam();
            beamPhase[k] = Constant.deg * signal.getph();
            if (beamPhase[k] > 180) {
                beamPhase[k] = beamPhase[k] - 360;
            }
            myDoc.beamAmp[k] = beamAmplitude[k];
            myDoc.beamPhase[k] = beamPhase[k];
            myDoc.signalA[k] = signalAmplitude[k];
            myDoc.signalP[k] = signalPhase[k];
            myDoc.noiseA[k] = noiseAmplitude[k];
            myDoc.noiseP[k] = noisePhase[k];
        }
    }
}
