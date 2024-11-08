package org.mineground.handlers.irc;

import java.util.ArrayList;
import java.util.List;
import org.jibble.pircbot.Colors;
import org.jibble.pircbot.PircBot;
import org.jibble.pircbot.User;
import org.mineground.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @file Handler.java (22.01.2012)
 * @author Daniel Koenen
 *
 */
public class Handler extends PircBot {

    private static final Logger ExceptionLogger = LoggerFactory.getLogger(Handler.class);

    public CommandHandler commandHandler;

    private String ircServer;

    private int ircServerPort;

    private String ircName;

    private String ircChannel;

    private String ircCrewChannel;

    private String ircCrewChannelPassword;

    private String ircBindAddress;

    public Handler() {
        commandHandler = new CommandHandler(this);
        ircServer = Main.getInstance().getConfigHandler().ircNetwork;
        ircServerPort = Main.getInstance().getConfigHandler().ircPort;
        ircName = Main.getInstance().getConfigHandler().ircBotName;
        ircChannel = Main.getInstance().getConfigHandler().ircChannel;
        ircCrewChannel = Main.getInstance().getConfigHandler().ircDevChannel;
        ircCrewChannelPassword = Main.getInstance().getConfigHandler().ircDevPassword;
        ircBindAddress = Main.getInstance().getConfigHandler().ircBindAddr;
        setVerbose(false);
        setName(ircName);
        setFinger(ircName);
        setLogin(ircName);
        setVersion(ircName);
        setMessageDelay(500);
        setAutoNickChange(true);
        if (Main.getInstance().getConfigHandler().liveVersion) {
            bindLocalAddr(ircBindAddress, ircServerPort);
        }
        try {
            connect(ircServer, ircServerPort, "");
        } catch (Exception exception) {
            ExceptionLogger.error("Exception caught", exception);
        }
    }

    @Override
    public void onConnect() {
        joinChannel(ircChannel);
        joinChannel(ircCrewChannel, ircCrewChannelPassword);
    }

    @Override
    public void onDisconnect() {
        String[] botChannels;
        botChannels = getChannels();
        for (String botChannel : botChannels) {
            sendMessage(botChannel, Colors.RED + "* Disabling LVM Plugin...");
        }
    }

    @Override
    public void onMessage(String channel, String sender, String login, String hostname, String message) {
        if (message.charAt(0) == Main.getInstance().getConfigHandler().ircCommandPrefix) {
            int firstSpace = message.indexOf(" ");
            String commandName;
            String[] arguments;
            if (firstSpace == -1) {
                commandName = message.substring(1);
                arguments = new String[0];
            } else {
                commandName = message.substring(1, firstSpace);
                String nonSplittedArguments;
                nonSplittedArguments = message.substring(2 + commandName.length());
                arguments = nonSplittedArguments.split(" ");
            }
            commandHandler.triggerCommand(commandName, sender, channel, arguments);
        }
    }

    @Override
    public void onPrivateMessage(String sender, String login, String hostname, String message) {
        if (message.charAt(0) == Main.getInstance().getConfigHandler().ircCommandPrefix) {
            int firstSpace = message.indexOf(" ");
            String commandName;
            String[] arguments;
            if (firstSpace == -1) {
                commandName = message.substring(1);
                arguments = new String[0];
            } else {
                commandName = message.substring(1, firstSpace);
                String nonSplittedArguments;
                nonSplittedArguments = message.substring(2 + commandName.length());
                arguments = nonSplittedArguments.split(" ");
            }
            commandHandler.triggerPrivateCommand(commandName, sender, arguments);
        }
    }

    public boolean isHop(String username, String channel) {
        User user = getUser(username, channel);
        List<String> allowedPrefixes = new ArrayList<String>();
        allowedPrefixes.add("%");
        allowedPrefixes.add("@");
        allowedPrefixes.add("&");
        allowedPrefixes.add("~");
        if (allowedPrefixes.contains(getHighestUserPrefix(user))) return true;
        return false;
    }

    public boolean isOp(String username, String channel) {
        User user = getUser(username, channel);
        List<String> allowedPrefixes = new ArrayList<String>();
        allowedPrefixes.add("@");
        allowedPrefixes.add("&");
        allowedPrefixes.add("~");
        if (allowedPrefixes.contains(getHighestUserPrefix(user))) return true;
        return false;
    }

    public boolean isOwner(String username, String channel) {
        User user = getUser(username, channel);
        List<String> allowedPrefixes = new ArrayList<String>();
        allowedPrefixes.add("~");
        if (allowedPrefixes.contains(getHighestUserPrefix(user))) return true;
        return false;
    }

    public boolean isSop(String username, String channel) {
        User user = getUser(username, channel);
        List<String> allowedPrefixes = new ArrayList<String>();
        allowedPrefixes.add("&");
        allowedPrefixes.add("~");
        if (allowedPrefixes.contains(getHighestUserPrefix(user))) return true;
        return false;
    }

    public boolean isVoice(String username, String channel) {
        User user = getUser(username, channel);
        List<String> allowedPrefixes = new ArrayList<String>();
        allowedPrefixes.add("+");
        allowedPrefixes.add("%");
        allowedPrefixes.add("@");
        allowedPrefixes.add("&");
        allowedPrefixes.add("~");
        if (allowedPrefixes.contains(getHighestUserPrefix(user))) return true;
        return false;
    }

    public void sendEchoMessage(String message) {
        sendMessage(ircChannel, message);
    }

    public void sendAdminMessage(String message) {
        sendMessage("@" + ircChannel, message);
    }
}
