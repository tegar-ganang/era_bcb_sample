package gov.sns.tools.scan;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.border.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import gov.sns.tools.apputils.PVSelection.PVSelector;
import gov.sns.tools.apputils.AbsolutePathFinder;
import gov.sns.tools.plot.*;
import gov.sns.ca.*;
import gov.sns.xal.smf.*;
import gov.sns.xal.smf.data.*;

public class GeneralOneDimScan extends JPanel {

    private static int nPanels = 4;

    private static int MAIN_PANEL = 0;

    private static int PV_SELECT_PANEL = 1;

    private static int ANALISYS_PANEL = 2;

    private static int INFO_MESSAGE_PANEL = 3;

    private JPanel[] jpanels = new JPanel[nPanels];

    private int iPanelActive = 0;

    private int iPanelOld = 0;

    private JButton mSetPVsButton = new JButton("SET OR SEE PVs' NAMES");

    private JButton mClearDataButton = new JButton("CLEAR DATA");

    private JButton mReadDataButton = new JButton("READ DATA");

    private JButton mSaveDataButton = new JButton("SAVE DATA");

    private JButton mAnalysisButton = new JButton("GO TO ANALYSIS");

    private FunctionGraphsJPanel graphPanel = new FunctionGraphsJPanel();

    private IndependentValueRange rangePanel = new IndependentValueRange("Scanner");

    private AveragingController avgControl = new AveragingController(5, 0.2);

    private ValuatorLimitsManager thresholdControl = new ValuatorLimitsManager(0.0, 1000.);

    private IndependentValue independValue = new IndependentValue();

    private MeasuredValue measuredValue = new MeasuredValue();

    private MeasuredValues measuredVals = new MeasuredValues();

    private Valuator valuatorMain = new Valuator();

    private Valuator valuatorValue = new Valuator();

    private JRadioButton mPV_ShowButton = new JRadioButton("Set PV", true);

    private JRadioButton mPV_RB_ShowButton = new JRadioButton("Read Back PV", true);

    private JButton mReadPVsButton = new JButton("READ PVs' VALUES");

    private JLabel mScannedPV_Label = new JLabel("Scanned PV : ", JLabel.LEFT);

    private JLabel mScannedPV_RB_Label = new JLabel("Read Back PV : ", JLabel.LEFT);

    private JLabel mMeasured_Label = new JLabel("Measured PV : ", JLabel.LEFT);

    private JLabel mValidationPV_Label = new JLabel("Validation PV : ", JLabel.LEFT);

    private JTextField mScanPV_Name_Text = new JTextField(10);

    private JTextField mScanPV_RB_Name_Text = new JTextField(10);

    private JTextField mMeasurePV_Name_Text = new JTextField(10);

    private JTextField mValidationPV_Name_Text = new JTextField(10);

    private JLabel mScannedPV_Val_Label = new JLabel("", JLabel.CENTER);

    private JLabel mScannedPV_RB_Val_Label = new JLabel("", JLabel.CENTER);

    private JLabel mMeasured_Val_Label = new JLabel("", JLabel.CENTER);

    private JLabel mValidationPV_Val_Label = new JLabel("", JLabel.CENTER);

    private DecimalFormat mScannedPVFormat = new DecimalFormat("0.###E0");

    private DecimalFormat mMeasuredPVFormat = new DecimalFormat("0.###E0");

    private DecimalFormat mValidationPVFormat = new DecimalFormat("0.###E0");

    private PVSelector pvSelector = new PVSelector("ACCELERATOR TREE");

    private JRadioButton spScanPV_Button = new JRadioButton("PV for Scanning (X axis)", true);

    private JRadioButton spScanPV_RB_Button = new JRadioButton("PV Read Back for Scanning (X axis)", false);

    private JRadioButton spMeasurePV_Button = new JRadioButton("PV for Measuring (Y axis)", false);

    private JRadioButton spValidationPV_Button = new JRadioButton("PV for Validation", false);

    private JTextField spScanPV_Text = new JTextField(40);

    private JTextField spScanPV_RB_Text = new JTextField(40);

    private JTextField spMeasurePV_Text = new JTextField(40);

    private JTextField spValidationPV_Text = new JTextField(40);

    private JButton spChooseAcceleratorButton = new JButton("SET NEW ACCELERATOR");

    private JButton spCloseSelectionButton = new JButton("SET PVs AND CLOSE");

    private JButton spEscapeButton = new JButton("ESCAPE");

    private String xalRootName = "xaldev";

    private String relNameOfXALFile = "xal_xmls/main_source.xml";

    private File accelDataFile = null;

    private BasicGraphData fitGraphData = new CubicSplineGraphData();

    private FunctionGraphsJPanel analysisGraphPanel = new FunctionGraphsJPanel();

    private FunctionGraphsJPanel.ClickedPoint clickedPointAtAnalPanel = null;

    private JRadioButton apPV_ShowButton = new JRadioButton("Set PV", true);

    private JRadioButton apPV_RB_ShowButton = new JRadioButton("Read Back PV", true);

    private JButton apReturnToScanButton = new JButton("RETURN TO SCAN");

    private JButton apRemoveSelectedButton = new JButton("REMOVE SELECTED GRAPH");

    private JButton apStartFittingButton = new JButton("START FITTING");

    private JButton apClearFittingButton = new JButton("CLEAR FITTING RESULTS");

    private JLabel apOrderOfFitLabel = new JLabel("Order of Fitting : ", JLabel.RIGHT);

    private JTextField apFittingFormula_Text = new JTextField(40);

    private JSpinner apFitOrder_Spinner = new JSpinner(new SpinnerNumberModel(0, 0, 3, 1));

    private DecimalFormat apFormulaNumbersFormat = new DecimalFormat("0.###E0");

    private int infoReturnIndex = 0;

    private JTextArea infoTextArea = new JTextArea(10, 40);

    private JButton infoOkButton = new JButton("OK");

    private static Border etchedBorder = BorderFactory.createEtchedBorder();

    private File graphDataFile = null;

    public GeneralOneDimScan() {
        setLayout(new BorderLayout());
        for (int i = 0; i < nPanels; i++) {
            jpanels[i] = new JPanel();
        }
        makeMainPanel();
        makePV_SelectionPanel();
        makeInfoPanel();
        makeAnalysisPanel();
        activatePanel(MAIN_PANEL);
    }

    private void activatePanel(int indexOfPanel) {
        if (indexOfPanel < 0 || indexOfPanel >= nPanels) return;
        iPanelOld = iPanelActive;
        iPanelActive = indexOfPanel;
        removeAll();
        add(jpanels[indexOfPanel], BorderLayout.CENTER);
        validate();
        repaint();
    }

