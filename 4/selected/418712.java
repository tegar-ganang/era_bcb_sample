package org.nexopenframework.ide.eclipse.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.nexopenframework.ide.eclipse.commons.Activator;
import org.nexopenframework.ide.eclipse.commons.events.NexOpenEventDispatcher;
import org.nexopenframework.ide.eclipse.commons.io.IOUtils;
import org.nexopenframework.ide.eclipse.commons.log.Logger;
import org.nexopenframework.ide.eclipse.commons.util.ResourceLocator;
import org.nexopenframework.ide.eclipse.commons.util.StringUtils;
import org.nexopenframework.ide.eclipse.commons.util.VersionCoordinator;
import org.nexopenframework.ide.eclipse.memory.MemoryMonitorJob;
import org.nexopenframework.ide.eclipse.ui.audit.AuditStrategyExporter;
import org.nexopenframework.ide.eclipse.ui.model.ContinuumServerManager;
import org.nexopenframework.ide.eclipse.ui.preferences.PreferenceConstants;
import org.nexopenframework.ide.eclipse.ui.ws.WebServiceProvider;
import org.osgi.framework.BundleContext;

/**
 * <p>NexOpen Framework</p>
 * 
 * <p>The activator class controls the plug-in life cycle. Here, we can
 * explicetly know the preferences of the <code>NexOpen IDE</code> (such as versions, web features,
 * web services features and so on).</p>
 * 
 * @see org.nexopenframework.ide.eclipse.memory.MemoryMonitorJob
 * @see org.nexopenframework.ide.eclipse.ui.audit.AuditStrategyExporter
 * @see org.nexopenframework.ide.eclipse.ui.model.ContinuumServerManager
 * @see org.eclipse.ui.plugin.AbstractUIPlugin
 * @author Francesc Xavier Magdaleno
 * @version 1.0
 * @since 1.0
 */
