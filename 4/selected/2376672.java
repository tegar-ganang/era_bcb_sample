package gov.sns.apps.pasta;

import java.util.*;
import javax.swing.*;
import java.net.*;
import gov.sns.tools.xml.*;
import gov.sns.tools.data.*;
import gov.sns.xal.smf.data.*;
import gov.sns.xal.smf.*;
import gov.sns.tools.scan.SecondEdition.*;
import gov.sns.tools.plot.*;
import gov.sns.tools.StringJoiner;
import gov.sns.ca.*;

/**
 * A class to save and restore pasta documents to an xml file
 * @author  J. Galambos
 */
public class SaveOpen {

    /** The pasta documentto deal with */
    private PastaDocument theDoc;

    private XmlDataAdaptor xdaRead, xdaWrite;

    private String paramsName_SR = "app_params";

    private String paramPV_SR = "param_PV";

    private String scanPV_SR = "scan_PV";

    private String measurePVs_SR = "measure_PVs";

    private String measureOffPVs_SR = "measureOff_PVs";

    private String validationPVs_SR = "validation_PVs";

    private String BPM1AmpName = "BPM1_Amp";

    private String BPM1PhaseName = "BPM1_Phase";

    private String BPM2AmpName = "BPM2_Amp";

    private String BPM2PhaseName = "BPM2_Phase";

    private String cavAmpRBName = "cavAmpRB";

    private String BCMName = "BCMMV";

    private String stringValue = "";

    private StringJoiner joiner = new StringJoiner(",");

    /** constructor:
     * @param xyp - the XyPlot object
     */
    public SaveOpen(PastaDocument doc) {
        theDoc = doc;
    }

