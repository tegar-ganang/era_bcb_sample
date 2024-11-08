package com.jetigy.magicbus.event.bus;

import java.lang.reflect.Method;
import java.util.EventListener;
import java.util.List;
import com.jetigy.magicbus.event.EventVetoException;
import com.jetigy.magicbus.factory.ManufacturingException;

/**
 * @author gene
 * 
 */
public class EventBusTopic implements EventTopic {

    private String name;

    private Class<? extends ChanneledEvent> eventClass;

    private Class<? extends EventListener> listenerClass;

    private DispatchType dispatchType;

    private EventTopicDispatcher eventTopicDispatcher;

    private TopicType topicType;

    /**
   * Construct an EventBusTOpic
   * 
   * @param name
   *          The name of the Topic
   * @param c
   *          The class of the ChanneledEvent
   * @param dispatchType
   *          The Type of Dispatcher to use
   * @param l
   *          The Listener clas for the Topic
   * @param topicType
   *          The type of the topic
   * @param eventTypes
   * 					The List of supported ChannelEventTypes        
   */
    public EventBusTopic(String name, Class<? extends ChanneledEvent> c, DispatchType dispatchType, Class<? extends EventListener> l, TopicType topicType, List<? extends ChannelEventType> eventTypes) {
        this.name = name;
        this.eventClass = c;
        this.dispatchType = dispatchType;
        this.listenerClass = l;
        this.topicType = topicType;
        try {
            eventTopicDispatcher = EventTopicDispatcherFactory.createEventDispatcher(dispatchType);
        } catch (ManufacturingException e) {
            throw new IllegalStateException("Unable to create EventTopicDispatcher for " + dispatchType);
        }
        addAllChannels(eventTypes);
    }

    public void fireStateEvent(ChanneledEvent e) {
        if (topicType == TopicType.STATE) {
            if (eventClass.isAssignableFrom(e.getClass())) {
                getEventTopicDispatcher().fireStateEvent(e);
            } else {
                throw new EventBusException(e.getClass() + " events are not assignable from " + eventClass.getName());
            }
        } else {
            throw new EventBusException("cannot fire State Events via a command topic!");
        }
    }

    public void fireCommandEvent(ChanneledEvent e) throws EventVetoException {
        if (topicType == TopicType.COMMAND) {
            if (eventClass.isAssignableFrom(e.getClass())) {
                getEventTopicDispatcher().fireCommandEvent(e);
            } else {
                throw new EventBusException(e.getClass() + " events are not assignable from " + eventClass.getName());
            }
        } else {
            throw new EventBusException("cannot fire Command Events via a state topic!");
        }
    }

    public void addChannel(ChannelEventType type) {
        try {
            Method method = listenerClass.getDeclaredMethod(type.getChannel(), new Class[] { eventClass });
            eventTopicDispatcher.addChannel(type, method);
        } catch (SecurityException e) {
            throw new IllegalStateException("Unable to access " + type.getChannel(), e);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Unable to find " + type.getChannel(), e);
        }
    }

    private void addAllChannels(List<? extends ChannelEventType> eventTypes) {
        if (eventTypes != null) {
            for (ChannelEventType type : eventTypes) {
                addChannel(type);
            }
        }
    }

    /**
   * Get the EventTopicDispatcher for this EventTopic
   * 
   * @return Returns the eventTopicDispatcher.
   */
    protected EventTopicDispatcher getEventTopicDispatcher() {
        return eventTopicDispatcher;
    }

    public Class getListenerClass() {
        return listenerClass;
    }

    public String getName() {
        return name;
    }

    public DispatchType getType() {
        return dispatchType;
    }

    /**
   * Adds a listener to the EventBusTopic
   * 
   * @param listener
   * @throws EventBusException
   */
    public void addListener(EventListener listener) {
        if (listenerClass.isAssignableFrom(listener.getClass())) {
            getEventTopicDispatcher().addListener(listener);
        } else {
            throw new EventBusException(listener.getClass() + " listeners are not assignable from " + eventClass.getName());
        }
    }

    /**
   * 
   */
    public void removeListener(EventListener listener) {
        getEventTopicDispatcher().removeListener(listener);
    }
}
