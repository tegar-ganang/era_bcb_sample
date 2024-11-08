package net.jetrix.filter;

import net.jetrix.*;
import net.jetrix.messages.channel.*;
import net.jetrix.messages.channel.specials.*;
import java.util.*;
import org.apache.commons.lang.StringUtils;

/**
 * Game mod : The first player completing 7 tetris win.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 798 $, $Date: 2009-02-18 10:24:28 -0500 (Wed, 18 Feb 2009) $
 */
public class TetrisFilter extends GenericFilter {

    private int[] tetrisCount = new int[6];

    private int tetrisLimit = 7;

    private boolean addToAll = false;

    public void init() {
        tetrisLimit = config.getInt("limit", tetrisLimit);
        addToAll = config.getBoolean("addToAll", addToAll);
    }

    public void onMessage(StartGameMessage m, List<Message> out) {
        Arrays.fill(tetrisCount, 0);
        GmsgMessage message = new GmsgMessage();
        message.setKey("filter.tetris.start_message", tetrisLimit);
        out.add(m);
        out.add(message);
    }

    public void onMessage(FourLinesAddedMessage m, List<Message> out) {
        int from = m.getFromSlot() - 1;
        tetrisCount[from]++;
        if (addToAll) {
            out.add(m);
        }
        if (tetrisCount[from] >= tetrisLimit) {
            getChannel().send(new EndGameMessage());
            User winner = getChannel().getPlayer(m.getFromSlot());
            PlineMessage announce = new PlineMessage();
            announce.setKey("channel.player_won", winner.getName());
            getChannel().send(announce);
        } else {
            int max = 0;
            for (int i = 0; i < 6; i++) {
                if (tetrisCount[i] > max) {
                    max = tetrisCount[i];
                }
            }
            if (tetrisCount[from] == max) {
                List<String> leaders = new ArrayList<String>();
                for (int i = 0; i < 6; i++) {
                    if (tetrisCount[i] == max) {
                        Client client = getChannel().getClient(i + 1);
                        if (client != null) {
                            leaders.add(client.getUser().getName());
                        }
                    }
                }
                GmsgMessage announce = new GmsgMessage();
                if (leaders.size() == 1) {
                    announce.setKey("filter.tetris.lead", leaders.get(0), max);
                } else {
                    String leadersList = StringUtils.join(leaders.iterator(), ", ");
                    announce.setKey("filter.tetris.tied", leadersList, max);
                }
                out.add(announce);
            }
        }
    }

    public void onMessage(TwoLinesAddedMessage m, List<Message> out) {
        if (addToAll) {
            out.add(m);
        }
    }

    public void onMessage(OneLineAddedMessage m, List<Message> out) {
        if (addToAll) {
            out.add(m);
        }
    }

    public String getName() {
        return "7 Tetris Mod";
    }

    public String getDescription() {
        return "Game mod - The first player completing 7 tetris win.";
    }

    public String getVersion() {
        return "1.0";
    }

    public String getAuthor() {
        return "Emmanuel Bourg";
    }
}
