package gov.sns.apps.sclaffmonitor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.NumberFormat;
import gov.sns.tools.swing.DecimalField;
import gov.sns.xal.smf.impl.RfCavity;
import gov.sns.ca.*;

/**
 *
 * @author y32
 */
public class AffController implements ItemListener, ActionListener {

    JButton slcton;

    JButton slctoff;

    JButton slctfreeze;

    JButton slctreset;

    JButton slctupdate;

    JButton allon;

    JButton alloff;

    JButton allfreeze;

    JButton allreset;

    JButton allupdate;

    JLabel lbk;

    JLabel lbkp;

    JLabel lbki;

    JLabel lbstart;

    JLabel lbduration;

    JLabel lbtimeshift;

    JLabel lbmaxpulse;

    JLabel lbaverage;

    JLabel lbbuffer;

    JLabel lblimit;

    DecimalField tfk;

    DecimalField tfkp;

    DecimalField tfki;

    DecimalField tftimeshift;

    DecimalField tfstart;

    DecimalField tfduration;

    DecimalField tfmaxpulse;

    DecimalField tfaverage;

    JTextField tfbuffer;

    DecimalField tflimit;

    double affk;

    double affkp;

    double affki;

    double afftimeshift;

    double affstart;

    double affduration;

    double afflimit;

    int affmax;

    int affavg;

    String affbuffer;

    Channel ch;

    String sclpv;

    AffDocument myDoc;

    ChannelFactory cf = ChannelFactory.defaultFactory();

    NumberFormat nf = NumberFormat.getNumberInstance();

    public AffController(AffDocument doc) {
        myDoc = doc;
    }

