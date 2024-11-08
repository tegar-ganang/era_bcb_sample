package org.monome.pages;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import org.w3c.dom.Element;

public class PageChangeConfigurationPage implements Page, ActionListener {

    /**
	 * The MonomeConfiguration that this page belongs to
	 */
    MonomeConfiguration monome;

    /**
	 * The index of this page (the page number) 
	 */
    int index;

    private JButton saveBtn;

    private int lastMIDIChannel = 0;

    private int lastMIDINote = 0;

    private ArrayList<JTextField> midiChannels;

    private ArrayList<JTextField> midiNotes;

    private JLabel lastMsgLbl;

    private JPanel panel;

    private JPanel gridPanel;

    public PageChangeConfigurationPage(MonomeConfiguration monome, int index) {
        this.monome = monome;
        this.index = index;
    }

    public void actionPerformed(ActionEvent e) {
        System.out.println(e.getActionCommand());
        Object source = e.getSource();
        if (source instanceof JCheckBox) {
            if (e.getActionCommand().equals("Enable Page Change Button")) {
                JCheckBox checkBox = (JCheckBox) source;
                this.monome.usePageChangeButton = checkBox.isSelected();
            }
            if (e.getActionCommand().equals("Enable MIDI Page Changing")) {
                JCheckBox checkBox = (JCheckBox) source;
                this.monome.useMIDIPageChanging = checkBox.isSelected();
            }
        }
        if (e.getActionCommand().equals("Click to save and exit config mode.")) {
            this.monome.midiPageChangeRules = new ArrayList<MIDIPageChangeRule>();
            for (int i = 0; i < this.midiChannels.size(); i++) {
                int channel = Integer.parseInt(this.midiChannels.get(i).getText());
                int note = Integer.parseInt(this.midiNotes.get(i).getText());
                MIDIPageChangeRule mpcr = new MIDIPageChangeRule(note, channel, i);
                this.monome.midiPageChangeRules.add(mpcr);
            }
            this.monome.pageChangeConfigMode = false;
            this.monome.deletePageX(this.index);
        }
    }

    public void addMidiOutDevice(String deviceName) {
    }

    public void clearPanel() {
    }

    public void configure(Element pageElement) {
    }

    public void destroyPage() {
    }

    public ADCOptions getAdcOptions() {
        return null;
    }

    public boolean getCacheDisabled() {
        return false;
    }

    public String getName() {
        return null;
    }

    public JPanel getPanel() {
        if (panel != null) {
            if (lastMsgLbl != null) {
                gridPanel.remove(lastMsgLbl);
            }
            lastMsgLbl = new JLabel("Last MIDI Message: Channel " + this.lastMIDIChannel + ", Note: " + this.lastMIDINote);
            gridPanel.add(lastMsgLbl);
            return panel;
        }
        this.midiChannels = new ArrayList<JTextField>();
        this.midiNotes = new ArrayList<JTextField>();
        panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1));
        gridPanel = new JPanel();
        gridPanel.setLayout(new GridLayout(0, 1));
        JCheckBox checkBox = new JCheckBox();
        checkBox.setText("Enable Page Change Button");
        checkBox.setName("PageChangeButton");
        checkBox.setSelected(this.monome.usePageChangeButton);
        checkBox.addActionListener(this);
        gridPanel.add(checkBox);
        checkBox = new JCheckBox();
        checkBox.setText("Enable MIDI Page Changing");
        checkBox.setName("MIDIPageChange");
        checkBox.setSelected(this.monome.useMIDIPageChanging);
        checkBox.addActionListener(this);
        gridPanel.add(checkBox);
        panel.add(gridPanel);
        JPanel pagePanel = new JPanel();
        pagePanel.setLayout(new BoxLayout(pagePanel, BoxLayout.PAGE_AXIS));
        for (int i = 0; i < this.monome.pages.size() - 1; i++) {
            int note = 0;
            int channel = 0;
            if (this.monome.midiPageChangeRules.size() > i) {
                MIDIPageChangeRule mpcr = this.monome.midiPageChangeRules.get(i);
                note = mpcr.getNote();
                channel = mpcr.getChannel();
            }
            JPanel subPanel = new JPanel();
            subPanel.setLayout(new GridLayout(1, 1));
            Page page = this.monome.pages.get(i);
            String pageName = page.getName();
            JLabel label = new JLabel(pageName, JLabel.LEFT);
            subPanel.add(label);
            label = new JLabel("MIDI Channel", JLabel.RIGHT);
            subPanel.add(label);
            JTextField tf = new JTextField();
            tf.setName("" + i);
            tf.setText("" + channel);
            this.midiChannels.add(i, tf);
            subPanel.add(tf);
            label = new JLabel("MIDI Note", JLabel.RIGHT);
            subPanel.add(label);
            label.setAlignmentX(Component.RIGHT_ALIGNMENT);
            tf = new JTextField();
            tf.setName("" + i);
            tf.setText("" + note);
            this.midiNotes.add(i, tf);
            subPanel.add(tf);
            gridPanel.add(subPanel);
        }
        saveBtn = new JButton("Click to save and exit config mode.");
        gridPanel.add(saveBtn);
        this.saveBtn.addActionListener(this);
        panel.add(gridPanel);
        return panel;
    }

    public void handleADC(int adcNum, float value) {
    }

    public void handleADC(float x, float y) {
    }

    public void handlePress(int x, int y, int value) {
    }

    public void handleReset() {
    }

    public void handleTick() {
    }

    public boolean isTiltPage() {
        return false;
    }

    public void redrawMonome() {
    }

    public void send(MidiMessage message, long timeStamp) {
        if (message instanceof ShortMessage) {
            ShortMessage msg = (ShortMessage) message;
            int velocity = msg.getData2();
            if (msg.getCommand() == ShortMessage.NOTE_ON && velocity != 0) {
                int channel = msg.getChannel();
                lastMIDIChannel = channel;
                int note = msg.getData1();
                lastMIDINote = note;
                this.monome.redrawPanel();
            }
        }
    }

    public void setAdcOptions(ADCOptions options) {
    }

    public void setIndex(int index) {
    }

    public void setName(String name) {
    }

    public String toXml() {
        return null;
    }
}
