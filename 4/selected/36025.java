package com.chessclub.simulbot.timers;

import java.util.Timer;
import java.util.TimerTask;
import com.chessclub.simulbot.SimulHandler;
import com.chessclub.simulbot.commands.Common;
import com.chessclub.simulbot.commands.Latejoin;

public class LatejoinTimer {

    private static final long LATEJOIN_PERIOD = 5 * 60 * 1000;

    private static final Timer timer = new Timer("latejoin timer", false);

    private static TimerTask task;

    private LatejoinTimer() {
    }

    public static synchronized void start() {
        if (task != null) {
            cancel();
        }
        Latejoin.setLateJoinAllowed(true);
        task = new TimerTask() {

            @Override
            public void run() {
                Latejoin.setLateJoinAllowed(false);
                if (SimulHandler.getChannelOut()) {
                    SimulHandler.getHandler().qaddevent(Common.buildFollowEventString());
                }
            }
        };
        timer.schedule(task, LATEJOIN_PERIOD);
    }

    public static synchronized void cancel() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
