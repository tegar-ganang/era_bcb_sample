package org.psepr.services.service;

import java.util.Iterator;
import org.psepr.services.ServiceStatus;

/**
 * Describes the use of a channel. Gives some text and a set of events
 * that are send and received.
 * @author radams1
 */
public class ChannelUseDescription extends ServiceDescriptionObject {

    private String channelName = null;

    private String channelDesc = null;

    private long messagesPerHour = 0;

    private long kBytesPerHour = 0;

    private long leaseLengthSeconds = 0;

    private EventDescriptionCollection sendEvents = new EventDescriptionCollection();

    private EventDescriptionCollection receiveEvents = new EventDescriptionCollection();

    public ChannelUseDescription() {
        super();
    }

    public ChannelUseDescription(String chan, String desc, long mph, long kbph) {
        super();
        this.setChannelName(chan);
        this.setChannelDesc(desc);
        this.setMessagesPerHour(mph);
        this.setKBytesPerHour(kbph);
    }

    public ChannelUseDescription(String chan, String desc, long mph, long kbph, EventDescriptionCollection send, EventDescriptionCollection receive) {
        super();
        this.setChannelName(chan);
        this.setChannelDesc(desc);
        this.setMessagesPerHour(mph);
        this.setKBytesPerHour(kbph);
        this.setSendEvents(send);
        this.setReceiveEvents(receive);
    }

    public String getChannelDesc() {
        return channelDesc;
    }

    public void setChannelDesc(String channelDesc) {
        this.channelDesc = channelDesc;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public long getKBytesPerHour() {
        return kBytesPerHour;
    }

    public void setKBytesPerHour(long bytesPerMinute) {
        kBytesPerHour = bytesPerMinute;
    }

    public long getMessagesPerHour() {
        return messagesPerHour;
    }

    public void setMessagesPerHour(long messagesPerMinute) {
        this.messagesPerHour = messagesPerMinute;
    }

    public long getLeaseLengthSeconds() {
        return leaseLengthSeconds;
    }

    public void setLeaseLengthSeconds(long leaseLengthSeconds) {
        this.leaseLengthSeconds = leaseLengthSeconds;
    }

    public EventDescriptionCollection getReceiveEvents() {
        return receiveEvents;
    }

    public void setReceiveEvents(EventDescriptionCollection receiveEvents) {
        this.receiveEvents = receiveEvents;
    }

    public EventDescriptionCollection getSendEvents() {
        return sendEvents;
    }

    public void setSendEvents(EventDescriptionCollection sendEvents) {
        this.sendEvents = sendEvents;
    }

    public void addSendEvent(EventDescription ed) {
        sendEvents.add(ed);
    }

    public void addReceiveEvent(EventDescription ed) {
        receiveEvents.add(ed);
    }

    public String toXML(String namespace) {
        String pre = (namespace == null) ? "" : namespace + ":";
        StringBuffer buff = new StringBuffer();
        buff.append("<" + pre + "channelUseDescription>\n");
        outThing(buff, pre, "channelName", this.getChannelName());
        outThing(buff, pre, "channelDesc", this.getChannelDesc());
        outThing(buff, pre, "messagesPerHour", this.getMessagesPerHour());
        outThing(buff, pre, "kBytesPerHour", this.getKBytesPerHour());
        outThing(buff, pre, "leaseLengthSeconds", this.getLeaseLengthSeconds());
        if (this.receiveEvents != null) {
            buff.append("<" + pre + "receiveEvents>");
            for (Iterator<EventDescription> ii = receiveEvents.iterator(); ii.hasNext(); ) {
                buff.append(ii.next().toXML(namespace));
            }
            buff.append("</" + pre + "receiveEvents>");
        }
        if (this.sendEvents != null) {
            buff.append("<" + pre + "sendEvents>");
            for (Iterator<EventDescription> ii = sendEvents.iterator(); ii.hasNext(); ) {
                buff.append(ii.next().toXML(namespace));
            }
            buff.append("</" + pre + "sendEvents>");
        }
        buff.append("</" + pre + "channelUseDescription>\n");
        return buff.toString();
    }

    private void outThing(StringBuffer buff, String pre, String element, String value) {
        if (value != null) {
            buff.append("<");
            buff.append(pre);
            buff.append(element);
            buff.append(">");
            buff.append(ServiceStatus.escapeXMLSpecial(value));
            buff.append("</");
            buff.append(pre);
            buff.append(element);
            buff.append(">/n");
        }
    }

    private void outThing(StringBuffer buff, String pre, String element, long value) {
        outThing(buff, pre, element, Long.toString(value));
    }
}
