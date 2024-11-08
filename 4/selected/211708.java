package nl.tranquilizedquality.itest.cargo;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
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
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.ConfigurationFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;
import org.codehaus.cargo.generic.deployable.DefaultDeployableFactory;
import org.codehaus.cargo.util.log.FileLogger;
import org.codehaus.cargo.util.log.LogLevel;
import org.codehaus.cargo.util.log.Logger;

/**
 * AbstractTomcatContainerUtil is an implementation of {@link ContainerUtil}
 * which managaes a Tomcat servlet container. It can configure, start and stop
 * the Tomcat servlet container.
 * 
 * @author Salomo Petrus
 * 
 */
public abstract class AbstractTomcatContainerUtil extends AbstractInstalledContainerUtil {

    /** Logger for this class */
    private static final Log log = LogFactory.getLog(AbstractTomcatContainerUtil.class);

    /**
	 * Default constructor that will detect which OS is used to make sure the
	 * Tomcat will be downloaded in the correct location.
	 */
    public AbstractTomcatContainerUtil() {
        setContainerName("Tomcat");
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
    @Override
    protected void setupContainer() throws Exception {
        super.setupContainer();
        setupConfiguration();
    }

    /**
	 * Deploys the application to the correct
	 */
    @Override
    protected void deploy() {
        final ConfigurationFactory configurationFactory = new DefaultConfigurationFactory();
        final LocalConfiguration configuration = (LocalConfiguration) configurationFactory.createConfiguration("tomcat5x", ContainerType.INSTALLED, ConfigurationType.EXISTING, containerHome);
        final StringBuilder args = new StringBuilder();
        for (final String arg : jvmArguments) {
            args.append(arg);
            args.append(" ");
            if (log.isInfoEnabled()) {
                log.info("Added JVM argument: " + arg);
            }
        }
        configuration.setProperty(GeneralPropertySet.JVMARGS, args.toString());
        configuration.setProperty(ServletPropertySet.PORT, containerPort.toString());
        final Set<Entry<String, String>> entrySet = deployableLocations.entrySet();
        final Iterator<Entry<String, String>> iterator = entrySet.iterator();
        while (iterator.hasNext()) {
            final Entry<String, String> entry = iterator.next();
            final String key = entry.getKey();
            final String value = entry.getValue();
            DeployableType deployableType = null;
            deployableType = determineDeployableType(value);
            addDeployable(configuration, key, deployableType);
        }
        for (final DeployableLocationConfiguration config : deployableLocationConfigurations) {
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
                    } catch (final IOException e) {
                        throw new DeployException("Failed to copy WAR file: " + path, e);
                    }
                    path = destFile.getPath();
                }
            } else {
                deployableType = determineDeployableType(type);
            }
            addDeployable(configuration, path, deployableType);
        }
        installedLocalContainer = (InstalledLocalContainer) new DefaultContainerFactory().createContainer("tomcat5x", ContainerType.INSTALLED, configuration);
        installedLocalContainer.setHome(containerHome);
        final Logger fileLogger = new FileLogger(new File(cargoLogFilePath + "cargo.log"), true);
        fileLogger.setLevel(LogLevel.DEBUG);
        installedLocalContainer.setLogger(fileLogger);
        installedLocalContainer.setOutput(cargoLogFilePath + "output.log");
        installedLocalContainer.setSystemProperties(systemProperties);
        if (log.isInfoEnabled()) {
            log.info("Starting Tomcat ...");
        }
        installedLocalContainer.start();
        if (log.isInfoEnabled()) {
            log.info("Tomcat up and running!");
        }
    }

    /**
	 * @return the deployableLocations
	 */
    public Map<String, String> getDeployableLocations() {
        return deployableLocations;
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
            throw new DeployException("Tomcat doesn't support EAR files!");
        } else if ("WAR".equals(type)) {
            deployableType = DeployableType.WAR;
        } else if ("EJB".equals(type)) {
            throw new DeployException("Tomcat doesn't support EJB files!");
        } else {
            deployableType = DeployableType.WAR;
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
    private void addDeployable(final LocalConfiguration configuration, final String path, final DeployableType deployableType) {
        final Deployable deployable = new DefaultDeployableFactory().createDeployable("jetty", path, deployableType);
        configuration.addDeployable(deployable);
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
}
