package com.chessclub.simulbot;

import java.util.ArrayList;
import com.chessclub.simulbot.chess.ChessclubWildType;
import com.chessclub.simulbot.chess.Player;
import com.chessclub.simulbot.chess.WildType;
import com.chessclub.simulbot.commands.Access;
import com.chessclub.simulbot.commands.Alarm;
import com.chessclub.simulbot.commands.Announce;
import com.chessclub.simulbot.commands.Assess;
import com.chessclub.simulbot.commands.Clear;
import com.chessclub.simulbot.commands.Close;
import com.chessclub.simulbot.commands.Common;
import com.chessclub.simulbot.commands.Exec;
import com.chessclub.simulbot.commands.Exit;
import com.chessclub.simulbot.commands.Finish;
import com.chessclub.simulbot.commands.Follow;
import com.chessclub.simulbot.commands.Followers;
import com.chessclub.simulbot.commands.Forcejoin;
import com.chessclub.simulbot.commands.Games;
import com.chessclub.simulbot.commands.Games2;
import com.chessclub.simulbot.commands.Help;
import com.chessclub.simulbot.commands.History;
import com.chessclub.simulbot.commands.Info;
import com.chessclub.simulbot.commands.Join;
import com.chessclub.simulbot.commands.Lasttells;
import com.chessclub.simulbot.commands.Latejoin;
import com.chessclub.simulbot.commands.Leave;
import com.chessclub.simulbot.commands.Load;
import com.chessclub.simulbot.commands.Managers;
import com.chessclub.simulbot.commands.Open;
import com.chessclub.simulbot.commands.Players;
import com.chessclub.simulbot.commands.Remove;
import com.chessclub.simulbot.commands.Results;
import com.chessclub.simulbot.commands.Save;
import com.chessclub.simulbot.commands.Set;
import com.chessclub.simulbot.commands.Start;
import com.chessclub.simulbot.commands.Status;
import com.chessclub.simulbot.commands.Stored;
import com.chessclub.simulbot.commands.TellAll;
import com.chessclub.simulbot.commands.Test;
import com.chessclub.simulbot.commands.Unfollow;
import com.chessclub.simulbot.commands.Vars;
import com.chessclub.simulbot.commands.View;
import com.chessclub.simulbot.datagrams.Datagram;
import com.chessclub.simulbot.datagrams.GameFinished;
import com.chessclub.simulbot.datagrams.GameStarted;
import com.chessclub.simulbot.datagrams.Notify;
import com.chessclub.simulbot.datagrams.Tell;
import com.chessclub.simulbot.objects.Feedback;
import com.chessclub.simulbot.objects.FixedLengthQueue;
import com.chessclub.simulbot.objects.Game;
import com.chessclub.simulbot.objects.GameList;
import com.chessclub.simulbot.objects.PlayerList;
import com.chessclub.simulbot.objects.Recorder;

public class SimulHandler {

    private static CommandHandler handler;

    private static int status;

    private static int total;

    private static int wins;

    private static int draws;

    private static int losses;

    private static boolean firstGame;

    private static PlayerList players;

    private static PlayerList playersWithAdjournedGames;

    private static PlayerList finishedPlayers;

    private static PlayerList winners;

    private static PlayerList followers;

    private static GameList simulGames;

    private static GameList nonSimulGames;

    private static ArrayList<String> managers;

    private static ArrayList<String> MOTD;

    private static Player giver;

    private static boolean allowGuests;

    private static boolean allowComps;

    private static boolean rated;

    private static boolean white;

    private static boolean channelOut;

    private static boolean messWinners;

    private static boolean noManager;

    private static boolean randColor;

    private static boolean autoAdjud;

    private static boolean lottery;

    private static int maxRating;

    private static int maxPlayers;

    private static int time;

    private static int inc;

    private static WildType wild;

    private static String channel;

    private static String manager;

    private static Recorder recorder;

    private static FixedLengthQueue lastTells;

    public static final boolean DEBUG_MODE = true;

    public static void setHandler(CommandHandler c) {
        handler = c;
    }

