package gov.sns.apps.scan.dblscanmebt;

import java.awt.event.*;
import javax.swing.*;
import java.awt.*;
import javax.swing.border.*;
import java.util.*;
import java.text.*;
import java.io.*;
import gov.sns.tools.plot.*;
import gov.sns.tools.scan.*;
import gov.sns.ca.*;

/**
 * The panel with GUI for double scan - voltage + phase
 *
 * @author  shishlo
 * @version 1.0
 */
public class CavityAmplAndPhaseScan extends JPanel {

    static {
        ChannelFactory.defaultFactory().init();
    }

    private JPanel leftControlAndAnalysisPanel = new JPanel();

    private JPanel leftControlPanel = new JPanel();

    private JPanel leftAnalysisPanel = new JPanel();

    private JPanel graphPanel = new JPanel();

    private IndependentValueRange voltageControlPanel = new IndependentValueRange("voltage control");

    private IndependentValueRange phaseControlPanel = new IndependentValueRange("phase control");

    private FunctionGraphsJPanel bpmPhaseGraph = new FunctionGraphsJPanel();

    private AveragingController avgControl = new AveragingController(5, 0.2);

    private ValuatorLimitsManager thresholdControl = new ValuatorLimitsManager(0.0, 1.0E+38);

    private JComboBox cavityChooserListC = null;

    private JComboBox bpmChooserListC = null;

    private JComboBox setReadBackChooserListC = null;

    private JComboBox cavityChooserListA = null;

    private JComboBox bpmChooserListA = null;

    private JComboBox setReadBackChooserListA = null;

    private int bpmNumber = 6;

    private int cavityNumber = 4;

    private String[] setReadBackList = { "Cavity's Set Phase", "Cavity's Read Back Phase" };

    private JButton findIntersectButton = null;

    private JRadioButton splineIntersectButton = null;

    private JRadioButton linerIntersectButton = null;

    private Vector interpolated_GD_V = new Vector();

    private JTextField cavSoughtPhaseText = null;

    private JTextField cavSoughtPhaseSpreadText = null;

    private DecimalFormat cavSoughtPhaseFormat = new DecimalFormat("###.#");

    private DecimalFormat cavSoughtPhaseSpreadFormat = new DecimalFormat("###.##");

    private JButton setFoundPhaseButton = null;

    private JButton getCurrentAnalButton = null;

    private JTextField cavPhaseSetText = null;

    private JTextField cavPhaseRBText = null;

    private JTextField cavAmpSetText = null;

    private JTextField cavAmpRBText = null;

    private DecimalFormat ampFormat = new DecimalFormat("##.###");

    private DecimalFormat phaseFormat = new DecimalFormat("###.#");

    private JButton removeGraphButton = null;

    private JTextField cavAmpSetChoosenGraphText = null;

    private JTextField cavAmpRBChoosenGraphText = null;

    private int indexOfChoosenGraph = -1;

    private JButton analysisOnButton = null;

    private JButton controlOnButton = null;

    private boolean ControlStateOn = true;

    private String[] bpmAmpNames = { "MEBT_Diag:BPM01:amplitudeAvg", "MEBT_Diag:BPM04:amplitudeAvg", "MEBT_Diag:BPM05:amplitudeAvg", "MEBT_Diag:BPM10:amplitudeAvg", "MEBT_Diag:BPM11:amplitudeAvg", "MEBT_Diag:BPM14:amplitudeAvg" };

    private String[] bpmPhaseNames = { "MEBT_Diag:BPM01:phaseAvg", "MEBT_Diag:BPM04:phaseAvg", "MEBT_Diag:BPM05:phaseAvg", "MEBT_Diag:BPM10:phaseAvg", "MEBT_Diag:BPM11:phaseAvg", "MEBT_Diag:BPM14:phaseAvg" };

    private String[] cavPhaseSetNames = { "MEBT_LLRF:FCM1:CtlPhaseSet", "MEBT_LLRF:FCM2:CtlPhaseSet", "MEBT_LLRF:FCM3:CtlPhaseSet", "MEBT_LLRF:FCM4:CtlPhaseSet" };

    private String[] cavPhaseRbNames = { "MEBT_LLRF:FCM1:cavPhaseAvg", "MEBT_LLRF:FCM2:cavPhaseAvg", "MEBT_LLRF:FCM3:cavPhaseAvg", "MEBT_LLRF:FCM4:cavPhaseAvg" };

    private String[] cavAmpSetNames = { "MEBT_LLRF:FCM1:CtlAmpSet", "MEBT_LLRF:FCM2:CtlAmpSet", "MEBT_LLRF:FCM3:CtlAmpSet", "MEBT_LLRF:FCM4:CtlAmpSet" };

    private String[] cavAmpRbNames = { "MEBT_LLRF:FCM1:cavAmpAvg", "MEBT_LLRF:FCM2:cavAmpAvg", "MEBT_LLRF:FCM3:cavAmpAvg", "MEBT_LLRF:FCM4:cavAmpAvg" };

    private String[] bpmList = { " BPM01 ", " BPM04 ", " BPM05 ", " BPM10 ", " BPM11 ", " BPM14 " };

    private Color[] bpmColors = { Color.red, Color.green, Color.blue, Color.yellow, Color.pink, Color.cyan };

    private String[] cavityList = { " Cavity#1 ", " Cavity#2 ", " Cavity#3 ", " Cavity#4 " };

    private int cavityIndex = 0;

    private int bpmIndex = 0;

    private boolean[] maskCavPhaseSetRB = { true, false };

    private IndependentValue[][] independentVars = null;

    private MeasuredValue[][] bpmPhasesMeasAll = null;

    private Valuator mainValuator = new Valuator();

    private Valuator[] valuators = null;

    private MeasuredValues[] measCont = null;

    private ScanStopper scanStopper = ScanStopper.getScanStopper();

    private File dataFile = null;

    public CavityAmplAndPhaseScan() {
        super();
        setFont(new Font(getFont().getFamily(), Font.BOLD, 10));
    }

