package gnu.java.util.prefs;

import gnu.classpath.SystemProperties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Properties;
import java.util.prefs.AbstractPreferences;
import java.util.prefs.BackingStoreException;

/**
 * This is a simple file-based preference implementation which writes
 * the preferences as properties files.  A node is represented as a directory
 * beneath the user's home directory.  The preferences for the node are
 * stored in a single properties file in that directory.  Sub-nodes are
 * stored in subdirectories.  This implementation uses file locking to
 * mediate access to the properties files. 
 */
public class FileBasedPreferences extends AbstractPreferences {

    /**
   * Name of the property file storing the data in a given directory.
   */
    private static final String DATA_FILE = "data.properties";

    /**
   * The directory corresponding to this preference node.
   */
    private File directory;

    /**
   * The file holding the data for this node.
   */
    private File dataFile;

    /**
   * The data in this node.
   */
    private Properties properties;

    /**
   * Create the root node for the file-based preferences.
   */
    FileBasedPreferences() {
        super(null, "");
        String home = SystemProperties.getProperty("user.home");
        this.directory = new File(new File(home, ".classpath"), "userPrefs");
        this.dataFile = new File(this.directory, DATA_FILE);
        load();
    }

    /**
   * Create a new file-based preference object with the given parent
   * and the given name.
   * @param parent the parent
   * @param name the name of this node
   */
    FileBasedPreferences(FileBasedPreferences parent, String name) {
        super(parent, name);
        this.directory = new File(parent.directory, name);
        this.dataFile = new File(this.directory, DATA_FILE);
        load();
    }

    private void load() {
        this.properties = new Properties();
        FileInputStream fis = null;
        FileLock lock = null;
        try {
            fis = new FileInputStream(this.dataFile);
            FileChannel channel = fis.getChannel();
            lock = channel.lock(0, Long.MAX_VALUE, true);
            this.properties.load(fis);
        } catch (IOException _) {
            newNode = true;
        } finally {
            try {
                if (lock != null) lock.release();
            } catch (IOException ignore) {
            }
            try {
                if (fis != null) fis.close();
            } catch (IOException ignore) {
            }
        }
    }

    public boolean isUserNode() {
        return true;
    }

    protected String[] childrenNamesSpi() throws BackingStoreException {
        String[] result = directory.list(new FilenameFilter() {

            public boolean accept(File dir, String name) {
                return new File(dir, name).isDirectory();
            }
        });
        if (result == null) result = new String[0];
        return result;
    }

    protected AbstractPreferences childSpi(String name) {
        return new FileBasedPreferences(this, name);
    }

    protected String[] keysSpi() throws BackingStoreException {
        return (String[]) properties.keySet().toArray(new String[0]);
    }

    protected String getSpi(String key) {
        return properties.getProperty(key);
    }

    protected void putSpi(String key, String value) {
        properties.put(key, value);
    }

    protected void removeSpi(String key) {
        properties.remove(key);
    }

    protected void flushSpi() throws BackingStoreException {
        try {
            if (isRemoved()) {
                dataFile.delete();
            } else {
                directory.mkdirs();
                FileOutputStream fos = null;
                FileLock lock = null;
                try {
                    fos = new FileOutputStream(dataFile);
                    FileChannel channel = fos.getChannel();
                    lock = channel.lock();
                    properties.store(fos, "created by GNU Classpath FileBasedPreferences");
                } finally {
                    try {
                        if (lock != null) lock.release();
                    } catch (IOException _) {
                    }
                    try {
                        if (fos != null) fos.close();
                    } catch (IOException _) {
                    }
                }
            }
        } catch (IOException ioe) {
            throw new BackingStoreException(ioe);
        }
    }

    protected void syncSpi() throws BackingStoreException {
        flushSpi();
    }

    protected void removeNodeSpi() throws BackingStoreException {
        flushSpi();
    }
}
