package org.retro.gis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import org.retro.gis.relate.DatabaseNotFoundException;
import org.retro.gis.util.BotRecord;
import org.w3c.dom.Document;

/**
 * Abstract class for the core IRC functionality, all IRC communication
 * is sent through an subclass of this class.
 * 
 * @author Berlin Brown
 * @author Paul James Mutton
 * @see PircBotInterface
 */
public abstract class PircBot implements PircBotInterface {

    private InputThread _inputThread = null;

    private OutputThread _outputThread = null;

    private String _charset = null;

    private InetAddress _inetAddress = null;

    private String _server = null;

    private int _port = -1;

    private String _password = null;

    private Queue _outQueue = new Queue();

    private long _messageDelay = 300;

    private Hashtable _channels = new Hashtable();

    private Hashtable _topics = new Hashtable();

    private DccManager _dccManager = new DccManager(this);

    private boolean _autoNickChange = false;

    private boolean _verbose = false;

    private boolean _fullVerbose = false;

    private String _name = "PircBot";

    private String _nick = _name;

    private String _login = "PircBot";

    private String _version = "SpiritBot " + VERSION + " Smart Agent[based on PIRC bot]";

    private String _finger = "You ought to be arrested for fingering a bot!";

    private String _channelPrefixes = "#&+!";

    public static final String VERSION = "0.1.13_b857";

    private String _channelAttempt = null;

    private String _parentBotName = null;

    private Document configTreeDoc = null;

    private SpiritKidWrapper _daycare = null;

    private List _sendMessageIRCSend = new ArrayList();

    private List _sendMessageClientSend = new ArrayList();

    private List _sendMessageThinkSend = new ArrayList();

    public PircBot() {
    }

    protected abstract void onConfigOptionsSet();

    public abstract BotRecord findRecordIDBot(String database, int id) throws Exception;

    public Object[] simpleFindRecord(String database, String distinct_query) throws Exception {
        return null;
    }

    /**
	 * Queue any number of messages to send to the IRC channel, Client or 
	 * for Thought processing.
	 * 	 
	 * <p>
	 * The messages can come from the Consumer thread, or internal searches
	 * 
	 * <p>
	 * <ul>
	 * 	<li>irc-send
	 *  <li>client-send
	 *  <li>think-send
	 * </ul>
	 * 
	 * <p>
	 * These commands are used for smart action create
	 * <p>
	 * 
	 * <ul> 
	 *  <li>ircsendmsg
	 *	<li>clientsendmsg
	 *	<li>thinksendmsg
	 * </ul>
     *
	 * 
	 * @param _type
	 * @param _msg
	 */
    public final void sendMessageInternalBotChannel(String _type, String _msg) {
        if (_type.equalsIgnoreCase("irc-send")) {
            _sendMessageIRCSend.add(_msg);
        } else if (_type.equalsIgnoreCase("client-send")) {
            _sendMessageClientSend.add(_msg);
        } else {
            _sendMessageThinkSend.add(_msg);
        }
    }

    public final List[] getMessageInternalBotChannel() {
        List[] l = new List[3];
        l[0] = (List) _sendMessageIRCSend;
        l[1] = (List) _sendMessageClientSend;
        l[2] = (List) _sendMessageThinkSend;
        return l;
    }

    public final void clearMessageInternalBotChannel(String _type) {
        if (_type.equalsIgnoreCase("irc-send")) {
            _sendMessageIRCSend.clear();
        } else if (_type.equalsIgnoreCase("client-send")) {
            _sendMessageClientSend.clear();
        } else {
            _sendMessageThinkSend.clear();
        }
    }

    public final Document getConfigOptionTree() {
        return configTreeDoc;
    }

    public final void setConfigOptions(Document _doc) throws IOException {
        if (_doc == null) {
            throw new IOException("Invalid configuration file doc = null");
        } else {
            configTreeDoc = _doc;
            onConfigOptionsSet();
        }
    }

    public final synchronized void connect(String hostname) throws IOException, IrcException, NickAlreadyInUseException {
        this.connect(hostname, 6667, null);
    }

    public final synchronized void connect(String hostname, int port) throws IOException, IrcException, NickAlreadyInUseException {
        this.connect(hostname, port, null);
    }

