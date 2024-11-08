package jp.jparc.apps.bbc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.*;
import javax.swing.border.Border;
import gov.sns.tools.apputils.SimpleChartPopupMenu;
import gov.sns.tools.apputils.VerticalLayout;
import gov.sns.tools.plot.BasicGraphData;
import gov.sns.tools.plot.FunctionGraphsJPanel;
import gov.sns.tools.scan.SecondEdition.IncrementalColor;
import gov.sns.tools.scan.SecondEdition.MeasuredValue;
import gov.sns.xal.model.scenario.Scenario;
import gov.sns.xal.smf.AcceleratorNode;
import gov.sns.xal.smf.AcceleratorSeq;
import gov.sns.xal.smf.application.*;
import gov.sns.xal.smf.impl.*;

/** BBC document window */
public class BbcWindow extends AcceleratorWindow {

    protected JTabbedPane mainTabbedPane;

    private BbcDocument theDoc;

    /** main panels */
    private JPanel setupPanel, scanXPanel, scanYPanel;

    private JSplitPane analysisSplitPane;

    protected JTextArea pvListTextAreaX, pvListTextAreaY;

    /** the lists to choose BPMs and corrector */
    protected JList BPM1List, BPM2List, BPM3List, correctorHList, correctorVList, quadMagnetList, BCMList;

    /** the label to choose BPMs and corrector */
    private JLabel BPM1ListLabel, BPM2ListLabel, BPM3ListLabel, correctorHListLabel, correctorVListLabel, BCMListLabel;

    /** the scroll pane to choose BPMs and corrector */
    private JScrollPane BPM1SelectScrollPane, BPM2SelectScrollPane, BPM3SelectScrollPane, correctorHScrollPane, correctorVScrollPane, BCMSelectScrollPane;

    /** button to confirm device setup */
    private JButton setupButton;

    /** customized button color */
    private Color buttonColor = new Color(0, 225, 255);

    /** error message text field */
    protected JTextField errorText;

    /** Name of selected quadrupole magnet text field */
    private JTextField selectedQuadNameText;

    /** horizontal center analysis message text area */
    protected JTextArea analysisTextAreaX;

    /** vertical center analysis message text area */
    protected JTextArea analysisTextAreaY;

    /** horizontal center result message text area */
    protected JTextArea resultTextAreaX = new JTextArea("BPM Horizontal Center");

    /** vertical center result message text area */
    protected JTextArea resultTextAreaY = new JTextArea("BPM Vertical Center");

    /**graph panel to display horizontal scanned data */
    protected FunctionGraphsJPanel graphAnalysisH = new FunctionGraphsJPanel();

    /**graph panel to display vertical scanned data */
    protected FunctionGraphsJPanel graphAnalysisV = new FunctionGraphsJPanel();

    private boolean plotBpm2QuadFlagX = true;

    private boolean plotBpm2QuadFlagY = true;

    private JRadioButton setStrobeRadioX = new JRadioButton("Using Strobe for Quad Current Setting");

    private JRadioButton setStrobeRadioY = new JRadioButton("Using Strobe for Quad Current Setting");

    /** Constructor */
    public BbcWindow(final BbcDocument bbcDocument) {
        super(bbcDocument);
        setSize(950, 825);
        theDoc = bbcDocument;
        BPM1List = new JList();
        BPM2List = new JList();
        BPM3List = new JList();
        correctorHList = new JList();
        correctorVList = new JList();
        quadMagnetList = new JList();
        BCMList = new JList();
        BPM1ListLabel = new JLabel("CENTER BPM");
        BPM1ListLabel.setHorizontalAlignment(SwingConstants.CENTER);
        BPM2ListLabel = new JLabel("Downstream BPM");
        BPM2ListLabel.setHorizontalAlignment(SwingConstants.CENTER);
        BPM3ListLabel = new JLabel("Upstream BPM");
        BPM3ListLabel.setHorizontalAlignment(SwingConstants.CENTER);
        correctorHListLabel = new JLabel("Steer(H)");
        correctorHListLabel.setHorizontalAlignment(SwingConstants.CENTER);
        correctorVListLabel = new JLabel("Steer (V)");
        correctorVListLabel.setHorizontalAlignment(SwingConstants.CENTER);
        BCMListLabel = new JLabel("Validator SCT");
        BCMListLabel.setHorizontalAlignment(SwingConstants.CENTER);
        BPM1SelectScrollPane = new JScrollPane(BPM1List);
        BPM2SelectScrollPane = new JScrollPane(BPM2List);
        BPM3SelectScrollPane = new JScrollPane(BPM3List);
        correctorHScrollPane = new JScrollPane(correctorHList);
        correctorVScrollPane = new JScrollPane(correctorVList);
        BCMSelectScrollPane = new JScrollPane(BCMList);
        setupButton = new JButton("Set Selections");
        setupButton.setBackground(buttonColor);
        pvListTextAreaX = new JTextArea();
        pvListTextAreaX.setLineWrap(true);
        pvListTextAreaX.setWrapStyleWord(true);
        pvListTextAreaY = new JTextArea();
        pvListTextAreaY.setLineWrap(true);
        pvListTextAreaY.setWrapStyleWord(true);
        analysisTextAreaX = new JTextArea();
        analysisTextAreaX.setLineWrap(true);
        analysisTextAreaX.setWrapStyleWord(true);
        analysisTextAreaY = new JTextArea();
        analysisTextAreaY.setLineWrap(true);
        analysisTextAreaY.setWrapStyleWord(true);
        errorText = new JTextField();
        errorText.setForeground(java.awt.Color.RED);
        initAnalysisGraph(graphAnalysisH);
        graphAnalysisH.setName("Horizontal Center");
        initAnalysisGraph(graphAnalysisV);
        graphAnalysisV.setName("Vertical Center");
        makeContent();
    }

