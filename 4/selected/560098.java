package hailmary.network.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.TimerTask;
import org.schwering.irc.lib.IRCParser;
import org.schwering.irc.lib.IRCUtil;
import java.util.Date;

/**
 * A connection thread, handling a single connection to the IRC server.
 * This class handles most of the IRC commands, and only uses the
 * <code>Server</code> class for tasks that require coordination between
 * multiple connections.
 * @author Corvass
 * @see Server
 */
public class ConnectionThread extends Thread {

    /** The default ping interval, which is 1 minute. */
    public static final long PING_DELAY = 60000;

    /** The default timeout delay, which is 2 minutes. */
    public static final long TIMEOUT_DELAY = 120000;

    /** The default timeout delay for joining the channel,
      which is 2 minutes. */
    public static final long JOIN_TIMEOUT_DELAY = 120000;

    /** The connection socket and its reader and writer */
    private Socket socket;

    private BufferedReader in;

    private PrintWriter out;

    private int connectionStatus;

    /** The server this connection belongs to */
    private Server server;

    /** User information */
    private String nick;

    private String user;

    private String host;

    private String realName;

    /** Timer variables */
    private TimerTask pingTask;

    private TimerTask timeoutTask;

    private TimerTask joinTimeoutTask;

    private long pingDelay;

    private long timeoutDelay;

    private long joinTimeoutDelay;

    /**
   * Constructs a connection thread using the default delays.
   * Automatically starts listening for incoming messages.
   * @param socket this connection's socket
   * @param server the server this connection belongs to
   */
    public ConnectionThread(Socket socket, Server server) {
        this(socket, server, PING_DELAY, TIMEOUT_DELAY, JOIN_TIMEOUT_DELAY);
    }

