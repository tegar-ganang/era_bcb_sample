package midi;

import javax.sound.midi.*;
import java.util.*;
import java.lang.*;
import java.util.concurrent.*;
import utils.*;
import symbols.*;
import gui.*;

public class JHumMidiPlayer extends Thread {

    static final int DITHERDELAY = 100;

    static final int noteEquiv[] = { 0, 2, 4, 5, 7, 9, 11 };

    static final int noteMod = 7;

    static final int noteEquivMod = 12;

    static final int middleOctave = 5;

    int noteTranslate = JHumClef.SOL;

    int tempo = 120;

    int tempoRelative = JHumNote.NOIRE;

    int mesureAlts[] = new int[noteMod];

    int repeatBeginPos = 0;

    ArrayList<JHumNoteEvent> notes = new ArrayList<JHumNoteEvent>();

    JHumScore myScore;

    JHumPlayFrame myPlayFrame;

    public JHumMidiPlayer(JHumScore score, JHumPlayFrame frame) {
        myScore = score;
        myPlayFrame = frame;
    }

    public void addNote(int level, int index, boolean alted, int alt, double duree) {
        notes.add(new JHumNoteEvent(convertNote(level, alted, alt), index, convertDuree(duree)));
    }

    public void addSoupir(int index, double duree) {
        notes.add(new JHumNoteEvent(-1, index, convertDuree(duree)));
    }

    public void clearMesureAlts() {
        for (int i = 0; i < mesureAlts.length; i++) mesureAlts[i] = JHumNote.BECARE;
    }

    public void setKey(int key) {
        noteTranslate = key;
    }

    public void setBeginRepeat(int pos) {
        repeatBeginPos = pos;
    }

    public int getBeginRepeat() {
        return repeatBeginPos;
    }

    private int convertNote(int level, boolean alted, int alt) {
        int rlevel = level + noteTranslate + middleOctave * noteMod;
        int octave = rlevel / noteMod;
        if (alted) mesureAlts[rlevel % noteMod] = alt;
        int demitons = noteEquiv[rlevel % noteMod] + mesureAlts[rlevel % noteMod];
        return octave * noteEquivMod + demitons;
    }

    private int convertDuree(double duree) {
        return (int) Math.round((60000 * tempoRelative) / (tempo * duree));
    }

    static final int instrumentIndex = 0;

    static final int velocity = 64;

    private boolean validNote(int note) {
        return (note >= 0) && (note < 128);
    }

    public void preparePlayFrame() {
        Iterator<JHumNoteEvent> i = notes.iterator();
        double len = 0;
        while (i.hasNext()) len += (double) i.next().delay / 10;
        myPlayFrame.setLength((int) len);
    }

    Semaphore playerBlocker = new Semaphore(1);

    Semaphore boolsAccess = new Semaphore(1);

    boolean playing = false;

    boolean paused = false;

    boolean wantReplay = false;

    boolean mustExit = false;

    boolean mustStop = false;

    public void safeStart() {
        boolean freePlayer = false;
        utils.log("safeStart boolsAccess acquire");
        boolsAccess.acquireUninterruptibly();
        utils.log("safeStart boolsAccess acquire OK");
        if (!playing) {
            playing = true;
            freePlayer = true;
        }
        if (paused) {
            paused = false;
            freePlayer = true;
        }
        boolsAccess.release();
        utils.log("safeStart: " + freePlayer);
        if (freePlayer) playerBlocker.release();
    }

    public void safeReplay() {
        boolean freePlayer = false;
        boolsAccess.acquireUninterruptibly();
        if (playing) {
            wantReplay = true;
            mustStop = true;
            if (paused) {
                freePlayer = true;
            }
        }
        boolsAccess.release();
        if (freePlayer) playerBlocker.release();
    }

    public void safePause() {
        utils.log("safePause boolsAccess acquire");
        boolsAccess.acquireUninterruptibly();
        utils.log("safePause boolsAccess acquire OK");
        if (playing && (!paused)) paused = true;
        boolsAccess.release();
    }

