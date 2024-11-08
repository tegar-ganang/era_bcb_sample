package org.xaware.server.engine.channel.smtp;

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
public class SmtpTemplateFactory {

    protected static final String className = SmtpTemplateFactory.class.getName();

    /**
     * Protected constructor.
     */
    protected SmtpTemplateFactory() {
    }

    /**
     * Get the SmtpTemplate, creating a new one if necessary
     * 
     * @param bizDriver - a SmtpBizDriver
     * @param node IScriptNode that can hopefully call getChannelScope().getScopedChannel method
     * @return SmtpTemplate that should never be null
     * @throws XAwareException
     */
    public SmtpTemplate getTemplate(final SmtpBizDriver bizDriver, IScriptNode node) throws XAwareException {
        final IChannelKey key = bizDriver.getChannelSpecificationKey();
        SmtpTemplate template = (SmtpTemplate) node.getChannelScope().getScopedChannel(IScopedChannel.Type.SMTP, key);
        if (template == null) {
            template = new SmtpTemplate(bizDriver);
            node.getChannelScope().setScopedChannel(key, template);
        }
        return template;
    }
}
