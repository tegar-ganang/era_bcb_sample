package jmms;

import jm.music.data.Note;
import jm.music.data.Phrase;

/**
 * @author wooller
 *
 *11/07/2005
 *
 * Copyright JEDI/Rene Wooller
 *
 */
public class NoteEvent {

    Phrase phr;

    int chan;

    /**
     * 
     */
    public NoteEvent(int pitch, double rv, double dur, int dyn, double st, int c) {
        super();
        Note n = new Note(pitch, rv, dyn);
        n.setDuration(dur);
        phr = new Phrase(st);
        phr.addNote(n);
        this.chan = c;
    }

    public Phrase getNotePhr() {
        return phr;
    }

    public int getChannel() {
        return chan;
    }
}
