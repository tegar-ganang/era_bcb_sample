package jrackattack.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jonkoshare.util.VersionInformation;
import jrackattack.midi.MidiThread;
import jrackattack.midi.RackAttack;
import jrackattack.midi.SoundParameter;

/**
 * @author  methke01
 */
@VersionInformation(lastChanged = "$LastChangedDate: 2009-09-15 15:15:46 -0400 (Tue, 15 Sep 2009) $", authors = { "Alexander Methke" }, revision = "$LastChangedRevision: 15 $", lastEditor = "$LastChangedBy: onkobu $", id = "$Id")
public class ChannelVolumePanel extends JPanel {

    /** Creates new form ChannelVolumePanel */
    public ChannelVolumePanel() {
        initComponents();
        initValues();
    }

    private void initComponents() {
        GridBagConstraints gridBagConstraints;
        buttonGroup1 = new ButtonGroup();
        volumeSlider = new JSlider();
        channelLabel = new JLabel();
        panKnob = new JKnob();
        crackKnob = new JKnob();
        ringModKnob = new JKnob();
        osc2Knob = new JKnob();
        osc1Knob = new JKnob();
        fxSendKnob = new JKnob();
        fxSendSpinner = new JSpinner();
        jRadioButton1 = new JRadioButton();
        jRadioButton2 = new JRadioButton();
        jRadioButton3 = new JRadioButton();
        jRadioButton4 = new JRadioButton();
        jRadioButton5 = new JRadioButton();
        jRadioButton6 = new JRadioButton();
        jRadioButton7 = new JRadioButton();
        jRadioButton8 = new JRadioButton();
        jRadioButton9 = new JRadioButton();
        setLayout(new GridBagLayout());
        volumeSlider.setMajorTickSpacing(20);
        volumeSlider.setMaximum(127);
        volumeSlider.setMinorTickSpacing(5);
        volumeSlider.setOrientation(JSlider.VERTICAL);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setValue(100);
        volumeSlider.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent evt) {
                volumeSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridheight = 9;
        gridBagConstraints.fill = GridBagConstraints.VERTICAL;
        gridBagConstraints.weighty = 0.8;
        add(volumeSlider, gridBagConstraints);
        channelLabel.setFont(new Font("Courier", 1, 18));
        channelLabel.setText("jLabel1");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 16;
        gridBagConstraints.gridwidth = 2;
        add(channelLabel, gridBagConstraints);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("jrackattack/gui");
        panKnob.setToolTipText(bundle.getString("tooltip.pan"));
        panKnob.setMaximumValue(127);
        panKnob.setPaintMarkers(true);
        panKnob.setPreferredSize(new Dimension(60, 60));
        panKnob.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent evt) {
                panKnobStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.1;
        add(panKnob, gridBagConstraints);
        crackKnob.setToolTipText(bundle.getString("tooltip.crack_mix"));
        crackKnob.setMaximumValue(127);
        crackKnob.setPaintMarkers(true);
        crackKnob.setPreferredSize(new Dimension(60, 60));
        crackKnob.setValue(0.0);
        crackKnob.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent evt) {
                crackKnobStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.1;
        add(crackKnob, gridBagConstraints);
        ringModKnob.setToolTipText(bundle.getString("tooltip.ringmod"));
        ringModKnob.setMaximumValue(127);
        ringModKnob.setPaintMarkers(true);
        ringModKnob.setPreferredSize(new Dimension(60, 60));
        ringModKnob.setValue(0.0);
        ringModKnob.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent evt) {
                ringModKnobStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.1;
        add(ringModKnob, gridBagConstraints);
        osc2Knob.setToolTipText(bundle.getString("tooltip.osc2"));
        osc2Knob.setMaximumValue(127);
        osc2Knob.setPaintMarkers(true);
        osc2Knob.setPreferredSize(new Dimension(60, 60));
        osc2Knob.setValue(0.0);
        osc2Knob.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent evt) {
                osc2KnobStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.1;
        add(osc2Knob, gridBagConstraints);
        osc1Knob.setToolTipText(bundle.getString("tooltip.osc1"));
        osc1Knob.setMaximumValue(127);
        osc1Knob.setPaintMarkers(true);
        osc1Knob.setPreferredSize(new Dimension(60, 60));
        osc1Knob.setValue(0.0);
        osc1Knob.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent evt) {
                osc1KnobStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.1;
        add(osc1Knob, gridBagConstraints);
        fxSendKnob.setToolTipText(bundle.getString("tooltip.fx_send"));
        fxSendKnob.setIntValue(0);
        fxSendKnob.setMaximumValue(127);
        fxSendKnob.setPaintMarkers(true);
        fxSendKnob.setPreferredSize(new Dimension(60, 60));
        fxSendKnob.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent evt) {
                fxSendKnobStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 0.1;
        add(fxSendKnob, gridBagConstraints);
        fxSendSpinner.setToolTipText(bundle.getString("tooltip.fx_send_idx"));
        fxSendSpinner.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent evt) {
                fxSendSpinnerStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        add(fxSendSpinner, gridBagConstraints);
        jRadioButton1.setText(bundle.getString("checkbox.out.1"));
        jRadioButton1.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jRadioButton1.setMargin(new Insets(0, 0, 0, 0));
        jRadioButton1.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent evt) {
                jRadioButton1ItemStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(jRadioButton1, gridBagConstraints);
        jRadioButton2.setText(bundle.getString("checkbox.out.1_2"));
        jRadioButton2.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jRadioButton2.setMargin(new Insets(0, 0, 0, 0));
        jRadioButton2.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent evt) {
                jRadioButton2ItemStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(jRadioButton2, gridBagConstraints);
        jRadioButton3.setText(bundle.getString("checkbox.out.2"));
        jRadioButton3.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jRadioButton3.setMargin(new Insets(0, 0, 0, 0));
        jRadioButton3.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent evt) {
                jRadioButton3ItemStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(jRadioButton3, gridBagConstraints);
        jRadioButton4.setText(bundle.getString("checkbox.out.3"));
        jRadioButton4.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jRadioButton4.setMargin(new Insets(0, 0, 0, 0));
        jRadioButton4.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent evt) {
                jRadioButton4ItemStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(jRadioButton4, gridBagConstraints);
        jRadioButton5.setText(bundle.getString("checkbox.out.3_4"));
        jRadioButton5.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jRadioButton5.setMargin(new Insets(0, 0, 0, 0));
        jRadioButton5.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent evt) {
                jRadioButton5ItemStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(jRadioButton5, gridBagConstraints);
        jRadioButton6.setText(bundle.getString("checkbox.out.4"));
        jRadioButton6.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jRadioButton6.setMargin(new Insets(0, 0, 0, 0));
        jRadioButton6.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent evt) {
                jRadioButton6ItemStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(jRadioButton6, gridBagConstraints);
        jRadioButton7.setText(bundle.getString("checkbox.out.5"));
        jRadioButton7.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jRadioButton7.setMargin(new Insets(0, 0, 0, 0));
        jRadioButton7.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent evt) {
                jRadioButton7ItemStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(jRadioButton7, gridBagConstraints);
        jRadioButton8.setText(bundle.getString("checkbox.out.5_6"));
        jRadioButton8.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jRadioButton8.setMargin(new Insets(0, 0, 0, 0));
        jRadioButton8.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent evt) {
                jRadioButton8ItemStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(jRadioButton8, gridBagConstraints);
        jRadioButton9.setText(bundle.getString("checkbox.out.6"));
        jRadioButton9.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jRadioButton9.setMargin(new Insets(0, 0, 0, 0));
        jRadioButton9.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent evt) {
                jRadioButton9ItemStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        add(jRadioButton9, gridBagConstraints);
    }

