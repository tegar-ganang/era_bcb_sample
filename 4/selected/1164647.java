package org.eaiframework.config.xml;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.digester.Digester;
import org.eaiframework.ChannelManager;
import org.eaiframework.config.ConfigurationException;
import org.xml.sax.SAXException;

/**
 * 
 */
public class ChannelsConfigHelper {

    private ChannelManager channelManager;

    public void configure(InputStream channelsConfigFile) throws ConfigurationException {
        Digester digester = new Digester();
        digester.setClassLoader(this.getClass().getClassLoader());
        digester.setValidating(false);
        digester.push(channelManager);
        digester.addObjectCreate("channels/channel", "org.eaiframework.Channel");
        digester.addSetNext("channels/channel", "createChannel", "org.eaiframework.Channel");
        digester.addSetProperties("channels/channel");
        try {
            digester.parse(channelsConfigFile);
        } catch (IOException e) {
            throw new ConfigurationException(e);
        } catch (SAXException e) {
            throw new ConfigurationException(e);
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(e);
        }
    }

    /**
	 * @return the channelManager
	 */
    public ChannelManager getChannelManager() {
        return channelManager;
    }

    /**
	 * @param channelManager the channelManager to set
	 */
    public void setChannelManager(ChannelManager channelManager) {
        this.channelManager = channelManager;
    }
}
