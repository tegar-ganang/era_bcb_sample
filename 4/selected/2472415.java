package org.xmatthew.spy2servers.rule;

import org.apache.log4j.Logger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.xmatthew.spy2servers.core.MessageAlertChannel;
import org.xmatthew.spy2servers.util.CollectionUtils;

/**
 * @author Matthew Xie
 *
 */
public class SimpleAlertRule implements AlertRule {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(SimpleAlertRule.class);

    private Set<Channel> channles;

    private Map<String, Set<String>> channelRules;

    /**
     * @param channles the channles to set
     */
    public void setChannles(Set<Channel> channles) {
        this.channles = channles;
        buildRule(this.channles);
    }

    /**
	 * @return the channelRules
	 */
    public Map<String, Set<String>> getChannelRules() {
        return channelRules;
    }

    private void buildRule(Set<Channel> channles) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("bulding rule.");
        }
        if (CollectionUtils.isBlankCollection(channles)) {
            return;
        }
        channelRules = new LinkedHashMap<String, Set<String>>();
        Set<String> fromRules;
        Set<String> toRules;
        Set<String> tos;
        for (Channel channel : channles) {
            fromRules = channel.getFrom();
            if (CollectionUtils.isBlankCollection(fromRules)) {
                continue;
            }
            toRules = channel.getTo();
            if (CollectionUtils.isBlankCollection(toRules)) {
                continue;
            }
            for (String from : fromRules) {
                tos = channelRules.get(from);
                if (tos == null) {
                    channelRules.put(from, toRules);
                } else {
                    tos.addAll(toRules);
                    channelRules.put(from, tos);
                }
            }
        }
    }

    /**
     * 
     */
    public SimpleAlertRule() {
    }

    public boolean isAlertAllow(MessageAlertChannel channel) {
        if (CollectionUtils.isBlankMap(channelRules)) {
            return true;
        }
        String fromName = channel.getSpyComponent().getName();
        if (channelRules.containsKey(fromName)) {
            String toName = channel.getAlertComponent().getName();
            Set<String> toRules = channelRules.get(fromName);
            if (toRules.contains(toName)) {
                return true;
            }
        }
        return false;
    }
}
