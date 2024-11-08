package wtanaka.praya.irc;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Vector;
import wtanaka.debug.Debug;
import wtanaka.praya.InfoMessage;
import wtanaka.praya.PrayaMessage;
import wtanaka.praya.Protocol;
import wtanaka.praya.Recipient;
import wtanaka.praya.Status;
import wtanaka.praya.config.ChoiceConfigItem;
import wtanaka.praya.config.ConfigItem;
import wtanaka.praya.config.ConfigItemChangeEvent;
import wtanaka.praya.config.ConfigItemChangeListener;
import wtanaka.praya.config.IntegerConfigItem;
import wtanaka.praya.config.StringConfigItem;
import wtanaka.praya.console.CommandInterface;
import wtanaka.praya.console.ConsoleRunnable;
import wtanaka.praya.console.HelpCommand;
import wtanaka.praya.console.NoParamCommand;
import wtanaka.praya.obj.Message;

/**
 * @author $Author: wtanaka $
 * @version $Name:  $ $Date: 2003/12/17 01:27:21 $
 **/
public class IRCClient extends Protocol implements Runnable {

    private Socket serverSocket = null;

    private InputStream socketInputStream = null;

    private OutputStream socketOutputStream = null;

    private Thread myThread = null;

    private String currentChannel = null;

    private static final int CONF_NICK = 0;

    private static final int CONF_SERVER = 1;

    private static final int CONF_PORT = 2;

    private static final int CONF_REALNAME = 3;

    private static final int CONF_AWAYREASON = 4;

    private static final int CONF_USERNAME = 5;

    private static final int CONF_STATUS = 6;

    private static final int CONF_ITEM_COUNT = 7;

    private static final String IRC_STATUS_ONLINE = "Online";

    private static final String IRC_STATUS_AWAY = "Away";

    private static final String IRC_STATUS_OFFLINE = "Offline";

    public static final String[] s_statusList = { IRC_STATUS_ONLINE, IRC_STATUS_AWAY, IRC_STATUS_OFFLINE };

    private ConfigItem[] m_configItems = new ConfigItem[CONF_ITEM_COUNT];

    private IRCReplyParser m_parser = new IRCReplyParser(this);

    public IRCClient() {
        this("irc.mcs.net", "user", "Praya User", "prayauser");
    }

    public IRCClient(String server, String nick, String realName, String username) {
        this(server, nick, realName, username, 6667, "Away from Praya");
    }

    public IRCClient(String server, String nick, String realName, String username, int port, String awayReason) {
        m_configItems[CONF_NICK] = new StringConfigItem("Nickname", nick, 9);
        m_configItems[CONF_SERVER] = new StringConfigItem("Server", server, 15);
        m_configItems[CONF_AWAYREASON] = new StringConfigItem("Away Reason", awayReason, 20);
        m_configItems[CONF_REALNAME] = new StringConfigItem("Real Name", realName, 20);
        m_configItems[CONF_PORT] = new IntegerConfigItem("Port", port);
        m_configItems[CONF_STATUS] = new ChoiceConfigItem("Status", s_statusList, "Offline");
        m_configItems[CONF_STATUS].addConfigItemChangeListener(new ConfigItemChangeListener() {

            public void configItemChanged(ConfigItemChangeEvent evt) {
                if (IRC_STATUS_OFFLINE.equals(evt.getNewValue())) {
                    disconnect();
                } else {
                    try {
                        connect();
                        if (IRC_STATUS_AWAY.equals(evt.getNewValue())) {
                            sendAway((String) m_configItems[CONF_AWAYREASON].getValue());
                        } else {
                            sendUnaway();
                        }
                    } catch (IOException e) {
                        pushMessage(new InfoMessage(IRCClient.this, e.getMessage()));
                        disconnect();
                    }
                }
            }
        });
        m_configItems[CONF_USERNAME] = new StringConfigItem("Username", username, 8);
        updateDescription();
    }

    /**
    * This shouldn't do anything, messages are received by my thread.
    **/
    public void checkForNewMessages(boolean isAutomatic) {
    }

    /**
    * Override visibility to let in IRCReplyParser
    **/
    protected void addRecipient(Recipient r, Status s) {
        super.addRecipient(r, s);
    }

    public void sendMessage(IRCSelfPrivMessage m) throws IOException {
        String[] recips = new String[] { m.getRecipient().getTargetNick() };
        sendPrivMessage(recips, m.getMessageText());
        pushMessage(m);
    }

    public void sendMessage(IRCSelfChannelMessage m) throws IOException {
        String[] recips = new String[] { m.getRecipient().getChannelName() };
        sendPrivMessage(recips, m.getMessageText());
        pushMessage(m);
    }

    public void setServer(String newServer, int port) {
        m_configItems[CONF_SERVER].setValue(newServer);
        m_configItems[CONF_PORT].setValue(new Integer(port));
        updateDescription();
    }

    public String getAwayReason() {
        return ((StringConfigItem) m_configItems[CONF_AWAYREASON]).getStringValue();
    }

