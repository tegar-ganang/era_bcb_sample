package gov.sns.xal.tools.simulationmanager;

import gov.sns.ca.ConnectionException;
import gov.sns.ca.GetException;
import gov.sns.tools.data.EditContext;
import gov.sns.tools.data.GenericRecord;
import gov.sns.tools.data.SortOrdering;
import gov.sns.tools.plot.BasicGraphData;
import gov.sns.tools.plot.FunctionGraphsJPanel;
import gov.sns.xal.model.scenario.Scenario;
import gov.sns.xal.smf.AcceleratorNode;
import gov.sns.xal.smf.AcceleratorSeq;
import gov.sns.xal.smf.attr.ApertureBucket;
import gov.sns.xal.smf.impl.BPM;
import gov.sns.xal.tools.simulationmanager.SimulationManagerFactory;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

public class TwissPlotterSource implements ItemListener {

    static final boolean benchmark = false;

    static final int EDIT_CONTEXT = 0;

    static final int TWISS_FAST_DATA = 1;

    static final int TWISS_FAST_LISTS = 2;

    static final int useFastData = TWISS_FAST_LISTS;

    TwissFastLists listsPrev;

    TwissFastLists listsCur;

    TwissFastLists listsDiff;

    FunctionGraphsJPanel chart = null;

    protected SimulationManager manager;

    private int simulationType;

    private String simLabel;

    JCheckBox showX, showXP, showY, showYP, showZ, showZP;

    JCheckBox showSigmaX, showSigmaY, showSigmaZ;

    JCheckBox showPhiX, showPhiY, showPhiZ;

    JCheckBox showAlphaX, showAlphaY, showAlphaZ;

    JCheckBox showBetaX, showBetaY, showBetaZ;

    JCheckBox showEmitX, showEmitY, showEmitZ;

    JCheckBox showBpmX, showBpmY;

    JCheckBox showEtaX, showEtaXP, showEtaY, showEtaYP;

    JCheckBox showWsmX, showWsmY;

    JCheckBox showEng;

    JCheckBox showApx, showApy;

    Color xColor, yColor, zColor;

    Color xpColor, ypColor, zpColor;

    Color sigmaxColor, sigmayColor, sigmazColor;

    Color phixColor, phiyColor, phizColor;

    Color alphaxColor, alphayColor, alphazColor;

    Color betaxColor, betayColor, betazColor;

    Color emitxColor, emityColor, emitzColor;

    Color bpmxColor, bpmyColor, bpmzColor;

    Color etaxColor, etayColor, etaxpColor, etaypColor;

    Color wsmxColor, wsmyColor;

    Color engColor;

    Color apxColor, apyColor;

    int symbolSize;

    int symbolSizeBPM;

    int lineThickness;

    BasicGraphData elpsxds, elpsyds, elpszds;

    BasicGraphData xds, xpds, alphaxds, betaxds;

    BasicGraphData yds, ypds, alphayds, betayds;

    BasicGraphData zds, zpds, alphazds, betazds;

    BasicGraphData sigmaxds, sigmayds, sigmazds;

    BasicGraphData phixds, phiyds, phizds;

    BasicGraphData emitxds, emityds, emitzds;

    BasicGraphData bpmxds, bpmyds;

    BasicGraphData etaxds, etayds;

    BasicGraphData etaxpds, etaypds;

    BasicGraphData wsmxds, wsmyds;

    BasicGraphData engds;

    BasicGraphData apxds, apyds;

    BasicGraphData xdsP, xpdsP, alphaxdsP, betaxdsP;

    BasicGraphData ydsP, ypdsP, alphaydsP, betaydsP;

    BasicGraphData zdsP, zpdsP, alphazdsP, betazdsP;

    BasicGraphData sigmaxdsP, sigmaydsP, sigmazdsP;

    BasicGraphData phixdsP, phiydsP, phizdsP;

    BasicGraphData emitxdsP, emitydsP, emitzdsP;

    BasicGraphData etaxdsP, etaydsP;

    BasicGraphData etaxpdsP, etaypdsP;

    BasicGraphData engdsP;

    BasicGraphData apxdsP, apydsP;

    protected String elementId[][];

    protected double posData[][];

    protected double elpsxData[][];

    protected double elpsxpData[][];

    protected double elpsyData[][];

    protected double elpsypData[][];

    protected double elpszData[][];

    protected double elpszpData[][];

    protected double xData[][];

    protected double xpData[][];

    protected double alphaxData[][];

    protected double betaxData[][];

    protected double yData[][];

    protected double ypData[][];

    protected double alphayData[][];

    protected double betayData[][];

    protected double zData[][];

    protected double zpData[][];

    protected double alphazData[][];

    protected double betazData[][];

    protected double emitxData[][];

    protected double emityData[][];

    protected double emitzData[][];

    protected double sigmaxData[][];

    protected double sigmayData[][];

    protected double sigmazData[][];

    protected double phixData[][];

    protected double phiyData[][];

    protected double phizData[][];

    double bpmPosData[][];

    protected double bpmxData[][];

    protected double bpmyData[][];

    protected double etaxData[][];

    protected double etayData[][];

    protected double etaxpData[][];

    protected double etaypData[][];

    double wsmPosData[][];

    double wsmxData[][];

    double wsmyData[][];

    protected double bpmxpData[][];

    protected double bpmypData[][];

    protected double engData[][];

    protected double apxData[][];

    protected double apyData[][];

    protected double apPosData[][];

    public void setSymbolSize(int size) {
        symbolSize = size;
    }

    public void setSymbolSizeBPM(int size) {
        symbolSizeBPM = size;
    }

    public void setLineThickness(int thick) {
        lineThickness = thick;
    }

    public int getSimulationType() {
        return simulationType;
    }

    public TwissPlotterSource(SimulationManager m) {
        manager = m;
        simulationType = manager.getType();
        _init();
    }

