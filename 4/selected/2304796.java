package org.chernovia.games.arcade.loxball;

import org.chernovia.lib.music.midi.JMIDI;

public class LoxSonifier extends Thread {

    public static int[] instruments = { 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46, 46 };

    static int BASS_CHAN = 0, ARP_CHAN = 1, MEL_CHAN = 2, MAX_CHAN = 16, MAX_VOL = 127;

    static int[] ARP_LENS = { 4, 3, 2 };

    int[][] pitches;

    long tempo = 2000;

    int bass = 50, melody = 100;

    int beat, sub_beat, meter, sub_meter;

    static boolean RUNNING = false;

    public LoxSonifier(int notes, int interval) {
        for (int i = 0; i < ARP_LENS.length; i++) {
            if (notes % ARP_LENS[i] == 0) {
                sub_meter = ARP_LENS[i];
                break;
            }
        }
        if (sub_meter == 0) sub_meter = notes;
        meter = notes / sub_meter;
        pitches = new int[meter][sub_meter];
        for (int i = 0; i < meter; i++) for (int n = 0; n < sub_meter; n++) {
            pitches[i][n] = (bass + 16) + (n * interval);
        }
        beat = 0;
        sub_beat = 0;
    }

    public static boolean init() {
        JMIDI.load();
        if (!JMIDI.isReady()) return false;
        for (int i = 0; i < MAX_CHAN; i++) JMIDI.setChannel(i, instruments[i]);
        return true;
    }

    @Override
    public void run() {
        RUNNING = true;
        while (RUNNING) playNextPitch();
    }

    public static void stopRun() {
        RUNNING = false;
    }

    public void changeTempo(long new_temp) {
        if (RUNNING) tempo = new_temp;
    }

    public void changePitch(int note, int interval, boolean down) {
        if (!RUNNING) return;
        int changeBeat = note / sub_meter, changeSubBeat = note % sub_meter;
        boolean downOK = true, upOK = true;
        if (pitches[changeBeat][changeSubBeat] >= (melody - 12)) upOK = false;
        if (pitches[changeBeat][changeSubBeat] <= (bass + 12)) downOK = false;
        if (changeSubBeat < sub_meter - 1 && pitches[changeBeat][changeSubBeat] == pitches[changeBeat][changeSubBeat + 1] - interval) upOK = false;
        if (changeSubBeat > 0 && pitches[changeBeat][changeSubBeat] == pitches[changeBeat][changeSubBeat - 1] + interval) downOK = false;
        int newInterval = interval;
        if (down) newInterval = -interval;
        if (!upOK && !downOK) newInterval = 0; else if (!upOK) newInterval = -interval; else if (!downOK) newInterval = interval;
        pitches[changeBeat][changeSubBeat] += newInterval;
    }

    public void ping(int note, LoxBall ball) {
        if (!RUNNING) return;
        int pingBeat = note / sub_meter, pingSubBeat = note % sub_meter;
        int pitch;
        long delay;
        if (ball.getState() == 0) {
            pitch = 33;
            delay = 50;
        } else {
            pitch = pitches[pingBeat][pingSubBeat] + (int) (24 / ((ball.getMaxState() - 1) / (float) ball.getState()));
            delay = 25;
        }
        JMIDI.getChannel(beat).noteOn(pitch, MAX_VOL);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ignore) {
        }
        JMIDI.getChannel(beat).noteOff(pitch);
    }

    private void playNextPitch() {
        int pitch = pitches[beat][sub_beat];
        JMIDI.getChannel(sub_beat).noteOn(pitch, 75);
        try {
            Thread.sleep(tempo / sub_meter);
        } catch (InterruptedException ignore) {
        }
        JMIDI.getChannel(sub_beat).noteOff(pitch);
        if (++sub_beat >= sub_meter) {
            sub_beat = 0;
            if (++beat >= meter) beat = 0;
        }
    }
}