    public static void passTell(Datagram d) {
        Tell t = new Tell(d);
        lastTells.add(t.getHandle() + " tells you: " + t.getMessage(false));
        String command = Library.grabUptoFirstSpace(t.getMessage(true));
        if (Common.followingSimul(t.getHandle())) {
            handler.tell(t.getHandle(), "If you are trying to stop following the simul, tell me \"unfollow\".");
        }
        if (command.equals("join")) {
            Join.join(t.getHandle());
        } else if (command.equals("play")) {
            Join.join(t.getHandle());
        } else if (command.equals("leave")) {
            Leave.leave(t.getHandle());
        } else if (command.equals("remove")) {
            Remove.remove(t);
        } else if (command.equals("status")) {
            Status.status(t.getHandle());
        } else if (command.equals("players")) {
            Players.players(t.getHandle());
        } else if (command.equals("open")) {
            Open.open(t);
        } else if (command.equals("close")) {
            Close.close(t);
        } else if (command.equals("set")) {
            Set.set(t);
        } else if (command.equals("vars")) {
            Vars.vars(t.getHandle());
        } else if (command.equals("help")) {
            Help.help(t);
        } else if (command.equals("forcejoin")) {
            Forcejoin.forcejoin(t);
        } else if (command.equals("tellall")) {
            TellAll.tellAll(t);
        } else if (command.equals("start")) {
            Start.start(t);
        } else if (command.equals("games")) {
            Games.games(t.getHandle());
        } else if (command.equals("results")) {
            Results.results(t.getHandle());
        } else if (command.equals("finish")) {
            Finish.finish(t);
        } else if (command.equals("load")) {
            Load.load(t);
        } else if (command.equals("info")) {
            Info.info(t.getHandle());
        } else if (command.equals("follow")) {
            Follow.follow(t.getHandle());
        } else if (command.equals("test")) {
            Test.test(t);
        } else if (command.equals("feedback")) {
            handleFeedback(t);
        } else if (command.equals("announce")) {
            Announce.announce(t);
        } else if (command.equals("latejoin")) {
            Latejoin.latejoin(t.getHandle());
        } else if (command.equals("followers")) {
            Followers.followers(t.getHandle());
        } else if (command.equals("exec")) {
            Exec.exec(t);
        } else if (command.equals("history")) {
            History.history(t.getHandle());
        } else if (command.equals("access")) {
            Access.access(t);
        } else if (command.equals("exit")) {
            Exit.exit(t);
        } else if (command.equals("unfollow")) {
            Unfollow.unfollow(t.getHandle());
        } else if (command.equals("lasttells")) {
            Lasttells.lasttells(t);
        } else if (command.equals("stored")) {
            Stored.stored(t);
        } else if (command.equals("alarm")) {
            Alarm.alarm(t.getHandle());
        } else if (command.equals("managers")) {
            Managers.managers(t.getHandle());
        } else if (command.equals("save")) {
            Save.save(t);
        } else if (command.equals("assess")) {
            Assess.assess(t.getHandle());
        } else if (command.equals("clear")) {
            Clear.clear(t);
        } else if (command.equals("view")) {
            View.view(t);
        } else if (command.equals("games2")) {
            Games2.games2(t.getHandle());
        } else {
            if (!t.getHandle().equals(Settings.username.toLowerCase()) && !t.getHandle().equals("jeeves")) {
                String msg = "I did not understand your message. Please check to make sure you typed everything correctly, or \"tell " + Settings.username + " help\" for a list of commands.";
                handler.tell(t.getHandle(), msg);
            }
        }
    }

    public static void passStart(Datagram d) {
        GameStarted s = new GameStarted(d);
        if (status == Settings.SIMUL_RUNNING && s.isRealGame()) {
            if (Common.inSimul(s.getWhiteName()) && s.getBlackName().equals(giver.getHandle())) {
                Game g = new Game(s);
                simulGames.add(g);
                recorder.record("Game starting: " + g.getInfo());
                Player p = getPlayersWithAdjournedGames().find(s.getWhiteName());
                if (p != null) {
                    getPlayersWithAdjournedGames().remove(p);
                }
            } else if (Common.inSimul(s.getBlackName()) && s.getWhiteName().equals(giver.getHandle())) {
                Game g = new Game(s);
                simulGames.add(g);
                recorder.record("Game starting: " + g.getInfo());
                Player p = getPlayersWithAdjournedGames().find(s.getBlackName());
                if (p != null) {
                    getPlayersWithAdjournedGames().remove(p);
                }
            } else {
                nonSimulGames.add(new Game(s));
            }
        } else if (s.isRealGame()) {
            nonSimulGames.add(new Game(s));
        }
    }

    public static void passFinish(Datagram d) {
        GameFinished f = new GameFinished(d);
        if (status == Settings.SIMUL_RUNNING) {
            Game g = simulGames.get(f.getGameNumber());
            if (g != null) {
                recorder.record("Game ending: " + g.getInfo());
                g.update(f);
                Common.updateResults(g);
            } else {
                nonSimulGames.remove(nonSimulGames.get(f.getGameNumber()));
            }
        } else {
            nonSimulGames.remove(nonSimulGames.get(f.getGameNumber()));
        }
    }

