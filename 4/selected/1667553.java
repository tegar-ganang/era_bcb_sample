package com.frinika.audio.toot;

import javax.sound.midi.ShortMessage;

public class MidiHashUtil {

    public static long hashValue(ShortMessage mess) {
        long chn = mess.getChannel();
        long cmd = mess.getCommand();
        long cntrl;
        if (cmd == ShortMessage.CONTROL_CHANGE) cntrl = mess.getData1(); else if (cmd == ShortMessage.PITCH_BEND) cntrl = 0; else {
            System.out.println(" Don't know what to do with " + mess);
            return -1;
        }
        return ((chn << 8 + cmd) << 8) + cntrl;
    }

    public static void hashDisp(long hash) {
        long cntrl = hash & 0xFF;
        long cmd = (hash & 0xFF00) >> 8;
        long chn = (hash & 0xFF00) >> 16;
        System.out.println(chn + "  " + cmd + " " + cntrl);
    }
}
