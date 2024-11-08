package jp.jparc.apps.bbc;

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
 * @author  jdg, Guobao Shen
 */
public class ScanStuff implements ConnectionListener {

    /** the parametric scan variable (corrector field) */
    protected ScanVariable scanVariableCorrX = null;

    protected ScanVariable scanVariableCorrY = null;

    /** the scan variable (quadrupole magnet firld) */
    protected ScanVariable scanVariableQuadX = null;

    protected ScanVariable scanVariableQuadY = null;

    /** container for the measured variables (BPM X and Y Position) */
    protected Vector measuredXValuesV, measuredYValuesV;

    /** the measured quantities for the Scan of BPM1*/
    protected MeasuredValue BPM1XPosMV, BPM1YPosMV;

    /** the measured quantities for the Scan of BPM2s*/
    protected MeasuredValue BPM2XPosMV[], BPM2YPosMV[];

    /** the measured quantities for the Scan of BPM3*/
    protected MeasuredValue BPM3XPosMV, BPM3YPosMV;

    /** the measured quantities for the Scan of horizontal and vertical corrector*/
    protected MeasuredValue HDipoleCorrMV, VDipoleCorrMV;

    protected MeasuredValue HDipoleCorrRBMV, VDipoleCorrRBMV;

    /** the measured quantities for the Scan of quadrupole magnet*/
    protected MeasuredValue QuadMagXSetMV, QuadMagYSetMV;

    protected MeasuredValue QuadMagXSetRBMV, QuadMagYSetRBMV;

    protected MeasuredValue BCMMVX, BCMMVY;

    /**scan controller for horizontal scans*/
    protected ScanController2D scanControllerX;

    /**scan controller for vertical scan*/
    protected ScanController2D scanControllerY;

    protected AvgController avgCntrX, avgCntrY;

    /** validation controller - used with a BCM */
    protected ValidationController vldCntrX, vldCntrY;

    /**graph panel to display scanned data for horizontal and vertical*/
    protected FunctionGraphsJPanel graphScanX, graphScanY;

    /** names of the PVs used of BPM1*/
    private String BPM1XPosPVName, BPM1YPosPVName;

    /** names of the PVs used of BPM2s*/
    private String BPM2XPosPVName[], BPM2YPosPVName[];

    /** names of the PVs used of BPM3*/
    private String BPM3XPosPVName, BPM3YPosPVName;

    /** names of the PVs used of dipole correctors*/
    private String HDipoleCorrPVName, VDipoleCorrPVName;

    private String HDipoleCorrRBPVName, VDipoleCorrRBPVName;

    private String BCMXPvName, BCMYPvName;

    /** names of the PVs used of quadrupole magnet*/
    private String QuadMagXSetPVName, QuadMagYSetPVName;

    private String QuadMagXSetRBPVName, QuadMagYSetRBPVName;

    /** the document this scanstuff belongs to */
    BbcDocument theDoc;

    /** workaround to avoid jca context initialization exception */
    static {
        ChannelFactory.defaultFactory().init();
    }

    protected String thePVTextX, thePVTextY;

    private Map connectionMap;

    /** Create an object */
    public ScanStuff(BbcDocument doc) {
        theDoc = doc;
        scanControllerX = new ScanController2D("Horizontal Scan Control Panel");
        scanControllerY = new ScanController2D("Vertical Scan Control Panel");
        avgCntrX = new AvgController();
        graphScanX = new FunctionGraphsJPanel();
        vldCntrX = new ValidationController(3., 50.);
        vldCntrX.setOnOff(true);
        avgCntrY = new AvgController();
        graphScanY = new FunctionGraphsJPanel();
        vldCntrY = new ValidationController(3., 50.);
        vldCntrY.setOnOff(true);
        scanVariableQuadX = new ScanVariable("quadXFieldSP", "quadXFieldRB");
        scanVariableCorrX = new ScanVariable("corrXFieldSP", "corrXFieldRB");
        scanVariableQuadY = new ScanVariable("quadYFieldSP", "quadYFieldRB");
        scanVariableCorrY = new ScanVariable("corrYFieldSP", "corrYFieldRB");
        measuredXValuesV = new Vector();
        measuredYValuesV = new Vector();
        clearMeasuredValue();
        BCMMVX = new MeasuredValue("BCMXcurrent");
        BCMMVY = new MeasuredValue("BCMYcurrent");
        initScanController(scanControllerX, scanVariableQuadX, scanVariableCorrX, avgCntrX, graphScanX, measuredXValuesV, BCMMVX, vldCntrX);
        initScanController(scanControllerY, scanVariableQuadY, scanVariableCorrY, avgCntrY, graphScanY, measuredYValuesV, BCMMVY, vldCntrY);
    }

