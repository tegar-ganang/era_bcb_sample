package org.herac.tuxguitar.io.tg.v07;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.herac.tuxguitar.gm.GMChannelRoute;
import org.herac.tuxguitar.io.base.TGFileFormat;
import org.herac.tuxguitar.io.base.TGFileFormatException;
import org.herac.tuxguitar.io.base.TGInputStreamBase;
import org.herac.tuxguitar.song.factory.TGFactory;
import org.herac.tuxguitar.song.models.TGBeat;
import org.herac.tuxguitar.song.models.TGChannel;
import org.herac.tuxguitar.song.models.TGChannelParameter;
import org.herac.tuxguitar.song.models.TGColor;
import org.herac.tuxguitar.song.models.TGDuration;
import org.herac.tuxguitar.song.models.TGMeasure;
import org.herac.tuxguitar.song.models.TGMeasureHeader;
import org.herac.tuxguitar.song.models.TGNote;
import org.herac.tuxguitar.song.models.TGNoteEffect;
import org.herac.tuxguitar.song.models.TGSong;
import org.herac.tuxguitar.song.models.TGString;
import org.herac.tuxguitar.song.models.TGTempo;
import org.herac.tuxguitar.song.models.TGTimeSignature;
import org.herac.tuxguitar.song.models.TGTrack;
import org.herac.tuxguitar.song.models.TGDivisionType;
import org.herac.tuxguitar.song.models.TGVoice;
import org.herac.tuxguitar.song.models.effects.TGEffectBend;

