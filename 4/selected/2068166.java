package de.boardgamesonline.bgo2.webserver.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;

/**
 * Chat channels for inter-user communication &mdash;
 * before starting a game and in-game,
 * to the discretion of the played game.
 * <p>
 * A User being a channel's member currently means that he is allowed
 * to post messages to the channel. As a special case, when the member list
 * is <code>null</code>, every user is allowed to post. (This is different
 * from an empty list, which wouldn't allow anyone to post.)
 * <p>
 * It is suggested that code using this class also checks a user's membership for
 * determining reading permissions, i.e., before passing channel messages
 * on to him. This can easily be done using {@link #isMember(User)}.
 * <p>
 * This class is part of a larger "framework" for providing chat service
 * to Boardgames Online 2:
 * <ul>
 * <li>The ChatChannel backend. Channels for the lobby and each game
 *     are created and stored here. The {@link Game} class initiates
 *     game channel creation and also logs information for the user
 *     as well as status changes there.</li>
 * <li>A {@link de.boardgamesonline.bgo2.webserver.wicket.util.ChatPanel
 *     Wicket frontend}.
 *     This is used for the lobby, a global channel in which all logged-in
 *     users can talk and agree upon playing a game.</li>
 * <li>A {@link de.boardgamesonline.bgo2.webserver.axis.ChatManager WebService}
 *     allowing the client-side part of the games
 *     to post messages and retrieve posted messages, without being
 *     able to create new channels or modify channel membership.</li>
 * <li>A shared, reusable, graphical component implementing
 *     the client side of the chat WebService defined above.
 *     This is <code>de.boardgamesonline.bgo2.shared.chat.ChatPanel</code>,
 *     in bgoShared.</li>    
 * </ul>
 * 
 * @author Peter Lohmann (initial skeleton), Fabian Pietsch
 * 
 */
public final class ChatChannel extends Observable {

    /** Prefix for game channels, i.e. associated with a {@link Game}. */
    public static final String GAME_CHAN_PREFIX = "Game/";

    /** Prefix for user channels, i.e. private messages for that user. */
    public static final String USER_CHAN_PREFIX = "User/";

    /**
	 * The fewton holder.
	 */
    private static final Map<String, ChatChannel> CHANNELS = new HashMap<String, ChatChannel>();

    /**
	 * Creates a new ChatChannel.
	 *  
	 * @param channelID Channel ID of new channel.
	 * @param members   Initial list of users that are allowed
	 *                  to post to the channel.
	 * @return The new channel, or <code>null</code> if a channel
	 *         with this <code>channelID</code> already exists.
	 */
    public static synchronized ChatChannel createChannel(String channelID, Collection<User> members) {
        if (CHANNELS.containsKey(channelID)) {
            return null;
        }
        ChatChannel channel = new ChatChannel(channelID, members);
        CHANNELS.put(channelID, channel);
        return channel;
    }

    /**
	 * Removes a ChatChannel from the list of active channels.
	 * The ChatChannel object should not be used anymore afterwards,
	 * and a new channel with the same <code>channelID</code> may be created.
	 *   
	 * @param channelID ID of channel to remove.
	 * @return <code>true</code> if there was such a channel;
	 *         <code>false</code> otherwise.
	 */
    public static synchronized boolean removeChannel(String channelID) {
        if (!CHANNELS.containsKey(channelID)) {
            return false;
        }
        CHANNELS.remove(channelID);
        return true;
    }

    /**
	 * Removes a ChatChannel from the list of active channels.
	 * 
	 * @param channel The channel to remove.
	 * @return <code>true</code> if there was such a channel;
	 *         <code>false</code> otherwise.
	 *         
	 * @see #removeChannel(String)
	 */
    public static synchronized boolean removeChannel(ChatChannel channel) {
        return removeChannel(channel.getID());
    }

    /**
	 * Returns the ChatChannel for a specific <code>channelID</code>.
	 * 
	 * @param channelID The ID of the desired channel;
	 *   <code>null</code> addresses the lobby.
	 *     
	 * @return The associated ChatChannel object,
	 *         or <code>null</code> if there is no such object.   
	 */
    public static synchronized ChatChannel getChannel(String channelID) {
        if (channelID == null) {
            if (!CHANNELS.containsKey(null)) {
                return createChannel(null, null);
            }
        }
        return CHANNELS.get(channelID);
    }

    /**
	 * Indication on what has changed,
	 * passed to {@link java.util.Observer#update(Observable, Object)}. 
	 */
    public enum Changed {

        /** Members have changed. Currently only on setMembers(). */
        MEMBERS, /** Messages have changed. New messages may have been posted. */
        MESSAGES
    }

    /**
	 * The <code>channelID</code> of this ChatChannel,
	 * as used in {@link #getChannel(String)}.
	 */
    private final String id;

    /**
	 * The list of channel members, i.e., users that are allowed to post
	 * messages to this channel.
	 */
    private Collection<User> members;

    private int messagesBaseIndex = 0;

    private List<ChatMessage> messages = new ArrayList<ChatMessage>();

    /**
	 * Private constructor to ensure fewton pattern.
	 * 
	 * @param channelID The new channel's ID as used as key
	 *     in {@link #getChannel(String)}.
	 * @param members Initial list of channel members, i.e.,
	 *     users allowed to post.
	 */
    private ChatChannel(String channelID, Collection<User> members) {
        this.id = channelID;
        this.members = members;
        postMessage(getName() + " created");
    }

