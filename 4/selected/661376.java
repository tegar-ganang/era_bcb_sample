package nl.tranquilizedquality.itest.cargo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import nl.tranquilizedquality.itest.cargo.exception.ConfigurationException;
import nl.tranquilizedquality.itest.cargo.exception.DeployException;
import nl.tranquilizedquality.itest.domain.DeployableLocationConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.Deployable;
import org.codehaus.cargo.container.deployable.DeployableType;
import org.codehaus.cargo.container.installer.ZipURLInstaller;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;
import org.codehaus.cargo.generic.deployable.DefaultDeployableFactory;
import org.codehaus.cargo.util.log.FileLogger;
import org.codehaus.cargo.util.log.LogLevel;
import org.codehaus.cargo.util.log.Logger;
import org.springframework.beans.factory.annotation.Required;

/**
 * This is a base utility class that can be used to use Cargo with an installed
 * installedLocalContainer. It can configure, start and stop the
 * installedLocalContainer. In this case a JBoss instance.
 * 
 * @author Salomo Petrus (sape)
 * @since 11/12/2008
 * 
 */
public abstract class AbstractJBossContainerUtil extends AbstractInstalledContainerUtil {

    /** Logger for this class */
    private static final Log log = LogFactory.getLog(AbstractJBossContainerUtil.class);

    /**
	 * The suffix of properties files that will be picked up when they are
	 * located in the configuration resource directory which by default is
	 * src/test/resources/.
	 */
    private static final String PROPERTIES_FILES_SUFFIX = ".properties";

    /**
	 * The JBoss log4j XML file for logging configuration that will be picked up
	 * automatically by this container utility if it exists in the configuration
	 * resource directory which by default is src/test/resources. So if there is
	 * a log4j.xml file in there this will be used.
	 */
    private static final String LOG4J_XML = "log4j.xml";

    /**
	 * The suffix of JBoss data source files that will be picked up when they
	 * are located in the configuration resource directory which by default is
	 * src/test/resources/.
	 */
    private static final String DATA_SOURCE_FILES_SUFFIX = "-ds.xml";

    /**
	 * The port where the JNP service will run on. This service is used to be
	 * able to stop JBoss in a graceful way. Use the property ${cargo.jnp.port}
	 * to set the port dynamically and set the system properties with this
	 * value. Cargo seems to search for this service on port 1299.
	 */
    private Integer jnpPort;

    /** The ZIP file containing the JBoss configuration. */
    private String containerConfigurationFile;

    /**
	 * Determines if the auto detection of configuration files is enabled or
	 * not.
	 */
    private boolean autoDetect;

    /** The name of the JBOSS configuration to use. */
    protected String configurationName;

    /**
	 * Default constructor that will detect which OS is used to make sure the
	 * JBOSS will be downloaded in the correct location.
	 */
    public AbstractJBossContainerUtil() {
        autoDetect = true;
        setContainerName("JBoss");
        cleanUpContainer();
    }

    /**
	 * Installs the container and the application configuration. It also sets
	 * some system properties so the container can startup properly. Finally it
	 * sets up additional configuration like jndi.proprties files etc.
	 * 
	 * @throws Exception
	 *             Is thrown when something goes wrong during the setup of the
	 *             container.
	 */
    protected void setupContainer() throws Exception {
        super.setupContainer();
        if (log.isInfoEnabled()) {
            log.info("Downloading configuration from: " + remoteLocation);
            log.info("Container configuration file: " + containerConfigurationFile);
        }
        if (log.isInfoEnabled()) {
            log.info("Installing [" + configurationName + "] configuration...");
        }
        final URL remoteLocation = new URL(this.remoteLocation + containerConfigurationFile);
        final String installDir = containerHome + "server/";
        final ZipURLInstaller installer = new ZipURLInstaller(remoteLocation, installDir);
        installer.install();
        systemProperties.put("jboss.server.lib.url:lib", "file:lib/");
        systemProperties.put("cargo.jnp.port", jnpPort.toString());
        if (autoDetect) {
            copyResourceFileToConfDir(LOG4J_XML);
            final List<String> dataSourceFiles = findConfigurationFiles(DATA_SOURCE_FILES_SUFFIX);
            for (final String fileName : dataSourceFiles) {
                final String deployDirectory = getContainerDirectory("deploy/");
                copyResourceFile(fileName, deployDirectory);
            }
            final List<String> propertiesFiles = findConfigurationFiles(PROPERTIES_FILES_SUFFIX);
            for (final String fileName : propertiesFiles) {
                copyResourceFileToConfDir(fileName);
            }
        }
        setupConfiguration();
    }

