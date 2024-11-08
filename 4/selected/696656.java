package org.monome.pages;

import java.awt.Label;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.illposed.osc.OSCMessage;

/**
 * The Ableton Clip Control page.  Usage information is available at:
 * 
 * http://code.google.com/p/monome-pages/wiki/AbletonClipControlPage
 *   
 * @author Julien Bayle
 *
 */
public class AbletonClipControlPage implements ActionListener, Page {

    /**
	 * Reference to the MonomeConfiguration this page belongs to.
	 */
    MonomeConfiguration monome;

    /**
	 * This page's index (page number).
	 */
    private int index;

    /**
	 * This page's GUI / control panel.
	 */
    private JPanel panel;

    /**
	 * clipState[track_number][clip_number] - The current state of all clips in Ableton.
	 */
    private int[][] clipState = new int[200][250];

    /**
	 * Used to represent an empty clip slot
	 */
    private static final int CLIP_STATE_EMPTY = 0;

    /**
	 * Used to represent a clip slot with a clip that is stopped 
	 */
    private static final int CLIP_STATE_STOPPED = 1;

    /**
	 * Used to represent a clip slot with a clip that is playing 
	 */
    private static final int CLIP_STATE_PLAYING = 2;

    /**
	 * flashState[track_number][clip_number} - Whether to flash on or off on the next tick
	 */
    private boolean[][] flashState = new boolean[200][250];

    /**
	 * tracksStopped[track_number] - Table container the stopped tracks information, true if the track was stopped at least one time.
	 */
    private boolean[] tracksStopped = new boolean[200];

    /**
	 * The amount to offset the monome display of the clips
	 */
    private int clipOffset = 0;

    /**
	 * The number of scene per song
	 */
    private int scenesPerSong = 6;

    /**
	 * The maximum number of scenes (here, it is a multiple of scenePerSong ; here: maximum 50 songs...!)
	 */
    private int maxScenes = 300;

    /**
	 * The amount to offset the monome display of the tracks
	 */
    private int trackOffset;

    /**
	 * Ableton's current tempo/BPM setting
	 */
    private float tempo = (float) 120.0;

    /**
	 * The text field that stores the delay value 
	 */
    private JTextField delayBlinking;

    /**
	 * The label for the delay setting
	 */
    private JLabel delayBlinkingLabel;

    /**
	 * The delay amount for midi notes feedback
	 */
    private int delayBlinkingAmount = 80;

    /**
	 * The Update Preferences button 
	 */
    private JButton updatePrefsButton;

    /**
	 * The number of control rows (track stop/midi notes feedback line + multi command line) that are enabled currently
	 */
    private int numEnabledRows = 2;

    /**
	 * The name of the page 
	 */
    private String pageName = "Ableton Clip Controller";

    private JLabel pageNameLBL;

    /**
	 * @param monome The MonomeConfiguration this page belongs to
	 * @param index This page's index number
	 */
    public AbletonClipControlPage(MonomeConfiguration monome, int index) {
        this.monome = monome;
        this.index = index;
        this.monome.configuration.initAbleton();
    }

    public void actionPerformed(ActionEvent e) {
        int numEnabledRows = 0;
        this.numEnabledRows = numEnabledRows;
        if (e.getActionCommand().equals("Add MIDI Output")) {
            String[] midiOutOptions = this.monome.getMidiOutOptions();
            String deviceName = (String) JOptionPane.showInputDialog(this.monome, "Choose a MIDI Output to add", "Add MIDI Output", JOptionPane.PLAIN_MESSAGE, null, midiOutOptions, "");
            if (deviceName == null) {
                return;
            }
            this.addMidiOutDevice(deviceName);
        }
        return;
    }

    public void addMidiOutDevice(String deviceName) {
        return;
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
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        pageNameLBL = new JLabel("Page " + (this.index + 1) + ": Ableton Clip Controller");
        panel.add(pageNameLBL);
        this.panel = panel;
        return panel;
    }

    public void handlePress(int x, int y, int value) {
        if (value == 1) {
            if (y > this.monome.sizeY - this.numEnabledRows - 1) {
                if (y == this.monome.sizeY - 2) {
                    int track_num = x + (this.trackOffset * (this.monome.sizeX - 1));
                    this.stopTrack(track_num);
                    this.viewTrack(track_num);
                }
                if (y == this.monome.sizeY - 1) {
                    if (x == 0) {
                        if (this.clipOffset > 0) {
                            this.clipOffset -= 1;
                        }
                    } else if (x == 1) {
                        if ((this.clipOffset + 1) * (this.scenesPerSong) < this.maxScenes) {
                            this.clipOffset += 1;
                        }
                    } else if (x == 2) {
                        this.tempoDown();
                    } else if (x == 3) {
                        this.tempoUp();
                    } else if (x == 4) {
                    } else if (x == 5) {
                    } else if (x == 6) {
                    } else if (x == 7) {
                    }
                }
            } else {
                int clip_num = y + (this.clipOffset * (this.monome.sizeY - this.numEnabledRows));
                int track_num = x + (this.trackOffset * (this.monome.sizeX - 1));
                this.viewTrack(track_num);
                this.playClip(track_num, clip_num);
            }
        }
    }