    public void setPVsNames(String[] bpmList, Color[] bpmColors, String[] bpmAmpNames, String[] bpmPhaseNames, String[] cavityList, String[] cavPhaseSetNames, String[] cavPhaseRbNames, String[] cavAmpSetNames, String[] cavAmpRbNames) {
        if (bpmList != null && bpmAmpNames != null && bpmPhaseNames != null && cavityList != null && cavPhaseSetNames != null && cavPhaseRbNames != null && cavAmpSetNames != null && cavAmpRbNames != null && bpmList.length == bpmAmpNames.length && bpmAmpNames.length == bpmPhaseNames.length && cavityList.length == cavPhaseSetNames.length && cavPhaseSetNames.length == cavPhaseRbNames.length && cavPhaseRbNames.length == cavAmpSetNames.length && cavAmpSetNames.length == cavAmpRbNames.length) {
            if (bpmColors == null || bpmColors.length != bpmList.length) {
                bpmColors = new Color[bpmList.length];
                for (int i = 0; i < bpmColors.length; i++) {
                    bpmColors[i] = getBackground();
                }
            }
            this.bpmList = bpmList;
            this.bpmColors = bpmColors;
            this.bpmAmpNames = bpmAmpNames;
            this.bpmPhaseNames = bpmPhaseNames;
            this.cavityList = cavityList;
            this.cavPhaseSetNames = cavPhaseSetNames;
            this.cavPhaseRbNames = cavPhaseRbNames;
            this.cavAmpSetNames = cavAmpSetNames;
            this.cavAmpRbNames = cavAmpRbNames;
            initialize();
        }
    }

    public void setPVsNames() {
        initialize();
    }

    private void initialize() {
        scanStopper.setOwner(this);
        leftControlAndAnalysisPanel.setLayout(new BorderLayout());
        createLeftControlPanel();
        createLeftAnalysisPanel();
        createGraphPanel();
        setLayout(new BorderLayout());
        JPanel tmp_0 = new JPanel();
        tmp_0.setLayout(new BorderLayout());
        tmp_0.add(leftControlAndAnalysisPanel, BorderLayout.NORTH);
        add(tmp_0, BorderLayout.WEST);
        add(graphPanel, BorderLayout.CENTER);
        setConnections();
        ControlStateOn = true;
        activateLeftControlPanel();
    }

    private void activateLeftControlPanel() {
        leftControlAndAnalysisPanel.removeAll();
        leftControlAndAnalysisPanel.add(leftControlPanel, BorderLayout.CENTER);
        bpmPhaseGraph.clearZoomStack();
        bpmPhaseGraph.setVerLinesButtonVisible(false);
        bpmPhaseGraph.setChooseModeButtonVisible(false);
        validate();
        repaint();
        updateGraphContent();
    }

    private void activateLeftAnalysisPanel() {
        leftControlAndAnalysisPanel.removeAll();
        leftControlAndAnalysisPanel.add(leftAnalysisPanel, BorderLayout.CENTER);
        bpmPhaseGraph.clearZoomStack();
        bpmPhaseGraph.setVerLinesButtonVisible(true);
        bpmPhaseGraph.setChooseModeButtonVisible(true);
        validate();
        repaint();
        updateGraphContent();
    }

