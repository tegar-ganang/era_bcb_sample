package org.xaware.server.engine.channel.jms;

import javax.jms.ConnectionFactory;
import org.xaware.server.engine.IBizDriver;
import org.xaware.server.engine.IGenericChannelTemplate;
import org.xaware.server.engine.channel.AbstractBizDriver;
import org.xaware.shared.util.XAwareException;

/**
 * The SqlBizDriver returns a {@link javax.jms.ConnectionFactory} from the
 * createChannelObject() method which it is depending on the channel specification and
 * pooling channel specification to apply the correct configuration information to
 * 
 * @author jweaver
 * 
 */
public class JmsBizDriver extends AbstractBizDriver implements IBizDriver {

    /**
     * Returns a see {@link ConnectionFactory} object
     */
    public Object createChannelObject() throws XAwareException {
        Object connectionFactory = this.m_channelSpecification.getChannelObject();
        this.m_channelPoolingSpecification.applyConfiguration(connectionFactory);
        return connectionFactory;
    }

    public IGenericChannelTemplate createTemplate() throws XAwareException {
        throw new XAwareException("Unimplemented, user createChannelObject() at this time");
    }
}
