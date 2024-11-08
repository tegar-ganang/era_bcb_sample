package net.sf.zorobot.util;

import be.trc.core.*;
import be.trc.core.hive.*;
import java.net.*;
import java.util.*;
import net.sf.zorobot.core.ZorobotSystem;

/**
 * The IRCBot class makes the development of an IRC bot a piece of cake. It's a kind of an abstract layer
 * between your bot class and the IRCApplication class (which it extends). All you have to do is to create
 * your own bot class that extends IRCBot, and override the methods you want. Unlike when extending IRCApplication
 * directly, you don't have to call "super" anywhere if you use this class. We strongly recommend only to override 
 * the methods that are documented below. The other methods are used by the IRCBot class, and override the methods 
 * of IRCApplication.<p>
 * The methods documented should be sufficient to meet your needs when creating your bot. However, if not, you
 * can always call methods implemented by IRCApplication, of course :).
 * @version 1.1
 * @author Paul Kiekens
 */
public abstract class IRCBot extends IRCApplication {

    /**
	 * Connects to the server with the specified hostname.
	 * @param hostname the server's address
	 * @return the created IRCServer object
	 */
    public IRCServer connect(String hostname) {
        IRCServer server = connect(hostname, 6667);
        return server;
    }

    /**
	 * Connects to the server with the specified hostname and port.
	 * @param hostname the server's address
	 * @param port the port to connect to
	 * @return the created IRCServer object
	 */
    public IRCServer connect(String hostname, int port) {
        IRCServer server = getConnectionHandler().createIRCServer(hostname, port);
        try {
            super.connectToServer(server);
            return server;
        } catch (Exception e) {
            System.err.println("Unable to connect to server");
            return null;
        }
    }

    /**
	 * Joins a channel.
	 * @param channel the channel to join
	 * @param server the server this channel is on
	 */
    public void joinChannel(String channel, IRCServer server) {
        sendToServer("JOIN " + channel, server);
    }

    /**
	 * Joins a channel, using a specific key
	 * @param channel the channel to join
	 * @param key the key to use when joining the channel
	 * @param server the server the channel is on
	 */
    public void joinChannel(String channel, String key, IRCServer server) {
        sendToServer("JOIN " + channel + " " + key, server);
    }

    /**
	 * Parts a channel.
	 * @param channel the channel to part
	 * @param server the server the channel is on
	 */
    public void partChannel(String channel, IRCServer server) {
        sendToServer("PART " + channel, server);
    }

    /**
	 * Parts a channel with a specified reason.
	 * @param channel the channel to part
	 * @param reason the reason
	 * @param server the server the channel is on
	 */
    public void partChannel(String channel, String reason, IRCServer server) {
        sendToServer("PART " + channel + " :" + reason, server);
    }

    /**
	 * Grants an operator status to a user.
	 * @param channel the channel on which the user is getting an operator status
	 * @param nickname the user's nickname
	 * @param server the server this user is on
	 */
    public void op(String channel, String nickname, IRCServer server) {
        sendToServer("MODE " + channel + " +o " + nickname, server);
    }

    /**
	 * Takes the operator status from a user.
	 * @param channel the channel on which the status has to be removed
	 * @param nickname the user's nickname
	 * @param server the server this user is on
	 */
    public void deop(String channel, String nickname, IRCServer server) {
        sendToServer("MODE " + channel + " -o " + nickname, server);
    }

    /**
	 * Sets the default nickname. This is the nickname that the bot will use when connecting to a server.
	 * @param nickname the nickname to use
	 */
    public void setDefaultNickname(String nickname) {
        getConfiguration().set(Configuration.USER_DEFAULT_NICKNAME, nickname);
    }

    /**
	 * Sets the default alternative nickname. This is the alternative nickname that the bot will use when connecting to a server.
	 * @param altnickname the alternative nickname to use
	 */
    public void setDefaultAltNickname(String altnickname) {
        getConfiguration().set(Configuration.USER_DEFAULT_ALTNICKNAME, altnickname);
    }

    /**
	 * Sets the default username. This is the username that the bot will use when connecting to a server.
	 * @param username the username to use
	 */
    public void setDefaultUsername(String username) {
        getConfiguration().set(Configuration.USER_DEFAULT_USERNAME, username);
    }

    /**
	 * Sets the default full name. This is the full name that the bot will use when connecting to a server.
	 * @param fullname the full name to use
	 */
    public void setDefaultFullname(String fullname) {
        getConfiguration().set(Configuration.USER_DEFAULT_FULLNAME, fullname);
    }

    /**
	 * Sets the default password. This is the password that the bot will use when connecting to a server.
	 * @param password the password to use
	 */
    public void setDefaultPassword(String password) {
        getConfiguration().set(Configuration.USER_DEFAULT_PASSWORD, password);
    }

    /**
	 * This method is called when the bot successfully connected to a server.
	 * @param server the server in question
	 */
    public void onConnect(IRCServer server) {
    }

    /**
	 * This method is called when the bot receives a private message from someone.
	 * @param nickname the nickname of the user that sent us the message
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param message the message itself
	 * @param server the server this user is on
	 */
    public void onPrivateMessage(String nickname, String host, String username, String message, IRCServer server) {
    }

    /**
	 * This method is called when someone sent a message to a channel the bot is on.
	 * @param nickname the nickname of the user that sent the message
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param message the message itself
	 * @param channel the channel the message was sent to
	 * @param server the server the user is on
	 */
    public void onPublicMessage(String nickname, String host, String username, String message, String channel, IRCServer server) {
    }

