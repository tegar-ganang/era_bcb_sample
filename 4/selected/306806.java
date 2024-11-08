package gov.sns.apps.mpx;

import gov.sns.tools.apputils.SimpleChartPopupMenu;
import gov.sns.tools.beam.BeamConstants;
import gov.sns.tools.beam.TraceXalUnitConverter;
import gov.sns.tools.beam.Twiss;
import gov.sns.xal.model.ModelException;
import gov.sns.xal.model.probe.traj.EnvelopeProbeState;
import gov.sns.xal.model.probe.traj.ParticleProbeState;
import gov.sns.xal.model.probe.traj.TransferMapState;
import gov.sns.xal.model.probe.traj.ProbeState;
import gov.sns.xal.model.mpx.ModelProxy;
import gov.sns.xal.model.mpx.MPXStopWatch;
import gov.sns.xal.model.scenario.Scenario;
import gov.sns.xal.smf.AcceleratorSeq;
import gov.sns.xal.smf.AcceleratorNode;
import gov.sns.xal.tools.widgets.XALSynopticPanel;
import gov.sns.xal.smf.impl.qualify.NotTypeQualifier;
import gov.sns.xal.smf.impl.qualify.OrTypeQualifier;
import gov.sns.tools.plot.*;
import gov.sns.tools.apputils.files.RecentFileTracker;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;

public class MPXTwissPlot extends JPanel implements ItemListener, ActionListener {

    private XALSynopticPanel synoptic;

    FunctionGraphsJPanel chart = null;

    JCheckBox showX = new JCheckBox("x");

    JCheckBox showXP = new JCheckBox("x'");

    JCheckBox showY = new JCheckBox("y");

    JCheckBox showYP = new JCheckBox("y'");

    JCheckBox showZ = new JCheckBox("z");

    JCheckBox showZP = new JCheckBox("z'");

    JCheckBox showSigmaX = new JCheckBox("Sigma X");

    JCheckBox showSigmaY = new JCheckBox("Sigma Y");

    JCheckBox showSigmaZ = new JCheckBox("Sigma Z");

    JCheckBox showAlphaX = new JCheckBox("Alpha X");

    JCheckBox showBetaX = new JCheckBox("Beta X");

    JCheckBox showAlphaY = new JCheckBox("Alpha Y");

    JCheckBox showBetaY = new JCheckBox("Beta Y");

    JCheckBox showAlphaZ = new JCheckBox("Alpha Z");

    JCheckBox showBetaZ = new JCheckBox("Beta Z");

    JCheckBox showEmitX = new JCheckBox("Emit X");

    JCheckBox showEmitY = new JCheckBox("Emit Y");

    JCheckBox showEmitZ = new JCheckBox("Emit Z");

    JCheckBox showEng = new JCheckBox("W");

    JCheckBox showBpmX = new JCheckBox("BPM X");

    JCheckBox showBpmY = new JCheckBox("BPM Y");

    JCheckBox showWsX = new JCheckBox("WS X");

    JCheckBox showWsY = new JCheckBox("WS Y");

    JLabel dummy = new JLabel("");

    JRadioButton diff = new JRadioButton("Diff.");

    JRadioButton current = new JRadioButton("Current");

    ButtonGroup group = new ButtonGroup();

    JButton exportData = new JButton("Save Data");

    final Color xColor = new Color(255, 0, 0);

    final Color xpColor = new Color(255, 165, 0);

    final Color yColor = new Color(0, 0, 255);

    final Color ypColor = new Color(173, 255, 47);

    final Color zColor = new Color(0, 255, 0);

    final Color zpColor = new Color(0, 0, 139);

    final Color sigmaxColor = new Color(0, 0, 70);

    final Color sigmayColor = new Color(0, 140, 70);

    final Color sigmazColor = new Color(0, 70, 70);

    final Color alphaxColor = new Color(47, 255, 225);

    final Color betaxColor = new Color(148, 0, 211);

    final Color alphayColor = new Color(0, 255, 255);

    final Color betayColor = new Color(255, 69, 0);

    final Color alphazColor = new Color(255, 255, 0);

    final Color betazColor = new Color(0, 0, 0);

    final Color emitxColor = new Color(225, 150, 0);

    final Color emityColor = new Color(120, 225, 255);

    final Color emitzColor = new Color(50, 25, 175);

    final Color engColor = new Color(200, 0, 70);

    final Color bpmxColor = new Color(120, 70, 70);

    final Color bpmyColor = new Color(0, 140, 70);

    final Color wsxColor = new Color(240, 20, 20);

    final Color wsyColor = new Color(120, 0, 140);

    BasicGraphData xds, xpds, alphaxds, betaxds;

    BasicGraphData yds, ypds, alphayds, betayds;

    BasicGraphData zds, zpds, alphazds, betazds;

    BasicGraphData sigmaxds, sigmayds, sigmazds;

    BasicGraphData emitxds, emityds, emitzds;

    BasicGraphData engds;

    BasicGraphData bpmxds, bpmyds;

    BasicGraphData wsxds, wsyds;

    double posData[][];

    double xData[][];

    double xpData[][];

    double alphaxData[][];

    double betaxData[][];

    double yData[][];

    double ypData[][];

    double alphayData[][];

    double betayData[][];

    double zData[][];

    double zpData[][];

    double alphazData[][];

    double betazData[][];

    double emitxData[][];

    double emityData[][];

    double emitzData[][];

    double sigmaxData[][];

    double sigmayData[][];

    double sigmazData[][];

    double bpmPosData[][];

    double wsPosData[][];

    double engData[][];

    double bpmxData[][];

    double bpmyData[][];

    double wsxData[][];

    double wsyData[][];

    double xDataDiff[][];

    double xpDataDiff[][];

    double alphaxDataDiff[][];

    double betaxDataDiff[][];

    double yDataDiff[][];

    double ypDataDiff[][];

    double alphayDataDiff[][];

    double betayDataDiff[][];

    double zDataDiff[][];

    double zpDataDiff[][];

    double alphazDataDiff[][];

    double betazDataDiff[][];

    double emitxDataDiff[][];

    double emityDataDiff[][];

    double emitzDataDiff[][];

    double sigmaxDataDiff[][];

    double sigmayDataDiff[][];

    double sigmazDataDiff[][];

    double engDataDiff[][];

    double bpmxDataDiff[][];

    double bpmyDataDiff[][];

    double wsxDataDiff[][];

    double wsyDataDiff[][];

    double xDataOld[][];

    double xpDataOld[][];

    double alphaxDataOld[][];

    double betaxDataOld[][];

    double yDataOld[][];

    double ypDataOld[][];

    double alphayDataOld[][];

    double betayDataOld[][];

    double zDataOld[][];

    double zpDataOld[][];

    double alphazDataOld[][];

    double betazDataOld[][];

    double emitxDataOld[][];

    double emityDataOld[][];

    double emitzDataOld[][];

    double sigmaxDataOld[][];

    double sigmayDataOld[][];

    double sigmazDataOld[][];

    double bpmPosDataOld[][];

    double engDataOld[][];

    double bpmxDataOld[][];

    double bpmyDataOld[][];

    double wsxDataOld[][];

    double wsyDataOld[][];

    int elementCount = 0;

    int oldProbeType = 9999;

    MPXProxy myMP;

    TraceXalUnitConverter uc;

    String[] elementIds;

    private RecentFileTracker _savedFileTracker;

    BpmData bpmData;

    WsData wsData;

    File wsFile;

    MPXMainWindow myWindow;

    boolean bpmReset = true;

    boolean wsReset = true;