    /** save the object to a file
     * @param file- the file to save it to
     */
    public void saveTo(URL url) {
        xdaWrite = XmlDataAdaptor.newEmptyDocumentAdaptor();
        XmlDataAdaptor pastada = xdaWrite.createChild("PastaSetup");
        DataAdaptor daAccel = pastada.createChild("accelerator");
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
            if (theDoc.BPM1 != null) daNodes.setValue("BPM1", theDoc.BPM1);
            if (theDoc.BPM2 != null) daNodes.setValue("BPM2", theDoc.BPM2);
            if (theDoc.theCavity != null) daNodes.setValue("cav", theDoc.theCavity);
            if (theDoc.theBCM != null) daNodes.setValue("BCM", theDoc.theBCM);
        }
        XmlDataAdaptor daScan1d = pastada.createChild("scan1d");
        saveScan1D(daScan1d);
        XmlDataAdaptor daScan2d = pastada.createChild("scan");
        saveScan2D(daScan2d);
        DataAdaptor daAnalysis = pastada.createChild("analysis");
        DataAdaptor daImport = daAnalysis.createChild("import");
        daImport.setValue("DTLPhaseOffset", theDoc.DTLPhaseOffset);
        daImport.setValue("BPMPhaseOffset", theDoc.BPMPhaseDiffOffset);
        DataAdaptor daModel = daAnalysis.createChild("model");
        if (theDoc.analysisStuff.probeFile != null) daModel.setValue("probeFile", theDoc.analysisStuff.probeFile.getPath());
        daModel.setValue("BPMAmpMin", theDoc.analysisStuff.minBPMAmp);
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
        XmlDataAdaptor pastada = xdaWrite.childAdaptor("PastaSetup");
        Double bigD;
        DataAdaptor daAccel = pastada.childAdaptor("accelerator");
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
        temp = daSeq.childAdaptors("seq");
        Iterator itr = temp.iterator();
        while (itr.hasNext()) {
            DataAdaptor da = (DataAdaptor) itr.next();
            seqs.add(theDoc.getAccelerator().getSequence(da.stringValue("name")));
        }
        theDoc.setSelectedSequence(new AcceleratorSeqCombo(seqName, seqs));
        AcceleratorNode bpm1, bpm2, cav, bcm;
        DataAdaptor daNodes = daSeq.childAdaptor("Nodes");
        if (daNodes.hasAttribute("BPM1")) {
            bpm1 = theDoc.getAccelerator().getNode(daNodes.stringValue("BPM1"));
            theDoc.myWindow().BPM1List.setSelectedValue(bpm1, true);
        }
        if (daNodes.hasAttribute("BPM2")) {
            bpm2 = theDoc.getAccelerator().getNode(daNodes.stringValue("BPM2"));
            theDoc.myWindow().BPM2List.setSelectedValue(bpm2, true);
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
        XmlDataAdaptor daScan1d = pastada.childAdaptor("scan1d");
        if (daScan1d != null) readScan1D(daScan1d);
        XmlDataAdaptor daScan2d = pastada.childAdaptor("scan");
        if (daScan2d != null) readScan(daScan2d);
        theDoc.scanStuff.setColors(-1);
        theDoc.scanStuff.setColors1D(-1);
        theDoc.scanStuff.updateGraphPanel();
        theDoc.scanStuff.updateGraph1DPanel();
        DataAdaptor daAnalysis = pastada.childAdaptor("analysis");
        DataAdaptor daModel = daAnalysis.childAdaptor("model");
        DataAdaptor daImport = daAnalysis.childAdaptor("import");
        if (daImport != null) {
            if (daImport.hasAttribute("DTLPhaseOffset")) {
                theDoc.DTLPhaseOffset = daImport.doubleValue("DTLPhaseOffset");
            }
            if (daImport.hasAttribute("BPMPhaseOffset")) {
                theDoc.BPMPhaseDiffOffset = daImport.doubleValue("BPMPhaseOffset");
            }
        }
        if (daModel != null) {
            String fname = daModel.stringValue("probeFile");
            if (fname != null) {
                theDoc.analysisStuff.probeFileName = fname;
            }
            if (daModel.hasAttribute("BPMAmpMin")) {
                theDoc.analysisStuff.minBPMAmp = daModel.doubleValue("BPMAmpMin");
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
        writeMeasuredValue(theDoc.scanStuff.BPM1PhaseMV, measurePVs_scan2D, BPM1PhaseName);
        writeMeasuredValue(theDoc.scanStuff.BPM1AmpMV, measurePVs_scan2D, BPM1AmpName);
        writeMeasuredValue(theDoc.scanStuff.BPM2PhaseMV, measurePVs_scan2D, BPM2PhaseName);
        writeMeasuredValue(theDoc.scanStuff.BPM2AmpMV, measurePVs_scan2D, BPM2AmpName);
        writeMeasuredValue(theDoc.scanStuff.cavAmpRBMV, measurePVs_scan2D, cavAmpRBName);
        writeMeasuredValue(theDoc.scanStuff.BCMMV, measurePVs_scan2D, BCMName);
    }

    private void saveScan1D(XmlDataAdaptor scan1D_Adaptor) {
        XmlDataAdaptor scanPV_scan1D = scan1D_Adaptor.createChild(scanPV_SR);
        XmlDataAdaptor measurePVs_scan1D = scan1D_Adaptor.createChild(measureOffPVs_SR);
        ScanVariable scanVariable = theDoc.scanStuff.scanVariable;
        ScanVariable scanVariableParameter = theDoc.scanStuff.scanVariableParameter;
        ScanController1D scanController = theDoc.scanStuff.scanController1D;
        AvgController avgCntr = theDoc.scanStuff.avgCntr1D;
        Vector measuredValuesV = theDoc.scanStuff.measuredValuesOffV;
        FunctionGraphsJPanel graphScan = theDoc.scanStuff.graphScan1D;
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
        if (scanVariable.getChannel() != null) {
            XmlDataAdaptor scan_PV_name = scanPV_scan1D.createChild("PV");
            scan_PV_name.setValue("name", scanVariable.getChannelName());
        }
        if (scanVariable.getChannelRB() != null) {
            XmlDataAdaptor scan_PV_RB_name = scanPV_scan1D.createChild("PV_RB");
            scan_PV_RB_name.setValue("name", scanVariable.getChannelNameRB());
        }
        writeMeasuredValue(theDoc.scanStuff.BPM1PhaseOffMV, measurePVs_scan1D, BPM1PhaseName);
        writeMeasuredValue(theDoc.scanStuff.BPM2PhaseOffMV, measurePVs_scan1D, BPM2PhaseName);
        writeMeasuredValue(theDoc.scanStuff.BCMOffMV, measurePVs_scan1D, BCMName);
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
            if (name.equals(BPM1PhaseName)) {
                mv_tmp = theDoc.scanStuff.BPM1PhaseMV;
            } else if (name.equals(BPM1AmpName)) {
                mv_tmp = theDoc.scanStuff.BPM1AmpMV;
            } else if (name.equals(BPM2PhaseName)) {
                mv_tmp = theDoc.scanStuff.BPM2PhaseMV;
            } else if (name.equals(BPM2AmpName)) {
                mv_tmp = theDoc.scanStuff.BPM2AmpMV;
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

    /** A method to parse the scan parameters and set up the scanStuff */
    private void readScan1D(XmlDataAdaptor scan1D_Adaptor) {
        ScanVariable scanVariable = theDoc.scanStuff.scanVariable;
        ScanController1D scanController = theDoc.scanStuff.scanController1D;
        AvgController avgCntr = theDoc.scanStuff.avgCntr1D;
        Vector measuredValuesV = theDoc.scanStuff.measuredValuesOffV;
        FunctionGraphsJPanel graphScan = theDoc.scanStuff.graphScan1D;
        XmlDataAdaptor scanPV_scan1D = scan1D_Adaptor.childAdaptor(scanPV_SR);
        XmlDataAdaptor measurePVs_scan1D = scan1D_Adaptor.childAdaptor(measureOffPVs_SR);
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
        XmlDataAdaptor scan_PV_DA = scan1D_Adaptor.childAdaptor("scan_PV");
        if (scan_PV_DA != null) {
            XmlDataAdaptor scan_PV_name_DA = scan_PV_DA.childAdaptor("PV");
            if (scan_PV_name_DA != null) {
                String scan_PV_name = scan_PV_name_DA.stringValue("name");
                if (scan_PV_name != null) {
                    Channel channel = ChannelFactory.defaultFactory().getChannel(scan_PV_name);
                    scanVariable.setChannel(channel);
                }
            }
            XmlDataAdaptor scan_PV_RB_name_DA = scan_PV_DA.childAdaptor("PV_RB");
            if (scan_PV_RB_name_DA != null) {
                String scan_PV_RB_name = scan_PV_RB_name_DA.stringValue("name");
                Channel channel = ChannelFactory.defaultFactory().getChannel(scan_PV_RB_name);
                scanVariable.setChannelRB(channel);
            }
        }
        java.util.Iterator<XmlDataAdaptor> measuredPVs_children = measurePVs_scan1D.childAdaptorIterator();
        MeasuredValue mv_tmp;
        while (measuredPVs_children.hasNext()) {
            XmlDataAdaptor measuredPV_DA = measuredPVs_children.next();
            String name = measuredPV_DA.stringValue("name");
            boolean onOff = measuredPV_DA.booleanValue("on");
            boolean unWrappedData = false;
            if (measuredPV_DA.stringValue("unWrapped") != null) {
                unWrappedData = measuredPV_DA.booleanValue("unWrapped");
            }
            if (name.equals(BPM1PhaseName)) {
                mv_tmp = theDoc.scanStuff.BPM1PhaseOffMV;
            } else if (name.equals(BPM2PhaseName)) {
                mv_tmp = theDoc.scanStuff.BPM2PhaseOffMV;
            } else if (name.equals(BCMName)) {
                mv_tmp = theDoc.scanStuff.BCMOffMV;
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
