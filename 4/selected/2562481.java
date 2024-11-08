package gov.sns.apps.sclaffmonitor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import gov.sns.ca.*;
import gov.sns.tools.swing.DecimalField;
import gov.sns.tools.apputils.SimpleChartPopupMenu;
import gov.sns.tools.plot.*;

/**
 *
 * @author y32
 */
public class AffMonitor implements ItemListener, ActionListener {

    AffDocument myDoc;

    Monitor campMonitor = null;

    Monitor ierrMonitor = null;

    Monitor cphsMonitor = null;

    Monitor fampMonitor = null;

    Monitor qerrMonitor = null;

    Monitor fphsMonitor = null;

    Monitor peakMonitor = null;

    JPanel monitorPanel = null;

    JPanel title = null;

    JPanel contents = null;

    JButton update;

    JLabel cavname;

    JPanel summary;

    JButton on;

    JButton off;

    JButton freeze;

    JButton reset;

    JButton kip;

    JButton start;

    JButton duration;

    JButton tshift;

    JButton max;

    JButton avg;

    JButton bfon;

    JButton bfoff;

    JButton bfauto;

    JLabel error;

    JLabel buffer;

    DecimalField tfk;

    DecimalField tfkp;

    DecimalField tfki;

    DecimalField tftimeshift;

    DecimalField tfstart;

    DecimalField tfduration;

    DecimalField tfmax;

    DecimalField tfavg;

    DecimalField tferr;

    double affk;

    double affkp;

    double affki;

    double afftimeshift;

    double affstart;

    double affduration;

    int affmax;

    int affavg;

    double afferr;

    CurveData fwda;

    CurveData cava;

    CurveData erri;

    CurveData errq;

    CurveData cavp;

    CurveData fwdp;

    double[] cavx;

    double[] fpp;

    double[] fpa;

    double[] cvp;

    double[] cva;

    double[] eri;

    double[] erq;

    protected FunctionGraphsJPanel plotamp;

    protected FunctionGraphsJPanel ploterr;

    protected FunctionGraphsJPanel plotphs;

    ChannelFactory cf = ChannelFactory.defaultFactory();

    Channel cmd;

    Channel buf;

    public AffMonitor(AffDocument doc) {
        myDoc = doc;
    }

    public JPanel makeMonitorPanel() {
        monitorPanel = new JPanel();
        BorderLayout gdl = new BorderLayout();
        monitorPanel.setLayout(gdl);
        mktitle();
        mkcontents();
        monitorPanel.add(title, gdl.NORTH);
        monitorPanel.add(contents, BorderLayout.CENTER);
        return monitorPanel;
    }

    private void mkcontents() {
        if (contents != null) {
            monitorPanel.remove(contents);
            stopmonitor();
        }
        fwda = new CurveData();
        fwdp = new CurveData();
        cava = new CurveData();
        cavp = new CurveData();
        erri = new CurveData();
        errq = new CurveData();
        plotamp = new FunctionGraphsJPanel();
        ploterr = new FunctionGraphsJPanel();
        plotphs = new FunctionGraphsJPanel();
        contents = new JPanel();
        contents.setLayout(new GridLayout(2, 2));
        contents.addMouseListener(new SimpleChartPopupMenu(plotphs));
        plotphs.setLayout(new FlowLayout());
        plotphs.setGraphBackGroundColor(Color.white);
        plotphs.setPreferredSize(new Dimension(400, 300));
        plotphs.setAxisNames("Time (us)", "Phase Cav & Fwd");
        cavp.setColor(Color.RED);
        fwdp.setColor(Color.BLUE);
        plotphs.addCurveData(cavp);
        plotphs.addCurveData(fwdp);
        plotphs.setVisible(true);
        contents.addMouseListener(new SimpleChartPopupMenu(plotamp));
        plotamp.setLayout(new FlowLayout());
        plotamp.setGraphBackGroundColor(Color.white);
        plotamp.setPreferredSize(new Dimension(400, 300));
        plotamp.setAxisNames("Time (us)", "Amp. Cav & Fwd");
        cava.setColor(Color.RED);
        fwda.setColor(Color.BLUE);
        plotamp.addCurveData(cava);
        plotamp.addCurveData(fwda);
        plotamp.setVisible(true);
        contents.addMouseListener(new SimpleChartPopupMenu(ploterr));
        ploterr.setLayout(new FlowLayout());
        ploterr.setGraphBackGroundColor(Color.white);
        ploterr.setPreferredSize(new Dimension(400, 300));
        ploterr.setAxisNames("Time (us)", "Err I & Q");
        erri.setColor(Color.RED);
        errq.setColor(Color.BLUE);
        ploterr.addCurveData(erri);
        ploterr.addCurveData(errq);
        ploterr.setVisible(true);
        mksummary();
        contents.add(plotamp);
        contents.add(ploterr);
        contents.add(plotphs);
        contents.add(summary);
    }

