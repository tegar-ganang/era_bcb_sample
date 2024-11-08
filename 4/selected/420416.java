package net.sourceforge.midivolumizer;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

/**
 * Manage a javax.sound.midi.Sequence in a way that allows the volume values
 * to be changed.
 * </p>
 * I'm sure this class reflects my lack of insight on how to actually manage
 * a javax.sound.midi.Sequence. I wish the documentation was better. I have
 * actually figured out that the MidiEvent returned by a track and the
 * MidiMessage returned by the MidiEvent contain the actual data used by the
 * sequence and not copies. This class uses that fact to be able to adjust
 * volume on the fly.
 * <p/>
 * Another thing that is good to know is that javax.sound.midi.Sequence
 */
public class VolumizerSequence {

    private static boolean debug = false;

    /**
     * The maximum volume value attainable. ((Max MSB << 7)+Max LSB)*Max Velocity.
     */
    public static final int MAX_VOLUME = ((127 << 7) + 127) * 127;

    /**
     * Manage the three values that make up a MIDI volume. A MIDI volume
     * consists of a 14-bit value (comprised of two 7-bit values), and
     * a 7-bit velocity value that multiplies the 14 bit value. Because of
     * this construct, there are some volume values that cannot be created
     * and others that have multiple ways of being created.
     */
    public static class VolumeTriple {

        public int msb;

        public int lsb;

        public int velocity;

        /**
         * Create a volume triple.
         *
         * @param msb The seven most significant bits of the volume value. Bits
         * outside the valid range are ignored.
         *
         * @param lsb The seven least significant bits of the volume value. Bits
         * outside the valid range are ignored.
         *
         * @param velocity The 7-bit MIDI "note on" velocity value. Bits
         * outside the valid range are ignored.
         */
        public VolumeTriple(int msb, int lsb, int velocity) {
            this.msb = msb & 0x7F;
            this.lsb = lsb & 0x7F;
            this.velocity = velocity & 0x7F;
            return;
        }

        /**
         * Given an absolute volume value, return the best volume triple
         * that comes closest to the same value. MIDI volume is a
         * combination of the 7-bit volume most significant bits, the
         * 7-bit volume least significant bits, and the 7-bit note on
         * velocity value. All volume values fit within 21 bits but not
         * all 7-bit values are possible. Some identical volumes can also
         * come from different combinations of msb, lsb, and velocity.
         * When there are multiple possible VolumeTriple values, this
         * function always returns the value with the smallest velocity.
         *
         * @param absoluteVolume absolute volume value to convert to a
         * VolumeTriple.
         *
         * @return the best match for the given absolute volume value.
         */
        public VolumeTriple(int absoluteVolume) {
            if (absoluteVolume > 0) {
                if (absoluteVolume < MAX_VOLUME) {
                    velocity = 127;
                    int diff = Math.abs(((absoluteVolume / 127) * 127) - absoluteVolume);
                    int minvelocity = (absoluteVolume / ((127 << 7) + 127));
                    if (minvelocity * ((127 << 7) + 127) < absoluteVolume) {
                        ++minvelocity;
                        for (int i = minvelocity; i <= 127; ++i) {
                            int checkdiff = Math.abs(((absoluteVolume / i) * i) - absoluteVolume);
                            if (checkdiff < diff) {
                                velocity = i;
                                if (checkdiff == 0) {
                                    break;
                                }
                                diff = checkdiff;
                            }
                        }
                    } else {
                        velocity = minvelocity;
                    }
                    int combinedVolume = absoluteVolume / velocity;
                    msb = combinedVolume >> 7;
                    lsb = combinedVolume & 0x7F;
                } else {
                    msb = lsb = velocity = 127;
                }
            } else {
                msb = lsb = velocity = 0;
            }
        }
    }

    ;

    /**
     * Convert msb/lsb/velocity data to an absolute volume value. The absolute
     * volume is a value that is less than 2^22 but not all values in that range
     * can be absolute volume values.
     *
     * @param msb Volume most significant bits. Only values from zero to 127
     * will produce meaningful results.
     *
     * @param lsb Volume least significant bits. Only values from zero to 127
     * will produce meaningful results.
     *
     * @param velocity Note-on velocity value. Only values from zero to 127
     * will produce meaningful results.
     *
     * @return An absolute volume value based upon the given parameters.
     */
    public static int absoluteVolume(int msb, int lsb, int velocity) {
        return ((msb << 7) + lsb) * velocity;
    }

