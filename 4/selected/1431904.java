package com.timenes.clips.model.utils;

import com.timenes.clips.model.Chord;
import com.timenes.clips.model.ChordChange;
import com.timenes.clips.model.Clip;
import com.timenes.clips.model.FuzzyValue;
import com.timenes.clips.model.KeyChange;
import com.timenes.clips.model.KeyTrack;
import com.timenes.clips.model.Note;
import com.timenes.clips.model.Project;
import com.timenes.clips.model.Rythm;
import com.timenes.clips.model.RythmChange;
import com.timenes.clips.model.Track;

/**
 * 
 * A class providing a few handy utils for model objects
 * @author helge@timenes.com
 *
 */
public class Utils {

    public static Clip copy(Clip clip) {
        Clip copy = new Clip();
        for (int i = 0; i < clip.getChordChanges().size(); i++) {
            copy.getChordChanges().add(copy(clip.getChordChanges().get(i)));
        }
        for (int i = 0; i < clip.getRythmChanges().size(); i++) {
            copy.getRythmChanges().add(copy(clip.getRythmChanges().get(i)));
        }
        copy.setLength(clip.getLength());
        copy.setRepeats(clip.getRepeats());
        copy.setTitle(new String(clip.getTitle()));
        return copy;
    }

    public static RythmChange copy(RythmChange rythmChange) {
        RythmChange copy = new RythmChange();
        copy.setRythm(copy(rythmChange.getRythm()));
        copy.setTick(rythmChange.getTick());
        return copy;
    }

    public static Rythm copy(Rythm rythm) {
        Rythm copy = new Rythm();
        for (int i = 0; i < rythm.getNotes().size(); i++) {
            copy.getNotes().add(copy(rythm.getNotes().get(i)));
        }
        copy.setTitle(new String(rythm.getTitle()));
        return copy;
    }

    public static Note copy(Note note) {
        Note copy = new Note();
        copy.setTimeIndex(copy(note.getTimeIndex()));
        copy.setTimeBend(copy(note.getTimeBend()));
        copy.setPitchIndex(copy(note.getPitchIndex()));
        copy.setPitchBend(copy(note.getPitchBend()));
        copy.setLength(copy(note.getLength()));
        copy.setVelocity(copy(note.getVelocity()));
        return copy;
    }

    public static FuzzyValue copy(FuzzyValue value) {
        FuzzyValue copy = new FuzzyValue();
        copy.setMean(value.getMean());
        copy.setStddev(value.getStddev());
        copy.setMin(value.getMin());
        copy.setMax(value.getMax());
        return copy;
    }

    public static ChordChange copy(ChordChange chordChange) {
        ChordChange copy = new ChordChange();
        copy.setChord(copy(chordChange.getChord()));
        copy.setTick(chordChange.getTick());
        return copy;
    }

    public static Chord copy(Chord chord) {
        Chord copy = new Chord();
        for (int i = 0; i < chord.getPitches().size(); i++) {
            copy.getPitches().add(chord.getPitches().get(i));
        }
        copy.setTitle(new String(chord.getTitle()));
        return copy;
    }

    public static KeyChange copy(KeyChange keyChange) {
        KeyChange copy = new KeyChange();
        copy.setKey(keyChange.getKey());
        copy.setTick(keyChange.getTick());
        return copy;
    }

    public static KeyTrack copy(KeyTrack keyTrack) {
        KeyTrack copy = new KeyTrack();
        for (int i = 0; i < keyTrack.getKeyChanges().size(); i++) {
            copy.getKeyChanges().add(copy(keyTrack.getKeyChanges().get(i)));
        }
        return copy;
    }

    public static Track copy(Track track) {
        Track copy = new Track();
        copy.setTitle(new String(track.getTitle()));
        copy.setChannel(track.getChannel());
        copy.setInstrument(track.getInstrument());
        for (int i = 0; i < track.getClips().size(); i++) {
            copy.getClips().add(copy(track.getClips().get(i)));
        }
        return copy;
    }

    public static Project copy(Project project) {
        Project copy = new Project();
        copy.setTitle(new String(project.getTitle()));
        copy.setKeyTrack(copy(project.getKeyTrack()));
        for (int i = 0; i < project.getRythms().size(); i++) {
            copy.getRythms().add(copy(project.getRythms().get(i)));
        }
        for (int i = 0; i < project.getChords().size(); i++) {
            copy.getChords().add(copy(project.getChords().get(i)));
        }
        for (int i = 0; i < project.getTracks().size(); i++) {
            copy.getTracks().add(copy(project.getTracks().get(i)));
        }
        return copy;
    }

    public static Object copy(Object modelObject) {
        if (modelObject instanceof Clip) return copy((Clip) modelObject);
        if (modelObject instanceof Chord) return copy((Chord) modelObject);
        if (modelObject instanceof Rythm) return copy((Rythm) modelObject);
        if (modelObject instanceof ChordChange) return copy((ChordChange) modelObject);
        if (modelObject instanceof Note) return copy((Note) modelObject);
        throw new IllegalArgumentException("No methods for copying model objects of class " + modelObject.getClass().getSimpleName() + " implemented.");
    }
}