    public void setAwayReason(String reason) {
        m_configItems[CONF_AWAYREASON].setValue(reason);
    }

    public synchronized void setUsername(String newUsername) {
        m_configItems[CONF_USERNAME].setValue(newUsername);
    }

    public String getCurrentChannel() {
        return currentChannel;
    }

    public String getUserName() {
        return ((StringConfigItem) m_configItems[CONF_USERNAME]).getStringValue();
    }

    public String getRealName() {
        return ((StringConfigItem) m_configItems[CONF_REALNAME]).getStringValue();
    }

    public String getNick() {
        return ((StringConfigItem) m_configItems[CONF_NICK]).getStringValue();
    }

    private void setNick(String newNick) throws IOException {
        m_configItems[CONF_NICK].setValue(newNick);
        if (isConnected()) {
            final String nickCmd = "NICK " + getNick() + "\r\n";
            socketOutputStream.write(nickCmd.getBytes());
        }
        updateDescription();
    }

    public String getServer() {
        return ((StringConfigItem) m_configItems[CONF_SERVER]).getStringValue();
    }

    public int getPort() {
        return ((IntegerConfigItem) m_configItems[CONF_PORT]).getIntValue();
    }

    public synchronized void connect() throws IOException {
        if (!isConnected()) {
            System.err.println("debug: IRC Opening socket");
            serverSocket = new Socket(getServer(), getPort());
            updateDescription();
            socketInputStream = serverSocket.getInputStream();
            socketOutputStream = serverSocket.getOutputStream();
            setNick(getNick());
            System.err.println("debug: Registering");
            sendUserCommand(getUserName(), getRealName());
            myThread = new Thread(this);
            myThread.start();
        }
    }

    public synchronized boolean isConnected() {
        return (serverSocket != null);
    }

    public synchronized void disconnect() {
        if (isConnected()) {
            try {
                doQuit();
            } catch (IOException e) {
                serverSocket = null;
                updateDescription();
                myThread.interrupt();
            }
        }
        m_configItems[CONF_STATUS].setValue(IRC_STATUS_OFFLINE);
    }

    public synchronized void setStatus(Status status) throws IOException {
        if (!isConnected()) {
            throw new IOException("IRC client cannot set status because it is offline.");
        }
        if (status.isOnline()) {
            sendUnaway();
        } else {
            sendAway(status.getStatusString());
        }
    }

    public synchronized void join(String channel) throws IOException {
        currentChannel = channel;
        sendJoinCommand(channel, null);
    }

    public synchronized void leave(String channel) throws IOException {
        sendPartCommand(channel);
    }

    private String[] splitLine(String line, char splitChar) {
        Vector elements = new Vector();
        int nextVal = 0;
        for (int i = -1; nextVal >= 0; i = nextVal) {
            nextVal = line.indexOf(splitChar, i + 1);
            if (nextVal >= 0) elements.addElement(line.substring(i + 1, nextVal)); else elements.addElement(line.substring(i + 1));
        }
        String[] toret = new String[elements.size()];
        elements.copyInto(toret);
        return toret;
    }

    public void whois(String nick) throws IOException {
        sendWhoisMessage(nick);
    }

    public void who(String channel) throws IOException {
        sendWhoMessage(channel, false);
    }

    /**
    * Reads a line terminated by \r or \n
    **/
    private String readLineFrom(InputStream input) throws IOException {
        int ch = input.read();
        StringBuffer toret = new StringBuffer();
        while (ch != -1 && ch != '\n' && ch != '\r') {
            toret.append((char) ch);
            ch = input.read();
        }
        return toret.toString();
    }

    private void updateDescription() {
        setCurrentDescription("IRC (" + getNick() + "@" + (isConnected() ? getServer() : "(disconnected)") + ")");
    }

