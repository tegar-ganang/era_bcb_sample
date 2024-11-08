package com.musparke.midi.model;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;
import com.musparke.midi.PercussionBank;
import com.musparke.midi.util.MusicTool;

/**
 * NoteWrapper wrapped a music note,because in midi a note contains
 * a Note-On and a compared Note-Off event.the wrapper wrapped the 
 * music note information. 
 * @author Mao
 *
 */
public class Note implements MusicalFragment {

    private static final long serialVersionUID = 1L;

    private boolean isChord;

    private boolean isUnpitch;

    private int key;

    private int channel;

    private int track;

    private int duration;

    private int velocity;

    /**
	 * 
	 * @param isChord is a chord?
	 * @param key midi value 0~127
	 * @param track track number
	 * @param duration 
	 */
    public Note(boolean isChord, int key, int track, int duration) {
        super();
        this.isChord = isChord;
        this.key = key;
        this.track = track;
        this.duration = duration;
        isUnpitch = false;
    }

    public MidiMessage[] createMidiMessage() throws InvalidMidiDataException {
        ShortMessage on = new ShortMessage();
        ShortMessage off = new ShortMessage();
        on.setMessage(ShortMessage.NOTE_ON, channel, key, velocity);
        off.setMessage(ShortMessage.NOTE_OFF, channel, key, velocity);
        return new MidiMessage[] { on, off };
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public int getVelocity() {
        return velocity;
    }

    public void setVelocity(int velocity) {
        this.velocity = velocity;
    }

    public boolean isChord() {
        return isChord;
    }

    public void setChord(boolean isChord) {
        this.isChord = isChord;
    }

    public int getTrack() {
        return track;
    }

    public void setTrackNO(int track) {
        this.track = track;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("NOTE(");
        buffer.append((isChord ? "CH" : ""));
        buffer.append(isUnpitch ? "UPI" + PercussionBank.getPercussion(key) : "PI");
        buffer.append(MusicTool.getPitchString(key));
        buffer.append("V");
        buffer.append(velocity);
        buffer.append("T");
        buffer.append(track);
        buffer.append(")");
        return buffer.toString();
    }

    public boolean isUnpitch() {
        return isUnpitch;
    }

    public void setUnpitch(boolean isUnpitch) {
        this.isUnpitch = isUnpitch;
    }

    public void setTrack(int track) {
        this.track = track;
    }

    public String toJFugueString() {
        return null;
    }

    public String toMusicXmlString() {
        return null;
    }
}
