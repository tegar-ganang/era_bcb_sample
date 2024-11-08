package net.jetrix.filter;

import static java.lang.Math.*;
import static net.jetrix.GameState.*;
import net.jetrix.*;
import net.jetrix.config.*;
import net.jetrix.messages.channel.*;
import net.jetrix.messages.channel.specials.*;
import java.util.*;
import org.apache.commons.lang.time.StopWatch;

/**
 * Sudden death mode. This filter implements the well known sudden death mode
 * from tetrinetx: after a given time, lines are added to all players at a
 * specified rate until the game ends.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 798 $, $Date: 2009-02-18 10:24:28 -0500 (Wed, 18 Feb 2009) $
 */
public class SuddenDeathFilter extends GenericFilter {

    private StopWatch stopWatch;

    private Timer timer;

    public void init() {
        stopWatch = new StopWatch();
    }

    public void onMessage(StartGameMessage m, List<Message> out) {
        out.add(m);
        if (getChannel().getGameState() == STOPPED) {
            stopWatch.reset();
            stopWatch.start();
        }
        Settings settings = getChannel().getConfig().getSettings();
        int time = settings.getSuddenDeathTime() * 1000;
        if (time > 0) {
            timer = new Timer();
            timer.schedule(new Task(time), max(0, time - Task.WARNING_DELAY * 1000), 200);
        }
    }

    public void onMessage(EndGameMessage m, List<Message> out) {
        out.add(m);
        if (getChannel().getGameState() != STOPPED) {
            stopWatch.stop();
        }
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void onMessage(PauseMessage m, List<Message> out) {
        out.add(m);
        if (getChannel().getGameState() == STARTED) {
            stopWatch.suspend();
        }
    }

    public void onMessage(ResumeMessage m, List<Message> out) {
        out.add(m);
        if (getChannel().getGameState() == PAUSED) {
            stopWatch.resume();
        }
    }

    private class Task extends TimerTask {

        public static final int WARNING_DELAY = 60;

        private boolean suddenDeathEnabled;

        private long nextTriggerTime;

        private long nextWarningTime;

        public Task(long nextTriggerTime) {
            this.nextTriggerTime = nextTriggerTime;
            this.nextWarningTime = nextTriggerTime - WARNING_DELAY * 1000;
        }

        public void run() {
            Settings settings = getChannel().getConfig().getSettings();
            if (stopWatch.getTime() >= nextTriggerTime) {
                if (!suddenDeathEnabled) {
                    suddenDeathEnabled = true;
                    GmsgMessage gmsg = new GmsgMessage();
                    String message = settings.getSuddenDeathMessage();
                    if (message.startsWith("key:")) {
                        gmsg.setKey(message.substring(4));
                    } else {
                        gmsg.setText(message);
                    }
                    getChannel().send(gmsg);
                    GmsgMessage rate = new GmsgMessage();
                    rate.setKey("filter.suddendeath.rate", settings.getSuddenDeathLinesAdded(), settings.getSuddenDeathDelay());
                    getChannel().send(rate);
                }
                sendLines(settings.getSuddenDeathLinesAdded());
                nextTriggerTime = nextTriggerTime + settings.getSuddenDeathDelay() * 1000;
            } else if (stopWatch.getTime() >= nextWarningTime && !suddenDeathEnabled) {
                GmsgMessage gmsg = new GmsgMessage();
                gmsg.setKey("filter.suddendeath.warning", Math.ceil((nextTriggerTime - stopWatch.getTime()) / 1000d));
                getChannel().send(gmsg);
                nextWarningTime = nextWarningTime + WARNING_DELAY * 1000;
            }
        }

        /**
         * Add lines to all players in the channel
         *
         * @param count the number of lines to add
         */
        private void sendLines(int count) {
            Channel channel = getChannel();
            if (count == 1) {
                channel.send(new OneLineAddedMessage());
            }
            if (count >= 4) {
                channel.send(new FourLinesAddedMessage());
                sendLines(count - 4);
            } else if (count >= 2) {
                channel.send(new TwoLinesAddedMessage());
                sendLines(count - 2);
            }
        }
    }

    public String getName() {
        return "Sudden Death";
    }

    public String getDescription() {
        return "Sudden death mode for never ending games";
    }

    public String getVersion() {
        return "1.0";
    }

    public String getAuthor() {
        return "Emmanuel Bourg";
    }
}
