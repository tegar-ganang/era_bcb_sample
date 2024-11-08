package jp.jparc.apps.iPod;

import java.util.*;
import javax.swing.*;
import java.net.*;
import gov.sns.tools.xml.*;
import gov.sns.tools.data.*;
import gov.sns.xal.smf.data.*;
import gov.sns.xal.smf.*;
import gov.sns.tools.scan.SecondEdition.*;
import gov.sns.tools.plot.*;
import gov.sns.ca.*;

/**
 * A class to save and restore iPod documents to an xml file
 * @author  J. Galambos
 */
public class SaveOpen {

    /** The iPod documentto deal with */
    private iPodDocument theDoc;

    private XmlDataAdaptor xdaWrite;

    private String paramsName_SR = "app_params";

    private String paramPV_SR = "param_PV";

    private String scanPV_SR = "scan_PV";

    private String measurePVs_SR = "measure_PVs";

    private String FCT1PhaseName = "FCT1_Phase";

    private String FCT2PhaseName = "FCT2_Phase";

    private String FCT3PhaseName = "FCT3_Phase";

    private String cavAmpRBName = "cavAmpRB";

    private String BCMName = "BCMMV";

    /** constructor:
     * @param xyp - the XyPlot object
     */
    public SaveOpen(iPodDocument doc) {
        theDoc = doc;
    }

