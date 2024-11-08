package jp.jparc.apps.iTuning;

import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;
import gov.sns.ca.ConnectionListener;
import gov.sns.tools.apputils.SimpleChartPopupMenu;
import gov.sns.tools.plot.BasicGraphData;
import gov.sns.tools.plot.FunctionGraphsJPanel;
import gov.sns.tools.plot.IncrementalColors;
import gov.sns.xal.smf.impl.CurrentMonitor;
import gov.sns.xal.smf.impl.FCT;
import gov.sns.xal.smf.impl.RfCavity;
import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.swing.JOptionPane;
import jp.jparc.apps.iTuning.Scan2D.*;

/**
 * This class contains the components internal to the Scanning procedure.
 * @author  jdg
 */
public class ScanStuff implements ConnectionListener {

    /** the document this scanstuff belongs to */
    iTuningDocument theDoc;

    /** the parametric scan variable (cavity amplitude) */
    protected ScanVariable scanVariableParameter = null;

    /** the scan variable (cavity phase) */
    protected ScanVariable scanVariable = null;

    /**scan controller for 2D scans*/
    protected Scan2D scanController;

    protected AvgController avgCntr;

    /** validation controller - used with a SCT */
    protected ValidationController vldCntr;

    /** MPS Status Validation */
    protected MPSStatusValidator mpsCntr;

    /**graph panel to display scanned data */
    protected FunctionGraphsJPanel graphScan;

    /** container for the measured variables FCT phases */
    protected Vector measuredValuesV;

    /** the measured quantities for the Scan */
    protected MeasuredValue longUpFCTVoltMVNorm, longDownFCTVoltMVNorm;

    protected MeasuredValue shortUpFCTVoltMVNorm, shortDownFCTVoltMVNorm;

    protected MeasuredValue longUpFCTVoltMVInv, longDownFCTVoltMVInv;

    protected MeasuredValue shortUpFCTVoltMVInv, shortDownFCTVoltMVInv;

    protected MeasuredValue cav1AmpRBMV, cav1PhaseRBMV, cav2AmpRBMV, cav2PhaseRBMV;

    protected MeasuredValue sctMV, mpsStatusMV;

    private String cavAmpPVName, cavPhasePVName, ampStrobePVName, phaseStrobePVName;

    private String cavAmpPVRBName, cavPhasePVRBName;

    private String cav1AmpRBPVName, cav1PhaseRBPVName, cav2AmpRBPVName, cav2PhaseRBPVName;

    private String MPSStatusPVName;

    private String sctPVName;

    private class getFctPVName {

        private FCT fct;

        private String FCTVoltPVName = null;

        private String FCTSwitchPVName = null;

        private String FCTSwitchRBPVName = null;

        private String FCTGainPVName = null;

        private String FCTGainRBPVName = null;

        /** get the PV name for voltage detected by FCT*/
        protected String getFCTVoltPVName() {
            return this.FCTVoltPVName;
        }

        /** get the PV name of phase detector switcher */
        protected String getFCTSwitchPVName() {
            return this.FCTSwitchPVName;
        }

        /** get the PV name of phase detector switcher readback */
        protected String getFCTSwitchRBPVName() {
            return this.FCTSwitchRBPVName;
        }

        /** get the PV name of phase detector attenuator */
        protected String getFCTGainPVName() {
            return this.FCTGainPVName;
        }

        /** get the PV name of phase detector attenuator readback */
        protected String getFCTGainRBPVName() {
            return this.FCTGainRBPVName;
        }

        /** get the FCT */
        protected FCT getFCT() {
            return this.fct;
        }

        /** A method to set FCT PV name and channel */
        protected void setFctPvName(FCT fct) {
            this.fct = fct;
            this.FCTVoltPVName = fct.getChannel(FCT.VOLT_AVG_HANDLE).getId();
            this.FCTSwitchPVName = fct.getChannel(FCT.PHASE_SWITCH_HANDLE).getId();
            this.FCTSwitchRBPVName = fct.getChannel(FCT.SWITCH_RB_HANDLE).getId();
            this.FCTGainPVName = fct.getChannel(FCT.PHASE_GAIN_HANDLE).getId();
            this.FCTGainRBPVName = fct.getChannel(FCT.GAIN_RB_HANDLE).getId();
        }
    }

    private getFctPVName longUpFctPV = new getFctPVName();

    private getFctPVName longDownFctPV = new getFctPVName();

    private getFctPVName shortUpFctPV = new getFctPVName();

    private getFctPVName shortDownFctPV = new getFctPVName();

    protected String thePVText;

    private Map connectionMap;

    /** workaround to avoid jca context initialization exception */
    static {
        ChannelFactory.defaultFactory().init();
    }