    public void safeStop() {
        boolean freePlayer = false;
        boolsAccess.acquireUninterruptibly();
        if (playing) {
            mustStop = true;
            if (paused) {
                freePlayer = true;
                paused = false;
            }
        }
        boolsAccess.release();
        if (freePlayer) playerBlocker.release();
    }

    public void safeEnd() {
        boolean freePlayer = false;
        boolsAccess.acquireUninterruptibly();
        mustExit = true;
        if ((paused) || (!playing)) {
            freePlayer = true;
            paused = false;
        }
        boolsAccess.release();
        utils.log("safeEnd: " + freePlayer);
        if (freePlayer) playerBlocker.release();
    }

    private void mayPause(boolean starting) {
        boolean pause = false;
        boolsAccess.acquireUninterruptibly();
        if (mustStop && starting) {
            pause = (paused && wantReplay) || (!paused && !wantReplay);
            playing = wantReplay;
            wantReplay = false;
            mustStop = false;
        } else if ((!playing) || (paused && !wantReplay)) pause = true;
        boolsAccess.release();
        if (pause) {
            playerBlocker.acquireUninterruptibly(2);
            playerBlocker.release();
        }
    }

    private void finished() {
        boolsAccess.acquireUninterruptibly();
        playing = false;
        if (!wantReplay) myPlayFrame.notifyFinished();
        boolsAccess.release();
    }

    private static Synthesizer synthesizer = null;

    private static MidiChannel[] midiChannels;

    private void initSynth() {
        if (synthesizer != null) return;
        utils.log("Creation (unique) du synthetiser");
        try {
            synthesizer = MidiSystem.getSynthesizer();
            synthesizer.open();
            Instrument[] instruments = synthesizer.getDefaultSoundbank().getInstruments();
            midiChannels = synthesizer.getChannels();
            Instrument instrument = instruments[instrumentIndex];
            synthesizer.loadInstrument(instrument);
            midiChannels[0].programChange(instrumentIndex);
        } catch (Exception e) {
            return;
        }
    }

    public void run() {
        utils.log("Nouveau thread JHumMidiPlayer!");
        initSynth();
        while (!mustExit) {
            myPlayFrame.updateTime(0);
            mayPause(true);
            utils.log("Commence � jouer");
            Iterator i = notes.iterator();
            double timer = 0;
            while (i.hasNext() && !mustExit && !mustStop) {
                JHumNoteEvent ne = (JHumNoteEvent) i.next();
                if (validNote(ne.note)) midiChannels[0].noteOn(ne.note, velocity);
                myScore.highlight(ne.noteIndex);
                try {
                    for (int j = 0; (j < ne.delay / DITHERDELAY) && !mustExit && !mustStop; j++) {
                        Thread.sleep(DITHERDELAY);
                        timer += (double) DITHERDELAY / 10;
                        mayPause(false);
                        myPlayFrame.updateTime((int) timer);
                    }
                    Thread.sleep(ne.delay % DITHERDELAY);
                    timer += (double) (ne.delay % DITHERDELAY) / 10;
                    myPlayFrame.updateTime((int) timer);
                    mayPause(false);
                } catch (InterruptedException e) {
                }
                if (validNote(ne.note)) midiChannels[0].noteOff(ne.note);
            }
            myScore.highlight(-1);
            utils.log("Fini de jouer");
            finished();
        }
        utils.log("JHumMidiPlayer: je suis mort!");
        notes.clear();
    }
}

class JHumNoteEvent {

    int note;

    int delay;

    int noteIndex;

    JHumNoteEvent(int note, int noteIndex, int delay) {
        utils.log("Creation note: #" + note + ", " + delay + "ms");
        this.note = note;
        this.delay = delay;
        this.noteIndex = noteIndex;
    }

    ;
}
