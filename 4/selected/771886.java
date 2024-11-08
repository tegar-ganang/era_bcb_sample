package com.chessclub.simulbot.commands;

import com.chessclub.simulbot.Library;
import com.chessclub.simulbot.Settings;
import com.chessclub.simulbot.SimulHandler;
import com.chessclub.simulbot.chess.Player;
import com.chessclub.simulbot.datagrams.Tell;
import com.chessclub.simulbot.listeners.AdminListener;
import com.chessclub.simulbot.objects.PlayerList;
import com.chessclub.simulbot.objects.Raffle;
import com.chessclub.simulbot.objects.RecordKeeper;
import com.chessclub.simulbot.timers.ElapsedTimer;
import com.chessclub.simulbot.timers.FollowTimer;
import com.chessclub.simulbot.timers.LatejoinTimer;

/**
 * This class is designed to handle the "start" command.
 */
public class Start {

    private static int ACCESS_LEVEL = Settings.SIMUL_GIVER;

    private static String simulString;

    private static String lastTeller;

    private static final int DEFAULT_TICKETS = 20;

    private static final int SIMULS_PLAYED_MODIFIER = 5;

    private static final int PLAYED_LAST_SIMUL_VALUE = -15;

    private static final int LOST_LAST_LOTTERY_VALUE = 20;

    private static final int DID_NOT_PLAY_LAST_SIMUL_VALUE = 10;

    public static void start(Tell t) {
        int authorized = Common.isAuthorized(t);
        if (authorized < ACCESS_LEVEL) {
            SimulHandler.getHandler().tell(t.getHandle(), "You are not authorized to issue this command");
        } else if (SimulHandler.getGiver().equals("") || SimulHandler.getGiver().getHandle().equalsIgnoreCase(Load.NO_SIMUL_GIVER)) {
            SimulHandler.getHandler().tell(t.getHandle(), "You cannot start the simul because no simul giver has been set.");
        } else if (SimulHandler.getStatus() != Settings.SIMUL_OPEN && SimulHandler.getStatus() != Settings.GIVER_DISCONNECTED) {
            SimulHandler.getHandler().tell(t.getHandle(), "You cannot start the simul because the simul is not currently open or paused.");
        } else if (SimulHandler.getPlayers().size() == 0) {
            SimulHandler.getHandler().tell(t.getHandle(), "You cannot start the simul because the simul no players have joined.");
        } else if (!SimulHandler.getGiver().isLoggedOn()) {
            SimulHandler.getHandler().tell(t.getHandle(), "You cannot start the simul because the simul give is not logged on.");
        } else {
            SimulHandler.getRecorder().record(t.getHandle() + " has started the simul");
            lastTeller = t.getHandle();
            startSimul();
        }
    }

    public static void startSimul() {
        SimulHandler.setStatus(Settings.SIMUL_RUNNING);
        Open.closeAdvertising();
        if (SimulHandler.getPlayers().size() > SimulHandler.getMaxPlayers() && SimulHandler.isLottery()) {
            runLottery();
        }
        if (SimulHandler.getChannelOut()) {
            if (SimulHandler.getPlayers().size() == SimulHandler.getMaxPlayers()) {
                SimulHandler.getHandler().qaddevent(Common.buildFollowEventString());
            } else {
                SimulHandler.getHandler().qaddevent(Common.buildEventString(true));
            }
        }
        String setVars = "multi set open 1; set ropen 1; set wopen 1; set useformula 0; set noescape 0";
        SimulHandler.getHandler().spoof(SimulHandler.getGiver().getHandle(), setVars, "Enable Match Requests");
        for (Player p : SimulHandler.getPlayers()) {
            if (SimulHandler.isRandColor()) {
                SimulHandler.setGiverWhite(Library.randomBoolean());
            }
            SimulHandler.getHandler().qsetTourney(p.getHandle(), Common.buildMatchSettings(true, false), !SimulHandler.isGiverWhite());
            SimulHandler.getHandler().qmatch(p.getHandle(), SimulHandler.getGiver().getHandle());
            sleep(10);
        }
        simulString = "multi ";
        for (Player p : SimulHandler.getPlayers()) {
            simulString += "+simul " + p.getHandle() + "; ";
            SimulHandler.getHandler().spoof(SimulHandler.getGiver().getHandle(), "+simul " + p.getHandle());
            sleep(10);
        }
        SimulHandler.getHandler().spoof(SimulHandler.getGiver().getHandle(), "tell " + Settings.username + " feedback " + Settings.PASS_TO_START_2);
        if (!AdminListener.isAdmin()) {
            SimulHandler.getHandler().qsuggest(SimulHandler.getGiver().getHandle(), "tell " + Settings.username + " feedback " + Settings.PASS_TO_START_2, "Confirm starts");
        }
    }

