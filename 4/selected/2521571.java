package jp.jparc.apps.iPod;

import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.text.*;
import gov.sns.tools.scan.SecondEdition.*;
import gov.sns.xal.smf.impl.*;
import gov.sns.tools.apputils.*;
import gov.sns.tools.plot.*;
import gov.sns.ca.*;

/**
 * This class contains the components internal to the Scanning procedure.
 * @author  jdg
 */
public class ScanStuff implements ConnectionListener {

    /** the parametric scan variable (cavity amplitude) */
    protected ScanVariable scanVariableParameter = null;

    /** the scan variable (cavity phase) */
    protected ScanVariable scanVariable = null;

    /** container for the measured variables FCT phases */
    protected Vector measuredValuesV;

    /** the measured quantities for the Scan */
    protected MeasuredValue FCT1PhaseMV, FCT2PhaseMV, FCT3PhaseMV, cavAmpRBMV, BCMMV;

    /**scan controller for 2D scans*/
    protected ScanController2D scanController;

    protected AvgController avgCntr;

    /**graph panel to display scanned data */
    protected FunctionGraphsJPanel graphScan;

    /** validation controller - used with a BCM */
    protected ValidationController vldCntr;

    /** names of the PVs used */
    private String cavAmpPVName, cavPhasePVName, FCT1PhasePVName, FCT2PhasePVName, FCT3PhasePVName, cavAmpRBPVName, BCMPVName;

    /** the document this scanstuff belongs to */
    iPodDocument theDoc;

    /** workaround to avoid jca context initialization exception */
    static {
        ChannelFactory.defaultFactory().init();
    }

    protected String thePVText;

    private Map connectionMap;

    /** since this PV is not inthe xal xml file make it here */
    private Channel cavAmpRBChan, BCMChan;

    /** Create an object */
    public ScanStuff(iPodDocument doc) {
        theDoc = doc;
        scanController = new ScanController2D("SCAN CONTROL PANEL");
        avgCntr = new AvgController();
        graphScan = new FunctionGraphsJPanel();
        vldCntr = new ValidationController(2., 100.);
        scanVariable = new ScanVariable("cavPhaseSP", "cavPhaseRB");
        scanVariableParameter = new ScanVariable("cavAmpSP", "cavAmpRB");
        FCT1PhaseMV = new MeasuredValue("FCT1Phase");
        FCT2PhaseMV = new MeasuredValue("FCT2Phase");
        FCT3PhaseMV = new MeasuredValue("FCT3Phase");
        cavAmpRBMV = new MeasuredValue("CavAmpRB");
        BCMMV = new MeasuredValue("BCMcurrent");
        measuredValuesV = new Vector();
        measuredValuesV.clear();
        measuredValuesV.add(FCT1PhaseMV);
        measuredValuesV.add(FCT2PhaseMV);
        measuredValuesV.add(FCT3PhaseMV);
        measuredValuesV.add(cavAmpRBMV);
        measuredValuesV.add(BCMMV);
        Iterator itr = measuredValuesV.iterator();
        while (itr.hasNext()) {
            scanController.addMeasuredValue((MeasuredValue) itr.next());
        }
        connectionMap = Collections.synchronizedMap(new HashMap());
        initScanController2D();
        IncrementalColors.setColor(0, Color.blue);
        IncrementalColors.setColor(1, Color.red);
        IncrementalColors.setColor(2, Color.black);
    }

