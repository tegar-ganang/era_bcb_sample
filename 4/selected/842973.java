package org.simpleframework.http;

import org.simpleframework.util.net.Part;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

class Payload implements Entity {

    private RequestHeader header;

    private Channel channel;

    private Body body;

    public Payload(RequestHeader header, Channel channel, Body body) {
        this.header = header;
        this.channel = channel;
        this.body = body;
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
}
