package com.chessclub.simulbot.commands;

import com.chessclub.simulbot.Library;
import com.chessclub.simulbot.Settings;
import com.chessclub.simulbot.SimulHandler;
import com.chessclub.simulbot.chess.Player;
import com.chessclub.simulbot.datagrams.Tell;
import com.chessclub.simulbot.objects.Game;
import com.chessclub.simulbot.objects.Qtell;
import com.chessclub.simulbot.objects.RecordKeeper;

/**
 * This class is designed to handle the "remove" command.
 */
public class Remove {

    private static int ACCESS_LEVEL = Settings.SIMUL_GIVER;

    public static void remove(Tell t) {
        int authorized = Common.isAuthorized(t);
        if (authorized < ACCESS_LEVEL) {
            SimulHandler.getHandler().tell(t.getHandle(), "You are not authorized to issue this command");
            return;
        }
        String player = Library.grabUptoFirstSpace(Library.truncateFirstSpace(t.getMessage(true)));
        boolean wasInSimul = Common.inSimul(player);
        if (!wasInSimul) {
            SimulHandler.getHandler().tell(t.getHandle(), player + " has not been removed because he/she was not in the simul.");
        } else {
            SimulHandler.getHandler().tell(player, t.getHandle() + " has removed you from the simul.");
            SimulHandler.getRecorder().record(player + " has was removed from the simul by " + t.getHandle());
            remove(player);
        }
    }

    public static void remove(String player) {
        SimulHandler.setTotal(SimulHandler.getTotal() - 1);
        Player p = Common.findPlayer(player);
        removeByHandle(player, false);
        if (SimulHandler.getChannelOut()) {
            Qtell qtell = new Qtell(SimulHandler.getChannel());
            qtell.addLine(p.getDisplayHandle(false) + " has left " + SimulHandler.getGiver().getDisplayHandle(false) + "'s simul - " + Common.buildRemainingString(false));
            qtell.send();
            SimulHandler.getHandler().qremoveevent(Settings.EVENT_TYPE);
            SimulHandler.getHandler().qaddevent(Common.buildEventString(false));
        }
    }

    public static void removeByHandle(String player, boolean doneWithSimul) {
        if (doneWithSimul) {
            SimulHandler.getFinishedPlayers().add(Common.findPlayer(player));
            RecordKeeper.setSimulsPlayed(player, RecordKeeper.getSimulsPlayed(player) + 1);
            if (SimulHandler.isLottery()) {
                RecordKeeper.setLotteries(player, RecordKeeper.getLotteries(player) + 1);
                RecordKeeper.setLastLottery(player, RecordKeeper.PLAYED);
            }
        }
        SimulHandler.getHandler().qclearTourney(player);
        SimulHandler.getHandler().minusNotify(player);
        SimulHandler.getPlayers().remove(Common.findPlayer(player));
    }

    public static void removeByGame(Game g) {
        if (Common.inSimul(g.getWhite().getHandle())) {
            removeByHandle(g.getWhite().getHandle(), true);
            SimulHandler.getRecorder().record("Removing " + g.getWhite().getHandle() + " from simul because his game is finished.");
        }
        if (Common.inSimul(g.getBlack().getHandle())) {
            removeByHandle(g.getBlack().getHandle(), true);
            SimulHandler.getRecorder().record("Removing " + g.getBlack().getHandle() + " from simul because his game is finished.");
        }
    }
}
