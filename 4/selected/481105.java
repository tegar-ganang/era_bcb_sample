package net.jetrix.filter;

import java.util.*;
import net.jetrix.*;
import net.jetrix.config.*;
import net.jetrix.messages.*;

/**
 * A filter computing and displaying the number of pieces dropped per minute
 * by each player.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 8 $, $Date: 2002-10-19 12:16:08 -0400 (Sat, 19 Oct 2002) $
 */
public class PPMFilter extends GenericFilter {

    private long totalTime;

    private long lastStart;

    private int blockCount[];

    private long slotTime[];

    public void init(FilterConfig conf) {
        blockCount = new int[6];
        slotTime = new long[6];
    }

    public void onMessage(StartGameMessage m, List out) {
        totalTime = 0;
        lastStart = new Date().getTime();
        Arrays.fill(blockCount, 0);
        Arrays.fill(slotTime, 0);
        out.add(m);
    }

    public void onMessage(EndGameMessage m, List out) {
        long now = new Date().getTime();
        for (int slot = 0; slot < 6; slot++) {
            slotTime[slot] = slotTime[slot] + (now - lastStart);
        }
        for (int slot = 0; slot < 6; slot++) {
            Client client = getChannel().getPlayer(slot);
            Player player = client.getPlayer();
            System.out.println(player.getName() + " : " + "time played=" + slotTime[slot] + " ");
        }
        out.add(m);
    }

    public void onMessage(PauseMessage m, List out) {
        long now = new Date().getTime();
        totalTime = totalTime + (now - lastStart);
        out.add(m);
    }

    public void onMessage(ResumeMessage m, List out) {
        lastStart = new Date().getTime();
        out.add(m);
    }

    public void onMessage(FieldMessage m, List out) {
        int slot = m.getSlot();
        blockCount[slot]++;
        out.add(m);
    }

    public String getName() {
        return "PPM Filter";
    }

    public String getDescription() {
        return "Counts pieces dropped per minute by each player in a channel " + "and displays the result at the end of the game.";
    }

    public String getVersion() {
        return "1.0";
    }

    public String getAuthor() {
        return "Emmanuel Bourg";
    }
}
