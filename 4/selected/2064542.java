package com.frinika.midi;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Transmitter;
import javax.sound.midi.VoiceStatus;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import rasmus.midi.provider.RasmusSynthesizer;
import com.frinika.project.gui.ProjectFrame;
import com.frinika.sequencer.gui.mixer.MidiDeviceIconProvider;
import com.frinika.sequencer.model.MidiLane;
import com.frinika.synth.Synth;

/**
 * 
 * DrumMapper is a midi device that redirects midi events to other devices doing some mapping enroute.
 * 
 */
public class DrumMapper implements MidiDevice, MidiDeviceIconProvider {

    public class NoteMap {

        public int note;
    }

    private static Icon icon = new javax.swing.ImageIcon(RasmusSynthesizer.class.getResource("/icons/frinika.png"));

    public Icon getIcon() {
        if (icon.getIconHeight() > 16 || icon.getIconWidth() > 16) {
            BufferedImage img = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D g = img.createGraphics();
            icon.paintIcon(null, g, 0, 0);
            g.dispose();
            Image im = img.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
            icon = new ImageIcon(im);
        }
        return icon;
    }

    MidiDevice defaultDevice;

    Receiver defRecv;

    NoteMap noteMap[] = new NoteMap[128];

    public static class DrumMapperInfo extends Info {

        DrumMapperInfo() {
            super("DrumMapper", "drpj.co.uk", "A MIDI drum mapper", "0.0.1");
        }
    }

    Info deviceInfo = new DrumMapperInfo();

    Receiver receiver;

    List<Receiver> receivers;

    DrumMapper() {
        int i = 0;
        for (; i < 128; i++) {
            NoteMap n = noteMap[i] = new NoteMap();
            n.note = i;
        }
        receiver = new Receiver() {

            public void send(MidiMessage message, long timeStamp) {
                try {
                    if (message instanceof ShortMessage) {
                        ShortMessage shm = (ShortMessage) message;
                        if (shm.getCommand() == ShortMessage.NOTE_ON) {
                            int note = shm.getData1();
                            Receiver recv = defRecv;
                            if (recv == null) return;
                            int noteByte = noteMap[note].note;
                            shm.setMessage(shm.getCommand(), shm.getChannel(), noteByte, shm.getData2());
                            recv.send(shm, timeStamp);
                            return;
                        }
                    }
                    if (defRecv != null) defRecv.send(message, timeStamp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public void close() {
            }
        };
        receivers = new ArrayList<Receiver>();
        receivers.add(receiver);
    }

    public void save(File file) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(" DRUM MAP SAVE ");
    }

    public void load(File file) {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(" DRUM MAP LOAD ");
    }

    public int getMaxPolyphony() {
        return 0;
    }

    public MidiChannel[] getChannels() {
        return null;
    }

    public VoiceStatus[] getVoiceStatus() {
        return null;
    }

    public boolean isSoundbankSupported(Soundbank soundbank) {
        return false;
    }

    public boolean loadInstrument(Instrument instrument) {
        return false;
    }

    public void unloadInstrument(Instrument instrument) {
    }

    public boolean remapInstrument(Instrument from, Instrument to) {
        return false;
    }

    public Soundbank getDefaultSoundbank() {
        return null;
    }

    public Instrument[] getAvailableInstruments() {
        return null;
    }

    public Instrument[] getLoadedInstruments() {
        return null;
    }

    public boolean loadAllInstruments(Soundbank soundbank) {
        return false;
    }

    public void unloadAllInstruments(Soundbank soundbank) {
    }

    public boolean loadInstruments(Soundbank soundbank, Patch[] patchList) {
        return false;
    }

    public void unloadInstruments(Soundbank soundbank, Patch[] patchList) {
    }

    public Info getDeviceInfo() {
        return deviceInfo;
    }

    public void open() throws MidiUnavailableException {
    }

    public void close() {
    }

    public boolean isOpen() {
        return false;
    }

    public long getMicrosecondPosition() {
        return 0;
    }

    public int getMaxReceivers() {
        return -1;
    }

    public int getMaxTransmitters() {
        return 0;
    }

    public Receiver getReceiver() throws MidiUnavailableException {
        return receiver;
    }

    @SuppressWarnings("unchecked")
    public List getReceivers() {
        return receivers;
    }

    public Transmitter getTransmitter() throws MidiUnavailableException {
        return null;
    }

    @SuppressWarnings("unchecked")
    public List getTransmitters() {
        return null;
    }

    /**
	 * over to provide easier GUI manufactoring
	 */
    public String toString() {
        return getDeviceInfo().toString();
    }

    public void instrumentNameChange(Synth synth, String instrumentName) {
    }

    public MidiDevice getDefaultMidiDevice() {
        return defaultDevice;
    }

    public void setDefaultMidiDevice(MidiDevice midiDevice) {
        if (defaultDevice != midiDevice) {
            if (defRecv != null) defRecv.close();
            try {
                midiDevice.open();
                defRecv = midiDevice.getReceiver();
                System.out.println(" Set default receiver " + defRecv);
                defaultDevice = midiDevice;
            } catch (MidiUnavailableException e) {
                e.printStackTrace();
            }
        }
        defaultDevice = midiDevice;
    }

    public JPanel getGUIPanel(ProjectFrame frame, MidiLane lane) {
        return new DrumMapperGUI(this, frame.getProjectContainer(), lane);
    }

    public NoteMap getNoteMap(int i) {
        return noteMap[i];
    }

    public void setMapping(int in, int out) {
        if (in < 0 || in > 127) return;
        if (out < 0 || out > 127) return;
        System.out.println(in + " --->" + out);
        noteMap[in].note = out;
    }

    public void setNoteMap(int[] noteMap2) {
        for (int i = 0; i < 128; i++) {
            noteMap[i].note = noteMap2[i];
        }
    }
}
