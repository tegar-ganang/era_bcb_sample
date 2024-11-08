package com.sun.sgs.impl.service.channel;

import com.sun.sgs.impl.profile.ProfileCollectorImpl;
import com.sun.sgs.management.ChannelServiceMXBean;
import com.sun.sgs.profile.AggregateProfileOperation;
import com.sun.sgs.profile.ProfileCollector;
import com.sun.sgs.profile.ProfileCollector.ProfileLevel;
import com.sun.sgs.profile.ProfileConsumer;
import com.sun.sgs.profile.ProfileConsumer.ProfileDataType;
import com.sun.sgs.profile.ProfileOperation;

/**
 * The Statistics MBean object for the channel service.
 */
class ChannelServiceStats implements ChannelServiceMXBean {

    final ProfileOperation createChannelOp;

    final ProfileOperation getChannelOp;

    ChannelServiceStats(ProfileCollector collector) {
        ProfileConsumer consumer = collector.getConsumer(ProfileCollectorImpl.CORE_CONSUMER_PREFIX + "ChannelService");
        ProfileLevel level = ProfileLevel.MAX;
        ProfileDataType type = ProfileDataType.TASK_AND_AGGREGATE;
        createChannelOp = consumer.createOperation("createChannel", type, level);
        getChannelOp = consumer.createOperation("getChannel", type, level);
    }

    /** {@inheritDoc} */
    public long getCreateChannelCalls() {
        return ((AggregateProfileOperation) createChannelOp).getCount();
    }

    /** {@inheritDoc} */
    public long getGetChannelCalls() {
        return ((AggregateProfileOperation) getChannelOp).getCount();
    }
}