    public static void start2() {
        SimulHandler.getHandler().spoof(SimulHandler.getGiver().getHandle(), "startsimul", "Start the Simul");
        simulString += "startsimul";
        if (!AdminListener.isAdmin()) {
            SimulHandler.getHandler().qsuggest(SimulHandler.getGiver().getHandle(), simulString, "Start the simul.");
        }
        LatejoinTimer.start();
        FollowTimer.start();
        ElapsedTimer.start();
        SimulHandler.getHandler().tell(lastTeller, "The simul has started.");
        if (SimulHandler.getChannelOut()) {
            if (SimulHandler.getPlayers().size() < SimulHandler.getMaxPlayers()) {
                SimulHandler.getHandler().tell(SimulHandler.getChannel(), SimulHandler.getGiver().getDisplayHandle(false) + "'s simul has started, but there is still room for more players! \"tell " + Settings.username + " latejoin\" to join the action!");
            } else {
                SimulHandler.getHandler().tell(SimulHandler.getChannel(), SimulHandler.getGiver().getDisplayHandle(false) + "'s simul has started! \"tell " + Settings.username + " follow\" to follow the action!");
            }
        }
    }

    public static void sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void runLottery() {
        Raffle raffle = new Raffle();
        PlayerList players = SimulHandler.getPlayers();
        for (int i = 0; i < players.size(); ++i) {
            String handle = players.get(i).getHandle();
            int times = determineTickets(handle);
            raffle.add(handle, times);
            if (times == 0) {
                SimulHandler.getHandler().tell(handle, "Sorry, but you were not picked to be in the simul.");
                Remove.removeByHandle(handle, false);
            }
        }
        try {
            for (int i = 0; i < SimulHandler.getMaxPlayers(); ++i) {
                Player p = Common.findPlayer(raffle.pickWinner());
                SimulHandler.getHandler().tell(p.getHandle(), "You have been picked to be in the simul!");
                raffle.removeAll(p.getHandle());
            }
        } catch (Exception e) {
            SimulHandler.getHandler().tell("clutch", "Error: " + e);
        }
        raffle.minimize();
        if (SimulHandler.isLottery()) {
            RecordKeeper.clearLastLottery();
        }
        for (int i = 0; i < raffle.size(); ++i) {
            String handle = raffle.get(i);
            SimulHandler.getHandler().tell(handle, "Sorry, but you were not picked to be in the simul.");
            RecordKeeper.setLastLottery(handle, RecordKeeper.LOST_LOTTERY);
            Remove.removeByHandle(handle, false);
        }
        SimulHandler.setTotal(SimulHandler.getMaxPlayers());
    }

    public static void startAdjournedSimul() {
        SimulHandler.getHandler().qclear(Settings.username);
        for (Player p : SimulHandler.getPlayers()) {
            if (p.isLoggedOn()) {
                SimulHandler.getHandler().qmatch(p.getHandle(), SimulHandler.getGiver().getHandle());
            }
            sleep(10);
        }
        for (Player p : SimulHandler.getPlayers()) {
            if (p.isLoggedOn()) {
                SimulHandler.getHandler().spoof(SimulHandler.getGiver().getHandle(), "+simul " + p.getHandle());
            }
            sleep(10);
        }
        SimulHandler.getHandler().spoof(SimulHandler.getGiver().getHandle(), "tell " + Settings.username + " feedback " + Settings.PASS_TO_START_3, "Confirm starts");
    }

    public static void startAdjournedSimul2() {
        SimulHandler.getHandler().spoof(SimulHandler.getGiver().getHandle(), "startsimul");
        SimulHandler.setFirstGame(true);
        if (SimulHandler.getChannelOut()) {
            SimulHandler.getHandler().tell(SimulHandler.getChannel(), SimulHandler.getGiver().getDisplayHandle(false) + "'s simul has resumed! \"tell " + Settings.username + " follow\" to follow the action!");
        }
    }

    public static int determineTickets(String handle) {
        int playedLast = RecordKeeper.getLastLottery(handle);
        double simulsPlayed = RecordKeeper.getLotteries(handle);
        double totalSimuls = RecordKeeper.getTotalSimuls();
        int times = DEFAULT_TICKETS;
        if (playedLast == RecordKeeper.PLAYED) {
            times += PLAYED_LAST_SIMUL_VALUE;
        } else if (playedLast == RecordKeeper.LOST_LOTTERY) {
            times += LOST_LAST_LOTTERY_VALUE;
        } else {
            times += DID_NOT_PLAY_LAST_SIMUL_VALUE;
        }
        times += (int) (SIMULS_PLAYED_MODIFIER * 2 * (.50 - simulsPlayed / totalSimuls));
        return times;
    }
}
