package org.actioncenters.listeners.contributionsservice.cachemanagement;

import org.actioncenters.cometd.channelcache.ChannelCacheController;
import org.actioncenters.core.constants.Channels;
import org.actioncenters.core.contribution.data.IRelationship;
import org.actioncenters.core.contribution.svc.notification.listeners.IRemoveRelationshipListener;

/**
 * The Class RemoveRelationshipByParentRelationshipListener.
 * 
 * @author dougk
 */
public class RemoveRelationshipByParentRelationshipListener implements IRemoveRelationshipListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public void relationshipRemoved(IRelationship relationship) {
        String channel = getChannelName(relationship);
        ChannelCacheController.getChannelCache().remove(channel);
    }

    /**
     * Gets the add channel name so that we can remove the cache for this channel.
     * 
     * @param relationship
     *            the relationship
     * 
     * @return the channel name
     */
    private String getChannelName(IRelationship relationship) {
        StringBuffer returnValue = new StringBuffer();
        returnValue.append("/").append(Channels.CONTRIBUTIONS).append("/").append(Channels.RELATIONSHIP).append("/").append(relationship.getSuperior().getId()).append("/").append(relationship.getRelationship()).append("/").append(relationship.getSubordinate().getType()).append("/").append(Channels.ADD);
        return returnValue.toString();
    }
}
