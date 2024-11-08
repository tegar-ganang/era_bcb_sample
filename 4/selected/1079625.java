package com.jetigy.magicbus.event.bus;

import java.lang.reflect.Method;

/**
 * 
 * @author Tim
 * 
 */
public class Channel {

    private ChannelEventType channelEventType;

    private Method method;

    /**
   * @param type
   * @param method
   */
    protected Channel(ChannelEventType type, Method method) {
        channelEventType = type;
        this.method = method;
    }

    /**
   * @return Returns the channelEventType.
   */
    public ChannelEventType getChannelEventType() {
        return channelEventType;
    }

    /**
   * @return Returns the method.
   */
    public Method getMethod() {
        return method;
    }

    @Override
    public String toString() {
        return "Channel:" + channelEventType + ":" + method.getName();
    }
}
