package org.herac.tuxguitar.io.ptb;

import java.util.Iterator;
import org.herac.tuxguitar.io.ptb.base.PTBar;
import org.herac.tuxguitar.io.ptb.base.PTBeat;
import org.herac.tuxguitar.io.ptb.base.PTComponent;
import org.herac.tuxguitar.io.ptb.base.PTGuitarIn;
import org.herac.tuxguitar.io.ptb.base.PTNote;
import org.herac.tuxguitar.io.ptb.base.PTPosition;
import org.herac.tuxguitar.io.ptb.base.PTSection;
import org.herac.tuxguitar.io.ptb.base.PTSong;
import org.herac.tuxguitar.io.ptb.base.PTSongInfo;
import org.herac.tuxguitar.io.ptb.base.PTTempo;
import org.herac.tuxguitar.io.ptb.base.PTTrack;
import org.herac.tuxguitar.io.ptb.base.PTTrackInfo;
import org.herac.tuxguitar.io.ptb.helper.TrackHelper;
import org.herac.tuxguitar.song.factory.TGFactory;
import org.herac.tuxguitar.song.managers.TGSongManager;
import org.herac.tuxguitar.song.models.TGBeat;
import org.herac.tuxguitar.song.models.TGChannel;
import org.herac.tuxguitar.song.models.TGDuration;
import org.herac.tuxguitar.song.models.TGMeasure;
import org.herac.tuxguitar.song.models.TGNote;
import org.herac.tuxguitar.song.models.TGSong;
import org.herac.tuxguitar.song.models.TGString;
import org.herac.tuxguitar.song.models.TGStroke;
import org.herac.tuxguitar.song.models.TGTrack;
import org.herac.tuxguitar.song.models.TGVoice;
import org.herac.tuxguitar.song.models.effects.TGEffectBend;

public class PTSongParser {

    private TGSongManager manager;

    private TrackHelper helper;

    public PTSongParser(TGFactory factory) {
        this.manager = new TGSongManager(factory);
        this.helper = new TrackHelper();
    }

    public TGSong parseSong(PTSong src) {
        PTSong song = new PTSong();
        PTSongSynchronizerUtil.synchronizeTracks(src, song);
        this.manager.setSong(this.manager.getFactory().newSong());
        this.parseTrack(song.getTrack1());
        this.parseTrack(song.getTrack2());
        this.parseProperties(song.getInfo());
        this.manager.orderBeats();
        return this.manager.getSong();
    }

    private void parseProperties(PTSongInfo info) {
        if (info.getName() != null) {
            this.manager.getSong().setName(info.getName());
        }
        if (info.getAlbum() != null) {
            this.manager.getSong().setAlbum(info.getAlbum());
        }
        if (info.getAuthor() != null) {
            this.manager.getSong().setAuthor(info.getAuthor());
        }
        if (info.getCopyright() != null) {
            this.manager.getSong().setCopyright(info.getCopyright());
        }
        if (info.getArrenger() != null) {
            this.manager.getSong().setWriter(info.getArrenger());
        }
        if (info.getGuitarTranscriber() != null || info.getBassTranscriber() != null) {
            String transcriber = new String();
            if (info.getGuitarTranscriber() != null) {
                transcriber += info.getGuitarTranscriber();
            }
            if (info.getBassTranscriber() != null) {
                if (transcriber.length() > 0) {
                    transcriber += (" - ");
                }
                transcriber += info.getBassTranscriber();
            }
            this.manager.getSong().setTranscriber(transcriber);
        }
        if (info.getGuitarInstructions() != null || info.getBassInstructions() != null) {
            String comments = new String();
            if (info.getGuitarInstructions() != null) {
                comments += info.getGuitarInstructions();
            }
            if (info.getBassInstructions() != null) {
                comments += info.getBassInstructions();
            }
            this.manager.getSong().setComments(comments);
        }
    }

    private void parseTrack(PTTrack track) {
        this.helper.reset(track.getDefaultInfo());
        long start = TGDuration.QUARTER_TIME;
        for (int sIndex = 0; sIndex < track.getSections().size(); sIndex++) {
            PTSection section = (PTSection) track.getSections().get(sIndex);
            section.sort();
            this.helper.getStartHelper().init(section.getNumber(), section.getStaffs());
            this.helper.getStartHelper().initVoices(start);
            for (int pIndex = 0; pIndex < section.getPositions().size(); pIndex++) {
                PTPosition position = (PTPosition) section.getPositions().get(pIndex);
                parsePosition(track, position);
            }
            start = this.helper.getStartHelper().getMaxStart();
        }
    }

    private void parsePosition(PTTrack track, PTPosition position) {
        for (int i = 0; i < position.getComponents().size(); i++) {
            PTComponent component = (PTComponent) position.getComponents().get(i);
            if (component instanceof PTBar) {
                parseBar((PTBar) component);
            } else if (component instanceof PTGuitarIn) {
                parseGuitarIn(track, (PTGuitarIn) component);
            } else if (component instanceof PTTempo) {
                parseTempo((PTTempo) component);
            } else if (component instanceof PTBeat) {
                parseBeat((PTBeat) component);
            }
        }
    }