    private void jRadioButton9ItemStateChanged(ItemEvent evt) {
        if (getInitProvider() == null || getInitProvider().isInitiating() || evt.getStateChange() != ItemEvent.SELECTED) {
            return;
        }
        MidiThread.getInstance().emitParamChange(this, getChannelNumber(), RackAttack.DRY_OUT, 8);
    }

    private void jRadioButton8ItemStateChanged(ItemEvent evt) {
        if (getInitProvider() == null || getInitProvider().isInitiating() || evt.getStateChange() != ItemEvent.SELECTED) {
            return;
        }
        MidiThread.getInstance().emitParamChange(this, getChannelNumber(), RackAttack.DRY_OUT, 7);
    }

    private void jRadioButton7ItemStateChanged(ItemEvent evt) {
        if (getInitProvider() == null || getInitProvider().isInitiating() || evt.getStateChange() != ItemEvent.SELECTED) {
            return;
        }
        MidiThread.getInstance().emitParamChange(this, getChannelNumber(), RackAttack.DRY_OUT, 6);
    }

    private void jRadioButton6ItemStateChanged(ItemEvent evt) {
        if (getInitProvider() == null || getInitProvider().isInitiating() || evt.getStateChange() != ItemEvent.SELECTED) {
            return;
        }
        MidiThread.getInstance().emitParamChange(this, getChannelNumber(), RackAttack.DRY_OUT, 5);
    }

