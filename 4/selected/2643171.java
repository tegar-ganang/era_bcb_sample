package org.chernovia.sims.ca.fishbowl;

import java.util.Vector;
import org.chernovia.lib.graphics.lib3d.P2D;
import org.chernovia.lib.misc.MiscUtil;
import org.chernovia.lib.music.midi.JMIDI;
import org.chernovia.lib.sims.ca.fishbowl.Fish2D;
import org.chernovia.lib.sims.ca.fishbowl.FishBowl2D;

public class P2D_Sonifyer {

    public static final int SONIC_DIST = 0, SONIC_PROX = 1, SONIC_X = 2;

    public static final String[] SONIC_OPT = { "Dist", "Prox", "X" };

    public static final int ORCH_PIANOS = 0, ORCH_CLAR = 1, ORCH_ORCH = 2;

    public static final String[] ORCH_OPT = { "Pianos", "Clarinets", "Orchestra" };

    public static final int INT_MAJ_THIRD = 0, INT_WHOLE = 1, INT_OCT = 2, INT_PENT = 3, INT_ALL = 4, INT_RAND = 5;

    public static final String[] INT_OPT = { "Major 3rds", "Whole", "Octaves", "Pent", "All", "Rand" };

    public static final int[] allInts = { 1, -1, 2, -2, 3, -3, 4, -4, 5, -5, 6, -6, 7, -7, 8, 8 };

    public static final int[] pentatonic = { 1, -1, 4, -4, 7, -7, 2, -2, 1, -1, 4, -4, 7, -7, 2, -2 };

    public static final int[] wholeTones = { 2, -2, 4, -4, 6, -6, 8, -8, 10, -10, 12, -12, 2, -2, 2, -2 };

    public static final int[] majorThirds = { 3, -3, 4, -4, 6, -6, 8, -8, 9, -9, 12, -12, 12, -12, 12, -12 };

    public static final int[] octaves = { 12, -12, 12, -12, 12, -12, 12, -12, 12, -12, 12, -12, 12, -12, 12, -12 };

    public static final int[][] allIntervals = { majorThirds, wholeTones, octaves, pentatonic, allInts };

    public static final int[] pianos = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    public static final int[] clars = { 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71, 71 };

    public static final int[] orch = { 49, 49, 0, 71, 60, 41, 68, 70, 42, 57, 0, 0, 0, 0, 0, 0 };

    public static final int[][] orchestras = { pianos, clars, orch };

    public boolean DEBUG = false, RAND_INT = true, BREAK_COLL = false, TRIADS = false, ANGLES = false;

    public int MAX_VOL = 100, MAX_CHAN = 16, MAX_ROW = 8, MAX_INT = 12, MAX_POLY = 16;

    private P2D centerPt;

    private int[] Orchestra = null, intervals = null;

    private int style = SONIC_DIST;

    private int randIntPct = 1, flipProb = 25, tick = 0, bounceCount = 0;

    private int pitch_base, pitch_range;

    private int[][] currentTriad;

    private int[] currentPitch;

    private boolean[] currentAngleDir;

    private int[] fishTempi;

    private int[] pitchCenters;

    private Vector<Integer> bowl_pitch_set;

    private int currentBowlPitch;

    private int currentInterval = -1, currentOrchestra = -1;

    private int lastCollision = Fish2D.NO_FISH;

    public P2D_Sonifyer(int pit_base, int pit_range) {
        if (!JMIDI.isReady()) JMIDI.load();
        pitch_base = pit_base;
        pitch_range = pit_range;
        centerPt = new P2D(0, 0);
        setOrchestra(ORCH_ORCH);
        setIntervals(INT_ALL);
        currentBowlPitch = pitch_base + (int) (Math.random() * pitch_range);
        bowl_pitch_set = new Vector<Integer>();
        currentPitch = new int[MAX_CHAN];
        currentTriad = new int[MAX_CHAN][3];
        currentAngleDir = new boolean[MAX_CHAN];
        fishTempi = new int[MAX_CHAN];
        pitchCenters = new int[MAX_CHAN];
        for (int i = 0; i < MAX_CHAN; i++) {
            currentPitch[i] = pitch_base + (int) (pitch_range / 3.0) + (int) (Math.random() * (pitch_range / 3.0));
            pitchCenters[i] = currentPitch[i];
            fishTempi[i] = 16;
            if (DEBUG) {
                System.out.println("Current Pitch #" + i + ": " + currentPitch[i]);
                System.out.println("Current Interval #" + i + ": " + intervals[i]);
            }
            JMIDI.getChannel(i).allNotesOff();
        }
    }

