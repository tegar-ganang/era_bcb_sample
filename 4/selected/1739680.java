package net.teqlo.bus.messages.mail;

import net.teqlo.TeqloException;
import net.teqlo.bus.BusConnection;
import net.teqlo.bus.messages.ConsumerMessage;
import net.teqlo.bus.messages.VoidReply;
import net.teqlo.queue.ActionQueue;

public class ActionQueueEntry implements ConsumerMessage {

    private static final VoidReply voidReply = new VoidReply();

    private String fromAlias;

    private String toAlias;

    private String topic;

    private String channel;

    private String data;

    public ActionQueueEntry(String fromAlias, String toAlias, String topic, String channel, String data) {
        this.fromAlias = fromAlias;
        this.toAlias = toAlias;
        this.topic = topic;
        this.channel = channel;
        this.data = data;
    }

    public boolean isPersistent() {
        return true;
    }

    public String getFromAlias() {
        return fromAlias;
    }

    public String getToAlias() {
        return toAlias;
    }

    public String getTopic() {
        return topic;
    }

    public String getData() {
        return data;
    }

    public Object consumeMessage() throws TeqloException {
        ActionQueue.getInstance().handleMessage(this);
        return voidReply;
    }

    public String getMessageName() {
        return "Mail." + getClass().getSimpleName();
    }

    public String getJMSSubject() {
        return BusConnection.ACTION_SUBJECT;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }
}
