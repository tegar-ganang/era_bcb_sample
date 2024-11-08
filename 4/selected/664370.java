package net.sourceforge.recman.backend;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;

/**
 * Config
 * 
 * @author Marcus Kessel
 * 
 */
@SuppressWarnings("serial")
public class Config extends XMLConfiguration {

    private static final Logger LOG = Logger.getLogger(Config.class);

    private static final String CONFIG_DIR = "recman";

    private static final String CONFIG_FILE = "recman-config.xml";

    private static final String DEFAULT_CONFIG_FILE = "default-recman-config.xml";

    private static Config instance;

    /**
     * Constructor
     * 
     * @throws IOException
     * @throws ConfigurationException
     */
    protected Config() throws ConfigurationException, IOException {
        super(resolveConfigFile());
    }

    public static Config get() {
        if (instance == null) {
            try {
                instance = new Config();
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
        return instance;
    }

    /**
     * Resolve config file
     * 
     * @return
     * @throws IOException
     */
    private static File resolveConfigFile() throws IOException {
        File configDir = new File(SystemUtils.getUserHome(), CONFIG_DIR);
        if (!configDir.exists()) {
            if (!configDir.mkdir()) {
                throw new IOException("cannot read config directory: " + configDir.getAbsolutePath());
            }
        } else if (!configDir.canWrite()) {
            throw new IOException("no write access in config directory: " + configDir.getAbsolutePath());
        }
        File configFile = new File(configDir, CONFIG_FILE);
        if (!configFile.exists()) {
            File defaultConfig = new File(DEFAULT_CONFIG_FILE);
            FileUtils.copyFile(defaultConfig, configFile);
        }
        return configFile;
    }

    @SuppressWarnings("unchecked")
    public List<File> getRecordingDirectories() {
        List<String> dirList = getList("recordings.directory");
        List<File> directories = new ArrayList<File>(dirList.size());
        for (String directory : dirList) {
            try {
                File dir = this.validatePath(directory);
                directories.add(dir);
            } catch (IOException e) {
                LOG.warn(e);
            }
        }
        return directories;
    }

    public int getScanInterval() {
        return getInt("monitor.scanInterval", 30);
    }

    public String getDatabaseLocation() {
        return getString("database.location", "/tmp/recordings.xml");
    }

    public String getThumbnailDirectory() {
        return getString("thumbnails.directory");
    }

    public int getThumbnailAmount() {
        return getInt("thumbnails.amount", 10);
    }

    public int getThumbnailWidth() {
        return getInt("thumbnails.width", 320);
    }

    public int getThumbnailHeight() {
        return getInt("thumbnails.height", 240);
    }

    @SuppressWarnings("unchecked")
    public List<String> getIpFilterRules() {
        return getList("ipfilter.rule");
    }

    public File getTimersConf() throws IOException {
        String timersLocation = getString("timers..conf", "/tmp/timers.conf");
        return this.validateFile(timersLocation);
    }

    public File getChannelsConf() throws IOException {
        String channelsLocation = getString("channels..conf", "/tmp/channels.conf");
        return this.validateFile(channelsLocation);
    }

    /**
     * Validate given path
     * 
     * @param path
     * @return
     * @throws IOException
     */
    private File validatePath(String path) throws IOException {
        File dir = new File(path.trim());
        if (!dir.exists() || !dir.isDirectory() || !dir.canRead()) {
            throw new IOException("Given path is not a valid directory: " + dir.getAbsolutePath());
        }
        return dir;
    }

    /**
     * Validate given file
     * 
     * @param filePath
     * @return
     * @throws IOException
     */
    private File validateFile(String filePath) throws IOException {
        File file = new File(filePath.trim());
        if (!file.exists() || !file.isFile() || !file.canRead()) {
            throw new IOException("Given file is not a valid: " + file.getAbsolutePath());
        }
        return file;
    }
}
