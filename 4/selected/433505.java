package gov.sns.apps.ringmeasurement;

import java.io.*;
import java.util.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.event.*;
import java.text.NumberFormat;
import javax.swing.*;
import javax.swing.event.*;
import java.text.ParseException;
import gov.sns.tools.apputils.EdgeLayout;
import gov.sns.xal.smf.impl.*;
import gov.sns.xal.smf.Ring;
import gov.sns.tools.swing.DecimalField;
import gov.sns.ca.*;
import gov.sns.tools.apputils.files.*;
import gov.sns.tools.pvlogger.*;
import gov.sns.tools.database.*;
import gov.sns.xal.model.*;
import gov.sns.xal.model.probe.TransferMapProbe;
import gov.sns.xal.model.probe.ProbeFactory;
import gov.sns.xal.model.alg.TransferMapTracker;
import gov.sns.xal.model.scenario.Scenario;
import gov.sns.xal.model.probe.traj.TransferMapTrajectory;
import gov.sns.xal.model.probe.traj.TransferMapState;
import gov.sns.xal.model.pvlogger.PVLoggerDataSource;

/**
 * 
 * @author Paul Chu
 */
public class TunePanel extends JPanel implements ConnectionListener, ActionListener {

    static final long serialVersionUID = 0;

    RingDocument myDoc;

    EdgeLayout edgeLayout = new EdgeLayout();

    JTable bpmTable, quadTable;

    ArrayList<BPM> allBPMs;

    ArrayList<Integer> badBPMs = new ArrayList<Integer>();

    ArrayList<MagnetMainSupply> qPSs = new ArrayList<MagnetMainSupply>();

    JPanel bpmPane = new JPanel();

    private TuneMeasurement[] tuneMeasurement;

    JScrollPane bpmChooserPane, quadPane;

    JTabbedPane plotDisplayPane;

    JPanel phasePlotPane = new JPanel();

    JPanel posPlotPane = new JPanel();

    JPanel phaseDiffPlotPane = new JPanel();

    BpmTableModel bpmTableModel;

    QuadTableModel quadTableModel;

    private String selectedBPM = "";

    private JComboBox selectBPM;

    private JDialog configDialog = new JDialog();

    BPMPlotPane xBpmPlotPane, yBpmPlotPane, xPhasePlotPane, yPhasePlotPane;

    BPMPlotPane xPhDiffPlotPane, yPhDiffPlotPane;

    double[] xTune, yTune;

    double[] xPhase, yPhase, xPhaseDiff, yPhaseDiff, xDiffPlot, yDiffPlot, posArray, goodPosArry;

    JTextField dfXTune, dfYTune;

    NumberFormat numberFormat = NumberFormat.getNumberInstance();

    protected DecimalField df6, df7;

    int maxTime = 100;

    int fftSize = 64;

    int len = 40;

    protected JComboBox fftConf;

    boolean hasTune = false;

    JButton quadCorrBtn, setQuadBtn;

    JProgressBar progBar;

    double[] qSetVals;

    Channel[] setPVChs, rbPVChs;

    /** List of the monitors */
    final Vector<Monitor> mons = new Vector<Monitor>();

    private HashMap<String, Vector<InputPVTableCell>> monitorQueues = new HashMap<String, Vector<InputPVTableCell>>();

    JButton dumpData = new JButton("Save Fit Data");

    /** for data dump file */
    private RecentFileTracker _datFileTracker;

    File datFile;

    private LoggerSession loggerSession, loggerSession1;

    private MachineSnapshot snapshot, snapshot1;

    protected long pvLoggerId, pvLoggerId1;

    /** Timestamp when a scan was started */
    protected Date startTime;

    InputPVTableCell setPVCell[], rbPVCell[];

    HashMap<MagnetMainSupply, Double> designMap;

    CalcQuadSettings cqs;

    JLabel xTuneAvg, yTuneAvg;

    ArrayList<String> goodBPMs;

    /** for on/off line mode */
    private boolean isOnline = true;

    private long bpmPVLogId = 0;

    private long defPVLogId = 0;

    private boolean quadTableInit = false;

    public TunePanel(RingDocument doc) {
        myDoc = doc;
        _datFileTracker = new RecentFileTracker(1, this.getClass(), "recent_saved_file");
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
        ChannelGroup group1 = store.fetchGroup("Ring BPM Test");
        loggerSession1 = new LoggerSession(group1, store);
    }

