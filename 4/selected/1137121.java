package com.volantis.mcs.runtime.configuration;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Holds configuration information about MPS.
 * 
 * @todo Make plugin configuration a public API and move this all to MPS!
 * I.e. this, along with the various Channel objects, the related RuleSet and
 * test cases, should be moved into the MPS repository once the "plugin 
 * configuration" API is made public. This will also require that the 
 * {@link com.volantis.mcs.runtime.configuration.xml.digester.MarinerDigester}
 * is made part of that public API. 
 */
public class MpsPluginConfiguration implements ApplicationPluginConfiguration {

    /**
     * The copyright statement.
     */
    private static String mark = "(c) Volantis Systems Ltd 2003.";

    /**
     * The URL to use for asset resolution from internal requests.
     */
    private String internalBaseUrl;

    /**
     * The user supplied class used to resolve recipient devices and channels.
     */
    private String messageRecipientInfo;

    /**
     * List of the channels that MPS is to use.
     */
    private List channels = new ArrayList();

    /**
     * Application name
     */
    private static final String application = "MPS";

    public String getName() {
        return application;
    }

    /**
     * Returns the Base URL from the configuration for MPS.
     * @return The Base URL
     */
    public String getInternalBaseUrl() {
        return internalBaseUrl;
    }

    /**
     * Set the Base URL for MPS.
     * @param internalBaseUrl The Base URL from the configuration
     */
    public void setInternalBaseUrl(String internalBaseUrl) {
        this.internalBaseUrl = internalBaseUrl;
    }

    /**
     * Gets the string representation of the class name for the user
     * supplied MessageRecipientInfo module for MPS.
     * @return The name of the MessageRecipientInfo class.
     */
    public String getMessageRecipientInfo() {
        return messageRecipientInfo;
    }

    /**
     * Sets the name of the MessageRecipientInfo class
     * @param messageRecipientInfo The name of the class
     */
    public void setMessageRecipientInfo(String messageRecipientInfo) {
        this.messageRecipientInfo = messageRecipientInfo;
    }

    /**
     * Get an iterator for the @link 
     * com.volantis.mcs.runtime.configuration.MpsChannelConfiguration
     * @return an Iterator of @link 
     * com.volantis.mcs.runtime.configuration.MpsChannelConfiguration
     */
    public Iterator getChannelsIterator() {
        return channels.iterator();
    }

    /**
     * Add a @link 
     * com.volantis.mcs.runtime.configuration.MpsChannelConfiguration 
     * to the configuration
     * @param channel The @link 
     * com.volantis.mcs.runtime.configuration.MpsChannelConfiguration
     */
    public void addChannel(MpsChannelConfiguration channel) {
        channels.add(channel);
    }
}
