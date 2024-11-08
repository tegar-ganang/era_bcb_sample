package net.jetrix.filter;

import static net.jetrix.GameState.*;
import java.util.*;
import java.text.*;
import net.jetrix.*;
import net.jetrix.messages.channel.*;
import net.jetrix.messages.channel.specials.*;
import org.apache.commons.lang.time.StopWatch;

/**
 * A filter computing and displaying the number of pieces dropped per minute
 * by each player.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 799 $, $Date: 2009-02-18 11:28:08 -0500 (Wed, 18 Feb 2009) $
 */
public class StatsFilter extends GenericFilter {

    private List<PlayerStats> stats;

    private StopWatch stopWatch;

    private static DecimalFormat df = new DecimalFormat("0.00");

    public void init() {
        stopWatch = new StopWatch();
        stats = new ArrayList<PlayerStats>(6);
        for (int i = 0; i < 6; i++) {
            stats.add(null);
        }
    }

    public void onMessage(StartGameMessage m, List<Message> out) {
        if (getChannel().getGameState() == STOPPED) {
            stopWatch.reset();
            stopWatch.start();
            for (int i = 0; i < 6; i++) {
                if (getChannel().getClient(i + 1) != null) {
                    stats.set(i, new PlayerStats());
                } else {
                    stats.set(i, null);
                }
            }
        }
        out.add(m);
    }

    public void onMessage(EndGameMessage m, List<Message> out) {
        out.add(m);
        if (getChannel().getGameState() != STOPPED) {
            stopWatch.stop();
            displayStats(out);
        }
    }

    public void onMessage(PauseMessage m, List<Message> out) {
        if (getChannel().getGameState() == STARTED) {
            stopWatch.suspend();
        }
        out.add(m);
    }

    public void onMessage(ResumeMessage m, List<Message> out) {
        if (getChannel().getGameState() == PAUSED) {
            stopWatch.resume();
        }
        out.add(m);
    }

    public void onMessage(FieldMessage m, List<Message> out) {
        PlayerStats playerStats = stats.get(m.getSlot() - 1);
        if (playerStats != null && (stopWatch.getTime() > 1500)) {
            playerStats.blockCount++;
        }
        out.add(m);
    }

    public void onMessage(LinesAddedMessage m, List<Message> out) {
        out.add(m);
        updateStats(m);
        removeBlock(m);
    }

    public void onSpecial(SpecialMessage m, List<Message> out) {
        if (!(m instanceof LinesAddedMessage)) {
            PlayerStats playerStats = stats.get(m.getSlot() - 1);
            playerStats.specialsReceived++;
            playerStats.blockCount = playerStats.blockCount - 2;
            playerStats = stats.get(m.getFromSlot() - 1);
            playerStats.specialsSent++;
        }
    }

    public void onMessage(LevelMessage m, List<Message> out) {
        out.add(m);
        PlayerStats playerStats = stats.get(m.getSlot() - 1);
        if (playerStats != null) {
            playerStats.level = m.getLevel();
        }
    }

    public void onMessage(LeaveMessage m, List<Message> out) {
        out.add(m);
        stats.set(m.getSlot() - 1, null);
    }

    public void onMessage(PlayerLostMessage m, List<Message> out) {
        out.add(m);
        PlayerStats playerStats = stats.get(m.getSlot() - 1);
        if (playerStats != null) {
            playerStats.playing = false;
            playerStats.timePlayed = stopWatch.getTime();
        }
    }

    /**
     * Decrease the block count of players receiving an add to all message since
     * they will send back a field message assimilated by mistake as a block fall.
     */
    private void removeBlock(SpecialMessage message) {
        int slot = message.getFromSlot();
        String team = null;
        if (message.getSource() != null && message.getSource() instanceof Client) {
            Client client = (Client) message.getSource();
            team = client.getUser().getTeam();
        }
        for (int i = 1; i <= 6; i++) {
            Client client = getChannel().getClient(i);
            if (i != slot && client != null) {
                User user = client.getUser();
                if (user.isPlaying() && (user.getTeam() == null || !user.getTeam().equals(team))) {
                    PlayerStats playerStats = stats.get(i - 1);
                    playerStats.blockCount--;
                }
            }
        }
    }

    /**
     * Update the stats of the player sending the specified message.
     *
     * @param message
     */
    private void updateStats(LinesAddedMessage message) {
        if (message.getFromSlot() > 0) {
            PlayerStats playerStats = stats.get(message.getFromSlot() - 1);
            if (playerStats != null) {
                playerStats.linesAdded += message.getLinesAdded();
                if (message.getLinesAdded() == 4) {
                    playerStats.tetrisCount++;
                }
            }
        }
    }

    private void displayStats(List<Message> out) {
        for (int slot = 1; slot <= 6; slot++) {
            PlayerStats playerStats = stats.get(slot - 1);
            User user = getChannel().getPlayer(slot);
            if (playerStats != null && user != null) {
                if (playerStats.playing) {
                    playerStats.timePlayed = stopWatch.getTime();
                }
                String bpm = df.format(playerStats.getBlocksPerMinute());
                StringBuilder text = new StringBuilder();
                text.append("<purple>" + user.getName() + "</purple> : ");
                text.append(playerStats.blockCount + " <aqua>blocks @<red>" + bpm + "</red> bpm, ");
                text.append("<black>" + playerStats.linesAdded + "</black> added, ");
                text.append("<black>" + playerStats.tetrisCount + "</black> tetris");
                if (getChannel().getConfig().getSettings().getSpecialAdded() > 0) {
                    text.append(", <black>" + playerStats.specialsSent + " / " + playerStats.specialsReceived + "</black> specials");
                }
                out.add(new PlineMessage(text.toString()));
            }
        }
        PlineMessage time = new PlineMessage();
        time.setText("<brown>Total game time: <black>" + df.format(stopWatch.getTime() / 1000f) + "</black> seconds");
        out.add(time);
    }

    public String getName() {
        return "Stats Filter";
    }

    public String getDescription() {
        return "Displays stats about the game (pieces dropped per minute, lines added to all, time played, etc";
    }

    public String getVersion() {
        return "1.1";
    }

    public String getAuthor() {
        return "Emmanuel Bourg";
    }

    private class PlayerStats {

        long timePlayed;

        int tetrisCount;

        int linesAdded;

        int blockCount;

        int level;

        int specialsSent;

        int specialsReceived;

        boolean playing = true;

        public double getBlocksPerMinute() {
            return (double) blockCount * 60000 / (double) timePlayed;
        }
    }
}
