package gov.sns.apps.sclsetcm;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import gov.sns.ca.*;
import gov.sns.tools.swing.DecimalField;
import gov.sns.tools.apputils.SimpleChartPopupMenu;
import gov.sns.tools.plot.*;
import gov.sns.xal.smf.impl.RfCavity;
import gov.sns.xal.smf.impl.sclcavity.*;

/**
 *
 * @author y32
 */
public class SCLCmMonitor implements ItemListener, ActionListener {

    SCLCmDocument myDoc;

    double llrfDt;

    double bcmDt;

    double[] cavField;

    double[] cavPhase;

    double[] bcmWave;

    double[] bcmx;

    double[] cavx;

    Monitor cavPhaseMonitor = null;

    Monitor cavFieldMonitor = null;

    Monitor bcmMonitor = null;

    JPanel monitorPanel = null;

    JPanel boxPanel = null;

    JPanel graPanel = null;

    boolean running = false;

    boolean trans = false;

    int currentCAV;

    int currentBCM = 0;

    String bcmDtPv = "SCL_Diag:BCM00b:tSamplePeriod";

    String bcmWavePv = "SCL_Diag:BCM00:currentTBT";

    String[] cavs;

    String[] bcms = { "SCL:BCM00", "MEBT:BCM02", "MEBT:BCM11", "HEBT:BCM01", "DTL:BCM200", "DTL:BCM400", "CCL:BCM102" };

    JComboBox bcm;

    JComboBox cavity;

    JButton update;

    CurveData phase;

    CurveData amplitude;

    CurveData pulse;

    int strPt = 0;

    int endPt = 0;

    int difPt = 0;

    DecimalField tfStr;

    DecimalField tfEnd;

    DecimalField tfDif;

    JButton change;

    Phasor p1;

    Phasor p2;

    protected FunctionGraphsJPanel plotBeam;

    protected FunctionGraphsJPanel plotSignal;

    ChannelFactory cf = ChannelFactory.defaultFactory();

    Channel cavP;

    Channel cavF;

    Channel bcmC;

    public SCLCmMonitor(SCLCmDocument doc) {
        myDoc = doc;
        phase = new CurveData();
        amplitude = new CurveData();
        pulse = new CurveData();
        plotBeam = new FunctionGraphsJPanel();
        plotSignal = new FunctionGraphsJPanel();
        update = new JButton("Play");
        update.setEnabled(true);
        update.addActionListener(this);
        change = new JButton("Difference");
        change.setEnabled(true);
        change.addActionListener(this);
        bcm = new JComboBox(bcms);
        bcm.addActionListener(this);
        tfStr = new DecimalField(strPt, 5);
        tfEnd = new DecimalField(endPt, 5);
        tfDif = new DecimalField(difPt, 5);
    }

    public JPanel makeMonitorPanel() {
        monitorPanel = new JPanel();
        BorderLayout gdl = new BorderLayout();
        monitorPanel.setLayout(gdl);
        graPanel = new JPanel();
        graPanel.setLayout(new GridLayout(2, 1, 1, 1));
        graPanel.addMouseListener(new SimpleChartPopupMenu(plotBeam));
        plotBeam.setLayout(new FlowLayout());
        plotBeam.setGraphBackGroundColor(Color.white);
        plotBeam.setPreferredSize(new Dimension(400, 300));
        plotBeam.setAxisNames("Time (us)", "Beam current (A)");
        pulse.setColor(Color.RED);
        plotBeam.addCurveData(pulse);
        plotBeam.setVisible(true);
        graPanel.addMouseListener(new SimpleChartPopupMenu(plotSignal));
        plotSignal.setLayout(new FlowLayout());
        plotSignal.setGraphBackGroundColor(Color.white);
        plotSignal.setPreferredSize(new Dimension(400, 300));
        plotSignal.setAxisNames("Time (us)", "LLRF waveforms");
        amplitude.setColor(Color.RED);
        phase.setColor(Color.BLUE);
        plotSignal.addCurveData(amplitude);
        plotSignal.addCurveData(phase);
        plotSignal.setVisible(true);
        graPanel.add(plotSignal);
        graPanel.add(plotBeam);
        boxPanel = makeboxPanel();
        monitorPanel.add(graPanel, BorderLayout.CENTER);
        monitorPanel.add(boxPanel, BorderLayout.WEST);
        return monitorPanel;
    }

