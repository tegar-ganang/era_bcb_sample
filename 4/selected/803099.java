package org.mobicents.media.server.impl.rtp;

import java.io.IOException;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;

/**
 * This is the starting point for creating, maintaining
 * and closing an RTP session.
 * 
 * @author kulikov
 */
public class RTPManager {

    protected UdpManager udpManager;

    private Clock clock = new DefaultClock();

    /** Jitter value*/
    protected int jitter = 60;

    /** Bind address */
    private String bindAddress;

    /** Available port range */
    private int lowPort = 1024;

    private int highPort = 65535;

    protected boolean isControlEnabled;

    protected Scheduler scheduler;

    protected int dtmf = -1;

    public RTPManager(UdpManager udpManager) {
        this.udpManager = udpManager;
        this.bindAddress = udpManager.getBindAddress();
    }

    /**
     * Gets the IP address to which RTP is bound.
     *
     * @return the IP address as character string
     */
    public String getBindAddress() {
        return udpManager.getBindAddress();
    }

    /**
     * Modify the bind address.
     *
     * @param bindAddress the IP address as string or host name.
     * @deprecated 
     */
    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }

    /**
     * Gets the minimum available port number.
     *
     * @return port number
     */
    public int getLowPort() {
        return lowPort;
    }

    /**
     * Modifies minimum available port
     *
     * @param lowPort the port number.
     */
    public void setLowPort(int lowPort) {
        this.lowPort = lowPort;
    }

    /**
     * Gets the maximum available port number.
     *
     * @return port number
     */
    public int getHighPort() {
        return highPort;
    }

    /**
     * Modifies maximum available port
     *
     * @param port the port number.
     */
    public void setHighPort(int port) {
        this.highPort = port;
    }

    public void setJitter(int jitter) {
        this.jitter = jitter;
    }

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * Modifies payload id for RFC2833 DTMF event.
     * 
     * @param dtmf the payload id or -1 if disabled.
     */
    public void setDtmf(int dtmf) {
        this.dtmf = dtmf;
    }

    public void start() throws IOException {
    }

    public void stop() {
    }

    public Clock getClock() {
        return clock;
    }

    public RTPDataChannel getChannel() throws IOException {
        return new RTPDataChannel(this);
    }
}