    /**
	 * Searches for configuration files with the specified suffix.
	 * 
	 * @param suffix
	 *            The file suffix.
	 * @return Returns a list of file names that end with the specified suffix.
	 */
    protected List<String> findConfigurationFiles(final String suffix) {
        final List<String> files = new ArrayList<String>();
        final File directory = new File(configResourcesPath);
        final File[] listFiles = directory.listFiles();
        for (File file : listFiles) {
            final String name = file.getName();
            if (org.springframework.util.StringUtils.endsWithIgnoreCase(name, suffix)) {
                files.add(name);
                if (log.isInfoEnabled()) {
                    log.info("Added configuration file called: " + name);
                }
            }
        }
        return files;
    }

    /**
	 * Copies the specified resource file to the configuration directory of
	 * JBoss.
	 * 
	 * @param fileName
	 *            The file name that needs to be copied.
	 */
    protected void copyResourceFileToConfDir(final String fileName) {
        copyResourceFile(fileName, getConfDirectory());
    }

    protected void copyResourceFile(final String fileName, final String destinationDirectory) {
        final String originalFile = configResourcesPath + fileName;
        final File srcFile = new File(originalFile);
        final String newFile = destinationDirectory + fileName;
        final File destFile = new File(newFile);
        try {
            FileUtils.copyFile(srcFile, destFile);
            if (log.isInfoEnabled()) {
                log.info("Copied file " + fileName + " to " + destFile.getAbsolutePath());
            }
        } catch (IOException e) {
            if (log.isWarnEnabled()) {
                log.warn("Failed to copy resource file: " + fileName);
            }
        }
    }

    protected void deploy() {
        final ConfigurationFactory configurationFactory = new DefaultConfigurationFactory();
        final LocalConfiguration configuration = (LocalConfiguration) configurationFactory.createConfiguration("jboss4x", ContainerType.INSTALLED, ConfigurationType.EXISTING, containerHome + "server/" + configurationName);
        final StringBuilder args = new StringBuilder();
        for (String arg : jvmArguments) {
            args.append(arg);
            args.append(" ");
            if (log.isInfoEnabled()) {
                log.info("Added JVM argument: " + arg);
            }
        }
        configuration.setProperty(GeneralPropertySet.JVMARGS, args.toString());
        configuration.setProperty(ServletPropertySet.PORT, containerPort.toString());
        Set<Entry<String, String>> entrySet = deployableLocations.entrySet();
        Iterator<Entry<String, String>> iterator = entrySet.iterator();
        while (iterator.hasNext()) {
            Entry<String, String> entry = iterator.next();
            String key = entry.getKey();
            String value = entry.getValue();
            DeployableType deployableType = null;
            deployableType = determineDeployableType(value);
            addDeployable(configuration, key, deployableType);
        }
        for (DeployableLocationConfiguration config : deployableLocationConfigurations) {
            final String contextName = config.getContextName();
            final String type = config.getType();
            String path = config.getPath();
            DeployableType deployableType = null;
            if (contextName != null && contextName.length() > 0) {
                deployableType = determineDeployableType(type);
                if (DeployableType.WAR.equals(deployableType)) {
                    final File srcFile = new File(path);
                    final File destFile = new File("target/" + contextName + ".war");
                    try {
                        FileUtils.copyFile(srcFile, destFile);
                    } catch (IOException e) {
                        throw new DeployException("Failed to copy WAR file: " + path, e);
                    }
                    path = destFile.getPath();
                }
            } else {
                deployableType = determineDeployableType(type);
            }
            addDeployable(configuration, path, deployableType);
        }
        installedLocalContainer = (InstalledLocalContainer) new DefaultContainerFactory().createContainer("jboss4x", ContainerType.INSTALLED, configuration);
        installedLocalContainer.setHome(containerHome);
        final Logger fileLogger = new FileLogger(new File(cargoLogFilePath + "cargo.log"), true);
        fileLogger.setLevel(LogLevel.DEBUG);
        installedLocalContainer.setLogger(fileLogger);
        installedLocalContainer.setOutput(cargoLogFilePath + "output.log");
        installedLocalContainer.setSystemProperties(systemProperties);
        if (log.isInfoEnabled()) {
            log.info("Starting JBoss [" + configurationName + "]...");
        }
        installedLocalContainer.start();
        if (log.isInfoEnabled()) {
            log.info("JBoss up and running!");
        }
    }

