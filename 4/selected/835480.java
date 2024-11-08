package net.jetrix.commands;

import static net.jetrix.GameState.*;
import java.util.*;
import net.jetrix.*;
import net.jetrix.clients.TetrinetClient;
import net.jetrix.config.*;
import net.jetrix.messages.channel.CommandMessage;
import net.jetrix.messages.channel.PlineMessage;

/**
 * List available channels.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 860 $, $Date: 2010-05-06 07:21:05 -0400 (Thu, 06 May 2010) $
 */
public class ListCommand extends AbstractCommand {

    public String[] getAliases() {
        return (new String[] { "list", "l" });
    }

    public void execute(CommandMessage m) {
        Client client = (Client) m.getSource();
        ChannelManager channelManager = ChannelManager.getInstance();
        Locale locale = client.getUser().getLocale();
        String playerChannel = "";
        if (client.getChannel() != null) playerChannel = client.getChannel().getConfig().getName();
        PlineMessage response = new PlineMessage();
        response.setKey("command.list.header");
        client.send(response);
        int i = 0;
        for (Channel channel : channelManager.channels()) {
            i++;
            ChannelConfig conf = channel.getConfig();
            if (!conf.isVisible()) {
                continue;
            }
            if (client.getUser().isPlayer() && !conf.isProtocolAccepted(client.getProtocol().getName())) {
                continue;
            }
            StringBuilder message = new StringBuilder();
            message.append("<darkBlue>(" + (playerChannel.equals(conf.getName()) ? "<red>" + i + "</red>" : "<purple>" + i + "</purple>") + ") ");
            message.append("<purple>" + rightPad(conf.getName(), 6) + "</purple>\t");
            if (channel.isFull()) {
                message.append("[<red>" + Language.getText("command.list.status.full", locale) + "</red>]");
                for (int j = 0; j < 11 - Language.getText("command.list.status.full", locale).length(); j++) {
                    message.append(" ");
                }
            } else {
                message.append("[<aqua>" + Language.getText("command.list.status.open", locale) + "</aqua><blue>-" + channel.getPlayerCount() + "/" + conf.getMaxPlayers() + "</blue>]");
            }
            if (channel.getGameState() != STOPPED) {
                message.append(" <gray>{" + Language.getText("command.list.status.ingame", locale) + "}</gray> ");
            } else {
                message.append("                  ");
            }
            message.append("<black>" + conf.getDescription());
            client.send(new PlineMessage(message.toString()));
        }
    }

    /**
     * Add spaces at the right of the string if it's shorter than the specified length.
     */
    private String rightPad(String s, int length) {
        while (s.length() < length) {
            s += " ";
        }
        return s;
    }
}