    /**
     * Manage a block of MIDI "note on" messages on the same channel that
     * play at the same volume. This class keeps track of the original volume
     * and can adjust the volume that the notes play based upon the original
     * volume.
     */
    private static class VolumeUnit implements Comparable<VolumeUnit> {

        /**
         * Iterate through the on and off arrays in a on-off-on-off-on-etc fashion.
         */
        private static class OnOff implements Iterable<Long> {

            private class OnOffIter implements Iterator<Long> {

                private Iterator<Long> onIter;

                private Iterator<Long> offIter;

                private boolean doingOn = true;

                public OnOffIter(ArrayList<Long> on, ArrayList<Long> off) {
                    onIter = on.iterator();
                    offIter = off.iterator();
                }

                /**
                 * Returns true if the iteration has more elements.
                 */
                public boolean hasNext() {
                    return offIter.hasNext();
                }

                /**
                 * Returns the next element
                 */
                public Long next() {
                    doingOn = !doingOn;
                    return doingOn ? offIter.next() : onIter.next();
                }

                /**
                 * Unsupported
                 */
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            }

            ;

            private ArrayList<Long> on = new ArrayList<Long>();

            private ArrayList<Long> off = new ArrayList<Long>();

            private boolean onOffCleaned = false;

            private long maxTick;

            /**
             * Make a sorted list of unique elements out of "l".
             * @param l list to sort and make uniquey.
             */
            private static void sortAndUniq(ArrayList<Long> l) {
                if (l.size() > 1) {
                    java.util.Collections.sort(l);
                    Long p = l.get(0);
                    for (int i = 1; i != l.size(); ) {
                        Long c = l.get(i);
                        if (p.compareTo(c) == 0) {
                            l.remove(i);
                        } else {
                            p = c;
                            ++i;
                        }
                    }
                }
                return;
            }

            /**
             * Make sure on and off are sequenced properly.
             */
            private void cleanOnOff() {
                sortAndUniq(on);
                sortAndUniq(off);
                for (int i = 0; ; ++i) {
                    if (i == on.size()) {
                        while (off.size() != i) {
                            off.remove(i);
                        }
                        break;
                    }
                    if (i == off.size()) {
                        off.add(maxTick);
                    }
                    Long curOn = on.get(i);
                    for (; ; ) {
                        int cmp = curOn.compareTo(off.get(i));
                        if (cmp <= 0) {
                            break;
                        }
                        off.remove(i);
                        if (i == off.size()) {
                            off.add(maxTick);
                        }
                    }
                    Long curOff = off.get(i);
                    for (int i1 = i + 1; i1 != on.size(); ) {
                        int cmp = curOff.compareTo(on.get(i1));
                        if (cmp < 0) {
                            break;
                        }
                        on.remove(i1);
                    }
                }
                onOffCleaned = true;
                return;
            }

            public OnOff(long maxTick) {
                this.maxTick = maxTick;
            }

            /**
             * Add an on time. You cannot call this after calling "iterator()"
             * @param tick The on time.
             */
            public void addOn(long tick) {
                if (onOffCleaned) {
                    throw new IllegalStateException("Cannot add after the cleanup");
                }
                on.add(tick);
            }

            /**
             * Add an off time. You cannot call this after calling "iterator()"
             * @param tick The off time.
             */
            public void addOff(long tick) {
                if (onOffCleaned) {
                    throw new IllegalStateException("Cannot add after the cleanup");
                }
                off.add(tick);
            }

            /**
             * Get an iterator over the on and off values. The iterator returns
             * values in an on-off-on-off-etc sequence with the values always
             * increasing and the sequence, when not empty, always starts with
             * an on value and ends in an off value.
             * <p/>
             * This method will lock the OnOff such that no more values can added.
             * @return iterator of an on-off-on-off sequence.
             */
            public Iterator<Long> iterator() {
                if (!onOffCleaned) {
                    cleanOnOff();
                }
                return new OnOffIter(on, off);
            }
        }

        ;

        private int originalAbsoluteVolume = Integer.MIN_VALUE;

        private int absoluteVolume = 0;

        private ArrayList<ShortMessage> volumeMsb = new ArrayList<ShortMessage>();

        private ArrayList<ShortMessage> volumeLsb = new ArrayList<ShortMessage>();

        private ArrayList<ShortMessage> noteOn = new ArrayList<ShortMessage>();

