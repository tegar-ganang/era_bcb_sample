package org.simpleframework.http;

import java.io.InputStream;
import java.util.List;
import org.simpleframework.http.session.Session;
import org.simpleframework.util.net.ContentType;
import org.simpleframework.util.net.Cookie;
import org.simpleframework.util.net.Form;
import org.simpleframework.util.net.Part;
import org.simpleframework.util.net.Query;

class RequestEntity extends RequestMessage implements Request {

    private FormBuilder builder;

    private Profile profile;

    private Channel channel;

    private Entity entity;

    private Form form;

    public RequestEntity(Entity entity) {
        this.builder = new FormBuilder(this, entity);
        this.profile = new Profile(this);
        this.channel = entity.getChannel();
        this.header = entity.getHeader();
        this.entity = entity;
    }

    public Link getLink() {
        return channel.getLink();
    }

    public Cookie getCookie(String name) {
        return getState().getCookie(name);
    }

    public String getParameter(String name) {
        return getForm().get(name);
    }

    public Session getSession() {
        return profile.getSession();
    }

    public Session getSession(boolean create) {
        return profile.getSession(create);
    }

    public State getState() {
        return profile.getState();
    }

    public Form getForm() {
        if (form == null) {
            form = builder.getInstance();
        }
        return form;
    }

    public String getContent() {
        ContentType type = getContentType();
        if (type == null) {
            return entity.getContent("UTF-8");
        }
        return getContent(type);
    }

    public String getContent(ContentType type) {
        String charset = type.getCharset();
        if (type == null) {
            charset = "UTF-8";
        }
        return entity.getContent(charset);
    }

    public InputStream getInputStream() {
        return entity.getInputStream();
    }

    public List<Part> getParts() {
        return entity.getParts();
    }

    public int size() {
        return entity.size();
    }
}
