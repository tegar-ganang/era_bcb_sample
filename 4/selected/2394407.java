package com.chessclub.simulbot.commands;

import com.chessclub.simulbot.Settings;
import com.chessclub.simulbot.SimulHandler;
import com.chessclub.simulbot.datagrams.Tell;
import com.chessclub.simulbot.listeners.AdminListener;

public class Announce {

    private static int ACCESS_LEVEL = Settings.SUPER;

    public static void announce(Tell t) {
        int authorized = Common.isAuthorized(t);
        if (authorized < ACCESS_LEVEL) {
            SimulHandler.getHandler().tell(t.getHandle(), "You are not authorized to issue this command");
            return;
        }
        if (SimulHandler.getStatus() != Settings.SIMUL_OPEN && SimulHandler.getStatus() != Settings.SIMUL_RUNNING) {
            SimulHandler.getHandler().tell(t.getHandle(), "You cannot issue this command because the simul is not in the proper state.");
        } else if (!AdminListener.isAdmin()) {
            SimulHandler.getHandler().tell(t.getHandle(), "You cannot issue this command because " + Settings.username + " does not currently have admin status.");
        } else if (!SimulHandler.getChannelOut()) {
            SimulHandler.getHandler().tell(t.getHandle(), "You cannot issue this command because channlOut is set to \"false\".");
        } else if (SimulHandler.getStatus() == Settings.SIMUL_OPEN) {
            announceSimul();
        } else if (SimulHandler.getStatus() == Settings.SIMUL_RUNNING) {
            announceFollow();
        }
    }

    public static void announceSimul() {
        SimulHandler.getHandler().loudshout(Common.buildSimulString());
    }

    public static void announceFollow() {
        SimulHandler.getHandler().loudshout(Common.buildFollowString());
    }
}