    public static void passConnected(Datagram d) {
        Notify n = new Notify(d);
        SimulHandler.getRecorder().record("Seeing that " + n.getHandle() + " has connected.");
        if (status == Settings.SIMUL_OPEN) {
            if (n.getHandle().equalsIgnoreCase((giver.getHandle()))) {
                giver.setLoggedOn(true);
            }
        } else if (status == Settings.SIMUL_RUNNING) {
            SimulHandler.getRecorder().record("Checking if " + n.getHandle() + " has a stored simul games.");
            if (Common.inSimul(n.getHandle())) {
                String msg = n.getHandle() + " does have a stored simul game. Trying to resume it...";
                SimulHandler.getRecorder().record(msg);
                Player p = Common.findPlayer(n.getHandle());
                p.setLoggedOn(true);
                playersWithAdjournedGames.remove(p);
                getHandler().qmatch(n.getHandle(), giver.getHandle());
                getHandler().spoof(giver.getHandle(), "accept " + p.getHandle());
            }
        } else if (status == Settings.GIVER_DISCONNECTED) {
            if (n.getHandle().equals(giver.getHandle())) {
                status = Settings.SIMUL_RUNNING;
                Start.startAdjournedSimul();
            } else if (Common.inSimul(n.getHandle())) {
                Common.findPlayer(n.getHandle()).setLoggedOn(true);
                getHandler().qsetTourney(n.getHandle(), Common.buildMatchSettings(false, false), !SimulHandler.isGiverWhite());
            }
        }
    }

    public static void passDisconnected(Datagram d) {
        Notify n = new Notify(d);
        if (status == Settings.SIMUL_OPEN) {
            if (Common.inSimul(n.getHandle())) {
                Remove.remove(n.getHandle());
            }
        } else if (status == Settings.SIMUL_RUNNING) {
            if (Common.inSimul(n.getHandle())) {
                Player p = Common.findPlayer(n.getHandle());
                p.setLoggedOn(false);
                playersWithAdjournedGames.add(p);
            } else if (n.getHandle().equals(giver.getHandle())) {
                status = Settings.GIVER_DISCONNECTED;
                giver.setLoggedOn(false);
                for (Player p : players) {
                    getHandler().qsetTourney(p.getHandle(), Common.buildMatchSettings(false, false), !SimulHandler.isGiverWhite());
                }
                String msg = "The simul giver has disconnected. Please be patient - games will be automatically resumed as quick as possible.";
                Common.tellAllPlayers(msg);
                handler.tell(Settings.EVENTS_CHANNEL, "The simul giver has disconnected.");
            }
        } else if (status == Settings.GIVER_DISCONNECTED) {
            Common.findPlayer(n.getHandle()).setLoggedOn(false);
        }
        if (Common.followingSimul(n.getHandle())) {
            Follow.remove(n.getHandle());
        }
    }

    public static void handleFeedback(Tell t) {
        Feedback.handleFeedback(t);
    }

    public static int getRemainingPlayers() {
        return getMaxPlayers() - getPlayers().size();
    }

    public static CommandHandler getHandler() {
        return handler;
    }

    public static int getStatus() {
        return status;
    }

    public static void setStatus(int newStatus) {
        status = newStatus;
    }

    public static int getTotal() {
        return total;
    }

    public static void setTotal(int newTotal) {
        total = newTotal;
    }

    public static int getWins() {
        return wins;
    }

    public static void setWins(int newWins) {
        wins = newWins;
    }

    public static int getDraws() {
        return draws;
    }

    public static void setDraws(int newDraws) {
        draws = newDraws;
    }

    public static int getLosses() {
        return losses;
    }

    public static void setLosses(int newLosses) {
        losses = newLosses;
    }

    public static PlayerList getPlayers() {
        return players;
    }

    public static void setPlayers(PlayerList newPlayers) {
        players = newPlayers;
    }

    public static int getTotalPlayers() {
        return getPlayers().size() + getWins() + getDraws() + getLosses();
    }

    public static PlayerList getPlayersWithAdjournedGames() {
        return playersWithAdjournedGames;
    }

    public static void setPlayersWithAdjournedGames(PlayerList newPlayersWithAdjournedGames) {
        playersWithAdjournedGames = newPlayersWithAdjournedGames;
    }

    public static PlayerList getFollowers() {
        return followers;
    }

    public static void setFollowers(PlayerList newFollowers) {
        followers = newFollowers;
    }

    public static PlayerList getFinishedPlayers() {
        return finishedPlayers;
    }

