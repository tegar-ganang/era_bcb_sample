package net.sf.colossus.webclient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.colossus.webcommon.GameInfo;
import net.sf.colossus.webcommon.IWebClient;
import net.sf.colossus.webcommon.IWebServer;
import net.sf.colossus.webcommon.User;

/**
 *  This implements the webserver/client communication at client side.
 *  It implements the server interface on client side;
 *  i.e. something server wanted to execute for a client, is read
 *  from the client socket input stream, parsed, and executed
 *  by the (WebClient)SocketThread.
 *
 *  This also contains the methods which are called by the client
 *  (WebClient's GUI) and are sent over the socket to the server
 *  (note that those calls mostly happen in the EDT).
 *
 *  @author Clemens Katzer
 */
public class WebClientSocketThread extends Thread implements IWebServer {

    private static final Logger LOGGER = Logger.getLogger(WebClientSocketThread.class.getName());

    private IWebClient webClient = null;

    private final HashMap<String, GameInfo> gameHash;

    private String hostname = null;

    private final int port;

    private String username = null;

    private String password = null;

    private boolean force = false;

    private String email = null;

    private Socket socket;

    private BufferedReader in;

    private PrintWriter out;

    private boolean stillNeedsRun = true;

    private static final String sep = IWebServer.WebProtocolSeparator;

    private boolean loggedIn = false;

    private AckWaiter ackWaiter;

    private WcstException failedException = null;

    private static int counter = 0;

    private final Charset charset = Charset.forName("UTF-8");

    public WebClientSocketThread(WebClient wcGUI, String hostname, int port, String username, String password, boolean force, String email, String confCode, HashMap<String, GameInfo> gameHash) {
        super("WebClientSocketThread for user " + username + "-" + counter);
        counter++;
        this.webClient = wcGUI;
        this.gameHash = gameHash;
        this.hostname = hostname;
        LOGGER.info("WCST constructor: user " + username + " host " + hostname + " port " + port + " password " + password);
        this.port = port;
        this.username = username;
        this.password = password;
        this.force = force;
        this.email = email;
        this.ackWaiter = new AckWaiter();
        net.sf.colossus.util.InstanceTracker.register(this, "WCST " + username);
        try {
            connect();
            if (confCode != null) {
                confirm(confCode);
            } else if (email != null) {
                register();
            } else {
                login();
            }
        } catch (WcstException e) {
            this.failedException = e;
        }
    }

    public String getOneLine() throws IOException {
        String line = "No line - got exception!";
        try {
            line = this.in.readLine();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Exception during read from socket!", e);
            Thread.dumpStack();
            throw e;
        }
        return line;
    }

    public WcstException getException() {
        return failedException;
    }

