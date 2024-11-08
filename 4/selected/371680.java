package wizworld.navigate.worldwind;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.BasicDataFileStore;
import java.io.File;
import org.w3c.dom.Node;

/** Configurable cache */
public class ConfigurableWriteCache extends BasicDataFileStore {

    /** Where the cache is */
    private static File path;

    /** Constructor
	 */
    public ConfigurableWriteCache() {
        buildWritePaths(null);
        buildReadPaths(null);
    }

    /** Configure the environment for use
     * @param cacheRoot	Cache path
     */
    public static void configure(File cacheRoot) {
        Configuration.setValue(AVKey.DATA_FILE_STORE_CLASS_NAME, ConfigurableWriteCache.class.getName());
        path = cacheRoot;
    }

    /** Get the cache path
	 * @return	Cache file
	 */
    public File getWriteLocation() {
        if (path == null) {
            path = new BasicDataFileStore().getWriteLocation();
        }
        return path;
    }

    /** Make read paths
	 */
    protected void buildReadPaths(Node arg0) {
        this.readLocations.add(0, this.writeLocation);
    }

    /** Make write paths
	 */
    protected void buildWritePaths(Node arg0) {
        this.writeLocation = new StoreLocation(getWriteLocation());
        this.readLocations.add(0, this.writeLocation);
    }
}
