package com.outlandr.irc.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import com.outlandr.irc.client.events.ConnectedEvent;
import com.outlandr.irc.client.events.Event;
import com.outlandr.irc.client.events.NameReplyEvent;
import com.outlandr.irc.client.events.UpdateChatEvent;
import com.outlandr.irc.client.replies.RplFactory;
import com.outlandr.irc.client.replies.RplNamReply;
import com.outlandr.irc.client.replies.RplServerReply;
import com.outlandr.irc.client.replies.RplTopic;
import com.outlandr.irc.client.replies.RplWelcome;

public class IRCClient extends Thread {

    private String nick;

    private String realName;

    private String userName;

    public String getNick() {
        return nick;
    }

    private ClientState state = new ClientState();

    private String command;

    private Socket socket;

    private PrintWriter out = null;

    private BufferedReader in = null;

    private BufferedReader userIn = null;

    private MessageManager serverMessageHandler;

    private MessageManager userMessageHandler;

    private List<IRCListener> listeners = new ArrayList<IRCListener>();

    private Object monitor = new Object();

    private String host;

    private Integer port;

    public IRCClient(String host, Integer port, String nick, String realName, String userName) {
        this.nick = nick;
        this.realName = realName;
        this.userName = userName;
        this.host = host;
        this.port = port;
    }

    public void queueUserMessage(String channelName, String message) {
        if (message.charAt(0) == '/') {
            message = message.substring(1);
        } else if (channelName != null && !channelName.isEmpty()) {
            message = String.format("PRIVMSG %s :%s", channelName, message);
        }
        userMessageHandler.addMessage(message);
    }

    protected void registerConnection() {
        command = String.format("NICK %s\n", nick);
        command += String.format("USER %s %s %s :%s", userName, userName, userName, realName);
        internalSendCommand(command);
    }

    protected void connect(String host, Integer port) {
        SocketAddress socketAddress = new InetSocketAddress(host, port);
        try {
            socket.connect(socketAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initStreams() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addListener(IRCListener listner) {
        listeners.add(listner);
    }

    private void internalSendCommand(String command) {
        out.println(command);
    }

    private void readServerOutput() {
        String message = serverMessageHandler.getMessage();
        if (message != null) {
            System.out.println(message);
            if (message.contains("PING")) {
                handlePingMessage(message);
            } else {
                Event event = handleServerReplies(message);
                for (IRCListener listener : listeners) {
                    listener.handleEvent(event);
                }
            }
        }
    }

    private Event handleServerReplies(String textReply) {
        RplServerReply reply = RplFactory.getReply(textReply);
        if (reply == null) {
            return getDefaultEvent(textReply);
        }
        switch(reply.getReplyId()) {
            case RplServerReply.RPL_WELCOME:
                RplWelcome welcome = (RplWelcome) reply;
                return new ConnectedEvent(textReply);
            case RplServerReply.RPL_TOPIC:
                RplTopic topic = (RplTopic) reply;
                state.joinChannel(topic.getChannelName());
                return getDefaultEvent(textReply);
            case RplServerReply.RPL_NAMREPLY:
                RplNamReply nameReply = (RplNamReply) reply;
                Channel channel = state.joinChannel(nameReply.getChannelName());
                for (String name : nameReply.getNames()) {
                    channel.addMember(name);
                }
                return new NameReplyEvent(channel);
            default:
                return getDefaultEvent(textReply);
        }
    }

    private Event getDefaultEvent(String textReply) {
        String[] parts = textReply.split(" :");
        String channelName = host;
        String userName = null;
        String[] temp = parts[0].split(" ");
        if (parts[0].contains("PRIVMSG")) {
            channelName = temp[2];
            userName = temp[0].substring(1, temp[0].indexOf('!'));
            textReply = String.format("%s: %s", userName, parts[1]);
        }
        Room room = state.getChannel(channelName);
        room.updateText(textReply);
        return new UpdateChatEvent(channelName, textReply);
    }

    private void updateClientState() {
    }

    private void handlePingMessage(String message) {
        String substring = message.substring(message.indexOf(':'), message.length());
        internalSendCommand("PONG " + substring);
    }

    public void run() {
        socket = new Socket();
        connect(host, port);
        initStreams();
        serverMessageHandler = new MessageManager(in, monitor);
        userMessageHandler = new MessageManager(userIn, monitor);
        serverMessageHandler.start();
        userMessageHandler.start();
        registerConnection();
        state.setConnected(host, true);
        while (true) {
            if (!serverMessageHandler.hasMessages() && !userMessageHandler.hasMessages()) {
                try {
                    synchronized (monitor) {
                        monitor.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            readServerOutput();
            readUserInput();
        }
    }

    private void readUserInput() {
        if (userMessageHandler.hasMessages()) {
            String message = userMessageHandler.getMessage();
            internalSendCommand(message);
        }
    }

    public static void main(String[] args) {
        String SERVER = "irc.backwerds.net";
        int port = 6667;
        IRCClient client = new IRCClient(SERVER, port, "ar", "ar", "AR");
        client.start();
    }

    public void quit() {
        if (state.isConnected()) {
            internalSendCommand("QUIT");
            try {
                userMessageHandler.finish();
                serverMessageHandler.finish();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public ClientState getClientState() {
        return state;
    }

    public void leave(Channel selectedChannel) {
        final String name = selectedChannel.getName();
        internalSendCommand(String.format("PART %s :Leaving", name));
        state.leaveChannel(name);
    }
}
