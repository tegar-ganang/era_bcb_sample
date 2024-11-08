package org.dreamspeak.lib.data;

/**
 * TODO: Proper documentation
 * 
 * @author avithan
 */
public class Player {

    protected final int id;

    String nickname;

    String login;

    Version clientVersion;

    String os;

    long lastTickTalking;

    public static final long TALKING_TIMEOUT = 200;

    final PlayerServerPrivilegeSet serverPrivileges;

    final PlayerChannelPrivilegeSet channelPrivileges;

    public PlayerChannelPrivilegeSet getChannelPrivileges() {
        return channelPrivileges;
    }

    protected final PlayerStatusSet status;

    protected Channel currentChannel;

    public Player(int id, String nickname) {
        serverPrivileges = new PlayerServerPrivilegeSet();
        channelPrivileges = new PlayerChannelPrivilegeSet();
        status = new PlayerStatusSet();
        this.id = id;
        this.nickname = nickname;
        lastTickTalking = 0;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nick) {
        this.nickname = nick;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public Version getClientVersion() {
        return clientVersion;
    }

    public void setClientVersion(Version clientVersion) {
        this.clientVersion = clientVersion;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public Channel getCurrentChannel() {
        return currentChannel;
    }

    public void setCurrentChannel(Channel currentChannel) {
        this.currentChannel = currentChannel;
    }

    public int getId() {
        return id;
    }

    public PlayerServerPrivilegeSet getServerPrivileges() {
        return serverPrivileges;
    }

    public PlayerStatusSet getStatus() {
        return status;
    }

    public boolean isTalking() {
        return System.currentTimeMillis() > lastTickTalking + TALKING_TIMEOUT;
    }

    public void setTalking() {
        lastTickTalking = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(" [Id:" + getId() + "]");
        sb.append(" [Nick:" + getNickname() + "]");
        sb.append(" [ServerPrivileges:" + getServerPrivileges().toString() + "]");
        sb.append(" [ChannelPrivileges:" + getChannelPrivileges().toString() + "]");
        sb.append(" [Status:" + getStatus().toString() + "]");
        return sb.toString();
    }
}
