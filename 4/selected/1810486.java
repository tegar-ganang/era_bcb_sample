package uapp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import uapp.bus.Channel;
import uapp.bus.Message;
import uapp.bus.Subscriber;

@SuppressWarnings("serial")
public abstract class Widget implements Serializable, Subscriber {

    public static final String EXTENSION = ".html";

    private Page page;

    private String id;

    private String name;

    private String description;

    private String location;

    private WidgetMode mode;

    private Channel channel;

    protected Widget(WidgetMode initialMode) {
        setMode(initialMode);
    }

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
        if (getPage() != null) getPage().getApplication().subscribe(this);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }

    public WidgetMode getMode() {
        return mode;
    }

    protected void setMode(WidgetMode mode) {
        this.mode = mode;
        onModeChange();
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
    }

    public void view() {
        setMode(WidgetMode.View);
    }

    public void configure() {
        setMode(WidgetMode.Configuration);
    }

    public void help() {
        setMode(WidgetMode.Help);
    }

    protected void onModeChange() {
    }

    public List<Class<Message>> getListeningMessages() {
        return new ArrayList<Class<Message>>();
    }

    public void receive(Message message) {
    }
}
