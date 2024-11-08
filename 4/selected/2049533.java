package com.aionemu.chatserver.model.channel;

import com.aionemu.chatserver.model.ChannelType;
import com.aionemu.chatserver.utils.IdFactory;

/**
 * @author ATracer
 */
public abstract class Channel {

    protected ChannelType channelType;

    protected int channelId;

    /**
	 * 
	 * @param channelType
	 */
    public Channel(ChannelType channelType) {
        this.channelType = channelType;
        this.channelId = IdFactory.getInstance().nextId();
    }

    /**
	 * @return the channelId
	 */
    public int getChannelId() {
        return channelId;
    }

    /**
	 * @return the channelType
	 */
    public ChannelType getChannelType() {
        return channelType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + channelId;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Channel other = (Channel) obj;
        if (channelId != other.channelId) return false;
        return true;
    }
}
