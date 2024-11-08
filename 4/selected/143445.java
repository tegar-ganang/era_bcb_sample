package gov.sns.apps.istuner;

import gov.sns.ca.Channel;
import gov.sns.ca.ChannelFactory;
import gov.sns.ca.ChannelTimeRecord;
import gov.sns.ca.ConnectionException;
import gov.sns.ca.PutException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.WindowConstants;

public class IsTunerFrame extends JFrame {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private MonitorLabel labelLEBTSCT;

    private MonitorLabel labelMEBT1SCT;

    private MonitorLabel labelFILVOLTRB;

    private MonitorLabel labelFILVOLTMON;

    private LEDMonitorLabel labelFILONSTAT;

    private SetChannelSpinner spinnerFILVOLTSET;

    private IsTunerProperties prop;

    private IsTunerRefProperties refprop;

    private JButton buttonFILVOLTSTB;

    private JButton buttonSaveRef;

    private JLabel labelMaxFilVolt;

    private JLabel labelMinFilVolt;

    private JLabel labelMaxLEBTSCT;

    private JLabel labelMinLEBTSCT;

    private JLabel labelMaxMEBT1SCT;

    private JLabel labelMinMEBT1SCT;

    private JLabel labelRefDate;

    private JRadioButton rbLEBTSCT;

    private JRadioButton rbMEBT1SCT;

    private ButtonGroup rbGroup;

    private DecimalFormat df;

    private SimpleDateFormat sdf;

    public IsTunerFrame() {
        sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        df = new DecimalFormat("###.00");
        prop = new IsTunerProperties();
        refprop = new IsTunerRefProperties();
        createLabels();
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setPreferredSize(new Dimension(200, 100));
    }

    private void saveRef() {
        ChannelTimeRecord recLEBTSCT = labelLEBTSCT.takeSnapShot();
        ChannelTimeRecord recMEBT1SCT = labelMEBT1SCT.takeSnapShot();
        ChannelTimeRecord recFILVOLTRB = labelFILVOLTRB.takeSnapShot();
        if ((recLEBTSCT != null) && (recMEBT1SCT != null) && (recFILVOLTRB != null)) {
            double reffilvoltrb = recFILVOLTRB.doubleValue();
            double reflebtsct = recLEBTSCT.doubleValue();
            double refmebt1sct = recMEBT1SCT.doubleValue();
            Date refdate = new Date();
            refprop.setRef(reffilvoltrb, reflebtsct, refmebt1sct, refdate);
            refprop.save();
            updateRefLabels();
            spinnerFILVOLTSET.setRef(refprop.getRefFilVolt(), refprop.getRefFilVolt() - prop.getFilVoltRange(), refprop.getRefFilVolt() + prop.getFilVoltRange(), prop.getFilVoltVarUnit());
            repaint();
        }
    }

    private void updateRefLabels() {
        labelMaxLEBTSCT.setText(df.format(refprop.getRefLEBTSCT() * (1. + prop.getCurToleranceFrac())));
        labelMinLEBTSCT.setText(df.format(refprop.getRefLEBTSCT() * (1. - prop.getCurToleranceFrac())));
        labelMaxMEBT1SCT.setText(df.format(refprop.getRefMEBT1SCT() * (1. + prop.getCurToleranceFrac())));
        labelMinMEBT1SCT.setText(df.format(refprop.getRefMEBT1SCT() * (1. - prop.getCurToleranceFrac())));
        labelMaxFilVolt.setText(df.format(refprop.getRefFilVolt() + prop.getFilVoltRange()));
        labelMinFilVolt.setText(df.format(refprop.getRefFilVolt() - prop.getFilVoltRange()));
        labelRefDate.setText(sdf.format(refprop.getRefDate()));
        labelLEBTSCT.setValRef(refprop.getRefLEBTSCT());
        labelMEBT1SCT.setValRef(refprop.getRefMEBT1SCT());
        repaint();
    }

