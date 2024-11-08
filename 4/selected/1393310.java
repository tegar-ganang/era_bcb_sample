package fgk.util.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * This cache is backed by a disk cache for persisting data and a memory
 * cache to speed up access and to remove disk IO from most put and get calls.
 * There is no attempt to limit disk useage, callers should be careful to
 * manage total disk space and call cleanCacheDirectory() as needed. The
 * original purpose of this code was to provide a simple yet speedy 'proxy'
 * cache of images fetched from a slow distant server. The local server could
 * then provide the data quickly to local machines. Since the data was
 * static there was no need to worry about staleness.
 *
 * This assumes a 'reasonable' load and capacity. This has not been tested
 * in a heavy duty environment where extremes are likely to be encountered.
 * As always, multithreaded code can have subtle bugs, test everything!
 *
 * TODO: this could use a clean shutdown process to flush the pending queue.
 *
 */
public class Cache {

    /**
   * The memory cache uses a millisecond aperature to group items
   * together in order to clean up blocks of LRU items at once.
   */
    public static final int APERTURE_SIZE = 300;

    /**
   * Create a cache, set the disk cache using the given path appended to
   * the default directory. Use -Duser.workdir=/testdir/path if it is not
   * already defined. If not found, the temp directory will be used. The
   * memory cache is set to use half of the available memory as a maximum.
   *
   * @param cacheDirectoryPath cache dir name
   */
    public Cache(final String cacheDirectoryPath) {
        final long freememory = Runtime.getRuntime().freeMemory();
        final int cachesize = (int) (freememory / 2);
        final int cleansize = Math.min(cachesize / 8, 30000);
        init(cacheDirectoryPath, cachesize, cleansize, APERTURE_SIZE);
    }

    /**
   * Create a cache with all settable parameters.
   *
   * @param cacheDirectoryPath  path appened to work dir to store items
   * @param cachesize  the max bytes suggested to use
   * @param cleansize  when max exceeded, remove this many bytes
   * @param aperturesize  the millisecond aperture to group LRU items for removes
   */
    public Cache(String cacheDirectoryPath, int cachesize, int cleansize, int aperturesize) {
        init(cacheDirectoryPath, cachesize, cleansize, aperturesize);
    }

    /**
   * The constructors call this method with the complete settings for all
   * cache parameters.
   * @param cacheDirectoryPath  path appened to work dir to store items
   * @param cachesize  the max bytes suggested to use
   * @param cleansize  when max exceeded, remove this many bytes
   * @param aperturesize  the millisecond aperture to group LRU items for removes
   */
    private void init(String cacheDirectoryPath, int cachesize, int cleansize, int aperturesize) {
        diskcache_ = new DiskCache(cacheDirectoryPath);
        memorycache_ = new MemoryCache(cachesize, cleansize, aperturesize);
        writecacherunnable_ = new WriteCacheRunnable();
        final Thread writecacherunnablethread = new Thread(writecacherunnable_);
        writecacherunnablethread.setName("CacheWriterThread");
        writecacherunnablethread.start();
    }

    /**
   * Store the given block in the cache directory with the given identifier.
   * @param key  an id that is used to create a disk file.
   * @param datablock  the block of data to store in the cache directory.
   */
    public void put(final String key, final byte[] datablock) {
        final byte[] newdatablock = new byte[datablock.length];
        System.arraycopy(datablock, 0, newdatablock, 0, datablock.length);
        synchronized (lock_) {
            pendingwrites_.put(key, newdatablock);
        }
    }

    /**
   * Locate the block identified with the given key and return it, or an empty
   * block if not found, in which case the caller should store the results here.
   * @param key  an id that was used to create a disk file.
   * @return the block of data.
   */
    public byte[] get(final String key) {
        synchronized (lock_) {
            if (pendingwrites_.containsKey(key)) {
                return pendingwrites_.get(key);
            }
        }
        byte[] bytes = memorycache_.get(key);
        if ((bytes != null) && (bytes.length > 0)) {
            return bytes;
        }
        bytes = diskcache_.get(key);
        memorycache_.put(key, bytes);
        return bytes;
    }

    /**
   * Indicate if the cache contains this or not.
   * NOTE WELL: the item in that cache can be removed elsewhere after this call.
   * @param key  an id that was used to create a disk file.
   * @return true if key was found
   */
    public boolean contains(final String key) {
        if (memorycache_.contains(key)) {
            return true;
        }
        if (diskcache_.contains(key)) {
            return true;
        }
        synchronized (lock_) {
            if (pendingwrites_.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    /**
   * Brutally remove all cache files in the given directory. Intended for testing.
   */
    public void cleanCacheDirectory() {
        diskcache_.cleanCacheDirectory();
    }

    /**
   * Whenever a new item is fetched, a copy of it is put in the writepending map,
   * and this thread periodically wakes up and checks to see if it should write
   * anything to the cache.
   *
   * Warning: halting the program while items are in transit means they will be lost!
   */
    private class WriteCacheRunnable implements Runnable {

        private static final int SLEEP_MILLIS = 50;

        private WriteCacheRunnable() {
            active_ = true;
        }

        /**
     * when things appear in the write queue, send them to the cache for storing
     */
        public void run() {
            try {
                while (active_) {
                    synchronized (lock_) {
                        if (pendingwrites_.size() > 0) {
                            final String key = pendingwrites_.keySet().iterator().next();
                            final byte[] datablock = pendingwrites_.remove(key);
                            memorycache_.put(key, datablock);
                            diskcache_.put(key, datablock);
                        }
                    }
                    Thread.sleep(SLEEP_MILLIS);
                }
            } catch (Exception e) {
                LOG_.log(Level.SEVERE, Thread.currentThread().getName() + ".run exception:", e);
                e.printStackTrace();
            } catch (ThreadDeath e) {
                LOG_.log(Level.SEVERE, Thread.currentThread().getName() + " has been stopped!", e);
                e.printStackTrace();
            }
            LOG_.severe("\n\n" + Thread.currentThread().getName() + " has died! Caching fails from here on.\n\n");
            active_ = false;
        }

        private boolean active_ = false;
    }

    @SuppressWarnings({ "FieldCanBeLocal" })
    private WriteCacheRunnable writecacherunnable_;

    private DiskCache diskcache_;

    private MemoryCache memorycache_;

    private final Object lock_ = new Object();

    private final Map<String, byte[]> pendingwrites_ = new HashMap<String, byte[]>();

    private static final Logger LOG_ = Logger.getLogger("cache");
}
