package com.musparke.midi.model;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;

/**
 * Musparke internal command format, no MIDI event is created by ChannelProgram.<br>
 * In musparke MIDI engine, each of 16 channels will be arranged <br>
 * a music xml score part.In music xml, score part size is equals to part size.<br>
 * each score-part is abstracted to a track, these score-part will be arranged to <br>
 * channel 0-15 by the score-part's natural <br>
 * order. <br>
 * if part-list/score-part/midi-instrument/midi-channel defined, <br>
 * a ChannelProgram will be created to change<br>
 * the default channel for the score-pat.<br>
 * for example,<br>
 * &lt;part-list><br>
 *  &lt;score-part id="P1"&gt;<br>
 *      &lt;part-name print-object="no"&gt;Voice&lt;/part-name&gt;<br>
 *          &lt;score-instrument id="P1-I3"&gt;<br>
 *             &lt;instrument-name&gt;Voice&lt;/instrument-name&gt;<br>
      &lt;/score-instrument&gt;<br>
      &lt;midi-instrument id="P1-I3"&gt;<br>
        &lt;midi-channel&gt;4&lt;/midi-channel&gt;<br>
        &lt;midi-program&gt;55&lt;/midi-program&gt;<br>
      &lt;/midi-instrument&gt;<br>
    &lt;/score-part&gt;<br>
    &lt;score-part id="P2"&gt;<br>
      &lt;part-name print-object="no"&gt;Guitar&lt;/part-name&gt;<br>
      &lt;score-instrument id="P2-I2"&gt;<br>
        &lt;instrument-name&gt;Acoustic Guitar (steel)&lt;/instrument-name&gt;<br>
      &lt;/score-instrument&gt;<br>
      &lt;midi-instrument id="P2-I2"&gt;<br>
        &lt;midi-channel&gt;5&lt;/midi-channel&gt;<br>
        &lt;midi-program&gt;26&lt;/midi-program&gt;<br>
      &lt;/midi-instrument&gt;<br>
    &lt;/score-part&gt;<br>
  &lt;/part-list&gt;<br>
  there are two score-part node, so when creating<br>
  track 0 -> [MIDI channel 0]<br>
  track 1 -> [MIDI channel 1]<br>
  when &lt;midi-channel&gt;1&lt;/midi-channel&gt; and &lt;midi-channel&gt;2&lt;/midi-channel&gt; node are traversed,<br>
  two ChannelProgram will be created, after the ChannelProgramEvent fired, the track are arranged as<br>
  follows,<br>
  track 0 -> [MIDI channel 3]<br>
  track 1 -> [MIDI channel 4]<br>
 * <p>
 * Standard MIDI specialization:Null
 * </p>
 * <p>
 * Java MIDI Message:Null
 * </p>
 * <p>
 * MusicXML specialization:
 * part-list/score-part/midi-instrument/midi-channel,
 * 
 * </p>
 * @author Alex Mao
 *
 */
public class ChannelProgram implements MusicalFragment {

    private static final long serialVersionUID = 1L;

    /**
	 * track number, as standard MIDI specialization, 0-127
	 */
    private int track;

    /**
	 * channel number, as standard MIDI specialization, 0-15
	 */
    private int channel;

    /**
	 * @param track, 0-127
	 * @param channel, 0-15
	 */
    public ChannelProgram(int track, int channel) {
        super();
        this.track = track;
        this.channel = channel;
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
        return new StringBuilder().append("<midi-channel>").append(channel).append("</midi-channel>").toString();
    }
}