    /**
	 * Determines if a User is a member of the channel.
	 * 
	 * @param user The user to check against the member list.
	 * @return <code>true</code> if the User currently is a channel member,
	 *         <code>false</code> otherwise.
	 */
    public boolean isMember(User user) {
        if (user == null) {
            return false;
        }
        return (members == null) || members.contains(user);
    }

    /**
	 * Gets the current channel members, i.e., users allowed to post.
	 * 
	 * @return
	 *     The current list of channel members.
	 *     No attempts are made to prevent direct modification of this list,
	 *     but it may be unmodifiable for some other reason.
	 */
    public Collection<User> getMembers() {
        return members;
    }

    /**
	 * Sets new channel members, i.e., users allowed to post,
	 * replacing the previous ones.
	 * 
	 * @param members
	 *     The new list of channel members. As a special case,
	 *     <code>null</code> means every user is a member / allowed to post.
	 *     <p>
	 *     The list is used directly (no copy is made),
	 *     therefore changing the list externally will automagically change
	 *     the members of the channel. Hand in a copy if this is undesired.
	 *     <p>
	 *     You may also want to use {@link Collections#unmodifiableList(List)}
	 *     to prevent accidental modifications of, e.g., the list of all
	 *     players in a game, when just a channel's list of members
	 *     should have been changed.
	 * @return The old list of channel members.
	 */
    public Collection<User> setMembers(Collection<User> members) {
        Collection<User> oldMembers = this.members;
        this.members = members;
        setChanged();
        notifyObservers(Changed.MEMBERS);
        return oldMembers;
    }

    /**
	 * Gets the <code>channelID</code> of this ChatChannel,
	 * as used in {@link #getChannel(String)}.
	 * 
	 * @return The channelID.
	 */
    public String getID() {
        return id;
    }

    /**
	 * Gets a user-readable representation of the <code>channelID</code>'s
	 * category. This used to include the game's/user's name, but was dropped
	 * because of session IDs and circularity. The creating component should
	 * log details itself now, if appropriate.
	 * 
	 * @return The user-readable name of the channel.
	 */
    public String getName() {
        if (id == null) {
            return "Lobby";
        } else if (id.startsWith(GAME_CHAN_PREFIX)) {
            return "Game channel";
        } else if (id.startsWith(USER_CHAN_PREFIX)) {
            return "Private messages list";
        } else {
            return id;
        }
    }

    /**
	 * Returns a human-readable string representation of the Channel.
	 * 
	 * @return A String of the form "ChatChannel{CHANNELID}".
	 */
    public String toString() {
        return "ChatChannel{" + (id == null ? "null" : id) + "}";
    }

    /**
	 * Gets all messages posted to this channel.
	 * Maybe except for some old ones that may have been deleted
	 * as a means of manual garbage collection.
	 * <p>
	 * This method returns a copy of the internal messages list,
	 * i.e., it may be modified and won't change by itself.
	 * 
	 * @return A copy of the current messages list.
	 * @see #getMessages()
	 */
    public List<ChatMessage> getAllMessages() {
        return new ArrayList<ChatMessage>(messages);
    }

    /**
	 * Gets new messages posted to this channel since the client last checked.
	 *  
	 * @param firstIndex
	 *     Zero-based index of first message to be retrieved.
	 *     A client maintaining a full list of past messages
	 *     in a <code>List l</code> may simply use
	 *     <code>l.size()</code> for this parameter.
	 * @return A copy of a sublist of the current messages list,
	 *         starting at <code>firstIndex</code>. 
	 */
    public List<ChatMessage> getNewMessages(int firstIndex) {
        firstIndex -= messagesBaseIndex;
        int lastIndex = messages.size();
        return new ArrayList<ChatMessage>(messages.subList(firstIndex, lastIndex));
    }

    /**
	 * Gets an <em>unmodifiable</em> version of the internal messages list.
	 * That means it changes on new posts via the ChatChannel object,
	 * but can't be changed externally. 
	 * 
	 * @return The unmodifiable list of messages.
	 * @see #getAllMessages()
	 */
    public List<ChatMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    /**
	 * Post a message to the channel.
	 * 
	 * @param message The message to post.
	 * @return On success, the ChatMessage that was added to the list,
	 *     possibly different from <code>message</code>.
	 *     <code>null</code> otherwise.
	 */
    private ChatMessage postMessage(ChatMessage message) {
        if (message == null) {
            return null;
        }
        messages.add(message);
        setChanged();
        notifyObservers(Changed.MESSAGES);
        return message;
    }

    /**
	 * Post a message to the channel on behalf of a user.
	 * This includes checking whether the user is a member of the channel
	 * (and therefore allowed to post) and associating the message
	 * with the user's name and the time of processing at the server.
	 * 
	 * @param user    The user trying to post the message.
	 * @param message The message to post.
	 * @return On success, the {@link ChatMessage} created and added
	 *         to the list of messages. <code>null</code> otherwise,
	 *         e.g., if the user is not a member of the channel.
	 */
    public ChatMessage postMessage(User user, String message) {
        if (!isMember(user)) {
            return null;
        }
        return postMessage(new ChatMessage(user, message));
    }

    /**
	 * Post a message to the channel on behalf of the system.
	 * The message will have the current time as time field
	 * and the nick field will be null.
	 *  
	 * @param message The message to post.
	 * @return On success, the {@link ChatMessage} created and added
	 *         to the list of messages. <code>null</code> otherwise.
	 */
    public ChatMessage postMessage(String message) {
        return postMessage(new ChatMessage(new Date(), null, message));
    }
}