    private void parseBar(PTBar bar) {
        this.helper.getStartHelper().initVoices(this.helper.getStartHelper().getMaxStart());
        if (bar.getNumerator() > 0 && bar.getDenominator() > 0) {
            this.helper.getStartHelper().setBarStart(this.helper.getStartHelper().getMaxStart());
            this.helper.getStartHelper().setBarLength((long) (bar.getNumerator() * (TGDuration.QUARTER_TIME * (4.0f / bar.getDenominator()))));
        }
    }

    private void parseGuitarIn(PTTrack track, PTGuitarIn guitarIn) {
        PTTrackInfo info = track.getInfo(guitarIn.getTrackInfo());
        if (info != null) {
            while (this.helper.getInfoHelper().countStaffTracks() > guitarIn.getStaff()) {
                this.helper.getInfoHelper().removeStaffTrack(this.helper.getInfoHelper().countStaffTracks() - 1);
            }
            Iterator it = this.manager.getSong().getTracks();
            while (it.hasNext()) {
                TGTrack tgTrack = (TGTrack) it.next();
                if (hasSameInfo(tgTrack, info)) {
                    boolean exists = false;
                    for (int i = 0; i < this.helper.getInfoHelper().countStaffTracks(); i++) {
                        TGTrack existent = this.helper.getInfoHelper().getStaffTrack(i);
                        if (existent != null && existent.getNumber() == tgTrack.getNumber()) {
                            exists = true;
                        }
                    }
                    if (!exists) {
                        this.helper.getInfoHelper().addStaffTrack(tgTrack);
                        return;
                    }
                }
            }
            this.createTrack(info);
        }
    }

    private void parseTempo(PTTempo tempo) {
        TGMeasure measure = getMeasure(1, this.helper.getStartHelper().getMaxStart());
        measure.getTempo().setValue(tempo.getTempo());
        measure.getHeader().setTripletFeel(tempo.getTripletFeel());
    }

    private void parseBeat(PTBeat beat) {
        if (beat.isGrace()) {
            return;
        }
        if (beat.getMultiBarRest() > 1) {
            long start = this.helper.getStartHelper().getBarStart();
            long duration = (beat.getMultiBarRest() * this.helper.getStartHelper().getBarLength());
            this.helper.getStartHelper().setVoiceStart(beat.getStaff(), beat.getVoice(), (start + duration));
            return;
        }
        long start = this.helper.getStartHelper().getVoiceStart(beat.getStaff(), beat.getVoice());
        TGMeasure measure = getMeasure(getStaffTrack(beat.getStaff()), start);
        TGBeat tgBeat = getBeat(measure, start);
        TGVoice tgVoice = tgBeat.getVoice(beat.getVoice());
        tgVoice.setEmpty(false);
        tgVoice.getDuration().setValue(beat.getDuration());
        tgVoice.getDuration().setDotted(beat.isDotted());
        tgVoice.getDuration().setDoubleDotted(beat.isDoubleDotted());
        tgVoice.getDuration().getDivision().setTimes(beat.getTimes());
        tgVoice.getDuration().getDivision().setEnters(beat.getEnters());
        Iterator it = beat.getNotes().iterator();
        while (it.hasNext()) {
            PTNote ptNote = (PTNote) it.next();
            if (ptNote.getString() <= measure.getTrack().stringCount() && ptNote.getValue() >= 0) {
                TGNote note = this.manager.getFactory().newNote();
                note.setString(ptNote.getString());
                note.setValue(ptNote.getValue());
                note.setTiedNote(ptNote.isTied());
                note.getEffect().setVibrato(beat.isVibrato());
                note.getEffect().setDeadNote(ptNote.isDead());
                note.getEffect().setHammer(ptNote.isHammer());
                note.getEffect().setSlide(ptNote.isSlide());
                note.getEffect().setBend(makeBend(ptNote.getBend()));
                tgVoice.addNote(note);
            }
        }
        if (beat.isArpeggioUp()) {
            tgBeat.getStroke().setDirection(TGStroke.STROKE_DOWN);
            tgBeat.getStroke().setValue(TGDuration.SIXTEENTH);
        } else if (beat.isArpeggioDown()) {
            tgBeat.getStroke().setDirection(TGStroke.STROKE_UP);
            tgBeat.getStroke().setValue(TGDuration.SIXTEENTH);
        }
        this.helper.getStartHelper().checkBeat(tgVoice.isRestVoice());
        long duration = tgVoice.getDuration().getTime();
        if (tgVoice.isRestVoice() && tgBeat.getStart() == this.helper.getStartHelper().getBarStart() && duration > this.helper.getStartHelper().getBarLength()) {
            duration = this.helper.getStartHelper().getBarLength();
        }
        this.helper.getStartHelper().setVoiceStart(beat.getStaff(), beat.getVoice(), (tgBeat.getStart() + duration));
    }

