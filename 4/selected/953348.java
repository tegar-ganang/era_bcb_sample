package gov.sns.apps.rtbtwizard;

import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.text.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import Jama.*;
import gov.sns.ca.*;
import gov.sns.xal.smf.*;
import gov.sns.xal.smf.impl.*;
import gov.sns.tools.plot.*;
import gov.sns.tools.messaging.MessageCenter;
import gov.sns.tools.statistics.RunningWeightedStatistics;
import gov.sns.tools.text.FormattedNumber;
import gov.sns.xal.model.alg.*;
import gov.sns.xal.model.probe.*;
import gov.sns.xal.model.probe.traj.*;
import gov.sns.xal.model.scenario.*;
import gov.sns.xal.model.tools.OrbitMatcher;
import gov.sns.xal.model.pvlogger.PVLoggerDataSource;

/** view providing an alternative beam position measurement based on matching measurements from multiple BPMs */
public class BeamOrbitFace {

    /** document */
    protected final GenDocument DOCUMENT;

    /** archive view */
    protected final Component FACE_VIEW;

    /** BPM agents */
    protected final List<BpmAgent> BPM_AGENTS;

    /** table model of BPMs */
    protected final BPMTableModel BPM_TABLE_MODEL;

    /** RTBT sequence */
    protected final AcceleratorSeq RTBT_SEQUENCE;

    /** matches the target beam position to the measured BPM beam positions */
    protected TargetBeamPositionMatcher _beamPositionMatcher;

    /** target plot */
    protected TargetPlot TARGET_PLOT;

    /** Constructor */
    public BeamOrbitFace(final GenDocument document) {
        DOCUMENT = document;
        BPM_AGENTS = getAvailableBpmAgents(document);
        BPM_TABLE_MODEL = new BPMTableModel(BPM_AGENTS);
        TARGET_PLOT = new TargetPlot(6.0, 4.0);
        RTBT_SEQUENCE = DOCUMENT.getAccelerator().findSequence("RTBT2");
        useFieldSetpoint();
        FACE_VIEW = makeView();
    }

    /** use the field setpoints for the online model analysis otherwise the model produces ridiculous answers */
    protected void useFieldSetpoint() {
        final List<AcceleratorNode> magnets = RTBT_SEQUENCE.getNodesOfType(Electromagnet.s_strType, true);
        for (final AcceleratorNode magnet : magnets) {
            if (magnet instanceof Electromagnet) {
                ((Electromagnet) magnet).setUseFieldReadback(false);
            }
        }
    }

    /** get the available BPM agents */
    private static List<BpmAgent> getAvailableBpmAgents(final GenDocument document) {
        final List<BpmAgent> allBPMAgents = (List<BpmAgent>) document.getBPMAgents();
        final List<BpmAgent> bpmAgents = new ArrayList<BpmAgent>(allBPMAgents.size());
        for (final BpmAgent bpmAgent : allBPMAgents) {
            final String bpmID = bpmAgent.getNode().getId();
            if (!bpmID.equals("RTBT_Diag:BPM16") && !bpmID.equals("RTBT_Diag:BPM22")) {
                bpmAgents.add(bpmAgent);
            }
        }
        return bpmAgents;
    }

    /** get this controller's view */
    public Component getView() {
        return FACE_VIEW;
    }

    /** get the main window */
    protected JFrame getWindow() {
        return (JFrame) SwingUtilities.windowForComponent(FACE_VIEW);
    }