    public void setStyle(int s) {
        if (style == s) return;
        style = s;
        System.out.println("Style: " + SONIC_OPT[style]);
    }

    public void setRowSize(int r) {
        if (MAX_ROW == r) return;
        MAX_ROW = r;
        if (MAX_ROW < bowl_pitch_set.size()) bowl_pitch_set.clear();
        System.out.println("Row Size: " + MAX_ROW);
    }

    public void setIntervals(int ival) {
        if (currentInterval == ival) return; else currentInterval = ival;
        if (ival == INT_RAND) {
            intervals = new int[MAX_CHAN];
            System.out.print("New intervals: ");
            for (int i = 0; i < MAX_CHAN; i++) {
                intervals[i] = (int) (-MAX_INT + (Math.random() * (MAX_INT * 2)));
                System.out.print(intervals[i] + " ");
            }
            System.out.println();
        } else {
            intervals = allIntervals[currentInterval];
        }
        System.out.println("Set intervals: " + INT_OPT[currentInterval]);
    }

    public void setPoly(int p) {
        if (p == MAX_POLY) return; else MAX_POLY = p;
        System.out.println("Max polyphony: " + MAX_POLY);
    }

    public void setOrchestra(int o) {
        if (o == currentOrchestra) return;
        currentOrchestra = o;
        Orchestra = orchestras[currentOrchestra];
        try {
            for (int i = 0; i < Orchestra.length; i++) JMIDI.setChannel(i, Orchestra[i]);
        } catch (Exception augh) {
            System.out.println("MIDI error: make sure you've a soundbank installed!");
            System.out.println(augh);
        }
        System.out.println("Set orchestra: " + ORCH_OPT[currentOrchestra]);
    }

    private void doCollision(FishBowl2D bowl, int i, int f) {
        int rowsize = bowl_pitch_set.size();
        lastCollision = f;
        if (rowsize > 0) {
            currentBowlPitch = (bowl_pitch_set.elementAt(rowsize - 1)).intValue();
        }
        if ((Math.random() * 100) < randIntPct) {
            double randInt = -MAX_INT + (Math.random() * (MAX_INT * 2));
            currentBowlPitch += (int) randInt;
            if (DEBUG) {
                System.out.println("Random int: " + (int) randInt);
            }
        } else currentBowlPitch += intervals[f];
        if (currentBowlPitch < pitch_base || currentBowlPitch > (pitch_base + pitch_range)) {
            currentBowlPitch = pitchCenters[f];
        }
        bowl_pitch_set.add(new Integer(currentBowlPitch));
        if (bowl_pitch_set.size() > MAX_ROW) bowl_pitch_set.remove(0);
        if (DEBUG) {
            System.out.println("Collision, Fish #" + i + " -> " + "Fish #" + f);
            System.out.println("Interval: " + intervals[f]);
            System.out.println("Time: " + bowl.getTick());
            System.out.println("New pitch: " + currentBowlPitch);
            System.out.println("New pitch vector: " + bowl_pitch_set.toString());
        }
        if (currentInterval == INT_RAND && ++bounceCount > MAX_ROW) {
            bounceCount = 0;
            currentInterval = -1;
            setIntervals(INT_RAND);
        }
    }

    private void sonifyTriads(FishBowl2D bowl, int i, double pt_min, double pt_max, double max_dist) {
        int rowsize = bowl_pitch_set.size();
        int[] oldTriad = new int[3];
        for (int t = 0; t < 2; t++) oldTriad[t] = currentTriad[i][t];
        double prox = bowl.avgFishProx(i);
        double x = MiscUtil.mapValueToRange(bowl.getFish(i).x, pt_min, pt_max, 0, rowsize);
        double y = MiscUtil.mapValueToRange(bowl.getFish(i).y, pt_min, pt_max, 0, rowsize);
        double p = MiscUtil.mapValueToRange(prox, 0, max_dist * 2, 0, rowsize);
        double vol = MiscUtil.mapValueToRange(prox, 0, pt_max * 2, 0, MAX_VOL);
        currentTriad[i][0] = (bowl_pitch_set.elementAt((int) x)).intValue();
        currentTriad[i][1] = (bowl_pitch_set.elementAt((int) y)).intValue();
        currentTriad[i][2] = (bowl_pitch_set.elementAt((int) p)).intValue();
        for (int t = 0; t < 2; t++) {
            if (i < MAX_POLY && currentTriad[i][t] != oldTriad[t]) {
                JMIDI.getChannel(i).noteOff(oldTriad[t]);
                JMIDI.getChannel(i).noteOn(currentTriad[i][t], MAX_VOL - (int) vol);
            }
        }
    }