        private OnOff onOff;

        public VolumeUnit(long maxTick) {
            onOff = new OnOff(maxTick);
        }

        /**
         * Set the volume MSB message. Call this only once.
         * @param msb The ShortMessage containing the volume MSB.
         */
        public void setVolumeMsb(ShortMessage msb) {
            if (msb.getCommand() == ShortMessage.CONTROL_CHANGE && msb.getData1() == 7) {
                if (volumeMsb.size() == 0 || volumeMsb.get(0).getData2() == msb.getData2()) {
                    volumeMsb.add(msb);
                } else {
                    throw new IllegalStateException("Volume MSB already set to a different value");
                }
            } else {
                throw new IllegalArgumentException("The given ShortMessage does not contain a Volume MSB value");
            }
            return;
        }

        /**
         * Set the volume LSB message. Call this only once.
         * @param lsb The ShortMessage containing the volume MSB.
         */
        public void setVolumeLsb(ShortMessage lsb) {
            if (lsb.getCommand() == ShortMessage.CONTROL_CHANGE && lsb.getData1() == 39) {
                if (volumeLsb.size() == 0 || volumeLsb.get(0).getData2() == lsb.getData2()) {
                    volumeLsb.add(lsb);
                } else {
                    throw new IllegalStateException("Volume LSB already set to a different value");
                }
            } else {
                throw new IllegalArgumentException("The given ShortMessage does not contain a Volume LSB value");
            }
            return;
        }

        /**
         * Add a "note on" message. This message must have the same velocity
         * value as any previous "note on" message. Both the volume MSB and LSB
         * messages should already have been seen prior to calling this method.
         * @param no The "note on" message to add.
         */
        public void addNoteOn(ShortMessage no, long tick) {
            if (volumeMsb.size() != 0 && volumeLsb.size() != 0) {
                if (no.getCommand() == ShortMessage.NOTE_ON) {
                    if (no.getData2() == 0) {
                        addNoteOff(tick);
                    } else if (noteOn.size() == 0 || noteOn.get(0).getData2() == no.getData2()) {
                        noteOn.add(no);
                        onOff.addOn(tick);
                        if (originalAbsoluteVolume == Integer.MIN_VALUE) {
                            absoluteVolume = originalAbsoluteVolume = absoluteVolume(volumeMsb.get(0).getData2(), volumeLsb.get(0).getData2(), noteOn.get(0).getData2());
                        }
                    } else {
                        throw new IllegalArgumentException("The given \"note on\" message does not have the same velocity as a previous \"note on\" message");
                    }
                } else {
                    throw new IllegalArgumentException("Expected a \"note on\" message");
                }
            } else {
                throw new IllegalStateException("Should have already had volume MSB and LSB messages loaded");
            }
            return;
        }

        /**
         * Note off or note on velocity = 0 happens.
         * @param tick when it happens
         */
        public void addNoteOff(long tick) {
            onOff.addOff(tick);
            return;
        }

        /**
         * Put in rhs' MSB, LSB, NOTE_ON, and note times into this. The parameter given
         * will be locked so no more entries can be added.
         *
         * @param rhs The VolumeUnit to merge into this one.
         */
        private void merge(VolumeUnit rhs) {
            volumeMsb.addAll(rhs.volumeMsb);
            volumeLsb.addAll(rhs.volumeLsb);
            noteOn.addAll(rhs.noteOn);
            boolean doOn = true;
            for (Long tick : rhs.onOff) {
                if (doOn) {
                    onOff.addOn(tick);
                } else {
                    onOff.addOff(tick);
                }
                doOn = !doOn;
            }
            return;
        }

        /**
         * Whether this VolumeUnit has the volume most significant bits.
         * @return Whether this VolumeUnit has the volume most significant bits.
         */
        public boolean hasMsb() {
            return volumeMsb.size() != 0;
        }

        /**
         * Whether this VolumeUnit has the volume least significant bits.
         * @return Whether this VolumeUnit has the volume least significant bits.
         */
        public boolean hasLsb() {
            return volumeLsb.size() != 0;
        }

        /**
         * Original absolute volume value.
         */
        public int originalAbsoluteVolume() {
            return originalAbsoluteVolume;
        }