    /** initialize the 2-D scan controller used with the cavity on */
    private void initScanController2D() {
        scanController.setValuePhaseScanButtonVisible(true);
        scanController.setValuePhaseScanButtonOn(true);
        scanController.getParamUnitsLabel().setText(" AU ");
        scanController.setParamLowLimit(0.5);
        scanController.setParamUppLimit(0.55);
        scanController.setParamStep(0.1);
        scanController.getUnitsLabel().setText(" deg ");
        scanController.setUppLimit(175.0);
        scanController.setScanVariable(scanVariable);
        scanController.setParamVariable(scanVariableParameter);
        scanController.setAvgController(avgCntr);
        scanController.setValidationController(vldCntr);
        scanController.addValidationValue(BCMMV);
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

    /** set the scan  + measured PVs, based on the selected FCTs  = cavity */
    protected void updateScanVariables(FCT fct1, FCT fct2, FCT fct3, RfCavity cav, CurrentMonitor bcm) {
        graphScan.removeAllGraphData();
        cavAmpPVName = (cav.getChannel(RfCavity.CAV_AMP_SET_HANDLE)).getId();
        cavPhasePVName = cav.getChannel(RfCavity.CAV_PHASE_SET_HANDLE).getId();
        FCT1PhasePVName = fct1.getChannel(FCT.FCT_AVG_HANDLE).getId();
        FCT2PhasePVName = fct2.getChannel(FCT.FCT_AVG_HANDLE).getId();
        if (fct3 != null) FCT3PhasePVName = fct3.getChannel(FCT.FCT_AVG_HANDLE).getId();
        cavAmpRBPVName = cav.getChannel(RfCavity.CAV_AMP_AVG_HANDLE).getId();
        if (bcm != null) BCMPVName = bcm.getChannel(bcm.I_AVG_HANDLE).getId();
        System.out.println("pv = " + cavAmpRBPVName + "   " + BCMPVName);
        cavAmpRBChan = ChannelFactory.defaultFactory().getChannel(cavAmpRBPVName);
        if (bcm != null) BCMChan = ChannelFactory.defaultFactory().getChannel(BCMPVName);
        scanVariable.setChannel(cav.getChannel(RfCavity.CAV_PHASE_SET_HANDLE));
        scanVariableParameter.setChannel(cav.getChannel(RfCavity.CAV_AMP_SET_HANDLE));
        FCT1PhaseMV.setChannel(fct1.getChannel(FCT.FCT_AVG_HANDLE));
        FCT2PhaseMV.setChannel(fct2.getChannel(FCT.FCT_AVG_HANDLE));
        if (fct3 != null) FCT3PhaseMV.setChannel(fct3.getChannel(FCT.FCT_AVG_HANDLE));
        cavAmpRBMV.setChannel(cavAmpRBChan);
        BCMMV.setChannel(BCMChan);
        connectChannels(fct1, fct2, fct3, cav);
        scanController.setScanVariable(scanVariable);
        scanController.setParamVariable(scanVariableParameter);
        setPVText();
    }

    /** try and force a channel connection to the required channels */
    private void connectChannels(FCT fct1, FCT fct2, FCT fct3, RfCavity cav) {
        connectionMap.clear();
        connectionMap.put(cavAmpPVName, new Boolean(false));
        connectionMap.put(cavPhasePVName, new Boolean(false));
        connectionMap.put(FCT1PhasePVName, new Boolean(false));
        connectionMap.put(FCT2PhasePVName, new Boolean(false));
        if (fct3 != null) connectionMap.put(FCT3PhasePVName, new Boolean(false));
        connectionMap.put(cavAmpRBPVName, new Boolean(false));
        connectionMap.put(BCMPVName, new Boolean(false));
        cav.getChannel(RfCavity.CAV_PHASE_SET_HANDLE).addConnectionListener(this);
        cav.getChannel(RfCavity.CAV_AMP_SET_HANDLE).addConnectionListener(this);
        fct1.getChannel(FCT.FCT_AVG_HANDLE).addConnectionListener(this);
        fct2.getChannel(FCT.FCT_AVG_HANDLE).addConnectionListener(this);
        if (fct3 != null) fct3.getChannel(FCT.FCT_AVG_HANDLE).addConnectionListener(this);
        if (cavAmpRBChan != null) cavAmpRBChan.addConnectionListener(this);
        if (BCMChan != null) BCMChan.addConnectionListener(this);
        cav.getChannel(RfCavity.CAV_PHASE_SET_HANDLE).requestConnection();
        cav.getChannel(RfCavity.CAV_AMP_SET_HANDLE).requestConnection();
        fct1.getChannel(FCT.FCT_AVG_HANDLE).requestConnection();
        fct2.getChannel(FCT.FCT_AVG_HANDLE).requestConnection();
        if (fct3 != null) fct3.getChannel(FCT.FCT_AVG_HANDLE).requestConnection();
        if (cavAmpRBChan != null) cavAmpRBChan.requestConnection();
        if (BCMChan != null) BCMChan.requestConnection();
        int i = 0;
        int nDisconnects = 6;
        while (nDisconnects > 0 && i < 5) {
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
        String scanpv1 = "RF phase scan PV: " + scanVariable.getChannelName() + "  " + connectionMap.get(cavAmpPVName) + "\n";
        String scanpv2 = "RF amplitude scan PV: " + scanVariableParameter.getChannelName() + "  " + connectionMap.get(cavPhasePVName) + "\n";
        Iterator itr = measuredValuesV.iterator();
        int i = 1;
        String mvpvs = "\n";
        while (itr.hasNext()) {
            String name = ((MeasuredValue) itr.next()).getChannelName();
            mvpvs += "FCT monitor PV " + (new Integer(i)).toString() + " : " + name + "  " + connectionMap.get(name) + "\n";
            i++;
        }
        thePVText = scanpv1 + scanpv2 + mvpvs;
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

    /** Set colors of raw data scans */
    protected void setColors(int deleteIndex) {
        for (int i = 0, n = measuredValuesV.size(); i < n; i++) {
            MeasuredValue mv_tmp = (MeasuredValue) measuredValuesV.get(i);
            mv_tmp.setColor(IncrementalColor.getColor(i));
        }
        graphScan.refreshGraphJPanel();
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
            legend_string = measurePV_string + paramPV_string + " ";
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

    /** ConnectionListener interface methods:*/
    public void connectionMade(Channel chan) {
        String name = chan.getId();
        connectionMap.put(name, new Boolean(true));
    }

    public void connectionDropped(Channel chan) {
    }
}