    /** make the view for this face */
    protected Component makeView() {
        final Box mainBox = new Box(BoxLayout.Y_AXIS);
        final JTable bpmTable = new JTable(BPM_TABLE_MODEL);
        final JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(bpmTable), TARGET_PLOT.getPlotPanel());
        final Box mainSplitBox = new Box(BoxLayout.X_AXIS);
        mainSplitBox.add(mainSplitPane);
        mainBox.add(mainSplitBox);
        mainSplitPane.setOneTouchExpandable(true);
        mainSplitPane.addAncestorListener(new AncestorListener() {

            public void ancestorAdded(final AncestorEvent event) {
                if (mainSplitPane.getClientProperty("initialized") == null) {
                    mainSplitPane.setDividerLocation(0.4);
                    mainSplitPane.putClientProperty("initialized", true);
                }
            }

            public void ancestorRemoved(final AncestorEvent event) {
            }

            public void ancestorMoved(final AncestorEvent event) {
            }
        });
        final Box targetBox = new Box(BoxLayout.X_AXIS);
        mainBox.add(targetBox);
        targetBox.setBorder(BorderFactory.createTitledBorder("Target Beam Position"));
        final java.text.NumberFormat TARGET_NUMBER_FORMAT = new DecimalFormat("#,##0.0");
        final int NUMBER_WIDTH = 6;
        targetBox.add(new JLabel("X (mm):"));
        final JFormattedTextField xTargetField = new JFormattedTextField(TARGET_NUMBER_FORMAT);
        xTargetField.setToolTipText("Matching Target horizontal beam position");
        xTargetField.setEditable(false);
        xTargetField.setHorizontalAlignment(JTextField.RIGHT);
        xTargetField.setColumns(NUMBER_WIDTH);
        xTargetField.setMaximumSize(xTargetField.getPreferredSize());
        targetBox.add(xTargetField);
        targetBox.add(new JLabel("+/-"));
        final JFormattedTextField xTargetErrorField = new JFormattedTextField(TARGET_NUMBER_FORMAT);
        xTargetErrorField.setToolTipText("Estimated horizontal target beam position error.");
        xTargetErrorField.setEditable(false);
        xTargetErrorField.setHorizontalAlignment(JTextField.RIGHT);
        xTargetErrorField.setColumns(NUMBER_WIDTH);
        xTargetErrorField.setMaximumSize(xTargetErrorField.getPreferredSize());
        targetBox.add(xTargetErrorField);
        targetBox.add(Box.createHorizontalGlue());
        targetBox.add(new JLabel("Y (mm):"));
        final JFormattedTextField yTargetField = new JFormattedTextField(TARGET_NUMBER_FORMAT);
        yTargetField.setToolTipText("Matching Target vertical beam position");
        yTargetField.setEditable(false);
        yTargetField.setHorizontalAlignment(JTextField.RIGHT);
        yTargetField.setColumns(NUMBER_WIDTH);
        yTargetField.setMaximumSize(yTargetField.getPreferredSize());
        targetBox.add(yTargetField);
        targetBox.add(new JLabel("+/-"));
        final JFormattedTextField yTargetErrorField = new JFormattedTextField(TARGET_NUMBER_FORMAT);
        yTargetErrorField.setToolTipText("Estimated vertical target beam position error.");
        yTargetErrorField.setEditable(false);
        yTargetErrorField.setHorizontalAlignment(JTextField.RIGHT);
        yTargetErrorField.setColumns(NUMBER_WIDTH);
        yTargetErrorField.setMaximumSize(yTargetErrorField.getPreferredSize());
        targetBox.add(yTargetErrorField);
        targetBox.add(Box.createHorizontalGlue());
        final JButton clearButton = new JButton("Clear");
        clearButton.setToolTipText("Clear results and running average BPM statistics.");
        targetBox.add(clearButton);
        clearButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                TARGET_PLOT.clear();
                clearBeamPositionRunningAverage();
                xTargetField.setValue(null);
                xTargetErrorField.setValue(null);
                yTargetField.setValue(null);
                yTargetErrorField.setValue(null);
            }
        });
        final JButton startButton = new JButton("Start");
        startButton.setToolTipText("Start Beam Position Running Average after clearing it.");
        targetBox.add(startButton);
        startButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                for (final BpmAgent bpmAgent : BPM_AGENTS) {
                    bpmAgent.startAveraging();
                }
            }
        });
        final JButton stopButton = new JButton("Stop");
        stopButton.setToolTipText("Stop Beam Position Running Average.");
        targetBox.add(stopButton);
        stopButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                stopRunningAverage();
            }
        });
        final JButton targetMatchButton = new JButton("Match");
        targetMatchButton.setToolTipText("<html><body>Stop averaging the BPM beam position.<br>Match the target position to the BPM beam position data.</body></html>");
        targetBox.add(targetMatchButton);
        targetMatchButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                try {
                    stopButton.doClick();
                    final List<BpmAgent> enabledBpmAgents = BPM_TABLE_MODEL.getEnabledBpmAgents();
                    final List<BpmAgent> bpmAgents = new ArrayList<BpmAgent>(enabledBpmAgents.size());
                    for (final BpmAgent bpmAgent : enabledBpmAgents) {
                        if (bpmAgent.hasRunningAverageSamples()) {
                            bpmAgents.add(bpmAgent);
                        }
                    }
                    if (bpmAgents.size() < 2) {
                        DOCUMENT.displayError("Matching Error", "You must choose at least 2 BPMs to perform the matching analysis.");
                        return;
                    }
                    if (_beamPositionMatcher == null) {
                        _beamPositionMatcher = new TargetBeamPositionMatcher(RTBT_SEQUENCE);
                    }
                    final BeamPosition targetBeamPosition = _beamPositionMatcher.getMatchingTargetBeamPosition(bpmAgents);
                    xTargetField.setValue(targetBeamPosition.X);
                    xTargetErrorField.setValue(targetBeamPosition.X_RMS_ERROR);
                    yTargetField.setValue(targetBeamPosition.Y);
                    yTargetErrorField.setValue(targetBeamPosition.Y_RMS_ERROR);
                    DOCUMENT.xpos = targetBeamPosition.X;
                    DOCUMENT.ypos = targetBeamPosition.Y;
                    TARGET_PLOT.displayBeamPosition(targetBeamPosition.X, targetBeamPosition.Y, targetBeamPosition.X_RMS_ERROR, targetBeamPosition.Y_RMS_ERROR);
                } catch (Exception exception) {
                    exception.printStackTrace();
                    JOptionPane.showMessageDialog(FACE_VIEW, exception.getMessage(), "Error matching target beam position.", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        final JButton copyReportButton = new JButton("Report");
        copyReportButton.setToolTipText("Copy a report to the clipboard.");
        targetBox.add(copyReportButton);
        copyReportButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent event) {
                final StringBuffer reportBuffer = new StringBuffer("Target Beam Position Report\n");
                reportBuffer.append("Date:\t" + new SimpleDateFormat("MMM dd, yyyy HH:mm:ss").format(new Date()));
                reportBuffer.append("\n");
                reportBuffer.append("BPM Data:\n");
                BPM_TABLE_MODEL.appendReport(reportBuffer);
                reportBuffer.append("Matching Target X (mm):\t" + xTargetField.getText() + "\t+/-\t" + xTargetErrorField.getText() + "\n");
                reportBuffer.append("Matching Target Y (mm):\t" + yTargetField.getText() + "\t+/-\t" + yTargetErrorField.getText() + "\n");
                final String report = reportBuffer.toString();
                final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                final StringSelection contents = new StringSelection(report);
                clipboard.setContents(contents, contents);
            }
        });
        return mainBox;
    }

    /** stop the running average */
    protected void stopRunningAverage() {
        for (final BpmAgent bpmAgent : BPM_AGENTS) {
            bpmAgent.stopAveraging();
        }
    }

    /** clear the BPM running average */
    protected void clearBeamPositionRunningAverage() {
        for (final BpmAgent bpmAgent : BPM_AGENTS) {
            bpmAgent.clearBeamPositionRunningAverage();
        }
    }
}

