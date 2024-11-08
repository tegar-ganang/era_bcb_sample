package net.jetrix.commands;

import net.jetrix.*;
import net.jetrix.messages.channel.PlayerNumMessage;
import net.jetrix.messages.channel.CommandMessage;

/**
 * Display the ping to the server. To compute the ping of tetrinet and
 * tetrifast clients we send the <tt>playernum<tt> message that triggers
 * a <tt>team</tt> message as response. We assume the ping is half the time
 * between the request and the response. Since a command cannot wait for
 * a client response this command must work with the PingFilter that is
 * processing all <tt>team</tt> messages. This command sets the client
 * properties <tt>command.ping=true</tt> and <tt>command.ping.time</tt>
 * and send the <tt>playernum<tt> message. The filter will then listen for
 * <tt>team</tt> messages and check if the property <tt>command.ping</tt>
 * is true. If so it will display the ping of the player.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 798 $, $Date: 2009-02-18 10:24:28 -0500 (Wed, 18 Feb 2009) $
 */
public class PingCommand extends AbstractCommand {

    public String getAlias() {
        return "ping";
    }

    public void execute(CommandMessage m) {
        Client client = (Client) m.getSource();
        User user = client.getUser();
        if ("true".equals(user.getProperty("command.ping"))) return;
        PlayerNumMessage response = new PlayerNumMessage();
        response.setSlot(client.getChannel().getClientSlot(client));
        user.setProperty("command.ping", "true");
        user.setProperty("command.ping.time", new Long(System.currentTimeMillis()));
        client.send(response);
    }
}
