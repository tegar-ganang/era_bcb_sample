package org.jrobin.core;

import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.channels.FileChannel;

/**
 * JRobin backend which is used to store RRD data to ordinary files on the disk. This backend
 * is SAFE: it locks the underlying RRD file during update/fetch operations, and caches only static
 * parts of a RRD file in memory. Therefore, this backend is safe to be used when RRD files should
 * be shared between several JVMs at the same time. However, this backend is a little bit slow
 * since it does not use fast java.nio.* package (it's still based on the RandomAccessFile class).
 */
public class RrdSafeFileBackend extends RrdFileBackend {

    private static final Counters counters = new Counters();

    private FileLock m_lock;

    /**
	 * Creates RrdFileBackend object for the given file path, backed by RandomAccessFile object.
	 *
	 * @param path Path to a file
	 * @throws IOException Thrown in case of I/O error
	 */
    public RrdSafeFileBackend(final String path, final long lockWaitTime, final long lockRetryPeriod) throws IOException {
        super(path, false);
        try {
            lockFile(lockWaitTime, lockRetryPeriod);
        } catch (final IOException ioe) {
            super.close();
            throw ioe;
        }
    }

    private void lockFile(final long lockWaitTime, final long lockRetryPeriod) throws IOException {
        final long entryTime = System.currentTimeMillis();
        final FileChannel channel = file.getChannel();
        m_lock = channel.tryLock(0, Long.MAX_VALUE, false);
        if (m_lock != null) {
            counters.registerQuickLock();
            return;
        }
        do {
            try {
                Thread.sleep(lockRetryPeriod);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            m_lock = channel.tryLock(0, Long.MAX_VALUE, false);
            if (m_lock != null) {
                counters.registerDelayedLock();
                return;
            }
        } while (System.currentTimeMillis() - entryTime <= lockWaitTime);
        counters.registerError();
        throw new IOException("Could not obtain exclusive m_lock on file: " + getPath() + "] after " + lockWaitTime + " milliseconds");
    }

    public void close() throws IOException {
        try {
            if (m_lock != null) {
                m_lock.release();
                m_lock = null;
                counters.registerUnlock();
            }
        } finally {
            super.close();
        }
    }

    /**
	 * Defines the caching policy for this backend.
	 *
	 * @return <code>false</code>
	 */
    protected boolean isCachingAllowed() {
        return false;
    }

    public static String getLockInfo() {
        return counters.getInfo();
    }

    static class Counters {

        long locks, quickLocks, unlocks, locked, errors;

        synchronized void registerQuickLock() {
            locks++;
            quickLocks++;
            locked++;
        }

        synchronized void registerDelayedLock() {
            locks++;
            locked++;
        }

        synchronized void registerUnlock() {
            unlocks++;
            locked--;
        }

        synchronized void registerError() {
            errors++;
        }

        synchronized String getInfo() {
            return "LOCKS=" + locks + ", " + "UNLOCKS=" + unlocks + ", " + "DELAYED_LOCKS=" + (locks - quickLocks) + ", " + "LOCKED=" + locked + ", " + "ERRORS=" + errors;
        }
    }
}