    public JPanel makeboxPanel() {
        if (myDoc.selectedCav != null) cavs = myDoc.selectedCav; else {
            cavs = new String[myDoc.getSelector().allCavs.size()];
            for (int i = 0; i < cavs.length; i++) cavs[i] = ((RfCavity) myDoc.cavSelector.allCavs.get(i)).getId();
        }
        currentCAV = 0;
        boxPanel = new JPanel();
        cavity = new JComboBox(cavs);
        cavity.addActionListener(this);
        JPanel boxPanel = new JPanel();
        JLabel dummy1 = new JLabel("");
        JLabel dummy2 = new JLabel("");
        JLabel dummy3 = new JLabel("");
        JLabel dummy4 = new JLabel("");
        JLabel dummy5 = new JLabel("");
        JLabel dummy6 = new JLabel("Start Point");
        JLabel dummy7 = new JLabel("End Point");
        JLabel dummy8 = new JLabel("Differ Point");
        boxPanel.setLayout(new GridLayout(20, 1, 5, 6));
        boxPanel.setPreferredSize(new Dimension(120, 200));
        boxPanel.add(dummy1);
        boxPanel.add(cavity);
        boxPanel.add(dummy6);
        boxPanel.add(tfStr);
        boxPanel.add(dummy7);
        boxPanel.add(tfEnd);
        boxPanel.add(dummy8);
        boxPanel.add(tfDif);
        boxPanel.add(change);
        boxPanel.add(dummy2);
        boxPanel.add(update);
        boxPanel.add(dummy3);
        boxPanel.add(dummy4);
        boxPanel.add(bcm);
        getDeltaT();
        return boxPanel;
    }

    public void itemStateChanged(ItemEvent ie) {
        Checkbox cb = (Checkbox) ie.getItemSelectable();
    }

