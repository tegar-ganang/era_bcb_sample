package com.cjw.ircclient;

import java.awt.*;
import java.awt.event.*;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.*;
import org.moxy.irc.*;
import com.cjw.util.*;

class IRCPanel extends JPanel implements ChannelListener, ActionListener {

    int connectionID;

    ClientGUI application;

    ChatWindow window;

    String channel = null;

    JTextArea textarea;

    JScrollPane pane;

    JList nicklist;

    int highlightLevel;

    JButton close;

    JButton topic;

    boolean hasNewMessage;

    public static final int CMD_JOIN = 1;

    public static final int CMD_ME = 2;

    public static final int CMD_PART = 3;

    public static final int CMD_PRIVMSG = 4;

    public static final int CMD_NICK = 5;

    public static final int CMD_QUIT = 6;

    public static final int CMD_WHOIS = 7;

    public static final int CMD_WHOWAS = 8;

    public static final int CMD_MODE = 9;

    public static final int CMD_KICK = 10;

    public static final int CMD_SERVER = 11;

    public static final int CMD_RECONNECT = 12;

    public static final int CMD_DISCONNECT = 13;

    private static final String[] commandStrings = { "JOIN", "ME", "PART", "MSG", "NICK", "QUIT", "WHOIS", "WHOWAS", "MODE", "KICK", "SERVER", "RECONNECT", "DISCONNECT" };

    private static final Hashtable commandHasher;

    static {
        int i;
        commandHasher = new Hashtable();
        for (i = 0; i < commandStrings.length; i++) commandHasher.put(commandStrings[i], new Integer(i + 1));
    }

    private static int getID(String command) {
        Integer commandID;
        commandID = (Integer) commandHasher.get(command.toUpperCase());
        if (commandID == null) {
            System.out.println("*** command /" + command + " not supported");
            return 0;
        }
        return commandID.intValue();
    }

    String getNickName() {
        IRCConnection connection;
        connection = application.getConnection(connectionID);
        if (connection == null) return ""; else return connection.getNickName();
    }

    ListNick[] getNickList() {
        IRCConnection connection;
        connection = application.getConnection(connectionID);
        if (connection == null || channel == null) return null;
        return connection.getChannel(channel).getNickList();
    }

    ChatWindow getParentWindow() {
        return window;
    }

    boolean isTaken() {
        return channel != null;
    }

    private void updateNickList() {
        ListNick[] nicks;
        nicks = getNickList();
        if (nicks == null) return;
        sun.misc.Sort.quicksort(nicks, new ListNick(null));
        nicklist.setListData(nicks);
    }