    /** Create an object */
    public ScanStuff(iTuningDocument doc) {
        theDoc = doc;
        graphScan = new FunctionGraphsJPanel();
        scanController = new Scan2D("SCAN CONTROL PANEL (Fixed)");
        avgCntr = new AvgController();
        vldCntr = new ValidationController(2., 100.);
        mpsCntr = new MPSStatusValidator();
        scanVariable = new ScanVariable("cavPhaseSP", "cavPhaseRB", 2);
        scanVariableParameter = new ScanVariable("cavAmpSP", "cavAmpRB", 1);
        sctMV = new MeasuredValue("sctCurrent (Fixed)");
        measuredValuesV = new Vector();
        measuredValuesV.clear();
        connectionMap = Collections.synchronizedMap(new HashMap());
        initScan2D();
        IncrementalColors.setColor(0, Color.blue);
        IncrementalColors.setColor(1, Color.red);
        IncrementalColors.setColor(2, Color.black);
    }

    /** initialize the 2-D scan controller used with the cavity on */
    private void initScan2D() {
        scanController.getParamUnitsLabel().setText(" AU ");
        scanController.setParamLowLimit(2000);
        scanController.setParamUppLimit(4000);
        scanController.setParamStep(200);
        scanController.getUnitsLabel().setText(" deg ");
        scanController.setLowLimit(0.0);
        scanController.setUppLimit(360.0);
        scanController.setScanVariable(scanVariable);
        scanController.setParamVariable(scanVariableParameter);
        scanController.setAvgController(avgCntr);
        scanController.setValidationController(vldCntr);
        scanController.addValidationValue(sctMV);
        scanController.setMPSStatusValidator(mpsCntr);
        scanController.addMpsValidationValue(mpsStatusMV);
        scanController.addNewPointOfDataListener(new ActionListener() {

            public void actionPerformed(ActionEvent evn) {
                graphScan.refreshGraphJPanel();
            }
        });
        scanController.addNewSetOfDataListener(new ActionListener() {

            public void actionPerformed(ActionEvent evn) {
                newSetOfData();
            }
        });
        scanController.addStartButtonListener(new ActionListener() {

            public void actionPerformed(ActionEvent evn) {
                initScan();
            }
        });
        scanController.setFontForAll(new Font("Monospaced", Font.PLAIN, 12));
        avgCntr.setFontForAll(new Font("Monospaced", Font.PLAIN, 12));
        SimpleChartPopupMenu.addPopupMenuTo(graphScan);
        graphScan.setOffScreenImageDrawing(true);
        graphScan.setName("SCAN : FCT Values vs. Cavity Phase");
        graphScan.setAxisNames("Cavity Phase (deg)", "Measured Values");
        graphScan.setGraphBackGroundColor(Color.white);
        graphScan.setLegendButtonVisible(true);
    }

    /** Initialize the scan, when the start button is pressed */
    private void newSetOfData() {
        DecimalFormat valueFormat = new DecimalFormat("###.###");
        String paramPV_string = "";
        String scanPV_string = "";
        String measurePV_string = "";
        String legend_string = "";
        Double paramValue = new Double(scanController.getParamValue());
        Double paramValueRB = new Double(scanController.getParamValueRB());
        if (scanVariableParameter.getChannel() != null) {
            String paramValString = valueFormat.format(scanVariableParameter.getValue());
            paramPV_string = paramPV_string + " par.PV : " + scanVariableParameter.getChannel().getId() + "=" + paramValString;
            paramValue = new Double(scanVariableParameter.getValue());
        } else {
            paramPV_string = paramPV_string + " param.= " + paramValue;
        }
        if (scanVariable.getChannel() != null) {
            scanPV_string = "xPV=" + scanVariable.getChannel().getId();
        }
        for (int i = 0, n = measuredValuesV.size(); i < n; i++) {
            MeasuredValue mv_tmp = (MeasuredValue) measuredValuesV.get(i);
            BasicGraphData gd = mv_tmp.getDataContainer();
            if (mv_tmp.getChannel() != null) {
                measurePV_string = mv_tmp.getChannel().getId();
            }
            legend_string = mv_tmp.getAlias() + ": " + measurePV_string + paramPV_string + " ";
            if (gd != null) {
                gd.removeAllPoints();
                gd.setGraphProperty(graphScan.getLegendKeyString(), legend_string);
                if (paramValue != null) gd.setGraphProperty("PARAMETER_VALUE", paramValue);
                if (paramValueRB != null) gd.setGraphProperty("PARAMETER_VALUE_RB", paramValueRB);
            }
            if (scanVariable.getChannelRB() != null) {
                gd = mv_tmp.getDataContainerRB();
                if (gd != null) {
                    if (paramValue != null) gd.setGraphProperty("PARAMETER_VALUE", paramValue);
                    if (paramValueRB != null) gd.setGraphProperty("PARAMETER_VALUE_RB", paramValueRB);
                }
            }
        }
        updateGraphPanel();
        theDoc.setHasChanges(true);
    }

