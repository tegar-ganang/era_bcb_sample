package org.actioncenters.listeners.contributionsservice.cachemanagement;

import org.actioncenters.cometd.cache.channel.ChannelCacheController;
import org.actioncenters.core.constants.Channels;
import org.actioncenters.core.contribution.data.ISystemRole;
import org.actioncenters.core.contribution.data.IUser;
import org.actioncenters.core.contribution.svc.notification.listeners.IRemoveUserRoleListener;

/**
 * @author dkjeldgaard
 */
public class RemoveUserRoleListener implements IRemoveUserRoleListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public void userRoleRemoved(IUser user) {
        if (user != null && user.getSystemRoles() != null) {
            for (ISystemRole systemRole : user.getSystemRoles()) {
                String channel = Channels.getAddSystemUserChannel(systemRole);
                ChannelCacheController.getChannelCache().remove(channel);
            }
        }
    }
}