    private void _init() {
        symbolSize = 4;
        symbolSizeBPM = 6;
        lineThickness = 1;
        switch(simulationType) {
            case SimulationManagerFactory.Xal_Id:
                simLabel = "XAL";
                xColor = Color.red;
                yColor = Color.blue;
                zColor = Color.green;
                break;
            case SimulationManagerFactory.T3d_Id:
                simLabel = "T3D";
                xColor = Color.CYAN;
                yColor = Color.MAGENTA;
                zColor = Color.ORANGE;
                break;
            case SimulationManagerFactory.Sad_Id:
                simLabel = "SAD";
                xColor = Color.green;
                yColor = Color.RED;
                zColor = Color.GRAY;
                break;
            case SimulationManagerFactory.Mad_Id:
                simLabel = "MAD";
                xColor = Color.BLUE;
                yColor = Color.GREEN;
                zColor = Color.RED;
                break;
            default:
                simLabel = "NAV";
                xColor = Color.BLUE;
                yColor = Color.GREEN;
                zColor = Color.RED;
        }
        xpColor = xColor;
        ypColor = yColor;
        zpColor = zColor;
        sigmaxColor = xColor;
        sigmayColor = yColor;
        sigmazColor = zColor;
        phixColor = xColor;
        phiyColor = yColor;
        phizColor = zColor;
        alphaxColor = xColor;
        betaxColor = xColor;
        alphayColor = yColor;
        betayColor = yColor;
        alphazColor = zColor;
        betazColor = zColor;
        emitxColor = xColor;
        emityColor = yColor;
        emitzColor = zColor;
        bpmxColor = xColor;
        bpmyColor = yColor;
        etaxColor = xColor;
        etayColor = yColor;
        etaxpColor = xColor;
        etaypColor = yColor;
        wsmxColor = xColor;
        wsmyColor = yColor;
        engColor = xColor;
        apxColor = xColor;
        apyColor = yColor;
        showX = new JCheckBox("");
        showXP = new JCheckBox("");
        showY = new JCheckBox("");
        showYP = new JCheckBox("");
        showZ = new JCheckBox("");
        showZP = new JCheckBox("");
        showSigmaX = new JCheckBox("");
        showSigmaY = new JCheckBox("");
        showSigmaZ = new JCheckBox("");
        showPhiX = new JCheckBox("");
        showPhiY = new JCheckBox("");
        showPhiZ = new JCheckBox("");
        showAlphaX = new JCheckBox("");
        showAlphaY = new JCheckBox("");
        showAlphaZ = new JCheckBox("");
        showBetaX = new JCheckBox("");
        showBetaY = new JCheckBox("");
        showBetaZ = new JCheckBox("");
        showEmitX = new JCheckBox("");
        showEmitY = new JCheckBox("");
        showEmitZ = new JCheckBox("");
        showEng = new JCheckBox("");
        showApx = new JCheckBox("");
        showApy = new JCheckBox("");
        showBpmX = new JCheckBox("");
        showBpmY = new JCheckBox("");
        showEtaX = new JCheckBox("");
        showEtaXP = new JCheckBox("");
        showEtaY = new JCheckBox("");
        showEtaYP = new JCheckBox("");
        showWsmX = new JCheckBox("");
        showWsmY = new JCheckBox("");
        showX.setForeground(xColor);
        showX.setBackground(xColor);
        showX.addItemListener(this);
        showXP.setForeground(xpColor);
        showXP.setBackground(xpColor);
        showXP.addItemListener(this);
        showY.setForeground(yColor);
        showY.setBackground(yColor);
        showY.addItemListener(this);
        showYP.setForeground(ypColor);
        showYP.setBackground(ypColor);
        showYP.addItemListener(this);
        showZ.setForeground(zColor);
        showZ.setBackground(zColor);
        showZ.addItemListener(this);
        showZP.setForeground(zpColor);
        showZP.setBackground(zpColor);
        showZP.addItemListener(this);
        showSigmaX.setForeground(sigmaxColor);
        showSigmaX.setBackground(sigmaxColor);
        showSigmaX.addItemListener(this);
        showSigmaY.setForeground(sigmayColor);
        showSigmaY.setBackground(sigmayColor);
        showSigmaY.addItemListener(this);
        showSigmaZ.setForeground(sigmazColor);
        showSigmaZ.setBackground(sigmazColor);
        showSigmaZ.addItemListener(this);
        showPhiX.setForeground(phixColor);
        showPhiX.setBackground(phixColor);
        showPhiX.addItemListener(this);
        showPhiY.setForeground(phiyColor);
        showPhiY.setBackground(phiyColor);
        showPhiY.addItemListener(this);
        showPhiZ.setForeground(phizColor);
        showPhiZ.setBackground(phizColor);
        showPhiZ.addItemListener(this);
        showEtaX.setForeground(etaxColor);
        showEtaX.setBackground(etaxColor);
        showEtaX.addItemListener(this);
        showEtaY.setForeground(etayColor);
        showEtaY.setBackground(etayColor);
        showEtaY.addItemListener(this);
        showEtaXP.setForeground(etaxpColor);
        showEtaXP.setBackground(etaxpColor);
        showEtaXP.addItemListener(this);
        showEtaYP.setForeground(etaypColor);
        showEtaYP.setBackground(etaypColor);
        showEtaYP.addItemListener(this);
        showWsmX.setForeground(wsmxColor);
        showWsmX.setBackground(wsmxColor);
        showWsmX.addItemListener(this);
        showWsmY.setForeground(wsmyColor);
        showWsmY.setBackground(wsmyColor);
        showWsmY.addItemListener(this);
        showAlphaX.setForeground(alphaxColor);
        showAlphaX.setBackground(alphaxColor);
        showAlphaX.addItemListener(this);
        showBetaX.setForeground(betaxColor);
        showBetaX.setBackground(betaxColor);
        showBetaX.addItemListener(this);
        showAlphaY.setForeground(alphayColor);
        showAlphaY.setBackground(alphayColor);
        showAlphaY.addItemListener(this);
        showBetaY.setForeground(betayColor);
        showBetaY.setBackground(betayColor);
        showBetaY.addItemListener(this);
        showAlphaZ.setForeground(alphazColor);
        showAlphaZ.setBackground(alphazColor);
        showAlphaZ.addItemListener(this);
        showBetaZ.setForeground(betazColor);
        showBetaZ.setBackground(betazColor);
        showBetaZ.addItemListener(this);
        showBpmX.setForeground(bpmxColor);
        showBpmX.setBackground(bpmxColor);
        showBpmX.addItemListener(this);
        showBpmY.setForeground(bpmyColor);
        showBpmY.setBackground(bpmyColor);
        showBpmY.addItemListener(this);
        showEmitX.setForeground(emitxColor);
        showEmitX.setBackground(emitxColor);
        showEmitX.addItemListener(this);
        showEmitY.setForeground(emityColor);
        showEmitY.setBackground(emityColor);
        showEmitY.addItemListener(this);
        showEmitZ.setForeground(emitzColor);
        showEmitZ.setBackground(emitzColor);
        showEmitZ.addItemListener(this);
        showEng.setForeground(engColor);
        showEng.setBackground(engColor);
        showEng.addItemListener(this);
        showApx.setForeground(apxColor);
        showApx.setBackground(apxColor);
        showApx.addItemListener(this);
        showApy.setForeground(apyColor);
        showApy.setBackground(apyColor);
        showApy.addItemListener(this);
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
        phixds = new BasicGraphData();
        phiyds = new BasicGraphData();
        phizds = new BasicGraphData();
        engds = new BasicGraphData();
        bpmxds = new BasicGraphData();
        bpmyds = new BasicGraphData();
        etaxds = new BasicGraphData();
        etayds = new BasicGraphData();
        etaxpds = new BasicGraphData();
        etaypds = new BasicGraphData();
        wsmxds = new BasicGraphData();
        wsmyds = new BasicGraphData();
        engds = new BasicGraphData();
        apxds = new BasicGraphData();
        apyds = new BasicGraphData();
        xdsP = new BasicGraphData();
        xpdsP = new BasicGraphData();
        alphaxdsP = new BasicGraphData();
        betaxdsP = new BasicGraphData();
        ydsP = new BasicGraphData();
        ypdsP = new BasicGraphData();
        alphaydsP = new BasicGraphData();
        betaydsP = new BasicGraphData();
        zdsP = new BasicGraphData();
        zpdsP = new BasicGraphData();
        alphazdsP = new BasicGraphData();
        betazdsP = new BasicGraphData();
        emitxdsP = new BasicGraphData();
        emitydsP = new BasicGraphData();
        emitzdsP = new BasicGraphData();
        sigmaxdsP = new BasicGraphData();
        sigmaydsP = new BasicGraphData();
        sigmazdsP = new BasicGraphData();
        phixdsP = new BasicGraphData();
        phiydsP = new BasicGraphData();
        phizdsP = new BasicGraphData();
        engdsP = new BasicGraphData();
        etaxdsP = new BasicGraphData();
        etaydsP = new BasicGraphData();
        etaxpdsP = new BasicGraphData();
        etaypdsP = new BasicGraphData();
        engdsP = new BasicGraphData();
        apxdsP = new BasicGraphData();
        apydsP = new BasicGraphData();
    }

