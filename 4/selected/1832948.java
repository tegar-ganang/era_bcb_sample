package wand.channelControl;

import java.awt.Color;
import javax.swing.JCheckBox;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import wand.ChannelFrame;
import wand.genericChannel.*;

public class ChannelOutTriggerPanel extends JPanel {

    public JButton arm, punch;

    private int channelID;

    private Insets inset = new Insets(1, 1, 1, 1);

    public javax.swing.JSlider transparencySlider;

    private float alpha;

    private JCheckBox opaqueCheck;

    private GenericChannel channel;

    public void setChannelID(int chID) {
        channelID = chID;
        channel = ChannelFrame.channelGridPanel.channels[channelID];
        setOpaqueCheck(channel.getChannelDisplay().isOpaque());
        transparencySlider.setValue((int) (channel.getChannelDisplay().getAlpha() * 100));
    }

    public int getChannelID() {
        return channelID;
    }

    public ChannelOutTriggerPanel() {
        setBackground(Color.white);
        makeButtons();
        setLayout(new GridLayout(2, 2));
        add(transparencySlider);
        add(arm);
        add(opaqueCheck);
        add(punch);
        transparencySlider.setBackground(Color.lightGray);
    }

    public void setOpaqueCheck(boolean state) {
        opaqueCheck.setSelected(!state);
        opaqueCheck.doClick();
    }

    private void makeButtons() {
        arm = new JButton("ARM");
        class ButtonListener1 implements ActionListener {

            public void actionPerformed(ActionEvent event) {
                arm();
            }
        }
        arm.addActionListener(new ButtonListener1());
        punch = new JButton("PUNCH");
        class ButtonListener2 implements ActionListener {

            public void actionPerformed(ActionEvent event) {
                punch();
            }
        }
        punch.addActionListener(new ButtonListener2());
        arm.setMargin(inset);
        punch.setMargin(inset);
        transparencySlider = new javax.swing.JSlider();
        transparencySlider.setMaximum(100);
        transparencySlider.setMinimum(0);
        transparencySlider.setValue(100);
        transparencySlider.setOrientation(javax.swing.JSlider.VERTICAL);
        transparencySlider.setToolTipText("transparency");
        transparencySlider.setDoubleBuffered(true);
        transparencySlider.addChangeListener(new javax.swing.event.ChangeListener() {

            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                transparencySliderStateChanged(evt);
                if (transparencySlider.getValue() == transparencySlider.getMinimum() || transparencySlider.getValue() == transparencySlider.getMaximum()) transparencySlider.setBackground(Color.lightGray); else transparencySlider.setBackground(Color.red);
                restoreFocus();
            }
        });
        opaqueCheck = new JCheckBox("BG fill", false);
        opaqueCheck.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                opaqueCheckActionPerformed(evt);
            }
        });
    }

    private void arm() {
        ChannelFrame.setOutputChannel(channelID, false);
    }

    private void punch() {
        ChannelFrame.setOutputChannel(channelID, true);
    }

    private void transparencySliderStateChanged(javax.swing.event.ChangeEvent evt) {
        alpha = transparencySlider.getValue() * 1.0f / transparencySlider.getMaximum();
        ChannelFrame.channelGridPanel.channels[channelID].setAlpha(alpha);
    }

    private void opaqueCheckActionPerformed(java.awt.event.ActionEvent evt) {
        ChannelFrame.channelGridPanel.channels[channelID].setOpaque(opaqueCheck.isSelected());
        ChannelFrame.channelGridPanel.channels[channelID].refreshAlpha(false);
    }

    private void restoreFocus() {
        ChannelFrame.enginePanel.requestFocus();
    }
}
