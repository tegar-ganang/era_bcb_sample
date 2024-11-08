package com.langerra.server.channel;

import com.langerra.shared.channel.ChannelMessage;

public class ChannelMessageImpl<T> implements ChannelMessage<T> {

    private long key;

    public ChannelMessageImpl(long key) {
        this.key = key;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getValue() {
        return (T) ChannelServiceFactory.getChannelService().getServicePool().get(key);
    }
}
