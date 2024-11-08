package com.jpark.jamse.player.api;

import java.io.Serializable;

/**
 * Detailed file info for actual file 
 * bit rate,sample rate,channel
 * @author wklum
 *
 */
public class DetailInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private int bitrate;

    private int samplRate;

    private int channel;

    public DetailInfo(int bitrate, int samplRate, int channel) {
        super();
        this.bitrate = bitrate;
        this.samplRate = samplRate;
        this.channel = channel;
    }

    public int getBitrate() {
        return bitrate;
    }

    public int getSamplRate() {
        return samplRate;
    }

    public int getChannel() {
        return channel;
    }
}
