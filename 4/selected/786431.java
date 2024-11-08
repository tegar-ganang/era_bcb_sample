package com.jetigy.magicbus.event.bus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.jetigy.magicbus.event.EventVetoException;

/**
 * 
 * @author Tim
 * 
 */
public abstract class AbstractEventTopicDispatcher implements EventTopicDispatcher {

    private Set<EventListener> listeners = new HashSet<EventListener>();

    private Map<ChannelEventType, Method> channelMap = new HashMap<ChannelEventType, Method>();

    /**
   * Add A listener
   */
    public synchronized void addListener(EventListener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    /**
   * Remove a listener
   */
    public synchronized void removeListener(EventListener l) {
        if (l != null) {
            listeners.remove(l);
        }
    }

    /**
   * Retrieve a copy of the List of EventListeners.
   * 
   * @return List<EventListener>
   */
    protected synchronized List<EventListener> getListeners() {
        List<EventListener> list = new ArrayList<EventListener>(listeners.size());
        for (EventListener listener : listeners) {
            list.add(listener);
        }
        return list;
    }

    /**
   * Get the Map of channle types to Methods
   * 
   * @return The channelMap
   */
    protected Map<ChannelEventType, Method> getChannelMap() {
        return channelMap;
    }

    public void addChannel(ChannelEventType type, Method method) {
        getChannelMap().put(type, method);
    }

    /**
   * Fire the Event
   * 
   * @param event
   */
    protected void fireEvent(ChanneledEvent event) throws EventVetoException {
        Method method = getChannelMap().get(event.getEventType());
        try {
            List<EventListener> listeners = getListeners();
            for (EventListener listener : listeners) {
                method.invoke(listener, new Object[] { event });
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to access " + method, e);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof EventVetoException) {
                throw (EventVetoException) e.getTargetException();
            }
            throw new EventBusException("unable to deliver event on channel " + event.getEventType(), e);
        }
    }
}