    private void sonifyPitch(FishBowl2D bowl, int i, double pt_min, double pt_max, double max_dist) {
        int oldPitch = currentPitch[i];
        int rowsize = bowl_pitch_set.size();
        double distFromCenter = bowl.getFish(i).distance(centerPt);
        double prox = bowl.avgFishProx(i);
        double idx = 0;
        switch(style) {
            case SONIC_DIST:
                idx = MiscUtil.mapValueToRange(distFromCenter, 0, max_dist, 0, rowsize);
                break;
            case SONIC_PROX:
                idx = MiscUtil.mapValueToRange(prox, 0, max_dist * 2, 0, rowsize);
                break;
            case SONIC_X:
                idx = MiscUtil.mapValueToRange(bowl.getFish(i).x, pt_min, pt_max, 0, rowsize);
                break;
        }
        double v = MiscUtil.mapValueToRange(prox, 0, pt_max * 2, 0, MAX_VOL);
        currentPitch[i] = (bowl_pitch_set.elementAt((int) idx)).intValue();
        if (i < MAX_POLY && currentPitch[i] != oldPitch) {
            JMIDI.getChannel(i).noteOff(oldPitch);
            JMIDI.getChannel(i).noteOn(currentPitch[i], MAX_VOL - (int) v);
        }
    }

    public void sonify(FishBowl2D bowl) {
        tick++;
        if (tick > 10000) tick = 0;
        int num_fish = bowl.getNumFish();
        double pt_min = -bowl.getBowlSize();
        double pt_max = bowl.getBowlSize();
        double max_dist = new P2D(pt_max, pt_max).distance(centerPt);
        int loopdir = 1, loopexit = num_fish, loopinit = 0;
        if ((Math.random() * 100) < flipProb) {
            loopdir = -1;
            loopexit = -1;
            loopinit = num_fish - 1;
        }
        for (int i = loopinit; i != loopexit; i += loopdir) {
            int f = bowl.getFish(i).getCollided();
            if (ANGLES) {
                if (i < MAX_POLY) sonifyAngle(bowl, i);
            } else if (f != Fish2D.NO_FISH && (!BREAK_COLL || (f != lastCollision && i != lastCollision))) {
                doCollision(bowl, i, f);
                if (BREAK_COLL) break;
            } else if (TRIADS && bowl_pitch_set.size() > 1) {
                sonifyTriads(bowl, i, pt_min, pt_max, max_dist);
            } else if (bowl_pitch_set.size() > 1) {
                sonifyPitch(bowl, i, pt_min, pt_max, max_dist);
            }
        }
    }

    private void sonifyAngle(FishBowl2D bowl, int fish) {
        int oldPitch = currentPitch[fish];
        P2D vec = bowl.getFish(fish).getVec();
        double angle = new P2D(0, 0).angle(vec);
        double degrees = Math.toDegrees(angle) + 90;
        currentPitch[fish] = (int) MiscUtil.mapValueToRange(degrees, 0, 180, pitch_base, pitch_base + pitch_range);
        if (currentPitch[fish] != oldPitch) {
            if (currentPitch[fish] > oldPitch) currentAngleDir[fish] = true; else currentAngleDir[fish] = false;
            int vol = (int) (bowl.getFish(fish).getVolatility() / 4.0);
            if (tick % vol == 0) {
                JMIDI.getChannel(fish).allNotesOff();
                JMIDI.getChannel(fish).noteOn(currentPitch[fish], MAX_VOL);
            }
        }
    }

    public void silence() {
        JMIDI.silence();
    }
}