        /**
         * Sets the volume to the given value.
         *
         * @param vol the absolute volume to set the MSB, LSB, and velocity
         * values to.
         */
        private void set(int vol) {
            VolumeTriple vt = new VolumeTriple(vol);
            try {
                for (ShortMessage m : volumeMsb) {
                    m.setMessage(m.getStatus(), m.getData1(), vt.msb);
                }
                for (ShortMessage m : volumeLsb) {
                    m.setMessage(m.getStatus(), m.getData1(), vt.lsb);
                }
                for (ShortMessage m : noteOn) {
                    m.setMessage(m.getStatus(), m.getData1(), vt.velocity);
                }
            } catch (InvalidMidiDataException e) {
                throw new RuntimeException("Shouldn't get here", e);
            }
            return;
        }

        /**
         * Make sure all the MSB, LSB, and velocity values are identical. The
         * way MIDI works, there are multiple ways to get some absolute volumes.
         * This method makes sure that all the various values are the same.
         * <p/>
         * Calling this method is unnecessary to the proper operation of the
         * VolumeUnit or the encompassing VolumizerSequence. Using this call may
         * make the output file slightly smaller. This only needs to be called
         * once after all the adding and merging on the VolumeUnit has been
         * completed, after that, the VolumeUnit will remain normalized.
         */
        public void normalize() {
            set(absoluteVolume);
        }

        /**
         * Adjusts the original volume in the MIDI stream by the given
         * multiplier and shift value.
         *
         * @param mul Multiply the original volume by this value.
         *
         * @param shift Shift the result of the original volume multiplied by
         * mul by this value.
         *
         * @return The resulting absolute volume value.
         */
        public int adjust(double mul, double shift) {
            int ret = (originalAbsoluteVolume == 0) ? 0 : (int) Math.round((((double) originalAbsoluteVolume) * mul) + shift);
            if (ret != absoluteVolume) {
                set(ret);
                absoluteVolume = ret;
            }
            return ret;
        }

        /**
         * Order by original absolute volume.
         */
        public int compareTo(VolumeUnit o) {
            return originalAbsoluteVolume < o.originalAbsoluteVolume ? -1 : originalAbsoluteVolume == o.originalAbsoluteVolume ? 0 : 1;
        }

        /**
         * Indicates whether some other object is "equal to" this one. Matches if absolute
         * volume is the same.
         *
         * @param o the reference object with which to compare.
         * @return true if this object is the same as the obj argument; false otherwise.
         */
        public boolean equals(Object o) {
            return (this.getClass() == o.getClass()) && (originalAbsoluteVolume == ((VolumeUnit) o).originalAbsoluteVolume);
        }

        /**
         *  Returns a hash code value for the object. This method is supported for
         *  the benefit of hashtables such as those provided by java.util.Hashtable.
         *  <p/>
         *  Unfortunately, VolumeUnit's hash code isn't valid until the first call of
         *  "addNoteOn()". This behavior doesn't follow the hashCode() contract exactly
         *  so don't insert a VolumeUnit object into anything that needs the hashCode()
         *  contract prior to calling "addNoteOn()" for the first time.
         *
         *  @return a hash code value for this object.
         */
        public int hashCode() {
            return originalAbsoluteVolume;
        }

        /**
         * Draw the set of horizontal lines for this volume unit.
         * @param width The width of the window the lines go into.
         * @param height The height of the window the lines go into.
         * @param g
         */
        public void paintVolumeUnit(int width, long maxOnOff, int height, java.awt.Graphics g) {
            --height;
            --width;
            int vpos = height - (int) ((((long) this.absoluteVolume) * height) / MAX_VOLUME);
            int lpos = 0;
            boolean dash = false;
            for (Long p : onOff) {
                if (dash) {
                    int rpos = (int) ((p.longValue() * width) / maxOnOff);
                    g.drawLine(lpos, vpos, rpos, vpos);
                    dash = false;
                } else {
                    lpos = (int) ((p.longValue() * width) / maxOnOff);
                    dash = true;
                }
            }
            return;
        }
    }

    /**
     * Parse MIDI Controller event data
     */
    private static class ControllerMessage {

        private int type;

        private int value;

        public ControllerMessage(MidiMessage m) {
            byte[] b = m.getMessage();
            type = (int) (b[1] & 0xFF);
            value = (int) (b[2] & 0xFF);
        }

        public int type() {
            return type;
        }

        public int value() {
            return value;
        }
    }

    private int type;

    private Sequence seq;

    private int max;

    private int min;

    private long maxTick;

    private int originalMax;

    private int originalMinNotZero;

    private ArrayList<VolumeUnit> vu = new ArrayList<VolumeUnit>();

