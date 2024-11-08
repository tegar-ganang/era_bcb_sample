package org.actioncenters.listeners.contributionsservice.cachemanagement;

import org.actioncenters.cometd.cache.channel.ChannelCacheController;
import org.actioncenters.core.constants.Channels;
import org.actioncenters.core.contribution.data.IContribution;
import org.actioncenters.core.contribution.data.IRelationship;
import org.actioncenters.core.contribution.svc.notification.listeners.IAddRelationshipListener;

/**
 * The Class AddRelationshipListener.
 * 
 * @author dougk
 */
public class AddRelationshipListener implements IAddRelationshipListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public void relationshipAdded(IRelationship newRelationship) {
        IContribution subordinate = newRelationship.getSubordinate();
        if (subordinate != null && subordinate.getRelationships() != null && !subordinate.getRelationships().isEmpty()) {
            for (IRelationship relationshipFromSuperior : subordinate.getRelationships()) {
                String channel = Channels.getAddRelationshipChannel(relationshipFromSuperior);
                ChannelCacheController.getChannelCache().remove(channel);
                channel = Channels.getAddRelationshipWildcardChannel(relationshipFromSuperior);
                ChannelCacheController.getChannelCache().remove(channel);
            }
        }
    }
}
