package com.android1.amarena2d.engine;

import com.android1.amarena2d.annotations.Incomplete;
import com.android1.amarena2d.annotations.NotThreadsave;
import com.android1.amarena2d.commons.Callback;
import java.util.ArrayList;
import java.util.HashMap;

@NotThreadsave
@Incomplete
public class ChannelSystem {

    HashMap<String, Channel> channelMap = new HashMap<String, Channel>();

    public <V> Channel<V> getChannel(String name) {
        Channel<V> channel = channelMap.get(name);
        if (channel == null) {
            channel = new Channel<V>();
            channelMap.put(name, channel);
        }
        return channel;
    }

    public static class Channel<V> {

        ArrayList<Callback<V>> listenerList = new ArrayList<Callback<V>>();

        V value;

        public void write(V value) {
            this.value = value;
            notifyListener(value);
        }

        public void register(Callback<V> listener) {
            listenerList.add(listener);
        }

        public void unregister(Callback<V> listener) {
            listenerList.remove(listener);
        }

        public V getValue() {
            return value;
        }

        void notifyListener(V value) {
            for (int i = 0; i < listenerList.size(); i++) {
                final Callback<V> callback = listenerList.get(i);
                callback.on(value);
            }
        }
    }
}