    private void createLeftControlPanel() {
        Border etchedBorder = BorderFactory.createEtchedBorder();
        leftControlPanel.setLayout(new BorderLayout());
        leftControlPanel.setFont(getFont());
        leftControlPanel.setBorder(BorderFactory.createTitledBorder(etchedBorder, "SCAN CONTROL"));
        leftControlPanel.setBackground(leftControlPanel.getBackground().darker());
        cavityChooserListC = new JComboBox(cavityList);
        cavityChooserListC.setFont(getFont());
        cavityIndex = 0;
        cavityChooserListC.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (scanStopper.getScanStopper().isRunning()) {
                    scanStopper.getScanStopper().warning("Can not change the cavity # during the scan.");
                    cavityChooserListC.setSelectedIndex(cavityIndex);
                    cavityChooserListA.setSelectedIndex(cavityIndex);
                    return;
                }
                cavityIndex = cavityChooserListC.getSelectedIndex();
                cavityChooserListA.setSelectedIndex(cavityIndex);
                updateContent();
            }
        });
        bpmChooserListC = new JComboBox(bpmList);
        bpmChooserListC.setFont(getFont());
        bpmIndex = 0;
        bpmChooserListC.setBackground(bpmColors[0]);
        bpmChooserListC.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                bpmIndex = bpmChooserListC.getSelectedIndex();
                bpmChooserListC.setBackground(bpmColors[bpmIndex]);
                bpmChooserListA.setSelectedIndex(bpmIndex);
                bpmChooserListA.setBackground(bpmColors[bpmIndex]);
                updateGraphContent();
            }
        });
        setReadBackChooserListC = new JComboBox(setReadBackList);
        setReadBackChooserListC.setFont(getFont());
        maskCavPhaseSetRB[0] = true;
        maskCavPhaseSetRB[1] = false;
        setReadBackChooserListC.setSelectedIndex(0);
        setReadBackChooserListC.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int index = setReadBackChooserListC.getSelectedIndex();
                setReadBackChooserListA.setSelectedIndex(index);
                if (index == 0) {
                    maskCavPhaseSetRB[0] = true;
                    maskCavPhaseSetRB[1] = false;
                } else {
                    maskCavPhaseSetRB[1] = true;
                    maskCavPhaseSetRB[0] = false;
                }
                updateGraphContent();
            }
        });
        phaseControlPanel.dimensionText.setText("[deg]");
        voltageControlPanel.dimensionText.setText("[kV]");
        phaseControlPanel.limFormat = new DecimalFormat("##0.0");
        phaseControlPanel.stepFormat = new DecimalFormat("#0.0");
        phaseControlPanel.valueFormat = phaseControlPanel.limFormat;
        phaseControlPanel.sleepTimeFormat = new DecimalFormat("#0.0#");
        voltageControlPanel.limFormat = new DecimalFormat("##0.0##");
        voltageControlPanel.stepFormat = new DecimalFormat("##0.0##");
        voltageControlPanel.valueFormat = new DecimalFormat("##0.0##");
        voltageControlPanel.sleepTimeFormat = phaseControlPanel.sleepTimeFormat;
        phaseControlPanel.setCurrentValueRB(0.0);
        phaseControlPanel.setCurrentValue(0.0);
        phaseControlPanel.setLowLimit(-180.0);
        phaseControlPanel.setUppLimit(180.0);
        phaseControlPanel.setStep(5.0);
        phaseControlPanel.setSleepTime(0.5);
        voltageControlPanel.setCurrentValueRB(0.0);
        voltageControlPanel.setCurrentValue(0.0);
        voltageControlPanel.setLowLimit(0.0);
        voltageControlPanel.setUppLimit(100.0);
        voltageControlPanel.setStep(5.0);
        voltageControlPanel.setSleepTime(0.5);
        phaseControlPanel.addButtonPanel();
        voltageControlPanel.setChildRange(phaseControlPanel);
        thresholdControl.limFormat.applyPattern("#0.0#");
        thresholdControl.lowLimText.setColumns(5);
        thresholdControl.setLowLim(0.0);
        analysisOnButton = new JButton("GO TO ANALYSIS");
        analysisOnButton.setFont(getFont());
        ControlStateOn = true;
        analysisOnButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (scanStopper.getScanStopper().isRunning()) {
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }
                ControlStateOn = false;
                activateLeftAnalysisPanel();
            }
        });
        JPanel tmp_1_1 = new JPanel();
        tmp_1_1.setLayout(new BorderLayout());
        tmp_1_1.add(cavityChooserListC, BorderLayout.NORTH);
        tmp_1_1.add(bpmChooserListC, BorderLayout.SOUTH);
        JPanel tmp_1 = new JPanel();
        tmp_1.setLayout(new BorderLayout());
        tmp_1.add(tmp_1_1, BorderLayout.NORTH);
        tmp_1.add(setReadBackChooserListC, BorderLayout.SOUTH);
        JPanel tmp_2 = new JPanel();
        tmp_2.setLayout(new BorderLayout());
        tmp_2.add(voltageControlPanel, BorderLayout.NORTH);
        tmp_2.add(phaseControlPanel, BorderLayout.SOUTH);
        JPanel tmp_3 = new JPanel();
        tmp_3.setLayout(new BorderLayout());
        tmp_3.add(avgControl.getJPanel(0), BorderLayout.NORTH);
        tmp_3.add(thresholdControl.getJPanel(0), BorderLayout.SOUTH);
        JPanel tmp_0 = new JPanel();
        tmp_0.setLayout(new BorderLayout());
        tmp_0.add(tmp_1, BorderLayout.NORTH);
        tmp_0.add(tmp_2, BorderLayout.SOUTH);
        JPanel tmp_0_0 = new JPanel();
        tmp_0_0.setLayout(new BorderLayout());
        tmp_0_0.add(tmp_0, BorderLayout.NORTH);
        tmp_0_0.add(tmp_3, BorderLayout.SOUTH);
        JPanel tmp_0_1 = new JPanel();
        tmp_0_1.setLayout(new BorderLayout());
        tmp_0_1.add(tmp_0_0, BorderLayout.NORTH);
        tmp_0_1.add(analysisOnButton, BorderLayout.SOUTH);
        leftControlPanel.add(tmp_0_1, BorderLayout.NORTH);
    }

    private void createLeftAnalysisPanel() {
        Border etchedBorder = BorderFactory.createEtchedBorder();
        leftAnalysisPanel.setLayout(new BorderLayout());
        leftAnalysisPanel.setFont(getFont());
        leftAnalysisPanel.setBorder(BorderFactory.createTitledBorder(etchedBorder, "SCAN ANALYSIS"));
        leftAnalysisPanel.setBackground(leftControlPanel.getBackground().darker());
        cavityChooserListA = new JComboBox(cavityList);
        cavityChooserListA.setFont(getFont());
        cavityIndex = 0;
        cavityChooserListA.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (scanStopper.getScanStopper().isRunning()) {
                    scanStopper.getScanStopper().warning("Can not change the cavity # during the scan.");
                    cavityChooserListA.setSelectedIndex(cavityIndex);
                    cavityChooserListC.setSelectedIndex(cavityIndex);
                    return;
                }
                cavityIndex = cavityChooserListA.getSelectedIndex();
                cavityChooserListC.setSelectedIndex(cavityIndex);
                updateContent();
            }
        });
        bpmChooserListA = new JComboBox(bpmList);
        bpmChooserListA.setFont(getFont());
        bpmIndex = 0;
        bpmChooserListA.setBackground(bpmColors[0]);
        bpmChooserListA.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                bpmIndex = bpmChooserListA.getSelectedIndex();
                bpmChooserListA.setBackground(bpmColors[bpmIndex]);
                bpmChooserListC.setSelectedIndex(bpmIndex);
                bpmChooserListC.setBackground(bpmColors[bpmIndex]);
                updateGraphContent();
            }
        });
        setReadBackChooserListA = new JComboBox(setReadBackList);
        setReadBackChooserListA.setFont(getFont());
        maskCavPhaseSetRB[0] = true;
        maskCavPhaseSetRB[1] = false;
        setReadBackChooserListA.setSelectedIndex(0);
        setReadBackChooserListA.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int index = setReadBackChooserListA.getSelectedIndex();
                setReadBackChooserListC.setSelectedIndex(index);
                if (index == 0) {
                    maskCavPhaseSetRB[0] = true;
                    maskCavPhaseSetRB[1] = false;
                } else {
                    maskCavPhaseSetRB[1] = true;
                    maskCavPhaseSetRB[0] = false;
                }
                updateGraphContent();
            }
        });
        splineIntersectButton = new JRadioButton("spline");
        linerIntersectButton = new JRadioButton("linear");
        splineIntersectButton.setFont(getFont());
        linerIntersectButton.setFont(getFont());
        ButtonGroup bgrp0 = new ButtonGroup();
        bgrp0.add(splineIntersectButton);
        bgrp0.add(linerIntersectButton);
        splineIntersectButton.setSelected(true);
        findIntersectButton = new JButton("FIND INTERSECTION");
        findIntersectButton.setHorizontalTextPosition(SwingConstants.CENTER);
        findIntersectButton.setFont(getFont());
        findIntersectButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                bpmPhaseGraph.removeGraphData(interpolated_GD_V);
                interpolated_GD_V.clear();
                Vector gdV = bpmPhaseGraph.getAllGraphData();
                if (gdV.size() <= 0) {
                    cavSoughtPhaseText.setText(null);
                    cavSoughtPhaseSpreadText.setText(null);
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }
                double xMin = bpmPhaseGraph.getCurrentMinX();
                double xMax = bpmPhaseGraph.getCurrentMaxX();
                double yMin = bpmPhaseGraph.getCurrentMinY();
                double yMax = bpmPhaseGraph.getCurrentMaxY();
                Vector interpGD_V = null;
                if (splineIntersectButton.isSelected()) {
                    interpGD_V = bpmPhaseGraph.getAllGraphData();
                } else {
                    interpGD_V = new Vector();
                    Vector GD_V = bpmPhaseGraph.getAllGraphData();
                    for (int i = 0; i < GD_V.size(); i++) {
                        BasicGraphData interpGD = new BasicGraphData();
                        BasicGraphData GD = (BasicGraphData) GD_V.get(i);
                        GraphDataOperations.polynomialFit(GD, interpGD, xMin, xMax, 1, 0);
                        interpGD.setDrawPointsOn(false);
                        interpGD_V.add(interpGD);
                        interpolated_GD_V.add(interpGD);
                    }
                    bpmPhaseGraph.addGraphData(interpGD_V);
                }
                Double[] intersectV = GraphDataOperations.findIntersection(interpGD_V, xMin, xMax, yMin, yMax, 0.001);
                if (intersectV[0] != null) {
                    bpmPhaseGraph.setVerticalLineValue(intersectV[0].doubleValue(), 0);
                    cavSoughtPhaseText.setText(null);
                    cavSoughtPhaseText.setText(cavSoughtPhaseFormat.format(intersectV[0].doubleValue()));
                    cavSoughtPhaseSpreadText.setText(null);
                    cavSoughtPhaseSpreadText.setText(cavSoughtPhaseSpreadFormat.format(intersectV[2].doubleValue()));
                } else {
                    cavSoughtPhaseText.setText(null);
                    cavSoughtPhaseSpreadText.setText(null);
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        });
        JLabel intersectPositionLabel = new JLabel("found cavity phase [degr]:", JLabel.CENTER);
        intersectPositionLabel.setFont(getFont());
        JLabel intersectPositionPhaseLabel = new JLabel("phase =  ", JLabel.RIGHT);
        intersectPositionPhaseLabel.setFont(getFont());
        JLabel intersectPositionSpreadLabel = new JLabel("spread =  ", JLabel.RIGHT);
        intersectPositionSpreadLabel.setFont(getFont());
        cavSoughtPhaseText = new JTextField(8);
        cavSoughtPhaseText.setText(null);
        cavSoughtPhaseText.setFont(getFont());
        cavSoughtPhaseText.setHorizontalAlignment(JTextField.CENTER);
        cavSoughtPhaseText.setEditable(true);
        cavSoughtPhaseText.setBackground(Color.white);
        cavSoughtPhaseSpreadText = new JTextField(8);
        cavSoughtPhaseSpreadText.setText(null);
        cavSoughtPhaseSpreadText.setFont(getFont());
        cavSoughtPhaseSpreadText.setHorizontalAlignment(JTextField.CENTER);
        cavSoughtPhaseSpreadText.setEditable(true);
        cavSoughtPhaseSpreadText.setBackground(Color.white);
        setFoundPhaseButton = new JButton("SET THE FOUND PHASE TO CAVITY");
        setFoundPhaseButton.setHorizontalTextPosition(SwingConstants.CENTER);
        setFoundPhaseButton.setFont(getFont());
        setFoundPhaseButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                double d_val = 0.0;
                try {
                    d_val = Double.parseDouble(cavSoughtPhaseText.getText());
                } catch (NumberFormatException exc) {
                    cavSoughtPhaseText.setText(null);
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }
                bpmPhaseGraph.setVerticalLineValue(d_val, 0);
                independentVars[0][cavityIndex].setValue(d_val);
            }
        });
        JLabel phaseSetRBLabel = new JLabel("Cavity phase (Set/Read Back) [degr]:", JLabel.LEFT);
        phaseSetRBLabel.setFont(getFont());
        JLabel ampSetRBLabel = new JLabel("Cavity ampl. (Set/Read Back) [kV]:", JLabel.LEFT);
        ampSetRBLabel.setFont(getFont());
        cavPhaseSetText = new JTextField(8);
        cavPhaseSetText.setText(null);
        cavPhaseSetText.setFont(getFont());
        cavPhaseSetText.setHorizontalAlignment(JTextField.CENTER);
        cavPhaseSetText.setEditable(false);
        cavPhaseRBText = new JTextField(8);
        cavPhaseRBText.setText(null);
        cavPhaseRBText.setFont(getFont());
        cavPhaseRBText.setHorizontalAlignment(JTextField.CENTER);
        cavPhaseRBText.setEditable(false);
        cavAmpSetText = new JTextField(8);
        cavAmpSetText.setText(null);
        cavAmpSetText.setFont(getFont());
        cavAmpSetText.setHorizontalAlignment(JTextField.CENTER);
        cavAmpSetText.setEditable(false);
        cavAmpRBText = new JTextField(8);
        cavAmpRBText.setText(null);
        cavAmpRBText.setFont(getFont());
        cavAmpRBText.setHorizontalAlignment(JTextField.CENTER);
        cavAmpRBText.setEditable(false);
        getCurrentAnalButton = new JButton("READ FROM CAVITY");
        getCurrentAnalButton.setHorizontalTextPosition(SwingConstants.CENTER);
        getCurrentAnalButton.setFont(getFont());
        getCurrentAnalButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                independentVars[0][cavityIndex].measure();
                independentVars[1][cavityIndex].measure();
                cavPhaseSetText.setText(null);
                cavPhaseRBText.setText(null);
                cavAmpSetText.setText(null);
                cavAmpRBText.setText(null);
                cavPhaseSetText.setText(phaseFormat.format(independentVars[0][cavityIndex].getCurrentValue()));
                cavAmpSetText.setText(ampFormat.format(independentVars[1][cavityIndex].getCurrentValue()));
                cavPhaseRBText.setText(phaseFormat.format(independentVars[0][cavityIndex].getCurrentValue()));
                cavAmpRBText.setText(ampFormat.format(independentVars[1][cavityIndex].getCurrentValue()));
            }
        });
        removeGraphButton = new JButton("REMOVE SELECTED GRAPH");
        removeGraphButton.setHorizontalTextPosition(SwingConstants.CENTER);
        removeGraphButton.setFont(getFont());
        removeGraphButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                FunctionGraphsJPanel gp = bpmPhaseGraph;
                Integer Ind = gp.getGraphChosenIndex();
                if (Ind != null && Ind.intValue() >= 0) {
                    int ind = Ind.intValue();
                    MeasuredValue currMV = bpmPhasesMeasAll[cavityIndex][bpmIndex];
                    currMV.removeDataContainer(ind);
                    currMV.removeDataContainerRB(ind);
                    updateGraphContent();
                    cavAmpSetChoosenGraphText.setText(null);
                    cavAmpRBChoosenGraphText.setText(null);
                    gp.setChoosingGraphMode();
                    return;
                }
                Toolkit.getDefaultToolkit().beep();
            }
        });
        JLabel ampSetRBChoosenGraphLabel = new JLabel("Cavity ampl. (Set/Read Back) [kV]:", JLabel.LEFT);
        ampSetRBChoosenGraphLabel.setFont(getFont());
        cavAmpSetChoosenGraphText = new JTextField(8);
        cavAmpSetChoosenGraphText.setText(null);
        cavAmpSetChoosenGraphText.setFont(getFont());
        cavAmpSetChoosenGraphText.setHorizontalAlignment(JTextField.CENTER);
        cavAmpSetChoosenGraphText.setEditable(false);
        cavAmpRBChoosenGraphText = new JTextField(8);
        cavAmpRBChoosenGraphText.setText(null);
        cavAmpRBChoosenGraphText.setFont(getFont());
        cavAmpRBChoosenGraphText.setHorizontalAlignment(JTextField.CENTER);
        cavAmpRBChoosenGraphText.setEditable(false);
        controlOnButton = new JButton("RETURN TO SCAN CONTROL");
        controlOnButton.setFont(getFont());
        controlOnButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (scanStopper.getScanStopper().isRunning()) {
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }
                ControlStateOn = true;
                activateLeftControlPanel();
            }
        });
        JPanel tmp_1 = new JPanel();
        tmp_1.setLayout(new BorderLayout());
        tmp_1.add(cavityChooserListA, BorderLayout.NORTH);
        tmp_1.add(bpmChooserListA, BorderLayout.SOUTH);
        JPanel tmp_2 = new JPanel();
        tmp_2.setLayout(new BorderLayout());
        tmp_2.add(tmp_1, BorderLayout.NORTH);
        tmp_2.add(setReadBackChooserListA, BorderLayout.SOUTH);
        JPanel tmp_3 = new JPanel();
        tmp_3.setBorder(BorderFactory.createTitledBorder(etchedBorder, "find and set cavity phase"));
        tmp_3.setBackground(tmp_3.getBackground().darker());
        tmp_3.setLayout(new BorderLayout());
        JPanel tmp_3_0 = new JPanel();
        tmp_3_0.setLayout(new BorderLayout());
        tmp_3_0.add(intersectPositionLabel, BorderLayout.NORTH);
        JPanel tmp_3_1 = new JPanel();
        tmp_3_1.setLayout(new GridLayout(0, 2));
        tmp_3_1.add(intersectPositionPhaseLabel);
        tmp_3_1.add(cavSoughtPhaseText);
        tmp_3_1.add(intersectPositionSpreadLabel);
        tmp_3_1.add(cavSoughtPhaseSpreadText);
        tmp_3_0.add(tmp_3_1, BorderLayout.SOUTH);
        JPanel tmp_3_2 = new JPanel();
        tmp_3_2.setLayout(new BorderLayout());
        tmp_3_2.add(splineIntersectButton, BorderLayout.WEST);
        tmp_3_2.add(linerIntersectButton, BorderLayout.EAST);
        tmp_3_2.add(tmp_3_0, BorderLayout.SOUTH);
        tmp_3.add(findIntersectButton, BorderLayout.NORTH);
        tmp_3.add(tmp_3_2, BorderLayout.CENTER);
        tmp_3.add(setFoundPhaseButton, BorderLayout.SOUTH);
        JPanel tmp_4 = new JPanel();
        tmp_4.setBorder(BorderFactory.createTitledBorder(etchedBorder, "read from the cavity"));
        tmp_4.setBackground(tmp_4.getBackground().darker());
        tmp_4.setLayout(new BorderLayout());
        JPanel tmp_4_1 = new JPanel();
        tmp_4_1.setLayout(new BorderLayout());
        tmp_4_1.add(ampSetRBLabel, BorderLayout.NORTH);
        tmp_4_1.add(cavAmpSetText, BorderLayout.WEST);
        tmp_4_1.add(cavAmpRBText, BorderLayout.EAST);
        JPanel tmp_4_2 = new JPanel();
        tmp_4_2.setLayout(new BorderLayout());
        tmp_4_2.add(phaseSetRBLabel, BorderLayout.NORTH);
        tmp_4_2.add(cavPhaseSetText, BorderLayout.WEST);
        tmp_4_2.add(cavPhaseRBText, BorderLayout.EAST);
        tmp_4_1.add(tmp_4_2, BorderLayout.SOUTH);
        tmp_4.add(getCurrentAnalButton, BorderLayout.NORTH);
        tmp_4.add(tmp_4_1, BorderLayout.SOUTH);
        JPanel tmp_5 = new JPanel();
        tmp_5.setBorder(BorderFactory.createTitledBorder(etchedBorder, "removing selected graph"));
        tmp_5.setBackground(tmp_5.getBackground().darker());
        tmp_5.setLayout(new BorderLayout());
        JPanel tmp_5_1 = new JPanel();
        tmp_5_1.setLayout(new BorderLayout());
        tmp_5_1.add(ampSetRBChoosenGraphLabel, BorderLayout.NORTH);
        tmp_5_1.add(cavAmpSetChoosenGraphText, BorderLayout.WEST);
        tmp_5_1.add(cavAmpRBChoosenGraphText, BorderLayout.EAST);
        tmp_5.add(removeGraphButton, BorderLayout.NORTH);
        tmp_5.add(tmp_5_1, BorderLayout.SOUTH);
        JPanel tmp_0_0 = new JPanel();
        tmp_0_0.setLayout(new BorderLayout());
        tmp_0_0.add(tmp_2, BorderLayout.NORTH);
        tmp_0_0.add(tmp_3, BorderLayout.SOUTH);
        JPanel tmp_0_1 = new JPanel();
        tmp_0_1.setLayout(new BorderLayout());
        tmp_0_1.add(tmp_0_0, BorderLayout.NORTH);
        tmp_0_1.add(tmp_4, BorderLayout.SOUTH);
        JPanel tmp_0 = new JPanel();
        tmp_0.setLayout(new BorderLayout());
        tmp_0.add(tmp_0_1, BorderLayout.NORTH);
        tmp_0.add(tmp_5, BorderLayout.SOUTH);
        leftAnalysisPanel.add(tmp_0, BorderLayout.NORTH);
        leftAnalysisPanel.add(controlOnButton, BorderLayout.SOUTH);
    }

    private void createGraphPanel() {
        Border etchedBorder = BorderFactory.createEtchedBorder();
        JPanel tmp_0 = new JPanel();
        tmp_0.setLayout(new BorderLayout());
        tmp_0.setFont(getFont());
        tmp_0.setBorder(BorderFactory.createTitledBorder(etchedBorder, "bpm's phase [degr]"));
        tmp_0.add(bpmPhaseGraph, BorderLayout.CENTER);
        bpmPhaseGraph.setChooseModeButtonVisible(false);
        bpmPhaseGraph.setHorLinesButtonVisible(false);
        bpmPhaseGraph.setVerLinesButtonVisible(true);
        Font graphFont = new Font(getFont().getFamily(), Font.BOLD, 9);
        bpmPhaseGraph.setAxisNameFontX(graphFont);
        bpmPhaseGraph.setAxisNameFontY(graphFont);
        bpmPhaseGraph.setNumberFont(graphFont);
        GridLimits bpmPhaseGraph_GLforPhaseScan = bpmPhaseGraph.getNewGridLimits();
        bpmPhaseGraph_GLforPhaseScan.setNumberFormatX(new DecimalFormat("###.0"));
        bpmPhaseGraph_GLforPhaseScan.setNumberFormatY(new DecimalFormat("####"));
        bpmPhaseGraph_GLforPhaseScan.setLimitsAndTicksX(-180., 180., 60., 5);
        bpmPhaseGraph_GLforPhaseScan.setLimitsAndTicksY(0., 360., 30., 2);
        bpmPhaseGraph.setExternalGL(bpmPhaseGraph_GLforPhaseScan);
        bpmPhaseGraph.getExternalGL().setYminOn(false);
        bpmPhaseGraph.getExternalGL().setYmaxOn(false);
        bpmPhaseGraph.setAxisNameX("Phase of a Cavity [degr]");
        bpmPhaseGraph.setOffScreenImageDrawing(true);
        bpmPhaseGraph.addVerticalLine(-180.0, Color.black);
        bpmPhaseGraph.setDraggedVerLinesMotionListen(true);
        bpmPhaseGraph.addDraggedVerLinesListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                double val = ((FunctionGraphsJPanel) e.getSource()).getVerticalValue(0);
                cavSoughtPhaseText.setText(null);
                cavSoughtPhaseText.setText(cavSoughtPhaseFormat.format(val));
            }
        });
        JButton clearCurrentButton = new JButton("CLEAR CURR. DATA");
        JButton clearCurrentCavityButton = new JButton("CLEAR CURR. CAVITY DATA");
        JButton clearAllButton = new JButton("CLEAR ALL DATA");
        clearCurrentButton.setHorizontalTextPosition(SwingConstants.CENTER);
        clearCurrentCavityButton.setHorizontalTextPosition(SwingConstants.CENTER);
        clearAllButton.setHorizontalTextPosition(SwingConstants.CENTER);
        clearCurrentButton.setFont(getFont());
        clearCurrentCavityButton.setFont(getFont());
        clearAllButton.setFont(getFont());
        clearCurrentButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (scanStopper.getScanStopper().isRunning()) {
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }
                clearCurrentGraphData();
            }
        });
        clearCurrentCavityButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (scanStopper.getScanStopper().isRunning()) {
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }
                clearCurrCavityGraphData();
            }
        });
        clearAllButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (scanStopper.getScanStopper().isRunning()) {
                    Toolkit.getDefaultToolkit().beep();
                    return;
                }
                clearAllGraphData();
            }
        });
        bpmPhaseGraph.addChooseListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                FunctionGraphsJPanel gp = (FunctionGraphsJPanel) e.getSource();
                Integer Ind = gp.getGraphChosenIndex();
                if (Ind != null && Ind.intValue() >= 0) {
                    int ind = Ind.intValue();
                    MeasuredValue currMV = bpmPhasesMeasAll[cavityIndex][bpmIndex];
                    BasicGraphData gd = currMV.getDataContainer(ind);
                    if (gd != null) {
                        Double amp = (Double) gd.getGraphProperty(cavAmpSetNames[cavityIndex]);
                        if (amp != null) {
                            cavAmpSetChoosenGraphText.setText(null);
                            cavAmpSetChoosenGraphText.setText(ampFormat.format(amp.doubleValue()));
                        }
                        amp = (Double) gd.getGraphProperty(cavAmpRbNames[cavityIndex]);
                        if (amp != null) {
                            cavAmpRBChoosenGraphText.setText(null);
                            cavAmpRBChoosenGraphText.setText(ampFormat.format(amp.doubleValue()));
                        }
                        return;
                    }
                }
                cavAmpSetChoosenGraphText.setText(null);
                cavAmpRBChoosenGraphText.setText(null);
            }
        });
        JPanel tmp_1 = new JPanel();
        tmp_1.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tmp_1.setFont(getFont());
        tmp_1.add(clearCurrentButton);
        tmp_1.add(clearCurrentCavityButton);
        tmp_1.add(clearAllButton);
        graphPanel.setLayout(new BorderLayout());
        graphPanel.setFont(getFont());
        graphPanel.add(tmp_0, BorderLayout.CENTER);
        graphPanel.add(tmp_1, BorderLayout.SOUTH);
    }

    private void updateGraphContent() {
        MeasuredValue currMV = bpmPhasesMeasAll[cavityIndex][bpmIndex];
        bpmPhaseGraph.removeAllGraphData();
        bpmPhaseGraph.setDisplayGraphMode();
        if (maskCavPhaseSetRB[0]) {
            int nGD = currMV.getNumberOfDataContainers();
            for (int i = 0; i < nGD; i++) {
                if (bpmPhaseGraph.getGraphBackGroundColor() != bpmColors[bpmIndex]) {
                    currMV.getUnwrappedDataContainer(i).setGraphColor(bpmColors[bpmIndex]);
                }
            }
            bpmPhaseGraph.addGraphData(currMV.getUnwrappedDataContainers());
        }
        if (maskCavPhaseSetRB[1]) {
            int nGD_RB = currMV.getNumberOfDataContainersRB();
            for (int i = 0; i < nGD_RB; i++) {
                if (bpmPhaseGraph.getGraphBackGroundColor() != bpmColors[bpmIndex]) {
                    currMV.getUnwrappedDataContainerRB(i).setGraphColor(bpmColors[bpmIndex]);
                }
            }
            bpmPhaseGraph.addGraphData(currMV.getUnwrappedDataContainersRB());
        }
        bpmPhaseGraph.clearZoomStack();
        cavSoughtPhaseText.setText(null);
        cavSoughtPhaseSpreadText.setText(null);
        cavPhaseSetText.setText(null);
        cavPhaseRBText.setText(null);
        cavAmpSetText.setText(null);
        cavAmpRBText.setText(null);
        cavAmpSetChoosenGraphText.setText(null);
        cavAmpRBChoosenGraphText.setText(null);
    }

    private void clearCurrentGraphData() {
        bpmPhasesMeasAll[cavityIndex][bpmIndex].removeAllDataContainers();
        updateGraphContent();
    }

    private void clearCurrCavityGraphData() {
        for (int j = 0; j < bpmNumber; j++) {
            bpmPhasesMeasAll[cavityIndex][j].removeAllDataContainers();
        }
        updateGraphContent();
    }

    private void clearAllGraphData() {
        for (int i = 0; i < cavityNumber; i++) {
            for (int j = 0; j < bpmNumber; j++) {
                bpmPhasesMeasAll[i][j].removeAllDataContainers();
            }
        }
        updateGraphContent();
    }

    private void updateContent() {
        voltageControlPanel.registerIndependentValue(independentVars[1][cavityIndex]);
        phaseControlPanel.registerIndependentValue(independentVars[0][cavityIndex]);
        phaseControlPanel.registerMeasurer(measCont[cavityIndex]);
        updateGraphContent();
    }

    private void setConnections() {
        bpmNumber = bpmAmpNames.length;
        cavityNumber = cavPhaseSetNames.length;
        if (bpmPhaseNames.length != bpmNumber || cavPhaseRbNames.length != cavityNumber || cavAmpSetNames.length != cavityNumber) {
            System.out.println("==============================================");
            System.out.println("Check the input parameter for PV names arrays.");
            System.out.println("Stop.");
            System.exit(0);
        }
        independentVars = new IndependentValue[2][cavityNumber];
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < cavityNumber; j++) {
                independentVars[i][j] = new IndependentValue();
                if (i == 0) independentVars[i][j].setChannelName(cavPhaseSetNames[j]);
                if (i == 0) independentVars[i][j].setChannelNameRB(cavPhaseRbNames[j]);
                if (i == 1) independentVars[i][j].setChannelName(cavAmpSetNames[j]);
                if (i == 1) independentVars[i][j].setChannelNameRB(cavAmpRbNames[j]);
                independentVars[i][j].valueFormat.applyPattern("##0.0##");
            }
        }
        bpmPhasesMeasAll = new MeasuredValue[cavityNumber][bpmNumber];
        for (int i = 0; i < cavityNumber; i++) {
            for (int j = 0; j < bpmNumber; j++) {
                bpmPhasesMeasAll[i][j] = new MeasuredValue();
                bpmPhasesMeasAll[i][j].setChannelName(bpmPhaseNames[j]);
                bpmPhasesMeasAll[i][j].generateUnwrappedData(true);
            }
        }
        valuators = new Valuator[bpmNumber];
        for (int i = 0; i < bpmNumber; i++) {
            valuators[i] = new Valuator();
            valuators[i].setChannelName(bpmAmpNames[i]);
            valuators[i].setLimitsManager(thresholdControl);
            mainValuator.addExternalValuator(valuators[i]);
        }
        measCont = new MeasuredValues[cavityNumber];
        for (int i = 0; i < cavityNumber; i++) {
            measCont[i] = new MeasuredValues();
            measCont[i].setAvrgCntrl(avgControl);
            measCont[i].setValuator(mainValuator);
        }
        for (int i = 0; i < cavityNumber; i++) {
            measCont[i].addNewSetOfMeasurementsListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    updateGraphContent();
                }
            });
        }
        for (int i = 0; i < cavityNumber; i++) {
            for (int j = 0; j < bpmNumber; j++) {
                measCont[i].addMeasuredValueInstance(bpmPhasesMeasAll[i][j]);
            }
        }
        cavityIndex = 0;
        bpmIndex = 0;
        cavityChooserListC.setSelectedIndex(cavityIndex);
        bpmChooserListC.setSelectedIndex(bpmIndex);
        bpmChooserListC.setBackground(bpmColors[bpmIndex]);
        cavityChooserListA.setSelectedIndex(cavityIndex);
        bpmChooserListA.setSelectedIndex(bpmIndex);
        bpmChooserListA.setBackground(bpmColors[bpmIndex]);
        maskCavPhaseSetRB[0] = true;
        maskCavPhaseSetRB[1] = false;
        updateContent();
    }

    public void writeData() {
        if (scanStopper.getScanStopper().isRunning()) {
            scanStopper.getScanStopper().warning("Can not write data during the scan.");
            return;
        }
        if (bpmNumber == 0 || cavityNumber == 0) return;
        JFileChooser ch = new JFileChooser();
        ch.setDialogTitle("Write Data");
        if (dataFile != null) {
            ch.setSelectedFile(dataFile);
        }
        int returnVal = ch.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                dataFile = ch.getSelectedFile();
                BufferedWriter out = new BufferedWriter(new FileWriter(dataFile));
                out.write("%=======double (voltage+phase) cavity scan data  ===");
                out.newLine();
                out.write(" " + cavityNumber + " -numbers of cavity");
                out.newLine();
                out.write(" " + bpmNumber + " -numbers of bpm");
                out.newLine();
                for (int i = 0; i < cavityNumber; i++) {
                    for (int j = 0; j < bpmNumber; j++) {
                        out.write("%====new  set of graphs for cavity N=" + i + " and bpm N=" + j);
                        out.newLine();
                        int nDataCont = bpmPhasesMeasAll[i][j].getNumberOfDataContainers();
                        BasicGraphData gd;
                        BasicGraphData gdRB;
                        out.write(" " + nDataCont + " -number of data containers");
                        out.newLine();
                        for (int k = 0; k < nDataCont; k++) {
                            out.write("%-----------new graph data-------------------------");
                            out.newLine();
                            gd = bpmPhasesMeasAll[i][j].getDataContainer(k);
                            gdRB = bpmPhasesMeasAll[i][j].getDataContainerRB(k);
                            int nPoints = gd.getNumbOfPoints();
                            out.write("    " + nPoints + " -number of data points");
                            out.newLine();
                            Double cavAmp = (Double) gd.getGraphProperty(cavAmpSetNames[i]);
                            out.write(cavAmpSetNames[i] + "  " + cavAmp + " -cavity voltage (Set) for this data set");
                            out.newLine();
                            Double cavAmpRB = (Double) gd.getGraphProperty(cavAmpRbNames[i]);
                            out.write(cavAmpRbNames[i] + "  " + cavAmpRB + " -cavity voltage (Read Back) for this data set");
                            out.newLine();
                            for (int l = 0; l < nPoints; l++) {
                                out.write(" " + gd.getX(l) + " " + gd.getY(l) + " " + gd.getErr(l));
                                out.newLine();
                            }
                            out.write("%   now read back data----------------------");
                            out.newLine();
                            for (int l = 0; l < nPoints; l++) {
                                out.write(" " + gdRB.getX(l) + " " + gdRB.getY(l) + " " + gdRB.getErr(l));
                                out.newLine();
                            }
                        }
                    }
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

    public void readData() {
        if (scanStopper.getScanStopper().isRunning()) {
            scanStopper.getScanStopper().warning("Can not write data during the scan.");
            return;
        }
        if (bpmNumber == 0 || cavityNumber == 0) return;
        JFileChooser ch = new JFileChooser();
        ch.setDialogTitle("Read Data");
        if (dataFile != null) {
            ch.setSelectedFile(dataFile);
        }
        int returnVal = ch.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            try {
                dataFile = ch.getSelectedFile();
                BufferedReader in = new BufferedReader(new FileReader(dataFile));
                String inLine;
                inLine = in.readLine();
                inLine = in.readLine();
                StringTokenizer st = new StringTokenizer(inLine);
                int cavityNumberIn = Integer.parseInt(st.nextToken());
                inLine = in.readLine();
                st = new StringTokenizer(inLine);
                int bpmNumberIn = Integer.parseInt(st.nextToken());
                if (cavityNumberIn == cavityNumber && bpmNumberIn == bpmNumber) {
                    for (int i = 0; i < cavityNumber; i++) {
                        for (int j = 0; j < bpmNumberIn; j++) {
                            inLine = in.readLine();
                            inLine = in.readLine();
                            st = new StringTokenizer(inLine);
                            int nDataCont = Integer.parseInt(st.nextToken());
                            for (int k = 0; k < nDataCont; k++) {
                                inLine = in.readLine();
                                inLine = in.readLine();
                                st = new StringTokenizer(inLine);
                                int nPoints = Integer.parseInt(st.nextToken());
                                inLine = in.readLine();
                                st = new StringTokenizer(inLine);
                                String cavAmpSetCh = st.nextToken();
                                double cavAmp = Double.parseDouble(st.nextToken());
                                inLine = in.readLine();
                                st = new StringTokenizer(inLine);
                                String cavAmpSetChRB = st.nextToken();
                                double cavAmpRB = Double.parseDouble(st.nextToken());
                                bpmPhasesMeasAll[i][j].createNewDataContainer();
                                bpmPhasesMeasAll[i][j].createNewDataContainerRB();
                                int indCont = bpmPhasesMeasAll[i][j].getNumberOfDataContainers() - 1;
                                bpmPhasesMeasAll[i][j].getDataContainer(indCont).setGraphProperty(cavAmpSetCh, new Double(cavAmp));
                                bpmPhasesMeasAll[i][j].getDataContainer(indCont).setGraphProperty(cavAmpSetChRB, new Double(cavAmpRB));
                                for (int l = 0; l < nPoints; l++) {
                                    inLine = in.readLine();
                                    st = new StringTokenizer(inLine);
                                    double x = Double.parseDouble(st.nextToken());
                                    double y = Double.parseDouble(st.nextToken());
                                    double err = Double.parseDouble(st.nextToken());
                                    bpmPhasesMeasAll[i][j].getDataContainer(indCont).addPoint(x, y, err);
                                }
                                inLine = in.readLine();
                                for (int l = 0; l < nPoints; l++) {
                                    inLine = in.readLine();
                                    st = new StringTokenizer(inLine);
                                    double x = Double.parseDouble(st.nextToken());
                                    double y = Double.parseDouble(st.nextToken());
                                    double err = Double.parseDouble(st.nextToken());
                                    bpmPhasesMeasAll[i][j].getDataContainerRB(indCont).addPoint(x, y, err);
                                }
                            }
                        }
                    }
                } else {
                    Toolkit.getDefaultToolkit().beep();
                }
                updateGraphContent();
                in.close();
            } catch (IOException e) {
                Toolkit.getDefaultToolkit().beep();
                System.out.println(e.toString());
            }
        }
    }

    public static void main(String args[]) {
        String[] bpmAmpNames = { "MEBT_Diag:BPM01:amplitudeAvg", "MEBT_Diag:BPM04:amplitudeAvg", "MEBT_Diag:BPM05:amplitudeAvg", "MEBT_Diag:BPM10:amplitudeAvg", "MEBT_Diag:BPM11:amplitudeAvg", "MEBT_Diag:BPM14:amplitudeAvg" };
        String[] bpmPhaseNames = { "MEBT_Diag:BPM01:phaseAvg", "MEBT_Diag:BPM04:phaseAvg", "MEBT_Diag:BPM05:phaseAvg", "MEBT_Diag:BPM10:phaseAvg", "MEBT_Diag:BPM11:phaseAvg", "MEBT_Diag:BPM14:phaseAvg" };
        String[] cavPhaseSetNames = { "MEBT_LLRF:FCM1:CtlPhaseSet", "MEBT_LLRF:FCM2:CtlPhaseSet", "MEBT_LLRF:FCM3:CtlPhaseSet", "MEBT_LLRF:FCM4:CtlPhaseSet" };
        String[] cavPhaseRbNames = { "MEBT_LLRF:FCM1:cavPhaseAvg", "MEBT_LLRF:FCM2:cavPhaseAvg", "MEBT_LLRF:FCM3:cavPhaseAvg", "MEBT_LLRF:FCM4:cavPhaseAvg" };
        String[] cavAmpSetNames = { "MEBT_LLRF:FCM1:CtlAmpSet", "MEBT_LLRF:FCM2:CtlAmpSet", "MEBT_LLRF:FCM3:CtlAmpSet", "MEBT_LLRF:FCM4:CtlAmpSet" };
        String[] cavAmpRbNames = { "MEBT_LLRF:FCM1:cavAmpAvg", "MEBT_LLRF:FCM2:cavAmpAvg", "MEBT_LLRF:FCM3:cavAmpAvg", "MEBT_LLRF:FCM4:cavAmpAvg" };
        String[] bpmList = { " BPM01 ", " BPM04 ", " BPM05 ", " BPM10 ", " BPM11 ", " BPM14 " };
        Color[] bpmColors = { Color.red, Color.green, Color.blue, Color.yellow, Color.pink, Color.cyan };
        String[] cavityList = { " Cavity#1 ", " Cavity#2 ", " Cavity#3 ", " Cavity#4 " };
        JFrame mainFrame = new JFrame("MEBT voltage+phase scan");
        mainFrame.getContentPane().setLayout(new BorderLayout());
        mainFrame.addWindowListener(new java.awt.event.WindowAdapter() {

            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                System.exit(0);
            }
        });
        final CavityAmplAndPhaseScan scanner = new CavityAmplAndPhaseScan();
        scanner.setPVsNames(bpmList, bpmColors, bpmAmpNames, bpmPhaseNames, cavityList, cavPhaseSetNames, cavPhaseRbNames, cavAmpSetNames, cavAmpRbNames);
        mainFrame.getContentPane().add(scanner, BorderLayout.CENTER);
        JMenuBar menu = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem readDataMenu = new JMenuItem("Open data");
        JMenuItem saveDataMenu = new JMenuItem("Save data");
        JMenuItem saveAsDataMenu = new JMenuItem("Save data As");
        JMenuItem exitMenu = new JMenuItem("Exit");
        fileMenu.add(readDataMenu);
        fileMenu.add(saveDataMenu);
        fileMenu.add(saveAsDataMenu);
        fileMenu.addSeparator();
        fileMenu.add(exitMenu);
        menu.add(fileMenu);
        mainFrame.setJMenuBar(menu);
        exitMenu.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        saveDataMenu.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                scanner.writeData();
            }
        });
        saveAsDataMenu.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                scanner.writeData();
            }
        });
        readDataMenu.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                scanner.readData();
            }
        });
        mainFrame.pack();
        mainFrame.show();
    }
}
