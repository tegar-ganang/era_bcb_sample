package world.sound.tunes;

/** Represents one a Chord to be played on the given channel.
 *    
 *  Based in part on a class originally designed by Viera K. Proulx. */
public class Tune {

    /** The channel to be played on. */
    protected int channel;

    /** The Chord to be played. */
    protected Chord chord;

    /** Create a Tune on the given channel from the given Chord. */
    public Tune(int channel, Chord chord) {
        this.channel = Math.min(15, Math.max(channel, 0));
        this.chord = chord;
    }

    /** Create an empty Tune on the given channel. */
    public Tune(int channel) {
        this(channel, new Chord());
    }

    /** Create a Tune on the given channel from the given Notes. */
    public Tune(int channel, Note... notes) {
        this(channel, new Chord(notes));
    }

    /** Get the Channel number the Chord will be played on. */
    public int getChannel() {
        return this.channel;
    }

    /** Return the Chord that will be played. */
    public Chord getChord() {
        return this.chord;
    }

    /** Add the given Note to this Tune's Chord */
    public void addNote(Note note) {
        this.chord.addNote(note);
    }

    /** Add a Note (represented by the given String) this Tune's Chord. */
    public void addNote(String note) {
        this.addNote(new Note(note));
    }

    /** Add all the Notes fomr the given Chord to this Tune. */
    public void addChord(Chord c) {
        for (int i = 0; i < c.notes.size(); i++) this.chord.addNote(c.notes.get(i));
    }

    /** Produce a human readable representation of this Tune/Chord. */
    public String toString() {
        String ret = "Channel: " + this.channel;
        for (Note n : this.chord.notes) ret += ", " + n;
        return ret;
    }

    /** Has this Tune finished playing? Are all the Notes in the Chord done? */
    public boolean isSilent() {
        return this.chord.isSilent();
    }

    /** Remove all the silent Notes from this Tune. */
    public void removeSilent() {
        int i = 0;
        while (i < this.chord.notes.size()) {
            if (!this.chord.notes.get(i).isSilent()) this.chord.notes.remove(i); else i++;
        }
    }

    /** Remove all Notes from this Tune. */
    public void clearChord() {
        this.chord = new Chord();
    }

    /** Return the number of Notes in this Tune **/
    public int size() {
        return this.chord.size();
    }

    /** Does this tune contain the given Note? */
    public boolean containsNote(Note n) {
        return this.chord.containsNote(n);
    }
}