    private static void dumpByteString(byte[] bs) {
        System.out.print(" [");
        for (byte b : bs) {
            System.out.print((char) b);
        }
        System.out.println("]");
    }

    private static void dumpMetaMessage(long tick, javax.sound.midi.MetaMessage mm) {
        System.out.print("at " + tick + " ");
        switch(mm.getType()) {
            case 0:
                System.out.println("Sequence Number " + (((int) (mm.getData()[0]) << 8) | mm.getData()[1]));
                break;
            case 1:
                System.out.print("Text Event");
                dumpByteString(mm.getData());
                break;
            case 2:
                System.out.print("Copyright Notice");
                dumpByteString(mm.getData());
                break;
            case 3:
                System.out.print("Sequence/Track Name");
                dumpByteString(mm.getData());
                break;
            case 4:
                System.out.print("Instrument Name");
                dumpByteString(mm.getData());
                break;
            case 5:
                System.out.print("Lyrics");
                dumpByteString(mm.getData());
                break;
            case 6:
                System.out.print("Marker");
                dumpByteString(mm.getData());
                break;
            case 7:
                System.out.print("Cue Point");
                dumpByteString(mm.getData());
                break;
            case 32:
                System.out.println("MIDI Channel Prefix. Channel=" + mm.getData()[0]);
                break;
            case 47:
                System.out.println("End of Track");
                break;
            case 81:
                System.out.println("Set Tempo " + ((((int) mm.getData()[0]) << 16) | (((int) mm.getData()[1]) << 8) | ((int) mm.getData()[0])));
                break;
            case 84:
                {
                    NumberFormat fmt2 = NumberFormat.getIntegerInstance();
                    fmt2.setMinimumIntegerDigits(2);
                    System.out.println("SMPTE Offset " + (((mm.getData()[0] & 0xC0) == 0) ? "24 fps" : ((mm.getData()[0] & 0xC0) == 0x40) ? "25 fps" : ((mm.getData()[0] & 0xC0) == 0x80) ? "30 fps (drop frame)" : "30 fps") + " " + ((mm.getData()[0] & 0x3F)) + ":" + fmt2.format(mm.getData()[1]) + ":" + fmt2.format(mm.getData()[2]) + "/" + mm.getData()[3] + "." + fmt2.format(mm.getData()[4]));
                }
                break;
            case 88:
                System.out.println("Time Signature " + mm.getData()[0] + " " + mm.getData()[1] + " " + mm.getData()[2] + " " + mm.getData()[3]);
                break;
            case 89:
                System.out.println("Key Signature " + ((mm.getData()[0] & 0x80) == 0 ? mm.getData()[0] : -(mm.getData()[0] & 0x7F)) + " " + mm.getData()[1]);
                break;
            case 127:
                System.out.println("Sequence Specific " + mm.getData().length + " bytes");
                break;
            default:
                System.out.println("Unknown MetaMessage " + mm.getType() + " " + mm.getData().length + " bytes");
        }
        return;
    }

    /**
     * Return the maximum of the given tick and the last tick in the given track.
     * @param tick given tick.
     * @param t given track.
     * @return maximum of the given tick compared with the last tick in the given track.
     */
    private static long max(long tick, Track t) {
        MidiEvent e = t.get(t.size() - 1);
        return e.getTick() > tick ? e.getTick() : tick;
    }

