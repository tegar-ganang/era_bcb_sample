package gov.sns.apps.shakers.rfphaseshaker.utils;

import java.util.*;
import javax.swing.*;
import gov.sns.xal.smf.AcceleratorSeq;
import gov.sns.xal.smf.AcceleratorSeqCombo;
import gov.sns.xal.smf.AcceleratorNode;
import gov.sns.xal.smf.impl.BPM;
import gov.sns.xal.smf.impl.RfCavity;
import gov.sns.xal.model.scenario.Scenario;
import gov.sns.xal.model.probe.ParticleProbe;
import gov.sns.xal.model.alg.ParticleTracker;
import gov.sns.xal.model.probe.ProbeFactory;
import gov.sns.xal.model.ModelException;
import gov.sns.xal.model.probe.traj.Trajectory;
import gov.sns.tools.plot.BasicGraphData;
import gov.sns.ca.ChannelException;

/**
 *  AccCalculator calculates the BPM phase response for the RF cavities shaking.
 *
 *@author     shishlo
 */
public class AccCalculator {

    private Vector<AcceleratorSeq> accSeqV = null;

    private AcceleratorSeqCombo accSeqGlobal = null;

    private DevTreeNode rootRFTreeNode = null;

    private DevTreeNode rootBPMTreeNode = null;

    private Vector<DevTreeNode> bpmNodeV = null;

    private Vector<DevTreeNode> rfNodeV = null;

    private Thread runningThread = Thread.currentThread();

    private JTextField messageTextLocal = new JTextField();

    /**
	 *  Costructor
	 */
    public AccCalculator(DevTreeNode rootRFTreeNode_in, DevTreeNode rootBPMTreeNode_in) {
        rootRFTreeNode = rootRFTreeNode_in;
        rootBPMTreeNode = rootBPMTreeNode_in;
    }

    /**
	 *  Set accelerators sequences 
	 */
    public void setAccSeqV(Vector<AcceleratorSeq> accSeqV_in) {
        accSeqV = accSeqV_in;
        accSeqGlobal = new AcceleratorSeqCombo("global", accSeqV);
    }

    /**
	* Connects the local text message field with the outside field
	*/
    public void setMessageText(JTextField messageTextLocal) {
        this.messageTextLocal.setDocument(messageTextLocal.getDocument());
    }