    public void addCheckBoxes(JPanel selectionUp, int iSim, int nSim) {
        GridBagConstraints gbcUp = new GridBagConstraints();
        gbcUp.gridy = 0;
        gbcUp.gridx = iSim;
        String title;
        if (iSim == 0) {
            title = simLabel + " ";
        } else {
            title = " " + simLabel;
        }
        JLabel label = new JLabel(title);
        label.setFont(new java.awt.Font("", java.awt.Font.BOLD, 10));
        selectionUp.add(label, gbcUp);
        gbcUp.gridx = iSim + nSim + 1;
        JLabel label1 = new JLabel(title);
        label1.setFont(new java.awt.Font("", java.awt.Font.BOLD, 10));
        selectionUp.add(label1, gbcUp);
        gbcUp.gridy++;
        gbcUp.gridx = iSim;
        selectionUp.add(showX, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = nSim;
            selectionUp.add(new JLabel("x"), gbcUp);
        }
        gbcUp.gridx = iSim + nSim + 1;
        selectionUp.add(showXP, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = 2 * nSim + 1;
            selectionUp.add(new JLabel("x'"), gbcUp);
        }
        gbcUp.gridy++;
        gbcUp.gridx = iSim;
        selectionUp.add(showY, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = nSim;
            selectionUp.add(new JLabel("y"), gbcUp);
        }
        gbcUp.gridx = iSim + nSim + 1;
        selectionUp.add(showYP, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = 2 * nSim + 1;
            selectionUp.add(new JLabel("y'"), gbcUp);
        }
        gbcUp.gridy++;
        gbcUp.gridx = iSim;
        selectionUp.add(showZ, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = nSim;
            selectionUp.add(new JLabel("z"), gbcUp);
        }
        gbcUp.gridx = iSim + nSim + 1;
        selectionUp.add(showZP, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = 2 * nSim + 1;
            selectionUp.add(new JLabel("z'"), gbcUp);
        }
        gbcUp.gridy++;
        gbcUp.gridx = iSim;
        selectionUp.add(showAlphaX, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = nSim;
            selectionUp.add(new JLabel("AlphaX"), gbcUp);
        }
        gbcUp.gridx = iSim + nSim + 1;
        selectionUp.add(showBetaX, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = 2 * nSim + 1;
            selectionUp.add(new JLabel("BetaX"), gbcUp);
        }
        gbcUp.gridy++;
        gbcUp.gridx = iSim;
        selectionUp.add(showEtaX, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = nSim;
            selectionUp.add(new JLabel("EtaX"), gbcUp);
        }
        gbcUp.gridx = iSim + nSim + 1;
        selectionUp.add(showEtaXP, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = 2 * nSim + 1;
            selectionUp.add(new JLabel("EtaX'"), gbcUp);
        }
        gbcUp.gridy++;
        gbcUp.gridx = iSim;
        selectionUp.add(showEmitX, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = nSim;
            selectionUp.add(new JLabel("EmitX"), gbcUp);
        }
        gbcUp.gridx = iSim + nSim + 1;
        selectionUp.add(showSigmaX, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = 2 * nSim + 1;
            selectionUp.add(new JLabel("SigmaX"), gbcUp);
        }
        gbcUp.gridy++;
        gbcUp.gridx = iSim;
        selectionUp.add(showPhiX, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = nSim;
            selectionUp.add(new JLabel("PhiX"), gbcUp);
        }
        gbcUp.gridy++;
        gbcUp.gridx = iSim;
        selectionUp.add(showAlphaY, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = nSim;
            selectionUp.add(new JLabel("AlphaY"), gbcUp);
        }
        gbcUp.gridx = iSim + nSim + 1;
        selectionUp.add(showBetaY, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = 2 * nSim + 1;
            selectionUp.add(new JLabel("BetaY"), gbcUp);
        }
        gbcUp.gridy++;
        gbcUp.gridx = iSim;
        selectionUp.add(showEtaY, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = nSim;
            selectionUp.add(new JLabel("EtaY"), gbcUp);
        }
        gbcUp.gridx = iSim + nSim + 1;
        selectionUp.add(showEtaYP, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = 2 * nSim + 1;
            selectionUp.add(new JLabel("EtaYP"), gbcUp);
        }
        gbcUp.gridy++;
        gbcUp.gridx = iSim;
        selectionUp.add(showEmitY, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = nSim;
            selectionUp.add(new JLabel("EmitY"), gbcUp);
        }
        gbcUp.gridx = iSim + nSim + 1;
        selectionUp.add(showSigmaY, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = 2 * nSim + 1;
            selectionUp.add(new JLabel("SigmaY"), gbcUp);
        }
        gbcUp.gridy++;
        gbcUp.gridx = iSim;
        selectionUp.add(showPhiY, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = nSim;
            selectionUp.add(new JLabel("PhiY"), gbcUp);
        }
        gbcUp.gridy++;
        gbcUp.gridx = iSim;
        selectionUp.add(showAlphaZ, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = nSim;
            selectionUp.add(new JLabel("AlphaZ"), gbcUp);
        }
        gbcUp.gridx = iSim + nSim + 1;
        selectionUp.add(showBetaZ, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = 2 * nSim + 1;
            selectionUp.add(new JLabel("BetaZ"), gbcUp);
        }
        gbcUp.gridy++;
        gbcUp.gridx = iSim;
        selectionUp.add(showEmitZ, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = nSim;
            selectionUp.add(new JLabel("EmitZ"), gbcUp);
        }
        gbcUp.gridx = iSim + nSim + 1;
        selectionUp.add(showSigmaZ, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = 2 * nSim + 1;
            selectionUp.add(new JLabel("SigmaZ"), gbcUp);
        }
        gbcUp.gridy++;
        gbcUp.gridx = iSim;
        selectionUp.add(showPhiZ, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = nSim;
            selectionUp.add(new JLabel("PhiZ"), gbcUp);
        }
        gbcUp.gridx = iSim + nSim + 1;
        selectionUp.add(showEng, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = 2 * nSim + 1;
            selectionUp.add(new JLabel("Eng"), gbcUp);
        }
        gbcUp.gridy++;
        gbcUp.gridx = iSim;
        selectionUp.add(showApx, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = nSim;
            selectionUp.add(new JLabel("ApX"), gbcUp);
        }
        gbcUp.gridx = iSim + nSim + 1;
        selectionUp.add(showApy, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = 2 * nSim + 1;
            selectionUp.add(new JLabel("ApY"), gbcUp);
        }
        gbcUp.gridy++;
        gbcUp.gridx = iSim;
        selectionUp.add(showBpmX, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = nSim;
            selectionUp.add(new JLabel("BpmX"), gbcUp);
        }
        gbcUp.gridx = iSim + nSim + 1;
        selectionUp.add(showBpmY, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = 2 * nSim + 1;
            selectionUp.add(new JLabel("BpmY"), gbcUp);
        }
        gbcUp.gridy++;
        gbcUp.gridx = iSim;
        selectionUp.add(showWsmX, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = nSim;
            selectionUp.add(new JLabel("WsmX"), gbcUp);
        }
        gbcUp.gridx = iSim + nSim + 1;
        selectionUp.add(showWsmY, gbcUp);
        if (iSim == 0) {
            gbcUp.gridx = 2 * nSim + 1;
            selectionUp.add(new JLabel("WsmY"), gbcUp);
        }
    }

