package net.jetrix.commands;

import net.jetrix.*;
import net.jetrix.messages.channel.StopGameMessage;
import net.jetrix.messages.channel.CommandMessage;

/**
 * Stop the game.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 798 $, $Date: 2009-02-18 10:24:28 -0500 (Wed, 18 Feb 2009) $
 */
public class StopCommand extends AbstractCommand {

    public StopCommand() {
        setAccessLevel(AccessLevel.OPERATOR);
    }

    public String getAlias() {
        return "stop";
    }

    public void execute(CommandMessage m) {
        Client client = (Client) m.getSource();
        Channel channel = client.getChannel();
        if (channel != null) {
            StopGameMessage stop = new StopGameMessage();
            stop.setSlot(channel.getClientSlot(client));
            stop.setSource(client);
            channel.send(stop);
        }
    }
}