    /** save the object to a file
     * @param file- the file to save it to
     */
    public void saveTo(URL url) {
        xdaWrite = XmlDataAdaptor.newEmptyDocumentAdaptor();
        XmlDataAdaptor iPodda = xdaWrite.createChild("iPodSetup");
        DataAdaptor daAccel = iPodda.createChild("accelerator");
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
            DataAdaptor daNodes = daSeq.createChild("Nodes");
            if (theDoc.FCT1 != null) daNodes.setValue("FCT1", theDoc.FCT1);
            if (theDoc.FCT2 != null) daNodes.setValue("FCT2", theDoc.FCT2);
            if (theDoc.FCT3 != null) daNodes.setValue("FCT3", theDoc.FCT3);
            if (theDoc.theCavity != null) daNodes.setValue("cav", theDoc.theCavity);
            if (theDoc.theBCM != null) daNodes.setValue("BCM", theDoc.theBCM);
        }
        XmlDataAdaptor daScan2d = iPodda.createChild("scan");
        saveScan2D(daScan2d);
        DataAdaptor daAnalysis = iPodda.createChild("analysis");
        DataAdaptor daImport = daAnalysis.createChild("import");
        daImport.setValue("cavPhaseOffset", theDoc.cavPhaseOffset);
        daImport.setValue("beamPhaseOffset", theDoc.beamPhaseOffset);
        daImport.setValue("CavityType", theDoc.analysisStuff.cavTypeName);
        DataAdaptor daModel = daAnalysis.createChild("model");
        if (theDoc.analysisStuff.probeFile != null) daModel.setValue("probeFile", theDoc.analysisStuff.probeFile.getPath());
        daModel.setValue("FCTAmpMin", theDoc.analysisStuff.minFCTAmp);
        daModel.setValue("nCalcPoints", theDoc.analysisStuff.nCalcPoints);
        daModel.setValue("timeout", theDoc.analysisStuff.timeoutPeriod);
        daModel.setValue("phaseModelMin", theDoc.analysisStuff.phaseModelMin);
        daModel.setValue("phaseModelMax", theDoc.analysisStuff.phaseModelMax);
        daModel.setValue("WIn", theDoc.analysisStuff.WIn);
        daModel.setValue("cavPhaseOffset", theDoc.analysisStuff.cavPhaseOffset);
        daModel.setValue("ampValueIndex", theDoc.analysisStuff.amplitudeVariableIndex);
        daModel.setValue("ampValue", theDoc.analysisStuff.cavityVoltage);
        daModel.setValue("fudgeValue", theDoc.fudgePhaseOffset);
        xdaWrite.writeToUrl(url);
    }

    /** restore xyplot object settings from a file
     * @param file- the file to read from
     */
    public void readSetupFrom(URL url) {
        XmlDataAdaptor xdaWrite = XmlDataAdaptor.adaptorForUrl(url, false);
        XmlDataAdaptor iPodda = xdaWrite.childAdaptor("iPodSetup");
        DataAdaptor daAccel = iPodda.childAdaptor("accelerator");
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
        temp = daSeq.childAdaptors("seq");
        Iterator itr = temp.iterator();
        while (itr.hasNext()) {
            DataAdaptor da = (DataAdaptor) itr.next();
            seqs.add(theDoc.getAccelerator().getSequence(da.stringValue("name")));
        }
        theDoc.setSelectedSequence(new AcceleratorSeqCombo(seqName, seqs));
        AcceleratorNode bpm1, bpm2, cav, bcm;
        DataAdaptor daNodes = daSeq.childAdaptor("Nodes");
        if (daNodes.hasAttribute("FCT1")) {
            bpm1 = theDoc.getAccelerator().getNode(daNodes.stringValue("FCT1"));
            theDoc.myWindow().FCT1List.setSelectedValue(bpm1, true);
        }
        if (daNodes.hasAttribute("FCT2")) {
            bpm2 = theDoc.getAccelerator().getNode(daNodes.stringValue("FCT2"));
            theDoc.myWindow().FCT2List.setSelectedValue(bpm2, true);
        }
        if (daNodes.hasAttribute("FCT3")) {
            bpm2 = theDoc.getAccelerator().getNode(daNodes.stringValue("FCT3"));
            theDoc.myWindow().FCT3List.setSelectedValue(bpm2, true);
        }
        if (daNodes.hasAttribute("cav")) {
            cav = theDoc.getAccelerator().getNode(daNodes.stringValue("cav"));
            theDoc.myWindow().cavityList.setSelectedValue(cav, true);
        }
        if (daNodes.hasAttribute("BCM")) {
            bcm = theDoc.getAccelerator().getNode(daNodes.stringValue("BCM"));
            theDoc.myWindow().BCMList.setSelectedValue(bcm, true);
        }
        theDoc.myWindow().setAccelComponents();
        XmlDataAdaptor daScan2d = iPodda.childAdaptor("scan");
        if (daScan2d != null) readScan(daScan2d);
        theDoc.scanStuff.setColors(-1);
        theDoc.scanStuff.updateGraphPanel();
        DataAdaptor daAnalysis = iPodda.childAdaptor("analysis");
        DataAdaptor daModel = daAnalysis.childAdaptor("model");
        DataAdaptor daImport = daAnalysis.childAdaptor("import");
        if (daImport != null) {
            if (daImport.hasAttribute("cavPhaseOffset")) {
                theDoc.cavPhaseOffset = daImport.doubleValue("cavPhaseOffset");
            }
            if (daImport.hasAttribute("beamPhaseOffset")) {
                theDoc.beamPhaseOffset = daImport.doubleValue("beamPhaseOffset");
            }
            if (daImport.hasAttribute("CavityType")) {
                theDoc.analysisStuff.cavTypeName = daImport.stringValue("CavityType");
            }
        }
        if (daModel != null) {
            String fname = daModel.stringValue("probeFile");
            if (fname != null) {
                theDoc.analysisStuff.probeFileName = fname;
            }
            if (daModel.hasAttribute("FCTAmpMin")) {
                theDoc.analysisStuff.minFCTAmp = daModel.doubleValue("FCTAmpMin");
            }
            if (daModel.hasAttribute("nCalcPoints")) theDoc.analysisStuff.nCalcPoints = daModel.intValue("nCalcPoints");
            if (daModel.hasAttribute("timeout")) theDoc.analysisStuff.timeoutPeriod = daModel.doubleValue("timeout");
            if (daModel.hasAttribute("phaseModelMin")) theDoc.analysisStuff.phaseModelMin = daModel.doubleValue("phaseModelMin");
            if (daModel.hasAttribute("phaseModelMax")) theDoc.analysisStuff.phaseModelMax = daModel.doubleValue("phaseModelMax");
            if (daModel.hasAttribute("WIn")) theDoc.analysisStuff.WIn = daModel.doubleValue("WIn");
            if (daModel.hasAttribute("cavPhaseOffset")) theDoc.analysisStuff.cavPhaseOffset = daModel.doubleValue("cavPhaseOffset");
            if (daModel.hasAttribute("ampValueIndex")) theDoc.analysisStuff.amplitudeVariableIndex = daModel.intValue("ampValueIndex");
            if (daModel.hasAttribute("ampValue")) theDoc.analysisStuff.cavityVoltage = daModel.doubleValue("ampValue");
            if (daModel.hasAttribute("fudgeValue")) theDoc.fudgePhaseOffset = daModel.doubleValue("fudgeValue");
        }
        theDoc.myWindow().updateInputFields();
        theDoc.setHasChanges(false);
    }

    /** method to save scan stuff to the xml file */
    private void saveScan2D(XmlDataAdaptor scan2D_Adaptor) {
        XmlDataAdaptor params_scan2D = scan2D_Adaptor.createChild(paramsName_SR);
        XmlDataAdaptor paramPV_scan2D = scan2D_Adaptor.createChild(paramPV_SR);
        XmlDataAdaptor scanPV_scan2D = scan2D_Adaptor.createChild(scanPV_SR);
        XmlDataAdaptor measurePVs_scan2D = scan2D_Adaptor.createChild(measurePVs_SR);
        ScanVariable scanVariable = theDoc.scanStuff.scanVariable;
        ScanVariable scanVariableParameter = theDoc.scanStuff.scanVariableParameter;
        ScanController2D scanController = theDoc.scanStuff.scanController;
        AvgController avgCntr = theDoc.scanStuff.avgCntr;
        Vector measuredValuesV = theDoc.scanStuff.measuredValuesV;
        FunctionGraphsJPanel graphScan = theDoc.scanStuff.graphScan;
        XmlDataAdaptor params_limits = params_scan2D.createChild("limits_step_delay");
        params_limits.setValue("paramLow", scanController.getParamLowLimit());
        params_limits.setValue("paramUpp", scanController.getParamUppLimit());
        params_limits.setValue("paramStep", scanController.getParamStep());
        params_limits.setValue("low", scanController.getLowLimit());
        params_limits.setValue("upp", scanController.getUppLimit());
        params_limits.setValue("step", scanController.getStep());
        params_limits.setValue("delay", scanController.getSleepTime());
        XmlDataAdaptor params_trigger = params_scan2D.createChild("beam_trigger");
        params_trigger.setValue("on", scanController.getBeamTriggerState());
        params_trigger.setValue("delay", scanController.getBeamTriggerDelay());
        XmlDataAdaptor params_averg = params_scan2D.createChild("averaging");
        params_averg.setValue("on", avgCntr.isOn());
        params_averg.setValue("N", avgCntr.getAvgNumber());
        params_averg.setValue("delay", avgCntr.getTimeDelay());
        if (scanVariable.getChannel() != null) {
            XmlDataAdaptor scan_PV_name = scanPV_scan2D.createChild("PV");
            scan_PV_name.setValue("name", scanVariable.getChannelName());
        }
        if (scanVariable.getChannelRB() != null) {
            XmlDataAdaptor scan_PV_RB_name = scanPV_scan2D.createChild("PV_RB");
            scan_PV_RB_name.setValue("name", scanVariable.getChannelNameRB());
        }
        writeMeasuredValue(theDoc.scanStuff.FCT1PhaseMV, measurePVs_scan2D, FCT1PhaseName);
        writeMeasuredValue(theDoc.scanStuff.FCT2PhaseMV, measurePVs_scan2D, FCT2PhaseName);
        writeMeasuredValue(theDoc.scanStuff.FCT3PhaseMV, measurePVs_scan2D, FCT3PhaseName);
        writeMeasuredValue(theDoc.scanStuff.cavAmpRBMV, measurePVs_scan2D, cavAmpRBName);
        writeMeasuredValue(theDoc.scanStuff.BCMMV, measurePVs_scan2D, BCMName);
    }

    /** write a specific measuredValue to a data adaptor */
    private void writeMeasuredValue(MeasuredValue mv_tmp, XmlDataAdaptor measurePVs_scan2D, String name) {
        FunctionGraphsJPanel graphScan = theDoc.scanStuff.graphScan;
        XmlDataAdaptor measuredPV_DA = measurePVs_scan2D.createChild("MeasuredPV");
        measuredPV_DA.setValue("name", name);
        measuredPV_DA.setValue("unWrapped", new Boolean(mv_tmp.generateUnwrappedDataOn()));
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

    /** A method to parse the scan parameters and set up the scanStuff */
    private void readScan(XmlDataAdaptor scan2D_Adaptor) {
        ScanVariable scanVariable = theDoc.scanStuff.scanVariable;
        ScanVariable scanVariableParameter = theDoc.scanStuff.scanVariableParameter;
        ScanController2D scanController = theDoc.scanStuff.scanController;
        AvgController avgCntr = theDoc.scanStuff.avgCntr;
        Vector measuredValuesV = theDoc.scanStuff.measuredValuesV;
        FunctionGraphsJPanel graphScan = theDoc.scanStuff.graphScan;
        XmlDataAdaptor params_scan2D = scan2D_Adaptor.childAdaptor(paramsName_SR);
        XmlDataAdaptor paramPV_scan2D = scan2D_Adaptor.childAdaptor(paramPV_SR);
        XmlDataAdaptor scanPV_scan2D = scan2D_Adaptor.childAdaptor(scanPV_SR);
        XmlDataAdaptor measurePVs_scan2D = scan2D_Adaptor.childAdaptor(measurePVs_SR);
        XmlDataAdaptor params_limits = params_scan2D.childAdaptor("limits_step_delay");
        scanController.setLowLimit(params_limits.doubleValue("low"));
        scanController.setUppLimit(params_limits.doubleValue("upp"));
        scanController.setStep(params_limits.doubleValue("step"));
        scanController.setParamLowLimit(params_limits.doubleValue("paramLow"));
        scanController.setParamUppLimit(params_limits.doubleValue("paramUpp"));
        scanController.setParamStep(params_limits.doubleValue("paramStep"));
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
        XmlDataAdaptor scan_PV_name_DA = scanPV_scan2D.childAdaptor("PV");
        if (scan_PV_name_DA != null) {
            String scan_PV_name = scan_PV_name_DA.stringValue("name");
            Channel channel = ChannelFactory.defaultFactory().getChannel(scan_PV_name);
            scanVariable.setChannel(channel);
        }
        XmlDataAdaptor scan_PV_RB_name_DA = scanPV_scan2D.childAdaptor("PV_RB");
        if (scan_PV_RB_name_DA != null) {
            String scan_PV_RB_name = scan_PV_RB_name_DA.stringValue("name");
            Channel channel = ChannelFactory.defaultFactory().getChannel(scan_PV_RB_name);
            scanVariable.setChannelRB(channel);
        }
        java.util.Iterator<XmlDataAdaptor> measuredPVs_children = measurePVs_scan2D.childAdaptorIterator();
        MeasuredValue mv_tmp;
        while (measuredPVs_children.hasNext()) {
            XmlDataAdaptor measuredPV_DA = measuredPVs_children.next();
            String name = measuredPV_DA.stringValue("name");
            boolean onOff = measuredPV_DA.booleanValue("on");
            boolean unWrappedData = false;
            if (measuredPV_DA.stringValue("unWrapped") != null) {
                unWrappedData = measuredPV_DA.booleanValue("unWrapped");
            }
            if (name.equals(FCT1PhaseName)) {
                mv_tmp = theDoc.scanStuff.FCT1PhaseMV;
            } else if (name.equals(FCT2PhaseName)) {
                mv_tmp = theDoc.scanStuff.FCT2PhaseMV;
            } else if (name.equals(FCT3PhaseName)) {
                mv_tmp = theDoc.scanStuff.FCT3PhaseMV;
            } else if (name.equals(cavAmpRBName)) {
                mv_tmp = theDoc.scanStuff.cavAmpRBMV;
            } else if (name.equals(BCMName)) {
                mv_tmp = theDoc.scanStuff.BCMMV;
            } else {
                String errText = "Oh no!, an unidentified set of measured data was encountered while reading the setup file";
                theDoc.myWindow().errorText.setText(errText);
                System.err.println(errText);
                return;
            }
            mv_tmp.generateUnwrappedData(unWrappedData);
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