    public int addToChart(FunctionGraphsJPanel c, int iSim, int nSim, int dataViewCount) {
        chart = c;
        xds.setGraphColor(xColor);
        xds.setDrawLinesOn(true);
        xds.setGraphPointSize(symbolSize);
        xds.setLineThick(lineThickness);
        xpds.setGraphColor(xpColor);
        xpds.setDrawLinesOn(true);
        xpds.setGraphPointSize(symbolSize);
        xpds.setLineThick(lineThickness);
        alphaxds.setGraphColor(alphaxColor);
        alphaxds.setDrawLinesOn(true);
        alphaxds.setGraphPointSize(symbolSize);
        alphaxds.setLineThick(lineThickness);
        betaxds.setGraphColor(betaxColor);
        betaxds.setDrawLinesOn(true);
        betaxds.setGraphPointSize(symbolSize);
        betaxds.setLineThick(lineThickness);
        yds.setGraphColor(yColor);
        yds.setDrawLinesOn(true);
        yds.setGraphPointSize(symbolSize);
        yds.setLineThick(lineThickness);
        ypds.setGraphColor(ypColor);
        ypds.setDrawLinesOn(true);
        ypds.setGraphPointSize(symbolSize);
        ypds.setLineThick(lineThickness);
        alphayds.setGraphColor(alphayColor);
        alphayds.setDrawLinesOn(true);
        alphayds.setGraphPointSize(symbolSize);
        alphayds.setLineThick(lineThickness);
        betayds.setGraphColor(betayColor);
        betayds.setDrawLinesOn(true);
        betayds.setGraphPointSize(symbolSize);
        betayds.setLineThick(lineThickness);
        zds.setGraphColor(zColor);
        zds.setDrawLinesOn(true);
        zds.setGraphPointSize(symbolSize);
        zds.setLineThick(lineThickness);
        zpds.setGraphColor(zpColor);
        zpds.setDrawLinesOn(true);
        zpds.setGraphPointSize(symbolSize);
        zpds.setLineThick(lineThickness);
        alphazds.setGraphColor(alphazColor);
        alphazds.setDrawLinesOn(true);
        alphazds.setGraphPointSize(symbolSize);
        alphazds.setLineThick(lineThickness);
        betazds.setGraphColor(betazColor);
        betazds.setGraphPointSize(symbolSize);
        betazds.setLineThick(lineThickness);
        betazds.setDrawLinesOn(true);
        emitxds.setGraphColor(emitxColor);
        emitxds.setDrawLinesOn(true);
        emitxds.setGraphPointSize(symbolSize);
        emitxds.setLineThick(lineThickness);
        emityds.setGraphColor(emityColor);
        emityds.setDrawLinesOn(true);
        emityds.setGraphPointSize(symbolSize);
        emityds.setLineThick(lineThickness);
        emitzds.setGraphColor(emitzColor);
        emitzds.setDrawLinesOn(true);
        emitzds.setGraphPointSize(symbolSize);
        emitzds.setLineThick(lineThickness);
        sigmaxds.setGraphColor(sigmaxColor);
        sigmaxds.setDrawLinesOn(true);
        sigmaxds.setGraphPointSize(symbolSize);
        sigmaxds.setLineThick(lineThickness);
        sigmayds.setGraphColor(sigmayColor);
        sigmayds.setDrawLinesOn(true);
        sigmayds.setGraphPointSize(symbolSize);
        sigmayds.setLineThick(lineThickness);
        sigmazds.setGraphColor(sigmazColor);
        sigmazds.setDrawLinesOn(true);
        sigmazds.setGraphPointSize(symbolSize);
        sigmazds.setLineThick(lineThickness);
        phixds.setGraphColor(phixColor);
        phixds.setDrawLinesOn(true);
        phixds.setGraphPointSize(symbolSize);
        phixds.setLineThick(lineThickness);
        phiyds.setGraphColor(phiyColor);
        phiyds.setDrawLinesOn(true);
        phiyds.setGraphPointSize(symbolSize);
        phiyds.setLineThick(lineThickness);
        phizds.setGraphColor(phizColor);
        phizds.setDrawLinesOn(true);
        phizds.setGraphPointSize(symbolSize);
        phizds.setLineThick(lineThickness);
        engds.setGraphColor(engColor);
        engds.setDrawLinesOn(true);
        engds.setGraphPointSize(symbolSize);
        engds.setLineThick(lineThickness);
        apxds.setGraphColor(apxColor);
        apxds.setDrawLinesOn(true);
        apxds.setGraphPointSize(symbolSize);
        apxds.setLineThick(lineThickness);
        apyds.setGraphColor(apyColor);
        apyds.setDrawLinesOn(true);
        apyds.setGraphPointSize(symbolSize);
        apyds.setLineThick(lineThickness);
        bpmxds.setGraphColor(bpmxColor);
        bpmxds.setDrawLinesOn(true);
        bpmxds.setGraphPointSize(symbolSizeBPM);
        bpmxds.setLineThick(lineThickness);
        bpmyds.setGraphColor(bpmyColor);
        bpmyds.setDrawLinesOn(true);
        bpmyds.setGraphPointSize(symbolSizeBPM);
        bpmyds.setLineThick(lineThickness);
        wsmxds.setGraphColor(wsmxColor);
        wsmxds.setDrawLinesOn(false);
        wsmxds.setGraphPointSize(symbolSize);
        wsmxds.setLineThick(lineThickness);
        wsmyds.setGraphColor(wsmyColor);
        wsmyds.setDrawLinesOn(false);
        wsmyds.setGraphPointSize(symbolSize);
        wsmyds.setLineThick(lineThickness);
        etaxds.setGraphColor(etaxColor);
        etaxds.setDrawLinesOn(true);
        etaxds.setGraphPointSize(symbolSize);
        etaxds.setLineThick(lineThickness);
        etayds.setGraphColor(etayColor);
        etayds.setDrawLinesOn(true);
        etayds.setGraphPointSize(symbolSize);
        etayds.setLineThick(lineThickness);
        etaxpds.setGraphColor(etaxpColor);
        etaxpds.setDrawLinesOn(true);
        etaxpds.setGraphPointSize(symbolSize);
        etaxpds.setLineThick(lineThickness);
        etaypds.setGraphColor(etaypColor);
        etaypds.setDrawLinesOn(true);
        etaypds.setGraphPointSize(symbolSize);
        etaypds.setLineThick(lineThickness);
        apxds.setGraphColor(apxColor);
        apxds.setDrawLinesOn(true);
        apxds.setGraphPointSize(symbolSize);
        apxds.setLineThick(lineThickness);
        apyds.setGraphColor(apyColor);
        apyds.setDrawLinesOn(true);
        apyds.setGraphPointSize(symbolSize);
        apyds.setLineThick(lineThickness);
        etaxds.setGraphColor(etaxColor);
        etaxds.setDrawLinesOn(true);
        etaxds.setGraphPointSize(symbolSize);
        etaxds.setLineThick(lineThickness);
        etayds.setGraphColor(etayColor);
        etayds.setDrawLinesOn(true);
        etayds.setGraphPointSize(symbolSize);
        etayds.setLineThick(lineThickness);
        etaxpds.setGraphColor(etaxpColor);
        etaxpds.setDrawLinesOn(true);
        etaxpds.setGraphPointSize(symbolSize);
        etaxpds.setLineThick(lineThickness);
        etaypds.setGraphColor(etaypColor);
        etaypds.setDrawLinesOn(true);
        etaypds.setGraphPointSize(symbolSize);
        etaypds.setLineThick(lineThickness);
        xdsP.setDrawLinesOn(true);
        xdsP.setGraphPointSize(symbolSize);
        xdsP.setLineThick(lineThickness);
        xpdsP.setDrawLinesOn(true);
        xpdsP.setGraphPointSize(symbolSize);
        xpdsP.setLineThick(lineThickness);
        alphaxdsP.setDrawLinesOn(true);
        alphaxdsP.setGraphPointSize(symbolSize);
        alphaxdsP.setLineThick(lineThickness);
        betaxdsP.setDrawLinesOn(true);
        betaxdsP.setGraphPointSize(symbolSize);
        betaxdsP.setLineThick(lineThickness);
        ydsP.setDrawLinesOn(true);
        ydsP.setGraphPointSize(symbolSize);
        ydsP.setLineThick(lineThickness);
        ypdsP.setDrawLinesOn(true);
        ypdsP.setGraphPointSize(symbolSize);
        ypdsP.setLineThick(lineThickness);
        alphaydsP.setDrawLinesOn(true);
        alphaydsP.setGraphPointSize(symbolSize);
        alphaydsP.setLineThick(lineThickness);
        betaydsP.setDrawLinesOn(true);
        betaydsP.setGraphPointSize(symbolSize);
        betaydsP.setLineThick(lineThickness);
        zdsP.setDrawLinesOn(true);
        zdsP.setGraphPointSize(symbolSize);
        zdsP.setLineThick(lineThickness);
        zpdsP.setDrawLinesOn(true);
        zpdsP.setGraphPointSize(symbolSize);
        zpdsP.setLineThick(lineThickness);
        alphazdsP.setDrawLinesOn(true);
        alphazdsP.setGraphPointSize(symbolSize);
        alphazdsP.setLineThick(lineThickness);
        betazdsP.setGraphPointSize(symbolSize);
        betazdsP.setLineThick(lineThickness);
        betazdsP.setDrawLinesOn(true);
        emitxdsP.setDrawLinesOn(true);
        emitxdsP.setGraphPointSize(symbolSize);
        emitxdsP.setLineThick(lineThickness);
        emitydsP.setDrawLinesOn(true);
        emitydsP.setGraphPointSize(symbolSize);
        emitydsP.setLineThick(lineThickness);
        emitzdsP.setDrawLinesOn(true);
        emitzdsP.setGraphPointSize(symbolSize);
        emitzdsP.setLineThick(lineThickness);
        sigmaxdsP.setDrawLinesOn(true);
        sigmaxdsP.setGraphPointSize(symbolSize);
        sigmaxdsP.setLineThick(lineThickness);
        sigmaydsP.setDrawLinesOn(true);
        sigmaydsP.setGraphPointSize(symbolSize);
        sigmaydsP.setLineThick(lineThickness);
        sigmazdsP.setDrawLinesOn(true);
        sigmazdsP.setGraphPointSize(symbolSize);
        sigmazdsP.setLineThick(lineThickness);
        phixdsP.setDrawLinesOn(true);
        phixdsP.setGraphPointSize(symbolSize);
        phixdsP.setLineThick(lineThickness);
        phiydsP.setDrawLinesOn(true);
        phiydsP.setGraphPointSize(symbolSize);
        phiydsP.setLineThick(lineThickness);
        phizdsP.setDrawLinesOn(true);
        phizdsP.setGraphPointSize(symbolSize);
        phizdsP.setLineThick(lineThickness);
        engdsP.setDrawLinesOn(true);
        engdsP.setGraphPointSize(symbolSize);
        engdsP.setLineThick(lineThickness);
        apxdsP.setDrawLinesOn(true);
        apxdsP.setGraphPointSize(symbolSize);
        apxdsP.setLineThick(lineThickness);
        apydsP.setDrawLinesOn(true);
        apydsP.setGraphPointSize(symbolSize);
        apydsP.setLineThick(lineThickness);
        etaxdsP.setDrawLinesOn(true);
        etaxdsP.setGraphPointSize(symbolSize);
        etaxdsP.setLineThick(lineThickness);
        etaydsP.setDrawLinesOn(true);
        etaydsP.setGraphPointSize(symbolSize);
        etaydsP.setLineThick(lineThickness);
        etaxpdsP.setDrawLinesOn(true);
        etaxpdsP.setGraphPointSize(symbolSize);
        etaxpdsP.setLineThick(lineThickness);
        etaypdsP.setDrawLinesOn(true);
        etaypdsP.setGraphPointSize(symbolSize);
        etaypdsP.setLineThick(lineThickness);
        bpmxds.setGraphColor(bpmxColor);
        bpmxds.setDrawLinesOn(true);
        bpmxds.setGraphPointSize(symbolSizeBPM);
        bpmxds.setLineThick(lineThickness);
        bpmyds.setGraphColor(bpmyColor);
        bpmyds.setDrawLinesOn(true);
        bpmyds.setGraphPointSize(symbolSizeBPM);
        bpmyds.setLineThick(lineThickness);
        wsmxds.setGraphColor(wsmxColor);
        wsmxds.setDrawLinesOn(false);
        wsmxds.setGraphPointSize(symbolSize);
        wsmxds.setLineThick(lineThickness);
        wsmyds.setGraphColor(wsmyColor);
        wsmyds.setDrawLinesOn(false);
        wsmyds.setGraphPointSize(symbolSize);
        wsmyds.setLineThick(lineThickness);
        if (iSim == 0) {
            c.setBackground(Color.white);
            c.setGraphBackGroundColor(Color.white);
            c.setPreferredSize(new Dimension(100, 100));
            c.setMinimumSize(new Dimension(100, 100));
        }
        return dataViewCount;
    }

