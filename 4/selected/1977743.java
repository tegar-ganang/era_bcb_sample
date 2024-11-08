package gov.sns.apps.scan.Scan2D;

import java.net.*;
import java.io.*;
import java.text.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.*;
import javax.swing.tree.DefaultTreeModel;
import gov.sns.ca.*;
import gov.sns.tools.plot.*;
import gov.sns.application.*;
import gov.sns.tools.xml.*;
import gov.sns.tools.apputils.*;
import gov.sns.tools.apputils.PVSelection.*;
import gov.sns.tools.scan.SecondEdition.*;
import gov.sns.tools.swing.*;
import gov.sns.tools.scan.SecondEdition.analysis.*;
import gov.sns.tools.pvlogger.*;
import gov.sns.tools.database.*;

/**
 *  ScanDocument2D is a custom XalDocument for 2D scan application. The document
 *  manages the data that is displayed in the window.
 *
 *@author    shishlo
 */
public class ScanDocument2D extends XalDocument {

    static {
        ChannelFactory.defaultFactory().init();
    }

    private Action setScanPanelAction = null;

    private Action setPVsChooserPanelAction = null;

    private Action setAnalysisPanelAction = null;

    private Action setAcceleratorAction = null;

    private Action setPredefConfigAction = null;

    private JPanel selectionPVsPanel = new JPanel();

    private PVTreeNode root_Node = new PVTreeNode("ROOT");

    private PVTreeNode rootParameterPV_Node = new PVTreeNode("Parameter PV");

    private PVTreeNode parameterPV_Node = new PVTreeNode("PV Set");

    private PVTreeNode parameterPV_RB_Node = new PVTreeNode("PV Read Back");

    private PVTreeNode rootScanPV_Node = new PVTreeNode("Scan PV");

    private PVTreeNode scanPV_Node = new PVTreeNode("PV Set");

    private PVTreeNode scanPV_RB_Node = new PVTreeNode("PV Read Back");

    private PVTreeNode measuredPVs_Node = new PVTreeNode("Measured PVs");

    private PVTreeNode validationPVs_Node = new PVTreeNode("Validation PVs");

    private String rootParameterPV_Node_Name = "Parameter PV";

    private String rootScanPV_Node_Name = "Scan PV";

    private String measuredPVs_Node_Name = "Measured PVs";

    private String validationPVs_Node_Name = "Validation PVs";

    private PVsSelector pvsSelector = null;

    private ActionListener switchPVTreeListener = null;

    private ActionListener createDeletePVTreeListener = null;

    private ActionListener renamePVTreeListener = null;

    private JPanel scanPanel = new JPanel();

    private JPanel leftScanControlPanel = null;

    private ScanController2D scanController = new ScanController2D("SCAN CONTROL PANEL");

    private AvgController avgCntr = new AvgController();

    private ValidationController vldCntr = new ValidationController();

    private ScanVariable scanVariable = null;

    private Vector measuredValuesV = new Vector();

    private Vector validationValuesV = new Vector();

    private Vector measuredValuesShowStateV = new Vector();

    private boolean scanPV_ShowState = false;

    private boolean scanPV_RB_ShowState = false;

    private PVsTreePanel pvsTreePanelScan = null;

    private JPanel analysisPanel = new JPanel();

    private MainAnalysisController analysisController = null;

    private PVsTreePanel pvsTreePanelAnalysis = null;

    private ParameterPV_Controller parameterPV_Controller = null;

    private ScanVariable scanVariableParameter = null;

    private boolean paramPV_ON = true;

    private FunctionGraphsJPanel graphScan = new FunctionGraphsJPanel();

    private FunctionGraphsJPanel graphAnalysis = new FunctionGraphsJPanel();

    private JTextField messageTextLocal = null;

    private static volatile int monitoredPV_Count = 0;

    private JPanel preferencesPanel = new JPanel();

    private JButton setFont_PrefPanel_Button = new JButton("Set Font Size");

    private JSpinner fontSize_PrefPanel_Spinner = new JSpinner(new SpinnerNumberModel(7, 7, 26, 1));

    private Font globalFont = null;

    private JCheckBox useTimeStampButton = new JCheckBox("Use Time Stamp On Legend", true);

    private PredefinedConfController predefinedConfController = null;

    private JPanel configPanel = null;

    private int ACTIVE_PANEL = 0;

    private int SCAN_PANEL = 0;

    private int ANALYSIS_PANEL = 1;

    private int SET_PVs_PANEL = 2;

    private int PREFERENCES_PANEL = 3;

    private int PREDEF_CONF_PANEL = 4;

    private static DateAndTimeText dateAndTime = new DateAndTimeText();

    private File acceleratorDataFile = null;

    private String dataRootName_SR = "Scan2D_Application";

    private String paramsName_SR = "app_params";

    private String paramPV_SR = "param_PV";

    private String scanPV_SR = "scan_PV";

    private String measurePVs_SR = "measure_PVs";

    private String validationPVs_SR = "validation_PVs";

    private XmlDataAdaptor analysisConfig = null;

    private String analysisConfig_SR = "ANALYSIS_CONFIGURATIONS";

    private JButton makeSnapshotButton = new JButton("Make PV Logger Snapshot");

    private JButton clearSnapshotButton = new JButton("Clear Snapshot");

    private String noSnapshotIdString = "No Snapshot";

    private String snapshotIdString = "Last Snapshot Id: ";

    private JLabel snapshotIdLabel = new JLabel("No Snapshot", JLabel.LEFT);

    private long snapshotId = -1;

    private boolean pvLogged = false;

    private LoggerSession loggerSession = null;

    private MachineSnapshot snapshot = null;

