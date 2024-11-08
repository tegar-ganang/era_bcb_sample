package com.outlandr.irc.client;

import java.util.HashMap;
import java.util.Map;

public class ClientState {

    private Map<String, Room> channels = new HashMap<String, Room>();

    private boolean isConnected;

    private String host;

    public Channel joinChannel(String channelName) {
        Channel channel = new Channel(channelName);
        channels.put(channelName, channel);
        return channel;
    }

    public void leaveChannel(String channelName) {
        channels.remove(channelName);
    }

    public boolean isMember(String channelName) {
        return channels.get(channelName) != null;
    }

    public Room[] getChannels() {
        return channels.values().toArray(new Room[channels.size()]);
    }

    public Room getChannel(String channelName) {
        return channels.get(channelName);
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(String host, boolean isConnected) {
        this.isConnected = isConnected;
        this.host = host;
        Room room = new Room(host);
        channels.put(host, room);
    }

    public Room getHost() {
        return channels.get(host);
    }
}