    IRCPanel(int connectionID, ClientGUI application, ChatWindow window) {
        JSplitPane splitter;
        JPanel sidePanel;
        JScrollPane nickScroller;
        JPanel topPanel;
        this.connectionID = connectionID;
        this.application = application;
        this.window = window;
        highlightLevel = 0;
        setLayout(new BorderLayout());
        topPanel = new JPanel();
        topPanel.setLayout(new TopMinimalLayout());
        add(topPanel, BorderLayout.NORTH);
        close = new JButton("close");
        close.addActionListener(this);
        topPanel.add(close);
        topic = new JButton();
        topic.addActionListener(this);
        topPanel.add(topic);
        textarea = new JTextArea();
        textarea.setEditable(false);
        textarea.setLineWrap(true);
        pane = new JScrollPane(textarea);
        pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sidePanel = new JPanel();
        sidePanel.setLayout(new BorderLayout());
        splitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false, pane, sidePanel);
        add(splitter, BorderLayout.CENTER);
        nicklist = new JList();
        nicklist.setCellRenderer(new ListNickRenderer());
        nicklist.setSelectionBackground(new Color(0, 0, 177));
        nickScroller = new JScrollPane(nicklist);
        nickScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sidePanel.add(nickScroller, BorderLayout.CENTER);
        splitter.setDividerLocation(450);
    }

    void showString(String s) {
        JScrollBar x_scrollbar, y_scrollbar;
        boolean isAtMaximum;
        x_scrollbar = pane.getHorizontalScrollBar();
        y_scrollbar = pane.getVerticalScrollBar();
        isAtMaximum = (y_scrollbar.getValue() >= y_scrollbar.getMaximum() - y_scrollbar.getVisibleAmount());
        textarea.append(s + "\n");
        if (true) {
            Rectangle rect;
            rect = new Rectangle();
            rect.x = x_scrollbar.getMaximum() - x_scrollbar.getVisibleAmount();
            rect.y = y_scrollbar.getMaximum() - y_scrollbar.getVisibleAmount();
            rect.width = x_scrollbar.getVisibleAmount();
            rect.height = y_scrollbar.getVisibleAmount();
            if (rect.x < 0) rect.x = 0;
            if (rect.y < 0) rect.y = 0;
            textarea.scrollRectToVisible(rect);
        }
        setHighlightState(100);
    }

    void doInput(IRCLine line) {
        IRCConnection connection;
        String command;
        String target = null;
        int number;
        connection = application.getConnection(connectionID);
        if (line.getRemaining().charAt(0) != '/') {
            command = "MSG";
            target = channel;
        } else {
            command = line.getNextToken().substring(1);
        }
        switch(getID(command)) {
            case CMD_PRIVMSG:
                if (target == null) {
                    target = line.getNextToken();
                    showString(">" + target + "< " + line.getRemaining());
                } else handleMessage(connection.getNickName(), line);
                connection.sendPrivMsg(target, line.getRemaining());
                break;
            case CMD_ME:
                connection.sendAction(channel, line.getRemaining());
                handleAction(connection.getNickName(), line);
                break;
            case CMD_JOIN:
                connection.joinChannel(line.getNextToken());
                break;
            case CMD_NICK:
                connection.changeNick(line.getRemaining());
                break;
            case CMD_PART:
                if (channel == null) break;
                reset(true);
                showString("You left " + channel + ".");
                break;
            case CMD_QUIT:
                window.handleQuit(line.getRemaining());
                break;
            case CMD_WHOIS:
                connection.whois(line.getRemaining());
                break;
            case CMD_WHOWAS:
                connection.whowas(line.getRemaining());
                break;
            case CMD_MODE:
                connection.setMode(line.getNextToken(), line.getNextToken(), line.getRemaining());
                break;
            case CMD_KICK:
                connection.kick(channel, line.getNextToken(), line.getRemaining());
                break;
            case CMD_SERVER:
                {
                    int port;
                    String server;
                    server = line.getNextToken();
                    target = line.getNextToken();
                    try {
                        port = Integer.parseInt(target);
                    } catch (Exception e) {
                        port = 6667;
                    }
                    if (server != null) application.doChangeConnection(connectionID, server, port);
                }
                break;
            case CMD_RECONNECT:
                application.doReconnect(connectionID);
                break;
            case CMD_DISCONNECT:
                application.doDisconnect(connectionID);
                break;
        }
    }

    public void reset() {
    }

    void reset(boolean needToPart) {
        IRCConnection connection;
        connection = application.getConnection(connectionID);
        if (channel == null) return;
        connection.unregisterChannelListener(channel, this);
        if (needToPart) connection.partChannel(channel);
        channel = null;
        window.titleChanged(this);
    }

    String getChannelName() {
        return channel;
    }

    void setChannelName(String newChannel) {
        channel = newChannel;
        window.setTabName(this, channel);
    }

    public boolean isFocusTraversable() {
        return false;
    }

    public void init(IRCConnection connection) {
    }

    public void initialNickList(Vector nicks) {
        updateNickList();
    }

    public void initialTopic(String topic) {
        this.topic.setText(topic);
    }

    public void initialOpTopic(boolean mode) {
    }

    public void initialNoExtMsg(boolean mode) {
    }

    public void initialSecret(boolean mode) {
    }

    public void initialInviteOnly(boolean mode) {
    }

    public void initialPrivate(boolean mode) {
    }

    public void initialModerated(boolean mode) {
    }

    public void initialLimit(boolean mode, int limit) {
    }

    public void initialKey(boolean modek, String key) {
    }

    public void initialBan(Vector bans) {
    }

    public void setInviteOnly(boolean b, String chanop) {
    }

    public void setPrivate(boolean b, String chanop) {
    }

    public void setSecret(boolean b, String chanop) {
    }

    public void setModerated(boolean b, String chanop) {
    }

    public void setNoExtMsg(boolean b, String chanop) {
    }

    public void setOpTopic(boolean b, String chanop) {
    }

    public void setKey(String key, String chanop) {
    }

    public void setLimit(int limit, String chanop) {
    }

    public void ban(String mask, boolean mode, String chanop) {
    }

    public void setOtherMode(char mode, boolean type, String chanop) {
    }

    public void setTopic(String topic, String chanop) {
        this.topic.setText(topic);
        showString("--- " + chanop + " has set the topic to \"" + topic + "\"");
    }

    public void join(String name, String ident, String host) {
        showString("--> " + name + " (" + ident + "@" + host + ") has joined " + channel);
        updateNickList();
    }

    public void part(String name, String ident, String host, String msg) {
        String postfix;
        if (msg.equals("")) postfix = ""; else postfix = " (" + msg + ")";
        showString("<-- " + name + " (" + ident + "@" + host + ") has left " + channel + postfix);
        updateNickList();
    }

    public void quit(String name, String ident, String host, String msg) {
        showString("<-- " + name + " has quit (" + msg + ")");
        updateNickList();
    }

    public void nickChange(String oldName, String newName) {
        IRCConnection connection;
        connection = application.getConnection(connectionID);
        if (oldName.equalsIgnoreCase(connection.getNickName())) showString("--- You are now known as " + newName); else showString("--- " + oldName + " is now known as " + newName);
        window.nickChanged(this);
        updateNickList();
    }

    public void kick(String name, String reason, String chanop) {
        if (name.equalsIgnoreCase(getNickName())) {
            showString("-- You have been kicked from " + channel + " by " + chanop + " (" + reason + ")");
            reset(false);
        } else showString("<-- " + chanop + " has kicked " + name + " from " + channel + " (" + reason + ")");
        updateNickList();
    }

    public void op(String name, boolean mode, String chanop) {
        if (mode) showString("-- " + chanop + " gives channel operator status to " + name); else showString("-- " + chanop + " removes channel operator status from " + name);
        if (name.equalsIgnoreCase(getNickName())) window.nickChanged(this);
        updateNickList();
    }

    public void voice(String name, boolean mode, String chanop) {
        if (mode) showString("-- " + chanop + " gives voice to " + name); else showString("-- " + chanop + " removes voice from " + name);
        if (name.equalsIgnoreCase(getNickName())) window.nickChanged(this);
        updateNickList();
    }

    public void handleMessage(String sender, IRCLine message) {
        showString("<" + IRCMessage.getNickName(sender) + "> " + message.getRemaining());
        if (message.getRemaining().toLowerCase().indexOf(getNickName().toLowerCase()) >= 0) setHighlightState(200);
    }

    public void handleAction(String sender, IRCLine message) {
        showString("* " + IRCMessage.getNickName(sender) + " " + message.getRemaining());
        if (message.getRemaining().toLowerCase().indexOf(getNickName().toLowerCase()) >= 0) setHighlightState(200);
    }

    public void handleNotice(String sender, IRCLine message) {
        showString("Notice from " + sender + ": " + message.getRemaining());
    }

    public void handleCTCPMessage(String sender, IRCLine message) {
        showString("Received CTCP " + message.getRemaining() + " from " + sender);
    }

    public void handleCTCPReply(String sender, IRCLine message) {
        showString("CTCP Reply from " + sender + ": " + message.getRemaining());
    }

    boolean isOpped() {
        IRCConnection connection;
        connection = application.getConnection(connectionID);
        Channel channelObject;
        if (connection == null || channel == null) return false;
        channelObject = connection.getChannel(channel);
        if (channelObject == null) return false; else return channelObject.isOp(getNickName());
    }

    boolean isVoiced() {
        IRCConnection connection;
        connection = application.getConnection(connectionID);
        Channel channelObject;
        if (connection == null || channel == null) return false;
        channelObject = connection.getChannel(channel);
        if (channelObject == null) return false; else return channelObject.isVoice(getNickName());
    }

    private void setHighlightState(int level) {
        if (level > highlightLevel) {
            highlightLevel = level;
            window.somethingHappened(this, level);
        }
    }

    public void resetHighlightState() {
        highlightLevel = 0;
    }

    public void actionPerformed(ActionEvent event) {
        String command;
        command = event.getActionCommand();
        if (command.equals("close")) handleCloseButton(); else handleTopicButton();
    }

    private void handleCloseButton() {
        window.removePanel(this);
    }

    void detach() {
        IRCConnection connection;
        connection = application.getConnection(connectionID);
        if (channel == null) return;
        connection.unregisterChannelListener(channel, this);
        connection.partChannel(channel);
    }

    private void handleTopicButton() {
    }

    int getConnection() {
        return connectionID;
    }
}
