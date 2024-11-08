package org.openmobster.core.services.channel;

import org.openmobster.cloud.api.sync.Channel;

/**
 * @author openmobster@gmail
 *
 */
public final class ChannelRegistration {

    private String uri;

    private Channel channel;

    private long updateCheckInterval;

    public ChannelRegistration(String uri, Channel channel) {
        this.uri = uri;
        this.channel = channel;
        this.updateCheckInterval = 20000;
    }

    public String getUri() {
        return this.uri;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public long getUpdateCheckInterval() {
        return updateCheckInterval;
    }

    public void setUpdateCheckInterval(long updateCheckInterval) {
        this.updateCheckInterval = updateCheckInterval;
    }
}
