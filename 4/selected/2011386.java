package com.langerra.client.channel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import com.langerra.client.channel.rpc.RemoteChannelService;
import com.langerra.client.channel.rpc.RemoteChannelServiceAsync;
import com.langerra.shared.channel.Channel;
import com.langerra.shared.channel.ChannelMessage;
import com.langerra.shared.channel.ChannelService;
import com.langerra.shared.channel.ChannelServicePool;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class ChannelServiceFactory {

    public static ChannelService getChannelService() {
        return new ChannelServiceImpl(GWT.<RemoteChannelServiceAsync>create(RemoteChannelService.class));
    }

    public static ChannelService getChannelService(RemoteChannelServiceAsync api) {
        return new ChannelServiceImpl(api);
    }
}

class ChannelServiceImpl implements ChannelService {

    final int flushTimeout;

    final int flushBucketSize;

    final int maxFlushRetries;

    final RemoteChannelServiceAsync api;

    ChannelServiceImpl(RemoteChannelServiceAsync api) {
        this.api = api;
        this.flushTimeout = 500;
        this.flushBucketSize = 10;
        this.maxFlushRetries = 20;
    }

    @Override
    public <T extends Serializable> ChannelServicePool<T> getServicePool() {
        return new ChannelServicePoolImpl<T>(api);
    }

    @Override
    public <T extends Serializable> Channel<T> getChannel(final String name, boolean datastored) {
        final LinkedList<T> buffer = new LinkedList<T>();
        final ChannelImpl<T> channel = new ChannelImpl<T>(api, buffer, name, flushTimeout, flushBucketSize, maxFlushRetries);
        new Poller<ArrayList<T>>(new Poller.Query<ArrayList<T>>() {

            @Override
            public void doQuery(final AsyncCallback<ArrayList<T>> callback) {
                api.readAll(name, new AsyncCallback<ArrayList<ChannelMessage<T>>>() {

                    @SuppressWarnings("unchecked")
                    @Override
                    public void onSuccess(ArrayList<ChannelMessage<T>> result) {
                        callback.onSuccess((ArrayList<T>) result);
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        callback.onFailure(caught);
                    }
                });
            }
        }, new Poller.Publish<ArrayList<T>>() {

            public void doPublish(ArrayList<T> result) {
                for (T e : result) if (channel.callback != null) {
                    channel.callback.onSuccess(e);
                } else {
                    buffer.add(e);
                }
            }

            ;
        }, Integer.MAX_VALUE).start();
        return channel;
    }

    @Override
    public void deleteChannel(String name) {
    }
}

class ChannelServicePoolImpl<T extends Serializable> implements ChannelServicePool<T> {

    long counter = 0;

    final HashMap<Long, T> map = new HashMap<Long, T>();

    final RemoteChannelServiceAsync api;

    public ChannelServicePoolImpl(RemoteChannelServiceAsync api) {
        this.api = api;
    }

    @Override
    public ChannelMessage<T> getMessage(T value) {
        final Long key = counter++;
        map.put(key, value);
        api.<T>put("__pool__", new ChannelMessage<T>(value), new AsyncCallback<ChannelMessage<T>>() {

            @Override
            public void onSuccess(ChannelMessage<T> result) {
            }

            @Override
            public void onFailure(Throwable caught) {
            }
        });
        return new ChannelMessage<T>() {

            private static final long serialVersionUID = -368776059857041543L;

            @Override
            public T getValue() {
                return map.get(key);
            }
        };
    }

    @Override
    public T get(final Long key) {
        T value = map.get(key);
        if (value == null) {
            api.<T>get("__pool__", key, new AsyncCallback<ChannelMessage<T>>() {

                @Override
                public void onFailure(Throwable caught) {
                }

                @Override
                public void onSuccess(ChannelMessage<T> result) {
                    map.put(key, result.getValue());
                }
            });
        }
        return value;
    }
}

class ChannelImpl<T extends Serializable> implements Channel<T> {

    AsyncCallback<T> callback;

    final int maxRetries;

    final int flushTimeout;

    final int flushBuffer;

    final RemoteChannelServiceAsync api;

    final String namespace;

    final LinkedList<T> readBuffer;

    final LinkedList<T> writeBuffer = new LinkedList<T>();

    long flushTime = Long.MAX_VALUE;

    public ChannelImpl(RemoteChannelServiceAsync api, LinkedList<T> channel, String namespace, int flushTimeout, int flushBucketSize, int maxFlushRetries) {
        this.api = api;
        this.readBuffer = channel;
        this.namespace = namespace;
        this.maxRetries = maxFlushRetries;
        this.flushBuffer = flushBucketSize;
        this.flushTimeout = flushTimeout;
    }

    @Override
    public void async(AsyncCallback<T> asyncCallback) {
        this.callback = asyncCallback;
        for (T e : readBuffer) callback.onSuccess(e);
    }

    @Override
    public AsyncCallback<T> async() {
        return callback;
    }

    public LinkedList<T> getBuffer() {
        return writeBuffer;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public T read() {
        return readBuffer.poll();
    }

    @Override
    public Collection<T> readAll() {
        ArrayList<T> clone = new ArrayList<T>(readBuffer);
        readBuffer.clear();
        return clone;
    }

    @Override
    public Collection<T> readAll(long timeout) {
        return readAll();
    }

    @Override
    public void write(ChannelMessage<T> message) {
        throw new UnsupportedOperationException("Currently client side Channels do not support " + "ChannelMessage indirection.");
    }

    @Override
    @SuppressWarnings("unchecked")
    public long write(T message) {
        writeAll(Arrays.<T>asList(message));
        return -1;
    }

    @Override
    public void writeAll(Collection<? extends T> messages) {
        writeBuffer.addAll(messages);
        if (writeBuffer.size() == messages.size()) {
            flush();
        }
    }

    void flush() {
        if (writeBuffer.size() > flushBuffer) {
            flushNow();
        } else if (flushTime < System.currentTimeMillis()) {
            flushNow();
        } else {
            flushTime = Math.min(flushTime, System.currentTimeMillis() + flushTimeout);
        }
    }

    void flushNow() {
        final ArrayList<ChannelMessage<T>> messages = new ArrayList<ChannelMessage<T>>(readBuffer.size());
        for (T e : readBuffer) messages.add(new ChannelMessage<T>(e));
        api.writeAll(namespace, messages, new AsyncCallback<Boolean>() {

            int retries = 0;

            @Override
            public void onSuccess(Boolean result) {
            }

            @Override
            public void onFailure(Throwable caught) {
                if (retries++ < maxRetries) api.writeAll(namespace, messages, this);
            }
        });
    }
}