    /**
	 * Determines the type of deployable.
	 * 
	 * @param type
	 *            A string representation of the deployable type.
	 * @return Returns a {@link DeployableType} that corresponds to the string
	 *         representation or if none could be found the default value (EAR)
	 *         will be returned.
	 */
    private DeployableType determineDeployableType(final String type) {
        DeployableType deployableType;
        if ("EAR".equals(type)) {
            deployableType = DeployableType.EAR;
        } else if ("WAR".equals(type)) {
            deployableType = DeployableType.WAR;
        } else if ("EJB".equals(type)) {
            deployableType = DeployableType.EJB;
        } else {
            deployableType = DeployableType.EAR;
        }
        return deployableType;
    }

    /**
	 * Adds a deployable to the {@link LocalConfiguration}.
	 * 
	 * @param configuration
	 *            The configuration where a deployable can be added to.
	 * @param path
	 *            The path where the deployable can be found.
	 * @param deployableType
	 *            The type of deployable.
	 */
    private void addDeployable(LocalConfiguration configuration, String path, DeployableType deployableType) {
        Deployable deployable = new DefaultDeployableFactory().createDeployable(configurationName, path, deployableType);
        configuration.addDeployable(deployable);
    }

    /**
	 * @param containerConfigurationFile
	 *            the containerConfigurationFile to set
	 */
    @Required
    public void setContainerConfigurationFile(String containerConfigurationFile) {
        this.containerConfigurationFile = containerConfigurationFile;
    }

    /**
	 * @param configurationName
	 *            the configurationName to set
	 */
    @Required
    public void setConfigurationName(String configurationName) {
        this.configurationName = configurationName;
    }

    /**
	 * @param jnpPort
	 *            the jnpPort to set
	 */
    @Required
    public void setJnpPort(Integer jnpPort) {
        this.jnpPort = jnpPort;
    }

    /**
	 * Constructs the full path to a specific directory from the configuration.
	 * 
	 * @param dir
	 *            The directory name.
	 * @return Returns a String representation of the full path.
	 */
    private String getContainerDirectory(final String dir) {
        final StringBuilder fullPath = new StringBuilder();
        fullPath.append(this.containerHome);
        fullPath.append("server/");
        fullPath.append(this.configurationName);
        fullPath.append("/");
        fullPath.append(dir);
        final String path = fullPath.toString();
        final File directory = new File(path);
        if (!directory.exists()) {
            final String msg = dir + " directory does not excist! : " + path;
            if (log.isErrorEnabled()) {
                log.error(msg);
            }
            throw new ConfigurationException(msg);
        }
        return path;
    }

    public String getSharedLibDirectory() {
        return getContainerDirectory("lib/");
    }

    public String getConfDirectory() {
        return getContainerDirectory("conf/");
    }

    /**
	 * @param autoDetect
	 *            the autoDetect to set
	 */
    public void setAutoDetect(boolean autoDetect) {
        this.autoDetect = autoDetect;
    }
}
