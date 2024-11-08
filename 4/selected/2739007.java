package wand.channelControl;

import java.awt.GridLayout;
import javax.swing.JLabel;
import wand.*;

public class ClipParametersPanel extends javax.swing.JPanel {

    private javax.swing.ButtonGroup buttonGroup1;

    private javax.swing.JTextField datumField, initDelayIndexField;

    private javax.swing.JLabel datumjLabel;

    private javax.swing.JRadioButton discreteRB;

    private javax.swing.JRadioButton evenRB;

    private javax.swing.JCheckBox recoilCheck;

    private javax.swing.JButton saveButton;

    public javax.swing.JCheckBox tightCheck;

    private int channelID;

    public void setChannelID(int chID) {
        channelID = chID;
        loadParameters();
    }

    public int getChannelID() {
        return channelID;
    }

    /** Creates new form ClipParamtersPanel */
    public ClipParametersPanel() {
        initComponents();
        setLayout(new GridLayout(4, 2));
        add(datumjLabel);
        add(datumField);
        add(recoilCheck);
        add(initDelayIndexField);
        add(discreteRB);
        add(evenRB);
        add(tightCheck);
        add(saveButton);
    }

    public void loadParameters() {
        datumField.setText(String.valueOf(ChannelFrame.channelGridPanel.channels[channelID].getDatumShift()));
        recoilCheck.setSelected(ChannelFrame.channelGridPanel.channels[channelID].getRecoil());
        tightCheck.setSelected(ChannelFrame.channelGridPanel.channels[channelID].getTightBPC());
        if (ChannelFrame.channelGridPanel.channels[channelID].getStretchMode().equals("discrete")) discreteRB.setSelected(true);
        if (ChannelFrame.channelGridPanel.channels[channelID].getStretchMode().equals("even")) evenRB.setSelected(true);
        initDelayIndexField.setText(String.valueOf(ChannelFrame.channelGridPanel.channels[channelID].getInitDelayIndex()));
    }

    private void initComponents() {
        buttonGroup1 = new javax.swing.ButtonGroup();
        datumjLabel = new javax.swing.JLabel();
        datumField = new javax.swing.JTextField();
        initDelayIndexField = new javax.swing.JTextField();
        recoilCheck = new javax.swing.JCheckBox();
        discreteRB = new javax.swing.JRadioButton();
        evenRB = new javax.swing.JRadioButton();
        tightCheck = new javax.swing.JCheckBox();
        saveButton = new javax.swing.JButton();
        datumjLabel.setText("Datum");
        datumField.setColumns(3);
        datumField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        datumField.setText("0");
        datumField.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                datumFieldActionPerformed(evt);
            }
        });
        initDelayIndexField.setColumns(3);
        initDelayIndexField.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
        initDelayIndexField.setText("?");
        initDelayIndexField.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                initDelayIndexFieldActionPerformed(evt);
            }
        });
        recoilCheck.setText("Recoil   InitD:");
        discreteRB.setFont(new java.awt.Font("Tahoma", 0, 9));
        recoilCheck.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                recoilCheckActionPerformed(evt);
            }
        });
        buttonGroup1.add(discreteRB);
        discreteRB.setSelected(true);
        discreteRB.setText("Discrete");
        discreteRB.setFont(new java.awt.Font("Tahoma", 0, 9));
        discreteRB.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                discreteRBActionPerformed(evt);
            }
        });
        buttonGroup1.add(evenRB);
        evenRB.setText("Even");
        evenRB.setFont(new java.awt.Font("Tahoma", 0, 9));
        evenRB.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                evenRBActionPerformed(evt);
            }
        });
        tightCheck.setText("TightBPC");
        tightCheck.setFont(new java.awt.Font("Tahoma", 0, 9));
        tightCheck.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tightCheckActionPerformed(evt);
            }
        });
        saveButton.setText("Save");
        saveButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });
    }

    private void recoilCheckActionPerformed(java.awt.event.ActionEvent evt) {
        ChannelFrame.channelGridPanel.channels[channelID].setRecoil(recoilCheck.isSelected());
    }

    private void tightCheckActionPerformed(java.awt.event.ActionEvent evt) {
        ChannelFrame.channelGridPanel.channels[channelID].setTightBPC(tightCheck.isSelected());
    }

    private void datumFieldActionPerformed(java.awt.event.ActionEvent evt) {
        ChannelFrame.channelGridPanel.channels[channelID].setDatumShift(Integer.parseInt(datumField.getText()));
        ChannelFrame.enginePanel.ignition.markButton.doClick();
    }

    private void initDelayIndexFieldActionPerformed(java.awt.event.ActionEvent evt) {
    }

    private void discreteRBActionPerformed(java.awt.event.ActionEvent evt) {
        ChannelFrame.channelGridPanel.channels[channelID].setStretchMode("discrete");
    }

    private void evenRBActionPerformed(java.awt.event.ActionEvent evt) {
        ChannelFrame.channelGridPanel.channels[channelID].setStretchMode("even");
    }

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {
        ChannelFrame.channelGridPanel.channels[channelID].setInitDelayIndex(Integer.parseInt(initDelayIndexField.getText()));
        ChannelFrame.channelGridPanel.channels[channelID].saveParametersFile();
    }
}
