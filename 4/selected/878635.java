package jp.jparc.apps.bbc;

import java.util.*;
import javax.swing.*;
import java.net.*;
import gov.sns.tools.xml.*;
import gov.sns.tools.data.*;
import gov.sns.xal.smf.data.*;
import gov.sns.xal.smf.impl.Dipole;
import gov.sns.xal.smf.impl.HDipoleCorr;
import gov.sns.xal.smf.impl.VDipoleCorr;
import gov.sns.xal.smf.*;
import gov.sns.tools.scan.SecondEdition.*;
import gov.sns.tools.plot.*;
import gov.sns.ca.*;

/**
 * A class to save and restore iPod documents to an xml file
 * @author  J. Galambos
 */
public class SaveOpen {

    /** The bbc documentto deal with */
    private BbcDocument theDoc;

    private XmlDataAdaptor xdaWrite;

    private String paramsName_SR = "corr_params";

    private String paramPV_SR = "corr_param_PV";

    private String scanPV_SR = "quad_scan_PV";

    private String measurePVs_SR = "measure_PVs";

    private String centerBPMXName = "Center_BPM_X";

    private String centerBPMYName = "Center_BPM_Y";

    private String downstreamBPMXName = "downstreamBPMX";

    private String downstreamBPMYName = "downstreamBPMY";

    private String upstreamBPMXName = "Upstream_BPM_X";

    private String upstreamBPMYName = "Upstream_BPM_Y";

    private String quadMagXName = "Quad_X";

    private String quadMagYName = "Quad_Y";

    private String corrHName = "Corr_H";

    private String corrVName = "Corr_V";

    private int downstreamBPMNum = 0;

    private String BCMXName = "BCMXMV";

    private String BCMYName = "BCMYMV";

    /** constructor:
     * @param xyp - the XyPlot object
     */
    public SaveOpen(BbcDocument doc) {
        theDoc = doc;
    }

    /** save the object to a file
     * @param file- the file to save it to
     */
    public void saveTo(URL url) {
        xdaWrite = XmlDataAdaptor.newEmptyDocumentAdaptor();
        XmlDataAdaptor bbcda = xdaWrite.createChild("BbcSetup");
        XmlDataAdaptor daAccel = bbcda.createChild("accelerator");
        daAccel.setValue("xmlFile", theDoc.getAcceleratorFilePath());
        ArrayList seqs;
        if (theDoc.getSelectedSequence() != null) {
            DataAdaptor daSeq = daAccel.createChild("sequence");
            daSeq.setValue("name", theDoc.getSelectedSequence().getId());
            if (theDoc.getSelectedSequence().getClass() == AcceleratorSeqCombo.class) {
                AcceleratorSeqCombo asc = (AcceleratorSeqCombo) theDoc.getSelectedSequence();
                seqs = (ArrayList) asc.getConstituentNames();
            } else {
                seqs = new ArrayList();
                seqs.add(theDoc.getSelectedSequence().getId());
            }
            Iterator itr = seqs.iterator();
            while (itr.hasNext()) {
                DataAdaptor daSeqComponents = daSeq.createChild("seq");
                daSeqComponents.setValue("name", itr.next());
            }
            DataAdaptor downstreamBPMNodes = daSeq.createChild("DownstreamBPMs");
            downstreamBPMNodes.setValue("DownStreamBPMNum", theDoc.BPM2.length);
            DataAdaptor daNodes = daSeq.createChild("Nodes");
            if (theDoc.BPM1 != null) daNodes.setValue("CenterBPM", theDoc.BPM1);
            for (int i = 0; i < theDoc.BPM2.length; i++) if (theDoc.BPM2[i] != null) daNodes.setValue(("DownstreamBPM" + i), theDoc.BPM2[i]);
            if (theDoc.BPM3 != null) daNodes.setValue("UpstreamBPM", theDoc.BPM3);
            if (theDoc.correctorH != null) daNodes.setValue("HDipoleCorr", theDoc.correctorH);
            if (theDoc.correctorV != null) daNodes.setValue("VDipoleCorr", theDoc.correctorV);
            if (theDoc.selectedQuadMagnet != null) daNodes.setValue("QuadMagnet", theDoc.selectedQuadMagnet);
            if (theDoc.theBCM != null) daNodes.setValue("BCM", theDoc.theBCM);
        }
        XmlDataAdaptor daScanH = bbcda.createChild("HorizontalScan");
        saveScan(daScanH, theDoc.correctorH);
        XmlDataAdaptor daScanV = bbcda.createChild("VerticalScan");
        saveScan(daScanV, theDoc.correctorV);
        DataAdaptor daAnalysis = bbcda.createChild("initializeValue");
        DataAdaptor daImport = daAnalysis.createChild("import");
        if (theDoc.correctorH != null) daImport.setValue("HDipoleCorrInitField", theDoc.hCorrInitField);
        if (theDoc.correctorV != null) daImport.setValue("VDipoleCorrInitField", theDoc.vCorrInitField);
        if (theDoc.selectedQuadMagnet != null) daImport.setValue("QuadrupoleInitField", theDoc.quadInitField);
        xdaWrite.writeToUrl(url);
    }

