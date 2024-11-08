package org.monome.pages;

import com.cloudgarden.layout.AnchorConstraint;
import com.cloudgarden.layout.AnchorLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.apache.commons.lang.StringEscapeUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The MIDI Faders page.  Usage information is available at:
 * 
 * http://code.google.com/p/monome-pages/wiki/MIDIFadersPage
 *   
 * @author Tom Dinchak
 *
 */
public class MIDIFadersPage implements Page, ActionListener {

    /**
	 * The MonomeConfiguration that this page belongs to
	 */
    MonomeConfiguration monome;

    /**
	 * The index of this page (the page number) 
	 */
    int index;

    /**
	 * The GUI for this page
	 */
    JPanel panel;

    private JTextField ccOffsetTF;

    private JLabel ccOffsetLabel;

    private JTextField channelTF;

    private JLabel channelL;

    /**
	 * The Add MIDI Output button
	 */
    private JButton addMidiOutButton;

    /**
	 * The label for the delay setting
	 */
    private JLabel delayLabel;

    /**
	 * The delay amount per MIDI CC paramater change (in ms)
	 */
    private int delayAmount = 6;

    /**
	 * The Update Preferences button 
	 */
    private JButton updatePrefsButton;

    /**
	 * The text field that stores the delay value 
	 */
    private JTextField delayTF;

    /**
	 * monome buttons to MIDI CC values (monome height = 16, 256 only) 
	 */
    private int[] buttonValuesLarge = { 127, 118, 110, 101, 93, 84, 76, 67, 59, 50, 42, 33, 25, 16, 8, 0 };

    /**
	 * monome buttons to MIDI CC values (monome height = 8, all monome models except 256)
	 */
    private int[] buttonValuesSmall = { 127, 109, 91, 73, 54, 36, 18, 0 };

    /**
	 * Which level each fader is currently at
	 */
    private int[] buttonFaders = new int[16];

    /**
	 * The MIDI output device
	 */
    private Receiver recv;

    /**
	 * The name of the MIDI output device
	 */
    private String midiDeviceName;

    private int midiChannel;

    private int ccOffset;

    private ADCOptions pageADCOptions = new ADCOptions();

    /**
	 * The name of the page 
	 */
    private String pageName = "MIDI Faders";

    private JLabel pageNameLBL;

