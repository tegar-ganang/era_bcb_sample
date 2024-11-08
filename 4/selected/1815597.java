package org.xaware.server.engine.channel.jmx;

import org.xaware.server.engine.IBizDriver;
import org.xaware.server.engine.IChannelKey;
import org.xaware.server.engine.IScopedChannel;
import org.xaware.server.engine.IScriptNode;
import org.xaware.shared.util.XAwareException;

/**
 * This class implements a way to create and retrieve a JMXTemplate that will have a pooled connection behind it.
 * 
 * @author blueAlly
 */
public class JMXTemplateFactory implements IJMXTemplateFactory {

    public JMXTemplate getJMXTemplate(IBizDriver bizDriver, IScriptNode node) throws XAwareException {
        final IChannelKey key = bizDriver.getChannelKey();
        JMXTemplate template = (JMXTemplate) node.getChannelScope().getScopedChannel(IScopedChannel.Type.JMX, key);
        if (template == null) {
            template = (JMXTemplate) bizDriver.createTemplate();
            node.getChannelScope().setScopedChannel(key, template);
        }
        return template;
    }
}
