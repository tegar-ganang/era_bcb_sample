package com.chessclub.simulbot.commands;

import com.chessclub.simulbot.Settings;
import com.chessclub.simulbot.SimulHandler;
import com.chessclub.simulbot.chess.Player;
import com.chessclub.simulbot.objects.Game;
import com.chessclub.simulbot.objects.Qtell;

public class Results {

    public static void results(String handle) {
        if (SimulHandler.getStatus() < Settings.SIMUL_RUNNING) {
            SimulHandler.getHandler().tell(handle, "You cannot see results before the simul is in progress.");
        } else {
            printResults(handle);
        }
    }

    public static void printResults(String handle) {
        Qtell qtell = new Qtell(handle);
        qtell.addLine("");
        qtell.addLine("Results for " + SimulHandler.getGiver().getDisplayHandle(false) + " with " + (SimulHandler.getTotal() - SimulHandler.getWins() - SimulHandler.getLosses() - SimulHandler.getDraws()) + " games to go:");
        qtell.addLine(SimulHandler.getWins() + " win" + (SimulHandler.getWins() == 1 ? "" : "s") + ", " + SimulHandler.getDraws() + " draw" + (SimulHandler.getDraws() == 1 ? "" : "s") + ", " + SimulHandler.getLosses() + " loss" + (SimulHandler.getLosses() == 1 ? "" : "es") + ", and " + (SimulHandler.getSimulGames().size() + SimulHandler.getPlayersWithAdjournedGames().size()) + " remaining.");
        qtell.addLine("");
        qtell.send();
    }

    public static void printResultsInProgress(Game g) {
        Qtell qtell = new Qtell(SimulHandler.getChannel());
        qtell.addLine(g.getWhite().getDisplayHandle(true) + " vs " + g.getBlack().getDisplayHandle(true) + " : " + g.getResult());
        qtell.addLine(SimulHandler.getGiver().getDisplayHandle(false) + "'s simul: " + SimulHandler.getWins() + " win" + (SimulHandler.getWins() == 1 ? "" : "s") + ", " + SimulHandler.getDraws() + " draw" + (SimulHandler.getDraws() == 1 ? "" : "s") + ", " + SimulHandler.getLosses() + " loss" + (SimulHandler.getLosses() == 1 ? "" : "es") + ", and " + (SimulHandler.getSimulGames().size() + SimulHandler.getPlayersWithAdjournedGames().size()) + " remaining.");
        qtell.send();
    }

    public static void printFinalResults() {
        double totalGames = (double) SimulHandler.getWins() + SimulHandler.getDraws() + SimulHandler.getLosses();
        Qtell qtell = new Qtell(SimulHandler.getChannel());
        qtell.addLine("");
        qtell.addLine("Results for " + SimulHandler.getGiver().getDisplayHandle(false) + "'s simul: ");
        qtell.addLine("wins: " + SimulHandler.getWins() + " (" + Common.roundDouble(100 * SimulHandler.getWins() / totalGames) + "%)");
        qtell.addLine("draws: " + SimulHandler.getDraws() + " (" + Common.roundDouble(100 * SimulHandler.getDraws() / totalGames) + "%)");
        ;
        qtell.addLine("losses: " + SimulHandler.getLosses() + " (" + Common.roundDouble(100 * SimulHandler.getLosses() / totalGames) + "%)");
        qtell.addLine("");
        qtell.addLine("Congratulations to the (over-the-board) winners:");
        qtell.send();
        for (Player p : SimulHandler.getWinners()) {
            qtell.add(p.getDisplayHandle(false));
        }
        qtell.addLine("");
        qtell.addLine("");
        qtell.send();
    }
}
