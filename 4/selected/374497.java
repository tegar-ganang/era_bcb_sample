package gov.sns.apps.slacs;

import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import gov.sns.xal.smf.impl.*;
import gov.sns.tools.scan.SecondEdition.*;
import gov.sns.tools.apputils.*;
import gov.sns.tools.plot.*;
import gov.sns.tools.xml.*;
import gov.sns.tools.data.*;
import gov.sns.ca.*;

/**
 * This class contains the components internal to the Scanning procedure.
 * adapted from the pasta app. It contains 2 basic scan controllers. 
 * One for the cavity on, and another for the cavity off.
 * @author  jdg
 */
public class ScanStuff implements ConnectionListener {

    private String scanPV_SR = "scan_PV";

    /** the scan variable (cavity phase) */
    protected ScanVariable scanVariable = null;

    /** container for the measured variables (BPM phases + amplitudes) 
    * for cavity on and for cavity off */
    protected Vector measuredValuesOnV, measuredValuesOffV;

    /** the measured quantities for the Scan */
    protected MeasuredValue BPM1PhaseOnMV, BPM1AmpOnMV, BPM2PhaseOnMV, BPM2AmpOnMV, cavAmpRBOnMV, BCMOnMV;

    protected MeasuredValue BPM1PhaseOffMV, BPM1AmpOffMV, BPM2PhaseOffMV, BPM2AmpOffMV, BCMOffMV;

    /**scan controller for cavity on scan*/
    protected ScanController1D scanControllerOn;

    /**scan controller for 1D scan for cavity off*/
    protected ScanController1D scanControllerOff;

    protected AvgController avgCntrOn, avgCntrOff;

    /**graph panel to display scanned data */
    protected FunctionGraphsJPanel graphScanOn, graphScanOff;

    /** validation controller - used with a BCM */
    protected ValidationController vldCntrOn, vldCntrOff;

    /** the main panels for displaying the scan stuff */
    protected JPanel scanPanelOn = new JPanel();

    protected JPanel scanPanelOff = new JPanel();

    /** names of the PVs used */
    private String cavAmpPVName, cavPhasePVName, BPM1AmpPVName, BPM1PhasePVName, BPM2AmpPVName, BPM2PhasePVName, cavAmpRBPVName, BCMPVName;

    /** the conbtroller that has information on which devices to use */
    protected Controller controller;

    /** text area to display the PV connection info */
    protected JTextArea pvListTextAreaOn, pvListTextAreaOff;

    /** workaround to avoid jca context initialization exception */
    static {
        ChannelFactory.defaultFactory().init();
    }

    protected String thePVText;

    private Map connectionMap;

    /** since these PVs are not inthe xal xml file make em here */
    private Channel cavAmpRBChan, BCMChan;