    /**
	 *  Calculates the phase response from the model
	 */
    public void caclulatePhaseResponse(double phase_shift, BasicGraphData designGD) {
        double s_min = accSeqGlobal.getLength() + 10.;
        double s_max = -10.;
        for (DevTreeNode accSeqNode : rootBPMTreeNode.children) {
            for (DevTreeNode accNodeNode : accSeqNode.children) {
                if (accNodeNode.isOn == true) {
                    double s = accSeqGlobal.getPosition(accNodeNode.accNode);
                    if (s < s_min) s_min = s;
                    if (s > s_max) s_max = s;
                }
            }
        }
        for (DevTreeNode accSeqNode : rootRFTreeNode.children) {
            for (DevTreeNode accNodeNode : accSeqNode.children) {
                if (accNodeNode.isOn == true) {
                    double s = accSeqGlobal.getPosition(accNodeNode.accNode);
                    double L = accNodeNode.accNode.getLength();
                    if (s < s_min) s_min = s;
                    s = s + L;
                    if (s > s_max) s_max = s;
                }
            }
        }
        Vector<AcceleratorSeq> accSeq_slct_V = new Vector<AcceleratorSeq>();
        for (AcceleratorSeq accSeq : accSeqV) {
            double s = accSeqGlobal.getPosition(accSeq);
            double L = accSeq.getLength();
            if (s_min >= s && s_min < s + L) {
                accSeq_slct_V.add(accSeq);
            } else {
                if (s >= s_min && s < s_max) {
                    accSeq_slct_V.add(accSeq);
                }
            }
        }
        bpmNodeV = new Vector<DevTreeNode>();
        for (DevTreeNode accSeqNode : rootBPMTreeNode.children) {
            for (DevTreeNode accNodeNode : accSeqNode.children) {
                if (accNodeNode.isOn == true) bpmNodeV.add(accNodeNode);
            }
        }
        rfNodeV = new Vector<DevTreeNode>();
        for (DevTreeNode accSeqNode : rootRFTreeNode.children) {
            for (DevTreeNode accNodeNode : accSeqNode.children) {
                if (accNodeNode.isOn == true) rfNodeV.add(accNodeNode);
            }
        }
        if (rfNodeV.size() == 0) {
            messageTextLocal.setText("You have to specify RF Cavities! Stop.");
            return;
        }
        Vector<DevTreeNode> rfCalcNodeV = new Vector<DevTreeNode>();
        for (DevTreeNode accSeqNode : rootRFTreeNode.children) {
            if (accSeq_slct_V.contains(accSeqNode.accSeq)) {
                rfCalcNodeV.addAll(accSeqNode.children);
            }
        }
        AcceleratorSeqCombo accSeqTotal = new AcceleratorSeqCombo("selected", accSeq_slct_V);
        Scenario scenario = null;
        ParticleProbe probe = null;
        try {
            scenario = Scenario.newScenarioFor(accSeqTotal);
            scenario.setSynchronizationMode(Scenario.SYNC_MODE_DESIGN);
            ParticleTracker tracker = new ParticleTracker();
            tracker.setStopNodeInclusive(false);
            tracker.setRfGapPhaseCalculation(true);
            probe = ProbeFactory.getParticleProbe(accSeqTotal, tracker);
            scenario.setProbe(probe);
            probe.initialize();
        } catch (ModelException excpt) {
            messageTextLocal.setText("Can not create scenario! Stop.");
            return;
        }
        double kinEnergyIni = probe.getKineticEnergy();
        double rfFreq_Init = 1.0e+6 * ((RfCavity) rfCalcNodeV.get(0).accNode).getCavFreq();
        Vector<String> startNodeIdV = new Vector<String>();
        Vector<String> stopNodeIdV = new Vector<String>();
        Vector<Vector<DevTreeNode>> devTreeBPMVV = new Vector<Vector<DevTreeNode>>();
        for (int i = 0; i < rfCalcNodeV.size(); i++) {
            DevTreeNode rfNode = rfCalcNodeV.get(i);
            RfCavity rfCav = (RfCavity) rfNode.accNode;
            try {
                if (i != 0) {
                    AcceleratorNode startRFGap = (AcceleratorNode) rfCav.getGapsAsList().get(0);
                    scenario.setStartNode(startRFGap.getId());
                }
                if (i != (rfCalcNodeV.size() - 1)) {
                    RfCavity rfCavNext = (RfCavity) rfCalcNodeV.get(i + 1).accNode;
                    AcceleratorNode stopRFGap = (AcceleratorNode) rfCavNext.getGapsAsList().get(0);
                    scenario.setStopNode(stopRFGap.getId());
                }
            } catch (ModelException excpt) {
                messageTextLocal.setText("Can not create scenario! Stop.");
                return;
            }
            startNodeIdV.add(scenario.getStartElementId());
            stopNodeIdV.add(scenario.getStopElementId());
            rfNode.hashT.put("designTimeIn", new Double(0.));
            rfNode.hashT.put("designTimeOut", new Double(0.));
            rfNode.hashT.put("designTkIn", new Double(0.));
            rfNode.hashT.put("designTkOut", new Double(0.));
            rfNode.hashT.put("rffrequency", new Double(1.0e+6 * rfCav.getCavFreq()));
            rfNode.hashT.put("designAmp", new Double(rfCav.getDfltCavAmp()));
            rfNode.hashT.put("designPhase", new Double(rfCav.getDfltCavPhase()));
            Vector<DevTreeNode> devTreeBPMV = new Vector<DevTreeNode>();
            devTreeBPMVV.add(devTreeBPMV);
            AcceleratorNode startAccNode = scenario.nodeWithId(scenario.getStartElementId());
            AcceleratorNode stopAccNode = scenario.nodeWithId(scenario.getStopElementId());
            double s_start = 0.;
            if (startAccNode != null) s_start = accSeqTotal.getPosition(startAccNode);
            double s_stop = accSeqTotal.getLength();
            if (stopAccNode != null) s_stop = accSeqTotal.getPosition(stopAccNode);
            for (DevTreeNode bpmNode : bpmNodeV) {
                AcceleratorNode bpm = bpmNode.accNode;
                double bpm_pos = accSeqTotal.getPosition(bpm);
                if (bpm_pos >= s_start && bpm_pos <= s_stop) {
                    devTreeBPMV.add(bpmNode);
                }
            }
            scenario.unsetStartNode();
            scenario.unsetStopNode();
        }
        double eKinIn = kinEnergyIni;
        double eKinOut = kinEnergyIni;
        double timeIn = 0.;
        double timeOut = 0.;
        double n_graph_points = 300;
        double s_step = accSeqTotal.getLength() / (n_graph_points - 1);
        Vector<Double> positionsV = new Vector<Double>();
        Vector<Double> initTimeInV = new Vector<Double>();
        for (int i = 0; i < rfCalcNodeV.size(); i++) {
            DevTreeNode rfNode = rfCalcNodeV.get(i);
            RfCavity rfCav = (RfCavity) rfNode.accNode;
            scenario.setStartElementId(startNodeIdV.get(i));
            scenario.setStopElementId(stopNodeIdV.get(i));
            AcceleratorNode startAccNode = scenario.nodeWithId(scenario.getStartElementId());
            AcceleratorNode stopAccNode = scenario.nodeWithId(scenario.getStopElementId());
            double s_start = 0.;
            if (startAccNode != null) s_start = accSeqTotal.getPosition(startAccNode);
            double s_stop = accSeqTotal.getLength();
            if (stopAccNode != null) s_stop = accSeqTotal.getPosition(stopAccNode);
            double phase = rfNode.hashT.get("designPhase").doubleValue();
            double amp = rfNode.hashT.get("designAmp").doubleValue();
            rfCav.updateDesignPhase(phase);
            rfCav.updateDesignAmp(amp);
            probe.reset();
            probe.setKineticEnergy(eKinIn);
            probe.setTime(timeIn);
            probe.setPosition(s_start);
            try {
                scenario.resync();
                scenario.run();
            } catch (ModelException excpt) {
                messageTextLocal.setText("Can not create scenario! Stop.");
                return;
            }
            Trajectory traj = scenario.getProbe().getTrajectory();
            eKinOut = traj.finalState().getKineticEnergy();
            timeOut = traj.finalState().getTime();
            for (int j = 0; j < n_graph_points; j++) {
                double position = j * s_step;
                if (position >= s_start && position < s_stop) {
                    double pos = traj.stateNearestPosition(position).getPosition();
                    double time_state = traj.stateNearestPosition(position).getTime();
                    double energy_tmp = traj.stateNearestPosition(position).getKineticEnergy();
                    positionsV.add(new Double(pos));
                    initTimeInV.add(new Double(time_state));
                }
            }
            rfNode.hashT.put("designTimeIn", new Double(timeIn));
            rfNode.hashT.put("designTimeOut", new Double(timeOut));
            rfNode.hashT.put("designTkIn", new Double(eKinIn));
            rfNode.hashT.put("designTkOut", new Double(eKinOut));
            eKinIn = eKinOut;
            timeIn = timeOut;
        }
        double freq_init = rfNodeV.get(0).hashT.get("rffrequency").doubleValue();
        eKinIn = kinEnergyIni;
        eKinOut = kinEnergyIni;
        timeIn = 0.;
        timeOut = 0.;
        Vector<Double> shakedTimeInV = new Vector<Double>();
        for (int i = 0; i < rfCalcNodeV.size(); i++) {
            DevTreeNode rfNode = rfCalcNodeV.get(i);
            RfCavity rfCav = (RfCavity) rfNode.accNode;
            double freq = rfNode.hashT.get("rffrequency").doubleValue();
            scenario.setStartElementId(startNodeIdV.get(i));
            scenario.setStopElementId(stopNodeIdV.get(i));
            AcceleratorNode startAccNode = scenario.nodeWithId(scenario.getStartElementId());
            AcceleratorNode stopAccNode = scenario.nodeWithId(scenario.getStopElementId());
            double s_start = 0.;
            if (startAccNode != null) s_start = accSeqTotal.getPosition(startAccNode);
            double s_stop = accSeqTotal.getLength();
            if (stopAccNode != null) s_stop = accSeqTotal.getPosition(stopAccNode);
            double phase = rfNode.hashT.get("designPhase").doubleValue();
            double amp = rfNode.hashT.get("designAmp").doubleValue();
            if (rfNode.isOn) {
                phase = phase + phase_shift * freq_init / freq;
            }
            double timeIn_design = rfNode.hashT.get("designTimeIn").doubleValue();
            phase = phase + 360. * freq * (timeIn - timeIn_design);
            rfCav.updateDesignPhase(phase);
            rfCav.updateDesignAmp(amp);
            probe.reset();
            probe.setKineticEnergy(eKinIn);
            probe.setTime(timeIn);
            probe.setPosition(s_start);
            try {
                scenario.resync();
                scenario.run();
            } catch (ModelException excpt) {
                messageTextLocal.setText("Can not create scenario! Stop.");
                return;
            }
            Trajectory traj = scenario.getProbe().getTrajectory();
            eKinOut = traj.finalState().getKineticEnergy();
            timeOut = traj.finalState().getTime();
            for (int j = 0; j < n_graph_points; j++) {
                double position = j * s_step;
                if (position >= s_start && position < s_stop) {
                    double pos = traj.stateNearestPosition(position).getPosition();
                    double time_state = traj.stateNearestPosition(position).getTime();
                    shakedTimeInV.add(new Double(time_state));
                }
            }
            eKinIn = eKinOut;
            timeIn = timeOut;
        }
        for (int i = 0; i < rfCalcNodeV.size(); i++) {
            DevTreeNode rfNode = rfCalcNodeV.get(i);
            RfCavity rfCav = (RfCavity) rfNode.accNode;
            double phase = rfNode.hashT.get("designPhase").doubleValue();
            double amp = rfNode.hashT.get("designAmp").doubleValue();
            rfCav.updateDesignPhase(phase);
            rfCav.updateDesignAmp(amp);
        }
        if (shakedTimeInV.size() != positionsV.size()) {
            messageTextLocal.setText("Cannot create shaker table! Stop.");
        }
        double s_seq_start = accSeqGlobal.getPosition(accSeqTotal.getNodeAt(0));
        for (int i = 0; i < positionsV.size(); i++) {
            double pos = positionsV.get(i).doubleValue();
            double time_design = initTimeInV.get(i).doubleValue();
            double time_shaked = shakedTimeInV.get(i).doubleValue();
            double slope = (time_shaked - time_design) * 360 * freq_init / phase_shift;
            designGD.addPoint(pos + s_seq_start, slope);
        }
    }

