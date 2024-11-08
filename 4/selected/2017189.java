package eu.davidgamez.mas.midi;

import javax.sound.midi.*;
import eu.davidgamez.mas.*;

/**-------------------------------- Utilities ----------------------------------
   General purpose utility methods applied throughout the application.
 -----------------------------------------------------------------------------*/
public class Utilities implements Constants {

    public Utilities() {
    }

    public static void printShortMessage(ShortMessage shortMessage) {
        System.out.print("ShortMessage:");
        System.out.print(" Command: " + shortMessage.getCommand());
        System.out.print(" Channel: " + shortMessage.getChannel());
        System.out.print(" Status: " + shortMessage.getStatus());
        System.out.print(" Data1: " + shortMessage.getData1());
        System.out.print(" Data2: " + shortMessage.getData2());
    }

    public static void printShortMessage(ShortMessage shortMessage, long tick) {
        System.out.print("Tick: " + tick + " ");
        printShortMessage(shortMessage);
        System.out.println();
    }

    public static long getFirstBeatInBuffer(long bufferStart_ticks) {
        if (bufferStart_ticks % PPQ_RESOLUTION == 0) return bufferStart_ticks; else {
            long firstBeat_ppq = ((bufferStart_ticks / PPQ_RESOLUTION) * PPQ_RESOLUTION) + PPQ_RESOLUTION;
            return firstBeat_ppq;
        }
    }
}
