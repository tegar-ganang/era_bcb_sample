package de.iritgo.openmetix.comm.chat.chatter;

import de.iritgo.openmetix.core.base.BaseObject;
import de.iritgo.openmetix.framework.user.User;
import de.iritgo.openmetix.framework.user.UserRegistry;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class ChatChannel extends BaseObject {

    private String name;

    private List chatter;

    private UserRegistry userRegistry;

    public ChatChannel(String name, UserRegistry userRegistry) {
        this.name = name;
        chatter = new LinkedList();
        this.userRegistry = userRegistry;
    }

    public void addChatter(Long chatterId) {
        User user = userRegistry.getUser(chatterId);
        if (user != null) {
            chatter.add(user);
        }
    }

    public boolean existsChatterInChannel(Long chatterId) {
        User user = userRegistry.getUser(chatterId);
        if (user != null) {
            return chatter.contains(user);
        }
        return false;
    }

    public void removeChatter(Long chatterId) {
        chatter.remove(userRegistry.getUser(chatterId));
    }

    public void removeChatter(User user) {
        chatter.remove(user);
    }

    public String getName() {
        return name;
    }

    public int getChannelId() {
        return name.hashCode();
    }

    public int getNumChatters() {
        return chatter.size();
    }

    public Iterator getMembersIterator() {
        return chatter.iterator();
    }
}
