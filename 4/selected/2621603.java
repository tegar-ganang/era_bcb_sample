package com.neuemusic.eartoner;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

/**
 * This is an object to help wrap up the midi event tied to changing a patch on a channel.
 * 
 * @author Tom Jensen
 *
 */
public class PatchChange {

    private int channel = 0;

    private int patchNumber = 0;

    private long offset = 0;

    public PatchChange(int patchNum) {
        this(patchNum, 0);
    }

    public PatchChange(int patchNum, int chan) {
        this(patchNum, chan, 0);
    }

    public PatchChange(int patchNum, int chan, long fireAt) {
        setPatchNumber(patchNum);
        setChannel(chan);
        setOffset(fireAt);
    }

    /**
	 * @return Returns the channel.
	 */
    public int getChannel() {
        return channel;
    }

    /**
	 * @param channel The channel to set.
	 */
    public void setChannel(int channel) {
        if (channel > 15) {
            channel = 0;
        }
        this.channel = channel;
    }

    /**
	 * @return Returns the offset.
	 */
    public long getOffset() {
        return offset;
    }

    /**
	 * @param offset The offset to set.
	 */
    public void setOffset(long offset) {
        this.offset = offset;
    }

    /**
	 * @return Returns the patchNumber.
	 */
    public int getPatchNumber() {
        return patchNumber;
    }

    /**
	 * @param patchNumber The patchNumber to set.
	 */
    public void setPatchNumber(int patchNumber) {
        if (patchNumber > 127) {
            patchNumber = 0;
        }
        this.patchNumber = patchNumber;
    }

    /**
	 * Convenience method to get this as a midievent to add to a sequence.
	 * @return
	 */
    public MidiEvent getAsMidiEvent() {
        ShortMessage msg = new ShortMessage();
        try {
            msg.setMessage(ShortMessage.PROGRAM_CHANGE, channel, patchNumber, 0);
        } catch (InvalidMidiDataException e1) {
            e1.printStackTrace();
        }
        return new MidiEvent(msg, offset);
    }

    /**
	 * Overriding so that the equality is based on what values are in this object, not on
	 * address space.
	 */
    public boolean equals(Object o) {
        if (o instanceof PatchChange) {
            PatchChange tmpChg = (PatchChange) o;
            if (tmpChg.getChannel() == getChannel() && tmpChg.getOffset() == getOffset() && tmpChg.getPatchNumber() == getPatchNumber()) {
                return true;
            }
        }
        return false;
    }
}