    private void jRadioButton5ItemStateChanged(ItemEvent evt) {
        if (getInitProvider() == null || getInitProvider().isInitiating() || evt.getStateChange() != ItemEvent.SELECTED) {
            return;
        }
        MidiThread.getInstance().emitParamChange(this, getChannelNumber(), RackAttack.DRY_OUT, 4);
    }

    private void jRadioButton4ItemStateChanged(ItemEvent evt) {
        if (getInitProvider() == null || getInitProvider().isInitiating() || evt.getStateChange() != ItemEvent.SELECTED) {
            return;
        }
        MidiThread.getInstance().emitParamChange(this, getChannelNumber(), RackAttack.DRY_OUT, 3);
    }

    private void jRadioButton3ItemStateChanged(ItemEvent evt) {
        if (getInitProvider() == null || getInitProvider().isInitiating() || evt.getStateChange() != ItemEvent.SELECTED) {
            return;
        }
        MidiThread.getInstance().emitParamChange(this, getChannelNumber(), RackAttack.DRY_OUT, 2);
    }

    private void jRadioButton2ItemStateChanged(ItemEvent evt) {
        if (getInitProvider() == null || getInitProvider().isInitiating() || evt.getStateChange() != ItemEvent.SELECTED) {
            return;
        }
        MidiThread.getInstance().emitParamChange(this, getChannelNumber(), RackAttack.DRY_OUT, 1);
    }

    private void jRadioButton1ItemStateChanged(ItemEvent evt) {
        if (getInitProvider() == null || getInitProvider().isInitiating() || evt.getStateChange() != ItemEvent.SELECTED) {
            return;
        }
        MidiThread.getInstance().emitParamChange(this, getChannelNumber(), RackAttack.DRY_OUT, 0);
    }

    private void fxSendSpinnerStateChanged(ChangeEvent evt) {
        if (getInitProvider() == null || getInitProvider().isInitiating()) {
            return;
        }
        MidiThread.getInstance().emitParamChange(this, getChannelNumber(), RackAttack.FX_ASSIGN, ((Integer) fxSendSpinner.getValue()).intValue());
    }

    private void fxSendKnobStateChanged(ChangeEvent evt) {
        if (getInitProvider() == null || getInitProvider().isInitiating()) {
            return;
        }
        MidiThread.getInstance().emitParamChange(this, getChannelNumber(), RackAttack.FX_SEND_LEVEL, fxSendKnob.getIntValue());
    }

    private void crackKnobStateChanged(ChangeEvent evt) {
        if (getInitProvider() == null || getInitProvider().isInitiating()) {
            return;
        }
        MidiThread.getInstance().emitParamChange(this, getChannelNumber(), RackAttack.CRACK_LEVEL, crackKnob.getIntValue());
    }

    private void ringModKnobStateChanged(ChangeEvent evt) {
        if (getInitProvider() == null || getInitProvider().isInitiating()) {
            return;
        }
        MidiThread.getInstance().emitParamChange(this, getChannelNumber(), RackAttack.RING_MOD_LEVEL, ringModKnob.getIntValue());
    }

    private void osc2KnobStateChanged(ChangeEvent evt) {
        if (getInitProvider() == null || getInitProvider().isInitiating()) {
            return;
        }
        MidiThread.getInstance().emitParamChange(this, getChannelNumber(), RackAttack.OSC2_LEVEL, osc2Knob.getIntValue());
    }

    private void osc1KnobStateChanged(ChangeEvent evt) {
        if (getInitProvider() == null || getInitProvider().isInitiating()) {
            return;
        }
        MidiThread.getInstance().emitParamChange(this, getChannelNumber(), RackAttack.OSC1_LEVEL, osc1Knob.getIntValue());
    }

