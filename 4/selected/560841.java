package org.xaware.server.engine.channel.jmx;

import org.xaware.server.engine.IBizDriver;
import org.xaware.server.engine.IGenericChannelTemplate;
import org.xaware.server.engine.channel.AbstractBizDriver;
import org.xaware.shared.util.XAwareException;

/**
 * JMX BizDriver returns a JMXTemplate which provides connection to the MBeanServer.
 * 
 * @author blueAlly
 */
public class JMXBizDriver extends AbstractBizDriver implements IBizDriver {

    /**
     * (non-Javadoc)
     * 
     * @see org.xaware.server.engine.IBizDriver#createChannelObject()
     */
    public Object createChannelObject() throws XAwareException {
        throw new XAwareException("This method is deprecated. Instead use createTemplate().");
    }

    /**
     * (non-Javadoc)
     * 
     * @see org.xaware.server.engine.IBizDriver#createTemplate()
     */
    public IGenericChannelTemplate createTemplate() throws XAwareException {
        return m_channelSpecification.getChannelTemplate();
    }
}
