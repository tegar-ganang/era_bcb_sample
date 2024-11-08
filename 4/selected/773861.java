package com.petersalomonsen.pjsynth;

import com.petersalomonsen.pjsynth.control.ChannelControlMaster;
import com.petersalomonsen.pjsynth.note.Note;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.Patch;

/**
 *
 * @author Peter Johan Salomonsen
 */
public class PJSynthMidiChannel implements MidiChannel {

    Note[] notes = new Note[127];

    PJSynthPatch patch = new PJSynthPatch(0, 0);

    PJSynth synth;

    int pitchBend = 8192;

    ArrayList<Note> newNotes = new ArrayList<Note>();

    HashSet<Note> playingNotes = new HashSet<Note>();

    ArrayList<Note> finishedNotes = new ArrayList<Note>();

    float[] midiChannelFloatBuffer;

    public PJSynthMidiChannel(PJSynth synth, int floatBufferSize) {
        this.synth = synth;
        midiChannelFloatBuffer = new float[floatBufferSize];
    }

    public void noteOn(int noteNumber, int velocity) {
        if (notes[noteNumber] != null) noteOff(noteNumber, velocity);
        if (velocity > 0) {
            try {
                Note note = (Note) synth.getInstrumentByPatch(patch).getDataClass().newInstance();
                note.setNoteNumber(noteNumber);
                note.setVelocity(velocity);
                note.setMidiChannel(this);
                notes[noteNumber] = note;
                note.startPlaying();
            } catch (Exception ex) {
                Logger.getLogger(PJSynth.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void noteOff(int noteNumber, int velocity) {
        if (notes[noteNumber] != null) {
            notes[noteNumber].release(velocity);
            notes[noteNumber] = null;
        }
    }

    public void noteOff(int noteNumber) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setPolyPressure(int noteNumber, int pressure) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getPolyPressure(int noteNumber) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setChannelPressure(int pressure) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getChannelPressure() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void controlChange(int controller, int value) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getController(int controller) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void programChange(int program) {
        programChange(patch.getBank(), program);
    }

    public void programChange(int bank, int program) {
        patch = new PJSynthPatch(bank, program);
    }

    public int getProgram() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setPitchBend(int bend) {
        this.pitchBend = bend;
    }

    public int getPitchBend() {
        return pitchBend;
    }

    public void resetAllControllers() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void allNotesOff() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void allSoundOff() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean localControl(boolean on) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setMono(boolean on) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean getMono() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setOmni(boolean on) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean getOmni() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setMute(boolean mute) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean getMute() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setSolo(boolean soloState) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean getSolo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public PJSynth getSynth() {
        return synth;
    }

    public void addPlayingNote(Note note) {
        newNotes.add(note);
    }

    public void removePlayingNote(Note note) {
        finishedNotes.add(note);
    }

    void fillBuffer(float[] floatBuffer, int numberOfFrames, int channels) {
        ChannelControlMaster ccm = synth.getChannelControlMasterByPatch(patch);
        if (ccm != null) {
            ccm.fillBufferBeforeNotes(floatBuffer, numberOfFrames, channels);
        }
        while (newNotes.size() > 0) playingNotes.add(newNotes.remove(0));
        while (finishedNotes.size() > 0) playingNotes.remove(finishedNotes.remove(0));
        if (!playingNotes.isEmpty()) {
            for (int n = 0; n < midiChannelFloatBuffer.length; n++) midiChannelFloatBuffer[n] = 0;
            for (Note note : playingNotes) note.fillBuffer(midiChannelFloatBuffer, numberOfFrames, channels);
            for (int n = 0; n < midiChannelFloatBuffer.length; n++) {
                floatBuffer[n] += midiChannelFloatBuffer[n];
            }
        }
        if (ccm != null) {
            ccm.fillBufferAfterNotes(floatBuffer, numberOfFrames, channels);
        }
    }
}