    public void soundParameterChanged(SoundParameter sp) {
        volumeSlider.setValue(sp.getAmpVolume());
        panKnob.setIntValue(sp.getAmpPan());
        fxSendSpinner.setValue(sp.getFxIndex());
        fxSendKnob.setIntValue(sp.getFxSend());
        ringModKnob.setIntValue(sp.getRingModLevel());
        crackKnob.setIntValue(sp.getCrackLevel());
        osc1Knob.setIntValue(sp.getOsc1Level());
        osc2Knob.setIntValue(sp.getOsc2Level());
        switch(sp.getDryOutput()) {
            case 0:
                {
                    jRadioButton1.setSelected(true);
                }
                break;
            case 1:
                {
                    jRadioButton2.setSelected(true);
                }
                break;
            case 2:
                {
                    jRadioButton3.setSelected(true);
                }
                break;
            case 3:
                {
                    jRadioButton4.setSelected(true);
                }
                break;
            case 4:
                {
                    jRadioButton5.setSelected(true);
                }
                break;
            case 5:
                {
                    jRadioButton6.setSelected(true);
                }
                break;
            case 6:
                {
                    jRadioButton7.setSelected(true);
                }
                break;
            case 7:
                {
                    jRadioButton8.setSelected(true);
                }
                break;
            case 8:
                {
                    jRadioButton9.setSelected(true);
                }
        }
    }

    protected void initValues() {
        ((SpinnerNumberModel) fxSendSpinner.getModel()).setMinimum(0);
        ((SpinnerNumberModel) fxSendSpinner.getModel()).setMaximum(3);
        fxSendSpinner.setValue(new Integer(0));
        buttonGroup1.add(jRadioButton1);
        buttonGroup1.add(jRadioButton2);
        buttonGroup1.add(jRadioButton3);
        buttonGroup1.add(jRadioButton4);
        buttonGroup1.add(jRadioButton5);
        buttonGroup1.add(jRadioButton6);
        buttonGroup1.add(jRadioButton7);
        buttonGroup1.add(jRadioButton8);
        buttonGroup1.add(jRadioButton9);
    }

    private void panKnobStateChanged(ChangeEvent evt) {
        if (getInitProvider().isInitiating()) {
            return;
        }
        MidiThread.getInstance().emitPanChange(this, getChannelNumber(), panKnob.getIntValue());
    }

    private void volumeSliderStateChanged(ChangeEvent evt) {
        if (getInitProvider().isInitiating()) {
            return;
        }
        MidiThread.getInstance().emitVolumeChange(this, getChannelNumber(), volumeSlider.getValue());
    }

    /**
	 * Getter for property channelNumber.
	 * @return Value of property channelNumber.
	 */
    public int getChannelNumber() {
        return this.channelNumber;
    }

    /**
	 * Setter for property channelNumber.
	 * @param channelNumber New value of property channelNumber.
	 */
    public void setChannelNumber(int channelNumber) {
        this.channelNumber = channelNumber;
        if (channelNumber < 10) {
            channelLabel.setText("0" + (channelNumber + 1));
        } else {
            channelLabel.setText("" + (channelNumber + 1));
        }
    }

    /**
	 * Getter for property initProvider.
	 * @return Value of property initProvider.
	 */
    public InitProvider getInitProvider() {
        return this.initProvider;
    }

    /**
	 * Setter for property initProvider.
	 * @param initProvider New value of property initProvider.
	 */
    public void setInitProvider(InitProvider initProvider) {
        this.initProvider = initProvider;
    }

    private ButtonGroup buttonGroup1;

    private JLabel channelLabel;

    private JKnob crackKnob;

    private JKnob fxSendKnob;

    private JSpinner fxSendSpinner;

    private JRadioButton jRadioButton1;

    private JRadioButton jRadioButton2;

    private JRadioButton jRadioButton3;

    private JRadioButton jRadioButton4;

    private JRadioButton jRadioButton5;

    private JRadioButton jRadioButton6;

    private JRadioButton jRadioButton7;

    private JRadioButton jRadioButton8;

    private JRadioButton jRadioButton9;

    private JKnob osc1Knob;

    private JKnob osc2Knob;

    private JKnob panKnob;

    private JKnob ringModKnob;

    private JSlider volumeSlider;

    /**
	 * Holds value of property channelNumber.
	 */
    private int channelNumber;

    /**
	 * Holds value of property initProvider.
	 */
    private InitProvider initProvider;
}