    /** initialize the attribute of graph panel of analysis  */
    private void initAnalysisGraph(FunctionGraphsJPanel graphAnalysis) {
        SimpleChartPopupMenu.addPopupMenuTo(graphAnalysis);
        graphAnalysis.setOffScreenImageDrawing(true);
        graphAnalysis.setAxisNames("Slope of Quad-BPM", "Center BPM Measured Value");
        graphAnalysis.setGraphBackGroundColor(Color.white);
        graphAnalysis.setLegendButtonVisible(true);
    }

    /** Overrides the inherited method to create the toolbar. */
    @Override
    public boolean usesToolbar() {
        return true;
    }

    /** Create the main window subviews. */
    protected void makeContent() {
        Container container = getContentPane();
        makeSetupPanel();
        makeScanXPanel();
        makeScanYPanel();
        makeAnalysisPanel();
        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setVisible(true);
        mainTabbedPane.add("Setup", setupPanel);
        mainTabbedPane.add("Horizontal Center Scan", scanXPanel);
        mainTabbedPane.add("Vertical Center Scan", scanYPanel);
        mainTabbedPane.add("Analysis", analysisSplitPane);
        container.add(mainTabbedPane, BorderLayout.CENTER);
        container.add(errorText, BorderLayout.SOUTH);
    }

