package net.jetrix.commands;

import java.util.*;
import net.jetrix.*;
import net.jetrix.messages.*;
import net.jetrix.messages.channel.CommandMessage;
import net.jetrix.messages.channel.PlineMessage;

/**
 * Teleport a player to another channel.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 798 $, $Date: 2009-02-18 10:24:28 -0500 (Wed, 18 Feb 2009) $
 */
public class TeleportCommand extends AbstractCommand implements ParameterCommand {

    public TeleportCommand() {
        setAccessLevel(AccessLevel.OPERATOR);
    }

    public String[] getAliases() {
        return (new String[] { "teleport", "tp" });
    }

    public String getUsage(Locale locale) {
        return "/teleport <" + Language.getText("command.params.player_name_num", locale) + "> <" + Language.getText("command.params.channel_name_num", locale) + ">";
    }

    public int getParameterCount() {
        return 2;
    }

    public void execute(CommandMessage m) {
        Client client = (Client) m.getSource();
        String targetName = m.getParameter(0);
        Client target = m.getClientParameter(0);
        if (target == null) {
            client.send(new PlineMessage("command.player_not_found", targetName));
        } else {
            Channel channel = m.getChannelParameter(1);
            if (channel != null) {
                if (channel.isFull()) {
                    PlineMessage channelfull = new PlineMessage();
                    channelfull.setKey("command.join.full");
                    client.send(channelfull);
                } else {
                    AddPlayerMessage move = new AddPlayerMessage(target);
                    channel.send(move);
                    PlineMessage teleported = new PlineMessage();
                    teleported.setKey("command.teleport.message", target.getUser().getName(), channel.getConfig().getName());
                    client.send(teleported);
                }
            }
        }
    }
}
