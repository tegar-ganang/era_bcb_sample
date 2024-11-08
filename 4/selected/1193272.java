package com.chessclub.simulbot;

import org.chessworks.common.javatools.StringHelper;
import com.chessclub.simulbot.chess.ChessclubEventType;
import com.chessclub.simulbot.chess.GameSettings;
import com.chessclub.simulbot.chess.Player;
import com.chessclub.simulbot.commands.Common;
import com.chessclub.simulbot.commands.Forcejoin;
import com.chessclub.simulbot.commands.Games2;
import com.chessclub.simulbot.commands.Join;
import com.chessclub.simulbot.commands.Latejoin;
import com.chessclub.simulbot.commands.Open;
import com.chessclub.simulbot.commands.Set;
import com.chessclub.simulbot.datagrams.Datagram;
import com.chessclub.simulbot.gui.SBFrame;
import com.chessclub.simulbot.listeners.AdminListener;
import com.chessclub.simulbot.listeners.ECOListener;
import com.chessclub.simulbot.listeners.GamesListener;
import com.chessclub.simulbot.listeners.GetpxListener;
import com.chessclub.simulbot.listeners.MultipleLogonListener;
import com.chessclub.simulbot.listeners.NukeListener;
import com.chessclub.simulbot.objects.FixedLengthQueue;
import com.chessclub.simulbot.objects.GameList;
import com.chessclub.simulbot.objects.IntQueue;
import com.chessclub.simulbot.objects.Recorder;

public class CommandHandler {

    private final IntQueue queue;

    private ICCConnection connection;

    private SBFrame gui;

    private boolean relay;

    private boolean noShout = false;

    public CommandHandler() {
        SimulHandler.setHandler(this);
        SimulHandler.setRecorder(new Recorder(true, Library.getDate() + " record"));
        SimulHandler.setNonSimulGames(new GameList());
        SimulHandler.setLastTells(new FixedLengthQueue(Settings.MAX_TELLS));
        Common.loadManagers();
        GetpxListener.reset();
        queue = new IntQueue();
        relay = false;
    }

    /**
	 * If set to true, the bot will not sshout simuls.  This is useful when testing the bot.
	 */
    public void setNoShout(boolean noShout) {
        this.noShout = noShout;
    }

    public void setConnection(ICCConnection c) {
        connection = c;
    }

    public ICCConnection getConnection() {
        return connection;
    }

    public void setGUI(SBFrame s) {
        gui = s;
    }

    public void pass(String message, boolean isDG) {
        if (isDG) {
            Datagram d = new Datagram(message);
            decideWhetherToPrintDG(d);
            if (d.getNumber() == DatagramSetup.DG_PERSONAL_TELL) {
                SimulHandler.passTell(d);
            } else if (d.getNumber() == DatagramSetup.DG_GAME_STARTED) {
                SimulHandler.passStart(d);
            } else if (d.getNumber() == DatagramSetup.DG_GAME_RESULT) {
                SimulHandler.passFinish(d);
            } else if (d.getNumber() == DatagramSetup.DG_NOTIFY_ARRIVED) {
                SimulHandler.passConnected(d);
            } else if (d.getNumber() == DatagramSetup.DG_NOTIFY_LEFT) {
                SimulHandler.passDisconnected(d);
            } else if (d.getNumber() == DatagramSetup.DG_WHO_AM_I) {
                AdminListener.passWhoAmI(d);
            } else if (d.getNumber() == DatagramSetup.DG_DUMMY_RESPONSE) {
            }
        } else {
            decideWhetherToPrintNonDG(message);
            boolean sawGetpx = GetpxListener.passLine(message);
            if (sawGetpx) {
                int next = queue.dequeue();
                if (next == 1) {
                    Join.handleGetpx();
                } else if (next == 2) {
                    Forcejoin.handleGetpx();
                } else if (next == 3) {
                    Open.handleGetpx();
                } else if (next == 4) {
                    Set.handleGetpxForGiver();
                } else if (next == 5) {
                    Set.handleGetpxForManager();
                } else if (next == 6) {
                    Latejoin.handleGetpx();
                }
                GetpxListener.reset();
            }
            AdminListener.passLine(message);
            boolean sawMultipleLogon = MultipleLogonListener.passLine(message);
            if (sawMultipleLogon) {
                SimulHandler.getRecorder().record("Seeing a multiple logon and exiting.");
                System.exit(0);
            }
            boolean sawNuke = NukeListener.passLine(message);
            if (sawNuke) {
                SimulHandler.getRecorder().record("Seeing I was kicked out by Administrators.  Exiting.");
                System.exit(0);
            }
            boolean sawECO = ECOListener.passLine(message);
            if (sawECO) {
                Games2.handleEco(ECOListener.getECO());
            }
            boolean sawGame = GamesListener.passLine(message);
            if (sawGame) {
                Games2.handleGames(GamesListener.getToMove(), GamesListener.getMoveNumber());
            }
        }
    }

