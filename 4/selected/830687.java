package net.jetrix.agent;

import java.util.List;

/**
 * Information about a tetrinet server retrieved through the query protocol.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 794 $, $Date: 2009-02-17 14:08:39 -0500 (Tue, 17 Feb 2009) $
 */
public class QueryInfo {

    private String hostname;

    private String version;

    private List<PlayerInfo> players;

    private List<ChannelInfo> channels;

    private long ping;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * Return the version of the server.
     */
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<PlayerInfo> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerInfo> players) {
        this.players = players;
    }

    /**
     * Return the list of players in the specified channel.
     *
     * @param channel the name of the channel
     */
    public List getPlayers(String channel) {
        return players;
    }

    public List<ChannelInfo> getChannels() {
        return channels;
    }

    public void setChannels(List<ChannelInfo> channels) {
        this.channels = channels;
    }

    public long getPing() {
        return ping;
    }

    public void setPing(long ping) {
        this.ping = ping;
    }
}