/** manage the target plot */
class TargetPlot {

    /** panel in which to plot the target beam position relative to the tolerance */
    protected final FunctionGraphsJPanel PLOT_PANEL;

    /** tolerance of the beam position in the x direction */
    protected final double X_TOLERANCE;

    /** tolerance of the beam position in the y direction */
    protected final double Y_TOLERANCE;

    /** Constructor */
    public TargetPlot(final double xTolerance, final double yTolerance) {
        X_TOLERANCE = xTolerance;
        Y_TOLERANCE = yTolerance;
        PLOT_PANEL = new FunctionGraphsJPanel();
        PLOT_PANEL.setLegendPosition(FunctionGraphsJPanel.LEGEND_POSITION_ARBITRARY);
        PLOT_PANEL.setLegendKeyString("Legend");
        PLOT_PANEL.setLegendBackground(Color.GRAY);
        PLOT_PANEL.setLegendColor(Color.BLACK);
        PLOT_PANEL.setLegendVisible(true);
        PLOT_PANEL.setGraphBackGroundColor(Color.BLACK);
        PLOT_PANEL.setGridLineColor(Color.DARK_GRAY);
        PLOT_PANEL.setAxisNameX("X Target Position (mm)");
        PLOT_PANEL.setAxisNameY("Y Target Position (mm)");
        PLOT_PANEL.setName("Target - Position Tolerance and Measured Position");
        PLOT_PANEL.setLimitsAndTicksX(-20, 5, 8, 4);
        PLOT_PANEL.setLimitsAndTicksY(-20, 5, 8, 4);
        clear();
        PLOT_PANEL.refreshGraphJPanel();
    }

    /** display the beam position */
    public void displayBeamPosition(final double x, final double y, final double xError, final double yError) {
        clear();
        final Color positionColor = Color.ORANGE;
        drawBoxAboutCenter(x, y, xError, yError, positionColor);
        final BasicGraphData positionData = new BasicGraphData();
        positionData.setGraphColor(positionColor);
        positionData.addPoint(x, y);
        positionData.setGraphProperty(PLOT_PANEL.getLegendKeyString(), "Measured Position");
        PLOT_PANEL.addGraphData(positionData);
        PLOT_PANEL.refreshGraphJPanel();
    }

    /** clear the display */
    public void clear() {
        PLOT_PANEL.removeAllCurveData();
        PLOT_PANEL.removeAllGraphData();
        final Color toleranceColor = Color.BLUE;
        drawBoxAboutCenter(0.0, 0.0, X_TOLERANCE, Y_TOLERANCE, toleranceColor);
        final BasicGraphData toleranceData = new BasicGraphData();
        toleranceData.setGraphColor(toleranceColor);
        toleranceData.addPoint(0.0, 0.0);
        toleranceData.setGraphProperty(PLOT_PANEL.getLegendKeyString(), "Position Tolerance");
        PLOT_PANEL.addGraphData(toleranceData);
        PLOT_PANEL.refreshGraphJPanel();
    }

    /** draw the rectangle about the specified center coordinates */
    protected void drawBoxAboutCenter(final double x, final double y, final double halfWidth, final double halfHeight, final Color color) {
        drawEdge(x - halfWidth, y, 0.0, halfHeight / 2, color);
        drawEdge(x + halfWidth, y, 0.0, halfHeight / 2, color);
        drawEdge(x, y - halfHeight, halfWidth / 2, 0.0, color);
        drawEdge(x, y + halfHeight, halfWidth / 2, 0.0, color);
        drawEdge(x, y, 0, halfHeight, color);
        drawEdge(x, y, halfWidth, 0, color);
        PLOT_PANEL.refreshGraphJPanel();
    }

    /** draw an edge */
    protected void drawEdge(final double x, final double y, final double xShift, final double yShift, final Color color) {
        final CurveData edge = new CurveData();
        edge.setLineWidth(3);
        edge.setColor(color);
        edge.addPoint(x - xShift, y - yShift);
        edge.addPoint(x + xShift, y + yShift);
        PLOT_PANEL.addCurveData(edge);
    }

    /** Get the plot panel */
    public FunctionGraphsJPanel getPlotPanel() {
        return PLOT_PANEL;
    }
}

/** Projected beam position with error */
class BeamPosition {

    public final double X;

    public final double X_RMS_ERROR;

    public final double Y;

    public final double Y_RMS_ERROR;

    /** Constructor */
    public BeamPosition(final double x, final double xError, final double y, final double yError) {
        X = x;
        X_RMS_ERROR = xError;
        Y = y;
        Y_RMS_ERROR = yError;
    }
}

/** use the measured BPM beam positions to predict the best matching target beam position */
class TargetBeamPositionMatcher {

    protected final TargetOrbitAnalysis X_ORBIT_ANALYSIS;

