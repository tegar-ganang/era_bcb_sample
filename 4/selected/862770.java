package org.mandiwala.selenium;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.codehaus.plexus.util.StringOutputStream;
import org.mandiwala.ConfigurationPropertyKeys;

/**
 * This class contains the configuration for selenium server.
 */
public class SeleniumServerConfiguration {

    private String seleniumHost;

    private int seleniumPort;

    private boolean startSeleniumServer;

    private boolean seleniumScanPorts;

    private List<Integer> seleniumOmitPorts;

    private File seleniumUserExtensions;

    private boolean seleniumMultiWindow;

    private int seleniumTimeout;

    private File logFile;

    private Level logLevel;

    /**
     * Instantiates a new {@link SeleniumServerConfiguration}.
     * 
     * @param seleniumConfiguration
     *            the selenium configuration
     * 
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    SeleniumServerConfiguration(SeleniumConfiguration seleniumConfiguration) throws IOException {
        seleniumTimeout = seleniumConfiguration.getProps().get(Integer.class, ConfigurationPropertyKeys.SELENIUM_TIMEOUT);
        seleniumPort = seleniumConfiguration.getProps().get(Integer.class, ConfigurationPropertyKeys.SELENIUM_PORT);
        seleniumHost = seleniumConfiguration.getProps().getProperty(ConfigurationPropertyKeys.SELENIUM_HOST);
        startSeleniumServer = seleniumConfiguration.getProps().get(Boolean.class, ConfigurationPropertyKeys.START_SELENIUM_SERVER);
        seleniumScanPorts = seleniumConfiguration.getProps().get(Boolean.class, ConfigurationPropertyKeys.SELENIUM_SCAN_PORTS);
        seleniumOmitPorts = seleniumConfiguration.getProps().getList(Integer.class, ConfigurationPropertyKeys.SELENIUM_OMIT_PORTS);
        seleniumMultiWindow = seleniumConfiguration.getProps().get(Boolean.class, ConfigurationPropertyKeys.SELENIUM_MULTI_WINDOW);
        logFile = seleniumConfiguration.getFile(ConfigurationPropertyKeys.SELENIUM_LOG_FILE, seleniumConfiguration.getDirectoryConfiguration().getOutput(), false);
        if (logFile != null) {
            logLevel = Level.toLevel(seleniumConfiguration.getProps().get(String.class, ConfigurationPropertyKeys.SELENIUM_LOG_LEVEL));
        }
    }

    /**
     * Second initialization step. {@link SeleniumConfiguration} calls this
     * after calling all constructors.
     * 
     * @param seleniumConfiguration
     *            the selenium configuration
     * 
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    void initialize(SeleniumConfiguration seleniumConfiguration) throws IOException {
        initUserExtensions(seleniumConfiguration);
    }

    private void initUserExtensions(SeleniumConfiguration seleniumConfiguration) throws IOException {
        StringBuilder contents = new StringBuilder();
        StringOutputStream s = new StringOutputStream();
        IOUtils.copy(SeleniumConfiguration.class.getResourceAsStream("default-user-extensions.js"), s);
        contents.append(s.toString());
        File providedUserExtensions = seleniumConfiguration.getFile(ConfigurationPropertyKeys.SELENIUM_USER_EXTENSIONS, seleniumConfiguration.getDirectoryConfiguration().getInput(), false);
        if (providedUserExtensions != null) {
            contents.append(FileUtils.readFileToString(providedUserExtensions, null));
        }
        seleniumUserExtensions = new File(seleniumConfiguration.getDirectoryConfiguration().getInput(), "user-extensions.js");
        FileUtils.forceMkdir(seleniumUserExtensions.getParentFile());
        FileUtils.writeStringToFile(seleniumUserExtensions, contents.toString(), null);
    }

    /**
     * Gets the selenium host.
     * 
     * @return the selenium host
     */
    public String getSeleniumHost() {
        return seleniumHost;
    }

