package org.herac.tuxguitar.io.tg.v11;

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
import org.herac.tuxguitar.song.models.TGChord;
import org.herac.tuxguitar.song.models.TGColor;
import org.herac.tuxguitar.song.models.TGDuration;
import org.herac.tuxguitar.song.models.TGLyric;
import org.herac.tuxguitar.song.models.TGMarker;
import org.herac.tuxguitar.song.models.TGMeasure;
import org.herac.tuxguitar.song.models.TGMeasureHeader;
import org.herac.tuxguitar.song.models.TGNote;
import org.herac.tuxguitar.song.models.TGNoteEffect;
import org.herac.tuxguitar.song.models.TGSong;
import org.herac.tuxguitar.song.models.TGString;
import org.herac.tuxguitar.song.models.TGStroke;
import org.herac.tuxguitar.song.models.TGTempo;
import org.herac.tuxguitar.song.models.TGText;
import org.herac.tuxguitar.song.models.TGTimeSignature;
import org.herac.tuxguitar.song.models.TGTrack;
import org.herac.tuxguitar.song.models.TGDivisionType;
import org.herac.tuxguitar.song.models.TGVoice;
import org.herac.tuxguitar.song.models.effects.TGEffectBend;
import org.herac.tuxguitar.song.models.effects.TGEffectGrace;
import org.herac.tuxguitar.song.models.effects.TGEffectHarmonic;
import org.herac.tuxguitar.song.models.effects.TGEffectTremoloBar;
import org.herac.tuxguitar.song.models.effects.TGEffectTremoloPicking;
import org.herac.tuxguitar.song.models.effects.TGEffectTrill;