    protected void initTables(ArrayList<BPM> bpms, ArrayList<MagnetMainSupply> quads, HashMap<MagnetMainSupply, Double> map) {
        allBPMs = bpms;
        qPSs = quads;
        designMap = map;
        tuneMeasurement = new TuneMeasurement[allBPMs.size()];
        xTune = new double[allBPMs.size()];
        yTune = new double[allBPMs.size()];
        xPhase = new double[allBPMs.size()];
        yPhase = new double[allBPMs.size()];
        posArray = new double[allBPMs.size()];
        this.setSize(960, 850);
        setLayout(edgeLayout);
        String[] bpmColumnNames = { "BPM", "XTune", "XPhase", "YTune", "YPhase", "Ignore" };
        bpmTableModel = new BpmTableModel(allBPMs, bpmColumnNames, this);
        String[] quadColumnNames = { "Quad PS", "Set Pt.", "Readback", "fitted Field", "new Set Pt." };
        quadTableModel = new QuadTableModel(qPSs, quadColumnNames);
        EdgeLayout edgeLayout1 = new EdgeLayout();
        bpmPane.setLayout(edgeLayout1);
        JLabel label = new JLabel("Select a BPM for Tune Measurement:");
        edgeLayout.setConstraints(label, 0, 0, 0, 0, EdgeLayout.TOP, EdgeLayout.NO_GROWTH);
        bpmPane.add(label);
        bpmTable = new JTable(bpmTableModel);
        bpmTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        bpmTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionModel rowSM = bpmTable.getSelectionModel();
        rowSM.addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) return;
                ListSelectionModel lsm = (ListSelectionModel) e.getSource();
                if (lsm.isSelectionEmpty()) {
                } else {
                    int selectedRow = lsm.getMinSelectionIndex();
                    setSelectedBPM((allBPMs.get(selectedRow)).getId());
                    if (!badBPMs.contains(new Integer(selectedRow))) {
                        plotBPMData(selectedRow);
                    }
                }
            }
        });
        bpmChooserPane = new JScrollPane(bpmTable);
        bpmChooserPane.setPreferredSize(new Dimension(450, 300));
        bpmChooserPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        edgeLayout1.setConstraints(bpmChooserPane, 20, 0, 0, 0, EdgeLayout.TOP, EdgeLayout.NO_GROWTH);
        bpmPane.add(bpmChooserPane);
        JPanel selection = new JPanel();
        selection.setLayout(new GridLayout(2, 2));
        selection.setPreferredSize(new Dimension(330, 60));
        String[] options = { "Get tune (fit)", "Get tune (FFT)" };
        selectBPM = new JComboBox(options);
        selectBPM.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                System.out.println("App mode is " + isOnline);
                quadTableModel.setAppMode(isOnline);
                if (((String) (((JComboBox) evt.getSource()).getSelectedItem())).equals("Get tune (fit)")) {
                    tuneByFit();
                } else if (((String) (((JComboBox) evt.getSource()).getSelectedItem())).equals("Get tune (FFT)")) {
                    tuneByFFT();
                }
                if (isOnline) {
                    snapshot = loggerSession.takeSnapshot();
                    snapshot1 = loggerSession1.takeSnapshot();
                    startTime = new Date();
                }
            }
        });
        selectBPM.setPreferredSize(new Dimension(60, 10));
        JButton config = new JButton("Config. FFT/fit");
        config.setActionCommand("configuration");
        config.setPreferredSize(new Dimension(80, 10));
        config.addActionListener(this);
        selection.add(config);
        JLabel dummy = new JLabel("");
        selection.add(dummy);
        selection.add(selectBPM);
        configDialog.setBounds(300, 300, 330, 300);
        configDialog.setTitle("Config. fit/FFT parameters...");
        numberFormat.setMaximumFractionDigits(6);
        dumpData.setActionCommand("dumpData");
        dumpData.addActionListener(this);
        dumpData.setEnabled(false);
        selection.add(dumpData);
        xTuneAvg = new JLabel("avg. x tune = ");
        edgeLayout1.setConstraints(xTuneAvg, 440, 0, 0, 0, EdgeLayout.TOP, EdgeLayout.NO_GROWTH);
        bpmPane.add(xTuneAvg);
        yTuneAvg = new JLabel("avg. y tune = ");
        edgeLayout1.setConstraints(yTuneAvg, 460, 0, 0, 0, EdgeLayout.TOP, EdgeLayout.NO_GROWTH);
        bpmPane.add(yTuneAvg);
        quadTable = new JTable(quadTableModel);
        quadTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        quadPane = new JScrollPane(quadTable);
        quadPane.setPreferredSize(new Dimension(450, 200));
        quadPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        edgeLayout1.setConstraints(quadPane, 550, 0, 0, 0, EdgeLayout.TOP, EdgeLayout.NO_GROWTH);
        bpmPane.add(quadPane);
        quadCorrBtn = new JButton("Find Quad Error");
        quadCorrBtn.setActionCommand("findQuadError");
        quadCorrBtn.addActionListener(this);
        quadCorrBtn.setEnabled(false);
        edgeLayout.setConstraints(quadCorrBtn, 650, 500, 0, 0, EdgeLayout.TOP, EdgeLayout.NO_GROWTH);
        add(quadCorrBtn);
        progBar = new JProgressBar();
        progBar.setMinimum(0);
        edgeLayout.setConstraints(progBar, 680, 500, 0, 0, EdgeLayout.TOP, EdgeLayout.NO_GROWTH);
        add(progBar);
        setQuadBtn = new JButton("Set Quads");
        setQuadBtn.setActionCommand("setQuads");
        setQuadBtn.addActionListener(this);
        setQuadBtn.setEnabled(false);
        edgeLayout.setConstraints(setQuadBtn, 720, 500, 0, 0, EdgeLayout.TOP, EdgeLayout.NO_GROWTH);
        add(setQuadBtn);
        JPanel paramConf = new JPanel();
        JLabel fitFunction = new JLabel("Fit function: A*exp(-c*x) * sin(2PI*(w*x + b)) + d");
        JPanel ampPane = new JPanel();
        ampPane.setLayout(new GridLayout(1, 2));
        JPanel maxTimePane = new JPanel();
        maxTimePane.setLayout(new GridLayout(1, 2));
        JLabel label6 = new JLabel("Max. no of iterations: ");
        df6 = new DecimalField(maxTime, 9, numberFormat);
        maxTimePane.add(label6);
        maxTimePane.add(df6);
        paramConf.add(maxTimePane);
        JPanel fitLengthPane = new JPanel();
        fitLengthPane.setLayout(new GridLayout(1, 2));
        JLabel label7 = new JLabel("fit up to turn number:");
        numberFormat.setMaximumFractionDigits(0);
        df7 = new DecimalField(len, 4, numberFormat);
        fitLengthPane.add(label7);
        fitLengthPane.add(df7);
        paramConf.add(fitLengthPane);
        JPanel fftPane = new JPanel();
        fftPane.setLayout(new GridLayout(1, 2));
        JLabel label8 = new JLabel("FFT array size: ");
        String[] fftChoice = { "16", "32", "64", "128", "256" };
        fftConf = new JComboBox(fftChoice);
        fftConf.setSelectedIndex(2);
        fftConf.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                if (((String) (((JComboBox) evt.getSource()).getSelectedItem())).equals("16")) {
                    fftSize = 16;
                } else if (((String) (((JComboBox) evt.getSource()).getSelectedItem())).equals("32")) {
                    fftSize = 32;
                } else if (((String) (((JComboBox) evt.getSource()).getSelectedItem())).equals("64")) {
                    fftSize = 64;
                } else if (((String) (((JComboBox) evt.getSource()).getSelectedItem())).equals("128")) {
                    fftSize = 128;
                } else if (((String) (((JComboBox) evt.getSource()).getSelectedItem())).equals("256")) {
                    fftSize = 256;
                }
            }
        });
        fftConf.setPreferredSize(new Dimension(30, 18));
        fftPane.add(label8);
        fftPane.add(fftConf);
        paramConf.add(fftPane);
        JPanel paramConfBtn = new JPanel();
        EdgeLayout edgeLayout3 = new EdgeLayout();
        paramConfBtn.setLayout(edgeLayout3);
        JButton done = new JButton("OK");
        done.setActionCommand("paramsSet");
        done.addActionListener(this);
        edgeLayout3.setConstraints(done, 0, 50, 0, 0, EdgeLayout.LEFT_BOTTOM, EdgeLayout.NO_GROWTH);
        paramConfBtn.add(done);
        JButton cancel = new JButton("Cancel");
        cancel.setActionCommand("cancelConf");
        cancel.addActionListener(this);
        edgeLayout3.setConstraints(cancel, 0, 170, 0, 0, EdgeLayout.LEFT_BOTTOM, EdgeLayout.NO_GROWTH);
        paramConfBtn.add(cancel);
        configDialog.getContentPane().setLayout(new BorderLayout());
        configDialog.getContentPane().add(fitFunction, BorderLayout.NORTH);
        configDialog.getContentPane().add(paramConf, BorderLayout.CENTER);
        configDialog.getContentPane().add(paramConfBtn, BorderLayout.SOUTH);
        edgeLayout1.setConstraints(selection, 350, 10, 0, 0, EdgeLayout.TOP, EdgeLayout.NO_GROWTH);
        bpmPane.add(selection);
        edgeLayout.setConstraints(bpmPane, 10, 10, 0, 0, EdgeLayout.TOP, EdgeLayout.NO_GROWTH);
        add(bpmPane);
        plotDisplayPane = new JTabbedPane();
        plotDisplayPane.setPreferredSize(new Dimension(430, 600));
        plotDisplayPane.addTab("Phase", phasePlotPane);
        plotDisplayPane.addTab("Pos", posPlotPane);
        plotDisplayPane.addTab("phase diff.", phaseDiffPlotPane);
        edgeLayout.setConstraints(plotDisplayPane, 0, 480, 0, 0, EdgeLayout.TOP, EdgeLayout.NO_GROWTH);
        EdgeLayout el2 = new EdgeLayout();
        phasePlotPane.setLayout(el2);
        xPhasePlotPane = new BPMPlotPane(2);
        el2.setConstraints(xPhasePlotPane, 20, 20, 0, 0, EdgeLayout.TOP, EdgeLayout.NO_GROWTH);
        phasePlotPane.add(xPhasePlotPane);
        yPhasePlotPane = new BPMPlotPane(3);
        el2.setConstraints(yPhasePlotPane, 245, 20, 0, 0, EdgeLayout.TOP, EdgeLayout.NO_GROWTH);
        phasePlotPane.add(yPhasePlotPane);
        xBpmPlotPane = new BPMPlotPane(0);
        EdgeLayout el1 = new EdgeLayout();
        posPlotPane.setLayout(el1);
        el1.setConstraints(xBpmPlotPane, 20, 20, 0, 0, EdgeLayout.TOP, EdgeLayout.NO_GROWTH);
        posPlotPane.add(xBpmPlotPane);
        JPanel xTunePanel = new JPanel();
        xTunePanel.setLayout(new GridLayout(1, 2));
        JLabel xTuneLabel = new JLabel("X Tune:");
        numberFormat.setMaximumFractionDigits(6);
        dfXTune = new JTextField(15);
        dfXTune.setForeground(Color.RED);
        xTunePanel.add(xTuneLabel);
        xTunePanel.add(dfXTune);
        el1.setConstraints(xTunePanel, 245, 20, 0, 0, EdgeLayout.TOP, EdgeLayout.NO_GROWTH);
        posPlotPane.add(xTunePanel);
        yBpmPlotPane = new BPMPlotPane(1);
        el1.setConstraints(yBpmPlotPane, 275, 20, 0, 0, EdgeLayout.TOP, EdgeLayout.NO_GROWTH);
        posPlotPane.add(yBpmPlotPane);
        JPanel yTunePanel = new JPanel();
        yTunePanel.setLayout(new GridLayout(1, 2));
        JLabel yTuneLabel = new JLabel("Y Tune:");
        dfYTune = new JTextField(15);
        dfYTune.setForeground(Color.RED);
        yTunePanel.add(yTuneLabel);
        yTunePanel.add(dfYTune);
        el1.setConstraints(yTunePanel, 500, 20, 0, 0, EdgeLayout.TOP, EdgeLayout.NO_GROWTH);
        posPlotPane.add(yTunePanel);
        xPhDiffPlotPane = new BPMPlotPane(4);
        phaseDiffPlotPane.add(xPhDiffPlotPane);
        yPhDiffPlotPane = new BPMPlotPane(5);
        phaseDiffPlotPane.add(yPhDiffPlotPane);
        add(plotDisplayPane);
        for (int i = 0; i < allBPMs.size(); i++) {
            bpmTableModel.addRowName((allBPMs.get(i)).getId(), i);
            bpmTableModel.setValueAt("0", i, 1);
            bpmTableModel.setValueAt("0", i, 2);
            bpmTableModel.setValueAt("0", i, 3);
            bpmTableModel.setValueAt("0", i, 4);
            bpmTableModel.setValueAt(new Boolean(false), i, 5);
        }
        setPVChs = new Channel[qPSs.size()];
        rbPVChs = new Channel[qPSs.size()];
        setPVCell = new InputPVTableCell[qPSs.size()];
        rbPVCell = new InputPVTableCell[qPSs.size()];
        for (int i = 0; i < qPSs.size(); i++) {
            MagnetMainSupply mps = qPSs.get(i);
            setPVChs[i] = mps.getChannel(MagnetMainSupply.FIELD_SET_HANDLE);
            rbPVChs[i] = mps.getChannel(MagnetMainSupply.FIELD_RB_HANDLE);
            quadTableModel.addRowName(mps.getId(), i);
            quadTableModel.setValueAt("0", i, 3);
        }
    }

    protected void connectAll() {
        for (int i = 0; i < qPSs.size(); i++) {
            ConnectPV connectPV1 = new ConnectPV(setPVChs[i], this);
            Thread thread1 = new Thread(connectPV1);
            thread1.start();
            ConnectPV connectPV2 = new ConnectPV(rbPVChs[i], this);
            Thread thread2 = new Thread(connectPV2);
            thread2.start();
            getChannelVec(setPVChs[i]).add(setPVCell[i]);
            getChannelVec(rbPVChs[i]).add(rbPVCell[i]);
            Channel.flushIO();
        }
        final TableProdder prodder = new TableProdder(quadTableModel);
        prodder.start();
    }

    private void tuneByFit() {
        goodBPMs = new ArrayList<String>();
        if (isOnline) {
            if (!quadTableInit) {
                for (int i = 0; i < qPSs.size(); i++) {
                    setPVCell[i] = new InputPVTableCell(setPVChs[i], i, 1);
                    quadTableModel.addPVCell(setPVCell[i], i, 1);
                    rbPVCell[i] = new InputPVTableCell(rbPVChs[i], i, 2);
                    quadTableModel.addPVCell(rbPVCell[i], i, 2);
                }
            }
            quadTableInit = true;
        }
        HashMap<String, double[][]> bpmMap = null;
        quadTableModel.setAppMode(isOnline);
        if (!isOnline) {
            bpmPVLogId = myDoc.bpmPVLogId;
            defPVLogId = myDoc.defPVLogId;
            RingBPMTBTPVLog pvLog = new RingBPMTBTPVLog(bpmPVLogId);
            bpmMap = pvLog.getBPMMap();
        } else {
            connectAll();
        }
        TransferMapProbe myProbe = ProbeFactory.getTransferMapProbe(myDoc.getSelectedSequence(), new TransferMapTracker());
        Scenario scenario;
        HashMap<String, Double[]> goodBPMdata = new HashMap<String, Double[]>();
        try {
            scenario = Scenario.newScenarioFor(myDoc.getSelectedSequence());
            scenario.setProbe(myProbe);
            scenario.setSynchronizationMode(Scenario.SYNC_MODE_DESIGN);
            scenario.resetProbe();
            scenario.resync();
            scenario.run();
            TransferMapTrajectory traj = (TransferMapTrajectory) scenario.getTrajectory();
            double xSum = 0.;
            double ySum = 0.;
            double xAvgTune = 0.;
            double yAvgTune = 0.;
            int counter = 0;
            ArrayList<Double> xList = new ArrayList<Double>();
            ArrayList<Double> yList = new ArrayList<Double>();
            for (int i = 0; i < allBPMs.size(); i++) {
                BPM theBPM = allBPMs.get(i);
                posArray[i] = myDoc.getSelectedSequence().getPosition(theBPM);
                tuneMeasurement[i] = new TuneMeasurement();
                if (!isOnline) {
                    tuneMeasurement[i].setBPMData(bpmMap.get(theBPM.getId()));
                }
                tuneMeasurement[i].setBPM(theBPM);
                tuneMeasurement[i].setFitParameters(maxTime, len);
                if (!badBPMs.contains(new Integer(i))) {
                    Thread thread = new Thread(tuneMeasurement[i]);
                    thread.start();
                    try {
                        thread.join();
                        xTune[i] = tuneMeasurement[i].getXTune();
                        yTune[i] = tuneMeasurement[i].getYTune();
                        if (tuneMeasurement[i].getXPhase() > -100. && tuneMeasurement[i].getXPhase() < 0.) {
                            xPhase[i] = tuneMeasurement[i].getXPhase() - Math.floor(tuneMeasurement[i].getXPhase() / Math.PI / 2.) * Math.PI * 2.;
                        } else if (tuneMeasurement[i].getXPhase() < 100. && tuneMeasurement[i].getXPhase() >= 0.) {
                            xPhase[i] = Math.ceil(tuneMeasurement[i].getXPhase() / Math.PI / 2.) * Math.PI * 2. - tuneMeasurement[i].getXPhase();
                        } else {
                            xPhase[i] = 0.;
                        }
                        if (tuneMeasurement[i].getYPhase() > -100. && tuneMeasurement[i].getYPhase() < 0.) {
                            yPhase[i] = tuneMeasurement[i].getYPhase() - Math.floor(tuneMeasurement[i].getYPhase() / Math.PI / 2.) * Math.PI * 2.;
                        } else if (tuneMeasurement[i].getXPhase() < 100. && tuneMeasurement[i].getXPhase() >= 0.) {
                            yPhase[i] = Math.ceil(tuneMeasurement[i].getYPhase() / Math.PI / 2.) * Math.PI * 2. - tuneMeasurement[i].getYPhase();
                        } else {
                            yPhase[i] = 0.;
                        }
                        if (xPhase[i] != 0. && yPhase[i] != 0. && xTune[i] > 0.08 && yTune[i] > 0.08 && xTune[i] < 0.45 && yTune[i] < 0.45) {
                            xSum += xTune[i];
                            ySum += yTune[i];
                            xList.add(new Double(xTune[i]));
                            yList.add(new Double(yTune[i]));
                            counter++;
                            Double[] xyPair = new Double[2];
                            xyPair[0] = new Double(xPhase[i]);
                            xyPair[1] = new Double(yPhase[i]);
                            goodBPMdata.put(theBPM.getId(), xyPair);
                            goodBPMs.add(theBPM.getId());
                        }
                    } catch (InterruptedException ie) {
                        System.out.println("tune calculation for " + theBPM.getId() + " did not exit normally!");
                        xTune[i] = 0.;
                        yTune[i] = 0.;
                        xPhase[i] = 0.;
                        yPhase[i] = 0.;
                    }
                } else {
                    xTune[i] = 0.;
                    yTune[i] = 0.;
                }
                numberFormat.setMaximumFractionDigits(4);
                bpmTableModel.setValueAt(numberFormat.format(xTune[i]), i, 1);
                bpmTableModel.setValueAt(numberFormat.format(xPhase[i]), i, 2);
                bpmTableModel.setValueAt(numberFormat.format(yTune[i]), i, 3);
                bpmTableModel.setValueAt(numberFormat.format(yPhase[i]), i, 4);
            }
            xPhaseDiff = new double[goodBPMs.size()];
            yPhaseDiff = new double[goodBPMs.size()];
            goodPosArry = new double[goodBPMs.size()];
            xDiffPlot = new double[goodBPMs.size()];
            yDiffPlot = new double[goodBPMs.size()];
            double[] xModelPhase = new double[goodBPMs.size()];
            double[] yModelPhase = new double[goodBPMs.size()];
            double xPhaseDiff0 = goodBPMdata.get(goodBPMs.get(0))[0].doubleValue();
            double yPhaseDiff0 = goodBPMdata.get(goodBPMs.get(0))[1].doubleValue();
            TransferMapState state0 = (TransferMapState) traj.stateForElement(goodBPMs.get(0));
            double xModelPhase0 = state0.getBetatronPhase().getx();
            double yModelPhase0 = state0.getBetatronPhase().gety();
            for (int i = 0; i < goodBPMs.size(); i++) {
                goodPosArry[i] = myDoc.getSelectedSequence().getPosition(myDoc.getSelectedSequence().getNodeWithId(goodBPMs.get(i)));
                xPhaseDiff[i] = goodBPMdata.get(goodBPMs.get(i))[0].doubleValue() - xPhaseDiff0;
                yPhaseDiff[i] = goodBPMdata.get(goodBPMs.get(i))[1].doubleValue() - yPhaseDiff0;
                if (xPhaseDiff[i] < 0.) xPhaseDiff[i] = xPhaseDiff[i] + 2. * Math.PI;
                if (yPhaseDiff[i] < 0.) yPhaseDiff[i] = yPhaseDiff[i] + 2. * Math.PI;
                TransferMapState state = (TransferMapState) traj.stateForElement(goodBPMs.get(i));
                xModelPhase[i] = state.getBetatronPhase().getx() - xModelPhase0;
                yModelPhase[i] = state.getBetatronPhase().gety() - yModelPhase0;
                if (xModelPhase[i] < 0.) xModelPhase[i] = xModelPhase[i] + 2. * Math.PI;
                if (yModelPhase[i] < 0.) yModelPhase[i] = yModelPhase[i] + 2. * Math.PI;
                xDiffPlot[i] = xPhaseDiff[i] - xModelPhase[i];
                yDiffPlot[i] = yPhaseDiff[i] - yModelPhase[i];
                if (xDiffPlot[i] > 4.) xDiffPlot[i] = xDiffPlot[i] - 2. * Math.PI;
                if (xDiffPlot[i] < -4.) xDiffPlot[i] = xDiffPlot[i] + 2. * Math.PI;
                if (yDiffPlot[i] > 4.) yDiffPlot[i] = yDiffPlot[i] - 2. * Math.PI;
                if (yDiffPlot[i] < -4.) yDiffPlot[i] = yDiffPlot[i] + 2. * Math.PI;
            }
            if (counter != 0) {
                xAvgTune = xSum / counter;
                yAvgTune = ySum / counter;
            }
            double xSig = 0.;
            double ySig = 0.;
            for (int i = 0; i < xList.size(); i++) {
                xSig = xSig + (xList.get(i).doubleValue() - xAvgTune) * (xList.get(i).doubleValue() - xAvgTune);
                ySig = ySig + (yList.get(i).doubleValue() - yAvgTune) * (yList.get(i).doubleValue() - yAvgTune);
            }
            xSig = Math.sqrt(xSig) / xList.size();
            ySig = Math.sqrt(ySig) / yList.size();
            xTuneAvg.setForeground(Color.blue);
            yTuneAvg.setForeground(Color.blue);
            numberFormat.setMaximumFractionDigits(4);
            xTuneAvg.setText("avg. x tune = " + numberFormat.format(xAvgTune) + "+/-" + numberFormat.format(xSig));
            yTuneAvg.setText("avg. y tune = " + numberFormat.format(yAvgTune) + "+/-" + numberFormat.format(ySig));
            System.out.println("Got " + xList.size() + " sets of BPM data.");
            xPhasePlotPane.setDataArray(posArray, xPhase);
            xPhasePlotPane.plot();
            yPhasePlotPane.setDataArray(posArray, yPhase);
            yPhasePlotPane.plot();
            xPhDiffPlotPane.setDataArray(goodPosArry, xDiffPlot);
            xPhDiffPlotPane.plot();
            yPhDiffPlotPane.setDataArray(goodPosArry, yDiffPlot);
            yPhDiffPlotPane.plot();
        } catch (ModelException e) {
            System.out.println(e);
        }
        hasTune = true;
        if (isOnline) {
            qSetVals = new double[qPSs.size()];
            for (int i = 0; i < qPSs.size(); i++) {
                try {
                    qSetVals[i] = qPSs.get(i).getFieldSetting();
                } catch (ConnectionException ce) {
                    System.out.println(ce);
                } catch (GetException ge) {
                    System.out.println(ge);
                }
            }
            cqs = new CalcQuadSettings((Ring) myDoc.getSelectedSequence(), goodBPMs, this);
        }
        quadCorrBtn.setEnabled(true);
        dumpData.setEnabled(true);
    }

    private void tuneByFFT() {
        HashMap<String, double[][]> bpmMap = null;
        quadTableModel.setAppMode(isOnline);
        if (!isOnline) {
            bpmPVLogId = myDoc.bpmPVLogId;
            defPVLogId = myDoc.defPVLogId;
            RingBPMTBTPVLog pvLog = new RingBPMTBTPVLog(bpmPVLogId);
            bpmMap = pvLog.getBPMMap();
        } else {
            connectAll();
        }
        double xSum = 0.;
        double ySum = 0.;
        double xAvgTune = 0.;
        double yAvgTune = 0.;
        int xCounter = 0;
        int yCounter = 0;
        ArrayList<Double> xList = new ArrayList<Double>();
        ArrayList<Double> yList = new ArrayList<Double>();
        for (int i = 0; i < allBPMs.size(); i++) {
            BPM theBPM = allBPMs.get(i);
            posArray[i] = myDoc.getSelectedSequence().getPosition(theBPM);
            tuneMeasurement[i] = new TuneMeasurement();
            if (!isOnline) {
                tuneMeasurement[i].setBPMData(bpmMap.get(theBPM.getId()));
            }
            tuneMeasurement[i].setFFTArraySize(fftSize);
            tuneMeasurement[i].setTuneFromFit(false);
            tuneMeasurement[i].setBPM(theBPM);
            if (!badBPMs.contains(new Integer(i))) {
                Thread thread = new Thread(tuneMeasurement[i]);
                thread.start();
                try {
                    thread.join();
                    xTune[i] = tuneMeasurement[i].getXTune();
                    xPhase[i] = 0.;
                    yTune[i] = tuneMeasurement[i].getYTune();
                    yPhase[i] = 0.;
                    if (xTune[i] > 0.08 && xTune[i] < 0.45) {
                        xSum += xTune[i];
                        xList.add(new Double(xTune[i]));
                        xCounter++;
                    }
                    if (yTune[i] > 0.08 && xTune[i] < 0.45) {
                        ySum += yTune[i];
                        yList.add(new Double(yTune[i]));
                        yCounter++;
                    }
                    numberFormat.setMaximumFractionDigits(4);
                    bpmTableModel.setValueAt(numberFormat.format(xTune[i]), i, 1);
                    bpmTableModel.setValueAt(numberFormat.format(xPhase[i]), i, 2);
                    bpmTableModel.setValueAt(numberFormat.format(yTune[i]), i, 3);
                    bpmTableModel.setValueAt(numberFormat.format(yPhase[i]), i, 4);
                } catch (InterruptedException ie) {
                }
            }
        }
        if (xCounter != 0) {
            xAvgTune = xSum / xCounter;
        }
        if (yCounter != 0) {
            yAvgTune = ySum / yCounter;
        }
        double xSig = 0.;
        double ySig = 0.;
        for (int i = 0; i < xList.size(); i++) {
            xSig = xSig + (xList.get(i).doubleValue() - xAvgTune) * (xList.get(i).doubleValue() - xAvgTune);
        }
        for (int i = 0; i < yList.size(); i++) {
            ySig = ySig + (yList.get(i).doubleValue() - yAvgTune) * (yList.get(i).doubleValue() - yAvgTune);
        }
        xSig = Math.sqrt(xSig) / xList.size();
        ySig = Math.sqrt(ySig) / yList.size();
        xTuneAvg.setForeground(Color.blue);
        yTuneAvg.setForeground(Color.blue);
        xTuneAvg.setText("avg. x tune = " + numberFormat.format(xAvgTune) + "+/-" + numberFormat.format(xSig));
        yTuneAvg.setText("avg. y tune = " + numberFormat.format(yAvgTune) + "+/-" + numberFormat.format(ySig));
        System.out.println("Got " + xList.size() + " sets of BPM x data.");
        System.out.println("Got " + yList.size() + " sets of BPM y data.");
        hasTune = true;
    }

    protected void plotBPMData(int ind) {
        xBpmPlotPane.setDataArray(tuneMeasurement[ind].getXArray());
        xBpmPlotPane.setFittedData(tuneMeasurement[ind].getXFittedData());
        xBpmPlotPane.plot();
        yBpmPlotPane.setDataArray(tuneMeasurement[ind].getYArray());
        yBpmPlotPane.setFittedData(tuneMeasurement[ind].getYFittedData());
        yBpmPlotPane.plot();
        numberFormat.setMaximumFractionDigits(4);
        String xtune = numberFormat.format(xTune[ind]) + " +/- " + numberFormat.format(tuneMeasurement[ind].getXTuneError());
        dfXTune.setText(xtune);
        String ytune = numberFormat.format(yTune[ind]) + " +/- " + numberFormat.format(tuneMeasurement[ind].getYTuneError());
        dfYTune.setText(ytune);
    }

    public void actionPerformed(ActionEvent ev) {
        if (ev.getActionCommand().equals("configuration")) {
            configDialog.setVisible(true);
        } else if (ev.getActionCommand().equals("paramsSet")) {
            maxTime = Math.round((int) df6.getValue());
            len = Math.round((int) df7.getValue());
            configDialog.setVisible(false);
        } else if (ev.getActionCommand().equals("cancelConf")) {
            configDialog.setVisible(false);
        } else if (ev.getActionCommand().equals("findQuadError")) {
            progBar.setValue(0);
            if (!isOnline) {
                PVLoggerDataSource plds = new PVLoggerDataSource(defPVLogId);
                HashMap<String, Double> quadMap = plds.getQuadPSMap();
                qSetVals = new double[qPSs.size()];
                for (int i = 0; i < qPSs.size(); i++) {
                    qSetVals[i] = quadMap.get(qPSs.get(i).getChannel(MagnetMainSupply.FIELD_SET_HANDLE).getId());
                    quadTableModel.setValueAt(numberFormat.format(qSetVals[i]), i, 1);
                }
                cqs = new CalcQuadSettings((Ring) myDoc.getSelectedSequence(), goodBPMs, this);
                Thread thread = new Thread(cqs);
                thread.start();
            } else {
                Thread thread = new Thread(cqs);
                thread.start();
            }
        } else if (ev.getActionCommand().equals("setQuads")) {
            try {
                for (int i = 0; i < qPSs.size(); i++) {
                    qPSs.get(i).setField(numberFormat.parse((String) quadTableModel.getValueAt(i, 4)).doubleValue());
                }
            } catch (PutException pe) {
                System.out.println(pe);
            } catch (ConnectionException ce) {
                System.out.println(ce);
            } catch (ParseException pe) {
                System.out.println(pe);
            }
        } else if (ev.getActionCommand().equals("dumpData")) {
            String currentDirectory = _datFileTracker.getRecentFolderPath();
            JFileChooser fileChooser = new JFileChooser(currentDirectory);
            int status = fileChooser.showSaveDialog(this);
            if (status == JFileChooser.APPROVE_OPTION) {
                _datFileTracker.cacheURL(fileChooser.getSelectedFile());
                File file = fileChooser.getSelectedFile();
                try {
                    FileWriter fileWriter = new FileWriter(file);
                    NumberFormat nf = NumberFormat.getNumberInstance();
                    nf.setMaximumFractionDigits(5);
                    nf.setMinimumFractionDigits(5);
                    fileWriter.write("BPM_Id\t\t\t" + "s\t\t" + "xTune\t" + "xPhase\t" + "yTune\t" + "yPhase" + "\n");
                    for (int i = 0; i < xTune.length; i++) {
                        fileWriter.write((allBPMs.get(i)).getId() + "\t" + numberFormat.format(myDoc.getSelectedSequence().getPosition(allBPMs.get(i))) + "\t" + numberFormat.format(xTune[i]) + "\t" + numberFormat.format(xPhase[i]) + "\t" + numberFormat.format(yTune[i]) + "\t" + numberFormat.format(yPhase[i]) + "\n");
                    }
                    String comments = startTime.toString();
                    comments = comments + "\n" + "For Ring Measurement Application";
                    snapshot.setComment(comments);
                    snapshot1.setComment(comments);
                    loggerSession.publishSnapshot(snapshot);
                    loggerSession1.publishSnapshot(snapshot1);
                    pvLoggerId = snapshot.getId();
                    pvLoggerId1 = snapshot1.getId();
                    fileWriter.write("PVLoggerID = " + pvLoggerId + "\tPVLoggerId = " + pvLoggerId1 + "\n");
                    fileWriter.close();
                } catch (IOException ie) {
                    JFrame frame = new JFrame();
                    JOptionPane.showMessageDialog(frame, "Cannot open the file" + file.getName() + "for writing", "Warning!", JOptionPane.PLAIN_MESSAGE);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                }
            }
        }
    }

    protected void setSelectedBPM(String theBPM) {
        selectedBPM = theBPM;
        System.out.println("Selected BPM = " + selectedBPM);
    }

    protected String getSelectedBPM() {
        return selectedBPM;
    }

    /** ConnectionListener interface */
    public void connectionMade(Channel aChannel) {
        connectMons(aChannel);
    }

    /** ConnectionListener interface */
    public void connectionDropped(Channel aChannel) {
    }

    /** internal method to connect the monitors */
    private void connectMons(Channel p_chan) {
        Vector chanVec;
        try {
            chanVec = getChannelVec(p_chan);
            for (int i = 0; i < chanVec.size(); i++) {
                mons.add(p_chan.addMonitorValue((InputPVTableCell) chanVec.elementAt(i), Monitor.VALUE));
            }
            chanVec.removeAllElements();
        } catch (ConnectionException e) {
            System.out.println("Connection Exception");
        } catch (MonitorException e) {
            System.out.println("Monitor Exception");
        }
    }

    /** get the list of table cells monitoring the prescibed channel */
    private Vector<InputPVTableCell> getChannelVec(Channel p_chan) {
        if (!monitorQueues.containsKey(p_chan.channelName())) {
            monitorQueues.put(p_chan.channelName(), new Vector<InputPVTableCell>());
        }
        return monitorQueues.get(p_chan.channelName());
    }

    protected void setAppMode(boolean isOn) {
        isOnline = isOn;
    }
}
