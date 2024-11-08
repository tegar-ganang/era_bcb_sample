package org.rubato.audio.midi;

import static org.rubato.logeo.DenoFactory.makeDenotator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import javax.sound.midi.*;
import org.rubato.base.Repository;
import org.rubato.math.arith.Rational;
import org.rubato.math.yoneda.Denotator;
import org.rubato.math.yoneda.Form;

/**
 * This class reads in a MIDI file an converts it to a denotator of form Score.
 *  
 * @author Gérard Milmeister
 */
public class MidiReader {

    /**
     * Creates a MidiReader that reads from a file.
     * @throws InvalidMidiDataException
     * @throws IOException
     */
    public MidiReader(String fileName) throws InvalidMidiDataException, IOException {
        readSequence(fileName);
    }

    /**
     * Creates a MidiReader that reads from an InputStream.
     * @throws InvalidMidiDataException
     * @throws IOException
     */
    public MidiReader(InputStream stream) throws InvalidMidiDataException, IOException {
        readSequence(stream);
    }

    /**
     * Reads the MIDI file and returns a denotator of form Score.
     */
    public Denotator getDenotator() {
        processSequence();
        return score;
    }

    /**
     * The the factor that each time value is multiplied with.
     * The default is 1.0;
     */
    public void setTempoFactor(double f) {
        tempoFactor = f;
    }

    /**
     * Reads a MIDI sequence from a file.
     * @throws InvalidMidiDataException
     * @throws IOException
     */
    private void readSequence(String fileName) throws InvalidMidiDataException, IOException {
        File midiFile = new File(fileName);
        sequence = MidiSystem.getSequence(midiFile);
        initialize();
    }

    /**
     * Reads a MIDI sequence from an InputStream.
     * @throws InvalidMidiDataException
     * @throws IOException
     */
    private void readSequence(InputStream stream) throws InvalidMidiDataException, IOException {
        sequence = MidiSystem.getSequence(stream);
        initialize();
    }

    /**
     * Initializes various parameters from the sequence.
     */
    private void initialize() {
        float divisionType = sequence.getDivisionType();
        if (divisionType != Sequence.PPQ) {
            throw new IllegalStateException("Only division type PPQ supported");
        }
        ticksPerQuarter = sequence.getResolution();
        tracks = sequence.getTracks();
    }

    /**
     * Processes the sequence that has been read in
     * and construct the Score denotator.
     */
    private void processSequence() {
        LinkedList<Denotator> noteList = new LinkedList<Denotator>();
        for (int i = 0; i < tracks.length; i++) {
            initTrack(i);
            for (int j = 0; j < tracks[i].size(); j++) {
                processEvent(tracks[i].get(j));
            }
            for (Key key : notes) {
                noteList.add(makeNote(key));
            }
        }
        score = makeDenotator(scoreForm, noteList);
    }

    /**
     * Processes a MIDI event in a track.
     * According to the type of message, dispatches to processShortMessage
     * or processMetaMessage.
     */
    private void processEvent(MidiEvent event) {
        MidiMessage msg = event.getMessage();
        currentTick = event.getTick();
        if (msg instanceof ShortMessage) {
            processShortMessage((ShortMessage) msg);
        } else if (msg instanceof MetaMessage) {
            processMetaMessage((MetaMessage) msg);
        }
    }

    /**
     * Processes a short message from a track.
     * Currently only note on, note off and program change messages are considered.
     */
    private void processShortMessage(ShortMessage msg) {
        switch(msg.getCommand()) {
            case 0x80:
                {
                    processNoteOffEvent(msg.getData1(), msg.getChannel());
                    break;
                }
            case 0x90:
                {
                    if (msg.getData2() == 0) {
                        processNoteOffEvent(msg.getData1(), msg.getChannel());
                    } else {
                        processNoteOnEvent(msg.getData1(), msg.getData2(), msg.getChannel());
                    }
                    break;
                }
            case 0xa0:
                {
                    break;
                }
            case 0xb0:
                {
                    processControlChange(msg.getData1(), msg.getData2(), msg.getChannel());
                    break;
                }
            case 0xc0:
                {
                    currentProgram = msg.getData1();
                    break;
                }
            case 0xd0:
                {
                    break;
                }
            case 0xe0:
                {
                    break;
                }
            case 0xF0:
                {
                    break;
                }
        }
    }

    /**
     * Processes a meta message from a track.
     * Currently only tempo, time signature and key signature changes are considered.
     */
    private void processMetaMessage(MetaMessage msg) {
        switch(msg.getType()) {
            case 0:
                {
                    break;
                }
            case 1:
                {
                    break;
                }
            case 2:
                {
                    break;
                }
            case 3:
                {
                    break;
                }
            case 4:
                {
                    break;
                }
            case 5:
                {
                    break;
                }
            case 6:
                {
                    break;
                }
            case 7:
                {
                    break;
                }
            case 0x20:
                {
                    break;
                }
            case 0x2F:
                {
                    break;
                }
            case 0x51:
                {
                    break;
                }
            case 0x54:
                {
                    break;
                }
            case 0x58:
                {
                    break;
                }
            case 0x59:
                {
                    break;
                }
            case 0x7F:
                {
                    break;
                }
        }
    }