    protected final TargetOrbitAnalysis Y_ORBIT_ANALYSIS;

    protected final AcceleratorSeq SEQUENCE;

    protected List<BPM> _lastBPMs;

    /** Constructor */
    public TargetBeamPositionMatcher(final AcceleratorSeq sequence) {
        SEQUENCE = sequence;
        X_ORBIT_ANALYSIS = new TargetOrbitAnalysis(XPlaneAdaptor.getInstance(), sequence);
        Y_ORBIT_ANALYSIS = new TargetOrbitAnalysis(YPlaneAdaptor.getInstance(), sequence);
        _lastBPMs = new ArrayList<BPM>();
    }

    /** get the X orbit analysis */
    public TargetOrbitAnalysis getOrbitAnalysisX() {
        return X_ORBIT_ANALYSIS;
    }

    /** get the Y orbit analysis */
    public TargetOrbitAnalysis getOrbitAnalysisY() {
        return Y_ORBIT_ANALYSIS;
    }

    /** determine the best matching target beam position vector (x, y) */
    public BeamPosition getMatchingTargetBeamPosition(final List<BpmAgent> bpmAgents) throws Exception {
        final Probe probe = getProbe(SEQUENCE);
        final Scenario scenario = getScenario(SEQUENCE, probe);
        scenario.resync();
        scenario.run();
        final MatrixTrajectory trajectory = (MatrixTrajectory) scenario.getTrajectory();
        final AcceleratorNode target = SEQUENCE.getNodesOfType("Tgt").get(0);
        final int bpmCount = bpmAgents.size();
        final List<BPM> bpms = new ArrayList<BPM>(bpmCount);
        final double[] xBeamPositions = new double[bpmCount];
        final double[] xErrorBeamPositions = new double[bpmCount];
        final double[] yBeamPositions = new double[bpmCount];
        final double[] yErrorBeamPositions = new double[bpmCount];
        for (int index = 0; index < bpmCount; index++) {
            final BpmAgent bpmAgent = bpmAgents.get(index);
            bpms.add(bpmAgent.getNode());
            xBeamPositions[index] = bpmAgent.getHorizontalRunningAverageBeamPosition();
            xErrorBeamPositions[index] = xBeamPositions[index];
            yBeamPositions[index] = bpmAgent.getVerticalRunningAverageBeamPosition();
            yErrorBeamPositions[index] = yBeamPositions[index];
        }
        final OrbitMatcher orbitMatcher = new OrbitMatcher(target, bpms, trajectory);
        if (!bpms.equals(_lastBPMs)) {
            X_ORBIT_ANALYSIS.performAnalysis(bpms);
            Y_ORBIT_ANALYSIS.performAnalysis(bpms);
            _lastBPMs = bpms;
        }
        final double xScale = X_ORBIT_ANALYSIS.getScale();
        final double xOffset = X_ORBIT_ANALYSIS.getOffset();
        final double yScale = Y_ORBIT_ANALYSIS.getScale();
        final double yOffset = Y_ORBIT_ANALYSIS.getOffset();
        final double xTarget = xScale * orbitMatcher.getHorizontalTargetBeamPosition(xBeamPositions) + xOffset;
        final double yTarget = yScale * orbitMatcher.getVerticalTargetBeamPosition(yBeamPositions) + yOffset;
        final double xOrbitAnalysisError = X_ORBIT_ANALYSIS.getRmsError();
        double xSumSquareError = xOrbitAnalysisError * xOrbitAnalysisError;
        final double yOrbitAnalysisError = Y_ORBIT_ANALYSIS.getRmsError();
        double ySumSquareError = yOrbitAnalysisError * yOrbitAnalysisError;
        for (int index = 0; index < bpmCount; index++) {
            final BpmAgent bpmAgent = bpmAgents.get(index);
            xErrorBeamPositions[index] += bpmAgent.getHorizontalRunningAverageBeamPositionError();
            yErrorBeamPositions[index] += yBeamPositions[index] + bpmAgent.getVerticalRunningAverageBeamPositionError();
            final double xErrorTarget = xScale * orbitMatcher.getHorizontalTargetBeamPosition(xErrorBeamPositions) + xOffset;
            final double yErrorTarget = yScale * orbitMatcher.getVerticalTargetBeamPosition(yErrorBeamPositions) + yOffset;
            xSumSquareError += (xTarget - xErrorTarget) * (xTarget - xErrorTarget);
            ySumSquareError += (yTarget - yErrorTarget) * (yTarget - yErrorTarget);
            xErrorBeamPositions[index] = xBeamPositions[index];
            yErrorBeamPositions[index] = yBeamPositions[index];
        }
        final double xRmsError = Math.sqrt(xSumSquareError);
        final double yRmsError = Math.sqrt(ySumSquareError);
        return new BeamPosition(xTarget, xRmsError, yTarget, yRmsError);
    }

    protected static Probe getProbe(final AcceleratorSeq sequence) {
        return ProbeFactory.getTransferMapProbe(sequence, new TransferMapTracker());
    }

    protected static Scenario getScenario(final AcceleratorSeq sequence, final Probe probe) throws Exception {
        final Scenario scenario = Scenario.newScenarioFor(sequence);
        scenario.setSynchronizationMode(Scenario.SYNC_MODE_RF_DESIGN);
        scenario.setStartNode("RTBT_Diag:BPM15");
        scenario.setProbe(probe);
        return scenario;
    }
}

