package coopnetclient.frames.models;

import java.util.ArrayList;
import java.util.TreeSet;
import javax.swing.AbstractListModel;

/**
 * Model of the user list which is sorted 
 */
public class VoiceChatChannelListModel extends AbstractListModel {

    private ArrayList<Channel> channels = new ArrayList<Channel>();

    private ArrayList<String> mutelist = new ArrayList<String>();

    private ArrayList<String> talkinglist = new ArrayList<String>();

    public static class Channel {

        private String name;

        private TreeSet<String> users;

        public Channel(String name) {
            this.name = name;
            users = new TreeSet<String>();
        }

        public int size() {
            return (1 + users.size());
        }

        public Object getElementAt(int index) {
            if (index == 0) {
                return name;
            } else {
                index--;
            }
            if (index < users.size()) {
                return users.toArray()[index];
            } else {
                return null;
            }
        }
    }

    public VoiceChatChannelListModel() {
        super();
        channels.add(new Channel("Channel 1"));
        channels.add(new Channel("Channel 2"));
        channels.add(new Channel("Channel 3"));
        channels.add(new Channel("Channel 4"));
        channels.add(new Channel("Channel 5"));
        channels.add(new Channel("Channel 6"));
        channels.add(new Channel("Channel 7"));
        channels.add(new Channel("Channel 8"));
    }

    public void refresh() {
        fireContentsChanged(this, 0, getSize());
    }

    public void addUserToChannel(String user, int channelIndex) {
        Channel c = channels.get(channelIndex);
        c.users.add(user);
        talkinglist.remove(user);
        fireContentsChanged(this, 0, getSize());
    }

    public void removeUser(String user) {
        for (Channel c : channels) {
            c.users.remove(user);
        }
        fireContentsChanged(this, 0, getSize());
    }

    public int indexOfChannel(Channel c) {
        return channels.indexOf(c);
    }

    public void moveUserToChannel(String user, String channel) {
        removeUser(user);
        Channel c = getChannel(channel);
        c.users.add(user);
        fireContentsChanged(this, 0, getSize());
    }

    public void moveUserToChannel(String user, int channelIndex) {
        removeUser(user);
        Channel c = channels.get(channelIndex);
        c.users.add(user);
        fireContentsChanged(this, 0, getSize());
    }

    public void mute(String user) {
        mutelist.add(user);
        fireContentsChanged(this, 0, getSize());
    }

    public void unMute(String user) {
        mutelist.remove(user);
        fireContentsChanged(this, 0, getSize());
    }

    public boolean isMuted(String user) {
        return mutelist.contains(user);
    }

    public void setTalking(String user) {
        if (!talkinglist.contains(user)) {
            talkinglist.add(user);
            fireContentsChanged(this, 0, getSize());
        }
    }

    public void setNotTalking(String user) {
        talkinglist.remove(user);
        fireContentsChanged(this, 0, getSize());
    }

    public boolean isTalking(String user) {
        return talkinglist.contains(user);
    }

    public Channel getChannel(String value) {
        for (Channel c : channels) {
            if (c.name.equals(value)) {
                return c;
            }
        }
        return null;
    }

    public boolean updateName(String oldname, String newName) {
        Channel group = groupOfContact(oldname);
        if (group != null) {
            group.users.remove(oldname);
            group.users.add(newName);
            fireContentsChanged(this, 0, getSize());
            return true;
        }
        return false;
    }

    @Override
    public int getSize() {
        int size = 0;
        for (Channel g : channels) {
            size += g.size();
        }
        return size;
    }

    @Override
    public Object getElementAt(int index) {
        int sizethisfar = 0;
        for (Channel g : channels) {
            if ((sizethisfar + g.size()) > index) {
                return g.getElementAt(index - sizethisfar);
            } else {
                sizethisfar += g.size();
            }
        }
        return null;
    }

    public Channel groupOfContact(String contact) {
        for (Channel g : channels) {
            if (g.users.contains(contact)) {
                return g;
            }
        }
        return null;
    }

    public void clear() {
        for (Channel c : channels) {
            c.users.clear();
        }
        talkinglist.clear();
        mutelist.clear();
        fireContentsChanged(this, 0, getSize());
    }

    public boolean contains(Object element) {
        if (groupOfContact(element.toString()) != null) {
            return true;
        }
        if (getChannel(element.toString()) != null) {
            return true;
        }
        return false;
    }
}
