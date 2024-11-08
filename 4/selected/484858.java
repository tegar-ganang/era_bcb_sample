package com.timenes.clips.controller.marshalling;

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
 * @author helge@timenes.com
 * 
 */
public class Marshaller {

    public String marshallProject(Project project) {
        String s = "<Project title='" + project.getTitle() + "'>\n";
        s += indent(marshallKeyTrack(project.getKeyTrack()));
        for (int i = 0; i < project.getTracks().size(); i++) {
            s += indent(marshallTrack(project.getTracks().get(i)));
        }
        for (int i = 0; i < project.getRythms().size(); i++) {
            s += indent(marshallRythm(project.getRythms().get(i)));
        }
        for (int i = 0; i < project.getChords().size(); i++) {
            s += indent(marshallChord(project.getChords().get(i)));
        }
        s += "</Project>\n";
        return s;
    }

    private String marshallKeyTrack(KeyTrack keyTrack) {
        String s = "<KeyTrack>\n";
        for (int i = 0; i < keyTrack.getKeyChanges().size(); i++) {
            s += indent(marshallKeyChange(keyTrack.getKeyChanges().get(i)));
        }
        s += "</KeyTrack>\n";
        return s;
    }

    private String marshallKeyChange(KeyChange keyChange) {
        return "<KeyChange key='" + keyChange.getKey() + "' tick='" + keyChange.getTick() + "' />\n";
    }

    public String marshallTrack(Track track) {
        String s = "<Track";
        s += " title='" + track.getTitle() + "'";
        s += " channel='" + track.getChannel() + "'";
        s += " instrument='" + track.getInstrument() + "'";
        s += ">\n";
        for (int i = 0; i < track.getClips().size(); i++) {
            s += indent(marshallClip(track.getClips().get(i)));
        }
        s += "</Track>\n";
        return s;
    }

    public String marshallNote(Note note) {
        String s = "<Note>\n";
        s += indent(marshallFuzzyValue("TimeIndex", note.getTimeIndex()));
        s += indent(marshallFuzzyValue("TimeBend", note.getTimeBend()));
        s += indent(marshallFuzzyValue("TitchIndex", note.getPitchIndex()));
        s += indent(marshallFuzzyValue("PitchBend", note.getPitchBend()));
        s += indent(marshallFuzzyValue("Length", note.getLength()));
        s += indent(marshallFuzzyValue("Velocity", note.getVelocity()));
        s += "</Note>\n";
        return s;
    }

    public String marshallFuzzyValue(String name, FuzzyValue value) {
        String s = "<" + name;
        s += " mean='" + value.getMean() + "'";
        s += " stddev='" + value.getStddev() + "'";
        s += " min='" + value.getMin() + "'";
        s += " max='" + value.getMax() + "'";
        s += "/>\n";
        return s;
    }

    public String marshallRythm(Rythm rythm) {
        String s = "<Rythm";
        s += " id='" + rythm.getId() + "'";
        s += " title='" + rythm.getTitle() + "'";
        s += " ticksPerCell='" + rythm.getTicksPerColumn() + "'";
        s += ">\n";
        for (int i = 0; i < rythm.getNotes().size(); i++) {
            s += indent(marshallNote(rythm.getNotes().get(i)));
        }
        s += "</Rythm>\n";
        return s;
    }

    public String marshallClip(Clip clip) {
        String s = "<Clip";
        s += " title='" + clip.getTitle() + "'";
        s += " length='" + clip.getLength() + "'";
        s += " repeats='" + clip.getRepeats() + "'";
        s += ">\n";
        for (int i = 0; i < clip.getRythmChanges().size(); i++) {
            s += indent(marshallRythmChange(clip.getRythmChanges().get(i)));
        }
        for (int i = 0; i < clip.getChordChanges().size(); i++) {
            s += indent(marshallChordChange(clip.getChordChanges().get(i)));
        }
        s += "</Clip>\n";
        return s;
    }

    private String marshallChordChange(ChordChange chordChange) {
        return "<ChordChange id='" + chordChange.getChord().getId() + "' tick='" + chordChange.getTick() + "' />\n";
    }

    private String marshallRythmChange(RythmChange rythmChange) {
        return "<RythmChange id='" + rythmChange.getRythm().getId() + "' tick='" + rythmChange.getTick() + "' />\n";
    }

    private String indent(String s) {
        if (s.endsWith("\n")) {
            s = s.substring(0, s.length() - 1);
            return "   " + s.replace("\n", "\n   ") + "\n";
        } else {
            return "   " + s.replace("\n", "\n   ");
        }
    }

    public String marshallChord(Chord chord) {
        String s = "<Chord";
        s += " id='" + chord.getId() + "'";
        s += " title='" + chord.getTitle() + "'";
        s += ">\n";
        for (int i = 0; i < chord.getPitches().size(); i++) {
            s += "   <Pitch>" + chord.getPitches().get(i) + "</Pitch>\n";
        }
        s += "</Chord>\n";
        return s;
    }
}