    public static void setFinishedPlayers(PlayerList newFinishedPlayers) {
        finishedPlayers = newFinishedPlayers;
    }

    public static PlayerList getWinners() {
        return winners;
    }

    public static void setWinners(PlayerList newWinners) {
        winners = newWinners;
    }

    public static Player getGiver() {
        return giver;
    }

    public static void setGiver(Player newGiver) {
        giver = newGiver;
    }

    public static boolean getAllowComps() {
        return allowComps;
    }

    public static void setAllowComps(boolean newAllowComps) {
        allowComps = newAllowComps;
    }

    public static boolean getAllowGuests() {
        return allowGuests;
    }

    public static void setAllowGuests(boolean newAllowGuests) {
        allowGuests = newAllowGuests;
    }

    public static boolean isRated() {
        return rated;
    }

    public static void setRated(boolean newRated) {
        rated = newRated;
    }

    public static boolean isGiverWhite() {
        return white;
    }

    public static void setGiverWhite(boolean newWhite) {
        white = newWhite;
    }

    public static boolean getChannelOut() {
        return channelOut;
    }

    public static void setChannelOut(boolean newChannelOut) {
        channelOut = newChannelOut;
    }

    public static boolean getMessWinners() {
        return messWinners;
    }

    public static void setMessWinners(boolean newMessWinners) {
        messWinners = newMessWinners;
    }

    public static boolean isNoManager() {
        return noManager;
    }

    public static void setNoManager(boolean newNoManager) {
        noManager = newNoManager;
    }

    public static boolean isRandColor() {
        return randColor;
    }

    public static void setRandColor(boolean newRandColor) {
        randColor = newRandColor;
    }

    public static boolean isAutoAdjud() {
        return autoAdjud;
    }

    public static boolean isLottery() {
        return lottery;
    }

    public static void setLottery(boolean newLottery) {
        lottery = newLottery;
    }

    public static void setAutoAdjud(boolean newAutoAdjud) {
        autoAdjud = newAutoAdjud;
    }

    public static int getMaxRating() {
        return maxRating;
    }

    public static void setMaxRating(int newMaxRating) {
        maxRating = newMaxRating;
    }

    public static int getMaxPlayers() {
        return maxPlayers;
    }

    public static void setMaxPlayers(int newMaxPlayers) {
        maxPlayers = newMaxPlayers;
    }

    public static int getTime() {
        return SimulHandler.time;
    }

    public static void setTime(int time) {
        SimulHandler.time = time;
    }

    public static void setTime(String time) {
        SimulHandler.time = Integer.parseInt(time);
    }

    public static int getInc() {
        return SimulHandler.inc;
    }

    public static void setInc(int inc) {
        SimulHandler.inc = inc;
    }

    public static void setInc(String inc) {
        SimulHandler.inc = Integer.parseInt(inc);
    }

    public static WildType getWild() {
        return SimulHandler.wild;
    }

    public static void setWild(WildType wild) {
        SimulHandler.wild = wild;
    }

    public static void setWild(String wild) {
        SimulHandler.wild = ChessclubWildType.lookup(wild);
    }

    public static String getChannel() {
        return channel;
    }

    public static void setChannel(String newChannel) {
        channel = newChannel;
    }

    public static GameList getSimulGames() {
        return simulGames;
    }

    public static void setSimulGames(GameList newSimulGames) {
        simulGames = newSimulGames;
    }

    public static GameList getNonSimulGames() {
        return nonSimulGames;
    }

    public static void setNonSimulGames(GameList newNonSimulGames) {
        nonSimulGames = newNonSimulGames;
    }

    public static boolean isFirstGame() {
        return firstGame;
    }

    public static void setFirstGame(boolean newFirstGame) {
        firstGame = newFirstGame;
    }

    public static String getManager() {
        return manager;
    }

    public static void setManager(String newManager) {
        manager = newManager;
    }

    public static Recorder getRecorder() {
        return recorder;
    }

    public static void setRecorder(Recorder newRecorder) {
        recorder = newRecorder;
    }

    public static ArrayList<String> getManagers() {
        return managers;
    }

    public static void setManagers(ArrayList<String> newManagers) {
        managers = newManagers;
    }

    public static ArrayList<String> getMOTD() {
        return MOTD;
    }

    public static void setMOTD(ArrayList<String> newMOTD) {
        MOTD = newMOTD;
    }

    public static FixedLengthQueue getLastTells() {
        return lastTells;
    }

    public static void setLastTells(FixedLengthQueue newLastTells) {
        lastTells = newLastTells;
    }
}
