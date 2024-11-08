package webirc.client.gui.contactpanel;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import webirc.client.Channel;
import webirc.client.MainSystem;
import webirc.client.User;
import webirc.client.gui.sectionpanel.SectionPanel;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

/**
 * @author Ayzen
 * @version 1.0 08.07.2006 14:03:16
 */
public class ContactPanel extends SectionPanel {

    private ChannelsSection channelsSection = new ChannelsSection();

    private UsersSection usersSection = new UsersSection();

    /**
   * Entered channels with users information
   */
    private HashSet channels = new HashSet();

    /**
   * Selected channel
   */
    private Channel selectedChannel = null;

    private int addingUserIndex = 0;

    private Vector delayedEvents = new Vector();

    private Timer addingTimer = new Timer() {

        public void run() {
            selectChannelImpl();
        }
    };

    public ContactPanel() {
        super();
        addSection(channelsSection, "20%");
        addSection(usersSection, "80%");
    }

    public void userJoined(User user, Channel channel) {
        if (addingUserIndex != 0) {
            delayedEvents.add(new EventInfo(channel, user, EventInfo.TYPE_JOIN));
            return;
        }
        channel = getChannel(channel);
        if (channel != null) {
            int addedIndex = channel.addUser(user);
            if (channel.equals(selectedChannel) && addedIndex != -1) insertUser(user, addedIndex);
        } else {
        }
    }

    public void userLeft(User user, Channel channel) {
        if (addingUserIndex != 0) {
            delayedEvents.add(new EventInfo(channel, user, EventInfo.TYPE_LEFT));
            return;
        }
        channel = getChannel(channel);
        if (channel != null) {
            channel.removeUser(user);
            if (channel.equals(selectedChannel)) removeUser(user);
        } else {
        }
    }

    public Collection userQuit(User sender) {
        boolean allow = true;
        if (addingUserIndex != 0) {
            delayedEvents.add(new EventInfo(sender, EventInfo.TYPE_QUIT));
            allow = false;
        }
        Collection result = new Vector();
        for (Iterator itC = channels.iterator(); itC.hasNext(); ) {
            Channel channel = (Channel) itC.next();
            for (Iterator itU = channel.iterator(); itU.hasNext(); ) {
                User user = (User) itU.next();
                if (sender.equals(user)) {
                    result.add(channel);
                    if (allow) channel.removeUser(user);
                    if (channel.equals(selectedChannel) && allow) removeUser(user);
                    break;
                }
            }
        }
        return result;
    }

    /**
   * Changes user's nick and refreshes users section if that user is in the selected channel.
   *
   * @param sender the user that changes his/her nick
   * @param nick new nick
   */
    public Collection userChangesNick(User sender, String nick) {
        boolean allow = true;
        if (addingUserIndex != 0) {
            delayedEvents.add(new EventInfo(null, sender, nick, EventInfo.TYPE_NICK));
            allow = false;
        }
        Collection result = new Vector();
        for (Iterator itC = channels.iterator(); itC.hasNext(); ) {
            Channel channel = (Channel) itC.next();
            Vector users = (Vector) channel.getUsers();
            for (int i = 0; i < users.size(); i++) {
                User user = (User) users.get(i);
                if (sender.equals(user)) {
                    result.add(channel);
                    if (allow) {
                        user.setNickname(nick);
                        users.remove(i);
                        int index = channel.addUser(user);
                        if (channel.equals(selectedChannel)) {
                            removeUser(user);
                            insertUser(user, index);
                        }
                    }
                    break;
                }
            }
        }
        return result;
    }

    public void userModeChange(Channel channel, User user, char mode, boolean adding) {
        if (addingUserIndex != 0) {
            delayedEvents.add(new EventInfo(adding, channel, mode, null, user, EventInfo.TYPE_MODE));
            return;
        }
        channel = getChannel(channel);
        if (channel != null) {
            User thisUser = channel.getUser(user);
            if (adding) thisUser.addMode(mode); else thisUser.removeMode(mode);
            Vector users = (Vector) channel.getUsers();
            users.remove(thisUser);
            int index = channel.addUser(thisUser);
            if (channel.equals(selectedChannel)) {
                removeUser(thisUser);
                insertUser(thisUser, index);
            }
        } else {
        }
    }

    /**
   * Adds a channel to ContactPanel.
   *
   * @param channel entered channel
   */
    public void addChannel(Channel channel) {
        if (channels.add(channel)) {
            ChannelLine channelLine = new ChannelLine(channel);
            channelsSection.add(channelLine);
        } else MainSystem.showError("In addChannel channels.add couldn't add channel " + channel);
    }

    /**
   * Removes a channel from the ContactPanel.
   *
   * @param channel a channel user exited from
   */
    public void removeChannel(Channel channel) {
        channels.remove(channel);
        channelsSection.removeChannel(channel);
        if (channel.equals(selectedChannel)) {
            removeAllUsers();
            selectedChannel = null;
        }
    }

    public void removeAllChannels() {
        for (Iterator it = channels.iterator(); it.hasNext(); ) removeChannel((Channel) it.next());
    }

    /**
   * Exchanges old channel with new one.
   *
   * @param channel new channel
   */
    public void updateChannel(Channel channel) {
        boolean found = false;
        for (Iterator it = channels.iterator(); it.hasNext(); ) {
            Channel oldChannel = (Channel) it.next();
            if (oldChannel.equals(channel)) {
                oldChannel.removeAllUsers();
                oldChannel.setUsers(channel.getUsers());
                if (oldChannel.equals(selectedChannel)) selectChannelImpl();
                found = true;
                break;
            }
        }
        if (!found) MainSystem.showError("Channel " + channel + " is not found.");
    }

