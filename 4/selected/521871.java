package mudstrate.engine;

import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import mudstrate.init.MudEnvironment;
import mudstrate.user.User;
import mudstrate.util.Comm;
import mudstrate.util.Util;

public class GlobalTicker extends Timer {

    private static GlobalTicker instance = new GlobalTicker();

    public static GlobalTicker getInstance() {
        return GlobalTicker.instance;
    }

    private Date bootTime = new Date();

    private TimerTask sendOutputs = new TimerTask() {

        @Override
        public void run() {
            for (User u : UserHandler.getInstance().getAllUsers()) {
                if (u.outputBuffer.length() > 0) {
                    Comm.writeToChannel(u.getChannel(), u.outputBuffer.toString() + u.getCurrentLayer().getPrompt());
                    u.outputBuffer.delete(0, u.outputBuffer.length());
                }
            }
        }
    };

    private GlobalTicker() {
        this.scheduleAtFixedRate(this.sendOutputs, 0, MudEnvironment.getInstance().getResponseTime());
    }

    public String getDateString() {
        return new Date().toString();
    }

    public String getUptimeString() {
        long upMillis = new Date().getTime() - this.bootTime.getTime();
        HashMap<String, Integer> time = this.parseMillis(upMillis);
        int days = time.get("days");
        int hours = time.get("hours");
        int mins = time.get("minutes");
        int seconds = time.get("seconds");
        return days + " day" + (days == 1 ? "" : "s") + ", " + hours + " hour" + (hours == 1 ? "" : "s") + ", " + mins + " minute" + (mins == 1 ? "" : "s") + ", " + seconds + " second" + (seconds == 1 ? "" : "s");
    }

    private HashMap<String, Integer> parseMillis(long millis) {
        HashMap<String, Integer> time = new HashMap<String, Integer>();
        int seconds = (int) (millis / 1000);
        int days = seconds / 86400;
        seconds -= days * 86400;
        int hours = seconds / 3600;
        seconds -= hours * 3600;
        int mins = seconds / 60;
        seconds -= mins * 60;
        time.put("days", days);
        time.put("hours", hours);
        time.put("minutes", mins);
        time.put("seconds", seconds);
        return time;
    }
}
