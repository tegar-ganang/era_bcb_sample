package org.simpleframework.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.simpleframework.http.store.Storage;
import org.simpleframework.util.net.Part;

/**
 * THIS IS THE MAIN CONSUMER FOR A HTTP/1.1 body
 * 
 * 
 * @author Niall Gallagher
 */
class EntityConsumer implements BodyConsumer, Entity {

    private ConsumerFactory factory;

    private RequestHeader header;

    private BodyConsumer body;

    private Channel channel;

    public EntityConsumer(Storage storage, PartHeader header, Channel channel) {
        this.factory = new ConsumerFactory(storage, header);
        this.body = factory.getInstance();
        this.header = header;
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
    }

    public RequestHeader getHeader() {
        return header;
    }

    public String getContent(String charset) {
        return body.getContent(charset);
    }

    public InputStream getInputStream() {
        return body.getInputStream();
    }

    public List<Part> getParts() {
        return body.getParts();
    }

    public int size() {
        return body.size();
    }

    public void consume(Cursor cursor) throws IOException {
        body.consume(cursor);
    }

    public boolean isFinished() {
        return body.isFinished();
    }
}
