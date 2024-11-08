package org.actioncenters.listeners.contributionsservice.cachemanagement;

import org.actioncenters.cometd.cache.channel.ChannelCacheController;
import org.actioncenters.core.constants.Channels;
import org.actioncenters.core.contribution.data.ISystemRole;
import org.actioncenters.core.contribution.data.IUser;
import org.actioncenters.core.contribution.svc.notification.listeners.IAddUserRoleListener;

/**
 * @author dkjeldgaard
 */
public class AddUserRoleListener implements IAddUserRoleListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public void userRoleAdded(IUser user) {
        if (user != null && user.getSystemRoles() != null) {
            for (ISystemRole systemRole : user.getSystemRoles()) {
                String channel = Channels.getAddSystemUserChannel(systemRole);
                ChannelCacheController.getChannelCache().remove(channel);
            }
        }
    }
}
