package org.acaraus.triviachatbot.ruperechatcommunication;

public class RupereChatUser {

    private int id;

    private String username;

    private int role;

    private int channelId;

    RupereChatUser() {
        id = 0;
        username = "";
        role = 0;
        channelId = 0;
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