    /**
	 *  Create a new empty ScanDocument2D
	 */
    public ScanDocument2D() {
        ACTIVE_PANEL = SCAN_PANEL;
        rootParameterPV_Node.setPVNamesAllowed(false);
        parameterPV_Node.setPVNamesAllowed(true);
        parameterPV_RB_Node.setPVNamesAllowed(true);
        rootScanPV_Node.setPVNamesAllowed(false);
        scanPV_Node.setPVNamesAllowed(true);
        scanPV_RB_Node.setPVNamesAllowed(true);
        measuredPVs_Node.setPVNamesAllowed(true);
        validationPVs_Node.setPVNamesAllowed(true);
        parameterPV_Node.setPVNumberLimit(1);
        parameterPV_RB_Node.setPVNumberLimit(1);
        parameterPV_Node.setCheckBoxVisible(false);
        parameterPV_RB_Node.setCheckBoxVisible(false);
        scanPV_Node.setPVNumberLimit(1);
        scanPV_RB_Node.setPVNumberLimit(1);
        rootParameterPV_Node.add(parameterPV_Node);
        rootParameterPV_Node.add(parameterPV_RB_Node);
        rootScanPV_Node.add(scanPV_Node);
        rootScanPV_Node.add(scanPV_RB_Node);
        root_Node.add(rootParameterPV_Node);
        root_Node.add(rootScanPV_Node);
        root_Node.add(measuredPVs_Node);
        root_Node.add(validationPVs_Node);
        pvsSelector = new PVsSelector(root_Node);
        pvsSelector.removeMessageTextField();
        makeTreeListeners();
        scanPV_Node.setSwitchedOnOffListener(switchPVTreeListener);
        scanPV_RB_Node.setSwitchedOnOffListener(switchPVTreeListener);
        measuredPVs_Node.setSwitchedOnOffListener(switchPVTreeListener);
        validationPVs_Node.setSwitchedOnOffListener(switchPVTreeListener);
        parameterPV_Node.setCreateRemoveListener(createDeletePVTreeListener);
        parameterPV_RB_Node.setCreateRemoveListener(createDeletePVTreeListener);
        scanPV_Node.setCreateRemoveListener(createDeletePVTreeListener);
        scanPV_RB_Node.setCreateRemoveListener(createDeletePVTreeListener);
        measuredPVs_Node.setCreateRemoveListener(createDeletePVTreeListener);
        validationPVs_Node.setCreateRemoveListener(createDeletePVTreeListener);
        parameterPV_Node.setRenameListener(renamePVTreeListener);
        parameterPV_RB_Node.setRenameListener(renamePVTreeListener);
        scanPV_Node.setRenameListener(renamePVTreeListener);
        scanPV_RB_Node.setRenameListener(renamePVTreeListener);
        measuredPVs_Node.setRenameListener(renamePVTreeListener);
        validationPVs_Node.setRenameListener(renamePVTreeListener);
        scanVariableParameter = new ScanVariable("param_var_" + monitoredPV_Count, "param_var_RB_" + (monitoredPV_Count + 1));
        parameterPV_Controller = new ParameterPV_Controller(scanVariableParameter);
        paramPV_ON = true;
        scanVariable = new ScanVariable("scan_var_" + monitoredPV_Count, "scan_var_RB_" + (monitoredPV_Count + 1));
        monitoredPV_Count++;
        monitoredPV_Count++;
        scanController.setScanVariable(scanVariable);
        scanController.setParamVariable(scanVariableParameter);
        scanController.setAvgController(avgCntr);
        scanController.setValidationController(vldCntr);
        scanController.getUnitsLabel().setText(" [a.u.]");
        scanController.getParamUnitsLabel().setText(" [a.u.]");
        scanController.setParamPhaseScanButtonVisible(true);
        scanController.setValuePhaseScanButtonVisible(true);
        scanController.addNewSetOfDataListener(new ActionListener() {

            public void actionPerformed(ActionEvent evn) {
                String paramPV_string = "";
                String scanPV_string = "";
                String scanPV_RB_string = "";
                String measurePV_string = "";
                String legend_string = "";
                String legend_string_RB = "";
                Double paramValue = new Double(scanController.getParamValue());
                Double paramValueRB = new Double(scanController.getParamValueRB());
                if (paramPV_ON && parameterPV_Controller.getChannel() != null) {
                    paramPV_string = paramPV_string + " par.PV : " + parameterPV_Controller.getChannel().getId() + "=" + parameterPV_Controller.getValueAsString();
                    paramValue = new Double(parameterPV_Controller.getValue());
                    if (parameterPV_Controller.getChannelRB() != null) {
                        paramValueRB = new Double(parameterPV_Controller.getValueRB());
                    }
                } else {
                    paramPV_string = paramPV_string + " param.= " + paramValue;
                }
                if (scanVariable.getChannel() != null) {
                    scanPV_string = "xPV=" + scanVariable.getChannel().getId();
                }
                if (scanVariable.getChannelRB() != null) {
                    scanPV_RB_string = "xPV=" + scanVariable.getChannelRB().getId();
                }
                for (int i = 0, n = measuredValuesV.size(); i < n; i++) {
                    if (((Boolean) measuredValuesShowStateV.get(i)).booleanValue()) {
                        MeasuredValue mv_tmp = (MeasuredValue) measuredValuesV.get(i);
                        BasicGraphData gd = mv_tmp.getDataContainer();
                        if (mv_tmp.getChannel() != null) {
                            measurePV_string = "yPV=" + mv_tmp.getChannel().getId();
                        }
                        if (useTimeStampButton.isSelected()) {
                            legend_string = dateAndTime.getTime();
                        } else {
                            legend_string = "";
                        }
                        legend_string_RB = legend_string;
                        legend_string = legend_string + " " + scanPV_string + " " + measurePV_string + paramPV_string + " ";
                        if (gd != null) {
                            gd.setGraphProperty(graphScan.getLegendKeyString(), legend_string);
                            if (paramValue != null) {
                                gd.setGraphProperty("PARAMETER_VALUE", paramValue);
                            }
                            if (paramValueRB != null) {
                                gd.setGraphProperty("PARAMETER_VALUE_RB", paramValueRB);
                            }
                        }
                        legend_string_RB = legend_string_RB + " " + scanPV_RB_string + " " + measurePV_string + paramPV_string + " ";
                        if (scanVariable.getChannelRB() != null) {
                            gd = mv_tmp.getDataContainerRB();
                            if (gd != null) {
                                gd.setGraphProperty(graphScan.getLegendKeyString(), legend_string_RB);
                                if (paramValue != null) {
                                    gd.setGraphProperty("PARAMETER_VALUE", paramValue);
                                }
                                if (paramValueRB != null) {
                                    gd.setGraphProperty("PARAMETER_VALUE_RB", paramValueRB);
                                }
                            }
                        }
                    }
                }
                updateDataSetOnGraphPanels();
            }
        });
        scanController.addNewPointOfDataListener(new ActionListener() {

            public void actionPerformed(ActionEvent evn) {
                graphScan.refreshGraphJPanel();
                graphAnalysis.refreshGraphJPanel();
            }
        });
        SimpleChartPopupMenu.addPopupMenuTo(graphScan);
        SimpleChartPopupMenu.addPopupMenuTo(graphAnalysis);
        graphScan.setOffScreenImageDrawing(true);
        graphAnalysis.setOffScreenImageDrawing(true);
        graphScan.setName("SCAN : Measured Values vs. Scan PV's Values");
        graphAnalysis.setName("ANALYSIS : Measured Values vs. Scan PV's Values");
        graphScan.setAxisNames("Scan PV Values", "Measured Values");
        graphAnalysis.setAxisNames("Scan PV Values", "Measured Values");
        graphScan.setGraphBackGroundColor(Color.white);
        graphAnalysis.setGraphBackGroundColor(Color.white);
        analysisConfig = XmlDataAdaptor.newEmptyDocumentAdaptor().createChild(analysisConfig_SR);
        analysisConfig.createChild("MANAGEMENT");
        analysisConfig.createChild("FIND_MIN_MAX");
        analysisConfig.createChild("POLYNOMIAL_FITTING");
        analysisConfig.createChild("INTERSECTION_FINDING");
        makeScanPanel();
        makeAnalysisPanel();
        makeSelectionPVsPanel();
        makePreferencesPanel();
        makePredefinedConfigurationsPanel();
    }

    /**
	 *  Create a new document loaded from the URL file
	 *
	 *@param  url  The URL of the file to load into the new document.
	 */
    public ScanDocument2D(URL url) {
        this();
        if (url == null) {
            return;
        }
        setSource(url);
        readScanDocument(url);
        if (url.getProtocol().equals("jar")) {
            return;
        }
        setHasChanges(true);
    }

