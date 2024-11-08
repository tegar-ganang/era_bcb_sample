package midikernel;

import java.io.File;
import java.io.IOException;
import java.util.Vector;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;

/**
 * The MidiKernel's purpose is to encapsule all the midi specific details from the
 * rest of the application.
 * An instance of the MidiKernel class is passed around the whole class structure.
 * 
 * Synthesizer = Your soundcards output is one.
 * Sequencer = Your midi-keyboard is a sequencer but there is also a sequencer in your
 * soundcard, so that midi messages can be synthesized and forwarded to a Synthesizer.
 */
public class MidiKernel {

    private Vector synthInfos = new Vector();

    private Vector seqInfos = new Vector();

    private Synthesizer defSynth;

    private MidiChannel[] defSynthChannels;

    private Sequencer defSeq;

    private Transmitter defTrns;

    public MidiKernel() {
        try {
            initialize();
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }

    /**
     * @param mm Sends this MidiMessage to the receiver. Wich is the soundcard.
     */
    public void sendMessage(MidiMessage mm) {
        try {
            Receiver r = defSynth.getReceiver();
            r.send(mm, -1);
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Changes the program(instrument/sound) of the midi output.
     * 
     * @param channel channel
     * @param program program
     */
    public final void changeProgram(final int channel, final int program) {
        defSynthChannels[channel].programChange(program);
    }

    /**
     * Plug in keyboard and encapsule the the synthesizers receiver with a custom
     * one for data output.
     *  
     * @throws Exception ToDo: Proper handling.
     */
    public final void pluginKeyboard() throws Exception {
        defTrns.setReceiver(new DefaultMidiMessageReceiver(defSynth.getReceiver()));
    }

    /**
     * @param rec Plug in a custom receiver to the Transmitter. That could be the
     *            standard Midi-Synthesizer so a midi-file can 
     */
    public final void pluginKeyboard(final Receiver rec) {
        defTrns.setReceiver(rec);
    }

    public final Receiver getReceiver() {
        Receiver retVal = null;
        try {
            retVal = defSynth.getReceiver();
        } catch (Exception e) {
            System.out.println(e.toString());
            e.printStackTrace();
        }
        return retVal;
    }

    /**
     * Set the default Synthesizer and Sequencer, load the soundbank.
     * 
     * @throws Exception ToDo: Proper exception handling.
     */
    private void initialize() throws Exception {
        defSynth = MidiSystem.getSynthesizer();
        defSynth.open();
        defSynth.loadAllInstruments(MidiSystem.getSoundbank(new File("soundbank.gm")));
        defSynthChannels = defSynth.getChannels();
        defSeq = MidiSystem.getSequencer();
        defSeq.open();
        try {
            defTrns = MidiSystem.getTransmitter();
        } catch (Exception e) {
            System.out.println("Was not able to get handle to MIDI device.");
            System.out.println("Disabling MIDI input.");
            defTrns = new MockupMIDITransmitter();
        }
    }

    /**
     * Turns a midi-file into a sequence and plays it on the sequencer.
     * 
     * @param filename filename
     * @throws Exception ToDo: Proper exception handling.
     */
    public final void playMIDI(final String filename) throws Exception {
        playMIDI(readSequence("11candy.mid"));
    }

    /**
     * Plays a midi-sequence on the Sequencer.
     * 
     * @param seq Sequence.
     * @throws Exception ToDo: Proper exception handling.
     */
    public final void playMIDI(final Sequence seq) throws Exception {
        defSeq.setSequence(seq);
        defSeq.start();
    }

    /**
     * Turns a midi-file into a sequence.
     * 
     * @param filename filename
     * @return Returns the sequence.
     * @throws IOException When file not accessible. 
     * @throws InvalidMidiDataException When file format is wrong.
     */
    public static Sequence readSequence(final String filename) throws InvalidMidiDataException, IOException {
        return MidiSystem.getSequence(new File(filename));
    }

    public final void shutdown() {
        if (defTrns != null) {
            defTrns.close();
        }
        if (defSynth != null && defSynth.isOpen()) {
            defSynth.close();
        }
        if (defSeq != null && defSeq.isOpen()) {
            defSeq.close();
        }
    }

    public static void hexOut(final int[] i) {
        for (int n = 0; n < i.length; n++) {
            System.out.print(Integer.toHexString(i[n]));
        }
        System.out.println();
    }

    public static void hexOut(final byte[] b) {
        int[] i = byteToInt(b);
        for (int n = 0; n < i.length; n++) {
            System.out.print(Integer.toHexString(i[n]));
        }
        System.out.println();
    }

    public static int[] byteToInt(final byte[] b) {
        int[] i = new int[b.length];
        for (int n = 0; n < b.length; n++) {
            i[n] = (int) (b[n] & 0xFF);
        }
        return i;
    }

    /**
     * Calculates the tick length of a sequence.
     * 
     * @param s Sequence.
     * @return Returns the tick length.
     */
    public static double getTickLength(Sequence s) {
        return ((double) s.getMicrosecondLength()) / ((double) s.getTickLength()) / 1000.0;
    }

    /**
     * @param filename filename.
     * @return Returns a string containing information about the midi-file.
     * @throws Exception ToDo: Proper exception handling.
     */
    public static String getMidiInfo(String filename) throws Exception {
        String retval = "Filename: \"" + filename + "\"\n";
        Sequence s = MidiKernel.readSequence(filename);
        retval += "Sequence length in ms: " + (s.getMicrosecondLength() / 1000.0) + "\n";
        Track[] ts = s.getTracks();
        retval += "Number of tracks: " + ts.length + "\n";
        for (int i = 0; i < ts.length; i++) {
            retval += "Track" + (i + 1) + " length:" + (MidiKernel.getTickLength(s) * ts[i].ticks()) + "\"";
        }
        return retval;
    }
}
