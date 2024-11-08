package com.sun.sgs.impl.app.profile;

import com.sun.sgs.app.Channel;
import com.sun.sgs.app.ChannelListener;
import com.sun.sgs.app.ChannelManager;
import com.sun.sgs.app.Delivery;

/**
 * This is implementation of {@code ChannelManager} simply calls its 
 * backing manager for each manager method. 
 */
public class ProfileChannelManager implements ChannelManager {

    private final ChannelManager backingManager;

    /**
     * Creates an instance of <code>ProfileChannelManager</code>.
     *
     * @param backingManager the <code>ChannelManager</code> to call through to
     */
    public ProfileChannelManager(ChannelManager backingManager) {
        this.backingManager = backingManager;
    }

    /**
     * {@inheritDoc}
     */
    public Channel createChannel(String name, ChannelListener listener, Delivery delivery) {
        return backingManager.createChannel(name, listener, delivery);
    }

    /**
     * {@inheritDoc}
     */
    public Channel getChannel(String name) {
        return backingManager.getChannel(name);
    }
}
