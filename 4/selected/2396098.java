package com.musparke.midi.model;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;

/**
 * ex 1110xxxx bb tt pitch wheel change (2000h is normal or no change)
 * bb=bottom (least sig) 7 bits of value
 * tt=top (most sig) 7 bits of value
 * when parse musicxml sound attrubute may fire PitchBend 
 * @author Mao
 *
 */
public class PitchBend implements MusicalFragment {

    private static final long serialVersionUID = 1L;

    private int track;

    private int channel;

    private int bb;

    private int tt;

    public int getBb() {
        return bb;
    }

    public void setBb(int bb) {
        this.bb = bb;
    }

    public int getTt() {
        return tt;
    }

    public void setTt(int tt) {
        this.tt = tt;
    }

    public MidiMessage[] createMidiMessage() throws InvalidMidiDataException {
        return new MidiMessage[] {};
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getTrack() {
        return track;
    }

    public void setTrack(int track) {
        this.track = track;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("PITCHBEND(TR");
        buffer.append(track);
        buffer.append("CH");
        buffer.append(channel);
        buffer.append("BB");
        buffer.append(bb);
        buffer.append("TT");
        buffer.append(tt);
        buffer.append(")");
        return buffer.toString();
    }

    public String toJFugueString() {
        return null;
    }

    public String toMusicXmlString() {
        return null;
    }
}