    /**
   * Adds users from specified channel to users section.
   *
   * @param channel the channel to select, might not have users information inside
   */
    public void selectChannel(Channel channel) {
        Channel enteredChannel = null;
        boolean found = false;
        for (Iterator it = channels.iterator(); it.hasNext(); ) {
            enteredChannel = (Channel) it.next();
            if (enteredChannel.equals(channel)) {
                found = true;
                break;
            }
        }
        if (found && !enteredChannel.equals(selectedChannel)) {
            addingUserIndex = 0;
            delayedEvents.clear();
            selectedChannel = enteredChannel;
            selectChannelImpl();
        }
    }

    /**
   * Implementation part of selectChannel method.
   *
   * @param channel the channel to be selected, must have information about users
   */
    private void selectChannelImpl() {
        if (addingUserIndex == 0) removeAllUsers();
        Vector users = (Vector) selectedChannel.getUsers();
        if (users != null) {
            usersSection.add(new UserLine((User) users.get(addingUserIndex++)));
            if (addingUserIndex < users.size()) addingTimer.schedule(1); else {
                addingUserIndex = 0;
                handleDelayedEvents();
            }
        }
    }

    /**
   * Searches in entered channels the <b>channel</b>.
   *
   * @param channel channel to find
   * @return returns equivialent channel to <b>channel</b>, but from entered channels list
   */
    private Channel getChannel(Channel channel) {
        for (Iterator it = channels.iterator(); it.hasNext(); ) {
            Channel enteredChannel = (Channel) it.next();
            if (enteredChannel.equals(channel)) return enteredChannel;
        }
        return null;
    }

    /**
   * Adds new UserLine with user's info to users section.
   *
   * @param user user's info
   */
    private void addUser(User user) {
        usersSection.add(new UserLine(user));
    }

    /**
   * Inserts new UserLine with user's info to users section.
   *
   * @param user user's info
   */
    private void insertUser(User user, int index) {
        usersSection.insert(new UserLine(user), index);
    }

    /**
   * Removes UserLine with user's info from users section
   *
   * @param user user's info
   */
    private void removeUser(User user) {
        for (Iterator it = usersSection.iterator(); it.hasNext(); ) {
            UserLine userLine = (UserLine) it.next();
            if (user.equals(userLine.getUser())) {
                usersSection.remove(userLine);
                break;
            }
        }
    }

    /**
   * Removes all users from users section
   */
    private void removeAllUsers() {
        usersSection.clear();
    }

    public char getUserType(Channel channel, User user) {
        channel = getChannel(channel);
        if (channel != null) {
            return channel.getUser(user).getType();
        } else {
        }
        return ' ';
    }

    private void handleDelayedEvents() {
        for (Iterator it = delayedEvents.iterator(); it.hasNext(); ) {
            EventInfo info = (EventInfo) it.next();
            switch(info.getType()) {
                case EventInfo.TYPE_JOIN:
                    userJoined(info.getUser(), info.getChannel());
                    break;
                case EventInfo.TYPE_LEFT:
                    userLeft(info.getUser(), info.getChannel());
                    break;
                case EventInfo.TYPE_MODE:
                    userModeChange(info.getChannel(), info.getUser(), info.getMode(), info.isAdding());
                    break;
                case EventInfo.TYPE_NICK:
                    userChangesNick(info.getUser(), info.getNick());
                    break;
                case EventInfo.TYPE_QUIT:
                    userQuit(info.getUser());
                    break;
            }
            it.remove();
        }
    }

    public void fixup() {
        channelsSection.add(new ChannelLine(new Channel("#Temp")));
        usersSection.add(new UserLine(new User("Temp")));
        channelsSection.setVisible(false);
        usersSection.setVisible(false);
        DeferredCommand.add(new Command() {

            public void execute() {
                usersSection.setVisible(true);
                channelsSection.setVisible(true);
                channelsSection.fixup();
                usersSection.fixup();
                channelsSection.clear();
                usersSection.clear();
            }
        });
    }

    public Channel getSelectedChannel() {
        return selectedChannel;
    }

    private class EventInfo {

        public static final int TYPE_JOIN = 0;

        public static final int TYPE_LEFT = 1;

        public static final int TYPE_NICK = 2;

        public static final int TYPE_MODE = 3;

        public static final int TYPE_QUIT = 4;

        private Channel channel;

        private User user;

        private String nick;

        private int type;

        private char mode;

        private boolean adding;

        public EventInfo(boolean adding, Channel channel, char mode, String nick, User user, int type) {
            this.adding = adding;
            this.channel = channel;
            this.mode = mode;
            this.nick = nick;
            this.user = user;
            this.type = type;
        }

        public EventInfo(Channel channel, User user, int type) {
            this.channel = channel;
            this.user = user;
            this.type = type;
        }

        public EventInfo(Channel channel, User user, String nick, int type) {
            this.channel = channel;
            this.nick = nick;
            this.user = user;
            this.type = type;
            this.type = type;
        }

        public EventInfo(User user, int type) {
            this.type = type;
            this.user = user;
        }

        public Channel getChannel() {
            return channel;
        }

        public String getNick() {
            return nick;
        }

        public User getUser() {
            return user;
        }

        public int getType() {
            return type;
        }

        public boolean isAdding() {
            return adding;
        }

        public char getMode() {
            return mode;
        }
    }
}