    /**
     * Create a VolumizerSequence from the given MIDI type and sequence.
     * @param type The MIDI file type the sequence came from.
     * @param seq The MIDI sequence.
     */
    private VolumizerSequence(int type, Sequence seq) {
        this.type = type;
        this.seq = seq;
        this.maxTick = Long.MIN_VALUE;
        for (Track t : seq.getTracks()) {
            VolumeUnit[] curVu = new VolumeUnit[16];
            if (debug) System.out.println("track ticks=" + t.ticks() + " size=" + t.size());
            this.maxTick = max(this.maxTick, t);
            for (int i = 0; i < t.size(); ++i) {
                MidiEvent e = t.get(i);
                MidiMessage m = e.getMessage();
                int tmp = m.getStatus();
                if (debug && tmp == 255) dumpMetaMessage(e.getTick(), (javax.sound.midi.MetaMessage) m);
                int status = tmp & 0xF0;
                int channel = tmp & 0xF;
                switch(status) {
                    case ShortMessage.NOTE_ON:
                        curVu[channel].addNoteOn((ShortMessage) m, e.getTick());
                        break;
                    case ShortMessage.NOTE_OFF:
                        curVu[channel].addNoteOff(e.getTick());
                        break;
                    case ShortMessage.CONTROL_CHANGE:
                        ControllerMessage cm = new ControllerMessage(m);
                        switch(cm.type()) {
                            case 7:
                                if (curVu[channel] == null) {
                                    curVu[channel] = new VolumeUnit(t.ticks());
                                } else {
                                    if (curVu[channel].hasMsb()) {
                                        vu.add(curVu[channel]);
                                        curVu[channel] = new VolumeUnit(t.ticks());
                                    }
                                }
                                curVu[channel].setVolumeMsb((ShortMessage) m);
                                break;
                            case 39:
                                if (curVu[channel] == null) {
                                    curVu[channel] = new VolumeUnit(t.ticks());
                                } else {
                                    if (curVu[channel].hasLsb()) {
                                        vu.add(curVu[channel]);
                                        curVu[channel] = new VolumeUnit(t.ticks());
                                    }
                                }
                                curVu[channel].setVolumeLsb((ShortMessage) m);
                                break;
                            default:
                                if (debug) System.out.println("control change [" + channel + "] type=" + cm.type());
                        }
                        break;
                    default:
                        if (debug) System.out.println("status " + status + " [" + channel + "]=");
                }
            }
            for (VolumeUnit tmp : curVu) {
                if (tmp != null) {
                    vu.add(tmp);
                }
            }
        }
        if (debug) System.out.println("VolumeUnit count before consolodation=" + vu.size());
        if (vu.size() > 1) {
            java.util.Collections.sort(vu);
            VolumeUnit prev = vu.get(0);
            for (int i = 1; i != vu.size(); ) {
                VolumeUnit cur = vu.get(i);
                if (cur.originalAbsoluteVolume() == prev.originalAbsoluteVolume()) {
                    prev.merge(cur);
                    vu.remove(i);
                } else {
                    ++i;
                    if (i == vu.size()) {
                        break;
                    }
                    prev = vu.get(i);
                    ++i;
                }
            }
        }
        if (debug) {
            System.out.println("VolumeUnit count after consolodation=" + vu.size());
            for (VolumeUnit v : vu) {
                System.out.print(" " + v.originalAbsoluteVolume());
            }
            System.out.println("");
        }
        if (vu.size() > 0) {
            originalMax = max = vu.get(vu.size() - 1).originalAbsoluteVolume();
            if (debug) System.out.println("max absolute volume=" + max);
            originalMinNotZero = min = 0;
            for (VolumeUnit tmp : vu) {
                if (debug) System.out.println("min absolute volume=" + tmp.originalAbsoluteVolume());
                if (tmp.originalAbsoluteVolume() != 0) {
                    originalMinNotZero = min = tmp.originalAbsoluteVolume();
                    break;
                }
            }
        } else {
            originalMax = max = 0;
            originalMinNotZero = min = 0;
        }
        for (VolumeUnit tmp : vu) {
            tmp.normalize();
        }
        return;
    }

    private static void addLSB(Track t, int channel, int val, long tick) throws InvalidMidiDataException {
        ShortMessage sm = new ShortMessage();
        sm.setMessage(ShortMessage.CONTROL_CHANGE, channel, 39, 0);
        t.add(new MidiEvent(sm, tick));
    }

    private static void addMSB(Track t, int channel, int vol, long tick) throws InvalidMidiDataException {
        ShortMessage sm = new ShortMessage();
        sm.setMessage(ShortMessage.CONTROL_CHANGE, channel, 7, vol);
        t.add(new MidiEvent(sm, tick));
    }

