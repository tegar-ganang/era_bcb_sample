package org.herac.tuxguitar.io.tg.v08;

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
import org.herac.tuxguitar.song.models.TGMarker;
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
public class TGInputStream extends TGStream implements TGInputStreamBase {

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
        } catch (Throwable throwable) {
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
        int headerCount = readShort();
        long headerStart = TGDuration.QUARTER_TIME;
        TGMeasureHeader lastHeader = null;
        for (int i = 0; i < headerCount; i++) {
            TGMeasureHeader header = readMeasureHeader(i + 1, headerStart, lastHeader);
            song.addMeasureHeader(header);
            headerStart += header.getLength();
            lastHeader = header;
        }
        int trackCount = readByte();
        for (int i = 0; i < trackCount; i++) {
            song.addTrack(readTrack(i + 1, song));
        }
        return song;
    }

    private TGTrack readTrack(int number, TGSong song) {
        TGTrack track = this.factory.newTrack();
        track.setNumber(number);
        track.setName(readString());
        readChannel(song, track);
        int measureCount = song.countMeasureHeaders();
        TGMeasure lastMeasure = null;
        for (int i = 0; i < measureCount; i++) {
            TGMeasure measure = readMeasure(song.getMeasureHeader(i), lastMeasure);
            track.addMeasure(measure);
            lastMeasure = measure;
        }
        int stringCount = readByte();
        for (int i = 0; i < stringCount; i++) {
            track.getStrings().add(readInstrumentString(i + 1));
        }
        track.setOffset((TGTrack.MIN_OFFSET + readByte()));
        readColor(track.getColor());
        return track;
    }

    private TGMeasureHeader readMeasureHeader(int number, long start, TGMeasureHeader lastMeasureHeader) {
        int header = readHeader();
        TGMeasureHeader measureHeader = this.factory.newHeader();
        measureHeader.setNumber(number);
        measureHeader.setStart(start);
        if (((header & MEASURE_HEADER_TIMESIGNATURE) != 0)) {
            readTimeSignature(measureHeader.getTimeSignature());
        } else if (lastMeasureHeader != null) {
            lastMeasureHeader.getTimeSignature().copy(measureHeader.getTimeSignature());
        }
        if (((header & MEASURE_HEADER_TEMPO) != 0)) {
            readTempo(measureHeader.getTempo());
        } else if (lastMeasureHeader != null) {
            lastMeasureHeader.getTempo().copy(measureHeader.getTempo());
        }
        measureHeader.setRepeatOpen(((header & MEASURE_HEADER_OPEN_REPEAT) != 0));
        if (((header & MEASURE_HEADER_CLOSE_REPEAT) != 0)) {
            measureHeader.setRepeatClose(readShort());
        }
        if (((header & MEASURE_HEADER_MARKER) != 0)) {
            measureHeader.setMarker(readMarker(number));
        }
        measureHeader.setTripletFeel(((lastMeasureHeader != null) ? lastMeasureHeader.getTripletFeel() : TGMeasureHeader.TRIPLET_FEEL_NONE));
        if (((header & MEASURE_HEADER_TRIPLET_FEEL) != 0)) {
            measureHeader.setTripletFeel(readByte());
        }
        return measureHeader;
    }

    private TGMeasure readMeasure(TGMeasureHeader measureHeader, TGMeasure lastMeasure) {
        int header = readHeader();
        TGMeasure measure = this.factory.newMeasure(measureHeader);
        TGBeat previous = null;
        int componentCount = readShort();
        for (int i = 0; i < componentCount; i++) {
            previous = readComponent(measure, previous);
        }
        measure.setClef((lastMeasure == null) ? TGMeasure.CLEF_TREBLE : lastMeasure.getClef());
        if (((header & MEASURE_CLEF) != 0)) {
            measure.setClef(readByte());
        }
        measure.setKeySignature((lastMeasure == null) ? 0 : lastMeasure.getKeySignature());
        if (((header & MEASURE_KEYSIGNATURE) != 0)) {
            measure.setKeySignature(readByte());
        }
        return measure;
    }

    private void readChannel(TGSong song, TGTrack track) {
        int header = readHeader();
        TGChannel channel = this.factory.newChannel();
        TGChannelParameter gmChannel1Param = this.factory.newChannelParameter();
        TGChannelParameter gmChannel2Param = this.factory.newChannelParameter();
        int channel1 = (readByte() & 0xff);
        gmChannel1Param.setKey(GMChannelRoute.PARAMETER_GM_CHANNEL_1);
        gmChannel1Param.setValue(Integer.toString(channel1));
        int channel2 = (readByte() & 0xff);
        gmChannel2Param.setKey(GMChannelRoute.PARAMETER_GM_CHANNEL_2);
        gmChannel2Param.setValue(Integer.toString(channel2));
        channel.setBank(channel1 == 9 ? TGChannel.DEFAULT_PERCUSSION_BANK : TGChannel.DEFAULT_BANK);
        channel.setProgram((short) readByte());
        channel.setVolume((short) readByte());
        channel.setBalance((short) readByte());
        channel.setChorus((short) readByte());
        channel.setReverb((short) readByte());
        channel.setPhaser((short) readByte());
        channel.setTremolo((short) readByte());
        track.setSolo(((header & CHANNEL_SOLO) != 0));
        track.setMute(((header & CHANNEL_MUTE) != 0));
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

    private TGBeat readComponent(TGMeasure measure, TGBeat previous) {
        TGBeat beat = previous;
        int header = readHeader();
        if (beat == null) {
            beat = this.factory.newBeat();
            beat.setStart(measure.getStart());
            measure.addBeat(beat);
        } else if (((header & COMPONENT_NEXT_BEAT) != 0)) {
            beat = this.factory.newBeat();
            beat.setStart(previous.getStart() + previous.getVoice(0).getDuration().getTime());
            measure.addBeat(beat);
        }
        TGVoice voice = beat.getVoice(0);
        voice.setEmpty(false);
        if (((header & COMPONENT_NEXT_DURATION) != 0)) {
            readDuration(voice.getDuration());
        } else if (previous != null && !previous.equals(beat)) {
            previous.getVoice(0).getDuration().copy(voice.getDuration());
        }
        if (((header & COMPONENT_NOTE) != 0)) {
            TGNote note = this.factory.newNote();
            note.setValue(readByte());
            note.setVelocity(readByte());
            note.setString(readByte());
            note.setTiedNote(((header & COMPONENT_TIEDNOTE) != 0));
            if (((header & COMPONENT_EFFECT) != 0)) {
                readNoteEffect(note.getEffect());
            }
            voice.addNote(note);
        }
        return beat;
    }

    private TGString readInstrumentString(int number) {
        TGString string = this.factory.newString();
        string.setNumber(number);
        string.setValue(readByte());
        return string;
    }

    private void readTempo(TGTempo tempo) {
        tempo.setValue(readShort());
    }

    private void readTimeSignature(TGTimeSignature timeSignature) {
        timeSignature.setNumerator(readByte());
        readDuration(timeSignature.getDenominator());
    }

    private void readDuration(TGDuration duration) {
        int header = readHeader();
        duration.setDotted((header & DURATION_DOTTED) != 0);
        duration.setDoubleDotted((header & DURATION_DOUBLE_DOTTED) != 0);
        duration.setValue(readByte());
        if (((header & DURATION_TUPLETO) != 0)) {
            readDivisionType(duration.getDivision());
        }
    }

    private void readDivisionType(TGDivisionType divisionType) {
        divisionType.setEnters(readByte());
        divisionType.setTimes(readByte());
    }

    private void readNoteEffect(TGNoteEffect effect) {
        int header = readHeader();
        effect.setVibrato(((header & EFFECT_VIBRATO) != 0));
        effect.setDeadNote(((header & EFFECT_DEAD_NOTE) != 0));
        effect.setSlide(((header & EFFECT_SLIDE) != 0));
        effect.setHammer(((header & EFFECT_HAMMER) != 0));
        if (((header & EFFECT_BEND) != 0)) {
            effect.setBend(readBendEffect());
        }
    }

    private TGEffectBend readBendEffect() {
        TGEffectBend bend = this.factory.newEffectBend();
        int count = readByte();
        for (int i = 0; i < count; i++) {
            int position = readByte();
            int value = readByte();
            bend.addPoint(position, ((value > 0) ? value / 2 : value));
        }
        return bend;
    }

    private TGMarker readMarker(int measure) {
        TGMarker marker = this.factory.newMarker();
        marker.setMeasure(measure);
        marker.setTitle(readString());
        readColor(marker.getColor());
        return marker;
    }

    private void readColor(TGColor color) {
        color.setR(readShort());
        color.setG(readShort());
        color.setB(readShort());
    }

    private int readByte() {
        try {
            return this.dataInputStream.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int readHeader() {
        try {
            return this.dataInputStream.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private short readShort() {
        try {
            return this.dataInputStream.readShort();
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
}