    protected void clearMeasuredValue() {
        measuredXValuesV.clear();
        measuredYValuesV.clear();
    }

    protected void init() {
        clearMeasuredValue();
        BPM1XPosMV = new MeasuredValue("BPM1XPos");
        BPM1YPosMV = new MeasuredValue("BPM1YPos");
        BPM2XPosMV = new MeasuredValue[theDoc.BPM2.length];
        BPM2YPosMV = new MeasuredValue[theDoc.BPM2.length];
        for (int i = 0; i < theDoc.BPM2.length; i++) {
            BPM2XPosMV[i] = new MeasuredValue(("BPM2[" + i + "]XPos"));
            BPM2YPosMV[i] = new MeasuredValue("BPM2[" + i + "]YPos");
        }
        BPM2XPosPVName = new String[theDoc.BPM2.length];
        BPM2YPosPVName = new String[theDoc.BPM2.length];
        BPM3XPosMV = new MeasuredValue("BPM3XPos");
        BPM3YPosMV = new MeasuredValue("BPM3YPos");
        HDipoleCorrMV = new MeasuredValue("HDipoleCorr");
        HDipoleCorrRBMV = new MeasuredValue("HDipoleCorrRB");
        VDipoleCorrMV = new MeasuredValue("VDipoleCorr");
        VDipoleCorrRBMV = new MeasuredValue("VDipoleCorrRB");
        QuadMagXSetMV = new MeasuredValue("XQuadMag");
        QuadMagXSetRBMV = new MeasuredValue("XQuadMagRB");
        QuadMagYSetMV = new MeasuredValue("YQuadMag");
        QuadMagYSetRBMV = new MeasuredValue("YQuadMagRB");
        measuredXValuesV.add(BPM1XPosMV);
        for (int i = 0; i < theDoc.BPM2.length; i++) {
            measuredXValuesV.add(BPM2XPosMV[i]);
        }
        measuredXValuesV.add(BPM3XPosMV);
        measuredXValuesV.add(BCMMVX);
        measuredYValuesV.add(BCMMVY);
        measuredYValuesV.add(BPM1YPosMV);
        for (int i = 0; i < theDoc.BPM2.length; i++) {
            measuredYValuesV.add(BPM2YPosMV[i]);
        }
        measuredYValuesV.add(BPM3YPosMV);
        Iterator itr = measuredXValuesV.iterator();
        while (itr.hasNext()) {
            scanControllerX.addMeasuredValue((MeasuredValue) itr.next());
        }
        itr = measuredYValuesV.iterator();
        while (itr.hasNext()) {
            scanControllerY.addMeasuredValue((MeasuredValue) itr.next());
        }
        connectionMap = Collections.synchronizedMap(new HashMap());
    }