    public JPanel makeControlPanel() {
        JPanel controlPanel = new JPanel(new GridLayout(4, 10, 2, 5));
        slcton = new JButton("On");
        slctoff = new JButton("Off");
        slctfreeze = new JButton("Freeze");
        slctreset = new JButton("Reset");
        slctupdate = new JButton("Update");
        allon = new JButton("On");
        alloff = new JButton("Off");
        allfreeze = new JButton("Freeze");
        allreset = new JButton("Reset");
        allupdate = new JButton("Update");
        allon.setForeground(Color.RED);
        allupdate.setForeground(Color.RED);
        alloff.setForeground(Color.RED);
        allfreeze.setForeground(Color.RED);
        allreset.setForeground(Color.RED);
        tfk = new DecimalField(affk, 5);
        tfkp = new DecimalField(affkp, 5);
        tfki = new DecimalField(affki, 5);
        tftimeshift = new DecimalField(afftimeshift, 5);
        tfstart = new DecimalField(affstart, 5);
        tfduration = new DecimalField(affduration, 5);
        tfmaxpulse = new DecimalField(affmax, 5);
        tfaverage = new DecimalField(affavg, 5);
        tflimit = new DecimalField(afflimit, 5);
        tfbuffer = new JTextField(5);
        lbk = new JLabel("     K");
        lbkp = new JLabel("     Kp");
        lbki = new JLabel("     Ki");
        lbstart = new JLabel("   Start");
        lbduration = new JLabel(" Duration");
        lbtimeshift = new JLabel("Time shift");
        lbmaxpulse = new JLabel("   Max.");
        lbaverage = new JLabel("Wf to Avg");
        lbbuffer = new JLabel("FF Buffer");
        lblimit = new JLabel("  Limit");
        slcton.setEnabled(true);
        slcton.addActionListener(this);
        slctoff.setEnabled(true);
        slctoff.addActionListener(this);
        slctfreeze.setEnabled(true);
        slctfreeze.addActionListener(this);
        slctreset.setEnabled(true);
        slctreset.addActionListener(this);
        slctupdate.setEnabled(true);
        slctupdate.addActionListener(this);
        allon.setEnabled(true);
        allon.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < myDoc.getSelector().allsclcavs; i++) {
                    ch = cf.getChannel(((RfCavity) myDoc.getSelector().allCavs.get(i)).channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_Mode");
                    try {
                        ch.putVal(2);
                    } catch (ConnectionException ce) {
                        myDoc.errormsg("Error connect " + ch.getId());
                    } catch (PutException pe) {
                        myDoc.errormsg("Error write " + ch.getId());
                    }
                }
            }
        });
        alloff.setEnabled(true);
        alloff.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < myDoc.getSelector().allsclcavs; i++) {
                    ch = cf.getChannel(((RfCavity) myDoc.getSelector().allCavs.get(i)).channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_Mode");
                    try {
                        ch.putVal(0);
                    } catch (ConnectionException ce) {
                        myDoc.errormsg("Error connection " + ch.getId());
                    } catch (PutException pe) {
                        myDoc.errormsg("Error write to PV " + ch.getId());
                    }
                }
            }
        });
        allfreeze.setEnabled(true);
        allfreeze.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < myDoc.getSelector().allsclcavs; i++) {
                    ch = cf.getChannel(((RfCavity) myDoc.getSelector().allCavs.get(i)).channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_Mode");
                    try {
                        ch.putVal(1);
                    } catch (ConnectionException ce) {
                        myDoc.errormsg("Error connection " + ch.getId());
                    } catch (PutException pe) {
                        myDoc.errormsg("Error write to PV " + ch.getId());
                    }
                }
            }
        });
        allreset.setEnabled(true);
        allreset.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < myDoc.getSelector().allsclcavs; i++) {
                    ch = cf.getChannel(((RfCavity) myDoc.getSelector().allCavs.get(i)).channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_Reset.PROC");
                    try {
                        ch.putVal(1);
                    } catch (ConnectionException ce) {
                        myDoc.errormsg("Error connection " + ch.getId());
                    } catch (PutException pe) {
                        myDoc.errormsg("Error write to PV " + ch.getId());
                    }
                }
            }
        });
        allupdate.setEnabled(true);
        allupdate.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < myDoc.getSelector().allsclcavs; i++) {
                    if (tflimit.getValue() >= 0.2) myDoc.getSelector().cavTableModel.setValueAt(tflimit.getValue(), i, 13);
                    sclpv = ((RfCavity) myDoc.getSelector().allCavs.get(i)).channelSuite().getChannel("cavAmpSet").getId().substring(0, 16);
                    update();
                }
            }
        });
        JLabel dummy1 = new JLabel("Selected");
        JLabel dummy2 = new JLabel("");
        JLabel dummy3 = new JLabel("");
        JLabel dummy4 = new JLabel("");
        JLabel dummy5 = new JLabel("");
        JLabel dummy6 = new JLabel("");
        JLabel dummy7 = new JLabel("");
        JLabel dummy8 = new JLabel("");
        JLabel dummy9 = new JLabel("");
        JLabel dummya = new JLabel("All Cavity");
        dummya.setForeground(Color.RED);
        controlPanel.add(dummy1);
        controlPanel.add(slctoff);
        controlPanel.add(dummy5);
        controlPanel.add(slctfreeze);
        controlPanel.add(dummy2);
        controlPanel.add(slctreset);
        controlPanel.add(dummy3);
        controlPanel.add(slcton);
        controlPanel.add(dummy4);
        controlPanel.add(slctupdate);
        controlPanel.add(lbk);
        controlPanel.add(lbkp);
        controlPanel.add(lbki);
        controlPanel.add(lbstart);
        controlPanel.add(lbduration);
        controlPanel.add(lbtimeshift);
        controlPanel.add(lbmaxpulse);
        controlPanel.add(lbaverage);
        controlPanel.add(lbbuffer);
        controlPanel.add(lblimit);
        controlPanel.add(tfk);
        controlPanel.add(tfkp);
        controlPanel.add(tfki);
        controlPanel.add(tfstart);
        controlPanel.add(tfduration);
        controlPanel.add(tftimeshift);
        controlPanel.add(tfmaxpulse);
        controlPanel.add(tfaverage);
        controlPanel.add(tfbuffer);
        controlPanel.add(tflimit);
        controlPanel.add(dummya);
        controlPanel.add(alloff);
        controlPanel.add(dummy6);
        controlPanel.add(allfreeze);
        controlPanel.add(dummy7);
        controlPanel.add(allreset);
        controlPanel.add(dummy8);
        controlPanel.add(allon);
        controlPanel.add(dummy9);
        controlPanel.add(allupdate);
        controlPanel.setVisible(true);
        return controlPanel;
    }

    private void update() {
        try {
            if (tfk.getValue() >= 0.) {
                ch = cf.getChannel(sclpv + "AFF_K");
                ch.putVal(tfk.getValue());
            }
            if (tfkp.getValue() >= 0.) {
                ch = cf.getChannel(sclpv + "AFF_Kp");
                ch.putVal(tfkp.getValue());
            }
            if (tfki.getValue() >= 0.) {
                ch = cf.getChannel(sclpv + "AFF_Ki");
                ch.putVal(tfki.getValue());
            }
            if (tftimeshift.getValue() >= 0.) {
                ch = cf.getChannel(sclpv + "AFF_Shift");
                ch.putVal(tftimeshift.getValue());
            }
            if (tfstart.getValue() >= 0.) {
                ch = cf.getChannel(sclpv + "AFF_Start");
                ch.putVal(tfstart.getValue());
            }
            if (tfduration.getValue() >= 0.) {
                ch = cf.getChannel(sclpv + "AFF_Offset");
                ch.putVal(tfduration.getValue());
                ch = cf.getChannel(sclpv + "CtlRFPW.PROC");
                ch.putVal(1);
            }
            if (tfmaxpulse.getValue() >= 0) {
                ch = cf.getChannel(sclpv + "AFFVetoMax");
                ch.putVal(tfmaxpulse.getValue());
            }
            if (tfaverage.getValue() >= 0) {
                ch = cf.getChannel(sclpv + "AFFAvgN");
                ch.putVal(tfaverage.getValue());
            }
            ch = cf.getChannel(sclpv + "FdFwd2_Ctl");
            String buffer2 = tfbuffer.getText().toLowerCase();
            if (buffer2.indexOf("on") != -1 || buffer2 == "1") ch.putVal(1); else if (buffer2.indexOf("off") != -1 || buffer2 == "0") ch.putVal(0); else ch.putVal(2);
        } catch (ConnectionException ce) {
            myDoc.errormsg("Error connection " + ch.getId());
        } catch (PutException pe) {
            myDoc.errormsg("Error write to PV " + ch.getId());
        }
    }

    public void itemStateChanged(ItemEvent ie) {
        Checkbox cb = (Checkbox) ie.getItemSelectable();
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getActionCommand().equals("On")) {
            for (int i = 0; i < myDoc.numberOfCav; i++) {
                ch = cf.getChannel(((RfCavity) myDoc.cav[i]).channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_Mode");
                try {
                    ch.putVal(2);
                } catch (ConnectionException ce) {
                    myDoc.errormsg("Error connection " + ch.getId());
                } catch (PutException pe) {
                    myDoc.errormsg("Error write to PV " + ch.getId());
                }
            }
        } else if (ae.getActionCommand().equals("Off")) {
            for (int i = 0; i < myDoc.numberOfCav; i++) {
                ch = cf.getChannel(((RfCavity) myDoc.cav[i]).channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_Mode");
                try {
                    ch.putVal(0);
                } catch (ConnectionException ce) {
                    myDoc.errormsg("Error connection " + ch.getId());
                } catch (PutException pe) {
                    myDoc.errormsg("Error write to PV " + ch.getId());
                }
            }
        } else if (ae.getActionCommand().equals("Freeze")) {
            for (int i = 0; i < myDoc.numberOfCav; i++) {
                ch = cf.getChannel(((RfCavity) myDoc.cav[i]).channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_Mode");
                try {
                    ch.putVal(1);
                } catch (ConnectionException ce) {
                    myDoc.errormsg("Error connection " + ch.getId());
                } catch (PutException pe) {
                    myDoc.errormsg("Error write to PV " + ch.getId());
                }
            }
        } else if (ae.getActionCommand().equals("Reset")) {
            for (int i = 0; i < myDoc.numberOfCav; i++) {
                ch = cf.getChannel(((RfCavity) myDoc.cav[i]).channelSuite().getChannel("cavAmpSet").getId().substring(0, 16) + "AFF_Reset.PROC");
                try {
                    ch.putVal(1);
                } catch (ConnectionException ce) {
                    myDoc.errormsg("Error connection " + ch.getId());
                } catch (PutException pe) {
                    myDoc.errormsg("Error write to PV " + ch.getId());
                }
            }
        } else if (ae.getActionCommand().equals("Update")) {
            for (int i = 0; i < myDoc.numberOfCav; i++) {
                sclpv = ((RfCavity) myDoc.cav[i]).channelSuite().getChannel("cavAmpSet").getId().substring(0, 16);
                update();
            }
        }
    }
}
