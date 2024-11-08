package uk.org.toot.midi.message;

import javax.sound.midi.MidiMessage;

/**
 * This class provides methods to simplify the handling of Universal System Exclusive messages.
 * @author st
 *
 */
public class UniversalSysexMsg extends SysexMsg {

    public static final int ID_UNIVERSAL_NON_REALTIME = 0x7E;

    public static final int ID_UNIVERSAL_REALTIME = 0x7F;

    public static int getChannel(MidiMessage msg) {
        return getMessage(msg)[2] & 0x7F;
    }

    public static int getSubId1(MidiMessage msg) {
        return getMessage(msg)[3] & 0x7F;
    }

    public static int getSubId2(MidiMessage msg) {
        return getMessage(msg)[4] & 0x7F;
    }

    protected static int createUniversalHeader(byte[] data, int id, int channel, int subId1, int subId2) {
        data[0] = (byte) (SYSTEM_EXCLUSIVE);
        data[1] = (byte) id;
        data[2] = (byte) (channel & 0x7F);
        data[3] = (byte) (subId1 & 0x7f);
        data[4] = (byte) (subId2 & 0x7F);
        return 5;
    }
}
