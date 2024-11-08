package net.mikro2nd.s;

import com.google.common.base.Preconditions;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of UrlMap backed by a Properties file. Whilst this is not going to scale,
 * nor be particularly efficient, it allows a quick implementation to get going and usable for
 * onward experimentation.
 *
 * Note that changes to the contents of the Properties instance (including writes to backing
 * store) need to be synchronised or locked.
 */
class PropertyFileUrlMap implements UrlMap {

    public static final String PFX = App.PKG + "PropertyFileUrlMap.";

    public static final String CFG_FILEPATH = PFX + "filename";

    public static final String CFG_WRITE_INTERVAL = PFX + "writeInterval";

    public static final long DEFAULT_WRITE_CHECK_INTERVAL = 60000L;

    private final Logger syslog;

    private File backingFile;

    private final Thread writeThread;

    private long writeThreadCheckInterval;

    private final Properties urlMap;

    private long urlMapLastChange;

    private boolean keepRunning = false;

    private transient ShorteningStrategy shortener;

    private transient DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * {@inheritdoc}
     */
    @Override
    public String keyFor(final String url) {
        if (urlMap.containsValue(url)) {
            for (String key : urlMap.stringPropertyNames()) {
                if (url.equals(urlMap.getProperty(key))) return key;
            }
        }
        final String key = shortener.shorten(url);
        map(key, url);
        return key;
    }

    @Override
    public String urlFor(final String key) {
        return urlMap.getProperty(key);
    }

    @Override
    public void map(final String key, final String url) {
        synchronized (urlMap) {
            urlMap.setProperty(key, url);
            urlMapLastChange = System.currentTimeMillis();
        }
    }

    @Override
    public void shutdown() {
        if (writeThread != null) {
            shutdownWriter();
            saveMap();
        }
    }

    @Override
    public boolean containsKey(final String aKey) {
        return urlMap.containsKey(aKey);
    }

    /**
     * Let the writer-thread know it should stop, and wait for it to finish its last cycle.
     */
    private void shutdownWriter() {
        keepRunning = false;
        if (writeThread == null || !writeThread.isAlive()) return;
        writeThread.interrupt();
        try {
            writeThread.join();
        } catch (InterruptedException e) {
            syslog.log(Level.SEVERE, "Interrupted while waiting for write thread to join. Data file may not be saved.", e);
        }
    }

    /**
     * Save the current state of the Properties instance to backing store.
     *
     * Writes the Properties map to a temporary file, renames the previous datafile
     * out of the way (by appending a ".O" suffix to the filename), renames the new
     * temporary file to the datafile name. We don't delete the previous datafile
     * until the next round of writing so that there is a backup in case of disaster.
     *
     * It's not foolproof, just good enough for now.
     * 
     * It is (at the time of writing) unnecessary to synchronise or lock this
     * file-writing and swapping process against race conditions since it is
     * only ever done by the writerThread, or by the main thread after the
     * writerThread has terminated.
     */
    private void saveMap() {
        try {
            File tmpFile = File.createTempFile("shrtn", "");
            if (syslog.isLoggable(Level.FINE)) {
                syslog.log(Level.FINE, "Saving map to {0} at {1}", new Object[] { tmpFile.getAbsolutePath(), new Date() });
            }
            synchronized (urlMap) {
                final Writer out = new FileWriter(tmpFile);
                urlMap.store(out, "Last saved: " + dateFormat.format(new Date()));
                out.close();
            }
            final String dataFileName = backingFile.getAbsolutePath();
            File backup = new File(dataFileName + ".O");
            if (backup.exists()) backup.delete();
            backingFile.renameTo(backup);
            tmpFile.renameTo(new File(dataFileName));
            backingFile = tmpFile;
        } catch (IOException e) {
            syslog.log(Level.SEVERE, "IOException writing map", e);
        }
    }

    private long writerCheckInterval() {
        return writeThreadCheckInterval;
    }

    /**
     * General constructor initialising a Properties file backed UrlMap stored in the
     * file named by the given file pathname.
     * @param backingFilePath Full pathname of backing Properties file.
     * @throws IOException If any of the file or I/O operations fail.
     */
    public PropertyFileUrlMap(final Properties config) throws IOException {
        syslog = Logger.getLogger(config.getProperty(App.CFG_LOG_SYSTEM));
        writeThreadCheckInterval = config.getProperty(CFG_WRITE_INTERVAL) == null ? DEFAULT_WRITE_CHECK_INTERVAL : Long.valueOf(config.getProperty(CFG_WRITE_INTERVAL)) * 1000L;
        backingFile = new File(config.getProperty(CFG_FILEPATH));
        final boolean isNewBackingStore = backingFile.createNewFile();
        final FileReader in = new FileReader(backingFile);
        try {
            urlMap = new Properties();
            urlMap.load(in);
        } finally {
            in.close();
        }
        shortener = new BrokenSoManyWaysShortenerStrategy(this);
        writeThread = new Thread(new WriteDaemon());
        writeThread.setName("WriterThread");
        writeThread.setPriority(Math.max(Thread.currentThread().getPriority() - 1, Thread.MIN_PRIORITY));
        writeThread.setDaemon(true);
        keepRunning = true;
        writeThread.start();
    }

    /**
     * Constructor for use by unit tests. Backing store is the Properties object given,
     * and <strong>no</strong> background thread is started to periodically write changes
     * to the map.
     */
    PropertyFileUrlMap(final String urlDomain, final Properties map) {
        syslog = Logger.getLogger(getClass().getName());
        writeThreadCheckInterval = Long.MAX_VALUE;
        backingFile = null;
        writeThread = null;
        shortener = new BrokenSoManyWaysShortenerStrategy(this);
        urlMap = Preconditions.checkNotNull(map);
    }

    private class WriteDaemon implements Runnable {

        @Override
        public void run() {
            long lastCheckTime = System.currentTimeMillis();
            syslog.log(Level.CONFIG, "{0} started at {1}", new Object[] { Thread.currentThread().getName(), Long.valueOf(lastCheckTime) });
            while (keepRunning) {
                try {
                    Thread.sleep(writerCheckInterval());
                    if (syslog.isLoggable(Level.FINE)) {
                        syslog.log(Level.FINE, "{0} running at {1} (last map change at {2})", new Object[] { Thread.currentThread().getName(), Long.valueOf(lastCheckTime), Long.valueOf(urlMapLastChange) });
                    }
                    if (urlMapLastChange > lastCheckTime) {
                        saveMap();
                    }
                    lastCheckTime = System.currentTimeMillis();
                } catch (InterruptedException e) {
                }
            }
        }
    }
}
