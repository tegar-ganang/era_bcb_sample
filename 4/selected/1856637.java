package jmms;

import grame.midishare.Midi;
import grame.midishare.MidiAppl;
import grame.midishare.MidiException;
import java.util.Enumeration;
import java.util.Hashtable;
import jm.midi.MidiInputListener;
import jm.midi.event.CChange;
import jm.midi.event.Event;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Score;
import jm.music.data.Tempo;
import ren.env.ValueGraphModel;
import ren.util.PO;

/**
 * this class lets you play a jmusic score using midishare
 */
public class Sequencer extends MidiAppl {

    private int nrefnum;

    private PlayTask playTask;

    private boolean tempoFromScore = false;

    public static int[] msrefnums = new int[10];

    public static int refnumCount = 0;

    private boolean bankStylePrgChg = true;

    private MidiInputListener mil;

    private Tempo tempo;

    private int ccrem = 0;

    private static int CCRes = 125;

    double midiTempo = 200.0;

    private TickTask tickTask;

    private int ccseq1, ccseq2 = Integer.MIN_VALUE;

    private boolean ccsw = false;

    public Sequencer() {
        super();
        for (int i = 0; i < volumes.length; i++) {
            volumes[i] = 64;
        }
        int appls = 0;
        try {
            this.Open("seq " + super.refnum);
        } catch (MidiException e) {
            System.out.println(e);
        }
        msrefnums[refnumCount++] = this.refnum;
        Midi.Connect(this.refnum, 0, 1);
        Midi.Connect(0, this.refnum, 1);
        PO.p("refnum = " + this.refnum);
        ccseq1 = Midi.NewSeq();
        ccseq2 = Midi.NewSeq();
        PO.p(" port 0 state " + Midi.GetPortState(0) + ".. port 1 state " + Midi.GetPortState(1));
    }

    public void ApplAlarm(int a) {
        PO.p("appl alarm " + a);
    }

    public void setTickTask(TickTask t) {
        this.tickTask = t;
    }

    public void setTempo(Tempo newTempo) {
        this.tempo = newTempo;
    }

    public void close() {
        Midi.Close(this.refnum);
    }

    long lastClock = 0;

    long thisClock = 0;

    int timeDif = 0;

    int clockAt = 0;

    private MidiInputListener[] mils = new MidiInputListener[60];

    private MidiNoteListener[] mnol = new MidiNoteListener[60];

    private int mnolCount = 0;

    private int milCount = 0;

    public void addMidiInputListener(MidiInputListener mil) {
        this.mils[milCount++] = mil;
    }

    public void addMidiNoteListener(MidiNoteListener m) {
        this.mnol[mnolCount++] = m;
    }

    public void removeMidiInputListener(MidiInputListener mil) {
        for (int i = 0; i < milCount; i++) {
            if (mils[i] == mil) {
                mils[i] = mils[i + 1];
                mil = mils[i];
            }
        }
        milCount--;
    }

    public void removeMidiInputListener(MidiNoteListener m) {
        for (int i = 0; i < mnolCount; i++) {
            if (mnol[i] == m) {
                mnol[i] = mnol[i + 1];
                m = mnol[i];
            }
        }
        milCount--;
    }

    public double getMidiTempo() {
        return midiTempo;
    }

    public int getMidiClockAt() {
        return clockAt;
    }

    int recieveTimeDif = 0;

    public boolean isRecievingClock() {
        recieveTimeDif = (int) (Midi.GetTime() - thisClock);
        return recieveTimeDif < 2000;
    }

    double stn = -1;

    int veln = 0;

