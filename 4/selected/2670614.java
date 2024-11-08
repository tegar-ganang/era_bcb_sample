package org.simpleframework.http;

import org.simpleframework.http.store.Storage;

class TransactionProcessor implements TransactionHandler {

    private Container protocol;

    private Reactor handler;

    public TransactionProcessor(Reactor handler, Container protocol) {
        this.handler = handler;
        this.protocol = protocol;
    }

    public void handle(Transaction request) throws Exception {
        Entity entity = request.getEntity();
        Storage storage = request.getStorage();
        Channel channel = request.getChannel();
        handle(entity, storage, channel);
    }

    private void handle(Entity entity, Storage storage, Channel channel) throws Exception {
        Monitor monitor = new Monitor(handler, storage, channel);
        Request request = new RequestEntity(entity);
        Response response = new ResponseEntity(request, channel, monitor);
        protocol.handle(request, response);
    }
}
