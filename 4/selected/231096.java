package org.acegisecurity.securechannel;

import org.acegisecurity.ConfigAttribute;
import org.acegisecurity.ConfigAttributeDefinition;
import org.acegisecurity.intercept.web.FilterInvocation;
import org.springframework.beans.factory.InitializingBean;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;

/**
 * Implementation of {@link ChannelDecisionManager}.
 * 
 * <p>
 * Iterates through each configured {@link ChannelProcessor}. If a
 * <code>ChannelProcessor</code> has any issue with the security of the
 * request, it should cause a redirect, exception or whatever other action is
 * appropriate for the <code>ChannelProcessor</code> implementation.
 * </p>
 * 
 * <P>
 * Once any response is committed (ie a redirect is written to the response
 * object), the <code>ChannelDecisionManagerImpl</code> will not iterate
 * through any further <code>ChannelProcessor</code>s.
 * </p>
 *
 * @author Ben Alex
 * @version $Id: ChannelDecisionManagerImpl.java,v 1.4 2005/11/17 00:55:50 benalex Exp $
 */
public class ChannelDecisionManagerImpl implements ChannelDecisionManager, InitializingBean {

    private List channelProcessors;

    public void setChannelProcessors(List newList) {
        checkIfValidList(newList);
        Iterator iter = newList.iterator();
        while (iter.hasNext()) {
            Object currentObject = null;
            try {
                currentObject = iter.next();
                ChannelProcessor attemptToCast = (ChannelProcessor) currentObject;
            } catch (ClassCastException cce) {
                throw new IllegalArgumentException("ChannelProcessor " + currentObject.getClass().getName() + " must implement ChannelProcessor");
            }
        }
        this.channelProcessors = newList;
    }

    public List getChannelProcessors() {
        return this.channelProcessors;
    }

    public void afterPropertiesSet() throws Exception {
        checkIfValidList(this.channelProcessors);
    }

    public void decide(FilterInvocation invocation, ConfigAttributeDefinition config) throws IOException, ServletException {
        Iterator iter = this.channelProcessors.iterator();
        while (iter.hasNext()) {
            ChannelProcessor processor = (ChannelProcessor) iter.next();
            processor.decide(invocation, config);
            if (invocation.getResponse().isCommitted()) {
                break;
            }
        }
    }

    public boolean supports(ConfigAttribute attribute) {
        Iterator iter = this.channelProcessors.iterator();
        while (iter.hasNext()) {
            ChannelProcessor processor = (ChannelProcessor) iter.next();
            if (processor.supports(attribute)) {
                return true;
            }
        }
        return false;
    }

    private void checkIfValidList(List listToCheck) {
        if ((listToCheck == null) || (listToCheck.size() == 0)) {
            throw new IllegalArgumentException("A list of ChannelProcessors is required");
        }
    }
}
