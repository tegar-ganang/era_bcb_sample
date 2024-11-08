package com.volantis.testtools.config;

import com.volantis.mcs.runtime.configuration.MarkupPluginConfiguration;
import com.volantis.mcs.runtime.configuration.xml.XMLConfigurationBuilder;
import com.volantis.xml.schema.W3CSchemata;
import org.apache.log4j.Category;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A class that will create a mcs-config.xml and a mcs-config.dtd file
 * and make these available to others.
 *
 * @todo this class could use further refactoring now that JDOM is redundant
 * here, no time at the moment...
 */
public class ConfigFileBuilder {

    /**
     * The copyright statement.
     */
    private static String mark = "(c) Volantis Systems Ltd 2002.";

    /**
     * The log4j object to log to.
     */
    private static Category logger = Category.getInstance("com.volantis.testtools.config.ConfigFileBuilder");

    private static final String DEFAULT_FILE_DIR = System.getProperty("java.io.tmpdir");

    private static final String XML_FILE_NAME = "mcs-config.xml";

    private static final String XSD_FILE_NAME = "mcs-config.xsd";

    /**
     * String representation of a default name for named project one for
     * testing.  This is public so that other testcases can use them.
     */
    public static final String DEFAULT_NAMED_PROJECT_ONE = "#DefaultNamedProject";

    /**
     * String representation of a default name for named project two for
     * testing.  This is public so that other testcases can use them.
     */
    public static final String DEFAULT_NAMED_PROJECT_TWO = "#AnotherDefaultNamedProject";

    /**
     * String representation of the default base URL for assets.  It is
     * deliberately an absolute path for the tests to demonstrate MarinerURL
     * operations.  This is public so that other testcases can use it.
     */
    public static final String DEFAULT_ASSET_BASE_URL = "/volantis/";

    /**
     * The JDOM document for the config file.
     */
    private Document config;

    /**
     * The directory for the config files.
     */
    private String configFileDir = DEFAULT_FILE_DIR;

    /**
     * Used to holder plugin builders.
     */
    private Map pluginBuilders = new HashMap();

    public static final String SCHEMA_RESOURCE = "com/volantis/mcs/runtime/configuration/xml/" + XMLConfigurationBuilder.SCHEMA_NAME;

    public static final String SCHEMA_FILE = "architecture/built/output/classes/api/" + SCHEMA_RESOURCE;

    /**
     * Get the value of configFileDir.
     * @return value of configFileDir.
     */
    public String getConfigFileDir() {
        return configFileDir;
    }

    /**
     * Set the value of configFileDir.
     * @param v  Value to assign to configFileDir.
     */
    public void setConfigFileDir(String v) {
        this.configFileDir = v;
    }

    /**
     * Output the config xml file. Note that this config file is non validating
     * and has no dtd reference.
     */
    void outputXML() throws IOException {
        File xmlFile = new File(configFileDir, XML_FILE_NAME);
        xmlFile.deleteOnExit();
        FileWriter writer = new FileWriter(xmlFile);
        XMLOutputter outputter = new XMLOutputter("  ", false);
        outputter.output(config, writer);
        writer.close();
    }

    /**
     * Output the config dtd file.
     */
    void outputDTD() throws IOException {
        File dtdFile = new File(configFileDir, XSD_FILE_NAME);
        dtdFile.deleteOnExit();
        FileWriter writer = new FileWriter(dtdFile);
        String dtd = getConfigDTD();
        writer.write(dtd, 0, dtd.length());
        writer.close();
    }

