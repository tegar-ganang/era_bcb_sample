package org.wicketstuff.push.timer;

import java.util.ArrayList;
import java.util.List;
import org.wicketstuff.push.ChannelEvent;

/**
 * Here we are simulating a bus with this event store
 * It is an Internal class (volontary package)
 *
 *
 * @author Vincent Demay
 */
class EventStore {

    /**
	 *
	 */
    private static final long serialVersionUID = 1L;

    private final transient List<EventStoreListener> listenerList = new ArrayList<EventStoreListener>();

    private static final EventStore eventStore = new EventStore();

    public void add(final ChannelEvent value) {
        for (final EventStoreListener listener : listenerList) {
            listener.EventTriggered(value.getChannel(), value.getData());
        }
    }

    public static EventStore get() {
        return eventStore;
    }

    /**
	 * Adds a listener to this list which will be notified whenever the list is modified
	 * @param listener the listener to add
	 */
    public void addEventStoreListener(final EventStoreListener listener) {
        listenerList.add(listener);
    }
}