    /** initialize the 2-D scan controller used for scan */
    private void initScanController(final ScanController2D scanController, final ScanVariable scanVariableQuad, final ScanVariable scanVariableCorr, AvgController avgCntr, final FunctionGraphsJPanel graphScan, final Vector measuredValuesGraphV, MeasuredValue bcmmv, ValidationController vldCntr) {
        scanController.setParamPhaseScanButtonOn(false);
        scanController.getParamUnitsLabel().setText(" Tesla*m [Steer] ");
        scanController.setParamLowLimit(-0.1);
        scanController.setParamUppLimit(0.1);
        scanController.setParamStep(0.04);
        scanController.setValuePhaseScanButtonOn(false);
        scanController.getUnitsLabel().setText(" Tesla/m [Quad] ");
        scanController.setLowLimit(5.0);
        scanController.setUppLimit(10.0);
        scanController.setStep(1.0);
        scanController.setScanVariable(scanVariableQuad);
        scanController.setParamVariable(scanVariableCorr);
        scanController.setAvgController(avgCntr);
        scanController.setBeamTriggerState(false);
        scanController.setValidationController(vldCntr);
        scanController.addValidationValue(bcmmv);
        vldCntr.setOnOff(true);
        scanController.addNewPointOfDataListener(new ActionListener() {

            public void actionPerformed(ActionEvent evn) {
                graphScan.refreshGraphJPanel();
            }
        });
        scanController.addNewSetOfDataListener(new ActionListener() {

            public void actionPerformed(ActionEvent evn) {
                newSetOfData(scanController, scanVariableCorr, scanVariableQuad, measuredValuesGraphV, graphScan);
            }
        });
        scanController.addStartButtonListener(new ActionListener() {

            public void actionPerformed(ActionEvent evn) {
                initScan(measuredValuesGraphV, graphScan);
            }
        });
        scanController.setFontForAll(new Font("Monospaced", Font.PLAIN, 12));
        avgCntr.setFontForAll(new Font("Monospaced", Font.PLAIN, 12));
        SimpleChartPopupMenu.addPopupMenuTo(graphScan);
        graphScan.setOffScreenImageDrawing(true);
        graphScan.setName("SCAN : Measured Value vs. Quad Magnet Field");
        graphScan.setAxisNames("Quad Magnet Field [Tesla/m]", "Measured Values");
        graphScan.setGraphBackGroundColor(Color.white);
        graphScan.setLegendButtonVisible(true);
    }

