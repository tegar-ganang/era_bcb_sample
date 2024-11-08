package org.xaware.server.engine.channel.file;

import org.xaware.server.engine.IChannelKey;
import org.xaware.server.engine.IScopedChannel;
import org.xaware.server.engine.IScriptNode;
import org.xaware.shared.util.XAwareException;

/**
 * This class implements a way to create and retrieve a FileTemplate that will have an InputStream or OutputStream
 * behind it.
 * 
 * @author jtarnowski
 */
public class FileTemplateFactory {

    protected static final String className = FileTemplateFactory.class.getName();

    /**
     * Protected constructor.
     */
    protected FileTemplateFactory() {
    }

    /**
     * Get the FileTemplate, creating a new one if necessary
     * 
     * @param bizDriver - a FileBizDriver
     * @param node IScriptNode that can hopefully call getChannelScope().getScopedChannel method
     * @return FileTemplate that should never be null
     * @throws XAwareException
     */
    public FileTemplate getTemplate(final FileBizDriver bizDriver, IScriptNode node) throws XAwareException {
        final IChannelKey key = bizDriver.getChannelSpecificationKey();
        FileTemplate template = (FileTemplate) node.getChannelScope().getScopedChannel(IScopedChannel.Type.FILE, key);
        if (template == null) {
            template = new FileTemplate(bizDriver);
            node.getChannelScope().setScopedChannel(key, template);
        }
        return template;
    }
}