    /** method to update the graphPanel data (for raw scanned data  only)*/
    protected void updateGraphPanel() {
        graphScan.removeAllGraphData();
        int nCurves = 15;
        if (measuredValuesV.size() < nCurves) nCurves = measuredValuesV.size();
        for (int i = 0, n = nCurves; i < n; i++) {
            MeasuredValue mv_tmp = (MeasuredValue) measuredValuesV.get(i);
            graphScan.addGraphData(mv_tmp.getDataContainers());
        }
        graphScan.refreshGraphJPanel();
    }

    /** set the scan + measured PVs, based on the selected FCTs = cavity */
    protected void updateScanVariables(RfCavity[] cav, CurrentMonitor sct) {
        graphScan.removeAllGraphData();
        String cav1AmpPVName = cav[0].getChannel(RfCavity.CAV_AMP_SET_HANDLE).getId();
        if (cav.length == 2) {
            String cav2AmpPVName = cav[1].getChannel(RfCavity.CAV_AMP_SET_HANDLE).getId();
            if (!cav1AmpPVName.equals(cav2AmpPVName)) {
                JOptionPane.showMessageDialog(theDoc.myWindow(), "The EPICS Channel name is mismatched for Cavity amplitude set.");
                return;
            }
        }
        cavAmpPVName = cav1AmpPVName;
        String cav1AmpPVRBName = cav[0].getChannel(RfCavity.CAV_AMP_RB_HANDLE).getId();
        if (cav.length == 2) {
            String cav2AmpPVRBName = cav[1].getChannel(RfCavity.CAV_AMP_RB_HANDLE).getId();
            if (!cav1AmpPVRBName.equals(cav2AmpPVRBName)) {
                JOptionPane.showMessageDialog(theDoc.myWindow(), "The EPICS Channel name is mismatched for Cavity amplitude setting readback.");
                return;
            }
        }
        cavAmpPVRBName = cav1AmpPVRBName;
        String cav1PhasePVName = cav[0].getChannel(RfCavity.CAV_PHASE_SET_HANDLE).getId();
        if (cav.length == 2) {
            String cav2PhasePVName = cav[1].getChannel(RfCavity.CAV_PHASE_SET_HANDLE).getId();
            if (!cav1PhasePVName.equals(cav2PhasePVName)) {
                JOptionPane.showMessageDialog(theDoc.myWindow(), "The EPICS Channel name is mismatched for Cavity phase set.");
                return;
            }
        }
        cavPhasePVName = cav1PhasePVName;
        String cav1PhasePVRBName = cav[0].getChannel(RfCavity.CAV_PHASE_RB_HANDLE).getId();
        if (cav.length == 2) {
            String cav2PhasePVRBName = cav[1].getChannel(RfCavity.CAV_PHASE_RB_HANDLE).getId();
            if (!cav1PhasePVRBName.equals(cav2PhasePVRBName)) {
                JOptionPane.showMessageDialog(theDoc.myWindow(), "The EPICS Channel name is mismatched for Cavity phase setting readback.");
                return;
            }
        }
        cavPhasePVRBName = cav1PhasePVRBName;
        String amp1StrobePVName = cav[0].getChannel(RfCavity.CAV_AMP_STROBE_HANDLE).getId();
        if (cav.length == 2) {
            String amp2StrobePVName = cav[1].getChannel(RfCavity.CAV_AMP_STROBE_HANDLE).getId();
            if (!amp1StrobePVName.equals(amp2StrobePVName)) {
                JOptionPane.showMessageDialog(theDoc.myWindow(), "The EPICS Channel name is mismatched for strobe signal of Cavity amplitude set.");
                return;
            }
        }
        ampStrobePVName = amp1StrobePVName;
        String phase1StrobePVName = cav[0].getChannel(RfCavity.CAV_PHASE_STROBE_HANDLE).getId();
        if (cav.length == 2) {
            String phase2StrobePVName = cav[1].getChannel(RfCavity.CAV_PHASE_STROBE_HANDLE).getId();
            if (!phase1StrobePVName.equals(phase2StrobePVName)) {
                JOptionPane.showMessageDialog(theDoc.myWindow(), "The EPICS Channel name is mismatched for strobe signal of Cavity phase set.");
                return;
            }
        }
        phaseStrobePVName = phase1StrobePVName;
        cav1AmpRBPVName = cav[0].getChannel(RfCavity.CAV_AMP_AVG_HANDLE).getId();
        cav1PhaseRBPVName = cav[0].getChannel(RfCavity.CAV_PHASE_AVG_HANDLE).getId();
        if (cav.length == 2) {
            cav2AmpRBPVName = cav[1].getChannel(RfCavity.CAV_AMP_AVG_HANDLE).getId();
            cav2PhaseRBPVName = cav[1].getChannel(RfCavity.CAV_PHASE_AVG_HANDLE).getId();
        }
        String cav1MPSPVName = cav[0].getChannel(RfCavity.MPS_STATUS_HANDLE).getId();
        if (cav.length == 2) {
            String cav2MPSPVName = cav[1].getChannel(RfCavity.MPS_STATUS_HANDLE).getId();
            if (!cav1MPSPVName.equals(cav2MPSPVName)) {
                JOptionPane.showMessageDialog(theDoc.myWindow(), "The EPICS Channel name is mismatched for MPS status checking.");
                return;
            }
        }
        MPSStatusPVName = cav1MPSPVName;
        if (sct != null) {
            sctPVName = sct.getChannel(CurrentMonitor.I_AVG_HANDLE).getId();
        }
        longUpFctPV.setFctPvName(theDoc.longUpFCT);
        longDownFctPV.setFctPvName(theDoc.longDownFCT);
        if (theDoc.paramsStuff.useDoublePair && theDoc.shortUpFCT != null && theDoc.shortDownFCT != null) {
            if (theDoc.shortUpFCT == theDoc.longUpFCT) {
                shortUpFctPV = longUpFctPV;
            } else if (theDoc.shortUpFCT == theDoc.longDownFCT) {
                shortUpFctPV = longDownFctPV;
            } else {
                shortUpFctPV.setFctPvName(theDoc.shortUpFCT);
            }
            if (theDoc.shortDownFCT == theDoc.longUpFCT) {
                shortDownFctPV = longUpFctPV;
            } else if (theDoc.shortDownFCT == theDoc.longDownFCT) {
                shortDownFctPV = longDownFctPV;
            } else {
                shortDownFctPV.setFctPvName(theDoc.shortDownFCT);
            }
        }
        initMeasuredValueV();
        scanVariable.setChannel(cav[0].getChannel(RfCavity.CAV_PHASE_SET_HANDLE));
        scanVariable.setChannelRB(cav[0].getChannel(RfCavity.CAV_PHASE_RB_HANDLE));
        scanVariable.setStrobeChan(cav[0].getChannel(RfCavity.CAV_PHASE_STROBE_HANDLE));
        scanVariableParameter.setChannel(cav[0].getChannel(RfCavity.CAV_AMP_SET_HANDLE));
        scanVariableParameter.setChannelRB(cav[0].getChannel(RfCavity.CAV_AMP_RB_HANDLE));
        scanVariableParameter.setStrobeChan(cav[0].getChannel(RfCavity.CAV_AMP_STROBE_HANDLE));
        longUpFCTVoltMVNorm.setChannel(longUpFctPV.getFCT().getChannel(FCT.VOLT_AVG_HANDLE));
        longUpFCTVoltMVNorm.setSwitchChan(true, longUpFctPV.getFCT().getChannel(FCT.PHASE_SWITCH_HANDLE), longUpFctPV.getFCT().getChannel(FCT.SWITCH_RB_HANDLE), 0);
        longUpFCTVoltMVInv.setChannel(longUpFctPV.getFCT().getChannel(FCT.VOLT_AVG_HANDLE));
        longUpFCTVoltMVInv.setSwitchChan(true, longUpFctPV.getFCT().getChannel(FCT.PHASE_SWITCH_HANDLE), longUpFctPV.getFCT().getChannel(FCT.SWITCH_RB_HANDLE), 1);
        longDownFCTVoltMVNorm.setChannel(longDownFctPV.getFCT().getChannel(FCT.VOLT_AVG_HANDLE));
        longDownFCTVoltMVNorm.setSwitchChan(true, longDownFctPV.getFCT().getChannel(FCT.PHASE_SWITCH_HANDLE), longDownFctPV.getFCT().getChannel(FCT.SWITCH_RB_HANDLE), 0);
        longDownFCTVoltMVInv.setChannel(longDownFctPV.getFCT().getChannel(FCT.VOLT_AVG_HANDLE));
        longDownFCTVoltMVInv.setSwitchChan(true, longDownFctPV.getFCT().getChannel(FCT.PHASE_SWITCH_HANDLE), longDownFctPV.getFCT().getChannel(FCT.SWITCH_RB_HANDLE), 1);
        if (theDoc.paramsStuff.useDoublePair) {
            if (theDoc.shortUpFCT == theDoc.longUpFCT) {
                shortUpFCTVoltMVNorm = longUpFCTVoltMVNorm;
                shortUpFCTVoltMVInv = longUpFCTVoltMVInv;
            } else if (theDoc.shortUpFCT == theDoc.longDownFCT) {
                shortUpFCTVoltMVNorm = longDownFCTVoltMVNorm;
                shortUpFCTVoltMVInv = longDownFCTVoltMVInv;
            } else {
                shortUpFCTVoltMVNorm.setChannel(shortUpFctPV.getFCT().getChannel(FCT.VOLT_AVG_HANDLE));
                shortUpFCTVoltMVNorm.setSwitchChan(true, shortUpFctPV.getFCT().getChannel(FCT.PHASE_SWITCH_HANDLE), shortUpFctPV.getFCT().getChannel(FCT.SWITCH_RB_HANDLE), 0);
                shortUpFCTVoltMVInv.setChannel(shortUpFctPV.getFCT().getChannel(FCT.VOLT_AVG_HANDLE));
                shortUpFCTVoltMVInv.setSwitchChan(true, shortUpFctPV.getFCT().getChannel(FCT.PHASE_SWITCH_HANDLE), shortUpFctPV.getFCT().getChannel(FCT.SWITCH_RB_HANDLE), 1);
            }
            if (theDoc.shortDownFCT == theDoc.longUpFCT) {
                shortDownFCTVoltMVNorm = longUpFCTVoltMVNorm;
                shortDownFCTVoltMVInv = longUpFCTVoltMVInv;
            } else if (theDoc.shortDownFCT == theDoc.longDownFCT) {
                shortDownFCTVoltMVNorm = longDownFCTVoltMVNorm;
                shortDownFCTVoltMVInv = longDownFCTVoltMVInv;
            } else {
                shortDownFCTVoltMVNorm.setChannel(shortDownFctPV.getFCT().getChannel(FCT.VOLT_AVG_HANDLE));
                shortDownFCTVoltMVNorm.setSwitchChan(true, shortDownFctPV.getFCT().getChannel(FCT.PHASE_SWITCH_HANDLE), shortDownFctPV.getFCT().getChannel(FCT.SWITCH_RB_HANDLE), 0);
                shortDownFCTVoltMVInv.setChannel(shortDownFctPV.getFCT().getChannel(FCT.VOLT_AVG_HANDLE));
                shortDownFCTVoltMVInv.setSwitchChan(true, shortDownFctPV.getFCT().getChannel(FCT.PHASE_SWITCH_HANDLE), shortDownFctPV.getFCT().getChannel(FCT.SWITCH_RB_HANDLE), 1);
            }
        }
        cav1AmpRBMV.setChannel(cav[0].getChannel(RfCavity.CAV_AMP_AVG_HANDLE));
        cav1PhaseRBMV.setChannel(cav[0].getChannel(RfCavity.CAV_PHASE_AVG_HANDLE));
        if (theDoc.theCavity.length == 2) {
            cav2AmpRBMV.setChannel(cav[1].getChannel(RfCavity.CAV_AMP_AVG_HANDLE));
            cav2PhaseRBMV.setChannel(cav[1].getChannel(RfCavity.CAV_PHASE_AVG_HANDLE));
        }
        if (theDoc.theBCM != null) {
            sctMV.setChannel(sct.getChannel(CurrentMonitor.I_AVG_HANDLE));
        }
        mpsStatusMV.setChannel(cav[0].getChannel(RfCavity.MPS_STATUS_HANDLE));
        Channel.flushIO();
        connectChannels();
        scanController.setScanVariable(scanVariable);
        scanController.setParamVariable(scanVariableParameter);
        setPVText();
    }

