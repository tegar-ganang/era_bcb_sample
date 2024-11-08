package net.jetrix.commands;

import static net.jetrix.GameState.*;
import java.util.*;
import net.jetrix.*;
import net.jetrix.messages.channel.StartGameMessage;
import net.jetrix.messages.channel.CommandMessage;
import net.jetrix.messages.channel.GmsgMessage;
import net.jetrix.messages.channel.PlineMessage;

/**
 * Start the game.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 859 $, $Date: 2010-05-06 06:55:26 -0400 (Thu, 06 May 2010) $
 */
public class StartCommand extends AbstractCommand {

    public String getAlias() {
        return "start";
    }

    public String getUsage(Locale locale) {
        return "/start <" + Language.getText("command.params.seconds", locale) + ">";
    }

    public void execute(CommandMessage m) {
        Client client = (Client) m.getSource();
        Channel channel = client.getChannel();
        if (channel != null && channel.getGameState() == STOPPED) {
            int delay = 0;
            if (m.getParameterCount() > 0) {
                delay = m.getIntParameter(0, delay);
            }
            delay = Math.min(delay, 20);
            if (delay > 0) {
                PlineMessage message = new PlineMessage();
                message.setKey("channel.game.started-by", client.getUser().getName());
                channel.send(message);
                (new StartCommand.CountDown(channel, delay)).start();
            } else {
                StartGameMessage start = new StartGameMessage();
                start.setSlot(channel.getClientSlot(client));
                start.setSource(client);
                channel.send(start);
            }
        }
    }

    /**
     * A countdown thread to delay the beginning of the game.
     */
    public static class CountDown extends Thread {

        private Channel channel;

        private int delay;

        /** */
        private static Map<Channel, CountDown> countdowns = new HashMap<Channel, CountDown>();

        /**
         * Construct a new game countdown.
         *
         * @param channel the channel where game will start
         * @param delay   the delay in seconds for this countdown
         */
        public CountDown(Channel channel, int delay) {
            this.channel = channel;
            this.delay = delay;
        }

        public void run() {
            if (channel.getGameState() != STOPPED) return;
            if (countdowns.get(channel) != null) return;
            countdowns.put(channel, this);
            PlineMessage getready1 = new PlineMessage();
            GmsgMessage getready2 = new GmsgMessage();
            getready1.setKey("command.start.get_ready");
            getready2.setKey("command.start.get_ready");
            channel.send(getready1);
            channel.send(getready2);
            for (int i = delay; i > 0; i--) {
                PlineMessage msg1 = new PlineMessage();
                GmsgMessage msg2 = new GmsgMessage();
                String key = "command.start.second" + (i > 1 ? "s" : "");
                msg1.setKey(key, i);
                msg2.setKey(key, i);
                channel.send(msg1);
                channel.send(msg2);
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                }
                if (channel.getGameState() != STOPPED) {
                    countdowns.put(channel, null);
                    return;
                }
            }
            PlineMessage go1 = new PlineMessage();
            GmsgMessage go2 = new GmsgMessage();
            go1.setKey("command.start.go");
            go2.setKey("command.start.go");
            channel.send(go1);
            channel.send(go2);
            StartGameMessage start = new StartGameMessage();
            channel.send(start);
            countdowns.put(channel, null);
        }
    }
}
