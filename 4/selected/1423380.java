package net.kano.joscar.flap;

import net.kano.joscar.DefensiveTools;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Provides an interface to a FLAP command that contains FLAP data on a specific
 * channel of a FLAP connection.
 */
public abstract class FlapCommand {

    /**
     * The channel on which this FLAP command resides.
     */
    private final int channel;

    /**
     * Creates a FLAP command on the given FLAP channel.
     * @param channel the FLAP channel associated with this command
     */
    protected FlapCommand(int channel) {
        DefensiveTools.checkRange(channel, "channel", 0);
        this.channel = channel;
    }

    /**
     * Returns the channel on which this FLAP command resides.
     * @return this FLAP command's FLAP channel
     */
    public final int getChannel() {
        return channel;
    }

    /**
     * Writes this command's FLAP data to the given stream.
     *
     * @param out the stream to which the FLAP data should be written
     * @throws IOException if an I/O error occurs
     */
    public abstract void writeData(OutputStream out) throws IOException;
}