    /**
	 * Tries to change the nickname of the bot. Use this method if the bot is connected to the server.
	 * @param nickname the new nickname
	 * @param server the server on which the bot has to change its nickname
	 */
    public void changeNickname(String nickname, IRCServer server) {
        sendToServer("NICK " + nickname, server);
    }

    /**
	 * This method is called when the bot is granted an operator status.
	 * @param nickname the nickname of the user that opped the bot
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param channel the channel on which the bot was opped
	 * @param server the server that the channel is on
	 */
    public void onBotOp(String nickname, String host, String username, String channel, IRCServer server) {
    }

    /**
	 * This method is called when a user (not the bot) is granted an operator status.
	 * @param nickname the nickname of the user that gave the status
	 * @param host the host of the user that gave the status
	 * @param username the username of the user that gave the status
	 * @param target the nickname of the user the status was given to
	 * @param channel the channel on which the user was opped
	 * @param server the server that the channel is on
	 */
    public void onOtherOp(String nickname, String host, String username, String target, String channel, IRCServer server) {
    }

    /**
	 * This method is called when the operator status of the bot is removed.
	 * @param nickname the nickname of the user that removed the status
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param channel the channel on which the bot was deopped
	 * @param server the server that the channel is on
	 */
    public void onBotDeop(String nickname, String host, String username, String channel, IRCServer server) {
    }

    /**
	 * This method is called when the operator status of a user (not the bot) is removed.
	 * @param nickname the nickname of the user that removed the status
	 * @param host the host of the user that removed the status
	 * @param username the username of the user that removed the status
	 * @param target the nickname of the user that was deopped
	 * @param channel the channel on which the user was deopped
	 * @param server the server that the channel is on
	 */
    public void onOtherDeop(String nickname, String host, String username, String target, String channel, IRCServer server) {
    }

    /**
	 * This method is called when the bot is granted a voice status.
	 * @param nickname the nickname of the user that voiced the bot
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param channel the channel on which the bot was voiced
	 * @param server the server that the channel is on
	 */
    public void onBotVoice(String nickname, String host, String username, String channel, IRCServer server) {
    }

    /**
	 * This method is called when a user (not the bot) is granted a voice status.
	 * @param nickname the nickname of the user that gave the status
	 * @param host the host of the user that gave the status
	 * @param username the username of the user that gave the status
	 * @param target the nickname of the user the status was given to
	 * @param channel the channel on which the user was voiced
	 * @param server the server that the channel is on
	 */
    public void onOtherVoice(String nickname, String host, String username, String target, String channel, IRCServer server) {
    }

    /**
	 * This method is called when the voice status of the bot is removed.
	 * @param nickname the nickname of the user that removed the status
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param channel the channel on which the bot was devoiced
	 * @param server the server that the channel is on
	 */
    public void onBotDevoice(String nickname, String host, String username, String channel, IRCServer server) {
    }

    /**
	 * This method is called when the voice status of a user (not the bot) is removed.
	 * @param nickname the nickname of the user that removed the status
	 * @param host the host of the user that removed the status
	 * @param username the username of the user that removed the status
	 * @param target the nickname of the user that was devoiced
	 * @param channel the channel on which the user was devoiced
	 * @param server the server that the channel is on
	 */
    public void onOtherDevoice(String nickname, String host, String username, String target, String channel, IRCServer server) {
    }

    /**
	 * Sends a message to a user or a channel.
	 * @param target the target the message has to be sent to
	 * @param message the message that has to be sent
	 * @param server the server the target is on
	 */
    public void sendMessage(String target, String message, IRCServer server) {
        super.sendMessage(target, message, server);
    }

    /**
	 * Sends a notice to a user or a channel.
	 * @param target the target the notice has to be sent to
	 * @param message the message in the notice that has to be sent
	 * @param server the server the target is on
	 */
    public void sendNotice(String target, String message, IRCServer server) {
        super.sendNotice(target, message, server);
    }

    /**
	 * Sends an action to a user or a channel.
	 * @param target the target the action has to be sent to
	 * @param action the action that has to be sent
	 * @param server the server the target is on
	 */
    public void sendAction(String target, String action, IRCServer server) {
        String msg = '\001' + "ACTION " + action + '\001';
        super.sendMessage(target, msg, server);
    }

    /**
	 * Retrieves an array of all channels the bot has joined on the specified server.
	 * @param server the server to collect channels on
	 * @return an array of all channels the bot has joined
	 */
    public String[] getChannels(IRCServer server) {
        Channel[] inlist = getCoreChannelArrayOnServer(server);
        String[] outlist = new String[inlist.length];
        for (int i = 0; i < inlist.length; i++) {
            outlist[i] = new Character(inlist[i].getScope()).toString() + inlist[i].getName();
        }
        return outlist;
    }

    /**
	 * Returns the bot's nickname on a specific server.
	 * @param server the server to check
	 * @return the nickname of the bot
	 */
    public String getNickname(IRCServer server) {
        return getLocalUser(server).getNickname();
    }

    /**
	 * Returns the bot's full name on a specific server.
	 * @param server the server to check
	 * @return the full name of the bot
	 */
    public String getFullname(IRCServer server) {
        return getLocalUser(server).getFullName();
    }

