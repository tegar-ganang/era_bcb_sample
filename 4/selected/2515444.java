package net.jetrix.filter;

import java.util.*;
import net.jetrix.*;
import net.jetrix.messages.channel.PlineMessage;

/**
 * Blocks spam over pline.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 798 $, $Date: 2009-02-18 10:24:28 -0500 (Wed, 18 Feb 2009) $
 */
public class FloodFilter extends GenericFilter {

    private long timestamp[][];

    private int index[];

    private int capacity = 8;

    private int delay = 5000;

    /** Minimum time between two warnings (in seconds) */
    private int warningPeriod = 10;

    /** Date of the last warning */
    private long lastWarning;

    public void init() {
        capacity = config.getInt("capacity", capacity);
        delay = config.getInt("delay", delay);
        warningPeriod = config.getInt("warningPeriod", warningPeriod);
        timestamp = new long[6][capacity];
        index = new int[6];
    }

    public void onMessage(PlineMessage m, List<Message> out) {
        int slot = m.getSlot();
        if (slot < 1 || slot > 6) {
            out.add(m);
            return;
        }
        String text = m.getText();
        float charsByLine = 70;
        int lineCount = (int) Math.ceil(text.length() / charsByLine);
        long now = System.currentTimeMillis();
        boolean isRateExceeded = false;
        for (int i = 0; i < lineCount; i++) {
            isRateExceeded = isRateExceeded || isRateExceeded(slot - 1, now);
        }
        if (slot > 0 && isRateExceeded) {
            if ((now - lastWarning) > warningPeriod * 1000) {
                User user = getChannel().getPlayer(slot);
                out.add(new PlineMessage("filter.flood.blocked", user.getName()));
                lastWarning = now;
            }
        } else {
            out.add(m);
        }
    }

    /**
     * Records a message timestamp and checks the data rate.
     *
     * @param slot  message source slot
     * @param t     message timestamp
     *
     * @return <tt>true</tt> if over <tt>capacity</tt> messages in less than the <tt>delay</tt> specified
     */
    private boolean isRateExceeded(int slot, long t) {
        long t1 = timestamp[slot][index[slot]];
        timestamp[slot][index[slot]] = t;
        index[slot] = (index[slot] + 1) % capacity;
        return (t - t1) < delay;
    }

    public String getName() {
        return "Flood Filter";
    }

    public String getDescription() {
        return "Blocks exceeding messages on pline";
    }

    public String getVersion() {
        return "1.0";
    }

    public String getAuthor() {
        return "Emmanuel Bourg";
    }
}
