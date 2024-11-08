package com.chessclub.simulbot.commands;

import java.io.BufferedReader;
import java.util.ArrayList;
import org.chessworks.common.javatools.io.FileHelper;
import com.chessclub.simulbot.Settings;
import com.chessclub.simulbot.SimulHandler;
import com.chessclub.simulbot.chess.GameSettings;
import com.chessclub.simulbot.chess.Player;
import com.chessclub.simulbot.datagrams.Tell;
import com.chessclub.simulbot.objects.Advertiser;
import com.chessclub.simulbot.objects.Game;
import com.chessclub.simulbot.objects.GameList;
import com.chessclub.simulbot.objects.PlayerList;
import com.chessclub.simulbot.timers.ElapsedTimer;
import com.chessclub.simulbot.timers.FollowTimer;

/**
 * This class stores all functions common to SimulHandler functions
 * 
 */
public class Common {

    private static final String MANAGER_FILE_PATH = "settings/managers.txt";

    public static int isAuthorized(Tell t) {
        if (t.getHandle().equals("clutch") || t.getHandle().equals("zek")) {
            return Settings.ADMIN;
        } else if (t.getTitles().indexOf("*") != -1) {
            return Settings.SUPER;
        } else if (t.getHandle().equals(SimulHandler.getManager())) {
            return Settings.MANAGER;
        } else if (t.getHandle().equals(Settings.username.toLowerCase())) {
            return Settings.ADMIN;
        } else {
            for (String manager : SimulHandler.getManagers()) {
                if (t.getHandle().toLowerCase().equals(manager.toLowerCase())) {
                    return Settings.MANAGER;
                }
            }
        }
        if (t.getHandle().equals(SimulHandler.getGiver().getHandle())) {
            return Settings.SIMUL_GIVER;
        } else {
            return Settings.STANDARD;
        }
    }

    public static String getStatus(int status) {
        if (status == 0) {
            return "No active simul.";
        } else if (status == 1) {
            return "Open for joining.";
        } else if (status == 2) {
            return "Running.";
        } else if (status == 3) {
            return "Simul giver disconnected. Please hold.";
        } else {
            return "Unknown status";
        }
    }

    public static void tellAllPlayers(String message) {
        for (Player p : SimulHandler.getPlayers()) {
            SimulHandler.getHandler().tell(p.getHandle(), message);
        }
    }

    public static boolean inSimul(String handle) {
        handle = handle.toLowerCase();
        for (Player p : SimulHandler.getPlayers()) {
            if (handle.equals(p.getHandle())) {
                return true;
            }
        }
        return false;
    }

    public static boolean inSimulAll(String handle) {
        handle = handle.toLowerCase();
        if (handle.equals(SimulHandler.getGiver().getHandle())) {
            return true;
        }
        for (Player p : SimulHandler.getPlayers()) {
            if (p.getHandle().equals(handle)) {
                return true;
            }
        }
        for (Player p : SimulHandler.getPlayersWithAdjournedGames()) {
            if (p.getHandle().equals(handle)) {
                return true;
            }
        }
        for (Player p : SimulHandler.getFinishedPlayers()) {
            if (p.getHandle().equals(handle)) {
                return true;
            }
        }
        return false;
    }

    public static boolean followingSimul(String handle) {
        handle = handle.toLowerCase();
        for (Player p : SimulHandler.getFollowers()) {
            if (handle.equals(p.getHandle())) {
                return true;
            }
        }
        return false;
    }

    public static String buildMatch(boolean realTime) {
        String match = "";
        match += SimulHandler.getGiver().getHandle() + " ";
        if (realTime) {
            int timeLeft = SimulHandler.getTime() - ElapsedTimer.getMinutes();
            match += timeLeft + " " + SimulHandler.getInc() + " ";
        } else {
            match += SimulHandler.getTime() + " " + SimulHandler.getInc() + " ";
        }
        if (SimulHandler.isRated()) {
            match += "r ";
        } else {
            match += "u ";
        }
        if (SimulHandler.isGiverWhite()) {
            match += "black ";
        } else {
            match += "white ";
        }
        match += SimulHandler.getWild().getMatchString() + " ";
        return match;
    }