    /**
	 * Returns the bot's username on a specific server.
	 * @param server the server to check
	 * @return the username of the bot
	 */
    public String getUsername(IRCServer server) {
        return getLocalUser(server).getUserName();
    }

    /**
	 * Returns the bot's default nickname. This is the nickname the bot uses to connect to a server.
	 * @return the bot's default nickname
	 */
    public String getDefaultNickname() {
        return getConfiguration().get(Configuration.USER_DEFAULT_NICKNAME);
    }

    /**
	 * Returns the bot's default alternative nickname. This is the alternative nickname the bot uses to connect to a server.
	 * @return the bot's default alternative nickname
	 */
    public String getDefaultAltNickname() {
        return getConfiguration().get(Configuration.USER_DEFAULT_ALTNICKNAME);
    }

    /**
	 * Returns the bot's default username. This is the username the bot uses to connect to a server.
	 * @return the bot's default username
	 */
    public String getDefaultUsername() {
        return getConfiguration().get(Configuration.USER_DEFAULT_USERNAME);
    }

    /**
	 * Returns the bot's default full name. This is the full name the bot uses to connect to a server.
	 * @return the bot's default full name
	 */
    public String getDefaultFullname() {
        return getConfiguration().get(Configuration.USER_DEFAULT_FULLNAME);
    }

    /**
	 * Returns the bot's default password. This is the password the bot uses to connect to a server.
	 * @return the bot's default password
	 */
    public String getDefaultPassword() {
        return getConfiguration().get(Configuration.USER_DEFAULT_PASSWORD);
    }

    /**
	 * Returns whether the bot is connected to a specified server or not.
	 * @param server the server to check
	 * @return true if the bot is connected, false otherwise
	 */
    public boolean isConnected(IRCServer server) {
        return (getConnectionHandler().getConnection(server) != null);
    }

    /**
	 * Kicks a user.
	 * @param channel the channel the user has to be kicked on
	 * @param nickname the nickname of the user that has to be kicked
	 * @param server the server the channel is on
	 */
    public void kick(String channel, String nickname, IRCServer server) {
        sendToServer("KICK " + channel + " " + nickname, server);
    }

    /**
	 * Kicks a user, showing a specific reason
	 * @param channel the channel the user has to be kicked on
	 * @param nickname the nickname of the user that has to be kicked
	 * @param reason the reason of the kick
	 * @param server the server the channel is on
	 */
    public void kick(String channel, String nickname, String reason, IRCServer server) {
        sendToServer("KICK " + channel + " " + nickname + " :" + reason, server);
    }

    /**
	 * This method is called when the bot has joined a channel.
	 * @param channel the channel the bot joined
	 * @param server the server the channel is on
	 */
    public void onBotJoin(String channel, IRCServer server) {
    }

    /**
	 * This method is called when the bot has left a channel.
	 * @param channel the channel the bot left
	 * @param server the server the channel is on
	 */
    public void onBotPart(String channel, IRCServer server) {
    }

    /**
	 * This method is called when a user (not the bot) has joined a channel.
	 * @param nickname the nickname of the user that joined
	 * @param host the host of the user that joined
	 * @param username the username of the user that joined
	 * @param channel the channel the user joined
	 * @param server the server the channel is on
	 */
    public void onOtherJoin(String nickname, String host, String username, String channel, IRCServer server) {
    }

    /**
	 * This method is called when a user (not the bot) has left a channel.
	 * @param nickname the nickname of the user that left
	 * @param host the host of the user that left
	 * @param username the username of the user that left
	 * @param channel the channel the user left
	 * @param server the server the channel is on
	 */
    public void onOtherPart(String nickname, String host, String username, String channel, IRCServer server) {
    }

    /**
	 * This method is called when the bot was kicked.
	 * @param channel the channel the bot was kicked on
	 * @param nickname the nickname of the user the bot was kicked by
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param reason the reason of the kick
	 * @param server the server the channel is on
	 */
    public void onBotKick(String channel, String nickname, String host, String username, String reason, IRCServer server) {
    }

    /**
	 * This method is called when a user (not the bot) was kicked.
	 * @param targetNickname the nickname of the user that was kicked
	 * @param channel the channel the user was kicked on
	 * @param activatorNickname the nickname of the user the target was kicked by
	 * @param activatorHost the host of the user the target was kicked by
	 * @param activatorUsername the username of the user the target was kicked by
	 * @param reason the reason of the kick
	 * @param server the server the channel is on
	 */
    public void onOtherKick(String targetNickname, String channel, String activatorNickname, String activatorHost, String activatorUsername, String reason, IRCServer server) {
    }

    /**
	 * This method is called when a user (either the bot or another user) changed his/her/its nickname.
	 * @param oldNickname the user's old nickname
	 * @param host the host of the user that changed the nickname
	 * @param username the username of the user that changed the nickname
	 * @param newNickname the user's new nickname
	 * @param server the server the user is on
	 */
    public void onNickChange(String oldNickname, String host, String username, String newNickname, IRCServer server) {
    }

    /**
	 * This method is called when the topic of a channel was changed.
	 * @param channel the channel the topic was changed of
	 * @param topic the new topic set
	 * @param nickname the nickname of the user that changed the topic
	 * @param server the server the channel is on
	 */
    public void onTopicChange(String channel, String topic, String nickname, IRCServer server) {
    }

    /**
	 * This method is called when the bot has quit a server.
	 * @param reason the reason of the quit
	 * @param server the server the bot has quit
	 */
    public void onBotQuit(String reason, IRCServer server) {
    }

