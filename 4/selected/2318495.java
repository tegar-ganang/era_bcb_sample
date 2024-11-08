package net.jetrix.commands;

import java.util.*;
import net.jetrix.*;
import net.jetrix.messages.channel.CommandMessage;
import net.jetrix.messages.channel.PlineActMessage;

/**
 * Display an emote.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 798 $, $Date: 2009-02-18 10:24:28 -0500 (Wed, 18 Feb 2009) $
 */
public class EmoteCommand extends AbstractCommand {

    public String[] getAliases() {
        return (new String[] { "emote", "me" });
    }

    public String getUsage(Locale locale) {
        return "/me";
    }

    public void execute(CommandMessage m) {
        Client client = (Client) m.getSource();
        String emote = m.getText();
        PlineActMessage response = new PlineActMessage(emote);
        response.setSlot(client.getChannel().getClientSlot(client));
        response.setSource(client);
        client.getChannel().send(response);
    }
}
