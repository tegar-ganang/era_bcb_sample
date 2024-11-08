package org.dreamspeak.lib.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * TODO: Proper documentation
 * 
 * @author avithan
 * 
 */
public class Channel implements Comparable<Channel> {

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Channel && ((Channel) obj).id == this.id) return true;
        return super.equals(obj);
    }

    /**
	 * A order of 3200 (0x0C80) means that this channel is ordered by incoming
	 * order.
	 */
    static final short NO_ORDER_VALUE = 0x0C80;

    protected final int id;

    protected short order;

    private String name;

    protected String topic;

    protected String description;

    protected Codec codec;

    public Codec getCodec() {
        return codec;
    }

    public void setCodec(Codec codec) {
        this.codec = codec;
    }

    protected final ChannelAttributeSet flags;

    protected short maxUsers;

    protected short currentUsers;

    protected String password;

    protected final List<Channel> subChannels;

    public Channel(int id, String name) {
        if (name == null) throw new NullPointerException();
        flags = new ChannelAttributeSet();
        subChannels = new ArrayList<Channel>();
        this.id = id;
        this.setName(name);
        this.order = 0;
    }

    public void admSetMaxUsers(int maxUsers) throws SecurityException {
        throw new RuntimeException("This method is not yet implemented.");
    }

    public void admSetPassword(String password) throws SecurityException {
        throw new RuntimeException("This method is not yet implemented.");
    }

    public void addChannel(Channel channel) {
        subChannels.add(channel);
    }

    Channel getChannelByIdRecursive(int id) {
        if (this.id == id) return this;
        for (Channel c : subChannels) {
            if (c.id == id) return c;
            Channel subC = c.getChannelByIdRecursive(id);
            if (subC != null) return subC;
        }
        return null;
    }

    void clearSubchannelsRecursive() {
        for (Channel c : subChannels) {
            c.subChannels.clear();
        }
        subChannels.clear();
    }

    private void sort() {
        Collections.sort(subChannels);
    }

    public void sortRecursive() {
        sort();
        for (Channel sub : subChannels) sub.sortRecursive();
    }

    public void removeChannel(Channel channel) {
        if (subChannels.contains(channel)) subChannels.remove(channel);
    }

    public short getMaxUsers() {
        if (maxUsers < 0) return Short.MAX_VALUE;
        return maxUsers;
    }

    public ChannelAttributeSet getFlags() {
        return flags;
    }

    public Channel[] getSubChannels() {
        Channel[] arrType = {};
        return (Channel[]) subChannels.toArray(arrType);
    }

    public int compareTo(Channel o) {
        if (o == null) throw new NullPointerException();
        if (this.equals(o)) return 0;
        if (order < o.order) return -1; else if (order > o.order) return 1;
        if (id < o.id) return -1; else if (id < o.id) return 1;
        return 0;
    }

    public short getOrder() {
        return order;
    }

    public void setOrder(short order) {
        this.order = order;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public short getCurrentUsers() {
        if (currentUsers < 0) return Short.MAX_VALUE;
        return currentUsers;
    }

    public void setCurrentUsers(short currentUsers) {
        this.currentUsers = currentUsers;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getId() {
        return id;
    }

    public void setMaxUsers(short maxUsers) {
        this.maxUsers = maxUsers;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean hasSubchannels() {
        return !subChannels.isEmpty();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(" [ID : " + getId() + "]");
        sb.append(" [Name: " + getName() + "]");
        sb.append(" [Topic: " + getTopic() + "]");
        sb.append(" [Description: " + getDescription() + "]");
        sb.append(" [Order: " + getOrder() + "]");
        sb.append(" [MaxUsers: " + getMaxUsers() + "]");
        sb.append(" [Codec: " + getCodec().toString() + "]");
        sb.append(" [Flags: " + getFlags().toString() + "]");
        return sb.toString();
    }
}