    /**
	 * Disconnects from a server (sending a QUIT message).
	 * @param server the server to disconnect from
	 */
    public void disconnect(IRCServer server) {
        sendToServer("QUIT", server);
    }

    /**
	 * Disconnects from a server (sending a QUIT message), with a specified reason.
	 * @param reason the reason of the quit
	 * @param server the server to disconnect from
	 */
    public void disconnect(String reason, IRCServer server) {
        sendToServer("QUIT :" + reason, server);
    }

    /**
	 * This method is called when a user (not the bot) has quit a server.
	 * @param reason the reason of the quit
	 * @param server the server the user has quit
	 */
    public void onOtherQuit(String nickname, String host, String username, String reason, IRCServer server) {
    }

    /**
	 * This method is called when a channel is set invite only.
	 * @param channel the channel on which the mode was set
	 * @param nickname the nickname of the user that set the mode
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param server the server the channel is on
	 */
    public void onSetModeInviteOnly(String channel, String nickname, String host, String username, IRCServer server) {
    }

    /**
	 * This method is called when the invite only mode was removed from a channel.
	 * @param channel the channel the mode was removed from
	 * @param nickname the nickname of the user that removed the mode
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param server the server the channel is on
	 */
    public void onRemoveModeInviteOnly(String channel, String nickname, String host, String username, IRCServer server) {
    }

    /**
	 * This method is called when a channel is set moderated.
	 * @param channel the channel on which the mode was set
	 * @param nickname the nickname of the user that set the mode
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param server the server the channel is on
	 */
    public void onSetModeModerated(String channel, String nickname, String host, String username, IRCServer server) {
    }

    /**
	 * This method is called when the moderated mode was removed from a channel.
	 * @param channel the channel the mode was removed from
	 * @param nickname the nickname of the user that removed the mode
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param server the server the channel is on
	 */
    public void onRemoveModeModerated(String channel, String nickname, String host, String username, IRCServer server) {
    }

    /**
	 * This method is called when a channel is set to "no outside messages".
	 * @param channel the channel on which the mode was set
	 * @param nickname the nickname of the user that set the mode
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param server the server the channel is on
	 */
    public void onSetModeNoOutsideMessages(String channel, String nickname, String host, String username, IRCServer server) {
    }

    /**
	 * This method is called when the "no outside messages" mode was removed from a channel.
	 * @param channel the channel the mode was removed from
	 * @param nickname the nickname of the user that removed the mode
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param server the server the channel is on
	 */
    public void onRemoveModeNoOutsideMessages(String channel, String nickname, String host, String username, IRCServer server) {
    }

    /**
	 * This method is called when a channel is set quiet.
	 * @param channel the channel on which the mode was set
	 * @param nickname the nickname of the user that set the mode
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param server the server the channel is on
	 */
    public void onSetModeQuiet(String channel, String nickname, String host, String username, IRCServer server) {
    }

    /**
	 * This method is called when the quiet mode was removed from a channel.
	 * @param channel the channel the mode was removed from
	 * @param nickname the nickname of the user that removed the mode
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param server the server the channel is on
	 */
    public void onRemoveModeQuiet(String channel, String nickname, String host, String username, IRCServer server) {
    }

    /**
	 * This method is called when a channel is set private.
	 * @param channel the channel on which the mode was set
	 * @param nickname the nickname of the user that set the mode
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param server the server the channel is on
	 */
    public void onSetModePrivate(String channel, String nickname, String host, String username, IRCServer server) {
    }

    /**
	 * This method is called when the private mode was removed from a channel.
	 * @param channel the channel the mode was removed from
	 * @param nickname the nickname of the user that removed the mode
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param server the server the channel is on
	 */
    public void onRemoveModePrivate(String channel, String nickname, String host, String username, IRCServer server) {
    }

    /**
	 * This method is called when a channel is set secret.
	 * @param channel the channel on which the mode was set
	 * @param nickname the nickname of the user that set the mode
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param server the server the channel is on
	 */
    public void onSetModeSecret(String channel, String nickname, String host, String username, IRCServer server) {
    }

    /**
	 * This method is called when the secret mode was removed from a channel.
	 * @param channel the channel the mode was removed from
	 * @param nickname the nickname of the user that removed the mode
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param server the server the channel is on
	 */
    public void onRemoveModeSecret(String channel, String nickname, String host, String username, IRCServer server) {
    }

    /**
	 * This method is called when a channel is set to "topic by ops only".
	 * @param channel the channel on which the mode was set
	 * @param nickname the nickname of the user that set the mode
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param server the server the channel is on
	 */
    public void onSetModeTopicByOpsOnly(String channel, String nickname, String host, String username, IRCServer server) {
    }

    /**
	 * This method is called when the "topic by ops only" mode was removed from a channel.
	 * @param channel the channel the mode was removed from
	 * @param nickname the nickname of the user that removed the mode
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param server the server the channel is on
	 */
    public void onRemoveModeTopicByOpsOnly(String channel, String nickname, String host, String username, IRCServer server) {
    }

    /**
	 * This method is called when a ban is set.
	 * @param nickname the nickname of the user that set the ban
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param bannedHostmask the hostmask that is covered by the ban
	 * @param channel the channel on which the ban is active
	 * @param server the server the channel is on
	 */
    public void onSetModeBan(String nickname, String host, String username, String bannedHostmask, String channel, IRCServer server) {
    }

