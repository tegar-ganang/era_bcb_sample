package org.jmetis.messaging.osgi;

import java.util.Dictionary;
import java.util.Hashtable;
import org.jmetis.messaging.channel.OutboundChannel;
import org.jmetis.messaging.core.IChannelManager;
import org.jmetis.messaging.core.IMessage;
import org.jmetis.messaging.core.MessageException;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * {@code OutboundEventChannel}
 * 
 * @author era
 */
public class OutboundEventChannel<T> extends OutboundChannel<T> {

    private static final String EVENT_PAYLOAD = "payload";

    private final ServiceTracker eventAdminTracker;

    private final String eventTopic;

    /**
	 * Constructs a new {@code OutboundEventChannel} instance.
	 * 
	 */
    public OutboundEventChannel(BundleContext bundleContext, ServiceTracker eventAdminTracker, String eventTopic) {
        super();
        this.eventAdminTracker = eventAdminTracker;
        this.eventTopic = eventTopic;
    }

    protected EventAdmin eventAdmin() {
        EventAdmin eventAdmin = (EventAdmin) this.eventAdminTracker.getService();
        if (eventAdmin == null) {
            MessageException.channelNotAvailable(this.eventTopic);
        }
        return eventAdmin;
    }

    public IChannelManager getChannelManager() {
        return null;
    }

    public void sendMessage(IMessage<T> message) throws MessageException {
        Dictionary<String, Object> eventProperties = new Hashtable<String, Object>();
        eventProperties.put(OutboundEventChannel.EVENT_PAYLOAD, message);
        this.eventAdmin().postEvent(new Event(this.eventTopic, eventProperties));
    }
}
