package org.xaware.server.engine.channel.soap;

import org.xaware.server.engine.IChannelKey;
import org.xaware.server.engine.IScopedChannel;
import org.xaware.server.engine.IScriptNode;
import org.xaware.shared.util.XAwareException;

/**
 * This class implements a way to create and retrieve a SoapTemplate.
 * 
 * @author Basil Ibegbu
 */
public class SoapTemplateFactory {

    protected static final String className = SoapTemplateFactory.class.getName();

    /**
     * Protected constructor.
     */
    protected SoapTemplateFactory() {
    }

    /**
     * Get the SoapTemplate, creating a new one if necessary
     * 
     * @param bizDriver - a SoapBizDriver
     * @param node IScriptNode that can hopefully call getChannelScope().getScopedChannel method
     * @return SoapTemplate that should never be null
     * @throws XAwareException
     */
    public SoapTemplate getTemplate(final SoapBizDriver bizDriver, IScriptNode node) throws XAwareException {
        final IChannelKey key = bizDriver.getChannelSpecificationKey();
        SoapTemplate template = (SoapTemplate) node.getChannelScope().getScopedChannel(IScopedChannel.Type.SOAP, key);
        if (template == null) {
            template = (SoapTemplate) bizDriver.createChannelObject();
            node.getChannelScope().setScopedChannel(key, template);
        }
        return template;
    }
}
