package org.jnerve;

import java.util.*;
import java.io.*;
import org.jnerve.message.*;
import org.jnerve.session.*;
import org.jnerve.session.event.*;
import org.jnerve.util.*;

/** Manages channels server-wide.
  *
  * Keeps redundant info on channel membership,
  * for efficiency's sake. 
  *
  * Calling code should use addUserToChannel and removeUserFromChannel,
  * but should never call addMember, removeMember in the Channel objects,
  * or horrible things will happen.
  */
public class ChannelManager implements SessionEventListener {

    private Vector channels = new Vector();

    private Hashtable users = new Hashtable();

    private ServerFacilities serverFacilities;

    public static final int ACTION_JOIN = 0;

    public static final int ACTION_LEAVE = 1;

    public ChannelManager(ServerFacilities serverFacilities) {
        this.serverFacilities = serverFacilities;
        initChannelsFile(serverFacilities.getJNerveConfiguration().getChannelsFilePath());
    }

    public ServerFacilities getServerFacilities() {
        return serverFacilities;
    }

    /** Sets up permanent channels */
    public void initChannelsFile(String filename) {
        if (filename == null) return;
        FileReader freader = null;
        BufferedReader reader = null;
        try {
            freader = new FileReader(filename);
            reader = new BufferedReader(freader);
        } catch (FileNotFoundException fnfe) {
            Logger.getInstance().log(Logger.WARNING, "Could not load channels file [" + filename + "]");
        }
        try {
            if (reader != null) {
                String line = reader.readLine();
                while (line != null) {
                    line = line.trim();
                    if (line.indexOf("#") != 0 && line.length() > 0) {
                        Message m = new Message(0, line);
                        String name = m.getDataString(0);
                        String limit = m.getDataString(1);
                        String level = m.getDataString(2);
                        if (name.indexOf(" ") == -1 && isValidLevel(level)) {
                            Channel c = new Channel();
                            c.setName(name);
                            c.setTopic("Welcome to the " + name + " channel");
                            c.setLimit(Integer.valueOf(limit).intValue());
                            c.setLevel(level);
                            c.setPermanent(true);
                            addChannel(c);
                        } else {
                            Logger.getInstance().log(Logger.WARNING, "Ignoring invalid line in channels file: " + line);
                        }
                    }
                    line = reader.readLine();
                }
            }
            reader.close();
            freader.close();
        } catch (Exception ioe) {
            Logger.getInstance().log(Logger.WARNING, "Error reading channels file:" + ioe.toString());
        }
    }

    /** Adds a user to a channel (join) */
    public void addUserToChannel(String channel, String user) {
        Channel c = getChannel(channel);
        if (c == null) {
            c = new Channel();
            addChannel(c);
            c.setName(channel);
            c.setTopic("Welcome to the " + channel + " channel");
        }
        c.addMember(user);
        Vector v = (Vector) users.get(user);
        if (v == null) {
            v = new Vector();
            users.put(user, v);
        }
        v.addElement(channel);
    }

    public static final int USER_INITIATED = 0;

    public static final int NON_USER_INITIATED = 1;

    /** sends channel user list to session s 
	  * type specifies either USER_INITIATED or NON_USER_INITIATED
	  */
    public void sendChannelList(Session s, String channel, int type) {
        int entryMsgType = MessageTypes.SERVER_CHANNEL_USER_LIST_ENTRY;
        int endListMsgType = MessageTypes.SERVER_CHANNEL_USER_LIST_END;
        if (type == USER_INITIATED) {
            entryMsgType = MessageTypes.SERVER_CHANNEL_USER_LIST_ENTRY_2;
            endListMsgType = MessageTypes.CLIENT_CHANNEL_USER_LIST;
        }
        Channel c = getChannel(channel);
        String[] members = c.getMembers();
        if (members != null) {
            OutboundMessageQueue queue = s.getOutboundMessageQueue();
            for (int x = 0; x < members.length; x++) {
                StringBuffer msg = new StringBuffer(64);
                String singleMember = members[x];
                UserState us = serverFacilities.searchForUserState(singleMember);
                if (us != null) {
                    int numShares = us.getShareCount();
                    int linkType = us.getLinkType();
                    msg.append(channel);
                    msg.append(" ");
                    msg.append(singleMember);
                    msg.append(" ");
                    msg.append(numShares);
                    msg.append(" ");
                    msg.append(linkType);
                    try {
                        queue.queueMessage(new Message(entryMsgType, msg.toString()));
                    } catch (InvalidatedQueueException iqe) {
                    }
                }
            }
            try {
                queue.queueMessage(new Message(endListMsgType, channel));
            } catch (InvalidatedQueueException iqe) {
            }
        }
    }