    /**
	 *  Return the running thread
	 */
    public Thread getRunningThread() {
        return runningThread;
    }

    /**
	 *  Measures the phase response from the live machine
	 */
    public void measurePhaseResponse(double phase_shift, double sleepTime, int nAvg, BasicGraphData experGD) {
        runningThread = Thread.currentThread();
        double s_min = accSeqGlobal.getLength() + 10.;
        double s_max = -10.;
        for (DevTreeNode accSeqNode : rootBPMTreeNode.children) {
            for (DevTreeNode accNodeNode : accSeqNode.children) {
                if (accNodeNode.isOn == true) {
                    double s = accSeqGlobal.getPosition(accNodeNode.accNode);
                    if (s < s_min) s_min = s;
                    if (s > s_max) s_max = s;
                }
            }
        }
        for (DevTreeNode accSeqNode : rootRFTreeNode.children) {
            for (DevTreeNode accNodeNode : accSeqNode.children) {
                if (accNodeNode.isOn == true) {
                    double s = accSeqGlobal.getPosition(accNodeNode.accNode);
                    double L = accNodeNode.accNode.getLength();
                    if (s < s_min) s_min = s;
                    s = s + L;
                    if (s > s_max) s_max = s;
                }
            }
        }
        Vector<AcceleratorSeq> accSeq_slct_V = new Vector<AcceleratorSeq>();
        for (AcceleratorSeq accSeq : accSeqV) {
            double s = accSeqGlobal.getPosition(accSeq);
            double L = accSeq.getLength();
            if (s_min >= s && s_min < s + L) {
                accSeq_slct_V.add(accSeq);
            } else {
                if (s >= s_min && s < s_max) {
                    accSeq_slct_V.add(accSeq);
                }
            }
        }
        bpmNodeV = new Vector<DevTreeNode>();
        for (DevTreeNode accSeqNode : rootBPMTreeNode.children) {
            for (DevTreeNode accNodeNode : accSeqNode.children) {
                if (accNodeNode.isOn == true) bpmNodeV.add(accNodeNode);
            }
        }
        rfNodeV = new Vector<DevTreeNode>();
        for (DevTreeNode accSeqNode : rootRFTreeNode.children) {
            for (DevTreeNode accNodeNode : accSeqNode.children) {
                if (accNodeNode.isOn == true) rfNodeV.add(accNodeNode);
            }
        }
        if (rfNodeV.size() == 0) {
            messageTextLocal.setText("You have to specify RF Cavities! Stop.");
            return;
        }
        Vector<DevTreeNode> rfCalcNodeV = new Vector<DevTreeNode>();
        for (DevTreeNode accSeqNode : rootRFTreeNode.children) {
            if (accSeq_slct_V.contains(accSeqNode.accSeq)) {
                rfCalcNodeV.addAll(accSeqNode.children);
            }
        }
        double rfFreq_Init = 1.0e+6 * ((RfCavity) rfCalcNodeV.get(0).accNode).getCavFreq();
        for (DevTreeNode rfNode : rfCalcNodeV) {
            if (rfNode.isOn == true) {
                RfCavity rfCav = (RfCavity) rfNode.accNode;
                double freq = 1.0e+6 * rfCav.getCavFreq();
                rfNode.hashT.put("rffrequency", new Double(1.0e+6 * rfCav.getCavFreq()));
                try {
                    double livePhase = rfCav.channelSuite().getChannel("cavPhaseSet").getValDbl();
                    rfNode.hashT.put("livePhase", new Double(livePhase));
                } catch (ChannelException extp) {
                    messageTextLocal.setText("Cannot read from EPICS! " + rfCav.getId() + " Stop.");
                    return;
                }
            }
        }
        for (DevTreeNode bpmNode : bpmNodeV) {
            BPM bpm = (BPM) bpmNode.accNode;
            try {
                double phase = bpm.getPhaseAvg();
                bpmNode.hashT.put("initPhase", new Double(phase));
                bpmNode.hashT.put("newPhase", new Double(phase));
                bpmNode.hashT.put("n_measurements", new Integer(0));
                bpmNode.hashT.put("slopeSum", new Double(0));
                bpmNode.hashT.put("slopeSum2", new Double(0));
                double freq = 1.0e+6 * bpm.getBucket("bpm").getAttr("frequency").getDouble();
                double phaseCoeff = rfFreq_Init / freq;
                bpmNode.hashT.put("phaseCoeff", new Double(phaseCoeff));
            } catch (ChannelException extp) {
                messageTextLocal.setText("Cannot read from EPICS! " + bpm.getId() + " Stop.");
                return;
            }
        }
        for (int iM = 0; iM < nAvg; iM++) {
            for (int iST = 0; iST < 2; iST++) {
                int index = iST + 2 * iM + 1;
                int i_total = 2 * nAvg;
                messageTextLocal.setText("Measurement done " + index + " of " + i_total);
                for (DevTreeNode rfNode : rfCalcNodeV) {
                    if (rfNode.isOn == true) {
                        RfCavity rfCav = (RfCavity) rfNode.accNode;
                        try {
                            double livePhase = rfNode.hashT.get("livePhase").doubleValue();
                            rfCav.channelSuite().getChannel("cavPhaseSet").putVal(livePhase + iST * phase_shift);
                        } catch (ChannelException extp) {
                            messageTextLocal.setText("Cannot read from EPICS! " + rfCav.getId() + " Stop.");
                        }
                    }
                }
                try {
                    Thread.sleep((long) (1000 * sleepTime));
                } catch (InterruptedException expt) {
                    measureBPM(-1, phase_shift, experGD);
                    messageTextLocal.setText("The shaking was interrupted! Stop.");
                    resetRF_Phases(rfCalcNodeV);
                    return;
                }
                measureBPM(iST, phase_shift, experGD);
            }
        }
        resetRF_Phases(rfCalcNodeV);
        System.out.println("debug measurement done!");
    }

