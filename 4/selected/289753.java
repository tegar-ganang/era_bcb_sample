package com.sbook.canyonjam;

import java.io.IOException;
import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Synthesizer;
import com.jme.math.Vector3f;
import com.sbook.canyonjam.scale.CMajor;
import com.sbook.canyonjam.scale.Scale;

/**
 * @author Skye Book
 *
 */
public class TerrainSynth {

    private Synthesizer midiSynth;

    private MidiChannel[] midiChannels;

    private Instrument[] instruments;

    private float lowest = 0;

    private float highest = 0;

    private float difference;

    private float valuePer;

    private boolean started = false;

    private Scale scale = new CMajor();

    /**
	 * @throws MidiUnavailableException 
	 * @throws IOException 
	 * @throws InvalidMidiDataException 
	 * 
	 */
    public TerrainSynth(float lowest, float highest) throws MidiUnavailableException, InvalidMidiDataException, IOException {
        this.lowest = lowest;
        this.highest = highest;
        calculateDifference();
        midiSynth = MidiSystem.getSynthesizer();
        midiSynth.open();
        midiChannels = midiSynth.getChannels();
        instruments = midiSynth.getDefaultSoundbank().getInstruments();
        for (int i = 0; i < instruments.length; i++) {
        }
        midiSynth.loadInstrument(instruments[185]);
    }

    public void update(float heightHere, Vector3f cameraPosition) {
        if (heightHere == Float.NaN) {
            for (MidiChannel channel : midiChannels) {
                channel.allNotesOff();
            }
            return;
        }
        midiChannels[1].allNotesOff();
        midiChannels[1].controlChange(0x07, 100);
        midiChannels[1].noteOn(getValueInRange(heightHere), 100);
    }

    private int findNoteInScale(float value) {
        float split = difference / scale.getNotes().length;
        int note = (int) ((value - lowest) / split);
        System.out.println(note);
        if (note > scale.getNotes().length - 1) {
            note = scale.getNotes().length - 1;
        } else if (note < 0) {
            note = 0;
        }
        return scale.getNotes()[note];
    }

    private void calculateDifference() {
        difference = highest - lowest;
        valuePer = difference / 127;
    }

    private int getValueInRange(float value) {
        int returnable = (int) ((value - lowest) / valuePer);
        return returnable;
    }
}
