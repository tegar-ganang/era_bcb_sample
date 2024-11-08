package com.chessclub.simulbot.commands;

import java.io.BufferedReader;
import java.io.FileReader;
import com.chessclub.simulbot.Settings;
import com.chessclub.simulbot.SimulHandler;
import com.chessclub.simulbot.chess.Player;
import com.chessclub.simulbot.listeners.GetpxListener;
import com.chessclub.simulbot.objects.Qtell;

public class Join {

    private static final String MOTD_PATH = "settings/motd.txt";

    private static final int MOTD_LINE_SIZE = 72;

    public static void join(String handle) {
        if (SimulHandler.getStatus() == Settings.NO_SIMUL) {
            SimulHandler.getHandler().tell(handle, "You cannot join because there is no simul at the moment.");
        } else if (SimulHandler.getStatus() == Settings.SIMUL_RUNNING) {
            Latejoin.latejoin(handle);
        } else if (SimulHandler.getStatus() == Settings.GIVER_DISCONNECTED) {
            SimulHandler.getHandler().tell(handle, "You cannot join because the simul giver is offline at the moment.");
        } else if (Common.inSimul(handle)) {
            SimulHandler.getHandler().tell(handle, "You have already joined the simul.");
        } else if (SimulHandler.getGiver().getHandle().equals(handle)) {
            SimulHandler.getHandler().tell(handle, "You cannot join the simul because you are the simul giver.");
        } else if (!SimulHandler.isLottery() && SimulHandler.getPlayers().size() >= SimulHandler.getMaxPlayers()) {
            SimulHandler.getHandler().tell(handle, "You cannjot join the simul because it's currently full.");
        } else {
            SimulHandler.getHandler().passGetpxToJoin();
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
            addPlayer(new Player(GetpxListener.getUser(), GetpxListener.getTitle(), GetpxListener.isLoggedOn(), Common.playing(handle), GetpxListener.getRating()));
        }
    }

    public static void addPlayer(Player p) {
        SimulHandler.getPlayers().add(p);
        if (Common.followingSimul(p.getHandle())) {
            SimulHandler.getFollowers().remove(Common.findFollower(p.getHandle()));
        }
        SimulHandler.getHandler().qsetTourney(p.getHandle(), Common.buildMatchSettings(false, false), !SimulHandler.isGiverWhite());
        SimulHandler.getHandler().qchanplus(p.getHandle(), SimulHandler.getChannel());
        SimulHandler.getHandler().tell(p.getHandle(), "You have joined the simul.");
        SimulHandler.getRecorder().record(p.getHandle() + " has joined the simul");
        Qtell qtell = new Qtell(p.getHandle());
        String line;
        qtell.addLine("");
        qtell.addLine(createCopies(MOTD_LINE_SIZE, "*"));
        try {
            BufferedReader reader = new BufferedReader(new FileReader(MOTD_PATH));
            while ((line = reader.readLine()) != null) {
                String tempSpacer = createCopies(MOTD_LINE_SIZE - line.length() - 4, " ");
                qtell.addLine("* " + line + tempSpacer + " *");
            }
        } catch (Exception e) {
            SimulHandler.getRecorder().record("Error reading MOTD: " + e);
        }
        qtell.addLine(createCopies(MOTD_LINE_SIZE, "*"));
        qtell.send();
        int remainingPlayers = SimulHandler.getMaxPlayers() - SimulHandler.getPlayers().size();
        if (SimulHandler.getChannelOut()) {
            qtell = new Qtell(SimulHandler.getChannel());
            qtell.addLine(p.getDisplayHandle(false) + "(" + p.getRating() + ") has joined " + SimulHandler.getGiver().getDisplayHandle(false) + "'s simul - " + Common.buildRemainingString(false));
            if (!SimulHandler.isLottery() && remainingPlayers == 0) {
                qtell.addLine("Simul is now full.");
                Open.disableAdvertising();
            } else if (!SimulHandler.isLottery() && remainingPlayers == 1) {
                Open.enableAdvertising();
            }
            qtell.send();
        }
        SimulHandler.setTotal(SimulHandler.getTotal() + 1);
        SimulHandler.getHandler().plusNotify(p.getHandle());
    }

    public static String createCopies(int numCopies, String s) {
        String copy = "";
        for (int i = 0; i < numCopies; ++i) {
            copy += s;
        }
        return copy;
    }
}
