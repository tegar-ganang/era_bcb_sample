package org.actioncenters.preloadcomponentcontributionsqueue;

import java.util.List;
import org.actioncenters.cometd.cache.channel.ChannelCacheController;
import org.actioncenters.core.constants.Channels;
import org.actioncenters.core.constants.ReservedTypes;
import org.actioncenters.core.contribution.data.IContribution;
import org.actioncenters.core.contribution.data.extentions.IPopulationRule;
import org.actioncenters.core.contribution.svc.exception.ContributionPropertyNotFoundException;
import org.actioncenters.core.message.IMessage;
import org.actioncenters.core.message.IMessageData;
import org.actioncenters.core.message.Message;
import org.actioncenters.core.message.MessageData;
import org.actioncenters.core.message.MessagePriority;
import org.actioncenters.core.spring.ApplicationContextHelper;
import org.actioncenters.messaging.ActiveMQStarter;
import org.actioncenters.messaging.MessagePublisher;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;

/**
 * Preloaded component contributions loader.
 *
 * @author dougk
 */
public class PreloadComponentContributionsLoader {

    /** Logger. */
    private static Logger log = Logger.getLogger(PreloadComponentContributionsLoader.class.getName());

    /** Application context. */
    private static ApplicationContext ac = ApplicationContextHelper.getApplicationContext("actioncenters.xml");

    /** Publisher. */
    private static MessagePublisher publisher = (MessagePublisher) ac.getBean("preloadComponentContributionsPublisher");

    static {
        ActiveMQStarter.startBroker();
    }

    /** Instantiates a new controller. */
    private PreloadComponentContributionsLoader() {
    }

    /**
     * {@inheritDoc}
     * @throws ContributionPropertyNotFoundException
     */
    public static void loadSubordinates(String contributionId, List<IPopulationRule> populationRules) throws ContributionPropertyNotFoundException {
        if (contributionId != null && populationRules != null) {
            for (IPopulationRule populationRule : populationRules) {
                String channelName = getChannelName(contributionId, populationRule);
                if (!ChannelCacheController.isKeyInCache(channelName)) {
                    IMessage message = new Message();
                    IMessageData messageData = new MessageData(new PreloadComponentContributionsMessage(contributionId, populationRule, populationRules));
                    message.setMessageData(messageData);
                    message.setPriority(MessagePriority.MEDIUM);
                    publisher.sendMessage(message);
                    if (log.isDebugEnabled()) {
                        log.debug("load subordinates message sent");
                    }
                }
            }
        }
    }

    /**
     * Gets the channel name.
     *
     * @param contributionId
     *          the contribution id
     * @param populationRule
     *          the population rule
     * @return the channel name
     * @throws ContributionPropertyNotFoundException
     *          the contribution property not found exception
     */
    private static String getChannelName(String contributionId, IPopulationRule populationRule) throws ContributionPropertyNotFoundException {
        StringBuffer returnValue = new StringBuffer();
        String relationship = populationRule.getPropertyValue(ReservedTypes.POPULATION_RULE_RELATIONSHIP_TO_SUPERIOR);
        String subordinateType = populationRule.getPropertyValue(ReservedTypes.POPULATION_RULE_SUBORDINATE_TYPE);
        returnValue.append("/").append(Channels.CONTRIBUTIONS).append("/").append(Channels.RELATIONSHIP).append("/").append(contributionId).append("/").append(relationship).append("/").append(subordinateType).append("/").append(Channels.ADD);
        return returnValue.toString();
    }

    /**
     * Preload the channel.
     *
     * @param contributionId
     *          the contribution id
     * @param populationRule
     *          the population rule
     * @param populationRules
     *          the population rules
     * @throws ContributionPropertyNotFoundException
     *          the contribution property not found exception
     */
    public static void preloadChannel(String contributionId, IPopulationRule populationRule, List<IPopulationRule> populationRules) throws ContributionPropertyNotFoundException {
        String channelName = getChannelName(contributionId, populationRule);
        if (!ChannelCacheController.isKeyInCache(channelName)) {
            List<IContribution> contributions = ChannelCacheController.getContributionList(channelName);
            for (IContribution contribution : contributions) {
                for (IPopulationRule pr : populationRules) {
                    IMessage message = new Message();
                    IMessageData messageData = new MessageData(new PreloadComponentContributionsMessage(contribution.getId(), pr, populationRules));
                    message.setMessageData(messageData);
                    message.setPriority(MessagePriority.MEDIUM);
                    publisher.sendMessage(message);
                    if (log.isDebugEnabled()) {
                        log.debug("load subordinates message sent");
                    }
                }
            }
        }
    }
}
