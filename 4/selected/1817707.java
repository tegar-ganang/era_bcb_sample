package net.jetrix.monitor;

import java.util.Date;

/**
 * @author Emmanuel Bourg
 * @version $Revision: 742 $, $Date: 2008-08-26 09:31:14 -0400 (Tue, 26 Aug 2008) $
 */
public class PlayerStats {

    private String name;

    private String team;

    private String channel;

    private Date firstSeen;

    private Date lastSeen;

    private Date lastPlayed;

    private ServerInfo lastServer;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Date getFirstSeen() {
        return firstSeen;
    }

    public void setFirstSeen(Date firstSeen) {
        this.firstSeen = firstSeen;
    }

    public Date getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Date lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Date getLastPlayed() {
        return lastPlayed;
    }

    public void setLastPlayed(Date lastPlayed) {
        this.lastPlayed = lastPlayed;
    }

    public ServerInfo getLastServer() {
        return lastServer;
    }

    public void setLastServer(ServerInfo lastServer) {
        this.lastServer = lastServer;
    }

    /**
     * Tells if the player has been active in the last 15 minutes.
     */
    public boolean isActive() {
        return lastPlayed != null && lastPlayed.getTime() > System.currentTimeMillis() - 15 * 60 * 1000;
    }
}