/**
 * @author julian
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class TGInputStream extends TGStream implements TGInputStreamBase {

    private DataInputStream dataInputStream;

    private String version;

    private TGFactory factory;

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
        return (version.equals(TG_FORMAT_VERSION));
    }

    public boolean isSupportedVersion() {
        try {
            readVersion();
            return isSupportedVersion(this.version);
        } catch (Throwable throwable) {
            return false;
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

    private void readVersion() {
        if (this.version == null) {
            this.version = readUnsignedByteString();
        }
    }

    private TGSong read() {
        TGSong song = this.factory.newSong();
        song.setName(readUnsignedByteString());
        song.setArtist(readUnsignedByteString());
        song.setAlbum(readUnsignedByteString());
        song.setAuthor(readUnsignedByteString());
        int headerCount = readShort();
        TGMeasureHeader lastHeader = null;
        long headerStart = TGDuration.QUARTER_TIME;
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
        int header = readHeader();
        TGTrack track = this.factory.newTrack();
        track.setNumber(number);
        track.setName(readUnsignedByteString());
        track.setSolo((header & TRACK_SOLO) != 0);
        track.setMute((header & TRACK_MUTE) != 0);
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
        track.setOffset(TGTrack.MIN_OFFSET + readByte());
        readRGBColor(track.getColor());
        if (((header & TRACK_LYRICS) != 0)) {
            readLyrics(track.getLyrics());
        }
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
        measureHeader.setRepeatOpen((header & MEASURE_HEADER_REPEAT_OPEN) != 0);
        if (((header & MEASURE_HEADER_REPEAT_CLOSE) != 0)) {
            measureHeader.setRepeatClose(readShort());
        }
        if (((header & MEASURE_HEADER_REPEAT_ALTERNATIVE) != 0)) {
            measureHeader.setRepeatAlternative(readByte());
        }
        if (((header & MEASURE_HEADER_MARKER) != 0)) {
            measureHeader.setMarker(readMarker(number));
        }
        measureHeader.setTripletFeel((lastMeasureHeader != null) ? lastMeasureHeader.getTripletFeel() : TGMeasureHeader.TRIPLET_FEEL_NONE);
        if (((header & MEASURE_HEADER_TRIPLET_FEEL) != 0)) {
            measureHeader.setTripletFeel(readByte());
        }
        return measureHeader;
    }

    private TGMeasure readMeasure(TGMeasureHeader measureHeader, TGMeasure lastMeasure) {
        int header = readHeader();
        TGMeasure measure = this.factory.newMeasure(measureHeader);
        TGBeatData data = new TGBeatData(measure);
        readBeats(measure, data);
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
        channel.setProgram(readByte());
        channel.setVolume(readByte());
        channel.setBalance(readByte());
        channel.setChorus(readByte());
        channel.setReverb(readByte());
        channel.setPhaser(readByte());
        channel.setTremolo(readByte());
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

    private void readBeats(TGMeasure measure, TGBeatData data) {
        int header = BEAT_HAS_NEXT;
        while (((header & BEAT_HAS_NEXT) != 0)) {
            header = readHeader();
            readBeat(header, measure, data);
        }
    }

    private void readBeat(int header, TGMeasure measure, TGBeatData data) {
        TGBeat beat = this.factory.newBeat();
        beat.setStart(data.getCurrentStart());
        readVoices(header, beat, data);
        if (((header & BEAT_HAS_STROKE) != 0)) {
            readStroke(beat.getStroke());
        }
        if (((header & BEAT_HAS_CHORD) != 0)) {
            readChord(beat);
        }
        if (((header & BEAT_HAS_TEXT) != 0)) {
            readText(beat);
        }
        measure.addBeat(beat);
    }

    private void readVoices(int header, TGBeat beat, TGBeatData data) {
        for (int i = 0; i < TGBeat.MAX_VOICES; i++) {
            int shift = (i * 2);
            beat.getVoice(i).setEmpty(true);
            if (((header & (BEAT_HAS_VOICE << shift)) != 0)) {
                if (((header & (BEAT_HAS_VOICE_CHANGES << shift)) != 0)) {
                    data.getVoice(i).setFlags(readHeader());
                }
                int flags = data.getVoice(i).getFlags();
                if (((flags & VOICE_NEXT_DURATION) != 0)) {
                    readDuration(data.getVoice(i).getDuration());
                }
                if (((flags & VOICE_HAS_NOTES) != 0)) {
                    readNotes(beat.getVoice(i), data);
                }
                if (((flags & VOICE_DIRECTION_UP) != 0)) {
                    beat.getVoice(i).setDirection(TGVoice.DIRECTION_UP);
                } else if (((flags & VOICE_DIRECTION_DOWN) != 0)) {
                    beat.getVoice(i).setDirection(TGVoice.DIRECTION_DOWN);
                }
                data.getVoice(i).getDuration().copy(beat.getVoice(i).getDuration());
                data.getVoice(i).setStart(data.getVoice(i).getStart() + beat.getVoice(i).getDuration().getTime());
                beat.getVoice(i).setEmpty(false);
            }
        }
    }

    private void readNotes(TGVoice voice, TGBeatData data) {
        int header = NOTE_HAS_NEXT;
        while (((header & NOTE_HAS_NEXT) != 0)) {
            header = readHeader();
            readNote(header, voice, data);
        }
    }

    private void readNote(int header, TGVoice voice, TGBeatData data) {
        TGNote note = this.factory.newNote();
        note.setValue(readByte());
        note.setString(readByte());
        note.setTiedNote((header & NOTE_TIED) != 0);
        if (((header & NOTE_VELOCITY) != 0)) {
            data.getVoice(voice.getIndex()).setVelocity(readByte());
        }
        note.setVelocity(data.getVoice(voice.getIndex()).getVelocity());
        if (((header & NOTE_EFFECT) != 0)) {
            readNoteEffect(note.getEffect());
        }
        voice.addNote(note);
    }

    private void readChord(TGBeat beat) {
        TGChord chord = this.factory.newChord(readByte());
        chord.setName(readUnsignedByteString());
        chord.setFirstFret(readByte());
        for (int string = 0; string < chord.countStrings(); string++) {
            chord.addFretValue(string, readByte());
        }
        beat.setChord(chord);
    }

    private void readText(TGBeat beat) {
        TGText text = this.factory.newText();
        text.setValue(readUnsignedByteString());
        beat.setText(text);
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
        if (((header & DURATION_NO_TUPLET) != 0)) {
            readDivisionType(duration.getDivision());
        } else {
            TGDivisionType.NORMAL.copy(duration.getDivision());
        }
    }

    private void readDivisionType(TGDivisionType divisionType) {
        divisionType.setEnters(readByte());
        divisionType.setTimes(readByte());
    }

    private void readStroke(TGStroke stroke) {
        stroke.setDirection(readByte());
        stroke.setValue(readByte());
    }

    private void readNoteEffect(TGNoteEffect effect) {
        int header = readHeader(3);
        if (((header & EFFECT_BEND) != 0)) {
            effect.setBend(readBendEffect());
        }
        if (((header & EFFECT_TREMOLO_BAR) != 0)) {
            effect.setTremoloBar(readTremoloBarEffect());
        }
        if (((header & EFFECT_HARMONIC) != 0)) {
            effect.setHarmonic(readHarmonicEffect());
        }
        if (((header & EFFECT_GRACE) != 0)) {
            effect.setGrace(readGraceEffect());
        }
        if (((header & EFFECT_TRILL) != 0)) {
            effect.setTrill(readTrillEffect());
        }
        if (((header & EFFECT_TREMOLO_PICKING) != 0)) {
            effect.setTremoloPicking(readTremoloPickingEffect());
        }
        effect.setVibrato(((header & EFFECT_VIBRATO) != 0));
        effect.setDeadNote(((header & EFFECT_DEAD) != 0));
        effect.setSlide(((header & EFFECT_SLIDE) != 0));
        effect.setHammer(((header & EFFECT_HAMMER) != 0));
        effect.setGhostNote(((header & EFFECT_GHOST) != 0));
        effect.setAccentuatedNote(((header & EFFECT_ACCENTUATED) != 0));
        effect.setHeavyAccentuatedNote(((header & EFFECT_HEAVY_ACCENTUATED) != 0));
        effect.setPalmMute(((header & EFFECT_PALM_MUTE) != 0));
        effect.setStaccato(((header & EFFECT_STACCATO) != 0));
        effect.setTapping(((header & EFFECT_TAPPING) != 0));
        effect.setSlapping(((header & EFFECT_SLAPPING) != 0));
        effect.setPopping(((header & EFFECT_POPPING) != 0));
        effect.setFadeIn(((header & EFFECT_FADE_IN) != 0));
    }

    private TGEffectBend readBendEffect() {
        TGEffectBend bend = this.factory.newEffectBend();
        int count = readByte();
        for (int i = 0; i < count; i++) {
            int position = readByte();
            int value = readByte();
            bend.addPoint(position, value);
        }
        return bend;
    }

    private TGEffectTremoloBar readTremoloBarEffect() {
        TGEffectTremoloBar tremoloBar = this.factory.newEffectTremoloBar();
        int count = readByte();
        for (int i = 0; i < count; i++) {
            int position = readByte();
            int value = (readByte() - TGEffectTremoloBar.MAX_VALUE_LENGTH);
            tremoloBar.addPoint(position, value);
        }
        return tremoloBar;
    }

    private TGEffectHarmonic readHarmonicEffect() {
        TGEffectHarmonic effect = this.factory.newEffectHarmonic();
        effect.setType(readByte());
        if (effect.getType() != TGEffectHarmonic.TYPE_NATURAL) {
            effect.setData(readByte());
        }
        return effect;
    }

    private TGEffectGrace readGraceEffect() {
        int header = readHeader();
        TGEffectGrace effect = this.factory.newEffectGrace();
        effect.setDead((header & GRACE_FLAG_DEAD) != 0);
        effect.setOnBeat((header & GRACE_FLAG_ON_BEAT) != 0);
        effect.setFret(readByte());
        effect.setDuration(readByte());
        effect.setDynamic(readByte());
        effect.setTransition(readByte());
        return effect;
    }

    private TGEffectTremoloPicking readTremoloPickingEffect() {
        TGEffectTremoloPicking effect = this.factory.newEffectTremoloPicking();
        effect.getDuration().setValue(readByte());
        return effect;
    }

    private TGEffectTrill readTrillEffect() {
        TGEffectTrill effect = this.factory.newEffectTrill();
        effect.setFret(readByte());
        effect.getDuration().setValue(readByte());
        return effect;
    }

    private TGMarker readMarker(int measure) {
        TGMarker marker = this.factory.newMarker();
        marker.setMeasure(measure);
        marker.setTitle(readUnsignedByteString());
        readRGBColor(marker.getColor());
        return marker;
    }

    private void readRGBColor(TGColor color) {
        color.setR((readByte() & 0xff));
        color.setG((readByte() & 0xff));
        color.setB((readByte() & 0xff));
    }

    private void readLyrics(TGLyric lyrics) {
        lyrics.setFrom(readShort());
        lyrics.setLyrics(readIntegerString());
    }

    private byte readByte() {
        try {
            return (byte) this.dataInputStream.read();
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

    private int readHeader(int bCount) {
        int header = 0;
        for (int i = bCount; i > 0; i--) {
            header += (readHeader() << ((8 * i) - 8));
        }
        return header;
    }

    private short readShort() {
        try {
            return this.dataInputStream.readShort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String readUnsignedByteString() {
        try {
            return readString((this.dataInputStream.read() & 0xFF));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String readIntegerString() {
        try {
            return readString(this.dataInputStream.readInt());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String readString(int length) {
        try {
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