    /**
	 * This method is called when a ban is removed.
	 * @param nickname the nickname of the user that removed the ban
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param bannedHostmask the hostmask that was covered by the ban
	 * @param channel the channel the ban was removed from
	 * @param server the server the channel is on
	 */
    public void onRemoveModeBan(String nickname, String host, String username, String bannedHostmask, String channel, IRCServer server) {
    }

    /**
	 * This method is called when a channel key is set.
	 * @param nickname the nickname of the user that set the key
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param key the key
	 * @param channel the channel the key is set on
	 * @param server the server the channel is on
	 */
    public void onSetModeKey(String nickname, String host, String username, String key, String channel, IRCServer server) {
    }

    /**
	 * This method is called when a channel key is removed.
	 * @param nickname the nickname of the user that removed the key
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param key the key
	 * @param channel the channel on which the key was removed
	 * @param server the server the channel is on
	 */
    public void onRemoveModeKey(String nickname, String host, String username, String key, String channel, IRCServer server) {
    }

    /**
	 * This method is called when a userlimit is set.
	 * @param nickname the nickname of the user that set the userlimit
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param limit the userlimit
	 * @param channel the channel the userlimit is set on
	 * @param server the server the channel is on
	 */
    public void onSetModeUserLimit(String nickname, String host, String username, int limit, String channel, IRCServer server) {
    }

    /**
	 * This method is called when a userlimit is removed.
	 * @param nickname the nickname of the user that removed the userlimit
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param limit the userlimit
	 * @param channel the channel on which the userlimit was removed
	 * @param server the server the channel is on
	 */
    public void onRemoveModeUserLimit(String nickname, String host, String username, int limit, String channel, IRCServer server) {
    }

    /**
	 * This method is called when the bot receives a VERSION request.
	 * @param nickname the nickname of the user that sent the request
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param server the server the user is on
	 */
    public void onVersion(String nickname, String host, String username, IRCServer server) {
    }

    /**
	 * This method is called when the bot receives a TIME request.
	 * @param nickname the nickname of the user that sent the request
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param server the server the user is on
	 */
    public void onTime(String nickname, String host, String username, IRCServer server) {
    }

    /**
	 * This method is called when the bot receives a PING request.
	 * @param nickname the nickname of the user that sent the request
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param remoteTime a TimeStamp that contains the time of the user that sent the request
	 * @param server the server the user is on
	 */
    public void onPing(String nickname, String host, String username, Timestamp remoteTime, IRCServer server) {
        System.out.println("on PING: " + nickname);
    }

    /**
	 * This method is called when the bot receives a PING request from the server.
	 * @param server the server that sent the request
	 * @param response the parameter that was sent along with the request
	 */
    public void onServerPing(IRCServer server, String response) {
        System.out.println(new Date() + " - on SERVER PING: " + response);
    }

    /**
	 * Sets a ban.
	 * @param channel the channel to set the ban on
	 * @param hostmask the hostmask that has to be banned
	 * @param server the server the channel is on
	 */
    public void ban(String channel, String hostmask, IRCServer server) {
        sendToServer("MODE " + channel + " +b " + hostmask, server);
    }

    /**
	 * Removes a ban.
	 * @param channel the channel to remove the ban on
	 * @param hostmask the hostmask that is covered by the ban
	 * @param server the server the channel is on
	 */
    public void unban(String channel, String hostmask, IRCServer server) {
        sendToServer("MODE " + channel + " -b " + hostmask, server);
    }

    /**
	 * Returns an array of all servers the bot has connected to.
	 * @return an array of all servers the bot has connected to
	 */
    public IRCServer[] getServers() {
        IRCConnectionHandler connHandler = getConnectionHandler();
        Iterator it = connHandler.getAllServers();
        ArrayList list = new ArrayList();
        while (it.hasNext()) {
            IRCServer server = (IRCServer) it.next();
            list.add(server);
        }
        IRCServer[] serverlist = new IRCServer[list.size()];
        for (int i = 0; i < list.size(); i++) {
            serverlist[i] = (IRCServer) list.get(i);
        }
        return serverlist;
    }

    /**
	 * Returns an array of all users that are on a specific channel.
	 * @param channel the channel to check
	 * @param server the server the channel is on
	 * @return an array of all users on a specific channel
	 */
    public User[] getUsers(String channel, IRCServer server) {
        Channel chanid = getChannelByName(channel.charAt(0), channel.substring(1), server);
        if (chanid != null) {
            return getChannelUserWireCollection().getUserArrayInChannel(chanid);
        } else return null;
    }

    /**
	 * This method is called when an action is sent to a channel.
	 * @param channel the channel the action is sent to
	 * @param nickname the nickname of the user that sent the action
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param action the action message
	 * @param server the server the channel is on
	 */
    public void onPublicAction(String channel, String nickname, String host, String username, String action, IRCServer server) {
    }

    /**
	 * This method is called when the bot receives an action from a user.
	 * @param nickname the nickname of the user that sent the action
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param action the action message
	 * @param server the server the channel is on
	 */
    public void onPrivateAction(String nickname, String host, String username, String action, IRCServer server) {
    }

    /**
	 * This method is called when the bot receives a numeric response from a server.
	 * @param code the numeric code
	 * @param response the response message
	 * @param server the server the response was received from
	 */
    public void onServerResponse(int code, String response, IRCServer server) {
    }

