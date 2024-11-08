package net.jetrix.monitor;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import net.jetrix.agent.ChannelInfo;
import net.jetrix.agent.PlayerInfo;

/**
 * Information about a TetriNET server.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 819 $, $Date: 2010-02-22 10:03:33 -0500 (Mon, 22 Feb 2010) $
 */
public class ServerInfo {

    private long id;

    private String hostname;

    private String IP;

    private String version;

    private String description;

    private String country;

    private String website;

    private boolean spectate;

    private String spectatorPassword;

    /** The date the server was added to the survey. */
    private Date dateAdded;

    /** The last time the server was queried. */
    private Date lastChecked;

    /** The last time the server was online. */
    private Date lastOnline;

    /** The last time the server was populated with players. */
    private Date lastPopulated;

    /** The last time the server hosted a game. */
    private Date lastActive;

    private ServerStats stats = new ServerStats();

    private int maxPlayerCount;

    private Date maxPlayerDate;

    private int maxActivePlayerCount;

    private Date maxActivePlayerDate;

    private List<ChannelInfo> channels;

    private List<PlayerInfo> players;

    private Collection<String> aliases;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public boolean isSpectate() {
        return spectate;
    }

    public void setSpectate(boolean spectate) {
        this.spectate = spectate;
    }

    public String getSpectatorPassword() {
        return spectatorPassword;
    }

    public void setSpectatorPassword(String spectatorPassword) {
        this.spectatorPassword = spectatorPassword;
    }

    public Date getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(Date dateAdded) {
        this.dateAdded = dateAdded;
    }

    public Date getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(Date lastChecked) {
        this.lastChecked = lastChecked;
    }

    public Date getLastOnline() {
        return lastOnline;
    }

    public void setLastOnline(Date lastOnline) {
        this.lastOnline = lastOnline;
    }

    public Date getLastPopulated() {
        return lastPopulated;
    }

    public void setLastPopulated(Date lastPopulated) {
        this.lastPopulated = lastPopulated;
    }

    public Date getLastActive() {
        return lastActive;
    }

    public void setLastActive(Date lastActive) {
        this.lastActive = lastActive;
    }

    public ServerStats getStats() {
        return stats;
    }

    public void setStats(ServerStats stats) {
        this.stats = stats;
    }

    public int getMaxPlayerCount() {
        return maxPlayerCount;
    }

    public void setMaxPlayerCount(int maxPlayerCount) {
        this.maxPlayerCount = maxPlayerCount;
    }

    public Date getMaxPlayerDate() {
        return maxPlayerDate;
    }

    public void setMaxPlayerDate(Date maxPlayerDate) {
        this.maxPlayerDate = maxPlayerDate;
    }

    public int getMaxActivePlayerCount() {
        return maxActivePlayerCount;
    }

    public void setMaxActivePlayerCount(int maxActivePlayerCount) {
        this.maxActivePlayerCount = maxActivePlayerCount;
    }

    public Date getMaxActivePlayerDate() {
        return maxActivePlayerDate;
    }

    public void setMaxActivePlayerDate(Date maxActivePlayerDate) {
        this.maxActivePlayerDate = maxActivePlayerDate;
    }

    public List<ChannelInfo> getChannels() {
        return channels;
    }

    public void setChannels(List<ChannelInfo> channels) {
        this.channels = channels;
    }

    public List<PlayerInfo> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerInfo> players) {
        this.players = players;
    }

    public Collection<String> getAliases() {
        return aliases;
    }

    public void setAliases(Collection<String> aliases) {
        this.aliases = aliases;
    }

    /**
     * Tells if the server has been online in the last 15 minutes.
     */
    public boolean isOnline() {
        return lastOnline != null && lastOnline.getTime() > System.currentTimeMillis() - 15 * 60 * 1000;
    }
}
