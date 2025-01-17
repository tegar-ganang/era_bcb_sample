package net.sf.fmj.ui.wizards;

import javax.media.format.*;

/**
 * 
 * @author Ken Larson
 */
public class AudioFormatPanel extends javax.swing.JPanel {

    private javax.swing.ButtonGroup buttonGroupAudioBitsPerSample;

    private javax.swing.ButtonGroup buttonGroupAudioChannels;

    private javax.swing.ButtonGroup buttonGroupAudioEndian;

    private javax.swing.JCheckBox checkBoxAudioSigned;

    private javax.swing.JComboBox comboAudioEncoding;

    private javax.swing.JComboBox comboAudioSampleRate;

    private javax.swing.JLabel labelAudioBitsPerSample;

    private javax.swing.JLabel labelAudioEncoding;

    private javax.swing.JLabel labelAudioEndian;

    private javax.swing.JLabel labelAudioSampleRate;

    private javax.swing.JLabel labelChannels;

    private javax.swing.JRadioButton radioAudioBitsPerSample16;

    private javax.swing.JRadioButton radioAudioBitsPerSample8;

    private javax.swing.JRadioButton radioAudioChannelsMono;

    private javax.swing.JRadioButton radioAudioChannelsStereo;

    private javax.swing.JRadioButton radioAudioEndianBig;

    private javax.swing.JRadioButton radioAudioEndianLittle;

    /** Creates new form AudioFormtPanel */
    public AudioFormatPanel() {
        initComponents();
    }

    public AudioFormat getAudioFormat() {
        final String encoding = (String) comboAudioEncoding.getSelectedItem();
        final double sampleRate = Integer.parseInt((String) comboAudioSampleRate.getSelectedItem());
        final int sampleSizeInBits;
        if (radioAudioBitsPerSample8.isSelected()) sampleSizeInBits = 8; else if (radioAudioBitsPerSample16.isSelected()) sampleSizeInBits = 16; else throw new RuntimeException();
        final int channels;
        if (radioAudioChannelsMono.isSelected()) channels = 1; else if (radioAudioChannelsStereo.isSelected()) channels = 2; else throw new RuntimeException();
        final int endian;
        if (sampleSizeInBits <= 8) endian = -1; else if (radioAudioEndianLittle.isSelected()) endian = AudioFormat.LITTLE_ENDIAN; else if (radioAudioEndianBig.isSelected()) endian = AudioFormat.BIG_ENDIAN; else throw new RuntimeException();
        final int signed = checkBoxAudioSigned.isSelected() ? AudioFormat.SIGNED : AudioFormat.UNSIGNED;
        return new AudioFormat(encoding, sampleRate, sampleSizeInBits, channels, endian, signed);
    }

    public javax.swing.JComboBox getComboAudioEncoding() {
        return comboAudioEncoding;
    }

