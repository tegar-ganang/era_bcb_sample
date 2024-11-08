package org.simpleframework.http;

import org.simpleframework.http.store.Storage;

class StartEvent implements Event {

    private Storage storage;

    private Channel channel;

    public StartEvent(Channel channel, Storage storage) {
        this.storage = storage;
        this.channel = channel;
    }

    public Collector getCollector() {
        return new HeaderCollector(storage, channel);
    }

    public Channel getChannel() {
        return channel;
    }
}
