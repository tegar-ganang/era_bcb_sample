package com.chessclub.simulbot.commands;

import com.chessclub.simulbot.Settings;
import com.chessclub.simulbot.SimulHandler;
import com.chessclub.simulbot.chess.Player;
import com.chessclub.simulbot.listeners.AdminListener;
import com.chessclub.simulbot.listeners.GetpxListener;
import com.chessclub.simulbot.timers.LateStartTimer;

public class Latejoin {

    private static final int START_DELAY_TIME = 5;

    private static boolean lateJoinAllowed = false;

    public static void latejoin(String handle) {
        if (SimulHandler.getStatus() != Settings.SIMUL_RUNNING) {
            SimulHandler.getHandler().tell(handle, "You cannot latejoin the simul because the simul is not running.");
        } else if (Common.inSimul(handle)) {
            SimulHandler.getHandler().tell(handle, "You have already joined the simul.");
        } else if (Common.inSimulAll(handle)) {
            SimulHandler.getHandler().tell(handle, "You cannot latejoin the simul because you've already played a simul game.");
        } else if (SimulHandler.getPlayers().size() + SimulHandler.getFinishedPlayers().size() >= SimulHandler.getMaxPlayers()) {
            SimulHandler.getHandler().tell(handle, "You cannot latejoin the simul because it's currently full.");
        } else if (SimulHandler.getGiver().getHandle().equals(handle)) {
            SimulHandler.getHandler().tell(handle, "You cannot latejoin the simul because you are the simul giver.");
        } else if (!lateJoinAllowed) {
            SimulHandler.getHandler().tell(handle, "You cannot latejoin the simul because it is too late to join.");
        } else {
            SimulHandler.getHandler().passGetpxToLatejoin();
            SimulHandler.getHandler().getpx(handle);
        }
    }

    public static void handleGetpx() {
        if (!GetpxListener.isLoggedOn() && GetpxListener.isExisting()) {
            return;
        }
        String handle = GetpxListener.getUser();
        if (GetpxListener.getRating() > SimulHandler.getMaxRating()) {
            SimulHandler.getHandler().tell(handle, "Your rating is too high to play in this simul.");
        } else if (GetpxListener.isComputer() && !SimulHandler.getAllowComps()) {
            SimulHandler.getHandler().tell(handle, "You cannot join the simul because (C)omputer accounts are not allowed in this simul.");
        } else if (!GetpxListener.isExisting() && !SimulHandler.getAllowGuests()) {
            SimulHandler.getHandler().tell(handle, "You cannot join the simul because guest accounts are not allowed in this simul.");
        } else if (Common.playing(handle)) {
            SimulHandler.getHandler().tell(handle, "You cannot join the simul because you are currently playing a game.");
        } else if (GetpxListener.isTournament()) {
            SimulHandler.getHandler().tell(handle, "You cannot join the simul because you are currently playing a tournament.");
        } else if (GetpxListener.isGuest() && !SimulHandler.getAllowGuests()) {
            SimulHandler.getHandler().tell(handle, "You cannot join the simul because you guests are not allowed in this simul.");
        } else if (Common.inSimul(handle)) {
            SimulHandler.getHandler().tell(handle, "You have already joined the simul.");
        } else {
            addLatePlayer(new Player(GetpxListener.getUser(), GetpxListener.getTitle(), GetpxListener.isLoggedOn(), Common.playing(handle), GetpxListener.getRating()));
        }
    }

    public static void addLatePlayer(Player p) {
        String handle = p.getHandle();
        if (Common.inSimul(handle)) {
            SimulHandler.getHandler().tell(handle, "You cannot latejoin the simul because you are already in.");
            return;
        }
        SimulHandler.getPlayers().add(new Player(GetpxListener.getUser(), GetpxListener.getTitle(), GetpxListener.isLoggedOn(), GetpxListener.isPlaying(), GetpxListener.getRating()));
        SimulHandler.setTotal(SimulHandler.getTotal() + 1);
        SimulHandler.getHandler().plusNotify(handle);
        SimulHandler.getHandler().qchanplus(handle, SimulHandler.getChannel());
        if (SimulHandler.getChannelOut()) {
            if (SimulHandler.getPlayers().size() + SimulHandler.getFinishedPlayers().size() == SimulHandler.getMaxPlayers()) {
                SimulHandler.getHandler().qaddevent(Common.buildFollowEventString());
            }
        }
        SimulHandler.getHandler().tell(handle, "You have been added to the simul. Your game will start in " + START_DELAY_TIME + " seconds.");
        SimulHandler.getRecorder().record(handle + " has latejoined the simul.");
        LateStartTimer.start(handle, START_DELAY_TIME);
    }

    public static void startMatch(String handle) {
        SimulHandler.getHandler().qsetTourney(handle, Common.buildMatchSettings(true, true), !SimulHandler.isGiverWhite());
        SimulHandler.getHandler().qmatch(handle, SimulHandler.getGiver().getHandle());
        SimulHandler.getHandler().spoof(SimulHandler.getGiver().getHandle(), "accept " + handle);
        if (!AdminListener.isAdmin()) {
            SimulHandler.getHandler().qsuggest(SimulHandler.getGiver().getHandle(), "accept " + handle);
        }
    }

    public static boolean isLateJoinAllowed() {
        return lateJoinAllowed;
    }

    public static void setLateJoinAllowed(boolean newAllowLatejoin) {
        lateJoinAllowed = newAllowLatejoin;
    }
}