    public MPXTwissPlot(MPXMainWindow win) {
        myWindow = win;
        _savedFileTracker = new RecentFileTracker(1, this.getClass(), "recent_saved_file");
        JPanel selection = new JPanel();
        selection.setBorder(BorderFactory.createTitledBorder("Select... "));
        selection.setLayout(new GridLayout(14, 2));
        showX.setForeground(xColor);
        showX.addItemListener(this);
        showXP.setForeground(xpColor);
        showXP.addItemListener(this);
        showY.setForeground(yColor);
        showY.addItemListener(this);
        showYP.setForeground(ypColor);
        showYP.addItemListener(this);
        showZ.setForeground(zColor);
        showZ.addItemListener(this);
        showZP.setForeground(zpColor);
        showZP.addItemListener(this);
        showSigmaX.setForeground(sigmaxColor);
        showSigmaX.addItemListener(this);
        showSigmaY.setForeground(sigmayColor);
        showSigmaY.addItemListener(this);
        showSigmaZ.setForeground(sigmazColor);
        showSigmaZ.addItemListener(this);
        showAlphaX.setForeground(alphaxColor);
        showAlphaX.addItemListener(this);
        showBetaX.setForeground(betaxColor);
        showBetaX.addItemListener(this);
        showAlphaY.setForeground(alphayColor);
        showAlphaY.addItemListener(this);
        showBetaY.setForeground(betayColor);
        showBetaY.addItemListener(this);
        showAlphaZ.setForeground(alphazColor);
        showAlphaZ.addItemListener(this);
        showBetaZ.setForeground(betazColor);
        showBetaZ.addItemListener(this);
        showEmitX.setForeground(emitxColor);
        showEmitX.addItemListener(this);
        showEmitY.setForeground(emityColor);
        showEmitY.addItemListener(this);
        showEmitZ.setForeground(emitzColor);
        showEmitZ.addItemListener(this);
        showEng.setForeground(engColor);
        showEng.addItemListener(this);
        showBpmX.setForeground(bpmxColor);
        showBpmX.addItemListener(this);
        showBpmY.setForeground(bpmyColor);
        showBpmY.addItemListener(this);
        showWsX.setForeground(wsxColor);
        showWsX.addItemListener(this);
        showWsY.setForeground(wsyColor);
        showWsY.addItemListener(this);
        current.setActionCommand("plot current");
        current.addActionListener(this);
        diff.setActionCommand("plot difference");
        diff.addActionListener(this);
        diff.setEnabled(false);
        exportData.setActionCommand("export data");
        exportData.addActionListener(this);
        selection.add(showX);
        selection.add(showXP);
        selection.add(showY);
        selection.add(showYP);
        selection.add(showZ);
        selection.add(showZP);
        selection.add(showAlphaX);
        selection.add(showBetaX);
        selection.add(showEmitX);
        selection.add(showSigmaX);
        selection.add(showAlphaY);
        selection.add(showBetaY);
        selection.add(showEmitY);
        selection.add(showSigmaY);
        selection.add(showAlphaZ);
        selection.add(showBetaZ);
        selection.add(showEmitZ);
        selection.add(showSigmaZ);
        selection.add(showBpmX);
        selection.add(showBpmY);
        selection.add(showWsX);
        selection.add(showWsY);
        selection.add(showEng);
        selection.add(dummy);
        showBpmX.setEnabled(false);
        showBpmY.setEnabled(false);
        showWsX.setEnabled(false);
        showWsY.setEnabled(false);
        group.add(current);
        group.add(diff);
        selection.add(current);
        selection.add(diff);
        selection.add(exportData);
        xds = new BasicGraphData();
        xpds = new BasicGraphData();
        alphaxds = new BasicGraphData();
        betaxds = new BasicGraphData();
        yds = new BasicGraphData();
        ypds = new BasicGraphData();
        alphayds = new BasicGraphData();
        betayds = new BasicGraphData();
        zds = new BasicGraphData();
        zpds = new BasicGraphData();
        alphazds = new BasicGraphData();
        betazds = new BasicGraphData();
        emitxds = new BasicGraphData();
        emityds = new BasicGraphData();
        emitzds = new BasicGraphData();
        sigmaxds = new BasicGraphData();
        sigmayds = new BasicGraphData();
        sigmazds = new BasicGraphData();
        engds = new BasicGraphData();
        bpmxds = new BasicGraphData();
        bpmyds = new BasicGraphData();
        wsxds = new BasicGraphData();
        wsyds = new BasicGraphData();
        chart = makeChart();
        initSynopticView();
        setLayout(new GridBagLayout());
        add(chart, new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        add(selection, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        add(synoptic, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    }

    /**
     * initialize the data chart and set colors
     */
    private FunctionGraphsJPanel makeChart() {
        FunctionGraphsJPanel c = new FunctionGraphsJPanel();
        c.setAxisNameX("s (m)");
        xds.setGraphColor(xColor);
        xds.setDrawLinesOn(true);
        xpds.setGraphColor(xpColor);
        xpds.setDrawLinesOn(true);
        alphaxds.setGraphColor(alphaxColor);
        alphaxds.setDrawLinesOn(true);
        betaxds.setGraphColor(betaxColor);
        betaxds.setDrawLinesOn(true);
        yds.setGraphColor(yColor);
        yds.setDrawLinesOn(true);
        ypds.setGraphColor(ypColor);
        ypds.setDrawLinesOn(true);
        alphayds.setGraphColor(alphayColor);
        alphayds.setDrawLinesOn(true);
        betayds.setGraphColor(betayColor);
        betayds.setDrawLinesOn(true);
        zds.setGraphColor(zColor);
        zds.setDrawLinesOn(true);
        zpds.setGraphColor(zpColor);
        zpds.setDrawLinesOn(true);
        alphazds.setGraphColor(alphazColor);
        alphazds.setDrawLinesOn(true);
        betazds.setGraphColor(betazColor);
        betazds.setDrawLinesOn(true);
        emitxds.setGraphColor(emitxColor);
        emitxds.setDrawLinesOn(true);
        emityds.setGraphColor(emityColor);
        emityds.setDrawLinesOn(true);
        emitzds.setGraphColor(emitzColor);
        emitzds.setDrawLinesOn(true);
        sigmaxds.setGraphColor(sigmaxColor);
        sigmaxds.setDrawLinesOn(true);
        sigmayds.setGraphColor(sigmayColor);
        sigmayds.setDrawLinesOn(true);
        sigmazds.setGraphColor(sigmazColor);
        sigmazds.setDrawLinesOn(true);
        engds.setGraphColor(engColor);
        engds.setDrawLinesOn(true);
        bpmxds.setGraphColor(bpmxColor);
        bpmxds.setDrawLinesOn(false);
        xds.setGraphColor(xColor);
        xds.setDrawLinesOn(true);
        xpds.setGraphColor(xpColor);
        xpds.setDrawLinesOn(true);
        alphaxds.setGraphColor(alphaxColor);
        alphaxds.setDrawLinesOn(true);
        betaxds.setGraphColor(betaxColor);
        betaxds.setDrawLinesOn(true);
        yds.setGraphColor(yColor);
        yds.setDrawLinesOn(true);
        ypds.setGraphColor(ypColor);
        ypds.setDrawLinesOn(true);
        alphayds.setGraphColor(alphayColor);
        alphayds.setDrawLinesOn(true);
        betayds.setGraphColor(betayColor);
        betayds.setDrawLinesOn(true);
        zds.setGraphColor(zColor);
        zds.setDrawLinesOn(true);
        zpds.setGraphColor(zpColor);
        zpds.setDrawLinesOn(true);
        alphazds.setGraphColor(alphazColor);
        alphazds.setDrawLinesOn(true);
        betazds.setGraphColor(betazColor);
        betazds.setDrawLinesOn(true);
        emitxds.setGraphColor(emitxColor);
        emitxds.setDrawLinesOn(true);
        emityds.setGraphColor(emityColor);
        emityds.setDrawLinesOn(true);
        emitzds.setGraphColor(emitzColor);
        emitzds.setDrawLinesOn(true);
        sigmaxds.setGraphColor(sigmaxColor);
        sigmaxds.setDrawLinesOn(true);
        sigmayds.setGraphColor(sigmayColor);
        sigmayds.setDrawLinesOn(true);
        sigmazds.setGraphColor(sigmazColor);
        sigmazds.setDrawLinesOn(true);
        engds.setGraphColor(engColor);
        engds.setDrawLinesOn(true);
        bpmxds.setGraphColor(bpmxColor);
        bpmxds.setDrawLinesOn(false);
        bpmyds.setGraphColor(bpmyColor);
        bpmyds.setDrawLinesOn(false);
        wsxds.setGraphColor(wsxColor);
        wsxds.setDrawLinesOn(false);
        wsyds.setGraphColor(wsyColor);
        wsyds.setDrawLinesOn(false);
        bpmyds.setGraphColor(bpmyColor);
        bpmyds.setDrawLinesOn(false);
        wsxds.setGraphColor(wsxColor);
        wsxds.setDrawLinesOn(false);
        wsyds.setGraphColor(wsyColor);
        wsyds.setDrawLinesOn(false);
        c.setBackground(Color.white);
        SimpleChartPopupMenu popupMenu = SimpleChartPopupMenu.addPopupMenuTo(c);
        c.setGraphBackGroundColor(Color.white);
        c.setPreferredSize(new Dimension(100, 100));
        c.setMinimumSize(new Dimension(100, 100));
        return (c);
    }

    public void setPlotCheckbox() {
        if (myMP != null) {
            int probeType = myMP.getProbeType();
            if (probeType == ModelProxy.ENVELOPE_PROBE) {
                showAlphaX.setEnabled(true);
                showBetaX.setEnabled(true);
                showAlphaY.setEnabled(true);
                showBetaY.setEnabled(true);
                showAlphaZ.setEnabled(true);
                showBetaZ.setEnabled(true);
                showEmitX.setEnabled(true);
                showEmitY.setEnabled(true);
                showEmitZ.setEnabled(true);
                showSigmaX.setEnabled(true);
                showSigmaY.setEnabled(true);
                showSigmaZ.setEnabled(true);
                showEng.setEnabled(true);
            }
            if (probeType == ModelProxy.PARTICLE_PROBE) {
                showAlphaX.setEnabled(false);
                showBetaX.setEnabled(false);
                showAlphaY.setEnabled(false);
                showBetaY.setEnabled(false);
                showAlphaZ.setEnabled(false);
                showBetaZ.setEnabled(false);
                showEmitX.setEnabled(false);
                showEmitY.setEnabled(false);
                showEmitZ.setEnabled(false);
                showSigmaX.setEnabled(false);
                showSigmaY.setEnabled(false);
                showSigmaZ.setEnabled(false);
                showEng.setEnabled(true);
            }
            if (probeType == ModelProxy.TRANSFERMAP_PROBE) {
                showAlphaX.setEnabled(true);
                showBetaX.setEnabled(true);
                showAlphaY.setEnabled(true);
                showBetaY.setEnabled(true);
                showAlphaZ.setEnabled(false);
                showBetaZ.setEnabled(false);
                showEmitX.setEnabled(false);
                showEmitY.setEnabled(false);
                showEmitZ.setEnabled(false);
                showSigmaX.setEnabled(false);
                showSigmaY.setEnabled(false);
                showSigmaZ.setEnabled(false);
                showEng.setEnabled(false);
            }
        }
    }

    /**
     *  Get data from ModelProxy.
     */
    public void updatePlotData(MPXProxy mp) {
        if (xData != null) {
            diff.setEnabled(true);
            xDataDiff = new double[1][xData[0].length];
            xpDataDiff = new double[1][xData[0].length];
            alphaxDataDiff = new double[1][xData[0].length];
            betaxDataDiff = new double[1][xData[0].length];
            yDataDiff = new double[1][xData[0].length];
            ypDataDiff = new double[1][xData[0].length];
            alphayDataDiff = new double[1][xData[0].length];
            betayDataDiff = new double[1][xData[0].length];
            zDataDiff = new double[1][xData[0].length];
            zpDataDiff = new double[1][xData[0].length];
            alphazDataDiff = new double[1][xData[0].length];
            betazDataDiff = new double[1][xData[0].length];
            emitxDataDiff = new double[1][xData[0].length];
            emityDataDiff = new double[1][xData[0].length];
            emitzDataDiff = new double[1][xData[0].length];
            sigmaxDataDiff = new double[1][xData[0].length];
            sigmayDataDiff = new double[1][xData[0].length];
            sigmazDataDiff = new double[1][xData[0].length];
            engDataDiff = new double[1][xData[0].length];
            xDataOld = new double[1][xData[0].length];
            xpDataOld = new double[1][xData[0].length];
            alphaxDataOld = new double[1][xData[0].length];
            betaxDataOld = new double[1][xData[0].length];
            yDataOld = new double[1][xData[0].length];
            ypDataOld = new double[1][xData[0].length];
            alphayDataOld = new double[1][xData[0].length];
            betayDataOld = new double[1][xData[0].length];
            zDataOld = new double[1][xData[0].length];
            zpDataOld = new double[1][xData[0].length];
            alphazDataOld = new double[1][xData[0].length];
            betazDataOld = new double[1][xData[0].length];
            emitxDataOld = new double[1][xData[0].length];
            emityDataOld = new double[1][xData[0].length];
            emitzDataOld = new double[1][xData[0].length];
            sigmaxDataOld = new double[1][xData[0].length];
            sigmayDataOld = new double[1][xData[0].length];
            sigmazDataOld = new double[1][xData[0].length];
            engDataOld = new double[1][xData[0].length];
            System.arraycopy(xData[0], 0, xDataOld[0], 0, xData[0].length);
            System.arraycopy(xpData[0], 0, xpDataOld[0], 0, xpData[0].length);
            System.arraycopy(yData[0], 0, yDataOld[0], 0, yData[0].length);
            System.arraycopy(ypData[0], 0, ypDataOld[0], 0, ypData[0].length);
            System.arraycopy(zData[0], 0, zDataOld[0], 0, zData[0].length);
            System.arraycopy(zpData[0], 0, zpDataOld[0], 0, zpData[0].length);
            System.arraycopy(alphaxData[0], 0, alphaxDataOld[0], 0, alphaxData[0].length);
            System.arraycopy(betaxData[0], 0, betaxDataOld[0], 0, betaxData[0].length);
            System.arraycopy(alphayData[0], 0, alphayDataOld[0], 0, alphayData[0].length);
            System.arraycopy(betayData[0], 0, betayDataOld[0], 0, betayData[0].length);
            System.arraycopy(alphazData[0], 0, alphazDataOld[0], 0, alphazData[0].length);
            System.arraycopy(betazData[0], 0, betazDataOld[0], 0, betazData[0].length);
            System.arraycopy(emitxData[0], 0, emitxDataOld[0], 0, emitxData[0].length);
            System.arraycopy(emityData[0], 0, emityDataOld[0], 0, emityData[0].length);
            System.arraycopy(emitzData[0], 0, emitzDataOld[0], 0, emitzData[0].length);
            System.arraycopy(sigmaxData[0], 0, sigmaxDataOld[0], 0, sigmaxData[0].length);
            System.arraycopy(sigmayData[0], 0, sigmayDataOld[0], 0, sigmayData[0].length);
            System.arraycopy(sigmazData[0], 0, sigmazDataOld[0], 0, sigmazData[0].length);
            System.arraycopy(engData[0], 0, engDataOld[0], 0, engData[0].length);
        }
        AcceleratorSeq accSeq = mp.getAcceleratorSequence();
        OrTypeQualifier otq = new OrTypeQualifier();
        otq.or("Bnch");
        otq.or("SCLCavity");
        NotTypeQualifier ntq = new NotTypeQualifier(otq);
        java.util.List<AcceleratorNode> allNodes = accSeq.getAllNodesWithQualifier(ntq);
        allNodes = AcceleratorSeq.filterNodesByStatus(allNodes, true);
        elementCount = allNodes.size();
        myMP = mp;
        int probeType = mp.getProbeType();
        ArrayList states = new ArrayList();
        if (probeType == ModelProxy.ENVELOPE_PROBE) {
            showAlphaX.setEnabled(true);
            showBetaX.setEnabled(true);
            showAlphaY.setEnabled(true);
            showBetaY.setEnabled(true);
            showAlphaZ.setEnabled(true);
            showBetaZ.setEnabled(true);
            showEmitX.setEnabled(true);
            showEmitY.setEnabled(true);
            showEmitZ.setEnabled(true);
            showSigmaX.setEnabled(true);
            showSigmaY.setEnabled(true);
            showSigmaZ.setEnabled(true);
            showEng.setEnabled(true);
        }
        if (probeType == ModelProxy.PARTICLE_PROBE) {
            showAlphaX.setEnabled(false);
            showBetaX.setEnabled(false);
            showAlphaY.setEnabled(false);
            showBetaY.setEnabled(false);
            showAlphaZ.setEnabled(false);
            showBetaZ.setEnabled(false);
            showEmitX.setEnabled(false);
            showEmitY.setEnabled(false);
            showEmitZ.setEnabled(false);
            showSigmaX.setEnabled(false);
            showSigmaY.setEnabled(false);
            showSigmaZ.setEnabled(false);
            showEng.setEnabled(true);
        }
        if (probeType == ModelProxy.TRANSFERMAP_PROBE) {
            showAlphaX.setEnabled(true);
            showBetaX.setEnabled(true);
            showAlphaY.setEnabled(true);
            showBetaY.setEnabled(true);
            showAlphaZ.setEnabled(false);
            showBetaZ.setEnabled(false);
            showEmitX.setEnabled(false);
            showEmitY.setEnabled(false);
            showEmitZ.setEnabled(false);
            showSigmaX.setEnabled(false);
            showSigmaY.setEnabled(false);
            showSigmaZ.setEnabled(false);
            showEng.setEnabled(false);
        }
        if (mp.getChannelSource().equals(Scenario.SYNC_MODE_LIVE) || mp.getChannelSource().equals(Scenario.SYNC_MODE_RF_DESIGN)) {
            showBpmX.setEnabled(true);
            showBpmY.setEnabled(true);
            showWsX.setEnabled(true);
            showWsY.setEnabled(true);
            bpmData = new BpmData(mp.getAcceleratorSequence(), mp);
            double[][] tmpXData = bpmData.getXData();
            double[][] tmpYData = bpmData.getYData();
            bpmPosData = new double[1][tmpXData[0].length];
            bpmxData = new double[1][tmpXData[0].length];
            bpmyData = new double[1][tmpXData[0].length];
            bpmxDataDiff = new double[1][tmpXData[0].length];
            bpmyDataDiff = new double[1][tmpXData[0].length];
            for (int i = 0; i < tmpXData[0].length; i++) {
                bpmPosData[0][i] = tmpXData[0][i];
                bpmxData[0][i] = tmpXData[1][i];
                bpmyData[0][i] = tmpYData[1][i];
                if (bpmxDataOld != null) {
                    if (bpmxDataOld[0].length == bpmxData[0].length) {
                        bpmxDataDiff[0][i] = bpmxData[0][i] - bpmxDataOld[0][i];
                        bpmyDataDiff[0][i] = bpmyData[0][i] - bpmyDataOld[0][i];
                    }
                }
            }
            bpmxDataOld = new double[1][tmpXData[0].length];
            bpmyDataOld = new double[1][tmpXData[0].length];
            System.arraycopy(bpmxData[0], 0, bpmxDataOld[0], 0, bpmxData[0].length);
            System.arraycopy(bpmyData[0], 0, bpmyDataOld[0], 0, bpmyData[0].length);
            wsData = new WsData(mp.getAcceleratorSequence(), mp);
            double[][] tmpXData1 = wsData.getXData();
            double[][] tmpYData1 = wsData.getYData();
            wsPosData = new double[1][tmpXData1[0].length];
            wsxData = new double[1][tmpXData1[0].length];
            wsyData = new double[1][tmpXData1[0].length];
            wsxDataDiff = new double[1][tmpXData1[0].length];
            wsyDataDiff = new double[1][tmpXData1[0].length];
            for (int i = 0; i < tmpXData1[0].length; i++) {
                wsPosData[0][i] = tmpXData1[0][i];
                wsxData[0][i] = tmpXData1[1][i];
                wsyData[0][i] = tmpYData1[1][i];
                if (wsxDataOld != null) {
                    wsxDataDiff[0][i] = wsxData[0][i] - wsxDataOld[0][i];
                    wsyDataDiff[0][i] = wsyData[0][i] - wsyDataOld[0][i];
                }
            }
            wsxDataOld = new double[1][tmpXData1[0].length];
            wsyDataOld = new double[1][tmpXData1[0].length];
            System.arraycopy(wsxData[0], 0, wsxDataOld[0], 0, wsxData[0].length);
            System.arraycopy(wsyData[0], 0, wsyDataOld[0], 0, wsyData[0].length);
        } else if (myWindow.mpxDocument.useWsModel.isSelected()) {
            showWsX.setEnabled(true);
            showWsY.setEnabled(true);
        } else {
            showBpmX.setEnabled(false);
            showBpmY.setEnabled(false);
            showWsX.setEnabled(false);
            showWsY.setEnabled(false);
        }
        posData = new double[1][elementCount];
        xData = new double[1][elementCount];
        xpData = new double[1][elementCount];
        alphaxData = new double[1][elementCount];
        betaxData = new double[1][elementCount];
        yData = new double[1][elementCount];
        ypData = new double[1][elementCount];
        alphayData = new double[1][elementCount];
        betayData = new double[1][elementCount];
        zData = new double[1][elementCount];
        zpData = new double[1][elementCount];
        alphazData = new double[1][elementCount];
        betazData = new double[1][elementCount];
        emitxData = new double[1][elementCount];
        emityData = new double[1][elementCount];
        emitzData = new double[1][elementCount];
        sigmaxData = new double[1][elementCount];
        sigmayData = new double[1][elementCount];
        sigmazData = new double[1][elementCount];
        engData = new double[1][elementCount];
        if (elementCount > 0) {
            elementIds = new String[elementCount];
            if (probeType == ModelProxy.ENVELOPE_PROBE) {
                for (int i = 0; i < elementCount; i++) {
                    try {
                        ProbeState probeState = mp.stateForElement((allNodes.get(i)).getId());
                        uc = TraceXalUnitConverter.newConverter(BeamConstants.FREQUENCY, probeState.getSpeciesRestEnergy(), probeState.getKineticEnergy());
                        elementIds[i] = probeState.getElementId();
                        posData[0][i] = probeState.getPosition();
                        xData[0][i] = uc.xalToTraceCoordinates(((EnvelopeProbeState) probeState).phaseMean()).getx();
                        xpData[0][i] = uc.xalToTraceCoordinates(((EnvelopeProbeState) probeState).phaseMean()).getxp();
                        Twiss[] twiss = ((EnvelopeProbeState) probeState).twissParameters();
                        alphaxData[0][i] = uc.xalToTraceTransverse(twiss[0]).getAlpha();
                        betaxData[0][i] = uc.xalToTraceTransverse(twiss[0]).getBeta();
                        yData[0][i] = uc.xalToTraceCoordinates(((EnvelopeProbeState) probeState).phaseMean()).gety();
                        ypData[0][i] = uc.xalToTraceCoordinates(((EnvelopeProbeState) probeState).phaseMean()).getyp();
                        alphayData[0][i] = uc.xalToTraceTransverse(twiss[1]).getAlpha();
                        betayData[0][i] = uc.xalToTraceTransverse(twiss[1]).getBeta();
                        zData[0][i] = uc.xalToTraceCoordinates(((EnvelopeProbeState) probeState).phaseMean()).getz();
                        zpData[0][i] = uc.xalToTraceCoordinates(((EnvelopeProbeState) probeState).phaseMean()).getzp();
                        alphazData[0][i] = uc.xalToTraceLongitudinal(twiss[2]).getAlpha();
                        betazData[0][i] = uc.xalToTraceLongitudinal(twiss[2]).getBeta();
                        emitxData[0][i] = uc.xalToTraceTransverse(twiss[0]).getEmittance();
                        emityData[0][i] = uc.xalToTraceTransverse(twiss[1]).getEmittance();
                        emitzData[0][i] = uc.xalToTraceLongitudinal(twiss[2]).getEmittance();
                        sigmaxData[0][i] = twiss[0].getEnvelopeRadius() * 1000.;
                        sigmayData[0][i] = twiss[1].getEnvelopeRadius() * 1000.;
                        sigmazData[0][i] = twiss[2].getEnvelopeRadius() * 1000.;
                        engData[0][i] = ((EnvelopeProbeState) probeState).getKineticEnergy() / 1.e6;
                        if (xDataOld != null && probeType == oldProbeType) {
                            xDataDiff[0][i] = uc.xalToTraceCoordinates(((EnvelopeProbeState) probeState).phaseMean()).getx() - xDataOld[0][i];
                            xpDataDiff[0][i] = uc.xalToTraceCoordinates(((EnvelopeProbeState) probeState).phaseMean()).getxp() - xpDataOld[0][i];
                            alphaxDataDiff[0][i] = uc.xalToTraceTransverse(twiss[0]).getAlpha() - alphaxDataOld[0][i];
                            betaxDataDiff[0][i] = uc.xalToTraceTransverse(twiss[0]).getBeta() - betaxDataOld[0][i];
                            yDataDiff[0][i] = uc.xalToTraceCoordinates(((EnvelopeProbeState) probeState).phaseMean()).gety() - yDataOld[0][i];
                            ypDataDiff[0][i] = uc.xalToTraceCoordinates(((EnvelopeProbeState) probeState).phaseMean()).getyp() - ypDataOld[0][i];
                            alphayDataDiff[0][i] = uc.xalToTraceTransverse(twiss[1]).getAlpha() - alphayDataOld[0][i];
                            betayDataDiff[0][i] = uc.xalToTraceTransverse(twiss[1]).getBeta() - betayDataOld[0][i];
                            zDataDiff[0][i] = uc.xalToTraceCoordinates(((EnvelopeProbeState) probeState).phaseMean()).getz() - zDataOld[0][i];
                            zpDataDiff[0][i] = uc.xalToTraceCoordinates(((EnvelopeProbeState) probeState).phaseMean()).getzp() - zpDataOld[0][i];
                            alphazDataDiff[0][i] = uc.xalToTraceLongitudinal(twiss[2]).getAlpha() - alphazDataOld[0][i];
                            betazDataDiff[0][i] = uc.xalToTraceLongitudinal(twiss[2]).getBeta() - betazDataOld[0][i];
                            emitxDataDiff[0][i] = uc.xalToTraceTransverse(twiss[0]).getEmittance() - emitxDataOld[0][i];
                            emityDataDiff[0][i] = uc.xalToTraceTransverse(twiss[1]).getEmittance() - emityDataOld[0][i];
                            emitzDataDiff[0][i] = uc.xalToTraceLongitudinal(twiss[2]).getEmittance() - emitzDataOld[0][i];
                            sigmaxDataDiff[0][i] = twiss[0].getEnvelopeRadius() * 1000. - sigmaxDataOld[0][i];
                            sigmayDataDiff[0][i] = twiss[1].getEnvelopeRadius() * 1000. - sigmayDataOld[0][i];
                            sigmazDataDiff[0][i] = twiss[2].getEnvelopeRadius() * 1000. - sigmazDataOld[0][i];
                            engDataDiff[0][i] = ((EnvelopeProbeState) probeState).getKineticEnergy() / 1.e6 - engDataOld[0][i];
                        } else {
                            oldProbeType = probeType;
                        }
                    } catch (ModelException me) {
                        System.out.println(me.getMessage());
                    }
                }
            }
            if (probeType == ModelProxy.TRANSFERMAP_PROBE) {
                for (int i = 0; i < elementCount; i++) {
                    try {
                        ProbeState probeState = mp.stateForElement((allNodes.get(i)).getId());
                        uc = TraceXalUnitConverter.newConverter(BeamConstants.FREQUENCY, probeState.getSpeciesRestEnergy(), probeState.getKineticEnergy());
                        elementIds[i] = probeState.getElementId();
                        posData[0][i] = probeState.getPosition();
                        xData[0][i] = uc.xalToTraceCoordinates(((TransferMapState) probeState).getFixedOrbit()).getx();
                        xpData[0][i] = uc.xalToTraceCoordinates(((TransferMapState) probeState).getFixedOrbit()).getxp();
                        alphaxData[0][i] = ((TransferMapState) probeState).getTwiss()[0].getAlpha();
                        betaxData[0][i] = ((TransferMapState) probeState).getTwiss()[0].getBeta();
                        yData[0][i] = uc.xalToTraceCoordinates(((TransferMapState) probeState).getFixedOrbit()).gety();
                        ypData[0][i] = uc.xalToTraceCoordinates(((TransferMapState) probeState).getFixedOrbit()).getyp();
                        alphayData[0][i] = ((TransferMapState) probeState).getTwiss()[1].getAlpha();
                        betayData[0][i] = ((TransferMapState) probeState).getTwiss()[1].getBeta();
                        zData[0][i] = uc.xalToTraceCoordinates(((TransferMapState) probeState).phaseCoordinates()).getz();
                        zpData[0][i] = uc.xalToTraceCoordinates(((TransferMapState) probeState).phaseCoordinates()).getzp();
                        alphazData[0][i] = ((TransferMapState) probeState).getTwiss()[2].getAlpha();
                        betazData[0][i] = ((TransferMapState) probeState).getTwiss()[2].getBeta();
                        emitxData[0][i] = ((TransferMapState) probeState).getTwiss()[0].getEmittance();
                        emityData[0][i] = ((TransferMapState) probeState).getTwiss()[1].getEmittance();
                        emitzData[0][i] = ((TransferMapState) probeState).getTwiss()[2].getEmittance();
                        if (xDataOld != null && probeType == oldProbeType) {
                            xDataDiff[0][i] = uc.xalToTraceCoordinates(((TransferMapState) probeState).getFixedOrbit()).getx() - xDataOld[0][i];
                            xpDataDiff[0][i] = uc.xalToTraceCoordinates(((TransferMapState) probeState).getFixedOrbit()).getxp() - xpDataOld[0][i];
                            alphaxDataDiff[0][i] = uc.xalToTraceTransverse(((TransferMapState) probeState).getTwiss()[0]).getAlpha() - alphaxDataOld[0][i];
                            betaxDataDiff[0][i] = uc.xalToTraceTransverse(((TransferMapState) probeState).getTwiss()[0]).getBeta() - betaxDataOld[0][i];
                            yDataDiff[0][i] = uc.xalToTraceCoordinates(((TransferMapState) probeState).getFixedOrbit()).gety() - yDataOld[0][i];
                            ypDataDiff[0][i] = uc.xalToTraceCoordinates(((TransferMapState) probeState).getFixedOrbit()).getyp() - ypDataOld[0][i];
                            alphayDataDiff[0][i] = uc.xalToTraceTransverse(((TransferMapState) probeState).getTwiss()[1]).getAlpha() - alphayDataOld[0][i];
                            betayDataDiff[0][i] = uc.xalToTraceTransverse(((TransferMapState) probeState).getTwiss()[1]).getBeta() - betayDataOld[0][i];
                            zDataDiff[0][i] = uc.xalToTraceCoordinates(((TransferMapState) probeState).phaseCoordinates()).getz() - zDataOld[0][i];
                            zpDataDiff[0][i] = uc.xalToTraceCoordinates(((TransferMapState) probeState).phaseCoordinates()).getzp() - zpDataOld[0][i];
                            alphazDataDiff[0][i] = uc.xalToTraceLongitudinal(((TransferMapState) probeState).getTwiss()[2]).getAlpha() - alphazDataOld[0][i];
                            betazDataDiff[0][i] = uc.xalToTraceLongitudinal(((TransferMapState) probeState).getTwiss()[2]).getBeta() - betazDataOld[0][i];
                            emitxDataDiff[0][i] = uc.xalToTraceTransverse(((TransferMapState) probeState).getTwiss()[0]).getEmittance() - emitxDataOld[0][i];
                            emityDataDiff[0][i] = uc.xalToTraceTransverse(((TransferMapState) probeState).getTwiss()[1]).getEmittance() - emityDataOld[0][i];
                            emitzDataDiff[0][i] = uc.xalToTraceLongitudinal(((TransferMapState) probeState).getTwiss()[2]).getEmittance() - emitzDataOld[0][i];
                        } else {
                            oldProbeType = probeType;
                        }
                    } catch (ModelException me) {
                        System.out.println(me.getMessage());
                    }
                }
            }
            if (probeType == ModelProxy.PARTICLE_PROBE) {
                for (int i = 0; i < elementCount; i++) {
                    try {
                        ProbeState probeState = mp.stateForElement((allNodes.get(i)).getId());
                        uc = TraceXalUnitConverter.newConverter(402500000., probeState.getSpeciesRestEnergy(), probeState.getKineticEnergy());
                        elementIds[i] = probeState.getElementId();
                        posData[0][i] = probeState.getPosition();
                        xData[0][i] = uc.xalToTraceCoordinates(((ParticleProbeState) probeState).phaseCoordinates()).getx();
                        xpData[0][i] = uc.xalToTraceCoordinates(((ParticleProbeState) probeState).phaseCoordinates()).getxp();
                        yData[0][i] = uc.xalToTraceCoordinates(((ParticleProbeState) probeState).phaseCoordinates()).gety();
                        ypData[0][i] = uc.xalToTraceCoordinates(((ParticleProbeState) probeState).phaseCoordinates()).getyp();
                        zData[0][i] = uc.xalToTraceCoordinates(((ParticleProbeState) probeState).phaseCoordinates()).getz();
                        zpData[0][i] = uc.xalToTraceCoordinates(((ParticleProbeState) probeState).phaseCoordinates()).getzp();
                        engData[0][i] = ((ParticleProbeState) probeState).getKineticEnergy() / 1.e6;
                        if (xDataOld != null && probeType == oldProbeType) {
                            xDataDiff[0][i] = uc.xalToTraceCoordinates(((ParticleProbeState) probeState).phaseCoordinates()).getx() - xDataOld[0][i];
                            xpDataDiff[0][i] = uc.xalToTraceCoordinates(((ParticleProbeState) probeState).phaseCoordinates()).getxp() - xpDataOld[0][i];
                            yDataDiff[0][i] = uc.xalToTraceCoordinates(((ParticleProbeState) probeState).phaseCoordinates()).gety() - yDataOld[0][i];
                            ypDataDiff[0][i] = uc.xalToTraceCoordinates(((ParticleProbeState) probeState).phaseCoordinates()).getyp() - ypDataOld[0][i];
                            zDataDiff[0][i] = uc.xalToTraceCoordinates(((ParticleProbeState) probeState).phaseCoordinates()).getz() - zDataOld[0][i];
                            zpDataDiff[0][i] = uc.xalToTraceCoordinates(((ParticleProbeState) probeState).phaseCoordinates()).getzp() - zpDataOld[0][i];
                            engDataDiff[0][i] = ((ParticleProbeState) probeState).getKineticEnergy() / 1.e6 - engDataOld[0][i];
                        } else {
                            oldProbeType = probeType;
                        }
                    } catch (ModelException me) {
                        System.out.println(me.getMessage());
                    }
                }
            }
        }
        chart.removeAllGraphData();
        current.setSelected(true);
        showCurrent();
    }

    /**
     * Clear all checked boxes
     */
    public void clearCheckboxes() {
        showX.setSelected(false);
        showXP.setSelected(false);
        showAlphaX.setSelected(false);
        showBetaX.setSelected(false);
        showY.setSelected(false);
        showYP.setSelected(false);
        showAlphaY.setSelected(false);
        showBetaY.setSelected(false);
        showZ.setSelected(false);
        showZP.setSelected(false);
        showAlphaZ.setSelected(false);
        showBetaZ.setSelected(false);
        showEmitX.setSelected(false);
        showEmitY.setSelected(false);
        showEmitZ.setSelected(false);
        showSigmaX.setSelected(false);
        showSigmaY.setSelected(false);
        showSigmaZ.setSelected(false);
        showEng.setSelected(false);
        showBpmX.setSelected(false);
        showBpmY.setSelected(false);
        showWsX.setSelected(false);
        showWsY.setSelected(false);
    }

    /**
     * Clear the data plots
     */
    public void reset() {
        xds.removeAllPoints();
        xpds.removeAllPoints();
        alphaxds.removeAllPoints();
        betaxds.removeAllPoints();
        yds.removeAllPoints();
        ypds.removeAllPoints();
        alphayds.removeAllPoints();
        betayds.removeAllPoints();
        zds.removeAllPoints();
        zpds.removeAllPoints();
        alphazds.removeAllPoints();
        betazds.removeAllPoints();
        emitxds.removeAllPoints();
        emityds.removeAllPoints();
        emitzds.removeAllPoints();
        sigmaxds.removeAllPoints();
        sigmayds.removeAllPoints();
        sigmazds.removeAllPoints();
        engds.removeAllPoints();
        bpmxds.removeAllPoints();
        bpmyds.removeAllPoints();
        wsxds.removeAllPoints();
        wsyds.removeAllPoints();
    }

    public void resetData() {
        System.out.println("Resetting plot data...");
        xData = null;
        xDataOld = null;
        diff.setEnabled(false);
    }

    /**
     * Handle user interactions
     */
    public void itemStateChanged(ItemEvent ev) {
        if (ev.getSource() instanceof JToggleButton) {
            JToggleButton source = (JToggleButton) ev.getSource();
            boolean state = source.isSelected();
            if (source == showX) {
                try {
                    if (state) chart.addGraphData(xds); else chart.removeGraphData(xds);
                } catch (Exception e) {
                }
            }
            if (source == showXP) {
                try {
                    if (state) chart.addGraphData(xpds); else chart.removeGraphData(xpds);
                } catch (Exception e) {
                }
            }
            if (source == showAlphaX) {
                try {
                    if (state) chart.addGraphData(alphaxds); else chart.removeGraphData(alphaxds);
                } catch (Exception e) {
                }
            }
            if (source == showBetaX) {
                try {
                    if (state) chart.addGraphData(betaxds); else chart.removeGraphData(betaxds);
                } catch (Exception e) {
                }
            }
            if (source == showY) {
                try {
                    if (state) chart.addGraphData(yds); else chart.removeGraphData(yds);
                } catch (Exception e) {
                }
            }
            if (source == showYP) {
                try {
                    if (state) chart.addGraphData(ypds); else chart.removeGraphData(ypds);
                } catch (Exception e) {
                }
            }
            if (source == showAlphaY) {
                try {
                    if (state) chart.addGraphData(alphayds); else chart.removeGraphData(alphayds);
                } catch (Exception e) {
                }
            }
            if (source == showBetaY) {
                try {
                    if (state) chart.addGraphData(betayds); else chart.removeGraphData(betayds);
                } catch (Exception e) {
                }
            }
            if (source == showZ) {
                try {
                    if (state) chart.addGraphData(zds); else chart.removeGraphData(zds);
                } catch (Exception e) {
                }
            }
            if (source == showZP) {
                try {
                    if (state) chart.addGraphData(zpds); else chart.removeGraphData(zpds);
                } catch (Exception e) {
                }
            }
            if (source == showAlphaZ) {
                try {
                    if (state) chart.addGraphData(alphazds); else chart.removeGraphData(alphazds);
                } catch (Exception e) {
                }
            }
            if (source == showBetaZ) {
                try {
                    if (state) chart.addGraphData(betazds); else chart.removeGraphData(betazds);
                } catch (Exception e) {
                }
            }
            if (source == showEmitX) {
                try {
                    if (state) chart.addGraphData(emitxds); else chart.removeGraphData(emitxds);
                } catch (Exception e) {
                }
            }
            if (source == showEmitY) {
                try {
                    if (state) chart.addGraphData(emityds); else chart.removeGraphData(emityds);
                } catch (Exception e) {
                }
            }
            if (source == showEmitZ) {
                try {
                    if (state) chart.addGraphData(emitzds); else chart.removeGraphData(emitzds);
                } catch (Exception e) {
                }
            }
            if (source == showSigmaX) {
                try {
                    if (state) chart.addGraphData(sigmaxds); else chart.removeGraphData(sigmaxds);
                } catch (Exception e) {
                }
            }
            if (source == showSigmaY) {
                try {
                    if (state) chart.addGraphData(sigmayds); else chart.removeGraphData(sigmayds);
                } catch (Exception e) {
                }
            }
            if (source == showSigmaZ) {
                try {
                    if (state) chart.addGraphData(sigmazds); else chart.removeGraphData(sigmazds);
                } catch (Exception e) {
                }
            }
            if (source == showEng) {
                try {
                    if (state) chart.addGraphData(engds); else chart.removeGraphData(engds);
                } catch (Exception e) {
                }
            }
            if (source == showBpmX) {
                try {
                    if (state) chart.addGraphData(bpmxds); else chart.removeGraphData(bpmxds);
                } catch (Exception e) {
                }
            }
            if (source == showBpmY) {
                try {
                    if (state) chart.addGraphData(bpmyds); else chart.removeGraphData(bpmyds);
                } catch (Exception e) {
                }
            }
            if (source == showWsX) {
                try {
                    if (state) chart.addGraphData(wsxds); else chart.removeGraphData(wsxds);
                } catch (Exception e) {
                }
            }
            if (source == showWsY) {
                try {
                    if (state) chart.addGraphData(wsyds); else chart.removeGraphData(wsyds);
                } catch (Exception e) {
                }
            }
        }
    }

    public void actionPerformed(ActionEvent ev) {
        if (ev.getActionCommand().equals("export data")) {
            String currentDirectory = _savedFileTracker.getRecentFolderPath();
            JFileChooser fileChooser = new JFileChooser(currentDirectory);
            int status = fileChooser.showSaveDialog(this);
            if (status == JFileChooser.APPROVE_OPTION) {
                _savedFileTracker.cacheURL(fileChooser.getSelectedFile());
                File file = fileChooser.getSelectedFile();
                try {
                    int probeType = myMP.getProbeType();
                    try {
                        FileWriter fileWriter = new FileWriter(file);
                        NumberFormat nf = NumberFormat.getNumberInstance();
                        nf.setMaximumFractionDigits(5);
                        nf.setMinimumFractionDigits(5);
                        if (probeType == ModelProxy.ENVELOPE_PROBE) {
                            fileWriter.write("Element_Id\t" + "s\t" + "X\t" + "X'\t" + "Y\t" + "Y'\t" + "Z\t" + "Z'\t" + "alpha_x\t" + "beta_x\t" + "alpha_y\t" + "beta_y\t" + "alpha_z\t" + "beta_z\t" + "emit_x\t" + "emit_y\t" + "emit_z\t" + "sigma_x\t" + "sigma_y\t" + "sigma_z\t" + "energy\n");
                            for (int i = 0; i < xds.getNumbOfPoints(); i++) {
                                fileWriter.write(elementIds[i] + "\t" + nf.format(xds.getX(i)) + "\t" + nf.format(xds.getY(i)) + "\t" + nf.format(xpds.getY(i)) + "\t" + nf.format(yds.getY(i)) + "\t" + nf.format(ypds.getY(i)) + "\t" + nf.format(zds.getY(i)) + "\t" + nf.format(zpds.getY(i)) + "\t" + nf.format(alphaxds.getY(i)) + "\t" + nf.format(betaxds.getY(i)) + "\t" + nf.format(alphayds.getY(i)) + "\t" + nf.format(betayds.getY(i)) + "\t" + nf.format(alphazds.getY(i)) + "\t" + nf.format(betazds.getY(i)) + "\t" + nf.format(emitxds.getY(i)) + "\t" + nf.format(emityds.getY(i)) + "\t" + nf.format(emitzds.getY(i)) + "\t" + nf.format(sigmaxds.getY(i)) + "\t" + nf.format(sigmayds.getY(i)) + "\t" + nf.format(sigmazds.getY(i)) + "\t" + nf.format(engds.getY(i)) + "\n");
                            }
                        } else if (probeType == ModelProxy.PARTICLE_PROBE) {
                            fileWriter.write("Element_Id\t" + "s\t" + "X\t" + "X'\t" + "Y\t" + "Y'\t" + "Z\t" + "Z'\t" + "energy\n");
                            for (int i = 0; i < xds.getNumbOfPoints(); i++) {
                                fileWriter.write(elementIds[i] + "\t" + nf.format(xds.getX(i)) + "\t" + nf.format(xds.getY(i)) + "\t" + nf.format(xpds.getY(i)) + "\t" + nf.format(yds.getY(i)) + "\t" + nf.format(ypds.getY(i)) + "\t" + nf.format(zds.getY(i)) + "\t" + nf.format(zpds.getY(i)) + "\t" + nf.format(engds.getY(i)) + "\n");
                            }
                        } else if (probeType == ModelProxy.TRANSFERMAP_PROBE) {
                            fileWriter.write("Element_Id\t" + "s\t" + "X\t" + "X'\t" + "Y\t" + "Y'\t" + "Z\t" + "Z'\t" + "alpha_x\t" + "beta_x\t" + "alpha_y\t" + "beta_y" + "\n");
                            for (int i = 0; i < xds.getNumbOfPoints(); i++) {
                                fileWriter.write(elementIds[i] + "\t" + nf.format(xds.getX(i)) + "\t" + nf.format(xds.getY(i)) + "\t" + nf.format(xpds.getY(i)) + "\t" + nf.format(yds.getY(i)) + "\t" + nf.format(ypds.getY(i)) + "\t" + nf.format(zds.getY(i)) + "\t" + nf.format(zpds.getY(i)) + "\t" + nf.format(alphaxds.getY(i)) + "\t" + nf.format(betaxds.getY(i)) + "\t" + nf.format(alphayds.getY(i)) + "\t" + nf.format(betayds.getY(i)) + "\n");
                            }
                        }
                        if (showBpmX.isEnabled()) {
                            fileWriter.write("\n " + "BPM_Id\t" + "s\t" + "xAvg\t" + "yAvg\t" + "amp\n");
                            for (int i = 0; i < bpmxds.getNumbOfPoints(); i++) {
                                fileWriter.write(bpmData.getNames()[i] + "\t" + bpmxds.getX(i) + "\t" + bpmxds.getY(i) + "\t" + bpmyds.getY(i) + "\t" + bpmData.getAmpData()[i] + "\n");
                            }
                            try {
                                myWindow.mpxDocument.loggerSession.publishSnapshot(myWindow.mpxDocument.snapshot);
                                fileWriter.write("\n " + "PVLogger ID = " + myWindow.mpxDocument.snapshot.getId());
                            } catch (Exception e) {
                            }
                        }
                        if (showWsX.isEnabled()) {
                            fileWriter.write("\n " + "WS_Id\t" + "s\t" + "xAvg\t" + "yAvg\n");
                            for (int i = 0; i < wsxds.getNumbOfPoints(); i++) {
                                fileWriter.write(wsData.getNames()[i] + "\t" + wsxds.getX(i) + "\t" + wsxds.getY(i) + "\t" + wsyds.getY(i) + "\n");
                            }
                            try {
                                myWindow.mpxDocument.loggerSession.publishSnapshot(myWindow.mpxDocument.snapshot);
                                fileWriter.write("\n " + "PVLogger ID = " + myWindow.mpxDocument.snapshot.getId());
                            } catch (Exception e) {
                            }
                        }
                        fileWriter.close();
                    } catch (IOException ie) {
                        JFrame frame = new JFrame();
                        JOptionPane.showMessageDialog(frame, "Cannot open the file" + file.getName() + "for writing", "Warning!", JOptionPane.PLAIN_MESSAGE);
                        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    }
                } catch (java.lang.NullPointerException e) {
                    JFrame frame = new JFrame();
                    JOptionPane.showMessageDialog(frame, "You need to run the model first!", "Warning!", JOptionPane.PLAIN_MESSAGE);
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                }
            }
        }
        if (ev.getActionCommand().equals("plot current")) {
            showCurrent();
        }
        if (ev.getActionCommand().equals("plot difference")) {
            showDifference();
        }
    }

    private void showCurrent() {
        xds.addPoint(posData[0], xData[0]);
        xpds.addPoint(posData[0], xpData[0]);
        alphaxds.addPoint(posData[0], alphaxData[0]);
        betaxds.addPoint(posData[0], betaxData[0]);
        yds.addPoint(posData[0], yData[0]);
        ypds.addPoint(posData[0], ypData[0]);
        alphayds.addPoint(posData[0], alphayData[0]);
        betayds.addPoint(posData[0], betayData[0]);
        zds.addPoint(posData[0], zData[0]);
        zpds.addPoint(posData[0], zpData[0]);
        alphazds.addPoint(posData[0], alphazData[0]);
        betazds.addPoint(posData[0], betazData[0]);
        emitxds.addPoint(posData[0], emitxData[0]);
        emityds.addPoint(posData[0], emityData[0]);
        emitzds.addPoint(posData[0], emitzData[0]);
        sigmaxds.addPoint(posData[0], sigmaxData[0]);
        sigmayds.addPoint(posData[0], sigmayData[0]);
        sigmazds.addPoint(posData[0], sigmazData[0]);
        engds.addPoint(posData[0], engData[0]);
        if (!myMP.getChannelSource().equals(Scenario.SYNC_MODE_DESIGN)) {
            bpmxds.addPoint(bpmPosData[0], bpmxData[0]);
            bpmyds.addPoint(bpmPosData[0], bpmyData[0]);
        }
        wsxds.removeAllPoints();
        wsyds.removeAllPoints();
        if (wsxData != null) {
            wsxds.addPoint(wsPosData[0], wsxData[0]);
            wsyds.addPoint(wsPosData[0], wsyData[0]);
        }
    }

    private void showDifference() {
        xds.addPoint(posData[0], xDataDiff[0]);
        xpds.addPoint(posData[0], xpDataDiff[0]);
        alphaxds.addPoint(posData[0], alphaxDataDiff[0]);
        betaxds.addPoint(posData[0], betaxDataDiff[0]);
        yds.addPoint(posData[0], yDataDiff[0]);
        ypds.addPoint(posData[0], ypDataDiff[0]);
        alphayds.addPoint(posData[0], alphayDataDiff[0]);
        betayds.addPoint(posData[0], betayDataDiff[0]);
        zds.addPoint(posData[0], zDataDiff[0]);
        zpds.addPoint(posData[0], zpDataDiff[0]);
        alphazds.addPoint(posData[0], alphazDataDiff[0]);
        betazds.addPoint(posData[0], betazDataDiff[0]);
        emitxds.addPoint(posData[0], emitxDataDiff[0]);
        emityds.addPoint(posData[0], emityDataDiff[0]);
        emitzds.addPoint(posData[0], emitzDataDiff[0]);
        sigmaxds.addPoint(posData[0], sigmaxDataDiff[0]);
        sigmayds.addPoint(posData[0], sigmayDataDiff[0]);
        sigmazds.addPoint(posData[0], sigmazDataDiff[0]);
        engds.addPoint(posData[0], engDataDiff[0]);
        if (!myMP.getChannelSource().equals(Scenario.SYNC_MODE_DESIGN)) {
            bpmxds.addPoint(bpmPosData[0], bpmxDataDiff[0]);
            bpmyds.addPoint(bpmPosData[0], bpmyDataDiff[0]);
        }
        wsxds.removeAllPoints();
        wsyds.removeAllPoints();
        if (wsxDataDiff != null) {
            wsxds.addPoint(wsPosData[0], wsxDataDiff[0]);
            wsyds.addPoint(wsPosData[0], wsyDataDiff[0]);
        }
    }

    private void initSynopticView() {
        try {
            if (synoptic == null) {
                synoptic = new XALSynopticPanel();
                synoptic.setPreferredSize(new Dimension(100, 40));
                synoptic.setMinimumSize(new Dimension(100, 40));
                synoptic.setMargin(new Insets(5, 10, 10, 10));
            }
            if (chart != null) {
                chart.addHorLimitsListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        if (chart == null || synoptic == null) return;
                        synchronizeSynopticAxis();
                    }
                });
                synchronizeSynopticAxis();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void synchronizeSynopticAxis() {
        if (chart.getNumbTotalGraphPoints() == 0) return;
        double start = chart.getCurrentMinX();
        double end = chart.getCurrentMaxX();
        int left = chart.getScreenX(start);
        int right = synoptic.getWidth() - chart.getScreenX(end);
        if (synoptic.getMargin().left != left || synoptic.getMargin().right != right || start != synoptic.getStartPosition() || end != synoptic.getEndPosition()) {
            synoptic.setMargin(new Insets(5, left, 5, right));
            synoptic.setStartPosition(start);
            synoptic.setEndPosition(end);
            synoptic.repaint();
        }
    }

    /**
     * Sets a sequence which is displayed in synoptic.
     * @param seq a sequence which is displayed in synoptic
     */
    public void setAcceleratorSequence(AcceleratorSeq seq) {
        MPXStopWatch.timeElapsed("...synoptic.setAcceleratorSequence(seq): begin! ");
        synoptic.setAcceleratorSequence(seq);
        MPXStopWatch.timeElapsed("...synoptic.setAcceleratorSequence(seq): end! ");
        MPXStopWatch.timeElapsed("...synchronizeSynopticAxis(): begin! ");
        synchronizeSynopticAxis();
        MPXStopWatch.timeElapsed("...synchronizeSynopticAxis(): end! ");
    }

    /**
     * Returns a sequence which is displayed in synoptic.
     * @return a sequence which is displayed in synoptic
     */
    public AcceleratorSeq getAcceleratorSequence() {
        return synoptic.getAcceleratorSequence();
    }

    protected void setWSData(double[][] posData, double[][] xData, double[][] yData) {
        wsPosData = posData;
        wsxData = xData;
        wsyData = yData;
        wsxds.removeAllPoints();
        wsyds.removeAllPoints();
        wsxds.addPoint(wsPosData[0], wsxData[0]);
        wsyds.addPoint(wsPosData[0], wsyData[0]);
        if (wsReset) {
            wsxDataOld = new double[1][wsPosData[0].length];
            wsyDataOld = new double[1][wsPosData[0].length];
            wsxDataDiff = new double[1][wsPosData[0].length];
            wsyDataDiff = new double[1][wsPosData[0].length];
            wsReset = false;
        }
        for (int i = 0; i < wsPosData[0].length; i++) {
            if (wsxDataOld != null) {
                wsxDataDiff[0][i] = wsxData[0][i] - wsxDataOld[0][i];
                wsyDataDiff[0][i] = wsyData[0][i] - wsyDataOld[0][i];
            }
        }
        System.arraycopy(wsxData[0], 0, wsxDataOld[0], 0, wsxData[0].length);
        System.arraycopy(wsyData[0], 0, wsyDataOld[0], 0, wsyData[0].length);
    }
}
