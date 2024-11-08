package net.jetrix.filter;

import java.util.*;
import net.jetrix.*;
import net.jetrix.config.*;

/**
 * Abstract class defining a channel filter. A filter transforms a given message
 * into a list of messages. Concrete filters just need to inherit from this
 * class and implement the process() method.
 *
 * @author Emmanuel Bourg
 * @version $Revision: 794 $, $Date: 2009-02-17 14:08:39 -0500 (Tue, 17 Feb 2009) $
 */
public abstract class MessageFilter {

    private Properties props;

    private Channel channel;

    protected FilterConfig config;

    /**
     * Indicates if the filter is shared or not. A shared filter should be
     * a singleton if it's independant from the channel context (for example:
     * a color stripper or a profanity filter). By default a filter is not
     * a singleton. This method must be overwritten to return true if the
     * filter is meant to be instanciated only once.
     *
     * @return <tt>false</tt>
     */
    public boolean isShared() {
        return false;
    }

    /**
     * Called by the channel to indicate to a filter that the filter is being
     * placed into service.
     */
    public void init() {
    }

    /**
     * Set the configuration used to initialize this filter.
     */
    public void setConfig(FilterConfig config) {
        this.config = config;
    }

    /**
     * Return the configuration used to initialize this filter.
     */
    public FilterConfig getConfig() {
        return config;
    }

    /**
     * Called by the channel to indicate to a filter that the filter is being
     * taken out of service.
     */
    public void destroy() {
    }

    /**
     * Process a message and outputs messages to the specified List.
     */
    public abstract void process(Message m, List<Message> out);

    /**
     * Returns the name of this filter.
     */
    public String getName() {
        return "unknown filter";
    }

    /**
     * Returns a short description of this filter.
     */
    public String getDescription() {
        return "no description";
    }

    /**
     * Returns the version of this filter
     */
    public String getVersion() {
        return "1.0";
    }

    /**
     * Returns the author of this filter.
     */
    public String getAuthor() {
        return "unknown";
    }

    /**
     * Gets the filter property indicated by the specified key.
     */
    public final String getProperty(String key) {
        return (props == null) ? null : props.getProperty(key);
    }

    /**
     * Sets the filter property indicated by the specified key.
     */
    public final void setProperty(String key, String value) {
        if (props == null) {
            props = new Properties();
        }
        props.setProperty(key, value);
    }

    /**
     * Returns the channel this filter applies on.
     */
    public final Channel getChannel() {
        return this.channel;
    }

    /**
     * Sets the channel this filter applies on.
     */
    public final void setChannel(Channel channel) {
        if (!isShared()) {
            this.channel = channel;
        }
    }
}
