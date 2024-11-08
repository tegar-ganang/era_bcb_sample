package genericirc.irccore;

import java.net.UnknownHostException;
import java.util.*;
import javax.swing.event.*;

/**
 * Provides a layer between the IRC communication and the program via a set of
 * methods. Unknown messages will be sent to a status channel that will be 
 * named after the server url that this IRCCore is connected to
 * @author Steve "Uru" West <uruwolf@users.sourceforge.net>
 * @version 2011-07-29
 */
public class IRCCore {

    private String server;

    private String user;

    private int port;

    private IO io;

    private String realName;

    protected EventListenerList listenerList;

    private HashMap<String, ArrayList<User>> tempUserLists;

    /**
     * Creates a new IRCCore object for communicating with IRC servers
     * @param server The server to connect to (eg: irc.freenode.net)
     * @param user The user name to use
     * @param realName The "real" name to use
     * @param port Which port to connect on
     * @throws UnknownHostException will be thrown if it is not possible to
     * reach the given server
     */
    public IRCCore(String server, String user, String realName, int port) throws UnknownHostException {
        this.server = server;
        this.user = user;
        this.port = port;
        this.realName = realName;
        tempUserLists = new HashMap<String, ArrayList<User>>();
        listenerList = new EventListenerList();
        if (this.realName == null || this.realName.equals("")) {
            realName = "genericIRC";
        }
        io = new IO(server, port) {

            @Override
            void messageGot(String message) {
                processMessage(message);
            }
        };
        io.addOutput("NICK " + user + "\r\n");
        io.addOutput("USER gIRC localhost * :" + realName + "\r\n");
        io.sendOutputBuffer();
    }

    /**
     * Processes the incoming message and calls the listener(s). If the message
     * is not known then it will be dumped out to a "status" channel.
     * @param message The raw irc message to process
     */
    private void processMessage(String message) {
        String[] msg = message.split(" ");
        if (message.toLowerCase().startsWith("ping")) {
            firePingEvent(new PingEvent(this, message.substring(6)));
        } else if (msg[1].equals("332") || msg[1].toLowerCase().equals("topic")) {
            fireTopicUpdatedEvent(new TopicUpdatedEvent(this, message.substring(message.indexOf(" :") + 2), msg[3]));
        } else if (msg[1].equals("353")) {
            String[] userList = message.substring(message.lastIndexOf(" :") + 2).split(" ");
            for (String u : userList) {
                addUserToList(msg[4], new User(u, ""));
            }
        } else if (msg[1].equals("352")) {
            addUserToList(msg[3], new User(msg[msg.length - 1], ""));
        } else if (msg[1].equals("315") || msg[1].equals("366")) {
            String channel = msg[3];
            fireUserListEvent(new UserListEvent(this, channel, tempUserLists.get(channel)));
            tempUserLists.remove(channel);
        } else {
            fireNewMessageEvent(new NewMessageEvent(this, "", "", getHost(), message));
        }
    }

    /**
     * Adds the given user to the given channel's user list
     * @param channel
     * @param user 
     */
    private void addUserToList(String channel, User user) {
        ArrayList<User> list = tempUserLists.get(channel);
        if (list != null) {
            list.add(user);
        }
    }

    /**
     * Adds a IRCEventsListener that can be notified of IRC events
     * @param iel 
     */
    public void addIRCEventsListener(IRCEventsListener iel) {
        listenerList.add(IRCEventsListener.class, iel);
    }

    /**
     * Removes an IRCEvents listener. After the listener is removed it will
     * no longer receive updates from this IRCCore object
     * @param iel 
     */
    public void removeIRCEventsListener(IRCEventsListener iel) {
        listenerList.remove(IRCEventsListener.class, iel);
    }

    /**
     * Fires the {@link IRCEventsListener#ping(genericirc.irccore.PingEvent)} event
     * @param pe 
     */
    protected void firePingEvent(PingEvent pe) {
        IRCEventsListener[] list = listenerList.getListeners(IRCEventsListener.class);
        for (IRCEventsListener l : list) {
            l.ping(pe);
        }
    }

    /**
     * Fires the {@link IRCEventsListener#userList(genericirc.irccore.UserListEvent)} event
     * @param ule 
     */
    protected void fireUserListEvent(UserListEvent ule) {
        IRCEventsListener[] list = listenerList.getListeners(IRCEventsListener.class);
        for (IRCEventsListener l : list) {
            l.userList(ule);
        }
    }

    /**
     * Fires the {@link IRCEventsListener#topicUpdated(genericirc.irccore.TopicUpdatedEvent)} event
     * @param tue 
     */
    protected void fireTopicUpdatedEvent(TopicUpdatedEvent tue) {
        IRCEventsListener[] list = listenerList.getListeners(IRCEventsListener.class);
        for (IRCEventsListener l : list) {
            l.topicUpdated(tue);
        }
    }

    /**
     * Fires the {@link IRCEventsListener#newMessage(genericirc.irccore.NewMessageEvent)} event
     * @param nme 
     */
    protected void fireNewMessageEvent(NewMessageEvent nme) {
        IRCEventsListener[] list = listenerList.getListeners(IRCEventsListener.class);
        for (IRCEventsListener l : list) {
            l.newMessage(nme);
        }
    }

    /**
     * This will send a message to the server that the IO is connected to.
     * @param toSend
     */
    public void sendMessage(Message toSend) {
        io.addOutput("PRIVMSG " + toSend.getChannel() + " :" + toSend.getMessage() + "\r\n");
        io.sendOutputBuffer();
    }

    /**
     * Called when you want to join a channel.
     * @param channel
     */
    public void joinChannel(String channel) {
        io.addOutput("JOIN " + channel + "\r\n");
        tempUserLists.put(channel, new ArrayList<User>());
        io.sendOutputBuffer();
    }

    /**
     * Performs a WHO query on the given channel
     * @param channel The channel to query
     */
    public void who(String channel) {
        io.addOutput("WHO " + channel + "\r\n");
        tempUserLists.put(channel, new ArrayList<User>());
        io.sendOutputBuffer();
    }

    /**
     * Performs a NAMES query on the given channel
     * @param channel 
     */
    public void names(String channel) {
        io.addOutput("NAMES " + channel + "\r\n");
        tempUserLists.put(channel, new ArrayList<User>());
        io.sendOutputBuffer();
    }

    /**
     * Parts from a channel
     * @param channel
     */
    public void partChannel(String channel) {
        io.addOutput("PART " + channel + "\r\n");
        io.sendOutputBuffer();
    }

    /**
     * @return the host
     */
    public String getHost() {
        return server;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * Used to send raw commands using the IRC protocol. This will be removed
     */
    public void sendRawIRC(String msg) {
        System.out.println(msg);
        io.addOutput(msg + "\r\n");
        io.sendOutputBuffer();
    }
}