    public void ReceiveAlarm(int event) {
        Event midiEvent = null;
        switch(Midi.GetType(event)) {
            case Midi.typeCtrlChange:
                {
                    midiEvent = new CChange((short) Midi.GetData0(event), (short) Midi.GetData1(event), (short) Midi.GetChan(event), (short) Midi.GetDate(event));
                    break;
                }
            case Midi.typeKeyOn:
                {
                    if (tickTask != null) {
                        stn = tickTask.getTick();
                        veln = Midi.GetData1(event);
                    }
                    break;
                }
            case Midi.typeKeyOff:
                {
                    if (stn != -1 && tickTask != null) {
                        pumpMidiNoteInput(stn, tickTask.getTick(), Midi.GetData0(event), veln, Midi.GetChan(event));
                        stn = -1;
                    }
                    break;
                }
            case Midi.typeClock:
                {
                    lastClock = thisClock;
                    thisClock = Midi.GetTime();
                    timeDif = (int) (thisClock - lastClock);
                    timeDif = timeDif * 6;
                    if (timeDif != 0) midiTempo = (15000 / timeDif);
                    clockAt++;
                    break;
                }
        }
        if (midiEvent != null) {
            for (int i = 0; i < milCount; i++) {
                this.mils[i].newEvent(midiEvent);
            }
        }
        Midi.FreeEv(event);
    }

    private void pumpMidiNoteInput(double sta, double end, int pitch, int velocity, int chan) {
        for (int i = 0; i < this.mnolCount; i++) {
            this.mnol[i].noteRecieved(new NoteEvent(pitch, this.tickTask.getRes(), (end - sta), velocity, sta, chan));
        }
    }

    public void panic() {
        System.out.println("panicing in sequencer");
        int ev;
        for (int i = 0; i < 16; i++) {
            ev = Midi.NewEv(Midi.typeCtrlChange);
            Midi.SetChan(ev, i);
            Midi.SetData0(ev, 123);
            Midi.SendIm(refnum, ev);
        }
    }

    /**
	 * this is the method that plays the score that you give it instantaneously
	 * It ignores the tempo of the score. You need to set that using the
	 * setTempo(double tempo) method
	 * 
	 * @param Score
	 *            s the score that is to be played
	 * 
	 * public void play(int date) { playSeq(seq, date); }
	 */
    boolean useSeq1 = true;

    Sequence sequence;

    Sequence sequence1 = new Sequence();

    Sequence sequence2 = new Sequence();

    public Sequence getMSSeq(Score s) {
        if (s == null) {
            System.out.println("score is null");
            return null;
        }
        updateTempo(s);
        if (useSeq1) sequence = sequence1; else sequence = sequence2;
        sequence.reset();
        for (int i = 0; i < s.size(); i++) {
            partIntoSeq(s.getPart(i), sequence);
        }
        useSeq1 = !useSeq1;
        if (tempo == null) PO.p("tempo null");
        ccrem = (inMillis(this.tickTask.getRes(), this.tempo.getPerMinute()) + ccrem) % this.CCRes;
        return sequence;
    }

    protected void updateTempo(Score s) {
        if (tempoFromScore) {
            tempo.setTempo(s.getTempo());
        } else {
        }
    }

    private int[] instruments = new int[96];

    private int[] volumes = new int[96];

    private transient int pchan;