    private void initMeasuredValueV() {
        longUpFCTVoltMVNorm = new MeasuredValue("longUpFCTVoltNorm");
        measuredValuesV.add(longUpFCTVoltMVNorm);
        longDownFCTVoltMVNorm = new MeasuredValue("longDownFCTVoltNorm");
        measuredValuesV.add(longDownFCTVoltMVNorm);
        if (theDoc.paramsStuff.useDoublePair) {
            if (theDoc.shortUpFCT != theDoc.longUpFCT && theDoc.shortUpFCT != theDoc.longDownFCT) {
                shortUpFCTVoltMVNorm = new MeasuredValue("shortUpFCTVoltNorm");
                measuredValuesV.add(shortUpFCTVoltMVNorm);
            }
            if (theDoc.shortDownFCT != theDoc.longUpFCT && theDoc.shortDownFCT != theDoc.longDownFCT) {
                shortDownFCTVoltMVNorm = new MeasuredValue("shortDownFCTVoltNorm");
                measuredValuesV.add(shortDownFCTVoltMVNorm);
            }
        }
        longUpFCTVoltMVInv = new MeasuredValue("longUpFCTVoltInv");
        measuredValuesV.add(longUpFCTVoltMVInv);
        longDownFCTVoltMVInv = new MeasuredValue("longDownFCTVoltInv");
        measuredValuesV.add(longDownFCTVoltMVInv);
        if (theDoc.paramsStuff.useDoublePair) {
            if (theDoc.shortUpFCT != theDoc.longUpFCT && theDoc.shortUpFCT != theDoc.longDownFCT) {
                shortUpFCTVoltMVInv = new MeasuredValue("shortUpFCTVoltInv");
                measuredValuesV.add(shortUpFCTVoltMVInv);
            }
            if (theDoc.shortDownFCT != theDoc.longUpFCT && theDoc.shortDownFCT != theDoc.longDownFCT) {
                shortDownFCTVoltMVInv = new MeasuredValue("shortDownFCTVoltInv");
                measuredValuesV.add(shortDownFCTVoltMVInv);
            }
        }
        cav1AmpRBMV = new MeasuredValue("cav1AmpRB");
        measuredValuesV.add(cav1AmpRBMV);
        cav1PhaseRBMV = new MeasuredValue("cav1PhaseRB");
        measuredValuesV.add(cav1PhaseRBMV);
        if (theDoc.theCavity.length == 2) {
            cav2AmpRBMV = new MeasuredValue("cav2AmpRB");
            measuredValuesV.add(cav2AmpRBMV);
            cav2PhaseRBMV = new MeasuredValue("cav2PhaseRB");
            measuredValuesV.add(cav2PhaseRBMV);
        }
        if (theDoc.theBCM != null) {
            measuredValuesV.add(sctMV);
        }
        mpsStatusMV = new MeasuredValue("MPSStatus");
        Iterator itr = measuredValuesV.iterator();
        while (itr.hasNext()) {
            scanController.addMeasuredValue((MeasuredValue) itr.next());
        }
    }

