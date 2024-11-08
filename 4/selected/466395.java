package net.sf.dropboxmq.dropboxsupport;

import java.io.File;
import java.util.Properties;
import net.sf.dropboxmq.Configuration;
import net.sf.dropboxmq.FileSystem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created: 15 Oct 2008
 *
 * @author <a href="mailto:dwayne@schultz.net">Dwayne Schultz</a>
 * @version $Revision: 235 $, $Date: 2011-08-27 00:55:14 -0400 (Sat, 27 Aug 2011) $
 */
public class Locks {

    private static final Log log = LogFactory.getLog(Locks.class);

    private static final long LOCK_LOG_INTERVAL = 1000L * 60L * 5L;

    private final Configuration configuration;

    private final FileSystem fileSystem;

    private final String destinationName;

    private final File outgoingTargetDir;

    private final File globalReadLockFile;

    private final File localReadLockFile;

    private final File writeLockPropertiesFile;

    private long writeLockPropertiesLastModified = -1L;

    private long writeLockDelay = -1L;

    private int writeLockTargetLimit = 0;

    private long lastLockLogWarnTime = 0L;

    public Locks(final DirectoryStructure structure, final String destinationName, final Configuration configuration) {
        this.destinationName = destinationName;
        this.configuration = configuration;
        fileSystem = configuration.getFileSystem();
        outgoingTargetDir = structure.getOutgoingStructure().getTargetDir();
        globalReadLockFile = new File(configuration.getRootDir(), "global-read-lock");
        localReadLockFile = new File(structure.getIncomingStructure().getInOutDir(), "read-lock");
        writeLockPropertiesFile = new File(structure.getOutgoingStructure().getInOutDir(), "write-lock.properties");
    }

    public boolean isReadLocked() {
        final boolean locked = fileSystem.exists(localReadLockFile) || fileSystem.exists(globalReadLockFile);
        if (locked) {
            final long current = System.currentTimeMillis();
            if (current - lastLockLogWarnTime > LOCK_LOG_INTERVAL) {
                log.warn("Destination " + destinationName + " is read locked" + ", local lock = " + fileSystem.exists(localReadLockFile) + ", global lock = " + fileSystem.exists(globalReadLockFile));
                lastLockLogWarnTime = current;
            }
        } else {
            lastLockLogWarnTime = 0L;
        }
        return locked;
    }

    public void checkWriteLock(final long startTime) {
        if (fileSystem.exists(writeLockPropertiesFile)) {
            boolean waiting = true;
            while (waiting) {
                final long currentLastModified = writeLockPropertiesFile.lastModified();
                if (writeLockPropertiesLastModified != currentLastModified) {
                    readWriteLockProperties();
                    writeLockPropertiesLastModified = currentLastModified;
                }
                final long current = System.currentTimeMillis();
                final long currentDelay = current - startTime;
                final boolean delaying = writeLockDelay == -1L || writeLockDelay > currentDelay;
                final int targetCount = outgoingTargetDir.list().length;
                final boolean targetLimited = writeLockTargetLimit < targetCount + 1;
                if (!delaying || !targetLimited || !fileSystem.exists(writeLockPropertiesFile)) {
                    waiting = false;
                }
                if (waiting) {
                    if (current - lastLockLogWarnTime > LOCK_LOG_INTERVAL) {
                        log.warn("Destination " + destinationName + " is write locked delayed " + currentDelay + " ms of " + (writeLockDelay == -1L ? "indefinite" : String.valueOf(writeLockDelay)) + " ms, " + targetCount + " backlogged messages with a limit of " + writeLockTargetLimit);
                        lastLockLogWarnTime = current;
                    }
                    try {
                        Thread.sleep(configuration.getPollingInterval());
                    } catch (InterruptedException e) {
                        log.warn("Interrupted while waiting for write lock, " + e.getMessage());
                        waiting = false;
                    }
                }
            }
        }
        lastLockLogWarnTime = 0L;
    }

    void readWriteLockProperties() {
        final Properties properties = new Properties();
        if (fileSystem.readPropertiesFile(writeLockPropertiesFile, properties)) {
            writeLockDelay = getLongProperty(properties, "delay-milliseconds", -1L);
            writeLockTargetLimit = (int) getLongProperty(properties, "target-limit", 0L);
        }
        log.debug("Read write lock properties " + properties);
    }

    long getLongProperty(final Properties properties, final String key, final long defaultValue) {
        long value = defaultValue;
        final String stringValue = properties.getProperty(key);
        if (stringValue != null) {
            try {
                value = Long.parseLong(stringValue);
            } catch (NumberFormatException ignore) {
                log.warn("Could not parse key " + key + " from " + writeLockPropertiesFile);
            }
        }
        return value;
    }

    protected final String toObjectString() {
        return super.toString();
    }
}
