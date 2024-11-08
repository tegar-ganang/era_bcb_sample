package genericirc.irccore;

import java.net.UnknownHostException;
import java.util.HashMap;
import javax.swing.event.*;

/**
 * This class provides a simple way to manage communications with multiple IRC
 * channels. It handles starting new channels and automatically creating new
 * channels when another user starts a query.
 * @author Steve "Uru" West <uruwolf@users.sourceforge.net>
 * @version 2011-07-28
 */
public class ChannelManager {

    private IRCCore core;

    private EventListenerList listenerList;

    private HashMap<String, Channel> channelList;

    private String serverName;

    /**
     * Creates a new ChannelManager that uses the given IRCCore for 
     * communication.
     * @param core 
     */
    public ChannelManager(IRCCore core) {
        this.core = core;
        channelList = new HashMap<String, Channel>();
        listenerList = new EventListenerList();
        serverName = core.getHost();
    }

    /**
     * Creates a new channel manager that creates it's own IRCCore. I would
     * recommend <b>NOT</b> doing this however as it removes allot of the
     * control that IRCCore provides. ChannelManager should only be used to
     * manage the channels for an existing IRCCore. This constructor may well
     * be removed in the future or simply an accessor for core added.
     * @see IRCCore#IRCCore(java.lang.String, java.lang.String, java.lang.String, int)
     * @throws UnknownHostException 
     */
    public ChannelManager(String server, String user, String realName, int port) throws UnknownHostException {
        this(new IRCCore(server, user, realName, port));
    }

    /**
     * Takes an incoming message and sends is to the corresponding channel
     * or the status/server channel
     * @param message 
     */
    private void processNewMessage(Message message) {
        String[] msg = message.getMessage().split(" ");
        if (msg.length >= 2 && msg[1].equalsIgnoreCase("privmsg")) {
            Message processed = CommandProcessor.parseMessage(message.getMessage());
            sendMessageToChannel(processed.getChannel(), processed);
            return;
        }
        sendMessageToChannel(serverName, message);
    }

    /**
     * Sends the given message to the given channel
     * @param channelName
     * @param message 
     */
    private void sendMessageToChannel(String channelName, Message message) {
        ensureChannelExists(channelName);
        channelList.get(channelName).giveMessage(this, message);
    }

    /**
     * Creates a new Channel if it does not already exist then fires the
     * channel created event
     * @param name 
     */
    private void ensureChannelExists(String name) {
        if (channelList.get(name) == null) {
            Channel newChannel = new Channel(name);
            channelList.put(name, newChannel);
            fireChannelCreatedEvent(new ChannelCreatedEvent(this, newChannel));
        }
    }

    /**
     * Adds the given ChannelCreatedListener that is called when a new Channel
     * is created.
     * @param ccl 
     */
    public void addChannelCreatedListener(ChannelCreatedListener ccl) {
        listenerList.add(ChannelCreatedListener.class, ccl);
    }

    /**
     * Removes the given ChannelCreatedListener
     * @param ccl 
     */
    public void removeChannelCreatedListener(ChannelCreatedListener ccl) {
        listenerList.remove(ChannelCreatedListener.class, ccl);
    }

    /**
     * Triggers all listening listeners with the given ChannelCreatedEvent
     * @param cce 
     */
    protected void fireChannelCreatedEvent(ChannelCreatedEvent cce) {
        ChannelCreatedListener[] list = listenerList.getListeners(ChannelCreatedListener.class);
        for (int i = 0; i < list.length; i++) {
            list[i].channelCreated(cce);
        }
    }
}