    /** method to save scan stuff to the xml file */
    private void saveScan(XmlDataAdaptor scan_Adaptor, Dipole dipoleCorr) {
        XmlDataAdaptor params_scan2D = scan_Adaptor.createChild(paramsName_SR);
        XmlDataAdaptor paramPV_scan2D = scan_Adaptor.createChild(paramPV_SR);
        XmlDataAdaptor scanPV_scan2D = scan_Adaptor.createChild(scanPV_SR);
        XmlDataAdaptor measurePVs_scan2D = scan_Adaptor.createChild(measurePVs_SR);
        ScanVariable scanVariableCorr = theDoc.scanStuff.scanVariableCorrX;
        ScanVariable scanVariableQuad = theDoc.scanStuff.scanVariableQuadX;
        ScanController2D scanController = theDoc.scanStuff.scanControllerX;
        AvgController avgCntr = theDoc.scanStuff.avgCntrX;
        Vector measuredValuesV = theDoc.scanStuff.measuredXValuesV;
        FunctionGraphsJPanel graphScan = theDoc.scanStuff.graphScanX;
        ValidationController vldCntr = theDoc.scanStuff.vldCntrX;
        if (dipoleCorr instanceof VDipoleCorr) {
            scanVariableCorr = theDoc.scanStuff.scanVariableCorrY;
            scanVariableQuad = theDoc.scanStuff.scanVariableQuadY;
            scanController = theDoc.scanStuff.scanControllerY;
            avgCntr = theDoc.scanStuff.avgCntrY;
            measuredValuesV = theDoc.scanStuff.measuredYValuesV;
            graphScan = theDoc.scanStuff.graphScanY;
            vldCntr = theDoc.scanStuff.vldCntrY;
        }
        XmlDataAdaptor params_limits = params_scan2D.createChild("limits_step_delay");
        params_limits.setValue("DipoleCorrLow", scanController.getParamLowLimit());
        params_limits.setValue("DipoleCorrUpp", scanController.getParamUppLimit());
        params_limits.setValue("DipoleCorrStep", scanController.getParamStep());
        params_limits.setValue("QuadLow", scanController.getLowLimit());
        params_limits.setValue("QuadUpp", scanController.getUppLimit());
        params_limits.setValue("QuadStep", scanController.getStep());
        params_limits.setValue("delay", scanController.getSleepTime());
        XmlDataAdaptor params_trigger = params_scan2D.createChild("beam_trigger");
        params_trigger.setValue("on", scanController.getBeamTriggerState());
        params_trigger.setValue("delay", scanController.getBeamTriggerDelay());
        XmlDataAdaptor params_averg = params_scan2D.createChild("averaging");
        params_averg.setValue("on", avgCntr.isOn());
        params_averg.setValue("N", avgCntr.getAvgNumber());
        params_averg.setValue("delay", avgCntr.getTimeDelay());
        XmlDataAdaptor params_vld = params_scan2D.createChild("validation");
        params_vld.setValue("on", vldCntr.isOn());
        params_vld.setValue("low", vldCntr.getLowLim());
        params_vld.setValue("upp", vldCntr.getUppLim());
        if (scanVariableCorr.getChannel() != null) {
            XmlDataAdaptor scan_PV_name = paramPV_scan2D.createChild("CorrPV");
            scan_PV_name.setValue("name", scanVariableCorr.getChannelName());
        }
        if (scanVariableCorr.getChannelRB() != null) {
            XmlDataAdaptor scan_PV_RB_name = paramPV_scan2D.createChild("CorrPV_RB");
            scan_PV_RB_name.setValue("name", scanVariableCorr.getChannelNameRB());
        }
        if (scanVariableQuad.getChannel() != null) {
            XmlDataAdaptor scan_PV_name = scanPV_scan2D.createChild("QuadPV");
            scan_PV_name.setValue("name", scanVariableQuad.getChannelName());
        }
        if (scanVariableQuad.getChannelRB() != null) {
            XmlDataAdaptor scan_PV_RB_name = scanPV_scan2D.createChild("QuadPV_RB");
            scan_PV_RB_name.setValue("name", scanVariableQuad.getChannelNameRB());
        }
        if (dipoleCorr instanceof HDipoleCorr) {
            writeMeasuredValue(theDoc.scanStuff.BPM1XPosMV, measurePVs_scan2D, centerBPMXName, dipoleCorr);
            for (int i = 0; i < theDoc.BPM2.length; i++) writeMeasuredValue(theDoc.scanStuff.BPM2XPosMV[i], measurePVs_scan2D, (downstreamBPMXName + i), dipoleCorr);
            writeMeasuredValue(theDoc.scanStuff.BPM3XPosMV, measurePVs_scan2D, upstreamBPMXName, dipoleCorr);
            writeMeasuredValue(theDoc.scanStuff.BCMMVX, measurePVs_scan2D, BCMXName, dipoleCorr);
        }
        if (dipoleCorr instanceof VDipoleCorr) {
            writeMeasuredValue(theDoc.scanStuff.BPM1YPosMV, measurePVs_scan2D, centerBPMYName, dipoleCorr);
            for (int i = 0; i < theDoc.BPM2.length; i++) writeMeasuredValue(theDoc.scanStuff.BPM2YPosMV[i], measurePVs_scan2D, (downstreamBPMYName + i), dipoleCorr);
            writeMeasuredValue(theDoc.scanStuff.BPM3YPosMV, measurePVs_scan2D, upstreamBPMYName, dipoleCorr);
            writeMeasuredValue(theDoc.scanStuff.BCMMVY, measurePVs_scan2D, BCMYName, dipoleCorr);
        }
    }