    /**
	 * Sends "/live/play/clip track clip" to LiveOSC.
	 * 
	 * @param track The track number to play (0 = first track)
	 * @param clip The clip number to play (0 = first clip)
	 */
    public void playClip(int track, int clip) {
        Object args[] = new Object[2];
        args[0] = new Integer(track);
        args[1] = new Integer(clip);
        OSCMessage msg = new OSCMessage("/live/play/clipslot", args);
        try {
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Sends "/live/arm track" to LiveOSC.
	 * 
	 * @param track The track number to arm (0 = first track)
	 */
    public void armTrack(int track) {
        Object args[] = new Object[1];
        args[0] = new Integer(track);
        OSCMessage msg = new OSCMessage("/live/arm", args);
        try {
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Sends "/live/redo" to LiveOSC. 
	 */
    public void abletonRedo() {
        OSCMessage msg = new OSCMessage("/live/redo");
        try {
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Sends "/live/undo" to LiveOSC. 
	 */
    public void abletonUndo() {
        OSCMessage msg = new OSCMessage("/live/undo");
        try {
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Sends "/live/tempo tempo-1" to LiveOSC. 
	 */
    public void tempoDown() {
        if (this.tempo - 2.0 < 20.0) {
            this.tempo = (float) 22.0;
        }
        Object args[] = new Object[1];
        args[0] = new Float(this.tempo - 2.0);
        OSCMessage msg = new OSCMessage("/live/tempo", args);
        try {
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Sends "/live/tempo tempo+1" to LiveOSC. 
	 */
    public void tempoUp() {
        if (this.tempo + 2.0 > 999.0) {
            this.tempo = (float) 998.0;
        }
        Object args[] = new Object[1];
        args[0] = new Float(this.tempo + 2.0);
        OSCMessage msg = new OSCMessage("/live/tempo", args);
        try {
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Sends "/live/disarm track" to LiveOSC.
	 * 
	 * @param track The track number to disarm (0 = first track)
	 */
    public void disarmTrack(int track) {
        Object args[] = new Object[1];
        args[0] = new Integer(track);
        OSCMessage msg = new OSCMessage("/live/disarm", args);
        try {
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Sends "/live/stop/track track" to LiveOSC.
	 * 
	 * @param track The track number to stop (0 = first track)
	 */
    public void stopTrack(int track) {
        Object args[] = new Object[1];
        args[0] = new Integer(track);
        OSCMessage msg = new OSCMessage("/live/stop/track", args);
        try {
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Sends "/live/track/view track" to LiveOSC.
	 * 
	 * @param track The track number to stop (0 = first track)
	 */
    public void viewTrack(int track) {
        Object args[] = new Object[1];
        args[0] = new Integer(track);
        OSCMessage msg = new OSCMessage("/live/track/view", args);
        try {
            this.monome.configuration.getAbletonOSCPortOut().send(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handleReset() {
        return;
    }

    public void handleTick() {
        return;
    }

    public void redrawMonome() {
        for (int track = 0; track < this.monome.sizeX; track++) {
            for (int clip = 0; clip < (this.scenesPerSong); clip++) {
                int clip_num = clip + (this.clipOffset * (this.monome.sizeY - this.numEnabledRows));
                int track_num = track + (this.trackOffset * (this.monome.sizeX - 1));
                if (this.clipState[track_num][clip_num] == CLIP_STATE_PLAYING) {
                    if (this.flashState[track][clip] == true) {
                        this.flashState[track][clip] = false;
                        this.monome.led(track, clip, 1, this.index);
                    } else {
                        this.flashState[track][clip] = true;
                        this.monome.led(track, clip, 0, this.index);
                    }
                } else if (this.clipState[track_num][clip_num] == CLIP_STATE_STOPPED) {
                    this.monome.led(track, clip, 1, this.index);
                } else if (this.clipState[track_num][clip_num] == CLIP_STATE_EMPTY) {
                    this.monome.led(track, clip, 0, this.index);
                }
            }
        }
        for (int i = 0; i < this.monome.sizeX; i++) {
            int track_num = i + (this.trackOffset * (this.monome.sizeX - 1));
            if (this.tracksStopped[track_num] == true) {
                this.monome.led(i, this.monome.sizeY - this.numEnabledRows, 0, this.index);
            }
        }
    }

    public void send(MidiMessage message, long timeStamp) {
        ShortMessage shortMessage;
        if (message instanceof ShortMessage) {
            shortMessage = (ShortMessage) message;
            switch(shortMessage.getCommand()) {
                case 0x90:
                    if (shortMessage.getChannel() == 8) {
                        LEDBlink ledBlink = new LEDBlink(this.monome, 0, 6, this.delayBlinkingAmount, this.index);
                        new Thread(ledBlink).start();
                    }
                    if (shortMessage.getChannel() == 9) {
                        LEDBlink ledBlink = new LEDBlink(this.monome, 1, 6, this.delayBlinkingAmount, this.index);
                        new Thread(ledBlink).start();
                    }
                    if (shortMessage.getChannel() == 10) {
                        LEDBlink ledBlink = new LEDBlink(this.monome, 2, 6, this.delayBlinkingAmount, this.index);
                        new Thread(ledBlink).start();
                    }
                    if (shortMessage.getChannel() == 11) {
                        LEDBlink ledBlink = new LEDBlink(this.monome, 3, 6, this.delayBlinkingAmount, this.index);
                        new Thread(ledBlink).start();
                    }
                    if (shortMessage.getChannel() == 12) {
                        LEDBlink ledBlink = new LEDBlink(this.monome, 4, 6, this.delayBlinkingAmount, this.index);
                        new Thread(ledBlink).start();
                    }
                    if (shortMessage.getChannel() == 13) {
                        LEDBlink ledBlink = new LEDBlink(this.monome, 5, 6, this.delayBlinkingAmount, this.index);
                        new Thread(ledBlink).start();
                    }
                    if (shortMessage.getChannel() == 14) {
                        LEDBlink ledBlink = new LEDBlink(this.monome, 6, 6, this.delayBlinkingAmount, this.index);
                        new Thread(ledBlink).start();
                    }
                    if (shortMessage.getChannel() == 15) {
                        LEDBlink ledBlink = new LEDBlink(this.monome, 7, 6, this.delayBlinkingAmount, this.index);
                        new Thread(ledBlink).start();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    public String toXml() {
        String xml = "";
        xml += "      <name>Ableton Clip Control</name>\n";
        xml += "      <pageName>" + this.pageName + "</pageName>\n";
        return xml;
    }

    /**
	 * Called by AbletonClipUpdater based on messages received by LiveOSC.
	 * 
	 * @param track The track number to update
	 * @param clip The clip number to update
	 * @param state The new state
	 */
    public void updateClipState(int track, int clip, int state) {
        if (this.clipState[track][clip] != state) {
            for (int x = 0; x < this.monome.sizeX - 1; x++) {
                for (int y = 0; y < this.monome.sizeY - this.numEnabledRows; y++) {
                    int clip_num = y + (this.clipOffset * (this.monome.sizeY - this.numEnabledRows));
                    this.flashState[x][clip_num] = false;
                }
            }
        }
        this.clipState[track][clip] = state;
    }

    /**
	 * Called by AbletonClipUpdater based on messages received by LiveOSC.
	 * 
	 * @param track The track number to update
	 * @param armed The state of the track (true = armed)
	 */
    public void updateTrackState(int track, int armed) {
        boolean redrawNeeded = false;
        boolean state = (armed != 0);
        if (this.tracksStopped[track] != state) {
            redrawNeeded = true;
        }
        this.tracksStopped[track] = state;
        if (redrawNeeded) {
            this.redrawMonome();
        }
    }

    /**
	 * @return The delay setting GUI label
	 */
    private JLabel getDelayBlinkingLabel() {
        if (delayBlinkingLabel == null) {
            delayBlinkingLabel = new JLabel();
            delayBlinkingLabel.setText("Blinking delay (ms)");
            delayBlinkingLabel.setPreferredSize(new java.awt.Dimension(67, 14));
        }
        return delayBlinkingLabel;
    }

    /**
	 * @return The delay setting text field
	 */
    private JTextField getDelayBlinking() {
        if (delayBlinking == null) {
            delayBlinking = new JTextField();
            delayBlinking.setText("60");
            delayBlinking.setPreferredSize(new java.awt.Dimension(33, 20));
        }
        return delayBlinking;
    }

    /**
	 * @return The Update Preferences button
	 */
    private JButton getUpdatePrefsButton() {
        if (updatePrefsButton == null) {
            updatePrefsButton = new JButton();
            updatePrefsButton.setText("Update Preferences");
            updatePrefsButton.setPreferredSize(new java.awt.Dimension(149, 21));
        }
        return updatePrefsButton;
    }

    public boolean getCacheDisabled() {
        return false;
    }

    public void destroyPage() {
        return;
    }

    public void updateAbletonState(float tempo, int overdub) {
        this.tempo = tempo;
    }

    public void clearPanel() {
        this.panel = null;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void handleADC(int adcNum, float value) {
    }

    public void handleADC(float x, float y) {
    }

    public boolean isTiltPage() {
        return false;
    }

    public ADCOptions getAdcOptions() {
        return null;
    }

    public void setAdcOptions(ADCOptions options) {
    }

    public void configure(Element pageElement) {
        NodeList nameNL = pageElement.getElementsByTagName("pageName");
        Element el = (Element) nameNL.item(0);
        if (el != null) {
            NodeList nl = el.getChildNodes();
            String name = ((Node) nl.item(0)).getNodeValue();
            this.setName(name);
        }
    }

    public void updateAbletonArmState(int track, int state) {
    }

    public void updateAbletonMuteState(int track, int state) {
    }

    public void updateAbletonSoloState(int track, int state) {
    }
}
