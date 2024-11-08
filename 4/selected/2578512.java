package org.simpleframework.http;

import org.simpleframework.http.store.Storage;

class Transaction {

    private Storage storage;

    private Entity entity;

    private Channel channel;

    public Transaction(Entity entity, Storage storage, Channel channel) {
        this.entity = entity;
        this.storage = storage;
        this.channel = channel;
    }

    public Entity getEntity() {
        return entity;
    }

    public Storage getStorage() {
        return storage;
    }

    public Channel getChannel() {
        return channel;
    }
}