    /**
	 *  Reads the content of the document from the specified URL.
	 *
	 *@param  url  Description of the Parameter
	 */
    public void readScanDocument(URL url) {
        XmlDataAdaptor readAdp = null;
        readAdp = XmlDataAdaptor.adaptorForUrl(url, false);
        if (readAdp != null) {
            XmlDataAdaptor scan2D_Adaptor = readAdp.childAdaptor(dataRootName_SR);
            XmlDataAdaptor params_scan2D = scan2D_Adaptor.childAdaptor(paramsName_SR);
            XmlDataAdaptor paramPV_scan2D = scan2D_Adaptor.childAdaptor(paramPV_SR);
            XmlDataAdaptor scanPV_scan2D = scan2D_Adaptor.childAdaptor(scanPV_SR);
            XmlDataAdaptor measurePVs_scan2D = scan2D_Adaptor.childAdaptor(measurePVs_SR);
            XmlDataAdaptor validationPVs_scan2D = scan2D_Adaptor.childAdaptor(validationPVs_SR);
            XmlDataAdaptor tmp_analysisConfig = scan2D_Adaptor.childAdaptor(analysisConfig_SR);
            if (tmp_analysisConfig != null) {
                analysisConfig = tmp_analysisConfig;
            } else {
                analysisConfig = XmlDataAdaptor.newEmptyDocumentAdaptor().createChild(analysisConfig_SR);
                analysisConfig.createChild("MANAGEMENT");
                analysisConfig.createChild("FIND_MIN_MAX");
                analysisConfig.createChild("POLYNOMIAL_FITTING");
                analysisConfig.createChild("INTERSECTION_FINDING");
            }
            setTitle(scan2D_Adaptor.stringValue("title"));
            XmlDataAdaptor params_font = params_scan2D.childAdaptor("font");
            globalFont = new Font(params_font.stringValue("name"), params_font.intValue("style"), params_font.intValue("size"));
            XmlDataAdaptor params_scan_panel_title = params_scan2D.childAdaptor("scan_panel_title");
            if (params_scan_panel_title != null && params_scan_panel_title.stringValue("title") != null) {
                scanController.setTitle(params_scan_panel_title.stringValue("title"));
            } else {
                scanController.setTitle("SCAN CONTROL PANEL");
            }
            XmlDataAdaptor pv_logger_id = params_scan2D.childAdaptor("pv_logger_id");
            if (pv_logger_id != null && pv_logger_id.intValue("Id") > 0) {
                snapshotId = pv_logger_id.intValue("Id");
                snapshotIdLabel.setText(snapshotIdString + snapshotId + "  ");
                pvLogged = true;
            } else {
                snapshotId = -1;
                pvLogged = false;
                snapshotIdLabel.setText(noSnapshotIdString);
            }
            XmlDataAdaptor params_scan_panel_paramRB_label = params_scan2D.childAdaptor("sc_panel_paramRB_label");
            if (params_scan_panel_paramRB_label != null) {
                scanController.getParamRB_Label().setText(params_scan_panel_paramRB_label.stringValue("label"));
            }
            XmlDataAdaptor params_scan_panel_paramStep_label = params_scan2D.childAdaptor("sc_panel_paramStep_label");
            if (params_scan_panel_paramStep_label != null) {
                scanController.getParamScanStep_Label().setText(params_scan_panel_paramStep_label.stringValue("label"));
            }
            XmlDataAdaptor params_scan_panel_scanRB_label = params_scan2D.childAdaptor("sc_panel_scanRB_label");
            if (params_scan_panel_scanRB_label != null) {
                scanController.getValueRB_Label().setText(params_scan_panel_scanRB_label.stringValue("label"));
            }
            XmlDataAdaptor params_scan_panel_scanStep_label = params_scan2D.childAdaptor("sc_panel_scanStep_label");
            if (params_scan_panel_scanStep_label != null) {
                scanController.getScanStep_Label().setText(params_scan_panel_scanStep_label.stringValue("label"));
            }
            XmlDataAdaptor params_scan_panel_paramUnits_label = params_scan2D.childAdaptor("sc_panel_paramUnits_label");
            if (params_scan_panel_paramUnits_label != null) {
                scanController.getParamUnitsLabel().setText(params_scan_panel_paramUnits_label.stringValue("label"));
            }
            XmlDataAdaptor params_scan_panel_scanUnits_label = params_scan2D.childAdaptor("sc_panel_scanUnits_label");
            if (params_scan_panel_scanUnits_label != null) {
                scanController.getUnitsLabel().setText(params_scan_panel_scanUnits_label.stringValue("label"));
            }
            XmlDataAdaptor paramPV_tree_node_name = params_scan2D.childAdaptor("parameterPV_tree_name");
            if (paramPV_tree_node_name != null && paramPV_tree_node_name.stringValue("name") != null) {
                rootParameterPV_Node.setName(paramPV_tree_node_name.stringValue("name"));
            } else {
                rootParameterPV_Node.setName(rootParameterPV_Node_Name);
            }
            XmlDataAdaptor scanPV_tree_node_name = params_scan2D.childAdaptor("scanPV_tree_name");
            if (scanPV_tree_node_name != null && scanPV_tree_node_name.stringValue("name") != null) {
                rootScanPV_Node.setName(scanPV_tree_node_name.stringValue("name"));
            } else {
                rootScanPV_Node.setName(rootScanPV_Node_Name);
            }
            XmlDataAdaptor measuredPVs_tree_node_name = params_scan2D.childAdaptor("measuredPVs_tree_name");
            if (measuredPVs_tree_node_name != null && measuredPVs_tree_node_name.stringValue("name") != null) {
                measuredPVs_Node.setName(measuredPVs_tree_node_name.stringValue("name"));
            } else {
                measuredPVs_Node.setName(measuredPVs_Node_Name);
            }
            XmlDataAdaptor validationPVs_tree_node_name = params_scan2D.childAdaptor("validationPVs_tree_name");
            if (validationPVs_tree_node_name != null && validationPVs_tree_node_name.stringValue("name") != null) {
                validationPVs_Node.setName(validationPVs_tree_node_name.stringValue("name"));
            } else {
                validationPVs_Node.setName(validationPVs_Node_Name);
            }
            XmlDataAdaptor params_UseTimeStamp = params_scan2D.childAdaptor("UseTimeStamp");
            if (params_UseTimeStamp != null && params_UseTimeStamp.hasAttribute("yes")) {
                useTimeStampButton.setSelected(params_UseTimeStamp.booleanValue("yes"));
            }
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
            XmlDataAdaptor params_validation = params_scan2D.childAdaptor("validation");
            vldCntr.setOnOff(params_validation.booleanValue("on"));
            vldCntr.setLowLim(params_validation.doubleValue("low"));
            vldCntr.setUppLim(params_validation.doubleValue("upp"));
            XmlDataAdaptor params_paramPV_name = paramPV_scan2D.childAdaptor("PV");
            if (params_paramPV_name != null) {
                String PV_name = params_paramPV_name.stringValue("name");
                PVTreeNode pvNodeNew = new PVTreeNode(PV_name);
                Channel channel = ChannelFactory.defaultFactory().getChannel(PV_name);
                parameterPV_Controller.setChannel(channel);
                pvNodeNew.setChannel(channel);
                pvNodeNew.setAsPVName(true);
                pvNodeNew.setCheckBoxVisible(parameterPV_Node.isCheckBoxVisible());
                parameterPV_Node.add(pvNodeNew);
                pvNodeNew.setSwitchedOnOffListener(parameterPV_Node.getSwitchedOnOffListener());
                pvNodeNew.setCreateRemoveListener(parameterPV_Node.getCreateRemoveListener());
                pvNodeNew.setRenameListener(parameterPV_Node.getRenameListener());
            }
            XmlDataAdaptor params_paramPV_nameRB = paramPV_scan2D.childAdaptor("PV_RB");
            if (params_paramPV_nameRB != null) {
                String PV_nameRB = params_paramPV_nameRB.stringValue("name");
                PVTreeNode pvNodeNew = new PVTreeNode(PV_nameRB);
                Channel channel = ChannelFactory.defaultFactory().getChannel(PV_nameRB);
                parameterPV_Controller.setChannelRB(channel);
                pvNodeNew.setChannel(channel);
                pvNodeNew.setAsPVName(true);
                pvNodeNew.setCheckBoxVisible(parameterPV_RB_Node.isCheckBoxVisible());
                parameterPV_RB_Node.add(pvNodeNew);
                pvNodeNew.setSwitchedOnOffListener(parameterPV_RB_Node.getSwitchedOnOffListener());
                pvNodeNew.setCreateRemoveListener(parameterPV_RB_Node.getCreateRemoveListener());
                pvNodeNew.setRenameListener(parameterPV_RB_Node.getRenameListener());
            }
            String paramPV_PanelTitle = paramPV_scan2D.stringValue("panel_title");
            if (paramPV_PanelTitle != null) {
                parameterPV_Controller.setTitle(paramPV_PanelTitle);
            } else {
                parameterPV_Controller.setTitle("PARAMETER PV CONTROL");
            }
            XmlDataAdaptor scan_PV_name_DA = scanPV_scan2D.childAdaptor("PV");
            if (scan_PV_name_DA != null) {
                String scan_PV_name = scan_PV_name_DA.stringValue("name");
                boolean scan_PV_on = scan_PV_name_DA.booleanValue("on");
                scanPV_ShowState = scan_PV_on;
                PVTreeNode pvNodeNew = new PVTreeNode(scan_PV_name);
                Channel channel = ChannelFactory.defaultFactory().getChannel(scan_PV_name);
                pvNodeNew.setChannel(channel);
                pvNodeNew.setAsPVName(true);
                pvNodeNew.setCheckBoxVisible(scanPV_Node.isCheckBoxVisible());
                scanPV_Node.add(pvNodeNew);
                pvNodeNew.setSwitchedOnOffListener(scanPV_Node.getSwitchedOnOffListener());
                pvNodeNew.setCreateRemoveListener(scanPV_Node.getCreateRemoveListener());
                pvNodeNew.setRenameListener(scanPV_Node.getRenameListener());
                scanVariable.setChannel(channel);
                graphScan.setAxisNames("Scan PV : " + scan_PV_name, "Measured Values");
                graphAnalysis.setAxisNames("Scan PV : " + scan_PV_name, "Measured Values");
                pvNodeNew.setSwitchedOn(scanPV_ShowState);
            }
            XmlDataAdaptor scan_PV_RB_name_DA = scanPV_scan2D.childAdaptor("PV_RB");
            if (scan_PV_RB_name_DA != null) {
                String scan_PV_RB_name = scan_PV_RB_name_DA.stringValue("name");
                boolean scan_PV_RB_on = scan_PV_RB_name_DA.booleanValue("on");
                scanPV_RB_ShowState = scan_PV_RB_on;
                PVTreeNode pvNodeNew = new PVTreeNode(scan_PV_RB_name);
                Channel channel = ChannelFactory.defaultFactory().getChannel(scan_PV_RB_name);
                pvNodeNew.setChannel(channel);
                pvNodeNew.setAsPVName(true);
                pvNodeNew.setCheckBoxVisible(scanPV_RB_Node.isCheckBoxVisible());
                scanPV_RB_Node.add(pvNodeNew);
                pvNodeNew.setSwitchedOnOffListener(scanPV_RB_Node.getSwitchedOnOffListener());
                pvNodeNew.setCreateRemoveListener(scanPV_RB_Node.getCreateRemoveListener());
                pvNodeNew.setRenameListener(scanPV_RB_Node.getRenameListener());
                scanVariable.setChannelRB(channel);
                pvNodeNew.setSwitchedOn(scanPV_RB_ShowState);
            }
            Iterator<XmlDataAdaptor> validation_children = validationPVs_scan2D.childAdaptorIterator();
            while (validation_children.hasNext()) {
                XmlDataAdaptor validationPV_DA = validation_children.next();
                String name = validationPV_DA.stringValue("name");
                boolean onOff = validationPV_DA.booleanValue("on");
                PVTreeNode pvNodeNew = new PVTreeNode(name);
                Channel channel = ChannelFactory.defaultFactory().getChannel(name);
                pvNodeNew.setChannel(channel);
                pvNodeNew.setAsPVName(true);
                pvNodeNew.setCheckBoxVisible(validationPVs_Node.isCheckBoxVisible());
                validationPVs_Node.add(pvNodeNew);
                pvNodeNew.setSwitchedOn(onOff);
                pvNodeNew.setSwitchedOnOffListener(validationPVs_Node.getSwitchedOnOffListener());
                pvNodeNew.setCreateRemoveListener(validationPVs_Node.getCreateRemoveListener());
                pvNodeNew.setRenameListener(validationPVs_Node.getRenameListener());
                MeasuredValue mv_tmp = new MeasuredValue("validation_pv_" + monitoredPV_Count);
                monitoredPV_Count++;
                mv_tmp.setChannel(pvNodeNew.getChannel());
                validationValuesV.add(mv_tmp);
                if (onOff) {
                    scanController.addValidationValue(mv_tmp);
                }
            }
            java.util.Iterator<XmlDataAdaptor> measuredPVs_children = measurePVs_scan2D.childAdaptorIterator();
            while (measuredPVs_children.hasNext()) {
                XmlDataAdaptor measuredPV_DA = measuredPVs_children.next();
                String name = measuredPV_DA.stringValue("name");
                boolean onOff = measuredPV_DA.booleanValue("on");
                boolean unWrappedData = false;
                if (measuredPV_DA.stringValue("unWrapped") != null) {
                    unWrappedData = measuredPV_DA.booleanValue("unWrapped");
                }
                PVTreeNode pvNodeNew = new PVTreeNode(name);
                Channel channel = ChannelFactory.defaultFactory().getChannel(name);
                pvNodeNew.setChannel(channel);
                pvNodeNew.setAsPVName(true);
                pvNodeNew.setCheckBoxVisible(measuredPVs_Node.isCheckBoxVisible());
                measuredPVs_Node.add(pvNodeNew);
                pvNodeNew.setSwitchedOn(onOff);
                pvNodeNew.setSwitchedOnOffListener(measuredPVs_Node.getSwitchedOnOffListener());
                pvNodeNew.setCreateRemoveListener(measuredPVs_Node.getCreateRemoveListener());
                pvNodeNew.setRenameListener(measuredPVs_Node.getRenameListener());
                MeasuredValue mv_tmp = new MeasuredValue("measured_pv_" + monitoredPV_Count);
                mv_tmp.generateUnwrappedData(unWrappedData);
                monitoredPV_Count++;
                mv_tmp.setChannel(pvNodeNew.getChannel());
                measuredValuesShowStateV.add(new Boolean(onOff));
                measuredValuesV.add(mv_tmp);
                if (onOff) {
                    scanController.addMeasuredValue(mv_tmp);
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
            analysisController.createChildAnalysis(analysisConfig);
            setColors(-1);
            updateDataSetOnGraphPanels();
        }
    }

    /**
	 *  Make a main window by instantiating the ScanWindow2D window.
	 */
    @Override
    public void makeMainWindow() {
        mainWindow = new ScanWindow2D(this);
        getScanWindow().setJComponent(scanPanel);
        messageTextLocal = getScanWindow().getMessageTextField();
        scanController.getMessageText().setDocument(messageTextLocal.getDocument());
        pvsSelector.getMessageJTextField().setDocument(messageTextLocal.getDocument());
        analysisController.setMessageTextField(messageTextLocal);
        parameterPV_Controller.setMessageTextField(messageTextLocal);
        if (globalFont == null) {
            globalFont = new Font("Monospaced", Font.BOLD, 10);
        }
        fontSize_PrefPanel_Spinner.setValue(new Integer(globalFont.getSize()));
        setFontForAll(globalFont);
        predefinedConfController.setMessageTextField(getScanWindow().getMessageTextField());
        JToolBar toolbar = getScanWindow().getToolBar();
        JTextField timeTxt_temp = dateAndTime.getNewTimeTextField();
        timeTxt_temp.setHorizontalAlignment(JTextField.CENTER);
        toolbar.add(timeTxt_temp);
        mainWindow.setSize(new Dimension(700, 600));
    }

    /**
	 *  Dispose of ScanDocument2D resources. This method overrides an empty
	 *  superclass method.
	 */
    @Override
    protected void freeCustomResources() {
        cleanUp();
    }

    /**
	 *  Save the ScanDocument2D document to the specified URL.
	 *
	 *@param  url  Description of the Parameter
	 */
    @Override
    public void saveDocumentAs(URL url) {
        XmlDataAdaptor da = XmlDataAdaptor.newEmptyDocumentAdaptor();
        XmlDataAdaptor scan2D_Adaptor = da.createChild(dataRootName_SR);
        scan2D_Adaptor.setValue("title", url.getFile());
        XmlDataAdaptor params_scan2D = scan2D_Adaptor.createChild(paramsName_SR);
        XmlDataAdaptor paramPV_scan2D = scan2D_Adaptor.createChild(paramPV_SR);
        XmlDataAdaptor scanPV_scan2D = scan2D_Adaptor.createChild(scanPV_SR);
        XmlDataAdaptor validationPVs_scan2D = scan2D_Adaptor.createChild(validationPVs_SR);
        analysisConfig = scan2D_Adaptor.createChild(analysisConfig_SR);
        XmlDataAdaptor measurePVs_scan2D = scan2D_Adaptor.createChild(measurePVs_SR);
        analysisController.dumpChildAnalysisConfig(analysisConfig);
        XmlDataAdaptor params_font = params_scan2D.createChild("font");
        params_font.setValue("name", globalFont.getFamily());
        params_font.setValue("style", globalFont.getStyle());
        params_font.setValue("size", globalFont.getSize());
        XmlDataAdaptor params_scan_panel_title = params_scan2D.createChild("scan_panel_title");
        params_scan_panel_title.setValue("title", scanController.getTitle());
        XmlDataAdaptor pv_logger_id = params_scan2D.createChild("pv_logger_id");
        pv_logger_id.setValue("Id", (int) snapshotId);
        XmlDataAdaptor params_scan_panel_paramRB_label = params_scan2D.createChild("sc_panel_paramRB_label");
        params_scan_panel_paramRB_label.setValue("label", scanController.getParamRB_Label().getText());
        XmlDataAdaptor params_scan_panel_paramStep_label = params_scan2D.createChild("sc_panel_paramStep_label");
        params_scan_panel_paramStep_label.setValue("label", scanController.getParamScanStep_Label().getText());
        XmlDataAdaptor params_scan_panel_scanRB_label = params_scan2D.createChild("sc_panel_scanRB_label");
        params_scan_panel_scanRB_label.setValue("label", scanController.getValueRB_Label().getText());
        XmlDataAdaptor params_scan_panel_scanStep_label = params_scan2D.createChild("sc_panel_scanStep_label");
        params_scan_panel_scanStep_label.setValue("label", scanController.getScanStep_Label().getText());
        XmlDataAdaptor params_scan_panel_paramUnits_label = params_scan2D.createChild("sc_panel_paramUnits_label");
        params_scan_panel_paramUnits_label.setValue("label", scanController.getParamUnitsLabel().getText());
        XmlDataAdaptor params_scan_panel_scanUnits_label = params_scan2D.createChild("sc_panel_scanUnits_label");
        params_scan_panel_scanUnits_label.setValue("label", scanController.getUnitsLabel().getText());
        XmlDataAdaptor paramPV_tree_node_name = params_scan2D.createChild("parameterPV_tree_name");
        paramPV_tree_node_name.setValue("name", rootParameterPV_Node.getName());
        XmlDataAdaptor scanPV_tree_node_name = params_scan2D.createChild("scanPV_tree_name");
        scanPV_tree_node_name.setValue("name", rootScanPV_Node.getName());
        XmlDataAdaptor measuredPVs_tree_node_name = params_scan2D.createChild("measuredPVs_tree_name");
        measuredPVs_tree_node_name.setValue("name", measuredPVs_Node.getName());
        XmlDataAdaptor validationPVs_tree_node_name = params_scan2D.createChild("validationPVs_tree_name");
        validationPVs_tree_node_name.setValue("name", validationPVs_Node.getName());
        XmlDataAdaptor params_UseTimeStamp = params_scan2D.createChild("UseTimeStamp");
        params_UseTimeStamp.setValue("yes", useTimeStampButton.isSelected());
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
        XmlDataAdaptor params_validation = params_scan2D.createChild("validation");
        params_validation.setValue("on", vldCntr.isOn());
        params_validation.setValue("low", vldCntr.getInnerLowLim());
        params_validation.setValue("upp", vldCntr.getInnerUppLim());
        paramPV_scan2D.setValue("on", paramPV_ON);
        paramPV_scan2D.setValue("panel_title", parameterPV_Controller.getTitle());
        if (scanVariableParameter.getChannel() != null) {
            XmlDataAdaptor params_paramPV_name = paramPV_scan2D.createChild("PV");
            params_paramPV_name.setValue("name", scanVariableParameter.getChannelName());
        }
        if (scanVariableParameter.getChannelRB() != null) {
            XmlDataAdaptor params_paramPV_nameRB = paramPV_scan2D.createChild("PV_RB");
            params_paramPV_nameRB.setValue("name", scanVariableParameter.getChannelNameRB());
        }
        if (scanVariable.getChannel() != null) {
            XmlDataAdaptor scan_PV_name = scanPV_scan2D.createChild("PV");
            scan_PV_name.setValue("name", scanVariable.getChannelName());
            scan_PV_name.setValue("on", scanPV_ShowState);
        }
        if (scanVariable.getChannelRB() != null) {
            XmlDataAdaptor scan_PV_RB_name = scanPV_scan2D.createChild("PV_RB");
            scan_PV_RB_name.setValue("name", scanVariable.getChannelNameRB());
            scan_PV_RB_name.setValue("on", scanPV_RB_ShowState);
        }
        Enumeration validation_children = validationPVs_Node.children();
        while (validation_children.hasMoreElements()) {
            PVTreeNode pvNode = (PVTreeNode) validation_children.nextElement();
            XmlDataAdaptor validationPV_node = validationPVs_scan2D.createChild("Validation_PV");
            validationPV_node.setValue("name", pvNode.getChannel().channelName());
            validationPV_node.setValue("on", pvNode.isSwitchedOn());
        }
        for (int i = 0, n = measuredValuesV.size(); i < n; i++) {
            MeasuredValue mv_tmp = (MeasuredValue) measuredValuesV.get(i);
            XmlDataAdaptor measuredPV_DA = measurePVs_scan2D.createChild("MeasuredPV");
            measuredPV_DA.setValue("name", mv_tmp.getChannel().channelName());
            measuredPV_DA.setValue("on", ((Boolean) measuredValuesShowStateV.get(i)).booleanValue());
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
        try {
            scan2D_Adaptor.writeTo(new File(url.getFile()));
            setHasChanges(true);
        } catch (IOException e) {
            System.out.println("IOException e=" + e);
        }
    }

    /**
	 *  Edit preferences for the document.
	 */
    void editPreferences() {
        if (!scanController.isScanON()) {
            setAcceleratorAction.setEnabled(false);
            if (ACTIVE_PANEL == ANALYSIS_PANEL) {
                analysisController.isGoingShutUp();
                updateDataSetOnGraphPanels();
            }
            getScanWindow().setJComponent(preferencesPanel);
            cleanMessageTextField();
            ACTIVE_PANEL = PREFERENCES_PANEL;
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    /**
	 *  Convenience method for getting the ScanWindow2D window. It is the cast to
	 *  the proper subclass of XalWindow. This allows me to avoid casting the
	 *  window every time I reference it.
	 *
	 *@return    The main window cast to its dynamic runtime class
	 */
    private ScanWindow2D getScanWindow() {
        return (ScanWindow2D) mainWindow;
    }

    /**
	 *  Register actions for the menu items and toolbar.
	 *
	 *@param  commander  Description of the Parameter
	 */
    @Override
    protected void customizeCommands(Commander commander) {
        setScanPanelAction = new AbstractAction("show-scan-panel") {

            public void actionPerformed(ActionEvent event) {
                if (!scanController.isScanON()) {
                    setAcceleratorAction.setEnabled(false);
                    if (ACTIVE_PANEL == ANALYSIS_PANEL) {
                        analysisController.isGoingShutUp();
                        updateDataSetOnGraphPanels();
                    }
                    getScanWindow().setJComponent(scanPanel);
                    cleanMessageTextField();
                    ACTIVE_PANEL = SCAN_PANEL;
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        };
        commander.registerAction(setScanPanelAction);
        setPVsChooserPanelAction = new AbstractAction("show-pvs-panel") {

            public void actionPerformed(ActionEvent event) {
                if (!scanController.isScanON()) {
                    setAcceleratorAction.setEnabled(true);
                    if (ACTIVE_PANEL == ANALYSIS_PANEL) {
                        analysisController.isGoingShutUp();
                        updateDataSetOnGraphPanels();
                    }
                    getScanWindow().setJComponent(selectionPVsPanel);
                    cleanMessageTextField();
                    ACTIVE_PANEL = SET_PVs_PANEL;
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        };
        commander.registerAction(setPVsChooserPanelAction);
        setAnalysisPanelAction = new AbstractAction("show-analysis-panel") {

            public void actionPerformed(ActionEvent event) {
                if (!scanController.isScanON()) {
                    setAcceleratorAction.setEnabled(false);
                    analysisController.isGoingShowUp();
                    getScanWindow().setJComponent(analysisPanel);
                    cleanMessageTextField();
                    ACTIVE_PANEL = ANALYSIS_PANEL;
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        };
        commander.registerAction(setAnalysisPanelAction);
        setAcceleratorAction = new AbstractAction("set-accelerator") {

            public void actionPerformed(ActionEvent event) {
                JFileChooser ch = new JFileChooser();
                ch.setDialogTitle("READ ACCELERATOR DATA XML FILE");
                if (acceleratorDataFile != null) {
                    ch.setSelectedFile(acceleratorDataFile);
                }
                int returnVal = ch.showOpenDialog(selectionPVsPanel);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    acceleratorDataFile = ch.getSelectedFile();
                    String path = acceleratorDataFile.getAbsolutePath();
                    pvsSelector.setAcceleratorFileName(path);
                }
            }
        };
        commander.registerAction(setAcceleratorAction);
        setAcceleratorAction.setEnabled(false);
        setPredefConfigAction = new AbstractAction("set-predef-config") {

            public void actionPerformed(ActionEvent event) {
                if (!scanController.isScanON()) {
                    setAcceleratorAction.setEnabled(false);
                    getScanWindow().setJComponent(configPanel);
                    cleanMessageTextField();
                    ACTIVE_PANEL = PREDEF_CONF_PANEL;
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        };
        commander.registerAction(setPredefConfigAction);
    }

    /**
	 *  Description of the Method
	 */
    private void makeScanPanel() {
        pvsTreePanelScan = pvsSelector.getNewPVsTreePanel();
        pvsTreePanelScan.getJTree().setBackground(Color.white);
        pvsTreePanelScan.setPreferredSize(new Dimension(0, 0));
        pvsSelector.setPreferredSize(new Dimension(0, 0));
        scanPanel.setLayout(new BorderLayout());
        JPanel tmp_0 = new JPanel();
        tmp_0.setLayout(new VerticalLayout());
        tmp_0.add(scanController.getJPanel());
        tmp_0.add(avgCntr.getJPanel(0));
        tmp_0.add(vldCntr.getJPanel(0));
        tmp_0.add(parameterPV_Controller.getJPanel());
        leftScanControlPanel = tmp_0;
        JPanel tmp_1 = new JPanel();
        tmp_1.setLayout(new BorderLayout());
        tmp_1.add(tmp_0, BorderLayout.NORTH);
        tmp_1.add(pvsTreePanelScan, BorderLayout.CENTER);
        Border etchedBorder = BorderFactory.createEtchedBorder();
        JPanel tmp_2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
        tmp_2.add(makeSnapshotButton);
        tmp_2.add(snapshotIdLabel);
        tmp_2.add(clearSnapshotButton);
        tmp_2.setBorder(etchedBorder);
        JPanel tmp_3 = new JPanel();
        tmp_3.setLayout(new BorderLayout());
        tmp_3.setBorder(etchedBorder);
        tmp_3.add(tmp_2, BorderLayout.NORTH);
        tmp_3.add(graphScan, BorderLayout.CENTER);
        scanPanel.add(tmp_1, BorderLayout.WEST);
        scanPanel.add(tmp_3, BorderLayout.CENTER);
        makeSnapshotButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                ConnectionDictionary dict = ConnectionDictionary.defaultDictionary();
                SqlStateStore store;
                if (dict != null) {
                    store = new SqlStateStore(dict);
                } else {
                    ConnectionPreferenceController.displayPathPreferenceSelector();
                    dict = ConnectionDictionary.defaultDictionary();
                    store = new SqlStateStore(dict);
                }
                ChannelGroup group = store.fetchGroup("default");
                loggerSession = new LoggerSession(group, store);
                snapshot = loggerSession.takeSnapshot();
                Date startTime = new Date();
                String comments = startTime.toString();
                comments = comments + " === Scan1D asked for snapshot. ====";
                snapshot.setComment(comments);
                loggerSession.publishSnapshot(snapshot);
                snapshotId = snapshot.getId();
                pvLogged = true;
                snapshotIdLabel.setText(snapshotIdString + snapshotId + "  ");
            }
        });
        clearSnapshotButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                snapshotId = -1;
                pvLogged = false;
                snapshotIdLabel.setText(noSnapshotIdString);
            }
        });
    }

    /**
	 *  Description of the Method
	 */
    private void makeAnalysisPanel() {
        pvsTreePanelAnalysis = pvsSelector.getNewPVsTreePanel();
        pvsTreePanelAnalysis.setPreferredSize(new Dimension(0, 0));
        pvsTreePanelAnalysis.getJTree().setBackground(Color.white);
        analysisPanel.setLayout(new BorderLayout());
        JPanel tmp_0 = new JPanel();
        JPanel tmp_1 = new JPanel();
        tmp_1.setLayout(new BorderLayout());
        tmp_1.add(tmp_0, BorderLayout.NORTH);
        tmp_1.add(pvsTreePanelAnalysis, BorderLayout.CENTER);
        Border etchedBorder = BorderFactory.createEtchedBorder();
        JPanel tmp_2 = new JPanel();
        tmp_2.setLayout(new BorderLayout());
        tmp_2.setBorder(etchedBorder);
        analysisPanel.add(tmp_1, BorderLayout.WEST);
        analysisPanel.add(tmp_2, BorderLayout.CENTER);
        analysisController = new MainAnalysisController(this, analysisPanel, tmp_0, tmp_2, scanVariableParameter, scanVariable, measuredValuesV, measuredValuesShowStateV, graphScan, graphAnalysis);
        analysisController.createChildAnalysis(analysisConfig);
    }

    /**
	 *  Description of the Method
	 */
    private void makeSelectionPVsPanel() {
        selectionPVsPanel.setLayout(new BorderLayout());
        selectionPVsPanel.add(pvsSelector, BorderLayout.CENTER);
    }

    /**
	 *  Description of the Method
	 */
    private void makePreferencesPanel() {
        fontSize_PrefPanel_Spinner.setAlignmentX(JSpinner.CENTER_ALIGNMENT);
        preferencesPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
        preferencesPanel.add(fontSize_PrefPanel_Spinner);
        preferencesPanel.add(setFont_PrefPanel_Button);
        preferencesPanel.add(useTimeStampButton);
        setFont_PrefPanel_Button.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int fnt_size = ((Integer) fontSize_PrefPanel_Spinner.getValue()).intValue();
                globalFont = new Font(globalFont.getFamily(), globalFont.getStyle(), fnt_size);
                setFontForAll(globalFont);
            }
        });
    }

    /**
	 *  Description of the Method
	 */
    private void makePredefinedConfigurationsPanel() {
        predefinedConfController = new PredefinedConfController(this, "config", "predefinedConfiguration.scan2D");
        configPanel = predefinedConfController.getJPanel();
        ActionListener selectConfListener = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                URL url = (URL) e.getSource();
                if (url == null) {
                    Toolkit.getDefaultToolkit().beep();
                    messageTextLocal.setText(null);
                    messageTextLocal.setText("Cannot find an input configuration file!");
                }
                cleanUp();
                readScanDocument(url);
                setHasChanges(false);
                setFontForAll(globalFont);
                setAcceleratorAction.setEnabled(false);
                if (ACTIVE_PANEL == ANALYSIS_PANEL) {
                    analysisController.isGoingShutUp();
                    updateDataSetOnGraphPanels();
                }
                getScanWindow().setJComponent(scanPanel);
                cleanMessageTextField();
                ACTIVE_PANEL = SCAN_PANEL;
            }
        };
        predefinedConfController.setSelectorListener(selectConfListener);
    }

    /**
	 *  Description of the Method
	 */
    private void makeTreeListeners() {
        switchPVTreeListener = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                String command = e.getActionCommand();
                PVTreeNode pvn = (PVTreeNode) e.getSource();
                boolean switchOnLocal = command.equals(PVTreeNode.SWITCHED_ON_COMMAND);
                PVTreeNode pvn_parent = (PVTreeNode) pvn.getParent();
                int index = -1;
                if (switchOnLocal) {
                    if (pvn_parent == scanPV_Node) {
                        scanPV_ShowState = true;
                        updateDataSetOnGraphPanels();
                    }
                    if (pvn_parent == scanPV_RB_Node) {
                        scanPV_RB_ShowState = true;
                        updateDataSetOnGraphPanels();
                    }
                    if (pvn_parent == measuredPVs_Node) {
                        index = pvn_parent.getIndex(pvn);
                        measuredValuesShowStateV.set(index, new Boolean(true));
                        MeasuredValue mv_tmp = (MeasuredValue) measuredValuesV.get(index);
                        scanController.removeMeasuredValue(mv_tmp);
                        scanController.addMeasuredValue(mv_tmp);
                        updateDataSetOnGraphPanels();
                    }
                    if (pvn_parent == validationPVs_Node) {
                        index = pvn_parent.getIndex(pvn);
                        scanController.removeValidationValue((MeasuredValue) validationValuesV.get(index));
                        scanController.addValidationValue((MeasuredValue) validationValuesV.get(index));
                    }
                } else {
                    if (pvn_parent == scanPV_Node) {
                        scanPV_ShowState = false;
                        updateDataSetOnGraphPanels();
                    }
                    if (pvn_parent == scanPV_RB_Node) {
                        scanPV_RB_ShowState = false;
                        updateDataSetOnGraphPanels();
                    }
                    if (pvn_parent == measuredPVs_Node) {
                        index = pvn_parent.getIndex(pvn);
                        measuredValuesShowStateV.set(index, new Boolean(false));
                        MeasuredValue mv_tmp = (MeasuredValue) measuredValuesV.get(index);
                        scanController.removeMeasuredValue(mv_tmp);
                        updateDataSetOnGraphPanels();
                    }
                    if (pvn_parent == validationPVs_Node) {
                        index = pvn_parent.getIndex(pvn);
                        scanController.removeValidationValue((MeasuredValue) validationValuesV.get(index));
                    }
                }
            }
        };
        createDeletePVTreeListener = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                PVTreeNode pvn = (PVTreeNode) e.getSource();
                PVTreeNode pvn_parent = (PVTreeNode) pvn.getParent();
                String command = e.getActionCommand();
                boolean bool_removePV = command.equals(PVTreeNode.REMOVE_PV_COMMAND);
                int index = -1;
                if (bool_removePV) {
                    if (pvn_parent == parameterPV_Node) {
                        parameterPV_Controller.setChannel(null);
                    }
                    if (pvn_parent == parameterPV_RB_Node) {
                        parameterPV_Controller.setChannelRB(null);
                    }
                    if (pvn_parent == scanPV_Node) {
                        scanVariable.setChannel(null);
                        scanPV_ShowState = false;
                        graphScan.setAxisNames("Scan PV Values", "Measured Values");
                        graphAnalysis.setAxisNames("Scan PV Values", "Measured Values");
                        for (int i = 0, n = measuredValuesV.size(); i < n; i++) {
                            MeasuredValue mv_tmp = (MeasuredValue) measuredValuesV.get(i);
                            mv_tmp.removeAllDataContainersNonRB();
                        }
                        updateDataSetOnGraphPanels();
                    }
                    if (pvn_parent == scanPV_RB_Node) {
                        scanVariable.setChannelRB(null);
                        scanPV_RB_ShowState = false;
                        for (int i = 0, n = measuredValuesV.size(); i < n; i++) {
                            MeasuredValue mv_tmp = (MeasuredValue) measuredValuesV.get(i);
                            mv_tmp.removeAllDataContainersRB();
                        }
                        updateDataSetOnGraphPanels();
                    }
                    if (pvn_parent == measuredPVs_Node) {
                        index = pvn_parent.getIndex(pvn);
                        MeasuredValue mv_tmp = (MeasuredValue) measuredValuesV.get(index);
                        scanController.removeMeasuredValue(mv_tmp);
                        MonitoredPV mpv_tmp = mv_tmp.getMonitoredPV();
                        MonitoredPV.removeMonitoredPV(mpv_tmp);
                        measuredValuesV.remove(index);
                        measuredValuesShowStateV.remove(index);
                        updateDataSetOnGraphPanels();
                        setColors(index);
                    }
                    if (pvn_parent == validationPVs_Node) {
                        index = pvn_parent.getIndex(pvn);
                        MeasuredValue mv_tmp = (MeasuredValue) validationValuesV.get(index);
                        scanController.removeValidationValue(mv_tmp);
                        MonitoredPV mpv_tmp = mv_tmp.getMonitoredPV();
                        MonitoredPV.removeMonitoredPV(mpv_tmp);
                        validationValuesV.remove(index);
                    }
                } else {
                    if (pvn_parent == parameterPV_Node) {
                        parameterPV_Controller.setChannel(pvn.getChannel());
                    }
                    if (pvn_parent == parameterPV_RB_Node) {
                        parameterPV_Controller.setChannelRB(pvn.getChannel());
                    }
                    if (pvn_parent == scanPV_Node) {
                        scanVariable.setChannel(pvn.getChannel());
                        scanPV_ShowState = true;
                        graphScan.setAxisNames("Scan PV : " + pvn.getChannel().getId(), "Measured Values");
                        graphAnalysis.setAxisNames("Scan PV : " + pvn.getChannel().getId(), "Measured Values");
                        updateDataSetOnGraphPanels();
                    }
                    if (pvn_parent == scanPV_RB_Node) {
                        scanVariable.setChannelRB(pvn.getChannel());
                        scanPV_RB_ShowState = true;
                        updateDataSetOnGraphPanels();
                    }
                    if (pvn_parent == measuredPVs_Node) {
                        index = pvn_parent.getIndex(pvn);
                        MeasuredValue mv_tmp = new MeasuredValue("measured_pv_" + monitoredPV_Count);
                        monitoredPV_Count++;
                        mv_tmp.setChannel(pvn.getChannel());
                        measuredValuesV.add(mv_tmp);
                        measuredValuesShowStateV.add(new Boolean(true));
                        scanController.addMeasuredValue(mv_tmp);
                        setColors(-1);
                    }
                    if (pvn_parent == validationPVs_Node) {
                        index = pvn_parent.getIndex(pvn);
                        MeasuredValue mv_tmp = new MeasuredValue("measured_pv_" + monitoredPV_Count);
                        monitoredPV_Count++;
                        mv_tmp.setChannel(pvn.getChannel());
                        validationValuesV.add(mv_tmp);
                        scanController.addValidationValue(mv_tmp);
                    }
                }
            }
        };
        renamePVTreeListener = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                PVTreeNode pvn = (PVTreeNode) e.getSource();
                PVTreeNode pvn_parent = (PVTreeNode) pvn.getParent();
                int index = -1;
                if (pvn_parent == parameterPV_Node) {
                    parameterPV_Controller.setChannel(pvn.getChannel());
                }
                if (pvn_parent == parameterPV_RB_Node) {
                    parameterPV_Controller.setChannelRB(pvn.getChannel());
                }
                if (pvn_parent == scanPV_Node) {
                    scanVariable.setChannel(pvn.getChannel());
                    graphScan.setAxisNames("Scan PV : " + pvn.getChannel().getId(), "Measured Values");
                    graphAnalysis.setAxisNames("Scan PV : " + pvn.getChannel().getId(), "Measured Values");
                    graphScan.refreshGraphJPanel();
                    graphAnalysis.refreshGraphJPanel();
                }
                if (pvn_parent == scanPV_RB_Node) {
                    scanVariable.setChannelRB(pvn.getChannel());
                }
                if (pvn_parent == measuredPVs_Node) {
                    index = pvn_parent.getIndex(pvn);
                    MeasuredValue mv_tmp = (MeasuredValue) measuredValuesV.get(index);
                    MonitoredPV mpv_tmp = mv_tmp.getMonitoredPV();
                    mpv_tmp.setChannel(pvn.getChannel());
                }
                if (pvn_parent == validationPVs_Node) {
                    index = pvn_parent.getIndex(pvn);
                    MeasuredValue mv_tmp = (MeasuredValue) validationValuesV.get(index);
                    MonitoredPV mpv_tmp = mv_tmp.getMonitoredPV();
                    mpv_tmp.setChannel(pvn.getChannel());
                }
            }
        };
    }

    /**
	 *  Description of the Method
	 */
    private void updateDataSetOnGraphPanels() {
        graphScan.removeAllGraphData();
        for (int i = 0, n = measuredValuesV.size(); i < n; i++) {
            if (((Boolean) measuredValuesShowStateV.get(i)).booleanValue()) {
                MeasuredValue mv_tmp = (MeasuredValue) measuredValuesV.get(i);
                if (scanPV_ShowState || scanVariable.getChannel() == null) {
                    graphScan.addGraphData(mv_tmp.getDataContainers());
                }
                if (scanPV_RB_ShowState) {
                    graphScan.addGraphData(mv_tmp.getDataContainersRB());
                }
            }
        }
        analysisController.setScanPVandScanPV_RB_State(scanPV_ShowState, scanPV_RB_ShowState);
        analysisController.updateDataSetOnGraphPanel();
    }

    /**
	 *  Sets the colors attribute of the ScanDocument2D object
	 *
	 *@param  deleteIndex  The new colors value
	 */
    private void setColors(int deleteIndex) {
        for (int i = 0, n = measuredValuesV.size(); i < n; i++) {
            MeasuredValue mv_tmp = (MeasuredValue) measuredValuesV.get(i);
            mv_tmp.setColor(IncrementalColor.getColor(i));
        }
        graphScan.refreshGraphJPanel();
        graphAnalysis.refreshGraphJPanel();
        Enumeration enumNode = measuredPVs_Node.children();
        int i = 0;
        int count = 0;
        while (enumNode.hasMoreElements()) {
            PVTreeNode pvn = (PVTreeNode) enumNode.nextElement();
            if (count != deleteIndex) {
                pvn.setColor(IncrementalColor.getColor(i));
                i++;
            }
            count++;
        }
    }

    /**
	 *  Description of the Method
	 */
    private void cleanUp() {
        MonitoredPV mpv_tmp = scanVariable.getMonitoredPV();
        MonitoredPV.removeMonitoredPV(mpv_tmp);
        mpv_tmp = scanVariable.getMonitoredPV_RB();
        MonitoredPV.removeMonitoredPV(mpv_tmp);
        scanVariable.setChannel(null);
        scanVariable.setChannelRB(null);
        mpv_tmp = scanVariableParameter.getMonitoredPV();
        MonitoredPV.removeMonitoredPV(mpv_tmp);
        mpv_tmp = scanVariableParameter.getMonitoredPV_RB();
        MonitoredPV.removeMonitoredPV(mpv_tmp);
        scanVariableParameter.setChannel(null);
        scanVariableParameter.setChannelRB(null);
        for (int i = 0, n = measuredValuesV.size(); i < n; i++) {
            MeasuredValue mv_tmp = (MeasuredValue) measuredValuesV.get(i);
            mpv_tmp = mv_tmp.getMonitoredPV();
            MonitoredPV.removeMonitoredPV(mpv_tmp);
        }
        for (int i = 0, n = validationValuesV.size(); i < n; i++) {
            MeasuredValue mv_tmp = (MeasuredValue) validationValuesV.get(i);
            mpv_tmp = mv_tmp.getMonitoredPV();
            MonitoredPV.removeMonitoredPV(mpv_tmp);
        }
        measuredValuesV.clear();
        validationValuesV.clear();
        scanController.removeAllValidationValues();
        measuredValuesShowStateV.clear();
        scanPV_ShowState = false;
        scanPV_RB_ShowState = false;
        graphScan.removeAllGraphData();
        graphAnalysis.removeAllGraphData();
        scanPV_Node.removeAllChildren();
        scanPV_RB_Node.removeAllChildren();
        measuredPVs_Node.removeAllChildren();
        validationPVs_Node.removeAllChildren();
        parameterPV_Node.removeAllChildren();
        parameterPV_RB_Node.removeAllChildren();
        ((DefaultTreeModel) pvsSelector.getPVsTreePanel().getJTree().getModel()).reload();
        ((DefaultTreeModel) pvsTreePanelScan.getJTree().getModel()).reload();
        ((DefaultTreeModel) pvsTreePanelAnalysis.getJTree().getModel()).reload();
    }

    /**
	 *  Description of the Method
	 */
    private void cleanMessageTextField() {
        messageTextLocal.setText(null);
        messageTextLocal.setForeground(Color.red);
    }

    /**
	 *  Sets the fontForAll attribute of the ScanDocument2D object
	 *
	 *@param  fnt  The new fontForAll value
	 */
    private void setFontForAll(Font fnt) {
        scanController.setFontForAll(fnt);
        messageTextLocal.setFont(fnt);
        pvsSelector.setAllFonts(fnt);
        pvsTreePanelScan.setAllFonts(fnt);
        pvsTreePanelAnalysis.setAllFonts(fnt);
        parameterPV_Controller.setFontsForAll(fnt);
        globalFont = fnt;
        fontSize_PrefPanel_Spinner.setValue(new Integer(globalFont.getSize()));
        predefinedConfController.setFontsForAll(fnt);
        makeSnapshotButton.setFont(fnt);
        clearSnapshotButton.setFont(fnt);
    }

    /**
	 *  Description of the Class
	 *
	 *@author    shishlo
	 */
    private class ParameterPV_Controller {

        private ScanVariable scanVariableParameter = null;

        private JTextField messageTextParamCntrl = new JTextField(10);

        private JPanel paramPV_Panel = new JPanel();

        private JLabel paramPV_Label = new JLabel(" Param PV Set:");

        private JLabel paramPV_RB_Label = new JLabel(" Param PV RB :");

        private double memValue = 0.;

        private DoubleInputTextField paramPV_ValueText = new DoubleInputTextField(8);

        private DoubleInputTextField paramPV_RB_ValueText = new DoubleInputTextField(8);

        private DecimalFormat valueFormat = new DecimalFormat("###.###");

        private JButton readButton = new JButton("READ FROM EPICS");

        private TitledBorder border = null;

        /**
		 *  Constructor for the ParameterPV_Controller object
		 *
		 *@param  scanVariableParameter_In  Description of the Parameter
		 */
        protected ParameterPV_Controller(ScanVariable scanVariableParameter_In) {
            scanVariableParameter = scanVariableParameter_In;
            paramPV_RB_ValueText.setEditable(false);
            paramPV_ValueText.setDecimalFormat(valueFormat);
            paramPV_RB_ValueText.setDecimalFormat(valueFormat);
            paramPV_ValueText.setHorizontalAlignment(JTextField.CENTER);
            paramPV_RB_ValueText.setHorizontalAlignment(JTextField.CENTER);
            paramPV_ValueText.removeInnerFocusListener();
            paramPV_RB_ValueText.removeInnerFocusListener();
            Border etchedBorder = BorderFactory.createEtchedBorder();
            border = BorderFactory.createTitledBorder(etchedBorder, "PARAMETER PV CONTROL");
            paramPV_Panel.setBorder(border);
            paramPV_Panel.setBackground(paramPV_Panel.getBackground().darker());
            paramPV_ValueText.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    scanVariableParameter.setValue(paramPV_ValueText.getValue());
                }
            });
            readButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    if (scanVariableParameter.getChannel() != null) {
                        paramPV_ValueText.setValue(scanVariableParameter.getValue());
                    } else {
                        paramPV_ValueText.setText(null);
                        paramPV_ValueText.setBackground(Color.white);
                    }
                    if (scanVariableParameter.getChannelRB() != null) {
                        paramPV_RB_ValueText.setValue(scanVariableParameter.getValueRB());
                    } else {
                        paramPV_RB_ValueText.setText(null);
                        paramPV_RB_ValueText.setBackground(Color.white);
                    }
                }
            });
            JPanel tmp_1 = new JPanel();
            tmp_1.setLayout(new GridLayout(2, 2, 1, 1));
            tmp_1.add(paramPV_Label);
            tmp_1.add(paramPV_ValueText);
            tmp_1.add(paramPV_RB_Label);
            tmp_1.add(paramPV_RB_ValueText);
            JPanel tmp_2 = new JPanel();
            tmp_2.setLayout(new BorderLayout());
            tmp_2.add(tmp_1, BorderLayout.CENTER);
            tmp_2.add(readButton, BorderLayout.SOUTH);
            paramPV_Panel.setLayout(new BorderLayout());
            paramPV_Panel.add(tmp_2, BorderLayout.NORTH);
        }

        /**
		 *  Sets the messageTextField attribute of the ParameterPV_Controller object
		 *
		 *@param  messageTextParamCntrl  The new messageTextField value
		 */
        protected void setMessageTextField(JTextField messageTextParamCntrl) {
            this.messageTextParamCntrl = messageTextParamCntrl;
            scanVariableParameter.setMessageTextField(messageTextParamCntrl);
        }

        /**
		 *  Sets the fontsForAll attribute of the ParameterPV_Controller object
		 *
		 *@param  fnt  The new fontsForAll value
		 */
        protected void setFontsForAll(Font fnt) {
            paramPV_Label.setFont(fnt);
            paramPV_RB_Label.setFont(fnt);
            paramPV_ValueText.setFont(fnt);
            paramPV_RB_ValueText.setFont(fnt);
            readButton.setFont(fnt);
            border.setTitleFont(fnt);
            analysisController.setFontsForAll(fnt);
        }

        /**
		 *  Gets the jPanel attribute of the ParameterPV_Controller object
		 *
		 *@return    The jPanel value
		 */
        protected JPanel getJPanel() {
            return paramPV_Panel;
        }

        /**
		 *  Gets the format attribute of the ParameterPV_Controller object
		 *
		 *@return    The format value
		 */
        protected DecimalFormat getFormat() {
            return valueFormat;
        }

        /**
		 *  Gets the valueAsString attribute of the ParameterPV_Controller object
		 *
		 *@return    The valueAsString value
		 */
        protected String getValueAsString() {
            return valueFormat.format(scanVariableParameter.getValue());
        }

        /**
		 *  Gets the value attribute of the ParameterPV_Controller object
		 *
		 *@return    The value value
		 */
        protected double getValue() {
            return scanVariableParameter.getValue();
        }

        /**
		 *  Gets the valueRB attribute of the ParameterPV_Controller object
		 *
		 *@return    The valueRB value
		 */
        protected double getValueRB() {
            return scanVariableParameter.getValueRB();
        }

        /**
		 *  Gets the title attribute of the ParameterPV_Controller object
		 *
		 *@return    The title value
		 */
        protected String getTitle() {
            return border.getTitle();
        }

        /**
		 *  Sets the title attribute of the ParameterPV_Controller object
		 *
		 *@param  title  The new title value
		 */
        protected void setTitle(String title) {
            border.setTitle(title);
        }

        /**
		 *  Sets the channel attribute of the ParameterPV_Controller object
		 *
		 *@param  ch  The new channel value
		 */
        protected void setChannel(Channel ch) {
            scanVariableParameter.setChannel(ch);
            memValue = 0.;
        }

        /**
		 *  Sets the channelRB attribute of the ParameterPV_Controller object
		 *
		 *@param  ch  The new channelRB value
		 */
        protected void setChannelRB(Channel ch) {
            scanVariableParameter.setChannelRB(ch);
        }

        /**
		 *  Gets the channel attribute of the ParameterPV_Controller object
		 *
		 *@return    The channel value
		 */
        protected Channel getChannel() {
            return scanVariableParameter.getChannel();
        }

        /**
		 *  Gets the channelRB attribute of the ParameterPV_Controller object
		 *
		 *@return    The channelRB value
		 */
        protected Channel getChannelRB() {
            return scanVariableParameter.getChannelRB();
        }

        /**
		 *  Description of the Method
		 */
        protected void stopMonitor() {
            scanVariableParameter.getMonitoredPV().stopMonitor();
            scanVariableParameter.getMonitoredPV_RB().stopMonitor();
        }

        /**
		 *  Description of the Method
		 */
        protected void startMonitor() {
            scanVariableParameter.getMonitoredPV().startMonitor();
            scanVariableParameter.getMonitoredPV_RB().startMonitor();
        }
    }
}