    /**
	 * This method is called when the bot receives a userlist from the server after joining a channel.
	 * @param channel the channel the userlist belongs to
	 * @param users an array of all users on the channel
	 * @param server the server the channel is on
	 */
    public void onUserList(String channel, User[] users, IRCServer server) {
    }

    /**
	 * Sets a mode on a channel.
	 * @param channel the channel to set the mode on
	 * @param mode the mode to be set
	 * @param server the server the channel is on
	 */
    public void setMode(String channel, String mode, IRCServer server) {
        sendToServer("MODE " + channel + " " + mode, server);
    }

    /**
	 * Sets a topic for a channel.
	 * @param channel the channel to set the topic for
	 * @param topic the topic to be set
	 * @param server the server the channel is on
	 */
    public void setTopic(String channel, String topic, IRCServer server) {
        sendToServer("TOPIC " + channel + " :" + topic, server);
    }

    /**
	 * Grants a voice status to a user.
	 * @param channel the channel on which the user is getting a voice status
	 * @param nickname the user's nickname
	 * @param server the server this user is on
	 */
    public void voice(String channel, String nickname, IRCServer server) {
        sendToServer("MODE " + channel + " +v " + nickname, server);
    }

    /**
	 * Takes the voice status from a user.
	 * @param channel the channel on which the status has to be removed
	 * @param nickname the user's nickname
	 * @param server the server this user is on
	 */
    public void devoice(String channel, String nickname, IRCServer server) {
        sendToServer("MODE " + channel + " -v " + nickname, server);
    }

    /**
	 * This method is called when the server sent an ERROR message to the bot when the bot quit the server. Some servers
	 * do not send this ERROR message to disconnecting clients. If this is the case, use the onBotQuit method instead.
	 * @param server the server that sent the ERROR message
	 */
    public void onDisconnect(IRCServer server) {
    }

    /**
	 * This method is called when the mode of a channel is set.
	 * @param channel the channel the mode was set on
	 * @param nickname the nickname of the user that set the mode
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param mode the mode that was set on the channel
	 * @param server the server the channel is on
	 */
    public void onMode(String channel, String nickname, String host, String username, String mode, IRCServer server) {
    }

    /**
	 * This method is called when the bot receives an invite from a user.
	 * @param nickname the nickname of the user that sent the invite
	 * @param host the host of that user
	 * @param username the username of that user
	 * @param channel the channel the bot is invited to
	 */
    public void onInvite(String nickname, String host, String username, String channel, IRCServer server) {
    }

    /**
	 * Invites a user to a channel.
	 * @param nickname the nickname of the user to send the invite to
	 * @param channel the channel the user has to be invited to
	 * @param server the server the channel is on
	 */
    public void sendInvite(String nickname, String channel, IRCServer server) {
        sendToServer("INVITE " + nickname + " :" + channel, server);
    }

    public void onServerPong(IRCServer server, String msg) {
        System.out.println("PONG: " + msg);
    }

    public void receivedServerMessage(ServerMessageParser parser, IRCServer server) {
        String command = parser.fetchCommand();
        System.out.println("*** server > " + command + " " + parser.fetchParameters());
        if (command.equals("307")) {
            whoisCallback(parser.fetchParameters(), false);
        }
        if (command.equals("318")) {
            whoisCallback(parser.fetchParameters(), true);
        }
        super.receivedServerMessage(parser, server);
        if (command.equalsIgnoreCase("PING")) {
            onServerPing(server, parser.fetchTrailingParameter());
        } else if (command.equalsIgnoreCase("PONG")) {
            onServerPong(server, parser.fetchParameters());
        } else if (command.equalsIgnoreCase("ERROR")) {
            onDisconnect(server);
        } else {
            if (Character.isDigit(command.charAt(0))) {
                int code = new Integer(command).intValue();
                onServerResponse(code, parser.fetchParameters(), server);
                if (code == IRCServer.RPL_WELCOME) {
                    onConnect(server);
                }
            }
        }
    }

    public void whoisCallback(String msg, boolean endWhoIs) {
    }

    public void processMessage(String from, String target, String message, IRCServer ircServer) {
        HostParser hp = new DefaultHostParser(from);
        String fromNickname = hp.getNickname();
        User user = getUserByNickname(fromNickname, ircServer);
        if (user != null) user.setHost(hp.getHost());
        System.out.println(fromNickname + "> " + message);
        if (message.startsWith('\001' + "VERSION") && message.endsWith("" + '\001')) {
            onVersion(fromNickname, hp.getHost(), hp.getUserName(), ircServer);
        } else if (message.startsWith('\001' + "TIME") && message.endsWith("" + '\001')) {
            onTime(fromNickname, hp.getHost(), hp.getUserName(), ircServer);
        } else if (message.startsWith('\001' + "PING") && message.endsWith("" + '\001')) {
            try {
                long remoteTime = Long.parseLong(message.substring(6, message.length() - 1));
                Timestamp ts = new DefaultTimestamp(remoteTime);
                onPing(fromNickname, hp.getHost(), hp.getUserName(), ts, ircServer);
            } catch (Exception e) {
                System.out.println("PING ERROR: " + message);
            }
        } else if (message.startsWith('\001' + "PONG") && message.endsWith("" + '\001')) {
            System.out.println("PONG!!! from " + from);
        } else {
            super.processMessage(from, target, message, ircServer);
            MessageFilter mf = new DefaultMessageFilter();
            if (mf.isActionMessage(message)) {
                if (target.equalsIgnoreCase(getLocalUser(ircServer).getNickname())) {
                    onPublicAction(target, hp.getNickname(), hp.getHost(), hp.getUserName(), mf.filterMessage(message, MessageFilter.TYPE_ACTION), ircServer);
                } else {
                    onPrivateAction(hp.getNickname(), hp.getHost(), hp.getUserName(), mf.filterMessage(message, MessageFilter.TYPE_ACTION), ircServer);
                }
            } else {
                if (target.equalsIgnoreCase(getLocalUser(ircServer).getNickname())) {
                    onPrivateMessage(hp.getNickname(), hp.getHost(), hp.getUserName(), message, ircServer);
                } else {
                    onPublicMessage(hp.getNickname(), hp.getHost(), hp.getUserName(), message, target, ircServer);
                }
            }
        }
    }

