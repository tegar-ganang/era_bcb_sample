package gov.sns.apps.quadshaker.utils;

import java.text.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.util.*;
import gov.sns.tools.scan.SecondEdition.UpdatingEventController;
import gov.sns.tools.scan.SecondEdition.WrappedChannel;
import gov.sns.tools.plot.*;
import gov.sns.tools.apputils.*;
import gov.sns.tools.swing.*;
import gov.sns.tools.xml.*;
import gov.sns.tools.pvlogger.*;
import gov.sns.tools.database.*;
import gov.sns.ca.*;
import gov.sns.ca.view.*;
import gov.sns.tools.fit.lsm.Polynomial;

/**
 *  Description of the Class
 *
 *@author     shishlo
 *@created    December 12, 2006
 */
public class ShakerRunController {

    private JPanel shakerRunMainPanel = new JPanel();

    UpdatingEventController updatingController = null;

    private ShakeAnalysis shakeAnalysis = null;

    private QuadsTable quadsTableModel = null;

    private BPMsTable bpmsTableModel = null;

    ScanRunner scanRunner = new ScanRunner();

    private FunctionGraphsJPanel graphPanel = new FunctionGraphsJPanel();

    private BasicGraphData graphXData = new BasicGraphData();

    private BasicGraphData graphYData = new BasicGraphData();

    private TitledBorder quadTableBorder = null;

    private TitledBorder bpmTableBorder = null;

    private JTable quadsTable = new JTable();

    private JTable bpmsTable = new JTable();

    private JButton selectQuadsButton = new JButton("Select All");

    private JButton unSelectQuadsButton = new JButton("Unselect All");

    private JButton selectBPMsButton = new JButton("Select All");

    private JButton unSelectBPMsButton = new JButton("Unselect All");

    private TitledBorder scanParamBorder = null;

    private DoubleInputTextField currChangeTextField = new DoubleInputTextField(8);

    private JLabel currChangeLabel = new JLabel("- % of current change for quads");

    private DoubleInputTextField currTrimTextField = new DoubleInputTextField(8);

    private JLabel currTrimLabel = new JLabel("- max I[A] for trims");

    private DoubleInputTextField nPointsTextField = new DoubleInputTextField(8);

    private JLabel nPointsLabel = new JLabel("- number of points");

    private DoubleInputTextField nAvgTextField = new DoubleInputTextField(8);

    private JLabel nAvgLabel = new JLabel("- number of averaging");

    private TitledBorder validationBorder = null;

    private JTextField validationPVTextField = new JTextField(new ChannelNameDocument(), "", 20);

    private JButton setPVButton = new JButton("Set PV =>");

    private DoubleInputTextField minValidationTextField = new DoubleInputTextField(8);

    private JLabel minValidationLabel = new JLabel("Valid from:");

    private DoubleInputTextField maxValidationTextField = new DoubleInputTextField(8);

    private JLabel maxValidationLabel = new JLabel("  to:");

    private WrappedChannel wrpChValidation = new WrappedChannel();

    private JTextField messageTextLocal = new JTextField();

    private Polynomial polinom_x_fitter = new Polynomial();

    private Polynomial polinom_y_fitter = new Polynomial();

    private JButton makeSnapshotButton = new JButton("Make PV Logger Snapshot");

    private JButton clearSnapshotButton = new JButton("Clear Snapshot");

    private String noSnapshotIdString = "No Snapshot";

    private String snapshotIdString = "Last Snapshot Id: ";

    private JLabel snapshotIdLabel = new JLabel("No Snapshot", JLabel.LEFT);

    private long snapshotId = -1;

    private boolean pvLogged = false;

    private String rootName = "SHAKER_RUN_CONTROLLER_DATA";

