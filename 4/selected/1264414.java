package org.jiinxed.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A store for peer data fragments.
 * @author S�bastien M�dan
 */
public class PeerDataStorage implements Serializable {

    public PeerDataStorage() {
    }

    /**
    * Creates the client peer storage.
    * @param capacity the desired peer storage capacity. That value is expressed
    *    in blocks of {@link #FRAGMENT_SIZE} bytes. For example,
    *    <code>100</code> would create a peer store that can store 100
    *    fragments. The store would be 100MB big.
    */
    public void create(long capacity) throws IOException {
        if (capacity < MIN_CAPACITY) {
            throw new IllegalArgumentException("Minimum store capacity is " + MIN_CAPACITY + " fragments");
        }
        final File file = new File(getPath());
        file.createNewFile();
        FileOutputStream fos = null;
        FileChannel fileChannel = null;
        try {
            fos = new FileOutputStream(file);
            fileChannel = fos.getChannel();
            fileChannel.position(capacity * FRAGMENT_SIZE - 1);
            fos.write(0);
            logger.log(Level.INFO, "Created a new " + capacity + "MB store at " + getPath());
        } finally {
            if (fileChannel != null) {
                try {
                    fileChannel.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, null, e);
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    logger.log(Level.WARNING, null, e);
                }
            }
        }
    }

    /**
    * Fetches the name of the file that backs the peer store. Includes the path.
    * @return A non null string.
    **/
    public String getPath() {
        return Config.properties.getProperty(Config.PROP_PEER_STORAGE_PATH) + STORE_FILENAME;
    }

    private static final transient Logger logger = Logger.getLogger("org.jiinxed");

    /**
    * The size of a file fragment, in bytes. Currently set to 1MB.
    */
    public static final transient int FRAGMENT_SIZE = 1024 * 1024;

    /**
    * The minimum number of fragments that a store is allowed to contain.
    */
    public static final transient int MIN_CAPACITY = 20;

    /**
    * The name of the file that contains the peer data.
    */
    public static final transient String STORE_FILENAME = "peer_data.jiinxed";
}