    /** Sends msgs to all users in channel, that user has joined/left. */
    public void notifyChannelOfUserAction(int actionType, String channel, String user) {
        int msgType = -1;
        switch(actionType) {
            case ACTION_JOIN:
                {
                    msgType = MessageTypes.SERVER_CHANNEL_JOIN_NOTIFY;
                    break;
                }
            case ACTION_LEAVE:
                {
                    msgType = MessageTypes.SERVER_CHANNEL_LEAVE_NOTIFY;
                    break;
                }
        }
        UserState us = serverFacilities.searchForUserState(user);
        int numShares = us.getShareCount();
        int linkType = us.getLinkType();
        StringBuffer msg = new StringBuffer(64);
        msg.append(channel);
        msg.append(" ");
        msg.append(user);
        msg.append(" ");
        msg.append(numShares);
        msg.append(" ");
        msg.append(linkType);
        sendMessage(channel, new Message(msgType, msg.toString()));
    }

    /** Removes a user from a channel */
    public void removeUserFromChannel(String channel, String user) {
        Channel c = getChannel(channel);
        if (c != null) {
            c.removeMember(user);
            if (c.numMembers() == 0 && !c.isPermanent()) {
                removeChannel(c);
            }
        }
        Vector v = (Vector) users.get(user);
        if (v != null) {
            v.removeElement(channel);
        }
    }

    /** Sends a public msg to channel, from user */
    public void sendMessage(String channel, String user, String msg) {
        Message msgToSend = new Message(MessageTypes.SERVER_CHANNEL_PUBLIC_MESSAGE, channel + " " + user + " " + msg);
        sendMessage(channel, msgToSend);
    }

    /** variation of sendMessage(), takes a preconstructed msg and broadcasts to channel */
    public void sendMessage(String channel, Message msg) {
        ServerFacilities serverFacilities = getServerFacilities();
        Channel c = getChannel(channel);
        if (c != null) {
            String[] members = c.getMembers();
            if (members != null) {
                int numMembers = members.length;
                for (int x = 0; x < numMembers; x++) {
                    Session targetSession = serverFacilities.searchForSession(members[x]);
                    if (targetSession != null) {
                        OutboundMessageQueue queue = targetSession.getOutboundMessageQueue();
                        try {
                            queue.queueMessage(msg);
                        } catch (InvalidatedQueueException iqe) {
                        }
                    }
                }
            }
        }
    }

    /** Returns array of channels that user is currently a member of. */
    public Channel[] getChannelsForUser(String user) {
        Vector v = (Vector) users.get(user);
        if (v != null) {
            int numChannels = v.size();
            if (numChannels > 0) {
                Channel[] retArray = new Channel[numChannels];
                for (int x = 0; x < numChannels; x++) {
                    retArray[x] = getChannel((String) v.elementAt(x));
                }
                return retArray;
            }
        }
        return null;
    }

    /** Returns array of all channels in system. */
    public Channel[] getChannels() {
        Vector v = new Vector(channels.size());
        Enumeration channelEnum = channels.elements();
        while (channelEnum.hasMoreElements()) {
            Channel c = (Channel) channelEnum.nextElement();
            v.addElement(c);
        }
        int numChannels = v.size();
        if (numChannels > 0) {
            Channel[] retArray = new Channel[numChannels];
            for (int x = 0; x < numChannels; x++) {
                retArray[x] = (Channel) v.elementAt(x);
            }
            return retArray;
        }
        return null;
    }

    /** Returns the Channel object for given channel name */
    public Channel getChannel(String channel) {
        Enumeration channelEnum = channels.elements();
        while (channelEnum.hasMoreElements()) {
            Channel c = (Channel) channelEnum.nextElement();
            if (channel.equals(c.getName())) {
                return c;
            }
        }
        return null;
    }

    public void addChannel(Channel channel) {
        channels.addElement(channel);
    }

    public void removeChannel(Channel channel) {
        channels.removeElement(channel);
    }

    /** Returns true if channelName is at capacity */
    public boolean isAtCapacity(String channelName) {
        Channel c = getChannel(channelName);
        if (c != null) {
            if (c.getLimit() != 0 && c.numMembers() >= c.getLimit()) {
                return true;
            }
        }
        return false;
    }

    /** Listens for session terminate events; removes the user from all channels */
    public void processEvent(SessionEvent se) {
        if (se.getType() == SessionEvent.TERMINATE) {
            User u = se.getSession().getUserState().getUser();
            if (u != null) {
                String nickname = u.getNickname();
                Channel[] channels = getChannelsForUser(nickname);
                if (channels != null) {
                    for (int x = 0; x < channels.length; x++) {
                        String channelName = channels[x].getName();
                        removeUserFromChannel(channelName, nickname);
                        notifyChannelOfUserAction(ACTION_LEAVE, channelName, nickname);
                    }
                }
            }
        }
    }

    /** Checks a string to see if its a valid level */
    private static boolean isValidLevel(String level) {
        if (level.equalsIgnoreCase(DataDefinitions.UserLevelTypes.LEECH) || level.equalsIgnoreCase(DataDefinitions.UserLevelTypes.USER) || level.equalsIgnoreCase(DataDefinitions.UserLevelTypes.MODERATOR) || level.equalsIgnoreCase(DataDefinitions.UserLevelTypes.ELITE) || level.equalsIgnoreCase(DataDefinitions.UserLevelTypes.ADMIN)) {
            return true;
        }
        return false;
    }
}