/**
 *  Description of the Class
 *
 *@author    shishlo
 */
class DateAndTimeText {

    private SimpleDateFormat dFormat = null;

    private JFormattedTextField dateTimeField = null;

    /**
	 *  Constructor for the DateAndTimeText object
	 */
    public DateAndTimeText() {
        dFormat = new SimpleDateFormat("'Time': MM.dd.yy HH:mm ");
        dateTimeField = new JFormattedTextField(dFormat);
        dateTimeField.setEditable(false);
        Runnable timer = new Runnable() {

            public void run() {
                while (true) {
                    dateTimeField.setValue(new Date());
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        };
        Thread thr = new Thread(timer);
        thr.start();
    }

    /**
	 *  Gets the time attribute of the DateAndTimeText object
	 *
	 *@return    The time value
	 */
    protected String getTime() {
        return dateTimeField.getText();
    }

    /**
	 *  Gets the timeTextField attribute of the DateAndTimeText object
	 *
	 *@return    The timeTextField value
	 */
    protected JFormattedTextField getTimeTextField() {
        return dateTimeField;
    }

    /**
	 *  Gets the newTimeTextField attribute of the DateAndTimeText object
	 *
	 *@return    The newTimeTextField value
	 */
    protected JTextField getNewTimeTextField() {
        JTextField newText = new JTextField();
        newText.setDocument(dateTimeField.getDocument());
        newText.setEditable(false);
        return newText;
    }
}
