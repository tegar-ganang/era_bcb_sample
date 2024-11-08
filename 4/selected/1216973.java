package main;

import java.util.Map;
import java.util.Set;

public class GameSetup {

    private String name;

    private String gameName;

    private Set<Channel> channels;

    private Map<String, Object> properties;

    private String hostName;

    private int port;

    private String botName;

    private String botClassName;

    private String password;

    private String botMode;

    private String identify;

    public GameSetup(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public String getGameName() {
        return this.gameName;
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }

    public String getHostName() {
        return this.hostName;
    }

    public int getPort() {
        return this.port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getBotName() {
        return this.botName;
    }

    public void setBotName(String botName) {
        this.botName = botName;
    }

    public String getBotClassName() {
        return this.botClassName;
    }

    public void setBotClassName(String botClassName) {
        this.botClassName = botClassName;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBotMode() {
        return this.botMode;
    }

    public void setBotMode(String botMode) {
        this.botMode = botMode;
    }

    public Set<Channel> getChannels() {
        return this.channels;
    }

    public void setChannels(Set<Channel> channels) {
        this.channels = channels;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public void setGameName(String gameName) {
        this.gameName = gameName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getIdentify() {
        return this.identify;
    }

    public void setIdentify(String identify) {
        this.identify = identify;
    }
}
