package org.chernovia.sims.wondrous;

import org.chernovia.lib.misc.MiscUtil;
import org.chernovia.lib.music.midi.JMIDI;

public class JWondrousPlayer extends Thread {

    public static final int DEF_LIMIT = 600, DEF_MULT = 9, DEF_VOL = 50;

    public static final int[] orchestra = { 71, 0, 40, 68, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    private int current_pitch, pitch_base, pitch_range;

    private int chan, inst, start_val, end_val, vol, limit, mult;

    private long speed;

    public static void main(String[] args) {
        playChan(0, 0, 88, 250, 33, 66, 101, 999, 200, 3);
    }

    public static void rndCanon() {
        canon((int) (Math.random() * 8), (int) (Math.random() * 500), (int) (Math.random() * 5000), 33, 64, (int) (Math.random() * 1000), (int) (Math.random() * 1000), 200, 3, true);
    }

    public static void canon(int players, int spd, long waittime, int pit_base, int pit_range, int startValue, int range, int lim, int m, boolean randInst) {
        JWondrousPlayer[] JWoP = new JWondrousPlayer[players];
        int instr = 0;
        for (int i = 0; i < players; i++) {
            if (randInst) instr = (int) (Math.random() * 88); else instr = orchestra[i];
            JWoP[i] = new JWondrousPlayer(i, instr, DEF_VOL, spd, pit_base, pit_range, startValue, startValue + range, lim, m);
        }
        for (int i = 0; i < players; i++) {
            JWoP[i].start();
            try {
                Thread.sleep(waittime);
            } catch (InterruptedException ignore) {
            }
        }
    }

    public static void rndPlay(int players) {
        for (int ch = 0; ch < players; ch++) {
            int startVal = (int) (Math.random() * 1000);
            int m = 3;
            if (Math.random() < .5) m = 3 + ((int) (Math.random() * 3) * 2);
            playChan(ch, (int) (Math.random() * 88), DEF_VOL, 100 + (int) (Math.random() * 400), 33 + (int) (Math.random() * 25), 24 + (int) (Math.random() * 66), startVal, startVal + 1000, 100 + (int) (Math.random() * 400), m);
        }
    }

    public static void simplePlay() {
        playChan(0, orchestra[0], DEF_VOL, 250, 33, 64, 100, 1000, DEF_LIMIT, DEF_MULT);
    }

    public static void playChan(int ch, int i, int v, long spd, int pitchBase, int pitchRange, int startVal, int endVal, int lim, int m) {
        new JWondrousPlayer(ch, i, v, spd, pitchBase, pitchRange, startVal, endVal, lim, m).start();
    }

    public JWondrousPlayer(int ch, int i, int v, long spd, int pitchBase, int pitchRange, int startVal, int endVal, int lim, int m) {
        if (!JMIDI.isReady()) {
            JMIDI.load();
            System.out.println("---------------------------");
        }
        chan = ch;
        inst = i;
        vol = v;
        speed = spd;
        current_pitch = 0;
        pitch_base = pitchBase;
        pitch_range = pitchRange;
        start_val = startVal;
        end_val = endVal;
        limit = lim;
        mult = m;
    }

    @Override
    public void run() {
        initRun();
        for (int p = start_val; p < end_val; p++) {
            int w = JWondrous.wondrousness(p, limit, mult);
            current_pitch = playPitch(w);
            try {
                sleep(speed);
            } catch (InterruptedException augh) {
                return;
            }
        }
        JMIDI.getChannel(chan).allNotesOff();
        System.out.println("Channel " + chan + " done.");
    }

    private synchronized void initRun() {
        JMIDI.setChannel(chan, inst);
        System.out.println("Starting chan " + chan + "...");
        System.out.println("Settings:");
        System.out.println("Volume: " + vol + " Speed: " + speed);
        System.out.println("Pitch Base: " + pitch_base + " Pitch Range: " + pitch_range);
        System.out.println("Start Value: " + start_val + " End Value: " + end_val);
        System.out.println("Limit: " + limit + " Mult: " + mult);
        System.out.println("***");
    }

    private int playPitch(int val) {
        int new_pitch = (int) MiscUtil.mapValueToRange(val, 0, limit, pitch_base, pitch_base + pitch_range);
        if (new_pitch != current_pitch) {
            JMIDI.getChannel(chan).noteOff(current_pitch);
            JMIDI.getChannel(chan).noteOn(new_pitch, vol);
        }
        return new_pitch;
    }
}