    /** write a specific measuredValue to a data adaptor */
    private void writeMeasuredValue(MeasuredValue mv_tmp, XmlDataAdaptor measurePVs_scan2D, String name, Dipole dipoleCorr) {
        FunctionGraphsJPanel graphScan = theDoc.scanStuff.graphScanX;
        if (dipoleCorr instanceof VDipoleCorr) graphScan = theDoc.scanStuff.graphScanY;
        XmlDataAdaptor measuredPV_DA = measurePVs_scan2D.createChild("MeasuredPV");
        measuredPV_DA.setValue("name", name);
        Vector dataV = mv_tmp.getDataContainers();
        for (int j = 0, nd = dataV.size(); j < nd; j++) {
            BasicGraphData gd = (BasicGraphData) dataV.get(j);
            if (gd.getNumbOfPoints() > 0) {
                XmlDataAdaptor graph_DA = measuredPV_DA.createChild("Graph_For_scanPV");
                graph_DA.setValue("legend", (String) gd.getGraphProperty(graphScan.getLegendKeyString()));
                Double paramValue = (Double) gd.getGraphProperty("PARAMETER_VALUE");
                if (paramValue != null) {
                    XmlDataAdaptor paramDataValue = graph_DA.createChild("parameter_value");
                    paramDataValue.setValue("value", paramValue.doubleValue());
                }
                Double paramValueRB = (Double) gd.getGraphProperty("PARAMETER_VALUE_RB");
                if (paramValueRB != null) {
                    XmlDataAdaptor paramDataValueRB = graph_DA.createChild("parameter_value_RB");
                    paramDataValueRB.setValue("value", paramValueRB.doubleValue());
                }
                for (int k = 0, np = gd.getNumbOfPoints(); k < np; k++) {
                    XmlDataAdaptor point_DA = graph_DA.createChild("XYErr");
                    point_DA.setValue("x", gd.getX(k));
                    point_DA.setValue("y", gd.getY(k));
                    point_DA.setValue("err", gd.getErr(k));
                }
            }
        }
        dataV = mv_tmp.getDataContainersRB();
        for (int j = 0, nd = dataV.size(); j < nd; j++) {
            BasicGraphData gd = (BasicGraphData) dataV.get(j);
            if (gd.getNumbOfPoints() > 0) {
                XmlDataAdaptor graph_DA = measuredPV_DA.createChild("Graph_For_scanPV_RB");
                graph_DA.setValue("legend", (String) gd.getGraphProperty(graphScan.getLegendKeyString()));
                for (int k = 0, np = gd.getNumbOfPoints(); k < np; k++) {
                    XmlDataAdaptor point_DA = graph_DA.createChild("XYErr");
                    point_DA.setValue("x", gd.getX(k));
                    point_DA.setValue("y", gd.getY(k));
                    point_DA.setValue("err", gd.getErr(k));
                }
            }
        }
    }

