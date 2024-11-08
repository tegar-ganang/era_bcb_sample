package com.langerra.server.channel.impl;

import java.io.Serializable;
import java.util.Collections;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import com.langerra.server.channel.ChannelServiceFactory;
import com.langerra.server.channel.NamedCounter;
import com.langerra.shared.channel.Channel;
import com.langerra.shared.channel.ChannelService;
import com.langerra.shared.channel.ChannelServicePool;

public class JCacheChannelServiceImpl implements ChannelService {

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Serializable> ChannelServicePool<T> getServicePool() {
        return new ChannelServicePoolImpl<T>((ChannelImpl<T>) getChannel("__pool__", true));
    }

    @Override
    public <T extends Serializable> Channel<T> getChannel(String channelName, boolean persistence) {
        final String namespace = ChannelServiceFactory.getNamespace(channelName);
        Cache cache = null;
        final CacheManager manager = CacheManager.getInstance();
        while (cache == null) {
            try {
                manager.registerCache(namespace, manager.getCacheFactory().createCache(Collections.emptyMap()));
            } catch (CacheException e) {
                e.printStackTrace();
            }
            cache = manager.getCache(namespace);
        }
        final NamedCounter rOffset = new JCacheCounter(cache, "R");
        final NamedCounter wOffset = new JCacheCounter(cache, "W");
        return new ChannelImpl<T>(namespace, cache, rOffset, wOffset, persistence);
    }

    @Override
    public void deleteChannel(String channelName) {
        final String namespace = ChannelServiceFactory.getNamespace(channelName);
        final CacheManager manager = CacheManager.getInstance();
        final Cache cache = manager.getCache(namespace);
        if (cache != null) cache.clear();
    }
}