/** table model of BPMs */
class BPMTableModel extends AbstractTableModel implements BpmAgentListener {

    /** default number format */
    protected static final java.text.NumberFormat DEFAULT_NUMBER_FORMAT = new DecimalFormat("#,##0.0");

    /** column displaying the name of the BPM */
    protected static final int NAME_COLUMN = 0;

    /** column displaying whether the BPM is enabled */
    protected static final int ENABLE_COLUMN = 1;

    /** column displaying the running average horizontal beam position */
    protected static final int X_BEAM_POSITION_COLUMN = 2;

    /** column displaying the running average horizontal beam position RMS error */
    protected static final int X_BEAM_POSITION_ERROR_COLUMN = 3;

    /** column displaying the running average vertical beam position */
    protected static final int Y_BEAM_POSITION_COLUMN = 4;

    /** column displaying the running average vertical beam position RMS error */
    protected static final int Y_BEAM_POSITION_ERROR_COLUMN = 5;

    /** list of available BPM agents */
    protected final List<BpmAgent> BPM_AGENTS;

    /** table of enable status */
    protected final Map<BpmAgent, Boolean> BPM_ENABLES;

    /** Constructor */
    public BPMTableModel(final List<BpmAgent> bpmAgents) {
        BPM_AGENTS = bpmAgents;
        BPM_ENABLES = new HashMap<BpmAgent, Boolean>();
        for (final BpmAgent bpmAgent : BPM_AGENTS) {
            bpmAgent.addBpmAgentListener(this);
        }
    }

    /** get the number of rows in the table */
    public int getRowCount() {
        return BPM_AGENTS.size();
    }

    /** get the number of columns in the table */
    public int getColumnCount() {
        return 6;
    }

    /** get the enabled BPM agents */
    public List<BpmAgent> getEnabledBpmAgents() {
        final List<BpmAgent> enabledAgents = new ArrayList<BpmAgent>();
        for (final BpmAgent bpmAgent : BPM_AGENTS) {
            if (isBPMEnabled(bpmAgent)) {
                enabledAgents.add(bpmAgent);
            }
        }
        return enabledAgents;
    }

    /** determine if the specified BPM Agent is enabled */
    public boolean isBPMEnabled(final BpmAgent bpmAgent) {
        return BPM_ENABLES.containsKey(bpmAgent) ? BPM_ENABLES.get(bpmAgent) : true;
    }

    /** determine whether the specified cell is editable */
    public boolean isCellEditable(final int row, final int column) {
        return column == ENABLE_COLUMN;
    }

    /** get the class of the specified table column */
    public Class getColumnClass(final int column) {
        switch(column) {
            case NAME_COLUMN:
                return String.class;
            case ENABLE_COLUMN:
                return Boolean.class;
            case X_BEAM_POSITION_COLUMN:
                return Number.class;
            case X_BEAM_POSITION_ERROR_COLUMN:
                return Number.class;
            case Y_BEAM_POSITION_COLUMN:
                return Number.class;
            case Y_BEAM_POSITION_ERROR_COLUMN:
                return Number.class;
            default:
                return super.getColumnClass(column);
        }
    }

    /** get the name of the specified table column */
    public String getColumnName(final int column) {
        switch(column) {
            case NAME_COLUMN:
                return "BPM";
            case ENABLE_COLUMN:
                return "Enable";
            case X_BEAM_POSITION_COLUMN:
                return "X Mean Position (mm)";
            case X_BEAM_POSITION_ERROR_COLUMN:
                return "X RMS Error (mm)";
            case Y_BEAM_POSITION_COLUMN:
                return "Y Mean Position (mm)";
            case Y_BEAM_POSITION_ERROR_COLUMN:
                return "Y RMS Error (mm)";
            default:
                return "?";
        }
    }

    /** get the value associated with the specified table cell */
    public Object getValueAt(final int row, final int column) {
        final BpmAgent bpmAgent = BPM_AGENTS.get(row);
        switch(column) {
            case NAME_COLUMN:
                return bpmAgent.getNode().getId();
            case ENABLE_COLUMN:
                return isBPMEnabled(bpmAgent);
            case X_BEAM_POSITION_COLUMN:
                final double xBeamPosition = bpmAgent.getHorizontalRunningAverageBeamPosition();
                return !Double.isNaN(xBeamPosition) ? new FormattedNumber(DEFAULT_NUMBER_FORMAT, xBeamPosition) : null;
            case X_BEAM_POSITION_ERROR_COLUMN:
                return new FormattedNumber(DEFAULT_NUMBER_FORMAT, bpmAgent.getHorizontalRunningAverageBeamPositionError());
            case Y_BEAM_POSITION_COLUMN:
                final double yBeamPosition = bpmAgent.getVerticalRunningAverageBeamPosition();
                return !Double.isNaN(yBeamPosition) ? new FormattedNumber(DEFAULT_NUMBER_FORMAT, yBeamPosition) : null;
            case Y_BEAM_POSITION_ERROR_COLUMN:
                return new FormattedNumber(DEFAULT_NUMBER_FORMAT, bpmAgent.getVerticalRunningAverageBeamPositionError());
            default:
                return null;
        }
    }

    /** set the value of the table cell */
    public void setValueAt(final Object value, final int row, final int column) {
        final BpmAgent bpmAgent = BPM_AGENTS.get(row);
        switch(column) {
            case ENABLE_COLUMN:
                BPM_ENABLES.put(bpmAgent, (Boolean) value);
                return;
            default:
                return;
        }
    }

