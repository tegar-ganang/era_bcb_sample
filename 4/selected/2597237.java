package org.eaiframework.config.xml;

import java.io.IOException;
import java.io.InputStream;

/**
 * 
 */
public class ClasspathXmlConfiguration extends AbstractXmlConfiguration {

    public String[] channelsConfigLocations;

    public String[] filtersDescriptorsConfigLocations;

    public String[] filtersConfigLocations;

    /**
	 * @param channelsConfigLocation
	 * @param filterDescriptorConfigLocation
	 * @param filtersConfigLocations
	 */
    public ClasspathXmlConfiguration(String channelsConfigLocation, String filterDescriptorConfigLocation, String filtersConfigLocations) {
        super(channelsConfigLocation, filterDescriptorConfigLocation, filtersConfigLocations);
    }

    /**
	 * @param channelsConfigLocation
	 * @param filterDescriptorsConfigLocations
	 * @param filtersConfigLocation
	 */
    public ClasspathXmlConfiguration(String channelsConfigLocation, String[] filterDescriptorsConfigLocations, String filtersConfigLocation) {
        super(channelsConfigLocation, filterDescriptorsConfigLocations, filtersConfigLocation);
    }

    /**
	 * @param channelsConfigLocations
	 * @param filterDescriptorsConfigLocations
	 * @param filtersConfigLocations
	 */
    public ClasspathXmlConfiguration(String[] channelsConfigLocations, String[] filterDescriptorsConfigLocations, String[] filtersConfigLocations) {
        super(channelsConfigLocations, filterDescriptorsConfigLocations, filtersConfigLocations);
    }

    @Override
    protected InputStream[] getChannelsInputStreams() throws IOException {
        ClassLoader classloader = this.getClass().getClassLoader();
        InputStream[] ret = new InputStream[channelsConfigLocations.length];
        int i = 0;
        for (String channelConfigLocation : channelsConfigLocations) {
            ret[i] = classloader.getResourceAsStream(channelConfigLocation);
            i++;
        }
        return ret;
    }

    @Override
    protected InputStream[] getFiltersDescriptorsInputStreams() throws IOException {
        ClassLoader classloader = this.getClass().getClassLoader();
        InputStream[] ret = new InputStream[filtersDescriptorsConfigLocations.length];
        int i = 0;
        for (String filterDescriptorConfigLocation : filtersDescriptorsConfigLocations) {
            ret[i] = classloader.getResourceAsStream(filterDescriptorConfigLocation);
            i++;
        }
        return ret;
    }

    @Override
    protected InputStream[] getFiltersInputStreams() throws IOException {
        ClassLoader classloader = this.getClass().getClassLoader();
        InputStream[] ret = new InputStream[filtersConfigLocations.length];
        int i = 0;
        for (String filterConfigLocation : filtersConfigLocations) {
            ret[i] = classloader.getResourceAsStream(filterConfigLocation);
            i++;
        }
        return ret;
    }
}
