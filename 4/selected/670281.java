package org.xaware.server.engine.channel.ldap;

import org.springframework.ldap.core.ContextSource;
import org.xaware.server.engine.IBizDriver;
import org.xaware.server.engine.IGenericChannelTemplate;
import org.xaware.server.engine.channel.AbstractBizDriver;
import org.xaware.shared.util.XAwareException;

/**
 * The LDAPBizDriver returns a
 * {@link org.springframework.ldap.core.ContextSource} from the
 * createChannelObject() method which it is depending on the channel
 * specification and pooling channel specification to apply the correct
 * configuration information to
 * 
 * @author Vasu Thadaka
 * 
 */
public class LDAPBizDriver extends AbstractBizDriver implements IBizDriver {

    public Object createChannelObject() throws XAwareException {
        ContextSource contextSource = (ContextSource) this.m_channelSpecification.getChannelObject();
        this.m_channelPoolingSpecification.applyConfiguration(contextSource);
        return contextSource;
    }

    public IGenericChannelTemplate createTemplate() throws XAwareException {
        throw new XAwareException("Unimplemented, user createChannelObject() at this time");
    }
}