    /** event indicating that the X running average value changed */
    public void xRunningAverageValueChanged(final BpmAgent source, final double value) {
        final int row = BPM_AGENTS.indexOf(source);
        if (row >= 0) {
            fireTableCellUpdated(row, X_BEAM_POSITION_COLUMN);
            fireTableCellUpdated(row, X_BEAM_POSITION_ERROR_COLUMN);
        }
    }

    /** event indicating that the Y running average value changed */
    public void yRunningAverageValueChanged(final BpmAgent source, final double value) {
        final int row = BPM_AGENTS.indexOf(source);
        if (row >= 0) {
            fireTableCellUpdated(row, Y_BEAM_POSITION_COLUMN);
            fireTableCellUpdated(row, Y_BEAM_POSITION_ERROR_COLUMN);
        }
    }

    /** append this table's data to the report buffer */
    public void appendReport(final StringBuffer reportBuffer) {
        final int rowCount = getRowCount();
        final int columnCount = getColumnCount();
        for (int column = 0; column < columnCount; column++) {
            reportBuffer.append(getColumnName(column));
            if (column < columnCount - 1) {
                reportBuffer.append("\t");
            }
        }
        reportBuffer.append("\n");
        for (int row = 0; row < rowCount; row++) {
            for (int column = 0; column < columnCount; column++) {
                reportBuffer.append(getValueAt(row, column));
                if (column < columnCount - 1) {
                    reportBuffer.append("\t");
                }
            }
            reportBuffer.append("\n");
        }
    }
}

/** PV Logger snapshot */
class PVLoggerSnapshot {

    protected final PVLoggerDataSource _dataSource;

    protected final long _snapshotID;

    protected final Map<String, Double> _bpmXMap;

    protected final Map<String, Double> _bpmYMap;

    /** Constructor */
    public PVLoggerSnapshot(final long snapshotID) {
        _snapshotID = snapshotID;
        _dataSource = new PVLoggerDataSource(snapshotID);
        _bpmXMap = _dataSource.getBPMXMap();
        _bpmYMap = _dataSource.getBPMYMap();
        _dataSource.closeConnection();
    }

    public long getID() {
        return _snapshotID;
    }

    public PVLoggerDataSource getDataSource() {
        return _dataSource;
    }

    public double getXAvg(final BPM bpm) {
        return getXAvg(bpm.getId());
    }

    public double getXAvg(final String bpmID) {
        return _bpmXMap.get(bpmID + ":xAvg");
    }

    public double getYAvg(final BPM bpm) {
        return getYAvg(bpm.getId());
    }

    public double getYAvg(final String bpmID) {
        return _bpmYMap.get(bpmID + ":yAvg");
    }

    public double getMainFieldSetting(final Electromagnet magnet) {
        final Channel channel = magnet.getChannel(MagnetMainSupply.FIELD_SET_HANDLE);
        return _dataSource.getChannelSnapshotValue(channel.channelName())[0];
    }
}

/** record of a beam position measurement at the view screen and the associated PV Logger snapshot */
class ViewScreenMeasurement {

    protected final PVLoggerSnapshot _snapshot;

    protected final double[] _beamPosition;

    protected MatrixTrajectory _trajectory;

    protected AcceleratorSeq _sequence;

    /** Constructor */
    public ViewScreenMeasurement(final long snapshotID, final double x, final double y) {
        _snapshot = new PVLoggerSnapshot(snapshotID);
        _beamPosition = new double[] { x, y };
        _trajectory = null;
    }

    public PVLoggerSnapshot getSnapshot() {
        return _snapshot;
    }

    public double getBeamPositionX() {
        return _beamPosition[0];
    }

    public double getBeamPositionY() {
        return _beamPosition[1];
    }

    public MatrixTrajectory getTrajectory(final AcceleratorSeq sequence) throws Exception {
        return _trajectory != null && sequence == _sequence ? _trajectory : calculateTrajectory(sequence);
    }

    protected MatrixTrajectory calculateTrajectory(final AcceleratorSeq sequence) throws Exception {
        _sequence = sequence;
        final PVLoggerSnapshot snapshot = _snapshot;
        final Scenario scenario = getScenario(snapshot.getDataSource());
        scenario.run();
        final MatrixTrajectory trajectory = (MatrixTrajectory) scenario.getTrajectory();
        _trajectory = trajectory;
        return trajectory;
    }

    /** generate a new scenario given the data source */
    protected Scenario getScenario(final PVLoggerDataSource dataSource) throws Exception {
        final Probe probe = getProbe();
        return getScenario(probe, dataSource);
    }

    /** get a scenario with the specified probe and data source */
    protected Scenario getScenario(final Probe probe, final PVLoggerDataSource dataSource) throws Exception {
        final Scenario scenario = Scenario.newScenarioFor(_sequence);
        scenario.setSynchronizationMode(Scenario.SYNC_MODE_DESIGN);
        final Scenario loggerScenario = dataSource.setModelSource(_sequence, scenario);
        loggerScenario.setStartNode("RTBT_Diag:BPM15");
        loggerScenario.setProbe(probe);
        return loggerScenario;
    }

    /** get a new probe */
    protected Probe getProbe() {
        final Probe probe = ProbeFactory.getTransferMapProbe(_sequence, new TransferMapTracker());
        probe.setKineticEnergy(840.e6);
        return probe;
    }

