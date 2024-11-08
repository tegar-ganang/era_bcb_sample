package jifx.connection.configurator.filters;

import javax.management.NotificationListener;
import jifx.commons.messages.IMessage;
import org.w3c.dom.Element;

public abstract class AbstractFilter implements IFilter, NotificationListener {

    protected String channelName;

    protected int timeout;

    protected int flushTime;

    protected String incomingTopic;

    protected String outgoingTopic;

    protected String jndi;

    protected IFilter filterTM;

    protected IFilter filterTC;

    public AbstractFilter() {
    }

    public abstract void configure(Element element);

    public abstract void messageProcessTC(IMessage message);

    public abstract void messageProcessTM(IMessage message);

    public void setFilterTM(IFilter filter) {
        filterTM = filter;
    }

    public IFilter getFilterTM() {
        return filterTM;
    }

    public IFilter getFilterTC() {
        return filterTC;
    }

    public void setFilterTC(IFilter prevFilter) {
        this.filterTC = prevFilter;
    }

    public String getChannelName() {
        return this.channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getIncomingTopic() {
        return incomingTopic;
    }

    public void setIncomingTopic(String incomingTopic) {
        this.incomingTopic = incomingTopic;
    }

    public String getOutgoingTopic() {
        return outgoingTopic;
    }

    public void setOutgoingTopic(String outgoingTopic) {
        this.outgoingTopic = outgoingTopic;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getFlushTime() {
        return flushTime;
    }

    public void setFlushTime(int flushtime) {
        this.flushTime = flushtime;
    }

    public String getJndi() {
        return jndi;
    }

    public void setJndi(String jndi) {
        this.jndi = jndi;
    }
}
