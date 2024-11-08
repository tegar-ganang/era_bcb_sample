package org.eaiframework.config.xml;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;

/**
 * 
 */
public class SpringResourceXmlConfiguration extends AbstractXmlConfiguration implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    /**
	 * @param channelsConfigLocation
	 * @param filterDescriptorConfigLocation
	 * @param filtersConfigLocations
	 */
    public SpringResourceXmlConfiguration(String channelsConfigLocation, String filterDescriptorConfigLocation, String filtersConfigLocations) {
        super(channelsConfigLocation, filterDescriptorConfigLocation, filtersConfigLocations);
    }

    /**
	 * @param channelsConfigLocation
	 * @param filterDescriptorsConfigLocations
	 * @param filtersConfigLocation
	 */
    public SpringResourceXmlConfiguration(String channelsConfigLocation, String[] filterDescriptorsConfigLocations, String filtersConfigLocation) {
        super(channelsConfigLocation, filterDescriptorsConfigLocations, filtersConfigLocation);
    }

    /**
	 * @param channelsConfigLocations
	 * @param filterDescriptorsConfigLocations
	 * @param filtersConfigLocations
	 */
    public SpringResourceXmlConfiguration(String[] channelsConfigLocations, String[] filterDescriptorsConfigLocations, String[] filtersConfigLocations) {
        super(channelsConfigLocations, filterDescriptorsConfigLocations, filtersConfigLocations);
    }

    @Override
    protected InputStream[] getChannelsInputStreams() throws IOException {
        InputStream[] ret = new InputStream[channelsConfigLocations.length];
        int i = 0;
        for (String channelConfigLocation : channelsConfigLocations) {
            Resource resource = applicationContext.getResource(channelConfigLocation);
            ret[i] = resource.getInputStream();
            i++;
        }
        return ret;
    }

    @Override
    protected InputStream[] getFiltersDescriptorsInputStreams() throws IOException {
        InputStream[] ret = new InputStream[filtersDescriptorsConfigLocations.length];
        int i = 0;
        for (String filterDescriptorConfigLocation : filtersDescriptorsConfigLocations) {
            Resource resource = applicationContext.getResource(filterDescriptorConfigLocation);
            ret[i] = resource.getInputStream();
            i++;
        }
        return ret;
    }

    @Override
    protected InputStream[] getFiltersInputStreams() throws IOException {
        InputStream[] ret = new InputStream[filtersConfigLocations.length];
        int i = 0;
        for (String filterConfigLocation : filtersConfigLocations) {
            Resource resource = applicationContext.getResource(filterConfigLocation);
            ret[i] = resource.getInputStream();
        }
        return ret;
    }

    /**
	 * @param applicationContext the applicationContext to set
	 */
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
