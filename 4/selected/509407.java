package net.jetrix.monitor;

import java.util.Date;
import net.jetrix.agent.ChannelInfo;
import net.jetrix.agent.PlayerInfo;
import net.jetrix.agent.QueryInfo;

/**
 * Server stats at a given time.
 * 
 * @author Emmanuel Bourg
 * @version $Revision: 729 $, $Date: 2008-08-18 12:35:43 -0400 (Mon, 18 Aug 2008) $
 */
public class ServerStats {

    private long id;

    private long serverId;

    private Date date;

    private int ping;

    private int playerCount;

    private int activePlayerCount;

    private int channelCount;

    private int activeChannelCount;

    public void update(QueryInfo info) {
        date = new Date();
        ping = (int) info.getPing();
        playerCount = info.getPlayers().size();
        channelCount = info.getChannels().size();
        int activePlayerCount = 0;
        int activeChannelCount = 0;
        for (PlayerInfo player : info.getPlayers()) {
            if (player.isPlaying()) {
                activePlayerCount++;
            }
        }
        for (ChannelInfo channel : info.getChannels()) {
            if (channel.isPlaying()) {
                activeChannelCount++;
            }
        }
        this.activePlayerCount = activePlayerCount;
        this.activeChannelCount = activeChannelCount;
    }

    /**
     * Merge the best player/channel count into the current ServerStat instance.
     *
     * @param stats
     */
    public void merge(ServerStats stats) {
        playerCount = Math.max(playerCount, stats.playerCount);
        activePlayerCount = Math.max(activePlayerCount, stats.activePlayerCount);
        channelCount = Math.max(channelCount, stats.channelCount);
        activeChannelCount = Math.max(activeChannelCount, stats.activeChannelCount);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getServerId() {
        return serverId;
    }

    public void setServerId(long serverId) {
        this.serverId = serverId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getPing() {
        return ping;
    }

    public void setPing(int ping) {
        this.ping = ping;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {
        this.playerCount = playerCount;
    }

    public int getActivePlayerCount() {
        return activePlayerCount;
    }

    public void setActivePlayerCount(int activePlayerCount) {
        this.activePlayerCount = activePlayerCount;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public void setChannelCount(int channelCount) {
        this.channelCount = channelCount;
    }

    public int getActiveChannelCount() {
        return activeChannelCount;
    }

    public void setActiveChannelCount(int activeChannelCount) {
        this.activeChannelCount = activeChannelCount;
    }
}
