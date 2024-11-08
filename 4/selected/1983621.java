package net.kano.joscar.flapcmd;

import net.kano.joscar.flap.FlapCommand;
import net.kano.joscar.flap.FlapCommandFactory;
import net.kano.joscar.flap.FlapPacket;

/**
 * Provides a default implementation of a FLAP command factory. This factory
 * converts FLAP packets to the <code>FlapCommand</code>s located in this
 * package, such as <code>SnacFlapCmd</code>.
 */
public class DefaultFlapCmdFactory implements FlapCommandFactory {

    public FlapCommand genFlapCommand(FlapPacket packet) {
        int channel = packet.getChannel();
        if (channel == LoginFlapCmd.CHANNEL_LOGIN) {
            return new LoginFlapCmd(packet);
        } else if (channel == SnacFlapCmd.CHANNEL_SNAC) {
            return new SnacFlapCmd(packet);
        } else if (channel == FlapErrorCmd.CHANNEL_ERROR) {
            return new FlapErrorCmd(packet);
        } else if (channel == CloseFlapCmd.CHANNEL_CLOSE) {
            return new CloseFlapCmd(packet);
        } else {
            return null;
        }
    }

    public String toString() {
        return "DefaultFlapCmdFactory";
    }
}
