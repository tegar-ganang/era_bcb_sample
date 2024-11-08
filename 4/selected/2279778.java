package com.langerra.server.channel.impl;

import java.io.Serializable;
import com.langerra.server.channel.ChannelServiceFactory;
import com.langerra.shared.channel.ChannelMessage;

public class ChannelMessageImpl<T extends Serializable> extends ChannelMessage<T> {

    private static final long serialVersionUID = -2378395885118983249L;

    private long key;

    public ChannelMessageImpl(long key) {
        this.key = key;
    }

    @Override
    public T getValue() {
        return (T) ChannelServiceFactory.getChannelService().<T>getServicePool().get(key);
    }
}
