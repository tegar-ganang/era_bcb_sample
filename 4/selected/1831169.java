package net.sf.japi.midi;

import javax.sound.midi.MidiDevice;
import org.jetbrains.annotations.NotNull;

/** OutputConfiguration describes a MIDI target, which is the combination of device and channel.
 * @author <a href="mailto:cher@riedquat.de">Christian Hujer</a>
 * @since 0.1
 */
public class OutputConfiguration {

    /** The device. */
    @NotNull
    private final MidiDevice device;

    /** The channel. */
    private final int channel;

    /** Creates an OutputConfiguration.
     * @param device MidiDevice.
     * @param channel Midi Channel.
     */
    public OutputConfiguration(@NotNull final MidiDevice device, final int channel) {
        this.device = device;
        this.channel = channel;
    }

    /** Returns the MidiDevice.
     * @return The MidiDevice.
     */
    @NotNull
    public MidiDevice getDevice() {
        return device;
    }

    /** Returns the Midi Channel.
     * @return The Midi Channel.
     */
    public int getChannel() {
        return channel;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return device.getDeviceInfo().getName() + ": " + channel;
    }
}