    private void connect() throws WcstException {
        String info = null;
        writeLog("About to connect client socket to " + hostname + ":" + port);
        try {
            socket = new Socket(hostname, port);
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), charset)), true);
        } catch (UnknownHostException e) {
            info = "Unknown host: " + e.getMessage() + "\" - wrong address?";
            writeLog(e.toString());
        } catch (ConnectException e) {
            info = "Could not connect: '" + e.getMessage() + "' - wrong address/port, or server not running?";
            writeLog(e.toString());
        } catch (Exception e) {
            info = "Exception during connect: " + e.toString();
            writeLog(e.toString());
        }
        if (info != null) {
            String message = info;
            throw new WcstException(message);
        }
    }

    /**
     * Initial registration attempt
     *
     * @throws WcstException
     */
    private void register() throws WcstException {
        String info = null;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), charset));
            send(RegisterUser + sep + username + sep + password + sep + email);
            String fromServer = null;
            if ((fromServer = getOneLine()) != null) {
                if (fromServer.startsWith("ACK:")) {
                } else {
                    String prefix = "NACK: " + IWebServer.RegisterUser + sep;
                    if (fromServer.startsWith(prefix)) {
                        info = fromServer.substring(prefix.length());
                    } else {
                        info = fromServer;
                    }
                }
            } else {
                info = "NULL reply from server (socket closed??)!";
            }
        } catch (Exception ex) {
            writeLog(ex.toString());
            info = "Creating or reading from buffered reader failed";
        }
        if (info != null) {
            LOGGER.info("register() : info != null, info: " + info);
            String message = info;
            throw new WcstException(message);
        }
    }

    /**
     * Send the confirmation code
     * @throws WcstException
     */
    private void confirm(String confCode) throws WcstException {
        String info = null;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), charset));
            send(ConfirmRegistration + sep + username + sep + confCode);
            String fromServer = null;
            if ((fromServer = getOneLine()) != null) {
                if (fromServer.startsWith("ACK:")) {
                } else {
                    String prefix = "NACK: " + IWebServer.ConfirmRegistration + sep;
                    if (fromServer.startsWith(prefix)) {
                        info = fromServer.substring(prefix.length());
                    } else {
                        info = fromServer;
                    }
                }
            } else {
                info = "NULL reply from server (socket closed??)!";
            }
        } catch (Exception ex) {
            writeLog(ex.toString());
            info = "Creating or reading from buffered reader failed";
        }
        if (info != null) {
            throw new WcstException(info);
        }
    }

    private void login() throws WcstException {
        String info = null;
        boolean duplicateLogin = false;
        try {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), charset));
            int version = webClient.getClientVersion();
            send(Login + sep + username + sep + password + sep + force + sep + version);
            String fromServer = null;
            if ((fromServer = getOneLine()) != null) {
                if (fromServer.startsWith("ACK:")) {
                    loggedIn = true;
                } else if (fromServer.equals("NACK: " + IWebServer.Login + sep + IWebClient.alreadyLoggedIn)) {
                    duplicateLogin = true;
                    info = "Already logged in!";
                } else {
                    String prefix = "NACK: " + IWebServer.Login + sep;
                    if (fromServer.startsWith(prefix)) {
                        info = fromServer.substring(prefix.length());
                    } else {
                        info = fromServer;
                    }
                }
            } else {
                info = "NULL reply from server (socket closed??)!";
            }
        } catch (Exception ex) {
            writeLog(ex.toString());
            info = "Creating or reading from buffered reader failed";
        }
        if (info != null) {
            String message = "Login failed: " + info;
            throw new WcstException(message, duplicateLogin);
        }
    }

    public boolean stillNeedsRun() {
        return stillNeedsRun;
    }

    @Override
    public void run() {
        String threadName = Thread.currentThread().getName();
        stillNeedsRun = false;
        if (this.socket == null) {
            LOGGER.info(threadName + ": socket null, cleanup+return");
            doCleanup();
            return;
        }
        if (this.failedException != null) {
            LOGGER.info(threadName + ": failedException set, cleanup+return");
            doCleanup();
            return;
        }
        LOGGER.info(threadName + ": everything normal, going to run loop!");
        String fromServer = null;
        boolean done = false;
        boolean forcedLogout = false;
        try {
            while (!done && (fromServer = getOneLine()) != null) {
                String[] tokens = fromServer.split(sep, -1);
                String command = tokens[0];
                if (fromServer.startsWith("ACK: ")) {
                    command = tokens[0].substring(5);
                    handleAckNack(command, tokens);
                } else if (fromServer.startsWith("NACK: ")) {
                    command = tokens[0].substring(6);
                    handleAckNack(command, tokens);
                } else if (fromServer.equals(IWebClient.connectionClosed)) {
                    done = true;
                } else if (fromServer.equals(IWebClient.forcedLogout)) {
                    forcedLogout = true;
                    done = true;
                } else if (command.equals(IWebClient.gameInfo)) {
                    GameInfo gi = restoreGameInfo(tokens);
                    webClient.gameInfo(gi);
                } else if (command.equals(IWebClient.userInfo)) {
                    int loggedin = Integer.parseInt(tokens[1]);
                    int enrolled = Integer.parseInt(tokens[2]);
                    int playing = Integer.parseInt(tokens[3]);
                    int dead = Integer.parseInt(tokens[4]);
                    long ago = Long.parseLong(tokens[5]);
                    String text = tokens[6];
                    webClient.userInfo(loggedin, enrolled, playing, dead, ago, text);
                } else if (command.equals(IWebClient.didEnroll)) {
                    String gameId = tokens[1];
                    String user = tokens[2];
                    webClient.didEnroll(gameId, user);
                } else if (command.equals(IWebClient.didUnenroll)) {
                    String gameId = tokens[1];
                    String user = tokens[2];
                    webClient.didUnenroll(gameId, user);
                } else if (command.equals(IWebClient.gameCancelled)) {
                    String gameId = tokens[1];
                    String byUser = tokens[2];
                    webClient.gameCancelled(gameId, byUser);
                } else if (command.equals(IWebClient.gameStartsSoon)) {
                    String gameId = tokens[1];
                    String startUser = tokens[2];
                    confirmCommand(command, gameId, startUser, "nothing");
                    webClient.gameStartsSoon(gameId, startUser);
                } else if (command.equals(IWebClient.gameStartsNow)) {
                    String gameId = tokens[1];
                    int port = Integer.parseInt(tokens[2]);
                    String host = tokens[3];
                    confirmCommand(command, gameId, port + "", host);
                    webClient.gameStartsNow(gameId, port, host);
                } else if (command.equals(IWebClient.chatDeliver)) {
                    String chatId = tokens[1];
                    long when = Long.parseLong(tokens[2]);
                    String sender = tokens[3];
                    String message = tokens[4];
                    boolean resent = Boolean.valueOf(tokens[5]).booleanValue();
                    webClient.chatDeliver(chatId, when, sender, message, resent);
                } else if (command.equals(IWebClient.pingRequest)) {
                    String arg1 = tokens[1];
                    String arg2 = tokens[2];
                    String arg3 = tokens[3];
                    pingResponse(arg1, arg2, arg3);
                } else if (command.equals(IWebClient.generalMessage)) {
                    long when = Long.parseLong(tokens[1]);
                    boolean error = Boolean.valueOf(tokens[2]).booleanValue();
                    String title = tokens[3];
                    String message = tokens[4];
                    webClient.deliverGeneralMessage(when, error, title, message);
                } else if (command.equals(IWebClient.requestAttention)) {
                    long when = Long.parseLong(tokens[1]);
                    String byUser = tokens[2];
                    boolean byAdmin = Boolean.valueOf(tokens[3]).booleanValue();
                    String message = tokens[4];
                    int beepCount = Integer.parseInt(tokens[5]);
                    long beepInterval = Long.parseLong(tokens[6]);
                    boolean windows = Boolean.valueOf(tokens[7]).booleanValue();
                    webClient.requestAttention(when, byUser, byAdmin, message, beepCount, beepInterval, windows);
                } else if (command.equals(IWebClient.grantAdmin)) {
                    webClient.grantAdminStatus();
                } else {
                    if (webClient != null) {
                        if (webClient instanceof WebClient) {
                            ((WebClient) webClient).showAnswer(fromServer);
                        }
                    }
                }
            }
            writeLog("End of SocketClientThread while loop, done = " + done + " readline " + (fromServer == null ? " null " : "'" + fromServer + "'"));
            if (loggedIn) {
                webClient.connectionReset(forcedLogout);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "WebClientSocketThread IOException!");
            webClient.connectionReset(false);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "WebClientSocketThread whatever Exception!", e);
            Thread.dumpStack();
            webClient.connectionReset(false);
        }
        doCleanup();
    }

    private GameInfo restoreGameInfo(String[] tokens) {
        GameInfo gi = GameInfo.fromString(tokens, gameHash, false);
        return gi;
    }

    private void doCleanup() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "WebClientSocketThread close() IOException!", ex);
            }
        }
        socket = null;
        webClient = null;
        ackWaiter = null;
    }

    public void dispose() {
        doCleanup();
    }

    private void send(String s) {
        out.println(s);
    }

    private class AckWaiter {

        String command;

        String result;

        boolean waiting = false;

        public AckWaiter() {
        }

        public boolean isWaiting() {
            return waiting;
        }

        public synchronized String sendAndWait(String command, String args) {
            waiting = true;
            setCommand(command);
            send(command + sep + args);
            String result = waitForAck();
            waiting = false;
            return result;
        }

        public void setCommand(String command) {
            this.command = command;
        }

        public String getCommand() {
            return command;
        }

        public synchronized String waitForAck() {
            try {
                wait();
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, " got exception " + e.toString());
            }
            return result;
        }

        public synchronized void setResult(String result) {
            this.result = result;
            this.notify();
        }
    }

    public void logout() {
        loggedIn = false;
        send(Logout);
    }

    public String changeProperties(String username, String oldPW, String newPW, String email, Boolean isAdminObj) {
        String reason = ackWaiter.sendAndWait(ChangePassword, username + sep + oldPW + sep + newPW + sep + email + sep + isAdminObj);
        return reason;
    }

    private void handleAckNack(String command, String[] tokens) {
        if (ackWaiter != null && ackWaiter.isWaiting()) {
            String cmd = ackWaiter.getCommand();
            if (cmd != null && cmd.equals(command)) {
                ackWaiter.setResult(tokens[1]);
            } else {
                LOGGER.log(Level.WARNING, "Waiting for (N)ACK for command " + cmd + " but " + "got " + command);
            }
        }
    }

    public GameInfo proposeGame(String initiator, String variant, String viewmode, long startAt, int duration, String summary, String expire, boolean unlimitedMulligans, boolean balancedTowers, int min, int target, int max) {
        send(Propose + sep + initiator + sep + variant + sep + viewmode + sep + startAt + sep + duration + sep + summary + sep + expire + sep + unlimitedMulligans + sep + balancedTowers + sep + min + sep + target + sep + max);
        return null;
    }

    public void enrollUserToGame(String gameId, String username) {
        send(Enroll + sep + gameId + sep + username);
    }

    public void unenrollUserFromGame(String gameId, String username) {
        send(Unenroll + sep + gameId + sep + username);
    }

    public void cancelGame(String gameId, String byUser) {
        send(Cancel + sep + gameId + sep + byUser);
    }

    public void startGame(String gameId, User byUser) {
        send(Start + sep + gameId + sep + byUser.getName());
    }

    public void informStartedByPlayer(String gameId) {
        send(StartedByPlayer + sep + gameId);
    }

    public void informLocallyGameOver(String gameId) {
        send(LocallyGameOver + sep + gameId);
    }

    public void startGameOnPlayerHost(String gameId, String hostingPlayer, String playerHost, int port) {
        send(StartAtPlayer + sep + gameId + sep + hostingPlayer + sep + playerHost + sep + port);
    }

    public void chatSubmit(String chatId, String sender, String message) {
        String sending = ChatSubmit + sep + chatId + sep + sender + sep + message;
        send(sending);
    }

    public void pingResponse(String arg1, String arg2, String arg3) {
        send(PingResponse + sep + arg1 + sep + arg2 + sep + arg3);
    }

    public void sleepFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

    public void confirmCommand(String cmd, String arg1, String arg2, String arg3) {
        sleepFor(200);
        long now = new Date().getTime();
        send(ConfirmCommand + sep + now + sep + cmd + sep + arg1 + sep + arg2 + sep + arg3);
    }

    public void requestUserAttention(long when, String sender, boolean isAdmin, String recipient, String message, int beepCount, long beepInterval, boolean windows) {
        String sending = RequestUserAttention + sep + when + sep + sender + sep + isAdmin + sep + recipient + sep + message + sep + beepCount + sep + beepInterval + sep + windows;
        send(sending);
    }

    public void shutdownServer() {
        send(IWebServer.ShutdownServer);
    }

    public void rereadLoginMessage() {
        send(IWebServer.RereadLoginMessage);
    }

    public void dumpInfo() {
        send(IWebServer.DumpInfo);
    }

    public void submitAnyText(String text) {
        if (text.equals("die")) {
            System.exit(1);
        }
        send(text);
    }

    private void writeLog(String s) {
        if (true) {
            LOGGER.log(Level.INFO, s);
        }
    }

    public class WcstException extends Exception {

        boolean failedBecauseAlreadyLoggedIn = false;

        public WcstException(String message, boolean dupl) {
            super(message);
            failedBecauseAlreadyLoggedIn = dupl;
        }

        public WcstException(String message) {
            super(message);
            failedBecauseAlreadyLoggedIn = false;
        }

        public boolean failedBecauseAlreadyLoggedIn() {
            return failedBecauseAlreadyLoggedIn;
        }
    }
}
