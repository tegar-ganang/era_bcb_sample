package net.sourceforge.projectss;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

/**
 * This class is an implementation of Net interface.
 * It connects to Jabber servers.
 */
public class Jabber implements Net {

    XMPPConnection connection;

    UI program;

    List<User> users;

    ChatListener chats;

    Roster roster;

    public void chatClosed(String from) {
        chats.chatEnded(from);
        User u = User.find(users, from);
        if (u == null) {
            DEBUG.error("Jabber: chatClosed: cannot find user " + from);
            return;
        }
        program.chatClosed(this, u);
    }

    public void closeChat(User user) {
        chats.chatEnded(user.fullName);
    }

    public User createGroup(String name) {
        DEBUG.println("Jabber: createGroup: group chats not implemented in jabber part yet");
        return null;
    }

    public void inviteToGroup(User group, User user) {
        DEBUG.println("Jabber: inviteToGroup: group chats not implemented in jabber part yet");
    }

    public List<User> listGroupUsers(User group) {
        DEBUG.println("Jabber: listGroupUsers: group chats not implemented in jabber part yet");
        return null;
    }

    public void receiveMessage(String from, String data) {
        User u = User.find(users, from);
        if (u == null) {
            DEBUG.warn("Jabber: receiveMessage: received message from unknown user: " + from);
            DEBUG.warn("Jabber: receiveMessage: msg follow: " + data);
        } else {
            program.newMessage(this, User.find(users, from), User.findMyself(users), data);
        }
    }

    public void removeFromGroup(User group, User name) {
        DEBUG.println("Jabber: removeFromGroup: group chats not implemented in jabber part yet");
    }

    public void removeGroup(User groupName) {
        DEBUG.println("Jabber: removeGroup: group chats not implemented in jabber part yet");
    }

    public void sendMessage(User to, String msg) {
        if (!to.realUser) return;
        Chat c = chats.getChat(to.fullName);
        if (c == null) {
            c = chats.chatBegin(to.fullName);
        }
        try {
            c.sendMessage(msg);
        } catch (XMPPException e) {
            DEBUG.warn("Jabber: sendMessage: cannot send message to " + to);
        }
    }

    public void setStatus(int availability, String newStatus) {
    }

    public boolean stop() {
        connection.disconnect();
        return true;
    }

    /**
	 * Method is used by other classes (listeners) when new user joins roster 
	 * @param user JID of a user that joined
	 */
    public void userJoined(String user) {
        RosterEntry e = roster.getEntry(user);
        Presence p = roster.getPresence(user);
        if (User.find(users, user) == null) {
            String status = p.getStatus();
            if (status == null) status = new String();
            Presence.Mode mode = p.getMode();
            int avail;
            switch(mode) {
                case available:
                case chat:
                    avail = 3;
                    break;
                case away:
                case dnd:
                case xa:
                    avail = 2;
                    break;
                default:
                    avail = 0;
            }
            User u = new User(e.getName(), user, avail, status);
            synchronized (users) {
                users.add(u);
            }
            program.updateUsers(this);
        } else {
            DEBUG.warn("Jabber: userJoined: user " + user + " already exists");
        }
    }

    /**
	 * Method invoked by other classes (listeners) when user left roster
	 * @param user JID of a user who left
	 */
    public void userLeft(String user) {
        User u = User.find(users, user);
        if (u == null) {
            DEBUG.warn("Jabber: userLeft: cannot find user " + user);
        } else {
            synchronized (users) {
                users.remove(u);
            }
            program.updateUsers(this);
        }
    }

    /**
	 * Method invoked by other classes (listeners) when remote user changes status
	 * @param user JID of a user
	 */
    public void userStatus(String user) {
        user = user.substring(0, user.indexOf("/"));
        RosterEntry e = roster.getEntry(user);
        System.out.println(user);
        System.out.println(e);
        Presence p = roster.getPresence(user);
        String status = p.getStatus();
        System.out.println("p " + p);
        System.out.println(status);
        user = e.getUser();
        User u;
        if (status == null) status = new String();
        Presence.Mode mode = p.getMode();
        if ((u = User.find(users, user)) != null) {
            int avail;
            if (mode == null) mode = Presence.Mode.away;
            switch(mode) {
                case available:
                case chat:
                    avail = 3;
                    break;
                case away:
                case dnd:
                case xa:
                    avail = 2;
                    break;
                default:
                    avail = 0;
            }
            u = new User(e.getName(), user, avail, status);
            synchronized (users) {
                users.add(u);
            }
            program.updateUsers(this);
        } else {
            DEBUG.warn("Jabber: userStatus: cannot find user " + user);
        }
    }

