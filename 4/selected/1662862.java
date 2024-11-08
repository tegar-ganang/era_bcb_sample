package org.eaiframework.config.xml;

/**
 * 
 */
public abstract class AbstractXmlConfiguration extends XMLConfiguration {

    public String[] channelsConfigLocations = new String[0];

    public String[] filtersDescriptorsConfigLocations = new String[0];

    public String[] filtersConfigLocations = new String[0];

    public AbstractXmlConfiguration(String channelsConfigLocation, String filterDescriptorConfigLocation, String filtersConfigLocations) {
        this(new String[] { channelsConfigLocation }, new String[] { filterDescriptorConfigLocation }, new String[] { filtersConfigLocations });
    }

    public AbstractXmlConfiguration(String channelsConfigLocation, String[] filterDescriptorsConfigLocations, String filtersConfigLocation) {
        this(new String[] { channelsConfigLocation }, filterDescriptorsConfigLocations, new String[] { filtersConfigLocation });
    }

    public AbstractXmlConfiguration(String[] channelsConfigLocations, String[] filterDescriptorsConfigLocations, String[] filtersConfigLocations) {
        if (channelsConfigLocations != null) {
            this.channelsConfigLocations = channelsConfigLocations;
        }
        if (filterDescriptorsConfigLocations != null) {
            this.filtersDescriptorsConfigLocations = filterDescriptorsConfigLocations;
        }
        if (filtersConfigLocations != null) {
            this.filtersConfigLocations = filtersConfigLocations;
        }
    }

    /**
	 * @return the channelsConfigLocations
	 */
    public String[] getChannelsConfigLocations() {
        return channelsConfigLocations;
    }

    /**
	 * @param channelsConfigLocations the channelsConfigLocations to set
	 */
    public void setChannelsConfigLocations(String[] channelsConfigLocations) {
        this.channelsConfigLocations = channelsConfigLocations;
    }

    /**
	 * @return the filtersDescriptorsConfigLocations
	 */
    public String[] getFiltersDescriptorsConfigLocations() {
        return filtersDescriptorsConfigLocations;
    }

    /**
	 * @param filtersDescriptorsConfigLocations the filtersDescriptorsConfigLocations to set
	 */
    public void setFiltersDescriptorsConfigLocations(String[] filtersDescriptorsConfigLocations) {
        this.filtersDescriptorsConfigLocations = filtersDescriptorsConfigLocations;
    }

    /**
	 * @return the filtersConfigLocations
	 */
    public String[] getFiltersConfigLocations() {
        return filtersConfigLocations;
    }

    /**
	 * @param filtersConfigLocations the filtersConfigLocations to set
	 */
    public void setFiltersConfigLocations(String[] filtersConfigLocations) {
        this.filtersConfigLocations = filtersConfigLocations;
    }
}