/**
 * @author julian
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TGInputStream implements TGInputStreamBase {

    private static final String TG_VERSION = "TG_DEVEL-0.01";

    private DataInputStream dataInputStream;

    private TGFactory factory;

    private String version;

    public TGInputStream() {
        super();
    }

    public void init(TGFactory factory, InputStream stream) {
        this.factory = factory;
        this.dataInputStream = new DataInputStream(stream);
        this.version = null;
    }

    public TGFileFormat getFileFormat() {
        return new TGFileFormat("TuxGuitar", "*.tg");
    }

    public boolean isSupportedVersion(String version) {
        return (version.equals(TG_VERSION));
    }

    public boolean isSupportedVersion() {
        try {
            readVersion();
            return isSupportedVersion(this.version);
        } catch (Exception e) {
            return false;
        } catch (Error e) {
            return false;
        }
    }

    private void readVersion() {
        if (this.version == null) {
            this.version = readString();
        }
    }

    public TGSong readSong() throws TGFileFormatException {
        try {
            if (this.isSupportedVersion()) {
                TGSong song = this.read();
                this.dataInputStream.close();
                return song;
            }
            throw new TGFileFormatException("Unsopported Version");
        } catch (Throwable throwable) {
            throw new TGFileFormatException(throwable);
        }
    }

    private TGSong read() {
        TGSong song = this.factory.newSong();
        song.setName(readString());
        song.setArtist(readString());
        song.setAlbum(readString());
        song.setAuthor(readString());
        int trackCount = readInt();
        for (int i = 0; i < trackCount; i++) {
            song.addTrack(readTrack(song));
        }
        return song;
    }

    private TGTrack readTrack(TGSong song) {
        TGTrack track = this.factory.newTrack();
        track.setNumber((int) readLong());
        track.setName(readString());
        readChannel(song, track);
        track.setSolo(readBoolean());
        track.setMute(readBoolean());
        int measureCount = readInt();
        if (song.countMeasureHeaders() == 0) {
            for (int i = 0; i < measureCount; i++) {
                TGMeasureHeader header = this.factory.newHeader();
                song.addMeasureHeader(header);
            }
        }
        for (int i = 0; i < measureCount; i++) {
            track.addMeasure(readMeasure(song.getMeasureHeader(i)));
        }
        int stringCount = readInt();
        for (int i = 0; i < stringCount; i++) {
            track.getStrings().add(readInstrumentString());
        }
        readColor(track.getColor());
        return track;
    }

    private TGMeasure readMeasure(TGMeasureHeader header) {
        TGMeasure measure = this.factory.newMeasure(header);
        header.setNumber(readInt());
        header.setStart((TGDuration.QUARTER_TIME * readLong() / 1000));
        int noteCount = readInt();
        TGBeat previous = null;
        for (int i = 0; i < noteCount; i++) {
            previous = readNote(measure, previous);
        }
        int silenceCount = readInt();
        previous = null;
        for (int i = 0; i < silenceCount; i++) {
            previous = readSilence(measure, previous);
        }
        readTimeSignature(header.getTimeSignature());
        readTempo(header.getTempo());
        measure.setClef(readInt());
        measure.setKeySignature(readInt());
        header.setRepeatOpen(readBoolean());
        header.setRepeatClose(readInt());
        return measure;
    }

    private TGBeat readNote(TGMeasure measure, TGBeat previous) {
        TGBeat beat = previous;
        int value = readInt();
        long start = (TGDuration.QUARTER_TIME * readLong() / 1000);
        if (beat == null || beat.getStart() != start) {
            beat = this.factory.newBeat();
            beat.setStart(start);
            measure.addBeat(beat);
        }
        TGVoice voice = beat.getVoice(0);
        voice.setEmpty(false);
        readDuration(voice.getDuration());
        TGNote note = this.factory.newNote();
        note.setValue(value);
        note.setVelocity(readInt());
        note.setString(readInt());
        note.setTiedNote(readBoolean());
        readNoteEffect(note.getEffect());
        voice.addNote(note);
        return beat;
    }

    private void readChannel(TGSong song, TGTrack track) {
        TGChannel channel = this.factory.newChannel();
        TGChannelParameter gmChannel1Param = this.factory.newChannelParameter();
        TGChannelParameter gmChannel2Param = this.factory.newChannelParameter();
        int channel1 = readShort();
        gmChannel1Param.setKey(GMChannelRoute.PARAMETER_GM_CHANNEL_1);
        gmChannel1Param.setValue(Integer.toString(channel1));
        int channel2 = readShort();
        gmChannel2Param.setKey(GMChannelRoute.PARAMETER_GM_CHANNEL_2);
        gmChannel2Param.setValue(Integer.toString(channel2));
        channel.setBank(channel1 == 9 ? TGChannel.DEFAULT_PERCUSSION_BANK : TGChannel.DEFAULT_BANK);
        channel.setProgram(readShort());
        channel.setVolume(readShort());
        channel.setBalance(readShort());
        channel.setChorus(readShort());
        channel.setReverb(readShort());
        channel.setPhaser(readShort());
        channel.setTremolo(readShort());
        for (int i = 0; i < song.countChannels(); i++) {
            TGChannel channelAux = song.getChannel(i);
            for (int n = 0; n < channelAux.countParameters(); n++) {
                TGChannelParameter channelParameter = channelAux.getParameter(n);
                if (channelParameter.getKey().equals(GMChannelRoute.PARAMETER_GM_CHANNEL_1)) {
                    if (Integer.toString(channel1).equals(channelParameter.getValue())) {
                        channel.setChannelId(channelAux.getChannelId());
                    }
                }
            }
        }
        if (channel.getChannelId() <= 0) {
            channel.setChannelId(song.countChannels() + 1);
            channel.setName(("#" + channel.getChannelId()));
            channel.addParameter(gmChannel1Param);
            channel.addParameter(gmChannel2Param);
            song.addChannel(channel);
        }
        track.setChannelId(channel.getChannelId());
    }

    private TGBeat readSilence(TGMeasure measure, TGBeat previous) {
        TGBeat beat = previous;
        long start = (TGDuration.QUARTER_TIME * readLong() / 1000);
        if (beat == null || beat.getStart() != start) {
            beat = this.factory.newBeat();
            beat.setStart(start);
            measure.addBeat(beat);
        }
        TGVoice voice = beat.getVoice(0);
        voice.setEmpty(false);
        readDuration(voice.getDuration());
        return beat;
    }

    private TGString readInstrumentString() {
        TGString string = this.factory.newString();
        string.setNumber(readInt());
        string.setValue(readInt());
        return string;
    }

    private void readTempo(TGTempo tempo) {
        tempo.setValue(readInt());
    }

    private void readTimeSignature(TGTimeSignature timeSignature) {
        timeSignature.setNumerator(readInt());
        readDuration(timeSignature.getDenominator());
    }

    private void readDuration(TGDuration duration) {
        duration.setValue(readInt());
        duration.setDotted(readBoolean());
        duration.setDoubleDotted(readBoolean());
        readDivisionType(duration.getDivision());
    }

    private void readDivisionType(TGDivisionType divisionType) {
        divisionType.setEnters(readInt());
        divisionType.setTimes(readInt());
    }

    private void readNoteEffect(TGNoteEffect effect) {
        effect.setVibrato(readBoolean());
        if (readBoolean()) {
            effect.setBend(readBendEffect());
        }
        effect.setDeadNote(readBoolean());
        effect.setSlide(readBoolean());
        effect.setHammer(readBoolean());
    }

    private TGEffectBend readBendEffect() {
        TGEffectBend bend = this.factory.newEffectBend();
        int count = readInt();
        for (int i = 0; i < count; i++) {
            int position = readInt();
            int value = readInt();
            bend.addPoint(position, ((value > 0) ? value / 2 : value));
        }
        return bend;
    }

    private void readColor(TGColor color) {
        color.setR(readInt());
        color.setG(readInt());
        color.setB(readInt());
    }

    private short readShort() {
        try {
            return this.dataInputStream.readShort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int readInt() {
        try {
            return this.dataInputStream.readInt();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private long readLong() {
        try {
            return this.dataInputStream.readLong();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String readString() {
        try {
            int length = this.dataInputStream.read();
            char[] chars = new char[length];
            for (int i = 0; i < chars.length; i++) {
                chars[i] = this.dataInputStream.readChar();
            }
            return String.copyValueOf(chars);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean readBoolean() {
        try {
            return this.dataInputStream.readBoolean();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
}
