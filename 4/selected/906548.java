package org.simpleframework.http;

import org.simpleframework.http.store.Storage;

class TransactionEvent implements Event {

    private Storage storage;

    private Channel channel;

    private Entity entity;

    public TransactionEvent(Entity entity, Storage storage, Channel channel) {
        this.storage = storage;
        this.channel = channel;
        this.entity = entity;
    }

    public Transaction getTransaction() {
        return new Transaction(entity, storage, channel);
    }

    public Entity getEntity() {
        return entity;
    }

    public Channel getChannel() {
        return channel;
    }
}