    public final synchronized void connect(String hostname, int port, String password) throws IOException, IrcException, NickAlreadyInUseException {
        _server = hostname;
        _port = port;
        _password = password;
        if (isConnected()) {
            throw new IOException("The SpiritBot is already connected to an IRC server.  Disconnect first.");
        }
        this.removeAllChannels();
        Socket socket = new Socket(hostname, port);
        this.log("*** Connected to server....");
        _inetAddress = socket.getLocalAddress();
        InputStreamReader inputStreamReader = null;
        OutputStreamWriter outputStreamWriter = null;
        if (getEncoding() != null) {
            inputStreamReader = new InputStreamReader(socket.getInputStream(), getEncoding());
            outputStreamWriter = new OutputStreamWriter(socket.getOutputStream(), getEncoding());
        } else {
            inputStreamReader = new InputStreamReader(socket.getInputStream());
            outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
        }
        BufferedReader breader = new BufferedReader(inputStreamReader);
        BufferedWriter bwriter = new BufferedWriter(outputStreamWriter);
        if (password != null && !password.equals("")) {
            OutputThread.sendRawLine(this, bwriter, "PASS " + password);
        }
        String nick = this.getName();
        OutputThread.sendRawLine(this, bwriter, "NICK " + nick);
        OutputThread.sendRawLine(this, bwriter, "USER " + this.getLogin() + " 8 * :" + this.getVersion());
        _inputThread = new InputThread(this, socket, breader, bwriter);
        String line = null;
        int tries = 1;
        while ((line = breader.readLine()) != null) {
            this.handleLine(line);
            int firstSpace = line.indexOf(" ");
            int secondSpace = line.indexOf(" ", firstSpace + 1);
            if (secondSpace >= 0) {
                String code = line.substring(firstSpace + 1, secondSpace);
                if (code.equals("004")) {
                    break;
                } else if (code.equals("433")) {
                    if (_autoNickChange) {
                        tries++;
                        nick = getName() + tries;
                        OutputThread.sendRawLine(this, bwriter, "NICK " + nick);
                    } else {
                        socket.close();
                        _inputThread = null;
                        throw new NickAlreadyInUseException(line);
                    }
                } else if (code.startsWith("5") || code.startsWith("4")) {
                    socket.close();
                    _inputThread = null;
                    throw new IrcException("Could not log into the IRC server: " + line);
                }
            }
            this.setNick(nick);
        }
        this.log("*** Logged onto server.");
        socket.setSoTimeout(5 * 60 * 1000);
        _inputThread.start();
        if (_outputThread == null) {
            _outputThread = new OutputThread(this, _outQueue);
            _outputThread.start();
        }
        this.onConnect();
    }

    public final synchronized void reconnect() throws IOException, IrcException, NickAlreadyInUseException {
        if (getServer() == null) {
            throw new IrcException("Cannot reconnect to an IRC server because we were never connected to one previously!");
        }
        connect(getServer(), getPort(), getPassword());
    }

    public final synchronized void disconnect() {
        this.quitServer();
    }

    public void setAutoNickChange(boolean autoNickChange) {
        _autoNickChange = autoNickChange;
    }

    public final void startIdentServer() {
        new IdentServer(this, getLogin());
    }

    public final void joinChannel(String channel) {
        _channelAttempt = channel.toLowerCase();
        this.sendRawLine("JOIN " + channel);
    }

    public final void joinChannel(String channel, String key) {
        this.joinChannel(channel + " " + key);
    }

    public final void partChannel(String channel) {
        this.sendRawLine("PART " + channel);
    }

    public final void partChannel(String channel, String reason) {
        this.sendRawLine("PART " + channel + " :" + reason);
    }

    public final void quitServer() {
        this.quitServer("");
    }

    public final void quitServer(String reason) {
        this.sendRawLine("QUIT :" + reason);
    }

    public final synchronized void sendRawLine(String line) {
        if (isConnected()) {
            _inputThread.sendRawLine(line);
        }
    }

    public final synchronized void sendRawLineViaQueue(String line) {
        if (line == null) {
            throw new NullPointerException("Cannot send null messages to server");
        }
        if (isConnected()) {
            _outQueue.add(line);
        }
    }

    /**
	 * The OutputThread will wait on the queue and once a message has been added,
	 * it will send the message through the socket.
	 * 
	 * <p>
	 * Ideally, a message can only go through if connected.
	 * 
	 * @see #sendRawLine(String)
	 * @see org.retro.gis.BotServer#loadServer(String, String)
	 */
    public final void sendMessage(String target, String message) {
        _outQueue.add("PRIVMSG " + target + " :" + message);
    }

    public final void sendAction(String target, String action) {
        sendCTCPCommand(target, "ACTION " + action);
    }

    public final void sendNotice(String target, String notice) {
        _outQueue.add("NOTICE " + target + " :" + notice);
    }

    public final void sendCTCPCommand(String target, String command) {
        _outQueue.add("PRIVMSG " + target + " :" + command + "");
    }

