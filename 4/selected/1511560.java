package de.z8bn.ircg.conf;

import java.util.*;

/**
 *
 * @author  cf2
 */
public class ServerConfig {

    /**
     * Holds value of property host.
     */
    private String host;

    /**
     * Holds value of property port.
     */
    private short port;

    /**
     * Holds value of property botName.
     */
    private String botName = "IRCG Bot";

    /**
     * Holds value of property nickname.
     */
    private String nickname = "ircg";

    /**
     * Holds value of property alternateNick1.
     */
    private String alternateNick1 = "ircg_";

    /**
     * Holds value of property alternateNick2.
     */
    private String alternateNick2 = "ircg__";

    /**
     * Holds value of property nickServPassword.
     */
    private String nickServPassword;

    /**
     * Holds value of property serverPassword.
     */
    private String serverPassword;

    private List<ChannelConfig> channels = new LinkedList<ChannelConfig>();

    /** Creates a new instance of ServerConfig */
    public ServerConfig() {
    }

    public void addChannel(ChannelConfig channel) {
        channels.add(channel);
    }

    /**
     * Getter for property serverPassword.
     * @return Value of property serverPassword.
     */
    public String getServerPassword() {
        return this.serverPassword;
    }

    /**
     * Setter for property serverPassword.
     * @param serverPassword New value of property serverPassword.
     */
    public void setServerPassword(String serverPassword) {
        this.serverPassword = serverPassword;
    }

    public List<ChannelConfig> getChannels() {
        return Collections.unmodifiableList(channels);
    }

    /**
     * Getter for property name.
     * @return Value of property name.
     */
    public String getName() {
        return this.host + ":" + this.port;
    }

    /**
     * Getter for property host.
     * @return Value of property host.
     */
    public String getHost() {
        return this.host;
    }

    /**
     * Setter for property host.
     * @param host New value of property host.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Getter for property port.
     * @return Value of property port.
     */
    public short getPort() {
        return this.port;
    }

    /**
     * Setter for property port.
     * @param port New value of property port.
     */
    public void setPort(short port) {
        this.port = port;
    }

    /**
     * Getter for property botName.
     * @return Value of property botName.
     */
    public String getBotName() {
        return this.botName;
    }

    /**
     * Setter for property botName.
     * @param botName New value of property botName.
     */
    public void setBotName(String botName) {
        this.botName = botName;
    }

    /**
     * Getter for property nickname.
     * @return Value of property nickname.
     */
    public String getNickname() {
        return this.nickname;
    }

    /**
     * Setter for property nickname.
     * @param nickname New value of property nickname.
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * Getter for property alternateNick1.
     * @return Value of property alternateNick1.
     */
    public String getAlternateNick1() {
        return this.alternateNick1;
    }

    /**
     * Setter for property alternateNick1.
     * @param alternateNick1 New value of property alternateNick1.
     */
    public void setAlternateNick1(String alternateNick1) {
        this.alternateNick1 = alternateNick1;
    }

    /**
     * Getter for property alternateNick2.
     * @return Value of property alternateNick2.
     */
    public String getAlternateNick2() {
        return this.alternateNick2;
    }

    /**
     * Setter for property alternateNick2.
     * @param alternateNick2 New value of property alternateNick2.
     */
    public void setAlternateNick2(String alternateNick2) {
        this.alternateNick2 = alternateNick2;
    }

    /**
     * Getter for property nickServPassword.
     * @return Value of property nickServPassword.
     */
    public String getNickServPassword() {
        return this.nickServPassword;
    }

    /**
     * Setter for property nickServPassword.
     * @param nickServPassword New value of property nickServPassword.
     */
    public void setNickServPassword(String nickServPassword) {
        this.nickServPassword = nickServPassword;
    }
}
