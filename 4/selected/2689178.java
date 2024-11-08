package org.personalsmartspace.lm.dianne.impl.analysis;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class AnalyserGUI extends JFrame implements ActionListener {

    private NetworkEmulator netem;

    private static final long serialVersionUID = 1L;

    private JPanel contentPane;

    private JPanel preferencePane;

    private JPanel contextPane;

    private JPanel channelGraphPane;

    private JPanel channelRulePane;

    private JPanel soundGraphPane;

    private JPanel soundRulePane;

    private JTextArea channelRule;

    private JTextArea soundRule;

    private JRadioButton screen1;

    private JRadioButton screen2;

    private JRadioButton screen3;

    private ChannelCanvas channelCanvas;

    private SoundCanvas soundCanvas;

    public AnalyserGUI(NetworkEmulator netem) {
        this.netem = netem;
        this.setSize(800, 800);
        this.setTitle("DIANNE Analyser");
        this.setContentPane(getMyContentPane());
        this.setVisible(true);
    }

    private JPanel getMyContentPane() {
        if (contentPane == null) {
            contentPane = new JPanel();
            contentPane.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridheight = 6;
            c.gridwidth = 4;
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1;
            c.weighty = 1;
            c.insets = new Insets(10, 10, 10, 10);
            contentPane.add(getPreferencePane(), c);
            c.gridx = 0;
            c.gridy = 6;
            c.gridheight = 2;
            c.gridwidth = 4;
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1;
            c.weighty = 0.2;
            contentPane.add(getContextPane(), c);
        }
        return contentPane;
    }

    private JPanel getPreferencePane() {
        if (preferencePane == null) {
            preferencePane = new JPanel();
            preferencePane.setLayout(new GridBagLayout());
            preferencePane.setBorder(BorderFactory.createTitledBorder("Preferences"));
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridheight = 4;
            c.gridwidth = 2;
            c.weightx = 1;
            c.weighty = 1;
            c.insets = new Insets(5, 5, 5, 5);
            c.fill = GridBagConstraints.BOTH;
            preferencePane.add(getChannelGraphPane(), c);
            c.gridx = 0;
            c.gridy = 4;
            c.gridheight = 2;
            c.gridwidth = 2;
            c.weightx = 0;
            c.weighty = 0.4;
            preferencePane.add(getChannelRulePane(), c);
        }
        return preferencePane;
    }

    private JPanel getContextPane() {
        if (contextPane == null) {
            contextPane = new JPanel();
            contextPane.setLayout(new GridBagLayout());
            contextPane.setBorder(BorderFactory.createTitledBorder("Context"));
            JLabel groupName = new JLabel("Location:");
            screen1 = new JRadioButton("Screen 1");
            screen1.setActionCommand("corridor");
            screen1.addActionListener(this);
            screen2 = new JRadioButton("Screen 2");
            screen2.setActionCommand("learningZone");
            screen2.addActionListener(this);
            screen3 = new JRadioButton("Screen 3");
            screen3.setActionCommand("lab");
            screen3.addActionListener(this);
            ButtonGroup locations = new ButtonGroup();
            locations.add(screen1);
            locations.add(screen2);
            locations.add(screen3);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridheight = 1;
            c.gridwidth = 1;
            c.weightx = 1;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            contextPane.add(groupName, c);
            c.gridx = 1;
            c.weighty = 0;
            contextPane.add(screen1, c);
            c.gridx = 2;
            contextPane.add(screen2, c);
            c.gridx = 3;
            contextPane.add(screen3, c);
        }
        return contextPane;
    }

    /***************************************************************************
	 * 
	 * Preference Pane panels
	 * 
	 ***************************************************************************/
    private JPanel getChannelGraphPane() {
        if (channelGraphPane == null) {
            channelGraphPane = new JPanel();
            channelGraphPane.setLayout(new GridBagLayout());
            channelGraphPane.setBackground(Color.LIGHT_GRAY);
            JLabel prefName = new JLabel("Channel");
            channelCanvas = new ChannelCanvas(netem);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridheight = 1;
            c.gridwidth = 2;
            c.weightx = 0;
            c.weighty = 0;
            c.fill = GridBagConstraints.BOTH;
            channelGraphPane.add(prefName, c);
            c.gridx = 0;
            c.gridy = 1;
            c.gridheight = 4;
            c.gridwidth = 2;
            c.weightx = 1;
            c.weighty = 1;
            channelGraphPane.add(channelCanvas, c);
        }
        return channelGraphPane;
    }

    private JPanel getChannelRulePane() {
        if (channelRulePane == null) {
            channelRulePane = new JPanel();
            channelRulePane.setLayout(new GridBagLayout());
            channelRulePane.setBackground(Color.WHITE);
            channelRule = new JTextArea(10, 100);
            channelRule.setEditable(false);
            channelRule.setText("No context selected");
            JScrollPane scroll = new JScrollPane(channelRule);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridheight = 2;
            c.gridwidth = 2;
            c.weightx = 1;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            channelRulePane.add(scroll, c);
        }
        return channelRulePane;
    }

    private JPanel getSoundGraphPane() {
        if (soundGraphPane == null) {
            soundGraphPane = new JPanel();
            soundGraphPane.setLayout(new GridBagLayout());
            soundGraphPane.setBackground(Color.LIGHT_GRAY);
            JLabel prefName = new JLabel("Sound");
            soundCanvas = new SoundCanvas(netem);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridheight = 1;
            c.gridwidth = 2;
            c.weightx = 0;
            c.weighty = 0;
            c.fill = GridBagConstraints.BOTH;
            soundGraphPane.add(prefName, c);
            c.gridx = 0;
            c.gridy = 1;
            c.gridheight = 4;
            c.gridwidth = 2;
            c.weightx = 1;
            c.weighty = 1;
            soundGraphPane.add(soundCanvas, c);
        }
        return soundGraphPane;
    }

    private JPanel getSoundRulePane() {
        if (soundRulePane == null) {
            soundRulePane = new JPanel();
            soundRulePane.setLayout(new GridBagLayout());
            soundRulePane.setBackground(Color.WHITE);
            soundRule = new JTextArea(10, 100);
            soundRule.setEditable(false);
            soundRule.setText("No context selected");
            JScrollPane scroll = new JScrollPane(soundRule);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            c.gridheight = 2;
            c.gridwidth = 2;
            c.weightx = 1;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            soundRulePane.add(scroll, c);
        }
        return soundRulePane;
    }

    /***************************************************************************
	 * 
	 * Action Listener methods
	 * 
	 ***************************************************************************/
    @Override
    public void actionPerformed(ActionEvent e) {
        String ctxNodeName = null;
        if (e.getSource().equals(screen1)) {
            ctxNodeName = e.getActionCommand();
        } else if (e.getSource().equals(screen2)) {
            ctxNodeName = e.getActionCommand();
        } else if (e.getSource().equals(screen3)) {
            ctxNodeName = e.getActionCommand();
        }
        if (ctxNodeName != null) {
            netem.setActiveCtxNode(ctxNodeName);
            channelCanvas.repaint();
            channelRule.setText(netem.getRule("channel"));
        }
    }
}
