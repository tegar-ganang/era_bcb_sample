package net.jetrix.commands;

import static java.lang.Math.*;
import java.util.*;
import net.jetrix.*;
import net.jetrix.messages.channel.CommandMessage;
import net.jetrix.messages.channel.PlineMessage;

/**
 * Display a random number.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 803 $, $Date: 2009-02-25 16:22:12 -0500 (Wed, 25 Feb 2009) $
 */
public class RandomCommand extends AbstractCommand {

    private Random random = new Random();

    public String[] getAliases() {
        return (new String[] { "random", "roll" });
    }

    public String getUsage(Locale locale) {
        return "/random <min> <max>";
    }

    public void execute(CommandMessage m) {
        Client client = (Client) m.getSource();
        int min = 1;
        int max = 6;
        if (m.getParameterCount() >= 2) {
            int a = m.getIntParameter(0, min);
            int b = m.getIntParameter(1, max);
            min = min(a, b);
            max = max(a, b);
        } else if (m.getParameterCount() == 1) {
            max = Math.max(min, m.getIntParameter(0, max));
        }
        int result = random.nextInt(abs(max - min) + 1);
        PlineMessage response = new PlineMessage();
        response.setKey("command.random.result", client.getUser().getName(), min, max, (result + min));
        client.getChannel().send(response);
    }
}
