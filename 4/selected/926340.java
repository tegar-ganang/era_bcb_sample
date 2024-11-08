package org.thole.phiirc.client.model;

import java.util.Hashtable;
import java.util.Map;

public class User implements Comparable<User> {

    private String nick;

    private String name;

    private String realname;

    private String host;

    private Map<Channel, UserChannelPermission> channels = new Hashtable<Channel, UserChannelPermission>();

    public User(final String nick, final String name, final String realname, final String host, final Channel chan, final UserChannelPermission chanRole) {
        this.setNick(nick);
        this.setName(name);
        this.setRealname(realname);
        this.setHost(host);
        if (chan != null) {
            this.getChannels().put(chan, chanRole);
        }
    }

    /**
	 * for ordering
	 */
    public int compareTo(final User user) {
        return this.getNick().compareToIgnoreCase(((User) user).getNick());
    }

    public String getNick() {
        return nick;
    }

    public void setNick(final String nick) {
        this.nick = nick;
    }

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public Map<Channel, UserChannelPermission> getChannels() {
        return channels;
    }

    public void setChannels(final Map<Channel, UserChannelPermission> channels) {
        this.channels = channels;
    }

    public String getRealname() {
        return realname;
    }

    public void setRealname(final String realname) {
        this.realname = realname;
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