    private void convert() {
        try {
            for (int i = strPt; i < endPt; i++) {
                p1 = new Phasor(cavField[i], cavPhase[i] * Constant.rad);
                p2 = new Phasor(cavField[i + difPt], cavPhase[i + difPt] * Constant.rad);
                p1.minus(p2);
                cavField[i] = p1.getam();
                cavPhase[i] = p1.getph() * Constant.deg;
                if (cavPhase[i] > 180.) cavPhase[i] -= 360.;
            }
        } catch (ArrayIndexOutOfBoundsException ae) {
            trans = false;
            change.setText("Difference");
            myDoc.errormsg(ae + " in monitor");
        }
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getActionCommand().equals(cavity.getActionCommand())) {
            stopmonitor();
            currentCAV = cavity.getSelectedIndex();
            currentBCM = bcm.getSelectedIndex();
            getBCMname();
            getDeltaT();
            startmonitor();
            running = true;
            update.setText("Stop");
        } else if (ae.getActionCommand().equals("Play")) {
            startmonitor();
            running = true;
            update.setText("Stop");
        } else if (ae.getActionCommand().equals("Stop")) {
            stopmonitor();
            running = false;
            update.setText("Play");
        } else if (ae.getActionCommand().equals("Difference")) {
            if (running) {
                stopmonitor();
                running = false;
                update.setText("Play");
            }
            strPt = (int) tfStr.getValue();
            endPt = (int) tfEnd.getValue();
            difPt = (int) tfDif.getValue();
            if (strPt * endPt * difPt == 0) trans = false; else {
                change.setText("Origin");
                trans = true;
            }
        } else if (ae.getActionCommand().equals("Origin")) {
            if (running) {
                stopmonitor();
                running = false;
                update.setText("Play");
            }
            trans = false;
            change.setText("Difference");
        }
    }

    public void startmonitor() {
        double dx = bcmDt * 1.E6;
        cavx = new double[512];
        bcmx = new double[1120];
        for (int i = 0; i < bcmx.length; i++) bcmx[i] = dx * i;
        for (int i = 0; i < cavx.length; i++) cavx[i] = llrfDt * i;
        bcmC = cf.getChannel(bcmWavePv);
        cavF = cf.getChannel(myDoc.cav[currentCAV].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "Field_WfA");
        cavP = cf.getChannel(myDoc.cav[currentCAV].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "Field_WfP");
        try {
            cavFieldMonitor = cavF.addMonitorValTime(new IEventSinkValTime() {

                public void eventValue(ChannelTimeRecord newRecord, Channel chan) {
                    cavField = newRecord.doubleArray();
                }
            }, Monitor.VALUE);
            cavPhaseMonitor = cavP.addMonitorValTime(new IEventSinkValTime() {

                public void eventValue(ChannelTimeRecord newRecord, Channel chan) {
                    cavPhase = newRecord.doubleArray();
                    if (trans) convert();
                    amplitude.setPoints(cavx, cavField);
                    phase.setPoints(cavx, cavPhase);
                    plotSignal.refreshGraphJPanel();
                }
            }, Monitor.VALUE);
        } catch (ConnectionException ce) {
            myDoc.errormsg("Error, in connection " + cavF.getId() + " or " + cavP.getId());
        } catch (MonitorException me) {
            myDoc.errormsg("Error, in LLRF waveform monitor " + me);
        }
        try {
            bcmMonitor = bcmC.addMonitorValTime(new IEventSinkValTime() {

                public void eventValue(ChannelTimeRecord newRecord, Channel chan) {
                    bcmWave = newRecord.doubleArray();
                    pulse.setPoints(bcmx, bcmWave);
                    plotBeam.refreshGraphJPanel();
                }
            }, Monitor.VALUE);
        } catch (ConnectionException ce) {
            myDoc.errormsg("Error, in connection " + bcmC.getId());
        } catch (MonitorException me) {
            myDoc.errormsg("Error, in beam current monitor " + me);
        }
    }

    public void stopmonitor() {
        if (cavPhaseMonitor != null) {
            cavPhaseMonitor.clear();
            cavPhaseMonitor = null;
        }
        if (cavFieldMonitor != null) {
            cavFieldMonitor.clear();
            cavFieldMonitor = null;
        }
        if (bcmMonitor != null) {
            bcmMonitor.clear();
            bcmMonitor = null;
        }
    }

    public void reset() {
        stopmonitor();
        currentCAV = 0;
        running = false;
        trans = false;
    }

    private void getBCMname() {
        switch(currentBCM) {
            case 0:
                bcmDtPv = "SCL_Diag:BCM00b:tSamplePeriod";
                bcmWavePv = "SCL_Diag:BCM00:currentTBT";
                break;
            case 5:
                {
                    bcmWavePv = "DTL_Diag:BCM400:currentTBT";
                    bcmDtPv = "DTL_Diag:BCM400:tSamplePeriod";
                    break;
                }
            case 6:
                {
                    bcmWavePv = "CCL_Diag:BCM102:currentTBT";
                    bcmDtPv = "CCL_Diag:BCM102:tSamplePeriod";
                    break;
                }
            case 3:
                {
                    bcmWavePv = "HEBT_Diag:BCM01:currentTBT";
                    bcmDtPv = "HEBT_Diag:BCM01:tSamplePeriod";
                    break;
                }
            case 2:
                {
                    bcmWavePv = "MEBT_Diag:BCM11:currentTBT";
                    bcmDtPv = "MEBT_Diag:BCM11:tSamplePeriod";
                    break;
                }
            case 4:
                {
                    bcmWavePv = "DTL_Diag:BCM200:currentTBT";
                    bcmDtPv = "DTL_Diag:BCM200:tSamplePeriod";
                    break;
                }
            default:
                bcmWavePv = "MEBT_Diag:BCM02:currentTBT";
                bcmDtPv = "MEBT_Diag:BCM02:tSamplePeriod";
        }
        return;
    }

    protected double[] getBCMwave() {
        double[] fullarray = new double[1];
        try {
            Channel ca1 = cf.getChannel(bcmWavePv);
            fullarray = ca1.getArrDbl();
        } catch (ConnectionException ce) {
            myDoc.errormsg(ce + " " + bcmWavePv);
        } catch (GetException ge) {
            myDoc.errormsg(ge + " " + bcmWavePv);
        }
        return fullarray;
    }

    private void getDeltaT() {
        try {
            Channel ca1 = cf.getChannel(bcmDtPv);
            bcmDt = ca1.getValDbl() * 1.E-6;
            myDoc.bcmDt = bcmDt;
            Channel ca2 = cf.getChannel(myDoc.cav[currentCAV].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "Wf_Dt");
            llrfDt = ca2.getValDbl();
            myDoc.llrfDt = llrfDt;
            myDoc.startPt = (int) Math.round(myDoc.endS / llrfDt);
            myDoc.pts = (int) Math.round((myDoc.endT - myDoc.endS) / llrfDt);
        } catch (ConnectionException ce) {
            myDoc.errormsg(ce + " Wf_Dt " + myDoc.cav[currentCAV]);
        } catch (GetException ge) {
            myDoc.errormsg(ge + " Wf_Dt " + myDoc.cav[currentCAV]);
        }
    }

    protected double[] getBeamOnly() {
        getBCMname();
        getDeltaT();
        double iMax = -100.;
        double[] beamArray;
        double[] fullArray = getBCMwave();
        for (int i = 0; i < fullArray.length; i++) {
            if (fullArray[i] > iMax) iMax = fullArray[i];
        }
        if (iMax > 0.005) {
            int start;
            int end;
            int counter = 0;
            while (fullArray[counter] < iMax / 9.) {
                counter++;
            }
            start = counter;
            while (fullArray[counter] > iMax / 9.) {
                counter++;
            }
            end = counter;
            if (start != end) {
                beamArray = new double[end - start];
                System.arraycopy(fullArray, start, beamArray, 0, end - start);
            } else {
                beamArray = new double[1];
            }
        } else {
            beamArray = new double[1];
        }
        return beamArray;
    }
}