    public void load(File inFile) throws IOException {
        listsPrev = listsCur;
        listsCur = new TwissFastLists();
        listsCur.load(inFile);
    }

    public void save(File outFile, int showOption) throws IOException {
        if ((showOption == TwissPlotter.SHOW_CURRENT) || (showOption == TwissPlotter.SHOW_CURRENT_PREV)) {
            if (listsCur != null) {
                listsCur.save(outFile);
            } else {
                System.err.println("TwissPlotterSource.save, current data is empty, save aborting");
            }
        } else if (showOption == TwissPlotter.SHOW_PREV) {
            if (listsPrev != null) {
                listsPrev.save(outFile);
            } else {
                System.err.println("TwissPlotterSource.save, previous data is empty, save aborting");
            }
        } else if (showOption == TwissPlotter.SHOW_DIFFERENCE) {
            if (listsDiff != null) {
                listsDiff.save(outFile);
            } else {
                System.err.println("TwissPlotterSource.save, difference data is empty, save aborting");
            }
        } else {
            System.err.println("TwissPlotterSource.save, unknown showOption = " + showOption);
        }
    }

    public void setPlotCheckbox() {
        switch(simulationType) {
            case SimulationManagerFactory.Xal_Id:
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
                showPhiX.setEnabled(true);
                showPhiY.setEnabled(true);
                showPhiZ.setEnabled(true);
                showEtaX.setEnabled(true);
                showEtaY.setEnabled(true);
                showEtaXP.setEnabled(true);
                showEtaYP.setEnabled(true);
                showWsmX.setEnabled(true);
                showWsmY.setEnabled(true);
                showEng.setEnabled(true);
                showApx.setEnabled(true);
                showApy.setEnabled(true);
                break;
            case SimulationManagerFactory.T3d_Id:
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
                showPhiX.setEnabled(true);
                showPhiY.setEnabled(true);
                showPhiZ.setEnabled(true);
                showEtaX.setEnabled(true);
                showEtaY.setEnabled(true);
                showEtaXP.setEnabled(true);
                showEtaYP.setEnabled(true);
                showEng.setEnabled(true);
                showApx.setEnabled(false);
                showApy.setEnabled(false);
                break;
            case SimulationManagerFactory.Sad_Id:
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
                showPhiX.setEnabled(true);
                showPhiY.setEnabled(true);
                showPhiZ.setEnabled(true);
                showEtaX.setEnabled(true);
                showEtaY.setEnabled(true);
                showEtaXP.setEnabled(true);
                showEtaYP.setEnabled(true);
                showEng.setEnabled(true);
                showApx.setEnabled(false);
                showApy.setEnabled(false);
                break;
            case SimulationManagerFactory.Mad_Id:
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
                showPhiX.setEnabled(true);
                showPhiY.setEnabled(true);
                showPhiZ.setEnabled(true);
                showEtaX.setEnabled(true);
                showEtaY.setEnabled(true);
                showEtaXP.setEnabled(true);
                showEtaYP.setEnabled(true);
                showEng.setEnabled(true);
                showApx.setEnabled(false);
                showApy.setEnabled(false);
                break;
            default:
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
                showPhiX.setEnabled(true);
                showPhiY.setEnabled(true);
                showPhiZ.setEnabled(true);
                showEtaX.setEnabled(true);
                showEtaY.setEnabled(true);
                showEtaXP.setEnabled(true);
                showEtaYP.setEnabled(true);
                showWsmX.setEnabled(true);
                showWsmY.setEnabled(true);
                showEng.setEnabled(false);
                showApx.setEnabled(false);
                showApy.setEnabled(false);
        }
    }

