package org.xaware.server.engine.channel.http;

import org.xaware.server.engine.IChannelKey;
import org.xaware.server.engine.IScopedChannel;
import org.xaware.server.engine.IScriptNode;
import org.xaware.shared.util.XAwareException;

/**
 * This class implements a way to create and retrieve a HttpTemplate that will have an InputStream or OutputStream
 * behind it.
 * 
 * @author jtarnowski
 */
public class HttpTemplateFactory {

    protected static final String className = HttpTemplateFactory.class.getName();

    /**
     * Protected constructor.
     */
    protected HttpTemplateFactory() {
    }

    /**
     * Get the HttpTemplate, creating a new one if necessary
     * 
     * @param bizDriver - a HttpBizDriver
     * @param node IScriptNode that can hopefully call getChannelScope().getScopedChannel method
     * @return HttpTemplate that should never be null
     * @throws XAwareException
     */
    public HttpTemplate getTemplate(final HttpBizDriver bizDriver, IScriptNode node) throws XAwareException {
        final IChannelKey key = bizDriver.getChannelSpecificationKey();
        HttpTemplate template = (HttpTemplate) node.getChannelScope().getScopedChannel(IScopedChannel.Type.HTTP, key);
        if (template == null) {
            template = new HttpTemplate(bizDriver);
            node.getChannelScope().setScopedChannel(key, template);
        }
        return template;
    }
}