    public static void updateResults(Game g) {
        if (g.getWhite().getHandle().equalsIgnoreCase((SimulHandler.getGiver().getHandle()))) {
            if (g.getResult().equals("1-0")) {
                Remove.removeByGame(g);
                SimulHandler.getSimulGames().remove(g);
                SimulHandler.setWins(SimulHandler.getWins() + 1);
                if (!checkForFinish() && SimulHandler.getChannelOut()) {
                    Results.printResultsInProgress(g);
                }
                SimulHandler.getRecorder().record("Finished removing game " + g.getGameNumber());
            } else if (g.getResult().equals("0-1")) {
                Remove.removeByGame(g);
                SimulHandler.getSimulGames().remove(g);
                SimulHandler.setLosses(SimulHandler.getLosses() + 1);
                if (g.isValidWin()) {
                    SimulHandler.getWinners().add(g.getBlack());
                }
                if (!checkForFinish() && SimulHandler.getChannelOut()) {
                    Results.printResultsInProgress(g);
                }
            } else if (g.getResult().equals("1/2-1/2")) {
                Remove.removeByGame(g);
                SimulHandler.getSimulGames().remove(g);
                SimulHandler.setDraws(SimulHandler.getDraws() + 1);
                if (!checkForFinish() && SimulHandler.getChannelOut()) {
                    Results.printResultsInProgress(g);
                }
            } else if (g.getResult().equals("aborted")) {
                Remove.removeByGame(g);
                SimulHandler.getSimulGames().remove(g);
                SimulHandler.setTotal(SimulHandler.getTotal() - 1);
                if (!checkForFinish() && SimulHandler.getChannelOut()) {
                    Results.printResultsInProgress(g);
                }
            } else if (g.getResult().equals("*")) {
                SimulHandler.getRecorder().record("Seeing a adjourned game...");
                SimulHandler.getSimulGames().remove(g);
            } else {
                SimulHandler.getRecorder().record("Error updating results - invalid game result");
            }
        } else {
            if (g.getResult().equals("1-0")) {
                Remove.removeByGame(g);
                SimulHandler.getSimulGames().remove(g);
                SimulHandler.setLosses(SimulHandler.getLosses() + 1);
                if (g.isValidWin()) {
                    SimulHandler.getWinners().add(g.getWhite());
                }
                if (!checkForFinish() && SimulHandler.getChannelOut()) {
                    Results.printResultsInProgress(g);
                }
            } else if (g.getResult().equals("0-1")) {
                Remove.removeByGame(g);
                SimulHandler.getSimulGames().remove(g);
                SimulHandler.setWins(SimulHandler.getWins() + 1);
                if (!checkForFinish() && SimulHandler.getChannelOut()) {
                    Results.printResultsInProgress(g);
                }
            } else if (g.getResult().equals("1/2-1/2")) {
                Remove.removeByGame(g);
                SimulHandler.getSimulGames().remove(g);
                SimulHandler.setDraws(SimulHandler.getDraws() + 1);
                if (!checkForFinish() && SimulHandler.getChannelOut()) {
                    Results.printResultsInProgress(g);
                }
            } else if (g.getResult().equals("aborted")) {
                Remove.removeByGame(g);
                SimulHandler.getSimulGames().remove(g);
                SimulHandler.setTotal(SimulHandler.getTotal() - 1);
                if (!checkForFinish() && SimulHandler.getChannelOut()) {
                    Results.printResultsInProgress(g);
                }
            } else if (g.getResult().equals("*")) {
                SimulHandler.getRecorder().record("Seeing a adjourned game...");
                SimulHandler.getSimulGames().remove(g);
            } else {
                SimulHandler.getRecorder().record("Error updating results - invalid game result");
            }
        }
    }

    public static void resetSimul() {
        SimulHandler.setStatus(Settings.NO_SIMUL);
        SimulHandler.setPlayers(new PlayerList());
        SimulHandler.setPlayersWithAdjournedGames(new PlayerList());
        SimulHandler.setWinners(new PlayerList());
        SimulHandler.setFinishedPlayers(new PlayerList());
        SimulHandler.setFollowers(new PlayerList());
        SimulHandler.setSimulGames(new GameList());
        SimulHandler.getHandler().sendCommand("-notify *");
        Advertiser.stop();
        SimulHandler.setFirstGame(true);
        FollowTimer.cancel();
        ElapsedTimer.cancel();
        resetScores();
        setDefaults();
    }