    /**
     */
    public void buildConfigDocument(ConfigValue value) throws JDOMException, IOException {
        String configXml = createConfigXml(value);
        System.out.println(configXml);
        StringReader reader = new StringReader(configXml);
        SAXBuilder builder = new SAXBuilder();
        try {
            config = builder.build(reader);
        } catch (JDOMException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error parsing created XML config " + configXml, e);
            }
            throw e;
        }
        outputXML();
        outputDTD();
    }

    private String getConfigDTD() throws IOException {
        File currentDir = new File(System.getProperty("user.dir"));
        File voyagerDir = new File(currentDir, "mps/voyager");
        if (!voyagerDir.exists()) {
            voyagerDir = new File(currentDir, "voyager");
        }
        if (!voyagerDir.exists()) {
            voyagerDir = currentDir;
        }
        Reader reader = null;
        StringWriter sw = new StringWriter();
        File dtdFile = new File(voyagerDir, SCHEMA_FILE);
        if (!dtdFile.exists()) {
            InputStream stream = getClass().getClassLoader().getResourceAsStream(SCHEMA_RESOURCE);
            if (stream == null) {
                throw new IllegalStateException("can't find schema resource " + SCHEMA_RESOURCE + " or schema file " + dtdFile);
            } else {
                reader = new InputStreamReader(stream);
            }
        } else {
            reader = new FileReader(dtdFile);
        }
        char[] buf = new char[1024];
        int read;
        try {
            while ((read = reader.read(buf)) != -1) {
                sw.write(buf, 0, read);
            }
        } finally {
            reader.close();
        }
        return sw.toString();
    }

    /**
     * Create an attribute name=value pair, if the value is not null.
     *
     * @param name name of the attribute
     * @param value value of the attribute, may be null.
     * @return the name=value pair, or empty string if the value is null.
     */
    private String attr(String name, Object value) {
        if (value != null) {
            return name + "=\"" + String.valueOf(value) + "\" ";
        } else {
            return "";
        }
    }

    /**
     * Create an attribute name=value pair, translating null values into "".
     *
     * @param name name of the attribute
     * @param value value of the attribute, may be null.
     * @return the name=value pair, or empty string if the value is null.
     */
    private String attrMan(String name, Object value) {
        if (value == null) {
            value = "";
        }
        return name + "=\"" + String.valueOf(value) + "\" ";
    }

    /**
     * Register a plugin builder for a pluginValue class.
     *
     * @param builder The builder
     * @param pluginClass The plugin value associated with the builder.
     */
    public void registerPluginBuilder(PluginConfigFileBuilder builder, Class pluginClass) {
        pluginBuilders.put(pluginClass, builder);
    }

    /**
     * Provide String representation of a config file.
     */
    protected String createConfigXml(ConfigValue value) {
        String config = "<mcs-config " + "xmlns=\"" + XMLConfigurationBuilder.SCHEMA_NAMESPACE + "\" " + "xmlns:xsi=\"" + W3CSchemata.XSI_NAMESPACE + "\" " + "xsi:schemaLocation=\"" + XMLConfigurationBuilder.SCHEMA_NAMESPACE + " " + XMLConfigurationBuilder.SCHEMA_LOCATION + "\"> \n";
        config += "<local-repository> \n";
        if ("xml".equals(value.repositoryType)) {
            config += "<xml-repository/>\n";
        } else {
            config += "<jdbc-repository>\n";
            config += "<connection-pool " + attrMan("maximum", value.repositoryDbPoolMax) + attrMan("keep-alive", value.repositoryKeepConnectionsAlive) + attrMan("poll-interval", value.repositoryConnectionPollInterval) + ">\n";
            config += "<anonymous-data-source " + attrMan("user", value.repositoryUser) + attrMan("password", value.repositoryPassword) + ">\n";
            config += "<mcs-database " + attrMan("vendor", value.repositoryVendor) + attrMan("source", value.repositorySource) + attrMan("host", value.repositoryHost) + attrMan("port", value.repositoryPort) + "/>\n";
            config += "</anonymous-data-source>\n";
            config += "</connection-pool>\n";
            config += "</jdbc-repository>\n";
        }
        config += "</local-repository>\n";
        config += "<projects>";
        config += "<default>\n" + "    " + createPolicySource(value) + createDefaultAssets("    ") + "</default>\n";
        config += "<project " + attr("name", DEFAULT_NAMED_PROJECT_ONE) + ">\n" + "    " + createPolicySource(value) + createDefaultAssets("    ") + "</project>\n";
        config += "<project " + attr("name", DEFAULT_NAMED_PROJECT_TWO) + ">\n" + "    " + createPolicySource(value) + createDefaultAssets("    ") + "</project>\n";
        config += "</projects>\n";
        config += "<secondary-repository> \n" + "<inline-metadata " + attr("enabled", "true") + "/> \n" + "</secondary-repository> \n" + "<page-messages " + attr("heading", value.pageMessageHeading) + "/> \n" + "<policy-cache> \n";
        Iterator policyCacheNames = value.policyCaches.keySet().iterator();
        while (policyCacheNames.hasNext()) {
            String name = (String) policyCacheNames.next();
            ConfigValuePolicyCache cache = (ConfigValuePolicyCache) value.policyCaches.get(name);
            config += "<" + name + " " + attr("enabled", "true") + attr("strategy", cache.strategy) + attr("max-entries", cache.maxEntries) + attr("timeout", cache.timeout) + "/> \n";
        }
        config += "</policy-cache> \n" + "<jsp " + attr("support-required", value.jspSupportRequired) + attr("write-direct", value.jspWriteDirect) + attr("resolve-character-references", value.jspResolveCharacterReferences) + attr("evaluate-page-after-canvas", value.jspEvaluatePageAfterCanvas) + "/> \n" + "<!-- Configure style sheet associated properties --> \n" + "<style-sheets> \n" + "<external-generation " + attr("base-url", value.styleBaseUrl) + attr("base-directory", "/dummy") + "/> \n" + "</style-sheets> \n" + "<scripts " + attr("base", value.scriptsBase) + "/> \n" + "<modesets " + attr("base", value.modesetsBase) + "/> \n" + "<chartimages " + attr("base", value.chartImagesBase) + "/> \n" + "<log4j " + attr("xml-configuration-file", value.log4jXmlConfigFile) + "/> \n" + "<plugins " + attr("asset-url-rewriter", value.assetUrlRewriterPluginClass) + " " + attr("asset-transcoder", value.assetTranscoderPluginClass) + "/> \n" + "<mcs-agent " + attr("enabled", "false") + attr("port", "8888") + attr("password", "007") + "/> \n" + "<remote-policies " + attr("connection-timeout", value.remoteRepositoryTimeout) + ">";
        ConfigValueRemotePolicy remoteCache = (ConfigValueRemotePolicy) value.remotePolicyCaches;
        if (remoteCache != null) {
            config += createRemoteCache(remoteCache);
        }
        config += "<remote-policy-quotas>";
        if (value.remotePolicyQuotaList != null) {
            config += createRemoteQuotas(value.remotePolicyQuotaList);
        }
        config += "</remote-policy-quotas>" + "</remote-policies>" + "<web-application " + attr("base-url", value.baseUrl) + attr("internal-url", value.internalUrl) + attr("app-server-name", "Tomcat31") + attr("jndi-provider", "t3://sunfish:7801") + attr("use-server-connection-pool", "false") + attr("datasource", "mdatasource") + attr("user", "volantis") + attr("password", "fish") + attr("page-base", value.pageBase) + "/> \n" + "<debug " + attr("comments", value.debugComments) + attr("logPageOutput", value.debugLogPageOutput) + "/> \n" + "<page-packaging>\n" + "<mime-packaging " + attr("enabled", value.pagePackagingMimeEnabled) + "/> \n" + "</page-packaging>\n" + "<session-proxy>\n" + "<map-cookies " + attr("enabled", value.sessionProxyCookieMappingEnabled) + "/> \n" + "</session-proxy>\n" + "<mcs-plugins> \n" + createMarkupPlugins(value.markupPlugins) + "</mcs-plugins> \n" + "<application-plugins>\n";
        Iterator i = value.pluginConfigValues.iterator();
        while (i.hasNext()) {
            PluginConfigValue pluginValue = (PluginConfigValue) i.next();
            PluginConfigFileBuilder pluginBuilder = (PluginConfigFileBuilder) pluginBuilders.get(pluginValue.getClass());
            if (pluginBuilder == null) {
                logger.error("No builder registered for " + pluginValue.getClass());
                throw new IllegalStateException("No builder registered for " + pluginValue.getClass());
            } else {
                config += pluginBuilder.build(pluginValue) + "\n";
            }
        }
        config += "</application-plugins>\n";
        if (value.wmlPreferredOutputFormat != null) {
            config += "<protocols>\n";
            config += "<wml " + attr("preferred-output-format", value.wmlPreferredOutputFormat) + " />\n";
            config += "</protocols>\n";
        }
        config += "<pipeline-configuration/>\n";
        config += "<devices>\n" + "    <standard>\n";
        if (value.standardFileDeviceRepositoryLocation != null) {
            config += "        <file-repository " + attr("location", value.standardFileDeviceRepositoryLocation) + "/> \n";
        }
        if (value.standardJDBCDeviceRepositoryProject != null) {
            config += "        <jdbc-repository " + attr("project", value.standardJDBCDeviceRepositoryProject) + "/> \n";
        }
        config += "    </standard>\n" + "    <logging>\n" + "        <log-file></log-file>\n" + "        <e-mail>\n" + "            <e-mail-sending>disable</e-mail-sending>\n" + "        </e-mail>\n" + "    </logging>\n" + "</devices>\n";
        config += "</mcs-config>";
        return config;
    }

    /**
     * A utility method that creates the policy source statement based on the
     * current policy.
     * @param value The configuration information to use for the policy values.
     * @return A string of the xml representation of the policy location.
     */
    private String createPolicySource(ConfigValue value) {
        String config = "";
        if (value.defaultProjectPolicies instanceof ConfigProjectPoliciesJdbcValue) {
            ConfigProjectPoliciesJdbcValue jdbcPolicies = (ConfigProjectPoliciesJdbcValue) value.defaultProjectPolicies;
            config += "<jdbc-policies " + attrMan("name", jdbcPolicies.projectName);
        } else if (value.defaultProjectPolicies instanceof ConfigProjectPoliciesXmlValue) {
            ConfigProjectPoliciesXmlValue xmlPolicies = (ConfigProjectPoliciesXmlValue) value.defaultProjectPolicies;
            config += "<xml-policies " + attrMan("directory", xmlPolicies.projectDir);
        } else {
            throw new IllegalStateException("Unexpected default project " + "policies type: " + value.defaultProjectPolicies);
        }
        config += " />\n";
        return config;
    }

    /**
     * A utility method that outputs the various asset urls with default values
     * for all of the prefixes.
     * @param indent The indent to use for the output.
     * @return A string of the xml representing the asset definitions.
     */
    private String createDefaultAssets(String indent) {
        StringBuffer assetString = new StringBuffer(150);
        assetString.append(indent);
        assetString.append("<assets ");
        assetString.append(attr("base-url", DEFAULT_ASSET_BASE_URL));
        assetString.append(">\n");
        assetString.append(indent);
        assetString.append(indent);
        assetString.append("<audio-assets ");
        assetString.append(attr("prefix-url", "/audio/"));
        assetString.append("/>\n");
        assetString.append(indent);
        assetString.append(indent);
        assetString.append("<dynamic-visual-assets ");
        assetString.append(attr("prefix-url", "/dynvis/"));
        assetString.append("/>\n");
        assetString.append(indent);
        assetString.append(indent);
        assetString.append("<image-assets ");
        assetString.append(attr("prefix-url", "/images/"));
        assetString.append("/>\n");
        assetString.append(indent);
        assetString.append(indent);
        assetString.append("<script-assets ");
        assetString.append(attr("prefix-url", "/scripts/"));
        assetString.append("/>\n");
        assetString.append(indent);
        assetString.append(indent);
        assetString.append("<text-assets ");
        assetString.append(attr("prefix-url", "/text/"));
        assetString.append("/>\n");
        assetString.append(indent);
        assetString.append("</assets> \n");
        return assetString.toString();
    }

    private String createMarkupPlugins(List plugins) {
        String result = new String();
        if (plugins != null) {
            for (int i = 0; i < plugins.size(); i++) {
                MarkupPluginConfiguration configuration = (MarkupPluginConfiguration) plugins.get(i);
                result += "<markup-plugin " + attr("name", configuration.getName()) + attr("class", configuration.getClassName()) + attr("scope", configuration.getScope()) + "> \n" + "<initialize> \n" + createArguments(configuration.getArguments()) + "</initialize> \n" + "</markup-plugin> \n";
            }
        }
        return result;
    }

    private String createArguments(Map arguments) {
        StringBuffer result = new StringBuffer();
        for (Iterator i = arguments.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry) i.next();
            result.append("<argument ").append(attr("name", entry.getKey())).append(attr("value", entry.getValue())).append("/> \n");
        }
        return result.toString();
    }

    private String createRemoteQuotas(List remoteQuotaList) {
        String value = "";
        for (int i = 0; i < remoteQuotaList.size(); i++) {
            ConfigValueRemoteQuota remoteQuota = (ConfigValueRemoteQuota) remoteQuotaList.get(i);
            if (remoteQuota != null) {
                if (remoteQuota.url != null || remoteQuota.percentage != null) {
                    value += "<remote-policy-quota " + attr("URL", remoteQuota.url) + attr("percentage", remoteQuota.percentage) + "/> \n";
                }
            }
        }
        return value;
    }

    private String createRemoteCache(ConfigValueRemotePolicy remoteCache) {
        String value = "<remote-policy-cache " + attr("cachePolicies", remoteCache.cachePolicies) + attr("defaultTimeToLive", remoteCache.defaultTimeToLive) + attr("defaultRetryFailedRetrieval", remoteCache.defaultRetryFailedRetrieval) + attr("defaultRetryInterval", remoteCache.defaultRetryInterval) + attr("defaultRetryMaxCount", remoteCache.defaultRetryMaxCount) + attr("defaultRetainDuringRetry", remoteCache.defaultRetainDuringRetry) + attr("maxCacheSize", remoteCache.maxCacheSize) + attr("maxTimeToLive", remoteCache.maxTimeToLive) + attr("allowRetryFailedRetrieval", remoteCache.allowRetryFailedRetrieval) + attr("minRetryInterval", remoteCache.minRetryInterval) + attr("maxRetryMaxCount", remoteCache.maxRetryMaxCount) + attr("allowRetainDuringRetry", remoteCache.allowRetainDuringRetry) + "/> \n";
        return value;
    }
}
