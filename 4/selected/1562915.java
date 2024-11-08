package net.jetrix.commands;

import static net.jetrix.GameState.*;
import net.jetrix.*;
import net.jetrix.messages.channel.ChannelMessage;
import net.jetrix.messages.channel.PauseMessage;
import net.jetrix.messages.channel.ResumeMessage;
import net.jetrix.messages.channel.CommandMessage;

/**
 * Pause the game.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 798 $, $Date: 2009-02-18 10:24:28 -0500 (Wed, 18 Feb 2009) $
 */
public class PauseCommand extends AbstractCommand implements Command {

    public PauseCommand() {
        setAccessLevel(AccessLevel.OPERATOR);
    }

    public String getAlias() {
        return "pause";
    }

    public void execute(CommandMessage m) {
        Client client = (Client) m.getSource();
        Channel channel = client.getChannel();
        if (channel != null) {
            ChannelMessage pause = null;
            if (channel.getGameState() == PAUSED) {
                pause = new ResumeMessage();
            } else {
                pause = new PauseMessage();
            }
            pause.setSlot(channel.getClientSlot(client));
            pause.setSource(client);
            channel.send(pause);
        }
    }
}