    public void changeChannelModes(String nickname, String host, String username, Channel channel, be.trc.core.Queue queueArgModes, be.trc.core.Queue queueNoArgModes, be.trc.core.Queue queueArguments, String modeString) {
        super.changeChannelModes(nickname, host, username, channel, queueArgModes, queueNoArgModes, queueArguments, modeString);
        onMode(new Character(channel.getScope()).toString() + channel.getName(), nickname, host, username, modeString, channel.getServer());
        String activator = nickname;
        while (!queueArgModes.isEmpty()) {
            String modeStr = queueArgModes.pop();
            String argString = queueArguments.pop();
            char modeChar = modeStr.toCharArray()[1];
            char active = modeStr.toCharArray()[0];
            if (modeChar == Mode.CHANNELOPERATOR) {
                String chanid = new Character(channel.getScope()).toString() + channel.getName();
                if (getLocalUser(channel.getServer()).getNickname().equalsIgnoreCase(argString)) {
                    if (active == Mode.ACTIVATEMODE) onBotOp(activator, host, username, chanid, channel.getServer()); else onBotDeop(activator, host, username, chanid, channel.getServer());
                } else {
                    if (active == Mode.ACTIVATEMODE) onOtherOp(activator, host, username, argString, chanid, channel.getServer()); else onOtherDeop(activator, host, username, argString, chanid, channel.getServer());
                }
            } else if (modeChar == Mode.CHANNELVOICE) {
                String chanid = new Character(channel.getScope()).toString() + channel.getName();
                if (getLocalUser(channel.getServer()).getNickname().equalsIgnoreCase(argString)) {
                    if (active == Mode.ACTIVATEMODE) onBotVoice(activator, host, username, chanid, channel.getServer()); else onBotDevoice(activator, host, username, chanid, channel.getServer());
                } else {
                    if (active == Mode.ACTIVATEMODE) onOtherVoice(activator, host, username, argString, chanid, channel.getServer()); else onOtherDevoice(activator, host, username, argString, chanid, channel.getServer());
                }
            } else if (modeChar == Mode.CHANNELBAN) {
                String chanid = new Character(channel.getScope()).toString() + channel.getName();
                if (active == Mode.ACTIVATEMODE) onSetModeBan(activator, host, username, argString, chanid, channel.getServer()); else onRemoveModeBan(activator, host, username, argString, chanid, channel.getServer());
            } else if (modeChar == Mode.CHANNELKEY) {
                String chanid = new Character(channel.getScope()).toString() + channel.getName();
                if (active == Mode.ACTIVATEMODE) onSetModeKey(activator, host, username, argString, chanid, channel.getServer()); else onRemoveModeKey(activator, host, username, argString, chanid, channel.getServer());
            } else if (modeChar == Mode.CHANNELUSERLIMIT) {
                String chanid = new Character(channel.getScope()).toString() + channel.getName();
                try {
                    int limit = Integer.parseInt(argString);
                    if (active == Mode.ACTIVATEMODE) onSetModeUserLimit(activator, host, username, limit, chanid, channel.getServer()); else onRemoveModeUserLimit(activator, host, username, limit, chanid, channel.getServer());
                } catch (NumberFormatException nfe) {
                }
            }
        }
        while (!queueNoArgModes.isEmpty()) {
            String modeStr = queueNoArgModes.pop();
            char modeChar = modeStr.toCharArray()[1];
            char active = modeStr.toCharArray()[0];
            if (modeChar == Mode.CHANNELINVITEONLY) {
                String chanid = new Character(channel.getScope()).toString() + channel.getName();
                if (active == Mode.ACTIVATEMODE) onSetModeInviteOnly(chanid, activator, host, username, channel.getServer()); else onRemoveModeInviteOnly(chanid, activator, host, username, channel.getServer());
            } else if (modeChar == Mode.CHANNELMODERATED) {
                String chanid = new Character(channel.getScope()).toString() + channel.getName();
                if (active == Mode.ACTIVATEMODE) onSetModeModerated(chanid, activator, host, username, channel.getServer()); else onRemoveModeModerated(chanid, activator, host, username, channel.getServer());
            } else if (modeChar == Mode.CHANNELNOOUTSIDEMSGS) {
                String chanid = new Character(channel.getScope()).toString() + channel.getName();
                if (active == Mode.ACTIVATEMODE) onSetModeNoOutsideMessages(chanid, activator, host, username, channel.getServer()); else onRemoveModeNoOutsideMessages(chanid, activator, host, username, channel.getServer());
            } else if (modeChar == Mode.CHANNELQUIET) {
                String chanid = new Character(channel.getScope()).toString() + channel.getName();
                if (active == Mode.ACTIVATEMODE) onSetModeQuiet(chanid, activator, host, username, channel.getServer()); else onRemoveModeQuiet(chanid, activator, host, username, channel.getServer());
            } else if (modeChar == Mode.CHANNELPRIVATE) {
                String chanid = new Character(channel.getScope()).toString() + channel.getName();
                if (active == Mode.ACTIVATEMODE) onSetModePrivate(chanid, activator, host, username, channel.getServer()); else onRemoveModePrivate(chanid, activator, host, username, channel.getServer());
            } else if (modeChar == Mode.CHANNELSECRET) {
                String chanid = new Character(channel.getScope()).toString() + channel.getName();
                if (active == Mode.ACTIVATEMODE) onSetModeSecret(chanid, activator, host, username, channel.getServer()); else onRemoveModeSecret(chanid, activator, host, username, channel.getServer());
            } else if (modeChar == Mode.CHANNELTOPICBYOPSONLY) {
                String chanid = new Character(channel.getScope()).toString() + channel.getName();
                if (active == Mode.ACTIVATEMODE) onSetModeTopicByOpsOnly(chanid, activator, host, username, channel.getServer()); else onRemoveModeTopicByOpsOnly(chanid, activator, host, username, channel.getServer());
            }
        }
    }