    public static void setDefaults() {
        SimulHandler.setGiver(Settings.DEFAULT_giver);
        SimulHandler.setRated(Settings.DEFAULT_rated);
        SimulHandler.setGiverWhite(Settings.DEFAULT_white);
        SimulHandler.setAllowGuests(Settings.DEFAULT_allowGuests);
        SimulHandler.setAllowComps(Settings.DEFAULT_allowComps);
        SimulHandler.setChannelOut(Settings.DEFAULT_channelOut);
        SimulHandler.setMessWinners(Settings.DEFAULT_messWinners);
        SimulHandler.setMaxRating(Settings.DEFAULT_maxRating);
        SimulHandler.setMaxPlayers(Settings.DEFAULT_maxPlayers);
        SimulHandler.setTime(Settings.DEFAULT_time);
        SimulHandler.setInc(Settings.DEFAULT_inc);
        SimulHandler.setWild(Settings.DEFAULT_wild);
        SimulHandler.setChannel(Settings.DEFAULT_channel);
        SimulHandler.setManager(Settings.DEFAULT_manager);
        SimulHandler.setNoManager(Settings.DEFAULT_noManager);
        SimulHandler.setRandColor(Settings.DEFAULT_randColor);
        SimulHandler.setAutoAdjud(Settings.DEFAULT_autoAdjud);
        SimulHandler.setLottery(Settings.DEFAULT_lottery);
    }

    public static void resetScores() {
        SimulHandler.setTotal(0);
        SimulHandler.setWins(0);
        SimulHandler.setDraws(0);
        SimulHandler.setLosses(0);
    }

    public static Player findPlayer(String handle) {
        handle = handle.toLowerCase();
        for (Player p : SimulHandler.getPlayers()) {
            if (p.getHandle().equals(handle)) {
                return p;
            }
        }
        return null;
    }

    public static Player findPlayerAll(String handle) {
        handle = handle.toLowerCase();
        if (handle.equals(SimulHandler.getGiver().getHandle())) {
            return SimulHandler.getGiver();
        }
        for (Player p : SimulHandler.getPlayers()) {
            if (p.getHandle().equals(handle)) {
                return p;
            }
        }
        for (Player p : SimulHandler.getPlayersWithAdjournedGames()) {
            if (p.getHandle().equals(handle)) {
                return p;
            }
        }
        for (Player p : SimulHandler.getFinishedPlayers()) {
            if (p.getHandle().equals(handle)) {
                return p;
            }
        }
        return new Player(handle, "");
    }

    public static Player findFollower(String handle) {
        handle = handle.toLowerCase();
        for (Player p : SimulHandler.getFollowers()) {
            if (p.getHandle().equals(handle)) {
                return p;
            }
        }
        return null;
    }

    public static boolean checkForFinish() {
        if (SimulHandler.getSimulGames().size() == 0 && SimulHandler.getPlayersWithAdjournedGames().size() <= 3) {
            Finish.finishSimul();
            return true;
        }
        return false;
    }

    public static void clearAdjournedGames() {
        while (SimulHandler.getPlayersWithAdjournedGames().size() != 0) {
            Player p = SimulHandler.getPlayersWithAdjournedGames().get(0);
            SimulHandler.getHandler().adjudicate(p.getHandle(), SimulHandler.getGiver().getHandle(), "abort");
            SimulHandler.getPlayersWithAdjournedGames().remove(p);
        }
    }

    public static String buildEventString(boolean latejoin) {
        String temp = "" + Settings.EVENT_TYPE.getTypeCode() + " " + Settings.SIMUL_BITFIELD + " ";
        String description = "Play " + SimulHandler.getGiver().getDisplayHandle(false) + " in a simultaneous exhibition, free! " + SimulHandler.getTime() + " " + SimulHandler.getInc() + " time control. " + SimulHandler.getMaxPlayers() + " will play.";
        temp += description;
        if (!latejoin) {
            temp += " | tell " + Settings.username + " join" + " | ";
        } else {
            temp += " | tell " + Settings.username + " latejoin" + " | ";
        }
        temp += " | tell " + Settings.username + " info |";
        return temp;
    }

    public static String buildFollowEventString() {
        String temp = "" + Settings.EVENT_TYPE.getTypeCode() + " " + Settings.SIMUL_BITFIELD + " ";
        String description = "Follow " + SimulHandler.getGiver().getDisplayHandle(false) + "'s simul against " + SimulHandler.getPlayers().size() + " players!";
        temp += description;
        temp += " | | tell " + Settings.username + " follow";
        temp += " | tell " + Settings.username + " help follow |";
        return temp;
    }