    /**
	 * Classes constructor
	 * @param program Reference to UI interface that will be informed on new events
	 * @param myName JID of a local user
	 * @param myPass password for connection of a user "myName"
	 * @param users List of a users that this class should keep updated
	 */
    public Jabber(UI program, String myName, String myPass, List<User> users) throws XMPPException {
        User me, u;
        this.program = program;
        me = new User(myName.substring(0, myName.indexOf('@')), myName, 0, "");
        me.myself = true;
        users.add(me);
        u = new User("*", "*@jabber", 0, "");
        u.realUser = false;
        users.add(u);
        this.users = users;
        try {
            connection = new XMPPConnection(new ConnectionConfiguration(myName.substring(myName.indexOf('@') + 1), 5222));
            connection.connect();
            connection.login(myName.substring(0, myName.indexOf('@')), myPass);
        } catch (XMPPException e) {
            DEBUG.error("Jabber: Jabber: cannot connect or authenticate to jabber server (wrong pass?");
            throw e;
        }
        chats = new ChatListener(this, connection.getChatManager());
        roster = connection.getRoster();
        if (roster != null) {
            roster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
            Collection<RosterEntry> e = roster.getEntries();
            for (RosterEntry r : e) {
                String user = r.getUser();
                String name = r.getName();
                if (name == null) name = user.substring(0, user.indexOf('@'));
                Presence p = roster.getPresence(user);
                if (User.find(users, user) == null) {
                    String status = p.getStatus();
                    if (status == null) status = new String();
                    Presence.Mode mode = p.getMode();
                    int avail;
                    if (mode != null) {
                        switch(mode) {
                            case available:
                            case chat:
                                avail = 3;
                                break;
                            case away:
                            case dnd:
                            case xa:
                                avail = 2;
                                break;
                            default:
                                avail = 0;
                        }
                    } else avail = 0;
                    u = new User(name, user, avail, status);
                    synchronized (users) {
                        users.add(u);
                    }
                } else {
                    DEBUG.warn("Jabber: userJoined: user " + user + " already exists");
                }
            }
            roster.addRosterListener(new RostListener(this));
            program.updateUsers(this);
        } else {
            DEBUG.error("Jabber: Jabber: No roster could be bound");
        }
        DEBUG.println("Jabber: Jabber: initialized");
    }
}

/**
 * Class that listens for new chats
 */
class ChatListener implements ChatManagerListener {

    Net prog;

    Map<String, MsgListener> listeners;

    ChatManager chats;

    public void chatCreated(Chat chat, boolean local) {
        if (!local) {
            MsgListener listener = new MsgListener(prog, chat);
            listeners.put(chat.getParticipant(), listener);
            chat.addMessageListener(listener);
        }
    }

    public void chatEnded(String participant) {
        MsgListener m = listeners.get(participant);
        if (m == null) {
            DEBUG.warn("ChatListener: chatEnded: cannot find listener for given participant");
        } else {
            m.myChat.removeMessageListener(m);
        }
    }

    public Chat chatBegin(String user) {
        MsgListener lis = new MsgListener(prog, null);
        Chat c = chats.createChat(user, lis);
        lis.myChat = c;
        listeners.put(user, lis);
        return c;
    }

    public Chat getChat(String name) {
        MsgListener l = listeners.get(name);
        if (l == null) return null; else return l.myChat;
    }

    ChatListener(Net prog, ChatManager chats) {
        this.prog = prog;
        this.chats = chats;
        listeners = new HashMap<String, MsgListener>();
        chats.addChatListener(this);
    }
}

/**
 * Class that listens for new messages on already created chats
 */
class MsgListener implements MessageListener {

    private Net prog;

    Chat myChat;

    public void processMessage(Chat chat, Message msg) {
        prog.receiveMessage(chat.getParticipant(), msg.getBody());
    }

    MsgListener(Net prog, Chat myChat) {
        this.prog = prog;
        this.myChat = myChat;
    }
}

/**
 * Class that listens for changes in roster (user list)
 */
class RostListener implements RosterListener {

    Jabber prog;

    public void entriesAdded(Collection<String> arg0) {
        for (String s : arg0) {
            prog.userJoined(s);
        }
    }

    public void entriesDeleted(Collection<String> arg0) {
        for (String s : arg0) {
            prog.userLeft(s);
        }
    }

    public void entriesUpdated(Collection<String> arg0) {
        for (String s : arg0) {
            prog.userStatus(s);
        }
    }

    public void presenceChanged(Presence arg0) {
        prog.userStatus(arg0.getFrom());
    }

    RostListener(Jabber prog) {
        this.prog = prog;
    }
}
