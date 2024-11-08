package org.jchunk;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class JChunk {

    public static final String VERSION = "0.1";

    public static final int IO_ERROR = 4;

    public static void printVersion() {
        System.out.println("Java binchunker, version " + VERSION + " by Sergiu Dumitriu");
        System.out.println("\tCreated starting from the bchunk program.");
        System.out.println("\t");
        System.out.println("\tReleased under the GNU GPL, version 3 or later (at your option).\n");
    }

    String progressbar(float f, int l) {
        StringBuffer s = new StringBuffer();
        int i, n;
        n = Math.round(l * f);
        for (i = 0; i < n; ++i) {
            s.append('*');
        }
        for (; i < l; i++) {
            s.append(' ');
        }
        return s.toString();
    }

    public int writetrack(FileInputStream bf, Track track, String bname, Params params) throws IOException {
        String fname;
        BufferedOutputStream f = null;
        byte buf[] = new byte[Constants.SECTLEN + 10];
        long sz, sect, realsz;
        int reallen;
        int p, p2, ep;
        byte c;
        int l;
        short i;
        float fl;
        fname = String.format("%s%02d.%s", new Object[] { bname, Integer.valueOf(track.getNum()), track.getExtension() });
        System.out.println(String.format("%2d: %s", new Object[] { Integer.valueOf(track.getNum()), fname }));
        try {
            f = new BufferedOutputStream(new FileOutputStream(fname));
        } catch (FileNotFoundException ex1) {
            System.err.println(" Could not fopen track file \"" + fname + "\": " + ex1.getLocalizedMessage());
            return IO_ERROR;
        }
        bf.getChannel().position(0);
        if (bf.skip(track.getStart()) != track.getStart()) {
            System.err.println(" Could not skip to track location");
            return IO_ERROR;
        }
        reallen = (track.getStopsect() - track.getStartsect() + 1) * track.getBsize();
        if (params.isVerbose()) {
            System.out.println(" mmc sectors " + track.getStartsect() + "->" + track.getStopsect() + " (" + (track.getStopsect() - track.getStartsect() + 1) + ")");
            System.out.println(" mmc bytes " + track.getStart() + "->" + track.getStop() + " (" + (track.getStop() - track.getStart() + 1) + ")");
            System.out.println(" sector data at " + track.getBstart() + ", " + track.getBsize() + " bytes per sector");
            System.out.println(" real data " + ((track.getStopsect() - track.getStartsect() + 1) * track.getBsize()) + " bytes");
            System.out.println();
        }
        System.out.print("                                          ");
        if (track.isAudio() && params.isToWav()) {
            f.write("RIFF".getBytes());
            l = reallen + Constants.WAV_DATA_HLEN + Constants.WAV_FORMAT_HLEN + 4;
            f.write(Helper.getIntBytes(l));
            f.write("WAVE".getBytes());
            f.write("fmt ".getBytes());
            l = 0x10;
            f.write(Helper.getIntBytes(l));
            i = 0x01;
            f.write(Helper.getShortBytes(i));
            i = 0x02;
            f.write(Helper.getShortBytes(i));
            l = 44100;
            f.write(Helper.getIntBytes(l));
            l = 44100 * 4;
            f.write(Helper.getIntBytes(l));
            i = 4;
            f.write(Helper.getShortBytes(i));
            i = 2 * 8;
            f.write(Helper.getShortBytes(i));
            f.write("data".getBytes());
            l = reallen;
            f.write(Helper.getIntBytes(l));
        }
        realsz = 0;
        sz = track.getStart();
        sect = track.getStartsect();
        fl = 0;
        while ((sect <= track.getStopsect()) && (bf.read(buf, 0, Constants.SECTLEN) > 0)) {
            if (track.isAudio()) {
                if (params.isSwabAudio()) {
                    p = track.getBstart();
                    ep = p + track.getBsize();
                    while (p < ep) {
                        p2 = p + 1;
                        c = buf[p];
                        buf[p] = buf[p2];
                        buf[p2] = c;
                        p += 2;
                    }
                }
            }
            try {
                f.write(buf, track.getBstart(), track.getBsize());
            } catch (IOException ex) {
                System.err.println(" Could not write to track: " + ex.getLocalizedMessage());
                System.exit(4);
            }
            sect++;
            sz += Constants.SECTLEN;
            realsz += track.getBsize();
            if (((sz / Constants.SECTLEN) % 500) == 0) {
                fl = (float) realsz / (float) reallen;
                System.out.print(String.format("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b%4d/%-4d MB  [%s] %3.0f %%", new Object[] { new Long(realsz >> 20), new Long(reallen >> 20), progressbar(fl, 20), new Float(fl * 100) }));
                System.out.flush();
            }
        }
        fl = (float) realsz / (float) reallen;
        System.out.print(String.format("\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b\b%4d/%-4d MB  [%s] %3.0f %%", new Object[] { new Long(realsz >> 20), new Long(reallen >> 20), progressbar(1, 20), new Float(fl * 100) }));
        System.out.flush();
        System.out.println();
        return 0;
    }

    public static void main(String[] args) {
        try {
            new JChunk().process(args);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void process(String[] args) throws Exception {
        String s;
        int p, t;
        int idx;
        Cue cue = new Cue();
        Track track = null;
        Track prevtrack = null;
        BufferedReader cuef;
        FileInputStream binf;
        printVersion();
        Params params = new Params();
        if (!params.parseArgs(args)) {
            return;
        }
        try {
            binf = new FileInputStream(params.getBinfile());
        } catch (IOException ex) {
            System.err.println("Could not open BIN " + params.getBinfile() + ": " + ex.getLocalizedMessage());
            return;
        } catch (NullPointerException ex) {
            System.err.println("No BIN file defined.");
            params.usage();
            return;
        }
        try {
            cuef = new BufferedReader(new InputStreamReader(new FileInputStream(params.getCuefile())));
        } catch (IOException ex) {
            System.err.println("Could not open CUE " + params.getCuefile() + ": " + ex.getLocalizedMessage());
            return;
        } catch (NullPointerException ex) {
            System.err.println("No CUE file defined.");
            params.usage();
            return;
        }
        System.out.println("Reading the CUE file:");
        if (cuef.readLine() == null) {
            System.err.println("Could not read first line from " + params.getCuefile());
            return;
        }
        while ((s = cuef.readLine()) != null) {
            while (((p = s.indexOf('\r')) != -1) || ((p = s.indexOf('\n')) != -1)) {
                s = s.substring(p + 1);
            }
            if ((p = s.indexOf("TRACK")) != -1) {
                System.out.print("\nTrack ");
                if ((p = s.indexOf(' ', p)) == -1) {
                    System.err.println("... ouch, no space after TRACK.");
                    continue;
                }
                p++;
                if ((t = s.indexOf(' ', p)) == -1) {
                    System.out.println("... ouch, no space after track number.");
                    continue;
                }
                prevtrack = track;
                track = new Track();
                cue.addTrack(track);
                track.setNum(Integer.parseInt(s.substring(p, t)));
                p = t + 1;
                s = s.substring(p);
                System.out.print(String.format("%2d: %-12.12s ", new Object[] { new Integer(track.getNum()), s }));
                track.setModes(s);
                cue.getTrackMode(track, s, params);
            } else if ((p = s.indexOf("INDEX")) != -1) {
                if (track == null) {
                    System.err.println("... ouch, misplaced INDEX.");
                    continue;
                }
                if ((p = s.indexOf(' ', p)) == -1) {
                    System.err.println("... ouch, no space after TRACK.");
                    continue;
                }
                p++;
                if ((t = s.indexOf(' ', p)) == -1) {
                    System.out.println("... ouch, no space after track number.");
                    continue;
                }
                idx = Integer.parseInt(s.substring(p, t));
                s = s.substring(t + 1);
                System.out.print(String.format(" %02d %s", new Object[] { new Integer(idx), s }));
                track.setStartsect(Helper.time2frames(s));
                track.setStart(track.getStartsect() * Constants.SECTLEN);
                if (params.isVerbose()) {
                    System.out.print(" (startsect " + track.getStartsect() + " ofs " + track.getStart());
                }
                if ((prevtrack != null) && (prevtrack.getStopsect() < 0)) {
                    prevtrack.setStopsect(track.getStartsect());
                    prevtrack.setStop(track.getStart() - 1);
                }
            }
        }
        if (track != null) {
            track.setStop((int) binf.getChannel().size());
            track.setStopsect(track.getStop() / Constants.SECTLEN);
        }
        System.out.println("\n");
        System.out.println("Writing tracks:\n");
        for (track = cue.getFirstTrack(); cue.hasMoreTracks(); track = cue.getNextTrack()) {
            writetrack(binf, track, params.getBasefile(), params);
        }
        binf.close();
        cuef.close();
    }
}
