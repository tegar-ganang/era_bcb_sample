package org.red5.server.api.stream.support;

import org.red5.server.api.IBandwidthConfigure;
import org.red5.server.api.IConnectionBWConfig;

/**
 * Simple implementation of connection bandwidth configuration.
 * @author Steven Gong (steven.gong@gmail.com)
 * @version $Id$
 */
public class SimpleConnectionBWConfig extends SimpleBandwidthConfigure implements IConnectionBWConfig {

    private long upstreamBW;

    public long getDownstreamBandwidth() {
        long[] channelBandwidth = getChannelBandwidth();
        if (channelBandwidth[IBandwidthConfigure.OVERALL_CHANNEL] >= 0) {
            return channelBandwidth[IBandwidthConfigure.OVERALL_CHANNEL];
        } else {
            long bw = 0;
            if (channelBandwidth[IBandwidthConfigure.AUDIO_CHANNEL] < 0 || channelBandwidth[IBandwidthConfigure.VIDEO_CHANNEL] < 0) {
                bw = -1;
            } else {
                bw = channelBandwidth[IBandwidthConfigure.AUDIO_CHANNEL] + channelBandwidth[IBandwidthConfigure.VIDEO_CHANNEL];
            }
            if (channelBandwidth[IBandwidthConfigure.DATA_CHANNEL] >= 0) {
                bw += channelBandwidth[IBandwidthConfigure.DATA_CHANNEL];
            }
            return bw;
        }
    }

    public long getUpstreamBandwidth() {
        return upstreamBW;
    }

    public void setUpstreamBandwidth(long bw) {
        upstreamBW = bw;
    }
}
