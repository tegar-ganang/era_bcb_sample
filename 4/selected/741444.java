package com.sun.sgs.impl.kernel;

import com.sun.sgs.app.ChannelManager;
import com.sun.sgs.app.DataManager;
import com.sun.sgs.app.TaskManager;
import com.sun.sgs.internal.ManagerLocator;

/**
 * Package-private implementation of {@code ManagerLocator} that is
 * to be used as the default locator for the
 * {@link com.sun.sgs.internal.InternalContext InternalContext}.
 * 
 * @see com.sun.sgs.internal.InternalContext#setManagerLocator
 */
class ManagerLocatorImpl implements ManagerLocator {

    public ChannelManager getChannelManager() {
        return ContextResolver.getChannelManager();
    }

    public DataManager getDataManager() {
        return ContextResolver.getDataManager();
    }

    public TaskManager getTaskManager() {
        return ContextResolver.getTaskManager();
    }

    public <T> T getManager(Class<T> type) {
        return ContextResolver.getManager(type);
    }
}