    public final void changeNick(String newNick) {
        this.sendRawLine("NICK " + newNick);
    }

    public final void setMode(String channel, String mode) {
        this.sendRawLine("MODE " + channel + " " + mode);
    }

    public final void sendInvite(String nick, String channel) {
        this.sendRawLine("INVITE " + nick + " :" + channel);
    }

    public final void ban(String channel, String hostmask) {
        this.sendRawLine("MODE " + channel + " +b " + hostmask);
    }

    public final void unBan(String channel, String hostmask) {
        this.sendRawLine("MODE " + channel + " -b " + hostmask);
    }

    public final void op(String channel, String nick) {
        this.setMode(channel, "+o " + nick);
    }

    public final void deOp(String channel, String nick) {
        this.setMode(channel, "-o " + nick);
    }

    public final void voice(String channel, String nick) {
        this.setMode(channel, "+v " + nick);
    }

    public final void deVoice(String channel, String nick) {
        this.setMode(channel, "-v " + nick);
    }

    public final void setTopic(String channel, String topic) {
        this.sendRawLine("TOPIC " + channel + " :" + topic);
    }

    public final void kick(String channel, String nick) {
        this.kick(channel, nick, "");
    }

    public final void kick(String channel, String nick, String reason) {
        this.sendRawLine("KICK " + channel + " " + nick + " :" + reason);
    }

    public final void listChannels() {
        this.listChannels(null);
    }

    public final void listChannels(String parameters) {
        if (parameters == null) {
            this.sendRawLine("LIST");
        } else {
            this.sendRawLine("LIST " + parameters);
        }
    }

    public final DccFileTransfer dccSendFile(File file, String nick, int timeout) {
        DccFileTransfer transfer = new DccFileTransfer(this, _dccManager, file, nick, timeout);
        transfer.doSend(true);
        return transfer;
    }

    public final DccChat dccSendChatRequest(String nick, int timeout) {
        DccChat chat = null;
        try {
            ServerSocket ss = new ServerSocket(0);
            ss.setSoTimeout(timeout);
            int port = ss.getLocalPort();
            byte[] ip = _inetAddress.getAddress();
            long ipNum = ipToLong(ip);
            sendCTCPCommand(nick, "DCC CHAT chat " + ipNum + " " + port);
            Socket socket = ss.accept();
            ss.close();
            chat = new DccChat(this, nick, socket);
        } catch (Exception e) {
        }
        return chat;
    }

    public void runDefaultLoadBot() {
    }

    public void log(String line) {
        if (_verbose) {
            Date _date = new Date();
            DateFormat _df = DateFormat.getTimeInstance(DateFormat.LONG);
            String slim = "[" + _df.format(_date) + "]: [" + line + "]";
            if (_fullVerbose) {
                System.out.println(slim);
            } else {
                if (slim.length() >= 84) {
                    System.out.println("... " + slim.substring(16, 84) + " ...");
                } else {
                    System.out.println(slim);
                }
            }
        }
    }

