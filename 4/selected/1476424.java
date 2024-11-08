package org.jjazz.midi;

import javax.sound.midi.*;

/**
 * Convenience MIDI functions.
 */
public class MidiUtilities {

    public static String getCtrlChgString(int ctrl) {
        if (ctrl < 0 || ctrl > 256) {
            throw new IllegalArgumentException("ctrl=" + ctrl);
        }
        String[] strs = new String[256];
        for (int i = 0; i < 256; i++) {
            strs[i] = "?";
        }
        strs[0] = "BANK_SELECT_MSB";
        strs[1] = "MODULATION_MSB";
        strs[7] = "VOLUME_MSB";
        strs[10] = "PAN_MSB";
        strs[11] = "EXPRESSION_MSB";
        strs[32] = "BANK_SELECT_LSB";
        strs[64] = "SUSTAIN";
        strs[91] = "REVERB_DEPTH";
        strs[93] = "CHORUS_DEPTH";
        strs[110] = "JJAZZ_MARKER_SYNC";
        strs[111] = "JJAZZ_CHORD_CHANGE";
        strs[112] = "JJAZZ_BAR_CHANGE";
        strs[120] = "ALL_SOUND_OFF";
        strs[121] = "RESET_ALL_CONTROLLERS";
        strs[123] = "ALL_NOTES_OFF";
        return strs[ctrl];
    }

    public static String getCmdString(int cmd) {
        if (cmd < 0 || cmd > 255) {
            throw new IllegalArgumentException("cmd=" + cmd);
        }
        String[] strs = new String[256];
        for (int i = 0; i < 256; i++) {
            strs[i] = "?";
        }
        strs[ShortMessage.ACTIVE_SENSING] = "ACTIVE_SENSING";
        strs[ShortMessage.CHANNEL_PRESSURE] = "CHANNEL_PRESSURE";
        strs[ShortMessage.CONTINUE] = "CONTINUE";
        strs[ShortMessage.CONTROL_CHANGE] = "CONTROL_CHANGE";
        strs[ShortMessage.END_OF_EXCLUSIVE] = "END_OF_EXCLUSIVE";
        strs[ShortMessage.MIDI_TIME_CODE] = "MIDI_TIME_CODE";
        strs[ShortMessage.NOTE_OFF] = "NOTE_OFF";
        strs[ShortMessage.NOTE_ON] = "NOTE_ON";
        strs[ShortMessage.PITCH_BEND] = "PITCH_BEND";
        strs[ShortMessage.POLY_PRESSURE] = "POLY_PRESSURE";
        strs[ShortMessage.PROGRAM_CHANGE] = "PROGRAM_CHANGE";
        strs[ShortMessage.SONG_POSITION_POINTER] = "SONG_POSITION_POINTER";
        return strs[cmd];
    }

    /**
    * Function for convenience : give an explicit string for a MidiMessage
    * @param msg A MidiMessage.
    * @param time The timestamp of the MidiMessage.
    * @return A string representing the MidiMessage.
    */
    public static String MidiMessageToString(MidiMessage msg, long time) {
        StringBuilder sb = new StringBuilder();
        sb.append(msg.getClass().getName() + " st=" + msg.getStatus());
        if (msg instanceof ShortMessage) {
            ShortMessage sm = (ShortMessage) msg;
            sb.append(" ch=" + sm.getChannel());
            String cmd;
            switch(sm.getCommand()) {
                case 128:
                    cmd = new String("NOTE_OFF");
                    break;
                case 144:
                    cmd = new String("NOTE_ON");
                    break;
                case 176:
                    cmd = new String("CTRL_CHG");
                    break;
                case 192:
                    cmd = new String("PRG_CHG");
                    break;
                case 208:
                    cmd = new String("AFTRTCH");
                    break;
                case 224:
                    cmd = new String("PTCH_BND");
                    break;
                default:
                    cmd = new String("" + sm.getCommand());
            }
            sb.append(" cmd=" + cmd);
            sb.append(" d1=" + sm.getData1());
            sb.append(" d2=" + sm.getData2());
        } else if (msg instanceof SysexMessage) {
            SysexMessage sm = (SysexMessage) msg;
            sb.append(" SysEx");
        } else if (msg instanceof MetaMessage) {
            MetaMessage mm = (MetaMessage) msg;
            sb.append("ty=" + mm.getType());
        }
        sb.append(" t=" + time);
        return sb.toString();
    }
}
