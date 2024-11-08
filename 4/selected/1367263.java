package com.chessclub.simulbot.commands;

import com.chessclub.simulbot.Settings;
import com.chessclub.simulbot.SimulHandler;
import com.chessclub.simulbot.chess.Player;
import com.chessclub.simulbot.datagrams.Tell;
import com.chessclub.simulbot.objects.PlayerList;

public class Close {

    private static int ACCESS_LEVEL = Settings.SIMUL_GIVER;

    public static void close(Tell t) {
        int authorized = Common.isAuthorized(t);
        String message;
        if (authorized < ACCESS_LEVEL) {
            message = "You are not authorized to issue this command";
        } else if (SimulHandler.getStatus() == Settings.NO_SIMUL) {
            message = "There is no simul to close.";
        } else if (SimulHandler.getStatus() == Settings.SIMUL_RUNNING) {
            message = "Use the \"finish\" command to finish a simul in progress.";
        } else {
            message = "The simul is now closed.";
            closeSimul();
        }
        SimulHandler.getHandler().tell(t.getHandle(), message);
    }

    public static void closeSimul() {
        Open.closeAdvertising();
        clearTourneyStuff();
        if (SimulHandler.getStatus() == Settings.SIMUL_OPEN) {
            String message = "The current simul has been closed. Please watch channel " + SimulHandler.getChannel() + " or ask the simul giver for more information.";
            Common.tellAllPlayers(message);
        }
        if (SimulHandler.getChannelOut()) {
            String message = "**** " + SimulHandler.getGiver().getDisplayHandle(false) + "'s simul has been closed. ****";
            SimulHandler.getHandler().tell(SimulHandler.getChannel(), message);
        }
        Common.resetSimul();
    }

    public static void clearTourneyStuff() {
        PlayerList players = SimulHandler.getPlayers();
        if (players == null) return;
        for (Player p : SimulHandler.getPlayers()) {
            SimulHandler.getHandler().qclearTourney(p.getHandle());
        }
    }
}