    /**
     * Sets the selenium host.
     * 
     * @param seleniumHost
     *            the new selenium host
     */
    public void setSeleniumHost(String seleniumHost) {
        this.seleniumHost = seleniumHost;
    }

    /**
     * Gets the selenium port.
     * 
     * @return the selenium port
     */
    public int getSeleniumPort() {
        return seleniumPort;
    }

    /**
     * Sets the selenium port.
     * 
     * @param seleniumPort
     *            the new selenium port
     */
    public void setSeleniumPort(int seleniumPort) {
        this.seleniumPort = seleniumPort;
    }

    /**
     * Checks if is start selenium server.
     * 
     * @return true, if is start selenium server
     */
    public boolean isStartSeleniumServer() {
        return startSeleniumServer;
    }

    /**
     * Sets the start selenium server.
     * 
     * @param startSeleniumServer
     *            the new start selenium server
     */
    public void setStartSeleniumServer(boolean startSeleniumServer) {
        this.startSeleniumServer = startSeleniumServer;
    }

    /**
     * Checks if is selenium scan ports.
     * 
     * @return true, if is selenium scan ports
     */
    public boolean isSeleniumScanPorts() {
        return seleniumScanPorts;
    }

    /**
     * Sets the selenium scan ports.
     * 
     * @param seleniumScanPorts
     *            the new selenium scan ports
     */
    public void setSeleniumScanPorts(boolean seleniumScanPorts) {
        this.seleniumScanPorts = seleniumScanPorts;
    }

    /**
     * Gets the selenium user extensions.
     * 
     * @return the selenium user extensions
     */
    public File getSeleniumUserExtensions() {
        return seleniumUserExtensions;
    }

    /**
     * Sets the selenium user extensions.
     * 
     * @param seleniumUserExtensions
     *            the new selenium user extensions
     */
    public void setSeleniumUserExtensions(File seleniumUserExtensions) {
        this.seleniumUserExtensions = seleniumUserExtensions;
    }

    /**
     * Checks if is selenium multi window.
     * 
     * @return true, if is selenium multi window
     */
    public boolean isSeleniumMultiWindow() {
        return seleniumMultiWindow;
    }

    /**
     * Sets the selenium multi window.
     * 
     * @param seleniumMultiWindow
     *            the new selenium multi window
     */
    public void setSeleniumMultiWindow(boolean seleniumMultiWindow) {
        this.seleniumMultiWindow = seleniumMultiWindow;
    }

    /**
     * Gets the selenium omit ports.
     * 
     * @return the selenium omit ports
     */
    public List<Integer> getSeleniumOmitPorts() {
        return seleniumOmitPorts;
    }

    /**
     * Sets the selenium omit ports.
     * 
     * @param seleniumOmitPorts
     *            the new selenium omit ports
     */
    public void setSeleniumOmitPorts(List<Integer> seleniumOmitPorts) {
        this.seleniumOmitPorts = seleniumOmitPorts;
    }

    /**
     * Gets the selenium timeout.
     * 
     * @return the selenium timeout
     */
    public int getSeleniumTimeout() {
        return seleniumTimeout;
    }

    /**
     * Sets the selenium timeout.
     * 
     * @param seleniumTimeout
     *            the new selenium timeout
     */
    public void setSeleniumTimeout(int seleniumTimeout) {
        this.seleniumTimeout = seleniumTimeout;
    }

    /**
     * @return the logFile
     */
    public File getLogFile() {
        return logFile;
    }

    /**
     * @param logFile
     *            the logFile to set
     */
    public void setLogFile(File logFile) {
        this.logFile = logFile;
    }

    /**
     * @return the logLevel
     */
    public Level getLogLevel() {
        return logLevel;
    }

    /**
     * @param logLevel
     *            the logLevel to set
     */
    public void setLogLevel(Level logLevel) {
        this.logLevel = logLevel;
    }
}
