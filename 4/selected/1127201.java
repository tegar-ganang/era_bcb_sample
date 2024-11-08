package org.xaware.server.engine.channel.sf;

import org.xaware.server.engine.IBizDriver;
import org.xaware.server.engine.IChannelKey;
import org.xaware.server.engine.IScopedChannel;
import org.xaware.server.engine.IScriptNode;
import org.xaware.shared.util.XAwareException;

/**
 * This class implements a way to create and retrieve a SalesForceTemplate 
 * 
 * @author openweaver
 */
public class SalesForceTemplateFactory implements ISalesForceTemplateFactory {

    protected static final String className = SalesForceTemplateFactory.class.getName();

    /**
     * Protected constructor.
     */
    protected SalesForceTemplateFactory() {
    }

    /**
     * Get the SalesforceTemplate, creating a new one if necessary
     * 
     * @param bizDriver - a SalesForceBizDriver
     * @param node IScriptNode that can hopefully call getChannelScope().getScopedChannel method
     * @return SalesForceTemplate that should never be null
     * @throws XAwareException
     */
    public ISalesForceTemplate getTemplate(final IBizDriver bizDriver, IScriptNode node) throws XAwareException {
        final IChannelKey key = bizDriver.getChannelSpecificationKey();
        ISalesForceTemplate template = null;
        if (node != null) {
            template = (SalesForceTemplate) node.getChannelScope().getScopedChannel(IScopedChannel.Type.SF, key);
        }
        if (template == null) {
            template = (ISalesForceTemplate) bizDriver.createTemplate();
            if (node != null) {
                node.getChannelScope().setScopedChannel(key, template);
            }
        }
        return template;
    }
}