    public void update() {
        monitorPanel.setVisible(false);
        mktitle();
        mkcontents();
        monitorPanel.add(title, BorderLayout.NORTH);
        monitorPanel.add(contents, BorderLayout.CENTER);
    }

    private void mktitle() {
        if (title != null) {
            monitorPanel.remove(title);
            stopmonitor();
        }
        title = new JPanel(new BorderLayout());
        cavname = new JLabel(myDoc.selectedCav[0]);
        cavname.setForeground(Color.BLUE);
        update = new JButton("Play");
        update.setEnabled(true);
        update.addActionListener(this);
        title.add(cavname, BorderLayout.WEST);
        title.add(update, BorderLayout.EAST);
    }

    private void mksummary() {
        summary = new JPanel();
        summary.setLayout(new GridLayout(8, 4, 2, 12));
        summary.setPreferredSize(new Dimension(400, 300));
        on = new JButton("On");
        off = new JButton("Off");
        freeze = new JButton("Freeze");
        reset = new JButton("Reset");
        reset.addActionListener(this);
        reset.setEnabled(true);
        cmd = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_Mode");
        buf = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "FdFwd2_Ctl");
        int cm = 0;
        try {
            cm = cmd.getRawValueRecord().intValue();
        } catch (ConnectionException ce) {
            myDoc.errormsg(ce + cmd.getId());
        } catch (GetException ge) {
            myDoc.errormsg(ge + cmd.getId());
        }
        mode(cm);
        on.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    cmd.putVal(2);
                } catch (ConnectionException ce) {
                    myDoc.errormsg("Error connection " + cmd.getId());
                } catch (PutException pe) {
                    myDoc.errormsg("Error write to PV " + cmd.getId());
                }
                mode(2);
            }
        });
        off.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    cmd.putVal(0);
                } catch (ConnectionException ce) {
                    myDoc.errormsg("Error connection " + cmd.getId());
                } catch (PutException pe) {
                    myDoc.errormsg("Error write to PV " + cmd.getId());
                }
                mode(0);
            }
        });
        freeze.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    cmd.putVal(1);
                } catch (ConnectionException ce) {
                    myDoc.errormsg("Error connection " + cmd.getId());
                } catch (PutException pe) {
                    myDoc.errormsg("Error write to PV " + cmd.getId());
                }
                mode(1);
            }
        });
        bfon = new JButton("On");
        bfoff = new JButton("Off");
        bfauto = new JButton("Auto");
        try {
            cm = buf.getRawValueRecord().intValue();
        } catch (ConnectionException ce) {
            myDoc.errormsg("Error connect " + buf.getId());
        } catch (GetException ge) {
            myDoc.errormsg("Error read " + buf.getId());
        }
        ctrl(cm);
        bfon.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    buf.putVal(1);
                } catch (ConnectionException ce) {
                    myDoc.errormsg("Error connection " + buf.getId());
                } catch (PutException pe) {
                    myDoc.errormsg("Error write to PV " + buf.getId());
                }
                ctrl(1);
            }
        });
        bfoff.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    buf.putVal(0);
                } catch (ConnectionException ce) {
                    myDoc.errormsg("Error connection " + buf.getId());
                } catch (PutException pe) {
                    myDoc.errormsg("Error write to PV " + buf.getId());
                }
                ctrl(0);
            }
        });
        bfauto.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                try {
                    buf.putVal(2);
                } catch (ConnectionException ce) {
                    myDoc.errormsg("Error connection " + buf.getId());
                } catch (PutException pe) {
                    myDoc.errormsg("Error write to PV " + buf.getId());
                }
                ctrl(2);
            }
        });
        kip = new JButton("K,Kp,Ki");
        kip.setEnabled(true);
        kip.addActionListener(this);
        start = new JButton("Start");
        start.setEnabled(true);
        start.addActionListener(this);
        duration = new JButton("Duration");
        duration.setEnabled(true);
        duration.addActionListener(this);
        tshift = new JButton("TShift");
        tshift.setEnabled(true);
        tshift.addActionListener(this);
        max = new JButton("Max.");
        max.setEnabled(true);
        max.addActionListener(this);
        avg = new JButton("Wf. Avg");
        avg.setEnabled(true);
        avg.addActionListener(this);
        error = new JLabel("Peak err (%)");
        buffer = new JLabel("Second buffer");
        getallparameters();
        tfk = new DecimalField(affk, 5);
        tfkp = new DecimalField(affkp, 5);
        tfki = new DecimalField(affki, 5);
        tftimeshift = new DecimalField(afftimeshift, 5);
        tfstart = new DecimalField(affstart, 5);
        tfduration = new DecimalField(affduration, 5);
        tfmax = new DecimalField(affmax, 5);
        tfavg = new DecimalField(affavg, 5);
        tferr = new DecimalField(afferr, 5);
        JLabel dy1 = new JLabel("");
        JLabel dy2 = new JLabel("AFF Control");
        JLabel dy3 = new JLabel("");
        JLabel dy4 = new JLabel("");
        JLabel dy5 = new JLabel("");
        JLabel dy6 = new JLabel("");
        JLabel dy7 = new JLabel("");
        summary.add(dy1);
        summary.add(dy2);
        summary.add(dy3);
        summary.add(dy4);
        summary.add(on);
        summary.add(off);
        summary.add(freeze);
        summary.add(reset);
        summary.add(kip);
        summary.add(tfk);
        summary.add(tfkp);
        summary.add(tfki);
        summary.add(start);
        summary.add(tfstart);
        summary.add(duration);
        summary.add(tfduration);
        summary.add(tshift);
        summary.add(tftimeshift);
        summary.add(error);
        summary.add(tferr);
        summary.add(max);
        summary.add(tfmax);
        summary.add(avg);
        summary.add(tfavg);
        summary.add(buffer);
        summary.add(bfon);
        summary.add(bfoff);
        summary.add(bfauto);
        summary.add(dy5);
        summary.add(dy6);
        summary.add(dy7);
        if (peakMonitor != null) {
            peakMonitor.clear();
            peakMonitor = null;
        }
        Channel c1 = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "PeakErr");
        try {
            peakMonitor = c1.addMonitorValTime(new IEventSinkValTime() {

                public void eventValue(ChannelTimeRecord newRecord, Channel chan) {
                    afferr = newRecord.doubleValue();
                    if (afferr >= 2.0) tferr.setForeground(Color.RED); else if (afferr >= 1.0) tferr.setForeground(Color.BLUE); else tferr.setForeground(Color.BLACK);
                    tferr.setValue(afferr);
                }
            }, Monitor.VALUE);
        } catch (ConnectionException ce) {
            myDoc.errormsg("Error connect " + c1.getId());
        } catch (MonitorException me) {
            myDoc.errormsg("Error monitor " + c1.getId());
        }
        summary.setVisible(true);
    }

    private void mode(int c) {
        if (c == 2) {
            on.setEnabled(false);
            off.setEnabled(true);
            freeze.setEnabled(true);
        } else if (c == 1) {
            on.setEnabled(true);
            off.setEnabled(true);
            freeze.setEnabled(false);
        } else {
            on.setEnabled(true);
            off.setEnabled(false);
            freeze.setEnabled(true);
        }
    }

    private void ctrl(int c) {
        if (c == 1) {
            bfon.setEnabled(false);
            bfoff.setEnabled(true);
            bfauto.setEnabled(true);
        } else if (c == 2) {
            bfon.setEnabled(true);
            bfoff.setEnabled(true);
            bfauto.setEnabled(false);
        } else {
            bfon.setEnabled(true);
            bfoff.setEnabled(false);
            bfauto.setEnabled(true);
        }
    }

    private void getallparameters() {
        Channel ca = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_K");
        try {
            affk = ca.getValDbl();
        } catch (ConnectionException ce) {
            myDoc.errormsg("Error " + ce);
        } catch (GetException ge) {
            myDoc.errormsg("Error " + ge);
        }
        ca = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_Kp");
        try {
            affkp = ca.getValDbl();
        } catch (ConnectionException ce) {
            myDoc.errormsg("Error " + ce);
        } catch (GetException ge) {
            myDoc.errormsg("Error " + ge);
        }
        ca = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_Ki");
        try {
            affki = ca.getValDbl();
        } catch (ConnectionException ce) {
            myDoc.errormsg("Error " + ce);
        } catch (GetException ge) {
            myDoc.errormsg("Error " + ge);
        }
        ca = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_Shift");
        try {
            afftimeshift = ca.getValDbl();
        } catch (ConnectionException ce) {
            myDoc.errormsg("Error " + ce);
        } catch (GetException ge) {
            myDoc.errormsg("Error " + ge);
        }
        ca = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_Start");
        try {
            affstart = ca.getValDbl();
        } catch (ConnectionException ce) {
            myDoc.errormsg("Error " + ce);
        } catch (GetException ge) {
            myDoc.errormsg("Error " + ge);
        }
        ca = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_Duration");
        try {
            affduration = ca.getValDbl();
        } catch (ConnectionException ce) {
            myDoc.errormsg("Error " + ce);
        } catch (GetException ge) {
            myDoc.errormsg("Error " + ge);
        }
        ca = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFFVetoMax");
        try {
            affmax = ca.getValInt();
        } catch (ConnectionException ce) {
            myDoc.errormsg("Error " + ce);
        } catch (GetException ge) {
            myDoc.errormsg("Error " + ge);
        }
        ca = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFFAvgN");
        try {
            affavg = ca.getValInt();
        } catch (ConnectionException ce) {
            myDoc.errormsg("Error " + ce);
        } catch (GetException ge) {
            myDoc.errormsg("Error " + ge);
        }
    }

    public void itemStateChanged(ItemEvent ie) {
        Checkbox cb = (Checkbox) ie.getItemSelectable();
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getActionCommand().equals("Play")) {
            startmonitor();
            update.setText("Stop");
        } else if (ae.getActionCommand().equals("Stop")) {
            stopmonitor();
            update.setText("Play");
        } else if (ae.getActionCommand().equals("K,Kp,Ki")) {
            Channel ca = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_K");
            try {
                ca.putVal(tfk.getValue());
            } catch (ConnectionException ce) {
                myDoc.errormsg(myDoc.selectedCav[0] + " K " + ce);
            } catch (PutException ge) {
                myDoc.errormsg(myDoc.selectedCav[0] + " K " + ge);
            }
            ca = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_Kp");
            try {
                ca.putVal(tfkp.getValue());
            } catch (ConnectionException ce) {
                myDoc.errormsg(myDoc.selectedCav[0] + " Kp " + ce);
            } catch (PutException ge) {
                myDoc.errormsg(myDoc.selectedCav[0] + " Kp " + ge);
            }
            ca = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_Ki");
            try {
                ca.putVal(tfki.getValue());
            } catch (ConnectionException ce) {
                myDoc.errormsg(myDoc.selectedCav[0] + " Ki " + ce);
            } catch (PutException ge) {
                myDoc.errormsg(myDoc.selectedCav[0] + " Ki " + ge);
            }
        } else if (ae.getActionCommand().equals("Start")) {
            Channel ca = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_Start");
            try {
                ca.putVal(tfstart.getValue());
            } catch (ConnectionException ce) {
                myDoc.errormsg(myDoc.selectedCav[0] + " Start " + ce);
            } catch (PutException ge) {
                myDoc.errormsg(myDoc.selectedCav[0] + " Start " + ge);
            }
        } else if (ae.getActionCommand().equals("Duration")) {
            Channel ca = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_Offset");
            try {
                ca.putVal(tfduration.getValue());
            } catch (ConnectionException ce) {
                myDoc.errormsg("Error " + ce);
            } catch (PutException ge) {
                myDoc.errormsg("Error " + ge);
            }
            ca = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "CtlRFPW.PROC");
            try {
                ca.putVal(1);
            } catch (ConnectionException ce) {
                myDoc.errormsg("Error " + ce);
            } catch (PutException ge) {
                myDoc.errormsg("Error " + ge);
            }
        } else if (ae.getActionCommand().equals("TShift")) {
            Channel ca = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_Shift");
            try {
                ca.putVal(tftimeshift.getValue());
            } catch (ConnectionException ce) {
                myDoc.errormsg("Error " + ce);
            } catch (PutException ge) {
                myDoc.errormsg("Error " + ge);
            }
        } else if (ae.getActionCommand().equals("Max.")) {
            Channel ca = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFFVetoMax");
            try {
                ca.putVal(tfmax.getValue());
            } catch (ConnectionException ce) {
                myDoc.errormsg("Error " + ce);
            } catch (PutException ge) {
                myDoc.errormsg("Error " + ge);
            }
        } else if (ae.getActionCommand().equals("Wf. Avg")) {
            Channel ca = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFFAvgN");
            try {
                ca.putVal(tfavg.getValue());
            } catch (ConnectionException ce) {
                myDoc.errormsg("Error " + ce);
            } catch (PutException ge) {
                myDoc.errormsg("Error " + ge);
            }
        } else if (ae.getActionCommand().equals("Reset")) {
            Channel ca = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_Reset.PROC");
            try {
                ca.putVal(1);
            } catch (ConnectionException ce) {
                myDoc.errormsg("Error " + ce);
            } catch (PutException ge) {
                myDoc.errormsg("Error " + ge);
            }
        }
    }

    public void startmonitor() {
        double dx = 0.;
        Channel dt = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "Wf_Dt");
        try {
            dx = dt.getValDbl();
        } catch (ConnectionException ce) {
            myDoc.errormsg(ce + " Wf_Dt\n");
        } catch (GetException ge) {
            myDoc.errormsg(ge + " Wf_Dt\n");
        }
        cavx = new double[512];
        for (int i = 0; i < cavx.length; i++) {
            cavx[i] = dx * i;
        }
        Channel cavA = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "Field_WfA");
        Channel cavP = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "Field_WfP");
        Channel fwdA = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "Fwd_WfA");
        Channel fwdP = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "Fwd_WfP");
        Channel errI = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_Err_I");
        Channel errQ = cf.getChannel(myDoc.cav[0].channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_Err_Q");
        try {
            campMonitor = cavA.addMonitorValTime(new IEventSinkValTime() {

                public void eventValue(ChannelTimeRecord newRecord, Channel chan) {
                    cva = newRecord.doubleArray();
                    cava.setPoints(cavx, cva);
                    plotamp.refreshGraphJPanel();
                }
            }, Monitor.VALUE);
            cphsMonitor = cavP.addMonitorValTime(new IEventSinkValTime() {

                public void eventValue(ChannelTimeRecord newRecord, Channel chan) {
                    cvp = newRecord.doubleArray();
                    cavp.setPoints(cavx, cvp);
                    plotphs.refreshGraphJPanel();
                }
            }, Monitor.VALUE);
            fampMonitor = fwdA.addMonitorValTime(new IEventSinkValTime() {

                public void eventValue(ChannelTimeRecord newRecord, Channel chan) {
                    fpa = newRecord.doubleArray();
                    fwda.setPoints(cavx, fpa);
                    plotamp.refreshGraphJPanel();
                }
            }, Monitor.VALUE);
            fphsMonitor = fwdP.addMonitorValTime(new IEventSinkValTime() {

                public void eventValue(ChannelTimeRecord newRecord, Channel chan) {
                    fpp = newRecord.doubleArray();
                    fwdp.setPoints(cavx, fpp);
                    plotphs.refreshGraphJPanel();
                }
            }, Monitor.VALUE);
            ierrMonitor = errI.addMonitorValTime(new IEventSinkValTime() {

                public void eventValue(ChannelTimeRecord newRecord, Channel chan) {
                    eri = newRecord.doubleArray();
                    erri.setPoints(cavx, eri);
                    ploterr.refreshGraphJPanel();
                }
            }, Monitor.VALUE);
            qerrMonitor = errQ.addMonitorValTime(new IEventSinkValTime() {

                public void eventValue(ChannelTimeRecord newRecord, Channel chan) {
                    erq = newRecord.doubleArray();
                    errq.setPoints(cavx, erq);
                    ploterr.refreshGraphJPanel();
                }
            }, Monitor.VALUE);
        } catch (ConnectionException ce) {
            myDoc.errormsg("Error, in connection Amp or Err");
        } catch (MonitorException me) {
            myDoc.errormsg("Error, in LLRF waveform monitor " + me);
        }
    }

    public void stopmonitor() {
        if (campMonitor != null) {
            campMonitor.clear();
            campMonitor = null;
            fampMonitor.clear();
            fampMonitor = null;
        }
        if (ierrMonitor != null) {
            ierrMonitor.clear();
            ierrMonitor = null;
            qerrMonitor.clear();
            qerrMonitor = null;
        }
        if (cphsMonitor != null) {
            cphsMonitor.clear();
            cphsMonitor = null;
            fphsMonitor.clear();
            fphsMonitor = null;
        }
    }
}