    public static GameSettings buildMatchSettings(boolean start, boolean late) {
        int time = SimulHandler.getTime();
        int inc = SimulHandler.getInc();
        if (late) {
            time -= ElapsedTimer.getMinutes();
            if (time < 0) time = 0;
        }
        GameSettings match = new GameSettings();
        boolean giverIsWhite = SimulHandler.isGiverWhite();
        Player giver = SimulHandler.getGiver();
        if (giverIsWhite) {
            match.setWhitePlayer(giver);
        } else {
            match.setBlackPlayer(giver);
        }
        match.setWhiteTime(time);
        match.setWhiteInc(inc);
        match.setBlackTime(time);
        match.setBlackInc(inc);
        match.setRated(SimulHandler.isRated());
        match.setWild(SimulHandler.getWild());
        return match;
    }

    public static double roundDouble(double number) {
        return ((double) Math.round(number * 10)) / 10;
    }

    public static String buildSimulString() {
        String temp = SimulHandler.getGiver().getDisplayHandle(true) + " is giving a " + SimulHandler.getTime() + " " + SimulHandler.getInc() + " simul to " + SimulHandler.getMaxPlayers() + " players";
        if (SimulHandler.getMaxRating() != Settings.NO_MAX_RATING) {
            temp += " under " + SimulHandler.getMaxRating();
        }
        temp += "! \"tell " + Settings.username + " join\" to be in!";
        return temp;
    }

    public static String buildRemainingString(boolean joinString) {
        String temp;
        int numPlayers = SimulHandler.getPlayers().size();
        if (numPlayers == 1) {
            temp = numPlayers + " player has joined (" + SimulHandler.getMaxPlayers() + " will play).";
        } else {
            temp = numPlayers + " players have joined (" + SimulHandler.getMaxPlayers() + " will play).";
        }
        if (joinString) {
            temp += " \"tell " + Settings.username + " join\" to be in!";
        }
        return temp;
    }

    public static void messageWinnersList() {
        String temp;
        if (SimulHandler.getWinners().size() > 0) {
            temp = "The following people won their game against " + SimulHandler.getGiver().getDisplayHandle(false) + ": ";
            for (Player p : SimulHandler.getWinners()) {
                temp += p.getDisplayHandle(false) + " ";
            }
        } else {
            temp = "There were no winners in " + SimulHandler.getGiver().getDisplayHandle(false) + "'s simul.";
        }
        SimulHandler.getHandler().message(Settings.TrophyContact, temp);
    }

    public static boolean playing(String handle) {
        handle = handle.toLowerCase();
        for (Game g : SimulHandler.getNonSimulGames()) {
            if (g.getWhite().getHandle().equals(handle) || g.getBlack().getHandle().equals(handle)) {
                return true;
            }
        }
        return false;
    }

    public static void loadManagers() {
        ArrayList<String> managers = new ArrayList<String>(0);
        BufferedReader reader = null;
        try {
            reader = FileHelper.openExternalTextFile(MANAGER_FILE_PATH);
            String s;
            while ((s = reader.readLine()) != null) {
                managers.add(s);
            }
        } catch (java.io.FileNotFoundException e) {
            SimulHandler.getRecorder().record("Could not the open managers file: " + e);
            e.printStackTrace();
        } catch (java.io.IOException e) {
            SimulHandler.getRecorder().record("Error reading the managers file: " + e);
            e.printStackTrace();
        } finally {
            FileHelper.closeQuietly(reader);
        }
        SimulHandler.setManagers(managers);
    }

    public static void loadMOTD() {
        ArrayList<String> managers = new ArrayList<String>(0);
        BufferedReader reader = null;
        try {
            reader = FileHelper.openExternalTextFile(MANAGER_FILE_PATH);
            String s;
            while ((s = reader.readLine()) != null) {
                managers.add(s);
            }
        } catch (java.io.FileNotFoundException e) {
            SimulHandler.getRecorder().record("Could not the open managers file: " + e);
            e.printStackTrace();
        } catch (java.io.IOException e) {
            SimulHandler.getRecorder().record("Error reading the managers file: " + e);
            e.printStackTrace();
        } finally {
            FileHelper.closeQuietly(reader);
        }
        SimulHandler.setManagers(managers);
    }

    public static String buildFollowString() {
        String temp = "";
        temp += SimulHandler.getGiver().getDisplayHandle(false);
        temp += " is giving a " + SimulHandler.getMaxPlayers() + " board simul! To follow, type \"tell " + Settings.username + " follow\".";
        return temp;
    }
}
