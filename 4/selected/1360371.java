package com.tysanclan.site.projectewok.ws.mumble;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Jeroen
 */
@XmlRootElement(name = "hash")
public class ServerStatus implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Channel> channels;

    private List<MumbleUser> users;

    @XmlElementWrapper(name = "channels")
    public List<Channel> getChannels() {
        if (channels == null) channels = new LinkedList<Channel>();
        return channels;
    }

    public void setChannels(List<Channel> channels) {
        this.channels = channels;
    }

    @XmlElementWrapper(name = "users")
    public List<MumbleUser> getUsers() {
        if (users == null) users = new LinkedList<MumbleUser>();
        return users;
    }

    public void setUsers(List<MumbleUser> users) {
        this.users = users;
    }
}
