package org.actioncenters.listeners.contributionsservice.cachemanagement;

import org.actioncenters.cometd.cache.channel.ChannelCacheController;
import org.actioncenters.cometd.cache.channel.ChannelCacheEventListener;
import org.actioncenters.core.contribution.data.IContribution;
import org.actioncenters.core.contribution.svc.notification.listeners.IUpdateContributionListener;

/**
 * The Class UpdateContributionListener.
 * 
 * @author dougk
 */
public class UpdateContributionListener implements IUpdateContributionListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public void contributionUpdated(IContribution contribution) {
        if (contribution != null && contribution.getId() != null) {
            for (String channelName : ChannelCacheEventListener.getChannelsForContributionId(contribution.getId())) {
                ChannelCacheController.getChannelCache().remove(channelName);
            }
        }
    }
}
