package net.siuying.any2rss.handler;

import java.net.MalformedURLException;
import java.net.URL;
import de.nava.informa.core.ChannelBuilderIF;
import de.nava.informa.core.ChannelIF;
import org.apache.commons.configuration.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DummyContentHandler extends AbstractContentHandler {

    private static Log log = LogFactory.getLog(AbstractContentHandler.class);

    public DummyContentHandler() {
    }

    public DummyContentHandler(ChannelBuilderIF builder) {
        super(builder);
    }

    public ChannelIF handle(String content) throws ContentHandlerException {
        log.trace("DummyContentHandler handling request ... ");
        this.setTitle("Unnamed Feed");
        this.setDescription("This is a dummy feed for test");
        try {
            this.setLocation(new URL("http://hi"));
        } catch (MalformedURLException me) {
            throw new ContentHandlerException("Error Handling Content", me);
        }
        try {
            this.addItem("Test item", "Test item desc", new URL("http://foo"));
            this.addItem("Test item (2)", "Test item desc, again", new URL("http://foo2"));
        } catch (MalformedURLException e) {
            throw new ContentHandlerException("Error Handling Content", e);
        }
        log.trace("DummyContentHandler finished handling request.");
        return this.getChannel();
    }

    public void configure(Configuration config) throws ConfigurationException {
        log.info("really no configururation needed.");
    }
}
