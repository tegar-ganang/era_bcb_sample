package org.xaware.server.engine.channel.ftp;

import org.xaware.server.engine.IChannelKey;
import org.xaware.server.engine.IScopedChannel;
import org.xaware.server.engine.IScriptNode;
import org.xaware.shared.util.XAwareException;

/**
 * Factory class to generate FTPTempate.
 * 
 * @author Vasu Thadaka
 */
public class FTPTemplateFactory {

    /**
	 * Protected constructor.
	 */
    protected FTPTemplateFactory() {
    }

    /**
	 * Get the FTPTemplate, creating a new one if necessary
	 * 
	 * @param bizDriver -
	 *            a FTPBizDriver
	 * @param node
	 *            IScriptNode that can hopefully call
	 *            getChannelScope().getScopedChannel method
	 * @return FTPTemplate that should never be null
	 * @throws XAwareException
	 *             when unable to create FTPTemplate instance.
	 */
    public FTPTemplate getTemplate(final FTPBizDriver bizDriver, IScriptNode node) throws XAwareException {
        final IChannelKey key = bizDriver.getChannelSpecificationKey();
        FTPTemplate template = (FTPTemplate) node.getChannelScope().getScopedChannel(IScopedChannel.Type.FTP, key);
        if (template == null) {
            template = new FTPTemplate(bizDriver);
            node.getChannelScope().setScopedChannel(key, template);
        }
        return template;
    }
}
