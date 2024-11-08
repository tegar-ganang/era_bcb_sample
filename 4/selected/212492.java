package net.jetrix.filter;

import static net.jetrix.GameState.*;
import java.util.*;
import net.jetrix.*;
import net.jetrix.commands.StartCommand;
import net.jetrix.messages.channel.*;

/**
 * Starts the game automatically once everyone said "go".
 *
 * @author Emmanuel Bourg
 * @version $Revision: 798 $, $Date: 2009-02-18 10:24:28 -0500 (Wed, 18 Feb 2009) $
 */
public class StartFilter extends GenericFilter {

    private long timestamp[];

    private int delay = 10000;

    private int countdown = 0;

    public void init() {
        delay = config.getInt("delay", delay);
        countdown = config.getInt("countdown", countdown);
        timestamp = new long[6];
    }

    public void onMessage(PlineMessage m, List<Message> out) {
        int slot = m.getSlot();
        String text = m.getText();
        out.add(m);
        if (slot == 0) {
            return;
        }
        if (getChannel().getGameState() != STOPPED) {
            return;
        }
        if (text != null && text.toLowerCase().trim().startsWith("go")) {
            long now = System.currentTimeMillis();
            timestamp[slot - 1] = now;
            boolean doStart = true;
            int i = 0;
            while (i < 6 && doStart) {
                Client player = getChannel().getClient(i + 1);
                doStart = (player == null) || (player != null && (now - timestamp[i]) <= delay);
                i = i + 1;
            }
            if (doStart) {
                Arrays.fill(timestamp, 0);
                if (countdown == 0) {
                    StartGameMessage startMessage = new StartGameMessage();
                    out.add(startMessage);
                } else {
                    (new StartCommand.CountDown(getChannel(), countdown)).start();
                }
            }
        }
    }

    public String getName() {
        return "Start Filter";
    }

    public String getDescription() {
        return "Starts the game automatically once everyone said 'go'.";
    }

    public String getVersion() {
        return "1.1";
    }

    public String getAuthor() {
        return "Emmanuel Bourg";
    }
}