    /**
     * Processes a note on event.
     */
    private void processNoteOnEvent(int key, int velocity, int channel) {
        Key k = keys[channel][key] = new Key();
        k.key = key;
        k.channel = channel;
        k.velocity = velocity;
        k.tick = currentTick;
        k.program = currentProgram;
        notes.add(k);
    }

    /**
     * Processes a note off event.
     */
    private void processNoteOffEvent(int key, int channel) {
        Key k = keys[channel][key];
        if (k != null) {
            k.duration = currentTick - k.tick;
        }
    }

    /**
     * Processes a control change event.
     * Currently only the damper pedal is considered.
     */
    private void processControlChange(int controller, int value, int channel) {
        switch(controller) {
            case 0x40:
                {
                    if (value > 0) {
                        Key k = controls[channel][controller] = new Key();
                        k.key = controller;
                        k.channel = channel;
                        k.velocity = value;
                        k.tick = currentTick;
                        k.program = currentProgram;
                        pedals.add(k);
                    } else {
                        Key k = controls[channel][controller];
                        if (k != null) {
                            k.duration = currentTick - k.tick;
                        }
                    }
                }
        }
    }

    /**
     * This private class represents a key event.
     */
    protected class Key {

        int velocity;

        long tick;

        long duration;

        int channel;

        int program;

        int key;
    }

    /**
     * Initializes various data structures before a track is processed.
     */
    private void initTrack(int tracknr) {
        keys = new Key[NR_CHANNELS][NR_KEYS];
        controls = new Key[NR_CHANNELS][NR_CONTROLS];
        notes = new LinkedList<Key>();
        pedals = new LinkedList<Key>();
        currentProgram = 0;
        currentTick = 0;
    }

    private Denotator makeNote(Key key) {
        LinkedList<Denotator> factors = new LinkedList<Denotator>();
        factors.add(makeOnset(key.tick));
        factors.add(makePitch(key.key));
        factors.add(makeLoudness(key.velocity));
        factors.add(makeDuration(key.duration));
        factors.add(makeVoice(key.channel));
        return makeDenotator(noteForm, factors);
    }

    /**
     * Creates an Onset denotator from a key event.
     */
    private Denotator makeOnset(long tick) {
        double onset = tempoFactor * tick / ticksPerQuarter;
        return makeDenotator(onsetForm, onset);
    }

    /**
     * Creates a Pitch denotator from a key event.
     */
    private Denotator makePitch(int key) {
        return makeDenotator(pitchForm, new Rational(key));
    }

    /**
     * Creates a Loudness denotator from a key event.
     */
    private Denotator makeLoudness(int velocity) {
        return makeDenotator(loudnessForm, velocity);
    }

    /**
     * Creates a Duration denotator from a key event.
     */
    private Denotator makeDuration(long duration) {
        double d = tempoFactor * duration / ticksPerQuarter;
        return makeDenotator(durationForm, d);
    }

    /**
     * Creates a Voice denotator from a key event.
     */
    private Denotator makeVoice(int channel) {
        return makeDenotator(voiceForm, channel);
    }

    private final int NR_CHANNELS = 16;

    private final int NR_KEYS = 128;

    private final int NR_CONTROLS = 128;

    private Sequence sequence;

    private Track[] tracks;

    private double tempoFactor = 1.0;

    private int ticksPerQuarter;

    private long currentTick;

    private int currentProgram;

    private Key[][] keys;

    private Key[][] controls;

    private LinkedList<Key> notes;

    private LinkedList<Key> pedals;

    private Denotator score;

    private static Repository rep = Repository.systemRepository();

    private static Form onsetForm = rep.getForm("Onset");

    private static Form pitchForm = rep.getForm("Pitch");

    private static Form loudnessForm = rep.getForm("Loudness");

    private static Form durationForm = rep.getForm("Duration");

    private static Form voiceForm = rep.getForm("Voice");

    private static Form noteForm = rep.getForm("Note");

    private static Form scoreForm = rep.getForm("Score");

    @SuppressWarnings("unused")
    private static int[][] sigs = { { 2, 0, 2, 0, 2, 2, 0, 2, 0, 2, 0, 2 }, { 2, 0, 2, 0, 2, 0, 0, 2, 0, 2, 0, 2 }, { 0, 0, 2, 0, 2, 0, 0, 2, 0, 2, 0, 2 }, { 0, 0, 2, 0, 2, 0, 0, 0, 0, 2, 0, 2 }, { 0, 0, 0, 0, 2, 0, 0, 0, 0, 2, 0, 2 }, { 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 2 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2 }, { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 }, { 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0 }, { 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0 }, { 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0 }, { 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0 }, { 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0, 0 }, { 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 0 }, { 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1 } };
}
