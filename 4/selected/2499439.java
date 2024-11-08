package org.actioncenters.listeners.contributionsservice.cachemanagement;

import org.actioncenters.cometd.cache.channel.ChannelCacheController;
import org.actioncenters.cometd.cache.channel.ChannelCacheEventListener;
import org.actioncenters.core.contribution.data.IContribution;
import org.actioncenters.core.contribution.svc.notification.listeners.IUpdateContributionThumbprintsListener;

/**
 * @author amametjanov
 */
public class UpdateContributionThumbprintsListener implements IUpdateContributionThumbprintsListener {

    /**
     * {@inheritDoc}
     */
    @Override
    public void contributionThumbprintsUpdated(IContribution contribution) {
        if (contribution != null && contribution.getId() != null) {
            for (String channelName : ChannelCacheEventListener.getChannelsForContributionId(contribution.getId())) {
                ChannelCacheController.getChannelCache().remove(channelName);
            }
        }
    }
}
