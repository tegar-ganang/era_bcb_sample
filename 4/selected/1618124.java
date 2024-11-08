package org.granite.gravity;

import java.util.TimerTask;

/**
 * @author Franck WOLFF
 */
public class TimeChannel {

    private final Channel channel;

    private TimerTask timerTask;

    public TimeChannel(Channel channel) {
        this(channel, null);
    }

    public TimeChannel(Channel channel, TimerTask timerTask) {
        if (channel == null) throw new NullPointerException("Channel cannot be null");
        this.channel = channel;
        this.timerTask = timerTask;
    }

    public Channel getChannel() {
        return channel;
    }

    public TimerTask getTimerTask() {
        return timerTask;
    }

    public void setTimerTask(TimerTask timerTask) {
        this.timerTask = timerTask;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TimeChannel && channel.equals(((TimeChannel) obj).channel);
    }

    @Override
    public int hashCode() {
        return channel.hashCode();
    }
}