    /**
   * Constructs a connection thread. Automatically starts listening
   * for incoming messages.
   * @param socket this connection's socket
   * @param server the server this connection belongs to
   * @param pingDelay the ping interval
   * @param timeoutDelay the timeout delay
   * @param joinTimeoutDelay the timeout delay for joining a channel
   */
    public ConnectionThread(Socket socket, Server server, long pingDelay, long timeoutDelay, long joinTimeoutDelay) {
        this.socket = socket;
        this.server = server;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
        } catch (IOException e) {
            disconnect();
            return;
        }
        this.pingDelay = pingDelay;
        this.timeoutDelay = timeoutDelay;
        this.joinTimeoutDelay = joinTimeoutDelay;
        pingTask = new PingTask();
        timeoutTask = null;
        joinTimeoutTask = new JoinTimeoutTask();
        connectionStatus = 0;
        host = socket.getInetAddress().getCanonicalHostName();
        start();
    }

    /**
   * Returns this connection's nickname.
   * @return this connection's nickname
   */
    public String getNick() {
        return nick;
    }

    /**
   * Disconnects this connection, and cleans this object up.
   */
    public void disconnect() {
        connectionStatus = -1;
        sendPrefix("ERROR :Disconnected");
        try {
            socket.close();
        } catch (IOException e) {
        }
        server.unregisterConnection(this);
        pingTask.cancel();
        joinTimeoutTask.cancel();
        if (timeoutTask != null) {
            timeoutTask.cancel();
            timeoutTask = null;
        }
    }

    /**
   * Disconnects this connection because of an error.
   * The error is broadcasted as a <code>QUIT</code> message
   * before disconnecting.
   * @param error the error message
   */
    public void disconnect(String error) {
        if (connectionStatus > -1) {
            connectionStatus = -1;
            if (connectionStatus >= 3) server.send(null, prefix() + "QUIT :" + error);
            disconnect();
        }
    }

    /**
   * Returns the prefix to use for messages originating from
   * this connection.
   * @return the prefix
   */
    public String prefix() {
        return ":" + nick + "!" + user + "@" + host + " ";
    }

    /**
   * Sends a line to the client.
   * @param line the line to send
   */
    public void send(String line) {
        System.err.println("[" + new Date() + " OUT " + nick + "] " + line);
        out.write(line + "\r\n");
        out.flush();
    }

    /**
   * Sends a line to the client with a prefix indicating the
   * message originated from the server.
   * @param line the line to send
   */
    public void sendPrefix(String line) {
        send(":" + server.getServerName() + " " + line);
    }

    /**
   * Receives a line from the other side of the connection.
   * @param line the received line
   */
    protected void receive(String line) {
        System.err.println("[" + new Date() + " IN " + nick + "] " + line);
        IRCParser p = new IRCParser(line, true);
        String command = p.getCommand();
        if (command.equalsIgnoreCase("user")) receiveUser(p); else if (command.equalsIgnoreCase("nick")) receiveNick(p); else if (command.equalsIgnoreCase("join")) receiveJoin(p); else if (command.equalsIgnoreCase("part")) receivePart(p); else if (command.equalsIgnoreCase("quit")) receiveQuit(p); else if (command.equalsIgnoreCase("kick")) receiveKick(p); else if (command.equalsIgnoreCase("topic")) receiveTopic(p); else if (command.equalsIgnoreCase("privmsg")) receivePrivmsg(p); else if (command.equalsIgnoreCase("pong")) receivePong(p);
    }

    /**
   * Sends a welcome message to the client,
   * and schedules join timeout and ping.
   */
    protected void welcome() {
        sendPrefix(Integer.toString(IRCUtil.RPL_WELCOME) + " " + nick + " :Welcome to the Internet Relay Network " + nick + "!" + user + "@" + host);
        sendPrefix(Integer.toString(IRCUtil.RPL_YOURHOST) + " " + nick + " :Your host is " + server.getServerName() + ", running version " + Server.VERSION);
        sendPrefix(Integer.toString(IRCUtil.RPL_CREATED) + " " + nick + " :This server was created " + server.getCreationDate());
        sendPrefix(Integer.toString(IRCUtil.RPL_MYINFO) + " " + nick + " :" + server.getServerName() + " " + Server.VERSION);
        server.getTimer().scheduleAtFixedRate(pingTask, 0, pingDelay);
        server.getTimer().schedule(joinTimeoutTask, joinTimeoutDelay);
    }

    /**
   * Receives a <code>USER</code> command from the client.
   * @param p the IRC parser associated with the command
   */
    protected void receiveUser(IRCParser p) {
        if (user != null) {
            sendPrefix(Integer.toString(IRCUtil.ERR_ALREADYREGISTRED) + " :Unauthorized command (already registered)");
        } else {
            if (p.getParameterCount() < 4) {
                sendPrefix(Integer.toString(IRCUtil.ERR_NEEDMOREPARAMS) + " :Not enough parameters");
            } else {
                user = p.getParameter(1);
                realName = p.getTrailing();
                connectionStatus++;
                if (connectionStatus == 2) welcome();
            }
        }
    }

    /**
   * Receives a <code>NICK</code> command from the client.
   * @param p the IRC parser associated with the command
   */
    protected void receiveNick(IRCParser p) {
        if (p.getParameterCount() < 1) {
            sendPrefix(Integer.toString(IRCUtil.ERR_NONICKNAMEGIVEN) + " :No nickname given");
        } else {
            String nick = p.getParameter(1);
            if (server.nickExists(nick)) {
                sendPrefix(Integer.toString(IRCUtil.ERR_NICKNAMEINUSE) + " " + nick + " :Nickname is already in use");
            } else if (!server.nickIsAcceptable(nick)) {
                sendPrefix(Integer.toString(IRCUtil.ERR_ERRONEUSNICKNAME) + " " + nick + " :Erroneous nickname");
            } else {
                if (this.nick == null) {
                    server.registerConnection(this, nick);
                    connectionStatus++;
                    this.nick = nick;
                    if (connectionStatus == 2) welcome();
                } else {
                    server.nickChange(this, nick);
                    if (connectionStatus >= 3) server.send(null, prefix() + p.getLine());
                    this.nick = nick;
                }
            }
        }
    }

    /**
   * Receives a <code>JOIN</code> command from the client.
   * @param p the IRC parser associated with the command
   */
    protected void receiveJoin(IRCParser p) {
        if (connectionStatus <= 1) {
            sendPrefix(Integer.toString(IRCUtil.ERR_NOTREGISTERED) + " :You have not registered");
        } else if (p.getParameterCount() < 1) {
            sendPrefix(Integer.toString(IRCUtil.ERR_NEEDMOREPARAMS) + " :Not enough parameters");
        } else if (connectionStatus >= 3) {
            String channel = p.getParameter(1);
            if (channel.equals("0")) {
                server.part(this.nick);
                server.send(null, prefix() + "PART " + server.getChannel());
                disconnect();
            } else if (!channel.equalsIgnoreCase(server.getChannel())) {
                sendPrefix(Integer.toString(IRCUtil.ERR_TOOMANYCHANNELS) + " " + channel + " :You have joined too many channels");
            }
        } else {
            String channel = p.getParameter(1);
            if (!channel.equalsIgnoreCase(server.getChannel())) {
                sendPrefix(Integer.toString(IRCUtil.ERR_NOSUCHCHANNEL) + " " + channel + " :No such channel");
                disconnect("Incorrect channel");
            } else {
                joinTimeoutTask.cancel();
                server.join(this.nick);
                server.send(null, prefix() + p.getLine());
                sendPrefix(Integer.toString(IRCUtil.RPL_NAMREPLY) + " " + nick + " = " + channel + " :" + server.getNickList());
                sendPrefix(Integer.toString(IRCUtil.RPL_ENDOFNAMES) + " " + nick + " " + channel + " :End of NAMES list");
                if (!server.getTopic().equals("")) {
                    sendPrefix(Integer.toString(IRCUtil.RPL_TOPIC) + " " + nick + " " + channel + " :" + server.getTopic());
                    sendPrefix(Integer.toString(IRCUtil.RPL_TOPICINFO) + " " + nick + " " + channel + " " + server.getTopicNick() + " :" + Long.toString(server.getTopicDate().getTime()));
                }
                connectionStatus = 3;
            }
        }
    }

    /**
   * Receives a <code>PART</code> command from the client.
   * @param p the IRC parser associated with the command
   */
    protected void receivePart(IRCParser p) {
        if (connectionStatus <= 1) {
            sendPrefix(Integer.toString(IRCUtil.ERR_NOTREGISTERED) + " :You have not registered");
        } else if (p.getParameterCount() < 1) {
            sendPrefix(Integer.toString(IRCUtil.ERR_NEEDMOREPARAMS) + " :Not enough parameters");
        } else {
            String channel = p.getParameter(1);
            if (!channel.equalsIgnoreCase(server.getChannel())) {
                sendPrefix(Integer.toString(IRCUtil.ERR_NOSUCHCHANNEL) + " " + channel + " :No such channel");
            } else if (connectionStatus == 2) {
                sendPrefix(Integer.toString(IRCUtil.ERR_NOTONCHANNEL) + " " + channel + " :You're not on that channel");
            } else {
                server.part(this.nick);
                server.send(null, prefix() + p.getLine());
                disconnect();
            }
        }
    }

    /**
   * Receives a <code>QUIT</code> command from the client.
   * @param p the IRC parser associated with the command
   */
    protected void receiveQuit(IRCParser p) {
        if (connectionStatus <= 1) {
            sendPrefix(Integer.toString(IRCUtil.ERR_NOTREGISTERED) + " :You have not registered");
        } else {
            if (connectionStatus >= 3) server.send(null, prefix() + p.getLine());
            disconnect();
        }
    }

    /**
   * Receives a <code>KICK</code> command from the client.
   * @param p the IRC parser associated with the command
   */
    protected void receiveKick(IRCParser p) {
        if (connectionStatus <= 1) {
            sendPrefix(Integer.toString(IRCUtil.ERR_NOTREGISTERED) + " :You have not registered");
        } else if (p.getParameterCount() < 2) {
            sendPrefix(Integer.toString(IRCUtil.ERR_NEEDMOREPARAMS) + " :Not enough parameters");
        } else {
            String channel = p.getParameter(1);
            if (!channel.equalsIgnoreCase(server.getChannel())) {
                sendPrefix(Integer.toString(IRCUtil.ERR_NOSUCHCHANNEL) + " " + channel + " :No such channel");
            } else if (connectionStatus == 2) {
                sendPrefix(Integer.toString(IRCUtil.ERR_NOTONCHANNEL) + " " + channel + " :You're not on that channel");
            } else {
                String nick = p.getParameter(2);
                if (!server.nickInChannel(nick)) {
                    sendPrefix(Integer.toString(IRCUtil.ERR_USERNOTINCHANNEL) + " " + nick + " " + channel + " :They aren't on that channel");
                } else if (!server.isOperator(this.nick)) {
                    sendPrefix(Integer.toString(IRCUtil.ERR_CHANOPRIVSNEEDED) + " " + channel + " :You're not channel operator");
                } else {
                    server.part(nick);
                    server.send(null, prefix() + p.getLine());
                }
            }
        }
    }

    /**
   * Receives a <code>TOPIC</code> command from the client.
   * @param p the IRC parser associated with the command
   */
    protected void receiveTopic(IRCParser p) {
        if (connectionStatus <= 1) {
            sendPrefix(Integer.toString(IRCUtil.ERR_NOTREGISTERED) + " :You have not registered");
        } else if (p.getParameterCount() < 1) {
            sendPrefix(Integer.toString(IRCUtil.ERR_NEEDMOREPARAMS) + " :Not enough parameters");
        } else {
            String channel = p.getParameter(1);
            if (!channel.equalsIgnoreCase(server.getChannel())) {
                sendPrefix(Integer.toString(IRCUtil.ERR_NOSUCHCHANNEL) + " " + channel + " :No such channel");
            } else if (connectionStatus == 2) {
                sendPrefix(Integer.toString(IRCUtil.ERR_NOTONCHANNEL) + " " + channel + " :You're not on that channel");
            } else {
                if (p.getParameterCount() < 2) {
                    if (server.getTopic().equals("")) {
                        sendPrefix(Integer.toString(IRCUtil.RPL_NOTOPIC) + " :No topic is set");
                    } else {
                        sendPrefix(Integer.toString(IRCUtil.RPL_TOPIC) + " " + nick + " " + channel + " :" + server.getTopic());
                        sendPrefix(Integer.toString(IRCUtil.RPL_TOPICINFO) + " " + nick + " " + channel + " " + server.getTopicNick() + " :" + Long.toString(server.getTopicDate().getTime()));
                    }
                } else {
                    server.setTopic(nick, p.getTrailing());
                    server.send(null, prefix() + p.getLine());
                }
            }
        }
    }

    /**
   * Receives a <code>PRIVMSG</code> command from the client.
   * @param p the IRC parser associated with the command
   */
    protected void receivePrivmsg(IRCParser p) {
        if (connectionStatus <= 1) {
            sendPrefix(Integer.toString(IRCUtil.ERR_NOTREGISTERED) + " :You have not registered");
        } else if (p.getParameterCount() < 2) {
            if (p.getLine().indexOf(" :") > -1) {
                sendPrefix(Integer.toString(IRCUtil.ERR_NORECIPIENT) + " :No recipient given (PRIVMSG)");
            } else {
                sendPrefix(Integer.toString(IRCUtil.ERR_NOTEXTTOSEND) + " :No text to send");
            }
        } else {
            String recipient = p.getParameter(1);
            if (recipient.equalsIgnoreCase(server.getChannel())) {
                if (connectionStatus == 2) {
                    sendPrefix(Integer.toString(IRCUtil.ERR_CANNOTSENDTOCHAN) + " " + recipient + " :Cannot send to channel");
                } else {
                    server.sendExcept(this.nick, prefix() + p.getLine());
                }
            } else if (!server.nickExists(recipient)) {
                sendPrefix(Integer.toString(IRCUtil.ERR_NOSUCHNICK) + " " + recipient + " :No such nick/channel");
            } else {
                server.send(recipient, prefix() + p.getLine());
            }
        }
    }

    /**
   * Receives a <code>PONG</code> command from the client.
   * @param p the IRC parser associated with the command
   */
    protected void receivePong(IRCParser p) {
        if (connectionStatus <= 1) {
            sendPrefix(Integer.toString(IRCUtil.ERR_NOTREGISTERED) + " :You have not registered");
        } else {
            if (timeoutTask != null) {
                timeoutTask.cancel();
                timeoutTask = null;
            }
        }
    }

    /**
   * Indicates whether some other object is "equal to" this one.
   * Two <code>ConnectionThread</code> objects are equal if their
   * nicks are equal, ignoring case.
   * @param obj the reference object with which to compare
   * @return <code>true</code> if this object is the same as the obj argument
   */
    public boolean equals(Object obj) {
        return obj != null && nick.equalsIgnoreCase(((ConnectionThread) obj).nick);
    }

    /**
   * Waits for incoming data and sends it to <code>receive(String)</code>.
   * @see #receive(String)
   */
    public void run() {
        try {
            String line;
            while (connectionStatus > -1 && !isInterrupted()) {
                line = in.readLine();
                if (line != null) {
                    receive(line);
                } else {
                    disconnect("Read error to " + nick + "[" + host + "]: EOF from client");
                }
            }
        } catch (IOException e) {
            String message = e.getMessage();
            disconnect("Read error to " + nick + "[" + host + "]" + (message == null ? "" : ": " + message));
        }
    }

    /**
   * A <code>TimerTask</code> that sends a <code>PING</code>
   * to the client when triggered, and sets a timeout timer
   * if one wasn't set already.
   */
    protected class PingTask extends TimerTask {

        public void run() {
            send("PING " + server.getServerName());
            if (timeoutTask == null) {
                timeoutTask = new TimeoutTask();
                server.getTimer().schedule(timeoutTask, timeoutDelay);
            }
        }
    }

    /**
   * A <code>TimerTask</code> that disconnects the
   * client due to ping timeout when triggered.
   */
    protected class TimeoutTask extends TimerTask {

        public void run() {
            disconnect("Ping timeout for " + nick + "[" + host + "]");
        }
    }

    /**
   * A <code>TimerTask</code> that disconnects the
   * client due to join timeout when triggered.
   */
    protected class JoinTimeoutTask extends TimerTask {

        public void run() {
            disconnect("Join timeout for " + nick + "[" + host + "]");
        }
    }
}