    /** get a string representation */
    public String toString() {
        return "snapshot:  " + _snapshot.getID() + ", x_vs:  " + _beamPosition[0] + ", y_vs:  " + _beamPosition[1];
    }
}

/** adaptor for getting the right data for the specified plane */
abstract class PlaneAdaptor {

    public abstract String getType();

    public abstract double getAverageBeamPosition(final PVLoggerSnapshot snapshot, final BPM bpm);

    public abstract double getViewScreenBeamPosition(final ViewScreenMeasurement measurement);

    public abstract double getMatchedBeamPosition(final OrbitMatcher orbitMatcher, final double[] beamPositions);

    /** scale to adjust the online model's predicted beam position */
    public double getDefaultScale() {
        return 1.0;
    }

    /** offset to adjust the online model's predicted beam position */
    public double getDefaultOffset() {
        return 0.0;
    }
}

/** adaptor for getting the data for the X plane */
class XPlaneAdaptor extends PlaneAdaptor {

    static final XPlaneAdaptor PLANE_ADAPTOR;

    static {
        PLANE_ADAPTOR = new XPlaneAdaptor();
    }

    /** get the shared instance */
    static PlaneAdaptor getInstance() {
        return PLANE_ADAPTOR;
    }

    public String getType() {
        return "X";
    }

    public double getAverageBeamPosition(final PVLoggerSnapshot snapshot, final BPM bpm) {
        return snapshot.getXAvg(bpm);
    }

    public double getViewScreenBeamPosition(final ViewScreenMeasurement measurement) {
        return measurement.getBeamPositionX();
    }

    public double getMatchedBeamPosition(final OrbitMatcher orbitMatcher, final double[] beamPositions) {
        return orbitMatcher.getHorizontalTargetBeamPosition(beamPositions);
    }

    /** scale to adjust the online model's predicted beam position */
    public double getDefaultScale() {
        return 0.94;
    }

    /** offset to adjust the online model's predicted beam position */
    public double getDefaultOffset() {
        return -14.12;
    }
}

/** adaptor for getting the data for the Y plane */
class YPlaneAdaptor extends PlaneAdaptor {

    static final YPlaneAdaptor PLANE_ADAPTOR;

    static {
        PLANE_ADAPTOR = new YPlaneAdaptor();
    }

    /** get the shared instance */
    static PlaneAdaptor getInstance() {
        return PLANE_ADAPTOR;
    }

    public String getType() {
        return "Y";
    }

    public double getAverageBeamPosition(final PVLoggerSnapshot snapshot, final BPM bpm) {
        return snapshot.getYAvg(bpm);
    }

    public double getViewScreenBeamPosition(final ViewScreenMeasurement measurement) {
        return measurement.getBeamPositionY();
    }

    public double getMatchedBeamPosition(final OrbitMatcher orbitMatcher, final double[] beamPositions) {
        return orbitMatcher.getVerticalTargetBeamPosition(beamPositions);
    }

    /** scale to adjust the online model's predicted beam position */
    public double getDefaultScale() {
        return 0.62;
    }

    /** offset to adjust the online model's predicted beam position */
    public double getDefaultOffset() {
        return -1.52;
    }
}

/** result record */
class TargetAnalysisResultRecord {

    protected final double _predictedPosition;

    protected final double _measuredPosition;

    /** Constructor */
    public TargetAnalysisResultRecord(final double measuredPosition, final double predictedPosition) {
        _measuredPosition = measuredPosition;
        _predictedPosition = predictedPosition;
    }

    /** get the prediction */
    public double getPredictedPosition() {
        return _predictedPosition;
    }

    /** get the measurement */
    public double getMeasuredPosition() {
        return _measuredPosition;
    }
}

/** analysis of the target beam position relationship to the RTBT orbit measurements */
class TargetOrbitAnalysis {

    /** list of view screen measurements */
    protected static final List<ViewScreenMeasurement> VIEW_SCREEN_MEASUREMENTS;

    /** sequence for which to analyze the data */
    protected final AcceleratorSeq SEQUENCE;

    /** adaptor for handling the calls that are specific to a particular plane (either x or y) */
    protected final PlaneAdaptor PLANE_ADAPTOR;

    /** node for which to predict the beam position */
    protected final AcceleratorNode TARGET;

    /** scale to adjust the online model's predicted beam position */
    protected double _scale;

    /** offset to adjust the online model's predicted beam position */
    protected double _offset;

    /** RMS error of the fit to the historical measurements */
    protected double _rmsError;

    static {
        VIEW_SCREEN_MEASUREMENTS = loadViewScreenMeasurements();
    }

    /** Constructor */
    public TargetOrbitAnalysis(final PlaneAdaptor planeAdaptor, final AcceleratorSeq sequence) {
        PLANE_ADAPTOR = planeAdaptor;
        SEQUENCE = sequence;
        TARGET = sequence.getNodesOfType("Tgt").get(0);
        _scale = planeAdaptor.getDefaultScale();
        _offset = planeAdaptor.getDefaultOffset();
        _rmsError = Double.NaN;
    }

    /** get the scale */
    public double getScale() {
        return _scale;
    }

    /** get the offset */
    public double getOffset() {
        return _offset;
    }

    /** get the RMS error */
    public double getRmsError() {
        return _rmsError;
    }

