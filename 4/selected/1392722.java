package org.rdv.rbnb;

import java.util.Iterator;
import com.rbnb.sapi.ChannelTree;

/**
 * A collection of RBNB helper methods.
 * 
 * @author Jason P.Hanley
 */
public class RBNBHelper {

    /**
   * Calculate the start and end time based on the currently subscribed channels
   * time bounds. If no channels are subscribed all available channels will be
   * used for this calculation.
   */
    public static TimeRange getChannelsTimeRange() {
        double startTime = -1;
        double endTime = -1;
        RBNBController rbnb = RBNBController.getInstance();
        ChannelTree channelTree = rbnb.getMetadataManager().getMetadataChannelTree();
        if (channelTree == null) {
            startTime = 0;
            endTime = Double.MAX_VALUE;
        } else {
            boolean hasSubscribedChannels = rbnb.hasSubscribedChannels();
            startTime = -1;
            endTime = -1;
            Iterator it = channelTree.iterator();
            while (it.hasNext()) {
                ChannelTree.Node node = (ChannelTree.Node) it.next();
                ChannelTree.NodeTypeEnum type = node.getType();
                if (type != ChannelTree.CHANNEL) {
                    continue;
                }
                String channelName = node.getFullName();
                if (rbnb.isSubscribed(channelName) || !hasSubscribedChannels) {
                    double channelStart = node.getStart();
                    double channelDuration = node.getDuration();
                    double channelEnd = channelStart + channelDuration;
                    if (startTime == -1 || channelStart < startTime) {
                        startTime = channelStart;
                    }
                    if (endTime == -1 || channelEnd > endTime) {
                        endTime = channelEnd;
                    }
                }
            }
            if (startTime == -1 || endTime == -1) {
                startTime = 0;
                endTime = Double.MAX_VALUE;
            }
        }
        return new TimeRange(startTime, endTime);
    }
}