    public void decideWhetherToPrintDG(Datagram d) {
        if (!(d.getNumber() == DatagramSetup.DG_GAME_STARTED) && !(d.getNumber() == DatagramSetup.DG_GAME_RESULT) && !(d.getNumber() == DatagramSetup.DG_PLAYERS_IN_MY_GAME)) {
            perform(d.toString());
        }
    }

    public void decideWhetherToPrintNonDG(String message) {
        if (message.length() > 5 && message.indexOf("qtell") != -1) {
            return;
        }
        perform(message);
    }

    public void perform(String message) {
        gui.print(message);
        if (relay) {
            qtell("clutch", " " + message);
        }
    }

    /**
	 * Sends a command to the server through the ICC Connection. The command is
	 * prefixed with "multi " to avoid problems with aliases.
	 */
    public void sendCommand(String command) {
        command = addMulti(command);
        connection.send(command);
    }

    /**
	 * Sends the text as is to the server through the ICC Connection.
	 */
    public void sendRaw(String message) {
        connection.send(message);
    }

    public boolean isRelay() {
        return relay;
    }

    public void setRelay(boolean newRelay) {
        relay = newRelay;
    }

    public void tell(String handle, String message) {
        sendCommand("tell " + handle + " " + message);
    }

    public void getpx(String handle) {
        sendCommand("getpx " + handle + " " + Settings.EVENT_TYPE.getTypeCode());
    }

    public void plusNotify(String handle) {
        sendCommand("+notify " + handle);
    }

    public void minusNotify(String handle) {
        sendCommand("-notify " + handle);
    }

    public void plusGnotify(String handle) {
        sendCommand("+gnotify " + handle);
    }

    public void minusGnotify(String handle) {
        sendCommand("-gnotify " + handle);
    }

    public void turnSettingOn(int setting) {
        sendCommand("set-2 " + setting + " 1");
    }

    public void turnSettingOff(int setting) {
        sendCommand("set-2 " + setting + " 0");
    }

    public void passGetpxToJoin() {
        queue.enqueue(1);
    }

    public void passGetpxToForcejoin() {
        queue.enqueue(2);
    }

    public void passGetpxToOpen() {
        queue.enqueue(3);
    }

    public void passGetpxToSet(int code) {
        queue.enqueue(4 + code);
    }

    public void passGetpxToLatejoin() {
        queue.enqueue(6);
    }

    public void spoof(String handle, String command) {
        spoof(handle, command, null);
    }

    public void spoof(String handle, String command, String info) {
        if (AdminListener.isAdmin()) {
            command = addMulti(command);
            sendCommand("spoof " + handle + " " + command);
        } else {
            qsuggest(handle, command, info);
        }
    }

    public void qsuggest(String player, String command) {
        qsuggest(player, command, null);
    }

    public void qsuggest(String player, String command, String info) {
        if (AdminListener.isAdmin() || AdminListener.isTD()) {
            command = addMulti(command);
            String cmd = "qsuggest " + player + " " + command;
            if ((info != null) && !info.isEmpty()) {
                cmd += "#" + info;
            }
            sendCommand(cmd);
        } else {
            if (info == null) {
                info = "Suggest";
            }
            tell(player, info + ": \"" + command + "\"");
        }
    }

    public void sshout(String message) {
        if (!noShout) {
            sendCommand("sshout " + message);
        }
    }

    public void adjudicate(String handle1, String handle2, String result) {
        sendCommand("adjudicate " + handle1 + " " + handle2 + " " + result);
    }

    public void qmatch(String handle, String matchString) {
        if (AdminListener.isTD()) {
            sendCommand("qmatch " + handle + " " + matchString);
        } else if (AdminListener.isAdmin()) {
            spoof(handle, "match " + matchString);
        }
    }

    public void qaddevent(String eventString) {
        sendCommand("qaddevent " + eventString);
    }

    public void qremoveevent(ChessclubEventType event) {
        sendCommand("qremoveevent " + event.getTypeCode());
    }

    public void qremoveevent(int eventNumber) {
        sendCommand("qremoveevent " + eventNumber);
    }

    public void qtell(String handle, String message) {
        if (AdminListener.isAdmin()) {
            sendCommand("qtell " + handle + " " + message);
        } else {
            String[] lines = message.split("\n");
            for (String s : lines) {
                tell(handle, s);
            }
        }
    }

    public void qtell(String message) {
        qtell("" + SimulHandler.getChannel(), message);
    }