    /** restore xyplot object settings from a file
     * @param file- the file to read from
     */
    public void readSetupFrom(URL url) {
        XmlDataAdaptor xdaWrite = XmlDataAdaptor.adaptorForUrl(url, false);
        XmlDataAdaptor bbcda = xdaWrite.childAdaptor("BbcSetup");
        XmlDataAdaptor daAccel = bbcda.childAdaptor("accelerator");
        String acceleratorPath = daAccel.stringValue("xmlFile");
        if (acceleratorPath.length() > 0) {
            theDoc.setAcceleratorFilePath(acceleratorPath);
            System.out.println("accelFile = " + theDoc.getAcceleratorFilePath());
            String accelUrl = "file://" + theDoc.getAcceleratorFilePath();
            try {
                XMLDataManager dMgr = new XMLDataManager(accelUrl);
                theDoc.setAccelerator(dMgr.getAccelerator(), theDoc.getAcceleratorFilePath());
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(null, "Hey - I had trouble parsing the accelerator input xml file you fed me", "Xyz setup error", JOptionPane.ERROR_MESSAGE);
            }
        }
        List temp = daAccel.childAdaptors("sequence");
        if (temp.isEmpty()) return;
        ArrayList seqs = new ArrayList();
        DataAdaptor daSeq = daAccel.childAdaptor("sequence");
        String seqName = daSeq.stringValue("name");
        System.out.println("seq name = " + seqName);
        AcceleratorNode bpm1, bpm2[], bpm3, hCorrector, vCorrector, quadMagnet, BCM;
        temp = daSeq.childAdaptors("seq");
        Iterator itr = temp.iterator();
        while (itr.hasNext()) {
            DataAdaptor da = (DataAdaptor) itr.next();
            seqs.add(theDoc.getAccelerator().getSequence(da.stringValue("name")));
        }
        theDoc.setSelectedSequence(new AcceleratorSeqCombo(seqName, seqs));
        DataAdaptor bpm2Nodes = daSeq.childAdaptor("DownstreamBPMs");
        if (bpm2Nodes.hasAttribute("DownStreamBPMNum")) downstreamBPMNum = bpm2Nodes.intValue("DownStreamBPMNum");
        bpm2 = new AcceleratorNode[downstreamBPMNum];
        DataAdaptor daNodes = daSeq.childAdaptor("Nodes");
        if (daNodes.hasAttribute("CenterBPM")) {
            bpm1 = theDoc.getAccelerator().getNode(daNodes.stringValue("CenterBPM"));
            theDoc.myWindow().BPM1List.setSelectedValue(bpm1, true);
        }
        ArrayList<Integer> indexArray = new ArrayList();
        for (int i = 0; i < downstreamBPMNum; i++) {
            if (daNodes.hasAttribute("DownstreamBPM" + i)) {
                bpm2[i] = theDoc.getAccelerator().getNode(daNodes.stringValue("DownstreamBPM" + i));
                theDoc.myWindow().BPM2List.setSelectedValue(bpm2[i], true);
                indexArray.add(theDoc.myWindow().BPM2List.getSelectedIndex());
            }
        }
        if (!indexArray.isEmpty()) {
            int[] indices = new int[indexArray.size()];
            for (int i = 0; i < indexArray.size(); i++) {
                indices[i] = indexArray.get(i);
            }
            theDoc.myWindow().BPM2List.setSelectedIndices(indices);
        }
        if (daNodes.hasAttribute("UpstreamBPM")) {
            bpm3 = theDoc.getAccelerator().getNode(daNodes.stringValue("UpstreamBPM"));
            theDoc.myWindow().BPM3List.setSelectedValue(bpm3, true);
        }
        if (daNodes.hasAttribute("HDipoleCorr")) {
            hCorrector = theDoc.getAccelerator().getNode(daNodes.stringValue("HDipoleCorr"));
            theDoc.myWindow().correctorHList.setSelectedValue(hCorrector, true);
        }
        if (daNodes.hasAttribute("VDipoleCorr")) {
            vCorrector = theDoc.getAccelerator().getNode(daNodes.stringValue("VDipoleCorr"));
            theDoc.myWindow().correctorVList.setSelectedValue(vCorrector, true);
        }
        if (daNodes.hasAttribute("selectedQuadMagnet")) {
            quadMagnet = theDoc.getAccelerator().getNode(daNodes.stringValue("selectedQuadMagnet"));
        }
        if (daNodes.hasAttribute("BCM")) {
            BCM = theDoc.getAccelerator().getNode(daNodes.stringValue("BCM"));
            theDoc.myWindow().BCMList.setSelectedValue(BCM, true);
        }
        theDoc.myWindow().setAccelComponents();
        XmlDataAdaptor daScanH = bbcda.childAdaptor("HorizontalScan");
        if (daScanH != null) readScan(daScanH, theDoc.scanStuff.scanVariableQuadX, theDoc.scanStuff.scanVariableCorrX, theDoc.scanStuff.scanControllerX, theDoc.scanStuff.avgCntrX, theDoc.scanStuff.measuredXValuesV, theDoc.scanStuff.graphScanX, theDoc.scanStuff.vldCntrX);
        XmlDataAdaptor daScanV = bbcda.childAdaptor("VerticalScan");
        if (daScanV != null) readScan(daScanV, theDoc.scanStuff.scanVariableQuadY, theDoc.scanStuff.scanVariableCorrY, theDoc.scanStuff.scanControllerY, theDoc.scanStuff.avgCntrY, theDoc.scanStuff.measuredYValuesV, theDoc.scanStuff.graphScanY, theDoc.scanStuff.vldCntrY);
        theDoc.scanStuff.setColors(theDoc.scanStuff.measuredXValuesV, theDoc.scanStuff.graphScanX, -1);
        theDoc.scanStuff.updateGraphPanel(theDoc.scanStuff.measuredXValuesV, theDoc.scanStuff.graphScanX);
        theDoc.scanStuff.setColors(theDoc.scanStuff.measuredYValuesV, theDoc.scanStuff.graphScanY, -1);
        theDoc.scanStuff.updateGraphPanel(theDoc.scanStuff.measuredYValuesV, theDoc.scanStuff.graphScanY);
        DataAdaptor daAnalysis = bbcda.childAdaptor("initializeValue");
        DataAdaptor daImport = daAnalysis.childAdaptor("import");
        if (daImport != null) {
            if (daImport.hasAttribute("HDipoleCorrInitField")) {
                theDoc.hCorrInitField = daImport.doubleValue("HDipoleCorrInitField");
            }
            if (daImport.hasAttribute("VDipoleCorrInitField")) {
                theDoc.vCorrInitField = daImport.doubleValue("VDipoleCorrInitField");
            }
            if (daImport.hasAttribute("QuadrupoleInitField")) {
                theDoc.quadInitField = daImport.doubleValue("QuadrupoleInitField");
            }
        }
        theDoc.setHasChanges(false);
    }