    private void partIntoSeq(Part p, Sequence seq) {
        pchan = p.getChannel() - 1;
        if (pchan < 0) {
            try {
                Exception e = new Exception("part channel mus be between > 0 ");
                e.fillInStackTrace();
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ccenvIntoSeq(pchan, p.getCCEnvs(), seq);
        if (p.getSize() == 0) {
            return;
        }
        if (instruments[pchan] != p.getInstrument()) {
            if (bankStylePrgChg) {
                sendBankPrgChng(p.getInstrument(), pchan, refnumCount - 1);
            } else {
                sendPrgChng(p.getInstrument(), pchan);
            }
            instruments[pchan] = p.getInstrument();
        }
        if (volumes[pchan] != p.getVolume()) {
            sendVol(pchan, p.getVolume());
            volumes[pchan] = p.getVolume();
        }
        for (int i = 0; i < p.size(); i++) {
            phraseIntoSeq(p.getPhrase(i), pchan, seq);
        }
    }

    private void ccenvIntoSeq(int chan, Hashtable envs, Sequence seq) {
        int catf = 0;
        while (catf * this.CCRes + this.ccrem <= inMillis(this.tickTask.getRes(), this.tempo.getPerMinute())) {
            int currDate = catf * this.CCRes + this.ccrem;
            Enumeration en = envs.keys();
            while (en.hasMoreElements()) {
                Integer ctype = (Integer) en.nextElement();
                int cce = Midi.NewEv(Midi.typeCtrlChange);
                this.setPortChan(cce, chan);
                Midi.SetData0(cce, ctype.intValue());
                Midi.SetData1(cce, (int) (((ValueGraphModel) envs.get(ctype)).getValAt(currDate) + 0.5));
                Midi.SetDate(cce, currDate);
                seq.addMidishareEvent(cce);
            }
            catf++;
        }
    }

    private void phraseIntoSeq(Phrase phr, int channel, Sequence seq) {
        try {
            if (phr == null) new NullPointerException();
        } catch (NullPointerException e) {
            e.toString();
            return;
        }
        int dateOfEvent = 0;
        int ev = 0;
        double counter = 0.0;
        double startTime = phr.getStartTime();
        for (int i = 0; i < phr.size(); i++) {
            dateOfEvent = inMillis((counter + startTime), tempo.getPerMinute());
            Note n = (Note) phr.getNote(i);
            if (n.getPitch() > jm.JMC.REST + 60) {
                double idur = n.getDuration();
                int milDur = inMillis(idur, tempo.getPerMinute());
                ev = makeMSNote(n.getPitch(), n.getDynamic(), inMillis(n.getDuration(), tempo.getPerMinute()), dateOfEvent, channel);
                seq.addMidishareEvent(ev);
            }
            counter += n.getRhythmValue();
        }
    }

    public void sendPrgChng(int instrument, int channel) {
        if (channel < 1 || channel > 15) {
            try {
                Exception e = new Exception("part channel mus be between 1 and 16, or 0-15 if sending directly");
                e.fillInStackTrace();
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        int event = Midi.NewEv(Midi.typeProgChange);
        if (event != 0) {
            setPortChan(channel, event);
            Midi.SetField(event, 0, instrument);
            Midi.SetDate(event, 0);
        }
        Midi.SendIm(refnum, event);
    }

    private static void setPortChan(int chan, int event) {
        Midi.SetChan(event, (chan) % 16);
        Midi.SetPort(event, (int) ((chan) * 1.0 / 16.0));
    }

    /**
	 * static version is a bank style program change
	 */
    public void sendBankPrgChng(int instrument, int channel, int refnumIndex) {
        Integer bank = null;
        int prgChng = instrument;
        int event = Midi.NewEv(Midi.typeProgChange);
        if (event != 0) {
            setPortChan(channel, event);
            if (instrument > 999999) {
                prgChng = instrument % 1000;
                int lsb = (int) (instrument / 1000);
                lsb = lsb % 1000;
                int msb = instrument / 1000000;
                sendControllerData(channel, 0, msb, msrefnums[refnumIndex]);
                sendControllerData(channel, 32, lsb, msrefnums[refnumIndex]);
            } else if (instrument > 99) {
                bank = new Integer((100 + ((int) (instrument / 100))));
                prgChng = instrument % 100;
            }
            Midi.SetField(event, 0, prgChng);
            Midi.SetDate(event, 0);
        }
        if (bank != null) {
            int bevent = Midi.NewEv(Midi.typeProgChange);
            setPortChan(channel, bevent);
            Midi.SetField(bevent, 0, bank.intValue());
            Midi.SetDate(bevent, 0);
            Midi.SendIm(msrefnums[refnumIndex], bevent);
            Midi.SetDate(event, 2000);
        }
        Midi.SendIm(msrefnums[refnumIndex], event);
    }

    public void sendVol(int channel, int value) {
        int event = Midi.NewEv(Midi.typeCtrlChange);
        if (event != 0) {
            setPortChan(channel, event);
            Midi.SetField(event, 0, 7);
            Midi.SetField(event, 1, value);
            Midi.SetDate(event, 0);
        }
        Midi.SendIm(refnum, event);
    }

    public static void sendVol(int channel, int value, int refNumIndex) {
        int event = Midi.NewEv(Midi.typeCtrlChange);
        if (event != 0) {
            setPortChan(channel, event);
            Midi.SetField(event, 0, 7);
            Midi.SetField(event, 1, value);
            Midi.SetDate(event, 0);
        }
        Midi.SendIm(msrefnums[refNumIndex], event);
    }

    /**
	 * sonds MIDI controller messages
	 * 
	 * @param channel
	 * @param type
	 * @param value
	 * @param refNumIndex
	 */
    private static int[] evToDispose = new int[300];

    static {
        for (int i = 0; i < evToDispose.length; i++) {
            evToDispose[i] = -1;
        }
    }

    static int freeCount = 0;

    public static void sendControllerData(int channel, int type, int value, int refNumIndex) {
        if (value < 0 || value > 127) {
            try {
                Exception e = new Exception("ctrl value must be from 0-127");
                e.fillInStackTrace();
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        int event = Midi.NewEv(Midi.typeCtrlChange);
        if (event != 0) {
            setPortChan(channel, event);
            Midi.SetField(event, 0, type);
            Midi.SetField(event, 1, value);
            Midi.SendIm(msrefnums[refNumIndex], event);
        }
        freeCount++;
        freeCount = freeCount % 400;
    }

    public static int makeCtrlData(int channel, int type, int value, int date) {
        int event = Midi.NewEv(Midi.typeCtrlChange);
        if (event != 0) {
            setPortChan(channel, event);
            Midi.SetField(event, 0, type);
            Midi.SetField(event, 1, value);
            Midi.SetDate(event, date);
            return event;
        }
        return -1;
    }

    /**
	 * toggles the mode of wether to get the tempo from the score, or to rely
	 * soley on it being set. true means that the tempo will come from the score
	 *  
	 */
    public void setTempoFromScore(boolean b) {
        tempoFromScore = b;
    }

    /**
	 * converts from beats to milliseconds.  The tempo should be in BPM.
	 * @param toConvert
	 * @param tempo
	 * @return
	 */
    private int inMillis(double toConvert, double tempo) {
        double toRet = ((toConvert * 60000 / tempo));
        return (int) (toRet + 0.5);
    }

    int count = 0;

    private int makeMSNote(int pitch, int dynamic, int duration, int date, int channel) {
        int event = Midi.NewEv(Midi.typeNote);
        if (event != 0) {
            p("setting parameters");
            setPortChan(channel, event);
            Midi.SetField(event, 0, pitch);
            Midi.SetField(event, 1, dynamic);
            Midi.SetField(event, 2, duration);
            Midi.SetDate(event, date);
        }
        return event;
    }

    public void setMidiInput(MidiInputListener mil) {
        this.mil = mil;
    }

    private void p(String toPrint) {
    }

    /**
	 * 
	 * @param channel
	 * @param type
	 * @param val
	 * @param grad
	 * @param refnumindex
	 */
    public void sendCCtrlData(int channel, int type, int val, double grad, int refnumindex) {
        int tseq = -1;
        ccsw = !ccsw;
        if (ccsw) {
            tseq = ccseq1;
        } else {
            tseq = ccseq2;
        }
        Midi.ClearSeq(tseq);
        int ti = this.inMillis(this.tickTask.getRes(), this.tempo.getPerMinute());
        int nv = val;
        PO.p("grad = " + grad);
        for (int d = 0; d < ti; d += this.CCRes) {
            nv = val + (int) (((d / 1000.0) * grad) + 0.5);
            PO.p("date = " + d + " new value " + nv);
            Midi.AddSeq(this.makeCtrlData(channel, type, val, d), tseq);
        }
    }
}