    private TGEffectBend makeBend(int value) {
        if (value >= 1 && value <= 8) {
            TGEffectBend bend = this.manager.getFactory().newEffectBend();
            if (value == 1) {
                bend.addPoint(0, 0);
                bend.addPoint(6, (TGEffectBend.SEMITONE_LENGTH * 4));
                bend.addPoint(12, (TGEffectBend.SEMITONE_LENGTH * 4));
            } else if (value == 2) {
                bend.addPoint(0, 0);
                bend.addPoint(3, (TGEffectBend.SEMITONE_LENGTH * 4));
                bend.addPoint(6, (TGEffectBend.SEMITONE_LENGTH * 4));
                bend.addPoint(9, 0);
                bend.addPoint(12, 0);
            } else if (value == 3) {
                bend.addPoint(0, 0);
                bend.addPoint(6, (TGEffectBend.SEMITONE_LENGTH * 4));
                bend.addPoint(12, (TGEffectBend.SEMITONE_LENGTH * 4));
            } else if (value == 4) {
                bend.addPoint(0, (TGEffectBend.SEMITONE_LENGTH * 4));
                bend.addPoint(12, (TGEffectBend.SEMITONE_LENGTH * 4));
            } else if (value == 5) {
                bend.addPoint(0, (TGEffectBend.SEMITONE_LENGTH * 4));
                bend.addPoint(4, (TGEffectBend.SEMITONE_LENGTH * 4));
                bend.addPoint(8, 0);
                bend.addPoint(12, 0);
            } else if (value == 6) {
                bend.addPoint(0, 8);
                bend.addPoint(12, 8);
            } else if (value == 7) {
                bend.addPoint(0, (TGEffectBend.SEMITONE_LENGTH * 4));
                bend.addPoint(4, (TGEffectBend.SEMITONE_LENGTH * 4));
                bend.addPoint(8, 0);
                bend.addPoint(12, 0);
            } else if (value == 8) {
                bend.addPoint(0, (TGEffectBend.SEMITONE_LENGTH * 4));
                bend.addPoint(4, (TGEffectBend.SEMITONE_LENGTH * 4));
                bend.addPoint(8, 0);
                bend.addPoint(12, 0);
            }
            return bend;
        }
        return null;
    }

    private TGMeasure getMeasure(int trackNumber, long start) {
        return getMeasure(getTrack(trackNumber), start);
    }

    private TGMeasure getMeasure(TGTrack track, long start) {
        TGMeasure measure = null;
        while ((measure = this.manager.getTrackManager().getMeasureAt(track, start)) == null) {
            this.manager.addNewMeasureBeforeEnd();
        }
        return measure;
    }

    private TGTrack getTrack(int number) {
        TGTrack track = null;
        while ((track = this.manager.getTrack(number)) == null) {
            track = createTrack();
        }
        return track;
    }

    public TGTrack getStaffTrack(int staff) {
        TGTrack track = this.helper.getInfoHelper().getStaffTrack(staff);
        return (track != null ? track : createTrack());
    }

    private TGTrack createTrack() {
        return createTrack(this.helper.getInfoHelper().getDefaultInfo());
    }

    private TGTrack createTrack(PTTrackInfo info) {
        TGTrack track = this.manager.addTrack();
        this.helper.getInfoHelper().addStaffTrack(track);
        this.setTrackInfo(track, info);
        return track;
    }

    private void setTrackInfo(TGTrack tgTrack, PTTrackInfo info) {
        TGChannel tgChannel = this.manager.addChannel();
        tgChannel.setProgram((short) info.getInstrument());
        tgChannel.setVolume((short) info.getVolume());
        tgChannel.setBalance((short) info.getBalance());
        tgTrack.setName(info.getName());
        tgTrack.setChannelId(tgChannel.getChannelId());
        tgTrack.getStrings().clear();
        for (int i = 0; i < info.getStrings().length; i++) {
            TGString string = this.manager.getFactory().newString();
            string.setNumber((i + 1));
            string.setValue(info.getStrings()[i]);
            tgTrack.getStrings().add(string);
        }
    }

    private boolean hasSameInfo(TGTrack track, PTTrackInfo info) {
        TGChannel tgChannel = this.manager.getChannel(track.getChannelId());
        if (tgChannel == null) {
            return false;
        }
        if (!info.getName().equals(track.getName())) {
            return false;
        }
        if (info.getInstrument() != tgChannel.getProgram()) {
            return false;
        }
        if (info.getVolume() != tgChannel.getVolume()) {
            return false;
        }
        if (info.getBalance() != tgChannel.getBalance()) {
            return false;
        }
        if (info.getStrings().length != track.stringCount()) {
            return false;
        }
        for (int i = 0; i < info.getStrings().length; i++) {
            if (info.getStrings()[i] != track.getString((i + 1)).getValue()) {
                return false;
            }
        }
        return true;
    }

    private TGBeat getBeat(TGMeasure measure, long start) {
        int count = measure.countBeats();
        for (int i = 0; i < count; i++) {
            TGBeat beat = measure.getBeat(i);
            if (beat.getStart() == start) {
                return beat;
            }
        }
        TGBeat beat = this.manager.getFactory().newBeat();
        beat.setStart(start);
        measure.addBeat(beat);
        return beat;
    }
}