    /**
	 *  Constructor for the ShakerRunController object
	 *
	 *@param  updatingController_in  The Parameter
	 */
    public ShakerRunController(UpdatingEventController updatingController_in) {
        Border border = BorderFactory.createEtchedBorder();
        updatingController = updatingController_in;
        shakerRunMainPanel.setLayout(new BorderLayout());
        SimpleChartPopupMenu.addPopupMenuTo(graphPanel);
        graphPanel.setOffScreenImageDrawing(true);
        graphPanel.setName("Results of Quads Shaking");
        graphPanel.setAxisNames("Quad #", "d(X,Y)/dG, [mm/(T/m)]");
        graphPanel.setGraphBackGroundColor(Color.white);
        graphPanel.setLegendButtonVisible(true);
        graphPanel.setLegendBackground(Color.white);
        graphPanel.addGraphData(graphXData);
        graphPanel.addGraphData(graphYData);
        graphPanel.setSmartGL(true);
        graphXData.setDrawLinesOn(false);
        graphXData.setGraphColor(Color.blue);
        graphXData.setGraphPointSize(5);
        graphXData.setImmediateContainerUpdate(false);
        graphXData.setGraphProperty(graphPanel.getLegendKeyString(), "X coefficient");
        graphYData.setDrawLinesOn(false);
        graphYData.setGraphColor(Color.red);
        graphYData.setGraphPointSize(5);
        graphYData.setImmediateContainerUpdate(false);
        graphYData.setGraphProperty(graphPanel.getLegendKeyString(), "Y coefficient");
        JPanel panel_G = new JPanel(new BorderLayout());
        panel_G.setBorder(border);
        panel_G.add(graphPanel, BorderLayout.CENTER);
        shakerRunMainPanel.add(panel_G, BorderLayout.CENTER);
        JPanel tmp_PVLog = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 3));
        tmp_PVLog.add(makeSnapshotButton);
        tmp_PVLog.add(snapshotIdLabel);
        tmp_PVLog.add(clearSnapshotButton);
        tmp_PVLog.setBorder(border);
        panel_G.add(tmp_PVLog, BorderLayout.NORTH);
        quadTableBorder = BorderFactory.createTitledBorder(border, "quads table");
        bpmTableBorder = BorderFactory.createTitledBorder(border, "bpms table");
        scanParamBorder = BorderFactory.createTitledBorder(border, "scan parameters for quad current");
        validationBorder = BorderFactory.createTitledBorder(border, "validation parameters");
        JPanel panel_ScP = new JPanel(new GridLayout(4, 1, 1, 1));
        panel_ScP.setBorder(scanParamBorder);
        JPanel panel_0_ScP = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JPanel panel_1_ScP = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JPanel panel_2_ScP = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JPanel panel_3_ScP = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel_0_ScP.add(currChangeTextField);
        panel_0_ScP.add(currChangeLabel);
        panel_1_ScP.add(currTrimTextField);
        panel_1_ScP.add(currTrimLabel);
        panel_2_ScP.add(nPointsTextField);
        panel_2_ScP.add(nPointsLabel);
        panel_3_ScP.add(nAvgTextField);
        panel_3_ScP.add(nAvgLabel);
        panel_ScP.add(panel_0_ScP);
        panel_ScP.add(panel_1_ScP);
        panel_ScP.add(panel_2_ScP);
        panel_ScP.add(panel_3_ScP);
        JPanel panel_ValidPV = new JPanel(new GridLayout(2, 1, 1, 1));
        panel_ValidPV.setBorder(validationBorder);
        JPanel panel_0_ValidPV = new JPanel(new BorderLayout());
        JPanel panel_1_ValidPV = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel_0_ValidPV.add(setPVButton, BorderLayout.WEST);
        panel_0_ValidPV.add(validationPVTextField, BorderLayout.CENTER);
        panel_1_ValidPV.add(minValidationLabel);
        panel_1_ValidPV.add(minValidationTextField);
        panel_1_ValidPV.add(maxValidationLabel);
        panel_1_ValidPV.add(maxValidationTextField);
        minValidationTextField.setValue(0.);
        maxValidationTextField.setValue(100.);
        panel_ValidPV.add(panel_0_ValidPV);
        panel_ValidPV.add(panel_1_ValidPV);
        JPanel panel_l0 = new JPanel(new BorderLayout());
        panel_l0.add(scanRunner.getPanel(), BorderLayout.NORTH);
        panel_l0.add(panel_ScP, BorderLayout.CENTER);
        panel_l0.add(panel_ValidPV, BorderLayout.SOUTH);
        JPanel panel_l = new JPanel(new BorderLayout());
        JPanel panel_lT = new JPanel(new GridLayout(2, 1, 1, 1));
        panel_l.add(panel_l0, BorderLayout.NORTH);
        panel_l.add(panel_lT, BorderLayout.CENTER);
        shakerRunMainPanel.add(panel_l, BorderLayout.WEST);
        JPanel panel_lT0 = new JPanel(new BorderLayout());
        JPanel panel_lT1 = new JPanel(new BorderLayout());
        JPanel panel_bT0 = new JPanel(new GridLayout(1, 2, 1, 1));
        JPanel panel_bT1 = new JPanel(new GridLayout(1, 2, 1, 1));
        panel_bT0.add(selectQuadsButton);
        panel_bT0.add(unSelectQuadsButton);
        panel_bT1.add(selectBPMsButton);
        panel_bT1.add(unSelectBPMsButton);
        panel_lT0.add(panel_bT0, BorderLayout.NORTH);
        panel_lT1.add(panel_bT1, BorderLayout.NORTH);
        panel_lT0.setBorder(quadTableBorder);
        panel_lT1.setBorder(bpmTableBorder);
        panel_lT.add(panel_lT0);
        panel_lT.add(panel_lT1);
        JScrollPane scrollPane0 = new JScrollPane(quadsTable);
        JScrollPane scrollPane1 = new JScrollPane(bpmsTable);
        panel_lT0.add(scrollPane0, BorderLayout.CENTER);
        panel_lT1.add(scrollPane1, BorderLayout.CENTER);
        quadsTable.setPreferredScrollableViewportSize(new Dimension(1, 1));
        bpmsTable.setPreferredScrollableViewportSize(new Dimension(1, 1));
        ScanQuadsObject scanQuadsObject = new ScanQuadsObject();
        scanRunner.setScanObject(scanQuadsObject);
        currChangeTextField.setValue(5.0);
        currTrimTextField.setValue(20.);
        nPointsTextField.setValue(3.0);
        nAvgTextField.setValue(1);
        validationPVTextField.setForeground(Color.red);
        validationPVTextField.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                validationPVTextField.setForeground(Color.red);
            }
        });
        setPVButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                validationPVTextField.setForeground(Color.blue);
                wrpChValidation.setChannelName(validationPVTextField.getText());
            }
        });
        makeSnapshotButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                RemoteLoggingCenter rL = new RemoteLoggingCenter();
                Date startTime = new Date();
                String comments = startTime.toString();
                comments = comments + " = QuadShaker =";
                snapshotId = rL.takeAndPublishSnapshot("default", comments);
                if (snapshotId > 0) {
                    pvLogged = true;
                    snapshotIdLabel.setText(snapshotIdString + snapshotId + "  ");
                    shakeAnalysis.setPVLoggerId(snapshotId);
                } else {
                    pvLogged = false;
                    snapshotIdLabel.setText("Unsuccessful PV Logging");
                }
            }
        });
        clearSnapshotButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                snapshotId = -1;
                pvLogged = false;
                snapshotIdLabel.setText(noSnapshotIdString);
            }
        });
        selectQuadsButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                for (int i = 0, n = quadsTableModel.getListModel().size(); i < n; i++) {
                    Quad_Element quadElm = (Quad_Element) quadsTableModel.getListModel().get(i);
                    quadElm.setActive(true);
                }
                quadsTableModel.fireTableDataChanged();
            }
        });
        unSelectQuadsButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                for (int i = 0, n = quadsTableModel.getListModel().size(); i < n; i++) {
                    Quad_Element quadElm = (Quad_Element) quadsTableModel.getListModel().get(i);
                    quadElm.setActive(false);
                }
                quadsTableModel.fireTableDataChanged();
            }
        });
        selectBPMsButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                for (int i = 0, n = bpmsTableModel.getListModel().size(); i < n; i++) {
                    BPM_Element bpmElm = (BPM_Element) bpmsTableModel.getListModel().get(i);
                    bpmElm.setActive(true);
                }
                bpmsTableModel.fireTableDataChanged();
            }
        });
        unSelectBPMsButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                for (int i = 0, n = bpmsTableModel.getListModel().size(); i < n; i++) {
                    BPM_Element bpmElm = (BPM_Element) bpmsTableModel.getListModel().get(i);
                    bpmElm.setActive(false);
                }
                bpmsTableModel.fireTableDataChanged();
            }
        });
    }

    /**
	 *  Returns the panel attribute of the ShakerRunController object
	 *
	 *@return    The panel value
	 */
    public JPanel getPanel() {
        return shakerRunMainPanel;
    }

    /**
	 *  Sets the shakeAnalysis attribute of the ShakerRunController object
	 *
	 *@param  shakeAnalysis  The new shakeAnalysis value
	 */
    public void setShakeAnalysis(ShakeAnalysis shakeAnalysis) {
        this.shakeAnalysis = shakeAnalysis;
    }

    /**
	 *  Sets the tableModels attribute of the ShakerRunController object
	 *
	 *@param  quadsTableModel  The new tableModels value
	 *@param  bpmsTableModel   The new tableModels value
	 */
    public void setTableModels(QuadsTable quadsTableModel, BPMsTable bpmsTableModel) {
        this.quadsTableModel = quadsTableModel;
        this.bpmsTableModel = bpmsTableModel;
        quadsTable.setModel(quadsTableModel);
        bpmsTable.setModel(bpmsTableModel);
    }

    /**
	 *  Constructor for the showGraphData object
	 *
	 *@param  quadElm  The feature to be added to the PointToGraph attribute
	 */
    private void addPointToGraph(Quad_Element quadElm) {
        int q_index = quadsTableModel.getListModel().indexOf(quadElm);
        if (q_index >= 0) {
            Vector measV = quadElm.getMeasures();
            if (measV.size() > 0) {
                for (int i = 0, n = bpmsTableModel.getListModel().size(); i < n; i++) {
                    BPM_Element bpmElm = (BPM_Element) bpmsTableModel.getListModel().get(i);
                    if (bpmElm.isActive()) {
                        polinom_x_fitter.clear();
                        polinom_y_fitter.clear();
                        for (int im = 0, nm = measV.size(); im < nm; im++) {
                            QuadMeasure qMeasure = (QuadMeasure) measV.get(im);
                            for (int i_bpm = 0, n_bpm = qMeasure.getSize(); i_bpm < n_bpm; i_bpm++) {
                                BPM_Element bpmElm_inn = qMeasure.getBPM_Element(i_bpm);
                                if (bpmElm_inn == bpmElm) {
                                    double x = qMeasure.getRBField();
                                    double x_bpm = qMeasure.getXPos(i_bpm);
                                    double y_bpm = qMeasure.getYPos(i_bpm);
                                    polinom_x_fitter.addData(x, x_bpm);
                                    polinom_y_fitter.addData(x, y_bpm);
                                }
                            }
                        }
                        polinom_x_fitter.fit();
                        polinom_y_fitter.fit();
                        double coef_x = polinom_x_fitter.getParameter(1);
                        double coef_y = polinom_y_fitter.getParameter(1);
                        double coef_x_err = polinom_x_fitter.getParameterError(1);
                        double coef_y_err = polinom_y_fitter.getParameterError(1);
                        graphXData.addPoint((double) q_index, coef_x, coef_x_err);
                        graphYData.addPoint((double) q_index, coef_y, coef_y_err);
                        quadElm.addSensitivityCoef(bpmElm, coef_x, coef_x_err, coef_y, coef_y_err);
                    }
                }
            }
            graphPanel.refreshGraphJPanel();
        }
    }

    /**
	 *  Description of the Method
	 */
    public void performAnalysis() {
        for (int i = 0, n = quadsTableModel.getListModel().size(); i < n; i++) {
            Quad_Element quadElm = (Quad_Element) quadsTableModel.getListModel().get(i);
            addPointToGraph(quadElm);
        }
    }

    /**
	 *  Clear the graph region of the sub-panel.
	 */
    public void clearGraphData() {
        graphXData.removeAllPoints();
        graphYData.removeAllPoints();
        graphPanel.refreshGraphJPanel();
        messageTextLocal.setText(null);
        messageTextLocal.setForeground(Color.red);
    }

    /**
	 *  Description of the Method
	 */
    public void update() {
    }

    /**
	 *  Sets the fontForAll attribute of the ShakerRunController object
	 *
	 *@param  fnt  The new fontForAll value
	 */
    public void setFontForAll(Font fnt) {
        scanRunner.setFontForAll(fnt);
        quadTableBorder.setTitleFont(fnt);
        bpmTableBorder.setTitleFont(fnt);
        scanParamBorder.setTitleFont(fnt);
        quadsTable.setFont(fnt);
        bpmsTable.setFont(fnt);
        int font_width = quadsTable.getFontMetrics(fnt).charWidth('U');
        int font_height = quadsTable.getFontMetrics(fnt).getHeight();
        quadsTable.setRowHeight((int) 1.1 * font_height);
        bpmsTable.setRowHeight((int) 1.1 * font_height);
        quadsTable.getColumnModel().getColumn(0).setPreferredWidth(30 * font_width);
        quadsTable.getColumnModel().getColumn(0).setMaxWidth(2000);
        quadsTable.getColumnModel().getColumn(0).setMinWidth(20 * font_width);
        quadsTable.getColumnModel().getColumn(1).setMaxWidth(6 * font_width);
        quadsTable.getColumnModel().getColumn(1).setMinWidth(6 * font_width);
        quadsTable.getColumnModel().getColumn(2).setMaxWidth(6 * font_width);
        quadsTable.getColumnModel().getColumn(2).setMinWidth(6 * font_width);
        bpmsTable.getColumnModel().getColumn(0).setPreferredWidth(30 * font_width);
        bpmsTable.getColumnModel().getColumn(0).setMaxWidth(2000);
        bpmsTable.getColumnModel().getColumn(0).setMinWidth(20 * font_width);
        bpmsTable.getColumnModel().getColumn(1).setMaxWidth(6 * font_width);
        bpmsTable.getColumnModel().getColumn(1).setMinWidth(6 * font_width);
        currChangeTextField.setFont(fnt);
        currChangeLabel.setFont(fnt);
        currTrimTextField.setFont(fnt);
        currTrimLabel.setFont(fnt);
        nPointsTextField.setFont(fnt);
        nPointsLabel.setFont(fnt);
        nAvgTextField.setFont(fnt);
        nAvgLabel.setFont(fnt);
        validationBorder.setTitleFont(fnt);
        validationPVTextField.setFont(fnt);
        setPVButton.setFont(fnt);
        minValidationTextField.setFont(fnt);
        minValidationLabel.setFont(fnt);
        maxValidationTextField.setFont(fnt);
        maxValidationLabel.setFont(fnt);
        makeSnapshotButton.setFont(fnt);
        clearSnapshotButton.setFont(fnt);
        snapshotIdLabel.setFont(fnt);
        selectQuadsButton.setFont(fnt);
        unSelectQuadsButton.setFont(fnt);
        selectBPMsButton.setFont(fnt);
        unSelectBPMsButton.setFont(fnt);
    }

    /**
	 *  Sets the lockForGUI attribute of the ShakerRunController object
	 *
	 *@param  lock  The new lockForGUI value
	 */
    private void setLockForGUI(boolean lock) {
        quadsTable.setEnabled(!lock);
        bpmsTable.setEnabled(!lock);
        currChangeTextField.setEnabled(!lock);
        currTrimTextField.setEnabled(!lock);
        nPointsTextField.setEnabled(!lock);
        nAvgTextField.setEnabled(!lock);
    }

    /**
	 *  Sets the validationChannel attribute of the ShakerRunController object
	 *
	 *@param  ca  The new validationChannel value
	 */
    public void setValidationChannel(Channel ca) {
        wrpChValidation.setChannel(ca);
        if (ca != null) {
            validationPVTextField.setText(ca.channelName());
        } else {
            validationPVTextField.setText(null);
        }
    }

    /**
	 *  Returns the messageText attribute of the ShakerRunController object
	 *
	 *@return    The messageText value
	 */
    public JTextField getMessageText() {
        return messageTextLocal;
    }

    /**
	 *  Description of the Method
	 *
	 *@param  da  The Parameter
	 */
    public void dumpData(XmlDataAdaptor da) {
        XmlDataAdaptor shakerRunDA = (XmlDataAdaptor) da.createChild(rootName);
        scanRunner.dumpData(shakerRunDA);
        XmlDataAdaptor shakerRunPramsDA = (XmlDataAdaptor) shakerRunDA.createChild("PARAMS");
        shakerRunPramsDA.setValue("delta_precents", currChangeTextField.getValue());
        shakerRunPramsDA.setValue("max_current_trims", currTrimTextField.getValue());
        shakerRunPramsDA.setValue("number_of_points", (int) nPointsTextField.getValue());
        shakerRunPramsDA.setValue("n_avrg", (int) nAvgTextField.getValue());
        shakerRunPramsDA.setValue("validationPV", validationPVTextField.getText());
        shakerRunPramsDA.setValue("min_valid", minValidationTextField.getValue());
        shakerRunPramsDA.setValue("max_valid", maxValidationTextField.getValue());
        if (snapshotId > 0 && pvLogged == true) {
            shakerRunPramsDA.setValue("pvLoggerId", snapshotId);
        }
        XmlDataAdaptor shakerRunBPMsDA = (XmlDataAdaptor) shakerRunDA.createChild("BPMs_TABLE");
        Enumeration bpm_enum = bpmsTableModel.getListModel().elements();
        while (bpm_enum.hasMoreElements()) {
            BPM_Element bpmElm = (BPM_Element) bpm_enum.nextElement();
            XmlDataAdaptor bpmDA = (XmlDataAdaptor) shakerRunBPMsDA.createChild("BPM");
            bpmElm.dumpData(bpmDA);
        }
        XmlDataAdaptor shakerRunQuadsDA = (XmlDataAdaptor) shakerRunDA.createChild("QUADS_TABLE");
        Enumeration quad_enum = quadsTableModel.getListModel().elements();
        while (quad_enum.hasMoreElements()) {
            Quad_Element quadElm = (Quad_Element) quad_enum.nextElement();
            XmlDataAdaptor quadDA = (XmlDataAdaptor) shakerRunQuadsDA.createChild("QUAD");
            quadElm.dumpData(quadDA);
        }
    }

    /**
	 *  Description of the Method
	 *
	 *@param  da  The Parameter
	 */
    public void readData(XmlDataAdaptor da) {
        XmlDataAdaptor shakerRunDA = (XmlDataAdaptor) da.childAdaptor(rootName);
        scanRunner.readData(shakerRunDA);
        XmlDataAdaptor shakerRunPramsDA = (XmlDataAdaptor) shakerRunDA.childAdaptor("PARAMS");
        currChangeTextField.setValue(shakerRunPramsDA.doubleValue("delta_precents"));
        currTrimTextField.setValue(shakerRunPramsDA.doubleValue("max_current_trims"));
        nPointsTextField.setValue(shakerRunPramsDA.intValue("number_of_points"));
        nAvgTextField.setValue(shakerRunPramsDA.intValue("n_avrg"));
        validationPVTextField.setText(shakerRunPramsDA.stringValue("validationPV"));
        if (validationPVTextField.getText().length() > 0) {
            wrpChValidation.setChannelName(validationPVTextField.getText());
            validationPVTextField.setForeground(Color.blue);
        } else {
            wrpChValidation.setChannel(null);
            validationPVTextField.setForeground(Color.red);
        }
        minValidationTextField.setValue(shakerRunPramsDA.doubleValue("min_valid"));
        maxValidationTextField.setValue(shakerRunPramsDA.doubleValue("max_valid"));
        snapshotId = -1;
        pvLogged = false;
        snapshotIdLabel.setText("No Snapshot");
        if (shakerRunPramsDA.hasAttribute("pvLoggerId")) {
            snapshotId = shakerRunPramsDA.longValue("pvLoggerId");
            snapshotIdLabel.setText("Snapshot Id=" + snapshotId + "  ");
            pvLogged = true;
        }
        XmlDataAdaptor shakerRunBPMsDA = (XmlDataAdaptor) shakerRunDA.childAdaptor("BPMs_TABLE");
        bpmsTableModel.removeAllElements();
        DefaultListModel bpmsListModel = bpmsTableModel.getListModel();
        Iterator bpms_itr = shakerRunBPMsDA.childAdaptorIterator();
        while (bpms_itr.hasNext()) {
            XmlDataAdaptor bpmDA = (XmlDataAdaptor) bpms_itr.next();
            BPM_Element bpmElm = new BPM_Element();
            bpmElm.readData(bpmDA);
            bpmsListModel.addElement(bpmElm);
        }
        XmlDataAdaptor shakerRunQuadsDA = (XmlDataAdaptor) shakerRunDA.childAdaptor("QUADS_TABLE");
        quadsTableModel.removeAllElements();
        DefaultListModel quadsListModel = quadsTableModel.getListModel();
        Iterator quads_itr = shakerRunQuadsDA.childAdaptorIterator();
        while (quads_itr.hasNext()) {
            XmlDataAdaptor quadDA = (XmlDataAdaptor) quads_itr.next();
            Quad_Element quadElm = new Quad_Element();
            quadElm.setBPMsTable(bpmsTableModel);
            quadElm.readData(quadDA);
            quadsListModel.addElement(quadElm);
        }
        performAnalysis();
        shakerRunMainPanel.revalidate();
        shakerRunMainPanel.repaint();
    }

    /**
	 *  Description of the Method
	 *
	 *@return    The Return Value
	 */
    public long pvLoggerSnapshotId() {
        return snapshotId;
    }

    /**
	 *  Description of the Class
	 *
	 *@author     shishlo
	 *@created    December 19, 2006
	 */
    public class ScanQuadsObject extends ScanObject {

        private volatile int current_step = -1;

        private Vector quadMeasuresAllV = new Vector();

        /**
		 *  Constructor for the ScanQuadsObject object
		 */
        public ScanQuadsObject() {
        }

        /**
		 *  Description of the Method
		 */
        public void initScan() {
            setLockForGUI(false);
            internal_init();
        }

        /**
		 *  Description of the Method
		 */
        private void internal_init() {
            current_step = 0;
            quadMeasuresAllV.clear();
            int nPoints = (int) nPointsTextField.getValue();
            if (nPoints < 2) {
                nPoints = 2;
                nPointsTextField.setValue(2.0);
            }
            for (int i = 0, n = quadsTableModel.getListModel().size(); i < n; i++) {
                Quad_Element quadElm = (Quad_Element) quadsTableModel.getListModel().get(i);
                quadElm.clear();
                quadElm.memorizeState();
                if (quadElm.isActive()) {
                    double current = quadElm.getCurrent();
                    double current_min = 0.;
                    double step = 0.;
                    if (quadElm.isItTrim()) {
                        step = currTrimTextField.getValue() / (nPoints - 1);
                        current_min = 0.;
                    } else {
                        double stepRel = currChangeTextField.getValue() / 100.;
                        double delta_val = stepRel * current;
                        step = 2.0 * delta_val / (nPoints - 1);
                        current_min = current - delta_val;
                    }
                    for (int j = 0; j < nPoints; j++) {
                        double val = current_min + j * step;
                        int nAvg = (int) nAvgTextField.getValue();
                        for (int k = 0; k < nAvg; k++) {
                            QuadMeasure quadMeasure = new QuadMeasure(bpmsTableModel);
                            quadMeasure.setCurrent(val);
                            quadElm.addMeasure(quadMeasure);
                            quadMeasuresAllV.add(quadMeasure);
                        }
                    }
                }
            }
            clearGraphData();
        }

        /**
		 *  Description of the Method
		 *
		 *@return    The Return Value
		 */
        public boolean nextStepExists() {
            if (current_step >= quadMeasuresAllV.size()) {
                return false;
            }
            return true;
        }

        /**
		 *  Description of the Method
		 */
        public void startScan() {
            if (current_step == 0) {
                internal_init();
            }
            setLockForGUI(true);
        }

        /**
		 *  Description of the Method
		 */
        public void makeStep() {
            if (wrpChValidation.getChannel() != null) {
                wrpChValidation.setValueChanged(false);
            }
            QuadMeasure quadMeasure = (QuadMeasure) quadMeasuresAllV.get(current_step);
            quadMeasure.setCurrentToEPICS();
            scanRunner.setMessage("Quad: " + quadMeasure.getQuad_Element().getName());
        }

        /**
		 *  Description of the Method
		 *
		 *@return    The Return Value
		 */
        public boolean validateStep() {
            boolean valid_res = false;
            if (wrpChValidation.getChannel() != null) {
                double val_min = minValidationTextField.getValue();
                double val_max = maxValidationTextField.getValue();
                double val = wrpChValidation.getValue();
                if (val >= val_min && val <= val_max && wrpChValidation.valueChanged()) {
                    valid_res = true;
                }
            }
            return valid_res;
        }

        /**
		 *  Description of the Method
		 */
        public void accountStep() {
            QuadMeasure quadMeasure = (QuadMeasure) quadMeasuresAllV.get(current_step);
            quadMeasure.measure();
            if (current_step == (quadMeasuresAllV.size() - 1)) {
                quadMeasure.getQuad_Element().restoreState();
                addPointToGraph(quadMeasure.getQuad_Element());
            } else {
                QuadMeasure quadMeasure_next = (QuadMeasure) quadMeasuresAllV.get(current_step + 1);
                if (quadMeasure_next.getQuad_Element() != quadMeasure.getQuad_Element()) {
                    quadMeasure.getQuad_Element().restoreState();
                    addPointToGraph(quadMeasure.getQuad_Element());
                }
            }
            current_step = current_step + 1;
        }

        /**
		 *  Description of the Method
		 */
        public void restoreInitialState() {
            current_step = 0;
        }

        /**
		 *  Description of the Method
		 */
        public void errorHappened() {
            QuadMeasure quadMeasure = (QuadMeasure) quadMeasuresAllV.get(current_step);
            quadMeasure.getQuad_Element().restoreState();
        }

        /**
		 *  Description of the Method
		 */
        public void scanFinished() {
            if (current_step == quadMeasuresAllV.size()) {
                setLockForGUI(false);
            } else {
                QuadMeasure quadMeasure = (QuadMeasure) quadMeasuresAllV.get(current_step);
                quadMeasure.getQuad_Element().restoreState();
            }
        }

        /**
		 *  Returns the progress attribute of the ScanQuadsObject object
		 *
		 *@return    The progress value
		 */
        public int getProgress() {
            int res = 0;
            if (quadMeasuresAllV.size() > 1) {
                res = (100 * (current_step)) / (quadMeasuresAllV.size() - 1);
            }
            return res;
        }
    }
}
