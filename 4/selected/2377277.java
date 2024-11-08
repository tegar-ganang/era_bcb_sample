package gov.sns.tools.scan.SecondEdition;

import javax.swing.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.awt.Font;
import java.awt.Color;
import java.awt.BorderLayout;
import gov.sns.ca.*;
import gov.sns.tools.swing.*;
import gov.sns.xal.smf.TimingCenter;

public class BeamTrigger {

    private String triggerNamePV = "ICS_Tim:Gate_BeamOn:SSTrigger";

    private Channel ch = null;

    private JRadioButton useTriggerButton = new JRadioButton(" Use Beam Trigger, Delay [sec]:");

    private DoubleInputTextField tDelayText = new DoubleInputTextField(5);

    private DecimalFormat tDelayFormat = new DecimalFormat("0.0#");

    public BeamTrigger() {
        tDelayText.setHorizontalAlignment(JTextField.CENTER);
        tDelayText.setNormalBackground(Color.white);
        tDelayText.setDecimalFormat(tDelayFormat);
        tDelayText.setValue(0.2);
        TimingCenter tmCenter = TimingCenter.getDefaultTimingCenter();
        ch = tmCenter.getChannel(TimingCenter.TRIGGER_HANDLE);
        useTriggerButton.setSelected(true);
        useTriggerButton.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    setOnOff(true);
                } else {
                    setOnOff(false);
                }
            }
        });
    }

    public void setOnOff(boolean onOff) {
        tDelayText.setEditable(onOff);
        useTriggerButton.setSelected(onOff);
    }

    public void setFontForAll(Font fnt) {
        tDelayText.setFont(fnt);
        useTriggerButton.setFont(fnt);
    }

    public JPanel getJPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(useTriggerButton, BorderLayout.CENTER);
        panel.add(tDelayText, BorderLayout.EAST);
        return panel;
    }

    public boolean isOn() {
        return useTriggerButton.isSelected();
    }

    public void makePulse() {
        if (useTriggerButton.isSelected() && ch != null) {
            try {
                ch.putVal(1.0);
            } catch (ConnectionException e) {
                setOnOff(false);
                return;
            } catch (PutException e) {
                setOnOff(false);
                return;
            }
            try {
                Thread.sleep((long) (tDelayText.getValue() * 1000.0));
            } catch (InterruptedException e) {
            }
        }
    }

    public String getChannelName() {
        return triggerNamePV;
    }

    public Channel getChannel() {
        return ch;
    }

    public void setChannelName(String chanName) {
        triggerNamePV = chanName;
        ch = ChannelFactory.defaultFactory().getChannel(triggerNamePV);
    }

    public void setChannel(Channel ch_In) {
        ch = ch_In;
        triggerNamePV = ch.channelName();
    }

    public void setDelay(double timeDelay) {
        tDelayText.setValue(timeDelay);
    }

    public double getDelay() {
        return tDelayText.getValue();
    }
}
