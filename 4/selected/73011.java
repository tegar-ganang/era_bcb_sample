package uk.org.toot.midi.core.channel;

import static uk.org.toot.midi.misc.Controller.*;
import static uk.org.toot.midi.message.ChannelMsg.*;

/**
 * This class decodes state and provides a channel=based read API.
 * It has state.
 */
public class DefaultMidiChannelReader implements MidiChannelReader {

    private int index;

    public DefaultMidiChannelReader(int index) {
        this.index = index;
    }

    public void decode(int command, int data1, int data2) {
        switch(command) {
            case CONTROL_CHANGE:
                decodeControlChange(data1, data2);
                break;
            case PROGRAM_CHANGE:
                decodeProgramChange(data1);
                break;
            case CHANNEL_PRESSURE:
                decodeChannelPressure(data1);
                break;
            case POLY_PRESSURE:
                decodePolyPressure(data1, data2);
                break;
            case PITCH_BEND:
                decodePitchBend(data1, data2);
                break;
        }
    }

    private int getPseudoControl(int control) {
        switch(control) {
            case PITCH_BEND_PSEUDO:
                return getPitchBend();
            case POLY_PRESSURE_PSEUDO:
                return 0;
            case CHANNEL_PRESSURE_PSEUDO:
                return getChannelPressure();
            case PROGRAM_PSEUDO:
                return getProgram();
        }
        return 0;
    }

    public int getControl(int control) {
        if (control < 0) {
            return getPseudoControl(control);
        }
        if (is7bit(control)) {
            return getController(control);
        } else {
            control &= 0x1F;
            int msb = getController(control);
            int lsb = 0;
            return 128 * msb + lsb;
        }
    }

    /**
     * Obtains the pressure with which the specified key is being depressed.
     * @param noteNumber the MIDI note number, from 0 to 127 (60 = Middle C)
     * @return the amount of pressure for that note, from 0 to 127 (127 = maximum pressure)
     * @see #setPolyPressure(int, int)
     */
    public int getPolyPressure(int noteNumber) {
        return polyPressure[noteNumber];
    }

    protected void decodePolyPressure(int note, int pressure) {
        polyPressure[note] = pressure;
    }

    protected void decodeChannelPressure(int pressure) {
        channelPressure = pressure;
    }

    /**
     * Obtains the channel's keyboard pressure.
     * @return the pressure with which the keyboard is being depressed, from 0 to 127 (127 = maximum pressure)
     * @see #setChannelPressure(int)
     */
    public int getChannelPressure() {
        return channelPressure;
    }

    protected void decodeControlChange(int controller, int value) {
        if (controller < 0 || controller > 127) return;
        control[controller] = value;
        if (controller < 0x20) {
            control[controller + 0x20] = 0;
        }
    }

    /**
     * Obtains the current value of the specified controller.  The return
     * value is represented with 7 bits. For 14-bit controllers, the MSB and
     * LSB controller value needs to be obtained separately. For example,
     * the 14-bit value of the volume controller can be calculated by
     * multiplying the value of controller 7 (0x07, channel volume MSB) with 128 and adding the value of controller 39
     * (0x27, channel volume LSB).
     * @param controller the number of the controller whose value is desired. The allowed range is 0-127; see the MIDI
     * 1.0 Specification for the interpretation.
     * @return the current value of the specified controller (0 to 127)
     * @see #controlChange(int, int)
     */
    public int getController(int controller) {
        return control[controller];
    }

    protected void decodeProgramChange(int program) {
        this.program = program;
    }

    /**
     * Obtains the current program number for this channel.
     * @return the program number of the currently selected patch
     * @see javax.sound.midi.Patch#getProgram
     * @see javax.sound.midi.Synthesizer#loadInstrument
     * @see #programChange(int)
     */
    public int getProgram() {
        return program;
    }

    protected void decodePitchBend(int data1, int data2) {
        int bend = (data2 << 7) | (data1 & 0x3f);
        pitchBend = bend;
    }

    /**
     * Obtains the upward or downward pitch offset for this channel.
     * @return bend amount, as a nonnegative 14-bit value (8192 = no bend)
     * @see #setPitchBend(int)
     */
    public int getPitchBend() {
        return pitchBend;
    }

    public int getVolume() {
        return getControl(VOLUME);
    }

    public int getPan() {
        return getControl(PAN);
    }

    /**
     * Obtains the current mono/poly mode.
     * @return <code>true</code> if mono mode is on, otherwise <code>false</code> (meaning poly mode is on).
     * @see #setMono(boolean)
     */
    public boolean getMono() {
        return mono;
    }

    /**
     * Obtains the current omni mode status.
     * @return <code>true</code> if omni mode is on, otherwise <code>false</code>.
     * @see #setOmni(boolean)
     */
    public boolean getOmni() {
        return omni;
    }

    /**
     * Obtains the current mute state for this channel.
     * @return <code>true</code> the channel is muted, <code>false</code> if not
     * @see #setMute(boolean)
     */
    public boolean getMute() {
        return mute;
    }

    /**
     * Obtains the current solo state for this channel.
     * @return <code>true</code> if soloed, <code>false</code> if not
     * @see #setSolo(boolean)
     */
    public boolean getSolo() {
        return solo;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    private boolean solo = false;

    private boolean mute = false;

    private boolean omni = false;

    private boolean mono = false;

    private int pitchBend = 0;

    private int program = 0;

    private int channelPressure = 0;

    private int[] polyPressure = new int[128];

    private int[] control = new int[128];
}
