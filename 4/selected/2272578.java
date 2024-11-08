package net.jetrix.filter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.jetrix.Client;
import net.jetrix.Message;
import net.jetrix.messages.channel.PlineMessage;
import net.jetrix.messages.channel.StartGameMessage;
import net.jetrix.messages.channel.GmsgMessage;
import net.jetrix.messages.channel.TextMessage;
import net.jetrix.protocols.TetrifastProtocol;
import org.apache.commons.lang.StringUtils;

/**
 * Filter displaying a warning at the beginning of the game if everyone
 * isn't playing with the same piece delay.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 860 $, $Date: 2010-05-06 07:21:05 -0400 (Thu, 06 May 2010) $
 * @since 0.3
 */
public class UnbalancedSpeedWarningFilter extends GenericFilter {

    public void onMessage(StartGameMessage m, List<Message> out) {
        Iterator<Client> players = getChannel().getPlayers();
        List<String> fastClients = new ArrayList<String>();
        List<String> normalClients = new ArrayList<String>();
        while (players.hasNext()) {
            Client client = players.next();
            if (client != null && client.getUser().isPlayer()) {
                if (client.getProtocol() instanceof TetrifastProtocol) {
                    fastClients.add(client.getUser().getName());
                } else {
                    normalClients.add(client.getUser().getName());
                }
            }
        }
        if (!normalClients.isEmpty() && !fastClients.isEmpty()) {
            TextMessage warning = createWarningMessage(normalClients, fastClients);
            out.add(warning);
            out.add(m);
            GmsgMessage gmsg = new GmsgMessage();
            gmsg.setKey(warning.getKey());
            gmsg.setParams(warning.getParams());
            out.add(gmsg);
        } else {
            out.add(m);
        }
    }

    private TextMessage createWarningMessage(List<String> normalClients, List<String> fastClients) {
        String type;
        List<String> clients;
        if (normalClients.size() < fastClients.size()) {
            type = "normal";
            clients = normalClients;
        } else {
            type = "fast";
            clients = fastClients;
        }
        PlineMessage message = new PlineMessage();
        if (clients.size() == 1) {
            message.setKey("filter.unbalanced_speed." + type + ".one", clients.get(0));
        } else {
            List<String> firstClients = clients.subList(0, clients.size() - 1);
            String lastClient = clients.get(clients.size() - 1);
            System.out.println("first: " + firstClients);
            System.out.println("last: " + lastClient);
            message.setKey("filter.unbalanced_speed." + type + ".many", StringUtils.join(firstClients.toArray(), ", "), lastClient);
        }
        return message;
    }

    public String getName() {
        return "Unbalanced speed warning";
    }

    public String getDescription() {
        return "Displays a warning at the beginning of the game if everyone isn't playing with the same piece delay";
    }

    public String getVersion() {
        return "1.0";
    }

    public String getAuthor() {
        return "Emmanuel Bourg";
    }
}
