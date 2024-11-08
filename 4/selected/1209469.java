package org.chernovia.lib.music.midi;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;

/**
 * Write a description of class JMIDI here.
 * 
 * @author (your name)
 * @version (a version number or a date)
 */
public class JMIDI {

    private static Synthesizer Synth = null;

    private static MidiChannel[] channels = null;

    private static Instrument[] instruments = null;

    private static Soundbank DefBank = null;

    private static boolean READY = false;

    private static Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

    public static boolean load() {
        try {
            MidiDevice.Info[] devinfo = MidiSystem.getMidiDeviceInfo();
            for (int d = 0; d < devinfo.length; d++) logger.log(Level.INFO, "Device: " + devinfo[d].toString());
            Synth = MidiSystem.getSynthesizer();
            Synth.open();
            channels = Synth.getChannels();
            logger.log(Level.INFO, channels.length + " Channels");
            DefBank = Synth.getDefaultSoundbank();
            if (DefBank != null) {
                logger.log(Level.INFO, "Default Soundbank: " + DefBank.getName());
                Synth.loadAllInstruments(DefBank);
            }
            instruments = Synth.getAvailableInstruments();
            logger.log(Level.INFO, instruments.length + " Instruments");
        } catch (Exception e) {
            logger.log(Level.INFO, "Error loading Synth: " + e.getMessage());
            e.printStackTrace(System.out);
            return false;
        }
        READY = true;
        return true;
    }

    public static boolean isReady() {
        return READY && (Synth != null);
    }

    public static Synthesizer getSynth() {
        return Synth;
    }

    public static MidiChannel[] getChannels() {
        return channels;
    }

    public static MidiChannel getChannel(int chan) {
        if (isReady()) return channels[chan]; else return null;
    }

    public static int numChannels() {
        if (isReady()) return channels.length; else return -1;
    }

    public static void setChannel(int chan, int instr) {
        if (!isReady()) return;
        if (chan >= channels.length) chan = 0;
        if (instr >= instruments.length) instr = 0;
        channels[chan].programChange(instruments[instr].getPatch().getProgram());
        logger.log(Level.INFO, "Channel " + chan + ": " + instruments[instr].getName());
    }

    public static Instrument[] getInstruments() {
        return instruments;
    }

    public static Instrument getInstrument(int index) {
        if (isReady()) return instruments[index]; else return null;
    }

    public static int numInstruments() {
        if (isReady()) return instruments.length; else return -1;
    }

    public static void unload() {
        READY = false;
        try {
            Synth.close();
        } catch (Exception e) {
            logger.log(Level.INFO, "Error loading Synth: " + e.getMessage());
            e.printStackTrace(System.out);
        }
    }

    public static void silence() {
        if (isReady()) {
            for (int c = 0; c < getChannels().length; c++) getChannel(c).allNotesOff();
        }
    }
}
