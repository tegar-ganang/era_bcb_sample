package net.siuying.any2rss.handler;

import java.net.URL;
import de.nava.informa.core.ChannelBuilderIF;
import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;
import org.apache.commons.configuration.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base of ContentHandler class, provide common implementations
 * 
 * @author Francis Chong
 * @version $Revision: 1.4 $
 */
public abstract class AbstractContentHandler implements ContentHandlerIF {

    private static Log log = LogFactory.getLog(AbstractContentHandler.class);

    private ChannelIF channel;

    private ChannelBuilderIF builder;

    /**
     * Creates a new ContentHandler object.
     * 
     */
    public AbstractContentHandler() {
    }

    /**
     * Creates a new AbstractContentHandler object.
     * 
     * @param builder ChannelBuilder used to create channel
     */
    public AbstractContentHandler(ChannelBuilderIF builder) {
        setChannelBuilder(builder);
    }

    /**
     * Return the channel builder used to create channel
     * 
     * @return the channel builder used to build channel
     * @link <a
     *       herf="http://informa.sourceforge.net/apidocs/de/nava/informa/core/ChannelBuilderIF.html">ChannelBuilderIF</a>
     */
    public ChannelBuilderIF getChannelBuilder() {
        return builder;
    }

    public void setChannelBuilder(ChannelBuilderIF builder) {
        this.builder = builder;
        this.channel = builder.createChannel("");
    }

    /**
     * Create a new instance of Content Handler, as this class is abstract, this
     * method is not implemented
     * 
     * @param builder DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    public ContentHandlerIF createContentHandler(ChannelBuilderIF builder) {
        log.fatal("AbstractContentHandler.getContentHandler() is called, " + "this class is abstract and this method is disabled");
        throw new IllegalArgumentException("Cannot create abstract class");
    }

    /**
     * handle the input string by extract information and build a Channel. The
     * function should use setSite(), setTitle(), setLocation(),
     * setDescription() and addItem() to create the channel
     * 
     * @param content String of text that contains information that could be
     *            extracted
     * 
     * @throws ContentHandlerException Error handling content
     */
    public abstract ChannelIF handle(String content) throws ContentHandlerException;

    /**
     * Set the link to the site
     * 
     * @param url The link to the site
     */
    public void setSite(URL url) {
        if (channel == null) throw new NullPointerException("ChannelBuilder has not been set!");
        log.info("site: " + url);
        channel.setSite(url);
    }

    /**
     * Set the title of this channel
     * 
     * @param link
     */
    public void setTitle(String title) {
        if (channel == null) throw new NullPointerException("ChannelBuilder has not been set!");
        log.info("title: " + title);
        channel.setTitle(title);
    }

    /**
     * Set the generator of this channel
     * 
     * @param link
     */
    public void setGenerator(String generator) {
        if (channel == null) throw new NullPointerException("ChannelBuilder has not been set!");
        log.info("generator: " + generator);
        channel.setGenerator(generator);
    }

    /**
     * Set the URL where this RSS can be retrieved from.
     * 
     * @param link
     */
    public void setLocation(URL link) {
        if (channel == null) throw new NullPointerException("ChannelBuilder has not been set!");
        log.info("location: " + link);
        channel.setLocation(link);
    }

    /**
     * Set the description of this channel
     * 
     * @param description Description of this channel
     */
    public void setDescription(String description) {
        if (channel == null) throw new NullPointerException("ChannelBuilder has not been set!");
        log.info("description: " + description);
        channel.setDescription(description);
    }

    public String getDescription() {
        if (channel == null) throw new NullPointerException("ChannelBuilder has not been set!");
        return channel.getDescription();
    }

    public String getGenerator() {
        if (channel == null) throw new NullPointerException("ChannelBuilder has not been set!");
        return channel.getGenerator();
    }

    /**
     * Add an item to the Channel
     * 
     * @param item the item to be added
     * @return ItemIF item added
     */
    public ItemIF addItem(ItemIF item) {
        if (channel == null) throw new NullPointerException("ChannelBuilder has not been set!");
        if (log.isInfoEnabled()) {
            log.info("item: " + item);
        }
        channel.addItem(item);
        return item;
    }

    /**
     * Add an item to the Channel
     * 
     * @param title Title of the iten
     * @param description Description of the item
     * @param link Link of the item
     */
    public ItemIF addItem(String title, String description, URL link) {
        if (channel == null) throw new NullPointerException("ChannelBuilder has not been set!");
        log.debug("AddItem: " + title + ", " + description + ", " + link);
        ItemIF item = builder.createItem(channel, title, description, link);
        addItem(item);
        return item;
    }

    /**
     * Retrieve the channel, must be called after handle()
     * 
     * @return parsed Channel
     */
    public ChannelIF getChannel() {
        if (channel == null) throw new NullPointerException("ChannelBuilder has not been set!");
        return channel;
    }

    /**
     * Set the link to the site
     * 
     * @param url The link to the site
     */
    public URL getSite() {
        if (channel == null) throw new NullPointerException("ChannelBuilder has not been set!");
        return channel.getSite();
    }

    /**
     * Get the title of this channel
     * 
     * @param link
     */
    public String getTitle() {
        if (channel == null) throw new NullPointerException("ChannelBuilder has not been set!");
        return channel.getTitle();
    }

    /**
     * Get the URL where this RSS can be retrieved from.
     * 
     * @param link
     */
    public URL getLocation() {
        if (channel == null) throw new NullPointerException("ChannelBuilder has not been set!");
        return channel.getLocation();
    }

    /**
     * Configure the ChannelHandler with the ConfiguratorIF
     * 
     * @param config
     */
    public abstract void configure(Configuration config) throws ConfigurationException;
}
