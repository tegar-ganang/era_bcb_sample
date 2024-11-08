package org.chernovia.music.MIDICol;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;

public class ColTrack extends Thread {

    public boolean DEBUG = false;

    public static final int MICROMIN = 60000000, TEMPO = 81, TIME_SIG = 88, NOTE_OFF = 128, NOTE_ON1 = 144, NOTE_ON2 = 159;

    private static double millis_per_tick = .1, tfactor = .1;

    private static long tick = 0;

    private static int tempo = 120, tick_res = 24, beat = 8;

    private static Synthesizer synth;

    private Track track;

    private MIDIView viewer;

    private int trackNum;

    private MidiChannel chan;

    public ColTrack(Track t, int n, MIDIView v) {
        track = t;
        trackNum = n;
        viewer = v;
        setTempo(120, 36, 8);
        MidiChannel[] channels = synth.getChannels();
        if (trackNum < 16) chan = channels[trackNum]; else chan = channels[15];
    }

    public static void init() {
        try {
            synth = MidiSystem.getSynthesizer();
            synth.open();
            System.out.println(synth + ": " + synth.getLatency());
        } catch (Exception augh) {
        }
    }

    @Override
    public void run() {
        tick = 0;
        MidiEvent e;
        while (track.size() > 0) {
            e = track.get(0);
            handleMsg(e);
            track.remove(e);
        }
    }

    private synchronized void sleeper(long t) {
        if (t > tick) try {
            long m = (long) (millis_per_tick * (t - tick));
            if (DEBUG) System.out.println("Track " + trackNum + ": sleeping " + m + " milliseconds");
            sleep(m);
        } catch (Exception augh) {
            System.out.println("oops: " + augh);
        }
        tick = t;
    }

    private synchronized void handleMsg(MidiEvent e) {
        sleeper(e.getTick());
        MidiMessage msg = e.getMessage();
        int status = msg.getStatus();
        if (status >= NOTE_ON1 && status <= NOTE_ON2) {
            byte[] data = msg.getMessage();
            viewer.newNote(data[1] % 12);
            chan.noteOn(data[1], data[2]);
        }
        if (status == NOTE_OFF) {
            byte[] data = msg.getMessage();
            viewer.newNote(data[1] % 12);
            chan.noteOff(data[1]);
        } else if (status == MetaMessage.META) {
            byte[] data = msg.getMessage();
            int meta = data[1];
            if (meta == TEMPO) {
                setTempo(MICROMIN / ((data[3] << 16) + (data[4] << 8) + data[5]), tick_res, beat);
                System.out.println("Tempo change on track " + trackNum + " -> " + tempo + ", " + tick_res + ", " + "MPT: " + millis_per_tick);
            } else if (meta == TIME_SIG) {
                int b = (int) Math.pow(2, data[4]);
                System.out.println("Meter: " + data[3] + "/" + b);
                setTempo(tempo, data[5], b);
            }
        }
    }

    private static void setTempo(int t, int res, int b) {
        tempo = t;
        tick_res = res;
        beat = b;
        millis_per_tick = tfactor * (1000f / tick_res) * (60f / tempo) * (beat / 16f);
    }
}
