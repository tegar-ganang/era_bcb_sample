package org.archive.spring;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.archive.util.ArchiveUtils;
import org.archive.util.FilesystemLinkMaker;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Spring ApplicationContext extended for Heritrix use.
 * 
 * Notable extensions:
 * 
 * Remembers its primary XML configuration file, and can report its filesystem
 * path.
 * 
 * Reports a summary of Errors collected from self-Validating Beans.
 * 
 * Generates launchId from timestamp, creates launch directory
 * {jobDir}/{launchId}, and snapshots crawl configuration file into the launch
 * directory. Other configuration files, if any, are automatically snapshotted
 * into the launch directory when they are read (see
 * {@link ConfigFile#obtainReader()}). The token ${launchId} will be
 * interpolated in configuration-relative paths (see
 * {@link ConfigPathConfigurer}) so that launch-specific paths can be used for
 * logs, reports, warcs, etc.
 * 
 * @contributor gojomo
 */
public class PathSharingContext extends FileSystemXmlApplicationContext {

    private static Logger LOGGER = Logger.getLogger(PathSharingContext.class.getName());

    public PathSharingContext(String configLocation) throws BeansException {
        super(configLocation);
    }

    public PathSharingContext(String[] configLocations, ApplicationContext parent) throws BeansException {
        super(configLocations, parent);
    }

    public PathSharingContext(String[] configLocations, boolean refresh, ApplicationContext parent) throws BeansException {
        super(configLocations, refresh, parent);
    }

    public PathSharingContext(String[] configLocations, boolean refresh) throws BeansException {
        super(configLocations, refresh);
    }

    public PathSharingContext(String[] configLocations) throws BeansException {
        super(configLocations);
    }

    public String getPrimaryConfigurationPath() {
        return getConfigLocations()[0];
    }

    HashMap<String, Errors> allErrors;

    public void validate() {
        allErrors = new HashMap<String, Errors>();
        for (Entry<String, HasValidator> entry : getBeansOfType(HasValidator.class).entrySet()) {
            String name = entry.getKey();
            HasValidator hv = entry.getValue();
            Validator v = hv.getValidator();
            Errors errors = new BeanPropertyBindingResult(hv, name);
            v.validate(hv, errors);
            if (errors.hasErrors()) {
                allErrors.put(name, errors);
            }
        }
        for (String name : allErrors.keySet()) {
            for (Object obj : allErrors.get(name).getAllErrors()) {
                LOGGER.fine("validation error for '" + name + "': " + obj);
            }
        }
    }

    @Override
    public void start() {
        initLaunchDir();
        super.start();
    }

    public HashMap<String, Errors> getAllErrors() {
        return allErrors;
    }

    protected transient String currentLaunchId;

    protected void initLaunchId() {
        currentLaunchId = ArchiveUtils.getUnique14DigitDate();
        LOGGER.info("launch id " + currentLaunchId);
    }

    public String getCurrentLaunchId() {
        return currentLaunchId;
    }

    protected transient File currentLaunchDir;

    public File getCurrentLaunchDir() {
        return currentLaunchDir;
    }

    protected File getConfigurationFile() {
        String primaryConfigurationPath = getPrimaryConfigurationPath();
        if (primaryConfigurationPath.startsWith("file:")) {
            primaryConfigurationPath = primaryConfigurationPath.substring(5);
        }
        File configFile = new File(primaryConfigurationPath);
        return configFile;
    }

    protected void initLaunchDir() {
        initLaunchId();
        try {
            currentLaunchDir = new File(getConfigurationFile().getParentFile(), getCurrentLaunchId());
            if (!currentLaunchDir.mkdir()) {
                throw new IOException("failed to create directory " + currentLaunchDir);
            }
            FileUtils.copyFileToDirectory(getConfigurationFile(), currentLaunchDir);
            File latestSymlink = new File(getConfigurationFile().getParentFile(), "latest");
            latestSymlink.delete();
            boolean success = FilesystemLinkMaker.makeSymbolicLink(currentLaunchDir.getName(), latestSymlink.getPath());
            if (!success) {
                LOGGER.warning("failed to create symlink from " + latestSymlink + " to " + currentLaunchDir);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "failed to initialize launch directory: " + e);
            currentLaunchDir = null;
        }
    }

    /**
     * Initialize the LifecycleProcessor.
     * Uses HeritrixLifecycleProcessor, which prevents an automatic lifecycle
     * start(), if none defined in the context.
     * @see org.springframework.context.support.DefaultLifecycleProcessor
     */
    protected void initLifecycleProcessor() {
        ConfigurableListableBeanFactory beanFactory = getBeanFactory();
        if (!beanFactory.containsLocalBean(LIFECYCLE_PROCESSOR_BEAN_NAME)) {
            HeritrixLifecycleProcessor obj = (HeritrixLifecycleProcessor) beanFactory.createBean(HeritrixLifecycleProcessor.class);
            beanFactory.registerSingleton(LIFECYCLE_PROCESSOR_BEAN_NAME, obj);
        }
        super.initLifecycleProcessor();
    }
}
