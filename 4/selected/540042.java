package com.enjoyxstudy.hip;

import java.io.File;
import java.util.Properties;

/**
 * @author onozaty
 */
public class Config {

    /** irc nick */
    private String nick = "hipBot";

    /** irc server name */
    private String serverName;

    /** irc server port */
    private int serverPort = 6667;

    /** irc server password */
    private String serverPassword;

    /** channel */
    private String channel;

    /** join message */
    private String joinMessage = "This channel is logged.";

    /** logging output directory */
    private File outputDir;

    /** output directory default */
    private static final String DEFAULT_OUTPUT_DIR = "./irclog";

    /** http port */
    private int httpPort = 3333;

    /** use encoding */
    private String encoding = "UTF-8";

    /** prefix remote address */
    private boolean isPrefixRemoteAddress;

    /**
     * @param properties
     */
    public Config(Properties properties) {
        nick = properties.getProperty("nick", nick);
        serverName = properties.getProperty("serverName");
        serverPort = Integer.parseInt(properties.getProperty("serverPort", String.valueOf(serverPort)));
        serverPassword = properties.getProperty("serverPassword");
        channel = properties.getProperty("channel");
        encoding = properties.getProperty("encoding", encoding);
        joinMessage = properties.getProperty("joinMessage", joinMessage);
        outputDir = new File(properties.getProperty("outputDir", DEFAULT_OUTPUT_DIR));
        httpPort = Integer.parseInt(properties.getProperty("httpPort", String.valueOf(httpPort)));
        isPrefixRemoteAddress = "true".equals(properties.getProperty("prefixRemoteAddress"));
    }

    /**
     * 
     */
    public Config() {
    }

    /**
     * @return channel
     */
    public String getChannel() {
        return channel;
    }

    /**
     * @param channel channel
     */
    public void setChannel(String channel) {
        this.channel = channel;
    }

    /**
     * @return encoding
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * @param encoding encoding
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * @return httpPort
     */
    public int getHttpPort() {
        return httpPort;
    }

    /**
     * @param httpPort httpPort
     */
    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    /**
     * @return joinMessage
     */
    public String getJoinMessage() {
        return joinMessage;
    }

    /**
     * @param joinMessage joinMessage
     */
    public void setJoinMessage(String joinMessage) {
        this.joinMessage = joinMessage;
    }

    /**
     * @return nick
     */
    public String getNick() {
        return nick;
    }

    /**
     * @param nick nick
     */
    public void setNick(String nick) {
        this.nick = nick;
    }

    /**
     * @return outputDir
     */
    public File getOutputDir() {
        return outputDir;
    }

    /**
     * @param outputDir outputDir
     */
    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    /**
     * @return serverName
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * @param serverName serverName
     */
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    /**
     * @return serverPassword
     */
    public String getServerPassword() {
        return serverPassword;
    }

    /**
     * @param serverPassword serverPassword
     */
    public void setServerPassword(String serverPassword) {
        this.serverPassword = serverPassword;
    }

    /**
     * @return serverPort
     */
    public int getServerPort() {
        return serverPort;
    }

    /**
     * @param serverPort serverPort
     */
    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    /**
     * @return isPrefixRemoteAddress
     */
    public boolean isPrefixRemoteAddress() {
        return isPrefixRemoteAddress;
    }

    /**
     * @param isPrefixRemoteAddress isPrefixRemoteAddress
     */
    public void setPrefixRemoteAddress(boolean isPrefixRemoteAddress) {
        this.isPrefixRemoteAddress = isPrefixRemoteAddress;
    }
}