    /** A method to parse the scan parameters and set up the scanStuff */
    private void readScan(XmlDataAdaptor scan2D_Adaptor, ScanVariable scanVariableQuad, ScanVariable scanVariableCorr, ScanController2D scanController, AvgController avgCntr, Vector measuredValuesV, FunctionGraphsJPanel graphScan, ValidationController vldCntr) {
        XmlDataAdaptor params_scan2D = scan2D_Adaptor.childAdaptor(paramsName_SR);
        XmlDataAdaptor paramPV_scan2D = scan2D_Adaptor.childAdaptor(paramPV_SR);
        XmlDataAdaptor scanPV_scan2D = scan2D_Adaptor.childAdaptor(scanPV_SR);
        XmlDataAdaptor measurePVs_scan2D = scan2D_Adaptor.childAdaptor(measurePVs_SR);
        XmlDataAdaptor params_limits = params_scan2D.childAdaptor("limits_step_delay");
        scanController.setLowLimit(params_limits.doubleValue("QuadLow"));
        scanController.setUppLimit(params_limits.doubleValue("QuadUpp"));
        scanController.setStep(params_limits.doubleValue("QuadStep"));
        scanController.setParamLowLimit(params_limits.doubleValue("DipoleCorrLow"));
        scanController.setParamUppLimit(params_limits.doubleValue("DipoleCorrUpp"));
        scanController.setParamStep(params_limits.doubleValue("DipoleCorrStep"));
        scanController.setSleepTime(params_limits.doubleValue("delay"));
        XmlDataAdaptor params_trigger = params_scan2D.childAdaptor("beam_trigger");
        if (params_trigger != null) {
            scanController.setBeamTriggerDelay(params_trigger.doubleValue("delay"));
            scanController.setBeamTriggerState(params_trigger.booleanValue("on"));
        }
        XmlDataAdaptor params_averg = params_scan2D.childAdaptor("averaging");
        avgCntr.setOnOff(params_averg.booleanValue("on"));
        avgCntr.setAvgNumber(params_averg.intValue("N"));
        avgCntr.setTimeDelay(params_averg.doubleValue("delay"));
        XmlDataAdaptor params_vld = params_scan2D.childAdaptor("validation");
        vldCntr.setOnOff(params_vld.booleanValue("on"));
        vldCntr.setUppLim(params_vld.doubleValue("upp"));
        vldCntr.setLowLim(params_vld.doubleValue("low"));
        XmlDataAdaptor param_PV_name_DA = paramPV_scan2D.childAdaptor("PV");
        if (param_PV_name_DA != null) {
            String param_PV_name = param_PV_name_DA.stringValue("name");
            Channel channel = ChannelFactory.defaultFactory().getChannel(param_PV_name);
            scanVariableCorr.setChannel(channel);
        }
        XmlDataAdaptor param_PV_RB_name_DA = scanPV_scan2D.childAdaptor("PV_RB");
        if (param_PV_RB_name_DA != null) {
            String scan_PV_RB_name = param_PV_RB_name_DA.stringValue("name");
            Channel channel = ChannelFactory.defaultFactory().getChannel(scan_PV_RB_name);
            scanVariableCorr.setChannelRB(channel);
        }
        XmlDataAdaptor scan_PV_name_DA = scanPV_scan2D.childAdaptor("PV");
        if (scan_PV_name_DA != null) {
            String scan_PV_name = scan_PV_name_DA.stringValue("name");
            Channel channel = ChannelFactory.defaultFactory().getChannel(scan_PV_name);
            scanVariableQuad.setChannel(channel);
        }
        XmlDataAdaptor scan_PV_RB_name_DA = scanPV_scan2D.childAdaptor("PV_RB");
        if (scan_PV_RB_name_DA != null) {
            String scan_PV_RB_name = scan_PV_RB_name_DA.stringValue("name");
            Channel channel = ChannelFactory.defaultFactory().getChannel(scan_PV_RB_name);
            scanVariableQuad.setChannelRB(channel);
        }
        java.util.Iterator<XmlDataAdaptor> measuredPVs_children = measurePVs_scan2D.childAdaptorIterator();
        MeasuredValue mv_tmp = null;
        while (measuredPVs_children.hasNext()) {
            XmlDataAdaptor measuredPV_DA = measuredPVs_children.next();
            String name = measuredPV_DA.stringValue("name");
            boolean onOff = measuredPV_DA.booleanValue("on");
            if (name.equals(centerBPMXName)) {
                mv_tmp = theDoc.scanStuff.BPM1XPosMV;
            } else if (name.equals(centerBPMYName)) {
                mv_tmp = theDoc.scanStuff.BPM1YPosMV;
            } else if (name.startsWith("downstreamBPM")) {
                for (int i = 0; i < downstreamBPMNum; i++) {
                    if (name.equals(("downstreamBPMX" + i))) {
                        mv_tmp = theDoc.scanStuff.BPM2XPosMV[i];
                    } else if (name.equals(("downstreamBPMY" + i))) {
                        mv_tmp = theDoc.scanStuff.BPM2YPosMV[i];
                    }
                }
            } else if (name.equals(upstreamBPMXName)) {
                mv_tmp = theDoc.scanStuff.BPM3XPosMV;
            } else if (name.equals(upstreamBPMYName)) {
                mv_tmp = theDoc.scanStuff.BPM3YPosMV;
            } else if (name.equals(quadMagXName)) {
                mv_tmp = theDoc.scanStuff.QuadMagXSetMV;
            } else if (name.equals(quadMagYName)) {
                mv_tmp = theDoc.scanStuff.QuadMagYSetMV;
            } else if (name.equals(BCMXName)) {
                mv_tmp = theDoc.scanStuff.BCMMVX;
            } else if (name.equals(BCMYName)) {
                mv_tmp = theDoc.scanStuff.BCMMVY;
            } else {
                String errText = "Oh no!, an unidentified set of measured data was encountered while reading the setup file";
                theDoc.myWindow().errorText.setText(errText);
                System.err.println(errText);
                return;
            }
            java.util.Iterator<XmlDataAdaptor> dataIt = measuredPV_DA.childAdaptorIterator("Graph_For_scanPV");
            while (dataIt.hasNext()) {
                BasicGraphData gd = new BasicGraphData();
                mv_tmp.addNewDataConatainer(gd);
                XmlDataAdaptor data = dataIt.next();
                String legend = data.stringValue("legend");
                XmlDataAdaptor paramDataValue = data.childAdaptor("parameter_value");
                if (paramDataValue != null) {
                    double parameter_value = paramDataValue.doubleValue("value");
                    gd.setGraphProperty("PARAMETER_VALUE", new Double(parameter_value));
                }
                XmlDataAdaptor paramDataValueRB = data.childAdaptor("parameter_value_RB");
                if (paramDataValueRB != null) {
                    double parameter_value_RB = paramDataValueRB.doubleValue("value");
                    gd.setGraphProperty("PARAMETER_VALUE_RB", new Double(parameter_value_RB));
                }
                gd.setGraphProperty(graphScan.getLegendKeyString(), legend);
                java.util.Iterator<XmlDataAdaptor> xyerrIt = data.childAdaptorIterator("XYErr");
                while (xyerrIt.hasNext()) {
                    XmlDataAdaptor xyerr = xyerrIt.next();
                    gd.addPoint(xyerr.doubleValue("x"), xyerr.doubleValue("y"), xyerr.doubleValue("err"));
                }
            }
            dataIt = measuredPV_DA.childAdaptorIterator("Graph_For_scanPV_RB");
            while (dataIt.hasNext()) {
                XmlDataAdaptor data = dataIt.next();
                String legend = data.stringValue("legend");
                BasicGraphData gd = new BasicGraphData();
                mv_tmp.addNewDataConatainerRB(gd);
                if (gd != null) {
                    gd.setGraphProperty(graphScan.getLegendKeyString(), legend);
                    java.util.Iterator<XmlDataAdaptor> xyerrIt = data.childAdaptorIterator("XYErr");
                    while (xyerrIt.hasNext()) {
                        XmlDataAdaptor xyerr = xyerrIt.next();
                        gd.addPoint(xyerr.doubleValue("x"), xyerr.doubleValue("y"), xyerr.doubleValue("err"));
                    }
                }
            }
        }
    }
}