    public void qset(String handle, String var, String value) {
        sendCommand("qset " + handle + "  " + var + " " + value);
    }

    public void qsetTourney(String handle, GameSettings match) {
        if (match == null) {
            qclearTourney(handle);
        } else {
            String value = buildTourneyString(handle, match);
            qset(handle, "tourney", value);
        }
    }

    public void qsetTourney(String handle, GameSettings match, boolean playerIsWhite) {
        if (match == null) {
            qclearTourney(handle);
        } else {
            String value = buildTourneyString(match, playerIsWhite);
            qset(handle, "tourney", value);
        }
    }

    public void qclearTourney(String handle) {
        qset(handle, "tourney", "0");
    }

    public static String buildTourneyString(String handle, GameSettings match) {
        if (handle == null) throw new NullPointerException("handle is null");
        boolean playerIsWhite;
        Player player = match.getWhitePlayer();
        Player opponent = match.getBlackPlayer();
        if (Player.equals(player, handle)) {
            playerIsWhite = true;
        } else if (Player.equals(opponent, handle)) {
            playerIsWhite = false;
        } else if (player == null && opponent != null) {
            playerIsWhite = true;
        } else if (opponent == null && player != null) {
            playerIsWhite = false;
        } else {
            throw new IllegalArgumentException("given handle matches neither player nor opponent");
        }
        return buildTourneyString(match, playerIsWhite);
    }

    public static String buildTourneyString(GameSettings match, boolean playerIsWhite) {
        int playerTime, playerInc, opponentTime, opponentInc;
        String color;
        Player opponent;
        Player partner;
        if (playerIsWhite) {
            playerTime = match.getWhiteTime();
            playerInc = match.getWhiteInc();
            opponentTime = match.getBlackTime();
            opponentInc = match.getBlackInc();
            color = "white";
            opponent = match.getBlackPlayer();
            partner = match.getWhitesPartner();
        } else {
            playerTime = match.getBlackTime();
            playerInc = match.getBlackInc();
            opponentTime = match.getWhiteTime();
            opponentInc = match.getWhiteInc();
            color = "black";
            opponent = match.getWhitePlayer();
            partner = match.getBlacksPartner();
        }
        int event = match.getEventType().getTypeCode();
        String partnerName = Player.handle(partner, "*");
        String opponentName = Player.handle(opponent, "*");
        String rated = match.isRated() ? "r" : "u";
        String wild = match.getWild().getMatchString();
        String round = match.getRound();
        String gameid = match.getLoadGameId();
        if (gameid == null) gameid = "0";
        StringBuffer buf = new StringBuffer(100);
        buf.append(event);
        buf.append(" ");
        buf.append(partnerName);
        buf.append(" ");
        buf.append(opponentName);
        buf.append(" ");
        buf.append(playerTime);
        buf.append(" ");
        buf.append(playerInc);
        buf.append(" ");
        buf.append(opponentTime);
        buf.append(" ");
        buf.append(opponentInc);
        buf.append(" ");
        buf.append(rated);
        buf.append(wild);
        buf.append(color);
        buf.append(" ");
        buf.append(round);
        buf.append(" ");
        buf.append(gameid);
        String result = buf.toString();
        return result;
    }

    public void qclear(String event) {
        sendCommand("qclear " + event);
    }

    public void qchanplus(String player, String channel) {
        if (AdminListener.isTD()) {
            sendCommand("qchanplus " + player + " " + channel);
        } else if (AdminListener.isAdmin()) {
            spoof(player, "+channel ", channel);
        } else {
            tell(player, "Please join channel " + channel);
        }
    }

    public void qchanminus(String player, String channel) {
        if (AdminListener.isAdmin()) {
            sendCommand("qchanminus " + player + " " + channel);
        } else {
            tell(player, "Please leave channel " + channel);
        }
    }

    public void observe(String handle) {
        sendCommand("observe " + handle);
    }

    public void unobserve(String handle) {
        sendCommand("unobserve " + handle);
    }

    public void message(String handle, String message) {
        sendCommand("message " + handle + " " + message);
    }

    public void loudshout(String message) {
        if (!noShout) {
            sendCommand("loudshout " + message);
        }
    }

    public void atell(String handle, String message) {
        if (AdminListener.isAdmin()) {
            sendCommand("atell " + handle + " " + message);
        } else {
            tell(handle, message);
        }
    }

    public void games(String gameNumber) {
        sendCommand("games " + gameNumber);
    }

    public void eco(String gameNumber) {
        sendCommand("eco " + gameNumber);
    }

    private static String addMulti(String command) {
        command = command.trim();
        command = StringHelper.guaranteePrefix(command, "multi ");
        return command;
    }
}
