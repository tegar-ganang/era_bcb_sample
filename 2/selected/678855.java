package net.sf.dozer.util.mapping.config;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import net.sf.dozer.util.mapping.util.InitLogger;
import net.sf.dozer.util.mapping.util.MapperConstants;
import net.sf.dozer.util.mapping.util.MappingUtils;
import net.sf.dozer.util.mapping.util.ResourceLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Internal singleton class that holds the global settings used by Dozer. Most of these settings are configurable via an
 * optional Dozer properties file. By default, Dozer will look for a file named dozer.properties to load these
 * configuration properties. If a properties file is not found or specified, default values will be used.
 * 
 * <p>
 * An alternative Dozer properties file can be specified via the dozer.configuration system property.
 * 
 * <p>
 * ex) -Ddozer.configuration=someDozerConfigurationFile.properties
 * 
 * @author tierney.matt
 */
public class GlobalSettings {

    private static final Log log = LogFactory.getLog(GlobalSettings.class);

    private static GlobalSettings singleton;

    private String loadedByFileName;

    private boolean statisticsEnabled = MapperConstants.DEFAULT_STATISTICS_ENABLED;

    private long converterByDestTypeCacheMaxSize = MapperConstants.DEFAULT_CONVERTER_BY_DEST_TYPE_CACHE_MAX_SIZE;

    private long superTypesCacheMaxSize = MapperConstants.DEFAULT_SUPER_TYPE_CHECK_CACHE_MAX_SIZE;

    private boolean autoregisterJMXBeans = MapperConstants.DEFAULT_AUTOREGISTER_JMX_BEANS;

    private final boolean isJdk5 = (System.getProperty("java.version", "1.4").startsWith("1.5") || System.getProperty("java.version", "1.4").startsWith("1.6"));

    public static synchronized GlobalSettings getInstance() {
        if (singleton == null) {
            singleton = new GlobalSettings();
        }
        return singleton;
    }

    protected static GlobalSettings createNew() {
        return new GlobalSettings();
    }

    private GlobalSettings() {
        loadGlobalSettings();
    }

    protected String getLoadedByFileName() {
        return loadedByFileName;
    }

    public boolean isJava5() {
        return isJdk5;
    }

    public boolean isAutoregisterJMXBeans() {
        return autoregisterJMXBeans;
    }

    public long getConverterByDestTypeCacheMaxSize() {
        return converterByDestTypeCacheMaxSize;
    }

    public boolean isStatisticsEnabled() {
        return statisticsEnabled;
    }

    public void setStatisticsEnabled(boolean statisticsEnabled) {
        this.statisticsEnabled = statisticsEnabled;
    }

    public long getSuperTypesCacheMaxSize() {
        return superTypesCacheMaxSize;
    }

    private synchronized void loadGlobalSettings() {
        String propFileName = System.getProperty(MapperConstants.CONFIG_FILE_SYS_PROP);
        if (MappingUtils.isBlankOrNull(propFileName)) {
            propFileName = MapperConstants.DEFAULT_CONFIG_FILE;
        }
        InitLogger.log(log, "Trying to find Dozer configuration file: " + propFileName);
        ResourceLoader loader = new ResourceLoader();
        URL url = loader.getResource(propFileName);
        if (url == null) {
            InitLogger.log(log, "Dozer configuration file not found: " + propFileName + ".  Using defaults for all Dozer global properties.");
            return;
        } else {
            InitLogger.log(log, "Using URL [" + url + "] for Dozer global property configuration");
        }
        Properties props = new Properties();
        try {
            InitLogger.log(log, "Reading Dozer properties from URL [" + url + "]");
            props.load(url.openStream());
        } catch (IOException e) {
            MappingUtils.throwMappingException("Problem loading Dozer properties from URL [" + propFileName + "]", e);
        }
        String propValue = props.getProperty(PropertyConstants.STATISTICS_ENABLED);
        if (propValue != null) {
            statisticsEnabled = Boolean.valueOf(propValue).booleanValue();
        }
        propValue = props.getProperty(PropertyConstants.CONVERTER_CACHE_MAX_SIZE);
        if (propValue != null) {
            converterByDestTypeCacheMaxSize = Long.parseLong(propValue);
        }
        propValue = props.getProperty(PropertyConstants.SUPERTYPE_CACHE_MAX_SIZE);
        if (propValue != null) {
            superTypesCacheMaxSize = Long.parseLong(propValue);
        }
        propValue = props.getProperty(PropertyConstants.AUTOREGISTER_JMX_BEANS);
        if (propValue != null) {
            autoregisterJMXBeans = Boolean.valueOf(propValue).booleanValue();
        }
        loadedByFileName = propFileName;
        InitLogger.log(log, "Finished configuring Dozer global properties");
    }
}
