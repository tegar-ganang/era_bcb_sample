package net.siuying.any2rss.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import de.nava.informa.core.ChannelBuilderIF;
import de.nava.informa.core.ChannelIF;
import org.apache.commons.configuration.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>Nested multiple PatternContentHandler, can handler different sites</p>
 * <p>Note this class is not thread save, if you expected multiple thread access, create multiple instance of this handler</p>
 */
public class MultiPatternContentHandler extends AbstractContentHandler {

    private static Log log = LogFactory.getLog(MultiPatternContentHandler.class);

    private Map<String, PatternContentHandler> handlers;

    private String currentHandlerId;

    public static final String KEY_PREFIX = "handler.patterns.";

    public MultiPatternContentHandler() {
        super();
        handlers = new HashMap<String, PatternContentHandler>();
    }

    public void setChannelBuilder(ChannelBuilderIF builder) {
        super.setChannelBuilder(builder);
        for (PatternContentHandler handler : handlers.values()) {
            handler.setChannelBuilder(builder);
        }
    }

    public ChannelIF handle(String content) throws ContentHandlerException {
        PatternContentHandler handler = handlers.get(currentHandlerId);
        if (handler != null) {
            log.info("Using handler to handler content: " + this.currentHandlerId + ": " + handler.toString());
            return handler.handle(content);
        } else {
            log.warn("Content handler for \"" + currentHandlerId + "\" not found!");
            throw new ContentHandlerException("Content handler for \"" + currentHandlerId + "\" not found!");
        }
    }

    /**
     * Configure the handler, using the defaulr site setting. Default site name
     * is read from parameter "handler.patterns.usesite", if this value do not
     * exist, an ConfigurationException is thrown
     * 
     * @throws ConfigurationException handler.multipattern.usesite not exists,
     *             or the config under handler.multipattern.<site-name>. is
     *             invalid
     * @see net.siuying.http2rss.handler.MultiPatternContentHandler#site(String
     *      site, net.siuying.http2rss.ConfiguratorIF)
     */
    public void configure(Configuration config) throws ConfigurationException {
        if (config == null) throw new NullPointerException("Config is null!");
        String[] sites = config.getStringArray("handler.patterns.avaliable");
        for (int i = 0; i < sites.length; i++) {
            if (!sites[i].equals("")) {
                addHandler(sites[i], config);
            }
        }
        if (sites.length == 0) {
            throw new ConfigurationException("Avaliable site (handler.patterns.avaliable) is not defined!");
        }
        log.info("avaliable handlers setting: " + handlers.keySet());
        currentHandlerId = config.getString(KEY_PREFIX + "defaultsite", sites[0]);
        if (currentHandlerId == null) {
            throw new ConfigurationException("Required parameter " + KEY_PREFIX + "defaultsite is undefined.");
        }
    }

    /**
     * Read the config of the site, and set the handler to use these settings
     * 
     * <ul>
     * <li>handler.patterns.[handlerId].title(optional) Title of RSS</li>
     * <li>handler.patterns.[handlerId].url (optional) URL of RSS</li>
     * <li>handler.patterns.[handlerId].description (optional) Description of
     * RSS</li>
     * <li>handler.patterns.[handlerId].pattern (required) RegExp Pattern to
     * parse the URL, should contains THREE section</li>
     * <li>handler.patterns.[handlerId].order (required) three string
     * "link,title,description", separated by comma, in any order. If any of
     * them is omitted, their default value is used.</li>
     * </ul>
     * <small>* [site] should be replaced by site id</small>
     * 
     * The selected site can be changed by calling setSite(String name)
     */
    public void addHandler(String handlerId, Configuration config) throws ConfigurationException {
        if (config == null) {
            throw new NullPointerException("Configuration is null");
        }
        if (this.getChannelBuilder() == null) {
            throw new NullPointerException("ChannelBuilder has not been set!");
        }
        String titleKey = KEY_PREFIX + handlerId + ".title";
        String urlKey = KEY_PREFIX + handlerId + ".url";
        String descKey = KEY_PREFIX + handlerId + ".description";
        String patternKey = KEY_PREFIX + handlerId + ".pattern";
        String orderKey = KEY_PREFIX + handlerId + ".order";
        String dateKey = KEY_PREFIX + handlerId + ".date";
        String generator = config.getString("any2rss.generator", "Any2Rss");
        String title = config.getString(titleKey, "Untitled");
        String siteUrl = config.getString(urlKey, "http://nourl.org");
        String desc = config.getString(descKey, "No Descriptions");
        String datePatternStr = config.getString(dateKey);
        String itemPatternStr = null;
        String[] itemPatternOrderStr = null;
        try {
            itemPatternStr = config.getString(patternKey);
            itemPatternOrderStr = config.getStringArray(orderKey);
        } catch (NoSuchElementException ne) {
            throw new ConfigurationException("Missing required configurations.", ne);
        }
        log.info("generator=" + generator + ", " + titleKey + "=" + title + ", " + urlKey + "=" + siteUrl + ", " + descKey + "=" + desc + ", " + patternKey + "=" + itemPatternStr + ", " + orderKey + "=" + itemPatternOrderStr + ", " + dateKey + "=" + datePatternStr);
        PatternContentHandler newPattern = new PatternContentHandler();
        newPattern.setChannelBuilder(this.getChannelBuilder());
        newPattern.configure(generator, title, siteUrl, desc, itemPatternStr, itemPatternOrderStr, datePatternStr);
        handlers.put(handlerId, newPattern);
    }

    public void setHandler(String handlerId) {
        this.currentHandlerId = handlerId;
    }

    public ChannelIF getChannel() {
        if (handlers.get(this.currentHandlerId) != null) {
            return handlers.get(this.currentHandlerId).getChannel();
        } else {
            return null;
        }
    }

    public Set getSites() {
        return handlers.keySet();
    }
}
