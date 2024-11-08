package tsr;

/**
 * Stores the player information for one ts player.
 * 
 * @author andiinthehouse
 */
public class PlayerInfo {

    private final int playerId;

    private final int channelId;

    private final int ps;

    private final int bs;

    private final int pr;

    private final int br;

    private final int pl;

    private final int ping;

    private final int loginTime;

    private final int idleTime;

    private final int cPrivs;

    private final int pPrivs;

    private final int pFlags;

    private final String ip;

    private final String nick;

    private final String loginName;

    public PlayerInfo(final String[] pi) {
        this.playerId = Integer.parseInt(pi[0]);
        this.channelId = Integer.parseInt(pi[1]);
        this.ps = Integer.parseInt(pi[2]);
        this.bs = Integer.parseInt(pi[3]);
        this.pr = Integer.parseInt(pi[4]);
        this.br = Integer.parseInt(pi[5]);
        this.pl = Integer.parseInt(pi[6]);
        this.ping = Integer.parseInt(pi[7]);
        this.loginTime = Integer.parseInt(pi[8]);
        this.idleTime = Integer.parseInt(pi[9]);
        this.cPrivs = Integer.parseInt(pi[10]);
        this.pPrivs = Integer.parseInt(pi[11]);
        this.pFlags = Integer.parseInt(pi[12]);
        this.ip = pi[13];
        this.nick = pi[14];
        this.loginName = pi[15];
    }

    public int getPlayerId() {
        return playerId;
    }

    public int getChannelId() {
        return channelId;
    }

    public int getPs() {
        return ps;
    }

    public int getBs() {
        return bs;
    }

    public int getPr() {
        return pr;
    }

    public int getBr() {
        return br;
    }

    public int getPl() {
        return pl;
    }

    public int getPing() {
        return ping;
    }

    public int getLoginTime() {
        return loginTime;
    }

    public int getIdleTime() {
        return idleTime;
    }

    public int getCPrivs() {
        return cPrivs;
    }

    public int getPPrivs() {
        return pPrivs;
    }

    public int getPFlags() {
        return pFlags;
    }

    public String getIp() {
        return ip;
    }

    public String getNick() {
        return nick;
    }

    public String getLoginName() {
        return loginName;
    }
}
