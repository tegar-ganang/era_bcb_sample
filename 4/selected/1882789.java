package uk.org.toot.midix.control.neck;

import java.util.List;
import uk.org.toot.midi.core.*;
import uk.org.toot.midi.core.channel.*;

public class StrungNeck extends AbstractMidiDevice implements Bendable {

    private static int nextNum = 1;

    private int num;

    private List<TunedString> strings;

    private int frets = 24;

    private int barreSize = 6;

    private boolean octaveStrings = false;

    private ChannelWriteMidiOutput outPort;

    private NeckFamily family;

    private StringTuning tuning;

    private String name;

    public StrungNeck(NeckFamily family) {
        this(family, 24);
    }

    public StrungNeck(NeckFamily family, int nfrets) {
        super("Neck");
        num = nextNum;
        name = "Neck " + num + ": " + family.getName();
        outPort = new ChannelWriteMidiOutput(name);
        addMidiOutput(outPort);
        this.family = family;
        frets = nfrets;
        Tunings t = family.getTunings();
        String tuning = t.getTunings().get(0);
        StringTuning st = t.createTuning(tuning);
        setTuning(st);
        nextNum += 1;
    }

    public void closeMidi() {
    }

    public String getName() {
        return name;
    }

    public void setTuning(StringTuning tuning) {
        strings = new java.util.ArrayList<TunedString>();
        for (int i = 0; i < tuning.getPolyphony(); i++) {
            strings.add(new TunedString(i, outPort.getChannelWriter(i)));
        }
        this.tuning = tuning;
    }

    public StringTuning getTuning() {
        return tuning;
    }

    public NeckFamily getFamily() {
        return family;
    }

    public List<TunedString> getStrings() {
        return strings;
    }

    public TunedString getString(int index) {
        if (index < 0 || index >= getStringCount()) return null;
        return strings.get(index);
    }

    public int getStringCount() {
        return strings.size();
    }

    public int getFrets() {
        return frets;
    }

    public void setBarreSize(int size) {
        barreSize = size;
    }

    public int getBarreSize() {
        return barreSize;
    }

    public void barre(int fret) {
        for (int i = 0; i < strings.size(); i++) {
            if (i < getStringCount() - getBarreSize()) {
                strings.get(i).hold(0);
            } else {
                strings.get(i).hold(fret);
            }
        }
    }

    public void shape(int offset, ChordShape shape, int firstString) {
        for (int i = 0; i < strings.size(); i++) {
            TunedString string = strings.get(i);
            if (shape == null) {
                string.hold(0);
                continue;
            }
            if (i - firstString < 0) {
                string.mute(0);
                continue;
            }
            ChordShape.Fretting f = shape.getFretting(i - firstString);
            if (f.finger == -1) {
                string.hold(f.fret);
            } else if (offset + f.fret >= 0) {
                string.hold(offset + f.fret);
            } else {
                string.mute(0);
            }
        }
    }

    public void mute() {
        for (TunedString string : strings) {
            string.mute();
        }
    }

    protected void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ie) {
        }
    }

    public void bend(int amount) {
        for (TunedString string : strings) {
            if (string.isBending() || amount == 0) {
                string.bend(amount);
            }
        }
    }

    public boolean isBending() {
        for (TunedString string : strings) {
            if (string.isBending()) {
                return true;
            }
        }
        return false;
    }

    public void setProgram(int prg) {
        mute();
        for (TunedString string : strings) {
            string.programChange(prg);
        }
    }

    public void setOctaveStrings(boolean oct) {
        octaveStrings = oct;
        for (TunedString string : strings) {
            string.setOctave(oct);
        }
    }

    public boolean hasOctaveStrings() {
        return octaveStrings;
    }

    public class TunedString implements Bendable {

        private int fret = 0;

        private int note = -1;

        private int chan;

        private boolean octave = false;

        private int diatonics = 0;

        private boolean bending = false;

        private MidiChannelWriter channel;

        public TunedString(int aChan, MidiChannelWriter channel) {
            chan = aChan;
            this.channel = channel;
        }

        public int getChannel() {
            return chan;
        }

        public int getOpenTuning() {
            return getTuning().getPitch(chan);
        }

        public int getDoubleInterval() {
            return getTuning().getDoubleInterval(chan);
        }

        public int getNote() {
            return note;
        }

        public int getFret() {
            return fret;
        }

        public void setOctave(boolean oct) {
            octave = oct;
        }

        public void hold(int aFret) {
            fret = aFret;
        }

        public void mute(int aFret) {
            fret = -aFret;
        }

        public void open() {
            fret = 0;
        }

        /** Pick the string or string pair */
        public void pick(boolean up, int velocity) {
            this.mute();
            if (fret < 0) return;
            if (velocity < 10) velocity = 10;
            note = getOpenTuning() + fret;
            if (up && octave && note < 127 - getDoubleInterval()) {
                channel.noteOn(note + getDoubleInterval(), velocity);
                sleep(10);
            }
            channel.noteOn(note, velocity);
            if (!up && octave && note < 127 - getDoubleInterval()) {
                sleep(10);
                channel.noteOn(note + getDoubleInterval(), velocity);
            }
        }

        public void mute() {
            if (note < 0) return;
            channel.noteOff(note);
            if (octave) {
                channel.noteOff(note + getDoubleInterval());
            }
            note = -1;
        }

        public void bend(int bend) {
            channel.setPitchBend(bend);
        }

        public void programChange(int prg) {
            channel.programChange(prg);
        }

        public boolean isBending() {
            return bending;
        }

        public void setBending(boolean b) {
            bending = b;
        }

        /** @return true if fret is diatonic, false otherwise */
        public boolean diatonic(int fret) {
            return (diatonics & (1 << fret)) != 0;
        }

        public void diatonics(int mask) {
            diatonics = mask;
        }
    }
}
