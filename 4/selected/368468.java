package org.eaiframework.filters.routers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eaiframework.ChannelNotFoundException;
import org.eaiframework.Message;
import org.eaiframework.MessageException;
import org.eaiframework.MessageSender;
import org.eaiframework.filters.BaseFilter;

/**
 * 
 */
public class HeaderPropertyFilter extends BaseFilter {

    private Log log = LogFactory.getLog(HeaderPropertyFilter.class);

    private String rule;

    private String channelId;

    public void doFilter(Message message) {
        HeaderPropertyRuleEvaluator ruleEvaluator = new HeaderPropertyRuleEvaluator(rule);
        boolean matches = ruleEvaluator.evaluateRule(message, null);
        MessageSender sender = filterContext.getMessageSender();
        if (matches) {
            try {
                sender.sendMessage(message, channelId);
            } catch (MessageException e) {
                log.warn(getLogHead() + "message could not be forwarded to " + channelId + ": " + e.getMessage(), e);
            } catch (ChannelNotFoundException c) {
                log.warn(getLogHead() + "channel " + channelId + " was not found: " + c.getMessage(), c);
            }
        } else {
            log.debug(getLogHead() + "message filtered: " + message);
        }
    }

    /**
	 * @return the rule
	 */
    public String getRule() {
        return rule;
    }

    /**
	 * @param rule the rule to set
	 */
    public void setRule(String rule) {
        this.rule = rule;
    }

    /**
	 * @return the channelId
	 */
    public String getChannelId() {
        return channelId;
    }

    /**
	 * @param channelId the channelId to set
	 */
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
}
