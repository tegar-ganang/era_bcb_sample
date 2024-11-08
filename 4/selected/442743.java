package jw.bznetwork.client.data.model;

import java.io.Serializable;

public class IrcBot implements Serializable {

    private int botid;

    private String nick;

    private String server;

    private int port;

    private String password;

    private String channel;

    public int getBotid() {
        return botid;
    }

    public void setBotid(int botid) {
        this.botid = botid;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}
