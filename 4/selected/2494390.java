package org.actioncenters.listeners.contributionsservice;

import org.actioncenters.cometd.clientservlet.CometClientServlet;
import org.actioncenters.core.constants.Channels;
import org.actioncenters.core.contribution.data.IContribution;
import org.actioncenters.core.contribution.data.IRelationship;
import org.actioncenters.core.contribution.svc.notification.listeners.IRemoveRelationshipListener;
import org.cometd.Channel;

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
        if (someoneIsListening(channel)) {
            publish(relationship.getSubordinate(), channel);
        }
    }

    /**
     * Publish.
     * 
     * @param contribution
     *            the contribution
     * @param channelName
     *            the channel name
     */
    private void publish(IContribution contribution, String channelName) {
        Channel channel = CometClientServlet.getBayeux().getChannel(channelName, false);
        if (channel != null) {
            channel.publish(CometClientServlet.getClient(), JSONConverter.convertToJSON(contribution), channelName);
        }
    }

    /**
     * Someone is listening.
     * 
     * @param channel
     *            the channel
     * 
     * @return true, if someone is listening
     */
    private boolean someoneIsListening(String channel) {
        return CometClientServlet.getBayeux().hasChannel(channel);
    }

    /**
     * Gets the channel name.
     * 
     * @param relationship
     *            the relationship
     * 
     * @return the channel name
     */
    private String getChannelName(IRelationship relationship) {
        StringBuffer returnValue = new StringBuffer();
        returnValue.append("/").append(Channels.CONTRIBUTIONS).append("/").append(Channels.RELATIONSHIP).append("/").append(relationship.getSuperior().getId()).append("/").append(relationship.getRelationship()).append("/").append(relationship.getSubordinate().getType()).append("/").append(Channels.DELETE);
        return returnValue.toString();
    }
}