    private void resetRF_Phases(Vector<DevTreeNode> rfCalcNodeV) {
        for (DevTreeNode rfNode : rfCalcNodeV) {
            if (rfNode.isOn == true) {
                RfCavity rfCav = (RfCavity) rfNode.accNode;
                try {
                    double livePhase = rfNode.hashT.get("livePhase").doubleValue();
                    rfCav.channelSuite().getChannel("cavPhaseSet").putVal(livePhase);
                } catch (ChannelException extp) {
                }
            }
        }
    }

    private void measureBPM(int iStage, double phase_shift, BasicGraphData experGD) {
        for (DevTreeNode bpmNode : bpmNodeV) {
            if (iStage < 0) continue;
            BPM bpm = (BPM) bpmNode.accNode;
            try {
                double phase = bpm.getPhaseAvg();
                if (iStage == 0) bpmNode.hashT.put("initPhase", new Double(phase));
                if (iStage == 1) {
                    double initPhase = bpmNode.hashT.get("initPhase").doubleValue();
                    phase = wrap(phase, initPhase);
                    bpmNode.hashT.put("newPhase", new Double(phase));
                    int nm = bpmNode.hashT.get("n_measurements").intValue();
                    nm++;
                    bpmNode.hashT.put("n_measurements", new Integer(nm));
                    double slopeSum = bpmNode.hashT.get("slopeSum").doubleValue();
                    double slopeSum2 = bpmNode.hashT.get("slopeSum2").doubleValue();
                    double phaseCoeff = bpmNode.hashT.get("phaseCoeff").doubleValue();
                    double slope = phaseCoeff * (phase - initPhase) / phase_shift;
                    bpmNode.hashT.put("slopeSum", new Double(slopeSum + slope));
                    bpmNode.hashT.put("slopeSum2", new Double(slopeSum2 + slope * slope));
                }
            } catch (ChannelException extp) {
                messageTextLocal.setText("Cannot read from EPICS! " + bpm.getId() + " Stop.");
                return;
            }
        }
        experGD.removeAllPoints();
        for (DevTreeNode bpmNode : bpmNodeV) {
            if (bpmNode.isOn == true) {
                BPM bpm = (BPM) bpmNode.accNode;
                int nm = bpmNode.hashT.get("n_measurements").intValue();
                double slopeSum = bpmNode.hashT.get("slopeSum").doubleValue();
                double slopeSum2 = bpmNode.hashT.get("slopeSum2").doubleValue();
                if (nm > 0) {
                    if (nm == 1) {
                        experGD.addPoint(bpmNode.position, slopeSum);
                    } else {
                        double avgSlope = slopeSum / nm;
                        double err = slopeSum2 - nm * avgSlope * avgSlope;
                        err = Math.sqrt(Math.abs(err) / (nm * (nm - 1)));
                        experGD.addPoint(bpmNode.position, avgSlope, err);
                    }
                }
            }
        }
    }

    /** This method finds +-2*PI to produce the nearest points.
	 */
    protected double wrap(double y, double yIn) {
        if (y == yIn) return y;
        int n = 0;
        double diff = yIn - y;
        double diff_min = Math.abs(diff);
        double sign = diff / diff_min;
        int n_curr = n + 1;
        double diff_min_curr = Math.abs(y + sign * n_curr * 360. - yIn);
        while (diff_min_curr < diff_min) {
            n = n_curr;
            diff_min = Math.abs(y + sign * n * 360. - yIn);
            n_curr++;
            diff_min_curr = Math.abs(y + sign * n_curr * 360. - yIn);
        }
        return (y + sign * n * 360.);
    }
}