    protected void handleLine(String line) {
        this.log(line);
        if (line.startsWith("PING ")) {
            this.onServerPing(line.substring(5));
            return;
        }
        String sourceNick = "";
        String sourceLogin = "";
        String sourceHostname = "";
        StringTokenizer tokenizer = new StringTokenizer(line);
        String senderInfo = tokenizer.nextToken();
        String command = tokenizer.nextToken();
        String target = null;
        int exclamation = senderInfo.indexOf("!");
        int at = senderInfo.indexOf("@");
        if (senderInfo.startsWith(":")) {
            if (exclamation > 0 && at > 0 && exclamation < at) {
                sourceNick = senderInfo.substring(1, exclamation);
                sourceLogin = senderInfo.substring(exclamation + 1, at);
                sourceHostname = senderInfo.substring(at + 1);
            } else {
                if (tokenizer.hasMoreTokens()) {
                    String token = command;
                    int code = -1;
                    try {
                        code = Integer.parseInt(token);
                    } catch (NumberFormatException e) {
                    }
                    if (code != -1) {
                        String errorStr = token;
                        String response = line.substring(line.indexOf(errorStr, senderInfo.length()) + 4, line.length());
                        this.processServerResponse(code, response);
                        return;
                    } else {
                        sourceNick = senderInfo;
                        target = token;
                    }
                } else {
                    this.onUnknown(line);
                    return;
                }
            }
        }
        command = command.toUpperCase();
        if (sourceNick.startsWith(":")) {
            sourceNick = sourceNick.substring(1);
        }
        if (target == null) {
            target = tokenizer.nextToken();
        }
        if (target.startsWith(":")) {
            target = target.substring(1);
        }
        if (command.equals("PRIVMSG") && line.indexOf(":") > 0 && line.endsWith("")) {
            String request = line.substring(line.indexOf(":") + 2, line.length() - 1);
            if (request.equals("VERSION")) {
                this.onVersion(sourceNick, sourceLogin, sourceHostname, target);
            } else if (request.startsWith("ACTION ")) {
                this.onAction(sourceNick, sourceLogin, sourceHostname, target, request.substring(7));
            } else if (request.startsWith("PING ")) {
                this.onPing(sourceNick, sourceLogin, sourceHostname, target, request.substring(5));
            } else if (request.equals("TIME")) {
                this.onTime(sourceNick, sourceLogin, sourceHostname, target);
            } else if (request.equals("FINGER")) {
                this.onFinger(sourceNick, sourceLogin, sourceHostname, target);
            } else if ((tokenizer = new StringTokenizer(request)).countTokens() >= 5 && tokenizer.nextToken().equals("DCC")) {
                boolean success = _dccManager.processRequest(sourceNick, sourceLogin, sourceHostname, request);
                if (!success) {
                    this.onUnknown(line);
                }
            } else {
                this.onUnknown(line);
            }
        } else if (command.equals("PRIVMSG") && _channelPrefixes.indexOf(target.charAt(0)) >= 0) {
            this.onMessage(target, sourceNick, sourceLogin, sourceHostname, line.substring(line.indexOf(" :") + 2));
        } else if (command.equals("PRIVMSG")) {
            this.onPrivateMessage(sourceNick, sourceLogin, sourceHostname, line.substring(line.indexOf(" :") + 2));
        } else if (command.equals("JOIN")) {
            String channel = target;
            this.addUser(channel, new User("", sourceNick));
            this.onJoin(channel, sourceNick, sourceLogin, sourceHostname);
        } else if (command.equals("PART")) {
            this.removeUser(target, sourceNick);
            if (sourceNick.equals(this.getNick())) {
                this.removeChannel(target);
            }
            this.onPart(target, sourceNick, sourceLogin, sourceHostname);
        } else if (command.equals("NICK")) {
            String newNick = target;
            this.renameUser(sourceNick, newNick);
            if (sourceNick.equals(this.getNick())) {
                this.setNick(newNick);
            }
            this.onNickChange(sourceNick, sourceLogin, sourceHostname, newNick);
        } else if (command.equals("NOTICE")) {
            this.onNotice(sourceNick, sourceLogin, sourceHostname, target, line.substring(line.indexOf(" :") + 2));
        } else if (command.equals("QUIT")) {
            if (sourceNick.equals(this.getNick())) {
                this.removeAllChannels();
            } else {
                this.removeUser(sourceNick);
            }
            this.onQuit(sourceNick, sourceLogin, sourceHostname, line.substring(line.indexOf(" :") + 2));
        } else if (command.equals("KICK")) {
            String recipient = tokenizer.nextToken();
            if (recipient.equals(this.getNick())) {
                this.removeChannel(target);
            }
            this.removeUser(target, recipient);
            this.onKick(target, sourceNick, sourceLogin, sourceHostname, recipient, line.substring(line.indexOf(" :") + 2));
        } else if (command.equals("MODE")) {
            String mode = line.substring(line.indexOf(target, 2) + target.length() + 1);
            if (mode.startsWith(":")) {
                mode = mode.substring(1);
            }
            this.processMode(target, sourceNick, sourceLogin, sourceHostname, mode);
        } else if (command.equals("TOPIC")) {
            this.onTopic(target, line.substring(line.indexOf(" :") + 2), sourceNick, System.currentTimeMillis(), true);
        } else if (command.equals("INVITE")) {
            this.onInvite(target, sourceNick, sourceLogin, sourceHostname, line.substring(line.indexOf(" :") + 2));
        } else {
            this.onUnknown(line);
        }
    }

    protected final DccChat dccAcceptChatRequest(String sourceNick, long address, int port) {
        throw new RuntimeException("dccAcceptChatRequest is deprecated, please use onIncomingChatRequest");
    }

    protected final void dccReceiveFile(File file, long address, int port, int size) {
        throw new RuntimeException("dccReceiveFile is deprecated, please use sendFile");
    }

    protected void onConnect() {
    }

    protected void onDisconnect() {
    }