    /**
     * Trim leading and trailing blank space from the given sequence.
     * <p/>
     * The Java Sequencer doesn't keep track of the end of the music like
     * most MIDI players do. Some songs put meta events way after the end
     * of the music. This function was written to modify the sequence such
     * that no non-sound events would make the Java sequencer thing the
     * sequence is longer than the sound it contains.
     * <p/>
     * In addition (because I wanted it to do this for my own sequences),
     * this function removes any leading quiet space from the sequence.
     *
     * @param seq Sequence to trim leading and trailing blank space from.
     *
     * @return Sequence with no leading or trailing blank space.
     */
    private static Sequence trim(Sequence seq) {
        Sequence ret;
        try {
            Long maxTick = Long.MIN_VALUE;
            Long minTick = Long.MAX_VALUE;
            for (Track t : seq.getTracks()) {
                int msb[] = { 90, 90, 90, 90, 90, 90, 90, 90, 90, 90, 90, 90, 90, 90, 90, 90 };
                int lsb[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
                boolean noteOn[] = { false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false };
                for (int i = 0; i < t.size(); ++i) {
                    MidiEvent e = t.get(i);
                    MidiMessage m = e.getMessage();
                    int tmp = m.getStatus();
                    int status = tmp & 0xF0;
                    int channel = tmp & 0xF;
                    switch(status) {
                        case ShortMessage.NOTE_ON:
                            if (msb[channel] != 0 || lsb[channel] != 0) {
                                if (((ShortMessage) m).getData2() != 0) {
                                    if (minTick > e.getTick()) {
                                        minTick = e.getTick();
                                    }
                                    if (maxTick < e.getTick()) {
                                        maxTick = e.getTick();
                                    }
                                    noteOn[channel] = true;
                                } else {
                                    noteOn[channel] = false;
                                }
                            }
                        case ShortMessage.NOTE_OFF:
                            noteOn[channel] = false;
                            break;
                        default:
                            if (tmp != javax.sound.midi.MetaMessage.META) {
                                if (noteOn[channel]) {
                                    if (minTick > e.getTick()) {
                                        minTick = e.getTick();
                                    }
                                    if (maxTick < e.getTick()) {
                                        maxTick = e.getTick();
                                    }
                                }
                            }
                    }
                }
            }
            ret = new Sequence(seq.getDivisionType(), seq.getResolution());
            for (Track t : seq.getTracks()) {
                Track to = ret.createTrack();
                for (int i = 0; i < t.size(); ++i) {
                    MidiEvent e = t.get(i);
                    MidiMessage m = e.getMessage();
                    Long tick = e.getTick() - minTick;
                    if (tick < 0) {
                        tick = 0L;
                    } else if (tick > maxTick) {
                        tick = maxTick;
                    }
                    to.add(new MidiEvent(m, tick));
                }
            }
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException("Shouldn't get here", e);
        }
        return ret;
    }

    /**
     * Run through the give sequence and return one with volume
     * controller events prior to any keypress with a different
     * value than the previous keypress.
     * <p/>
     * Expects there to be MSB and LSB volume values prior to any
     * track's NOTE_ON event.
     * @param seq Sequence to modify.
     * @return modified sequence.
     */
    private static Sequence insertKeypressChangeVolumes(Sequence seq) {
        try {
            Sequence ret = new Sequence(seq.getDivisionType(), seq.getResolution());
            for (Track t : seq.getTracks()) {
                int velocity[] = { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };
                int msb[] = { 90, 90, 90, 90, 90, 90, 90, 90, 90, 90, 90, 90, 90, 90, 90, 90 };
                int lsb[] = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
                Track to = ret.createTrack();
                for (int i = 0; i < t.size(); ++i) {
                    MidiEvent e = t.get(i);
                    MidiMessage m = e.getMessage();
                    int tmp = m.getStatus();
                    int status = tmp & 0xF0;
                    int channel = tmp & 0xF;
                    switch(status) {
                        case ShortMessage.NOTE_ON:
                            if (velocity[channel] != -1) {
                                int vel = ((ShortMessage) m).getData2();
                                if (vel != 0 && vel != velocity[channel]) {
                                    addMSB(to, channel, msb[channel], e.getTick());
                                    addLSB(to, channel, lsb[channel], e.getTick());
                                    velocity[channel] = vel;
                                }
                            } else {
                                addMSB(to, channel, msb[channel], e.getTick());
                                addLSB(to, channel, lsb[channel], e.getTick());
                                velocity[channel] = ((ShortMessage) m).getData2();
                            }
                            to.add(new MidiEvent(e.getMessage(), e.getTick()));
                            break;
                        case ShortMessage.CONTROL_CHANGE:
                            ControllerMessage cm = new ControllerMessage(m);
                            switch(cm.type()) {
                                case 7:
                                    msb[channel] = cm.value();
                                    break;
                                case 39:
                                    lsb[channel] = cm.value();
                                    break;
                                default:
                                    to.add(new MidiEvent(e.getMessage(), e.getTick()));
                            }
                            break;
                        default:
                            to.add(new MidiEvent(e.getMessage(), e.getTick()));
                    }
                }
            }
            return ret;
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException("Shouldn't get here", e);
        }
    }

    /**
     * Create a VolumizerSequence from the given MIDI file.
     * @param f The MIDI file to create the VolumizerSequence from.
     * @return The created VolumizerSequence or null on failure.
     */
    public static VolumizerSequence create(File f) {
        VolumizerSequence ret = null;
        try {
            Sequence seq = trim(insertKeypressChangeVolumes(MidiSystem.getSequence(f)));
            int type = MidiSystem.getMidiFileFormat(f).getType();
            ret = new VolumizerSequence(type, seq);
        } catch (InvalidMidiDataException e) {
        } catch (IOException e) {
        }
        return ret;
    }

    public void paintVolumizerSequence(int width, int height, java.awt.Graphics g) {
        for (VolumeUnit tmp : vu) {
            tmp.paintVolumeUnit(width, maxTick, height, g);
        }
    }

    /**
     * Write the VolumizerSequence to the given File.
     * @param f The file to write.
     * @throws IOException if an I/O exception occurs.
     */
    public void write(File f) throws IOException {
        try {
            MidiSystem.write(seq, type, f);
        } catch (IllegalArgumentException e) {
            IOException ioe = new IOException("Cannot write to the same time the file was read from");
            ioe.initCause(e);
            throw ioe;
        }
    }

    /**
     * Adjust the current volume values so that the volumeHigh() and
     * volumeLow() member functions will be as close as possible to the
     * given arguments. It may not be possible to get exact because of
     * the way MIDI volume values work.
     *
     * @param fromHigh High original volume value.
     * @param fromLow Low original volume value.
     * @param toHigh High desired volume value.
     * @param toLow Low desired volume value.
     */
    public void changeVolume(int fromHigh, int fromLow, int toHigh, int toLow) {
        if (toHigh < toLow) {
            int tmp = toLow;
            toLow = toHigh;
            toHigh = tmp;
        }
        if (toHigh > MAX_VOLUME) {
            toHigh = MAX_VOLUME;
        } else if (toHigh < 0) {
            toHigh = 0;
        }
        if (toLow > MAX_VOLUME) {
            toLow = MAX_VOLUME;
        } else if (toLow < 0) {
            toLow = 0;
        }
        double mul;
        double shift;
        if (fromHigh != fromLow) {
            mul = (double) (toHigh - toLow) / (fromHigh - fromLow);
            shift = toHigh - (mul * fromHigh);
        } else {
            mul = 1.0;
            shift = ((toHigh + toLow) / 2) - fromHigh;
        }
        int curMax = Integer.MIN_VALUE;
        int curMin = Integer.MAX_VALUE;
        for (VolumeUnit tmp : vu) {
            int check = tmp.adjust(mul, shift);
            if (check > curMax) {
                curMax = check;
            }
            if (tmp.originalAbsoluteVolume() != 0 && check < curMin) {
                curMin = check;
            }
        }
        max = curMax;
        min = curMin;
        return;
    }

    /**
     * Adjust the current volume values so that the volumeHigh() and
     * volumeLow() member functions will be as close as possible to the
     * given arguments. It may not be possible to get exact because of
     * the way MIDI volume values work.
     * @param volumeHigh
     * @param volumeLow
     */
    public void changeVolumeFromOriginal(int volumeHigh, int volumeLow) {
        changeVolume(originalMax, originalMinNotZero, volumeHigh, volumeLow);
        return;
    }

    /**
     * The MIDI sequence.
     * @return the MIDI sequence.
     */
    public Sequence sequence() {
        return seq;
    }

    /**
     * The highest volume in the MIDI sequence.
     * @return The highest volume in the MIDI sequence.
     */
    public int volumeHigh() {
        return max;
    }

    /**
     * The lowest non-zero volume in the MIDI sequence.
     * @return The lowest non-zero volume in the MIDI sequence.
     */
    public int volumeLow() {
        return min;
    }

    /**
     * The highest volume in the MIDI sequence from when the sequence
     * was originally loaded.
     * @return The original highest volume in the MIDI sequence.
     */
    public int originalVolumeHigh() {
        return originalMax;
    }

    /**
     * The lowest non-zero volume in the MIDI sequence from when
     * the sequence was originally loaded.
     * @return The original lowest non-zero volume in the MIDI sequence.
     */
    public int originalVolumeLow() {
        return originalMinNotZero;
    }
}