    /** set the scan  + measured PVs, based on the selected BPMs and dipole correctors */
    protected void updateScanVariables(BPM bpm1, BPM[] bpm2, BPM bpm3, HDipoleCorr hDipoleCorr, VDipoleCorr vDipoleCorr, Electromagnet quadMagnet, CurrentMonitor bcm) {
        graphScanX.removeAllGraphData();
        graphScanY.removeAllGraphData();
        if (hDipoleCorr != null) {
            BPM1XPosPVName = bpm1.getChannel(BPM.X_AVG_HANDLE).getId();
            for (int i = 0; i < bpm2.length; i++) {
                BPM2XPosPVName[i] = bpm2[i].getChannel(BPM.X_AVG_HANDLE).getId();
            }
            if (bpm3 != null) BPM3XPosPVName = bpm3.getChannel(BPM.X_AVG_HANDLE).getId();
            QuadMagXSetPVName = quadMagnet.getChannel(MagnetMainSupply.FIELD_SET_HANDLE).getId();
            QuadMagXSetRBPVName = quadMagnet.getChannel(MagnetMainSupply.FIELD_RB_HANDLE).getId();
            HDipoleCorrPVName = hDipoleCorr.getChannel(MagnetMainSupply.FIELD_SET_HANDLE).getId();
            HDipoleCorrRBPVName = hDipoleCorr.getChannel(MagnetMainSupply.FIELD_RB_HANDLE).getId();
        }
        if (vDipoleCorr != null) {
            BPM1YPosPVName = bpm1.getChannel(BPM.Y_AVG_HANDLE).getId();
            for (int i = 0; i < bpm2.length; i++) {
                BPM2YPosPVName[i] = bpm2[i].getChannel(BPM.Y_AVG_HANDLE).getId();
            }
            if (bpm3 != null) BPM3YPosPVName = bpm3.getChannel(BPM.Y_AVG_HANDLE).getId();
            QuadMagYSetPVName = quadMagnet.getChannel(MagnetMainSupply.FIELD_SET_HANDLE).getId();
            QuadMagYSetRBPVName = quadMagnet.getChannel(MagnetMainSupply.FIELD_RB_HANDLE).getId();
            VDipoleCorrPVName = vDipoleCorr.getChannel(MagnetMainSupply.FIELD_SET_HANDLE).getId();
            VDipoleCorrRBPVName = vDipoleCorr.getChannel(MagnetMainSupply.FIELD_RB_HANDLE).getId();
        }
        if (hDipoleCorr != null) {
            scanVariableCorrX.setChannel(hDipoleCorr.getChannel(MagnetMainSupply.FIELD_SET_HANDLE));
            scanVariableCorrX.setChannelRB(hDipoleCorr.getChannel(MagnetMainSupply.FIELD_RB_HANDLE));
            scanVariableQuadX.setChannel(quadMagnet.getChannel(MagnetMainSupply.FIELD_SET_HANDLE));
            scanVariableQuadX.setChannelRB(quadMagnet.getChannel(MagnetMainSupply.FIELD_RB_HANDLE));
            BPM1XPosMV.setChannel(bpm1.getChannel(BPM.X_AVG_HANDLE));
            for (int i = 0; i < bpm2.length; i++) {
                BPM2XPosMV[i].setChannel(bpm2[i].getChannel(BPM.X_AVG_HANDLE));
            }
            if (bpm3 != null) BPM3XPosMV.setChannel(bpm3.getChannel(BPM.X_AVG_HANDLE));
            QuadMagXSetMV.setChannel(quadMagnet.getChannel(MagnetMainSupply.FIELD_SET_HANDLE));
            QuadMagXSetRBMV.setChannel(quadMagnet.getChannel(MagnetMainSupply.FIELD_RB_HANDLE));
            HDipoleCorrMV.setChannel(hDipoleCorr.getChannel(MagnetMainSupply.FIELD_SET_HANDLE));
            HDipoleCorrRBMV.setChannel(hDipoleCorr.getChannel(MagnetMainSupply.FIELD_RB_HANDLE));
        }
        if (vDipoleCorr != null) {
            scanVariableCorrY.setChannel(vDipoleCorr.getChannel(MagnetMainSupply.FIELD_SET_HANDLE));
            scanVariableCorrY.setChannelRB(vDipoleCorr.getChannel(MagnetMainSupply.FIELD_RB_HANDLE));
            scanVariableQuadY.setChannel(quadMagnet.getChannel(MagnetMainSupply.FIELD_SET_HANDLE));
            scanVariableQuadY.setChannelRB(quadMagnet.getChannel(MagnetMainSupply.FIELD_RB_HANDLE));
            BPM1YPosMV.setChannel(bpm1.getChannel(BPM.Y_AVG_HANDLE));
            for (int i = 0; i < bpm2.length; i++) {
                BPM2YPosMV[i].setChannel(bpm2[i].getChannel(BPM.Y_AVG_HANDLE));
            }
            if (bpm3 != null) BPM3YPosMV.setChannel(bpm3.getChannel(BPM.Y_AVG_HANDLE));
            QuadMagYSetMV.setChannel(quadMagnet.getChannel(MagnetMainSupply.FIELD_SET_HANDLE));
            QuadMagYSetRBMV.setChannel(quadMagnet.getChannel(MagnetMainSupply.FIELD_RB_HANDLE));
            VDipoleCorrMV.setChannel(vDipoleCorr.getChannel(MagnetMainSupply.FIELD_SET_HANDLE));
            VDipoleCorrRBMV.setChannel(vDipoleCorr.getChannel(MagnetMainSupply.FIELD_RB_HANDLE));
        }
        if (bcm != null) {
            BCMXPvName = bcm.getChannel(bcm.I_AVG_HANDLE).getId();
            BCMYPvName = BCMXPvName;
            BCMMVX.setChannel(bcm.getChannel(CurrentMonitor.I_AVG_HANDLE));
            BCMMVY.setChannel(bcm.getChannel(CurrentMonitor.I_AVG_HANDLE));
        }
        Channel.flushIO();
        connectChannels(bpm1, bpm2, bpm3, hDipoleCorr, vDipoleCorr, quadMagnet, bcm);
        if (vDipoleCorr != null) {
            scanControllerY.setScanVariable(scanVariableQuadY);
            scanControllerY.setParamVariable(scanVariableCorrY);
        }
        if (hDipoleCorr != null) {
            scanControllerX.setScanVariable(scanVariableQuadX);
            scanControllerX.setParamVariable(scanVariableCorrX);
        }
        setPVText(bpm1, bpm2, bpm3, hDipoleCorr, vDipoleCorr, quadMagnet);
    }

