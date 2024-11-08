package com.art.anette.client.controller;

import java.util.ArrayList;
import java.util.List;
import com.art.anette.common.logging.LogController;
import com.art.anette.common.logging.Logger;

/**
 * @author mgee
 */
public class RWMutex {

    private Boolean writing = false;

    private Integer reader = 0;

    private static final RWMutex instance = new RWMutex();

    private static final int RETRY_TIME = 50;

    private static final int MAX_RETRIES = 60;

    private static final boolean TRACE_READS = true;

    private static final boolean SHOW_READ_LOCKING_OPS = false;

    private static final boolean SHOW_WRITE_LOCKING_OPS = false;

    private static final Logger logger = LogController.getLogger(BasicController.class);

    private long writeStartTime;

    private Throwable writeStartTrace;

    private List<ReadAccess> readers = new ArrayList<ReadAccess>();

    public static RWMutex getInstance() {
        return instance;
    }

    private synchronized boolean startWrite() {
        if (reader == 0 && !writing) {
            if (SHOW_WRITE_LOCKING_OPS) {
                logger.info(Thread.currentThread().getName() + " get a write lock");
            }
            writing = true;
            writeStartTime = System.currentTimeMillis();
            writeStartTrace = new Throwable();
            return true;
        } else {
            return false;
        }
    }

    private synchronized void stopWrite() {
        writing = false;
        writeStartTime = 0;
        writeStartTrace = null;
        if (SHOW_WRITE_LOCKING_OPS) {
            logger.info(Thread.currentThread().getName() + " returns a write lock");
        }
    }

    private synchronized boolean startRead() {
        if (writing) {
            return false;
        } else {
            if (SHOW_READ_LOCKING_OPS) {
                logger.info(Thread.currentThread().getName() + " gets a read lock");
            }
            reader++;
            if (TRACE_READS) {
                readers.add(new ReadAccess(new Throwable()));
            }
            return true;
        }
    }

    private synchronized void stopRead() {
        if (reader > 0) {
            if (SHOW_READ_LOCKING_OPS) {
                logger.info(Thread.currentThread().getName() + " returns a read lock");
            }
            reader--;
            if (TRACE_READS) {
                ReadAccess readAccess = new ReadAccess(new Throwable());
                final boolean removed = readers.remove(readAccess);
                if (!removed) {
                    throw new IllegalArgumentException("Can't find " + readAccess + " in readers " + readers);
                }
            }
        } else {
            logger.warning("Released too many readers!");
            throw new UnsupportedOperationException("There aren't any readers in here");
        }
    }

    public void lockForReading() {
        try {
            for (int i = 0; i < MAX_RETRIES; i++) {
                if (startRead()) {
                    return;
                }
                Thread.sleep(RETRY_TIME);
            }
            throw new IllegalStateException("Can't get a read lock. Tried for " + RETRY_TIME * MAX_RETRIES + "ms. The mutex was locked for write " + (System.currentTimeMillis() - writeStartTime) + "ms ago by the following party", writeStartTrace);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.severe(ex.getMessage(), ex);
        }
    }

    public void lockForWriting() {
        try {
            for (int i = 0; i < MAX_RETRIES; i++) {
                if (startWrite()) {
                    return;
                }
                Thread.sleep(RETRY_TIME);
            }
            if (writing) {
                throw new IllegalStateException("Can't get a write lock. Tried for " + RETRY_TIME * MAX_RETRIES + "ms. The mutex was locked for write " + (System.currentTimeMillis() - writeStartTime) + "ms ago by the following party", writeStartTrace);
            } else {
                throw new IllegalStateException("Can't get a write lock. Tried for " + RETRY_TIME * MAX_RETRIES + "ms. Some read lock is in the way. Open readers=" + readers);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.severe(ex.getMessage(), ex);
        }
    }

    public void unlockForWriting() {
        stopWrite();
    }

    public void unlockForReading() {
        stopRead();
    }

    private static class ReadAccess {

        private final Throwable trace;

        private final String topLevel;

        private final long time;

        private ReadAccess(Throwable trace) {
            this.trace = trace;
            topLevel = extractTopLevel();
            time = System.currentTimeMillis();
        }

        @SuppressWarnings({ "UseOfSystemOutOrSystemErr" })
        private String extractTopLevel() {
            final StackTraceElement[] elements = trace.getStackTrace();
            boolean found = false;
            for (StackTraceElement element : elements) {
                final String name = element.getClassName() + '.' + element.getMethodName();
                if (found) {
                    return name;
                }
                if (name.endsWith("Logic.lockReading") || name.endsWith("Logic.unlockReading")) {
                    found = true;
                }
            }
            for (StackTraceElement element : elements) {
                final String name = element.getClassName() + '.' + element.getMethodName();
                System.err.println("name='" + name + '\'');
            }
            throw new IllegalStateException("Can't find the top-level user method.");
        }

        @SuppressWarnings({ "ControlFlowStatementWithoutBraces", "RedundantIfStatement" })
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ReadAccess that = (ReadAccess) o;
            if (!topLevel.equals(that.topLevel)) return false;
            return true;
        }

        @SuppressWarnings({ "NonFinalFieldReferencedInHashCode" })
        public int hashCode() {
            return topLevel.hashCode();
        }

        public String toString() {
            return topLevel;
        }
    }
}
