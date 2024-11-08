package ren.io;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.sound.midi.spi.MidiFileReader;
import jm.music.data.Note;
import jm.music.data.Part;
import jm.music.data.Phrase;
import jm.music.data.Score;
import ren.util.PO;

public class Midi {

    MidiFileReader mifir;

    private int limit;

    private static Midi inst = new Midi();

    private boolean verbose = true;

    private int res = 0;

    public Midi() {
        super();
    }

    public static Midi getInstance() {
        return inst;
    }

    public void read(Score s, String fileName) {
        read(s, fileName, Integer.MAX_VALUE);
    }

    public void read(Score s, String fileName, int limit) {
        limit = limit;
        s.empty();
        try {
            System.out.println("--------------------- Reading MIDI File ---------------------");
            InputStream is = new FileInputStream(fileName);
            is = new BufferedInputStream(is, 1024);
            MidiFileFormat mff = MidiSystem.getMidiFileFormat(is);
            if (verbose) PO.p(" midi file type = " + mff.getType());
            if (verbose) {
                if (mff.getDivisionType() == Sequence.PPQ) {
                    res = mff.getResolution();
                    PO.p("div = PPQ. res = " + res);
                } else if (mff.getDivisionType() == Sequence.SMPTE_24 || mff.getDivisionType() == Sequence.SMPTE_25 || mff.getDivisionType() == Sequence.SMPTE_30 || mff.getDivisionType() == Sequence.SMPTE_30DROP) {
                    PO.p("div = smpte");
                }
            }
            Sequence seq = MidiSystem.getSequence(is);
            seqToScore(seq, s);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    private void seqToScore(Sequence seq, Score sco) {
        sco.empty();
        Track[] tra = seq.getTracks();
        Part[] pc = new Part[16];
        for (int i = 0; i < pc.length; i++) {
            pc[i] = new Part();
            pc[i].setChannel(i + 1);
        }
        long[][][] nons = new long[16][128][2];
        for (int i = 0; i < tra.length; i++) {
            for (int ch = 0; ch < nons.length; ch++) {
                for (int pi = 0; pi < nons[ch].length; pi++) {
                    nons[ch][pi][0] = -1;
                    nons[ch][pi][1] = -1;
                }
            }
            trackIntoParts(tra[i], pc, nons);
        }
        for (int i = 0; i < pc.length; i++) {
            if (pc[i].length() > 0) {
                sco.add(pc[i]);
            }
        }
    }

    /**
	 * 
	 * @param t track to get the data from
	 * @param p part to put the data into.  must be of length 16, 
	 * 			one for each midi channel
	 */
    private void trackIntoParts(Track t, Part[] p, long[][][] nons) {
        for (int i = 0; i < t.size(); i++) {
            MidiEvent ev = t.get(i);
            ShortMessage mess;
            if (ev.getMessage() instanceof ShortMessage) {
                mess = (ShortMessage) ev.getMessage();
                if (mess.getCommand() == ShortMessage.NOTE_ON && mess.getData2() > 0) {
                    if (nons[mess.getChannel()][mess.getData1()][0] != -1) {
                        p("WARNING: two note on " + "messages with same pitch " + mess.getData1() + " chan = " + mess.getChannel() + " vel = " + mess.getData2() + " date orig " + nons[mess.getChannel()][mess.getData1()][1] * 1.0 / 96.0 + " date new " + ev.getTick() * 1.0 / 96.0 + ". creating new note.");
                        createNote(nons, (ev.getTick() * 1.0 / this.res * 1.0) * 0.9, mess, p);
                    }
                    nons[mess.getChannel()][mess.getData1()][0] = mess.getData2();
                    nons[mess.getChannel()][mess.getData1()][1] = ev.getTick();
                } else if (mess.getCommand() == ShortMessage.NOTE_OFF || (mess.getCommand() == ShortMessage.NOTE_ON && mess.getData2() == 0)) {
                    if (nons[mess.getChannel()][mess.getData1()][0] == -1) {
                        System.out.println(" note off message " + mess.getData1() + ", without a note on message!");
                    } else {
                        createNote(nons, (ev.getTick() * 1.0 / this.res * 1.0), mess, p);
                    }
                } else if (mess.getCommand() == ShortMessage.PROGRAM_CHANGE) {
                    p[mess.getChannel()].setInstrument(mess.getData1());
                }
            }
        }
    }

    private void createNote(long[][][] nons, double endTime, ShortMessage mess, Part[] p) {
        Phrase phr = new Phrase(nons[mess.getChannel()][mess.getData1()][1] * 1.0 / this.res * 1.0);
        Note n = new Note();
        n.setPitch(mess.getData1());
        n.setDuration(endTime - phr.getStartTime());
        n.setDynamic((int) nons[mess.getChannel()][mess.getData1()][0]);
        phr.add(n);
        p[mess.getChannel()].add(phr);
        nons[mess.getChannel()][mess.getData1()][0] = -1;
        nons[mess.getChannel()][mess.getData1()][1] = -1;
    }

    private void p(String p) {
        System.out.println(p);
    }
}