    /** try and force a channel connection to the required channels */
    private void connectChannels(BPM bpm1, BPM[] bpm2, BPM bpm3, HDipoleCorr hDipoleCorr, VDipoleCorr vDipoleCorr, Electromagnet quadMagnet, CurrentMonitor bcm) {
        connectionMap.clear();
        if (hDipoleCorr != null) {
            connectionMap.put(BPM1XPosPVName, new Boolean(false));
            for (int i = 0; i < bpm2.length; i++) {
                connectionMap.put(BPM2XPosPVName[i], new Boolean(false));
            }
            if (bpm3 != null) connectionMap.put(BPM3XPosPVName, new Boolean(false));
            connectionMap.put(QuadMagXSetPVName, new Boolean(false));
            connectionMap.put(QuadMagXSetRBPVName, new Boolean(false));
            connectionMap.put(HDipoleCorrPVName, new Boolean(false));
            connectionMap.put(HDipoleCorrRBPVName, new Boolean(false));
        }
        if (vDipoleCorr != null) {
            connectionMap.put(BPM1YPosPVName, new Boolean(false));
            for (int i = 0; i < bpm2.length; i++) {
                connectionMap.put(BPM2YPosPVName[i], new Boolean(false));
            }
            if (bpm3 != null) connectionMap.put(BPM3YPosPVName, new Boolean(false));
            connectionMap.put(QuadMagYSetPVName, new Boolean(false));
            connectionMap.put(QuadMagYSetRBPVName, new Boolean(false));
            connectionMap.put(VDipoleCorrPVName, new Boolean(false));
            connectionMap.put(VDipoleCorrRBPVName, new Boolean(false));
        }
        if (hDipoleCorr != null && bcm != null) {
            connectionMap.put(BCMXPvName, new Boolean(false));
        }
        if (vDipoleCorr != null && bcm != null) {
            connectionMap.put(BCMYPvName, new Boolean(false));
        }
        if (hDipoleCorr != null) {
            bpm1.getChannel(BPM.X_AVG_HANDLE).addConnectionListener(this);
            for (int i = 0; i < bpm2.length; i++) {
                bpm2[i].getChannel(BPM.X_AVG_HANDLE).addConnectionListener(this);
            }
            if (bpm3 != null) bpm3.getChannel(BPM.X_AVG_HANDLE).addConnectionListener(this);
            quadMagnet.getChannel(MagnetMainSupply.FIELD_SET_HANDLE).addConnectionListener(this);
            quadMagnet.getChannel(MagnetMainSupply.FIELD_RB_HANDLE).addConnectionListener(this);
            hDipoleCorr.getChannel(MagnetMainSupply.FIELD_SET_HANDLE).addConnectionListener(this);
            hDipoleCorr.getChannel(MagnetMainSupply.FIELD_RB_HANDLE).addConnectionListener(this);
        }
        if (vDipoleCorr != null) {
            bpm1.getChannel(BPM.Y_AVG_HANDLE).addConnectionListener(this);
            for (int i = 0; i < bpm2.length; i++) {
                bpm2[i].getChannel(BPM.Y_AVG_HANDLE).addConnectionListener(this);
            }
            if (bpm3 != null) bpm3.getChannel(BPM.Y_AVG_HANDLE).addConnectionListener(this);
            quadMagnet.getChannel(MagnetMainSupply.FIELD_SET_HANDLE).addConnectionListener(this);
            quadMagnet.getChannel(MagnetMainSupply.FIELD_RB_HANDLE).addConnectionListener(this);
            vDipoleCorr.getChannel(MagnetMainSupply.FIELD_SET_HANDLE).addConnectionListener(this);
            vDipoleCorr.getChannel(MagnetMainSupply.FIELD_RB_HANDLE).addConnectionListener(this);
        }
        if (hDipoleCorr != null) {
            bpm1.getChannel(BPM.X_AVG_HANDLE).requestConnection();
            for (int i = 0; i < bpm2.length; i++) {
                bpm2[i].getChannel(BPM.X_AVG_HANDLE).requestConnection();
            }
            if (bpm3 != null) bpm3.getChannel(BPM.X_AVG_HANDLE).requestConnection();
            quadMagnet.getChannel(MagnetMainSupply.FIELD_SET_HANDLE).requestConnection();
            quadMagnet.getChannel(MagnetMainSupply.FIELD_RB_HANDLE).requestConnection();
            hDipoleCorr.getChannel(MagnetMainSupply.FIELD_SET_HANDLE).requestConnection();
            hDipoleCorr.getChannel(MagnetMainSupply.FIELD_RB_HANDLE).requestConnection();
        }
        if (vDipoleCorr != null) {
            bpm1.getChannel(BPM.Y_AVG_HANDLE).requestConnection();
            for (int i = 0; i < bpm2.length; i++) {
                bpm2[i].getChannel(BPM.Y_AVG_HANDLE).requestConnection();
            }
            if (bpm3 != null) bpm3.getChannel(BPM.Y_AVG_HANDLE).requestConnection();
            quadMagnet.getChannel(MagnetMainSupply.FIELD_SET_HANDLE).requestConnection();
            quadMagnet.getChannel(MagnetMainSupply.FIELD_RB_HANDLE).requestConnection();
            vDipoleCorr.getChannel(MagnetMainSupply.FIELD_SET_HANDLE).requestConnection();
            vDipoleCorr.getChannel(MagnetMainSupply.FIELD_RB_HANDLE).requestConnection();
        }
        if (bcm != null) {
            (bcm.getChannel(CurrentMonitor.I_AVG_HANDLE)).addConnectionListener(this);
            (bcm.getChannel(CurrentMonitor.I_AVG_HANDLE)).requestConnection();
        }
        Channel.flushIO();
        int i = 0;
        int nDisconnects = connectionMap.size();
        int totalDisconnect = nDisconnects - 1;
        while (nDisconnects > 0 && i < totalDisconnect) {
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
        if (hDipoleCorr == null) {
            scanControllerX.setStartButtonEnabled(false);
        } else {
            scanControllerX.setStartButtonEnabled(true);
        }
        if (vDipoleCorr == null) {
            scanControllerY.setStartButtonEnabled(false);
        } else {
            scanControllerY.setStartButtonEnabled(true);
        }
    }

    /** sets the text describing which PVs will be used */
    private void setPVText(BPM bpm1, BPM[] bpm2, BPM bpm3, HDipoleCorr hDipoleCorr, VDipoleCorr vDipoleCorr, Electromagnet quadMagnet) {
        StringBuffer scanPVX = new StringBuffer("Horizontal scan PV:\n");
        StringBuffer scanPVY = new StringBuffer("Vertical scan PV:\n");
        if (hDipoleCorr != null) {
            scanPVX.append("Corrector Scan PV: ");
            scanPVX.append(scanVariableCorrX.getChannelName());
            scanPVX.append("  ");
            scanPVX.append(connectionMap.get(HDipoleCorrPVName));
            scanPVX.append("\n");
            scanPVX.append("Quadrupole Scan PV: ");
            scanPVX.append(scanVariableQuadX.getChannelName());
            scanPVX.append("  ");
            scanPVX.append(connectionMap.get(QuadMagXSetPVName));
            scanPVX.append("\n");
        }
        if (vDipoleCorr != null) {
            scanPVY.append("Corrector SCAN PV: ");
            scanPVY.append(scanVariableCorrY.getChannelName());
            scanPVY.append("  ");
            scanPVY.append(connectionMap.get(VDipoleCorrPVName));
            scanPVY.append("\n");
            scanPVY.append("Quadrupole SCAN PV: ");
            scanPVY.append(scanVariableQuadY.getChannelName());
            scanPVY.append("  ");
            scanPVY.append(connectionMap.get(QuadMagYSetPVName));
            scanPVY.append("\n");
        }
        Iterator itrX = measuredXValuesV.iterator();
        int x = 1;
        String mvPVXs = "\n";
        while (itrX.hasNext()) {
            String name = ((MeasuredValue) itrX.next()).getChannelName();
            mvPVXs += "monitor PV " + (new Integer(x)).toString() + " : " + name + "  " + connectionMap.get(name) + "\n";
            x++;
        }
        Iterator itrY = measuredYValuesV.iterator();
        int y = 1;
        String mvPVYs = "\n";
        while (itrY.hasNext()) {
            String name = ((MeasuredValue) itrY.next()).getChannelName();
            mvPVYs += "monitor PV " + (new Integer(y)).toString() + " : " + name + "  " + connectionMap.get(name) + "\n";
            y++;
        }
        thePVTextX = scanPVX + mvPVXs;
        thePVTextY = scanPVY + mvPVYs;
    }

    /** method to update the graphPanel data (for raw scanned data  only)*/
    protected void updateGraphPanel(Vector measuredValuesGraphV, FunctionGraphsJPanel graphScan) {
        graphScan.removeAllGraphData();
        int nCurves = measuredValuesGraphV.size();
        for (int i = 0, n = nCurves; i < n; i++) {
            MeasuredValue mv_tmp = (MeasuredValue) measuredValuesGraphV.get(i);
            graphScan.addGraphData(mv_tmp.getDataContainers());
        }
        graphScan.refreshGraphJPanel();
    }

    /** Set colors of raw data scans */
    protected void setColors(Vector measuredValuesGraphV, FunctionGraphsJPanel graphScan, int deleteIndex) {
        for (int i = 0, n = measuredValuesGraphV.size(); i < n; i++) {
            MeasuredValue mv_tmp = (MeasuredValue) measuredValuesGraphV.get(i);
            mv_tmp.setColor(IncrementalColor.getColor(i));
        }
        graphScan.refreshGraphJPanel();
    }

    /** clear all data and start over */
    private void initScan(Vector measuredValuesGraphV, FunctionGraphsJPanel graphScan) {
        graphScan.removeAllGraphData();
        for (int i = 0, n = measuredValuesGraphV.size(); i < n; i++) {
            MeasuredValue mv_tmp = (MeasuredValue) measuredValuesGraphV.get(i);
            mv_tmp.removeAllDataContainers();
        }
        setColors(measuredValuesGraphV, graphScan, 1);
    }

    /** Initialize the scan, when the start button is pressed */
    private void newSetOfData(ScanController2D scanController, ScanVariable scanVariableCorr, ScanVariable scanVariableQuad, Vector measuredValuesGraphV, FunctionGraphsJPanel graphScan) {
        DecimalFormat valueFormat = new DecimalFormat("###.###");
        String paramPV_string = "";
        String scanPV_string = "";
        String measurePV_string = "";
        String legend_string = "";
        Double paramValue = new Double(scanController.getParamValue());
        Double paramValueRB = new Double(scanController.getParamValueRB());
        if (scanVariableCorr.getChannel() != null) {
            String paramValString = valueFormat.format(scanVariableCorr.getValue());
            paramPV_string = paramPV_string + " par.PV : " + scanVariableCorr.getChannel().getId() + "=" + paramValString;
            paramValue = new Double(scanVariableCorr.getValue());
        } else {
            paramPV_string = paramPV_string + " param.= " + paramValue;
        }
        if (scanVariableQuad.getChannel() != null) {
            scanPV_string = "xPV=" + scanVariableQuad.getChannel().getId();
        }
        for (int i = 0, n = measuredValuesGraphV.size(); i < n; i++) {
            MeasuredValue mv_tmp = (MeasuredValue) measuredValuesGraphV.get(i);
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
            if (scanVariableQuad.getChannelRB() != null) {
                gd = mv_tmp.getDataContainerRB();
                if (gd != null) {
                    if (paramValue != null) gd.setGraphProperty("PARAMETER_VALUE", paramValue);
                    if (paramValueRB != null) gd.setGraphProperty("PARAMETER_VALUE_RB", paramValueRB);
                }
            }
        }
        updateGraphPanel(measuredValuesGraphV, graphScan);
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