    public void run() {
        Thread.currentThread().setName("IRC thread");
        System.err.println("debug: IRC thread starting");
        while (isConnected()) {
            try {
                String line = readLineFrom(socketInputStream);
                if (line != null) {
                    if (line.trim().length() > 0) {
                        Message[] nextMessages = m_parser.processLine(line);
                        for (int i = 0; i < nextMessages.length; ++i) pushMessage(nextMessages[i]);
                    }
                } else disconnect();
            } catch (InterruptedIOException e) {
                if (serverSocket == null) {
                    System.err.println("debug: IRC thread ending");
                    return;
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
                disconnect();
            } catch (IOException e) {
                disconnect();
            }
        }
        disconnect();
        System.err.println("debug: IRC thread ending");
    }

    /**
    * @exception IOException if the client is not connected
    **/
    private synchronized void sendNickCommand(String nickName) throws IOException {
        if (socketOutputStream != null) socketOutputStream.write(("NICK " + nickName + "\r\n").getBytes());
    }

    private synchronized void sendUserCommand(String username, String realname) throws IOException {
        if (socketOutputStream != null) socketOutputStream.write(("USER " + username + " " + InetAddress.getLocalHost().getHostName() + " " + getServer() + " :" + realname.replace('\r', ' ').replace('\n', ' ') + "\r\n").getBytes());
    }

    /**
    * @param key the key to join the channel with, null if none
    **/
    private synchronized void sendJoinCommand(String channel, String key) throws IOException {
        String postfix = "";
        if (key != null) postfix = " " + key;
        if (socketOutputStream != null) socketOutputStream.write(("JOIN " + channel + postfix + "\r\n").getBytes());
    }

    private synchronized void sendPartCommand(String channel) throws IOException {
        if (socketOutputStream != null) socketOutputStream.write(("PART " + channel + "\r\n").getBytes());
    }

    /**
    * This should only be called from sendPrivMessage
    **/
    private synchronized void sendPrivMessageOneLine(String arg1, String arg2) throws IOException {
        if (arg2.length() > 0) {
            socketOutputStream.write(("PRIVMSG " + arg1 + " :" + arg2 + "\r\n").getBytes());
        }
    }

    private synchronized void sendPrivMessage(String[] recipients, String text) throws IOException {
        StringBuffer firstArg = new StringBuffer();
        if (recipients != null && recipients.length > 0) {
            firstArg.append(recipients[0]);
            for (int i = 1; i < recipients.length; ++i) firstArg.append("," + recipients[i]);
        }
        if (socketOutputStream != null) {
            int nextLineBreak;
            while ((nextLineBreak = text.indexOf("\n")) >= 0) {
                String thisLine = text.substring(0, nextLineBreak);
                text = text.substring(nextLineBreak + 1);
                sendPrivMessageOneLine(firstArg.toString(), thisLine);
            }
            sendPrivMessageOneLine(firstArg.toString(), text);
        }
    }

    synchronized void sendPongMessage(String pongParam) throws IOException {
        if (pongParam == null) pongParam = getNick();
        socketOutputStream.write(("PONG " + pongParam + "\r\n").getBytes());
    }

    private synchronized void sendWhoisMessage(String nickMask) throws IOException {
        socketOutputStream.write(("WHOIS " + nickMask + "\r\n").getBytes());
    }

    private synchronized void sendWhoMessage(String name, boolean opsOnly) throws IOException {
        if (name == null) pushMessage(new PrayaMessage("name was null in sendWhoMessage")); else socketOutputStream.write(("WHO " + name + (opsOnly ? " o" : "") + "\r\n").getBytes());
    }

    private synchronized void sendUnaway() throws IOException {
        socketOutputStream.write(("AWAY\r\n").getBytes());
    }

    private synchronized void sendAway(String message) throws IOException {
        socketOutputStream.write(("AWAY :" + message + "\r\n").getBytes());
    }

    private synchronized void sendQuit() throws IOException {
        socketOutputStream.write(("QUIT :Praya $Id: IRCClient.java,v 1.10 2003/12/17 01:27:21 wtanaka Exp $\r\n").getBytes());
    }

    private synchronized void doQuit() throws IOException {
        sendQuit();
        serverSocket = null;
        updateDescription();
        myThread.interrupt();
    }

    /**
    * Text based IRC-specific command interpreter.
    * <pre>
    * /HELP                          : this message
    * /SERVER [hostname[:port]]      : view/switch IRC server (does
    *                                  not reconnect)
    * /QUOTE COMMAND PARAMS          : send COMMAND PARAMS to IRC
    *                                  server verbatim
    * /NICK [nickname]               : view/change nickname
    * /JOIN &lt;channel&gt;                : join a channel
    * /LEAVE &lt;channel&gt;               : leave a channel
    * /CHANNEL [channel]             : make channel current
    * /SERVERHELP                    : asks for help from server
    * /WHO [&lt;name&gt; [&lt;o>]]            : looks for users matching name
    * /WHOIS [server] &lt;nickmask,..&gt;  : gives information on users
    * /WHOWAS nick [count [server]]  : gives information on users
    * /AWAY [message]                : sets you unaway/away
    * /KICK #ch nick [message]       : kicks nick out of #ch
    * /QUIT                          : disconnect
    * </pre>
    **/
    public CommandInterface[] consoleCommands() {
        return new CommandInterface[] { new HelpCommand(this), new NoParamCommand("QUIT", "disconnect", new ConsoleRunnable() {

            public String run() throws IOException {
                doQuit();
                return "Quitting";
            }
        }) };
    }

    public Recipient getDefaultRecipient() {
        return null;
    }

    /**
    * Used to describe the class of this protocol, before any
    * instances have been created.
    **/
    public static String getProtocolDescription() {
        return "IRC";
    }

    public ConfigItem[] getConfiguration() {
        return m_configItems;
    }

    public static java.awt.Image getIcon() {
        java.net.URL imageURL = IRCClient.class.getResource("ircicon.gif");
        if (imageURL != null) {
            try {
                java.awt.Toolkit toolkit = java.awt.Toolkit.getDefaultToolkit();
                return toolkit.createImage((java.awt.image.ImageProducer) imageURL.getContent());
            } catch (Exception e) {
                Debug.reportBug(e);
            }
        }
        return null;
    }
}