    public void showCurrent(int showOption) {
        Date t1 = new Date();
        TwissFastLists listsTemp = listsCur;
        TwissFastLists listsTemp2 = null;
        if (useFastData == TWISS_FAST_LISTS) {
            if (showOption == TwissPlotter.SHOW_DIFFERENCE) {
                listsDiff = listsCur.difference(listsPrev);
                listsCur.printChi2All();
                listsTemp = listsDiff;
            } else if (showOption == TwissPlotter.SHOW_PREV) {
                listsTemp = listsPrev;
            } else if (showOption == TwissPlotter.SHOW_CURRENT_PREV) {
                listsTemp = listsCur;
                listsTemp2 = listsPrev;
            }
            if (listsTemp == null) {
                System.out.println("TwissPlotterSource> current data is empty, aborting...");
                return;
            }
            xds.addPoint(listsTemp.getS(), listsTemp.getX());
            xpds.addPoint(listsTemp.getS(), listsTemp.getXp());
            alphaxds.addPoint(listsTemp.getS(), listsTemp.getAx());
            betaxds.addPoint(listsTemp.getS(), listsTemp.getBx());
            yds.addPoint(listsTemp.getS(), listsTemp.getY());
            ypds.addPoint(listsTemp.getS(), listsTemp.getYp());
            alphayds.addPoint(listsTemp.getS(), listsTemp.getAy());
            betayds.addPoint(listsTemp.getS(), listsTemp.getBy());
            zds.addPoint(listsTemp.getS(), listsTemp.getZ());
            zpds.addPoint(listsTemp.getS(), listsTemp.getZp());
            alphazds.addPoint(listsTemp.getS(), listsTemp.getAz());
            betazds.addPoint(listsTemp.getS(), listsTemp.getBz());
            emitxds.addPoint(listsTemp.getS(), listsTemp.getEx());
            emityds.addPoint(listsTemp.getS(), listsTemp.getEy());
            emitzds.addPoint(listsTemp.getS(), listsTemp.getEz());
            sigmaxds.addPoint(listsTemp.getS(), listsTemp.getSx());
            sigmayds.addPoint(listsTemp.getS(), listsTemp.getSy());
            sigmazds.addPoint(listsTemp.getS(), listsTemp.getSz());
            phixds.addPoint(listsTemp.getS(), listsTemp.getMux());
            phiyds.addPoint(listsTemp.getS(), listsTemp.getMuy());
            phizds.addPoint(listsTemp.getS(), listsTemp.getMuz());
            etaxds.addPoint(listsTemp.getS(), listsTemp.getEtx());
            etayds.addPoint(listsTemp.getS(), listsTemp.getEty());
            etaxpds.addPoint(listsTemp.getS(), listsTemp.getEtpx());
            etaypds.addPoint(listsTemp.getS(), listsTemp.getEtpy());
            engds.addPoint(listsTemp.getS(), listsTemp.getW());
            if (listsTemp2 != null) {
                xdsP.addPoint(listsTemp2.getS(), listsTemp2.getX());
                xpdsP.addPoint(listsTemp2.getS(), listsTemp2.getXp());
                alphaxdsP.addPoint(listsTemp2.getS(), listsTemp2.getAx());
                betaxdsP.addPoint(listsTemp2.getS(), listsTemp2.getBx());
                ydsP.addPoint(listsTemp2.getS(), listsTemp2.getY());
                ypdsP.addPoint(listsTemp2.getS(), listsTemp2.getYp());
                alphaydsP.addPoint(listsTemp2.getS(), listsTemp2.getAy());
                betaydsP.addPoint(listsTemp2.getS(), listsTemp2.getBy());
                zdsP.addPoint(listsTemp2.getS(), listsTemp2.getZ());
                zpdsP.addPoint(listsTemp2.getS(), listsTemp2.getZp());
                alphazdsP.addPoint(listsTemp2.getS(), listsTemp2.getAz());
                betazdsP.addPoint(listsTemp2.getS(), listsTemp2.getBz());
                emitxdsP.addPoint(listsTemp2.getS(), listsTemp2.getEx());
                emitydsP.addPoint(listsTemp2.getS(), listsTemp2.getEy());
                emitzdsP.addPoint(listsTemp2.getS(), listsTemp2.getEz());
                sigmaxdsP.addPoint(listsTemp2.getS(), listsTemp2.getSx());
                sigmaydsP.addPoint(listsTemp2.getS(), listsTemp2.getSy());
                sigmazdsP.addPoint(listsTemp2.getS(), listsTemp2.getSz());
                phixdsP.addPoint(listsTemp2.getS(), listsTemp2.getMux());
                phiydsP.addPoint(listsTemp2.getS(), listsTemp2.getMuy());
                phizdsP.addPoint(listsTemp2.getS(), listsTemp2.getMuz());
                etaxdsP.addPoint(listsTemp2.getS(), listsTemp2.getEtx());
                etaydsP.addPoint(listsTemp2.getS(), listsTemp2.getEty());
                etaxpdsP.addPoint(listsTemp2.getS(), listsTemp2.getEtpx());
                etaypdsP.addPoint(listsTemp2.getS(), listsTemp2.getEtpy());
                engdsP.addPoint(listsTemp2.getS(), listsTemp2.getW());
            } else {
                xdsP.removeAllPoints();
                xpdsP.removeAllPoints();
                alphaxdsP.removeAllPoints();
                betaxdsP.removeAllPoints();
                ydsP.removeAllPoints();
                ypdsP.removeAllPoints();
                alphaydsP.removeAllPoints();
                betaydsP.removeAllPoints();
                zdsP.removeAllPoints();
                zpdsP.removeAllPoints();
                alphazdsP.removeAllPoints();
                betazdsP.removeAllPoints();
                emitxdsP.removeAllPoints();
                emitydsP.removeAllPoints();
                emitzdsP.removeAllPoints();
                sigmaxdsP.removeAllPoints();
                sigmaydsP.removeAllPoints();
                sigmazdsP.removeAllPoints();
                phixdsP.removeAllPoints();
                phiydsP.removeAllPoints();
                engdsP.removeAllPoints();
            }
            if (apPosData != null) {
                apxds.addPoint(apPosData[0], apxData[0]);
                apyds.addPoint(apPosData[0], apyData[0]);
            }
            if (bpmxData != null) {
                if (bpmxData[0] != null) {
                    bpmxds.addPoint(bpmPosData[0], bpmxData[0]);
                }
            }
            if (bpmyData != null) {
                if (bpmyData[0] != null) {
                    bpmyds.addPoint(bpmPosData[0], bpmyData[0]);
                }
            }
        } else {
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
            phixds.addPoint(posData[0], phixData[0]);
            phiyds.addPoint(posData[0], phiyData[0]);
            phizds.addPoint(posData[0], phizData[0]);
            if (bpmxData != null) {
                if (bpmxData[0] != null) {
                    bpmxds.addPoint(bpmPosData[0], bpmxData[0]);
                }
            }
            if (bpmyData != null) {
                if (bpmyData[0] != null) {
                    bpmyds.addPoint(bpmPosData[0], bpmyData[0]);
                }
            }
            etaxds.addPoint(posData[0], etaxData[0]);
            etayds.addPoint(posData[0], etayData[0]);
            etaxpds.addPoint(posData[0], etaxpData[0]);
            etaypds.addPoint(posData[0], etaypData[0]);
            engds.addPoint(posData[0], engData[0]);
            if (apPosData != null) {
                apxds.addPoint(apPosData[0], apxData[0]);
                apyds.addPoint(apPosData[0], apyData[0]);
            }
        }
        Date t2 = new Date();
        if (benchmark) {
            System.out.println("dt (showCurrent) = " + (t2.getTime() - t1.getTime()) / 1000.);
        }
    }

