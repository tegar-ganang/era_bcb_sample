package org.gamio.conf;

/**
 * @author Agemo Cui <agemocui@gamio.org>
 * @version $Rev: 19 $ $Date: 2008-09-26 19:00:58 -0400 (Fri, 26 Sep 2008) $
 */
public final class ServerProps extends GateProps {

    private int backLog = 50;

    private int channelIdleTimeout = 30;

    private String description = null;

    public int getBackLog() {
        return backLog;
    }

    public void setBackLog(int backLog) {
        this.backLog = backLog;
    }

    public String getBindAddr() {
        return getIP();
    }

    public void setBindAddr(String bindAddr) {
        setIP(bindAddr);
    }

    public int getChannelIdleTimeout() {
        return channelIdleTimeout;
    }

    public void setChannelIdleTimeout(int channelIdleTimeout) {
        this.channelIdleTimeout = channelIdleTimeout;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getTimeout() {
        return channelIdleTimeout;
    }
}