    /** Create an object */
    public ScanStuff(Controller cntrl) {
        controller = cntrl;
        scanControllerOn = new ScanController1D("Empty");
        scanControllerOff = new ScanController1D("Empty");
        avgCntrOn = new AvgController();
        avgCntrOff = new AvgController();
        graphScanOn = new FunctionGraphsJPanel();
        graphScanOff = new FunctionGraphsJPanel();
        graphScanOn.setPreferredSize(new Dimension(500, 200));
        graphScanOff.setPreferredSize(new Dimension(500, 200));
        vldCntrOn = new ValidationController(2., 100.);
        vldCntrOff = new ValidationController(2., 100.);
        scanVariable = new ScanVariable("cavPhaseSP", "cavPhaseRB");
        BPM1PhaseOnMV = new MeasuredValue("BPM1PhaseOn");
        BPM1AmpOnMV = new MeasuredValue("BPM1AmpOn");
        BPM2PhaseOnMV = new MeasuredValue("BPM2PhaseOn");
        BPM2AmpOnMV = new MeasuredValue("BPM2AmpOn");
        cavAmpRBOnMV = new MeasuredValue("CavAmpRBOn");
        BCMOnMV = new MeasuredValue("BCMcurrentOn");
        BPM1PhaseOffMV = new MeasuredValue("BPM1PhaseOff");
        BPM1AmpOffMV = new MeasuredValue("BPM1AmpOff");
        BPM2PhaseOffMV = new MeasuredValue("BPM2PhaseOff");
        BPM2AmpOffMV = new MeasuredValue("BPM1AmpOff");
        BCMOffMV = new MeasuredValue("BCMcurrentOff");
        pvListTextAreaOn = new JTextArea();
        pvListTextAreaOn.setLineWrap(true);
        pvListTextAreaOn.setWrapStyleWord(true);
        pvListTextAreaOff = new JTextArea();
        pvListTextAreaOff.setLineWrap(true);
        pvListTextAreaOff.setWrapStyleWord(true);
        measuredValuesOnV = new Vector();
        measuredValuesOnV.clear();
        measuredValuesOnV.add(BPM1PhaseOnMV);
        measuredValuesOnV.add(BPM1AmpOnMV);
        measuredValuesOnV.add(BPM2PhaseOnMV);
        measuredValuesOnV.add(BPM2AmpOnMV);
        measuredValuesOnV.add(cavAmpRBOnMV);
        measuredValuesOnV.add(BCMOnMV);
        measuredValuesOffV = new Vector();
        measuredValuesOffV.clear();
        measuredValuesOffV.add(BPM1PhaseOffMV);
        measuredValuesOffV.add(BPM1AmpOffMV);
        measuredValuesOffV.add(BPM2PhaseOffMV);
        measuredValuesOffV.add(BPM2AmpOffMV);
        measuredValuesOffV.add(BCMOffMV);
        Iterator itr = measuredValuesOnV.iterator();
        while (itr.hasNext()) {
            scanControllerOn.addMeasuredValue((MeasuredValue) itr.next());
        }
        itr = measuredValuesOffV.iterator();
        while (itr.hasNext()) {
            scanControllerOff.addMeasuredValue((MeasuredValue) itr.next());
        }
        connectionMap = Collections.synchronizedMap(new HashMap());
        initScanControllerOn();
        initScanControllerOff();
        IncrementalColors.setColor(0, Color.blue);
        IncrementalColors.setColor(1, Color.red);
        IncrementalColors.setColor(2, Color.black);
    }