    /** Create device selection panel */
    private void makeSetupPanel() {
        setupPanel = new JPanel();
        GridBagLayout spGridBag = new GridBagLayout();
        setupPanel.setLayout(spGridBag);
        setupPanel.setPreferredSize(new Dimension(200, 200));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.;
        gbc.weighty = 0.;
        gbc.gridx = 0;
        gbc.gridy = 0;
        spGridBag.setConstraints(BPM1ListLabel, gbc);
        setupPanel.add(BPM1ListLabel);
        gbc.gridx = 1;
        gbc.gridy = 0;
        spGridBag.setConstraints(BPM2ListLabel, gbc);
        setupPanel.add(BPM2ListLabel);
        gbc.gridx = 2;
        gbc.gridy = 0;
        spGridBag.setConstraints(BPM3ListLabel, gbc);
        setupPanel.add(BPM3ListLabel);
        gbc.gridx = 3;
        gbc.gridy = 0;
        spGridBag.setConstraints(correctorHListLabel, gbc);
        setupPanel.add(correctorHListLabel);
        gbc.gridx = 4;
        gbc.gridy = 0;
        spGridBag.setConstraints(correctorVListLabel, gbc);
        setupPanel.add(correctorVListLabel);
        gbc.gridx = 5;
        gbc.gridy = 0;
        spGridBag.setConstraints(BCMListLabel, gbc);
        setupPanel.add(BCMListLabel);
        BPM1List.setVisibleRowCount(8);
        gbc.weighty = 1.;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridheight = 5;
        spGridBag.setConstraints(BPM1SelectScrollPane, gbc);
        setupPanel.add(BPM1SelectScrollPane);
        BPM2List.setVisibleRowCount(8);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridheight = 5;
        spGridBag.setConstraints(BPM2SelectScrollPane, gbc);
        setupPanel.add(BPM2SelectScrollPane);
        BPM3List.setVisibleRowCount(8);
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.gridheight = 5;
        spGridBag.setConstraints(BPM3SelectScrollPane, gbc);
        setupPanel.add(BPM3SelectScrollPane);
        correctorHList.setVisibleRowCount(8);
        gbc.gridx = 3;
        gbc.gridy = 1;
        gbc.gridheight = 5;
        spGridBag.setConstraints(correctorHScrollPane, gbc);
        setupPanel.add(correctorHScrollPane);
        correctorVList.setVisibleRowCount(8);
        gbc.gridx = 4;
        gbc.gridy = 1;
        gbc.gridheight = 5;
        spGridBag.setConstraints(correctorVScrollPane, gbc);
        setupPanel.add(correctorVScrollPane);
        BCMList.setVisibleRowCount(8);
        gbc.gridx = 5;
        gbc.gridy = 1;
        gbc.gridheight = 5;
        spGridBag.setConstraints(BCMSelectScrollPane, gbc);
        setupPanel.add(BCMSelectScrollPane);
        gbc.weighty = 0.;
        gbc.gridx = 3;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        selectedQuadNameText = new JTextField("Selected Quadrupole");
        selectedQuadNameText.setEditable(false);
        selectedQuadNameText.setHorizontalAlignment(JTextField.CENTER);
        selectedQuadNameText.setFont(new Font("Monospaced", Font.PLAIN, 14));
        selectedQuadNameText.setBorder(javax.swing.BorderFactory.createLineBorder(Color.GRAY));
        selectedQuadNameText.setForeground(new Color(0, 127, 127));
        spGridBag.setConstraints(selectedQuadNameText, gbc);
        setupPanel.add(selectedQuadNameText);
        gbc.weighty = 0.;
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        spGridBag.setConstraints(setupButton, gbc);
        setupPanel.add(setupButton);
        setupButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setAccelComponents();
                setStrobeRadioX.setEnabled(true);
                setStrobeRadioY.setEnabled(true);
            }
        });
    }

    /** Create scan for horizontal scan */
    private void makeScanXPanel() {
        scanXPanel = new JPanel(new BorderLayout());
        scanXPanel.setPreferredSize(new Dimension(200, 400));
        JPanel tmp_0 = new JPanel();
        tmp_0.setLayout(new VerticalLayout());
        JPanel setStrobePanel = new JPanel();
        setStrobePanel.setLayout(new VerticalLayout());
        setStrobePanel.add(setStrobeRadioX);
        tmp_0.add(setStrobePanel);
        setStrobeRadioX.setEnabled(false);
        setStrobeRadioX.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (setStrobeRadioX.isSelected()) {
                    System.out.println("Strobe PV Name: " + theDoc.selectedQuadMagnet.getChannel(MagnetPowerSupply.CURRENT_STROBE_HANDLE).getId());
                    theDoc.scanStuff.scanVariableQuadX.setStrobeChannel(theDoc.selectedQuadMagnet.getChannel(MagnetPowerSupply.CURRENT_STROBE_HANDLE), 1);
                } else {
                    theDoc.scanStuff.scanVariableQuadX.removeStrobeChannel();
                }
            }
        });
        tmp_0.add(theDoc.scanStuff.scanControllerX.getJPanel());
        tmp_0.add(theDoc.scanStuff.avgCntrX.getJPanel(0));
        tmp_0.add(theDoc.scanStuff.vldCntrX.getJPanel());
        JScrollPane pvTextScrollPane = new JScrollPane(pvListTextAreaX);
        pvTextScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        pvTextScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tmp_0.add(pvTextScrollPane);
        Border etchedBorder = BorderFactory.createEtchedBorder();
        scanXPanel.add(tmp_0, BorderLayout.WEST);
        scanXPanel.add(theDoc.scanStuff.graphScanX, BorderLayout.CENTER);
    }

    /** Create scan for vertical scan */
    private void makeScanYPanel() {
        scanYPanel = new JPanel(new BorderLayout());
        scanYPanel.setPreferredSize(new Dimension(200, 400));
        JPanel tmp_0 = new JPanel();
        tmp_0.setLayout(new VerticalLayout());
        JPanel setStrobePanel = new JPanel();
        setStrobePanel.setLayout(new VerticalLayout());
        setStrobePanel.add(setStrobeRadioY);
        tmp_0.add(setStrobePanel);
        setStrobeRadioY.setEnabled(false);
        setStrobeRadioY.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (setStrobeRadioY.isSelected()) {
                    System.out.println("Strobe PV Name: " + theDoc.selectedQuadMagnet.getChannel(MagnetPowerSupply.CURRENT_STROBE_HANDLE).getId());
                    theDoc.scanStuff.scanVariableQuadY.setStrobeChannel(theDoc.selectedQuadMagnet.getChannel(MagnetPowerSupply.CURRENT_STROBE_HANDLE), 1);
                } else {
                    theDoc.scanStuff.scanVariableQuadY.removeStrobeChannel();
                }
            }
        });
        tmp_0.add(theDoc.scanStuff.scanControllerY.getJPanel());
        tmp_0.add(theDoc.scanStuff.avgCntrY.getJPanel(0));
        tmp_0.add(theDoc.scanStuff.vldCntrY.getJPanel());
        JScrollPane pvTextScrollPane = new JScrollPane(pvListTextAreaY);
        pvTextScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        pvTextScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tmp_0.add(pvTextScrollPane);
        Border etchedBorder = BorderFactory.createEtchedBorder();
        scanYPanel.add(tmp_0, BorderLayout.WEST);
        scanYPanel.add(theDoc.scanStuff.graphScanY, BorderLayout.CENTER);
    }

    /** Create analysis panel */
    private void makeAnalysisPanel() {
        analysisSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, makeSeperatedAnalysisPanel(graphAnalysisH), makeSeperatedAnalysisPanel(graphAnalysisV));
        analysisSplitPane.setOneTouchExpandable(true);
        analysisSplitPane.setResizeWeight(0.5);
    }

    private JSplitPane makeSeperatedAnalysisPanel(FunctionGraphsJPanel graphAnalysis) {
        final FunctionGraphsJPanel graphAnalysisLocal = graphAnalysis;
        final JRadioButton radioBpm2Quad = new JRadioButton("BPM vs Quad");
        final JRadioButton radioBpm2Slope = new JRadioButton("Center BPM vs Slope");
        final JRadioButton radioSteer2Slope = new JRadioButton("Steer Field vs Slope");
        JPanel analysisSetupPanel = new JPanel();
        GridBagLayout anGridBag = new GridBagLayout();
        analysisSetupPanel.setLayout(anGridBag);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.NONE;
        Insets sepInsets = new Insets(5, 0, 5, 0);
        Insets nullInsets = new Insets(0, 0, 0, 0);
        int sumy = 0;
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        JButton importDataButton = new JButton("Import Data");
        importDataButton.setBackground(buttonColor);
        buttonPanel.add(importDataButton);
        importDataButton.setActionCommand(graphAnalysis.getName());
        importDataButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals(graphAnalysisH.getName())) theDoc.analysisStuff.init(graphAnalysisH, theDoc.scanStuff.BPM1XPosMV, theDoc.scanStuff.BPM2XPosMV, theDoc.scanStuff.BPM3XPosMV);
                if (e.getActionCommand().equals(graphAnalysisV.getName())) theDoc.analysisStuff.init(graphAnalysisV, theDoc.scanStuff.BPM1YPosMV, theDoc.scanStuff.BPM2YPosMV, theDoc.scanStuff.BPM3YPosMV);
            }
        });
        JButton analysisButton = new JButton("Find Center");
        analysisButton.setBackground(buttonColor);
        buttonPanel.add(analysisButton);
        analysisButton.setActionCommand(graphAnalysis.getName());
        analysisButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals(graphAnalysisH.getName())) if (theDoc.analysisStuff.analysisDataXReady) {
                    theDoc.analysisStuff.solve(graphAnalysisH);
                    radioBpm2Slope.setEnabled(true);
                } else {
                    JOptionPane.showMessageDialog(theDoc.myWindow(), "The measured data is not imported yet.\n" + "Please import all measured data by clicking the \"Import data\" button.");
                }
                if (e.getActionCommand().equals(graphAnalysisV.getName())) if (theDoc.analysisStuff.analysisDataYReady) {
                    theDoc.analysisStuff.solve(graphAnalysisV);
                    radioBpm2Slope.setEnabled(true);
                } else {
                    JOptionPane.showMessageDialog(theDoc.myWindow(), "The measured data is not imported yet.\n" + "Please import all measured data by clicking the \"Import data\" button.");
                }
            }
        });
        JButton STFieldButton = new JButton("ST Field");
        STFieldButton.setBackground(buttonColor);
        buttonPanel.add(STFieldButton);
        STFieldButton.setActionCommand(graphAnalysis.getName());
        STFieldButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals(graphAnalysisH.getName())) {
                    if (theDoc.analysisStuff.analysisDataXReady) {
                        theDoc.analysisStuff.findSteerField(graphAnalysisH);
                        radioSteer2Slope.setEnabled(true);
                    } else {
                        JOptionPane.showMessageDialog(theDoc.myWindow(), "The measured data is not imported yet.\n" + "Please import all measured data by clicking the \"Import data\" button.");
                    }
                }
                if (e.getActionCommand().equals(graphAnalysisV.getName())) {
                    if (theDoc.analysisStuff.analysisDataYReady) {
                        theDoc.analysisStuff.findSteerField(graphAnalysisV);
                        radioSteer2Slope.setEnabled(true);
                    } else {
                        JOptionPane.showMessageDialog(theDoc.myWindow(), "The measured data is not imported yet.\n" + "Please import all measured data by clicking the \"Import data\" button.");
                    }
                }
            }
        });
        JButton setStFieldButton = new JButton("Set Steer");
        setStFieldButton.setBackground(buttonColor);
        buttonPanel.add(setStFieldButton);
        setStFieldButton.setActionCommand(graphAnalysis.getName());
        setStFieldButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (e.getActionCommand().equals(graphAnalysisH.getName())) if (theDoc.analysisStuff.stXFieldReady) {
                    theDoc.analysisStuff.setSteerField(true);
                } else {
                    JOptionPane.showMessageDialog(theDoc.myWindow(), "Please find the set value by clicking the \"ST Field\" button.");
                }
                if (e.getActionCommand().equals(graphAnalysisV.getName())) if (theDoc.analysisStuff.stYFieldReady) {
                    theDoc.analysisStuff.setSteerField(false);
                } else {
                    JOptionPane.showMessageDialog(theDoc.myWindow(), "Please find the set value by clicking the \"ST Field\" button.");
                }
            }
        });
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = nullInsets;
        gbc.weightx = 1.;
        gbc.gridwidth = 0;
        gbc.gridx = 0;
        gbc.gridy = sumy++;
        anGridBag.setConstraints(buttonPanel, gbc);
        analysisSetupPanel.add(buttonPanel);
        gbc.insets = sepInsets;
        gbc.weightx = 1.;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = sumy++;
        JSeparator sep01 = new JSeparator(SwingConstants.HORIZONTAL);
        anGridBag.setConstraints(sep01, gbc);
        analysisSetupPanel.add(sep01);
        sep01.setVisible(true);
        gbc.weightx = 1;
        gbc.gridwidth = 1;
        gbc.insets = nullInsets;
        gbc.gridx = 0;
        gbc.gridy = sumy++;
        JLabel daSourcePlot = new JLabel("Selected Plot data source");
        anGridBag.setConstraints(daSourcePlot, gbc);
        analysisSetupPanel.add(daSourcePlot);
        JPanel radioPanel = new JPanel();
        radioPanel.setLayout(new BoxLayout(radioPanel, BoxLayout.X_AXIS));
        ButtonGroup group = new ButtonGroup();
        group.add(radioBpm2Quad);
        radioPanel.add(radioBpm2Quad);
        radioBpm2Quad.setSelected(true);
        group.add(radioBpm2Slope);
        radioPanel.add(radioBpm2Slope);
        radioBpm2Slope.setEnabled(false);
        group.add(radioSteer2Slope);
        radioPanel.add(radioSteer2Slope);
        radioSteer2Slope.setEnabled(false);
        radioBpm2Quad.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (graphAnalysisLocal.getName().equals(graphAnalysisH.getName())) {
                    if (theDoc.analysisStuff.analysisDataXReady) {
                        plotBpm2QuadFlagX = true;
                        plotMeasuredData(graphAnalysisLocal, theDoc.scanStuff.BPM1XPosMV, theDoc.scanStuff.BPM2XPosMV, theDoc.scanStuff.BPM3XPosMV);
                        resultTextAreaX.setText(theDoc.analysisStuff.bestBPMCenterStrX);
                    } else {
                        plotBpm2QuadFlagX = true;
                        radioBpm2Quad.setSelected(true);
                        radioBpm2Slope.setSelected(false);
                        JOptionPane.showMessageDialog(theDoc.myWindow(), "The measured data is not imported yet.\n" + "Please import all measured data by clicking the \"Import scan data\" button.");
                    }
                } else if (graphAnalysisLocal.getName().equals(graphAnalysisV.getName())) {
                    if (theDoc.analysisStuff.analysisDataYReady) {
                        plotBpm2QuadFlagY = true;
                        plotMeasuredData(graphAnalysisLocal, theDoc.scanStuff.BPM1YPosMV, theDoc.scanStuff.BPM2YPosMV, theDoc.scanStuff.BPM3YPosMV);
                        resultTextAreaY.setText(theDoc.analysisStuff.bestBPMCenterStrY);
                    } else {
                        plotBpm2QuadFlagY = true;
                        radioBpm2Quad.setSelected(true);
                        radioBpm2Slope.setSelected(false);
                        JOptionPane.showMessageDialog(theDoc.myWindow(), "The measured data is not imported yet.\n" + "Please import all measured data by clicking the \"Import scan data\" button.");
                    }
                }
            }
        });
        radioBpm2Slope.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (graphAnalysisLocal.getName().equals(graphAnalysisH.getName())) {
                    if (theDoc.analysisStuff.analysisDataXReady) {
                        plotBpm2QuadFlagX = false;
                        plotMeasuredData(graphAnalysisLocal, theDoc.scanStuff.BPM1XPosMV, theDoc.scanStuff.BPM2XPosMV, theDoc.scanStuff.BPM3XPosMV);
                        resultTextAreaX.setText(theDoc.analysisStuff.bestBPMCenterStrX);
                    } else {
                        plotBpm2QuadFlagX = true;
                        radioBpm2Quad.setSelected(true);
                        radioBpm2Slope.setSelected(false);
                        JOptionPane.showMessageDialog(theDoc.myWindow(), "The measured data is not imported yet.\n" + "Please import all measured data by clicking the \"Import scan data\" button.");
                    }
                } else if (graphAnalysisLocal.getName().equals(graphAnalysisV.getName())) {
                    if (theDoc.analysisStuff.analysisDataYReady) {
                        plotBpm2QuadFlagY = false;
                        plotMeasuredData(graphAnalysisLocal, theDoc.scanStuff.BPM1YPosMV, theDoc.scanStuff.BPM2YPosMV, theDoc.scanStuff.BPM3YPosMV);
                        resultTextAreaY.setText(theDoc.analysisStuff.bestBPMCenterStrY);
                    } else {
                        plotBpm2QuadFlagY = true;
                        radioBpm2Quad.setSelected(true);
                        radioBpm2Slope.setSelected(false);
                        JOptionPane.showMessageDialog(theDoc.myWindow(), "The measured data is not imported yet.\n" + "Please import all measured data by clicking the \"Import scan data\" button.");
                    }
                }
            }
        });
        radioSteer2Slope.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (graphAnalysisLocal.getName().equals(graphAnalysisH.getName())) {
                    plotSteer2Slope(graphAnalysisLocal, theDoc.scanStuff.BPM1XPosMV, theDoc.scanStuff.BPM2XPosMV, theDoc.scanStuff.BPM3XPosMV);
                    resultTextAreaX.setText(theDoc.analysisStuff.bestSteerCenterStrX);
                } else if (graphAnalysisLocal.getName().equals(graphAnalysisV.getName())) {
                    plotSteer2Slope(graphAnalysisLocal, theDoc.scanStuff.BPM1YPosMV, theDoc.scanStuff.BPM2YPosMV, theDoc.scanStuff.BPM3YPosMV);
                    resultTextAreaY.setText(theDoc.analysisStuff.bestSteerCenterStrY);
                }
            }
        });
        gbc.weightx = 1;
        gbc.gridwidth = 1;
        gbc.insets = nullInsets;
        gbc.gridx = 0;
        gbc.gridy = sumy++;
        anGridBag.setConstraints(radioPanel, gbc);
        analysisSetupPanel.add(radioPanel);
        gbc.insets = sepInsets;
        gbc.weightx = 1.;
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = sumy++;
        JSeparator sep02 = new JSeparator(SwingConstants.HORIZONTAL);
        anGridBag.setConstraints(sep02, gbc);
        analysisSetupPanel.add(sep02);
        sep02.setVisible(true);
        gbc.weightx = 1;
        gbc.gridwidth = 1;
        gbc.insets = nullInsets;
        gbc.gridx = 0;
        gbc.gridy = sumy++;
        JLabel resultLabel = new JLabel();
        if (graphAnalysis.getName().equals(graphAnalysisH.getName())) resultLabel.setText("X Center Analysis Result");
        if (graphAnalysis.getName().equals(graphAnalysisV.getName())) resultLabel.setText("Y Center Analysis Result");
        anGridBag.setConstraints(resultLabel, gbc);
        analysisSetupPanel.add(resultLabel);
        gbc.weightx = 1;
        gbc.gridwidth = 1;
        gbc.insets = nullInsets;
        gbc.gridx = 0;
        gbc.gridy = sumy++;
        if (graphAnalysis.getName().equals(graphAnalysisH.getName())) {
            anGridBag.setConstraints(resultTextAreaX, gbc);
            analysisSetupPanel.add(resultTextAreaX);
        }
        if (graphAnalysis.getName().equals(graphAnalysisV.getName())) {
            anGridBag.setConstraints(resultTextAreaY, gbc);
            analysisSetupPanel.add(resultTextAreaY);
        }
        JSplitPane graphSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        graphSplitPane.setOneTouchExpandable(true);
        graphSplitPane.setResizeWeight(0.9);
        graphSplitPane.add(graphAnalysis);
        graphSplitPane.add(analysisSetupPanel);
        return graphSplitPane;
    }

    /** update the lists for the BPMs and correctors choices
     */
    protected void updateSelectionLists() {
        BPM1List.setListData(theDoc.theBPMs.toArray());
        BPM1List.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        BPM2List.setListData(theDoc.theBPMs.toArray());
        BPM2List.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        BPM3List.setListData(theDoc.theBPMs.toArray());
        BPM3List.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        correctorHList.setListData(theDoc.theCorrectorHs.toArray());
        correctorHList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        correctorVList.setListData(theDoc.theCorrectorVs.toArray());
        correctorVList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        BCMList.setListData(theDoc.theBCMs.toArray());
        BCMList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    }

    /** method to handle the selection of the BPMs, Correctors and Quadrupole to use */
    protected void setAccelComponents() {
        BPM BPM1 = (BPM) BPM1List.getSelectedValue();
        Object[] BPM2Obj = BPM2List.getSelectedValues();
        BPM[] BPM2 = new BPM[BPM2Obj.length];
        for (int i = 0; i < BPM2Obj.length; i++) {
            BPM2[i] = (BPM) BPM2Obj[i];
        }
        BPM BPM3 = (BPM) BPM3List.getSelectedValue();
        HDipoleCorr hDipoleCorr = (HDipoleCorr) correctorHList.getSelectedValue();
        VDipoleCorr vDipoleCorr = (VDipoleCorr) correctorVList.getSelectedValue();
        CurrentMonitor bcm = (CurrentMonitor) BCMList.getSelectedValue();
        if (BPM1 == null || (hDipoleCorr == null && vDipoleCorr == null)) {
            Toolkit.getDefaultToolkit().beep();
            errorText.setText("Hey - you need to select both BPMs and the corrector to use.");
            return;
        }
        System.out.println("To find the center of: " + BPM1);
        for (int i = 0; i < BPM2.length; i++) {
            if (BPM2[i] == null) {
                Toolkit.getDefaultToolkit().beep();
                errorText.setText("Oohs - you do not select BPM2.");
                return;
            }
        }
        if (BPM3 == null) {
            Toolkit.getDefaultToolkit().beep();
            JOptionPane.showMessageDialog(theDoc.myWindow(), "The upstream BPM will be not used.");
        }
        for (int i = 0; i < BPM2.length; i++) {
            if (BPM1 == BPM2[i] || (BPM3 != null && (BPM3 == BPM1 || BPM3 == BPM2[i]))) {
                Toolkit.getDefaultToolkit().beep();
                errorText.setText("Hey - you need to select unique BPMs");
                return;
            }
        }
        for (int i = 0; i < BPM2.length; i++) {
            if (theDoc.getSelectedSequence().getPosition(BPM2[i]) < theDoc.getSelectedSequence().getPosition(BPM1)) {
                Toolkit.getDefaultToolkit().beep();
                errorText.setText("Hey - all BPM2 should be downstream of the BPM1");
                return;
            }
        }
        if (BPM3 != null && theDoc.getSelectedSequence().getPosition(BPM1) < theDoc.getSelectedSequence().getPosition(BPM3)) {
            Toolkit.getDefaultToolkit().beep();
            errorText.setText("Hey - the BPM3 should be upstream of the BPM1");
            return;
        }
        if (hDipoleCorr != null) {
            if (theDoc.getSelectedSequence().getPosition(BPM1) < theDoc.getSelectedSequence().getPosition(hDipoleCorr)) {
                Toolkit.getDefaultToolkit().beep();
                errorText.setText("Hey - the BPM1 must be downstream of the corrector");
                return;
            }
            System.out.println("Horizontal Dipole Corrector: " + hDipoleCorr);
        }
        if (vDipoleCorr != null) {
            if (theDoc.getSelectedSequence().getPosition(BPM1) < theDoc.getSelectedSequence().getPosition(vDipoleCorr)) {
                Toolkit.getDefaultToolkit().beep();
                errorText.setText("Hey - the BPM1 must be downstream of the corrector");
                return;
            }
            System.out.println("Vertical Dipole Corrector: " + vDipoleCorr);
        }
        if (bcm != null) {
            System.out.println("Beam Current Monitor: " + bcm);
        }
        theDoc.BPM1 = BPM1;
        theDoc.BPM2 = BPM2;
        theDoc.BPM3 = BPM3;
        theDoc.correctorH = hDipoleCorr;
        theDoc.correctorV = vDipoleCorr;
        theDoc.theBCM = bcm;
        Electromagnet selectedQuad = getNearstQuadrupole(BPM1, theDoc.getSelectedSequence());
        if (selectedQuad == null) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        theDoc.selectedQuadMagnet = selectedQuad;
        selectedQuadNameText.setText("Use Quad: " + selectedQuad.getId());
        errorText.setText("");
        theDoc.scanStuff.init();
        if (hDipoleCorr != null && vDipoleCorr != null) {
            theDoc.scanStuff.updateScanVariables(BPM1, BPM2, BPM3, hDipoleCorr, vDipoleCorr, selectedQuad, bcm);
        } else {
            if (hDipoleCorr != null) theDoc.scanStuff.updateScanVariables(BPM1, BPM2, BPM3, hDipoleCorr, null, selectedQuad, bcm);
            if (vDipoleCorr != null) theDoc.scanStuff.updateScanVariables(BPM1, BPM2, BPM3, null, vDipoleCorr, selectedQuad, bcm);
        }
        System.out.println("Devices set OK");
        pvListTextAreaX.setText(theDoc.scanStuff.thePVTextX);
        pvListTextAreaY.setText(theDoc.scanStuff.thePVTextY);
        try {
            theDoc.analysisStuff.theModel = Scenario.newScenarioFor(theDoc.theSequence);
            System.out.println("Model set for " + theDoc.theSequence.getId());
        } catch (Exception ex) {
            Toolkit.getDefaultToolkit().beep();
            String errText = "Darn! I couldn't set up the model for your selected sequence";
            errorText.setText(errText);
            System.err.println(errText);
            ex.printStackTrace();
            return;
        }
        theDoc.setHasChanges(true);
    }

    /** get nearest quadrupole (both Quadrupole and PermQuadrupole) around specified BPM within 1.0 meter.
     * @param bpm1 the quadrupole should be around the bpm1
     * @param selectedSequence find the quadrupole in this sequence
     * @return quadrupole or null if quadrupole is not found
     */
    private Electromagnet getNearstQuadrupole(BPM bpm1, AcceleratorSeq selectedSequence) {
        List<AcceleratorNode> quadsList = selectedSequence.getNodesOfType(Quadrupole.s_strType, true);
        Electromagnet selectedQuad = null;
        if (!quadsList.isEmpty()) {
            selectedSequence.sortNodesByProximity(quadsList, bpm1);
            selectedQuad = (Electromagnet) quadsList.get(0);
        } else {
            errorText.setText("Error!  No matching quadrupole found within selected sequence. Exiting...");
            return null;
        }
        if (Math.abs(selectedSequence.getPosition(selectedQuad) - selectedSequence.getPosition(bpm1)) < 1.0) {
            System.out.println("Quadrupole to vary: " + selectedQuad);
            return selectedQuad;
        }
        errorText.setText("Error!  No matching quadrupole found within 1.0 m.  Exiting...");
        return null;
    }

    protected void plotSteer2Slope(FunctionGraphsJPanel graphAnalysis, MeasuredValue bpm1PosMV, MeasuredValue[] bpm2PosMV, MeasuredValue bpm3PosMV) {
        if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisH.getName())) {
            if (theDoc.analysisStuff.analysisDataXReady) {
                graphAnalysis.removeAllGraphData();
                BasicGraphData bgd, bgd2;
                for (int j = 0; j < theDoc.analysisStuff.downstreamBPMSlopeX.length; j++) {
                    bgd = new BasicGraphData();
                    bgd2 = new BasicGraphData();
                    ArrayList fittedArray = (ArrayList) theDoc.analysisStuff.stFieldXFitted.get(j);
                    for (int i = 0; i < theDoc.analysisStuff.centerBPMPosAvgX.size(); i++) {
                        bgd.addPoint(((double[]) theDoc.analysisStuff.downstreamBPMSlopeX[j].get(i))[0], (Double) (theDoc.analysisStuff.setStValVX.get(i)));
                        bgd2.addPoint(((double[]) theDoc.analysisStuff.downstreamBPMSlopeX[j].get(i))[0], (Double) (fittedArray.get(i)));
                    }
                    bgd.setDrawLinesOn(false);
                    bgd.setDrawPointsOn(true);
                    bgd.setGraphProperty("Legend", "Slope of downstream BPM" + theDoc.BPM2[j].getId());
                    bgd.setGraphColor(IncrementalColor.getColor(j));
                    graphAnalysis.addGraphData(bgd);
                    bgd2.setDrawLinesOn(true);
                    bgd2.setDrawPointsOn(false);
                    bgd2.setGraphProperty("Legend", "Fitted Slope of downstream BPM" + theDoc.BPM2[j].getId());
                    bgd2.setGraphColor(IncrementalColor.getColor(j));
                    graphAnalysis.addGraphData(bgd2);
                }
                graphAnalysis.setAxisNames("Slope of Quad-BPMs", "Steer Setting Field (T*M)");
                graphAnalysis.refreshGraphJPanel();
            } else {
                JOptionPane.showMessageDialog(theDoc.myWindow(), "The measured data is not imported yet.\n" + "Please import all measured data by clicking the \"Import scan data\" button on analysis panel.");
            }
        }
        if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisV.getName())) {
            if (theDoc.analysisStuff.analysisDataYReady) {
                graphAnalysis.removeAllGraphData();
                BasicGraphData bgd, bgd2;
                for (int j = 0; j < theDoc.analysisStuff.downstreamBPMSlopeY.length; j++) {
                    bgd = new BasicGraphData();
                    bgd2 = new BasicGraphData();
                    ArrayList fittedArray = (ArrayList) theDoc.analysisStuff.stFieldYFitted.get(j);
                    for (int i = 0; i < theDoc.analysisStuff.centerBPMPosAvgY.size(); i++) {
                        bgd.addPoint(((double[]) theDoc.analysisStuff.downstreamBPMSlopeY[j].get(i))[0], (Double) (theDoc.analysisStuff.setStValVY.get(i)));
                        bgd2.addPoint(((double[]) theDoc.analysisStuff.downstreamBPMSlopeY[j].get(i))[0], (Double) (fittedArray.get(i)));
                    }
                    bgd.setDrawLinesOn(false);
                    bgd.setDrawPointsOn(true);
                    bgd.setGraphProperty("Legend", "Slope of downstream BPM" + theDoc.BPM2[j].getId());
                    bgd.setGraphColor(IncrementalColor.getColor(j));
                    graphAnalysis.addGraphData(bgd);
                    bgd2.setDrawLinesOn(true);
                    bgd2.setDrawPointsOn(false);
                    bgd2.setGraphProperty("Legend", "Fitted Slope of downstream BPM" + theDoc.BPM2[j].getId());
                    bgd2.setGraphColor(IncrementalColor.getColor(j));
                    graphAnalysis.addGraphData(bgd2);
                }
                graphAnalysis.setAxisNames("Slope of Quad-BPMs", "Steer Setting Field (T*M)");
                graphAnalysis.refreshGraphJPanel();
            } else {
                JOptionPane.showMessageDialog(theDoc.myWindow(), "The measured data is not imported yet.\n" + "Please import all measured data by clicking the \"Import scan data\" button.");
            }
        }
    }

    /** plot the measured data to the graphPanel
     *  any existing data is removed 
     */
    protected void plotMeasuredData(FunctionGraphsJPanel graphAnalysis, MeasuredValue bpm1PosMV, MeasuredValue[] bpm2PosMV, MeasuredValue bpm3PosMV) {
        if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisH.getName())) {
            if (theDoc.analysisStuff.analysisDataXReady) {
                if (plotBpm2QuadFlagX) {
                    graphAnalysis.removeAllGraphData();
                    for (int i = 0; i < bpm1PosMV.getNumberOfDataContainers(); i++) {
                        graphAnalysis.addGraphData(bpm1PosMV.getDataContainer(i));
                    }
                    for (int j = 0; j < bpm2PosMV.length; j++) {
                        for (int i = 0; i < bpm1PosMV.getNumberOfDataContainers(); i++) graphAnalysis.addGraphData(bpm2PosMV[j].getDataContainer(i));
                    }
                    if (theDoc.BPM3 != null) {
                        for (int i = 0; i < bpm1PosMV.getNumberOfDataContainers(); i++) graphAnalysis.addGraphData(bpm3PosMV.getDataContainer(i));
                    }
                    graphAnalysis.setAxisNames("Quad Strength (Tesla)", "BPM measured Value (mm)");
                    graphAnalysis.refreshGraphJPanel();
                } else {
                    graphAnalysis.removeAllGraphData();
                    BasicGraphData bgd, bgd2;
                    for (int j = 0; j < theDoc.analysisStuff.downstreamBPMSlopeX.length; j++) {
                        bgd = new BasicGraphData();
                        bgd2 = new BasicGraphData();
                        ArrayList fittedArray = (ArrayList) theDoc.analysisStuff.bpmCenterPosXFitted.get(j);
                        for (int i = 0; i < theDoc.analysisStuff.centerBPMPosAvgX.size(); i++) {
                            bgd.addPoint(((double[]) theDoc.analysisStuff.downstreamBPMSlopeX[j].get(i))[0], (Double) (theDoc.analysisStuff.centerBPMPosAvgX.get(i)), (Double) (theDoc.analysisStuff.centerBPMPosAvgSigmaX.get(i)));
                            bgd2.addPoint(((double[]) theDoc.analysisStuff.downstreamBPMSlopeX[j].get(i))[0], (Double) (fittedArray.get(i)));
                        }
                        bgd.setDrawLinesOn(false);
                        bgd.setDrawPointsOn(true);
                        bgd.setGraphProperty("Legend", "Slope of downstream BPM" + theDoc.BPM2[j].getId());
                        bgd.setGraphColor(IncrementalColor.getColor(j));
                        graphAnalysis.addGraphData(bgd);
                        bgd2.setDrawLinesOn(true);
                        bgd2.setDrawPointsOn(false);
                        bgd2.setGraphProperty("Legend", "Fitted Slope of downstream BPM" + theDoc.BPM2[j].getId());
                        bgd2.setGraphColor(IncrementalColor.getColor(j));
                        graphAnalysis.addGraphData(bgd2);
                    }
                    graphAnalysis.setAxisNames("Slope of Quad-BPMs", "BPM measured Value (mm)");
                    graphAnalysis.refreshGraphJPanel();
                }
            } else {
                JOptionPane.showMessageDialog(theDoc.myWindow(), "The measured data is not imported yet.\n" + "Please import all measured data by clicking the \"Import scan data\" button on analysis panel.");
            }
        }
        if (graphAnalysis.getName().equals(theDoc.myWindow().graphAnalysisV.getName())) {
            if (theDoc.analysisStuff.analysisDataYReady) {
                if (plotBpm2QuadFlagY) {
                    graphAnalysis.removeAllGraphData();
                    for (int i = 0; i < bpm1PosMV.getNumberOfDataContainers(); i++) {
                        graphAnalysis.addGraphData(bpm1PosMV.getDataContainer(i));
                    }
                    for (int j = 0; j < bpm2PosMV.length; j++) {
                        for (int i = 0; i < bpm1PosMV.getNumberOfDataContainers(); i++) graphAnalysis.addGraphData(bpm2PosMV[j].getDataContainer(i));
                    }
                    if (theDoc.BPM3 != null) {
                        for (int i = 0; i < bpm1PosMV.getNumberOfDataContainers(); i++) graphAnalysis.addGraphData(bpm3PosMV.getDataContainer(i));
                    }
                    graphAnalysis.setAxisNames("Quad Strength (Tesla)", "BPM measured Value (mm)");
                    graphAnalysis.refreshGraphJPanel();
                } else {
                    graphAnalysis.removeAllGraphData();
                    BasicGraphData bgd, bgd2;
                    for (int j = 0; j < theDoc.analysisStuff.downstreamBPMSlopeY.length; j++) {
                        bgd = new BasicGraphData();
                        bgd2 = new BasicGraphData();
                        ArrayList fittedArray = (ArrayList) theDoc.analysisStuff.bpmCenterPosYFitted.get(j);
                        for (int i = 0; i < theDoc.analysisStuff.centerBPMPosAvgY.size(); i++) {
                            bgd.addPoint(((double[]) theDoc.analysisStuff.downstreamBPMSlopeY[j].get(i))[0], (Double) (theDoc.analysisStuff.centerBPMPosAvgY.get(i)), (Double) (theDoc.analysisStuff.centerBPMPosAvgSigmaY.get(i)));
                            bgd2.addPoint(((double[]) theDoc.analysisStuff.downstreamBPMSlopeY[j].get(i))[0], (Double) (fittedArray.get(i)));
                        }
                        bgd.setDrawLinesOn(false);
                        bgd.setDrawPointsOn(true);
                        bgd.setGraphProperty("Legend", "Slope of downstream BPM" + theDoc.BPM2[j].getId());
                        bgd.setGraphColor(IncrementalColor.getColor(j));
                        graphAnalysis.addGraphData(bgd);
                        bgd2.setDrawLinesOn(true);
                        bgd2.setDrawPointsOn(false);
                        bgd2.setGraphProperty("Legend", "Fitted Slope of downstream BPM" + theDoc.BPM2[j].getId());
                        bgd2.setGraphColor(IncrementalColor.getColor(j));
                        graphAnalysis.addGraphData(bgd2);
                    }
                    graphAnalysis.setAxisNames("Slope of Quad-BPMs", "BPM measured Value (mm)");
                    graphAnalysis.refreshGraphJPanel();
                }
            } else {
                JOptionPane.showMessageDialog(theDoc.myWindow(), "The measured data is not imported yet.\n" + "Please import all measured data by clicking the \"Import scan data\" button.");
            }
        }
    }
}
