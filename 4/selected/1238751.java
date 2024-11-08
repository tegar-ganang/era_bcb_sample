package org.actioncenters.listeners.contributionsservice.cachemanagement;

import org.actioncenters.cometd.cache.channel.ChannelCacheController;
import org.actioncenters.core.constants.Channels;
import org.actioncenters.core.contribution.data.IRelationship;
import org.actioncenters.core.contribution.svc.notification.listeners.IRemoveRelationshipListener;

/**
 * The Class RemoveRelationshipListener.
 * 
 * @author dougk
 */
public class RemoveRelationshipListener implements IRemoveRelationshipListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public void relationshipRemoved(IRelationship relationship) {
        String channel = Channels.getAddRelationshipChannel(relationship);
        ChannelCacheController.getChannelCache().remove(channel);
        channel = Channels.getAddRelationshipWildcardChannel(relationship);
        ChannelCacheController.getChannelCache().remove(channel);
    }
}
