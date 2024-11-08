package pl.java.ircbot.irc;

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IrcServer {

    private static Log logger = LogFactory.getLog(IrcServer.class);

    private String host;

    private int port = 6667;

    private List<IrcChannel> channels;

    /**
     * Metoda wypisuje informacje o konfiguracji serwera.
     */
    public void dumpServerInfo() {
        logger.debug("host: " + getHost() + ", port: " + getPort());
        if (channels == null) return;
        logger.debug("registed channels: ");
        for (IrcChannel channel : channels) {
            logger.debug("channel name = " + channel.getName());
        }
    }

    /**
     * TODO
     * 
     * @return Returns the host.
     */
    public String getHost() {
        return host;
    }

    /**
     * TODO
     * 
     * @param host
     *            The host to set.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * TODO
     * 
     * @return Returns the port.
     */
    public int getPort() {
        return port;
    }

    /**
     * TODO
     * 
     * @param port
     *            The port to set.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * TODO
     * 
     * @return Returns the channels.
     */
    public List<IrcChannel> getChannels() {
        return channels;
    }

    /**
     * TODO
     * 
     * @param channels
     *            The channels to set.
     */
    public void setChannels(List<IrcChannel> channels) {
        this.channels = channels;
    }

    /**
     * 
     * @param channelName
     * @return
     */
    public IrcChannel getChannelByName(String channelName) {
        if (channelName == null) return null;
        for (IrcChannel ircChannel : getChannels()) {
            if (ircChannel == null) continue;
            if (channelName.equalsIgnoreCase(ircChannel.getName())) return ircChannel;
        }
        return null;
    }
}
