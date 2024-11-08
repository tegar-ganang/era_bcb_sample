package org.simpleframework.http;

import org.simpleframework.http.store.Storage;

public class RequestEvent implements Event {

    private PartHeader header;

    private Storage storage;

    private Channel channel;

    public RequestEvent(PartHeader header, Storage storage, Channel channel) {
        this.channel = channel;
        this.storage = storage;
        this.header = header;
    }

    public Channel getChannel() {
        return channel;
    }

    public Collector getCollector() {
        return new BodyCollector(header, storage, channel);
    }

    public RequestHeader getHeader() {
        return header;
    }
}