    /** use the online model to predict the target position given the positions measured at the bpms */
    public void performAnalysis(final List<BPM> bpms) throws Exception {
        final int measurementCount = VIEW_SCREEN_MEASUREMENTS.size();
        double meanDifference = 0.0;
        final Matrix diag = new Matrix(measurementCount, 2);
        final Matrix viewScreenMeas = new Matrix(measurementCount, 1);
        final List<TargetAnalysisResultRecord> rawResults = new ArrayList<TargetAnalysisResultRecord>(measurementCount);
        int row = 0;
        for (final ViewScreenMeasurement measurement : VIEW_SCREEN_MEASUREMENTS) {
            final TargetAnalysisResultRecord rawResult = predictWithMatcher(bpms, measurement);
            rawResults.add(rawResult);
            final double measuredPosition = rawResult.getMeasuredPosition();
            viewScreenMeas.set(row, 0, measuredPosition);
            diag.set(row, 0, 1.0);
            diag.set(row, 1, rawResult.getPredictedPosition());
            meanDifference += rawResult.getMeasuredPosition() - rawResult.getPredictedPosition();
            ++row;
        }
        meanDifference /= measurementCount;
        final Matrix diagT = diag.transpose();
        final Matrix coef = diagT.times(diag).inverse().times(diagT).times(viewScreenMeas);
        final double offset = coef.get(0, 0);
        final double scale = coef.get(1, 0);
        final List<TargetAnalysisResultRecord> results = new ArrayList<TargetAnalysisResultRecord>();
        double errorSum = 0.0;
        for (final TargetAnalysisResultRecord rawResult : rawResults) {
            final double predictedPosition = scale * rawResult.getPredictedPosition() + offset;
            final double measuredPosition = rawResult.getMeasuredPosition();
            final TargetAnalysisResultRecord result = new TargetAnalysisResultRecord(measuredPosition, predictedPosition);
            results.add(result);
            errorSum += (measuredPosition - predictedPosition) * (measuredPosition - predictedPosition);
        }
        final double rmsError = Math.sqrt(errorSum / rawResults.size());
        _scale = scale;
        _offset = offset;
        _rmsError = rmsError;
    }

    /** use the online model to predict the target position given the positions measured at the bpms */
    protected TargetAnalysisResultRecord predictWithMatcher(final List<BPM> bpms, final ViewScreenMeasurement measurement) throws Exception {
        final MatrixTrajectory trajectory = measurement.getTrajectory(SEQUENCE);
        final double measuredBeamPosition = PLANE_ADAPTOR.getViewScreenBeamPosition(measurement);
        final PVLoggerSnapshot snapshot = measurement.getSnapshot();
        final int bpmCount = bpms.size();
        final double[] beamPositions = new double[bpmCount];
        for (int index = 0; index < bpmCount; index++) {
            final BPM bpm = bpms.get(index);
            beamPositions[index] = PLANE_ADAPTOR.getAverageBeamPosition(snapshot, bpm);
        }
        final OrbitMatcher orbitMatcher = new OrbitMatcher(TARGET, bpms, trajectory);
        final double prediction = PLANE_ADAPTOR.getMatchedBeamPosition(orbitMatcher, beamPositions);
        return new TargetAnalysisResultRecord(measuredBeamPosition, prediction);
    }

    /** load the target view screen measurements */
    protected static List<ViewScreenMeasurement> loadViewScreenMeasurements() {
        final List<ViewScreenMeasurement> measurements = new ArrayList<ViewScreenMeasurement>();
        if (true) {
            measurements.add(new ViewScreenMeasurement(2336611, -0.91, 2.58));
            measurements.add(new ViewScreenMeasurement(2336616, -30.95, -2.93));
            measurements.add(new ViewScreenMeasurement(2336622, -3.76, 2.99));
            measurements.add(new ViewScreenMeasurement(2336633, 3.33, 4.90));
            measurements.add(new ViewScreenMeasurement(2336638, -7.37, 2.57));
            measurements.add(new ViewScreenMeasurement(2336649, -7.70, 2.69));
            measurements.add(new ViewScreenMeasurement(2336664, -1.26, 0.92));
            measurements.add(new ViewScreenMeasurement(2336675, 0.17, 0.27));
        }
        if (true) {
            measurements.add(new ViewScreenMeasurement(2350426, 2.25, -1.00));
            measurements.add(new ViewScreenMeasurement(2350434, -3.10, 2.45));
            measurements.add(new ViewScreenMeasurement(2350447, 0.40, 0.00));
            measurements.add(new ViewScreenMeasurement(2350463, 0.30, 0.70));
            measurements.add(new ViewScreenMeasurement(2350480, 0.20, 1.40));
            measurements.add(new ViewScreenMeasurement(2350489, 0.50, -2.40));
            measurements.add(new ViewScreenMeasurement(2350500, 0.40, -3.10));
            measurements.add(new ViewScreenMeasurement(2350507, 0.40, -3.90));
            measurements.add(new ViewScreenMeasurement(2350526, 0.30, -0.90));
            measurements.add(new ViewScreenMeasurement(2350533, 0.40, -1.60));
            measurements.add(new ViewScreenMeasurement(2350540, 0.40, -0.50));
            measurements.add(new ViewScreenMeasurement(2350545, -5.00, -0.60));
            measurements.add(new ViewScreenMeasurement(2350555, -8.60, -0.70));
            measurements.add(new ViewScreenMeasurement(2350562, -2.10, -0.60));
            measurements.add(new ViewScreenMeasurement(2350574, -15.20, -1.00));
            measurements.add(new ViewScreenMeasurement(2350583, 4.40, -0.40));
        }
        return measurements;
    }
}