    private final void processServerResponse(int code, String response) {
        if (code == RPL_LIST) {
            int firstSpace = response.indexOf(' ');
            int secondSpace = response.indexOf(' ', firstSpace + 1);
            int thirdSpace = response.indexOf(' ', secondSpace + 1);
            int colon = response.indexOf(':');
            String channel = response.substring(firstSpace + 1, secondSpace);
            int userCount = 0;
            try {
                userCount = Integer.parseInt(response.substring(secondSpace + 1, thirdSpace));
            } catch (NumberFormatException e) {
            }
            String topic = response.substring(colon + 1);
            this.onChannelInfo(channel, userCount, topic);
        } else if (code == RPL_TOPIC) {
            int firstSpace = response.indexOf(' ');
            int secondSpace = response.indexOf(' ', firstSpace + 1);
            int colon = response.indexOf(':');
            String channel = response.substring(firstSpace + 1, secondSpace);
            String topic = response.substring(colon + 1);
            _topics.put(channel, topic);
            this.onTopic(channel, topic);
        } else if (code == RPL_TOPICINFO) {
            StringTokenizer tokenizer = new StringTokenizer(response);
            tokenizer.nextToken();
            String channel = tokenizer.nextToken();
            String setBy = tokenizer.nextToken();
            long date = 0;
            try {
                date = Long.parseLong(tokenizer.nextToken()) * 1000;
            } catch (NumberFormatException e) {
            }
            String topic = (String) _topics.get(channel);
            _topics.remove(channel);
            this.onTopic(channel, topic, setBy, date, false);
        } else if (code == RPL_NAMREPLY) {
            int channelEndIndex = response.indexOf(" :");
            String channel = response.substring(response.lastIndexOf(' ', channelEndIndex - 1) + 1, channelEndIndex);
            StringTokenizer tokenizer = new StringTokenizer(response.substring(response.indexOf(" :") + 2));
            while (tokenizer.hasMoreTokens()) {
                String nick = tokenizer.nextToken();
                String prefix = "";
                if (nick.startsWith("@")) {
                    prefix = "@";
                } else if (nick.startsWith("+")) {
                    prefix = "+";
                } else if (nick.startsWith(".")) {
                    prefix = ".";
                }
                nick = nick.substring(prefix.length());
                this.addUser(channel, new User(prefix, nick));
            }
        } else if (code == RPL_ENDOFNAMES) {
            String channel = response.substring(response.indexOf(' ') + 1, response.indexOf(" :"));
            User[] users = this.getUsers(channel);
            this.onUserList(channel, users);
        }
        this.onServerResponse(code, response);
    }

    protected void onServerResponse(int code, String response) {
    }

    protected void onUserList(String channel, User[] users) {
    }

    protected void onMessage(String channel, String sender, String login, String hostname, String message) {
    }

    protected void onPrivateMessage(String sender, String login, String hostname, String message) {
    }

    protected void onAction(String sender, String login, String hostname, String target, String action) {
    }

    protected void onNotice(String sourceNick, String sourceLogin, String sourceHostname, String target, String notice) {
    }

    protected void onJoin(String channel, String sender, String login, String hostname) {
    }

    protected void onPart(String channel, String sender, String login, String hostname) {
    }

    protected void onNickChange(String oldNick, String login, String hostname, String newNick) {
    }

    protected void onKick(String channel, String kickerNick, String kickerLogin, String kickerHostname, String recipientNick, String reason) {
    }