    private void createLabels() {
        labelFILONSTAT = new LEDMonitorLabel(prop.getChFilOnStat(), "Filament ON/OFF", prop.getBitFilOnStat());
        labelLEBTSCT = new MonitorLabel(prop.getChLEBTSCT(), "");
        labelLEBTSCT.setValRef(refprop.getRefLEBTSCT());
        labelLEBTSCT.setValRange(prop.getCurToleranceFrac() * refprop.getRefLEBTSCT());
        labelLEBTSCT.setCheckRange(true);
        labelMEBT1SCT = new MonitorLabel(prop.getChMEBT1SCT(), "");
        labelMEBT1SCT.setValRef(refprop.getRefMEBT1SCT());
        labelMEBT1SCT.setValRange(prop.getCurToleranceFrac() * refprop.getRefMEBT1SCT());
        labelMEBT1SCT.setCheckRange(false);
        labelFILVOLTRB = new MonitorLabel(prop.getChFilVoltRB(), "RB");
        labelFILVOLTMON = new MonitorLabel(prop.getChFilVoltMon(), "Mon");
        double ref = refprop.getRefFilVolt();
        double range = prop.getFilVoltRange();
        double step = prop.getFilVoltVarUnit();
        spinnerFILVOLTSET = new SetChannelSpinner(prop.getChFilVoltSet(), "", ref, ref - range, ref + range, step, prop.getCaputFlag());
        buttonFILVOLTSTB = new JButton("Set");
        buttonFILVOLTSTB.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                Channel channel = ChannelFactory.defaultFactory().getChannel(prop.getChFilVoltSTB());
                channel.connectAndWait();
                double newVal = prop.getValFilVoltSTB();
                try {
                    if (prop.getCaputFlag()) {
                        channel.putVal(newVal);
                    } else {
                        System.out.println("test mode, no caput");
                    }
                } catch (ConnectionException e1) {
                    e1.printStackTrace();
                } catch (PutException e1) {
                    e1.printStackTrace();
                }
            }
        });
        buttonSaveRef = new JButton("Save Ref data");
        buttonSaveRef.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                int answer = JOptionPane.showConfirmDialog(getContentPane(), "Are you sure to update SCT and filament reference data?");
                if (answer == JOptionPane.YES_OPTION) {
                    saveRef();
                }
            }
        });
        JPanel mainPanel = new JPanel();
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.insets = new Insets(10, 0, 10, 0);
        mainPanel.setLayout(gridbag);
        this.getContentPane().add(mainPanel, BorderLayout.CENTER);
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 10;
        constraints.gridheight = 2;
        JLabel titleFILONSTAT = new JLabel("Filament ON/OFF");
        gridbag.setConstraints(titleFILONSTAT, constraints);
        mainPanel.add(titleFILONSTAT);
        constraints.gridx = 10;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.gridheight = 2;
        gridbag.setConstraints(labelFILONSTAT, constraints);
        mainPanel.add(labelFILONSTAT);
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 10;
        constraints.gridheight = 2;
        JLabel titleFILVOLT = new JLabel("Filament Voltage (V)");
        gridbag.setConstraints(titleFILVOLT, constraints);
        mainPanel.add(titleFILVOLT);
        constraints.gridx = 10;
        constraints.gridy = 2;
        constraints.gridwidth = 5;
        constraints.gridheight = 2;
        gridbag.setConstraints(labelFILVOLTMON, constraints);
        mainPanel.add(labelFILVOLTMON);
        constraints.gridx = 15;
        constraints.gridy = 2;
        constraints.gridwidth = 5;
        constraints.gridheight = 2;
        gridbag.setConstraints(labelFILVOLTRB, constraints);
        mainPanel.add(labelFILVOLTRB);
        constraints.gridx = 22;
        constraints.gridy = 2;
        constraints.gridwidth = 3;
        constraints.gridheight = 2;
        gridbag.setConstraints(spinnerFILVOLTSET, constraints);
        mainPanel.add(spinnerFILVOLTSET);
        constraints.gridx = 29;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        JLabel titleMaxFilVolt = new JLabel("max:");
        gridbag.setConstraints(titleMaxFilVolt, constraints);
        mainPanel.add(titleMaxFilVolt);
        constraints.gridx = 31;
        constraints.gridy = 2;
        constraints.gridwidth = 4;
        constraints.gridheight = 1;
        labelMaxFilVolt = new JLabel(df.format(refprop.getRefFilVolt() + prop.getFilVoltRange()));
        gridbag.setConstraints(labelMaxFilVolt, constraints);
        mainPanel.add(labelMaxFilVolt);
        constraints.gridx = 29;
        constraints.gridy = 3;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        JLabel titleMinFilVolt = new JLabel("min:");
        gridbag.setConstraints(titleMinFilVolt, constraints);
        mainPanel.add(titleMinFilVolt);
        constraints.gridx = 31;
        constraints.gridy = 3;
        constraints.gridwidth = 4;
        constraints.gridheight = 1;
        labelMinFilVolt = new JLabel(df.format(refprop.getRefFilVolt() - prop.getFilVoltRange()));
        gridbag.setConstraints(labelMinFilVolt, constraints);
        mainPanel.add(labelMinFilVolt);
        constraints.gridx = 25;
        constraints.gridy = 2;
        constraints.gridwidth = 2;
        constraints.gridheight = 2;
        gridbag.setConstraints(buttonFILVOLTSTB, constraints);
        mainPanel.add(buttonFILVOLTSTB);
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = 10;
        constraints.gridheight = 2;
        JLabel titleLEBTSCT = new JLabel("LEBT SCT current (mA)");
        gridbag.setConstraints(titleLEBTSCT, constraints);
        mainPanel.add(titleLEBTSCT);
        constraints.gridx = 15;
        constraints.gridy = 4;
        constraints.gridwidth = 5;
        constraints.gridheight = 2;
        gridbag.setConstraints(labelLEBTSCT, constraints);
        mainPanel.add(labelLEBTSCT);
        constraints.gridx = 23;
        constraints.gridy = 4;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        JLabel titleMaxLEBTSCT = new JLabel("max:");
        gridbag.setConstraints(titleMaxLEBTSCT, constraints);
        mainPanel.add(titleMaxLEBTSCT);
        constraints.gridx = 23;
        constraints.gridy = 5;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        JLabel titleMinLEBTSCT = new JLabel("min:");
        gridbag.setConstraints(titleMinLEBTSCT, constraints);
        mainPanel.add(titleMinLEBTSCT);
        constraints.gridx = 25;
        constraints.gridy = 4;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        labelMaxLEBTSCT = new JLabel(df.format(refprop.getRefLEBTSCT() * (1 + prop.getCurToleranceFrac())));
        gridbag.setConstraints(labelMaxLEBTSCT, constraints);
        mainPanel.add(labelMaxLEBTSCT);
        constraints.gridx = 25;
        constraints.gridy = 5;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        labelMinLEBTSCT = new JLabel(df.format(refprop.getRefLEBTSCT() * (1 - prop.getCurToleranceFrac())));
        gridbag.setConstraints(labelMinLEBTSCT, constraints);
        mainPanel.add(labelMinLEBTSCT);
        rbLEBTSCT = new JRadioButton("", true);
        rbMEBT1SCT = new JRadioButton("", false);
        rbGroup = new ButtonGroup();
        rbGroup.add(rbLEBTSCT);
        rbGroup.add(rbMEBT1SCT);
        rbLEBTSCT.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (rbLEBTSCT.isSelected()) {
                    labelLEBTSCT.setCheckRange(true);
                    labelMEBT1SCT.setCheckRange(false);
                } else {
                    labelLEBTSCT.setCheckRange(false);
                    labelMEBT1SCT.setCheckRange(true);
                }
            }
        });
        rbMEBT1SCT.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (rbMEBT1SCT.isSelected()) {
                    labelLEBTSCT.setCheckRange(false);
                    labelMEBT1SCT.setCheckRange(true);
                } else {
                    labelLEBTSCT.setCheckRange(true);
                    labelMEBT1SCT.setCheckRange(false);
                }
            }
        });
        constraints.gridx = 10;
        constraints.gridy = 4;
        constraints.gridwidth = 5;
        constraints.gridheight = 1;
        JLabel titlerb = new JLabel("ref select");
        gridbag.setConstraints(titlerb, constraints);
        mainPanel.add(titlerb);
        constraints.gridx = 10;
        constraints.gridy = 5;
        constraints.gridwidth = 5;
        constraints.gridheight = 1;
        gridbag.setConstraints(rbLEBTSCT, constraints);
        mainPanel.add(rbLEBTSCT);
        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 10;
        constraints.gridheight = 2;
        JLabel titleMEBT1SCT = new JLabel("MEBT1 SCT current (mA)");
        gridbag.setConstraints(titleMEBT1SCT, constraints);
        mainPanel.add(titleMEBT1SCT);
        constraints.gridx = 15;
        constraints.gridy = 6;
        constraints.gridwidth = 5;
        constraints.gridheight = 2;
        gridbag.setConstraints(labelMEBT1SCT, constraints);
        mainPanel.add(labelMEBT1SCT);
        constraints.gridx = 23;
        constraints.gridy = 6;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        JLabel titleMaxMEBT1SCT = new JLabel("max:");
        gridbag.setConstraints(titleMaxMEBT1SCT, constraints);
        mainPanel.add(titleMaxMEBT1SCT);
        constraints.gridx = 23;
        constraints.gridy = 7;
        constraints.gridwidth = 2;
        constraints.gridheight = 1;
        JLabel titleMinMEBT1SCT = new JLabel("min:");
        gridbag.setConstraints(titleMinMEBT1SCT, constraints);
        mainPanel.add(titleMinMEBT1SCT);
        constraints.gridx = 25;
        constraints.gridy = 6;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        labelMaxMEBT1SCT = new JLabel(df.format(refprop.getRefMEBT1SCT() * (1 + prop.getCurToleranceFrac())));
        gridbag.setConstraints(labelMaxMEBT1SCT, constraints);
        mainPanel.add(labelMaxMEBT1SCT);
        constraints.gridx = 25;
        constraints.gridy = 7;
        constraints.gridwidth = 3;
        constraints.gridheight = 1;
        labelMinMEBT1SCT = new JLabel(df.format(refprop.getRefMEBT1SCT() * (1 - prop.getCurToleranceFrac())));
        gridbag.setConstraints(labelMinMEBT1SCT, constraints);
        mainPanel.add(labelMinMEBT1SCT);
        constraints.gridx = 10;
        constraints.gridy = 6;
        constraints.gridwidth = 5;
        constraints.gridheight = 2;
        gridbag.setConstraints(rbMEBT1SCT, constraints);
        mainPanel.add(rbMEBT1SCT);
        constraints.gridx = 27;
        constraints.gridy = 8;
        constraints.gridwidth = 5;
        constraints.gridheight = 2;
        gridbag.setConstraints(buttonSaveRef, constraints);
        mainPanel.add(buttonSaveRef);
        constraints.gridx = 10;
        constraints.gridy = 8;
        constraints.gridwidth = 5;
        constraints.gridheight = 2;
        JLabel titleRefDate = new JLabel("Last saved: ");
        gridbag.setConstraints(titleRefDate, constraints);
        mainPanel.add(titleRefDate);
        constraints.gridx = 15;
        constraints.gridy = 8;
        constraints.gridwidth = 10;
        constraints.gridheight = 2;
        labelRefDate = new JLabel(sdf.format(refprop.getRefDate()));
        gridbag.setConstraints(labelRefDate, constraints);
        mainPanel.add(labelRefDate);
        this.pack();
        this.setVisible(true);
    }

    public static void main(String[] args) {
        IsTunerFrame frame = new IsTunerFrame();
        frame.setTitle("Ion Source Filament Control");
    }
}