    private EditContext runSimulation() {
        EditContext context = null;
        try {
            context = manager.pass();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return context;
    }

    private List<TwissFastData> runFastSimulation() {
        List<TwissFastData> list = null;
        try {
            list = manager.fastpass();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    private TwissFastLists runFastListsSimulation() {
        TwissFastLists lists = null;
        try {
            lists = manager.fastlistspass();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lists;
    }

    private EditContext getTwissData(String twissFile) {
        EditContext context = null;
        try {
            context = SimulationManager.readData(twissFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return context;
    }

    private void updateFastPlot(List<TwissFastData> list) {
        updateAperture();
        updateBPM();
        int count = 0;
        count = list.size();
        if (count <= 0) {
            count = 1;
        }
        elementId = new String[1][count];
        posData = new double[1][count];
        xData = new double[1][count];
        xpData = new double[1][count];
        alphaxData = new double[1][count];
        betaxData = new double[1][count];
        yData = new double[1][count];
        ypData = new double[1][count];
        alphayData = new double[1][count];
        betayData = new double[1][count];
        zData = new double[1][count];
        zpData = new double[1][count];
        alphazData = new double[1][count];
        betazData = new double[1][count];
        emitxData = new double[1][count];
        emityData = new double[1][count];
        emitzData = new double[1][count];
        sigmaxData = new double[1][count];
        sigmayData = new double[1][count];
        sigmazData = new double[1][count];
        phixData = new double[1][count];
        phiyData = new double[1][count];
        phizData = new double[1][count];
        etaxData = new double[1][count];
        etayData = new double[1][count];
        etaxpData = new double[1][count];
        etaypData = new double[1][count];
        bpmxData = new double[1][count];
        bpmyData = new double[1][count];
        bpmxpData = new double[1][count];
        bpmypData = new double[1][count];
        wsmxData = new double[1][count];
        wsmyData = new double[1][count];
        engData = new double[1][count];
        Iterator<TwissFastData> iter = list.iterator();
        int icount = -1;
        int icountbpm = -1;
        while (iter.hasNext()) {
            TwissFastData data = iter.next();
            String id = data.getId();
            if (data != null) {
                icount++;
                elementId[0][icount] = data.getId();
                xData[0][icount] = data.getX();
                xpData[0][icount] = data.getXp();
                alphaxData[0][icount] = data.getAx();
                betaxData[0][icount] = data.getBx();
                yData[0][icount] = data.getY();
                ypData[0][icount] = data.getYp();
                alphayData[0][icount] = data.getAy();
                betayData[0][icount] = data.getBy();
                zData[0][icount] = data.getS();
                posData[0][icount] = data.getS();
                zpData[0][icount] = data.getZp();
                alphazData[0][icount] = data.getAz();
                betazData[0][icount] = data.getBz();
                emitxData[0][icount] = data.getEx();
                emityData[0][icount] = data.getEy();
                emitzData[0][icount] = data.getEz();
                etaxData[0][icount] = data.getEtx();
                etayData[0][icount] = data.getEty();
                etaxpData[0][icount] = data.getEtpx();
                etaypData[0][icount] = data.getEtpy();
                sigmaxData[0][icount] = data.getSx();
                sigmayData[0][icount] = data.getSy();
                sigmazData[0][icount] = data.getSz();
                phixData[0][icount] = data.getMux();
                phiyData[0][icount] = data.getMuy();
                phizData[0][icount] = data.getMuz();
                engData[0][icount] = data.getW();
                if (id.indexOf("RCS_C01:BPMQFL01") != -1) {
                    icountbpm++;
                    bpmxData[0][icountbpm] = xData[0][icount];
                    bpmyData[0][icountbpm] = yData[0][icount];
                    bpmxpData[0][icountbpm] = xpData[0][icount];
                    bpmypData[0][icountbpm] = ypData[0][icount];
                }
            }
            setViewVisible(false);
        }
    }

    private void updateFastListsPlot() {
        updateAperture();
        updateBPM();
        setViewVisible(false);
    }

    private void updatePlot(EditContext context) {
        updateAperture();
        updateBPM();
        int count = 0;
        System.out.println("TwissPlotterSource::updateData, simLabel = " + simLabel);
        SortOrdering sortOrder = new SortOrdering("z");
        List<GenericRecord> recall = context.getTable("twiss").getRecords(sortOrder);
        count = recall.size();
        if (count <= 0) {
            count = 1;
        }
        elementId = new String[1][count];
        posData = new double[1][count];
        xData = new double[1][count];
        xpData = new double[1][count];
        alphaxData = new double[1][count];
        betaxData = new double[1][count];
        yData = new double[1][count];
        ypData = new double[1][count];
        alphayData = new double[1][count];
        betayData = new double[1][count];
        zData = new double[1][count];
        zpData = new double[1][count];
        alphazData = new double[1][count];
        betazData = new double[1][count];
        emitxData = new double[1][count];
        emityData = new double[1][count];
        emitzData = new double[1][count];
        sigmaxData = new double[1][count];
        sigmayData = new double[1][count];
        sigmazData = new double[1][count];
        phixData = new double[1][count];
        phiyData = new double[1][count];
        phizData = new double[1][count];
        etaxData = new double[1][count];
        etayData = new double[1][count];
        etaxpData = new double[1][count];
        etaypData = new double[1][count];
        bpmxData = new double[1][count];
        bpmyData = new double[1][count];
        bpmxpData = new double[1][count];
        bpmypData = new double[1][count];
        wsmxData = new double[1][count];
        wsmyData = new double[1][count];
        engData = new double[1][count];
        Iterator<GenericRecord> reciter = recall.iterator();
        int icount = -1;
        int icountbpm = -1;
        while (reciter.hasNext()) {
            GenericRecord rec = reciter.next();
            String id = rec.stringValueForKey("id");
            if (rec != null) {
                icount++;
                elementId[0][icount] = rec.stringValueForKey("id");
                xData[0][icount] = rec.doubleValueForKey("x");
                xpData[0][icount] = rec.doubleValueForKey("xp");
                alphaxData[0][icount] = rec.doubleValueForKey("ax");
                betaxData[0][icount] = rec.doubleValueForKey("bx");
                yData[0][icount] = rec.doubleValueForKey("y");
                ypData[0][icount] = rec.doubleValueForKey("yp");
                alphayData[0][icount] = rec.doubleValueForKey("ay");
                betayData[0][icount] = rec.doubleValueForKey("by");
                zData[0][icount] = rec.doubleValueForKey("z");
                posData[0][icount] = zData[0][icount];
                zpData[0][icount] = rec.doubleValueForKey("zp");
                alphazData[0][icount] = rec.doubleValueForKey("az");
                betazData[0][icount] = rec.doubleValueForKey("bz");
                emitxData[0][icount] = rec.doubleValueForKey("ex");
                emityData[0][icount] = rec.doubleValueForKey("ey");
                emitzData[0][icount] = rec.doubleValueForKey("ez");
                etaxData[0][icount] = rec.doubleValueForKey("etx");
                etayData[0][icount] = rec.doubleValueForKey("ety");
                etaxpData[0][icount] = rec.doubleValueForKey("etpx");
                etaypData[0][icount] = rec.doubleValueForKey("etpy");
                sigmaxData[0][icount] = rec.doubleValueForKey("sx");
                sigmayData[0][icount] = rec.doubleValueForKey("sy");
                sigmazData[0][icount] = rec.doubleValueForKey("sz");
                phixData[0][icount] = rec.doubleValueForKey("mux");
                phiyData[0][icount] = rec.doubleValueForKey("muy");
                phizData[0][icount] = rec.doubleValueForKey("muz");
                if (id.indexOf("RCS_C01:BPMQFL01") != -1) {
                    icountbpm++;
                    bpmxData[0][icountbpm] = xData[0][icount];
                    bpmyData[0][icountbpm] = yData[0][icount];
                    bpmxpData[0][icountbpm] = xpData[0][icount];
                    bpmypData[0][icountbpm] = ypData[0][icount];
                }
            }
            setViewVisible(false);
        }
    }

    private void updateAperture() {
        if (simulationType != SimulationManagerFactory.Xal_Id) {
            return;
        }
        AcceleratorSeq seq = manager.getSequence();
        int count = 0;
        List<AcceleratorNode> list = seq.getAllNodes();
        count = list.size();
        if (count <= 0) {
            count = 1;
        }
        apxData = new double[1][count];
        apyData = new double[1][count];
        apPosData = new double[1][count];
        Iterator<AcceleratorNode> reciter = list.iterator();
        int icount = -1;
        while (reciter.hasNext()) {
            AcceleratorNode node = reciter.next();
            ApertureBucket aper = node.getAper();
            double z = seq.getPosition(node);
            if (node != null) {
                if (!node.getType().equals("Marker")) {
                    icount++;
                    apPosData[0][icount] = z;
                    apxData[0][icount] = aper.getAperX() * 1000;
                    apyData[0][icount] = aper.getAperY() * 1000;
                }
            }
        }
        setViewVisible(false);
    }

    private void updateBPM() {
        if (simulationType != SimulationManagerFactory.Xal_Id) {
            return;
        }
        if (!(manager.getChannelSource().equals(Scenario.SYNC_MODE_RF_DESIGN) || manager.getChannelSource().equals(Scenario.SYNC_MODE_MONITOR_ONLINE))) {
            return;
        }
        AcceleratorSeq seq = manager.getSequence();
        int count = 0;
        List<AcceleratorNode> list = seq.getAllNodes();
        count = list.size();
        if (count <= 0) {
            count = 1;
        }
        bpmxData = new double[1][count];
        bpmyData = new double[1][count];
        bpmPosData = new double[1][count];
        Iterator<AcceleratorNode> reciter = list.iterator();
        int icount = -1;
        while (reciter.hasNext()) {
            AcceleratorNode node = reciter.next();
            double z = seq.getPosition(node);
            if (node != null) {
                if (node.getType().equals("BPM")) {
                    BPM bpm = (BPM) node;
                    icount++;
                    bpmPosData[0][icount] = z;
                    try {
                        bpmxData[0][icount] = bpm.getXAvg();
                        bpmyData[0][icount] = bpm.getYAvg();
                    } catch (ConnectionException e) {
                        e.printStackTrace();
                    } catch (GetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        setViewVisible(false);
    }

    public void updateData(String twissFile) {
        EditContext context = getTwissData(twissFile);
        updatePlot(context);
    }

    public void updateData() {
        if (useFastData == TWISS_FAST_LISTS) {
            listsPrev = listsCur;
            listsCur = runFastListsSimulation();
            Date t1 = new Date();
            updateFastListsPlot();
            Date t2 = new Date();
            if (benchmark) {
                System.out.println("dt(updateFastListsPlot) = " + (t2.getTime() - t1.getTime()) / 1000.);
            }
        } else if (useFastData == TWISS_FAST_DATA) {
            List<TwissFastData> list = runFastSimulation();
            Date t1 = new Date();
            updateFastPlot(list);
            Date t2 = new Date();
            if (benchmark) {
                System.out.println("dt(updateFastPlot) = " + (t2.getTime() - t1.getTime()) / 1000.);
            }
        } else {
            EditContext context = runSimulation();
            Date t1 = new Date();
            updatePlot(context);
            Date t2 = new Date();
            if (benchmark) {
                System.out.println("dt(updatePlot) = " + (t2.getTime() - t1.getTime()) / 1000.);
            }
        }
    }

    public void reset() {
    }

    protected void setViewVisible(boolean b) {
    }

    /**
     * Handle user interactions
     */
    public void itemStateChanged(ItemEvent ev) {
        if (ev.getSource() instanceof JToggleButton) {
            JToggleButton source = (JToggleButton) ev.getSource();
            boolean state = source.isSelected();
            try {
                if (source == showX) {
                    if (state) {
                        chart.addGraphData(xds);
                        chart.addGraphData(xdsP);
                    } else {
                        chart.removeGraphData(xds);
                        chart.removeGraphData(xdsP);
                    }
                }
                if (source == showXP) {
                    if (state) {
                        chart.addGraphData(xpds);
                        chart.addGraphData(xpdsP);
                    } else {
                        chart.removeGraphData(xpds);
                        chart.removeGraphData(xpdsP);
                    }
                }
                if (source == showAlphaX) {
                    if (state) {
                        chart.addGraphData(alphaxds);
                        chart.addGraphData(alphaxdsP);
                    } else {
                        chart.removeGraphData(alphaxds);
                        chart.removeGraphData(alphaxdsP);
                    }
                }
                if (source == showBetaX) {
                    if (state) {
                        chart.addGraphData(betaxds);
                        chart.addGraphData(betaxdsP);
                    } else {
                        chart.removeGraphData(betaxds);
                        chart.removeGraphData(betaxdsP);
                    }
                }
                if (source == showY) {
                    if (state) {
                        chart.addGraphData(yds);
                        chart.addGraphData(ydsP);
                    } else {
                        chart.removeGraphData(yds);
                        chart.removeGraphData(ydsP);
                    }
                }
                if (source == showYP) {
                    if (state) {
                        chart.addGraphData(ypds);
                        chart.addGraphData(ypdsP);
                    } else {
                        chart.removeGraphData(ypds);
                        chart.removeGraphData(ypdsP);
                    }
                }
                if (source == showAlphaY) {
                    if (state) {
                        chart.addGraphData(alphayds);
                        chart.addGraphData(alphaydsP);
                    } else {
                        chart.removeGraphData(alphayds);
                        chart.removeGraphData(alphaydsP);
                    }
                }
                if (source == showBetaY) {
                    if (state) {
                        chart.addGraphData(betayds);
                        chart.addGraphData(betaydsP);
                    } else {
                        chart.removeGraphData(betayds);
                        chart.removeGraphData(betaydsP);
                    }
                }
                if (source == showZ) {
                    if (state) {
                        chart.addGraphData(zds);
                        chart.addGraphData(zdsP);
                    } else {
                        chart.removeGraphData(zds);
                        chart.removeGraphData(zdsP);
                    }
                }
                if (source == showZP) {
                    if (state) {
                        chart.addGraphData(zpds);
                        chart.addGraphData(zpdsP);
                    } else {
                        chart.removeGraphData(zpds);
                        chart.removeGraphData(zpdsP);
                    }
                }
                if (source == showAlphaZ) {
                    if (state) {
                        chart.addGraphData(alphazds);
                        chart.addGraphData(alphazdsP);
                    } else {
                        chart.removeGraphData(alphazds);
                        chart.removeGraphData(alphazdsP);
                    }
                }
                if (source == showBetaZ) {
                    if (state) {
                        chart.addGraphData(betazds);
                        chart.addGraphData(betazdsP);
                    } else {
                        chart.removeGraphData(betazds);
                        chart.removeGraphData(betazdsP);
                    }
                }
                if (source == showEmitX) {
                    if (state) {
                        chart.addGraphData(emitxds);
                        chart.addGraphData(emitxdsP);
                    } else {
                        chart.removeGraphData(emitxds);
                        chart.removeGraphData(emitxdsP);
                    }
                }
                if (source == showEmitY) {
                    if (state) {
                        chart.addGraphData(emityds);
                        chart.addGraphData(emitydsP);
                    } else {
                        chart.removeGraphData(emityds);
                        chart.removeGraphData(emitydsP);
                    }
                }
                if (source == showEmitZ) {
                    if (state) {
                        chart.addGraphData(emitzds);
                        chart.addGraphData(emitzdsP);
                    } else {
                        chart.removeGraphData(emitzds);
                        chart.removeGraphData(emitzdsP);
                    }
                }
                if (source == showSigmaX) {
                    if (state) {
                        chart.addGraphData(sigmaxds);
                        chart.addGraphData(sigmaxdsP);
                    } else {
                        chart.removeGraphData(sigmaxds);
                        chart.removeGraphData(sigmaxdsP);
                    }
                }
                if (source == showSigmaY) {
                    if (state) {
                        chart.addGraphData(sigmayds);
                        chart.addGraphData(sigmaydsP);
                    } else {
                        chart.removeGraphData(sigmayds);
                        chart.removeGraphData(sigmaydsP);
                    }
                }
                if (source == showSigmaZ) {
                    if (state) {
                        chart.addGraphData(sigmazds);
                        chart.addGraphData(sigmazdsP);
                    } else {
                        chart.removeGraphData(sigmazds);
                        chart.removeGraphData(sigmazdsP);
                    }
                }
                if (source == showPhiX) {
                    if (state) {
                        chart.addGraphData(phixds);
                        chart.addGraphData(phixdsP);
                    } else {
                        chart.removeGraphData(phixds);
                        chart.removeGraphData(phixdsP);
                    }
                }
                if (source == showPhiY) {
                    if (state) {
                        chart.addGraphData(phiyds);
                        chart.addGraphData(phiydsP);
                    } else {
                        chart.removeGraphData(phiyds);
                        chart.removeGraphData(phiydsP);
                    }
                }
                if (source == showPhiZ) {
                    if (state) {
                        chart.addGraphData(phizds);
                        chart.addGraphData(phizdsP);
                    } else {
                        chart.removeGraphData(phizds);
                        chart.removeGraphData(phizdsP);
                    }
                }
                if (source == showEng) {
                    if (state) {
                        chart.addGraphData(engds);
                        chart.addGraphData(engdsP);
                    } else {
                        chart.removeGraphData(engds);
                        chart.removeGraphData(engdsP);
                    }
                }
                if (source == showApx) {
                    if (state) {
                        chart.addGraphData(apxds);
                        chart.addGraphData(apxdsP);
                    } else {
                        chart.removeGraphData(apxds);
                        chart.removeGraphData(apxdsP);
                    }
                }
                if (source == showApy) {
                    if (state) {
                        chart.addGraphData(apyds);
                        chart.addGraphData(apydsP);
                    } else {
                        chart.removeGraphData(apyds);
                        chart.removeGraphData(apydsP);
                    }
                }
                if (source == showBpmX) {
                    if (state) chart.addGraphData(bpmxds); else chart.removeGraphData(bpmxds);
                }
                if (source == showBpmY) {
                    if (state) chart.addGraphData(bpmyds); else chart.removeGraphData(bpmyds);
                }
                if (source == showWsmX) {
                    if (state) chart.addGraphData(wsmxds); else chart.removeGraphData(wsmxds);
                }
                if (source == showWsmY) {
                    if (state) chart.addGraphData(wsmyds); else chart.removeGraphData(wsmyds);
                }
                if (source == showEtaX) {
                    if (state) {
                        chart.addGraphData(etaxds);
                        chart.addGraphData(etaxdsP);
                    } else {
                        chart.removeGraphData(etaxds);
                        chart.removeGraphData(etaxdsP);
                    }
                }
                if (source == showEtaY) {
                    if (state) {
                        chart.addGraphData(etayds);
                        chart.addGraphData(etaydsP);
                    } else {
                        chart.removeGraphData(etayds);
                        chart.removeGraphData(etaydsP);
                    }
                }
                if (source == showEtaXP) {
                    if (state) {
                        chart.addGraphData(etaxpds);
                        chart.addGraphData(etaxpdsP);
                    } else {
                        chart.removeGraphData(etaxpds);
                        chart.removeGraphData(etaxpdsP);
                    }
                }
                if (source == showEtaYP) {
                    if (state) {
                        chart.addGraphData(etaypds);
                        chart.addGraphData(etaypdsP);
                    } else {
                        chart.removeGraphData(etaypds);
                        chart.removeGraphData(etaypdsP);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void clearCheckBoxes() {
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
        showPhiX.setSelected(false);
        showPhiY.setSelected(false);
        showPhiZ.setSelected(false);
        showBpmX.setSelected(false);
        showBpmY.setSelected(false);
        showEtaX.setSelected(false);
        showEtaY.setSelected(false);
        showEtaXP.setSelected(false);
        showEtaYP.setSelected(false);
        showWsmX.setSelected(false);
        showWsmY.setSelected(false);
        showEng.setSelected(false);
        showApx.setSelected(false);
        showApy.setSelected(false);
    }

    public SimulationManager getManager() {
        return manager;
    }
}