    protected void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason) {
    }

    protected void onTopic(String channel, String topic) {
    }

    protected void onTopic(String channel, String topic, String setBy, long date, boolean changed) {
    }

    protected void onChannelInfo(String channel, int userCount, String topic) {
    }

    private final void processMode(String target, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
        if (_channelPrefixes.indexOf(target.charAt(0)) >= 0) {
            String channel = target;
            StringTokenizer tok = new StringTokenizer(mode);
            String[] params = new String[tok.countTokens()];
            int t = 0;
            while (tok.hasMoreTokens()) {
                params[t] = tok.nextToken();
                t++;
            }
            char pn = ' ';
            int p = 1;
            for (int i = 0; i < params[0].length(); i++) {
                char atPos = params[0].charAt(i);
                if (atPos == '+' || atPos == '-') {
                    pn = atPos;
                } else if (atPos == 'o') {
                    if (pn == '+') {
                        this.addUser(channel, new User("@", params[p]));
                        onOp(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                    } else {
                        this.addUser(channel, new User("", params[p]));
                        onDeop(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                    }
                    p++;
                } else if (atPos == 'v') {
                    if (pn == '+') {
                        this.addUser(channel, new User("+", params[p]));
                        onVoice(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                    } else {
                        this.addUser(channel, new User("", params[p]));
                        onDeVoice(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                    }
                    p++;
                } else if (atPos == 'k') {
                    if (pn == '+') {
                        onSetChannelKey(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                    } else {
                        onRemoveChannelKey(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                    }
                    p++;
                } else if (atPos == 'l') {
                    if (pn == '+') {
                        onSetChannelLimit(channel, sourceNick, sourceLogin, sourceHostname, Integer.parseInt(params[p]));
                        p++;
                    } else {
                        onRemoveChannelLimit(channel, sourceNick, sourceLogin, sourceHostname);
                    }
                } else if (atPos == 'b') {
                    if (pn == '+') {
                        onSetChannelBan(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                    } else {
                        onRemoveChannelBan(channel, sourceNick, sourceLogin, sourceHostname, params[p]);
                    }
                    p++;
                } else if (atPos == 't') {
                    if (pn == '+') {
                        onSetTopicProtection(channel, sourceNick, sourceLogin, sourceHostname);
                    } else {
                        onRemoveTopicProtection(channel, sourceNick, sourceLogin, sourceHostname);
                    }
                } else if (atPos == 'n') {
                    if (pn == '+') {
                        onSetNoExternalMessages(channel, sourceNick, sourceLogin, sourceHostname);
                    } else {
                        onRemoveNoExternalMessages(channel, sourceNick, sourceLogin, sourceHostname);
                    }
                } else if (atPos == 'i') {
                    if (pn == '+') {
                        onSetInviteOnly(channel, sourceNick, sourceLogin, sourceHostname);
                    } else {
                        onRemoveInviteOnly(channel, sourceNick, sourceLogin, sourceHostname);
                    }
                } else if (atPos == 'm') {
                    if (pn == '+') {
                        onSetModerated(channel, sourceNick, sourceLogin, sourceHostname);
                    } else {
                        onRemoveModerated(channel, sourceNick, sourceLogin, sourceHostname);
                    }
                } else if (atPos == 'p') {
                    if (pn == '+') {
                        onSetPrivate(channel, sourceNick, sourceLogin, sourceHostname);
                    } else {
                        onRemovePrivate(channel, sourceNick, sourceLogin, sourceHostname);
                    }
                } else if (atPos == 's') {
                    if (pn == '+') {
                        onSetSecret(channel, sourceNick, sourceLogin, sourceHostname);
                    } else {
                        onRemoveSecret(channel, sourceNick, sourceLogin, sourceHostname);
                    }
                }
            }
            this.onMode(channel, sourceNick, sourceLogin, sourceHostname, mode);
        } else {
            String nick = target;
            this.onUserMode(nick, sourceNick, sourceLogin, sourceHostname, mode);
        }
    }

    protected void onMode(String channel, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
    }

    protected void onUserMode(String targetNick, String sourceNick, String sourceLogin, String sourceHostname, String mode) {
    }

    protected void onOp(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
    }

    protected void onDeop(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
    }

    protected void onVoice(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
    }

    protected void onDeVoice(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
    }

    protected void onSetChannelKey(String channel, String sourceNick, String sourceLogin, String sourceHostname, String key) {
    }

    protected void onRemoveChannelKey(String channel, String sourceNick, String sourceLogin, String sourceHostname, String key) {
    }

    protected void onSetChannelLimit(String channel, String sourceNick, String sourceLogin, String sourceHostname, int limit) {
    }

    protected void onRemoveChannelLimit(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    protected void onSetChannelBan(String channel, String sourceNick, String sourceLogin, String sourceHostname, String hostmask) {
    }

    protected void onRemoveChannelBan(String channel, String sourceNick, String sourceLogin, String sourceHostname, String hostmask) {
    }

    protected void onSetTopicProtection(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    protected void onRemoveTopicProtection(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    protected void onSetNoExternalMessages(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    protected void onRemoveNoExternalMessages(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    protected void onSetInviteOnly(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    protected void onRemoveInviteOnly(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    protected void onSetModerated(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    protected void onRemoveModerated(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    protected void onSetPrivate(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    protected void onRemovePrivate(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    protected void onSetSecret(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    protected void onRemoveSecret(String channel, String sourceNick, String sourceLogin, String sourceHostname) {
    }

    protected void onInvite(String targetNick, String sourceNick, String sourceLogin, String sourceHostname, String channel) {
    }

    protected void onDccSendRequest(String sourceNick, String sourceLogin, String sourceHostname, String filename, long address, int port, int size) {
    }

    protected void onDccChatRequest(String sourceNick, String sourceLogin, String sourceHostname, long address, int port) {
    }

    protected void onIncomingFileTransfer(DccFileTransfer transfer) {
    }

    protected void onFileTransferFinished(DccFileTransfer transfer, Exception e) {
    }

    protected void onIncomingChatRequest(DccChat chat) {
    }

    protected void onVersion(String sourceNick, String sourceLogin, String sourceHostname, String target) {
        this.sendRawLine("NOTICE " + sourceNick + " :VERSION " + _version + "");
    }

    protected void onPing(String sourceNick, String sourceLogin, String sourceHostname, String target, String pingValue) {
        this.sendRawLine("NOTICE " + sourceNick + " :PING " + pingValue + "");
    }

    protected void onServerPing(String response) {
        this.sendRawLine("PONG " + response);
    }

    protected void onTime(String sourceNick, String sourceLogin, String sourceHostname, String target) {
        this.sendRawLine("NOTICE " + sourceNick + " :TIME " + new Date().toString() + "");
    }

    protected void onFinger(String sourceNick, String sourceLogin, String sourceHostname, String target) {
        this.sendRawLine("NOTICE " + sourceNick + " :FINGER " + _finger + "");
    }

    protected void onUnknown(String line) {
    }

    protected final void setName(String name) {
        _name = name;
    }

    private final void setNick(String nick) {
        _nick = nick;
    }

    protected final void setLogin(String login) {
        _login = login;
    }

    public final void setParentBotName() {
        if (_name != null) _parentBotName = _name; else _parentBotName = "noparentbotname";
    }

    public final void getParentBotName(String _bName) {
        _parentBotName = _bName;
    }

    protected final void setVersion(String version) {
        _version = version;
    }

    protected final void setFinger(String finger) {
        _finger = finger;
    }

    protected final int getServerPort() {
        int _portNo = -1;
        try {
            if (configTreeDoc == null) throw new IOException("Invalid configuration document equals 'null'");
            String serverport = ConfigReader.getConfigValue(configTreeDoc, "serverport");
            _portNo = Integer.parseInt(serverport);
            System.out.println("... [Pirc-Bot] : Enabling TCP Bot Server on Port; " + _portNo);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
        return _portNo;
    }

    public void addRecord(String db, BotRecord _record) throws DatabaseNotFoundException {
    }

    protected final boolean getServerEnable() {
        return false;
    }

    public final SpiritKidWrapper getDaycare() {
        return _daycare;
    }

    public final void setDaycare(SpiritKidWrapper _care) {
        _daycare = _care;
    }

    public final void setVerbose(boolean verbose) {
        _verbose = verbose;
    }

    public final String getAttemptedChannel() {
        return _channelAttempt;
    }

    public final String getName() {
        return _name;
    }

    public String getNick() {
        return _nick;
    }

    public final String getLogin() {
        return _login;
    }

    public final String getVersion() {
        return _version;
    }

    public final String getFinger() {
        return _finger;
    }

    public final synchronized boolean isConnected() {
        return _inputThread != null && _inputThread.isConnected();
    }

    public final void setMessageDelay(long delay) {
        if (delay < 0) {
            throw new IllegalArgumentException("Cannot have a negative time.");
        }
        _messageDelay = delay;
    }

    public final long getMessageDelay() {
        return _messageDelay;
    }

    public final int getMaxLineLength() {
        return InputThread.MAX_LINE_LENGTH;
    }

    public final int getOutgoingQueueSize() {
        return _outQueue.size();
    }

    public final String getServer() {
        return _server;
    }

    public final int getPort() {
        return _port;
    }

    public final String getPassword() {
        return _password;
    }

    public int[] longToIp(long address) {
        int[] ip = new int[4];
        for (int i = 3; i >= 0; i--) {
            ip[i] = (int) (address % 256);
            address = address / 256;
        }
        return ip;
    }

    public long ipToLong(byte[] address) {
        if (address.length != 4) {
            throw new IllegalArgumentException("byte array must be of length 4");
        }
        long ipNum = 0;
        long multiplier = 1;
        for (int i = 3; i >= 0; i--) {
            int byteVal = (address[i] + 256) % 256;
            ipNum += byteVal * multiplier;
            multiplier *= 256;
        }
        return ipNum;
    }

    public void setEncoding(String charset) throws UnsupportedEncodingException {
        "".getBytes(charset);
        _charset = charset;
    }

    public String getEncoding() {
        return _charset;
    }

    public InetAddress getInetAddress() {
        return _inetAddress;
    }

    public boolean equals(Object o) {
        if (o instanceof PircBot) {
            PircBot other = (PircBot) o;
            return other == this;
        }
        return false;
    }

    public int hashCode() {
        return super.hashCode();
    }

    public String toString() {
        return "Version{" + _version + "}" + " Connected{" + isConnected() + "}" + " Server{" + _server + "}" + " Port{" + _port + "}" + " Password{" + _password + "}";
    }

    public final User[] getUsers(String channel) {
        channel = channel.toLowerCase();
        User[] userArray = new User[0];
        synchronized (_channels) {
            Hashtable users = (Hashtable) _channels.get(channel);
            if (users != null) {
                userArray = new User[users.size()];
                Enumeration enumeration = users.elements();
                for (int i = 0; i < userArray.length; i++) {
                    User user = (User) enumeration.nextElement();
                    userArray[i] = user;
                }
            }
        }
        return userArray;
    }

    public final String[] getChannels() {
        String[] channels = new String[0];
        synchronized (_channels) {
            channels = new String[_channels.size()];
            Enumeration enumeration = _channels.keys();
            for (int i = 0; i < channels.length; i++) {
                channels[i] = (String) enumeration.nextElement();
            }
        }
        return channels;
    }

    /**
	 * Each bot may need to perform an internal shutdown on the object,
	 * override this method to perform this shutdown, this method will
	 * get called by the higher-level dispose.
	 *
	 *@see #dispose()
	 */
    protected void defaultShutdownBot() {
    }

    /**
	 * In client mode, certain aspects of the system may be turned off,
	 * use this method to toggle key aspects of the Bot system operating
	 * in client-only mode.
	 * 
	 * @see org.retro.gis.BotServer#defaultLoadSystem()
	 * @see org.retro.gis.BotProcessThread#sendClientMessage(String)
	 * @see #sendMessageInternalBotChannel(String, String)	 
	 *
	 */
    public void forceClientOnConnect() {
    }

    /**
	 * This communication method is a link between the external client and this
	 * bot-object.
	 * 
     * @see org.retro.gis.PircBot
     * @see #onMessage(String,String,String,String,String)
	 * 
	 * @param sender_nick		Sender Name/Nick 
	 * @param hostname			Host sender is using(probably get an IP)
	 * @param message			Message to send
	 */
    public void clientSendCommunication(String sender_nick, String hostname, String message) {
    }

    public synchronized void dispose() {
        defaultShutdownBot();
        if (_outputThread != null) _outputThread.interrupt();
        if (_inputThread != null) _inputThread.dispose();
        _inputThread = null;
        _outputThread = null;
        _outQueue = null;
        _channels = null;
        _topics = null;
        _dccManager = null;
        System.out.println(" : Parent-Name for destruction : " + _parentBotName);
        if (_name.equalsIgnoreCase(_parentBotName)) {
            BotHyperPool.destroyConnections();
            BotHyperPool.pool = null;
            BotMyPool.destroyConnections();
            BotMyPool.pool = null;
            System.out.println("..... The parent bot is shutting down the DB pools.");
        } else {
            System.out.println("..... A bot has been killed, he is trying to shutdown the DB pool, we won't let him");
        }
        System.out.println("..... My Name is [" + _name + "] I have been shutdown..... [ OK ]");
    }

    private final void addUser(String channel, User user) {
        channel = channel.toLowerCase();
        synchronized (_channels) {
            Hashtable users = (Hashtable) _channels.get(channel);
            if (users == null) {
                users = new Hashtable();
                _channels.put(channel, users);
            }
            users.put(user, user);
        }
    }

    private final User removeUser(String channel, String nick) {
        channel = channel.toLowerCase();
        User user = new User("", nick);
        synchronized (_channels) {
            Hashtable users = (Hashtable) _channels.get(channel);
            if (users != null) {
                return (User) users.remove(user);
            }
        }
        return null;
    }

    private final void removeUser(String nick) {
        synchronized (_channels) {
            Enumeration enumeration = _channels.keys();
            while (enumeration.hasMoreElements()) {
                String channel = (String) enumeration.nextElement();
                this.removeUser(channel, nick);
            }
        }
    }

    private final void renameUser(String oldNick, String newNick) {
        synchronized (_channels) {
            Enumeration enumeration = _channels.keys();
            while (enumeration.hasMoreElements()) {
                String channel = (String) enumeration.nextElement();
                User user = this.removeUser(channel, oldNick);
                if (user != null) {
                    user = new User(user.getPrefix(), newNick);
                    this.addUser(channel, user);
                }
            }
        }
    }

    private final void removeChannel(String channel) {
        channel = channel.toLowerCase();
        synchronized (_channels) {
            _channels.remove(channel);
        }
    }

    private final void removeAllChannels() {
        synchronized (_channels) {
            _channels = new Hashtable();
        }
    }
}