    public javax.swing.JComboBox getComboAudioSampleRate() {
        return comboAudioSampleRate;
    }

    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;
        buttonGroupAudioBitsPerSample = new javax.swing.ButtonGroup();
        buttonGroupAudioChannels = new javax.swing.ButtonGroup();
        buttonGroupAudioEndian = new javax.swing.ButtonGroup();
        labelAudioEncoding = new javax.swing.JLabel();
        comboAudioSampleRate = new javax.swing.JComboBox();
        comboAudioEncoding = new javax.swing.JComboBox();
        labelAudioSampleRate = new javax.swing.JLabel();
        labelAudioBitsPerSample = new javax.swing.JLabel();
        radioAudioBitsPerSample8 = new javax.swing.JRadioButton();
        radioAudioBitsPerSample16 = new javax.swing.JRadioButton();
        radioAudioChannelsStereo = new javax.swing.JRadioButton();
        radioAudioChannelsMono = new javax.swing.JRadioButton();
        labelChannels = new javax.swing.JLabel();
        labelAudioEndian = new javax.swing.JLabel();
        radioAudioEndianBig = new javax.swing.JRadioButton();
        radioAudioEndianLittle = new javax.swing.JRadioButton();
        checkBoxAudioSigned = new javax.swing.JCheckBox();
        setLayout(new java.awt.GridBagLayout());
        labelAudioEncoding.setText("Encoding:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(labelAudioEncoding, gridBagConstraints);
        comboAudioSampleRate.setModel(new javax.swing.DefaultComboBoxModel(new String[] {}));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(comboAudioSampleRate, gridBagConstraints);
        comboAudioEncoding.setModel(new javax.swing.DefaultComboBoxModel(new String[] {}));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(comboAudioEncoding, gridBagConstraints);
        labelAudioSampleRate.setText("Sample rate (Hz):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(labelAudioSampleRate, gridBagConstraints);
        labelAudioBitsPerSample.setText("Bits per sample:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(labelAudioBitsPerSample, gridBagConstraints);
        buttonGroupAudioBitsPerSample.add(radioAudioBitsPerSample8);
        radioAudioBitsPerSample8.setText("8");
        radioAudioBitsPerSample8.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        radioAudioBitsPerSample8.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(radioAudioBitsPerSample8, gridBagConstraints);
        buttonGroupAudioBitsPerSample.add(radioAudioBitsPerSample16);
        radioAudioBitsPerSample16.setText("16");
        radioAudioBitsPerSample16.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        radioAudioBitsPerSample16.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(radioAudioBitsPerSample16, gridBagConstraints);
        buttonGroupAudioChannels.add(radioAudioChannelsStereo);
        radioAudioChannelsStereo.setText("Stereo");
        radioAudioChannelsStereo.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        radioAudioChannelsStereo.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(radioAudioChannelsStereo, gridBagConstraints);
        buttonGroupAudioChannels.add(radioAudioChannelsMono);
        radioAudioChannelsMono.setText("Mono");
        radioAudioChannelsMono.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        radioAudioChannelsMono.setMargin(new java.awt.Insets(0, 0, 0, 0));
        radioAudioChannelsMono.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                radioAudioChannelsMonoActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(radioAudioChannelsMono, gridBagConstraints);
        labelChannels.setText("Channels:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(labelChannels, gridBagConstraints);
        labelAudioEndian.setText("Endian:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(labelAudioEndian, gridBagConstraints);
        buttonGroupAudioEndian.add(radioAudioEndianBig);
        radioAudioEndianBig.setText("Big");
        radioAudioEndianBig.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        radioAudioEndianBig.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(radioAudioEndianBig, gridBagConstraints);
        buttonGroupAudioEndian.add(radioAudioEndianLittle);
        radioAudioEndianLittle.setText("Little");
        radioAudioEndianLittle.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        radioAudioEndianLittle.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(radioAudioEndianLittle, gridBagConstraints);
        checkBoxAudioSigned.setText("Signed");
        checkBoxAudioSigned.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        checkBoxAudioSigned.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(4, 4, 4, 4);
        add(checkBoxAudioSigned, gridBagConstraints);
    }

    private void radioAudioChannelsMonoActionPerformed(java.awt.event.ActionEvent evt) {
    }

    public void setAudioFormat(AudioFormat f) {
        comboAudioEncoding.setSelectedItem(f.getEncoding());
        comboAudioSampleRate.setEnabled(false);
        radioAudioChannelsMono.setEnabled(false);
        radioAudioChannelsStereo.setEnabled(false);
        radioAudioEndianLittle.setEnabled(false);
        radioAudioEndianBig.setEnabled(false);
        radioAudioBitsPerSample8.setEnabled(false);
        radioAudioBitsPerSample16.setEnabled(false);
        checkBoxAudioSigned.setEnabled(false);
        comboAudioSampleRate.setSelectedItem("" + (int) f.getSampleRate());
        if (f.getChannels() == 1) radioAudioChannelsMono.setSelected(true); else if (f.getChannels() == 2) radioAudioChannelsStereo.setSelected(true); else throw new IllegalArgumentException();
        if (f.getEndian() == AudioFormat.LITTLE_ENDIAN) radioAudioEndianLittle.setSelected(true); else if (f.getEndian() == AudioFormat.BIG_ENDIAN) radioAudioEndianBig.setSelected(true); else {
            if (f.getSampleSizeInBits() > 8) throw new IllegalArgumentException("Unknown or unspecified endian: " + f.getEndian() + " format: " + f);
            radioAudioEndianLittle.setSelected(false);
            radioAudioEndianBig.setSelected(false);
        }
        if (f.getSampleSizeInBits() == 8) radioAudioBitsPerSample8.setSelected(true); else if (f.getSampleSizeInBits() == 16) radioAudioBitsPerSample16.setSelected(true); else throw new IllegalArgumentException();
        if (f.getSigned() == AudioFormat.SIGNED) checkBoxAudioSigned.setSelected(true); else if (f.getSigned() == AudioFormat.UNSIGNED) checkBoxAudioSigned.setSelected(false); else throw new IllegalArgumentException();
    }
}
