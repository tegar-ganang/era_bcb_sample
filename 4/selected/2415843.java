package org.xaware.server.engine.channel.file;

import org.xaware.server.engine.IChannelKey;
import org.xaware.server.engine.IScopedChannel;
import org.xaware.server.engine.IScriptNode;
import org.xaware.shared.util.XAwareException;

/**
 * @deprecated
 * This class implements a way to create and retrieve a FileTemplate that will have an InputStream or OutputStream
 * behind it.
 * 
 * @author jtarnowski
 */
public class FileTemplateFactory54 {

    protected static final String className = FileTemplateFactory54.class.getName();

    /**
     * Protected constructor.
     */
    protected FileTemplateFactory54() {
    }

    /**
     * Get the FileTemplate, creating a new one if necessary
     * 
     * @param bizDriver - a FileBizDriver
     * @param node IScriptNode that can hopefully call getChannelScope().getScopedChannel method
     * @return FileTemplate that should never be null
     * @throws XAwareException
     */
    public FileTemplate54 getTemplate(final FileBizDriver bizDriver, IScriptNode node) throws XAwareException {
        final IChannelKey key = bizDriver.getChannelKey();
        FileTemplate54 template = (FileTemplate54) node.getChannelScope().getScopedChannel(IScopedChannel.Type.FILE, key);
        if (template == null) {
            template = new FileTemplate54(bizDriver);
            node.getChannelScope().setScopedChannel(key, template);
        }
        return template;
    }
}
