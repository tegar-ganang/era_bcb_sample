package com.sun.mmedia;

import javax.microedition.media.*;
import javax.microedition.media.control.*;
import com.sun.mmedia.MIDIOut;
import java.io.IOException;

/**
 * This class implements MIDIControl functionality.
 * @author Florian Bomers
 */
class MIDICtrl implements MIDIControl {

    /** midiOut is NOT owned by this control, only used. */
    private MIDIOut midiOut;

    private static final boolean DEBUG = false;

    MIDICtrl(MIDIOut midiOut) {
        if (DEBUG) System.out.println("MIDICtrl constructor, sequencer mode");
        this.midiOut = midiOut;
    }

    private void checkChannel(int c) {
        if ((c & 0xFFFFFFF0) != 0) {
            throw new IllegalArgumentException("channel out of bounds (0-15)");
        }
    }

    private void check7bit(int d, String err) {
        if ((d & 0xFFFFFF80) != 0) {
            throw new IllegalArgumentException(err + " out of bounds (0-127)");
        }
    }

    private void checkState() {
        if (!midiOut.midiIsOpen()) {
            throw new IllegalStateException("player is not prefetched");
        }
    }

    public boolean isBankQuerySupported() {
        return false;
    }

    public int[] getProgram(int channel) throws MediaException {
        throw new MediaException("not implemented");
    }

    public int getChannelVolume(int channel) {
        checkState();
        checkChannel(channel);
        return -1;
    }

    public void setProgram(int channel, int bank, int program) {
        checkChannel(channel);
        check7bit(program, "program");
        if (bank == -1) {
            bank = 0;
        }
        if (bank < 0 || bank > 16383) {
            throw new IllegalArgumentException("bank out of bounds (-1, or 0-16383)");
        }
        synchronized (midiOut) {
            try {
                shortMidiEvent(0xB0 | channel, 0x00, bank >> 7);
                shortMidiEvent(0xB0 | channel, 0x20, bank & 0x7F);
                shortMidiEvent(0xC0 | channel, program, 0);
            } catch (IllegalArgumentException ie) {
            }
        }
    }

    public void setChannelVolume(int channel, int volume) {
        checkChannel(channel);
        check7bit(volume, "volume");
        try {
            shortMidiEvent(0xB0 | channel, 0x07, volume);
        } catch (IllegalArgumentException ie) {
        }
    }

    public int[] getBankList(boolean custom) throws MediaException {
        throw new MediaException("not implemented");
    }

    public int[] getProgramList(int bank) throws MediaException {
        throw new MediaException("not implemented");
    }

    public String getProgramName(int bank, int prog) throws MediaException {
        throw new MediaException("not implemented");
    }

    public String getKeyName(int bank, int prog, int key) throws MediaException {
        throw new MediaException("not implemented");
    }

    public void shortMidiEvent(int type, int data1, int data2) {
        checkState();
        if ((type & 0xFFFFFF00) != 0 || type < 0x80 || type == 0xF0 || type == 0xF7) {
            throw new IllegalArgumentException("type out of bounds");
        }
        check7bit(data1, "data1");
        check7bit(data2, "data2");
        midiOut.shortMsg(type, data1, data2);
    }

    public int longMidiEvent(byte[] data, int offset, int length) {
        checkState();
        if (data == null || offset < 0 || length < 0 || offset + length > data.length || offset + length < 0) {
            throw new IllegalArgumentException("invalid parameter");
        }
        return midiOut.longMsg(data, offset, length);
    }
}
