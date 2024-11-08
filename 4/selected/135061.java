package com.chessclub.simulbot.commands;

import com.chessclub.simulbot.Library;
import com.chessclub.simulbot.Settings;
import com.chessclub.simulbot.SimulHandler;
import com.chessclub.simulbot.chess.Player;
import com.chessclub.simulbot.datagrams.Tell;
import com.chessclub.simulbot.objects.RecordKeeper;
import com.chessclub.simulbot.timers.ElapsedTimer;
import com.chessclub.simulbot.timers.FollowTimer;

public class Finish {

    private static int ACCESS_LEVEL = Settings.SIMUL_GIVER;

    public static void finish(Tell t) {
        int authorized = Common.isAuthorized(t);
        t.truncateFirstSpace();
        String simulGiver = Library.grabUptoFirstSpace(t.getMessage(true));
        if (authorized < ACCESS_LEVEL) {
            String message = "You are not authorized to issue this command";
            SimulHandler.getHandler().tell(t.getHandle(), message);
        } else if (SimulHandler.getStatus() < Settings.SIMUL_RUNNING) {
            String message = "You cannot finish the simul because it hasn't begun.";
            SimulHandler.getHandler().tell(t.getHandle(), message);
        } else if (SimulHandler.getSimulGames().size() != 0) {
            String message = "You cannot finish the simul while there are still live games being played.";
            SimulHandler.getHandler().tell(t.getHandle(), message);
        } else {
            finishSimul();
        }
    }

    public static void finishSimul() {
        SimulHandler.getHandler().qremoveevent(Settings.EVENT_TYPE);
        FollowTimer.cancel();
        ElapsedTimer.cancel();
        if (SimulHandler.isAutoAdjud()) {
            Common.clearAdjournedGames();
        }
        for (Player p : SimulHandler.getPlayers()) {
            SimulHandler.getHandler().qclear(p.getHandle());
        }
        if (SimulHandler.getMessWinners()) {
            Common.messageWinnersList();
        }
        RecordKeeper.incSimulsManaged(SimulHandler.getManager());
        History.addSimul(Library.getDate() + SimulHandler.getGiver().getDisplayHandle(false) + " +" + SimulHandler.getWins() + " =" + SimulHandler.getDraws() + " -" + SimulHandler.getLosses() + "\n");
        if (SimulHandler.isLottery()) {
            RecordKeeper.incTotalSimuls();
        }
        while (SimulHandler.getPlayersWithAdjournedGames().size() != 0) {
            SimulHandler.setTotal(SimulHandler.getTotal() - 1);
            Remove.removeByHandle(SimulHandler.getPlayersWithAdjournedGames().get(0).getHandle(), true);
        }
        SimulHandler.getHandler().tell(SimulHandler.getGiver().getHandle(), "You have finished the simul!");
        if (SimulHandler.getChannelOut()) {
            Results.printFinalResults();
        }
        Common.resetSimul();
    }
}