    /** clear all data and start over */
    private void initScan() {
        graphScan.removeAllGraphData();
        for (int i = 0, n = measuredValuesV.size(); i < n; i++) {
            MeasuredValue mv_tmp = (MeasuredValue) measuredValuesV.get(i);
            mv_tmp.removeAllDataContainers();
        }
        setColors(1);
    }

    /** Set colors of raw data scans */
    protected void setColors(int deleteIndex) {
        for (int i = 0, n = measuredValuesV.size(); i < n; i++) {
            MeasuredValue mv_tmp = (MeasuredValue) measuredValuesV.get(i);
            mv_tmp.setColor(IncrementalColor.getColor(i));
        }
        graphScan.refreshGraphJPanel();
    }

    /** try and force a channel connection to the required channels */
    private void connectChannels() {
        connectionMap.clear();
        int chanCounter = 0;
        connectionMap.put(longUpFctPV.getFCTVoltPVName(), new Boolean(false));
        connectionMap.put(longUpFctPV.getFCTSwitchPVName(), new Boolean(false));
        connectionMap.put(longUpFctPV.getFCTSwitchRBPVName(), new Boolean(false));
        connectionMap.put(longDownFctPV.getFCTVoltPVName(), new Boolean(false));
        connectionMap.put(longDownFctPV.getFCTSwitchPVName(), new Boolean(false));
        connectionMap.put(longDownFctPV.getFCTSwitchRBPVName(), new Boolean(false));
        if (theDoc.paramsStuff.useDoublePair) {
            if (theDoc.shortUpFCT != theDoc.longUpFCT && theDoc.shortUpFCT != theDoc.longDownFCT) {
                connectionMap.put(shortUpFctPV.getFCTVoltPVName(), new Boolean(false));
                connectionMap.put(shortUpFctPV.getFCTSwitchPVName(), new Boolean(false));
                connectionMap.put(shortUpFctPV.getFCTSwitchRBPVName(), new Boolean(false));
            }
            if (theDoc.shortDownFCT != theDoc.longUpFCT && theDoc.shortDownFCT != theDoc.longDownFCT) {
                connectionMap.put(shortDownFctPV.getFCTVoltPVName(), new Boolean(false));
                connectionMap.put(shortDownFctPV.getFCTSwitchPVName(), new Boolean(false));
                connectionMap.put(shortDownFctPV.getFCTSwitchRBPVName(), new Boolean(false));
            }
        }
        connectionMap.put(cavAmpPVName, new Boolean(false));
        connectionMap.put(cavPhasePVName, new Boolean(false));
        connectionMap.put(cavAmpPVRBName, new Boolean(false));
        connectionMap.put(cavPhasePVRBName, new Boolean(false));
        connectionMap.put(ampStrobePVName, new Boolean(false));
        connectionMap.put(phaseStrobePVName, new Boolean(false));
        connectionMap.put(cav1AmpRBPVName, new Boolean(false));
        connectionMap.put(cav1PhaseRBPVName, new Boolean(false));
        if (theDoc.theCavity.length == 2) {
            connectionMap.put(cav2AmpRBPVName, new Boolean(false));
            connectionMap.put(cav2PhaseRBPVName, new Boolean(false));
        }
        connectionMap.put(MPSStatusPVName, new Boolean(false));
        if (theDoc.theBCM != null) {
            connectionMap.put(sctPVName, new Boolean(false));
        }
        longUpFctPV.getFCT().getChannel(FCT.VOLT_AVG_HANDLE).addConnectionListener(this);
        longUpFctPV.getFCT().getChannel(FCT.PHASE_SWITCH_HANDLE).addConnectionListener(this);
        longUpFctPV.getFCT().getChannel(FCT.SWITCH_RB_HANDLE).addConnectionListener(this);
        longDownFctPV.getFCT().getChannel(FCT.VOLT_AVG_HANDLE).addConnectionListener(this);
        longDownFctPV.getFCT().getChannel(FCT.PHASE_SWITCH_HANDLE).addConnectionListener(this);
        longDownFctPV.getFCT().getChannel(FCT.SWITCH_RB_HANDLE).addConnectionListener(this);
        if (theDoc.paramsStuff.useDoublePair) {
            if (theDoc.shortUpFCT != theDoc.longUpFCT && theDoc.shortUpFCT != theDoc.longDownFCT) {
                shortUpFctPV.getFCT().getChannel(FCT.VOLT_AVG_HANDLE).addConnectionListener(this);
                shortUpFctPV.getFCT().getChannel(FCT.PHASE_SWITCH_HANDLE).addConnectionListener(this);
                shortUpFctPV.getFCT().getChannel(FCT.SWITCH_RB_HANDLE).addConnectionListener(this);
            }
            if (theDoc.shortDownFCT != theDoc.longUpFCT && theDoc.shortDownFCT != theDoc.longDownFCT) {
                shortDownFctPV.getFCT().getChannel(FCT.VOLT_AVG_HANDLE).addConnectionListener(this);
                shortDownFctPV.getFCT().getChannel(FCT.PHASE_SWITCH_HANDLE).addConnectionListener(this);
                shortDownFctPV.getFCT().getChannel(FCT.SWITCH_RB_HANDLE).addConnectionListener(this);
            }
        }
        theDoc.theCavity[0].getChannel(RfCavity.CAV_AMP_SET_HANDLE).addConnectionListener(this);
        theDoc.theCavity[0].getChannel(RfCavity.CAV_PHASE_SET_HANDLE).addConnectionListener(this);
        theDoc.theCavity[0].getChannel(RfCavity.CAV_AMP_RB_HANDLE).addConnectionListener(this);
        theDoc.theCavity[0].getChannel(RfCavity.CAV_PHASE_RB_HANDLE).addConnectionListener(this);
        theDoc.theCavity[0].getChannel(RfCavity.CAV_AMP_STROBE_HANDLE).addConnectionListener(this);
        theDoc.theCavity[0].getChannel(RfCavity.CAV_PHASE_STROBE_HANDLE).addConnectionListener(this);
        theDoc.theCavity[0].getChannel(RfCavity.CAV_AMP_AVG_HANDLE).addConnectionListener(this);
        theDoc.theCavity[0].getChannel(RfCavity.CAV_PHASE_AVG_HANDLE).addConnectionListener(this);
        if (theDoc.theCavity.length == 2) {
            theDoc.theCavity[1].getChannel(RfCavity.CAV_AMP_AVG_HANDLE).addConnectionListener(this);
            theDoc.theCavity[1].getChannel(RfCavity.CAV_PHASE_AVG_HANDLE).addConnectionListener(this);
        }
        theDoc.theCavity[0].getChannel(RfCavity.MPS_STATUS_HANDLE).addConnectionListener(this);
        if (theDoc.theBCM != null) {
            theDoc.theBCM.getChannel(CurrentMonitor.I_AVG_HANDLE).addConnectionListener(this);
        }
        longUpFctPV.getFCT().getChannel(FCT.VOLT_AVG_HANDLE).requestConnection();
        chanCounter++;
        longUpFctPV.getFCT().getChannel(FCT.PHASE_SWITCH_HANDLE).requestConnection();
        chanCounter++;
        longUpFctPV.getFCT().getChannel(FCT.SWITCH_RB_HANDLE).requestConnection();
        chanCounter++;
        longDownFctPV.getFCT().getChannel(FCT.VOLT_AVG_HANDLE).requestConnection();
        chanCounter++;
        longDownFctPV.getFCT().getChannel(FCT.PHASE_SWITCH_HANDLE).requestConnection();
        chanCounter++;
        longDownFctPV.getFCT().getChannel(FCT.SWITCH_RB_HANDLE).requestConnection();
        chanCounter++;
        if (theDoc.paramsStuff.useDoublePair) {
            if (theDoc.shortUpFCT != theDoc.longUpFCT && theDoc.shortUpFCT != theDoc.longDownFCT) {
                shortUpFctPV.getFCT().getChannel(FCT.VOLT_AVG_HANDLE).requestConnection();
                shortUpFctPV.getFCT().getChannel(FCT.PHASE_SWITCH_HANDLE).requestConnection();
                chanCounter++;
                shortUpFctPV.getFCT().getChannel(FCT.SWITCH_RB_HANDLE).requestConnection();
                chanCounter++;
            }
            if (theDoc.shortDownFCT != theDoc.longUpFCT && theDoc.shortDownFCT != theDoc.longDownFCT) {
                shortDownFctPV.getFCT().getChannel(FCT.VOLT_AVG_HANDLE).requestConnection();
                chanCounter++;
                shortDownFctPV.getFCT().getChannel(FCT.PHASE_SWITCH_HANDLE).requestConnection();
                chanCounter++;
                shortDownFctPV.getFCT().getChannel(FCT.SWITCH_RB_HANDLE).requestConnection();
                chanCounter++;
            }
        }
        theDoc.theCavity[0].getChannel(RfCavity.CAV_AMP_SET_HANDLE).requestConnection();
        chanCounter++;
        theDoc.theCavity[0].getChannel(RfCavity.CAV_PHASE_SET_HANDLE).requestConnection();
        chanCounter++;
        theDoc.theCavity[0].getChannel(RfCavity.CAV_AMP_RB_HANDLE).requestConnection();
        chanCounter++;
        theDoc.theCavity[0].getChannel(RfCavity.CAV_PHASE_RB_HANDLE).requestConnection();
        chanCounter++;
        theDoc.theCavity[0].getChannel(RfCavity.CAV_AMP_STROBE_HANDLE).requestConnection();
        chanCounter++;
        theDoc.theCavity[0].getChannel(RfCavity.CAV_PHASE_STROBE_HANDLE).requestConnection();
        chanCounter++;
        theDoc.theCavity[0].getChannel(RfCavity.CAV_AMP_AVG_HANDLE).requestConnection();
        chanCounter++;
        theDoc.theCavity[0].getChannel(RfCavity.CAV_PHASE_AVG_HANDLE).requestConnection();
        chanCounter++;
        if (theDoc.theCavity.length == 2) {
            theDoc.theCavity[1].getChannel(RfCavity.CAV_AMP_AVG_HANDLE).requestConnection();
            chanCounter++;
            theDoc.theCavity[1].getChannel(RfCavity.CAV_PHASE_AVG_HANDLE).requestConnection();
            chanCounter++;
        }
        theDoc.theCavity[0].getChannel(RfCavity.MPS_STATUS_HANDLE).requestConnection();
        chanCounter++;
        if (theDoc.theBCM != null) {
            theDoc.theBCM.getChannel(CurrentMonitor.I_AVG_HANDLE).requestConnection();
            chanCounter++;
        }
        Channel.flushIO();
        int i = 0;
        int nDisconnects = chanCounter;
        while (nDisconnects > 0 && i < (chanCounter)) {
            try {
                Thread.currentThread().sleep(200);
            } catch (InterruptedException e) {
                System.out.println("Sleep interrupted during connection check");
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
            nDisconnects = 0;
            Set set = connectionMap.entrySet();
            Iterator itr = set.iterator();
            while (itr.hasNext()) {
                Map.Entry me = (Map.Entry) itr.next();
                Boolean tf = (Boolean) me.getValue();
                if (!(tf.booleanValue())) nDisconnects++;
            }
            i++;
        }
        if (nDisconnects > 0) {
            Toolkit.getDefaultToolkit().beep();
            theDoc.myWindow().errorText.setText((new Integer(nDisconnects)).toString() + " PVs were not able to connect");
            System.out.println(nDisconnects + " PVs were not able to connect");
        }
    }

    /** sets the text describing which PVs will be used */
    private void setPVText() {
        String scanpv1 = "RF phase scan PV: " + scanVariable.getChannelName() + "\n";
        scanpv1 += "RF phase strobe PV: " + scanVariable.getStrobeChan().channelName() + "\n";
        String scanpv2 = "RF amplitude scan PV: " + scanVariableParameter.getChannelName() + "\n";
        scanpv2 += "RF amplitude strobe PV: " + scanVariableParameter.getStrobeChan().channelName() + "\n";
        Iterator itr = measuredValuesV.iterator();
        int i = 1;
        String mvpvs = "\n";
        while (itr.hasNext()) {
            String name = ((MeasuredValue) itr.next()).getChannelName();
            mvpvs += "Monitor PV " + (new Integer(i)).toString() + ": " + name + "\n";
            i++;
        }
        Set connectSet = connectionMap.entrySet();
        Iterator connectItr = connectSet.iterator();
        String connects = "\nTotal channels: " + connectionMap.size();
        connects += "\nConnection Status:\n";
        while (connectItr.hasNext()) {
            Map.Entry me = (Map.Entry) connectItr.next();
            Boolean tf = (Boolean) me.getValue();
            connects += me.getKey().toString() + ": " + tf.toString() + "\n";
        }
        thePVText = scanpv1 + scanpv2 + mvpvs + connects;
    }

    /** ConnectionListener interface methods */
    public void connectionMade(Channel chan) {
        String name = chan.getId();
        connectionMap.put(name, new Boolean(true));
    }

    public void connectionDropped(Channel chan) {
        String name = chan.getId();
        connectionMap.put(name, new Boolean(false));
    }
}