    /** initialize the 1-D scan controller used with the cavity on */
    private void initScanControllerOn() {
        scanControllerOn.getUnitsLabel().setText(" deg ");
        scanControllerOn.setUppLimit(175.0);
        scanControllerOn.setStep(20.);
        scanControllerOn.setSleepTime(5.);
        scanControllerOn.setBeamTriggerDelay(1.01);
        scanControllerOn.setScanVariable(scanVariable);
        scanControllerOn.setAvgController(avgCntrOn);
        scanControllerOn.setValidationController(vldCntrOn);
        scanControllerOn.addValidationValue(BCMOnMV);
        scanControllerOn.addNewPointOfDataListener(new ActionListener() {

            public void actionPerformed(ActionEvent evn) {
                graphScanOn.refreshGraphJPanel();
            }
        });
        scanControllerOn.addNewSetOfDataListener(new ActionListener() {

            public void actionPerformed(ActionEvent evn) {
                newSetOfCavOnData();
            }
        });
        scanControllerOn.addStartListener(new ActionListener() {

            public void actionPerformed(ActionEvent evn) {
                initScanOn();
            }
        });
        scanControllerOn.setFontForAll(new Font("Monospaced", Font.PLAIN, 12));
        avgCntrOn.setFontForAll(new Font("Monospaced", Font.PLAIN, 12));
        SimpleChartPopupMenu.addPopupMenuTo(graphScanOn);
        graphScanOn.setOffScreenImageDrawing(true);
        graphScanOn.setName("SCAN : BPM Values vs. Cavity Phase");
        graphScanOn.setAxisNames("Cavity Phase (deg)", "Measured Values");
        graphScanOn.setGraphBackGroundColor(Color.white);
        graphScanOn.setLegendButtonVisible(true);
        scanControllerOn.addStopListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (scanControllerOn.isScanON() || scanControllerOn.isContinueON()) controller.cavitySetAction(controller.tuneSet.getCavity(), Controller.SCAN_DONE, "scan prematurely ended"); else {
                    controller.theDoc.setHasChanges(true);
                    controller.cavitySetAction(controller.tuneSet.getCavity(), Controller.SCAN_DONE);
                }
            }
        });
        scanControllerOn.addStartListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                controller.cavitySetAction(controller.tuneSet.getCavity(), Controller.SCANNING);
            }
        });
    }

    /** initialize the 1-D scan controller used with the cavity off */
    private void initScanControllerOff() {
        scanControllerOff.getUnitsLabel().setText(" deg ");
        scanControllerOff.setUppLimit(179.0);
        scanControllerOff.setScanVariable(scanVariable);
        scanControllerOff.setAvgController(avgCntrOff);
        scanControllerOff.setValidationController(vldCntrOff);
        scanControllerOff.addValidationValue(BCMOffMV);
        scanControllerOff.addNewPointOfDataListener(new ActionListener() {

            public void actionPerformed(ActionEvent evn) {
                graphScanOff.refreshGraphJPanel();
            }
        });
        scanControllerOff.addNewSetOfDataListener(new ActionListener() {

            public void actionPerformed(ActionEvent evn) {
                newSetOfCavOffData();
            }
        });
        scanControllerOff.addStartListener(new ActionListener() {

            public void actionPerformed(ActionEvent evn) {
                initScanOff();
            }
        });
        scanControllerOff.setFontForAll(new Font("Monospaced", Font.PLAIN, 12));
        avgCntrOff.setFontForAll(new Font("Monospaced", Font.PLAIN, 12));
        SimpleChartPopupMenu.addPopupMenuTo(graphScanOff);
        graphScanOff.setOffScreenImageDrawing(true);
        graphScanOff.setName("BPM Values vs. Cavity Phase, Cav Off");
        graphScanOff.setAxisNames("Cavity Phase (deg)", "Measured Values");
        graphScanOff.setGraphBackGroundColor(Color.white);
        graphScanOff.setLegendButtonVisible(true);
    }

    /** construct the panel to display the scanning */
    protected JPanel makeScanCavOnPanel() {
        scanPanelOn = new JPanel(new BorderLayout());
        JPanel tmp_0 = new JPanel();
        tmp_0.setLayout(new VerticalLayout());
        tmp_0.add(scanControllerOn.getJPanel());
        tmp_0.add(avgCntrOn.getJPanel(0));
        tmp_0.add(vldCntrOn.getJPanel());
        JScrollPane pvTextScrollPane = new JScrollPane(pvListTextAreaOn);
        pvTextScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        pvTextScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tmp_0.add(pvTextScrollPane);
        scanPanelOn.add(tmp_0, BorderLayout.WEST);
        scanPanelOn.add(graphScanOn, BorderLayout.CENTER);
        JButton acceptDataButton = new JButton("Accept Data");
        acceptDataButton.setBackground(SlacsWindow.buttonColor);
        acceptDataButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                controller.acceptData();
            }
        });
        scanPanelOn.add(acceptDataButton, BorderLayout.SOUTH);
        return scanPanelOn;
    }

    /** construct the panel to display the scanning with the cavity off*/
    protected JPanel makeScanCavOffPanel() {
        scanPanelOff = new JPanel(new BorderLayout());
        JPanel tmp_0 = new JPanel();
        tmp_0.setLayout(new VerticalLayout());
        tmp_0.add(scanControllerOff.getJPanel());
        tmp_0.add(avgCntrOff.getJPanel(0));
        tmp_0.add(vldCntrOff.getJPanel());
        JScrollPane pvTextScrollPane = new JScrollPane(pvListTextAreaOff);
        pvTextScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        pvTextScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tmp_0.add(pvTextScrollPane);
        scanPanelOff.add(tmp_0, BorderLayout.WEST);
        scanPanelOff.add(graphScanOff, BorderLayout.CENTER);
        return scanPanelOff;
    }

    /** set the scan  + measured PVs, based on the selected BPMs  = cavity 
    returns true if all connections are made - otherwise returns false
    */
    protected boolean updateScanVariables(BPM bpm1, BPM bpm2, RfCavity cav, CurrentMonitor bcm) {
        graphScanOn.removeAllGraphData();
        graphScanOff.removeAllGraphData();
        cavAmpPVName = (cav.getChannel(RfCavity.CAV_AMP_SET_HANDLE)).getId();
        cavPhasePVName = cav.getChannel(RfCavity.CAV_PHASE_SET_HANDLE).getId();
        BPM1AmpPVName = bpm1.getChannel(BPM.AMP_AVG_HANDLE).getId();
        BPM1PhasePVName = bpm1.getChannel(BPM.PHASE_AVG_HANDLE).getId();
        BPM2AmpPVName = bpm2.getChannel(BPM.AMP_AVG_HANDLE).getId();
        BPM2PhasePVName = bpm2.getChannel(BPM.PHASE_AVG_HANDLE).getId();
        cavAmpRBPVName = cavAmpPVName.replaceFirst("CtlAmpSet", "cavAmpAvg");
        if (bcm != null) BCMPVName = bcm.getId() + ":currentAvg";
        cavAmpRBChan = ChannelFactory.defaultFactory().getChannel(cavAmpRBPVName);
        if (bcm != null) BCMChan = ChannelFactory.defaultFactory().getChannel(BCMPVName);
        scanVariable.setChannel(cav.getChannel(RfCavity.CAV_PHASE_SET_HANDLE));
        BPM1PhaseOnMV.setChannel(bpm1.getChannel(BPM.PHASE_AVG_HANDLE));
        BPM1AmpOnMV.setChannel(bpm1.getChannel(BPM.AMP_AVG_HANDLE));
        BPM2PhaseOnMV.setChannel(bpm2.getChannel(BPM.PHASE_AVG_HANDLE));
        BPM2AmpOnMV.setChannel(bpm2.getChannel(BPM.AMP_AVG_HANDLE));
        cavAmpRBOnMV.setChannel(cavAmpRBChan);
        BCMOnMV.setChannel(BCMChan);
        BPM1PhaseOffMV.setChannel(bpm1.getChannel(BPM.PHASE_AVG_HANDLE));
        BPM1AmpOffMV.setChannel(bpm1.getChannel(BPM.AMP_AVG_HANDLE));
        BPM2PhaseOffMV.setChannel(bpm2.getChannel(BPM.PHASE_AVG_HANDLE));
        BPM2AmpOffMV.setChannel(bpm2.getChannel(BPM.AMP_AVG_HANDLE));
        BCMOffMV.setChannel(BCMChan);
        connectChannels(bpm1, bpm2, cav);
        scanControllerOn.setScanVariable(scanVariable);
        scanControllerOff.setScanVariable(scanVariable);
        setPVText();
        setTitles(controller.tuneSet.getCavity().getId());
        if (connectionMap.containsValue(new Boolean(false))) return false; else return true;
    }

    protected void setTitles(String str) {
        scanControllerOn.setTitle(str + " Scan Panel");
        scanControllerOff.setTitle(str + " Scan Panel");
        graphScanOn.setName(str + ": BPM Values vs. Cavity Phase");
        graphScanOff.setName(str + ": BPM Values vs. Cavity Phase");
    }

    /** try and force a channel connection to the required channels */
    private void connectChannels(BPM bpm1, BPM bpm2, RfCavity cav) {
        System.out.println("checking connections");
        connectionMap.clear();
        connectionMap.put(cavAmpPVName, new Boolean(false));
        connectionMap.put(cavPhasePVName, new Boolean(false));
        connectionMap.put(BPM1AmpPVName, new Boolean(false));
        connectionMap.put(BPM1PhasePVName, new Boolean(false));
        connectionMap.put(BPM2AmpPVName, new Boolean(false));
        connectionMap.put(BPM2PhasePVName, new Boolean(false));
        connectionMap.put(cavAmpRBPVName, new Boolean(false));
        connectionMap.put(BCMPVName, new Boolean(false));
        cav.getChannel(RfCavity.CAV_PHASE_SET_HANDLE).addConnectionListener(this);
        cav.getChannel(RfCavity.CAV_AMP_SET_HANDLE).addConnectionListener(this);
        bpm1.getChannel(BPM.PHASE_AVG_HANDLE).addConnectionListener(this);
        bpm2.getChannel(BPM.PHASE_AVG_HANDLE).addConnectionListener(this);
        bpm1.getChannel(BPM.AMP_AVG_HANDLE).addConnectionListener(this);
        bpm2.getChannel(BPM.AMP_AVG_HANDLE).addConnectionListener(this);
        if (cavAmpRBChan != null) cavAmpRBChan.addConnectionListener(this);
        if (BCMChan != null) BCMChan.addConnectionListener(this);
        cav.getChannel(RfCavity.CAV_PHASE_SET_HANDLE).requestConnection();
        cav.getChannel(RfCavity.CAV_AMP_SET_HANDLE).requestConnection();
        bpm1.getChannel(BPM.PHASE_AVG_HANDLE).requestConnection();
        bpm2.getChannel(BPM.PHASE_AVG_HANDLE).requestConnection();
        bpm1.getChannel(BPM.AMP_AVG_HANDLE).requestConnection();
        bpm2.getChannel(BPM.AMP_AVG_HANDLE).requestConnection();
        if (cavAmpRBChan != null) cavAmpRBChan.requestConnection();
        if (BCMChan != null) BCMChan.requestConnection();
        Channel.flushIO();
        int i = 0;
        int nDisconnects = 6;
        while (nDisconnects > 0 && i < 3) {
            try {
                Thread.currentThread().sleep(500);
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
        if (nDisconnects > 0) controller.dumpErr(nDisconnects + " PVs were not able to connect");
    }

    /** sets the text describing which PVs will be used */
    private void setPVText() {
        String scanpv1 = "RF phase scan PV: " + scanVariable.getChannelName() + "  " + connectionMap.get(cavAmpPVName) + "\n";
        Iterator itr = measuredValuesOnV.iterator();
        int i = 1;
        String mvpvs = "\n";
        while (itr.hasNext()) {
            String name = ((MeasuredValue) itr.next()).getChannelName();
            mvpvs += "BPM monitor PV " + (new Integer(i)).toString() + " : " + name + "  " + connectionMap.get(name) + "\n";
            i++;
        }
        thePVText = scanpv1 + mvpvs;
        pvListTextAreaOn.setText(thePVText);
        pvListTextAreaOff.setText(thePVText);
    }

    /** method to update the graphPanel data (for raw scanned data  only)*/
    protected void updateGraphOnPanel() {
        graphScanOn.removeAllGraphData();
        int nCurves = 4;
        if (measuredValuesOnV.size() < 4) nCurves = measuredValuesOnV.size();
        for (int i = 0, n = nCurves; i < n; i++) {
            MeasuredValue mv_tmp = (MeasuredValue) measuredValuesOnV.get(i);
            graphScanOn.addGraphData(mv_tmp.getDataContainers());
        }
        graphScanOn.refreshGraphJPanel();
    }

    /** method to update the graphPanel data (for raw scanned data  only)*/
    protected void updateGraphOffPanel() {
        graphScanOff.removeAllGraphData();
        int nCurves = 4;
        if (measuredValuesOffV.size() < 4) nCurves = measuredValuesOffV.size();
        for (int i = 0, n = nCurves; i < n; i++) {
            MeasuredValue mv_tmp = (MeasuredValue) measuredValuesOffV.get(i);
            graphScanOff.addGraphData(mv_tmp.getDataContainers());
        }
        graphScanOff.refreshGraphJPanel();
    }

    /** Set colors of raw data scans */
    protected void setColorsOn(int deleteIndex) {
        for (int i = 0, n = measuredValuesOnV.size(); i < n; i++) {
            MeasuredValue mv_tmp = (MeasuredValue) measuredValuesOnV.get(i);
            mv_tmp.setColor(IncrementalColor.getColor(i));
        }
        graphScanOn.refreshGraphJPanel();
    }

    protected void setColorsOff(int deleteIndex) {
        for (int i = 0, n = measuredValuesOffV.size(); i < n; i++) {
            MeasuredValue mv_tmp = (MeasuredValue) measuredValuesOffV.get(i);
            mv_tmp.setColor(IncrementalColor.getColor(i));
        }
        graphScanOff.refreshGraphJPanel();
    }

    /** clear all graph data */
    protected void clearAllData() {
        graphScanOn.removeAllGraphData();
        graphScanOff.removeAllGraphData();
    }

    /** clear all data and start over */
    private void initScanOn() {
        graphScanOn.removeAllGraphData();
        for (int i = 0, n = measuredValuesOnV.size(); i < n; i++) {
            MeasuredValue mv_tmp = (MeasuredValue) measuredValuesOnV.get(i);
            mv_tmp.removeAllDataContainers();
        }
        setColorsOn(1);
    }

    /** clear all data and start over */
    private void initScanOff() {
        graphScanOff.removeAllGraphData();
        for (int i = 0, n = measuredValuesOffV.size(); i < n; i++) {
            MeasuredValue mv_tmp = (MeasuredValue) measuredValuesOffV.get(i);
            mv_tmp.removeAllDataContainers();
        }
        setColorsOff(1);
    }

    /** Initialize the scan, when the start button is pressed */
    protected void newSetOfCavOnData() {
        String scanPV_string = "";
        String measurePV_string = "";
        if (scanVariable.getChannel() != null) {
            scanPV_string = "xPV=" + scanVariable.getChannel().getId();
        }
        for (int i = 0, n = measuredValuesOnV.size(); i < n; i++) {
            MeasuredValue mv_tmp = (MeasuredValue) measuredValuesOnV.get(i);
            BasicGraphData gd = mv_tmp.getDataContainer();
            if (mv_tmp.getChannel() != null) {
                measurePV_string = mv_tmp.getChannel().getId();
            }
            if (gd != null) {
                gd.setGraphProperty(graphScanOn.getLegendKeyString(), measurePV_string);
            }
        }
        updateGraphOnPanel();
    }

    /** Initialize the scan, when the start button is pressed */
    protected void newSetOfCavOffData() {
        String scanPV_string = "";
        String measurePV_string = "";
        if (scanVariable.getChannel() != null) {
            scanPV_string = "xPV=" + scanVariable.getChannel().getId();
        }
        for (int i = 0, n = measuredValuesOffV.size(); i < n; i++) {
            MeasuredValue mv_tmp = (MeasuredValue) measuredValuesOffV.get(i);
            BasicGraphData gd = mv_tmp.getDataContainer();
            if (mv_tmp.getChannel() != null) {
                measurePV_string = mv_tmp.getChannel().getId();
            }
            if (gd != null) {
            }
        }
        updateGraphOffPanel();
    }

    /** returns whether data is taken yet for the cavity off case */
    protected boolean haveCavOffData() {
        if (BPM1PhaseOffMV == null || BPM1PhaseOffMV == null) return false;
        if (BPM1PhaseOffMV.getDataContainer(0) == null || BPM1PhaseOffMV.getDataContainer(0) == null) return false;
        if (BPM1PhaseOffMV == null || BPM1PhaseOffMV.getDataContainer(0).getNumbOfPoints() == 0 || BPM1PhaseOffMV == null || BPM1PhaseOffMV.getDataContainer(0).getNumbOfPoints() == 0) return false; else return true;
    }

    /** ConnectionListener interface methods:*/
    public void connectionMade(Channel chan) {
        String name = chan.getId();
        connectionMap.put(name, new Boolean(true));
    }

    public void connectionDropped(Channel chan) {
    }

    /** save scan setup to a data adaptor */
    private void saveScanData(XmlDataAdaptor scan1D_Adaptor) {
        XmlDataAdaptor scanPV_scan1D = scan1D_Adaptor.createChild(scanPV_SR);
        ScanController1D scanController = scanControllerOn;
        AvgController avgCntr = avgCntrOn;
        XmlDataAdaptor scan_params_DA = scan1D_Adaptor.createChild("scan_params");
        XmlDataAdaptor scan_limits_DA = scan_params_DA.createChild("limits_step_delay");
        scan_limits_DA.setValue("low", scanController.getLowLimit());
        scan_limits_DA.setValue("upp", scanController.getUppLimit());
        scan_limits_DA.setValue("step", scanController.getStep());
        scan_limits_DA.setValue("delay", scanController.getSleepTime());
        XmlDataAdaptor params_trigger = scan_params_DA.createChild("beam_trigger");
        params_trigger.setValue("on", scanController.getBeamTriggerState());
        params_trigger.setValue("delay", scanController.getBeamTriggerDelay());
        XmlDataAdaptor params_averg = scan_params_DA.createChild("averaging");
        params_averg.setValue("on", avgCntr.isOn());
        params_averg.setValue("N", avgCntr.getAvgNumber());
        params_averg.setValue("delay", avgCntr.getTimeDelay());
    }

    /** A method to parse the scan parameters and set up the scanStuff */
    private void readScanData(XmlDataAdaptor scan1D_Adaptor) {
        ScanController1D scanController = scanControllerOn;
        AvgController avgCntr = avgCntrOn;
        FunctionGraphsJPanel graphScan = graphScanOn;
        XmlDataAdaptor scanPV_scan1D = scan1D_Adaptor.childAdaptor(scanPV_SR);
        XmlDataAdaptor scan_params_DA = scan1D_Adaptor.childAdaptor("scan_params");
        if (scan_params_DA != null) {
            XmlDataAdaptor scan_limits_DA = scan_params_DA.childAdaptor("limits_step_delay");
            if (scan_limits_DA != null) {
                scanController.setLowLimit(scan_limits_DA.doubleValue("low"));
                scanController.setUppLimit(scan_limits_DA.doubleValue("upp"));
                scanController.setStep(scan_limits_DA.doubleValue("step"));
                scanController.setSleepTime(scan_limits_DA.doubleValue("delay"));
            }
            XmlDataAdaptor params_trigger = scan_params_DA.childAdaptor("beam_trigger");
            if (params_trigger != null) {
                scanController.setBeamTriggerDelay(params_trigger.doubleValue("delay"));
                scanController.setBeamTriggerState(params_trigger.booleanValue("on"));
            }
            XmlDataAdaptor params_averg = scan_params_DA.childAdaptor("averaging");
            avgCntr.setOnOff(params_averg.booleanValue("on"));
            avgCntr.setAvgNumber(params_averg.intValue("N"));
            avgCntr.setTimeDelay(params_averg.doubleValue("delay"));
        }
    }
}