public class NexOpenUIActivator extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.nexopenframework.ide.eclipse.ui";

    /**Version of NexOpen Tools*/
    public static final String VERSION = "1.0.0-m2";

    public static final String AOP_ALLIANCE_INTERFACE_NAME = "org.aopalliance.aop.Advice";

    public static final String BUSINESS_INTERFACE_NAME = "org.nexopenframework.business.BusinessService";

    public static final String BUSINESS_ANNOTATION_SIMPLE_NAME = "BusinessService";

    public static final String BUSINESS_ANNOTATION_NAME = "org.nexopenframework.business.annotations.BusinessService";

    public static final String SERVICE_INTERFACE_NAME = "org.nexopenframework.services.Service";

    public static final String SERVICE_ANNOTATION_SIMPLE_NAME = "Service";

    /***/
    public static final String SERVICE_ANNOTATION_NAME = "org.nexopenframework.services.annotations.Service";

    /**NexOpen Framework versions*/
    public static final String[] NEXOPEN_VERSIONS;

    static {
        InputStream is = ResourceLocator.getResourceAsStream("nexopen.properties", NexOpenUIActivator.class.getClassLoader());
        try {
            if (is == null) {
                is = ResourceLocator.getResourceAsStream("nexopen-default.properties", NexOpenUIActivator.class.getClassLoader());
            }
            final Properties props = new Properties();
            props.load(is);
            NEXOPEN_VERSIONS = props.getProperty("nexopen.frwk.versions").split(",");
            props.clear();
        } catch (final IOException e) {
            e.printStackTrace();
            throw new ExceptionInInitializerError(e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**Maven2 archetypes*/
    public static final String[] MVN_ARCHETYPE_VERSIONS = new String[] { "1.2.5", "2.0.0", "2.0.5" };

    /**
     * <p>Archetypes web has provided a new release, 2.0.6, in order to adopt 
     * the One RM strategy (only JDBC RM is supposed) and avoiding configurations of JTA/JTS
     * Specially useful for web containers like Tomcat 5.x or Tomcat 6.x.</p>
     * 
     * @since 1.0.0-m2
     */
    public static final String[] MVN_WEB_ARCHETYPE_VERSIONS = new String[] { "1.2.5", "2.0.5", "2.0.6" };

    /**Struts2 versions*/
    public static final String[] STRUTS2_VERSIONS = new String[] { "2.0.9", "2.0.11", "2.0.11.1" };

    /**MSQL versions*/
    private static final Map<String, String> mysql_versions = new HashMap<String, String>();

    private static NexOpenUIActivator plugin;

    /**
	 * <p>Job which deals with monitoring of memory of NexOpen Tools. In case
	 * of low memory and possibility of OutOfMemoryErrors (OOME)
	 * displays a message with optimistic heap parameters.</p>
	 * 
	 * @since 1.0.0-m2
	 */
    private MemoryMonitorJob job;

    /**
	 * The default constructor constructor
	 */
    public NexOpenUIActivator() {
        plugin = this;
    }

    /**
	 * <p></p>
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
    public void start(final BundleContext context) throws Exception {
        super.start(context);
        {
            mysql_versions.put("3.1.14", "mysql-connector-java-3.1.14.jar");
            mysql_versions.put("5.0.8", "mysql-connector-java-5.0.8.jar");
            mysql_versions.put("5.1.6", "mysql-connector-java-5.1.6.jar");
        }
        NexOpenEventDispatcher.getInstance();
        ContinuumServerManager.getInstance().loadServers();
        final IPreferenceStore preferences = this.getPreferenceStore();
        final String levelName = preferences.getString(PreferenceConstants.P_LOG_LEVEL);
        final String logFileName = preferences.getString(PreferenceConstants.P_LOG_FILENAME);
        Logger.configureLogs(logFileName, levelName);
        Logger.log(Logger.INFO, "Started NexOpen Tools " + VERSION);
        checkWeavingJar();
        job = new MemoryMonitorJob();
        job.schedule();
    }

    /**
	 * <p></p>
	 * 
	 * @throws Exception 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
    public void stop(final BundleContext context) throws Exception {
        try {
            NexOpenEventDispatcher.getInstance().freeResources();
            ContinuumServerManager.getInstance().saveServers();
            AuditStrategyExporter.saveAuditStrategies();
            super.stop(context);
            if (job != null) {
                job.cancel();
            }
        } finally {
            mysql_versions.clear();
            Logger.shutdown();
            plugin = null;
        }
    }

    /**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
    public static NexOpenUIActivator getDefault() {
        return plugin;
    }

    /**
	 * <p>Returns timeout of Maven2 executions in order to avoid high delays 
	 *    if some internet problems appers.</p>
	 * 
	 * @return the timeout to be applied
	 */
    public static long getTimeout() {
        final String value = getDefault().getPreferenceStore().getString(PreferenceConstants.P_M2_TIMEOUT);
        return Long.parseLong(value) * 60 * 1000;
    }

    /**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
    public static ImageDescriptor getImageDescriptor(final String path) {
        return imageDescriptorFromPlugin(PLUGIN_ID, path);
    }

    /**
	 * <p>Returns the Spring version given a NexOpen version</p>
	 * 
	 * @param nexopenVersion
	 * @return
	 */
    public static String getSpringFrameworkVersion(final String nexopenVersion) {
        return VersionCoordinator.getSpringFrameworkVersion(nexopenVersion);
    }

    /**
	 * @return
	 */
    public int getDefaultNexOpenVersion() {
        final String version = this.getPreferenceStore().getDefaultString(PreferenceConstants.P_NEXOPEN_DEFAULT_VERSION);
        return getIntNexOpenVersion(version);
    }

    /**
	 * <p></p>
	 * 
	 * @see #getNexOpenVersion()
	 * @return
	 */
    public int getIntNexOpenVersion() {
        final String version = getNexOpenVersion();
        return getIntNexOpenVersion(version);
    }

    public int getIntNexOpenVersion(final String version) {
        if (version == null) {
            return -1;
        }
        if (version.equals("0.3.0") || version.equals("0.3.0-SNAPSHOT")) {
            return 0;
        } else if (version.equals("0.3.1") || version.equals("0.3.1-SNAPSHOT")) {
            return 1;
        } else if (version.equals("0.4.0") || version.equals("0.4.0-SNAPSHOT")) {
            return 2;
        } else if (version.equals("0.4.1") || version.equals("0.4.1-SNAPSHOT")) {
            return -1;
        } else if (version.equals("0.5.0") || version.equals("0.5.0-SNAPSHOT")) {
            return 3;
        } else if (version.equals("0.6.0") || version.equals("0.6.0-SNAPSHOT")) {
            return -1;
        }
        return -1;
    }

    /**
	 * <p>Returns the NexOpen version selected in <code>Preferences</code> page</p>
	 * 
	 * @return
	 */
    public String getNexOpenVersion() {
        final String version = this.getPreferenceStore().getString(PreferenceConstants.P_NEXOPEN_DEFAULT_VERSION);
        return version;
    }

    public void setNexOpenVersion(final String version) {
        if (version != null) {
            this.getPreferenceStore().setValue(PreferenceConstants.P_NEXOPEN_DEFAULT_VERSION, version);
        }
    }

    @Deprecated
    public int getDefaultJavaVersion() {
        String version = this.getPreferenceStore().getDefaultString(PreferenceConstants.P_JVM_CHOICE);
        if (version.equals("1.4")) {
            return 0;
        } else if (version.equals("5.0")) {
            return 1;
        }
        return -1;
    }

    @Deprecated
    public String getJavaVersion() {
        String version = this.getPreferenceStore().getString(PreferenceConstants.P_JVM_CHOICE);
        return version;
    }

    @Deprecated
    public int getIntJavaVersion() {
        String version = this.getPreferenceStore().getString(PreferenceConstants.P_JVM_CHOICE);
        if (version.equals("1.4")) {
            return 0;
        } else if (version.equals("5.0")) {
            return 1;
        }
        return -1;
    }

    @Deprecated
    public void setJavaVersion(String version) {
        this.getPreferenceStore().setValue(PreferenceConstants.P_JVM_CHOICE, version);
    }

    public String getModulesConfigurerPath() {
        return this.getPreferenceStore().getString(PreferenceConstants.P_MODULES_CONFIGURER_STRING);
    }

    public void setModulesConfigurerPath(String path) {
        this.getPreferenceStore().setValue(PreferenceConstants.P_MODULES_CONFIGURER_STRING, path);
    }

    public String getDefaultModulesConfigurerPath() {
        return this.getPreferenceStore().getDefaultString(PreferenceConstants.P_MODULES_CONFIGURER_STRING);
    }

    public String getDefaultWebModulesConfigurerPath() {
        return this.getPreferenceStore().getDefaultString(PreferenceConstants.P_WEB_MODULES_CONFIGURER_STRING);
    }

    public String getWebModulesConfigurerPath() {
        return this.getPreferenceStore().getString(PreferenceConstants.P_WEB_MODULES_CONFIGURER_STRING);
    }

    public void setWebModulesConfigurerPath(final String path) {
        this.getPreferenceStore().setValue(PreferenceConstants.P_WEB_MODULES_CONFIGURER_STRING, path);
    }

    /**
	 * <p>returns the default path of the business <code>pom.xml</code></p>
	 * 
	 * @return
	 */
    public String getDefaultBusinessPath() {
        return this.getPreferenceStore().getDefaultString(PreferenceConstants.P_BUSINESS_POM_STRING);
    }

    /**
	 * <p>Returns the path of the business <code>pom.xml</code></p>
	 * 
	 * @return
	 */
    public String getBusinessPath() {
        return this.getPreferenceStore().getString(PreferenceConstants.P_BUSINESS_POM_STRING);
    }

    public void setBusinessPath(String path) {
        this.getPreferenceStore().setValue(PreferenceConstants.P_BUSINESS_POM_STRING, path);
    }

    public String getDefaultWebBusinessPath() {
        return this.getPreferenceStore().getDefaultString(PreferenceConstants.P_WEB_BUSINESS_POM_STRING);
    }

    public String getWebBusinessPath() {
        return this.getPreferenceStore().getString(PreferenceConstants.P_WEB_BUSINESS_POM_STRING);
    }

    public void setWebBusinessPath(String path) {
        this.getPreferenceStore().setValue(PreferenceConstants.P_WEB_BUSINESS_POM_STRING, path);
    }

    public String getDefaultResourcePath() {
        return this.getPreferenceStore().getDefaultString(PreferenceConstants.P_RESOURCE_STRING);
    }

    /**
	 * <p>location of the <code>beanRefContext.xml/code> into a enterprise application</p>
	 * 
	 * @return
	 */
    public String getBeanRefContext() {
        return this.getPreferenceStore().getDefaultString(PreferenceConstants.P_BEANREF_STRING);
    }

    /**
	 * <p>location of the <code>beanRefContext.xml/code> into a web application</p>
	 * 
	 * @return
	 */
    public String getWebBeanRefContext() {
        return this.getPreferenceStore().getDefaultString(PreferenceConstants.P_BEANREF_WEB_STRING);
    }

    /**
	 * <p>The location of the default (<code>development</code> profile) <code>nexopen-dataAccess.xml</code></p>
	 * 
	 * @return
	 */
    public String getResourcePath() {
        return this.getPreferenceStore().getString(PreferenceConstants.P_RESOURCE_STRING);
    }

    public void setResourcePath(final String path) {
        this.getPreferenceStore().setValue(PreferenceConstants.P_RESOURCE_STRING, path);
    }

    public String getDefaultWebResourcePath() {
        return this.getPreferenceStore().getDefaultString(PreferenceConstants.P_WEB_RESOURCE_STRING);
    }

    public String getWebResourcePath() {
        return this.getPreferenceStore().getString(PreferenceConstants.P_WEB_RESOURCE_STRING);
    }

    public void setWebResourcePath(final String path) {
        this.getPreferenceStore().setValue(PreferenceConstants.P_WEB_RESOURCE_STRING, path);
    }

    public String getDefaultAppServerConfPath() {
        return this.getPreferenceStore().getDefaultString(PreferenceConstants.P_APP_SERVER_CONF_STRING);
    }

    public String getAppServerConfPath() {
        return this.getPreferenceStore().getString(PreferenceConstants.P_APP_SERVER_CONF_STRING);
    }

    public void setAppServerConfPath(String path) {
        this.getPreferenceStore().setValue(PreferenceConstants.P_APP_SERVER_CONF_STRING, path);
    }

    public boolean isShowWebServiceMethod() {
        return this.getPreferenceStore().getBoolean(PreferenceConstants.P_SHOW_WS_METHOD);
    }

    public void setShowWebServiceMethod(boolean swsm) {
        this.getPreferenceStore().setValue(PreferenceConstants.P_SHOW_WS_METHOD, swsm);
    }

    public boolean isMockWSEnabled() {
        return this.getPreferenceStore().getBoolean(PreferenceConstants.P_MOCK_WS_ENABLED);
    }

    public void setMockWSEnabled(boolean mockWSEnabled) {
        this.getPreferenceStore().setValue(PreferenceConstants.P_MOCK_WS_ENABLED, mockWSEnabled);
    }

    public String getComponentArchVersion() {
        final String version = this.getNexOpenVersion();
        if (version.startsWith("0.3")) {
            return this.getPreferenceStore().getString(PreferenceConstants.P_COMPONENT_03x_ARCHETYPE_VERSION);
        }
        return this.getPreferenceStore().getString(PreferenceConstants.P_COMPONENT_ARCHETYPE_VERSION);
    }

    /**
	 * @return
	 */
    public String getReverseEngVersion() {
        return "1.0.1";
    }

    public int getIntComponentArchVersion() {
        final String version = getWebArchVersion();
        return _getIntVersion(version);
    }

    /**
	 * @param compArchVersion
	 */
    public void setComponentArchVersion(final String compArchVersion) {
        final String version = this.getNexOpenVersion();
        if (version.startsWith("0.3")) {
            return;
        }
        this.getPreferenceStore().setValue(PreferenceConstants.P_COMPONENT_ARCHETYPE_VERSION, compArchVersion);
    }

    /**
	 * <p>returns the archetype version for EAR applications</p>
	 * 
	 * @return
	 */
    public String getApplicationArchVersion() {
        final String version = this.getNexOpenVersion();
        if (version.startsWith("0.3")) {
            return this.getPreferenceStore().getString(PreferenceConstants.P_APPLICATION_03x_ARCHETYPE_VERSION);
        }
        return this.getPreferenceStore().getString(PreferenceConstants.P_APPLICATION_ARCHETYPE_VERSION);
    }

    public int getIntApplicationArchVersion() {
        final String version = getApplicationArchVersion();
        return _getIntVersion(version);
    }

    /**
	 * @param appArchVersion
	 */
    public void setApplicationArchVersion(final String appArchVersion) {
        final String version = this.getNexOpenVersion();
        if (version.startsWith("0.3")) {
            return;
        }
        this.getPreferenceStore().setValue(PreferenceConstants.P_APPLICATION_ARCHETYPE_VERSION, appArchVersion);
    }

    /**
	 * <p>returns the archetype version for WAR applications</p>
	 * 
	 * @return
	 */
    public final String getWebArchVersion() {
        final String version = this.getNexOpenVersion();
        if (version.startsWith("0.3")) {
            return this.getPreferenceStore().getString(PreferenceConstants.P_WEB_03x_ARCHETYPE_VERSION);
        }
        return this.getPreferenceStore().getString(PreferenceConstants.P_WEB_ARCHETYPE_VERSION);
    }

    public final int getIntWebArchVersion() {
        final String version = getWebArchVersion();
        return _getIntWebVersion(version);
    }

    /**
	 * @param webArchVersion
	 */
    public void setWebArchVersion(final String webArchVersion) {
        this.getPreferenceStore().setValue(PreferenceConstants.P_WEB_ARCHETYPE_VERSION, webArchVersion);
    }

    /**
	 * @return
	 */
    public final WebServiceProvider getWebServiceProvider() {
        final String wsp = this.getPreferenceStore().getString(PreferenceConstants.P_WEB_SERVICE_PROVIDER);
        return Enum.valueOf(WebServiceProvider.class, wsp);
    }

    /**
	 * @return
	 */
    public void setWebServiceProvider(final WebServiceProvider provider) {
        this.getPreferenceStore().setValue(PreferenceConstants.P_WEB_SERVICE_PROVIDER, provider.toString());
    }

    /**
	 * @return
	 */
    public boolean getTomcatConfProperty() {
        return this.getPreferenceStore().getBoolean(PreferenceConstants.P_TOMCAT_CONF_CREATION);
    }

    public void setTomcatConfProperty(final String propertyValue) {
        this.getPreferenceStore().setValue(PreferenceConstants.P_TOMCAT_CONF_CREATION, Boolean.parseBoolean(propertyValue));
    }

    /**
	 * @return
	 */
    public boolean isSpringTransactionalProperty() {
        return this.getPreferenceStore().getBoolean(PreferenceConstants.P_SPRING_TX_ANNOTATION);
    }

    public void setSpringTransactionalProperty(final String propertyValue) {
        this.getPreferenceStore().setValue(PreferenceConstants.P_SPRING_TX_ANNOTATION, Boolean.parseBoolean(propertyValue));
    }

    /**
	 * @return
	 */
    public boolean isJee50TransactionalProperty() {
        return this.getPreferenceStore().getBoolean(PreferenceConstants.P_JEE50_TX_ANNOTATION);
    }

    public void setJee50TransactionalProperty(final String propertyValue) {
        this.getPreferenceStore().setValue(PreferenceConstants.P_JEE50_TX_ANNOTATION, Boolean.parseBoolean(propertyValue));
    }

    /**
	 * @return
	 */
    public final String getMySQLVersion() {
        final String jar = this.getPreferenceStore().getString(PreferenceConstants.P_MYSQL_DRIVER_VERSION);
        final Set<Entry<String, String>> entries = mysql_versions.entrySet();
        for (final Entry<String, String> entry : entries) {
            if (entry.getValue().equals(jar)) {
                return entry.getKey();
            }
        }
        return "3.1.14";
    }

    public final String getMySQLDriver() {
        return this.getPreferenceStore().getString(PreferenceConstants.P_MYSQL_DRIVER_VERSION);
    }

    public void setMySQLVersion(final String version) {
        this.getPreferenceStore().setValue(PreferenceConstants.P_MYSQL_DRIVER_VERSION, mysql_versions.get(version));
    }

    public final String getStruts2Version() {
        return this.getPreferenceStore().getString(PreferenceConstants.P_STRUTS2_VERSION);
    }

    public void setStruts2Version(final String version) {
        this.getPreferenceStore().setValue(PreferenceConstants.P_STRUTS2_VERSION, version);
    }

    /**
	 * @return
	 */
    @SuppressWarnings("unchecked")
    public List<String> getMySQLVersions() {
        return Collections.unmodifiableList(new ArrayList(mysql_versions.keySet()));
    }

    public boolean isPMDActive() {
        return this.getPreferenceStore().getBoolean(PreferenceConstants.P_PMD_ACTIVATION);
    }

    public void setPMDActive(final boolean active) {
        this.getPreferenceStore().setValue(PreferenceConstants.P_PMD_ACTIVATION, active);
    }

    /**
     * @param msg
     * @param e
     * @return
     */
    public static IStatus createErrorStatus(final String msg, final Exception e) {
        return new Status(IStatus.ERROR, PLUGIN_ID, 0, msg, e);
    }

    /**
     * Helper method to log error
     * 
     * @see IStatus
     */
    public void logError(final String message, final Throwable t) {
        getLog().log(new Status(IStatus.ERROR, getBundle().getSymbolicName(), 0, message + ": " + t.getMessage(), t));
    }

    /**
     * <p>Apply the log preferences</p>
     */
    public void applyLogPreferences() {
        final IPreferenceStore preferences = this.getPreferenceStore();
        final String levelName = preferences.getString(PreferenceConstants.P_LOG_LEVEL);
        final String logFileName = preferences.getString(PreferenceConstants.P_LOG_FILENAME);
        final IPreferenceStore commonStore = Activator.getDefault().getPreferenceStore();
        {
            commonStore.setValue(org.nexopenframework.ide.eclipse.commons.preferences.PreferenceConstants.P_LOG_FILENAME, logFileName);
            commonStore.setValue(org.nexopenframework.ide.eclipse.commons.preferences.PreferenceConstants.P_LOG_LEVEL, levelName);
        }
        Logger.applyLogPreferences(levelName, logFileName);
    }

    /**
     * <p>Returns the weaving folder</p>
     * 
     * @return
     */
    public static String getWeavingFolder() {
        final StringBuilder sb = new StringBuilder(System.getProperty("user.home")).append(File.separator);
        sb.append("nexopen-weaving");
        return sb.toString();
    }

    /**
	 * <p></p>
	 * 
	 * @see #getWeavingFolder()
	 * @return
	 */
    public static String getWeavingPath() {
        final StringBuilder sb = new StringBuilder(getWeavingFolder()).append(File.separator);
        sb.append("openfrwk-weaving.jar");
        return sb.toString();
    }

    protected void checkWeavingJar() throws IOException {
        OutputStream out = null;
        try {
            final File weaving = new File(getWeavingPath());
            if (!weaving.exists()) {
                new File(getWeavingFolder()).mkdir();
                weaving.createNewFile();
                final Path src = new Path("weaving/openfrwk-weaving.jar");
                final InputStream in = FileLocator.openStream(getBundle(), src, false);
                out = new FileOutputStream(getWeavingPath(), true);
                IOUtils.copy(in, out);
                Logger.log(Logger.INFO, "Put weaving jar at location " + weaving);
            } else {
                Logger.getLog().info("File openfrwk-weaving.jar already exists at " + weaving);
            }
        } catch (final SecurityException e) {
            Logger.log(Logger.ERROR, "[SECURITY EXCEPTION] Not enough privilegies to create " + "folder and copy NexOpen weaving jar at location " + getWeavingFolder());
            Logger.logException(e);
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    private int _getIntVersion(final String version) {
        if (version.equals("1.2.5")) {
            return 0;
        } else if (version.equals("2.0.0")) {
            return 1;
        } else if (version.equals("2.0.5")) {
            return 2;
        } else if (version.equals("2.0.6")) {
            return 2;
        } else if (version.equals("2.5.0")) {
            return 2;
        }
        return -1;
    }

    /**
     * <p>Specific method for dealing with NexOpen WEB archetypes.</p>
     * 
     * @param version the nexopen version
     * @return a int which deals with position of such version
     */
    private int _getIntWebVersion(final String version) {
        if (!StringUtils.hasText(version)) {
            Logger.getLog().warn("WEB archetype version [" + version + "] incorrect. Returning -1");
            return -1;
        }
        if (version.equals("1.2.5")) {
            return 0;
        } else if (version.equals("2.0.5")) {
            return 1;
        } else if (version.equals("2.0.6")) {
            return 2;
        } else if (version.equals("2.5.0")) {
            return 3;
        }
        Logger.getLog().warn("WEB archetype version [" + version + "] not available. Returning -1");
        return -1;
    }
}