    public void localUserJoinsChannel(char channelScope, String channelName, IRCServer ircServer) {
        super.localUserJoinsChannel(channelScope, channelName, ircServer);
        onBotJoin(new Character(channelScope).toString() + channelName, ircServer);
    }

    public void localUserPartsChannel(Channel channel) {
        if (channel != null) {
            String chanid = new Character(channel.getScope()).toString() + channel.getName();
            super.localUserPartsChannel(channel);
            onBotPart(chanid, channel.getServer());
        }
    }

    public void remoteUserJoinsChannel(String nickname, String host, String username, Channel channel) {
        String chanid = new Character(channel.getScope()).toString() + channel.getName();
        IRCServer server = channel.getServer();
        super.remoteUserJoinsChannel(nickname, host, username, channel);
        onOtherJoin(nickname, host, username, chanid, server);
    }

    public void remoteUserPartsChannel(String nickname, String host, String username, Channel channel) {
        System.out.println("QUIT: " + nickname + " @ " + host + ", " + username + " channel = " + channel);
        if (channel == null) return;
        User user = getUserByNickname(nickname, channel.getServer());
        if (user == null) return;
        IRCServer server = user.getServer();
        String chanid = new Character(channel.getScope()).toString() + channel.getName();
        super.remoteUserPartsChannel(nickname, host, username, channel);
        onOtherPart(nickname, host, username, chanid, server);
    }

    public void localUserGetsKicked(Channel channel, String activatorNickname, String activatorHost, String activatorUsername, String reason) {
        String chanid = new Character(channel.getScope()).toString() + channel.getName();
        IRCServer server = channel.getServer();
        super.localUserGetsKicked(channel, activatorNickname, activatorHost, activatorUsername, reason);
        onBotKick(chanid, activatorNickname, activatorHost, activatorUsername, reason, server);
    }

    public void remoteUserGetsKicked(String target, Channel channel, String activatorNickname, String activatorHost, String activatorUsername, String reason) {
        String chanid = new Character(channel.getScope()).toString() + channel.getName();
        IRCServer server = channel.getServer();
        super.remoteUserGetsKicked(target, channel, activatorNickname, activatorHost, activatorUsername, reason);
        onOtherKick(target, chanid, activatorNickname, activatorHost, activatorUsername, reason, server);
    }

    public void remoteUserChangesNickname(String oldNickname, String host, String username, String newNickname, IRCServer server) {
        System.out.println("NICK: " + oldNickname + " @ " + host + ", " + username + " new nick = " + newNickname);
        if (host == null || username == null || server == null) return;
        super.remoteUserChangesNickname(oldNickname, host, username, newNickname, server);
        onNickChange(oldNickname, host, username, newNickname, server);
    }

    public void userChangesTopic(Channel channel, String topic, String activator) {
        String chanid = new Character(channel.getScope()).toString() + channel.getName();
        IRCServer server = channel.getServer();
        super.userChangesTopic(channel, topic, activator);
        onTopicChange(chanid, topic, activator, server);
    }

    public void localUserQuitsIRC(User localUser, String reason) {
        IRCServer server = localUser.getServer();
        super.localUserQuitsIRC(localUser, reason);
        onBotQuit(reason, server);
    }

    public void remoteUserQuitsIRC(String nickname, String host, String username, String reason, IRCServer server) {
        User user = getUserByNickname(nickname, server);
        super.remoteUserQuitsIRC(nickname, host, username, reason, server);
        onOtherQuit(nickname, host, username, reason, server);
    }

    public void receivedUserList(Channel channel) {
        super.receivedUserList(channel);
        String chanid = new Character(channel.getScope()).toString() + channel.getName();
        User[] userlist = getChannelUserWireCollection().getUserArrayInChannel(channel);
        onUserList(chanid, userlist, channel.getServer());
    }

    public void receivedInvite(String nickname, String host, String username, String channel, IRCServer ircServer) {
        onInvite(nickname, host, username, channel, ircServer);
    }

    @Override
    public void processNotice(String from, String target, String message, IRCServer ircServer) {
    }
}
