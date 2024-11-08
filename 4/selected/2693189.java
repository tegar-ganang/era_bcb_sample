package cn.edu.wuse.musicxml.sound;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;

/**
 * 在本工程的实现中,先按照自然顺序把part-list中的
 * 每个score-part抽象成一条track
 * @author Mao
 *
 */
public class ChannelProgram implements Pronounceable {

    private static final long serialVersionUID = 1L;

    private int track;

    private int channel;

    public MidiMessage[] createMessage() throws InvalidMidiDataException {
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

    public ChannelProgram(int track, int channel) {
        super();
        this.track = track;
        this.channel = channel;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("CHANNEL(T");
        buffer.append(track);
        buffer.append("C");
        buffer.append(channel);
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