    /**
	 * Constructor.
	 * 
	 * @param monome The MonomeConfiguration object this page belongs to
	 * @param index The index of this page (page number)
	 */
    public MIDIFadersPage(MonomeConfiguration monome, int index) {
        this.monome = monome;
        this.index = index;
        for (int i = 0; i < 16; i++) {
            this.buttonFaders[i] = this.monome.sizeY - 1;
        }
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("Add MIDI Output")) {
            String[] midiOutOptions = this.monome.getMidiOutOptions();
            String deviceName = (String) JOptionPane.showInputDialog(this.monome, "Choose a MIDI Output to add", "Add MIDI Output", JOptionPane.PLAIN_MESSAGE, null, midiOutOptions, "");
            if (deviceName == null) {
                return;
            }
            this.addMidiOutDevice(deviceName);
        }
        if (e.getActionCommand().equals("Update Preferences")) {
            this.delayAmount = Integer.parseInt(this.getDelayTF().getText());
            this.midiChannel = Integer.parseInt(this.getChannelTF().getText()) - 1;
            if (this.midiChannel < 0) this.midiChannel = 0;
            this.ccOffset = Integer.parseInt(this.getCcOffsetTF().getText());
        }
    }

    public void addMidiOutDevice(String deviceName) {
        this.recv = this.monome.getMidiReceiver(deviceName);
        this.midiDeviceName = deviceName;
        this.getAddMidiOutButton().removeActionListener(this);
        this.getUpdatePrefsButton().removeActionListener(this);
        this.panel.removeAll();
        this.panel = null;
        this.monome.redrawPanel();
    }

    public String getName() {
        return pageName;
    }

    public void setName(String name) {
        this.pageName = name;
        this.pageNameLBL.setText("Page " + (this.index + 1) + ": " + pageName);
        this.monome.setJMenuBar(this.monome.createMenuBar());
    }

    public JPanel getPanel() {
        if (this.panel != null) {
            return this.panel;
        }
        JPanel panel = new JPanel();
        AnchorLayout panelLayout = new AnchorLayout();
        panel.setLayout(panelLayout);
        panel.setPreferredSize(new java.awt.Dimension(319, 148));
        panel.add(getAddMidiOutButton(), new AnchorConstraint(706, 963, 875, 521, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
        panel.add(getUpdatePrefsButton(), new AnchorConstraint(706, 487, 875, 20, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
        panel.add(getDelayTF(), new AnchorConstraint(347, 371, 489, 268, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
        panel.add(getDelayLabel(), new AnchorConstraint(347, 268, 489, 20, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
        this.getUpdatePrefsButton().addActionListener(this);
        this.getAddMidiOutButton().addActionListener(this);
        pageNameLBL = new JLabel("Page " + (this.index + 1) + ":  MIDI Faders");
        panel.add(pageNameLBL, new AnchorConstraint(0, 800, 82, 0, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
        panel.add(getChannelL(), new AnchorConstraint(347, 710, 489, 500, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
        panel.add(getChannelTF(), new AnchorConstraint(354, 813, 483, 710, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
        panel.add(getCcOffsetLabel(), new AnchorConstraint(489, 268, 638, 20, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
        panel.add(getCcOffsetTF(), new AnchorConstraint(503, 371, 625, 268, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
        JLabel midiout = new JLabel("MIDI Out: " + this.midiDeviceName);
        panel.add(midiout, new AnchorConstraint(179, 894, 307, 20, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL, AnchorConstraint.ANCHOR_REL));
        midiout.setPreferredSize(new java.awt.Dimension(279, 19));
        pageNameLBL.setPreferredSize(new java.awt.Dimension(272, 20));
        this.panel = panel;
        return panel;
    }

    public void handlePress(int x, int y, int value) {
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        int startVal = 0;
        int endVal = 0;
        int cc = this.ccOffset + x;
        if (value == 1) {
            int startY = this.buttonFaders[x];
            int endY = y;
            if (startY == endY) {
                return;
            }
            if (this.monome.sizeY == 8) {
                startVal = this.buttonValuesSmall[startY];
                endVal = this.buttonValuesSmall[endY];
            } else if (this.monome.sizeY == 16) {
                startVal = this.buttonValuesLarge[startY];
                endVal = this.buttonValuesLarge[endY];
            }
            if (this.monome.sizeY == 8) {
                MIDIFader fader = new MIDIFader(this.recv, this.midiChannel, cc, startVal, endVal, this.buttonValuesSmall, this.monome, x, startY, endY, this.index, this.delayAmount);
                new Thread(fader).start();
            } else if (this.monome.sizeY == 16) {
                MIDIFader fader = new MIDIFader(this.recv, this.midiChannel, cc, startVal, endVal, this.buttonValuesLarge, this.monome, x, startY, endY, this.index, this.delayAmount);
                new Thread(fader).start();
            }
            this.buttonFaders[x] = y;
        }
    }

    public void handleReset() {
    }

    public void handleTick() {
    }

    public void redrawMonome() {
        for (int x = 0; x < this.monome.sizeX; x++) {
            for (int y = 0; y < this.monome.sizeY; y++) {
                if (this.buttonFaders[x] <= y) {
                    this.monome.led(x, y, 1, this.index);
                } else {
                    this.monome.led(x, y, 0, this.index);
                }
            }
        }
    }

    public void send(MidiMessage message, long timeStamp) {
        return;
    }

    public String toXml() {
        String xml = "";
        xml += "      <name>MIDI Faders</name>\n";
        xml += "      <pageName>" + this.pageName + "</pageName>\n";
        xml += "      <selectedmidioutport>" + StringEscapeUtils.escapeXml(this.midiDeviceName) + "</selectedmidioutport>\n";
        xml += "      <delayamount>" + this.delayAmount + "</delayamount>\n";
        xml += "      <midichannel>" + (this.midiChannel + 1) + "</midichannel>\n";
        xml += "      <ccoffset>" + this.ccOffset + "</ccoffset>\n";
        xml += "      <ccoffsetADC>" + this.pageADCOptions.getCcOffset() + "</ccoffsetADC>\n";
        xml += "      <sendADC>" + this.pageADCOptions.isSendADC() + "</sendADC>\n";
        xml += "      <midiChannelADC>" + this.pageADCOptions.getMidiChannel() + "</midiChannelADC>\n";
        xml += "      <adcTranspose>" + this.pageADCOptions.getAdcTranspose() + "</adcTranspose>\n";
        xml += "      <recv>" + this.pageADCOptions.getRecv() + "</recv>\n";
        return xml;
    }

    /**
	 * @return The delay setting GUI label
	 */
    private JLabel getDelayLabel() {
        if (delayLabel == null) {
            delayLabel = new JLabel();
            delayLabel.setText("Delay (ms)");
            delayLabel.setPreferredSize(new java.awt.Dimension(79, 21));
        }
        return delayLabel;
    }

    /**
	 * @return The delay setting text field
	 */
    private JTextField getDelayTF() {
        if (delayTF == null) {
            delayTF = new JTextField();
            delayTF.setText("6");
            delayTF.setPreferredSize(new java.awt.Dimension(33, 21));
        }
        return delayTF;
    }

    /**
	 * @return The Add MIDI Output button
	 */
    private JButton getAddMidiOutButton() {
        if (addMidiOutButton == null) {
            addMidiOutButton = new JButton();
            addMidiOutButton.setText("Add MIDI Output");
            addMidiOutButton.setPreferredSize(new java.awt.Dimension(141, 25));
        }
        return addMidiOutButton;
    }

    /**
	 * @return The Update Preferences button
	 */
    private JButton getUpdatePrefsButton() {
        if (updatePrefsButton == null) {
            updatePrefsButton = new JButton();
            updatePrefsButton.setText("Update Preferences");
            updatePrefsButton.setPreferredSize(new java.awt.Dimension(149, 25));
        }
        return updatePrefsButton;
    }

    /**
	 * @param delayAmount The new delay amount (in ms)
	 */
    public void setDelayAmount(int delayAmount) {
        this.delayAmount = delayAmount;
        this.getDelayTF().setText(String.valueOf(delayAmount));
    }

    public boolean getCacheDisabled() {
        return false;
    }

    public void destroyPage() {
        return;
    }

    private JLabel getChannelL() {
        if (channelL == null) {
            channelL = new JLabel();
            channelL.setText("Channel");
            channelL.setPreferredSize(new java.awt.Dimension(67, 21));
        }
        return channelL;
    }

    private JTextField getChannelTF() {
        if (channelTF == null) {
            channelTF = new JTextField();
            channelTF.setText("1");
            channelTF.setPreferredSize(new java.awt.Dimension(33, 19));
        }
        return channelTF;
    }

    private JLabel getCcOffsetLabel() {
        if (ccOffsetLabel == null) {
            ccOffsetLabel = new JLabel();
            ccOffsetLabel.setText("CC Offset");
            ccOffsetLabel.setPreferredSize(new java.awt.Dimension(79, 22));
        }
        return ccOffsetLabel;
    }

    private JTextField getCcOffsetTF() {
        if (ccOffsetTF == null) {
            ccOffsetTF = new JTextField();
            ccOffsetTF.setText("0");
            ccOffsetTF.setPreferredSize(new java.awt.Dimension(33, 18));
        }
        return ccOffsetTF;
    }

    public void setMidiChannel(String midiChannel2) {
        this.midiChannel = Integer.parseInt(midiChannel2) - 1;
        this.getChannelTF().setText(midiChannel2);
    }

    public void setCCOffset(String ccOffset2) {
        this.ccOffset = Integer.parseInt(ccOffset2);
        this.getCcOffsetTF().setText(ccOffset2);
    }

    public void clearPanel() {
        this.panel = null;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void handleADC(int adcNum, float value) {
        if (this.pageADCOptions.isSendADC() && this.monome.adcObj.isEnabled()) {
            int midi = this.pageADCOptions.getMidiChannel();
            if (midi != -1) {
                this.monome.adcObj.sendCC(this.recv, midi, this.pageADCOptions.getCcADC(), monome, adcNum, value);
            } else {
                this.monome.adcObj.sendCC(this.recv, midiChannel, this.pageADCOptions.getCcADC(), monome, adcNum, value);
            }
        }
    }

    public void handleADC(float x, float y) {
        if (this.pageADCOptions.isSendADC() && this.monome.adcObj.isEnabled()) {
            int midi = this.pageADCOptions.getMidiChannel();
            if (midi != -1) {
                this.monome.adcObj.sendCC(this.recv, midi, this.pageADCOptions.getCcADC(), monome, x, y);
            } else {
                this.monome.adcObj.sendCC(this.recv, midiChannel, this.pageADCOptions.getCcADC(), monome, x, y);
            }
        }
    }

    public boolean isTiltPage() {
        return true;
    }

    public ADCOptions getAdcOptions() {
        return this.pageADCOptions;
    }

    public void setAdcOptions(ADCOptions options) {
        this.pageADCOptions = options;
    }

    public void configure(Element pageElement) {
        NodeList nl = pageElement.getElementsByTagName("pageName");
        Element el = (Element) nl.item(0);
        if (el != null) {
            nl = el.getChildNodes();
            String name = ((Node) nl.item(0)).getNodeValue();
            this.setName(name);
        }
        NodeList rowNL = pageElement.getElementsByTagName("delayamount");
        el = (Element) rowNL.item(0);
        if (el != null) {
            nl = el.getChildNodes();
            String delayAmount = ((Node) nl.item(0)).getNodeValue();
            this.setDelayAmount(Integer.parseInt(delayAmount));
        }
        NodeList channelNL = pageElement.getElementsByTagName("midichannel");
        el = (Element) channelNL.item(0);
        if (el != null) {
            nl = el.getChildNodes();
            String midiChannel = ((Node) nl.item(0)).getNodeValue();
            this.setMidiChannel(midiChannel);
        }
        NodeList ccOffsetNL = pageElement.getElementsByTagName("ccoffset");
        el = (Element) ccOffsetNL.item(0);
        if (el != null) {
            nl = el.getChildNodes();
            String ccOffset = ((Node) nl.item(0)).getNodeValue();
            this.setCCOffset(ccOffset);
        }
        nl = pageElement.getElementsByTagName("ccoffsetADC");
        el = (Element) nl.item(0);
        if (el != null) {
            nl = el.getChildNodes();
            String ccOffset = ((Node) nl.item(0)).getNodeValue();
            this.pageADCOptions.setCcOffset(Integer.parseInt(ccOffset));
        }
        nl = pageElement.getElementsByTagName("sendADC");
        el = (Element) nl.item(0);
        if (el != null) {
            nl = el.getChildNodes();
            String sendADC = ((Node) nl.item(0)).getNodeValue();
            this.pageADCOptions.setSendADC(Boolean.parseBoolean(sendADC));
        }
        nl = pageElement.getElementsByTagName("adcTranspose");
        el = (Element) nl.item(0);
        if (el != null) {
            nl = el.getChildNodes();
            String adcTranspose = ((Node) nl.item(0)).getNodeValue();
            this.pageADCOptions.setAdcTranspose(Integer.parseInt(adcTranspose));
        }
        nl = pageElement.getElementsByTagName("midiChannelADC");
        el = (Element) nl.item(0);
        if (el != null) {
            nl = el.getChildNodes();
            String midiChannelADC = ((Node) nl.item(0)).getNodeValue();
            this.pageADCOptions.setMidiChannel(Integer.parseInt(midiChannelADC));
        }
        nl = pageElement.getElementsByTagName("recv");
        el = (Element) nl.item(0);
        if (el != null) {
            nl = el.getChildNodes();
            String recv = ((Node) nl.item(0)).getNodeValue();
            this.pageADCOptions.setRecv(recv);
        }
    }
}