    private void makeMainPanel() {
        JPanel mp = jpanels[MAIN_PANEL];
        mp.setLayout(new BorderLayout());
        graphPanel.setChooseModeButtonVisible(false);
        graphPanel.setHorLinesButtonVisible(false);
        graphPanel.setVerLinesButtonVisible(false);
        Font graphFont = new Font(getFont().getFamily(), Font.BOLD, 10);
        graphPanel.setAxisNameFontX(graphFont);
        graphPanel.setAxisNameFontY(graphFont);
        graphPanel.setNumberFont(graphFont);
        GridLimits gridLimits = graphPanel.getNewGridLimits();
        gridLimits.setNumberFormatX(new DecimalFormat("####.###"));
        gridLimits.setNumberFormatY(new DecimalFormat("####.###"));
        gridLimits.setLimitsAndTicksX(0., 100., 20., 5);
        gridLimits.setLimitsAndTicksY(0., 100., 20., 5);
        graphPanel.setExternalGL(gridLimits);
        graphPanel.getExternalGL().setYminOn(false);
        graphPanel.getExternalGL().setYmaxOn(false);
        graphPanel.getExternalGL().setXminOn(false);
        graphPanel.getExternalGL().setXmaxOn(false);
        graphPanel.setAxisNameX("Scanned PV");
        graphPanel.setAxisNameY("Measured PV");
        graphPanel.setOffScreenImageDrawing(true);
        rangePanel.addButtonPanel();
        rangePanel.registerIndependentValue(independValue);
        rangePanel.setCurrentValueRB(0.0);
        rangePanel.setCurrentValue(0.0);
        rangePanel.setLowLimit(0.0);
        rangePanel.setUppLimit(100.0);
        rangePanel.setStep(5.0);
        rangePanel.registerMeasurer(measuredVals);
        rangePanel.dimensionText.setText(" [a.u.]");
        measuredVals.addMeasuredValueInstance(measuredValue);
        valuatorValue.setLimitsManager(thresholdControl);
        valuatorMain.addExternalValuator(valuatorValue);
        measuredVals.setValuator(valuatorMain);
        measuredVals.setAvrgCntrl(avgControl);
        mSetPVsButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                activatePanel(PV_SELECT_PANEL);
            }
        });
        mClearDataButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                graphPanel.removeAllGraphData();
                measuredValue.removeAllDataContainers();
            }
        });
        mReadDataButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                readDataFromDisk();
            }
        });
        mSaveDataButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                saveDataToDisk();
            }
        });
        mAnalysisButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateAnalysisGraphPanel();
                activatePanel(ANALISYS_PANEL);
            }
        });
        measuredVals.addNewSetOfMeasurementsListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateGraphPanel();
            }
        });
        mPV_ShowButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateGraphPanel();
            }
        });
        mPV_RB_ShowButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateGraphPanel();
            }
        });
        mScanPV_Name_Text.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                spScanPV_Text.setText(null);
                spScanPV_Text.setText(mScanPV_Name_Text.getText());
                Channel ch = null;
                if (mScanPV_Name_Text.getText().length() > 0) {
                    ch = ChannelFactory.defaultFactory().getChannel(mScanPV_Name_Text.getText());
                } else {
                    showInfoPanel("PV for scanning should be specified (X axis).", MAIN_PANEL);
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }
                try {
                    if (ch != null && !ch.writeAccess()) {
                        String msg = "PV for scanning:" + System.getProperty("line.separator");
                        msg = msg + "PV (" + ch.channelName() + ") does not have write access. Set the new PV name.";
                        Toolkit.getDefaultToolkit().beep();
                        showInfoPanel(msg, MAIN_PANEL);
                        return;
                    }
                    if (spScanPV_Text.getText().length() > 0) {
                        independValue.setChannelName(spScanPV_Text.getText());
                    } else {
                        independValue.setChannelName(null);
                    }
                } catch (ConnectionException expt) {
                    String msg = "Cannot connect to PVs:" + System.getProperty("line.separator");
                    msg = msg + mScanPV_Name_Text.getText() + System.getProperty("line.separator");
                    msg = msg + "Please, check EPICS or names of PVs.";
                    Toolkit.getDefaultToolkit().beep();
                    showInfoPanel(msg, MAIN_PANEL);
                }
            }
        });
        mScanPV_RB_Name_Text.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                spScanPV_RB_Text.setText(null);
                spScanPV_RB_Text.setText(mScanPV_RB_Name_Text.getText());
                Channel ch = null;
                if (mScanPV_RB_Name_Text.getText().length() > 0) {
                    ch = ChannelFactory.defaultFactory().getChannel(mScanPV_RB_Name_Text.getText());
                }
                try {
                    if (ch != null && !ch.readAccess()) {
                        String msg = "Read Back PV for scanning:" + System.getProperty("line.separator");
                        msg = msg + "PV (" + ch.channelName() + ") does not have read access. Set the new PV name.";
                        Toolkit.getDefaultToolkit().beep();
                        showInfoPanel(msg, MAIN_PANEL);
                        return;
                    }
                    if (spScanPV_RB_Text.getText().length() > 0) {
                        independValue.setChannelNameRB(spScanPV_RB_Text.getText());
                    } else {
                        independValue.setChannelNameRB(null);
                    }
                } catch (ConnectionException expt) {
                    String msg = "Cannot connect to PVs:" + System.getProperty("line.separator");
                    msg = msg + mScanPV_RB_Name_Text.getText() + System.getProperty("line.separator");
                    msg = msg + "Please, check EPICS or names of PVs.";
                    Toolkit.getDefaultToolkit().beep();
                    showInfoPanel(msg, MAIN_PANEL);
                }
            }
        });
        mMeasurePV_Name_Text.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                spMeasurePV_Text.setText(null);
                spMeasurePV_Text.setText(mMeasurePV_Name_Text.getText());
                Channel ch = null;
                if (mMeasurePV_Name_Text.getText().length() > 0) {
                    ch = ChannelFactory.defaultFactory().getChannel(mMeasurePV_Name_Text.getText());
                } else {
                    showInfoPanel("PV for measuring should be specified (Y axis).", MAIN_PANEL);
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }
                try {
                    if (ch != null && !ch.readAccess()) {
                        String msg = "PV for scanning:" + System.getProperty("line.separator");
                        msg = msg + "PV (" + ch.channelName() + ") does not have read access. Set the new PV name.";
                        Toolkit.getDefaultToolkit().beep();
                        showInfoPanel(msg, MAIN_PANEL);
                        return;
                    }
                    if (spMeasurePV_Text.getText().length() > 0) {
                        measuredValue.setChannelName(spMeasurePV_Text.getText());
                    } else {
                        measuredValue.setChannelName(null);
                    }
                } catch (ConnectionException expt) {
                    String msg = "Cannot connect to PVs:" + System.getProperty("line.separator");
                    msg = msg + mMeasurePV_Name_Text.getText() + System.getProperty("line.separator");
                    msg = msg + "Please, check EPICS or names of PVs.";
                    Toolkit.getDefaultToolkit().beep();
                    showInfoPanel(msg, MAIN_PANEL);
                }
            }
        });
        mValidationPV_Name_Text.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                spValidationPV_Text.setText(null);
                spValidationPV_Text.setText(mValidationPV_Name_Text.getText());
                Channel ch = null;
                if (mValidationPV_Name_Text.getText().length() > 0) {
                    ch = ChannelFactory.defaultFactory().getChannel(mValidationPV_Name_Text.getText());
                }
                try {
                    if (ch != null && !ch.readAccess()) {
                        String msg = "PV for scanning:" + System.getProperty("line.separator");
                        msg = msg + "PV (" + ch.channelName() + ") does not have read access. Set the new PV name.";
                        Toolkit.getDefaultToolkit().beep();
                        showInfoPanel(msg, MAIN_PANEL);
                        return;
                    }
                    if (spValidationPV_Text.getText().length() > 0) {
                        valuatorValue.setChannelName(spValidationPV_Text.getText());
                    } else {
                        valuatorValue.setChannelName(null);
                    }
                } catch (ConnectionException expt) {
                    String msg = "Cannot connect to PVs:" + System.getProperty("line.separator");
                    msg = msg + mValidationPV_Name_Text.getText() + System.getProperty("line.separator");
                    msg = msg + "Please, check EPICS or names of PVs.";
                    Toolkit.getDefaultToolkit().beep();
                    showInfoPanel(msg, MAIN_PANEL);
                }
            }
        });
        mReadPVsButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updatePV_ValuesOnMainPanel();
            }
        });
        Font diplayControlPanelFont = new Font(getFont().getFamily(), Font.BOLD, 10);
        mClearDataButton.setFont(diplayControlPanelFont);
        mAnalysisButton.setFont(diplayControlPanelFont);
        mSaveDataButton.setFont(diplayControlPanelFont);
        mReadDataButton.setFont(diplayControlPanelFont);
        mPV_ShowButton.setFont(diplayControlPanelFont);
        mPV_RB_ShowButton.setFont(diplayControlPanelFont);
        mReadPVsButton.setFont(diplayControlPanelFont);
        mScannedPV_Label.setFont(diplayControlPanelFont);
        mScannedPV_RB_Label.setFont(diplayControlPanelFont);
        mMeasured_Label.setFont(diplayControlPanelFont);
        mValidationPV_Label.setFont(diplayControlPanelFont);
        mScannedPV_Val_Label.setFont(diplayControlPanelFont);
        mScannedPV_RB_Val_Label.setFont(diplayControlPanelFont);
        mMeasured_Val_Label.setFont(diplayControlPanelFont);
        mValidationPV_Val_Label.setFont(diplayControlPanelFont);
        mScanPV_Name_Text.setFont(diplayControlPanelFont);
        mScanPV_RB_Name_Text.setFont(diplayControlPanelFont);
        mMeasurePV_Name_Text.setFont(diplayControlPanelFont);
        mValidationPV_Name_Text.setFont(diplayControlPanelFont);
        EmptyBorder epmtyBorder = new EmptyBorder(0, 0, 0, 0);
        JPanel tmp_1 = new JPanel();
        tmp_1.setLayout(new BorderLayout());
        tmp_1.setBorder(BorderFactory.createTitledBorder(etchedBorder, "X-Y plot data"));
        tmp_1.setBackground(getBackground().darker());
        tmp_1.add(graphPanel, BorderLayout.CENTER);
        JPanel tmp_2 = new JPanel();
        tmp_2.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tmp_2.add(mClearDataButton);
        tmp_2.add(mReadDataButton);
        tmp_2.add(mSaveDataButton);
        tmp_2.add(mAnalysisButton);
        JPanel tmp_3 = new JPanel();
        tmp_3.setLayout(new BorderLayout());
        tmp_3.add(tmp_1, BorderLayout.CENTER);
        tmp_3.add(tmp_2, BorderLayout.SOUTH);
        JPanel tmp_4 = new JPanel();
        tmp_4.setLayout(new BorderLayout());
        tmp_4.add(mSetPVsButton, BorderLayout.NORTH);
        tmp_4.add(rangePanel, BorderLayout.CENTER);
        tmp_4.add(avgControl.getJPanel(), BorderLayout.SOUTH);
        JPanel tmp_5 = new JPanel();
        tmp_5.setLayout(new BorderLayout());
        tmp_5.add(tmp_4, BorderLayout.NORTH);
        tmp_5.add(thresholdControl.getJPanel(), BorderLayout.CENTER);
        JPanel tmp_6 = new JPanel();
        tmp_6.setLayout(new BorderLayout());
        tmp_6.setBorder(BorderFactory.createTitledBorder(etchedBorder, "Scan Control"));
        tmp_6.setBackground(getBackground().darker());
        tmp_6.add(tmp_5, BorderLayout.NORTH);
        JPanel tmp_7 = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        tmp_7.setLayout(gridbag);
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(1, 0, 1, 0);
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0.33;
        c.weighty = 1.0;
        gridbag.setConstraints(mScannedPV_Label, c);
        tmp_7.add(mScannedPV_Label);
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 0.33;
        c.weighty = 1.0;
        gridbag.setConstraints(mScanPV_Name_Text, c);
        tmp_7.add(mScanPV_Name_Text);
        c.gridx = 2;
        c.gridy = 1;
        c.weightx = 0.33;
        c.weighty = 1.0;
        gridbag.setConstraints(mScannedPV_Val_Label, c);
        tmp_7.add(mScannedPV_Val_Label);
        c.gridx = 0;
        c.gridy = 2;
        c.weightx = 0.33;
        c.weighty = 1.0;
        gridbag.setConstraints(mScannedPV_RB_Label, c);
        tmp_7.add(mScannedPV_RB_Label);
        c.gridx = 1;
        c.gridy = 2;
        c.weightx = 0.33;
        c.weighty = 1.0;
        gridbag.setConstraints(mScanPV_RB_Name_Text, c);
        tmp_7.add(mScanPV_RB_Name_Text);
        c.gridx = 2;
        c.gridy = 2;
        c.weightx = 0.33;
        c.weighty = 1.0;
        gridbag.setConstraints(mScannedPV_RB_Val_Label, c);
        tmp_7.add(mScannedPV_RB_Val_Label);
        c.gridx = 0;
        c.gridy = 3;
        c.weightx = 0.33;
        c.weighty = 1.0;
        gridbag.setConstraints(mMeasured_Label, c);
        tmp_7.add(mMeasured_Label);
        c.gridx = 1;
        c.gridy = 3;
        c.weightx = 0.33;
        c.weighty = 1.0;
        gridbag.setConstraints(mMeasurePV_Name_Text, c);
        tmp_7.add(mMeasurePV_Name_Text);
        c.gridx = 2;
        c.gridy = 3;
        c.weightx = 0.33;
        c.weighty = 1.0;
        gridbag.setConstraints(mMeasured_Val_Label, c);
        tmp_7.add(mMeasured_Val_Label);
        c.gridx = 0;
        c.gridy = 4;
        c.weightx = 0.33;
        c.weighty = 1.0;
        gridbag.setConstraints(mValidationPV_Label, c);
        tmp_7.add(mValidationPV_Label);
        c.gridx = 1;
        c.gridy = 4;
        c.weightx = 0.33;
        c.weighty = 1.0;
        gridbag.setConstraints(mValidationPV_Name_Text, c);
        tmp_7.add(mValidationPV_Name_Text);
        c.gridx = 2;
        c.gridy = 4;
        c.weightx = 0.33;
        c.weighty = 1.0;
        gridbag.setConstraints(mValidationPV_Val_Label, c);
        tmp_7.add(mValidationPV_Val_Label);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridx = 0;
        c.gridy = 5;
        c.weightx = 1.0;
        c.weighty = 1.0;
        gridbag.setConstraints(mReadPVsButton, c);
        tmp_7.add(mReadPVsButton);
        JPanel tmp_8 = new JPanel();
        tmp_8.setLayout(new GridLayout(0, 2));
        tmp_8.add(mPV_ShowButton);
        tmp_8.add(mPV_RB_ShowButton);
        JPanel tmp_9 = new JPanel();
        tmp_9.setLayout(new BorderLayout());
        tmp_9.setBorder(BorderFactory.createTitledBorder(etchedBorder, "Display Control"));
        tmp_9.setBackground(getBackground().darker());
        tmp_9.add(tmp_8, BorderLayout.CENTER);
        tmp_9.add(tmp_7, BorderLayout.SOUTH);
        JPanel tmp_10 = new JPanel();
        tmp_10.setLayout(new BorderLayout());
        tmp_10.add(tmp_6, BorderLayout.NORTH);
        tmp_10.add(tmp_9, BorderLayout.SOUTH);
        JPanel tmp_11 = new JPanel();
        tmp_11.setLayout(new BorderLayout());
        tmp_11.add(tmp_10, BorderLayout.NORTH);
        mp.add(tmp_11, BorderLayout.WEST);
        mp.add(tmp_3, BorderLayout.CENTER);
    }

    private void makeAnalysisPanel() {
        JPanel ap = jpanels[ANALISYS_PANEL];
        ap.setLayout(new BorderLayout());
        analysisGraphPanel.setChooseModeButtonVisible(true);
        analysisGraphPanel.setHorLinesButtonVisible(false);
        analysisGraphPanel.setVerLinesButtonVisible(false);
        Font graphFont = new Font(getFont().getFamily(), Font.BOLD, 10);
        analysisGraphPanel.setAxisNameFontX(graphFont);
        analysisGraphPanel.setAxisNameFontY(graphFont);
        analysisGraphPanel.setNumberFont(graphFont);
        GridLimits gridLimits = graphPanel.getNewGridLimits();
        gridLimits.setNumberFormatX(new DecimalFormat("####.###"));
        gridLimits.setNumberFormatY(new DecimalFormat("####.###"));
        gridLimits.setLimitsAndTicksX(0., 100., 20., 5);
        gridLimits.setLimitsAndTicksY(0., 100., 20., 5);
        analysisGraphPanel.setExternalGL(gridLimits);
        analysisGraphPanel.getExternalGL().setYminOn(false);
        analysisGraphPanel.getExternalGL().setYmaxOn(false);
        analysisGraphPanel.getExternalGL().setXminOn(false);
        analysisGraphPanel.getExternalGL().setXmaxOn(false);
        analysisGraphPanel.setAxisNameX("Scanned PV");
        analysisGraphPanel.setAxisNameY("Measured PV");
        analysisGraphPanel.setOffScreenImageDrawing(true);
        clickedPointAtAnalPanel = analysisGraphPanel.getClickedPointObject();
        fitGraphData.setGraphColor(Color.blue);
        fitGraphData.removeAllPoints();
        fitGraphData.setDrawPointsOn(false);
        apFitOrder_Spinner.setAlignmentX(JSpinner.CENTER_ALIGNMENT);
        apPV_ShowButton.setFont(graphFont);
        apPV_RB_ShowButton.setFont(graphFont);
        apReturnToScanButton.setFont(graphFont);
        apRemoveSelectedButton.setFont(graphFont);
        apStartFittingButton.setFont(graphFont);
        apClearFittingButton.setFont(graphFont);
        apOrderOfFitLabel.setFont(graphFont);
        apFitOrder_Spinner.setFont(graphFont);
        apFittingFormula_Text.setFont(new Font(getFont().getFamily(), Font.PLAIN, 12));
        apPV_ShowButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateAnalysisGraphPanel();
            }
        });
        apPV_RB_ShowButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateAnalysisGraphPanel();
            }
        });
        apReturnToScanButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateGraphPanel();
                activatePanel(MAIN_PANEL);
            }
        });
        apRemoveSelectedButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                FunctionGraphsJPanel gp = analysisGraphPanel;
                Integer Ind = gp.getGraphChosenIndex();
                if (Ind != null && Ind.intValue() >= 0) {
                    int ind = Ind.intValue();
                    BasicGraphData gd = gp.getInstanceOfGraphData(ind);
                    Vector<BasicGraphData> gdV = measuredValue.getDataContainers();
                    ind = gdV.indexOf(gd);
                    measuredValue.removeDataContainer(ind);
                    gdV = measuredValue.getDataContainersRB();
                    ind = gdV.indexOf(gd);
                    measuredValue.removeDataContainerRB(ind);
                    if (gd.equals(fitGraphData)) {
                        fitGraphData.removeAllPoints();
                    }
                    updateAnalysisGraphPanel();
                    return;
                }
                Toolkit.getDefaultToolkit().beep();
            }
        });
        apStartFittingButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                apFittingFormula_Text.setText(null);
                fitGraphData.removeAllPoints();
                FunctionGraphsJPanel gp = analysisGraphPanel;
                Integer Ind = gp.getGraphChosenIndex();
                if (Ind != null && Ind.intValue() >= 0) {
                    int ind = Ind.intValue();
                    BasicGraphData gd = gp.getInstanceOfGraphData(ind);
                    double xMin = gp.getCurrentMinX();
                    double xMax = gp.getCurrentMaxX();
                    double yMin = gp.getCurrentMinY();
                    double yMax = gp.getCurrentMaxY();
                    int order = ((Integer) apFitOrder_Spinner.getValue()).intValue();
                    gp.removeGraphData(fitGraphData);
                    Color fitColor = fitGraphData.getGraphColor();
                    GraphDataOperations.polynomialFit(gd, fitGraphData, xMin, xMax, order, 0);
                    fitGraphData.setGraphColor(fitColor);
                    gp.addGraphData(fitGraphData);
                    double[][] coeff = GraphDataOperations.polynomialFit(gd, xMin, xMax, order);
                    if (coeff != null && coeff[0].length > 0) {
                        String formula = "y = ";
                        for (int i = 0, n = coeff[0].length; i < n; i++) {
                            formula = formula + "(" + apFormulaNumbersFormat.format(coeff[0][i]) + ")*X^" + i;
                            if (i != (n - 1)) {
                                formula = formula + " + ";
                            }
                        }
                        apFittingFormula_Text.setText(formula);
                    }
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
                updateAnalysisGraphPanel();
            }
        });
        apClearFittingButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                apFittingFormula_Text.setText(null);
                fitGraphData.removeAllPoints();
                updateAnalysisGraphPanel();
            }
        });
        JPanel tmp_1 = new JPanel();
        tmp_1.setLayout(new GridLayout(0, 2));
        tmp_1.add(apPV_ShowButton);
        tmp_1.add(apPV_RB_ShowButton);
        JPanel tmp_2 = new JPanel();
        tmp_2.setLayout(new GridLayout(0, 2));
        tmp_2.add(apOrderOfFitLabel);
        tmp_2.add(apFitOrder_Spinner);
        JPanel tmp_3 = new JPanel();
        tmp_3.setLayout(new BorderLayout());
        tmp_3.add(apStartFittingButton, BorderLayout.NORTH);
        tmp_3.add(tmp_2, BorderLayout.CENTER);
        tmp_3.add(apClearFittingButton, BorderLayout.SOUTH);
        JPanel tmp_4 = new JPanel();
        tmp_4.setLayout(new BorderLayout());
        tmp_4.setBorder(BorderFactory.createTitledBorder(etchedBorder, "Analysis Control"));
        tmp_4.setBackground(getBackground().darker());
        tmp_4.add(tmp_1, BorderLayout.NORTH);
        tmp_4.add(tmp_3, BorderLayout.SOUTH);
        JPanel tmp_5_0 = new JPanel();
        tmp_5_0.setLayout(new GridLayout(0, 2));
        tmp_5_0.setBorder(BorderFactory.createTitledBorder(etchedBorder, "Graph Data Reading"));
        tmp_5_0.setBackground(getBackground().darker());
        tmp_5_0.add(clickedPointAtAnalPanel.xValueLabel);
        tmp_5_0.add(clickedPointAtAnalPanel.xValueText);
        tmp_5_0.add(clickedPointAtAnalPanel.yValueLabel);
        tmp_5_0.add(clickedPointAtAnalPanel.yValueText);
        JPanel tmp_5_1 = new JPanel();
        tmp_5_1.setLayout(new BorderLayout());
        ;
        tmp_5_1.add(tmp_4, BorderLayout.NORTH);
        tmp_5_1.add(tmp_5_0, BorderLayout.SOUTH);
        JPanel tmp_5 = new JPanel();
        tmp_5.setLayout(new BorderLayout());
        ;
        tmp_5.add(tmp_5_1, BorderLayout.NORTH);
        JPanel tmp_6 = new JPanel();
        tmp_6.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tmp_6.add(apRemoveSelectedButton);
        tmp_6.add(apReturnToScanButton);
        JPanel tmp_7 = new JPanel();
        tmp_7.setLayout(new BorderLayout());
        tmp_7.setBorder(BorderFactory.createTitledBorder(etchedBorder, "X-Y plot data"));
        tmp_7.setBackground(getBackground().darker());
        tmp_7.add(analysisGraphPanel, BorderLayout.CENTER);
        JPanel tmp_8 = new JPanel();
        tmp_8.setLayout(new BorderLayout());
        tmp_8.add(tmp_7, BorderLayout.CENTER);
        tmp_8.add(tmp_6, BorderLayout.SOUTH);
        ap.add(tmp_8, BorderLayout.CENTER);
        ap.add(tmp_5, BorderLayout.WEST);
        ap.add(apFittingFormula_Text, BorderLayout.SOUTH);
    }

    private void makePV_SelectionPanel() {
        JPanel sp = jpanels[PV_SELECT_PANEL];
        sp.setLayout(new BorderLayout());
        pvSelector.getSlectButton().setText("Set PV");
        pvSelector.setBorder(BorderFactory.createTitledBorder(etchedBorder, "PV Selection Tree:"));
        pvSelector.setBackground(getBackground().darker());
        ButtonGroup groupSP = new ButtonGroup();
        groupSP.add(spScanPV_Button);
        groupSP.add(spMeasurePV_Button);
        groupSP.add(spValidationPV_Button);
        groupSP.add(spScanPV_RB_Button);
        EmptyBorder epmtyBorder = new EmptyBorder(0, 0, 0, 0);
        spScanPV_Button.setBorder(epmtyBorder);
        spScanPV_RB_Button.setBorder(epmtyBorder);
        spMeasurePV_Button.setBorder(epmtyBorder);
        spValidationPV_Button.setBorder(epmtyBorder);
        spScanPV_Text.setHorizontalAlignment(JTextField.CENTER);
        spScanPV_RB_Text.setHorizontalAlignment(JTextField.CENTER);
        spMeasurePV_Text.setHorizontalAlignment(JTextField.CENTER);
        spValidationPV_Text.setHorizontalAlignment(JTextField.CENTER);
        JPanel tmp_1 = new JPanel();
        tmp_1.setBorder(BorderFactory.createTitledBorder(etchedBorder, null));
        tmp_1.setBackground(getBackground().darker());
        tmp_1.setLayout(new GridLayout(2, 0));
        tmp_1.add(spScanPV_Button, BorderLayout.NORTH);
        tmp_1.add(spScanPV_Text, BorderLayout.SOUTH);
        JPanel tmp_2 = new JPanel();
        tmp_2.setBorder(BorderFactory.createTitledBorder(etchedBorder, null));
        tmp_2.setBackground(getBackground().darker());
        tmp_2.setLayout(new GridLayout(2, 0));
        tmp_2.add(spMeasurePV_Button, BorderLayout.NORTH);
        tmp_2.add(spMeasurePV_Text, BorderLayout.SOUTH);
        JPanel tmp_3 = new JPanel();
        tmp_3.setBorder(BorderFactory.createTitledBorder(etchedBorder, null));
        tmp_3.setBackground(getBackground().darker());
        tmp_3.setLayout(new GridLayout(2, 0));
        tmp_3.add(spValidationPV_Button, BorderLayout.NORTH);
        tmp_3.add(spValidationPV_Text, BorderLayout.SOUTH);
        JPanel tmp_4 = new JPanel();
        tmp_4.setBorder(BorderFactory.createTitledBorder(etchedBorder, null));
        tmp_4.setBackground(getBackground().darker());
        tmp_4.setLayout(new GridLayout(2, 0));
        tmp_4.add(spScanPV_RB_Button, BorderLayout.NORTH);
        tmp_4.add(spScanPV_RB_Text, BorderLayout.SOUTH);
        JPanel tmp_5 = new JPanel();
        tmp_5.setLayout(new GridLayout(0, 2));
        tmp_5.add(tmp_1);
        tmp_5.add(tmp_4);
        tmp_5.add(tmp_2);
        tmp_5.add(tmp_3);
        JPanel tmp_6 = new JPanel();
        tmp_6.setLayout(new GridLayout(0, 2));
        tmp_6.setBorder(BorderFactory.createTitledBorder(etchedBorder, null));
        tmp_6.setBackground(getBackground().darker());
        tmp_6.add(spCloseSelectionButton);
        tmp_6.add(spEscapeButton);
        JPanel tmp_7 = new JPanel();
        tmp_7.setLayout(new BorderLayout());
        tmp_7.setBorder(BorderFactory.createTitledBorder(etchedBorder, null));
        tmp_7.setBackground(getBackground().darker());
        tmp_7.add(spChooseAcceleratorButton, BorderLayout.NORTH);
        tmp_7.add(tmp_6, BorderLayout.SOUTH);
        sp.add(tmp_5, BorderLayout.NORTH);
        sp.add(pvSelector, BorderLayout.CENTER);
        sp.add(tmp_7, BorderLayout.SOUTH);
        pvSelector.setPVSelectedListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                PVSelector pvS = (PVSelector) e.getSource();
                String pvName = pvS.getSelectedPVName();
                if (pvName != null) {
                    if (spScanPV_Button.isSelected()) {
                        spScanPV_Text.setText(pvName);
                    } else if (spMeasurePV_Button.isSelected()) {
                        spMeasurePV_Text.setText(pvName);
                    } else if (spValidationPV_Button.isSelected()) {
                        spValidationPV_Text.setText(pvName);
                    } else if (spScanPV_RB_Button.isSelected()) {
                        spScanPV_RB_Text.setText(pvName);
                    }
                } else {
                    if (spScanPV_Button.isSelected()) {
                        spScanPV_Text.setText(null);
                    } else if (spMeasurePV_Button.isSelected()) {
                        spMeasurePV_Text.setText(null);
                    } else if (spValidationPV_Button.isSelected()) {
                        spValidationPV_Text.setText(null);
                    } else if (spScanPV_RB_Button.isSelected()) {
                        spScanPV_RB_Text.setText(null);
                    }
                }
            }
        });
        spChooseAcceleratorButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                AbsolutePathFinder pf = new AbsolutePathFinder(xalRootName);
                String xalRootPath = pf.getXALPath();
                JFileChooser ch;
                if (xalRootPath != null) {
                    ch = new JFileChooser(new File(xalRootPath));
                } else {
                    ch = new JFileChooser();
                }
                if (accelDataFile != null) {
                    ch.setSelectedFile(accelDataFile);
                }
                ch.setDialogTitle("Set new accelerator data XML file");
                int returnVal = ch.showOpenDialog(getJPanel());
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    accelDataFile = ch.getSelectedFile();
                    setNameOfAcceleartorFile(accelDataFile.getAbsolutePath());
                    return;
                }
                Toolkit.getDefaultToolkit().beep();
            }
        });
        spCloseSelectionButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (checkConnections()) {
                    createConnections();
                    activatePanel(MAIN_PANEL);
                    return;
                }
            }
        });
        spEscapeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                activatePanel(MAIN_PANEL);
            }
        });
        AbsolutePathFinder pf = new AbsolutePathFinder(xalRootName);
        String absName = pf.getAbsolutePath(relNameOfXALFile);
        if (absName != null) {
            setNameOfAcceleartorFile(absName);
        }
    }

    private void setNameOfAcceleartorFile(String nameOfXALFile) {
        File flIn = new File(nameOfXALFile);
        if (flIn.exists()) {
            String url = null;
            try {
                url = flIn.toURL().toString();
            } catch (MalformedURLException e) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            XMLDataManager dMgr = new XMLDataManager(url);
            Accelerator accel = null;
            try {
                accel = dMgr.getAccelerator();
            } catch (Exception e) {
                System.err.println("Cannot get accelerator: Exeption - " + e.getMessage());
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            pvSelector.setAccelerator(accel);
        } else {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
    }

    private void makeInfoPanel() {
        JPanel ip = jpanels[INFO_MESSAGE_PANEL];
        ip.setLayout(new BorderLayout());
        ip.add(infoTextArea, BorderLayout.CENTER);
        ip.add(infoOkButton, BorderLayout.SOUTH);
        infoTextArea.setLineWrap(true);
        infoOkButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                activatePanel(infoReturnIndex);
            }
        });
    }

    private void showInfoPanel(String message, int returnPanelIndex) {
        infoTextArea.setText(null);
        infoTextArea.setText(message);
        infoReturnIndex = returnPanelIndex;
        activatePanel(INFO_MESSAGE_PANEL);
    }

    private boolean checkConnections() {
        Channel chIndp = null;
        Channel chIndp_RB = null;
        Channel chMeasure = null;
        Channel chValidator = null;
        if (spScanPV_Text.getText().length() > 0) {
            chIndp = ChannelFactory.defaultFactory().getChannel(spScanPV_Text.getText());
        } else {
            showInfoPanel("PV for scanning should be specified (X axis).", PV_SELECT_PANEL);
            return false;
        }
        if (spMeasurePV_Text.getText().length() > 0) {
            chMeasure = ChannelFactory.defaultFactory().getChannel(spMeasurePV_Text.getText());
        } else {
            showInfoPanel("PV for measuring should be specified (Y axis).", PV_SELECT_PANEL);
            return false;
        }
        if (spScanPV_RB_Text.getText().length() > 0) {
            chIndp_RB = ChannelFactory.defaultFactory().getChannel(spScanPV_RB_Text.getText());
        }
        if (spValidationPV_Text.getText().length() > 0) {
            chValidator = ChannelFactory.defaultFactory().getChannel(spValidationPV_Text.getText());
        }
        try {
            if (chIndp != null && !chIndp.writeAccess()) {
                String msg = "PV for scanning:" + System.getProperty("line.separator");
                msg = msg + "PV (" + chIndp.channelName() + ") does not have write access. Set the new PV name.";
                showInfoPanel(msg, PV_SELECT_PANEL);
                return false;
            }
            if (chIndp_RB != null && !chIndp_RB.readAccess()) {
                String msg = "PV Read Back for scanning:" + System.getProperty("line.separator");
                msg = msg + "PV (" + chIndp.channelName() + ") does not have read access. Set the new PV name.";
                showInfoPanel(msg, PV_SELECT_PANEL);
                return false;
            }
            if (chMeasure != null && !chMeasure.readAccess()) {
                String msg = "PV for measuring:" + System.getProperty("line.separator");
                msg = msg + "PV (" + chIndp.channelName() + ") does not have read access. Set the new PV name.";
                showInfoPanel(msg, PV_SELECT_PANEL);
                return false;
            }
            if (chValidator != null && !chValidator.readAccess()) {
                String msg = "PV for valuation:" + System.getProperty("line.separator");
                msg = msg + "PV (" + chIndp.channelName() + ") does not have read access. Set the new PV name.";
                showInfoPanel(msg, PV_SELECT_PANEL);
                return false;
            }
        } catch (ConnectionException e) {
            String msg = "Cannot connect to PVs:" + System.getProperty("line.separator");
            if (spScanPV_Text.getText().length() > 0) {
                msg = msg + spScanPV_Text.getText() + System.getProperty("line.separator");
            }
            if (spScanPV_RB_Text.getText().length() > 0) {
                msg = msg + spScanPV_RB_Text.getText() + System.getProperty("line.separator");
            }
            if (spMeasurePV_Text.getText().length() > 0) {
                msg = msg + spMeasurePV_Text.getText() + System.getProperty("line.separator");
            }
            if (spValidationPV_Text.getText().length() > 0) {
                msg = msg + spValidationPV_Text.getText() + System.getProperty("line.separator");
            }
            msg = msg + "Please, check EPICS or names of PVs.";
            showInfoPanel(msg, PV_SELECT_PANEL);
            return false;
        }
        return true;
    }

    public void setPV_Names(String scanPV, String scanPV_RB, String measPV, String validPV) {
        spScanPV_Text.setText(scanPV);
        spScanPV_RB_Text.setText(scanPV_RB);
        spMeasurePV_Text.setText(measPV);
        spValidationPV_Text.setText(validPV);
        if (checkConnections()) {
            createConnections();
            activatePanel(MAIN_PANEL);
        }
    }

    private void createConnections() {
        mScanPV_Name_Text.setText(spScanPV_Text.getText());
        mScanPV_RB_Name_Text.setText(spScanPV_RB_Text.getText());
        mMeasurePV_Name_Text.setText(spMeasurePV_Text.getText());
        mValidationPV_Name_Text.setText(spValidationPV_Text.getText());
        if (spScanPV_Text.getText().length() > 0) {
            independValue.setChannelName(spScanPV_Text.getText());
        } else {
            independValue.setChannelName(null);
        }
        if (spScanPV_RB_Text.getText().length() > 0) {
            independValue.setChannelNameRB(spScanPV_RB_Text.getText());
        } else {
            independValue.setChannelNameRB(null);
        }
        if (spMeasurePV_Text.getText().length() > 0) {
            measuredValue.setChannelName(spMeasurePV_Text.getText());
        } else {
            measuredValue.setChannelName(null);
        }
        if (spValidationPV_Text.getText().length() > 0) {
            valuatorValue.setChannelName(spValidationPV_Text.getText());
        } else {
            valuatorValue.setChannelName(null);
        }
    }

    private void updateGraphPanel() {
        graphPanel.removeAllGraphData();
        if (mPV_ShowButton.isSelected()) {
            graphPanel.addGraphData(measuredValue.getDataContainers());
        }
        if (mPV_RB_ShowButton.isSelected()) {
            graphPanel.addGraphData(measuredValue.getDataContainersRB());
        }
    }

    private void updateAnalysisGraphPanel() {
        analysisGraphPanel.removeAllGraphData();
        analysisGraphPanel.setDisplayGraphMode();
        if (apPV_ShowButton.isSelected()) {
            analysisGraphPanel.addGraphData(measuredValue.getDataContainers());
        }
        if (apPV_RB_ShowButton.isSelected()) {
            analysisGraphPanel.addGraphData(measuredValue.getDataContainersRB());
        }
        analysisGraphPanel.addGraphData(fitGraphData);
    }

    private void updatePV_ValuesOnMainPanel() {
        if (independValue.getChannelName() != null) {
            mScannedPV_Val_Label.setText(mScannedPVFormat.format(independValue.getValue()));
        }
        if (independValue.getChannelNameRB() != null) {
            mScannedPV_RB_Val_Label.setText(mScannedPVFormat.format(independValue.getCurrentValueRB()));
        }
        if (measuredValue.getChannelName() != null) {
            mMeasured_Val_Label.setText(mMeasuredPVFormat.format(measuredValue.getInstantValue()));
        }
        if (valuatorValue.getChannelName() != null) {
            valuatorValue.validate();
            mValidationPV_Val_Label.setText(mValidationPVFormat.format(valuatorValue.getValue()));
        }
    }

    public void saveDataToDisk() {
        JFileChooser ch = new JFileChooser();
        ch.setDialogTitle("Save Data");
        if (graphDataFile != null) {
            ch.setSelectedFile(graphDataFile);
        }
        int returnVal = ch.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                graphDataFile = ch.getSelectedFile();
                BufferedWriter out = new BufferedWriter(new FileWriter(graphDataFile));
                Calendar rightNow = Calendar.getInstance();
                out.write("% one dimensional scan data date=" + (rightNow.get(Calendar.MONTH) + 1) + "/" + rightNow.get(Calendar.DAY_OF_MONTH) + "/" + rightNow.get(Calendar.YEAR) + " time=" + rightNow.get(Calendar.HOUR) + ":" + rightNow.get(Calendar.MINUTE) + ":" + rightNow.get(Calendar.SECOND));
                out.newLine();
                if (spScanPV_Text.getText().length() > 0) {
                    out.write("% " + spScanPV_Text.getText() + "  - scanned PV");
                } else {
                    out.write("% null  - scanned PV");
                }
                out.newLine();
                if (spScanPV_RB_Text.getText().length() > 0) {
                    out.write("% " + spScanPV_RB_Text.getText() + " - scanned Read Back PV");
                } else {
                    out.write("% null  - scanned Read Back PV");
                }
                out.newLine();
                if (spMeasurePV_Text.getText().length() > 0) {
                    out.write("% " + spMeasurePV_Text.getText() + " - measured PV ");
                } else {
                    out.write("% null  - measured PV");
                }
                out.newLine();
                if (spValidationPV_Text.getText().length() > 0) {
                    out.write("% " + spValidationPV_Text.getText() + " - validation PV");
                } else {
                    out.write("% null  - validation PV");
                }
                out.newLine();
                if (measuredValue != null) {
                    int nDataCont = measuredValue.getNumberOfDataContainers();
                    out.write("% " + nDataCont + " - number of data containers for scanned PV");
                    out.newLine();
                    for (int i = 0; i < nDataCont; i++) {
                        BasicGraphData gd = measuredValue.getDataContainer(i);
                        int nPoints = gd.getNumbOfPoints();
                        out.write("% " + nPoints + " - number of points in new graph data ===========");
                        out.newLine();
                        for (int k = 0; k < nPoints; k++) {
                            out.write(" " + gd.getX(k) + " " + gd.getY(k) + " " + gd.getErr(k));
                            out.newLine();
                        }
                    }
                    nDataCont = measuredValue.getNumberOfDataContainersRB();
                    out.write("% " + nDataCont + " - number of data containers for scanned Read Back PV");
                    out.newLine();
                    for (int i = 0; i < nDataCont; i++) {
                        BasicGraphData gd = measuredValue.getDataContainerRB(i);
                        int nPoints = gd.getNumbOfPoints();
                        out.write("% " + nPoints + " - number of points in new graph data ===========");
                        out.newLine();
                        for (int k = 0; k < nPoints; k++) {
                            out.write(" " + gd.getX(k) + " " + gd.getY(k) + " " + gd.getErr(k));
                            out.newLine();
                        }
                    }
                } else {
                    out.write("% 0 - number of data containers for scanned PV");
                    out.write("% 0 - number of data containers for scanned Read Back PV");
                    out.newLine();
                }
                out.write("%-----end of data---------");
                out.newLine();
                out.flush();
                out.close();
            } catch (IOException e) {
                Toolkit.getDefaultToolkit().beep();
                System.out.println(e.toString());
            }
        }
    }

    private void readDataFromDisk() {
        JFileChooser ch = new JFileChooser();
        ch.setDialogTitle("Read Data");
        if (graphDataFile != null) {
            ch.setSelectedFile(graphDataFile);
        }
        int returnVal = ch.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            graphDataFile = ch.getSelectedFile();
            readDataFromDisk(graphDataFile);
        }
    }

    public void readDataFromDisk(File inputFile) {
        try {
            StringTokenizer st = null;
            String strVar = null;
            BufferedReader in = new BufferedReader(new FileReader(inputFile));
            String inLine;
            inLine = in.readLine();
            inLine = in.readLine();
            st = new StringTokenizer(inLine);
            st.nextToken();
            strVar = st.nextToken();
            if (strVar.equals("null")) {
                spScanPV_Text.setText(null);
            } else {
                spScanPV_Text.setText(strVar);
            }
            inLine = in.readLine();
            st = new StringTokenizer(inLine);
            st.nextToken();
            strVar = st.nextToken();
            if (strVar.equals("null")) {
                spScanPV_RB_Text.setText(null);
            } else {
                spScanPV_RB_Text.setText(strVar);
            }
            inLine = in.readLine();
            st = new StringTokenizer(inLine);
            st.nextToken();
            strVar = st.nextToken();
            if (strVar.equals("null")) {
                spMeasurePV_Text.setText(null);
            } else {
                spMeasurePV_Text.setText(strVar);
            }
            inLine = in.readLine();
            st = new StringTokenizer(inLine);
            st.nextToken();
            strVar = st.nextToken();
            if (strVar.equals("null")) {
                spValidationPV_Text.setText(null);
            } else {
                spValidationPV_Text.setText(strVar);
            }
            inLine = in.readLine();
            st = new StringTokenizer(inLine);
            st.nextToken();
            int nDataCont = Integer.parseInt(st.nextToken());
            for (int i = 0; i < nDataCont; i++) {
                inLine = in.readLine();
                st = new StringTokenizer(inLine);
                st.nextToken();
                int nPoints = Integer.parseInt(st.nextToken());
                measuredValue.createNewDataContainer();
                int indCont = measuredValue.getNumberOfDataContainers() - 1;
                for (int k = 0; k < nPoints; k++) {
                    inLine = in.readLine();
                    st = new StringTokenizer(inLine);
                    double x = Double.parseDouble(st.nextToken());
                    double y = Double.parseDouble(st.nextToken());
                    double err = Double.parseDouble(st.nextToken());
                    measuredValue.getDataContainer(indCont).addPoint(x, y, err);
                }
            }
            inLine = in.readLine();
            st = new StringTokenizer(inLine);
            st.nextToken();
            nDataCont = Integer.parseInt(st.nextToken());
            for (int i = 0; i < nDataCont; i++) {
                inLine = in.readLine();
                st = new StringTokenizer(inLine);
                st.nextToken();
                int nPoints = Integer.parseInt(st.nextToken());
                measuredValue.createNewDataContainerRB();
                int indCont = measuredValue.getNumberOfDataContainersRB() - 1;
                for (int k = 0; k < nPoints; k++) {
                    inLine = in.readLine();
                    st = new StringTokenizer(inLine);
                    double x = Double.parseDouble(st.nextToken());
                    double y = Double.parseDouble(st.nextToken());
                    double err = Double.parseDouble(st.nextToken());
                    measuredValue.getDataContainerRB(indCont).addPoint(x, y, err);
                }
            }
            in.close();
            if (checkConnections()) {
                createConnections();
            }
            updateGraphPanel();
            updateAnalysisGraphPanel();
        } catch (IOException e) {
            Toolkit.getDefaultToolkit().beep();
            System.out.println(e.toString());
        }
    }

    public void readDataFromDisk(String nameOfFile) {
    }

    private JPanel getJPanel() {
        return this;
    }

    public static void main(String args[]) {
        JFrame mainFrame = new JFrame("GeneralOneDimScan Panel");
        mainFrame.addWindowListener(new java.awt.event.WindowAdapter() {

            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                System.exit(0);
            }
        });
        mainFrame.getContentPane().setLayout(new BorderLayout());
        GeneralOneDimScan scan1D = new GeneralOneDimScan();
        mainFrame.getContentPane().add(scan1D, BorderLayout.CENTER);
        mainFrame.pack();
        mainFrame.setVisible(true);
    }
}
