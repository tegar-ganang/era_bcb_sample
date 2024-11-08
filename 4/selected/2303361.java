package org.eoti.sound;

import org.eoti.util.SubArray;
import javax.sound.midi.*;
import java.util.*;

public class MIDI {

    protected HashMap<Integer, Note> noteMap;

    protected Synthesizer synth;

    protected Sequencer sequencer;

    protected Sequence sequence44;

    public MIDI() throws MidiUnavailableException, InvalidMidiDataException {
        noteMap = new HashMap<Integer, Note>();
        for (Note note : Note.values()) noteMap.put(note.getMidiNote(), note);
        synth = MidiSystem.getSynthesizer();
        synth.open();
        sequencer = MidiSystem.getSequencer();
        sequencer.open();
        sequence44 = new Sequence(Sequence.PPQ, 4);
    }

    public Note getNote(int midiNote) {
        return noteMap.get(midiNote);
    }

    public ArrayList<Note> getMajorChord(Note baseNote) {
        ArrayList<Note> notes = new ArrayList<Note>();
        notes.add(baseNote);
        notes.add(getNote(baseNote.getMidiNote() + 4));
        notes.add(getNote(baseNote.getMidiNote() + 7));
        return notes;
    }

    public Track create44Track() {
        return sequence44.createTrack();
    }

    public void startNote(Track track, ArrayList<Note> notes, int tick) throws InvalidMidiDataException {
        for (Note note : notes) startNote(track, note, tick);
    }

    public void startNote(Track track, Note note, int tick) throws InvalidMidiDataException {
        ShortMessage message = new ShortMessage();
        message.setMessage(ShortMessage.NOTE_ON, 0, note.getMidiNote(), 90);
        track.add(new MidiEvent(message, tick));
    }

    public void stopNote(Track track, ArrayList<Note> notes, int tick) throws InvalidMidiDataException {
        for (Note note : notes) stopNote(track, note, tick);
    }

    public void stopNote(Track track, Note note, int tick) throws InvalidMidiDataException {
        ShortMessage message = new ShortMessage();
        message.setMessage(ShortMessage.NOTE_OFF, 0, note.getMidiNote(), 90);
        track.add(new MidiEvent(message, tick));
    }

    public void playNote(Track track, ArrayList<Note> notes, int startTick, int stopTick) throws InvalidMidiDataException {
        for (Note note : notes) playNote(track, note, startTick, stopTick);
    }

    public void playNote(Track track, Note note, int startTick, int stopTick) throws InvalidMidiDataException {
        startNote(track, note, startTick);
        stopNote(track, note, stopTick);
    }

    public Note halfStepUp(Note note) {
        return getNote(note.getMidiNote() + 1);
    }

    public Note halfStepDown(Note note) {
        return getNote(note.getMidiNote() - 1);
    }

    public Note octaveUp(Note note) {
        return getNote(note.getMidiNote() + 12);
    }

    public Note octaveDown(Note note) {
        return getNote(note.getMidiNote() - 12);
    }

    public ArrayList<Instrument> getAvailableInstruments() {
        ArrayList<Instrument> instruments = new ArrayList<Instrument>();
        for (Instrument instrument : synth.getAvailableInstruments()) instruments.add(instrument);
        return instruments;
    }

    public void startSequencer44(float BPM) throws InvalidMidiDataException {
        sequencer.setSequence(sequence44);
        sequencer.start();
        sequencer.setTempoInBPM(BPM);
    }

    public void setInstrument(int instrument) {
        synth.getChannels()[0].programChange(instrument);
    }

    public static SubArray<Note> pianoNotes() {
        return new SubArray<Note>(Note.A1.ordinal(), Note.C8.ordinal(), Note.values());
    }

    public static Note middleC() {
        return Note.C4;
    }

    public static class Test {

        public static void showUsage() {
            System.out.println("USAGE: java org.eoti.sound.MIDI$Test option");
            System.out.println("Where option can be 'play' or 'piano'");
            System.exit(0);
        }

        public static void main(String[] args) {
            if (args.length != 1) showUsage();
            if (args[0].equalsIgnoreCase("piano")) {
                for (Note note : MIDI.pianoNotes()) System.out.format("%03d % 5.2f %s\n", note.getMidiNote(), note.getHertz(), note.getName());
                System.exit(0);
            }
            if (!args[0].equalsIgnoreCase("play")) showUsage();
            try {
                MIDI midi = new MIDI();
                ArrayList<Instrument> instruments = midi.getAvailableInstruments();
                int i = 0;
                for (Instrument instrument : instruments) {
                    Patch p = instrument.getPatch();
                    System.out.print("[Bank: " + p.getBank() + "]");
                    System.out.print("[Program: " + p.getProgram() + "]");
                    System.out.print("[Instrument: " + (i++) + "]");
                    System.out.println("\t" + instrument.getName());
                }
                Track track = midi.create44Track();
                midi.playNote(track, Note.B4, 1, 3);
                midi.playNote(track, Note.A4, 2, 4);
                midi.playNote(track, Note.G4, 3, 5);
                midi.playNote(track, Note.A4, 4, 6);
                midi.playNote(track, Note.B4, 5, 5);
                midi.playNote(track, Note.B4, 6, 6);
                midi.playNote(track, Note.B4, 7, 7);
                midi.playNote(track, Note.A4, 9, 9);
                midi.playNote(track, Note.A4, 10, 10);
                midi.playNote(track, Note.A4, 11, 11);
                midi.playNote(track, Note.B4, 13, 13);
                midi.playNote(track, Note.B4, 14, 14);
                midi.playNote(track, Note.B4, 15, 15);
                midi.playNote(track, Note.B4, 17, 19);
                midi.playNote(track, Note.A4, 18, 20);
                midi.playNote(track, Note.G4, 19, 21);
                midi.playNote(track, Note.A4, 20, 22);
                midi.playNote(track, Note.B4, 21, 21);
                midi.playNote(track, Note.B4, 22, 22);
                midi.playNote(track, Note.B4, 23, 23);
                midi.playNote(track, Note.A4, 25, 25);
                midi.playNote(track, Note.A4, 26, 26);
                midi.playNote(track, Note.B4, 27, 29);
                midi.playNote(track, Note.A4, 28, 30);
                midi.playNote(track, Note.G4, 29, 31);
                midi.startSequencer44(60);
            } catch (Exception e) {
                System.out.println("Exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
